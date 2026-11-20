package com.voltsrp.mobile

import android.content.Context

object LanguageManager {

    private const val PREFS = "volts_settings"
    private const val KEY_LANGUAGE = "selected_language"

    const val PT = "pt"
    const val EN = "en"
    const val ES = "es"

    fun getLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, PT) ?: PT
    }

    fun setLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    fun text(context: Context, key: String): String {
        val language = getLanguage(context)

        val translations = mapOf(
            "connect_city" to mapOf(
                PT to "CONECTE-SE E ENTRE NA CIDADE",
                EN to "CONNECT AND ENTER THE CITY",
                ES to "CONECTATE Y ENTRA A LA CIUDAD"
            ),
            "connecting" to mapOf(
                PT to "CONECTANDO...",
                EN to "CONNECTING...",
                ES to "CONECTANDO..."
            ),
            "google_login" to mapOf(
                PT to "Conecte-se com o Google",
                EN to "Sign in with Google",
                ES to "Iniciar sesión con Google"
            ),
            "terms_agreement" to mapOf(
                PT to "Eu concordo com os",
                EN to "I agree to the",
                ES to "Estoy de acuerdo con los"
            ),
            "terms" to mapOf(
                PT to "Termos de Uso",
                EN to "Terms of Service",
                ES to "Términos de servicio"
            ),
            "privacy" to mapOf(
                PT to "Política de Privacidade",
                EN to "Privacy Policy",
                ES to "Política de privacidad"
            )
        )

        return translations[key]?.get(language)
            ?: translations[key]?.get(PT)
            ?: key
    }
}
