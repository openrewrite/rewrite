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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.hcl.HclIsoVisitor;
import org.openrewrite.hcl.style.BracketsStyle;
import org.openrewrite.hcl.tree.*;
import org.openrewrite.internal.ListUtils;

import java.util.ArrayList;
import java.util.List;

public class BracketsVisitor<P> extends HclIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final BracketsStyle style;

    public BracketsVisitor(BracketsStyle style) {
        this(style, null);
    }

    public BracketsVisitor(BracketsStyle style, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, P p) {
        if (loc == Space.Location.BLOCK_CLOSE && !space.getLastWhitespace().contains("\n")) {
            return space.withLastWhitespace("\n");
        }

        if (loc == Space.Location.BLOCK_OPEN && !" ".equals(space.getWhitespace())) {
            return space.withWhitespace(" ");
        }

        return space;
    }

    @Override
    public <T extends Hcl> HclContainer<T> visitContainer(final HclContainer<T> container, final HclContainer.Location loc, final P p) {
        if (loc == HclContainer.Location.OBJECT_VALUE_ATTRIBUTES) {
            final Hcl.ObjectValue ov = getCursor().firstEnclosingOrThrow(Hcl.ObjectValue.class);
            final Space lastSpace = container.getLastSpace();
            if (isMultiline(ov)) {
                if (!lastSpace.getLastWhitespace().contains("\n")) {
                    return container.withLastSpace(lastSpace.withLastWhitespace("\n"));
                }
            } else {
                return container.withLastSpace(lastSpace.withLastWhitespace(" "));
            }
        } else if (loc == HclContainer.Location.TUPLE_VALUES) {
            final Hcl.Tuple tuple = getCursor().firstEnclosingOrThrow(Hcl.Tuple.class);
            final Space lastSpace = container.getLastSpace();
            if (isMultiline(tuple)) {
                if (!lastSpace.getLastWhitespace().contains("\n")) {
                    return container.withLastSpace(lastSpace.withLastWhitespace("\n"));
                }
            }
        }
        return super.visitContainer(container, loc, p);
    }

    @Override
    public Hcl.ObjectValue visitObjectValue(final Hcl.ObjectValue ov, final P p) {
        boolean multiLine = isMultiline(ov);
        if (multiLine) {
            final List<Expression> newAttributes = ListUtils.map(ov.getAttributes(), ((i, attr) -> {
                if (!attr.getPrefix().getLastWhitespace().contains("\n")) {
                    return attr.withPrefix(attr.getPrefix().withLastWhitespace("\n"));
                } else {
                    return attr;
                }
            }));
            return super.visitObjectValue(ov.withArguments(newAttributes), p);
        } else {
            final List<Expression> newAttributes = ListUtils.map(ov.getAttributes(),
                    ((i, attr) -> attr.withPrefix(attr.getPrefix().withWhitespace(" "))));
            return super.visitObjectValue(ov.withArguments(newAttributes), p);
        }
    }

    private boolean isMultiline(final Hcl.ObjectValue ov) {
        boolean multiLine = false;
        for (final Expression attribute : ov.getAttributes()) {
            if (attribute.getPrefix().getLastWhitespace().contains("\n")) {
                multiLine = true;
            }
        }
        return multiLine;
    }

    @Override
    public Hcl.Tuple visitTuple(final Hcl.Tuple tuple, final P p) {
        boolean multiLine = isMultiline(tuple);
        if (multiLine) {
            final List<Expression> newValues = ListUtils.map(tuple.getValues(), ((i, value) -> {
                if (!value.getPrefix().getLastWhitespace().contains("\n")) {
                    return value.withPrefix(value.getPrefix().withLastWhitespace("\n"));
                } else {
                    return value;
                }
            }));
            return super.visitTuple(tuple.withValues(newValues), p);
        }
        return super.visitTuple(tuple, p);
    }

    private boolean isMultiline(final Hcl.Tuple tuple) {
        boolean multiLine = false;
        for (final Expression value : tuple.getValues()) {
            if (value.getPrefix().getLastWhitespace().contains("\n")) {
                multiLine = true;
            }
        }
        return multiLine;
    }

    @Override
    public Hcl.Block visitBlock(final Hcl.Block block, final P p) {
        final List<BodyContent> body = block.getBody();
        if (!body.isEmpty()) {
            BodyContent first = body.get(0);
            if (!first.getPrefix().getLastWhitespace().contains("\n")) {
                List<BodyContent> newBody = new ArrayList<>(body);
                newBody.set(0, first.withPrefix(first.getPrefix().withWhitespace("\n")));
                return super.visitBlock(block.withBody(newBody), p);
            }
        }
        return super.visitBlock(block, p);
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
