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
package org.openrewrite.xml.search;

import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.RecipeSearchResult;

/**
 * Represents the result of a search in an XML AST.
 */
public class XmlSearchResult extends RecipeSearchResult {

    public XmlSearchResult(Recipe recipe, @Nullable String description) {
        super(recipe, description);
    }

    public XmlSearchResult(Recipe recipe) {
        super(recipe);
    }

    @Override
    public String print() {
        String description = getDescription();
        if(description == null) {
            return "<!--~~>-->";
        } else {
            return String.format("<!--~~(%s)~~>-->", description);
        }
    }
}
