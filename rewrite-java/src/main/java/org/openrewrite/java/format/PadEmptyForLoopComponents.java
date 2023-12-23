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
import org.openrewrite.java.style.EmptyForInitializerPadStyle;
import org.openrewrite.java.style.EmptyForIteratorPadStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.SearchResult;

import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;

public class PadEmptyForLoopComponents extends Recipe {

    @Override
    public String getDisplayName() {
        return "Pad empty `for` loop components";
    }

    @Override
    public String getDescription() {
        return "Fixes padding on empty `for` loop iterators and initializers to match Checkstyle policies.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    SourceFile cu = (SourceFile) requireNonNull(tree);
                    if (cu.getStyle(EmptyForIteratorPadStyle.class) != null || cu.getStyle(EmptyForInitializerPadStyle.class) != null) {
                        return cu.withMarkers(cu.getMarkers().add(new SearchResult(randomId(), null)));
                    }
                    return (JavaSourceFile) cu;
                }
                return (J) tree;
            }
        }, new JavaIsoVisitor<ExecutionContext>() {
            @Nullable
            EmptyForIteratorPadStyle emptyForIteratorPadStyle;

            @Nullable
            EmptyForInitializerPadStyle emptyForInitializerPadStyle;

            @Override
            public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    SourceFile cu = (SourceFile) requireNonNull(tree);
                    emptyForInitializerPadStyle = cu.getStyle(EmptyForInitializerPadStyle.class);
                    emptyForIteratorPadStyle = cu.getStyle(EmptyForIteratorPadStyle.class);
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J.ForLoop visitForLoop(J.ForLoop forLoop, ExecutionContext ctx) {
                J.ForLoop fl = super.visitForLoop(forLoop, ctx);
                List<Statement> updates = forLoop.getControl().getUpdate();
                if (emptyForIteratorPadStyle != null && updates.size() == 1 && updates.get(0) instanceof J.Empty) {
                    Statement update = updates.get(0);
                    if (emptyForIteratorPadStyle.getSpace() && update.getPrefix().getWhitespace().isEmpty()) {
                        update = update.withPrefix(update.getPrefix().withWhitespace(" "));
                    } else if (!emptyForIteratorPadStyle.getSpace() && !update.getPrefix().getWhitespace().isEmpty()) {
                        update = update.withPrefix(update.getPrefix().withWhitespace(""));
                    }
                    fl = fl.withControl(fl.getControl().withUpdate(singletonList(update)));
                }

                List<Statement> init = forLoop.getControl().getInit();
                if (emptyForInitializerPadStyle != null && init.get(0) instanceof J.Empty) {
                    if (emptyForInitializerPadStyle.getSpace() && init.get(0).getPrefix().getWhitespace().isEmpty()) {
                        init = ListUtils.mapFirst(init, i -> i.withPrefix(i.getPrefix().withWhitespace(" ")));
                    } else if (!emptyForInitializerPadStyle.getSpace() && !init.get(0).getPrefix().getWhitespace().isEmpty()) {
                        init = ListUtils.mapFirst(init, i -> i.withPrefix(i.getPrefix().withWhitespace("")));
                    }
                    fl = fl.withControl(fl.getControl().withInit(init));
                }
                return fl;
            }
        });
    }
}
