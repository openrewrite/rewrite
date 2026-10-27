/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.gradle;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparingInt;

/**
 * Starts the slowest test classes first, so that a test JVM running several classes at once doesn't end with one of
 * them still running alone. Configured in junit-platform.properties.
 */
public class SlowestFirstClassOrderer implements ClassOrderer {

    // Slowest first, from full runs of this module; a class that isn't listed starts after these
    private static final List<String> SLOWEST = asList(
            "org.openrewrite.gradle.UpgradeDependencyVersionTest",
            "org.openrewrite.gradle.UpgradeTransitiveDependencyVersionTest",
            "org.openrewrite.gradle.AddDependencyTest",
            "org.openrewrite.gradle.RemoveRedundantDependencyVersionsTest",
            "org.openrewrite.gradle.plugins.AddDevelocityGradlePluginTest",
            "org.openrewrite.gradle.ChangeDependencyTest",
            "org.openrewrite.gradle.search.DependencyInsightTest",
            "org.openrewrite.gradle.AddPlatformDependencyTest"
    );

    @Override
    public void orderClasses(ClassOrdererContext context) {
        context.getClassDescriptors().sort(comparingInt(descriptor -> {
            int index = SLOWEST.indexOf(descriptor.getTestClass().getName());
            return index < 0 ? SLOWEST.size() : index;
        }));
    }
}
