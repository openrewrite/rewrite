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

import org.jspecify.annotations.Nullable;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.internal.KotlinPrinter;
import org.openrewrite.kotlin.marker.*;
import org.openrewrite.kotlin.style.SpacesStyle;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class SpacesVisitor<P> extends KotlinIsoVisitor<P> {

    @Nullable
    private final Tree stopAfter;

    private final SpacesStyle style;

    // Unconfigurable default formatting in IntelliJ's Kotlin formatting.
    private static final boolean beforeKeywords = true;
    private static final boolean beforeLeftBrace = true;
    private static final boolean withinParentheses = false;

    public SpacesVisitor(SpacesStyle style) {
        this(style, null);
    }

    public SpacesVisitor(SpacesStyle style, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;
    }

    <T extends J> T spaceBefore(T j, boolean spaceBefore) {
        if (!j.getComments().isEmpty()) {
            return j;
        }

        if (spaceBefore && notSingleSpace(j.getPrefix().getWhitespace())) {
            return j.withPrefix(j.getPrefix().withWhitespace(" "));
        } else if (!spaceBefore && onlySpacesAndNotEmpty(j.getPrefix().getWhitespace())) {
            return j.withPrefix(j.getPrefix().withWhitespace(""));
        } else {
            return j;
        }
    }

    Space updateSpace(Space s, boolean haveSpace) {
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

    <T> JContainer<T> spaceBefore(JContainer<T> container, boolean spaceBefore, boolean formatComment) {
        if (!container.getBefore().getComments().isEmpty()) {
            if (formatComment) {
                // Perform the space rule for the suffix of the last comment only. Same as IntelliJ.
                List<Comment> comments = spaceLastCommentSuffix(container.getBefore().getComments(), spaceBefore);
                return container.withBefore(container.getBefore().withComments(comments));
            }
            return container;
        }

        return container.withBefore(updateSpace(container.getBefore(), spaceBefore));
    }

    <T extends J> JLeftPadded<T> spaceBefore(JLeftPadded<T> container, boolean spaceBefore) {
        if (!container.getBefore().getComments().isEmpty()) {
            return container;
        }

        return container.withBefore(updateSpace(container.getBefore(), spaceBefore));
    }

    <T extends J> JLeftPadded<T> spaceBeforeLeftPaddedElement(JLeftPadded<T> container, boolean spaceBefore) {
        return container.withElement(spaceBefore(container.getElement(), spaceBefore));
    }

    @SuppressWarnings("SameParameterValue")
    <T extends J> JRightPadded<T> spaceBeforeRightPaddedElement(JRightPadded<T> container, boolean spaceBefore) {
        return container.withElement(spaceBefore(container.getElement(), spaceBefore));
    }

    <T extends J> JRightPadded<T> spaceAfter(JRightPadded<T> container, boolean spaceAfter) {
        if (!container.getAfter().getComments().isEmpty()) {
            // Perform the space rule for the suffix of the last comment only. Same as IntelliJ.
            List<Comment> comments = spaceLastCommentSuffix(container.getAfter().getComments(), spaceAfter);
            return container.withAfter(container.getAfter().withComments(comments));
        }

        return container.withAfter(updateSpace(container.getAfter(), spaceAfter));
    }

    private static List<Comment> spaceLastCommentSuffix(List<Comment> comments, boolean spaceSuffix) {
        return ListUtils.mapLast(comments,
                comment -> spaceSuffix(comment, spaceSuffix));
    }

    private static Comment spaceSuffix(Comment comment, boolean spaceSuffix) {
        if (spaceSuffix && notSingleSpace(comment.getSuffix())) {
            return comment.withSuffix(" ");
        } else if (!spaceSuffix && onlySpacesAndNotEmpty(comment.getSuffix())) {
            return comment.withSuffix("");
        } else {
            return comment;
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

    // handle space before colon after declaration name
    private Markers spaceBeforeColonAfterDeclarationName(Markers markers) {
        return markers.withMarkers(ListUtils.map(markers.getMarkers(), marker -> {
            if (marker instanceof TypeReferencePrefix) {
                TypeReferencePrefix mf = (TypeReferencePrefix) marker;
                return mf.withPrefix(updateSpace(mf.getPrefix(),
                        style.getOther().getBeforeColonAfterDeclarationName()));
            }
            return marker;
        }));
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
        boolean omitBraces = c.getBody().getMarkers().findFirst(OmitBraces.class).isPresent();
        c = c.withBody(spaceBefore(c.getBody(), beforeLeftBrace && !omitBraces));
        if (c.getBody().getStatements().isEmpty()) {
            if (c.getKind() != J.ClassDeclaration.Kind.Type.Enum) {
                // withinCodeBraces is defaulted to `false` in IntelliJ's Kotlin formatting.
                if (" ".equals(c.getBody().getEnd().getWhitespace())) {
                    c = c.withBody(
                            c.getBody().withEnd(
                                    c.getBody().getEnd().withWhitespace("")
                            )
                    );
                }
            } else {
                // withinCodeBraces is defaulted to `false` in IntelliJ's Kotlin formatting.
                if (" ".equals(c.getBody().getEnd().getWhitespace())) {
                    c = c.withBody(c.getBody().withEnd(c.getBody().getEnd().withWhitespace("")));
                }
            }
        }
        if (c.getPadding().getTypeParameters() != null) {
            c = c.getPadding().withTypeParameters(
                    spaceBefore(c.getPadding().getTypeParameters(),
                            false, true)
            );
        }
        if (c.getPadding().getTypeParameters() != null) {
            // spaceWithinAngleBrackets is defaulted to `false` in IntelliJ's Kotlin formatting.
            int typeParametersSize = c.getPadding().getTypeParameters().getElements().size();
            c = c.getPadding().withTypeParameters(
                    c.getPadding().getTypeParameters().getPadding().withElements(
                            ListUtils.map(c.getPadding().getTypeParameters().getPadding().getElements(),
                                    (index, elemContainer) -> {
                                        if (index == 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(), false)
                                            );
                                        } else {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(),
                                                            style.getOther().getAfterComma())
                                            );
                                        }
                                        if (index == typeParametersSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, false);
                                        }
                                        return elemContainer;
                                    }
                            )
                    )
            );
        }
        return c;
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);
        boolean isConstructor = m.getMarkers().findFirst(PrimaryConstructor.class).isPresent();
        boolean hasReceiverType = method.getMarkers().findFirst(Extension.class).isPresent();

        // beforeParenthesesOfMethodDeclaration is defaulted to `false` in IntelliJ's Kotlin formatting.
        m = m.getPadding().withParameters(
                spaceBefore(m.getPadding().getParameters(), false, false));

        // handle space before comma
        JContainer<Statement> jc = m.getPadding().getParameters();
        List<JRightPadded<Statement>> rps = jc.getPadding().getElements();
        if (rps.size() > 1) {
            int range = rps.size() - 1;
            rps = ListUtils.map(rps, (index, rp) -> (index < range) ? spaceAfter(rp, style.getOther().getBeforeComma()) : rp);
            m = m.getPadding().withParameters(jc.getPadding().withElements(rps));
        }

        // handle space after comma
        m = m.withParameters(ListUtils.map(
                m.getParameters(), (index, param) ->
                        index == 0 || index == 1 && hasReceiverType ? param : spaceBefore(param, style.getOther().getAfterComma())
        ));

        // handle space before colon after declaration name
        m = m.withMarkers(spaceBeforeColonAfterDeclarationName(m.getMarkers()));

        // handle space after colon before declaration type
        if (m.getReturnTypeExpression() != null && !isConstructor) {
            m = m.withReturnTypeExpression(spaceBefore(m.getReturnTypeExpression(), style.getOther().getAfterColonBeforeDeclarationType()));
        }

        if (m.getBody() != null) {
            m = m.withBody(spaceBefore(m.getBody(), beforeLeftBrace));
        }

        if (m.getParameters().isEmpty() || m.getParameters().get(0) instanceof J.Empty) {
            m = m.getPadding().withParameters(
                    m.getPadding().getParameters().getPadding().withElements(
                            ListUtils.map(m.getPadding().getParameters().getPadding().getElements(),
                                    param -> param.withElement(spaceBefore(param.getElement(), withinParentheses))
                            )
                    )
            );
        } else {
            final int paramsSize = m.getParameters().size();
            m = m.getPadding().withParameters(
                    m.getPadding().getParameters().getPadding().withElements(
                            ListUtils.map(m.getPadding().getParameters().getPadding().getElements(),
                                    (index, param) -> {
                                        if (index == 0 || index == 1 && hasReceiverType) {
                                            param = param.withElement(spaceBefore(param.getElement(), false));
                                        } else {
                                            param = param.withElement(
                                                    spaceBefore(param.getElement(), style.getOther().getAfterComma())
                                            );
                                        }
                                        if (index == paramsSize - 1) {
                                            param = spaceAfter(param, false);
                                        } else {
                                            param = spaceAfter(param, style.getOther().getBeforeComma());
                                        }
                                        return param;
                                    }
                            )
                    )
            );
        }

        if (m.getAnnotations().getTypeParameters() != null) {
            // spaceWithinAngleBrackets is defaulted to `false` in IntelliJ's Kotlin formatting.
            int typeParametersSize = m.getAnnotations().getTypeParameters().getTypeParameters().size();
            m = m.getAnnotations().withTypeParameters(
                    m.getAnnotations().getTypeParameters().getPadding().withTypeParameters(
                            ListUtils.map(m.getAnnotations().getTypeParameters().getPadding().getTypeParameters(),
                                    (index, elemContainer) -> {
                                        if (index == 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(), false)
                                            );
                                        } else {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(),
                                                            style.getOther().getAfterComma())
                                            );
                                        }
                                        if (index == typeParametersSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, false);
                                        }
                                        return elemContainer;
                                    })
                    )
            );
        }
        return m;
    }

    @Override
    public K.Property visitProperty(K.Property property, P p) {
        K.Property prop = super.visitProperty(property, p);

        if (prop.getPadding().getReceiver() != null) {
            prop = prop.getPadding().withReceiver(prop.getPadding().getReceiver().withAfter(updateSpace(prop.getPadding().getReceiver().getAfter(), false)));
        }

        if (!requireNonNull(prop).getVariableDeclarations().getVariables().isEmpty()) {
            List<J.VariableDeclarations.NamedVariable> variables = ListUtils.mapFirst(prop.getVariableDeclarations().getVariables(),
                    v -> spaceBefore(v, property.getReceiver() == null));
            JRightPadded<J.VariableDeclarations> rp = prop.getPadding().getVariableDeclarations();
            rp = rp.withElement(rp.getElement().withVariables(variables));
            prop = prop.getPadding().withVariableDeclarations(rp);
        }
        return prop;
    }

    @Override
    public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam, P p) {
        J.TypeParameter pa = super.visitTypeParameter(typeParam, p);

        // handle space before colon after declaration name
        pa = pa.withMarkers(spaceBeforeColonAfterDeclarationName(pa.getMarkers()));
        if (pa.getMarkers().findFirst(TypeReferencePrefix.class).isPresent()) {
            pa = pa.withBounds(
                    ListUtils.map(pa.getBounds(), b ->
                            spaceBefore(b, style.getOther().getAfterColonBeforeDeclarationType()))
            );
        }
        return pa;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation m = super.visitMethodInvocation(method, p);

        boolean noParens = m.getPadding().getArguments().getMarkers().findFirst(OmitParentheses.class).isPresent();

        // Defaulted to `false` if parens exist and to `true` if parens are omitted in Kotlin's formatting.
        m = m.getPadding().withArguments(spaceBefore(m.getPadding().getArguments(), false, false));
        if (m.getArguments().isEmpty() || m.getArguments().get(0) instanceof J.Empty) {
            // withInEmptyMethodCallParentheses is defaulted to `false` in IntelliJ's Kotlin formatting.
            m = m.getPadding().withArguments(
                    m.getPadding().getArguments().getPadding().withElements(
                            ListUtils.map(m.getPadding().getArguments().getPadding().getElements(),
                                    arg -> arg.withElement(spaceBefore(arg.getElement(), false))
                            )
                    )
            );
        } else {
            final int argsSize = m.getArguments().size();

            // Defaulted to `false` in IntelliJ's Kotlin formatting IFF parens exist.
            m = m.getPadding().withArguments(
                    m.getPadding().getArguments().getPadding().withElements(
                            ListUtils.map(m.getPadding().getArguments().getPadding().getElements(),
                                    (index, arg) -> {
                                        boolean before = index == 0 ? noParens : style.getOther().getAfterComma();
                                        if (arg.getElement().getMarkers().findFirst(SpreadArgument.class).isPresent()) {
                                            SpreadArgument spreadArgument = arg.getElement().getMarkers().findFirst(SpreadArgument.class).get();
                                            arg = arg.withElement(
                                                    arg.getElement().withMarkers(arg.getMarkers().setByType(spreadArgument
                                                            .withPrefix(updateSpace(spreadArgument.getPrefix(), before))))
                                            );
                                            arg = arg.withElement(
                                                    spaceBefore(arg.getElement(), style.getAroundOperators().getUnary())
                                            );
                                        } else {
                                            arg = arg.withElement(
                                                    spaceBefore(arg.getElement(), before)
                                            );
                                        }
                                        if (index == argsSize - 1) {
                                            arg = spaceAfter(arg, false);
                                        } else {
                                            arg = spaceAfter(arg, style.getOther().getBeforeComma());
                                        }
                                        return arg;
                                    }
                            )
                    )
            );
        }

        if (m.getPadding().getTypeParameters() != null) {
            // typeArgumentsBeforeOpeningAngleBracket is defaulted to `false` in IntelliJ's Kotlin formatting.
            m = m.getPadding().withTypeParameters(
                    spaceBefore(m.getPadding().getTypeParameters(), false, true)
            );
            // typeArgumentsAfterOpeningAngleBracket is defaulted to `false` in IntelliJ's Kotlin formatting.
            m = m.withName(spaceBefore(m.getName(), false));
        }
        if (m.getPadding().getTypeParameters() != null) {
            m = m.getPadding().withTypeParameters(
                    m.getPadding().getTypeParameters().getPadding().withElements(
                            ListUtils.map(m.getPadding().getTypeParameters().getPadding().getElements(),
                                    (index, elemContainer) -> {
                                        if (index != 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(),
                                                            style.getOther().getAfterComma())
                                            );
                                        }
                                        return elemContainer;
                                    }
                            )
                    )
            );
        }
        return m;
    }

    @Override
    public J.MultiCatch visitMultiCatch(J.MultiCatch multiCatch, P p) {
        J.MultiCatch mc = super.visitMultiCatch(multiCatch, p);
        final int argsSize = mc.getAlternatives().size();

        // Defaulted to `true` in IntelliJ's Kotlin formatting.
        boolean aroundOperatorsBitwise = true;
        return mc.getPadding().withAlternatives(
                ListUtils.map(mc.getPadding().getAlternatives(),
                        (index, arg) -> {
                            if (index > 0) {
                                arg = arg.withElement(
                                        spaceBefore(arg.getElement(), aroundOperatorsBitwise)
                                );
                            }
                            if (index != argsSize - 1) {
                                arg = spaceAfter(arg, aroundOperatorsBitwise);
                            }
                            return arg;
                        }
                )
        );
    }

    @Override
    public J.If visitIf(J.If iff, P p) {
        J.If i = super.visitIf(iff, p);
        i = i.withIfCondition(spaceBefore(i.getIfCondition(), style.getBeforeParentheses().getIfParentheses()));

        i = i.getPadding().withThenPart(spaceBeforeRightPaddedElement(i.getPadding().getThenPart(), beforeLeftBrace));

        // useSpaceWithinIfParentheses is defaulted to `false` in IntelliJ's Kotlin formatting.
        return i.withIfCondition(
                i.getIfCondition().getPadding().withTree(
                        spaceAfter(
                                i.getIfCondition().getPadding().getTree().withElement(
                                        spaceBefore(i.getIfCondition().getPadding().getTree().getElement(), false
                                        )
                                ),
                                false
                        )
                )
        );
    }

    @Override
    public J.If.Else visitElse(J.If.Else else_, P p) {
        J.If.Else e = super.visitElse(else_, p);

        e = e.getPadding().withBody(spaceBeforeRightPaddedElement(e.getPadding().getBody(), beforeLeftBrace));

        // Defaulted to `true` in IntelliJ's Kotlin formatting.
        boolean beforeKeywordsElseKeyword = true;
        return spaceBefore(e, beforeKeywordsElseKeyword);
    }

    @Override
    public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, P p) {
        J.ForEachLoop f = super.visitForEachLoop(forLoop, p);
        f = f.withControl(spaceBefore(f.getControl(), style.getBeforeParentheses().getForParentheses()));
        f = f.getPadding().withBody(spaceBeforeRightPaddedElement(f.getPadding().getBody(), beforeLeftBrace));
        boolean spaceWithinForParens = withinParentheses;
        f = f.withControl(
                f.getControl().withVariable(
                        spaceBefore(f.getControl().getVariable(), spaceWithinForParens)
                )
        );
        f = f.withControl(
                f.getControl().getPadding().withIterable(
                        spaceAfter(f.getControl().getPadding().getIterable(), spaceWithinForParens)
                )
        );
        boolean otherBeforeColonInForLoop = true;
        return f.withControl(
                f.getControl().getPadding().withVariable(
                        spaceAfter(f.getControl().getPadding().getVariable(), otherBeforeColonInForLoop)
                )
        );
    }

    @Override
    public K.SpreadArgument visitSpreadArgument(K.SpreadArgument spreadArgument, P p) {
        K.SpreadArgument s = super.visitSpreadArgument(spreadArgument, p);
        return s.withExpression(spaceBefore(s.getExpression(), style.getAroundOperators().getUnary()));
    }

    @Override
    public K.When visitWhen(K.When when, P p) {
        K.When w = super.visitWhen(when, p);
        if (w.getSelector() != null) {
            w = w.withSelector(spaceBefore(
                    w.getSelector().getPadding().withTree(
                            spaceAfter(
                                    w.getSelector().getPadding().getTree().withElement(
                                            spaceBefore(w.getSelector().getPadding().getTree().getElement(), false
                                            )
                                    ),
                                    false
                            )
                    ), style.getBeforeParentheses().getWhenParentheses())
            );
        }
        return w;
    }

    @Override
    public K.WhenBranch visitWhenBranch(K.WhenBranch whenBranch, P p) {
        K.WhenBranch wb = super.visitWhenBranch(whenBranch, p);
        List<JRightPadded<Expression>> rps = wb.getPadding().getExpressions().getPadding().getElements();

        // handle space between multiple cases
        if (rps.size() > 1) {
            int count = rps.size();
            rps = ListUtils.map(rps, (i, exp) -> {
                if (i > 0) {
                    exp = exp.withElement(spaceBefore(exp.getElement(), style.getOther().getAfterComma()));
                }

                if (i < (count - 1)) {
                    exp = spaceAfter(exp, style.getOther().getBeforeComma());
                }
                return exp;
            });
        }

        // handle space before arrow
        rps = ListUtils.mapLast(rps, exp -> spaceAfter(exp, style.getOther().getAroundArrowInWhenClause()));
        wb = wb.getPadding().withExpressions(wb.getPadding().getExpressions().getPadding().withElements(rps));

        // handle space after arrow
        if (wb.getBody() instanceof J.Block) {
            J.Block block = (J.Block) wb.getBody();
            if (block.getMarkers().findFirst(OmitBraces.class).isPresent()) {
                block = block.withStatements(ListUtils.mapFirst(block.getStatements(), s -> spaceBefore(s, style.getOther().getAroundArrowInWhenClause())));
            } else {
                block = spaceBefore(block, style.getOther().getAroundArrowInWhenClause());
            }

            wb = wb.withBody(block);
        } else {
            wb = wb.withBody(spaceBefore(wb.getBody(), style.getOther().getAroundArrowInWhenClause()));
        }

        return wb;
    }

    @Override
    public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, P p) {
        J.WhileLoop w = super.visitWhileLoop(whileLoop, p);
        w = w.withCondition(spaceBefore(w.getCondition(), style.getBeforeParentheses().getWhileParentheses()));
        w = w.getPadding().withBody(spaceBeforeRightPaddedElement(w.getPadding().getBody(), beforeLeftBrace));
        w = w.withCondition(
                w.getCondition().withTree(
                        spaceBefore(w.getCondition().getTree(), withinParentheses)
                )
        );
        return w.withCondition(
                w.getCondition().getPadding().withTree(
                        spaceAfter(w.getCondition().getPadding().getTree(), withinParentheses)
                )
        );
    }

    @Override
    public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        J.DoWhileLoop d = super.visitDoWhileLoop(doWhileLoop, p);
        d = d.getPadding().withWhileCondition(spaceBefore(d.getPadding().getWhileCondition(), beforeKeywords));
        d = d.getPadding().withWhileCondition(spaceBeforeLeftPaddedElement(d.getPadding().getWhileCondition(), style.getBeforeParentheses().getWhileParentheses()));
        d = d.getPadding().withBody(spaceBeforeRightPaddedElement(d.getPadding().getBody(), beforeLeftBrace));
        d = d.withWhileCondition(
                d.getWhileCondition().withTree(
                        spaceBefore(d.getWhileCondition().getTree(), withinParentheses)
                )
        );
        return d.withWhileCondition(
                d.getWhileCondition().getPadding().withTree(
                        spaceAfter(d.getWhileCondition().getPadding().getTree(), withinParentheses)
                )
        );
    }

    @Override
    public J.Try visitTry(J.Try _try, P p) {
        J.Try t = super.visitTry(_try, p);
        t = t.withBody(spaceBefore(t.getBody(), beforeLeftBrace));
        if (t.getPadding().getFinally() != null) {
            JLeftPadded<J.Block> f = spaceBefore(t.getPadding().getFinally(), beforeKeywords);
            f = spaceBeforeLeftPaddedElement(f, beforeLeftBrace);
            t = t.getPadding().withFinally(f);
        }
        if (t.getResources() != null) {
            t = t.withResources(ListUtils.mapFirst(t.getResources(), res -> spaceBefore(res, withinParentheses)));
        }
        if (t.getPadding().getResources() != null) {
            t = t.getPadding().withResources(
                    t.getPadding().getResources().getPadding().withElements(
                            ListUtils.mapLast(t.getPadding().getResources().getPadding().getElements(),
                                    res -> spaceAfter(res, withinParentheses)
                            )
                    )
            );
        }
        return t;
    }

    @Override
    public J.Try.Catch visitCatch(J.Try.Catch _catch, P p) {
        J.Try.Catch c = super.visitCatch(_catch, p);
        c = spaceBefore(c, beforeKeywords);
        c = c.withParameter(spaceBefore(c.getParameter(), style.getBeforeParentheses().getCatchParentheses()));
        c = c.withBody(spaceBefore(c.getBody(), beforeLeftBrace));
        c = c.withParameter(
                c.getParameter().withTree(
                        spaceBefore(c.getParameter().getTree(), withinParentheses)
                )
        );
        return c.withParameter(
                c.getParameter().getPadding().withTree(
                        spaceAfter(c.getParameter().getPadding().getTree(), withinParentheses)
                )
        );
    }

    @Override
    public J.Annotation visitAnnotation(J.Annotation annotation, P p) {
        J.Annotation a = super.visitAnnotation(annotation, p);
        if (a.getPadding().getArguments() != null) {
            // beforeParenthesesOfAnnotation is defaulted to `false` in IntelliJ's Kotlin formatting.
            a = a.getPadding().withArguments(spaceBefore(a.getPadding().getArguments(), false, true));
        }
        if (a.getPadding().getArguments() != null) {
            int argsSize = a.getPadding().getArguments().getElements().size();
            a = a.getPadding().withArguments(
                    a.getPadding().getArguments().getPadding().withElements(
                            ListUtils.map(a.getPadding().getArguments().getPadding().getElements(),
                                    (index, arg) -> {
                                        if (index == 0) {
                                            // don't overwrite changes made by before left brace annotation array
                                            // initializer setting when space within annotation parens is false
                                            if (withinParentheses || !beforeLeftBrace) {
                                                arg = arg.withElement(
                                                        spaceBefore(arg.getElement(), withinParentheses)
                                                );
                                            }
                                        } else {
                                            arg = arg.withElement(spaceBefore(arg.getElement(), style.getOther().getAfterComma()));
                                        }
                                        if (index == argsSize - 1) {
                                            arg = spaceAfter(arg, withinParentheses);
                                        }
                                        return arg;
                                    }
                            )
                    )
            );
        }
        return a;
    }

    @Override
    public J.Assignment visitAssignment(J.Assignment assignment, P p) {
        J.Assignment a = super.visitAssignment(assignment, p);
        a = a.getPadding().withAssignment(spaceBefore(a.getPadding().getAssignment(), style.getAroundOperators().getAssignment()));
        return a.getPadding().withAssignment(
                a.getPadding().getAssignment().withElement(
                        spaceBefore(a.getPadding().getAssignment().getElement(), style.getAroundOperators().getAssignment())
                )
        );
    }

    @Override
    public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, P p) {
        J.AssignmentOperation a = super.visitAssignmentOperation(assignOp, p);
        J.AssignmentOperation.Padding padding = a.getPadding();
        JLeftPadded<J.AssignmentOperation.Type> operator = padding.getOperator();
        String operatorBeforeWhitespace = operator.getBefore().getWhitespace();
        if (style.getAroundOperators().getAssignment() && StringUtils.isNullOrEmpty(operatorBeforeWhitespace)) {
            a = padding.withOperator(
                    operator.withBefore(
                            operator.getBefore().withWhitespace(" ")
                    )
            );
        } else if (!style.getAroundOperators().getAssignment() && " ".equals(operatorBeforeWhitespace)) {
            a = padding.withOperator(
                    operator.withBefore(
                            operator.getBefore().withWhitespace("")
                    )
            );
        }
        return a.withAssignment(spaceBefore(a.getAssignment(), style.getAroundOperators().getAssignment()));
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, p);
        mv = mv.withMarkers(spaceBeforeColonAfterDeclarationName(mv.getMarkers()));

        if (mv.getTypeExpression() != null) {
            mv = mv.withTypeExpression(spaceBefore(mv.getTypeExpression(), style.getOther().getAfterColonBeforeDeclarationType()));
            mv = mv.getPadding().withVariables(ListUtils.mapLast(mv.getPadding().getVariables(),
                    x -> spaceAfter(x, style.getOther().getBeforeColonAfterDeclarationName())));
        }
        return mv;
    }

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, p);
        boolean hasReceiverType = v.getMarkers().findFirst(Extension.class).isPresent();

        if (v.getPadding().getInitializer() != null && !hasReceiverType) {
            v = v.getPadding().withInitializer(spaceBefore(v.getPadding().getInitializer(), style.getAroundOperators().getAssignment()));
        }
        if (v.getPadding().getInitializer() != null) {
            v = v.getPadding().withInitializer(
                    v.getPadding().getInitializer().withElement(
                            spaceBefore(v.getPadding().getInitializer().getElement(), style.getAroundOperators().getAssignment())
                    )
            );
        }
        return v;
    }

    @Override
    public J.Binary visitBinary(J.Binary binary, P p) {
        J.Binary b = super.visitBinary(binary, p);
        J.Binary.Type operator = b.getOperator();
        boolean logicalComma = b.getMarkers().findFirst(LogicalComma.class).isPresent();
        switch (operator) {
            case And:
            case Or:
                b = applyBinarySpaceAround(
                        b,
                        !logicalComma && style.getAroundOperators().getLogical(),
                        logicalComma || style.getAroundOperators().getLogical()
                );
                break;
            case Equal:
            case NotEqual:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getEquality());
                break;
            case LessThan:
            case LessThanOrEqual:
            case GreaterThan:
            case GreaterThanOrEqual:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getRelational());
                break;
            case Addition:
            case Subtraction:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getAdditive());
                break;
            case Multiplication:
            case Division:
            case Modulo:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getMultiplicative());
                break;
            case BitAnd:
            case BitOr:
            case BitXor:
            case LeftShift:
            case RightShift:
            case UnsignedRightShift:
                // around operators bitwise and shift are defaulted to true by IntelliJ formatting;
                b = applyBinarySpaceAround(b, true);
                break;
        }
        return b;
    }

    @Override
    public K.Binary visitBinary(K.Binary binary, P p) {
        K.Binary b = super.visitBinary(binary, p);
        K.Binary.Type operator = b.getOperator();
        switch (operator) {
            case NotContains:
            case Contains:
                break;
            case IdentityEquals:
            case IdentityNotEquals:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getEquality());
                break;
            case RangeTo:
            case RangeUntil:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getRange());
                break;
        }
        return b;
    }

    private J.Binary applyBinarySpaceAround(J.Binary binary, boolean spaceAround) {
        return applyBinarySpaceAround(binary, spaceAround, spaceAround);
    }

    private J.Binary applyBinarySpaceAround(J.Binary binary, boolean spaceBefore, boolean spaceAfter) {
        J.Binary.Padding padding = binary.getPadding();
        JLeftPadded<J.Binary.Type> operator = padding.getOperator();
        binary = padding.withOperator(
                operator.withBefore(updateSpace(operator.getBefore(), spaceBefore))
        );
        return binary.withRight(spaceBefore(binary.getRight(), spaceAfter));
    }

    private K.Binary applyBinarySpaceAround(K.Binary binary, boolean useSpaceAround) {
        K.Binary.Padding padding = binary.getPadding();
        JLeftPadded<K.Binary.Type> operator = padding.getOperator();
        binary = padding.withOperator(
                operator.withBefore(updateSpace(operator.getBefore(), useSpaceAround))
        );
        return binary.withRight(spaceBefore(binary.getRight(), useSpaceAround));
    }

    @Override
    public J.Unary visitUnary(J.Unary unary, P p) {
        J.Unary u = super.visitUnary(unary, p);
        switch (u.getOperator()) {
            case PostIncrement:
            case PostDecrement:
                u = applyUnaryOperatorBeforeSpace(u, style.getAroundOperators().getUnary());
                break;
            case PreIncrement:
            case PreDecrement:
            case Negative:
            case Positive:
            case Not:
            case Complement:
                u = applyUnaryOperatorBeforeSpace(u, style.getAroundOperators().getUnary());
                u = applyUnaryOperatorExprSpace(u, style.getAroundOperators().getUnary());
                break;
        }
        return u;
    }

    private J.Unary applyUnaryOperatorExprSpace(J.Unary unary, boolean useAroundUnaryOperatorSpace) {
        if (useAroundUnaryOperatorSpace && StringUtils.isNullOrEmpty(unary.getExpression().getPrefix().getWhitespace())) {
            unary = unary.withExpression(
                    unary.getExpression().withPrefix(
                            unary.getExpression().getPrefix().withWhitespace(" ")
                    )
            );
        } else if (!useAroundUnaryOperatorSpace && " ".equals(unary.getExpression().getPrefix().getWhitespace())) {
            unary = unary.withExpression(
                    unary.getExpression().withPrefix(
                            unary.getExpression().getPrefix().withWhitespace("")
                    )
            );
        }
        return unary;
    }

    private J.Unary applyUnaryOperatorBeforeSpace(J.Unary u, boolean useAroundUnaryOperatorSpace) {
        J.Unary.Padding padding = u.getPadding();
        JLeftPadded<J.Unary.Type> operator = padding.getOperator();
        if (useAroundUnaryOperatorSpace && StringUtils.isNullOrEmpty(operator.getBefore().getWhitespace())) {
            u = padding.withOperator(
                    operator.withBefore(
                            operator.getBefore().withWhitespace(" ")
                    )
            );
        } else if (!useAroundUnaryOperatorSpace && " ".equals(operator.getBefore().getWhitespace())) {
            u = padding.withOperator(
                    operator.withBefore(
                            operator.getBefore().withWhitespace("")
                    )
            );
        }
        return u;
    }

    @Override
    public K.FunctionType visitFunctionType(K.FunctionType functionType, P p) {
        K.FunctionType kf = super.visitFunctionType(functionType, p);

        // handle space around arrow in function type
        if (kf.getArrow() != null) {
            kf = kf.withArrow(updateSpace(kf.getArrow(), style.getOther().getAroundArrowInFunctionTypes()));
        }

        JRightPadded<TypedTree> rpTypedTree = kf.getReturnType();
        return kf.withReturnType(rpTypedTree.withElement(spaceBefore(rpTypedTree.getElement(), style.getOther().getAroundArrowInFunctionTypes())));
    }

    @Override
    public K.FunctionType.Parameter visitFunctionTypeParameter(K.FunctionType.Parameter parameter, P p) {
        K.FunctionType.Parameter pa = super.visitFunctionTypeParameter(parameter, p);
        // handle space around colon
        if (pa.getMarkers().findFirst(TypeReferencePrefix.class).orElse(null) != null) {
            pa = pa.withMarkers(spaceBeforeColonAfterDeclarationName(pa.getMarkers()));
            pa = pa.withParameterType(spaceBefore(pa.getParameterType(), style.getOther().getAfterColonBeforeDeclarationType()));
        }
        return pa;
    }

    @Override
    public J.Lambda visitLambda(J.Lambda lambda, P p) {
        J.Lambda l = super.visitLambda(lambda, p);
        boolean isFunctionType = requireNonNull(getCursor().getParent()).getValue() instanceof K.FunctionType;
        if (isFunctionType) {
            return lambda;
        }

        // handle space before Lambda arrow
        boolean useSpaceBeforeLambdaArrow = style.getOther().getBeforeLambdaArrow();
        boolean lastParamHasSpace = false;
        boolean trailingComma = false;
        List<JRightPadded<J>> parameters = l.getParameters().getPadding().getParams();
        boolean hasArrow = !parameters.isEmpty();
        if (hasArrow) {
            JRightPadded<J> lastParam = parameters.get(parameters.size() - 1);
            Space after = lastParam.getAfter();
            trailingComma = lastParam.getMarkers().findFirst(TrailingComma.class).isPresent();
            lastParamHasSpace = after.getComments().isEmpty() && onlySpacesAndNotEmpty(after.getWhitespace()) ||
                    lastParam.getMarkers().findFirst(TrailingComma.class).map(t -> onlySpacesAndNotEmpty(t.getSuffix().getWhitespace())).orElse(false);
            useSpaceBeforeLambdaArrow &= !trailingComma;
        } else {
            l = l.withArrow(Space.EMPTY);
            l = l.withParameters(l.getParameters().withPrefix(Space.EMPTY));
        }

        if (lastParamHasSpace) {
            boolean useSpace = useSpaceBeforeLambdaArrow;
            parameters = ListUtils.mapLast(parameters, rp -> spaceAfter(rp, useSpace));
            l = l.withParameters(l.getParameters().getPadding().withParams(parameters));
        } else if (hasArrow) {
            l = l.withArrow(updateSpace(l.getArrow(), useSpaceBeforeLambdaArrow));
        }

        // handle space after Lambda arrow
        // Intellij has a specific setting for Space before Lambda arrow, but no setting for space after Lambda arrow
        // presumably handled as around the Lambda arrow for the same
        l = l.withBody(spaceBefore(l.getBody(), false));

        if (l.getBody() instanceof J.Block ) {
            J.Block body = (J.Block) l.getBody();
            boolean t = useSpaceBeforeLambdaArrow;
            body = body.withStatements(ListUtils.mapFirst(body.getStatements(), s -> spaceBefore(s, t)));
            l = l.withBody(body);
        }

        if (!(l.getParameters().getParameters().isEmpty() || l.getParameters().getParameters().get(0) instanceof J.Empty)) {
            int parametersSize = l.getParameters().getParameters().size();
            l = l.withParameters(
                    l.getParameters().getPadding().withParams(
                            ListUtils.map(l.getParameters().getPadding().getParams(),
                                    (index, elemContainer) -> {
                                        if (index != 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(), style.getOther().getAfterComma())
                                            );
                                        }
                                        if (index != parametersSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, style.getOther().getBeforeComma());
                                        }
                                        return elemContainer;
                                    }
                            )
                    )
            );
        }

        // handle spaces in simple one line methods
        boolean omitBraces = l.getMarkers().findFirst(OmitBraces.class).isPresent();
        if (!omitBraces) {
            PrintOutputCapture<Integer> print = new PrintOutputCapture<>(0);
            new KotlinPrinter<Integer>().visitLambda(l, print);
            boolean singleLine = !print.out.toString().contains("\n");

            if (singleLine && !l.getParameters().getParameters().isEmpty()) {
                // handle leading spaces
                l = l.withParameters(spaceBefore(l.getParameters(), style.getOther().getInSimpleOneLineMethods()));

                // handle trailing spaces
                if (l.getBody() instanceof J.Block) {
                    J.Block block = (J.Block) l.getBody();
                    block = block.withEnd(updateSpace(block.getEnd(), style.getOther().getInSimpleOneLineMethods()));
                    l = l.withBody(block);
                }
            }
        }

        return l;
    }

    @Override
    public J.MemberReference visitMemberReference(J.MemberReference memberRef, P p) {
        J.MemberReference m = super.visitMemberReference(memberRef, p);
        // aroundOperatorsAfterMethodReferenceDoubleColon is defaulted to `false` in IntelliJ's Kotlin formatting.
        m = m.getPadding().withContaining(
                spaceAfter(m.getPadding().getContaining(), false)
        );

        // aroundOperatorsBeforeMethodReferenceDoubleColon is defaulted to `false` in IntelliJ's Kotlin formatting.
        if (m.getPadding().getTypeParameters() != null) {
            m = m.getPadding().withTypeParameters(spaceBefore(m.getPadding().getTypeParameters(), false, true));
        } else {
            m = m.getPadding().withReference(
                    spaceBefore(m.getPadding().getReference(), false)
            );
        }
        return m;
    }

    @Override
    public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, P p) {
        J.ArrayAccess a = super.visitArrayAccess(arrayAccess, p);
        if (" ".equals(a.getDimension().getPadding().getIndex().getElement().getPrefix().getWhitespace())) {
            a = a.withDimension(
                    a.getDimension().getPadding().withIndex(
                            a.getDimension().getPadding().getIndex().withElement(
                                    a.getDimension().getPadding().getIndex().getElement().withPrefix(
                                            a.getDimension().getPadding().getIndex().getElement().getPrefix().withWhitespace("")
                                    )
                            )
                    )
            );
        }
        if (" ".equals(a.getDimension().getPadding().getIndex().getAfter().getWhitespace())) {
            a = a.withDimension(
                    a.getDimension().getPadding().withIndex(
                            a.getDimension().getPadding().getIndex().withAfter(
                                    a.getDimension().getPadding().getIndex().getAfter().withWhitespace("")
                            )
                    )
            );
        }
        return a;
    }

    @Override
    public <T extends J> J.Parentheses<T> visitParentheses(J.Parentheses<T> parens, P p) {
        J.Parentheses<T> p2 = super.visitParentheses(parens, p);

        if (" ".equals(p2.getPadding().getTree().getElement().getPrefix().getWhitespace())) {
            p2 = p2.getPadding().withTree(
                    p2.getPadding().getTree().withElement(
                            p2.getPadding().getTree().getElement().withPrefix(
                                    p2.getPadding().getTree().getElement().getPrefix().withWhitespace("")
                            )
                    )
            );
        }
        if (" ".equals(p2.getPadding().getTree().getAfter().getWhitespace())) {
            p2 = p2.getPadding().withTree(
                    p2.getPadding().getTree().withAfter(
                            p2.getPadding().getTree().getAfter().withWhitespace("")
                    )
            );
        }
        return p2;
    }

    @SuppressWarnings("ConstantValue")
    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, P p) {
        J.NewClass nc = super.visitNewClass(newClass, p);
        if (nc.getPadding().getArguments() != null) {
            nc = nc.getPadding().withArguments(spaceBefore(nc.getPadding().getArguments(), false, true));
            int argsSize = nc.getPadding().getArguments().getElements().size();
            nc = nc.getPadding().withArguments(
                    nc.getPadding().getArguments().getPadding().withElements(
                            ListUtils.map(nc.getPadding().getArguments().getPadding().getElements(),
                                    (index, elemContainer) -> {
                                        if (index != 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(), style.getOther().getAfterComma())
                                            );
                                        }
                                        if (index != argsSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, style.getOther().getBeforeComma());
                                        }
                                        return elemContainer;
                                    }
                            )
                    )
            );
        }
        return nc;
    }

    @SuppressWarnings("ConstantValue")
    @Override
    public J.EnumValue visitEnumValue(J.EnumValue _enum, P p) {
        J.EnumValue e = super.visitEnumValue(_enum, p);
        if (e.getInitializer() != null && e.getInitializer().getPadding().getArguments() != null) {
            int initializerArgumentsSize = e.getInitializer().getPadding().getArguments().getPadding().getElements().size();
            e = e.withInitializer(
                    e.getInitializer().getPadding().withArguments(
                            e.getInitializer().getPadding().getArguments().getPadding().withElements(
                                    ListUtils.map(e.getInitializer().getPadding().getArguments().getPadding().getElements(),
                                            (index, elemContainer) -> {
                                                if (index != 0) {
                                                    elemContainer = elemContainer.withElement(
                                                            spaceBefore(elemContainer.getElement(),
                                                                    style.getOther().getAfterComma())
                                                    );
                                                }
                                                if (index != initializerArgumentsSize - 1) {
                                                    elemContainer = spaceAfter(elemContainer,
                                                            style.getOther().getBeforeComma());
                                                }
                                                return elemContainer;
                                            }
                                    )
                            )
                    )
            );
        }
        return e;
    }

    //    @Override
//    public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam, P p) {
//        J.TypeParameter tp = super.visitTypeParameter(typeParam, p);
//        if (tp.getPadding().getBounds() != null) {
//            boolean spaceAroundTypeBounds = style.getTypeParameters().getAroundTypeBounds();
//            int typeBoundsSize = tp.getPadding().getBounds().getPadding().getElements().size();
//            tp = tp.getPadding().withBounds(
//                    tp.getPadding().getBounds().getPadding().withElements(
//                            ListUtils.map(tp.getPadding().getBounds().getPadding().getElements(),
//                                    (index, elemContainer) -> {
//                                        if (index != 0) {
//                                            elemContainer = elemContainer.withElement(spaceBefore(elemContainer.getElement(), spaceAroundTypeBounds));
//                                        }
//                                        if (index != typeBoundsSize - 1) {
//                                            elemContainer = spaceAfter(elemContainer, spaceAroundTypeBounds);
//                                        }
//                                        return elemContainer;
//                                    }
//                            )
//                    )
//            );
//        }
//        return tp;
//    }

    @Override
    public @Nullable J postVisit(J tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().getRoot().putMessage("stop", true);
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
}
