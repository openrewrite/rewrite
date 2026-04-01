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
 * See <a href="https://docs.gradle.org/current/javadoc/org/gradle/api/attributes/Category.html">category javadoc</a>
 */
public enum Category implements Attribute {
    /**
     * The documentation category
     */
    DOCUMENTATION,

    /**
     * The enforced platform, usually a synthetic variant derived from the platform
     */
    ENFORCED_PLATFORM,

    /**
     * The library category
     */
    LIBRARY,

    /**
     * Typically marks a dependency as <a href="https://docs.gradle.org/current/userguide/platforms.html">platform</a>.
     * "platform" has very similar semantics to a Maven bill of materials (BOM) added to the "import" scope via dependencyManagement.
     */
    REGULAR_PLATFORM,

    /**
     * The verification category, for variants which contain the results of running verification tasks (e.g. Test, Jacoco).
     */
    VERIFICATION;

    /**
     * A regular platform or enforced platform dependency is being used as a bill-of-materials to provide version numbers,
     * not actually as a dependency on its own.
     */
    public boolean isBom() {
        return this == REGULAR_PLATFORM || this == ENFORCED_PLATFORM;
    }

    public static String key() {
        return "org.gradle.category";
    }

    public static @Nullable Category from(@Nullable String category) {
        if (category == null) {
            return null;
        }
        switch (category) {
            case "documentation":
                return Category.DOCUMENTATION;
            case "enforced-platform":
                return Category.ENFORCED_PLATFORM;
            case "library":
                return Category.LIBRARY;
            case "platform":
                return Category.REGULAR_PLATFORM;
            case "verification":
                return Category.VERIFICATION;
            default:
                return null;
        }
    }
}
