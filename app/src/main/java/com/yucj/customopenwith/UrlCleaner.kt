package com.yucj.customopenwith

import java.net.URLDecoder

/**
 * Removes tracking query parameters and unwraps Meta's link shim
 * (l.facebook.com/l.php?u=...). The parameter list is a curated subset of
 * open-source blocklists (ClearURLs rules / Firefox URL Query Stripping).
 * Implemented with plain string handling (no android.net.Uri) so it can be
 * unit-tested on the JVM.
 */
object UrlCleaner {

    private val TRACKING_PARAMS = setOf(
        // Meta
        "fbclid", "igsh", "igshid", "mibextid",
        // Google
        "gclid", "gbraid", "wbraid", "dclid", "srsltid",
        // Microsoft / Twitter / TikTok / LinkedIn / Yandex
        "msclkid", "twclid", "ttclid", "li_fat_id", "yclid",
        // Email/marketing platforms (Mailchimp, HubSpot, Marketo, Vero, Ortto)
        "mc_eid", "_hsenc", "_hsmi", "mkt_tok", "vero_id",
        "oly_anon_id", "oly_enc_id", "__s", "wickedid",
    )

    private val TRACKING_PREFIXES = listOf("utm_")

    private val LINK_SHIM_HOSTS = setOf(
        "l.facebook.com", "lm.facebook.com", "l.messenger.com", "l.instagram.com",
    )

    fun clean(url: String): String {
        var result = url
        repeat(3) {
            val unwrapped = unwrapLinkShim(result)
            if (unwrapped == result) return@repeat
            result = unwrapped
        }
        return stripTrackingParams(result)
    }

    private fun unwrapLinkShim(url: String): String {
        val host = hostOf(url) ?: return url
        if (host !in LINK_SHIM_HOSTS) return url
        val query = url.substringAfter('?', "").substringBefore('#')
        for (pair in query.split('&')) {
            if (pair.substringBefore('=') == "u") {
                val target = runCatching {
                    URLDecoder.decode(pair.substringAfter('=', ""), "UTF-8")
                }.getOrNull() ?: return url
                if (target.startsWith("http://") || target.startsWith("https://")) {
                    return target
                }
            }
        }
        return url
    }

    private fun hostOf(url: String): String? {
        val afterScheme = when {
            url.startsWith("https://") -> url.removePrefix("https://")
            url.startsWith("http://") -> url.removePrefix("http://")
            else -> return null
        }
        val authority = afterScheme.takeWhile { it != '/' && it != '?' && it != '#' }
        return authority.substringAfterLast('@').substringBefore(':').lowercase()
    }

    private fun stripTrackingParams(url: String): String {
        val queryStart = url.indexOf('?')
        if (queryStart == -1) return url
        val fragmentStart = url.indexOf('#', queryStart)
        val base = url.substring(0, queryStart)
        val query = if (fragmentStart == -1) url.substring(queryStart + 1)
        else url.substring(queryStart + 1, fragmentStart)
        val fragment = if (fragmentStart == -1) "" else url.substring(fragmentStart)

        val kept = query.split('&').filter { pair ->
            val name = pair.substringBefore('=').lowercase()
            name.isNotEmpty() &&
                name !in TRACKING_PARAMS &&
                TRACKING_PREFIXES.none { name.startsWith(it) }
        }
        return if (kept.isEmpty()) base + fragment
        else base + "?" + kept.joinToString("&") + fragment
    }
}
