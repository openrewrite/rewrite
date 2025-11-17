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
package org.openrewrite.maven.marketplace;

import org.openrewrite.config.ClasspathScanningLoader;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeListing;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.marketplace.RecipeMarketplaceWriter;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class MavenRecipeMarketplaceGenerator {
    private final GroupArtifactVersion gav;
    private final Path recipeJar;
    private final List<Path> classpath;

    public MavenRecipeMarketplaceGenerator(GroupArtifact ga, Path recipeJar, List<Path> classpath) {
        this.gav = new GroupArtifactVersion(ga.getGroupId(), ga.getArtifactId(), "");
        this.recipeJar = recipeJar;
        this.classpath = classpath;
    }

    public RecipeMarketplace generate() {
        try {
            if (Files.notExists(recipeJar)) {
                throw new IllegalArgumentException("Recipe JAR does not exist " + recipeJar);
            }

            // First pass: Scan only the target jar for recipes (using scanJar with jar name filter)
            // This gives us the correct set of recipes but without root categories
            Environment env1 = Environment.builder()
                    .scanJar(
                            recipeJar.toAbsolutePath(),
                            classpath.stream().map(Path::toAbsolutePath).collect(toList()),
                            RecipeClassLoader.forScanning(recipeJar, classpath)
                    )
                    .build();

            // Collect recipe names from first pass
            List<String> targetRecipeNames = env1.listRecipeDescriptors().stream()
                    .map(RecipeDescriptor::getName)
                    .collect(toList());

            // Second pass: Scan all jars in classpath for recipes and categories
            // This gives us proper root categories from category YAMLs.
            Environment env2 = Environment.builder()
                    .load(new ClasspathScanningLoader(new Properties(), new RecipeClassLoader(recipeJar, classpath)))
                    .build();

            RecipeMarketplace marketplace = new RecipeMarketplace();
            for (RecipeDescriptor descriptor : env2.listRecipeDescriptors()) {
                // Only include recipes that were found in the first pass (i.e., from target jar)
                if (!targetRecipeNames.contains(descriptor.getName())) {
                    continue;
                }
                marketplace.install(
                        RecipeListing.fromDescriptor(descriptor, new RecipeBundle(
                                "maven", gav.getGroupId() + ":" + gav.getArtifactId(),
                                requireNonNull(gav.getVersion()), null)),
                        descriptor.inferCategoriesFromName(env2)
                );
            }
            return marketplace;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate marketplace for " + gav, e);
        }
    }

    /**
     * Main method to generate recipe marketplace CSV for a single recipe JAR.
     *
     * @param args [0] = groupId:artifactId, [1] = output CSV path, [2] = recipe JAR path, [3...] = classpath entries
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: java MavenRecipeMarketplaceGenerator <groupId:artifactId> <output-csv-path> <recipe-jar> [classpath-entries...]");
            System.exit(1);
        }

        String[] gav = args[0].split(":");
        if (gav.length != 2) {
            System.err.println("Invalid groupId:artifactId format: " + args[0]);
            System.exit(1);
        }
        String groupId = gav[0];
        String artifactId = gav[1];

        Path outputCsv = Paths.get(args[1]);
        Path recipeJar = Paths.get(args[2]);

        List<Path> classpath = new ArrayList<>();
        for (int i = 3; i < args.length; i++) {
            classpath.add(Paths.get(args[i]));
        }

        GroupArtifact ga = new GroupArtifact(groupId, artifactId);
        RecipeMarketplace marketplace = new MavenRecipeMarketplaceGenerator(ga, recipeJar, classpath).generate();

        String csv = new RecipeMarketplaceWriter().toCsv(marketplace);
        Files.write(outputCsv, csv.getBytes(StandardCharsets.UTF_8));
        System.out.println("Generated marketplace CSV with " + marketplace.getAllRecipes().size() + " recipes");
        System.out.println("Output written to: " + outputCsv.toAbsolutePath());
    }
}
