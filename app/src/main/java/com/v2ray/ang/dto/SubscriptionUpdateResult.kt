package com.v2ray.ang.dto


data class SubscriptionUpdateResult(
    val configCount: Int = 0,      
    val successCount: Int = 0,     
    val failureCount: Int = 0,     
    val skipCount: Int = 0         
) {
    
    operator fun plus(other: SubscriptionUpdateResult): SubscriptionUpdateResult {
        return SubscriptionUpdateResult(
            configCount = this.configCount + other.configCount,
            successCount = this.successCount + other.successCount,
            failureCount = this.failureCount + other.failureCount,
            skipCount = this.skipCount + other.skipCount
        )
    }
}

