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
package org.openrewrite.python.internal.index;

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Distribution name and version parsed out of an artifact filename, per the wheel
 * filename spec ({@code {dist}-{version}(-{build})?-{py}-{abi}-{platform}.whl}) and
 * sdist naming conventions ({@code {dist}-{version}.tar.gz} et al.).
 */
@Value
public class DistFilename {

    public enum Type {
        WHEEL,
        SDIST,
        OTHER
    }

    private static final String[] SDIST_EXTENSIONS = {".tar.gz", ".tgz", ".zip"};
    // .tar.bz2 metadata cannot be read (no bzip2 support), so it is never an installable sdist
    private static final String[] OTHER_EXTENSIONS = {".egg", ".exe", ".msi", ".tar.bz2"};

    String distribution;
    String version;
    Type type;

    public static @Nullable DistFilename parse(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".whl")) {
            String[] parts = filename.substring(0, filename.length() - 4).split("-");
            return parts.length == 5 || parts.length == 6 ?
                    new DistFilename(parts[0], parts[1], Type.WHEEL) : null;
        }
        for (String ext : SDIST_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                String base = filename.substring(0, filename.length() - ext.length());
                int dash = base.lastIndexOf('-');
                if (dash <= 0 || dash == base.length() - 1) {
                    return null;
                }
                return new DistFilename(base.substring(0, dash), base.substring(dash + 1), Type.SDIST);
            }
        }
        for (String ext : OTHER_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                String[] parts = filename.substring(0, filename.length() - ext.length()).split("-");
                return parts.length >= 2 ? new DistFilename(parts[0], parts[1], Type.OTHER) : null;
            }
        }
        return null;
    }
}
