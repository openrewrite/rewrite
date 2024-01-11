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
package org.openrewrite.java.internal.template;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.newSetFromMap;

@RequiredArgsConstructor
public class AnnotationTemplateGenerator {
    private static final String TEMPLATE_COMMENT = "__TEMPLATE_cfcc2025-6662__";

    private final Set<String> imports;

    public String cacheKey(Cursor cursor, String template) {
        StringBuilder after = new StringBuilder();

        J j = cursor.getValue();
        if (j instanceof J.MethodDeclaration) {
            after.insert(0, " void $method() {}");
        } else if (j instanceof J.VariableDeclarations) {
            after.insert(0, " int $variable;");
        } else if (j instanceof J.ClassDeclaration) {
            if (cursor.getParentOrThrow().getValue() instanceof JavaSourceFile) {
                after.insert(0, "class $Clazz {}");
            } else {
                after.insert(0, "static class $Clazz {}");
            }
        }

        if (cursor.getParentOrThrow().getValue() instanceof J.ClassDeclaration &&
                cursor.getParentOrThrow().getParentOrThrow().getValue() instanceof JavaSourceFile) {
            after.append("class $Template {}");
        }

        return "/*" + TEMPLATE_COMMENT + "*/" + template + "\n" + after;
    }

    public String template(Cursor cursor, String template) {
        //noinspection ConstantConditions
        return Timer.builder("rewrite.template.generate.statement")
                .register(Metrics.globalRegistry)
                .record(() -> {
                    StringBuilder before = new StringBuilder();
                    StringBuilder after = new StringBuilder();

                    template(next(cursor), cursor.getValue(), before, after, newSetFromMap(new IdentityHashMap<>()));

                    J j = cursor.getValue();
                    J annotationParent = j instanceof J.Annotation && cursor.getParent() != null ? cursor.getParent().firstEnclosing(J.class) : null;
                    if (j instanceof J.MethodDeclaration || annotationParent instanceof J.MethodDeclaration) {
                        after.insert(0, " void $method() {}");
                    } else if (j instanceof J.VariableDeclarations || annotationParent instanceof J.VariableDeclarations) {
                        after.insert(0, " int $variable;");
                    } else if (j instanceof J.ClassDeclaration) {
                        if (cursor.getParentOrThrow().getValue() instanceof JavaSourceFile) {
                            after.insert(0, "class $Clazz {}");
                        } else {
                            after.insert(0, "static class $Clazz {}");
                        }
                    }
                    return before + "/*" + TEMPLATE_COMMENT + "*/" + template + "\n" + after;
                });
    }

    public List<J.Annotation> listAnnotations(JavaSourceFile cu) {
        List<J.Annotation> annotations = new ArrayList<>();

        new JavaIsoVisitor<Integer>() {
            @Nullable
            private Comment filterTemplateComment(Comment comment) {
                return comment instanceof TextComment && ((TextComment) comment).getText().equals(TEMPLATE_COMMENT) ?
                        null : comment;
            }

            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, Integer integer) {
                J.Annotation withoutTemplateComment = annotation.withComments(
                        ListUtils.concatAll(
                                ListUtils.map(getCursor().getParentOrThrow().<J>getValue().getComments(), this::filterTemplateComment),
                                ListUtils.map(annotation.getComments(), this::filterTemplateComment)
                        ));
                annotations.add(withoutTemplateComment);
                return annotation;
            }
        }.visit(cu, 0);

        return annotations;
    }

    private void template(Cursor cursor, J prior, StringBuilder before, StringBuilder after, Set<J> templated) {
        templated.add(cursor.getValue());
        J j = cursor.getValue();
        if (j instanceof JavaSourceFile) {
            JavaSourceFile cu = (JavaSourceFile) j;
            for (J.Import anImport : cu.getImports()) {
                before.insert(0, anImport.withPrefix(Space.EMPTY).printTrimmed(cursor) + ";\n");
            }
            for (String anImport : imports) {
                before.insert(0, anImport);
            }

            if (cu.getPackageDeclaration() != null) {
                before.insert(0, cu.getPackageDeclaration().withPrefix(Space.EMPTY).printTrimmed(cursor) + ";\n");
            }
            List<J.ClassDeclaration> classes = cu.getClasses();
            if (!classes.get(classes.size() - 1).getName().getSimpleName().equals("$Placeholder")) {
                after.append("@interface $Placeholder {}");
            }
            return;
        }
        if (j instanceof J.Block) {
            J parent = next(cursor).getValue();
            if (parent instanceof J.ClassDeclaration) {
                classDeclaration(before, (J.ClassDeclaration) parent, templated, cursor);
                after.append('}');
            } else if (parent instanceof J.MethodDeclaration) {
                J.MethodDeclaration m = (J.MethodDeclaration) parent;

                // variable declarations up to the point of insertion
                assert m.getBody() != null;
                for (Statement statement : m.getBody().getStatements()) {
                    if (statement == prior) {
                        break;
                    } else if (statement instanceof J.VariableDeclarations) {
                        before.insert(0, "\n" +
                                variable((J.VariableDeclarations) statement, cursor) +
                                ";\n");
                    }
                }

                if (m.getReturnTypeExpression() != null && !JavaType.Primitive.Void
                        .equals(m.getReturnTypeExpression().getType())) {
                    after.append("return ")
                            .append(valueOfType(m.getReturnTypeExpression().getType()))
                            .append(";\n");
                }

                before.insert(0, m.withBody(null)
                        .withLeadingAnnotations(emptyList())
                        .withPrefix(Space.EMPTY)
                        .printTrimmed(cursor).trim() + '{');
                after.append('}');
            } else if (parent instanceof J.Block) {
                J.Block b = (J.Block) j;

                // variable declarations up to the point of insertion
                for (Statement statement : b.getStatements()) {
                    if (statement == prior) {
                        break;
                    } else if (statement instanceof J.VariableDeclarations) {
                        J.VariableDeclarations v = (J.VariableDeclarations) statement;
                        if (v.hasModifier(J.Modifier.Type.Final)) {
                            before.insert(0, "\n" + variable(v, cursor) + ";\n");
                        }
                    }
                }

                before.insert(0, "{\n");
                if (b.isStatic()) {
                    before.insert(0, "static");
                }
                after.append('}');
            }
        } else if (j instanceof J.VariableDeclarations) {
            J.VariableDeclarations v = (J.VariableDeclarations) j;
            if (v.hasModifier(J.Modifier.Type.Final)) {
                before.insert(0, variable((J.VariableDeclarations) j, cursor) + '=');
            }
        } else if (j instanceof J.NewClass) {
            J.NewClass n = (J.NewClass) j;
            n = n.withBody(null).withPrefix(Space.EMPTY);
            before.insert(0, '{');
            before.insert(0, n.printTrimmed(cursor.getParentOrThrow()).trim());
            after.append("};");
        }

        template(next(cursor), j, before, after, templated);
    }

    private void classDeclaration(StringBuilder before, J.ClassDeclaration parent, Set<J> templated, Cursor cursor) {
        J.ClassDeclaration c = parent;
        for (Statement statement : c.getBody().getStatements()) {
            if (templated.contains(statement)) {
                continue;
            }

            if (statement instanceof J.VariableDeclarations) {
                J.VariableDeclarations v = (J.VariableDeclarations) statement;
                if (v.hasModifier(J.Modifier.Type.Final) && v.hasModifier(J.Modifier.Type.Static)) {
                    before.insert(0, variable((J.VariableDeclarations) statement, cursor) + ";\n");
                }
            } else if (statement instanceof J.ClassDeclaration) {
                // this is a sibling class. we need declarations for all variables and methods.
                // setting prior to null will cause them all to be written.
                before.insert(0, '}');
                classDeclaration(before, (J.ClassDeclaration) statement, templated, cursor);
            }
        }
        c = c.withBody(J.Block.createEmptyBlock()).withLeadingAnnotations(null).withPrefix(Space.EMPTY);
        String printed = c.printTrimmed(cursor);
        int braceIndex = printed.lastIndexOf('{');
        before.insert(0, braceIndex == -1 ? printed + '{' : printed.substring(0, braceIndex + 1));
    }

    private String variable(J.VariableDeclarations variable, Cursor cursor) {
        StringBuilder varBuilder = new StringBuilder();
        if (variable.getTypeExpression() != null) {
            for (J.Modifier modifier : variable.getModifiers()) {
                varBuilder.append(modifier.getType().toString().toLowerCase()).append(' ');
            }
            varBuilder.append(variable.getTypeExpression().withPrefix(Space.EMPTY).printTrimmed(cursor))
                    .append(' ');
        }

        List<J.VariableDeclarations.NamedVariable> variables = variable.getVariables();
        for (int i = 0, variablesSize = variables.size(); i < variablesSize; i++) {
            J.VariableDeclarations.NamedVariable nv = variables.get(i);
            varBuilder.append(nv.getSimpleName());

            if (i < variables.size() - 1) {
                varBuilder.append(',');
            }
        }

        return varBuilder.toString();
    }

    private String valueOfType(@Nullable JavaType type) {
        JavaType.Primitive primitive = TypeUtils.asPrimitive(type);
        if (primitive != null) {
            switch (primitive) {
                case Boolean:
                    return "true";
                case Byte:
                case Char:
                case Int:
                case Double:
                case Float:
                case Long:
                case Short:
                    return "0";
                case String:
                case Null:
                    return "null";
                case None:
                case Void:
                default:
                    return "";
            }
        }

        return "null";
    }

    private Cursor next(Cursor c) {
        return c.getParentTreeCursor();
    }
}
