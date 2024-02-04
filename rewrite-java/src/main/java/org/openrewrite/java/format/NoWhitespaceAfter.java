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
package org.openrewrite.java.format;

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.*;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

import static java.util.Objects.requireNonNull;

public class NoWhitespaceAfter extends Recipe {
    @Override
    public String getDisplayName() {
        return "No whitespace after";
    }

    @Override
    public String getDescription() {
        return "Removes unnecessary whitespace appearing after a token. " +
               "A linebreak after a token is allowed unless `allowLineBreaks` is set to `false`, in which case it will be removed.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new NoWhitespaceAfterVisitor();
    }

    private static class NoWhitespaceAfterVisitor extends JavaIsoVisitor<ExecutionContext> {
        SpacesStyle spacesStyle;
        NoWhitespaceAfterStyle noWhitespaceAfterStyle;

        @Nullable
        EmptyForInitializerPadStyle emptyForInitializerPadStyle;

        @Nullable
        EmptyForIteratorPadStyle emptyForIteratorPadStyle;

        @Override
        public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
            // This visitor causes problems for Groovy sources as method invocations without parentheses get squashed
            return sourceFile instanceof J.CompilationUnit;
        }

        @Override
        public J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                SourceFile cu = (SourceFile) requireNonNull(tree);
                spacesStyle = cu.getStyle(SpacesStyle.class) == null ? IntelliJ.spaces() : cu.getStyle(SpacesStyle.class);
                noWhitespaceAfterStyle = cu.getStyle(NoWhitespaceAfterStyle.class) == null ? Checkstyle.noWhitespaceAfterStyle() : cu.getStyle(NoWhitespaceAfterStyle.class);
                emptyForInitializerPadStyle = cu.getStyle(EmptyForInitializerPadStyle.class);
                emptyForIteratorPadStyle = cu.getStyle(EmptyForIteratorPadStyle.class);
            }
            return super.visit(tree, ctx);
        }

        @Override
        public J.TypeCast visitTypeCast(J.TypeCast typeCast, ExecutionContext ctx) {
            J.TypeCast t = super.visitTypeCast(typeCast, ctx);
            if (Boolean.TRUE.equals(noWhitespaceAfterStyle.getTypecast())) {
                t = (J.TypeCast) new SpacesVisitor<>(spacesStyle.withOther(spacesStyle.getOther().withAfterTypeCast(false)), emptyForInitializerPadStyle, emptyForIteratorPadStyle).visitNonNull(t, ctx);
            }
            return t;
        }

        @Override
        public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
            J.MemberReference m = super.visitMemberReference(memberRef, ctx);
            if (Boolean.TRUE.equals(noWhitespaceAfterStyle.getMethodRef())) {
                m = (J.MemberReference) new SpacesVisitor<>(spacesStyle, emptyForInitializerPadStyle, emptyForIteratorPadStyle).visitNonNull(m, ctx);
            }
            return m;
        }

        @SuppressWarnings("deprecation")
        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            J.VariableDeclarations vd = super.visitVariableDeclarations(multiVariable, ctx);
            if (Boolean.TRUE.equals(noWhitespaceAfterStyle.getArrayDeclarator())) {
                // For backwards compatibility.
                if (vd.getDimensionsBeforeName().stream().anyMatch(d -> d.getBefore().getWhitespace().contains(" "))) {
                    vd = vd.withDimensionsBeforeName(ListUtils.map(vd.getDimensionsBeforeName(), d -> {
                        d = d.withBefore(d.getBefore().withWhitespace(""));
                        return d;
                    }));
                }
            }
            return vd;
        }

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
            J.Annotation a = super.visitAnnotation(annotation, ctx);
            if (Boolean.TRUE.equals(noWhitespaceAfterStyle.getAnnotation())) {
                a = a.withAnnotationType(a.getAnnotationType().withPrefix(
                        a.getAnnotationType().getPrefix().withWhitespace("")
                ));
            }
            return a;
        }

        @Override
        public J.ArrayType visitArrayType(J.ArrayType arrayType, ExecutionContext ctx) {
            J.ArrayType a = super.visitArrayType(arrayType, ctx);
            if (Boolean.TRUE.equals(noWhitespaceAfterStyle.getArrayDeclarator())) {
                if (a.getDimension() != null && a.getDimension().getBefore().getWhitespace().contains(" ")) {
                    if (a.getAnnotations() == null || a.getAnnotations().isEmpty()) {
                        a = a.withDimension(a.getDimension().withBefore(a.getDimension().getBefore().withWhitespace("")));
                    }
                }
            }
            return a;
        }

        @Override
        public J.NewArray visitNewArray(J.NewArray newArray, ExecutionContext ctx) {
            J.NewArray n = super.visitNewArray(newArray, ctx);
            if (Boolean.TRUE.equals(noWhitespaceAfterStyle.getArrayInitializer())) {
                n = (J.NewArray) new SpacesVisitor<>(spacesStyle, emptyForInitializerPadStyle, emptyForIteratorPadStyle).visitNonNull(n, ctx);
            }
            return n;
        }

        @Override
        public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, ExecutionContext ctx) {
            J.ArrayAccess a = super.visitArrayAccess(arrayAccess, ctx);
            if (Boolean.TRUE.equals(noWhitespaceAfterStyle.getIndexOperation())) {
                a = a.withDimension(a.getDimension().withPrefix(a.getDimension().getPrefix().withWhitespace("")));
            }
            return a;
        }

        @Override
        public J.Unary visitUnary(J.Unary unary, ExecutionContext ctx) {
            J.Unary u = super.visitUnary(unary, ctx);
            J.Unary.Type op = u.getOperator();
            if ((Boolean.TRUE.equals(noWhitespaceAfterStyle.getInc()) && op == J.Unary.Type.PreIncrement) ||
                (Boolean.TRUE.equals(noWhitespaceAfterStyle.getDec()) && op == J.Unary.Type.PreDecrement) ||
                (Boolean.TRUE.equals(noWhitespaceAfterStyle.getBnoc()) && op == J.Unary.Type.Complement) ||
                (Boolean.TRUE.equals(noWhitespaceAfterStyle.getLnot()) && op == J.Unary.Type.Not) ||
                (Boolean.TRUE.equals(noWhitespaceAfterStyle.getUnaryPlus()) && op == J.Unary.Type.Positive) ||
                (Boolean.TRUE.equals(noWhitespaceAfterStyle.getUnaryMinus()) && op == J.Unary.Type.Negative)
            ) {
                u = (J.Unary) new SpacesVisitor<>(spacesStyle, emptyForInitializerPadStyle, emptyForIteratorPadStyle).visitNonNull(u, ctx);
            }
            return u;
        }

        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            J.FieldAccess f = super.visitFieldAccess(fieldAccess, ctx);
            if (Boolean.TRUE.equals(noWhitespaceAfterStyle.getDot())) {
                if (Boolean.TRUE.equals(noWhitespaceAfterStyle.getAllowLineBreaks()) && f.getName().getPrefix().getWhitespace().contains("\n")) {
                    return f;
                }
                if (f.getName().getPrefix().getWhitespace().contains(" ")) {
                    f = f.withName(f.getName().withPrefix(
                            f.getName().getPrefix().withWhitespace("")
                    ));
                }
            }
            return f;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (Boolean.TRUE.equals(noWhitespaceAfterStyle.getDot())) {
                if (Boolean.TRUE.equals(noWhitespaceAfterStyle.getAllowLineBreaks()) && m.getName().getPrefix().getWhitespace().contains("\n")) {
                    return m;
                }
                m = m.withName(m.getName().withPrefix(
                        m.getName().getPrefix().withWhitespace("")
                ));
                m = (J.MethodInvocation) new SpacesVisitor<>(spacesStyle, emptyForInitializerPadStyle, emptyForIteratorPadStyle).visitNonNull(m, ctx);
            }
            return m;
        }
    }
}
