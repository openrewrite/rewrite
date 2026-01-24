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
package org.openrewrite.kotlin.replace;

import org.openrewrite.kotlin.replace.DeprecatedMethodScanner.DeprecatedMethod;
import org.openrewrite.kotlin.replace.DeprecatedMethodScanner.ScanResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

/**
 * Generator that produces OpenRewrite YAML recipes from Kotlin
 * {@code @Deprecated(replaceWith=ReplaceWith(...))} annotations.
 */
public class KotlinDeprecatedRecipeGenerator {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: KotlinDeprecationRecipeGenerator <artifactId> [artifactId...]");
            System.exit(1);
        }

        DeprecatedMethodScanner scanner = new DeprecatedMethodScanner();

        for (String artifactId : args) {
            try {
                System.out.println("Processing artifact: " + artifactId);

                ScanResult result = scanner.scan(artifactId);
                if (result == null) {
                    System.err.println("Could not find JAR for artifact: " + artifactId);
                } else if (result.deprecatedMethods().isEmpty()) {
                    System.out.println("No deprecated methods with ReplaceWith found in " + artifactId);
                } else {
                    CharSequence yaml = generateYamlRecipe(result);
                    writeYamlRecipe(result, yaml);
                }

            } catch (Exception e) {
                System.err.println("Failed to generate recipes for " + artifactId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    static CharSequence generateYamlRecipe(ScanResult result) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("# Recipes generated for `@Deprecated` methods with `ReplaceWith` in `")
          .append(result.groupId()).append(":").append(result.artifactId()).append(":").append(result.version()).append("`\n");
        yaml.append("#\n\n");

        yaml.append("---\n");
        yaml.append("type: specs.openrewrite.org/v1beta/recipe\n");

        String recipeName = buildRecipeName(result.groupId(), result.artifactId(), result.majorVersion());
        yaml.append("name: ").append(recipeName).append("\n");
        yaml.append("displayName: Replace deprecated `").append(result.artifactId()).append("` methods\n");
        yaml.append("description: >-\n");
        yaml.append("  Automatically generated recipes to replace deprecated Kotlin methods based on\n");
        yaml.append("  `@Deprecated(replaceWith=ReplaceWith(...))` annotations.\n");
        yaml.append("recipeList:\n");

        Set<String> seen = new LinkedHashSet<>();
        List<DeprecatedMethod> methods = result.deprecatedMethods().stream()
                .sorted(comparing(DeprecatedMethod::methodPattern))
                // Deduplicate by method pattern (same method can be found on multiple classes)
                .filter(m -> seen.add(m.methodPattern()))
                .toList();
        for (DeprecatedMethod method : methods) {
            yaml.append("  # ").append(method.deprecatedAnnotation()).append("\n");
            yaml.append("  - org.openrewrite.kotlin.replace.ReplaceKotlinMethod:\n");
            yaml.append("      methodPattern: '").append(escapeYaml(method.methodPattern())).append("'\n");
            yaml.append("      replacement: '").append(escapeYaml(method.replacement())).append("'\n");

            if (!method.imports().isEmpty()) {
                yaml.append("      imports:\n");
                for (String imp : method.imports()) {
                    yaml.append("        - '").append(escapeYaml(imp)).append("'\n");
                }
            }

            yaml.append("      classpathFromResources:\n");
            yaml.append("        - '").append(escapeYaml(method.classpathResource())).append("'\n");
        }
        System.out.printf("Generated %d deprecated method recipes for %s:%s%n", methods.size(), result.artifactId(), result.version());
        return yaml;
    }

    private static void writeYamlRecipe(ScanResult result, CharSequence yaml) throws IOException {
        Path outputPath = Path.of("src/main/resources/META-INF/rewrite/kotlin-deprecations-%s-%s.yml"
          .formatted(result.artifactId(), result.majorVersion()));
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, yaml.toString().getBytes());
        System.out.println("Wrote recipes to " + outputPath);
    }

    static String buildRecipeName(String groupId, String artifactId, String majorVersion) {
        String moduleName = Arrays.stream(artifactId.split("-"))
                .map(KotlinDeprecatedRecipeGenerator::capitalize)
                .collect(joining());
        return groupId + ".ReplaceDeprecated" + moduleName + majorVersion + "Methods";
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String escapeYaml(String value) {
        return value.replace("'", "''");
    }
}
