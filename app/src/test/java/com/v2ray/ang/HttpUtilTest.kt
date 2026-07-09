package com.v2ray.ang

import com.v2ray.ang.util.HttpUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class HttpUtilTest {

    @Test
    fun testIdnToASCII() {
        
        val regularUrl = "https:
        assertEquals(regularUrl, HttpUtil.toIdnUrl(regularUrl))

        
        val nonAsciiUrl = "https:
        val expectedNonAscii = "https:
        assertEquals(expectedNonAscii, HttpUtil.toIdnUrl(nonAsciiUrl))

        
        val mixedUrl = "https:
        val expectedMixed = "https:
        assertEquals(expectedMixed, HttpUtil.toIdnUrl(mixedUrl))

        
        val basicAuthUrl = "https:
        assertEquals(basicAuthUrl, HttpUtil.toIdnUrl(basicAuthUrl))

        
        val basicAuthNonAscii = "https:
        val expectedBasicAuthNonAscii = "https:
        assertEquals(expectedBasicAuthNonAscii, HttpUtil.toIdnUrl(basicAuthNonAscii))

        
        val nonAsciiAuth = "https:
        
        assertEquals(nonAsciiAuth, HttpUtil.toIdnUrl(nonAsciiAuth))
    }


}