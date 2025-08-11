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
package org.openrewrite.gradle.attributes;

import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.attributes.Attribute;

/**
 * See Gradle javadocs for <a href="https://docs.gradle.org/current/javadoc/org/gradle/api/attributes/DocsType.html">org.gradle.api.attributes.DocsType</a>
 */
public enum DocsType implements Attribute {
    /**
     * The typical documentation for Java APIs
     */
    JAVADOC,

    /**
     * The source files of the module
     */
    SOURCES,
    /**
     * A user manual
     */
    USER_MANUAL,
    /**
     * Samples illustrating how to use the software module
     */
    SAMPLES,
    /**
     * The typical documentation for native APIs
     */
    DOXYGEN;

    public static String key() {
        return "org.gradle.docstype";
    }

    public static @Nullable DocsType from(@Nullable String docsType) {
        if (docsType == null) {
            return null;
        }
        switch (docsType) {
            case "javadoc":
                return DocsType.JAVADOC;
            case "sources":
                return DocsType.SOURCES;
            case "user-manual":
                return DocsType.USER_MANUAL;
            case "samples":
                return DocsType.SAMPLES;
            case "doxygen":
                return DocsType.DOXYGEN;
            default:
                return null;
        }
    }
}
