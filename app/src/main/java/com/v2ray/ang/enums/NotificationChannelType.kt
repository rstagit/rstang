package com.v2ray.ang.enums


enum class NotificationChannelType(
    val channelId: String,
    val channelName: String,
    val notificationId: Int
) {
    SUBSCRIPTION_UPDATE(
        channelId = "subscription_update_channel",
        channelName = "Subscription Update Service",
        notificationId = 13
    ),
    CORE_TEST(
        channelId = "core_test_channel",
        channelName = "Core Test Service",
        notificationId = 12
    ),
    CORE_PROXY(
        channelId = "core_proxy_channel",
        channelName = "Core Proxy Service",
        notificationId = 14
    ),
    RSTA_SCANNER(
        channelId = "rsta_scanner_channel",
        channelName = "RSTA Scanner Service",
        notificationId = 15
    )
}