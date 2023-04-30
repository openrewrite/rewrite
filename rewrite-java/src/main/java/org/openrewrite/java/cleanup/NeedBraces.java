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
package org.openrewrite.java.cleanup;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.java.style.NeedBracesStyle;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class NeedBraces extends Recipe {
    @Override
    public String getDisplayName() {
        return "Fix missing braces";
    }

    @Override
    public String getDescription() {
        return "Adds missing braces around code such as single-line `if`, `for`, `while`, and `do-while` block bodies.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-121");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new NeedBracesVisitor();
    }

    private static class NeedBracesVisitor extends JavaIsoVisitor<ExecutionContext> {
        NeedBracesStyle needBracesStyle;

        /**
         * A {@link J.Block} implies the section of code is implicitly surrounded in braces.
         * We can use that to our advantage by saying if you aren't a block (e.g. a single {@link Statement}, etc.),
         * then we're going to make this into a block. That's how we'll get the code bodies surrounded in braces.
         */
        private static <T extends Statement> J.Block buildBlock(T element) {
            return new J.Block(
                    Tree.randomId(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    JRightPadded.build(false),
                    element instanceof J.Empty ? Collections.emptyList() : Collections.singletonList(JRightPadded.build(element)),
                    Space.EMPTY
            );
        }

        @Override
        public J visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                SourceFile cu = (SourceFile) requireNonNull(tree);
                needBracesStyle = cu.getStyle(NeedBracesStyle.class) == null ? Checkstyle.needBracesStyle() : cu.getStyle(NeedBracesStyle.class);
            }
            return super.visit(tree, ctx);
        }

        @Override
        public J.If visitIf(J.If iff, ExecutionContext ctx) {
            J.If elem = super.visitIf(iff, ctx);
            boolean hasAllowableBodyType = elem.getThenPart() instanceof J.Block;
            if (!needBracesStyle.getAllowSingleLineStatement() && !hasAllowableBodyType) {
                J.Block b = buildBlock(elem.getThenPart());
                elem = maybeAutoFormat(elem, elem.withThenPart(b), ctx);
            }
            return elem;
        }

        @Override
        public J.If.Else visitElse(J.If.Else elze, ExecutionContext ctx) {
            J.If.Else elem = super.visitElse(elze, ctx);
            boolean hasAllowableBodyType = elem.getBody() instanceof J.Block || elem.getBody() instanceof J.If;
            if (!needBracesStyle.getAllowSingleLineStatement() && !hasAllowableBodyType) {
                J.Block b = buildBlock(elem.getBody());
                elem = maybeAutoFormat(elem, elem.withBody(b), ctx);
            }
            return elem;
        }

        @Override
        public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, ExecutionContext ctx) {
            J.WhileLoop elem = super.visitWhileLoop(whileLoop, ctx);
            boolean hasAllowableBodyType = needBracesStyle.getAllowEmptyLoopBody() ?
                    elem.getBody() instanceof J.Block || elem.getBody() instanceof J.Empty :
                    elem.getBody() instanceof J.Block;
            if (!needBracesStyle.getAllowEmptyLoopBody() && elem.getBody() instanceof J.Empty) {
                J.Block b = buildBlock(elem.getBody());
                elem = maybeAutoFormat(elem, elem.withBody(b), ctx);
            } else if (!needBracesStyle.getAllowSingleLineStatement() && !hasAllowableBodyType) {
                J.Block b = buildBlock(elem.getBody());
                elem = maybeAutoFormat(elem, elem.withBody(b), ctx);
            }
            return elem;
        }

        @Override
        public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, ExecutionContext ctx) {
            J.DoWhileLoop elem = super.visitDoWhileLoop(doWhileLoop, ctx);
            boolean hasAllowableBodyType = needBracesStyle.getAllowEmptyLoopBody() ?
                    elem.getBody() instanceof J.Block || elem.getBody() instanceof J.Empty :
                    elem.getBody() instanceof J.Block;
            if (!needBracesStyle.getAllowEmptyLoopBody() && elem.getBody() instanceof J.Empty) {
                J.Block b = buildBlock(elem.getBody());
                elem = maybeAutoFormat(elem, elem.withBody(b), ctx);
            } else if (!needBracesStyle.getAllowSingleLineStatement() && !hasAllowableBodyType) {
                J.Block b = buildBlock(elem.getBody());
                elem = maybeAutoFormat(elem, elem.withBody(b), ctx);
            }
            return elem;
        }

        @Override
        public J.ForLoop visitForLoop(J.ForLoop forLoop, ExecutionContext ctx) {
            J.ForLoop elem = super.visitForLoop(forLoop, ctx);
            boolean hasAllowableBodyType = needBracesStyle.getAllowEmptyLoopBody() ?
                    elem.getBody() instanceof J.Block || elem.getBody() instanceof J.Empty :
                    elem.getBody() instanceof J.Block;
            if (!needBracesStyle.getAllowEmptyLoopBody() && elem.getBody() instanceof J.Empty) {
                J.Block b = buildBlock(elem.getBody());
                elem = maybeAutoFormat(elem, elem.withBody(b), ctx);
            } else if (!needBracesStyle.getAllowSingleLineStatement() && !hasAllowableBodyType) {
                J.Block b = buildBlock(elem.getBody());
                elem = maybeAutoFormat(elem, elem.withBody(b), ctx);
            }
            return elem;
        }

    }
}
