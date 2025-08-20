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

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Checksum;
import org.openrewrite.gradle.util.DistributionInfos;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.Markup;
import org.openrewrite.remote.Remote;
import org.openrewrite.remote.RemoteResource;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.VersionComparator;

import java.io.*;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static java.util.Objects.requireNonNull;

public class GradleWrapperScriptLoader {
    @Getter
    private final NavigableMap<String, Version> allVersions = new TreeMap<>(
            new LatestRelease(null));

    public GradleWrapperScriptLoader() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(requireNonNull(
                getClass().getResourceAsStream("/META-INF/rewrite/gradle-wrapper/versions.csv"))))) {
            in.readLine(); // header
            String line;
            while ((line = in.readLine()) != null) {
                String[] row = line.split(",");
                allVersions.put(row[0], new Version(row[0], row[1], row[2], row[3], row[4], row[5]));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Version select(VersionComparator versionComparator) {
        return allVersions.entrySet()
                .stream()
                .filter(v -> versionComparator.isValid(null, v.getKey()))
                .max((v1, v2) -> versionComparator.compare(null, v1.getKey(), v2.getKey()))
                .map(Map.Entry::getValue)
                .orElseThrow(() -> new IllegalStateException("Expected to find at least one Gradle wrapper version to select from."));
    }

    /**
     * When the requested version is unavailable we pick the nearest available.
     */
    public Nearest findNearest(@Nullable String requestedVersion) {
        if (requestedVersion == null) {
            return new Nearest(null, allVersions.lastEntry().getValue());
        }
        return new Nearest(requestedVersion, allVersions.floorEntry(requestedVersion).getValue());
    }

    @SuppressWarnings("resource")
    @Value
    public static class Nearest {
        @Nullable
        String requestedVersion;

        Version resolved;

        public String downloadUrl() {
            return resolved.getDownloadUrl();
        }

        public String getChecksum() {
            return resolved.getChecksum();
        }

        public @Nullable String getWrapperChecksum() {
            return StringUtils.isBlank(resolved.getWrapperChecksum()) ? null : resolved.getWrapperChecksum();
        }

        public RemoteResource gradlew() {
            InputStream script = getClass().getResourceAsStream("/META-INF/rewrite/gradle-wrapper/unix/" + resolved.getGradlewChecksum() + ".txt");
            return maybeWarn(Remote.builder(GradleWrapper.WRAPPER_SCRIPT_LOCATION)
                    .description(String.format("Unix Gradle wrapper script template for %s", resolved.getVersion()))
                    .build(requireNonNull(script)));
        }

        public RemoteResource gradlewBat() {
            InputStream script = getClass().getResourceAsStream("/META-INF/rewrite/gradle-wrapper/windows/" + resolved.getGradlewBatChecksum() + ".txt");
            return maybeWarn(Remote.builder(GradleWrapper.WRAPPER_BATCH_LOCATION)
                    .description(String.format("Windows Gradle wrapper script template for %s", resolved.getVersion()))
                    .build(requireNonNull(script)));
        }

        public RemoteResource maybeWarn(RemoteResource script) {
            if (!resolved.getVersion().equals(requestedVersion)) {
                return Markup.warn(script, new Exception(
                        "rewrite-gradle does not contain a script for requested version" +
                        (requestedVersion == null ? "" : requestedVersion + ". ") +
                        "Using the script from nearest available version " + resolved.getVersion() + " instead."));
            }
            return script;
        }
    }

    @Value
    public static class Version {
        String version;
        String downloadUrl;
        String checksum;
        String wrapperChecksum;
        String gradlewChecksum;
        String gradlewBatChecksum;
    }
}
