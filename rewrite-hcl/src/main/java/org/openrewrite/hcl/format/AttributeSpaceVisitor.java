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
import org.openrewrite.hcl.tree.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;

public class AttributeSpaceVisitor<P> extends HclIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final SpacesStyle style;

    public AttributeSpaceVisitor(SpacesStyle style) {
        this(style, null);
    }

    public AttributeSpaceVisitor(SpacesStyle style, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;
    }

    @Override
    public Hcl.Attribute visitAttribute(Hcl.Attribute attribute, P p) {
        Hcl.Attribute a = super.visitAttribute(attribute, p);

        Hcl parent = getCursor().dropParentUntil(t -> t instanceof Hcl).getValue();
        if (parent instanceof Hcl.Block) {
            Hcl.Block block = (Hcl.Block) parent;

            if(attribute.getType().equals(Hcl.Attribute.Type.Assignment)) {
                HclLeftPadded<Hcl.Attribute.Type> type = a.getPadding().getType();

                if (Boolean.TRUE.equals(style.getBodyContent().getColumnarAlignment())) {
                    int rightMostColumnOfAttributeKey = 0;
                    for (BodyContent bodyContent : block.getBody()) {
                        if (bodyContent instanceof Hcl.Attribute) {
                            Hcl.Attribute sibling = (Hcl.Attribute) bodyContent;
                            if (sibling.getType().equals(Hcl.Attribute.Type.Assignment)) {
                                rightMostColumnOfAttributeKey = Math.max(rightMostColumnOfAttributeKey, endColumn(sibling));
                            }
                        }
                    }

                    rightMostColumnOfAttributeKey = Math.max(rightMostColumnOfAttributeKey, endColumn(a));

                    int indent = rightMostColumnOfAttributeKey - endColumn(a) + 1;
                    a = a.getPadding().withType(type.withBefore(type.getBefore().withWhitespace(
                            StringUtils.repeat(" ", indent))));
                } else if (Boolean.FALSE.equals(style.getBodyContent().getColumnarAlignment())) {
                    a = a.getPadding().withType(type.withBefore(type.getBefore().withWhitespace(" ")));
                }
            }
        }

        return a;
    }

    private int endColumn(Hcl.Attribute attribute) {
        return (attribute.getPrefix().getIndent() + attribute.getName().print()).length();
    }

    @Nullable
    @Override
    public Hcl postVisit(Hcl tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(Hcl.ConfigFile.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Nullable
    @Override
    public Hcl visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (Hcl) tree;
        }
        return super.visit(tree, p);
    }
}
