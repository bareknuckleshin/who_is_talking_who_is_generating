package com.whoistalking.core.data.security

import android.content.Context
import javax.inject.Inject

class IntegrityChecker @Inject constructor(
    private val context: Context,
) {
    suspend fun verify(): Boolean {
        // Stub for Play Integrity API binding.
        return context.packageName.isNotBlank()
    }
}
