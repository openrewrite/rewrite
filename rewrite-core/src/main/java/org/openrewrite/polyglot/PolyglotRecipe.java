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

package org.openrewrite.polyglot;

import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.config.CategoryDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.style.NamedStyles;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.graalvm.polyglot.Value.asValue;
import static org.openrewrite.polyglot.PolyglotUtils.invokeMemberOrElse;

@SuppressWarnings({"ConstantConditions", "unchecked", "rawtypes"})
public class PolyglotRecipe extends Recipe {

    private static final Value EMPTY_VISITOR = asValue(new TreeVisitor() {
    });

    private static final TypeLiteral<Set<String>> SET_OF_STRINGS = new TypeLiteral<Set<String>>() {
    };

    private final String name;
    private final Value value;

    public PolyglotRecipe(String name, Value value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getName() {
        return name;
    }

    public RecipeDescriptor getRecipeDescriptor() {
        return value.as(RecipeDescriptor.class);
    }

    public List<CategoryDescriptor> getCategoryDescriptors() {
        return Collections.emptyList();
    }

    public List<NamedStyles> getNamedStyles() {
        return Collections.emptyList();
    }

    @Override
    public String getDisplayName() {
        return invokeMemberOrElse(value, "getDisplayName", () -> asValue(name)).asString();
    }

    @Override
    public String getDescription() {
        return invokeMemberOrElse(value, "getDescription", () -> asValue(name)).asString();
    }

    @Override
    public Set<String> getTags() {
        return invokeMemberOrElse(value, "getTags", () -> asValue(Collections.emptySet())).as(SET_OF_STRINGS);
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return invokeMemberOrElse(value, "getApplicableTest", () -> asValue(null)).as(TreeVisitor.class);
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return invokeMemberOrElse(value, "getSingleSourceApplicableTest", () -> asValue(null)).as(TreeVisitor.class);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return invokeMemberOrElse(value, "getVisitor", () -> EMPTY_VISITOR).as(TreeVisitor.class);
    }

    @Override
    public String toString() {
        return "$classname{ value: " + value + " }";
    }

}
