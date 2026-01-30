/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.docker.search;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StringUtils;

import java.time.LocalDate;

/**
 * Enumeration of known end-of-life Docker base images.
 * <p>
 * Data sources:
 * <ul>
 *   <li>Debian: <a href="https://wiki.debian.org/DebianReleases">Debian Releases</a></li>
 *   <li>Ubuntu: <a href="https://wiki.ubuntu.com/Releases">Ubuntu Releases</a></li>
 *   <li>Alpine: <a href="https://alpinelinux.org/releases/">Alpine Releases</a></li>
 *   <li>Python: <a href="https://devguide.python.org/versions/">Python Versions</a></li>
 *   <li>Node.js: <a href="https://nodejs.org/en/about/releases/">Node.js Releases</a></li>
 * </ul>
 */
@RequiredArgsConstructor
@Getter
public enum EolImage {
    // Debian releases - https://wiki.debian.org/DebianReleases
    DEBIAN_JESSIE("debian", new String[]{"jessie", "jessie-*", "8", "8.*"},
            LocalDate.of(2020, 6, 30), "bookworm (12) or bullseye (11)"),
    DEBIAN_STRETCH("debian", new String[]{"stretch", "stretch-*", "9", "9.*"},
            LocalDate.of(2022, 7, 1), "bookworm (12) or bullseye (11)"),
    DEBIAN_BUSTER("debian", new String[]{"buster", "buster-*", "10", "10.*"},
            LocalDate.of(2024, 6, 30), "bookworm (12) or bullseye (11)"),

    // Ubuntu releases - https://wiki.ubuntu.com/Releases
    UBUNTU_TRUSTY("ubuntu", new String[]{"trusty", "trusty-*", "14.04", "14.04.*"},
            LocalDate.of(2019, 4, 25), "noble (24.04) or jammy (22.04)"),
    UBUNTU_XENIAL("ubuntu", new String[]{"xenial", "xenial-*", "16.04", "16.04.*"},
            LocalDate.of(2021, 4, 30), "noble (24.04) or jammy (22.04)"),
    UBUNTU_BIONIC("ubuntu", new String[]{"bionic", "bionic-*", "18.04", "18.04.*"},
            LocalDate.of(2023, 5, 31), "noble (24.04) or jammy (22.04)"),
    UBUNTU_FOCAL("ubuntu", new String[]{"focal", "focal-*", "20.04", "20.04.*"},
            LocalDate.of(2025, 4, 30), "noble (24.04) or jammy (22.04)"),

    // Alpine releases - https://alpinelinux.org/releases/
    ALPINE_3_14("alpine", new String[]{"3.14", "3.14.*"},
            LocalDate.of(2023, 5, 1), "3.21 or 3.20"),
    ALPINE_3_15("alpine", new String[]{"3.15", "3.15.*"},
            LocalDate.of(2023, 11, 1), "3.21 or 3.20"),
    ALPINE_3_16("alpine", new String[]{"3.16", "3.16.*"},
            LocalDate.of(2024, 5, 23), "3.21 or 3.20"),
    ALPINE_3_17("alpine", new String[]{"3.17", "3.17.*"},
            LocalDate.of(2024, 11, 22), "3.21 or 3.20"),

    // Python releases - https://devguide.python.org/versions/
    PYTHON_3_7("python", new String[]{"3.7", "3.7.*", "3.7-*"},
            LocalDate.of(2023, 6, 27), "3.12 or 3.11"),
    PYTHON_3_8("python", new String[]{"3.8", "3.8.*", "3.8-*"},
            LocalDate.of(2024, 10, 31), "3.12 or 3.11"),

    // Node.js releases - https://nodejs.org/en/about/releases/
    NODE_14("node", new String[]{"14", "14.*", "14-*"},
            LocalDate.of(2023, 4, 30), "22 or 20"),
    NODE_16("node", new String[]{"16", "16.*", "16-*"},
            LocalDate.of(2024, 4, 30), "22 or 20"),
    NODE_18("node", new String[]{"18", "18.*", "18-*"},
            LocalDate.of(2025, 4, 30), "22 or 20");

    private final String imageName;
    private final String[] tagPatterns;
    private final LocalDate eolDate;
    private final String suggestedReplacement;

    /**
     * Find the EOL entry matching the given image name and tag.
     *
     * @param imageName The Docker image name (without tag)
     * @param tag       The image tag, or null if no tag specified
     * @return The matching EolImage, or null if no match
     */
    public static @Nullable EolImage findMatch(String imageName, @Nullable String tag) {
        if (tag == null) {
            return null; // Cannot determine EOL without a tag
        }
        for (EolImage eol : values()) {
            if (matchesImageName(imageName, eol.imageName) && matchesTag(tag, eol.tagPatterns)) {
                return eol;
            }
        }
        return null;
    }

    private static boolean matchesImageName(String actual, String pattern) {
        // Handle library images (e.g., "library/debian" == "debian")
        String normalizedActual = actual.startsWith("library/") ?
                actual.substring("library/".length()) : actual;
        return normalizedActual.equals(pattern);
    }

    private static boolean matchesTag(String tag, String[] patterns) {
        for (String pattern : patterns) {
            if (StringUtils.matchesGlob(tag, pattern)) {
                return true;
            }
        }
        return false;
    }
}
