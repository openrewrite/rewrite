/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.maven.tree.MavenRepository

class MavenPomDownloaderTest {

    @Test
    fun normalizeAccept500() {
        val downloader = MavenPomDownloader(emptyMap(), InMemoryExecutionContext())
        val originalRepo = MavenRepository("id", "https://httpstat.us/500", true, true, false, null, null)
        val normalizedRepo = downloader.normalizeRepository(originalRepo, null)
        assertThat(normalizedRepo).isEqualTo(originalRepo)
    }

    @Test
    fun normalizeAccept404() {
        val downloader = MavenPomDownloader(emptyMap(), InMemoryExecutionContext())
        val originalRepo = MavenRepository("id", "https://httpstat.us/400", true, true, false, null, null)
        val normalizedRepo = downloader.normalizeRepository(originalRepo, null)
        assertThat(normalizedRepo).isEqualTo(originalRepo)
    }

    @Test
    fun normalizeRejectConnectException() {
        val downloader = MavenPomDownloader(emptyMap(), InMemoryExecutionContext())
        val normalizedRepository = downloader.normalizeRepository(MavenRepository("id", "https://localhost", true, true, false, null, null), null)
        assertThat(normalizedRepository).isEqualTo(null)
    }
}
