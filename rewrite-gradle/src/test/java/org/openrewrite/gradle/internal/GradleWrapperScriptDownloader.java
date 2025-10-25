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

import groovy.text.SimpleTemplateEngine;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.gradle.util.DistributionInfos;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.remote.Remote;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.Adler32;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.openrewrite.gradle.util.GradleWrapper.WRAPPER_BATCH_LOCATION;
import static org.openrewrite.gradle.util.GradleWrapper.WRAPPER_SCRIPT_LOCATION;

public class GradleWrapperScriptDownloader {
    private static final Path WRAPPER_SCRIPTS = Paths.get("rewrite-gradle/src/main/resources/META-INF/rewrite/gradle-wrapper/");

    public static void main(String[] args) throws IOException, InterruptedException {
        Lock lock = new ReentrantLock();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        Map<String, GradleWrapper.GradleVersion> allGradleReleases = GradleWrapper.listAllPublicVersions(ctx)
                .stream()
                .filter(v -> v.getDistributionType() == GradleWrapper.DistributionType.Bin)
                .collect(toMap(GradleWrapper.GradleVersion::getVersion, v -> v));
        Map<String, GradleWrapperScriptLoader.Version> allDownloadedVersions =
                new ConcurrentHashMap<>(new GradleWrapperScriptLoader().getAllVersions());
        allDownloadedVersions.forEach((key, value) -> {
            if (!allGradleReleases.containsKey(key)) {
                allDownloadedVersions.remove(key);
            }
        });

        Set<String> unixChecksums = new CopyOnWriteArraySet<>();
        Set<String> batChecksums = new CopyOnWriteArraySet<>();
        AtomicInteger i = new AtomicInteger();
        ForkJoinPool pool = ForkJoinPool.commonPool();
        pool.invokeAll(allGradleReleases.values().stream().map(version -> (Callable<Void>) () -> {
            String v = version.getVersion();
            String threadName = " [" + Thread.currentThread().getName() + "]";

            if (allDownloadedVersions.containsKey(v)) {
                unixChecksums.add(allDownloadedVersions.get(v).getGradlewChecksum());
                batChecksums.add(allDownloadedVersions.get(v).getGradlewChecksum());
                System.out.printf("%03d: %s already exists. Skipping.%s%n", i.incrementAndGet(),
                        v, threadName);
                return null;
            }

            try {
                DistributionInfos infos = DistributionInfos.fetch(version, ctx);
                GradleWrapper wrapper = new GradleWrapper(v, infos);

                // download
                String gradlewTemplate = downloadScript(WRAPPER_SCRIPT_LOCATION, wrapper, "unix", ctx);
                String gradlewBatTemplate = downloadScript(WRAPPER_BATCH_LOCATION, wrapper, "windows", ctx);

                // validate
                String gradlew = renderTemplate(gradlewTemplate, unixBindings(wrapper.getVersion()), "\n");
                String gradlewBat = renderTemplate(gradlewBatTemplate, windowsBindings(wrapper.getVersion()), "\r\n");

                // checksum
                String gradlewChecksum = hash("unix", gradlew);
                String gradlewBatChecksum = hash("windows", gradlewBat);

                unixChecksums.add(gradlewChecksum);
                batChecksums.add(gradlewBatChecksum);

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

        cleanupScripts("unix", unixChecksums);
        cleanupScripts("windows", batChecksums);
    }

    private static String downloadScript(Path wrapperScriptLocation, GradleWrapper wrapper, String os,
                                         InMemoryExecutionContext ctx) {
        InputStream is = Remote.builder(wrapperScriptLocation)
                .build(
                        URI.create(wrapper.getDistributionInfos().getDownloadUrl()),
                        "gradle-[^\\/]+/(?:.*\\/)+gradle-plugins-.*\\.jar",
                        "org/gradle/api/internal/plugins/" + os + "StartScript.txt"
                )
                .getInputStream(ctx);

        return StringUtils.readFully(is);
    }

    private static Map<String, String> unixBindings(String gradleVersion) {
        Map<String, String> binding = defaultBindings();
        String defaultJvmOpts = defaultJvmOpts(gradleVersion);
        binding.put("defaultJvmOpts", StringUtils.isNotEmpty(defaultJvmOpts) ? "'" + defaultJvmOpts + "'" : "");
        if (requireNonNull(Semver.validate("[8.14,)", null).getValue()).compare(null, gradleVersion, "8.14") >= 0) {
            binding.put("classpath", "\"\\\\\\\\\\\"\\\\\\\\\\\"\"");
            binding.put("entryPointArgs", "-jar \"$APP_HOME/gradle/wrapper/gradle-wrapper.jar\"");
            binding.put("mainClassName", "");
        } else {
            binding.put("classpath", "$APP_HOME/gradle/wrapper/gradle-wrapper.jar");
            binding.put("entryPointArgs", "");
            binding.put("mainClassName", "org.gradle.wrapper.GradleWrapperMain");
        }
        return binding;
    }

    private static Map<String, String> windowsBindings(String gradleVersion) {
        Map<String, String> binding = defaultBindings();
        binding.put("defaultJvmOpts", defaultJvmOpts(gradleVersion));
        if (requireNonNull(Semver.validate("[8.14,)", null).getValue()).compare(null, gradleVersion, "8.14") >= 0) {
            binding.put("classpath", "");
            binding.put("mainClassName", "");
            binding.put("entryPointArgs", "-jar \"%APP_HOME%\\gradle\\wrapper\\gradle-wrapper.jar\"");
        } else {
            binding.put("classpath", "%APP_HOME%\\gradle\\wrapper\\gradle-wrapper.jar");
            binding.put("mainClassName", "org.gradle.wrapper.GradleWrapperMain");
            binding.put("entryPointArgs", "");
        }
        return binding;
    }

    private static Map<String, String> defaultBindings() {
        Map<String, String> bindings = new HashMap<>();
        bindings.put("applicationName", "Gradle");
        bindings.put("optsEnvironmentVar", "GRADLE_OPTS");
        bindings.put("exitEnvironmentVar", "GRADLE_EXIT_CONSOLE");
        bindings.put("moduleEntryPoint", "");
        bindings.put("appNameSystemProperty", "org.gradle.appname");
        bindings.put("appHomeRelativePath", "");
        bindings.put("modulePath", "");
        return bindings;
    }

    private static String defaultJvmOpts(String gradleVersion) {
        VersionComparator gradle53VersionComparator = requireNonNull(Semver.validate("[5.3,)", null).getValue());
        VersionComparator gradle50VersionComparator = requireNonNull(Semver.validate("[5.0,)", null).getValue());

        if (gradle53VersionComparator.isValid(null, gradleVersion)) {
            return "\"-Xmx64m\" \"-Xms64m\"";
        } else if (gradle50VersionComparator.isValid(null, gradleVersion)) {
            return "\"-Xmx64m\"";
        }
        return "";
    }

    private static String renderTemplate(String source, Map<String, String> bindings, String lineSeparator) throws IOException, ClassNotFoundException {
        SimpleTemplateEngine engine = new SimpleTemplateEngine();
        return engine.createTemplate(source).make(new HashMap<>(bindings)).toString()
          .replaceAll("\r\n|\r|\n", lineSeparator)
          .replace("CLASSPATH=\"\\\\\\\\\\\"\\\\\\\\\\\"", "CLASSPATH=\"\\\\\\\"\\\\\\\"");
    }

    private static String hash(String os, String text) throws IOException {
        byte[] scriptText = text.getBytes(StandardCharsets.UTF_8);
        Adler32 adler32 = new Adler32();
        adler32.update(scriptText, 0, scriptText.length);

        String scriptChecksum = Long.toHexString(adler32.getValue());
        Path scriptChecksumPath = WRAPPER_SCRIPTS.resolve(os).resolve(scriptChecksum + ".txt");
        if (!Files.exists(scriptChecksumPath)) {
            Files.write(scriptChecksumPath, scriptText);
        }

        return scriptChecksum;
    }

    private static void cleanupScripts(String os, Set<String> checksums) throws IOException {
        Files.list(WRAPPER_SCRIPTS.resolve(os))
                .forEach(path -> {
                    if (!checksums.contains(path.getFileName().toString().replace(".txt", ""))) {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
    }
}
