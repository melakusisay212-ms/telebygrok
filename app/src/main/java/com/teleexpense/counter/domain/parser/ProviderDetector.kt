package com.teleexpense.counter.domain.parser

import com.teleexpense.counter.domain.model.Provider

object ProviderDetector {

    fun detect(sender: String, body: String): Provider {
        val s = sender.lowercase()
        val b = body.lowercase()
        when {
            s.contains("telebirr") || s.contains("tele-birr") -> return Provider.TELEBIRR
            s.contains("ebirr") || s.contains("e-birr") || s.contains("kaafi") -> return Provider.EBIRR
            s.contains("ethio") || s.contains("telecom") -> return Provider.ETHIO_TELECOM
            listOf("cbe", "dashen", "awash", "abyssinia", "coop bank").any { s.contains(it) } ->
                return Provider.BANK
        }
        when {
            listOf("telebirr", "tele birr").any { b.contains(it) } -> return Provider.TELEBIRR
            listOf("ebirr", "e-birr", "kaafi").any { b.contains(it) } -> return Provider.EBIRR
            listOf("ethio telecom", "ethiotelecom").any { b.contains(it) } -> return Provider.ETHIO_TELECOM
        }
        if (b.contains("airtime") || b.contains("data package") || b.contains("prepaid account") ||
            (b.contains("package") && (b.contains("mb") || b.contains("gb") || b.contains("sms")))
        ) return Provider.ETHIO_TELECOM
        return Provider.UNKNOWN
    }
}
