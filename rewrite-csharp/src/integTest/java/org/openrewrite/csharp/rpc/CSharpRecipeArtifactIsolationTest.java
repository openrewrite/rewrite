/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.csharp.rpc;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.Recipe;
import org.openrewrite.csharp.marketplace.NuGetRecipeBundleResolver;
import org.openrewrite.marketplace.RecipeBundle;
import org.openrewrite.marketplace.RecipeBundleReader;
import org.openrewrite.marketplace.RecipeListing;
import org.openrewrite.marketplace.RecipeMarketplace;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end regression test for the recipe artifact isolation fix.
 * <p>
 * Verifies that installing {@code OpenRewrite.Recipes.CSharp.Migration.Dotnet}
 * (which depends on {@code OpenRewrite.Recipes.CSharp.Core} as a transitive NuGet
 * dependency) does not "steal" Core's recipes: after all three bundles are installed,
 * {@code ChangeDotNetTargetFramework} must remain attributed to the Core bundle, must
 * not appear in the Migration bundle's listing, and Migration's composite
 * {@code UpgradeToDotNet10} recipe must still resolve Core's recipe as a child via
 * PrepareRecipe.
 */
@Tag("slow")
@Timeout(value = 300, unit = TimeUnit.SECONDS)
class CSharpRecipeArtifactIsolationTest {

    @BeforeAll
    static void setUpFactory() {
        Path basePath = Paths.get(System.getProperty("user.dir"));
        Path[] searchPaths = {
          basePath.resolve("csharp"),
          basePath.resolve("rewrite-csharp/csharp"),
        };
        for (Path searchPath : searchPaths) {
            Path csproj = searchPath.resolve("OpenRewrite.Tool/OpenRewrite.Tool.csproj");
            if (csproj.toFile().exists()) {
                CSharpRewriteRpc.setFactory(
                  CSharpRewriteRpc.builder()
                    .csharpServerEntry(csproj.toAbsolutePath().normalize())
                    .log(Paths.get(System.getProperty("java.io.tmpdir"), "csharp-rpc-isolation.log"))
                );
                return;
            }
        }
        throw new IllegalStateException("Could not find C# Rewrite project");
    }

    @AfterAll
    static void tearDown() {
        CSharpRewriteRpc.shutdownCurrent();
    }

    /**
     * The earliest snapshot version that introduced the explicit Core dependency in
     * Migration.Dotnet (0.3.0 generation).  Core has never had a stable release, so
     * an explicit version is required to bypass dotnet's "no stable version" guard.
     * All three packages must be at the same generation so their inter-package
     * version constraints are satisfied.
     */
    private static final String PACKAGE_VERSION = "0.3.0-snapshot.20260627172857";

    @Test
    void recipeIsolation_coreNotStolenByMigration() {
        CSharpRewriteRpc rpc = CSharpRewriteRpc.getOrStart();
        NuGetRecipeBundleResolver resolver = new NuGetRecipeBundleResolver(rpc);
        RecipeMarketplace marketplace = new RecipeMarketplace();

        // Install all three bundles in dependency order.
        // Migration.Dotnet depends on Core, so installing Migration would previously
        // pull Core's assemblies into Migration's scan dir and steal its recipes.
        install(marketplace, resolver, "OpenRewrite.Recipes.CSharp.Core", PACKAGE_VERSION);
        install(marketplace, resolver, "OpenRewrite.Recipes.CSharp.CodeQuality", PACKAGE_VERSION);
        install(marketplace, resolver, "OpenRewrite.Recipes.CSharp.Migration.Dotnet", PACKAGE_VERSION);

        Set<String> coreBundleRecipes = recipesOfBundle(marketplace, "OpenRewrite.Recipes.CSharp.Core");
        Set<String> migrationBundleRecipes = recipesOfBundle(marketplace, "OpenRewrite.Recipes.CSharp.Migration.Dotnet");

        // (a) Core still owns its recipes — they did not disappear from the deployment list.
        assertThat(coreBundleRecipes)
          .as("Core bundle must contain ChangeDotNetTargetFramework")
          .contains("OpenRewrite.CSharp.Recipes.ChangeDotNetTargetFramework");

        // (b) Core's recipe is NOT attributed to the Migration bundle.
        assertThat(migrationBundleRecipes)
          .as("Migration bundle must not steal ChangeDotNetTargetFramework from Core")
          .doesNotContain("OpenRewrite.CSharp.Recipes.ChangeDotNetTargetFramework");

        // (c) Migration lists its own composite recipe.
        assertThat(migrationBundleRecipes)
          .as("Migration bundle must contain an UpgradeToDotNet10 recipe")
          .anyMatch(name -> name.endsWith(".UpgradeToDotNet10"));

        // (d) Migration's composite still resolves Core's child recipe via PrepareRecipe.
        String upgradeName = migrationBundleRecipes.stream()
          .filter(n -> n.endsWith(".UpgradeToDotNet10"))
          .findFirst()
          .orElseThrow(() -> new AssertionError("No UpgradeToDotNet10 recipe in Migration bundle"));
        RecipeListing upgrade = marketplace.findRecipe(upgradeName);
        assertThat(upgrade).as("Could not find listing for " + upgradeName).isNotNull();
        Recipe prepared = upgrade.prepare(List.of(resolver), Map.of());
        assertThat(prepared.getRecipeList())
          .as("UpgradeToDotNet10 must resolve ChangeDotNetTargetFramework as a child recipe")
          .anyMatch(r -> r.getName().equals("OpenRewrite.CSharp.Recipes.ChangeDotNetTargetFramework"));
    }

    /**
     * Installs the NuGet bundle for {@code packageName} at {@code version}
     * into the given marketplace.
     */
    private static void install(RecipeMarketplace marketplace, NuGetRecipeBundleResolver resolver,
                                String packageName, String version) {
        RecipeBundle bundle = new RecipeBundle("nuget", packageName, version, null, null);
        RecipeBundleReader reader = resolver.resolve(bundle);
        marketplace.install(reader);
    }

    /**
     * Returns the names of all recipes in the marketplace that are attributed to
     * the bundle with the given package name.
     */
    private static Set<String> recipesOfBundle(RecipeMarketplace marketplace, String packageName) {
        return marketplace.getAllRecipes().stream()
          .filter(listing -> packageName.equals(listing.getBundle().getPackageName()))
          .map(RecipeListing::getName)
          .collect(Collectors.toSet());
    }
}
