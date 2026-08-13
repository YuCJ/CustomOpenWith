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

    // 點擊 ID 類：由來源平台附加在「任意目的地網址」上，天生跨網域，
    // 只能用全域名單清（Firefox Query Stripping 同樣做法）
    private val GLOBAL_TRACKING_PARAMS = setOf(
        // Meta
        "fbclid",
        // Google
        "gclid", "gbraid", "wbraid", "dclid", "srsltid",
        // Microsoft / Twitter / TikTok / LinkedIn / Yandex
        "msclkid", "twclid", "ttclid", "li_fat_id", "yclid",
        // Email/marketing platforms (Mailchimp, HubSpot, Marketo, Vero, Ortto)
        "mc_eid", "_hsenc", "_hsmi", "mkt_tok", "vero_id",
        "oly_anon_id", "oly_enc_id", "__s", "wickedid",
    )

    // 平台自家分享/追蹤參數：只出現在該平台網域上才清，
    // 避免通用參數名（si、s、t…）誤殺其他網站的正常參數。
    // 參數名單參考 ClearURLs 規則檔對應 provider。
    private class DomainRule(
        val params: Set<String> = emptySet(),
        val prefixes: List<String> = emptyList(),
        val hostMatches: (String) -> Boolean,
    )

    private val DOMAIN_RULES = listOf(
        DomainRule(params = setOf("igsh", "igshid"), hostMatches = { host ->
            host.inDomain("instagram.com") || host.inDomain("threads.net") || host.inDomain("threads.com")
        }),
        DomainRule(params = setOf("mibextid", "rdid"), hostMatches = { host ->
            host.inDomain("facebook.com")
        }),
        DomainRule(params = setOf("si", "feature", "pp"), hostMatches = { host ->
            host.inDomain("youtube.com") || host.inDomain("youtu.be")
        }),
        DomainRule(params = setOf("si"), hostMatches = { host ->
            host.inDomain("spotify.com")
        }),
        DomainRule(params = setOf("s", "t", "ref_src", "ref_url"), hostMatches = { host ->
            host.inDomain("twitter.com") || host.inDomain("x.com")
        }),
        // 刻意不清 tag / linkCode / camp / creative 等聯盟分潤參數
        DomainRule(
            params = setOf("ref"),
            prefixes = listOf("ref_", "pd_rd_", "pf_rd_"),
            hostMatches = { host -> host.matches(AMAZON_HOST) },
        ),
        DomainRule(params = setOf("publish_id", "sp_atk", "xptdk"), hostMatches = { host ->
            host.matches(SHOPEE_HOST)
        }),
    )

    private val AMAZON_HOST = Regex("""(?:[a-z0-9-]+\.)*amazon(?:\.[a-z]{2,}){1,2}""")
    private val SHOPEE_HOST = Regex("""(?:[a-z0-9-]+\.)*shopee(?:\.[a-z]{2,}){1,2}""")

    private fun String.inDomain(domain: String): Boolean =
        this == domain || endsWith(".$domain")

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
            host in setOf(
                "l.facebook.com", "lm.facebook.com", "l.messenger.com", "l.instagram.com",
                "l.threads.net", "l.threads.com",
            )
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
        val host = hostOf(url)
        val domainRule = host?.let { h -> DOMAIN_RULES.firstOrNull { it.hostMatches(h) } }
        val fragmentStart = url.indexOf('#', queryStart)
        val base = url.substring(0, queryStart)
        val query = if (fragmentStart == -1) url.substring(queryStart + 1)
        else url.substring(queryStart + 1, fragmentStart)
        val fragment = if (fragmentStart == -1) "" else url.substring(fragmentStart)

        val kept = query.split('&').filter { pair ->
            val name = pair.substringBefore('=').lowercase()
            name.isNotEmpty() &&
                name !in GLOBAL_TRACKING_PARAMS &&
                (domainRule == null ||
                    (name !in domainRule.params && domainRule.prefixes.none { name.startsWith(it) }))
        }
        return if (kept.isEmpty()) base + fragment
        else base + "?" + kept.joinToString("&") + fragment
    }
}
