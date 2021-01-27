/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.config;

import org.openrewrite.Recipe;
import org.openrewrite.style.NamedStyles;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class Environment {
    private final Collection<? extends ResourceLoader> resourceLoaders;

    public Collection<Recipe> listRecipes() {
        List<Recipe> recipes = resourceLoaders.stream()
                .flatMap(r -> r.listRecipes().stream())
                .collect(toList());
        for (Recipe recipe : recipes) {
            if (recipe instanceof DeclarativeRecipe) {
                ((DeclarativeRecipe) recipe).initialize(recipes);
            }
        }
        return recipes;
    }

    public Recipe activateRecipes(String... activeRecipes) {
        Recipe root = new Recipe();
        List<String> activeRecipeList = Arrays.asList(activeRecipes);
        for (Recipe recipe : listRecipes()) {
            if (activeRecipeList.contains(recipe.getName())) {
                root.doNext(recipe);
            }
        }
        return root;
    }

    /**
     * @return A list of validations of style names that could be activated.
     */
    public List<NamedStyles> listStyles() {
        return resourceLoaders.stream()
                .flatMap(r -> r.listStyles().stream())
                .collect(toList());
    }

    public List<NamedStyles> activateStyles(String... activeStyles) {
        List<NamedStyles> activated = new ArrayList<>(activeStyles.length);
        List<NamedStyles> styles = listStyles();
        for (String activeStyle : activeStyles) {
            for (NamedStyles style : styles) {
                if (style.getName().equals(activeStyle)) {
                    activated.add(style);
                }
            }
        }
        return activated;
    }

    public Environment(Collection<? extends ResourceLoader> resourceLoaders) {
        this.resourceLoaders = resourceLoaders;
    }

    public static Builder builder(Properties properties) {
        return new Builder(properties);
    }

    public static Builder builder() {
        return new Builder(new Properties());
    }

    public static class Builder {
        private final Properties properties;
        private final Collection<ResourceLoader> resourceLoaders = new ArrayList<>();

        public Builder(Properties properties) {
            this.properties = properties;
        }

        public Builder scanClasspath(Iterable<Path> compileClasspath, String... acceptPackages) {
            return load(new ClasspathScanningLoader(compileClasspath, properties, acceptPackages));
        }

        public Builder scanUserHome() {
            File userHomeRewriteConfig = new File(System.getProperty("user.home") + "/.rewrite/rewrite.yml");
            if (userHomeRewriteConfig.exists()) {
                try (FileInputStream is = new FileInputStream(userHomeRewriteConfig)) {
                    return load(new YamlResourceLoader(is, userHomeRewriteConfig.toURI(), properties));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return this;
        }

        public Builder load(ResourceLoader resourceLoader) {
            resourceLoaders.add(resourceLoader);
            return this;
        }

        public Environment build() {
            return new Environment(resourceLoaders);
        }
    }
}
