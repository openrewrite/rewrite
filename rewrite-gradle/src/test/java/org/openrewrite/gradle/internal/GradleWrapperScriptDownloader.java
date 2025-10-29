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
import org.openrewrite.gradle.internal.GradleWrapperScriptLoader.Version;
import org.openrewrite.gradle.util.DistributionInfos;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.gradle.util.GradleWrapper.GradleVersion;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.remote.Remote;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Adler32;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static org.openrewrite.gradle.util.GradleWrapper.WRAPPER_BATCH_LOCATION;
import static org.openrewrite.gradle.util.GradleWrapper.WRAPPER_SCRIPT_LOCATION;

public class GradleWrapperScriptDownloader {
    private static final Path WRAPPER_SCRIPTS = Paths.get("rewrite-gradle/src/main/resources/META-INF/rewrite/gradle-wrapper/");

    public static void main(String[] args) throws Exception {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        Map<String, GradleVersion> gradleBinVersions = GradleWrapper.listAllPublicVersions(ctx).stream()
          .filter(v -> v.getDistributionType() == GradleWrapper.DistributionType.Bin)
          .collect(toMap(GradleVersion::getVersion, v -> v));
        NavigableMap<String, Version> csvVersions = new GradleWrapperScriptLoader().getAllVersions();
        Map<String, Version> allVersions = new ConcurrentHashMap<>(csvVersions);
        allVersions.keySet().retainAll(gradleBinVersions.keySet());

        Map<String, String> unixChecksums = new ConcurrentHashMap<>();
        Map<String, String> batChecksums = new ConcurrentHashMap<>();
        AtomicInteger i = new AtomicInteger();

        // Use virtual threads for I/O-bound workload
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (GradleVersion version : gradleBinVersions.values()) {
                executor.submit(() -> processVersion(version, allVersions, unixChecksums, batChecksums, i, ctx));
            }
        }

        // Write versions.csv once at the end
        List<String> sortedVersions = new ArrayList<>(allVersions.keySet());
        sortedVersions.sort(new LatestRelease(null).reversed());
        try (BufferedWriter writer = Files.newBufferedWriter(WRAPPER_SCRIPTS.resolve("versions.csv"))) {
            writer.write("version,gradlew,gradlewBat\n");
            for (String sortedVersion : sortedVersions) {
                Version version = allVersions.get(sortedVersion);
                writer.write("%s,%s,%s\n".formatted(
                  sortedVersion,
                  version.getGradlewChecksum(),
                  version.getGradlewBatChecksum()));
            }
        }

        // Write new files once
        for (Map.Entry<String, String> entry : unixChecksums.entrySet()) {
            if (entry.getValue() != null) {
                Path scriptChecksumPath = WRAPPER_SCRIPTS.resolve("unix").resolve(entry.getKey() + ".txt");
                Files.writeString(scriptChecksumPath, entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : batChecksums.entrySet()) {
            if (entry.getValue() != null) {
                Path scriptChecksumPath = WRAPPER_SCRIPTS.resolve("windows").resolve(entry.getKey() + ".txt");
                Files.writeString(scriptChecksumPath, entry.getValue());
            }
        }

        // Remove old unused scripts
        cleanupScripts("unix", unixChecksums.keySet());
        cleanupScripts("windows", batChecksums.keySet());
    }

    private static void processVersion(
      GradleVersion version,
      Map<String, Version> allVersions,
      Map<String, String> unixChecksums,
      Map<String, String> batChecksums,
      AtomicInteger i,
      InMemoryExecutionContext ctx) {
        String v = version.getVersion();
        if (allVersions.containsKey(v)) {
            unixChecksums.put(allVersions.get(v).getGradlewChecksum(), null);
            batChecksums.put(allVersions.get(v).getGradlewBatChecksum(), null);
            System.out.printf("%03d: %s already exists. Skipping.%n", i.incrementAndGet(), v);
            return;
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
            String gradlewChecksum = hash(gradlew);
            String gradlewBatChecksum = hash(gradlewBat);

            // content
            unixChecksums.put(gradlewChecksum, gradlew);
            batChecksums.put(gradlewBatChecksum, gradlewBat);

            // Update map (ConcurrentHashMap is thread-safe)
            allVersions.put(v, new Version(v, gradlewChecksum, gradlewBatChecksum));

            System.out.printf("%03d: %s downloaded.%n", i.incrementAndGet(), v);
        } catch (Throwable t) {
            // FIXME There are some wrappers that are not downloading successfully. Why?
            System.out.printf("%03d: %s failed to download: %s.%n", i.incrementAndGet(), v, t.getMessage());
        }
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
          .replaceAll("\\R", lineSeparator)
          .replace("CLASSPATH=\"\\\\\\\\\\\"\\\\\\\\\\\"", "CLASSPATH=\"\\\\\\\"\\\\\\\"");
    }

    private static String hash(String text) {
        byte[] scriptText = text.getBytes(StandardCharsets.UTF_8);
        Adler32 adler32 = new Adler32();
        adler32.update(scriptText, 0, scriptText.length);
        return Long.toHexString(adler32.getValue());
    }

    private static void cleanupScripts(String os, Set<String> checksums) throws IOException {
        try (var stream = Files.list(WRAPPER_SCRIPTS.resolve(os))) {
            stream
              .filter(path -> !checksums.contains(path.getFileName().toString().replace(".txt", "")))
              .forEach(path -> {
                  try {
                      Files.deleteIfExists(path);
                  } catch (IOException e) {
                      throw new UncheckedIOException(e);
                  }
              });
        }
    }
}
