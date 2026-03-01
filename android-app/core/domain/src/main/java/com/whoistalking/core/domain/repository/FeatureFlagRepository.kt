package com.whoistalking.core.domain.repository

import com.whoistalking.core.domain.model.FeatureFlags
import kotlinx.coroutines.flow.Flow

interface FeatureFlagRepository {
    fun observeFlags(): Flow<FeatureFlags>
}
