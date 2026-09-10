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
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.internal.template.JavaTemplateJavaExtension;
import org.openrewrite.java.internal.template.JavaTemplateParser;
import org.openrewrite.java.internal.template.Substitutions;
import org.openrewrite.java.marker.OmitBraces;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.marker.Extension;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.kotlin.marker.SingleExpressionBlock;
import org.openrewrite.marker.Markers;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import static org.openrewrite.java.tree.Space.Location.EXTENDS;
import static org.openrewrite.java.tree.Space.Location.IMPLEMENTS;

/**
 * Kotlin tree surgery for template application.
 * <p>
 * Inherits everything from the Java extension and overrides only the constructs where Kotlin's LST genuinely
 * differs. Selected by the <em>target source file</em> via
 * {@link org.openrewrite.kotlin.service.KotlinTemplateService}, so it applies even when the snippet itself was
 * written as Java and applied through a plain {@code JavaTemplate}.
 */
public class KotlinTemplateJavaExtension extends JavaTemplateJavaExtension {

    public KotlinTemplateJavaExtension(JavaTemplateParser templateParser, Substitutions substitutions,
                                       String substitutedTemplate, JavaCoordinates coordinates, boolean autoFormat) {
        super(templateParser, substitutions, substitutedTemplate, coordinates, autoFormat);
    }

    @Override
    public TreeVisitor<? extends J, Integer> getMixin() {
        return new KotlinMixin();
    }

    protected class KotlinMixin extends Mixin {

        /**
         * Kotlin writes every supertype — superclass and interfaces alike — into the implements container,
         * rendered after a single {@code :}. The Java implementation splits them across {@code extends} and
         * {@code implements}, and its {@code EXTENDS} branch writes a slot the Kotlin printer never reads,
         * which would silently produce no change.
         */
        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
            if (isScope(classDecl) && (loc == EXTENDS || loc == IMPLEMENTS)) {
                List<TypeTree> gen = loc == EXTENDS ?
                        singletonList(unsubstitute(templateParser.parseExtends(getCursor(), substitutedTemplate))) :
                        unsubstitute(templateParser.parseImplements(getCursor(), substitutedTemplate));

                List<TypeTree> existing = classDecl.getImplements();
                List<TypeTree> supertypes;
                if (loc == EXTENDS && existing != null && !existing.isEmpty()) {
                    // `extends` addresses Kotlin's superclass position, which is the first supertype listed.
                    supertypes = ListUtils.concatAll(gen, existing.subList(1, existing.size()));
                } else if (mode == JavaCoordinates.Mode.REPLACEMENT || existing == null) {
                    supertypes = gen;
                } else {
                    supertypes = ListUtils.concatAll(existing, gen);
                }

                J.ClassDeclaration c = classDecl.withImplements(supertypes);
                // The space in the container's `before` precedes the `:` that the Kotlin printer emits, so
                // unlike Java it must be a single space rather than empty.
                //noinspection ConstantConditions
                c = c.getPadding().withImplements(c.getPadding().getImplements().withBefore(Space.format(" ")));
                return c;
            }
            return super.visitClassDeclaration(classDecl, p);
        }

        /**
         * An expression body (`fun f() = expr`) is modelled as a synthetic block carrying
         * {@link SingleExpressionBlock} and {@code OmitBraces}. Replacing its statements wholesale leaves those
         * markers behind, which would render the new statements after an `=` and without braces — invalid
         * Kotlin as soon as the template contains anything but a bare expression.
         */
        @Override
        public J visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
            if (isScope(method) && loc == Space.Location.METHOD_DECLARATION_PARAMETERS) {
                // An extension function's receiver is modelled as a synthetic first parameter, so replacing
                // the parameter list wholesale would consume the receiver slot and rewrite `fun String.foo()`
                // into `fun n.foo()`. Only the declared parameters are the template's to replace.
                List<Statement> original = method.getParameters();
                boolean hasReceiver = !original.isEmpty() &&
                                      original.get(0).getMarkers().findFirst(Extension.class).isPresent();
                J result = super.visitMethodDeclaration(method, p);
                if (hasReceiver && result instanceof J.MethodDeclaration) {
                    J.MethodDeclaration m = (J.MethodDeclaration) result;
                    return m.withParameters(ListUtils.concat(original.get(0), m.getParameters()));
                }
                return result;
            }
            if (isScope(method) && loc == Space.Location.BLOCK_PREFIX) {
                J result = super.visitMethodDeclaration(method, p);
                if (result instanceof J.MethodDeclaration) {
                    J.MethodDeclaration m = (J.MethodDeclaration) result;
                    J.Block body = m.getBody();
                    if (body != null) {
                        Markers markers = body.getMarkers()
                                .removeByType(SingleExpressionBlock.class)
                                .removeByType(OmitBraces.class)
                                .removeByType(org.openrewrite.kotlin.marker.OmitBraces.class);
                        if (markers != body.getMarkers()) {
                            // A brace-less block has no end whitespace; the closing brace now needs its own line.
                            if (body.getEnd().getWhitespace().isEmpty()) {
                                body = body.withEnd(Space.format("\n" + method.getPrefix().getIndent()));
                            }
                            m = m.withBody(body.withMarkers(markers));
                        }
                    }
                    return m;
                }
                return result;
            }
            return super.visitMethodDeclaration(method, p);
        }

        /**
         * Kotlin holds top-level declarations directly on the compilation unit, never in a
         * {@code J.Block}. The inherited mixin performs statement insertion only in {@code visitBlock}, so a
         * top-level insert would otherwise fall through to {@code maybeReplaceStatement} and be rejected for
         * any mode but replacement.
         */
        @Override
        public @Nullable J visit(@Nullable Tree tree, Integer p) {
            // The mixin is a JavaVisitor, so K.CompilationUnit cannot be reached by overriding a visit method
            // — it is dispatched through KotlinVisitor after adaptation. Intercept it here instead.
            if (tree instanceof K.CompilationUnit) {
                // autoFormat resolves the enclosing source file from getCursor(), not from the parent it is
                // handed, and this runs before the cursor has descended into the compilation unit.
                Cursor restore = getCursor();
                setCursor(new Cursor(restore, tree));
                try {
                    J handled = insertTopLevel((K.CompilationUnit) tree, p);
                    if (handled != null) {
                        return handled;
                    }
                } finally {
                    setCursor(restore);
                }
            }
            return super.visit(tree, p);
        }

        private @Nullable J insertTopLevel(K.CompilationUnit cu, Integer p) {
            Cursor parent = getCursor();
            if (loc == Space.Location.BLOCK_END && isScope(cu)) {
                List<Statement> gen = generatedStatements(parent);
                return cu.withStatements(ListUtils.concatAll(cu.getStatements(),
                        ListUtils.map(gen, (i, s) -> autoFormat(s, p, parent))));
            }
            if (loc == Space.Location.STATEMENT_PREFIX) {
                // A Kotlin script keeps its statements in a block, so the insertion point is not among the
                // file's own. Returning here regardless would both skip the shared source-file handling and
                // stop the traversal before it reached the block that does hold the anchor.
                if (cu.getStatements().stream().noneMatch(s -> s.isScope(insertionPoint))) {
                    return null;
                }
                return cu.withStatements(ListUtils.flatMap(cu.getStatements(), statement -> {
                    if (!isScope(statement)) {
                        return statement;
                    }
                    List<Statement> gen = generatedStatements(parent);
                    boolean inheritsPrefix = mode != JavaCoordinates.Mode.AFTER;
                    for (int i = 0; i < gen.size(); i++) {
                        Statement s = gen.get(i);
                        if (i == 0 && inheritsPrefix) {
                            s = s.withPrefix(statement.getPrefix().withComments(emptyList()));
                        } else if (!s.getPrefix().getWhitespace().contains("\n")) {
                            s = s.withPrefix(Space.format("\n"));
                        }
                        gen.set(i, autoFormat(s, p, parent));
                    }
                    switch (mode) {
                        case BEFORE:
                            // The displaced declaration now follows the insertion and needs its own line.
                            return ListUtils.concat(gen, onOwnLine(statement));
                        case AFTER:
                            return ListUtils.concat(statement, gen);
                        default:
                            return gen;
                    }
                }));
            }
            return null;
        }

        /**
         * A file's first top-level declaration has an empty prefix, unlike a statement in a block which
         * inherits a newline from its siblings. Without this, an insertion runs on from the declaration
         * before it.
         */
        private Statement onOwnLine(Statement statement) {
            return statement.getPrefix().getWhitespace().contains("\n") ? statement :
                    statement.withPrefix(Space.format("\n"));
        }

        private List<Statement> generatedStatements(Cursor parent) {
            return unsubstitute(templateParser.parseBlockStatements(
                    new Cursor(parent, insertionPoint), Statement.class, substitutedTemplate,
                    substitutions.getTypeVariables(), loc, mode));
        }

        /**
         * Kotlin models an expression used as a statement as {@link K.ExpressionStatement}. Splicing a
         * bare expression into a statement slot yields a tree the printer cannot handle — it casts block
         * statements to {@link Statement} and throws. Wrap whatever the template produced.
         */
        @Override
        public J visitExpression(Expression expression, Integer p) {
            J result = super.visitExpression(expression, p);
            return asStatementIfNeeded(result, expression);
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, Integer p) {
            J result = super.visitMethodInvocation(method, p);
            return asStatementIfNeeded(result, method);
        }

        private J asStatementIfNeeded(J result, J original) {
            if (loc == Space.Location.STATEMENT_PREFIX && result != original &&
                original instanceof Statement && result instanceof Expression && !(result instanceof Statement)) {
                return new K.ExpressionStatement(Tree.randomId(), (Expression) result);
            }
            return result;
        }
    }
}
