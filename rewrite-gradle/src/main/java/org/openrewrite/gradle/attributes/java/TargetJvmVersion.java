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
package org.openrewrite.gradle.attributes.java;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.attributes.Attribute;

import static java.lang.Integer.parseInt;

/**
 * Represents the <a href="https://docs.gradle.org/current/javadoc/org/gradle/api/attributes/java/TargetJvmVersion.html">target version</a>
 * of a Java library or platform. The target level is expected to correspond to a Java platform version number (integer).
 * For example, "5" for Java 5, "8" for Java 8, or "11" for Java 11.
 */
@Value
public class TargetJvmVersion implements Attribute {
    /**
     * The minimal target version for a Java library. Any consumer below this version would not be able to consume it.
     */
    int version;

    public static String key() {
        return "org.gradle.jvm.version";
    }

    public static @Nullable TargetJvmVersion from(@Nullable String value) {
        if (value == null) {
            return null;
        }
        return new TargetJvmVersion(parseInt(value));
    }
}
