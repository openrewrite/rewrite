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
 * Attribute representing the <a href="https://docs.gradle.org/current/javadoc/org/gradle/api/attributes/Bundling.html">bundling</a> of a dependency variant.
 */
public enum Bundling implements Attribute {
    /**
     * Dependencies are provided externally at runtime (not bundled)
     */
    EXTERNAL,

    /**
     * Dependencies are embedded inside the artifact (fat jar)
     */
    EMBEDDED,

    /**
     * Dependencies are shadowed (relocated) inside the artifact
     */
    SHADOWED;

    public static String key() {
        return "org.gradle.dependency.bundling";
    }

    public static @Nullable Bundling from(@Nullable String bundling) {
        if (bundling == null) {
            return null;
        }
        switch (bundling) {
            case "external":
                return Bundling.EXTERNAL;
            case "embedded":
                return Bundling.EMBEDDED;
            case "shadowed":
                return Bundling.SHADOWED;
            default:
                return null;
        }
    }
}
