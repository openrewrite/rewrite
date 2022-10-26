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
package org.openrewrite.java.cleanup;

import org.openrewrite.Incubating;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Javadoc;

import java.util.ArrayList;
import java.util.List;

@Incubating(since = "7.33.0")
public class RenameJavaDocParamNameVisitor<P> extends JavaIsoVisitor<P> {
    private final MethodMatcher methodMatcher;
    private final String oldName;
    private final String newName;

    public RenameJavaDocParamNameVisitor(J.MethodDeclaration targetScope,
                                         String oldName,
                                         String newName) {
        String methodPattern = MethodMatcher.methodPattern(targetScope);
        this.methodMatcher = new MethodMatcher(methodPattern);
        this.oldName = oldName;
        this.newName = newName;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration md = super.visitMethodDeclaration(method, p);
        if (methodMatcher.matches(md.getMethodType()) && md.getComments().stream().anyMatch(it -> it instanceof Javadoc.DocComment)) {
            List<Comment> comments = new ArrayList<>(md.getComments().size());
            for (Comment comment : md.getComments()) {
                if (comment instanceof Javadoc.DocComment) {
                    Javadoc.DocComment docComment = (Javadoc.DocComment) comment;
                    Comment newComment = (Comment) new RenameParamVisitor<P>(oldName, newName).visit(docComment, p);
                    comments.add(newComment);
                } else {
                    comments.add(comment);
                }
            }
            md = md.withPrefix(md.getPrefix().withComments(comments));
        }
        return md;
    }

    private static class RenameParamVisitor<P> extends JavadocVisitor<P> {
        private final String oldName;
        private final String newName;

        public RenameParamVisitor(String oldName,
                                  String newName) {
            super(new JavaVisitor<>());
            this.oldName = oldName;
            this.newName = newName;
        }

        @Override
        public Javadoc visitParameter(Javadoc.Parameter parameter, P p) {
            Javadoc.Parameter pp = (Javadoc.Parameter) super.visitParameter(parameter, p);
            if (pp.getNameReference() != null) {
                if (pp.getNameReference().getTree() instanceof J.Identifier &&
                        oldName.equals(((J.Identifier) pp.getNameReference().getTree()).getSimpleName())) {
                    J.Identifier id = ((J.Identifier) pp.getNameReference().getTree()).withSimpleName(newName);
                    pp = pp.withNameReference(pp.getNameReference().withTree(id));
                }
            }
            return pp;
        }
    }
}
