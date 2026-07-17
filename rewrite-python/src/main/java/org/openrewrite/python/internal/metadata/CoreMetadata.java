/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal.metadata;

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The subset of Python core metadata (METADATA / PKG-INFO) needed for dependency resolution.
 * Requirement strings are kept raw (unparsed PEP 508).
 */
@Value
public class CoreMetadata {
    String metadataVersion;
    String name;
    String version;
    List<String> requiresDist;

    @Nullable
    String requiresPython;

    List<String> providesExtra;

    /**
     * Lower-cased field names declared {@code Dynamic:} (PEP 643).
     */
    List<String> dynamic;

    /**
     * PEP 643 trust gate for sdist PKG-INFO: {@code Requires-Dist} is reliable only when
     * {@code Metadata-Version >= 2.2} and not declared dynamic. Wheel METADATA is always
     * trusted regardless; callers know which path they came through.
     */
    public boolean hasStaticRequiresDist() {
        String[] parts = metadataVersion.trim().split("\\.");
        if (parts.length < 2) {
            return false;
        }
        int major;
        int minor;
        try {
            major = Integer.parseInt(parts[0]);
            minor = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (major < 2 || (major == 2 && minor < 2)) {
            return false;
        }
        return !dynamic.contains("requires-dist");
    }
}
