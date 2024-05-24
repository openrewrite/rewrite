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
        try {
            return Integer.parseInt(normalize(sourceCompatibility));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * @return The major --release version.
     */
    public int getMajorReleaseVersion() {
        try {
            return Integer.parseInt(normalize(targetCompatibility));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String normalize(String version) {
        if (!version.contains(".")) {
            return version;
        }

        if (version.startsWith("1.")) {
            String removePrefix = version.substring(version.indexOf(".") + 1);
            return normalize(removePrefix);
        } else {
            return version.substring(0, version.indexOf("."));
        }
    }
}
