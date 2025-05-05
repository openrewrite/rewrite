package org.openrewrite.gradle.util;

import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GradleConfigurationNameUtils {
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
