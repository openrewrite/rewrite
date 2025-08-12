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

import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.attributes.Attribute;

/**
 * Attribute representing the <a href="https://docs.gradle.org/current/javadoc/org/gradle/api/attributes/java/TargetJvmEnvironment.html">target JVM environment</a> of a dependency variant.
 * This attribute can be used by libraries to indicate that a certain variant is better suited for a certain JVM environment.
 * It does however NOT strictly require environments to match, as the general assumption is that Java libraries can also run on environments they are not optimized for.
 */
public enum TargetJvmEnvironment implements Attribute {
    /**
     * An Android environment.
     */
    ANDROID,

    /**
     * A standard JVM environment (e.g. running on desktop or server machines).
     */
    STANDARD_JVM;

    public static String key() {
        return "org.gradle.jvm.environment";
    }

    public static @Nullable TargetJvmEnvironment from(@Nullable String targetJvmEnvironment) {
        if (targetJvmEnvironment == null) {
            return null;
        }
        switch (targetJvmEnvironment) {
            case "android":
                return TargetJvmEnvironment.ANDROID;
            case "standard-jvm":
                return TargetJvmEnvironment.STANDARD_JVM;
            default:
                return null;
        }
    }
}
