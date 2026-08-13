package com.yucj.customopenwith

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlCleanerTest {

    @Test
    fun `strips fbclid`() {
        assertEquals(
            "https://example.com/article",
            UrlCleaner.clean("https://example.com/article?fbclid=IwAR123abc"),
        )
    }

    @Test
    fun `keeps utm params for affiliate attribution`() {
        assertEquals(
            "https://example.com/watch?utm_source=fb&v=abc123&utm_medium=social",
            UrlCleaner.clean("https://example.com/watch?utm_source=fb&v=abc123&utm_medium=social&fbclid=IwAR1"),
        )
    }

    @Test
    fun `keeps fragment`() {
        assertEquals(
            "https://example.com/page#section",
            UrlCleaner.clean("https://example.com/page?gclid=xyz#section"),
        )
    }

    @Test
    fun `unwraps facebook link shim and cleans target`() {
        assertEquals(
            "https://example.com/post?id=5",
            UrlCleaner.clean(
                "https://l.facebook.com/l.php?u=https%3A%2F%2Fexample.com%2Fpost%3Fid%3D5%26fbclid%3DIwAR1&h=AT0abc",
            ),
        )
    }

    @Test
    fun `unwraps threads link shim`() {
        assertEquals(
            "https://example.com/post",
            UrlCleaner.clean("https://l.threads.net/?u=https%3A%2F%2Fexample.com%2Fpost&e=AT2abc"),
        )
    }

    @Test
    fun `unwraps google search redirect`() {
        assertEquals(
            "https://example.com/page",
            UrlCleaner.clean(
                "https://www.google.com/url?q=https%3A%2F%2Fexample.com%2Fpage&sa=D&source=gmail&usg=AOvVaw0",
            ),
        )
    }

    @Test
    fun `unwraps youtube redirect`() {
        assertEquals(
            "https://example.com/video",
            UrlCleaner.clean(
                "https://www.youtube.com/redirect?event=video_description&q=https%3A%2F%2Fexample.com%2Fvideo",
            ),
        )
    }

    @Test
    fun `does not unwrap google url without redirect path`() {
        assertEquals(
            "https://www.google.com/search?q=kotlin",
            UrlCleaner.clean("https://www.google.com/search?q=kotlin"),
        )
    }

    @Test
    fun `ignores non-http redirect target`() {
        assertEquals(
            "https://www.google.com/url?q=javascript%3Aalert(1)",
            UrlCleaner.clean("https://www.google.com/url?q=javascript%3Aalert(1)"),
        )
    }

    @Test
    fun `host match is exact suffix, lookalike domain not unwrapped`() {
        assertEquals(
            "https://evilgoogle.com/url?q=https%3A%2F%2Fexample.com",
            UrlCleaner.clean("https://evilgoogle.com/url?q=https%3A%2F%2Fexample.com"),
        )
    }

    @Test
    fun `strips youtube share si param`() {
        assertEquals(
            "https://youtu.be/dQw4w9WgXcQ",
            UrlCleaner.clean("https://youtu.be/dQw4w9WgXcQ?si=AbCdEf123"),
        )
    }

    @Test
    fun `si param outside its domains is kept`() {
        assertEquals(
            "https://example.com/page?si=keep-me",
            UrlCleaner.clean("https://example.com/page?si=keep-me"),
        )
    }

    @Test
    fun `strips x share params`() {
        assertEquals(
            "https://x.com/user/status/123",
            UrlCleaner.clean("https://x.com/user/status/123?s=20&t=AbC-dEf"),
        )
    }

    @Test
    fun `strips instagram igsh on its own domain`() {
        assertEquals(
            "https://www.instagram.com/p/abc123/",
            UrlCleaner.clean("https://www.instagram.com/p/abc123/?igsh=MzRlODBiNWFlZA=="),
        )
    }

    @Test
    fun `strips amazon noise but keeps affiliate tag`() {
        assertEquals(
            "https://www.amazon.co.jp/dp/B01N5IB20Q?tag=mysite-22&th=1",
            UrlCleaner.clean(
                "https://www.amazon.co.jp/dp/B01N5IB20Q?pd_rd_w=abc&pf_rd_p=def&ref_=cm_sw_r&tag=mysite-22&th=1",
            ),
        )
    }

    @Test
    fun `strips shopee share params`() {
        assertEquals(
            "https://shopee.tw/product/123/456",
            UrlCleaner.clean("https://shopee.tw/product/123/456?publish_id=abc&sp_atk=def&xptdk=ghi"),
        )
    }

    @Test
    fun `leaves clean url untouched`() {
        assertEquals(
            "https://example.com/a?b=c&d=e",
            UrlCleaner.clean("https://example.com/a?b=c&d=e"),
        )
    }

    @Test
    fun `param name matching is exact, not substring`() {
        assertEquals(
            "https://example.com/?myfbclid=1&fbclid2=2",
            UrlCleaner.clean("https://example.com/?myfbclid=1&fbclid2=2"),
        )
    }

    @Test
    fun `handles non-url input without crashing`() {
        assertEquals("not a url", UrlCleaner.clean("not a url"))
    }
}
