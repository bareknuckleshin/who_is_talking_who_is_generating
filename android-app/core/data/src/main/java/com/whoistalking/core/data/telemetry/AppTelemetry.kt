package com.whoistalking.core.data.telemetry

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.sentry.Sentry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppTelemetry @Inject constructor() {
    fun trackEvent(name: String, params: Map<String, Any>) {
        Log.d("Analytics", "$name=$params")
    }

    fun recordError(throwable: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(throwable)
        Sentry.captureException(throwable)
    }
}
