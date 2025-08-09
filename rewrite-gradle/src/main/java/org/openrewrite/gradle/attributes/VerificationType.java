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

import org.gradle.api.Named;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Attribute;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.tree.DependencyAttribute;

/**
 * Attribute to be used on variants containing the output <a href="https://docs.gradle.org/current/javadoc/org/gradle/api/attributes/VerificationType.html">verification checks</a> verification checks (Test data, Jacoco results, etc) which specify the type of verification data.
 * This attribute is usually found on variants that have the Category attribute valued at Category.VERIFICATION.
 */
public enum VerificationType implements DependencyAttribute {
    /**
     * Binary test coverage data gathered by JaCoCo
     */
    JACOCO_RESULTS,
    /**
     * A list of directories containing source code, includes code in transitive dependencies
     */
    MAIN_SOURCES,
    /**
     * Binary results of running tests containing pass/fail information
     */
    TEST_RESULTS;

    public static @Nullable VerificationType from(org.gradle.api.artifacts.Dependency dependency) {
        if (!(dependency instanceof ModuleDependency)) {
            return null;
        }
        return from(((ModuleDependency) dependency).getAttributes());
    }

    public static @Nullable VerificationType from(org.gradle.api.attributes.AttributeContainer attributes) {
        try {
            return from((Named) attributes.getAttribute(Attribute.of(Class.forName("org.gradle.api.attributes.VerificationType"))));
        } catch (ClassCastException | ClassNotFoundException e) {
            return null;
        }
    }

    public static @Nullable VerificationType from(@Nullable Named verificationType) {
        if (verificationType == null) {
            return null;
        }
        switch (verificationType.getName()) {
            case "jacoco-coverage":
                return VerificationType.JACOCO_RESULTS;
            case "main-sources":
                return VerificationType.MAIN_SOURCES;
            case "test-results":
                return VerificationType.TEST_RESULTS;
            default:
                return null;
        }
    }
}
