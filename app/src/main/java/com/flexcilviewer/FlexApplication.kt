package com.flexcilviewer

import android.app.Application
import com.flexcilviewer.crash.CrashHandler
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class FlexApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize PDFBox resource loader once for the entire app lifetime
        PDFBoxResourceLoader.init(applicationContext)

        // Register global crash handler — shows a crash report screen on any unhandled exception
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            CrashHandler(applicationContext, defaultHandler)
        )
    }
}
