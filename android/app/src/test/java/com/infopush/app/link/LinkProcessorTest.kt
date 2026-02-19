package com.infopush.app.link

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkProcessorTest {

    @Test
    fun normalize_shouldTrimAndAddScheme() {
        val normalized = LinkNormalizer.normalize("  www.example.com/path  ")
        assertEquals("https://www.example.com/path", normalized)
    }

    @Test
    fun prepare_shouldFallbackWhenResolverFails() = runTest {
        val processor = LinkProcessor(
            resolver = object : LinkResolver {
                override suspend fun resolve(url: String, timeoutMs: Long): String? {
                    throw RuntimeException("timeout")
                }
            }
        )

        val result = processor.prepare("https://short.example.com/a")
        assertTrue(result is PreparedLink.Valid)
        val valid = result as PreparedLink.Valid
        assertEquals("https://short.example.com/a", valid.finalUrl)
        assertEquals("https://short.example.com/a", valid.fallbackUrl)
    }

    @Test
    fun prepare_shouldReturnInvalidForMalformedUrl() = runTest {
        val processor = LinkProcessor(
            resolver = object : LinkResolver {
                override suspend fun resolve(url: String, timeoutMs: Long): String? = null
            }
        )

        val result = processor.prepare("not a url")
        assertTrue(result is PreparedLink.Invalid)
    }
}
