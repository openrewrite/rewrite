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
package org.openrewrite.gradle.security;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.gradle.util.RewriteStringLiteralValueVisitor;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class UseHttpsForRepositories extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use HTTPS for repositories";
    }

    @Override
    public String getDescription() {
        return "Use HTTPS for repository urls";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("security");
    }

    @Override
    protected GroovyVisitor<ExecutionContext> getVisitor() {
        return new RepositoryUrlVisitor();
    }

    private static class RepositoryUrlVisitor extends GroovyVisitor<ExecutionContext> {
        MethodMatcher repositories = new MethodMatcher("MavenArtifactRepositorySpec url(..)");

        private void fixupLiteralIfNeeded(J.Literal arg) {
            String url = (String) arg.getValue();
            //noinspection HttpUrlsUsage
            if (url.startsWith("http://")) {
                String newUrl = url.replaceAll("^http://(.*)", "https://$1");
                doAfterVisit(new RewriteStringLiteralValueVisitor<>(arg, newUrl));
            }
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
            if(!repositories.matches(method)) {
                return m;
            }
            List<Expression> args = m.getArguments();
            if (args.get(0) instanceof J.Literal) {
                J.Literal arg = (J.Literal) args.get(0);
                fixupLiteralIfNeeded(arg);
            } else if (args.get(0) instanceof G.GString) {
                G.GString arg = (G.GString) args.get(0);
                if (arg.getStrings().get(0) instanceof J.Literal) {
                    // Only fixup the first literal in the GString
                    fixupLiteralIfNeeded((J.Literal) arg.getStrings().get(0));
                }
            }
            return m;
        }
    }

}
