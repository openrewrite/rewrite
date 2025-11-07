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
import org.gradle.util.GradleVersion;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.gradle.internal.GradleWrapperScriptLoader.Version;
import org.openrewrite.gradle.util.DistributionInfos;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.remote.Remote;
import org.openrewrite.semver.LatestRelease;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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

import static java.util.stream.Collectors.toMap;
import static org.openrewrite.gradle.util.GradleWrapper.WRAPPER_BATCH_LOCATION;
import static org.openrewrite.gradle.util.GradleWrapper.WRAPPER_SCRIPT_LOCATION;

public class GradleWrapperScriptDownloader {
    private static final Path WRAPPER_SCRIPTS = Paths.get("rewrite-gradle/src/main/resources/META-INF/rewrite/gradle-wrapper/");

    public static void main(String[] args) throws Exception {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        Map<String, GradleWrapper.GradleVersion> gradleBinVersions = GradleWrapper.listAllPublicVersions(ctx).stream()
          .filter(v -> v.getDistributionType() == GradleWrapper.DistributionType.Bin)
          .collect(toMap(GradleWrapper.GradleVersion::getVersion, v -> v));
        Map<String, Version> csvVersions = new GradleWrapperScriptLoader().getAllVersions();
        Map<String, Version> allVersions = new ConcurrentHashMap<>(csvVersions);
        allVersions.keySet().retainAll(gradleBinVersions.keySet());

        Map<String, String> unixChecksums = new ConcurrentHashMap<>();
        Map<String, String> batChecksums = new ConcurrentHashMap<>();
        AtomicInteger i = new AtomicInteger();

        // Use virtual threads for I/O-bound workload
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (GradleWrapper.GradleVersion version : gradleBinVersions.values()) {
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
            if (!entry.getValue().isBlank()) {
                Path scriptChecksumPath = WRAPPER_SCRIPTS.resolve("unix").resolve(entry.getKey() + ".txt");
                Files.writeString(scriptChecksumPath, entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : batChecksums.entrySet()) {
            if (!entry.getValue().isBlank()) {
                Path scriptChecksumPath = WRAPPER_SCRIPTS.resolve("windows").resolve(entry.getKey() + ".txt");
                Files.writeString(scriptChecksumPath, entry.getValue());
            }
        }

        // Remove old unused scripts
        cleanupScripts("unix", unixChecksums.keySet());
        cleanupScripts("windows", batChecksums.keySet());
    }

    private static void processVersion(
      GradleWrapper.GradleVersion version,
      Map<String, Version> allVersions,
      Map<String, String> unixChecksums,
      Map<String, String> batChecksums,
      AtomicInteger i,
      InMemoryExecutionContext ctx
    ) {
        String v = version.getVersion();
        if (allVersions.containsKey(v)) {
            GradleWrapperScriptLoader.Version existingVersion = allVersions.get(v);
            Path unixFile = WRAPPER_SCRIPTS.resolve("unix").resolve(existingVersion.getGradlewChecksum() + ".txt");
            Path windowsFile = WRAPPER_SCRIPTS.resolve("windows").resolve(existingVersion.getGradlewBatChecksum() + ".txt");

            if (Files.exists(unixFile) && Files.exists(windowsFile)) {
                unixChecksums.computeIfAbsent(allVersions.get(v).getGradlewChecksum(), checksum -> loadScript("unix", checksum));
                batChecksums.computeIfAbsent(allVersions.get(v).getGradlewBatChecksum(), checksum -> loadScript("windows", checksum));
                System.out.printf("%03d: %s already exists. Skipping.%n", i.incrementAndGet(), v);
                return;
            }
            System.out.printf("%03d: %s exists in CSV but files missing. Re-downloading.%n", i.incrementAndGet(), v);
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

            // verify non-collision
            if ((unixChecksums.containsKey(gradlewChecksum) && !unixChecksums.get(gradlewChecksum).equals(gradlew)) ||
              (batChecksums.containsKey(gradlewBatChecksum) && !batChecksums.get(gradlewBatChecksum).equals(gradlewBat))) {
                throw new IllegalStateException(String.format("Checksum collision [gradlew=%s, gradlew.bat=%s]", gradlewChecksum, gradlewBatChecksum));
            }

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

    private static String loadScript(String os, String checksum) {
        try {
            return Files.readString(WRAPPER_SCRIPTS.resolve(os).resolve(checksum + ".txt"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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

    private static final GradleVersion GRADLE_9_1_0_RC_1 = GradleVersion.version("9.1.0-rc-1");
    private static final GradleVersion GRADLE_9_0_M_2 = GradleVersion.version("9.0-milestone-2");
    private static final GradleVersion GRADLE_9_0_M_1 = GradleVersion.version("9.0-milestone-1");
    private static final GradleVersion GRADLE_8_14_RC_1 = GradleVersion.version("8.14-rc-1");
    private static final GradleVersion GRADLE_5_3 = GradleVersion.version("5.3");
    private static final GradleVersion GRADLE_5_0_RC_1 = GradleVersion.version("5.0-rc-1");
    private static final GradleVersion GRADLE_1_7_RC_1 = GradleVersion.version("1.7-rc-1");
    private static final GradleVersion GRADLE_1_0_M_8 = GradleVersion.version("1.0-milestone-8");

    private static Map<String, String> unixBindings(String gradleVersion) {
        GradleVersion current = GradleVersion.version(gradleVersion);

        Map<String, String> binding = defaultBindings();
        if (current.compareTo(GRADLE_5_3) >= 0) {
            binding.put("defaultJvmOpts", "'\"-Xmx64m\" \"-Xms64m\"'");
        } else if (current.compareTo(GRADLE_5_0_RC_1) >= 0) {
            binding.put("defaultJvmOpts", "'\"-Xmx64m\"'");
        } else if (current.compareTo(GRADLE_1_7_RC_1) >= 0) {
            binding.put("defaultJvmOpts", "\"\"");
        } else {
            binding.put("defaultJvmOpts", "");
        }

        if (current.compareTo(GRADLE_9_1_0_RC_1) >= 0) {
            binding.put("classpath", "");
            binding.put("entryPointArgs", "-jar \"$APP_HOME/gradle/wrapper/gradle-wrapper.jar\"");
            binding.put("mainClassName", "");
        } else if (current.compareTo(GRADLE_9_0_M_2) >= 0) {
            binding.put("classpath", "\"\\\\\\\"\\\\\\\"\"");
            binding.put("entryPointArgs", "-jar \"$APP_HOME/gradle/wrapper/gradle-wrapper.jar\"");
            binding.put("mainClassName", "");
        } else if (current.compareTo(GRADLE_9_0_M_1) >= 0) {
            binding.put("classpath", "$APP_HOME/gradle/wrapper/gradle-wrapper.jar");
            binding.put("entryPointArgs", "");
            binding.put("mainClassName", "org.gradle.wrapper.GradleWrapperMain");
        } else if (current.compareTo(GRADLE_8_14_RC_1) >= 0) {
            binding.put("classpath", "\"\\\\\\\\\\\"\\\\\\\\\\\"\"");
            binding.put("entryPointArgs", "-jar \"$APP_HOME/gradle/wrapper/gradle-wrapper.jar\"");
            binding.put("mainClassName", "");
        } else if (current.compareTo(GRADLE_1_0_M_8) >= 0) {
            binding.put("classpath", "$APP_HOME/gradle/wrapper/gradle-wrapper.jar");
            binding.put("entryPointArgs", "");
            binding.put("mainClassName", "org.gradle.wrapper.GradleWrapperMain");
        } else {
            // Intentionally mixed slashes to match the 1.0-milestone versions
            binding.put("classpath", "$APP_HOME/gradle\\wrapper\\gradle-wrapper.jar");
            binding.put("entryPointArgs", "");
            binding.put("mainClassName", "org.gradle.wrapper.GradleWrapperMain");
        }
        return binding;
    }

    private static Map<String, String> windowsBindings(String gradleVersion) {
        GradleVersion current = GradleVersion.version(gradleVersion);

        Map<String, String> binding = defaultBindings();
        if (current.compareTo(GRADLE_5_3) >= 0) {
            binding.put("defaultJvmOpts", "\"-Xmx64m\" \"-Xms64m\"");
        } else if (current.compareTo(GRADLE_5_0_RC_1) >= 0) {
            binding.put("defaultJvmOpts", "\"-Xmx64m\"");
        } else {
            binding.put("defaultJvmOpts", "");
        }

        if (current.compareTo(GRADLE_9_0_M_2) >= 0) {
            binding.put("classpath", "");
            binding.put("mainClassName", "");
            binding.put("entryPointArgs", "-jar \"%APP_HOME%\\gradle\\wrapper\\gradle-wrapper.jar\"");
        } else if (current.compareTo(GRADLE_9_0_M_1) >= 0) {
            binding.put("classpath", "%APP_HOME%\\gradle\\wrapper\\gradle-wrapper.jar");
            binding.put("mainClassName", "org.gradle.wrapper.GradleWrapperMain");
            binding.put("entryPointArgs", "");
        } else if (current.compareTo(GRADLE_8_14_RC_1) >= 0) {
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
