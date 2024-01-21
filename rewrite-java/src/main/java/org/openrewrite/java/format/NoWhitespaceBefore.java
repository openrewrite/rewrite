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

public class NoWhitespaceBefore extends Recipe {
    @Override
    public String getDisplayName() {
        return "No whitespace before";
    }

    @Override
    public String getDescription() {
        return "Removes unnecessary whitespace preceding a token. " +
               "A linebreak before a token will be removed unless `allowLineBreaks` is set to `true`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new NoWhitespaceBeforeVisitor();
    }

    private static class NoWhitespaceBeforeVisitor extends JavaIsoVisitor<ExecutionContext> {
        SpacesStyle spacesStyle;
        NoWhitespaceBeforeStyle noWhitespaceBeforeStyle;

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
                noWhitespaceBeforeStyle = cu.getStyle(NoWhitespaceBeforeStyle.class) == null ? Checkstyle.noWhitespaceBeforeStyle() : cu.getStyle(NoWhitespaceBeforeStyle.class);
                emptyForInitializerPadStyle = cu.getStyle(EmptyForInitializerPadStyle.class);
                emptyForIteratorPadStyle = cu.getStyle(EmptyForIteratorPadStyle.class);
            }
            return super.visit(tree, ctx);
        }

        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            J.FieldAccess f = super.visitFieldAccess(fieldAccess, ctx);
            if (Boolean.TRUE.equals(noWhitespaceBeforeStyle.getDot())) {
                if (Boolean.TRUE.equals(noWhitespaceBeforeStyle.getAllowLineBreaks()) && f.getPadding().getName().getBefore().getWhitespace().contains("\n")) {
                    return f;
                }
                if (f.getPadding().getName().getBefore().getWhitespace().contains(" ")) {
                    f = f.getPadding().withName(f.getPadding().getName().withBefore(
                            f.getPadding().getName().getBefore().withWhitespace("")
                    ));
                }
            }
            return f;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (Boolean.TRUE.equals(noWhitespaceBeforeStyle.getDot())) {
                if (Boolean.TRUE.equals(noWhitespaceBeforeStyle.getAllowLineBreaks()) && m.getPadding().getSelect() != null && m.getPadding().getSelect().getAfter().getWhitespace().contains("\n")) {
                    return m;
                }
                if (m.getPadding().getSelect() != null && m.getPadding().getSelect().getAfter().getWhitespace().contains(" ")) {
                    m = m.getPadding().withSelect(m.getPadding().getSelect().withAfter(
                            m.getPadding().getSelect().getAfter().withWhitespace("")
                    ));
                }
            }
            if (Boolean.TRUE.equals(noWhitespaceBeforeStyle.getComma())) {
                m = (J.MethodInvocation) new SpacesVisitor<>(spacesStyle, emptyForInitializerPadStyle, emptyForIteratorPadStyle).visitNonNull(m, ctx);
            }
            return m;
        }

        @Override
        public J.ForLoop visitForLoop(J.ForLoop forLoop, ExecutionContext ctx) {
            J.ForLoop f = super.visitForLoop(forLoop, ctx);
            if (Boolean.TRUE.equals(noWhitespaceBeforeStyle.getSemi())) {
                f = (J.ForLoop) new SpacesVisitor<>(spacesStyle, emptyForInitializerPadStyle, emptyForIteratorPadStyle).visitNonNull(f, ctx);
            }
            return f;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            J.VariableDeclarations vd = super.visitVariableDeclarations(multiVariable, ctx);
            if (Boolean.TRUE.equals(noWhitespaceBeforeStyle.getComma())) {
                if (Boolean.TRUE.equals(noWhitespaceBeforeStyle.getAllowLineBreaks()) && vd.getPadding().getVariables().stream().anyMatch(v -> v.getAfter().getWhitespace().contains("\n"))) {
                    return vd;
                }
                vd = vd.getPadding().withVariables(ListUtils.map(vd.getPadding().getVariables(), v -> {
                    v = v.withAfter(v.getAfter().withWhitespace(""));
                    return v;
                }));
            }
            return vd;
        }

        @Override
        public J.Unary visitUnary(J.Unary unary, ExecutionContext ctx) {
            J.Unary u = super.visitUnary(unary, ctx);
            J.Unary.Type op = u.getOperator();
            if ((Boolean.TRUE.equals(noWhitespaceBeforeStyle.getPostInc()) && op == J.Unary.Type.PostIncrement) ||
                (Boolean.TRUE.equals(noWhitespaceBeforeStyle.getPostDec() && op == J.Unary.Type.PostDecrement))) {
                if (Boolean.FALSE.equals(noWhitespaceBeforeStyle.getAllowLineBreaks()) && u.getPadding().getOperator().getBefore().getWhitespace().contains("\n")) {
                    u = u.getPadding().withOperator(u.getPadding().getOperator().withBefore(u.getPadding().getOperator().getBefore().withWhitespace("")));
                }
                u = (J.Unary) new SpacesVisitor<>(spacesStyle, emptyForInitializerPadStyle, emptyForIteratorPadStyle).visitNonNull(u, ctx);
            }
            return u;
        }

        @Override
        public J.ParameterizedType visitParameterizedType(J.ParameterizedType type, ExecutionContext ctx) {
            J.ParameterizedType p = super.visitParameterizedType(type, ctx);
            if (Boolean.TRUE.equals(noWhitespaceBeforeStyle.getGenericStart())) {
                if (p.getPadding().getTypeParameters() != null) {
                    p = p.getPadding().withTypeParameters(p.getPadding().getTypeParameters().withBefore(
                            p.getPadding().getTypeParameters().getBefore().withWhitespace("")
                    ));
                }
            }
            if (Boolean.TRUE.equals(noWhitespaceBeforeStyle.getGenericEnd())) {
                if (p.getPadding().getTypeParameters() != null) {
                    p = p.getPadding().withTypeParameters(p.getPadding().getTypeParameters().getPadding().withElements(
                            ListUtils.map(p.getPadding().getTypeParameters().getPadding().getElements(), e -> {
                                e = e.withAfter(e.getAfter().withWhitespace(""));
                                return e;
                            })
                    ));
                }
            }
            return p;
        }

        @Override
        public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
            J.MemberReference m = super.visitMemberReference(memberRef, ctx);
            if (Boolean.TRUE.equals(noWhitespaceBeforeStyle.getMethodRef())) {
                if (Boolean.TRUE.equals(noWhitespaceBeforeStyle.getAllowLineBreaks()) && m.getPadding().getContaining().getAfter().getWhitespace().contains("\n")) {
                    return m;
                }
                m = m.getPadding().withContaining(m.getPadding().getContaining().withAfter(
                        m.getPadding().getContaining().getAfter().withWhitespace("")
                ));
            }
            return m;
        }

    }

}
