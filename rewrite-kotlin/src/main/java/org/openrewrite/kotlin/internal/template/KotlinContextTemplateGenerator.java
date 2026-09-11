/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.kotlin.internal.template;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinVisitor;
import org.openrewrite.kotlin.marker.By;
import org.openrewrite.kotlin.marker.OmitEquals;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

/**
 * Builds the stub for a context-sensitive Kotlin template by <em>printing the whole source file</em> with the
 * template substituted at the insertion point, rather than reconstructing the enclosing scope bottom-up the way
 * {@link org.openrewrite.java.internal.template.BlockStatementTemplateGenerator} does for Java.
 * <p>
 * The printer already round-trips every J/K node, so syntactic correctness is free and there is no per-construct
 * reconstruction to get wrong. What remains is deciding what may be <em>elided</em>, and elision is not needed for
 * legality — the file is already legal. It exists to shrink the set of types that must resolve against the
 * template parser's classpath. So the rule is to elide only where provably safe.
 * <p>
 * A consequence worth stating: because statements preceding the insertion point are kept verbatim rather than
 * re-synthesized with dummy initializers, smart casts, {@code when} subject bindings and destructuring
 * declarations all remain live at the hole. Java's approach destroys these.
 */
public class KotlinContextTemplateGenerator extends KotlinVisitor<Integer> {

    /**
     * {@code kotlin.TODO()} returns {@code Nothing}, which is a subtype of every type including non-null ones,
     * so it satisfies any declared return type or property type. kotlin-stdlib is unconditionally on every
     * Kotlin parse's classpath (see {@code KotlinParser.buildModule}), so this always resolves.
     */
    private static final String ELIDED = "kotlin.TODO()";

    private final Set<UUID> spine;
    private final J insertionPoint;
    private final String hole;
    private final boolean truncateAfterHole;

    private KotlinContextTemplateGenerator(Set<UUID> spine, J insertionPoint, String hole, boolean truncateAfterHole) {
        this.spine = spine;
        this.insertionPoint = insertionPoint;
        this.hole = hole;
        this.truncateAfterHole = truncateAfterHole;
    }

    /**
     * @param cursor            positioned at the insertion point.
     * @param hole              the substituted template, already wrapped in the template marker comments.
     * @param truncateAfterHole drop statements following the insertion point in its enclosing block. Only safe
     *                          when the insertion point is being replaced; for before/after insertion the
     *                          original statement still exists and later statements may reference it.
     */
    public static String stub(Cursor cursor, String hole, boolean truncateAfterHole) {
        J insertionPoint = cursor.getValue();
        Set<UUID> spine = new HashSet<>();
        for (Object o : (Iterable<Object>) () -> cursor.getPath(J.class::isInstance)) {
            spine.add(((J) o).getId());
        }
        K.CompilationUnit cu = cursor.firstEnclosingOrThrow(K.CompilationUnit.class);
        J elided = new KotlinContextTemplateGenerator(spine, insertionPoint, hole, truncateAfterHole)
                .visitNonNull(cu, 0);
        return elided.print(cursor.getRoot());
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, Integer p) {
        if (tree instanceof J && ((J) tree).getId().equals(insertionPoint.getId())) {
            return unknown(hole, ((J) tree).getPrefix());
        }
        return super.visit(tree, p);
    }

    // R3/R4: statements before the hole are kept verbatim; statements after it are dropped when the hole
    // replaces its statement. Kotlin has no forward references for local declarations, so nothing after the
    // hole can be referenced from it.
    @Override
    public J visitBlock(J.Block block, Integer p) {
        if (truncateAfterHole && onSpine(block)) {
            int holeIndex = indexOfSpineChild(block.getStatements());
            if (holeIndex >= 0) {
                block = block.withStatements(block.getStatements().subList(0, holeIndex + 1));
            }
        }
        return super.visitBlock(block, p);
    }

    @Override
    public J visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
        if (!onSpine(method) && method.getBody() != null && isElidableBody(method)) {
            return method.withBody(elidedBody(method.getBody()));
        }
        return super.visitMethodDeclaration(method, p);
    }

    @Override
    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, Integer p) {
        if (!onSpine(multiVariable) && isElidableProperty(multiVariable)) {
            return multiVariable.withVariables(ListUtils.map(multiVariable.getVariables(), v ->
                    v.getInitializer() == null ? v : v.withInitializer(unknown(ELIDED, Space.SINGLE_SPACE))));
        }
        return super.visitVariableDeclarations(multiVariable, p);
    }

    /**
     * R6: a body may only be elided when its type is written in the source. Eliding
     * {@code fun f() = expr} to {@code = kotlin.TODO()} would infer {@code Nothing}, which does not fail
     * loudly — it silently mis-attributes the type of every reference to {@code f}, defeating the very
     * capability context-sensitivity exists to provide.
     */
    private boolean isElidableBody(J.MethodDeclaration method) {
        J.Block body = method.getBody();
        if (body == null || isConstructor(method)) {
            return false;
        }
        boolean expressionBodied = body.getMarkers().findFirst(org.openrewrite.kotlin.marker.SingleExpressionBlock.class).isPresent();
        return !expressionBodied || method.getReturnTypeExpression() != null;
    }

    /**
     * R6 for properties: the type must be written, the property must not be {@code const} (which requires a
     * compile-time constant), and a {@code by} delegate must be left alone because eliding it would resolve
     * {@code getValue} against {@code Nothing}.
     */
    private boolean isElidableProperty(J.VariableDeclarations multiVariable) {
        if (multiVariable.getTypeExpression() == null) {
            return false;
        }
        for (J.Modifier modifier : multiVariable.getModifiers()) {
            if ("const".equals(modifier.getKeyword())) {
                return false;
            }
        }
        // `by` and the equals-less form live on the declaration itself, and the printer keys the separator off
        // them (see KotlinPrinter#getEqualsText). Eliding either would emit `val x: T by kotlin.TODO()`, which
        // resolves `getValue` against `Nothing`, or `val x: T kotlin.TODO()`, which is not syntax at all.
        return !multiVariable.getMarkers().findFirst(By.class).isPresent() &&
               !multiVariable.getMarkers().findFirst(OmitEquals.class).isPresent();
    }

    /**
     * A constructor body is never elided: a class with {@code val} properties assigned there relies on those
     * assignments for definite assignment, which {@code TODO()} does not satisfy. An {@code init} block is a
     * {@link J.Block}, not a declaration, so it is left alone for free.
     */
    private boolean isConstructor(J.MethodDeclaration method) {
        return method.getMethodType() != null && method.getMethodType().isConstructor() ||
               method.getReturnTypeExpression() == null && method.getName().getSimpleName().isEmpty();
    }

    private J.Block elidedBody(J.Block body) {
        // An expression body keeps its `= ` form, so the markers stay; a block body keeps its braces.
        return body.withStatements(singletonList(unknown(ELIDED, Space.SINGLE_SPACE)));
    }

    private boolean onSpine(J j) {
        return spine.contains(j.getId());
    }

    private int indexOfSpineChild(List<Statement> statements) {
        for (int i = 0; i < statements.size(); i++) {
            if (onSpine(statements.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * {@code J.Unknown} prints its source text verbatim through {@code JavaPrinter.visitUnknown}, which
     * {@code KotlinPrinter} inherits. That makes it the substitution vehicle for both the hole and elided
     * bodies without touching the printer at all.
     */
    private static J.Unknown unknown(String text, Space prefix) {
        return new J.Unknown(randomId(), prefix, Markers.EMPTY,
                new J.Unknown.Source(randomId(), Space.EMPTY, Markers.EMPTY, text));
    }
}
