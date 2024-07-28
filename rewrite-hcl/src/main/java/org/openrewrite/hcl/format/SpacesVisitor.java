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
package org.openrewrite.hcl.format;

import org.openrewrite.Tree;
import org.openrewrite.hcl.HclIsoVisitor;
import org.openrewrite.hcl.style.SpacesStyle;
import org.openrewrite.hcl.tree.Expression;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.internal.lang.Nullable;

public class SpacesVisitor<P> extends HclIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final SpacesStyle style;

    public SpacesVisitor(SpacesStyle style) {
        this(style, null);
    }

    public SpacesVisitor(SpacesStyle style, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;
    }

    @Override
    public Hcl.Attribute visitAttribute(Hcl.Attribute attribute, P p) {
        Hcl.Attribute a = super.visitAttribute(attribute, p);

        //noinspection ConstantConditions
        return (Hcl.Attribute) new AttributeSpaceVisitor<P>(style).visit(a, p, getCursor().getParentOrThrow());
    }

    @Override
    public Expression visitExpression(Expression expression, P p) {
        Expression e = super.visitExpression(expression, p);

        Hcl parent = getCursor().getParentOrThrow().getValue() instanceof Hcl ?
                getCursor().getParentOrThrow().getValue() : null;
        if (parent instanceof Hcl.Attribute && ((Hcl.Attribute) parent).getValue() == expression) {
            if (e.getPrefix().getWhitespace().isEmpty()) {
                e = e.withPrefix(e.getPrefix().withWhitespace(" "));
            }
        }

        return e;
    }

    @Override
    public Hcl.Block visitBlock(Hcl.Block block, P p) {
        Hcl.Block b = super.visitBlock(block, p);
        if (block.getType() != null) {
             b = b.withOpen(block.getOpen().withWhitespace(" "));
        }
        return b;
    }

    @Override
    public @Nullable Hcl postVisit(Hcl tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(Hcl.ConfigFile.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Override
    public @Nullable Hcl visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (Hcl) tree;
        }
        return super.visit(tree, p);
    }
}
