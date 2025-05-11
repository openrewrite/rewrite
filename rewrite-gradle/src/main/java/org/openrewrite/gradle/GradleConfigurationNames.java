/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.gradle;

import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class GradleConfigurationNames {
    private static final Set<String> gradleStandardConfigurations = new HashSet<>(Arrays.asList("api", "implementation", "compileOnly", "compileOnlyApi", "runtimeOnly", "testImplementation", "testCompileOnly", "testRuntimeOnly"));

    public static boolean isStandardConfiguration(String configuration) {
        return gradleStandardConfigurations.contains(configuration) || "default".equals(configuration);
    }

    public static String purgeConfigurationSuffix(String configuration) {
        if (configuration.endsWith("Implementation")) {
            return configuration.substring(0, configuration.length() - 14);
        } else if (configuration.endsWith("CompileOnly")) {
            return configuration.substring(0, configuration.length() - 11);
        } else if (configuration.endsWith("RuntimeOnly")) {
            return configuration.substring(0, configuration.length() - 11);
        } else if (configuration.endsWith("AnnotationProcessor")) {
            return configuration.substring(0, configuration.length() - 19);
        } else {
            return configuration;
        }
    }

    public static String purgeSourceSet(@Nullable String configuration) {
        if (StringUtils.isBlank(configuration) || configuration.endsWith("Implementation")) {
            return "implementation";
        } else if (configuration.endsWith("CompileOnly")) {
            return "compileOnly";
        } else if (configuration.endsWith("RuntimeOnly")) {
            return "runtimeOnly";
        } else if (configuration.endsWith("AnnotationProcessor")) {
            return "annotationProcessor";
        } else {
            return configuration;
        }
    }
}
