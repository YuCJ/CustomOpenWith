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
    fun `strips utm params but keeps real params`() {
        assertEquals(
            "https://example.com/watch?v=abc123",
            UrlCleaner.clean("https://example.com/watch?utm_source=fb&v=abc123&utm_medium=social"),
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
