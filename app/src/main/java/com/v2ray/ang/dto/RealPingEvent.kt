package com.v2ray.ang.dto

sealed class RealPingEvent {

    
    data class Progress(val text: String) : RealPingEvent()

    
    data class Result(val guid: String, val delayMillis: Long) : RealPingEvent()

    
    data class Finish(val status: String) : RealPingEvent()
}

