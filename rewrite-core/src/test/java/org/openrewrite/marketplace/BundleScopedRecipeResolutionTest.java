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
package org.openrewrite.marketplace;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

/**
 * A user's declarative recipe listing one marketplace recipe, which in turn reaches a recipe that
 * only the listed recipe's own bundle can resolve. Mirrors a Spring best practices recipe reaching
 * an Elasticsearch migration that ships as a transitive dependency of the Spring recipe artifact.
 */
class BundleScopedRecipeResolutionTest implements RewriteTest {

    @Language("yml")
    private static final String USER_YAML = """
      type: specs.openrewrite.org/v1beta/recipe
      name: com.example.MyBestPractices
      displayName: My best practices
      description: Test.
      recipeList:
        - com.example.spring.SpringBestPractices
      """;

    @Language("yml")
    private static final String SPRING_YAML = """
      type: specs.openrewrite.org/v1beta/recipe
      name: com.example.spring.SpringBestPractices
      displayName: Spring best practices
      description: Test.
      recipeList:
        - com.example.spring.UpgradeSpringBoot
      ---
      type: specs.openrewrite.org/v1beta/recipe
      name: com.example.spring.UpgradeSpringBoot
      displayName: Upgrade Spring Boot
      description: Test.
      recipeList:
        - com.example.elastic.MigrateToElastic
      """;

    @Language("yml")
    private static final String ELASTIC_YAML = """
      type: specs.openrewrite.org/v1beta/recipe
      name: com.example.elastic.MigrateToElastic
      displayName: Migrate to Elastic
      description: Test.
      recipeList:
        - org.openrewrite.text.ChangeText:
            toText: migrated
      """;

    private static final RecipeBundle SPRING_BUNDLE =
      new RecipeBundle("bundle", "com.example:spring", null, "1.0", null);

    private static final RecipeBundle USER_BUNDLE =
      new RecipeBundle("yaml", "user.yml", null, null, null);

    private static final Collection<RecipeBundleResolver> RESOLVERS = List.of(new SpringBundleResolver());

    @Test
    void aRecipeOnlyTheListedBundleCanResolveSurvivesTheOuterEnvironment() {
        Recipe prepared = prepareUserRecipe();

        assertThat(namesIn(prepared))
          .as("the Spring bundle resolved Elastic from its own contents, so the user's " +
              "marketplace-backed loader is never asked for it")
          .containsExactly("com.example.MyBestPractices",
            "com.example.spring.SpringBestPractices",
            "com.example.spring.UpgradeSpringBoot",
            "com.example.elastic.MigrateToElastic",
            "org.openrewrite.text.ChangeText");

        assertThat(prepared.validateAll())
          .allSatisfy(validated -> assertThat(validated.isValid()).isTrue());
    }

    @Test
    void theRecipeResolvedThroughTheBundleRuns() {
        rewriteRun(
          spec -> spec.recipe(prepareUserRecipe()),
          text("needs migrating", "migrated")
        );
    }

    @Test
    void aBundleRecipeActivatedDirectlyByTheOuterEnvironmentKeepsItsResolution() {
        Recipe activated = new Environment(List.of(new YamlResourceLoader(stream(USER_YAML),
          URI.create("file:///user.yml"), new Properties(), marketplace(), RESOLVERS)))
          .activateRecipes("com.example.spring.SpringBestPractices");

        assertThat(namesIn(activated))
          .containsExactly("com.example.spring.SpringBestPractices",
            "com.example.spring.UpgradeSpringBoot",
            "com.example.elastic.MigrateToElastic",
            "org.openrewrite.text.ChangeText");
        assertThat(activated.validateAll())
          .allSatisfy(validated -> assertThat(validated.isValid()).isTrue());
    }

    /**
     * Prepare the user's recipe the way a YAML bundle is prepared in production.
     */
    private static Recipe prepareUserRecipe() {
        return new YamlRecipeBundleReader(USER_BUNDLE, stream(USER_YAML),
          URI.create("file:///user.yml"), new Properties(), marketplace(), RESOLVERS)
          .prepare(listing("com.example.MyBestPractices", USER_BUNDLE), emptyMap());
    }

    /**
     * A listing for the Spring recipe and nothing below it, exactly as the marketplace would look
     * when the Elastic recipe reaches the Spring bundle as a transitive dependency.
     */
    private static RecipeMarketplace marketplace() {
        RecipeMarketplace marketplace = new RecipeMarketplace();
        marketplace.install(listing("com.example.spring.SpringBestPractices", SPRING_BUNDLE), emptyList());
        assertThat(marketplace.findRecipe("com.example.elastic.MigrateToElastic")).isNull();
        return marketplace;
    }

    /**
     * Stands in for {@code MavenRecipeBundleReader}, which activates recipes in an
     * {@link Environment} scoped to the bundle's own recipe artifact and classpath. A YAML bundle
     * cannot play this role: its loader is marketplace-backed, so it resolves nested references
     * through the marketplace rather than from the bundle it was read from.
     */
    private static class SpringBundleReader implements RecipeBundleReader {

        @Override
        public RecipeBundle getBundle() {
            return SPRING_BUNDLE;
        }

        @Override
        public RecipeMarketplace read() {
            throw new UnsupportedOperationException();
        }

        @Override
        public RecipeDescriptor describe(RecipeListing listing) {
            return prepare(listing, emptyMap()).getDescriptor();
        }

        @Override
        public Recipe prepare(RecipeListing listing, Map<String, Object> options) {
            return Environment.builder()
              .load(new YamlResourceLoader(stream(SPRING_YAML), URI.create("jar:spring.jar!/spring.yml"), new Properties()))
              .load(new YamlResourceLoader(stream(ELASTIC_YAML), URI.create("jar:elastic.jar!/elastic.yml"), new Properties()))
              .build()
              .activateRecipes(listing.getName());
        }
    }

    private static class SpringBundleResolver implements RecipeBundleResolver {

        @Override
        public String getEcosystem() {
            return "bundle";
        }

        @Override
        public RecipeBundleReader resolve(RecipeBundle bundle) {
            return new SpringBundleReader();
        }
    }

    private static RecipeListing listing(String name, RecipeBundle bundle) {
        return new RecipeListing(null, name, name, "Test.", null, emptyList(), emptyList(), 1, bundle);
    }

    private static ByteArrayInputStream stream(@Language("yml") String yaml) {
        return new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    }

    private static List<String> namesIn(Recipe recipe) {
        List<String> names = new ArrayList<>();
        collectNames(recipe, names);
        return names;
    }

    private static void collectNames(Recipe recipe, List<String> names) {
        names.add(recipe.getName());
        for (Recipe nested : recipe.getRecipeList()) {
            collectNames(nested, names);
        }
    }
}
