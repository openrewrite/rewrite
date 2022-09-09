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
    static final String EXPR_STATEMENT_PARAM = "" +
            "class __P__ {" +
            "  static native <T> T p();" +
            "  static native <T> T[] arrp();" +
            "  static native boolean booleanp();" +
            "  static native byte bytep();" +
            "  static native char charp();" +
            "  static native double doublep();" +
            "  static native int intp();" +
            "  static native long longp();" +
            "  static native short shortp();" +
            "  static native float floatp();" +
            "}";
    private static final String METHOD_INVOCATION_STUBS = "" +
            "class __M__ {" +
            "  static native Object any(Object o);" +
            "  static native <T> Object anyT();" +
            "}";

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
                        before.insert(0, m.printTrimmed(cursor).trim() + '{');
                        after.append('}');
                    }

                    template(next(cursor), cursor.getValue(), before, after, newSetFromMap(new IdentityHashMap<>()));

                    return before.toString().trim() + "\n/*" + TEMPLATE_COMMENT + "*/" + template + "\n" + after;
                });
    }

    public <J2 extends J> List<J2> listTemplatedTrees(JavaSourceFile cu, Class<J2> expected) {
        List<J2> js = new ArrayList<>();

        new JavaIsoVisitor<Integer>() {
            boolean done = false;

            @Nullable
            J.Block blockEnclosingTemplateComment;

            @Override
            public J.Block visitBlock(J.Block block, Integer p) {
                J.Block b = super.visitBlock(block, p);
                if (b == blockEnclosingTemplateComment) {
                    done = true;
                }
                return b;
            }

            @Nullable
            @Override
            public J visit(@Nullable Tree tree, Integer p) {
                if (done) {
                    return (J) tree;
                }

                if (expected.isInstance(tree)) {
                    @SuppressWarnings("unchecked") J2 t = (J2) tree;

                    if (blockEnclosingTemplateComment != null) {
                        js.add(t);
                        return t;
                    }

                    List<Comment> comments = t.getPrefix().getComments();
                    for (int i = 0; i < comments.size(); i++) {
                        Comment comment = comments.get(i);
                        if (comment instanceof TextComment && ((TextComment) comment).getText().equals(TEMPLATE_COMMENT)) {
                            blockEnclosingTemplateComment = getCursor().firstEnclosing(J.Block.class);
                            js.add(t.withPrefix(t.getPrefix().withComments(comments.subList(i + 1, comments.size()))));
                            return t;
                        }
                    }
                }

                return super.visit(tree, p);
            }
        }.visit(cu, 0);

        return js;
    }

    @SuppressWarnings("ConstantConditions")
    private void template(Cursor cursor, J prior, StringBuilder before, StringBuilder after, Set<J> templated) {
        templated.add(cursor.getValue());
        J j = cursor.getValue();
        if (j instanceof JavaSourceFile) {
            before.insert(0, EXPR_STATEMENT_PARAM + METHOD_INVOCATION_STUBS);

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

            return;
        } else if (j instanceof J.Block) {
            J parent = next(cursor).getValue();
            if (parent instanceof J.ClassDeclaration) {
                classDeclaration(prior, before, (J.ClassDeclaration) parent, templated, cursor);
            } else if (parent instanceof J.MethodDeclaration) {
                J.MethodDeclaration m = (J.MethodDeclaration) parent;

                // variable declarations up to the point of insertion
                assert m.getBody() != null;
                for (Statement statement : m.getBody().getStatements()) {
                    if (referToSameElement(prior, statement)) {
                        break;
                    } else if (statement instanceof J.VariableDeclarations) {
                        before.insert(0, "\n" +
                                variable((J.VariableDeclarations) statement, true, cursor) +
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
                        .printTrimmed(cursor).trim() + '{');
            } else if (parent instanceof J.Block || parent instanceof J.Lambda) {
                J.Block b = (J.Block) j;

                // variable declarations up to the point of insertion
                for (Statement statement : b.getStatements()) {
                    if (referToSameElement(prior, statement)) {
                        break;
                    } else if (statement instanceof J.VariableDeclarations) {
                        before.insert(0, "\n" +
                                variable((J.VariableDeclarations) statement, true, cursor) +
                                ";\n");
                    }
                }

                before.insert(0, "{\n");
                if (b.isStatic()) {
                    before.insert(0, "static");
                }
            } else {
                before.insert(0, "{\n");
            }

            after.append('}');
        } else if (j instanceof J.NewClass) {
            J.NewClass n = (J.NewClass) j;
            if (n.getArguments().stream().anyMatch(arg -> referToSameElement(prior, arg))) {
                StringBuilder beforeSegments = new StringBuilder();
                StringBuilder afterSegments = new StringBuilder();
                beforeSegments.append("new ").append(n.getClazz().printTrimmed(cursor)).append("(");
                boolean priorFound = false;
                for(Expression arg : n.getArguments()) {
                    if(!priorFound) {
                        if (referToSameElement(prior, arg)) {
                            priorFound = true;
                            continue;
                        }
                        beforeSegments.append(valueOfType(arg.getType())).append(",");
                    } else {
                        afterSegments.append(",").append(valueOfType(arg.getType()));
                    }
                }
                afterSegments.append(")");
                before.insert(0, beforeSegments);
                after.append(afterSegments);
                if(next(cursor).getValue() instanceof J.Block) {
                    after.append(";");
                }
            } else {
                n = n.withBody(null).withPrefix(Space.EMPTY);
                before.insert(0, n.printTrimmed(cursor).trim());
                if (!(next(cursor).getValue() instanceof J.MethodInvocation)) {
                    after.append(';');
                }
            }
        } else if (j instanceof J.ForLoop) {
            J.ForLoop f = (J.ForLoop) j;
            f = f.withBody(null).withPrefix(Space.EMPTY)
                    .withControl(f.getControl().withCondition(null).withUpdate(emptyList()));
            before.insert(0, f.printTrimmed(cursor).trim());
        } else if (j instanceof J.ForEachLoop.Control) {
            J.ForEachLoop.Control c = (J.ForEachLoop.Control)j;
            if (c.getVariable() == prior) {
                after.append(c.getIterable().printTrimmed(cursor));
            }
        } else if (j instanceof J.ForEachLoop) {
            J.ForEachLoop f = (J.ForEachLoop) j;
            if (!referToSameElement(prior, f.getControl())) {
                f = f.withBody(null).withPrefix(Space.EMPTY);
                before.insert(0, f.printTrimmed(cursor).trim());
            }
        }else if (j instanceof J.Try) {
            J.Try t = (J.Try) j;
            if (t.getResources() != null) {
                before.insert(0, ")");
                for (J.Try.Resource resource : t.getResources()) {
                    before.insert(0, resource.withPrefix(Space.EMPTY).printTrimmed(cursor).trim() + ';');
                }
                before.insert(0, "try(");
            }
        } else if (j instanceof J.Lambda) {
            J.Lambda l = (J.Lambda) j;
            if (l.getBody() instanceof Expression) {
                before.insert(0,"return ");
                after.append(";");
            }
            before.insert(0, l.withBody(null).withPrefix(Space.EMPTY).printTrimmed(cursor).trim() + "{ if(true) {");

            after.append("}\n");
            JavaType.Method mt = findSingleAbstractMethod(l.getType());
            if(mt == null) {
                // Missing type information, but usually the Java compiler can soldier on anyway
                after.append("return null;\n");
            } else if(mt.getReturnType() != JavaType.Primitive.Void) {
                after.append("return ").append(valueOfType(mt.getReturnType())).append(";\n");
            }
            after.append("}");
            if(next(cursor).getValue() instanceof J.Block) {
                after.append(";");
            }
        } else if (j instanceof J.VariableDeclarations) {
            before.insert(0, variable((J.VariableDeclarations) j, false, cursor) + '=');
        } else if(j instanceof J.MethodInvocation) {
            // If prior is an argument, wrap in __M__.any(prior)
            // If prior is a type parameter, wrap in __M__.anyT<prior>()
            // For anything else, ignore the invocation
            J.MethodInvocation m = (J.MethodInvocation) j;
            if(m.getArguments().stream().anyMatch(arg -> referToSameElement(prior, arg))) {
                before.insert(0, "__M__.any(");
                if(cursor.getParentOrThrow().firstEnclosing(J.class) instanceof J.Block) {
                    after.append(");");
                } else {
                    after.append(")");
                }
            } else if(m.getTypeParameters() != null && m.getTypeParameters().stream().anyMatch(tp -> referToSameElement(prior, tp))) {
                before.insert(0, "__M__.anyT<");
                if(cursor.getParentOrThrow().firstEnclosing(J.class) instanceof J.Block) {
                    after.append(">();");
                } else {
                    after.append(">()");
                }
            }
//            else if (m.getSelect() == prior) {
//                after.append("." + m.withSelect(null).withComments(comments).printTrimmed(cursor));
//            }
        } else if(j instanceof J.Return) {
            before.insert(0, "return ");
            after.append(";");
        } else if(j instanceof J.If) {
            J.If iff = (J.If)j;
            if(referToSameElement(prior, iff.getIfCondition())) {
                before.insert(0, "Object __b" + cursor.getPathAsStream().count() + "__ =");
                after.append(";");
            }
        } else if(j instanceof J.Assignment) {
            J.Assignment as = (J.Assignment)j;
            if(referToSameElement(prior, as.getAssignment())) {
                before.insert(0, as.getVariable() + " = ");
                after.append(";");
            }
        }
        template(next(cursor), j, before, after, templated);
    }

    private void classDeclaration(@Nullable J prior, StringBuilder before, J.ClassDeclaration parent, Set<J> templated, Cursor cursor) {
        J.ClassDeclaration c = parent;

        List<Statement> statements = c.getBody().getStatements();
        for (int i = statements.size() - 1; i >= 0; i--) {
            Statement statement = statements.get(i);
            if (templated.contains(statement)) {
                continue;
            }

            if (statement instanceof J.VariableDeclarations) {
                before.insert(0, variable((J.VariableDeclarations) statement, false, cursor) + ";\n");
            } else if (statement instanceof J.MethodDeclaration) {
                if (!referToSameElement(statement, prior)) {
                    before.insert(0, method((J.MethodDeclaration) statement, cursor));
                }
            } else if (statement instanceof J.ClassDeclaration) {
                // this is a sibling class. we need declarations for all variables and methods.
                // setting prior to null will cause them all to be written.
                before.insert(0, '}');
                classDeclaration(null, before, (J.ClassDeclaration) statement, templated, cursor);
            } else if (statement instanceof J.EnumValueSet) {
                J.EnumValueSet enumValues = (J.EnumValueSet) statement;
                before.insert(0, ";");
                StringJoiner enumStr = new StringJoiner(",");
                for (J.EnumValue anEnum : enumValues.getEnums()) {
                    enumStr.add(anEnum.getName().getSimpleName());
                }
                before.insert(0, enumStr);
            }
        }
        c = c.withBody(null).withLeadingAnnotations(null).withPrefix(Space.EMPTY);
        before.insert(0, c.printTrimmed(cursor).trim() + '{');
    }

    private String method(J.MethodDeclaration method, Cursor cursor) {
        if (method.isAbstract()) {
            return "\n" + method.withPrefix(Space.EMPTY).printTrimmed(cursor).trim() + ";\n";
        }

        StringBuilder methodBuilder = new StringBuilder("\n");
        J.MethodDeclaration m = method.withBody(null).withLeadingAnnotations(emptyList()).withPrefix(Space.EMPTY);
        methodBuilder.append(m.printTrimmed(cursor).trim()).append('{');
        if (method.getReturnTypeExpression() != null && !JavaType.Primitive.Void.equals(method.getReturnTypeExpression().getType())) {
            methodBuilder.append("\nreturn ")
                    .append(valueOfType(method.getReturnTypeExpression().getType()))
                    .append(";\n");
        }
        methodBuilder.append("}\n");
        return methodBuilder.toString();
    }

    private String variable(J.VariableDeclarations variable, boolean initializer, Cursor cursor) {
        StringBuilder varBuilder = new StringBuilder();
        for (J.Modifier modifier : variable.getModifiers()) {
            varBuilder.append(modifier.getType().toString().toLowerCase()).append(' ');
        }

        List<J.VariableDeclarations.NamedVariable> variables = variable.getVariables();
        for (int i = 0, variablesSize = variables.size(); i < variablesSize; i++) {
            J.VariableDeclarations.NamedVariable nv = variables.get(i);
            if (i == 0) {
                if (variable.getTypeExpression() != null) {
                    varBuilder.append(variable.getTypeExpression().withPrefix(Space.EMPTY).printTrimmed(cursor));
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

    private static boolean referToSameElement(@Nullable Tree t1, @Nullable Tree t2) {
        return t1 == t2 || (t1 != null && t2 != null && t1.getId().equals(t2.getId()));
    }

    /**
     * Accepts a @FunctionalInterface and returns the single abstract method from it, or null if the single abstract
     * method cannot be found
     */
    @Nullable
    private static JavaType.Method findSingleAbstractMethod(@Nullable JavaType javaType) {
        if(javaType == null) {
            return null;
        }
        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(javaType);
        if(fq == null) {
            return null;
        }
        return fq.getMethods().stream().filter(method -> method.hasFlags(Flag.Abstract)).findAny().orElse(null);
    }
}
