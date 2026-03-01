package com.whoistalking.core.data.featureflag

import com.whoistalking.core.domain.model.FeatureFlags
import com.whoistalking.core.domain.repository.FeatureFlagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlagRepositoryImpl @Inject constructor() : FeatureFlagRepository {
    private val flags = MutableStateFlow(FeatureFlags(canaryNewSessionUi = false))
    override fun observeFlags(): Flow<FeatureFlags> = flags
}
