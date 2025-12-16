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
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.marker.Markup;
import org.openrewrite.remote.Remote;
import org.openrewrite.remote.RemoteResource;
import org.openrewrite.semver.LatestRelease;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static java.util.Objects.requireNonNull;

public class GradleWrapperScriptLoader {
    @Getter
    private final Map<String, Version> allVersions = new HashMap<>();

    public GradleWrapperScriptLoader() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(requireNonNull(
                getClass().getResourceAsStream("/META-INF/rewrite/gradle-wrapper/versions.csv"))))) {
            in.readLine(); // header
            String line;
            while ((line = in.readLine()) != null) {
                String[] row = line.split(",");
                Version version = new Version(row[0], row[1], row[2]);
                allVersions.put(row[0], version);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Nearest findNearest(String requestedVersion) {
        return new Nearest(requestedVersion, allVersions.get(requestedVersion));
    }

    @SuppressWarnings("resource")
    @Value
    public static class Nearest {
        @Nullable
        String requestedVersion;

        Version resolved;

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
        String gradlewChecksum;
        String gradlewBatChecksum;
    }
}
