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
package org.openrewrite.java.marker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Marker;

import java.util.UUID;

@Value
@With
public class JavaVersion implements Marker {
    @EqualsAndHashCode.Exclude
    UUID id;

    String createdBy;
    String vmVendor;
    String sourceCompatibility;
    String targetCompatibility;

    public int getMajorVersion() {
        return majorVersionOf(sourceCompatibility);
    }

    /**
     * @return The major --release version.
     */
    public int getMajorReleaseVersion() {
        return majorVersionOf(targetCompatibility);
    }

    /**
     * The major version of the JDK that compiled the sources, parsed from {@link #createdBy}
     * (typically the {@code java.runtime.version}). Unlike {@link #getMajorVersion()} and
     * {@link #getMajorReleaseVersion()}, which reflect the {@code -source}/{@code --release}
     * bytecode level, this reflects the JDK actually performing the compilation. Some features,
     * such as Markdown documentation comments (JEP 467, JDK 23), are recognized based on the
     * compiling JDK regardless of the targeted bytecode level.
     *
     * @return The major version of the compiling JDK, or -1 if it cannot be determined.
     */
    public int getMajorCreatedByVersion() {
        return majorVersionOf(createdBy);
    }

    private static int majorVersionOf(@Nullable String version) {
        if (version == null) {
            return -1;
        }
        String normalized = version.trim();
        // Legacy "1.x" version scheme (e.g. "1.8.0_312") denotes major version x.
        if (normalized.startsWith("1.")) {
            normalized = normalized.substring(2);
        }
        // Take the leading run of digits, ignoring any ".minor", "+build" or "-suffix"
        // (e.g. "21+35-2513", "23.0.1+11", "17.0.5").
        int end = 0;
        while (end < normalized.length() && Character.isDigit(normalized.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return -1;
        }
        try {
            return Integer.parseInt(normalized.substring(0, end));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
