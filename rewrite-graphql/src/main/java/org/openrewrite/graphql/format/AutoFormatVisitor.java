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
package org.openrewrite.graphql.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.graphql.GraphQlIsoVisitor;
import org.openrewrite.graphql.style.GraphQlDefaultStyles;
import org.openrewrite.graphql.style.IndentsStyle;
import org.openrewrite.graphql.tree.GraphQl;
import org.openrewrite.style.GeneralFormatStyle;

public class AutoFormatVisitor<P> extends GraphQlIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    public AutoFormatVisitor() {
        this(null);
    }

    public AutoFormatVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public GraphQl preVisit(GraphQl tree, P p) {
        stopAfterPreVisit();
        GraphQl.Document document = getCursor().firstEnclosing(GraphQl.Document.class);
        if (document != null) {
            GraphQl g = new NormalizeFormatVisitor<P>().visit(tree, p, getCursor().fork());
            g = new MinimumViableSpacingVisitor<P>().visit(g, p, getCursor().fork());
            g = new IndentsVisitor<>(IndentsStyle.autodetect(document), stopAfter).visit(g, p, getCursor().fork());
            g = new NormalizeLineBreaksVisitor<>(GraphQlDefaultStyles.lineBreaksStyle(), stopAfter).visit(g, p, getCursor().fork());
            return g;
        }
        return tree;
    }

    @Override
    public GraphQl.Document visitDocument(GraphQl.Document document, P p) {
        // The root element has no cursor position, so we manually apply the visitors in root context
        GraphQl g = new NormalizeFormatVisitor<P>().visit(document, p);
        g = new MinimumViableSpacingVisitor<P>().visit(g, p);
        g = new IndentsVisitor<>(IndentsStyle.autodetect(document), stopAfter).visit(g, p);
        g = new NormalizeLineBreaksVisitor<>(GraphQlDefaultStyles.lineBreaksStyle(), stopAfter).visit(g, p);
        return (GraphQl.Document) g;
    }

    @Override
    public @Nullable GraphQl postVisit(GraphQl tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(GraphQl.Document.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Override
    public @Nullable GraphQl visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (GraphQl) tree;
        }
        return super.visit(tree, p);
    }
}