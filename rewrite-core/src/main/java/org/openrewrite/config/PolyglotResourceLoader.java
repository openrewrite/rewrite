/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.config;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.openrewrite.Recipe;
import org.openrewrite.polyglot.PolyglotRecipe;
import org.openrewrite.polyglot.PolyglotUtils;
import org.openrewrite.style.NamedStyles;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.openrewrite.polyglot.PolyglotUtils.invokeMember;

@Slf4j
public class PolyglotResourceLoader implements ResourceLoader {

    private final List<Recipe> recipes = new ArrayList<>();
    private final List<NamedStyles> styles = new ArrayList<>();

    private final List<RecipeDescriptor> recipeDescriptors = new ArrayList<>();
    private final List<CategoryDescriptor> categoryDescriptors = new ArrayList<>();
    private final List<RecipeExample> recipeExamples = new ArrayList<>();

    public PolyglotResourceLoader() {
    }

    public PolyglotResourceLoader(String language, Map<String, Object>... sources) {
        Context context = Context.newBuilder(language, "java")
                .allowAllAccess(true)
                .build();
        for (Map<String, Object> src : sources) {
            try {
                evalPolyglotRecipe(language,
                        context,
                        (String) src.get("name"),
                        (String) src.get("description"),
                        (String) src.get("file"),
                        URI.create((String) src.get("uri")),
                        (byte[]) src.get("bytes"));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public Collection<Recipe> listRecipes() {
        return recipes;
    }

    @Override
    public Collection<RecipeDescriptor> listRecipeDescriptors() {
        return recipeDescriptors;
    }

    @Override
    public Collection<NamedStyles> listStyles() {
        return styles;
    }

    @Override
    public Collection<CategoryDescriptor> listCategoryDescriptors() {
        return categoryDescriptors;
    }

    @Override
    public Collection<RecipeExample> listRecipeExamples() {
        return recipeExamples;
    }

    public Recipe evalPolyglotRecipe(String language,
                                     Context context,
                                     String moduleName,
                                     String description,
                                     String srcFile,
                                     URI srcUri,
                                     byte[] srcContent) throws IOException {
        Value bindings = context.getBindings(language);

        Source src = Source.newBuilder(language, new String(srcContent, UTF_8), srcFile).build();
        context.eval(src);

        String recipeName = bindings.getMemberKeys().iterator().next();
        Value recipeVal = bindings.getMember(recipeName);
        Value opts = PolyglotUtils.getValue(recipeVal, "Options")
                .map(Value::newInstance)
                .orElse(Value.asValue(emptyMap()));

        Recipe r = new PolyglotRecipe(moduleName, opts, recipeVal.getMember("default"));

        recipes.add(r);
        RecipeDescriptor descriptor = new RecipeDescriptor(
                moduleName,
                moduleName,
                description,
                Collections.emptySet(),
                Duration.ZERO,
                invokeMember(opts, "getOptionsDescriptors")
                        .map(descs -> descs.as(new TypeLiteral<List<OptionDescriptor>>() {
                        }))
                        .orElse(emptyList()),
                Collections.singletonList(language),
                emptyList(),
                srcUri);
        recipeDescriptors.add(descriptor);

        return r;
    }

}
