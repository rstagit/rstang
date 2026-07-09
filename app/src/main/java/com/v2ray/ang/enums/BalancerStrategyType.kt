package com.v2ray.ang.enums


enum class BalancerStrategyType(
    val policyGroupType: String,
    val policyGroupTypeValue: String,
    val requiresBurstObservatory: Boolean = false,
    val requiresObservatory: Boolean = false,
) {
    LEAST_LOAD("leastLoad", "1", requiresBurstObservatory = true),
    RANDOM("random", "2"),
    ROUND_ROBIN("roundRobin", "3"),
    LEAST_PING("leastPing", "", requiresObservatory = true); 

    companion object {
        fun from(policyGroupType: String?): BalancerStrategyType =
            entries.firstOrNull { it.policyGroupTypeValue == policyGroupType } ?: LEAST_PING
    }
}