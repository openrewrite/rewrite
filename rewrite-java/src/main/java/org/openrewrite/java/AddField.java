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
package org.openrewrite.java;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.Formatting;
import org.openrewrite.marker.Markers;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;

public final class AddField {
    private AddField() {
    }

    public static class Scoped extends JavaIsoRefactorVisitor {
        private final J.ClassDecl scope;
        private final List<J.Modifier> modifiers;
        private final String type;
        private final String name;

        @Nullable
        private final String init;

        public Scoped(J.ClassDecl scope, List<J.Modifier> modifiers, String type, String name, @Nullable String init) {
            this.scope = scope;
            this.modifiers = modifiers;
            this.type = type;
            this.name = name;
            this.init = init;
        }

        @Override
        public Iterable<Tag> getTags() {
            return Tags.of("field.class", type, "field.name", name);
        }

        @Override
        public J.ClassDecl visitClassDecl(J.ClassDecl classDecl) {
            J.ClassDecl c = super.visitClassDecl(classDecl);

            if (scope.isScope(classDecl) && classDecl.getBody().getStatements().stream()
                    .filter(s -> s instanceof J.VariableDecls)
                    .map(J.VariableDecls.class::cast)
                    .noneMatch(mv -> mv.getVars().stream().anyMatch(var -> var.getSimpleName().equals(name)))) {
                J.Block<J> body = c.getBody();
                JavaType javaType = JavaType.buildType(type);
                String fieldTypeString;
                if(javaType instanceof JavaType.FullyQualified) {
                    JavaType.FullyQualified classType = (JavaType.FullyQualified) javaType;
                    maybeAddImport(classType);
                    fieldTypeString = classType.getClassName();
                } else {
                    fieldTypeString = javaType.toTypeTree().print();
                }

                J.VariableDecls newField = new J.VariableDecls(randomId(),
                        emptyList(),
                        modifiers,
                        J.Ident.build(randomId(), fieldTypeString, javaType, emptyList(),
                                modifiers.isEmpty() ? Formatting.EMPTY : format(" "), Markers.EMPTY),
                        null,
                        emptyList(),
                        singletonList(new J.VariableDecls.NamedVar(randomId(),
                                J.Ident.build(randomId(), name, null, emptyList(),
                                        format("", init == null ? "" : " "), Markers.EMPTY),
                                new J.Empty(randomId(), emptyList(), Formatting.EMPTY, Markers.EMPTY),
                                emptyList(),
                                init == null ? null : new J.UnparsedSource(randomId(), init, emptyList(), format(" "), Markers.EMPTY),
                                new J.Empty(randomId(), emptyList(), Formatting.EMPTY, Markers.EMPTY),
                                javaType,
                                emptyList(),
                                format(" "),
                                Markers.EMPTY
                        )),
                        emptyList(),
                        formatter.format(body),
                        Markers.EMPTY
                );

                List<J> statements = new ArrayList<>(body.getStatements().size() + 1);
                statements.add(newField);
                statements.addAll(body.getStatements());

                c = c.withBody(body.withStatements(statements));
            }

            return c;
        }
    }
}
