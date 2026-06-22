/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.scala.marker.IndentedSyntax;
import org.openrewrite.scala.marker.OmitBraces;
import org.openrewrite.scala.marker.Semicolon;
import org.openrewrite.scala.marker.TypeAscriptionColonPrefix;
import org.openrewrite.scala.tree.S;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Builds the LST node for a Scala {@code try}/{@code catch}/{@code finally}, deciding between
 * the Java model ({@link J.Try}) and the Scala-specific {@link S.Try}.
 * <p>
 * OpenRewrite favours fitting Scala into {@code J.*} types so Java recipes apply: the common
 * {@code [name][: Type]} (guard-free) catch maps cleanly onto {@link J.Try.Catch}
 * ({@code catch (Type name)}), so a {@link J.Try} is emitted whenever every clause fits. Only
 * Scala-specific patterns (extractors, alternatives, at-bindings) or guards — which
 * {@code J.Try.Catch}'s fixed {@code ControlParentheses<VariableDeclarations>} cannot hold —
 * fall back to {@link S.Try} (which models the handler as a {@code J.Block} of {@code J.Case}).
 * <p>
 * This logic lives in Java rather than {@code ScalaTreeVisitor.scala} because it reads
 * Lombok-generated getters on same-module {@code S.*} types, which are invisible to the Scala
 * joint compiler (it sees {@code S.java} as source, before Lombok runs).
 */
public final class ScalaTryBuilder {

    private ScalaTryBuilder() {
    }

    /**
     * @param prefix    space before the {@code try} keyword
     * @param body      the try body
     * @param catches   the catch handler as a {@code J.Block} of {@code J.Case}; before-space is
     *                  the space before {@code catch}, an {@code OmitBraces} marker means Scala 3's
     *                  brace-less {@code catch case} form. {@code null} when there is no catch.
     * @param finalizer the finalizer block; before-space is the space before {@code finally}
     */
    public static J buildTry(Space prefix, J.Block body, @Nullable JLeftPadded<J.Block> catches,
                             @Nullable JLeftPadded<J.Block> finalizer) {
        if (catches == null || catchesFitJModel(catches.getElement())) {
            return buildJTry(prefix, body, catches, finalizer);
        }
        boolean braceless = catches.getElement().getMarkers().findFirst(OmitBraces.class).isPresent();
        Markers tryMarkers = braceless ?
                Markers.build(Collections.singletonList(new IndentedSyntax(Tree.randomId()))) :
                Markers.EMPTY;
        return S.Try.build(Tree.randomId(), prefix, tryMarkers, body, catches, finalizer, null);
    }

    /** True when every clause is a guard-free {@code [name][: Type]} catch, expressible as a {@code J.Try.Catch}. */
    private static boolean catchesFitJModel(J.Block casesBlock) {
        for (JRightPadded<Statement> rp : casesBlock.getPadding().getStatements()) {
            if (rp.getMarkers().findFirst(Semicolon.class).isPresent()) {
                return false;
            }
            if (!(rp.getElement() instanceof J.Case)) {
                return false;
            }
            J.Case c = (J.Case) rp.getElement();
            if (c.getGuard() != null || c.getPadding().getCaseLabels().getElements().size() != 1) {
                return false;
            }
            J label = c.getPadding().getCaseLabels().getElements().get(0);
            if (label instanceof S.TypeAscription) {
                S.TypeAscription ta = (S.TypeAscription) label;
                boolean nameOk = ta.getExpression() instanceof J.Identifier || ta.getExpression() instanceof S.Wildcard;
                boolean typeOk = ta.getTypeTree() instanceof J.Identifier || ta.getTypeTree() instanceof J.FieldAccess;
                if (!nameOk || !typeOk) {
                    return false;
                }
            } else if (!(label instanceof J.Identifier) && !(label instanceof S.Wildcard)) {
                return false;
            }
        }
        return true;
    }

    /** Convert the {@code J.Block} of {@code J.Case} catch handler into {@code J.Try}'s list of {@code J.Try.Catch}. */
    private static J.Try buildJTry(Space prefix, J.Block body, @Nullable JLeftPadded<J.Block> catches,
                                   @Nullable JLeftPadded<J.Block> finalizer) {
        List<J.Try.Catch> catchList = new ArrayList<>();
        boolean braceless = false;
        if (catches != null) {
            J.Block casesBlock = catches.getElement();
            braceless = casesBlock.getMarkers().findFirst(OmitBraces.class).isPresent();
            Space beforeCatch = catches.getBefore();
            Space blockPrefix = casesBlock.getPrefix();
            List<JRightPadded<Statement>> rps = casesBlock.getPadding().getStatements();
            for (int i = 0; i < rps.size(); i++) {
                J.Case c = (J.Case) rps.get(i).getElement();
                JRightPadded<J> labelRp = c.getPadding().getCaseLabels().getPadding().getElements().get(0);
                J label = labelRp.getElement();

                J.Identifier nameId;
                TypeTree typeTree;
                Space beforeColon;
                if (label instanceof S.TypeAscription) {
                    S.TypeAscription ta = (S.TypeAscription) label;
                    // The space after `case` is on the ascription's own prefix, not the inner expr.
                    nameId = ta.getExpression() instanceof J.Identifier ?
                            ((J.Identifier) ta.getExpression()).withPrefix(ta.getPrefix()) :
                            ident("_", ta.getPrefix());
                    Optional<TypeAscriptionColonPrefix> bc = ta.getMarkers().findFirst(TypeAscriptionColonPrefix.class);
                    beforeColon = bc.map(TypeAscriptionColonPrefix::getPrefix).orElse(null);
                    typeTree = ta.getTypeTree();
                } else if (label instanceof S.Wildcard) {
                    nameId = ident("_", ((S.Wildcard) label).getPrefix());
                    typeTree = null;
                    beforeColon = null;
                } else {
                    nameId = (J.Identifier) label;
                    typeTree = null;
                    beforeColon = null;
                }

                J.Block caseBody = wrapCatchBody(c.getPadding().getBody());
                J.VariableDeclarations.NamedVariable namedVar = new J.VariableDeclarations.NamedVariable(
                        Tree.randomId(), Space.EMPTY, Markers.EMPTY, nameId, Collections.emptyList(), null, null);
                J.VariableDeclarations varDecl = new J.VariableDeclarations(Tree.randomId(), c.getPrefix(), Markers.EMPTY,
                        Collections.emptyList(), Collections.emptyList(), typeTree, beforeColon,
                        Collections.emptyList(), Collections.singletonList(JRightPadded.build(namedVar)));
                // For the first catch the J.Try printer emits the catch keyword then the brace, so its
                // parameter prefix carries the space before `{`; subsequent catches don't reprint it.
                Space controlPrefix = i == 0 ? blockPrefix : Space.EMPTY;
                J.ControlParentheses<J.VariableDeclarations> controlParens = new J.ControlParentheses<>(
                        Tree.randomId(), controlPrefix, Markers.EMPTY,
                        JRightPadded.build(varDecl).withAfter(labelRp.getAfter()));
                Space catchPrefix = i == 0 ? beforeCatch : c.getPrefix();
                catchList.add(new J.Try.Catch(Tree.randomId(), catchPrefix, Markers.EMPTY, controlParens, caseBody));
            }
            // The space before the closing `}` lives on the cases-block end; in J.Try it belongs to
            // the last catch body's end space.
            if (!braceless && !catchList.isEmpty()) {
                J.Try.Catch last = catchList.get(catchList.size() - 1);
                catchList.set(catchList.size() - 1, last.withBody(last.getBody().withEnd(casesBlock.getEnd())));
            }
        }
        Markers tryMarkers = braceless ?
                Markers.build(Collections.singletonList(new IndentedSyntax(Tree.randomId()))) :
                Markers.EMPTY;
        return new J.Try(Tree.randomId(), prefix, tryMarkers, null, body, catchList, finalizer);
    }

    /** Wrap a catch-clause body (a bare {@code J} on {@code J.Case}) in an OmitBraces {@code J.Block} as {@code J.Try.Catch} requires. */
    private static J.Block wrapCatchBody(@Nullable JRightPadded<J> bodyRp) {
        Markers omit = Markers.build(Collections.singletonList(new OmitBraces(Tree.randomId())));
        if (bodyRp == null) {
            return emptyBlock(omit);
        }
        J body = bodyRp.getElement();
        if (body instanceof J.Block && ((J.Block) body).getMarkers().findFirst(OmitBraces.class).isPresent()) {
            return (J.Block) body;
        } else if (body instanceof J.Block) {
            return shell(omit, (J.Block) body);
        } else if (body instanceof Statement) {
            return shell(omit, (Statement) body);
        } else if (body instanceof Expression) {
            return shell(omit, new S.ExpressionStatement(Tree.randomId(), (Expression) body));
        }
        return emptyBlock(omit);
    }

    private static J.Block shell(Markers omit, Statement stmt) {
        List<JRightPadded<Statement>> s = new ArrayList<>();
        s.add(JRightPadded.build(stmt));
        return new J.Block(Tree.randomId(), Space.EMPTY, omit, JRightPadded.build(false), s, Space.EMPTY);
    }

    private static J.Block emptyBlock(Markers omit) {
        return new J.Block(Tree.randomId(), Space.EMPTY, omit, JRightPadded.build(false), new ArrayList<>(), Space.EMPTY);
    }

    private static J.Identifier ident(String name, Space prefix) {
        return new J.Identifier(Tree.randomId(), prefix, Markers.EMPTY, Collections.emptyList(), name, null, null);
    }
}
