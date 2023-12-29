/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindImports extends Recipe {
    @Option(displayName = "Type pattern",
            description = "A type pattern that is used to find matching field uses.",
            example = "org.springframework..*",
            required = false)
    @Nullable
    String typePattern;

    @Option(displayName = "Match inherited",
            description = "When enabled, find types that inherit from a deprecated type.",
            required = false)
    @Nullable
    Boolean matchInherited;

    @Override
    public String getDisplayName() {
        return "Find source files with imports";
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
        return "Locates source files that have imports matching the given type pattern, regardless of whether " +
               "that import is used in the code.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TypeMatcher typeMatcher = new TypeMatcher(typePattern, Boolean.TRUE.equals(matchInherited));
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Import visitImport(J.Import anImport, ExecutionContext ctx) {
                if (typeMatcher.matchesPackage(anImport.getTypeName())) {
                    return SearchResult.found(anImport);
                }
                return super.visitImport(anImport, ctx);
            }
        };
    }
}
