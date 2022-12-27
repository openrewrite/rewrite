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
package org.openrewrite.java.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.FallThroughStyle;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = true)
public class FallThroughVisitor<P> extends JavaIsoVisitor<P> {
    /**
     * Ignores any fall-through commented with a text matching the regex pattern.
     * This is currently non-user-configurable, though held within {@link FallThroughStyle}.
     */
    private static final Pattern RELIEF_PATTERN = Pattern.compile("falls?[ -]?thr(u|ough)");


    FallThroughStyle style;

    private static boolean isLastCase(J.Case caze, J.Switch switzh) {
        J.Block switchBlock = switzh.getCases();
        return caze == switchBlock.getStatements().get(switchBlock.getStatements().size() - 1);
    }

    @Override
    public J.Case visitCase(J.Case caze, P p) {
        J.Case c = super.visitCase(caze, p);
        if (getCursor().firstEnclosing(J.Switch.class) != null) {
            J.Switch switzh = getCursor().dropParentUntil(J.Switch.class::isInstance).getValue();
            if ((Boolean.TRUE.equals(style.getCheckLastCaseGroup()) || !isLastCase(c, switzh))) {
                if (FindLastLineBreaksOrFallsThroughComments.find(switzh, c).isEmpty()) {
                    doAfterVisit(new AddBreak<>(c));
                }
            }
        }
        return c;
    }

    private static class AddBreak<P> extends JavaIsoVisitor<P> {
        private final J.Case scope;

        public AddBreak(J.Case scope) {
            this.scope = scope;
        }

        @Override
        public J.Case visitCase(J.Case caze, P p) {
            J.Case c = super.visitCase(caze, p);
            if (scope.isScope(c) &&
                c.getStatements().stream().noneMatch(J.Break.class::isInstance) &&
                c.getStatements().stream()
                        .reduce((s1, s2) -> s2)
                        .map(s -> !(s instanceof J.Block))
                        .orElse(true)) {
                List<Statement> statements = new ArrayList<>(c.getStatements());
                J.Break breakToAdd = autoFormat(
                        new J.Break(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null),
                        p
                );
                statements.add(breakToAdd);
                c = c.withStatements(ListUtils.map(statements, stmt -> autoFormat(stmt, p)));
            }
            return c;
        }

        @Override
        public J.Block visitBlock(J.Block block, P p) {
            J.Block b = super.visitBlock(block, p);
            if (getCursor().isScopeInPath(scope) &&
                b.getStatements().stream().noneMatch(J.Break.class::isInstance) &&
                b.getStatements().stream()
                        .reduce((s1, s2) -> s2)
                        .map(s -> !(s instanceof J.Block))
                        .orElse(true)) {
                List<Statement> statements = b.getStatements();
                J.Break breakToAdd = autoFormat(
                        new J.Break(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null),
                        p
                );
                statements.add(breakToAdd);
                b = b.withStatements(ListUtils.map(statements, stmt -> autoFormat(stmt, p)));
            }
            return b;
        }
    }

    private static class FindLastLineBreaksOrFallsThroughComments {
        private FindLastLineBreaksOrFallsThroughComments() {
        }

        /**
         * If no results are found, it means we should append a {@link J.Break} to the provided {@link J.Case}.
         * A result is added to the set when the last line of the provided {@link J.Case} scope is either an acceptable "break"-able type,
         * specifically {@link J.Return}, {@link J.Break}, {@link J.Continue}, or {@link J.Throw}, or a "fallthrough" {@link Comment} matching a regular expression.
         *
         * @param enclosingSwitch The enclosing {@link J.Switch} subtree to search.
         * @param scope           the {@link J.Case} to use as a target.
         * @return A set representing whether the last {@link Statement} is an acceptable "break"-able type or has a "fallthrough" comment.
         */
        private static Set<J> find(J.Switch enclosingSwitch, J.Case scope) {
            Set<J> references = new HashSet<>();
            new FindLastLineBreaksOrFallsThroughCommentsVisitor(scope).visit(enclosingSwitch, references);
            return references;
        }

        private static class FindLastLineBreaksOrFallsThroughCommentsVisitor extends JavaIsoVisitor<Set<J>> {
            private static final Predicate<Comment> HAS_RELIEF_PATTERN_COMMENT = comment ->
                    comment instanceof TextComment &&
                    RELIEF_PATTERN.matcher(((TextComment) comment).getText()).find();
            private final J.Case scope;

            public FindLastLineBreaksOrFallsThroughCommentsVisitor(J.Case scope) {
                this.scope = scope;
            }

            private static boolean lastLineBreaksOrFallsThrough(List<? extends Tree> trees) {
                return trees.stream()
                        .reduce((s1, s2) -> s2) // last statement
                        .map(s -> s instanceof J.Return ||
                                  s instanceof J.Break ||
                                  s instanceof J.Continue ||
                                  s instanceof J.Throw ||
                                  ((J) s).getComments().stream().anyMatch(HAS_RELIEF_PATTERN_COMMENT)
                        ).orElse(false);
            }

            @Override
            public J.Switch visitSwitch(J.Switch switzh, Set<J> ctx) {
                J.Switch s = super.visitSwitch(switzh, ctx);
                List<Statement> statements = s.getCases().getStatements();
                for (int i = 0; i < statements.size() - 1; i++) {
                    if (!(statements.get(i) instanceof J.Case)) {
                        continue;
                    }

                    J.Case caze = (J.Case) statements.get(i);
                    /*
                     * {@code i + 1} because a last-line comment for a J.Case gets attached as a prefix comment in the next case
                     *
                     * <pre>
                     * SWITCH(..) {
                     *  CASE 1:
                     *      someStatement1; // fallthrough
                     *  CASE 2:
                     *      someStatement2;
                     * }
                     * </pre>
                     * <p>
                     * In order to know whether "CASE 1" ended with the comment "fallthrough", we have to check
                     * the "prefix" of CASE 2, because the CASE 2 prefix is what has the comments associated for CASE 1.
                     **/
                    if (caze == scope && statements.get(i + 1).getPrefix().getComments().stream().anyMatch(HAS_RELIEF_PATTERN_COMMENT)) {
                        ctx.add(s);
                    }
                }
                return s;
            }

            @Override
            public J.Case visitCase(J.Case caze, Set<J> ctx) {
                J.Case c = super.visitCase(caze, ctx);
                if (c == scope) {
                    if (c.getStatements().isEmpty() || lastLineBreaksOrFallsThrough(c.getStatements())) {
                        ctx.add(c);
                    }
                }
                return c;
            }

            @Override
            public J.Block visitBlock(J.Block block, Set<J> ctx) {
                J.Block b = super.visitBlock(block, ctx);
                if (getCursor().isScopeInPath(scope)) {
                    if (lastLineBreaksOrFallsThrough(b.getStatements()) || b.getEnd().getComments().stream().anyMatch(HAS_RELIEF_PATTERN_COMMENT)) {
                        ctx.add(b);
                    }
                }
                return b;
            }

        }

    }

}
