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
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.openrewrite.Recipe;
import org.openrewrite.polyglot.PolyglotRecipe;
import org.openrewrite.style.NamedStyles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
public class PolyglotResourceLoader implements ResourceLoader {

    private final List<Recipe> recipes = new ArrayList<>();
    private final List<NamedStyles> styles = new ArrayList<>();

    private final List<RecipeDescriptor> recipeDescriptors = new ArrayList<>();
    private final List<CategoryDescriptor> categoryDescriptors = new ArrayList<>();
    private final List<RecipeExample> recipeExamples = new ArrayList<>();

    public PolyglotResourceLoader() {
    }

    public PolyglotResourceLoader(Source... sources) {
        Context context = Context.newBuilder().allowAllAccess(true).build();
        for (Source src : sources) {
            try {
                evalPolyglotRecipe(context, src.getName(), src);
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

    public void evalPolyglotRecipe(Context context, String name, Source src) throws IOException {
        String language = src.getPath().substring(src.getPath().lastIndexOf('.') + 1);
        Value bindings = context.getBindings(language);
        context.eval(src);

        for (String exportedMember : bindings.getMemberKeys()) {
            Value defaultExport = bindings.getMember(exportedMember).getMember("default");
            if (defaultExport.canInstantiate()) {
                defaultExport = defaultExport.newInstance();
            } else if (defaultExport.canExecute()) {
                defaultExport = defaultExport.execute();
            }
            Recipe r = new PolyglotRecipe(name, defaultExport);
            recipes.add(r);
            recipeDescriptors.add(r.getDescriptor());
        }
    }

}
