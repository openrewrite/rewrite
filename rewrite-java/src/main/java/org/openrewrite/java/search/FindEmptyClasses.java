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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SearchResult;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class FindEmptyClasses extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find empty classes";
    }

    @Override
    public String getDescription() {
        return "Find empty classes without annotations that do not implement an interface or extend a class.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2094");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                if (classDecl.getType() != null && JavaType.Class.Kind.Class.equals(classDecl.getType().getKind()) &&
                    (classDecl.getBody() == null || classDecl.getBody().getStatements().isEmpty()) &&
                    classDecl.getLeadingAnnotations().isEmpty() && classDecl.getExtends() == null && classDecl.getImplements() == null) {
                    return SearchResult.found(classDecl);
                }
                return super.visitClassDeclaration(classDecl, ctx);
            }
        };
    }
}
