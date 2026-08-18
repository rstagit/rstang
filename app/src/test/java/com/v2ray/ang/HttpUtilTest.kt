package com.v2ray.ang

import com.v2ray.ang.util.HttpUtil
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.IDN

class HttpUtilTest {

    @Test
    fun testIdnToASCII() {
        
        val regularUrl = "https://www.google.com"
        assertEquals(regularUrl, HttpUtil.toIdnUrl(regularUrl))

        
        val nonAsciiUrl = "https://مثال.آزمایشی"
        
        
        val host = "مثال.آزمایشی"
        val asciiHost = IDN.toASCII(host, IDN.ALLOW_UNASSIGNED)
        val expectedNonAscii = "https://$asciiHost"
        assertEquals(expectedNonAscii, HttpUtil.toIdnUrl(nonAsciiUrl))

        
        val mixedUrl = "https://مثال.com"
        val mixedHost = "مثال.com"
        val asciiMixedHost = IDN.toASCII(mixedHost, IDN.ALLOW_UNASSIGNED)
        val expectedMixed = "https://$asciiMixedHost"
        assertEquals(expectedMixed, HttpUtil.toIdnUrl(mixedUrl))

        
        val basicAuthUrl = "https://user:pass@www.google.com"
        assertEquals(basicAuthUrl, HttpUtil.toIdnUrl(basicAuthUrl))

        
        val basicAuthNonAscii = "https://user:pass@مثال.آزمایشی"
        val expectedBasicAuthNonAscii = "https://user:pass@$asciiHost"
        assertEquals(expectedBasicAuthNonAscii, HttpUtil.toIdnUrl(basicAuthNonAscii))

        
        val nonAsciiAuth = "https://کاربر:رمز@example.com"
        
        assertEquals(nonAsciiAuth, HttpUtil.toIdnUrl(nonAsciiAuth))
    }
}
