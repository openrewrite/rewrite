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
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.newSetFromMap;

/**
 * Generates a stub containing enough variable, method, and class scope
 * for the insertion of a statement in any block scope.
 */
@RequiredArgsConstructor
public class BlockStatementTemplateGenerator {
    private static final String TEMPLATE_COMMENT = "__TEMPLATE__";

    private final Set<String> imports;

    public String template(Cursor cursor, String template, Space.Location location) {
        //noinspection ConstantConditions
        return Timer.builder("rewrite.template.generate.statement")
                .register(Metrics.globalRegistry)
                .record(() -> {
                    StringBuilder before = new StringBuilder();
                    StringBuilder after = new StringBuilder();

                    // for replaceBody()
                    if (cursor.getValue() instanceof J.MethodDeclaration &&
                            location.equals(Space.Location.BLOCK_PREFIX)) {
                        J.MethodDeclaration method = cursor.getValue();
                        J.MethodDeclaration m = method.withBody(null).withLeadingAnnotations(emptyList()).withPrefix(Space.EMPTY);
                        before.insert(0, m.printTrimmed().trim() + '{');
                        after.append('}');
                    }

                    template(next(cursor), cursor.getValue(), before, after, newSetFromMap(new IdentityHashMap<>()));

                    return before.toString().trim() + "\n/*" + TEMPLATE_COMMENT + "*/" + template + "\n" + after;
                });
    }

    public List<Statement> listTemplatedStatements(J.CompilationUnit cu) {
        List<Statement> statements = new ArrayList<>();

        new JavaIsoVisitor<Integer>() {
            boolean done = false;

            @Nullable
            J.Block blockEnclosingTemplateComment;

            @Override
            public Statement visitStatement(Statement statement, Integer integer) {
                return statement;
            }

            @Override
            public J.Block visitBlock(J.Block block, Integer integer) {
                J.Block b = super.visitBlock(block, integer);
                if (b == blockEnclosingTemplateComment) {
                    done = true;
                }
                return b;
            }

            @Nullable
            @Override
            public J visit(@Nullable Tree tree, Integer integer) {
                if (done) {
                    return (J) tree;
                }

                if (tree instanceof Statement) {
                    Statement statement = (Statement) tree;

                    if (blockEnclosingTemplateComment != null) {
                        statements.add(statement);
                        return statement;
                    }

                    for (Comment comment : statement.getPrefix().getComments()) {
                        if(comment instanceof TextComment && ((TextComment) comment).getText().equals(TEMPLATE_COMMENT)) {
                            blockEnclosingTemplateComment = getCursor().firstEnclosing(J.Block.class);
                            statements.add(statement.withPrefix(Space.EMPTY));
                            return statement;
                        }
                    }
                }

                return super.visit(tree, integer);
            }
        }.visit(cu, 0);

        return statements;
    }

    @SuppressWarnings("ConstantConditions")
    private void template(Cursor cursor, J prior, StringBuilder before, StringBuilder after, Set<J> templated) {
        templated.add(cursor.getValue());
        J j = cursor.getValue();
        if (j instanceof J.CompilationUnit) {
            J.CompilationUnit cu = (J.CompilationUnit) j;
            for (J.Import anImport : cu.getImports()) {
                before.insert(0, anImport.withPrefix(Space.EMPTY).printTrimmed() + ";\n");
            }
            for (String anImport : imports) {
                before.insert(0, anImport);
            }

            if (cu.getPackageDeclaration() != null) {
                before.insert(0, cu.getPackageDeclaration().withPrefix(Space.EMPTY).printTrimmed() + ";\n");
            }
            return;
        } else if (j instanceof J.Block) {
            J parent = next(cursor).getValue();
            if (parent instanceof J.ClassDeclaration) {
                classDeclaration(prior, before, (J.ClassDeclaration) parent, templated);
            } else if (parent instanceof J.MethodDeclaration) {
                J.MethodDeclaration m = (J.MethodDeclaration) parent;

                // variable declarations up to the point of insertion
                assert m.getBody() != null;
                for (Statement statement : m.getBody().getStatements()) {
                    if (statement == prior) {
                        break;
                    } else if (statement instanceof J.VariableDeclarations) {
                        before.insert(0, "\n" +
                                variable((J.VariableDeclarations) statement, true) +
                                ";\n");
                    }
                }

                if (m.getReturnTypeExpression() != null && !JavaType.Primitive.Void
                        .equals(m.getReturnTypeExpression().getType())) {
                    before.insert(0, "if(true) {");
                    after.append("}\nreturn ")
                            .append(valueOfType(m.getReturnTypeExpression().getType()))
                            .append(";\n");
                }

                before.insert(0, m.withBody(null)
                        .withLeadingAnnotations(emptyList())
                        .withPrefix(Space.EMPTY)
                        .printTrimmed().trim() + '{');
            } else if (parent instanceof J.Block) {
                J.Block b = (J.Block) j;

                // variable declarations up to the point of insertion
                for (Statement statement : b.getStatements()) {
                    if (statement == prior) {
                        break;
                    } else if (statement instanceof J.VariableDeclarations) {
                        before.insert(0, "\n" +
                                variable((J.VariableDeclarations) statement, true) +
                                ";\n");
                    }
                }

                before.insert(0, "{\n");
                if (b.isStatic()) {
                    before.insert(0, "static");
                }
            } else if (parent instanceof J.Lambda) {
                before.insert(0, "{\n");
            } else {
                before.insert(0, "{\n");
            }

            after.append('}');
            if (parent instanceof J.Lambda) {
                after.append(';');
            }
        } else if (j instanceof J.NewClass) {
            J.NewClass n = (J.NewClass) j;
            n = n.withBody(null).withPrefix(Space.EMPTY);
            before.insert(0, n.printTrimmed().trim());
            after.append(';');
        } else if (j instanceof J.ForLoop) {
            J.ForLoop f = (J.ForLoop) j;
            f = f.withBody(null).withPrefix(Space.EMPTY)
                    .withControl(f.getControl().withCondition(null).withUpdate(emptyList()));
            before.insert(0, f.printTrimmed().trim());
        } else if (j instanceof J.ForEachLoop) {
            J.ForEachLoop f = (J.ForEachLoop) j;
            f = f.withBody(null).withPrefix(Space.EMPTY);
            before.insert(0, f.printTrimmed().trim());
        } else if (j instanceof J.Try) {
            J.Try t = (J.Try) j;
            if (t.getResources() != null) {
                before.insert(0, ")");
                for (J.Try.Resource resource : t.getResources()) {
                    before.insert(0, resource.withPrefix(Space.EMPTY).printTrimmed().trim() + ';');
                }
                before.insert(0, "try(");
                after.append("catch(Throwable t) { throw new RuntimeException(t); }");
            }
        } else if (j instanceof J.Lambda) {
            // lambda with a single statement and no block
            J.Lambda l = (J.Lambda) j;
            before.insert(0, "{ if(true) {");
            after.append("}\nreturn ").append(valueOfType(l.getType())).append(";\n};\n");

            before.insert(0, l.withBody(null).withPrefix(Space.EMPTY).printTrimmed().trim());
        } else if (j instanceof J.VariableDeclarations) {
            before.insert(0, variable((J.VariableDeclarations) j, false) + '=');
        }
        template(next(cursor), j, before, after, templated);
    }

    private void classDeclaration(@Nullable J prior, StringBuilder before, J.ClassDeclaration parent, Set<J> templated) {
        J.ClassDeclaration c = parent;
        for (Statement statement : c.getBody().getStatements()) {
            if(templated.contains(statement)) {
                continue;
            }

            if (statement instanceof J.VariableDeclarations) {
                before.insert(0, variable((J.VariableDeclarations) statement, false) + ";\n");
            } else if (statement instanceof J.MethodDeclaration) {
                if (statement != prior) {
                    before.insert(0, method((J.MethodDeclaration) statement));
                }
            } else if (statement instanceof J.ClassDeclaration) {
                // this is a sibling class. we need declarations for all variables and methods.
                // setting prior to null will cause them all to be written.
                before.insert(0, '}');
                classDeclaration(null, before, (J.ClassDeclaration) statement, templated);
            }
        }
        c = c.withBody(null).withLeadingAnnotations(null).withPrefix(Space.EMPTY);
        before.insert(0, c.printTrimmed().trim() + '{');
    }

    private String method(J.MethodDeclaration method) {
        if (method.isAbstract()) {
            return "\n" + method.withPrefix(Space.EMPTY).printTrimmed().trim() + ";\n";
        }

        StringBuilder methodBuilder = new StringBuilder("\n");
        J.MethodDeclaration m = method.withBody(null).withLeadingAnnotations(emptyList()).withPrefix(Space.EMPTY);
        methodBuilder.append(m.printTrimmed().trim()).append('{');
        if (method.getReturnTypeExpression() != null && !JavaType.Primitive.Void.equals(method.getReturnTypeExpression().getType())) {
            methodBuilder.append("\nreturn ")
                    .append(valueOfType(method.getReturnTypeExpression().getType()))
                    .append(";\n");
        }
        methodBuilder.append("}\n");
        return methodBuilder.toString();
    }

    private String variable(J.VariableDeclarations variable, boolean initializer) {
        StringBuilder varBuilder = new StringBuilder();
        for (J.Modifier modifier : variable.getModifiers()) {
            varBuilder.append(modifier.getType().toString().toLowerCase()).append(' ');
        }

        List<J.VariableDeclarations.NamedVariable> variables = variable.getVariables();
        for (int i = 0, variablesSize = variables.size(); i < variablesSize; i++) {
            J.VariableDeclarations.NamedVariable nv = variables.get(i);
            if (i == 0) {
                if (variable.getTypeExpression() != null) {
                    varBuilder.append(variable.getTypeExpression().withPrefix(Space.EMPTY).printTrimmed());
                }
                if (nv.getType() instanceof JavaType.Array) {
                    varBuilder.append("[]");
                }
                varBuilder.append(" ");
            }

            varBuilder.append(nv.getSimpleName());

            JavaType type = nv.getType();
            if (initializer && type != null) {
                varBuilder.append('=').append(valueOfType(type));
            }

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
                case Wildcard:
                case Void:
                default:
                    return "";
            }
        }

        return "null";
    }

    private Cursor next(Cursor c) {
        return c.dropParentUntil(J.class::isInstance);
    }
}
