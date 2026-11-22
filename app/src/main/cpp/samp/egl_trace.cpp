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

using EglGetDisplayFn = EGLDisplay (*)(EGLNativeDisplayType);
using EglInitializeFn = EGLBoolean (*)(EGLDisplay, EGLint*, EGLint*);
using EglChooseConfigFn = EGLBoolean (*)(EGLDisplay, const EGLint*, EGLConfig*, EGLint, EGLint*);
using EglCreateContextFn = EGLContext (*)(EGLDisplay, EGLConfig, EGLContext, const EGLint*);
using EglMakeCurrentFn = EGLBoolean (*)(EGLDisplay, EGLSurface, EGLSurface, EGLContext);
using EglGetErrorFn = EGLint (*)();
using EglGetConfigAttribFn = EGLBoolean (*)(EGLDisplay, EGLConfig, EGLint, EGLint*);
using EglCreateWindowSurfaceFn = EGLSurface (*)(EGLDisplay, EGLConfig, EGLNativeWindowType, const EGLint*);
using EglDestroySurfaceFn = EGLBoolean (*)(EGLDisplay, EGLSurface);
using EglSwapIntervalFn = EGLBoolean (*)(EGLDisplay, EGLint);
using EglTerminateFn = EGLBoolean (*)(EGLDisplay);

static EglGetDisplayFn g_origEglGetDisplay = nullptr;
static EglInitializeFn g_origEglInitialize = nullptr;
static EglChooseConfigFn g_origEglChooseConfig = nullptr;
static EglCreateContextFn g_origEglCreateContext = nullptr;
static EglMakeCurrentFn g_origEglMakeCurrent = nullptr;
static EglGetErrorFn g_origEglGetError = nullptr;
static EglGetConfigAttribFn g_origEglGetConfigAttrib = nullptr;
static EglCreateWindowSurfaceFn g_origEglCreateWindowSurface = nullptr;
static EglDestroySurfaceFn g_origEglDestroySurface = nullptr;
static EglSwapIntervalFn g_origEglSwapInterval = nullptr;
static EglTerminateFn g_origEglTerminate = nullptr;

static void LogHookResult(const char* symbol, void* stub) {
    const int error = shadowhook_get_errno();
    const char* message = shadowhook_to_errmsg(error);
    EglTrace("EGL_HOOK symbol=%s stub=%p errno=%d message=%s",
             symbol, stub, error, message ? message : "null");
}

static EGLDisplay HookEglGetDisplay(EGLNativeDisplayType nativeDisplay) {
    EglTrace("EGL eglGetDisplay ENTER nativeDisplay=%p", nativeDisplay);
    EGLDisplay display = g_origEglGetDisplay(nativeDisplay);
    EglTrace("EGL eglGetDisplay EXIT display=%p", display);
    return display;
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

static EGLSurface HookEglCreateWindowSurface(EGLDisplay display, EGLConfig config,
                                               EGLNativeWindowType window, const EGLint* attrs) {
    EglTrace("EGL eglCreateWindowSurface ENTER display=%p config=%p window=%p", display, config, window);
    TraceEglAttrs("eglCreateWindowSurface", attrs);
    EGLSurface surface = g_origEglCreateWindowSurface(display, config, window, attrs);
    EglTrace("EGL eglCreateWindowSurface EXIT surface=%p", surface);
    return surface;
}

static EGLBoolean HookEglDestroySurface(EGLDisplay display, EGLSurface surface) {
    EglTrace("EGL eglDestroySurface ENTER display=%p surface=%p", display, surface);
    EGLBoolean ok = g_origEglDestroySurface(display, surface);
    EglTrace("EGL eglDestroySurface EXIT result=%d", ok);
    return ok;
}

static EGLBoolean HookEglSwapInterval(EGLDisplay display, EGLint interval) {
    EglTrace("EGL eglSwapInterval ENTER display=%p interval=%d", display, interval);
    EGLBoolean ok = g_origEglSwapInterval(display, interval);
    EglTrace("EGL eglSwapInterval EXIT result=%d", ok);
    return ok;
}

static EGLBoolean HookEglTerminate(EGLDisplay display) {
    EglTrace("EGL eglTerminate ENTER display=%p", display);
    EGLBoolean ok = g_origEglTerminate(display);
    EglTrace("EGL eglTerminate EXIT result=%d", ok);
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

    EglTrace("EGL_TRACE_INSTALL_BEGIN target=libEGL.so version=%s",
             shadowhook_get_version());

    void* eglHandle = shadowhook_dlopen("libEGL.so");
    EglTrace("EGL_TRACE_LIBRARY name=libEGL.so handle=%p", eglHandle);
    if (eglHandle) {
        const char* names[] = {"eglGetDisplay", "eglInitialize", "eglChooseConfig",
                               "eglGetConfigAttrib", "eglCreateContext", "eglCreateWindowSurface",
                               "eglDestroySurface", "eglMakeCurrent", "eglSwapInterval",
                               "eglTerminate", "eglGetError"};
        for (const char* name : names) {
            void* address = shadowhook_dlsym_dynsym(eglHandle, name);
            EglTrace("EGL_TRACE_DYNSYM library=libEGL.so symbol=%s address=%p", name, address);
        }
        shadowhook_dlclose(eglHandle);
    }

    void* h0 = shadowhook_hook_sym_name("libEGL.so", "eglGetDisplay",
            reinterpret_cast<void*>(HookEglGetDisplay),
            reinterpret_cast<void**>(&g_origEglGetDisplay));
    LogHookResult("eglGetDisplay", h0);
    void* h1 = shadowhook_hook_sym_name("libEGL.so", "eglInitialize",
            reinterpret_cast<void*>(HookEglInitialize),
            reinterpret_cast<void**>(&g_origEglInitialize));
    LogHookResult("eglInitialize", h1);
    void* h2 = shadowhook_hook_sym_name("libEGL.so", "eglChooseConfig",
            reinterpret_cast<void*>(HookEglChooseConfig),
            reinterpret_cast<void**>(&g_origEglChooseConfig));
    LogHookResult("eglChooseConfig", h2);
    void* h3 = shadowhook_hook_sym_name("libEGL.so", "eglGetConfigAttrib",
            reinterpret_cast<void*>(HookEglGetConfigAttrib),
            reinterpret_cast<void**>(&g_origEglGetConfigAttrib));
    LogHookResult("eglGetConfigAttrib", h3);
    void* h4 = shadowhook_hook_sym_name("libEGL.so", "eglCreateContext",
            reinterpret_cast<void*>(HookEglCreateContext),
            reinterpret_cast<void**>(&g_origEglCreateContext));
    LogHookResult("eglCreateContext", h4);
    void* h5 = shadowhook_hook_sym_name("libEGL.so", "eglMakeCurrent",
            reinterpret_cast<void*>(HookEglMakeCurrent),
            reinterpret_cast<void**>(&g_origEglMakeCurrent));
    LogHookResult("eglMakeCurrent", h5);
    void* h6 = shadowhook_hook_sym_name("libEGL.so", "eglGetError",
            reinterpret_cast<void*>(HookEglGetError),
            reinterpret_cast<void**>(&g_origEglGetError));
    LogHookResult("eglGetError", h6);

    void* h7 = shadowhook_hook_sym_name("libEGL.so", "eglCreateWindowSurface",
            reinterpret_cast<void*>(HookEglCreateWindowSurface),
            reinterpret_cast<void**>(&g_origEglCreateWindowSurface));
    LogHookResult("eglCreateWindowSurface", h7);
    void* h8 = shadowhook_hook_sym_name("libEGL.so", "eglDestroySurface",
            reinterpret_cast<void*>(HookEglDestroySurface),
            reinterpret_cast<void**>(&g_origEglDestroySurface));
    LogHookResult("eglDestroySurface", h8);
    void* h9 = shadowhook_hook_sym_name("libEGL.so", "eglSwapInterval",
            reinterpret_cast<void*>(HookEglSwapInterval),
            reinterpret_cast<void**>(&g_origEglSwapInterval));
    LogHookResult("eglSwapInterval", h9);
    void* h10 = shadowhook_hook_sym_name("libEGL.so", "eglTerminate",
            reinterpret_cast<void*>(HookEglTerminate),
            reinterpret_cast<void**>(&g_origEglTerminate));
    LogHookResult("eglTerminate", h10);

    EglTrace("EGL_TRACE_INSTALL_RESULT getDisplay=%p initialize=%p chooseConfig=%p "
             "getConfigAttrib=%p createContext=%p makeCurrent=%p getError=%p "
             "createWindowSurface=%p destroySurface=%p swapInterval=%p terminate=%p",
             h0, h1, h2, h3, h4, h5, h6, h7, h8, h9, h10);
}
