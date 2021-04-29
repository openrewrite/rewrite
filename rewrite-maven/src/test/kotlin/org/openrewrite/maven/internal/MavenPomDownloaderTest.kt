package org.openrewrite.maven.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.maven.cache.InMemoryMavenPomCache
import org.openrewrite.maven.tree.MavenRepository
import java.net.URI

class MavenPomDownloaderTest {

    @Test
    fun normalizeAccept500() {
        val downloader = MavenPomDownloader(InMemoryMavenPomCache(), emptyMap(), InMemoryExecutionContext())
        val originalRepo = MavenRepository("id", URI("https://httpstat.us/500"), true, true, false, null, null)
        val normalizedRepo = downloader.normalizeRepository(originalRepo)
        assertThat(normalizedRepo).isEqualTo(originalRepo)
    }

    @Test
    fun normalizeAccept404() {
        val downloader = MavenPomDownloader(InMemoryMavenPomCache(), emptyMap(), InMemoryExecutionContext())
        val originalRepo = MavenRepository("id", URI("https://httpstat.us/400"), true, true, false, null, null)
        val normalizedRepo = downloader.normalizeRepository(originalRepo)
        assertThat(normalizedRepo).isEqualTo(originalRepo)
    }

    @Test
    fun normalizeRejectConnectException() {
        val downloader = MavenPomDownloader(InMemoryMavenPomCache(), emptyMap(), InMemoryExecutionContext())
        val normalizedRepository = downloader.normalizeRepository(MavenRepository("id", URI("https://localhost"), true, true, false, null, null))
        assertThat(normalizedRepository).isEqualTo(null)
    }
}