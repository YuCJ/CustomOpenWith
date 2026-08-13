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

    // utm_* 刻意保留：部分分潤/導購連結靠它歸因

    // 目的地完整放在 query 參數裡、可本地還原的轉址服務
    // （同類 pattern 可參考 ClearURLs 規則檔的 redirections 分類）
    private class Redirector(
        val params: List<String>,
        val pathPrefix: String = "/",
        val hostMatches: (String) -> Boolean,
    )

    private val REDIRECTORS = listOf(
        // Meta link shim: l.facebook.com/l.php?u=...
        Redirector(params = listOf("u"), hostMatches = { host ->
            host in setOf("l.facebook.com", "lm.facebook.com", "l.messenger.com", "l.instagram.com")
        }),
        // Google 搜尋結果 / Gmail: www.google.com/url?q=... 或 url=...
        Redirector(params = listOf("q", "url"), pathPrefix = "/url", hostMatches = { host ->
            host == "google.com" || host.endsWith(".google.com")
        }),
        // YouTube 站外連結: www.youtube.com/redirect?q=...
        Redirector(params = listOf("q"), pathPrefix = "/redirect", hostMatches = { host ->
            host == "youtube.com" || host.endsWith(".youtube.com")
        }),
    )

    fun clean(url: String): String {
        var result = url
        repeat(3) {
            val unwrapped = unwrapRedirect(result)
            if (unwrapped == result) return@repeat
            result = unwrapped
        }
        return stripTrackingParams(result)
    }

    private fun unwrapRedirect(url: String): String {
        val host = hostOf(url) ?: return url
        val redirector = REDIRECTORS.firstOrNull { it.hostMatches(host) } ?: return url
        if (!pathOf(url).startsWith(redirector.pathPrefix)) return url
        val query = url.substringAfter('?', "").substringBefore('#')
        for (pair in query.split('&')) {
            if (pair.substringBefore('=') in redirector.params) {
                val target = runCatching {
                    URLDecoder.decode(pair.substringAfter('=', ""), "UTF-8")
                }.getOrNull() ?: continue
                // 只接受完整 http(s) URL，避免相對路徑或其他 scheme 被當成目的地
                if (target.startsWith("http://") || target.startsWith("https://")) {
                    return target
                }
            }
        }
        return url
    }

    private fun pathOf(url: String): String {
        val afterScheme = url.substringAfter("://", "")
        val path = afterScheme.dropWhile { it != '/' && it != '?' && it != '#' }
        return if (path.startsWith("/")) path.takeWhile { it != '?' && it != '#' } else "/"
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
            name.isNotEmpty() && name !in TRACKING_PARAMS
        }
        return if (kept.isEmpty()) base + fragment
        else base + "?" + kept.joinToString("&") + fragment
    }
}
