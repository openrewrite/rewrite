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

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.config.CategoryDescriptor;
import org.openrewrite.config.DeclarativeRecipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.marketplace.RecipeMarketplaceWriter;
import org.openrewrite.marketplace.YamlRecipeBundle;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.io.File;
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
import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class MavenRecipeMarketplaceGenerator {
    private final ResolvedGroupArtifactVersion gav;
    private final Path recipeJar;
    private final List<Path> classpath;

    public RecipeMarketplace generate() {
        try {
            if (Files.notExists(recipeJar)) {
                throw new IllegalArgumentException("Recipe JAR does not exist " + recipeJar);
            }
            ResolvedMavenRecipeBundle bundle = new ResolvedMavenRecipeBundle(gav, recipeJar, classpath, null);
            Environment env = bundle.getEnvironment();
            RecipeMarketplace root = RecipeMarketplace.newEmpty();
            for (RecipeDescriptor descriptor : env.listRecipeDescriptors()) {
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
                                   gav.getArtifactId() + ":" + gav.getVersion() + "!/" +
                                   extractPathFromJarUri(source.toString())),
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
        return new MavenRecipeBundle(gav, new InMemoryExecutionContext(), null, null);
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

    /* This is just for a quick sanity check */
    public static void main(String[] args) throws IOException {
        Path rewriteJavaBuild = null;
        for (File f : requireNonNull(new File("rewrite-java/build/libs").listFiles())) {
            if (f.getName().endsWith(".jar") &&
                !f.getName().endsWith("-javadoc.jar") &&
                !f.getName().endsWith("-sources.jar")) {
                rewriteJavaBuild = f.toPath().toAbsolutePath();
                break;
            }
        }

        RecipeMarketplace marketplace = new MavenRecipeMarketplaceGenerator(
                new ResolvedGroupArtifactVersion(null, "org.openrewrite", "rewrite-java", "8.64.0-SNAPSHOT", null),
                requireNonNull(rewriteJavaBuild),
                emptyList()
        ).generate();

        String csv = new RecipeMarketplaceWriter().toCsv(marketplace);
        Files.write(Paths.get("recipes.csv"), csv.getBytes(StandardCharsets.UTF_8));
        System.out.println(csv);
    }
}
