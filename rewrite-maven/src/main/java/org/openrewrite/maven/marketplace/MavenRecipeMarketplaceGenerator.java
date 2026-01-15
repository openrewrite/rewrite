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

import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeClassLoader;
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
        if (Files.notExists(recipeJar)) {
            throw new IllegalArgumentException("Recipe JAR does not exist " + recipeJar);
        }

        @SuppressWarnings("DataFlowIssue") MavenRecipeBundleReader bundleReader = new MavenRecipeBundleReader(
                new RecipeBundle("maven", gav.getGroupId() + ":" + gav.getArtifactId(),
                        gav.getVersion(), gav.getVersion(), null),
                // Since we're going to directly set recipeJar and classpath, we can get away with this
                null, null,
                RecipeClassLoader::new
        );

        bundleReader.recipeJar = recipeJar;
        bundleReader.classpath = classpath;

        return bundleReader.marketplaceFromClasspathScan();
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
