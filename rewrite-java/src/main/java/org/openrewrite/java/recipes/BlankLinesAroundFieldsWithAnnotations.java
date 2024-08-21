/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.format.BlankLinesVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

public class BlankLinesAroundFieldsWithAnnotations extends Recipe {

    @Override
    public String getDisplayName() {
        return "Add a blank line around fields with annotations";
    }

    @Override
    public String getDescription() {
        return "Fields with annotations should have a blank line " +
               "before them to clearly separate them from the field above. " +
               "If another field follows, it should also have a blank line after " +
               "so that the field with the annotation has space on either side of it, " +
               "visually distinguishing it from its neighbors.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                if (classDecl.getBody() != null) {
                    List<Statement> statements = requireNonNull(cd).getBody().getStatements();
                    AtomicBoolean precedingFieldHadAnnotation = new AtomicBoolean();
                    cd = cd.withBody(cd.getBody().withStatements(ListUtils.map(statements, (i, s) -> {
                        if (precedingFieldHadAnnotation.get()) {
                            precedingFieldHadAnnotation.set(false);
                            if (s instanceof J.VariableDeclarations) {
                                s = s.withPrefix(s.getPrefix().withWhitespace(BlankLinesVisitor.minimumLines(s.getPrefix().getWhitespace(), 1)));
                            }
                        }
                        if (s instanceof J.VariableDeclarations) {
                            J.VariableDeclarations mv = (J.VariableDeclarations) s;
                            if (!mv.getLeadingAnnotations().isEmpty()) {
                                if (i > 0 && statements.get(i - 1) instanceof J.VariableDeclarations) {
                                    mv = mv.withPrefix(mv.getPrefix().withWhitespace(BlankLinesVisitor.minimumLines(mv.getPrefix().getWhitespace(), 1)));
                                }
                                precedingFieldHadAnnotation.set(true);
                            }
                            return mv;
                        }
                        return s;
                    })));
                }
                return cd;
            }
        };
    }
}
