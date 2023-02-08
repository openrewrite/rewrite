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
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

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
    private static final String STOP_COMMENT = "__TEMPLATE_STOP__";
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
                        before.insert(0, m.printTrimmed(cursor.getParentOrThrow()).trim() + '{');
                        after.append('}');
                    }

                    template(next(cursor), cursor.getValue(), before, after, newSetFromMap(new IdentityHashMap<>()));

                    return before.toString().trim() + "\n/*" + TEMPLATE_COMMENT + "*/" + template + "/*" + STOP_COMMENT + "*/" + "\n" + after;
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
                        //noinspection unchecked
                        J2 trimmed = (J2) TemplatedTreeTrimmer.trimTree(t);
                        if (trimmed != null) {
                            js.add(trimmed);
                        } else {
                            done = true;
                        }
                        return t;
                    }

                    List<Comment> comments = t.getPrefix().getComments();
                    for (int i = 0; i < comments.size(); i++) {
                        Comment comment = comments.get(i);
                        if (comment instanceof TextComment && ((TextComment) comment).getText().equals(TEMPLATE_COMMENT)) {
                            blockEnclosingTemplateComment = getCursor().firstEnclosing(J.Block.class);
                            //noinspection unchecked
                            J2 trimmed = (J2) TemplatedTreeTrimmer.trimTree(t);
                            if (t != trimmed) {
                                done = true;
                            }
                            if (trimmed != null) {
                                js.add(trimmed.withPrefix(trimmed.getPrefix().withComments(comments.subList(i + 1, comments.size()))));
                            }
                            return t;
                        }
                    }
                }
                // Catch any trees having a STOP_COMMENT that are not an instance of `expected`
                else if (tree != null && !js.isEmpty()) {
                    //noinspection unchecked
                    J2 trimmed = (J2) TemplatedTreeTrimmer.trimTree((J) tree);
                    if (trimmed != tree) {
                        done = true;
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
                J.ClassDeclaration c = (J.ClassDeclaration) parent;
                if (c.getKind() == J.ClassDeclaration.Kind.Type.Enum) {
                    enumClassDeclaration(prior, c, before, after, templated, cursor, false);
                } else {
                    classDeclaration(prior, before, after, c, templated, cursor, false);
                }
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
        } else if (j instanceof J.DoWhileLoop) {
            J.DoWhileLoop dw = (J.DoWhileLoop) j;
            if (referToSameElement(prior, dw.getWhileCondition())) {
                insertControlWithBlock(dw.getBody(), before, after, () -> {
                    before.insert(0, "Object __b" + cursor.getPathAsStream().count() + "__ =");
                    after.append(";");
                });
            }
        } else if (j instanceof J.Assert) {
            before.insert(0, "assert ");
        } else if (j instanceof J.NewArray) {
            J.NewArray n = (J.NewArray) j;
            if (n.getInitializer() != null && n.getInitializer().stream().anyMatch(arg -> referToSameElement(prior, arg))) {
                before.insert(0, n.withInitializer(null).printTrimmed(cursor) + "{\n");
                after.append("\n}");
            } else {
                // same as `Expression` case
                before.insert(0, "__M__.any(");
                after.append(");");
            }
        } else if (j instanceof J.NewClass) {
            J.NewClass n = (J.NewClass) j;
            String newClassString;
            JavaType.Class constructorTypeClass = n.getConstructorType() != null ? TypeUtils.asClass(n.getConstructorType().getReturnType()) : null;
            boolean isEnum = constructorTypeClass != null && JavaType.FullyQualified.Kind.Enum == constructorTypeClass.getKind();
            if (n.getClazz() != null) {
                newClassString = "new " + n.getClazz().printTrimmed(cursor);
            } else if (constructorTypeClass != null) {
                // enum definitions with anonymous class initializers or a J.NewClass with a null clazz and valid constructor type.
                newClassString = isEnum ? "" : "new " + constructorTypeClass.getFullyQualifiedName();
            } else {
                throw new IllegalStateException("Unable to template a J.NewClass instance having a null clazz and constructor type.");
            }
            if (n.getArguments().stream().anyMatch(arg -> referToSameElement(prior, arg))) {
                StringBuilder beforeSegments = new StringBuilder();
                StringBuilder afterSegments = new StringBuilder();
                beforeSegments.append(newClassString).append("(");
                boolean priorFound = false;
                for (Expression arg : n.getArguments()) {
                    if (!priorFound) {
                        if (referToSameElement(prior, arg)) {
                            priorFound = true;
                            continue;
                        }
                        beforeSegments.append(valueOfType(arg.getType())).append(",");
                    } else {
                        afterSegments.append(",/*" + STOP_COMMENT + "*/").append(valueOfType(arg.getType()));
                    }
                }
                afterSegments.append(")");
                if (priorFound && !afterSegments.toString().contains(STOP_COMMENT)) {
                    if (isEnum) {
                        afterSegments.append(";");
                    }
                    afterSegments.append("/*" + STOP_COMMENT + "*/");
                }
                before.insert(0, beforeSegments);
                after.append(afterSegments);
                if (next(cursor).getValue() instanceof J.Block) {
                    after.append(";");
                }
            } else {
                n = n.withBody(null).withPrefix(Space.EMPTY);
                before.insert(0, n.printTrimmed(cursor.getParentOrThrow()).trim());
                if (!(next(cursor).getValue() instanceof MethodCall)) {
                    after.append(';');
                }
            }
        } else if (j instanceof J.ForLoop) {
            J.ForLoop f = (J.ForLoop) j;
            insertControlWithBlock(f.getBody(), before, after, () -> before.insert(0,
                    f.withBody(null).withPrefix(Space.EMPTY)
                            .withControl(f.getControl().withCondition(null).withUpdate(emptyList()))
                            .printTrimmed(cursor).trim()));
        } else if (j instanceof J.ForEachLoop.Control) {
            J.ForEachLoop.Control c = (J.ForEachLoop.Control) j;
            if (c.getVariable() == prior) {
                after.append(" = /*" + STOP_COMMENT + "/*").append(c.getIterable().printTrimmed(cursor));
            }
        } else if (j instanceof J.ForEachLoop) {
            J.ForEachLoop f = (J.ForEachLoop) j;
            if (!referToSameElement(prior, f.getControl())) {
                insertControlWithBlock(f.getBody(), before, after, () -> before.insert(0,
                        f.withBody(null).withPrefix(Space.EMPTY).printTrimmed(cursor).trim()));
            }
        } else if (j instanceof J.Try) {
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
                before.insert(0, "return ");
                after.append(";");
            }
            before.insert(0, l.withBody(null).withPrefix(Space.EMPTY).printTrimmed(cursor.getParentOrThrow()).trim() + "{ if(true) {");

            after.append("}\n");
            JavaType.Method mt = findSingleAbstractMethod(l.getType());
            if (mt == null) {
                // Missing type information, but usually the Java compiler can soldier on anyway
                after.append("return null;\n");
            } else if (mt.getReturnType() != JavaType.Primitive.Void) {
                after.append("return ").append(valueOfType(mt.getReturnType())).append(";\n");
            }
            after.append("}");
            if (next(cursor).getValue() instanceof J.Block) {
                after.append(";");
            }
        } else if (j instanceof J.VariableDeclarations) {
            before.insert(0, variable((J.VariableDeclarations) j, false, cursor) + '=');
            after.append(";");
        } else if (j instanceof J.MethodInvocation) {
            // If prior is an argument, wrap in __M__.any(prior)
            // If prior is a type parameter, wrap in __M__.anyT<prior>()
            // For anything else, ignore the invocation
            J.MethodInvocation m = (J.MethodInvocation) j;
            if (m.getArguments().stream().anyMatch(arg -> referToSameElement(prior, arg))) {
                before.insert(0, "__M__.any(");
                if (cursor.getParentOrThrow().firstEnclosing(J.class) instanceof J.Block) {
                    after.append(");");
                } else {
                    after.append(")");
                }
            } else if (m.getTypeParameters() != null && m.getTypeParameters().stream().anyMatch(tp -> referToSameElement(prior, tp))) {
                before.insert(0, "__M__.anyT<");
                if (cursor.getParentOrThrow().firstEnclosing(J.class) instanceof J.Block) {
                    after.append(">();");
                } else {
                    after.append(">()");
                }
            } else if (m.getSelect() == prior) {
                List<Comment> comments = new ArrayList<>(1);
                comments.add(new TextComment(true, STOP_COMMENT, "", Markers.EMPTY));
                after.append(".").append(m.withSelect(null).withComments(comments).printTrimmed(cursor.getParentOrThrow()));
                if (cursor.getParentOrThrow().firstEnclosing(J.class) instanceof J.Block) {
                    after.append(";");
                }
            }
        } else if (j instanceof J.Return) {
            before.insert(0, "return ");
            after.append(";");
        } else if (j instanceof J.If) {
            J.If iff = (J.If) j;
            if (referToSameElement(prior, iff.getIfCondition())) {
                insertControlWithBlock(iff.getThenPart(), before, after, () -> {
                    before.insert(0, "Object __b" + cursor.getPathAsStream().count() + "__ =");
                    after.append(";");
                });
            }
        } else if (j instanceof J.WhileLoop) {
            J.WhileLoop wl = (J.WhileLoop) j;
            if (referToSameElement(prior, wl.getCondition())) {
                insertControlWithBlock(wl.getBody(), before, after, () -> {
                    before.insert(0, "Object __b" + cursor.getPathAsStream().count() + "__ =");
                    after.append(";");
                });
            }
        } else if (j instanceof J.Assignment) {
            J.Assignment as = (J.Assignment) j;
            if (referToSameElement(prior, as.getAssignment())) {
                before.insert(0, as.getVariable() + " = ");
                after.append(";");
            }
        } else if (j instanceof J.EnumValue) {
            J.EnumValue ev = (J.EnumValue) j;
            before.insert(0, ev.getName());
        } else if (j instanceof J.EnumValueSet) {
            after.append(";");
        } else if (j instanceof Expression && j == prior) {
            before.insert(0, "__M__.any(");
            after.append(");");
        }
        template(next(cursor), j, before, after, templated);
    }

    private void insertControlWithBlock(J body, StringBuilder before, StringBuilder after, Runnable insertion) {
        insertion.run();
        if (!(body instanceof J.Block)) {
            before.insert(0, "{");
            after.append("}");
        }
    }

    // Enum class declarations are unique in that their EnumValue set must be the first statement
    private void enumClassDeclaration(@Nullable J prior, J.ClassDeclaration c, StringBuilder before, StringBuilder after, Set<J> templated, Cursor cursor, boolean isNestedClass) {
        StringBuilder enumClass = new StringBuilder("\nenum " + c.getSimpleName() + "{\n");
        List<Statement> statements = c.getBody().getStatements();
        for (Statement statement : statements) {
            if (templated.contains(statement)) {
                continue;
            }
            if (statement instanceof J.EnumValueSet) {
                J.EnumValueSet enumValues = (J.EnumValueSet) statements.get(0);
                StringJoiner enumStr = new StringJoiner(",");
                for (J.EnumValue anEnum : enumValues.getEnums()) {
                    String en = anEnum.getName().getSimpleName();
                    enumStr.add(en);
                }
                enumClass.append(enumStr).append(";\n");
            } else if (statement instanceof J.VariableDeclarations) {
                String vd = variable((J.VariableDeclarations) statement, false, cursor);
                if (isNestedClass) {
                    enumClass.append(vd).append(";\n");
                } else {
                    after.append(vd).append(";\n");
                }
            } else if (statement instanceof J.MethodDeclaration) {
                if (!referToSameElement(statement, prior)) {
                    String md = method((J.MethodDeclaration) statement, cursor);
                    if (isNestedClass) {
                        enumClass.append(md);
                    } else {
                        after.append(md);
                    }
                }
            } else if (statement instanceof J.ClassDeclaration) {
                // this is a sibling class. we need declarations for all variables and methods.
                // setting prior to null will cause them all to be written.
                // if the class is an enum then it's a nested enum and must be printed as a class.
                J.ClassDeclaration cd = (J.ClassDeclaration) statement;
                boolean isNestedEnum = cd.getKind() != J.ClassDeclaration.Kind.Type.Enum && !isNestedClass;
                classDeclaration(null, before, after, (J.ClassDeclaration) statement, templated, cursor, isNestedEnum);
            }
        }
        before.insert(0, enumClass);
        after.append("\n");
    }

    private void classDeclaration(@Nullable J prior, StringBuilder before, StringBuilder after, J.ClassDeclaration parent, Set<J> templated, Cursor cursor, boolean appendToAfter) {
        J.ClassDeclaration c = parent;

        List<Statement> statements = c.getBody().getStatements();
        for (int i = statements.size() - 1; i >= 0; i--) {
            Statement statement = statements.get(i);
            if (templated.contains(statement)) {
                continue;
            }

            if (statement instanceof J.VariableDeclarations) {
                String stmt = variable((J.VariableDeclarations) statement, false, cursor) + ";\n";
                if (appendToAfter) {
                    after.append(stmt);
                } else {
                    before.insert(0, stmt);
                }
            } else if (statement instanceof J.MethodDeclaration) {
                if (!referToSameElement(statement, prior)) {
                    String m = method((J.MethodDeclaration) statement, cursor);
                    if (appendToAfter) {
                        after.append(m);
                    } else {
                        before.insert(0, m);
                    }
                }
            } else if (statement instanceof J.ClassDeclaration) {
                // this is a sibling class. we need declarations for all variables and methods.
                // setting prior to null will cause them all to be written.
                if (appendToAfter) {
                    after.append('}');
                } else {
                    before.insert(0, '}');
                }
                J.ClassDeclaration cd = (J.ClassDeclaration) statement;
                if (cd.getKind() == J.ClassDeclaration.Kind.Type.Enum) {
                    enumClassDeclaration(prior, cd, before, after, templated, cursor, true);
                } else {
                    classDeclaration(prior, before, after, cd, templated, cursor, appendToAfter);
                }
                //classDeclaration(null, before, after, (J.ClassDeclaration) statement, templated, cursor, appendToAfter);
            } else if (statement instanceof J.EnumValueSet) {
                J.EnumValueSet enumValues = (J.EnumValueSet) statement;
                if (appendToAfter) {
                    after.append(";");
                } else {
                    before.insert(0, ";");
                }
                StringJoiner enumStr = new StringJoiner(",");
                for (J.EnumValue anEnum : enumValues.getEnums()) {
                    enumStr.add(anEnum.getName().getSimpleName());
                }
                if (appendToAfter) {
                    after.append(enumStr);
                } else {
                    before.insert(0, enumStr);
                }
            }
        }
        c = c.withBody(null).withLeadingAnnotations(null).withPrefix(Space.EMPTY);
        if (appendToAfter) {
            after.append(c.printTrimmed(cursor).trim()).append('{');
        } else {
            before.insert(0, c.printTrimmed(cursor).trim() + '{');
        }
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
                    if (nv.getInitializer() instanceof J.NewArray && !((J.NewArray) nv.getInitializer()).getDimensions().isEmpty()) {
                        J.NewArray na = (J.NewArray) nv.getInitializer();
                        na.getDimensions().forEach(d -> varBuilder.append("[]"));
                    } else {
                        varBuilder.append("[]");
                    }
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
        return c.getParentTreeCursor();
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
        if (javaType == null) {
            return null;
        }
        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(javaType);
        if (fq == null) {
            return null;
        }
        return fq.getMethods().stream().filter(method -> method.hasFlags(Flag.Abstract)).findAny().orElse(null);
    }

    // Visitor for removing any trees having or following the `STOP_COMMENT`
    private static class TemplatedTreeTrimmer {

        @Nullable
        static J trimTree(J j) {
            J trimmed = new TemplatedTreeTrimmerVisitor().visit(j, 0);
            if (trimmed == null || trimmed.getMarkers().findFirst(RemoveTreeMarker.class).isPresent()) {
                return null;
            }
            return trimmed;
        }

        @Value
        @AllArgsConstructor
        private static class RemoveTreeMarker implements Marker {
            @With
            UUID id;
        }

        private static class TemplatedTreeTrimmerVisitor extends JavaVisitor<Integer> {
            private boolean stopCommentExists(@Nullable J j) {
                if (j != null) {
                    for (Comment comment : j.getComments()) {
                        if (comment instanceof TextComment && ((TextComment) comment).getText().equals(STOP_COMMENT)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public @Nullable J visit(@Nullable Tree tree, Integer integer) {
                J j = super.visit(tree, integer);
                if (stopCommentExists(j)) {
                    Cursor parent = getCursor().getParent();
                    if (parent == null || !(parent.getValue() instanceof J.MethodInvocation)) {
                        return j.withMarkers(j.getMarkers().addIfAbsent(new RemoveTreeMarker(Tree.randomId())));
                    }
                }
                return j;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, integer);
                if (stopCommentExists(mi.getName())) {
                    //noinspection ConstantConditions
                    return mi.getSelect();
                }
                return mi;
            }
        }
    }
}
