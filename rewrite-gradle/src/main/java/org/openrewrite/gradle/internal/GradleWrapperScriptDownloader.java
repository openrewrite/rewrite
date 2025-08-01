/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.gradle.internal;

import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.gradle.util.DistributionInfos;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.remote.Remote;
import org.openrewrite.semver.LatestRelease;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.Adler32;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.gradle.util.GradleWrapper.WRAPPER_BATCH_LOCATION;
import static org.openrewrite.gradle.util.GradleWrapper.WRAPPER_SCRIPT_LOCATION;

public class GradleWrapperScriptDownloader {
    private static final Path WRAPPER_SCRIPTS = Paths.get("rewrite-gradle/src/main/resources/META-INF/rewrite/gradle-wrapper/");

    public static void main(String[] args) throws IOException, InterruptedException {
        Lock lock = new ReentrantLock();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        List<GradleWrapper.GradleVersion> allGradleReleases = GradleWrapper.listAllVersions(ctx);
        Map<String, GradleWrapperScriptLoader.Version> allDownloadedVersions =
                new ConcurrentHashMap<>(new GradleWrapperScriptLoader().getAllVersions());

        AtomicInteger i = new AtomicInteger();
        ForkJoinPool pool = ForkJoinPool.commonPool();
        pool.invokeAll(allGradleReleases.stream().map(version -> (Callable<Void>) () -> {
            String v = version.getVersion();
            String threadName = " [" + Thread.currentThread().getName() + "]";

            if (allDownloadedVersions.containsKey(v)) {
                System.out.printf("%03d: %s already exists. Skipping.%s%n", i.incrementAndGet(),
                        v, threadName);
                return null;
            }

            try {
                DistributionInfos infos = DistributionInfos.fetch(GradleWrapper.DistributionType.Bin, version, ctx);
                GradleWrapper wrapper = new GradleWrapper(v, infos);

                String gradlewChecksum = downloadScript(WRAPPER_SCRIPT_LOCATION, wrapper, "unix", ctx);
                String gradlewBatChecksum = downloadScript(WRAPPER_BATCH_LOCATION, wrapper, "windows", ctx);

                lock.lock();
                allDownloadedVersions.put(v, new GradleWrapperScriptLoader.Version(v, gradlewChecksum, gradlewBatChecksum));

                List<String> sortedVersions = new ArrayList<>(allDownloadedVersions.keySet());
                sortedVersions.sort(new LatestRelease(null).reversed());
                try (BufferedWriter writer = Files.newBufferedWriter(WRAPPER_SCRIPTS.resolve("versions.csv"))) {
                    writer.write("version,gradlew,gradlewBat\n");
                    for (String sortedVersion : sortedVersions) {
                        GradleWrapperScriptLoader.Version version1 = allDownloadedVersions.get(sortedVersion);
                        writer.write(sortedVersion + "," + version1.getGradlewChecksum() + "," + version1.getGradlewBatChecksum() + "\n");
                    }
                }
                System.out.printf("%03d: %s downloaded.%s%n", i.incrementAndGet(), v, threadName);
            } catch (Throwable t) {
                // FIXME There are some wrappers that are not downloading successfully. Why?
                System.out.printf("%03d: %s failed to download: %s.%s%n", i.incrementAndGet(), v, t.getMessage(), threadName);
                return null;
            } finally {
                lock.unlock();
            }
            return null;
        }).collect(toList()));

        while (pool.getActiveThreadCount() > 0) {
            //noinspection BusyWait
            Thread.sleep(100);
        }
    }

    private static String downloadScript(Path wrapperScriptLocation, GradleWrapper wrapper, String os,
                                         InMemoryExecutionContext ctx) throws IOException, NoSuchAlgorithmException {
        InputStream is = Remote.builder(wrapperScriptLocation)
                .build(
                        URI.create(wrapper.getDistributionInfos().getDownloadUrl()),
                        "gradle-[^\\/]+/(?:.*\\/)+gradle-plugins-.*\\.jar",
                        "org/gradle/api/internal/plugins/" + os + "StartScript.txt"
                )
                .getInputStream(ctx);

        byte[] scriptText = StringUtils.readFully(is).getBytes(StandardCharsets.UTF_8);
        Adler32 adler32 = new Adler32();
        adler32.update(scriptText, 0, scriptText.length);

        String scriptChecksum = Long.toHexString(adler32.getValue());
        Path scriptChecksumPath = WRAPPER_SCRIPTS.resolve(os).resolve(scriptChecksum + ".txt");
        if (!Files.exists(scriptChecksumPath)) {
            Files.write(scriptChecksumPath, scriptText);
        }

        return scriptChecksum;
    }
}
