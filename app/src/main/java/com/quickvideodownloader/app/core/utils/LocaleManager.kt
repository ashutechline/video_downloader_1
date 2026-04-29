package com.quickvideodownloader.app.core.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleManager {

    fun setLocale(context: Context, languageCode: String): Context {
        val locale = if (languageCode == "auto") {
            Locale.getDefault()
        } else {
            Locale(languageCode)
        }
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    fun updateResources(context: Context, languageCode: String): Context {
        val locale = if (languageCode == "auto") {
            Locale.getDefault()
        } else {
            Locale(languageCode)
        }
        Locale.setDefault(locale)
        val res = context.resources
        val config = Configuration(res.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
