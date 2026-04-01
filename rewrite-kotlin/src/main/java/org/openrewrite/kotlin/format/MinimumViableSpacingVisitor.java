/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin.format;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.marker.*;
import org.openrewrite.kotlin.tree.K;

import java.util.List;

public class MinimumViableSpacingVisitor<P> extends KotlinIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    @JsonCreator
    public MinimumViableSpacingVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @SuppressWarnings("unused")
    public MinimumViableSpacingVisitor() {
        this(null);
    }

    @Override
    public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, P p) {
        K.CompilationUnit kcu = super.visitCompilationUnit(cu, p);
        return kcu.getPadding().withStatements(visitStatementList(kcu.getPadding().getStatements()));
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);

        boolean first = c.getLeadingAnnotations().isEmpty();
        boolean hasFinalModifierOnly = (c.getModifiers().size() == 1) &&
                (c.getModifiers().get(0).getType() == J.Modifier.Type.Final);

        if (!hasFinalModifierOnly && !c.getModifiers().isEmpty()) {
            if (!first && Space.firstPrefix(c.getModifiers()).getWhitespace().isEmpty()) {
                c = c.withModifiers(Space.formatFirstPrefix(c.getModifiers(),
                        updateSpace(c.getModifiers().iterator().next().getPrefix(), true)));
            }
            if (c.getModifiers().size() > 1) {
                c = c.withModifiers(ListUtils.map(c.getModifiers(), (index, modifier) -> {
                    if (index > 0 &&
                            modifier.getPrefix().getWhitespace().isEmpty() &&
                            modifier.getType() != J.Modifier.Type.Final) {
                        return spaceBefore(modifier, true);
                    }
                    return modifier;
                }));
            }
            first = false;
        }

        if (c.getPadding().getKind().getPrefix().isEmpty()) {
            if (!first) {
                c = c.getPadding().withKind(c.getPadding().getKind().withPrefix(
                        c.getPadding().getKind().getPrefix().withWhitespace(" ")));
            }
            first = false;
        }

        if (!first && !c.getName().getMarkers().findFirst(Implicit.class).isPresent() &&
                c.getName().getPrefix().getWhitespace().isEmpty()) {
            c = c.withName(spaceBefore(c.getName(), true));
        }

        J.ClassDeclaration.Padding padding = c.getPadding();
        JContainer<J.TypeParameter> typeParameters = padding.getTypeParameters();
        if (typeParameters != null && !typeParameters.getElements().isEmpty()) {
            if (!first && !typeParameters.getBefore().getWhitespace().isEmpty()) {
                c = padding.withTypeParameters(typeParameters.withBefore(updateSpace(typeParameters.getBefore(), true)));
            }
        }

        if (c.getPadding().getExtends() != null) {
            Space before = c.getPadding().getExtends().getBefore();
            if (before.getWhitespace().isEmpty()) {
                c = c.getPadding().withExtends(c.getPadding().getExtends().withBefore(updateSpace(before, true)));
            }
        }

        return c.withBody(c.getBody().withStatements(ListUtils.map(c.getBody().getStatements(),
                (i, st) -> (i != 0) ? st.withPrefix(addNewline(st.getPrefix())) : st)));
    }

    private Space addNewline(Space prefix) {
        if (prefix.getComments().isEmpty() ||
                prefix.getWhitespace().contains("\n") ||
                prefix.getComments().get(0) instanceof Javadoc ||
                (prefix.getComments().get(0).isMultiline() && prefix.getComments().get(0)
                        .printComment(getCursor()).contains("\n"))) {
            return prefix.withWhitespace(minimumLines(prefix.getWhitespace()));
        }

        // the first comment is a trailing comment on the previous line
        return prefix.withComments(ListUtils.map(prefix.getComments(), (i, c) -> i == 0 ?
                c.withSuffix(minimumLines(c.getSuffix())) : c));
    }

    private String minimumLines(String whitespace) {
        String minWhitespace = whitespace;

        if (getNewLineCount(whitespace) == 0) {
            minWhitespace = "\n" + minWhitespace;
        }

        return minWhitespace;
    }

    private static int getNewLineCount(String whitespace) {
        int newLineCount = 0;
        for (char c : whitespace.toCharArray()) {
            if (c == '\n') {
                newLineCount++;
            }
        }
        return newLineCount;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);

        if (m.getMarkers().findFirst(PrimaryConstructor.class).isPresent()) {
            return m;
        }

        boolean first = m.getLeadingAnnotations().isEmpty();
        if (!m.getModifiers().isEmpty()) {
            boolean firstFinal = m.getModifiers().get(0).getType() == J.Modifier.Type.Final;
            int startPosition = firstFinal ? 1 : 0;

            if (!first && !firstFinal && Space.firstPrefix(m.getModifiers()).getWhitespace().isEmpty()) {
                m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(),
                        updateSpace(m.getModifiers().iterator().next().getPrefix(), true)));
            }

            if (m.getModifiers().size() > 1) {
                m = m.withModifiers(ListUtils.map(m.getModifiers(), (index, modifier) -> {
                    if (index > startPosition &&
                            modifier.getType() != J.Modifier.Type.Final &&
                            modifier.getPrefix().getWhitespace().isEmpty()) {
                        return spaceBefore(modifier, true);
                    }
                    return modifier;
                }));
            }
            first = false;
        }

        J.TypeParameters typeParameters = m.getAnnotations().getTypeParameters();
        if (typeParameters != null && !typeParameters.getTypeParameters().isEmpty()) {
            if (!first && typeParameters.getPrefix().getWhitespace().isEmpty()) {
                m = m.getAnnotations().withTypeParameters(
                        spaceBefore(typeParameters, true)
                );
            }
            first = false;
        }

        if (m.getReturnTypeExpression() != null && m.getReturnTypeExpression().getPrefix().getWhitespace().isEmpty()) {
            if (!first) {
                TypeTree returnTypeExpression = m.getReturnTypeExpression();
                // If it's a J.AnnotatedType, because the first annotation has its prefix, so don't need to set the
                // prefix for the return type again to avoid two spaces, instead, we need to trim the prefix of the 1st
                // annotation to be single space.
                if (returnTypeExpression instanceof J.AnnotatedType) {
                    J.AnnotatedType annotatedType = (J.AnnotatedType) returnTypeExpression;
                    List<J.Annotation> annotations = ListUtils.mapFirst(annotatedType.getAnnotations(), annotation ->
                            spaceBefore(annotation, true)
                    );
                    m = m.withReturnTypeExpression(annotatedType.withAnnotations(annotations));
                }
            }
            first = false;
        }

        boolean hasReceiverType = method.getMarkers().findFirst(Extension.class).isPresent();
        if (!first && !hasReceiverType) {
            m = m.withName(m.getName().withPrefix(updateSpace(m.getName().getPrefix(), true)));
        } else if (m.getPrefix().isEmpty() && getCursor().getParentTreeCursor().getValue() instanceof K.Property) {
            m = spaceBefore(m, true);
        }

        if (m.getPadding().getThrows() != null) {
            Space before = m.getPadding().getThrows().getBefore();
            if (before.getWhitespace().isEmpty()) {
                m = m.getPadding().withThrows(m.getPadding().getThrows().withBefore(updateSpace(before, true)));
            }
        }

        return m;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation m = super.visitMethodInvocation(method, p);
        boolean infix = m.getMarkers().findFirst(Infix.class).isPresent();
        if (infix) {
            m = m.withName(m.getName().withPrefix(updateSpace(m.getName().getPrefix(), true)));
            m = m.getPadding().withArguments(spaceBefore(m.getPadding().getArguments(), true));
        }
        return m;
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        J.Block b = super.visitBlock(block, p);
        return b.getPadding().withStatements(visitStatementList(b.getPadding().getStatements()));
    }

    private List<JRightPadded<Statement>> visitStatementList(List<JRightPadded<Statement>> statements) {
        return ListUtils.map(statements,
                (i, st) -> {
                    Statement element = st.getElement();
                    if (i == 0 ||
                            element.getPrefix().getWhitespace().contains("\n") ||
                            element.getPrefix().getLastWhitespace().contains("\n") ||
                            statements.get(i - 1).getMarkers().findFirst(Semicolon.class).isPresent()) {
                        return st;
                    }
                    return st.withElement(element.withPrefix(addNewline(element.getPrefix())));
                });
    }

    @Override
    public J.Return visitReturn(J.Return return_, P p) {
        J.Return r = super.visitReturn(return_, p);
        if (r.getExpression() != null && r.getExpression().getPrefix().getWhitespace().isEmpty() &&
                !return_.getMarkers().findFirst(ImplicitReturn.class).isPresent()) {
            r = r.withExpression(spaceBefore(r.getExpression(), true));
        }
        return r;
    }

    @Override
    public K.Binary visitBinary(K.Binary binary, P p) {
        K.Binary kb = super.visitBinary(binary, p);
        if (kb.getOperator() == K.Binary.Type.Contains || kb.getOperator() == K.Binary.Type.NotContains) {
            if (!(kb.getLeft() instanceof J.Empty)) {
                kb = kb.getPadding().withOperator(spaceBefore(kb.getPadding().getOperator(), true));
            }
            kb = kb.withRight(spaceBefore(kb.getRight(), true));
        }
        return kb;
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public J.If visitIf(J.If iff, P p) {
        J.If updatedIff = super.visitIf(iff, p);

        if (updatedIff.getElsePart() != null) {
            updatedIff = updatedIff.withElsePart(spaceBefore(updatedIff.getElsePart(), true));
            updatedIff = updatedIff.withElsePart(updatedIff.getElsePart().withBody(spaceBefore(updatedIff.getElsePart().getBody(), true)));
        }
        return updatedIff;
    }

    @Override
    public J.ForEachLoop.Control visitForEachControl(J.ForEachLoop.Control control, P p) {
        J.ForEachLoop.Control c = super.visitForEachControl(control, p);
        c = c.getPadding().withVariable(c.getPadding().getVariable().withAfter(updateSpace(c.getPadding().getVariable().getAfter(), true)));
        return c.withIterable(spaceBefore(c.getIterable(), true));
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, p);

        boolean first = v.getLeadingAnnotations().isEmpty();

        /*
         * We need at least one space between multiple modifiers, otherwise we could get a run-on like "publicstaticfinal".
         * Note, this is applicable anywhere that modifiers can exist, such as class declarations, etc.
         */
        if (!v.getModifiers().isEmpty()) {
            boolean needFirstSpace = !first;
            v = v.withModifiers(
                    ListUtils.map(v.getModifiers(), (index, modifier) -> {
                        if (index != 0 || needFirstSpace) {
                            if (modifier.getPrefix().getWhitespace().isEmpty()) {
                                modifier = spaceBefore(modifier, true);
                            }
                        }
                        return modifier;
                    })
            );
        }

        J firstEnclosing = getCursor().getParentOrThrow().firstEnclosing(J.class);
        if (!v.getVariables().isEmpty() && !(firstEnclosing instanceof J.Lambda)) {
            boolean extension = v.getMarkers().findFirst(Extension.class).isPresent();
            if (v.getVariables().get(0).getPrefix().getWhitespace().isEmpty() && !v.getModifiers().isEmpty() && !extension) {
                v = v.withVariables(ListUtils.mapFirst(v.getVariables(), v0 -> v0.withName(spaceBefore(v0.getName(), true))));
            }
        }

        return v;
    }

    @Override
    public @Nullable J postVisit(J tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (J) tree;
        }
        return super.visit(tree, p);
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> JLeftPadded<T> spaceBefore(JLeftPadded<T> padded, boolean spaceBefore) {
        if (!padded.getBefore().getComments().isEmpty()) {
            return padded;
        }

        return padded.withBefore(updateSpace(padded.getBefore(), spaceBefore));
    }

    @SuppressWarnings("SameParameterValue")
    private static <T extends J> JContainer<T> spaceBefore(JContainer<T> container, boolean spaceBefore) {
        if (!container.getBefore().getComments().isEmpty()) {
            return container;
        }

        return container.withBefore(updateSpace(container.getBefore(), spaceBefore));
    }

    @SuppressWarnings("SameParameterValue")
    private static <T extends J> T spaceBefore(T j, boolean spaceBefore) {
        if (!j.getComments().isEmpty()) {
            return j;
        }

        return j.withPrefix(updateSpace(j.getPrefix(), spaceBefore));
    }

    @SuppressWarnings("SameParameterValue")
    private static Space updateSpace(Space s, boolean haveSpace) {
        if (!s.getComments().isEmpty()) {
            return s;
        }

        if (haveSpace && notSingleSpace(s.getWhitespace())) {
            return s.withWhitespace(" ");
        } else if (!haveSpace && onlySpacesAndNotEmpty(s.getWhitespace())) {
            return s.withWhitespace("");
        } else {
            return s;
        }
    }

    /**
     * Checks if a string only contains spaces or tabs (excluding newline characters).
     *
     * @return true if contains spaces or tabs only, or true for empty string.
     */
    private static boolean onlySpaces(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t') {
                return false;
            }
        }
        return true;
    }

    private static boolean onlySpacesAndNotEmpty(String s) {
        return !StringUtils.isNullOrEmpty(s) && onlySpaces(s);
    }

    private static boolean notSingleSpace(String str) {
        return onlySpaces(str) && !" ".equals(str);
    }
}
