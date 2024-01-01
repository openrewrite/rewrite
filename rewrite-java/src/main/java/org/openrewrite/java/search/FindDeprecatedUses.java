/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;

import java.util.Arrays;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindDeprecatedUses extends Recipe {
    @Option(displayName = "Type pattern",
            description = "A type pattern that is used to find deprecations from certain types.",
            example = "org.springframework..*",
            required = false)
    @Nullable
    String typePattern;

    @Option(displayName = "Match inherited",
            description = "When enabled, find types that inherit from a deprecated type.",
            required = false)
    @Nullable
    Boolean matchInherited;

    @Option(displayName = "Ignore deprecated scopes",
            description = "When a deprecated type is used in a deprecated method or class, ignore it.",
            required = false)
    @Nullable
    Boolean ignoreDeprecatedScopes;

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(
                new FindDeprecatedMethods((typePattern == null || typePattern.isEmpty() ? null : typePattern + " *(..)"), ignoreDeprecatedScopes),
                new FindDeprecatedClasses(typePattern, matchInherited, ignoreDeprecatedScopes),
                new FindDeprecatedFields(typePattern, matchInherited, ignoreDeprecatedScopes)
        );
    }

    @Override
    public String getDisplayName() {
        return "Find uses of deprecated classes, methods, and fields";
    }

    @Override
    public String getInstanceNameSuffix() {
        if (typePattern != null) {
            return "matching `" + typePattern + "`";
        }
        return super.getInstanceNameSuffix();
    }

    @Override
    public String getDescription() {
        return "Find deprecated uses of methods, fields, and types. Optionally ignore those classes that are inside deprecated scopes.";
    }
}
