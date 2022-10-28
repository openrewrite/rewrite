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
package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;

import static java.util.stream.Collectors.toList;

public class RemoveDeadRepos extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove dead repositories";
    }

    @Override
    public String getDescription() {
        return "Remove repositories that are no longer used.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveDeadReposVisitor();
    }

    private static class RemoveDeadReposVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            J.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
            c.getImports().forEach(i -> System.out.println(i.getQualid().getType()));
            c = c.withImports(c.getImports().stream()
                    .filter(i -> !i.getQualid().getName().equals("maven.glassfish.org"))
                    .collect(toList()));
            return c;
        }
    }


}
