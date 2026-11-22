#include <jni.h>
#include <EGL/egl.h>
#include <android/log.h>
#include <shadowhook.h>
#include <atomic>
#include <cstdarg>
#include <cstdio>
#include <unistd.h>

static std::atomic<int> g_eglTraceFd{-1};

static void EglTrace(const char* format, ...) {
    int fd = g_eglTraceFd.load(std::memory_order_relaxed);
    if (fd < 0) return;
    char line[1024];
    va_list args;
    va_start(args, format);
    int n = vsnprintf(line, sizeof(line) - 2, format, args);
    va_end(args);
    if (n <= 0) return;
    size_t count = static_cast<size_t>(n);
    if (count > sizeof(line) - 2) count = sizeof(line) - 2;
    line[count++] = '\n';
    const char* p = line;
    while (count) {
        ssize_t written = write(fd, p, count);
        if (written <= 0) break;
        p += written;
        count -= static_cast<size_t>(written);
    }
    fsync(fd);
}

static void TraceEglAttrs(const char* label, const EGLint* attrs) {
    if (!attrs) {
        EglTrace("%s attrs=null", label);
        return;
    }
    char out[512];
    size_t used = 0;
    for (int i = 0; i < 32 && used < sizeof(out) - 32; i += 2) {
        EGLint key = attrs[i];
        if (key == EGL_NONE) {
            used += static_cast<size_t>(snprintf(out + used, sizeof(out) - used, " EGL_NONE"));
            break;
        }
        EGLint value = attrs[i + 1];
        used += static_cast<size_t>(snprintf(out + used, sizeof(out) - used,
                " 0x%x=%d(0x%x)", key, value, value));
    }
    EglTrace("%s attrs:%s", label, out);
}

using EglInitializeFn = EGLBoolean (*)(EGLDisplay, EGLint*, EGLint*);
using EglChooseConfigFn = EGLBoolean (*)(EGLDisplay, const EGLint*, EGLConfig*, EGLint, EGLint*);
using EglCreateContextFn = EGLContext (*)(EGLDisplay, EGLConfig, EGLContext, const EGLint*);
using EglMakeCurrentFn = EGLBoolean (*)(EGLDisplay, EGLSurface, EGLSurface, EGLContext);
using EglGetErrorFn = EGLint (*)();
using EglGetConfigAttribFn = EGLBoolean (*)(EGLDisplay, EGLConfig, EGLint, EGLint*);

static EglInitializeFn g_origEglInitialize = nullptr;
static EglChooseConfigFn g_origEglChooseConfig = nullptr;
static EglCreateContextFn g_origEglCreateContext = nullptr;
static EglMakeCurrentFn g_origEglMakeCurrent = nullptr;
static EglGetErrorFn g_origEglGetError = nullptr;
static EglGetConfigAttribFn g_origEglGetConfigAttrib = nullptr;

static void LogHookResult(const char* symbol, void* stub) {
    const int error = shadowhook_get_errno();
    const char* message = shadowhook_to_errmsg(error);
    EglTrace("EGL_HOOK symbol=%s stub=%p errno=%d message=%s",
             symbol, stub, error, message ? message : "null");
}

static EGLBoolean HookEglInitialize(EGLDisplay display, EGLint* major, EGLint* minor) {
    EglTrace("EGL eglInitialize ENTER display=%p", display);
    EGLBoolean ok = g_origEglInitialize(display, major, minor);
    EglTrace("EGL eglInitialize EXIT result=%d version=%d.%d", ok,
             major ? *major : -1, minor ? *minor : -1);
    return ok;
}

static EGLBoolean HookEglChooseConfig(EGLDisplay display, const EGLint* attrs,
                                      EGLConfig* configs, EGLint configSize, EGLint* count) {
    TraceEglAttrs("eglChooseConfig", attrs);
    EGLBoolean ok = g_origEglChooseConfig(display, attrs, configs, configSize, count);
    EglTrace("EGL eglChooseConfig EXIT result=%d configSize=%d numConfigs=%d firstConfig=%p",
             ok, configSize, count ? *count : -1,
             (configs && count && *count > 0) ? configs[0] : nullptr);
    return ok;
}

static EGLBoolean HookEglGetConfigAttrib(EGLDisplay display, EGLConfig config,
                                          EGLint attribute, EGLint* value) {
    EGLBoolean ok = g_origEglGetConfigAttrib(display, config, attribute, value);
    EglTrace("EGL eglGetConfigAttrib config=%p attr=0x%x result=%d value=%d",
             config, attribute, ok, (ok && value) ? *value : -1);
    return ok;
}

static EGLContext HookEglCreateContext(EGLDisplay display, EGLConfig config,
                                       EGLContext share, const EGLint* attrs) {
    TraceEglAttrs("eglCreateContext", attrs);
    EglTrace("EGL eglCreateContext ENTER display=%p config=%p share=%p",
             display, config, share);
    EGLContext context = g_origEglCreateContext(display, config, share, attrs);
    EglTrace("EGL eglCreateContext EXIT context=%p", context);
    return context;
}

static EGLBoolean HookEglMakeCurrent(EGLDisplay display, EGLSurface draw,
                                     EGLSurface read, EGLContext context) {
    EGLBoolean ok = g_origEglMakeCurrent(display, draw, read, context);
    EglTrace("EGL eglMakeCurrent result=%d display=%p draw=%p read=%p context=%p",
             ok, display, draw, read, context);
    return ok;
}

static EGLint HookEglGetError() {
    EGLint error = g_origEglGetError();
    EglTrace("EGL eglGetError=0x%x", error);
    return error;
}

extern "C" JNIEXPORT void JNICALL
Java_com_voltsrp_mobile_oswrapper_GameNative_startEglTrace(
        JNIEnv*, jclass, jint fileDescriptor) {
    if (fileDescriptor < 0) {
        EglTrace("EGL_TRACE_INSTALL_FAILED invalid_fd=%d", fileDescriptor);
        return;
    }
    int duplicate = dup(fileDescriptor);
    if (duplicate < 0) {
        EglTrace("EGL_TRACE_INSTALL_FAILED dup_failed");
        return;
    }
    int previous = g_eglTraceFd.exchange(duplicate);
    if (previous >= 0) close(previous);

    EglTrace("EGL_TRACE_INSTALL_BEGIN target=libGame.so version=%s init_errno=%d",
             shadowhook_get_version(), shadowhook_get_init_errno());

    void* gameHandle = shadowhook_dlopen("libGame.so");
    EglTrace("EGL_TRACE_LIBRARY handle=%p", gameHandle);
    if (gameHandle) {
        const char* names[] = {"eglInitialize", "eglChooseConfig", "eglGetConfigAttrib",
                               "eglCreateContext", "eglMakeCurrent", "eglGetError"};
        for (const char* name : names) {
            void* address = shadowhook_dlsym_dynsym(gameHandle, name);
            EglTrace("EGL_TRACE_DYNSYM symbol=%s address=%p", name, address);
        }
        shadowhook_dlclose(gameHandle);
    }

    void* h1 = shadowhook_hook_sym_name("libGame.so", "eglInitialize",
            reinterpret_cast<void*>(HookEglInitialize),
            reinterpret_cast<void**>(&g_origEglInitialize));
    LogHookResult("eglInitialize", h1);
    void* h2 = shadowhook_hook_sym_name("libGame.so", "eglChooseConfig",
            reinterpret_cast<void*>(HookEglChooseConfig),
            reinterpret_cast<void**>(&g_origEglChooseConfig));
    LogHookResult("eglChooseConfig", h2);
    void* h3 = shadowhook_hook_sym_name("libGame.so", "eglGetConfigAttrib",
            reinterpret_cast<void*>(HookEglGetConfigAttrib),
            reinterpret_cast<void**>(&g_origEglGetConfigAttrib));
    LogHookResult("eglGetConfigAttrib", h3);
    void* h4 = shadowhook_hook_sym_name("libGame.so", "eglCreateContext",
            reinterpret_cast<void*>(HookEglCreateContext),
            reinterpret_cast<void**>(&g_origEglCreateContext));
    LogHookResult("eglCreateContext", h4);
    void* h5 = shadowhook_hook_sym_name("libGame.so", "eglMakeCurrent",
            reinterpret_cast<void*>(HookEglMakeCurrent),
            reinterpret_cast<void**>(&g_origEglMakeCurrent));
    LogHookResult("eglMakeCurrent", h5);
    void* h6 = shadowhook_hook_sym_name("libGame.so", "eglGetError",
            reinterpret_cast<void*>(HookEglGetError),
            reinterpret_cast<void**>(&g_origEglGetError));
    LogHookResult("eglGetError", h6);

    EglTrace("EGL_TRACE_INSTALL_RESULT initialize=%p chooseConfig=%p getConfigAttrib=%p "
             "createContext=%p makeCurrent=%p getError=%p",
             h1, h2, h3, h4, h5, h6);
}
