/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.remote;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.test.MockHttpSender;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteArchiveTest {

    @ParameterizedTest
    @ValueSource(strings = {"7.4.2", "7.5-rc-1", "7.6"})
    void gradleWrapper(String version) throws Exception {
        URL distributionUrl = requireNonNull(RemoteArchiveTest.class.getClassLoader().getResource("gradle-" + version + "-bin.zip"));
        ExecutionContext ctx = new InMemoryExecutionContext();

        RemoteArchive remoteArchive = Remote
          .builder(Path.of("gradle/wrapper/gradle-wrapper.jar"))
          .build(distributionUrl.toURI(), "gradle-[^\\/]+\\/(?:.*\\/)+gradle-wrapper-(?!shared).*\\.jar");

        long actual = getInputStreamSize(remoteArchive.getInputStream(ctx));
        assertThat(actual).isGreaterThan(50_000);
    }

    @Test
    void gradleWrapperDownloadFails() throws Exception {
        URL distributionUrl = URI.create("http://example").toURL();
        ExecutionContext ctx = new InMemoryExecutionContext();

        HttpSenderExecutionContextView.view(ctx)
          .setLargeFileHttpSender(new MockHttpSender(408));

        RemoteArchive remoteArchive = Remote
          .builder(Path.of("gradle/wrapper/gradle-wrapper.jar"))
          .build(distributionUrl.toURI(), "gradle-[^\\/]+\\/(?:.*\\/)+gradle-wrapper-(?!shared).*\\.jar");

        assertThatThrownBy(() -> getInputStreamSize(remoteArchive.getInputStream(ctx)))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Failed to download " + distributionUrl.toURI() + " to artifact cache");
    }

    @ParameterizedTest
    @ValueSource(strings = {"7.4.2", "7.5-rc-1", "7.6"})
    void gradleWrapperConcurrent(String version) throws Exception {
        int executionCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(executionCount);
        CompletionService<Long> completionService = new ExecutorCompletionService<>(executorService);
        LocalRemoteArtifactCache localRemoteArtifactCache = new LocalRemoteArtifactCache(
          Path.of(System.getProperty("user.home") + "/.rewrite/remote/gradleWrapperConcurrent"));

        for (int i = 0; i < executionCount; i++) {
            completionService.submit(() -> {
                URL distributionUrl = requireNonNull(RemoteArchiveTest.class.getClassLoader()
                  .getResource("gradle-" + version + "-bin.zip"));

                ExecutionContext ctx = new InMemoryExecutionContext();
                RemoteExecutionContextView.view(ctx).setArtifactCache(localRemoteArtifactCache);
                HttpSenderExecutionContextView.view(ctx)
                  .setLargeFileHttpSender(new MockHttpSender(distributionUrl::openStream));

                RemoteArchive remoteArchive = Remote
                  .builder(Path.of("gradle/wrapper/gradle-wrapper.jar"))
                  .build(distributionUrl.toURI(), "gradle-[^\\/]+\\/(?:.*\\/)+gradle-wrapper-(?!shared).*\\.jar");

                return getInputStreamSize(remoteArchive.getInputStream(ctx));
            });
        }

        for (int i = 0; i < executionCount; i++) {
            Future<Long> result = completionService.take();
            Long actual = result.get();
            assertThat(actual).isGreaterThan(50_000);
        }

        executorService.shutdown();
    }

    @Test
    void cachesExtractedFileNotEntireArchive(@TempDir Path cacheDir) throws Exception {
        URL distributionUrl = requireNonNull(RemoteArchiveTest.class.getClassLoader().getResource("gradle-7.6-bin.zip"));
        long archiveSize = Files.size(Path.of(distributionUrl.toURI()));

        ExecutionContext ctx = new InMemoryExecutionContext();
        RemoteExecutionContextView.view(ctx).setArtifactCache(new LocalRemoteArtifactCache(cacheDir));

        RemoteArchive remoteArchive = Remote
          .builder(Path.of("gradle/wrapper/gradle-wrapper.jar"))
          .build(distributionUrl.toURI(), "gradle-[^\\/]+\\/(?:.*\\/)+gradle-wrapper-(?!shared).*\\.jar");

        long extractedSize = getInputStreamSize(remoteArchive.getInputStream(ctx));
        assertThat(extractedSize).isGreaterThan(50_000);

        try (Stream<Path> cached = Files.list(cacheDir)) {
            long cachedBytes = cached.filter(Files::isRegularFile).mapToLong(p -> p.toFile().length()).sum();
            assertThat(cachedBytes)
              .as("only the extracted entry should be cached, not the whole archive")
              .isEqualTo(extractedSize)
              .isLessThan(archiveSize);
        }
    }

    @Test
    void distinctCacheEntriesPerExtractedPath(@TempDir Path cacheDir) throws Exception {
        URL distributionUrl = requireNonNull(RemoteArchiveTest.class.getClassLoader().getResource("gradle-7.6-bin.zip"));
        ExecutionContext ctx = new InMemoryExecutionContext();
        RemoteExecutionContextView.view(ctx).setArtifactCache(new LocalRemoteArtifactCache(cacheDir));

        // Two different members extracted from the same archive URI: the plugins wrapper jar and the shared wrapper jar.
        long pluginJar = getInputStreamSize(Remote
          .builder(Path.of("plugin.jar"))
          .build(distributionUrl.toURI(), "gradle-[^\\/]+\\/(?:.*\\/)+gradle-wrapper-(?!shared).*\\.jar")
          .getInputStream(ctx));
        long sharedJar = getInputStreamSize(Remote
          .builder(Path.of("shared.jar"))
          .build(distributionUrl.toURI(), "gradle-[^\\/]+\\/lib\\/gradle-wrapper-shared-.*\\.jar")
          .getInputStream(ctx));

        assertThat(pluginJar)
          .as("each path must extract its own member, not a shared cache entry")
          .isNotEqualTo(sharedJar);
        try (Stream<Path> cached = Files.list(cacheDir)) {
            assertThat(cached.filter(Files::isRegularFile).count())
              .as("the same archive URI with different paths must produce distinct cache entries")
              .isEqualTo(2);
        }
    }

    @Test
    void printingRemoteArchive() throws Exception {
        URL zipUrl = requireNonNull(RemoteArchiveTest.class.getClassLoader().getResource("zipfile.zip"));

        RemoteArchive remoteArchive = Remote
          .builder(Path.of("content.txt"))
          .build(zipUrl.toURI(), "content.txt");

        String printed = remoteArchive.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.DEFAULT));
        assertThat(printed).isEqualTo("this is a zipped file");
    }

    private Long getInputStreamSize(InputStream is) {
        BlackHoleOutputStream out = new BlackHoleOutputStream();
        try {
            return is.transferTo(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
