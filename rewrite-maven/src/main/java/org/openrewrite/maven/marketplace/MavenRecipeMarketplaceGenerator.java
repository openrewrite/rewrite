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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Recipe;
import org.openrewrite.config.CategoryDescriptor;
import org.openrewrite.config.DeclarativeRecipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.marketplace.RecipeMarketplaceWriter;
import org.openrewrite.marketplace.YamlRecipeBundle;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class MavenRecipeMarketplaceGenerator {
    private final ResolvedGroupArtifactVersion gav;
    private final Path recipeJar;
    private final List<Path> classpath;

    public MavenRecipeMarketplaceGenerator(GroupArtifact ga, Path recipeJar, List<Path> classpath) {
        //noinspection DataFlowIssue
        this.gav = new ResolvedGroupArtifactVersion(null, ga.getGroupId(), ga.getArtifactId(),
                null, null);
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
            Environment firstPassEnv = Environment.builder().scanJar(
                    recipeJar.toAbsolutePath(),
                    classpath.stream().map(Path::toAbsolutePath).collect(toList()),
                    RecipeClassLoader.forScanning(recipeJar, classpath)
            ).build();

            // Collect recipe names from first pass
            List<String> targetRecipeNames = firstPassEnv.listRecipeDescriptors().stream()
                    .map(RecipeDescriptor::getName)
                    .collect(toList());

            // Second pass: Scan all jars in classpath for recipes and categories
            // This gives us proper root categories from core-categories.yml
            ResolvedMavenRecipeBundle bundle = new ResolvedMavenRecipeBundle(gav, recipeJar, classpath,
                    RecipeClassLoader::new, null);
            Environment env = bundle.getEnvironment();

            RecipeMarketplace root = RecipeMarketplace.newEmpty();
            for (RecipeDescriptor descriptor : env.listRecipeDescriptors()) {
                // Only include recipes that were found in the first pass (i.e., from target jar)
                if (!targetRecipeNames.contains(descriptor.getName())) {
                    continue;
                }
                RecipeDescriptor bundledDescriptor = descriptor.withBundle(createBundle(descriptor, env));
                List<String> categories = extractCategories(descriptor.getName(), env);
                root.addRecipe(bundledDescriptor, categories.toArray(new String[0]));
            }
            return root;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate marketplace for " + gav, e);
        }
    }

    private RecipeBundle createBundle(RecipeDescriptor descriptor, Environment env) {
        Recipe recipe = env.activateRecipes(descriptor.getName());
        if (recipe instanceof DeclarativeRecipe) {
            DeclarativeRecipe declarativeRecipe = (DeclarativeRecipe) recipe;
            URI source = getDeclarativeSource(declarativeRecipe);
            if (source != null && isYamlSource(source)) {
                return new YamlRecipeBundle(
                        URI.create("maven-rewrite-yaml:" + gav.getGroupId() + ":" +
                                   gav.getArtifactId() + ":" + "!/" +
                                   extractPathFromJarUri(source.toString())),
                        gav.getVersion(),
                        new Properties(),
                        null
                ) {
                    @Override
                    public String getVersion() {
                        return gav.getDatedSnapshotVersion() == null ?
                                gav.getVersion() :
                                gav.getDatedSnapshotVersion();
                    }
                };
            }
        }
        return new ResolvedMavenRecipeBundle(gav, recipeJar, classpath,
                RecipeClassLoader::new, null);
    }

    private @Nullable URI getDeclarativeSource(DeclarativeRecipe recipe) {
        try {
            Field sourceField = DeclarativeRecipe.class.getDeclaredField("source");
            sourceField.setAccessible(true);
            return (URI) sourceField.get(recipe);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isYamlSource(URI source) {
        String path = source.toString();
        return path.contains("META-INF/rewrite") &&
               (path.endsWith(".yml") || path.endsWith(".yaml"));
    }

    private String extractPathFromJarUri(String uriString) {
        // Handle jar:file:/path/to/jar.jar!/META-INF/rewrite/file.yml
        int bangIndex = uriString.indexOf("!");
        if (bangIndex != -1) {
            // Skip "!/"
            return uriString.substring(bangIndex + 2);
        }
        // Fallback: try to find META-INF/rewrite
        int metaInfIndex = uriString.indexOf("META-INF/rewrite");
        if (metaInfIndex != -1) {
            return uriString.substring(metaInfIndex);
        }
        return uriString;
    }

    private List<String> extractCategories(String recipeName, Environment env) {
        // Extract package from recipe name (everything before the last dot)
        int lastDot = recipeName.lastIndexOf('.');
        if (lastDot == -1) {
            return emptyList();
        }

        String packageName = recipeName.substring(0, lastDot);

        String[] parts = packageName.split("\\.");
        List<String> categories = new ArrayList<>(parts.length);

        nextPart:
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            String partialPackage = String.join(".", Arrays.copyOfRange(parts, 0, i + 1));
            for (CategoryDescriptor categoryDescriptor : env.listCategoryDescriptors()) {
                String categoryPackageName = categoryDescriptor.getPackageName();
                if (categoryPackageName.equals(partialPackage)) {
                    if (categoryDescriptor.isRoot()) {
                        continue nextPart;
                    }
                    categories.add(categoryDescriptor.getDisplayName());
                    continue nextPart;
                }
            }

            if (!part.isEmpty()) {
                String capitalized = Character.toUpperCase(part.charAt(0)) + part.substring(1);
                categories.add(capitalized);
            }
        }

        return categories;
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
