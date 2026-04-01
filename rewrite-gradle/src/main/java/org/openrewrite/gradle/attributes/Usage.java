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
 * Attribute representing the <a href="https://docs.gradle.org/current/javadoc/org/gradle/api/attributes/Usage.html">usage</a> of a dependency variant.
 * This describes the intended consumer use case for a variant (e.g., compiling against an API, runtime execution).
 * See <a href="https://docs.gradle.org/current/javadoc/org/gradle/api/attributes/Usage.html">usage javadoc</a>
 */
public enum Usage implements Attribute {
    /**
     * The Java API of a library, packaged as class path elements, either a JAR or a classes directory.
     */
    JAVA_API,

    /**
     * The Java runtime of a component, packaged as class path elements, either a JAR or a classes directory.
     */
    JAVA_RUNTIME,

    /**
     * The C++ API of a library, packaged as header directories.
     */
    C_PLUS_PLUS_API,

    /**
     * The native link files of a library, packaged as static or shared library.
     */
    NATIVE_LINK,

    /**
     * The native runtime files of a library, packaged as a shared library.
     */
    NATIVE_RUNTIME,

    /**
     * The Swift API of a library, packaged as swiftmodule files.
     */
    SWIFT_API,

    /**
     * A version catalog, packaged as TOML files, for use as recommendations for dependency and plugin versions.
     */
    VERSION_CATALOG;

    public static String key() {
        return "org.gradle.usage";
    }

    public static @Nullable Usage from(@Nullable String usage) {
        if (usage == null) {
            return null;
        }
        switch (usage) {
            case "java-api":
                return Usage.JAVA_API;
            case "java-runtime":
                return Usage.JAVA_RUNTIME;
            case "cplusplus-api":
                return Usage.C_PLUS_PLUS_API;
            case "native-link":
                return Usage.NATIVE_LINK;
            case "native-runtime":
                return Usage.NATIVE_RUNTIME;
            case "swift-api":
                return Usage.SWIFT_API;
            case "version-catalog":
                return Usage.VERSION_CATALOG;
            default:
                return null;
        }
    }
}
