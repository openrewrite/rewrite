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
package org.openrewrite.gradle.internal;

import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class GradleWrapperScriptGenerator {
    private GradleWrapperScriptGenerator() {}

    public static Map<String, String> unixBindings(String gradleVersion) {
        Map<String, String> binding = defaultBindings();
        String defaultJvmOpts = defaultJvmOpts(gradleVersion);
        binding.put("defaultJvmOpts", StringUtils.isNotEmpty(defaultJvmOpts) ? "'" + defaultJvmOpts + "'" : "");
        if (requireNonNull(Semver.validate("[8.14,)", null).getValue()).compare(null, gradleVersion, "8.14") >= 0) {
            binding.put("classpath", "\"\\\\\\\\\\\"\\\\\\\\\\\"\"");
            binding.put("entryPointArgs", "-jar \"$APP_HOME/gradle/wrapper/gradle-wrapper.jar\"");
            binding.put("mainClassName", "");
        } else {
            binding.put("classpath", "$APP_HOME/gradle/wrapper/gradle-wrapper.jar");
            binding.put("entryPointArgs", "");
            binding.put("mainClassName", "org.gradle.wrapper.GradleWrapperMain");
        }
        return binding;
    }

    public static Map<String, String> windowsBindings(String gradleVersion) {
        Map<String, String> binding = defaultBindings();
        binding.put("defaultJvmOpts", defaultJvmOpts(gradleVersion));
        if (requireNonNull(Semver.validate("[8.14,)", null).getValue()).compare(null, gradleVersion, "8.14") >= 0) {
            binding.put("classpath", "");
            binding.put("mainClassName", "");
            binding.put("entryPointArgs", "-jar \"%APP_HOME%\\gradle\\wrapper\\gradle-wrapper.jar\"");
        } else {
            binding.put("classpath", "%APP_HOME%\\gradle\\wrapper\\gradle-wrapper.jar");
            binding.put("mainClassName", "org.gradle.wrapper.GradleWrapperMain");
            binding.put("entryPointArgs", "");
        }
        return binding;
    }

    private static Map<String, String> defaultBindings() {
        Map<String, String> bindings = new HashMap<>();
        bindings.put("applicationName", "Gradle");
        bindings.put("optsEnvironmentVar", "GRADLE_OPTS");
        bindings.put("exitEnvironmentVar", "GRADLE_EXIT_CONSOLE");
        bindings.put("moduleEntryPoint", "");
        bindings.put("appNameSystemProperty", "org.gradle.appname");
        bindings.put("appHomeRelativePath", "");
        bindings.put("modulePath", "");
        return bindings;
    }

    private static String defaultJvmOpts(String gradleVersion) {
        VersionComparator gradle53VersionComparator = requireNonNull(Semver.validate("[5.3,)", null).getValue());
        VersionComparator gradle50VersionComparator = requireNonNull(Semver.validate("[5.0,)", null).getValue());

        if (gradle53VersionComparator.isValid(null, gradleVersion)) {
            return "\"-Xmx64m\" \"-Xms64m\"";
        } else if (gradle50VersionComparator.isValid(null, gradleVersion)) {
            return "\"-Xmx64m\"";
        }
        return "";
    }

    public static String renderTemplate(String source, Map<String, String> bindings, String lineSeparator) {
        String script = source;
        for (Map.Entry<String, String> variable : bindings.entrySet()) {
            script = script.replace("${" + variable.getKey() + "}", variable.getValue())
                    .replace("$" + variable.getKey(), variable.getValue());
        }
        script = script.replace("${mainClassName ?: entryPointArgs}", StringUtils.isNotEmpty(bindings.get("mainClassName")) ? bindings.get("mainClassName") : bindings.get("entryPointArgs"));

        script = script.replaceAll("(?sm)<% /\\*.*?\\*/ %>", "");
        script = script.replaceAll("(?sm)<% if \\( mainClassName\\.startsWith\\('--module '\\) \\) \\{.*?} %>", "");
        script = script.replaceAll("(?sm)<% if \\( appNameSystemProperty \\) \\{.*?%>(.*?)<% } %>(\\\\\n|(\n))?", "$1$3");
        script = script.replaceAll("(?sm)<% if \\( classpath \\) \\{%>\\\\?\n?(.*?)<% } %>\\\\?\n?", !bindings.get("classpath").isEmpty() ? "$1" : "");
        script = script.replace("\\$", "$");
        script = script.replaceAll("DIRNAME=\\.\\\\[\r\n]", "DIRNAME=.");
        script = script.replaceAll("(?<!\\\\)\\\\n", "\n");
        script = script.replace("\\\\", "\\");
        return script.replaceAll("\r\n|\r|\n", lineSeparator);
    }
}
