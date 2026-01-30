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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Represents a known end-of-life Docker base image.
 * <p>
 * Data is loaded from the classpath resource {@code eol-images.json}.
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
@Value
public class EolImage {
    private static final List<EolImage> EOL_IMAGES = loadEolImages();

    String imageName;
    List<String> tagPatterns;
    LocalDate eolDate;
    String suggestedReplacement;

    private static List<EolImage> loadEolImages() {
        try (InputStream is = EolImage.class.getResourceAsStream("/eol-images.json")) {
            if (is == null) {
                return Collections.emptyList();
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            return mapper.readValue(is, new TypeReference<List<EolImage>>() {});
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

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
        for (EolImage eol : EOL_IMAGES) {
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

    private static boolean matchesTag(String tag, List<String> patterns) {
        for (String pattern : patterns) {
            if (StringUtils.matchesGlob(tag, pattern)) {
                return true;
            }
        }
        return false;
    }
}
