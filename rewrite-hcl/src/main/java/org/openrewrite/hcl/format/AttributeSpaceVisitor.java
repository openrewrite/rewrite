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
import org.openrewrite.hcl.style.SpacesStyle;
import org.openrewrite.hcl.tree.BodyContent;
import org.openrewrite.hcl.tree.Expression;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.hcl.tree.HclLeftPadded;
import org.openrewrite.internal.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

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

        Hcl parent = getCursor().getParentTreeCursor().getValue();
        if (parent instanceof Hcl.Block || parent instanceof Hcl.ObjectValue) {
            List<Hcl.Attribute> siblingAttributes = getSiblingAttributes(parent);

            if (attribute.getType() == Hcl.Attribute.Type.Assignment) {
                HclLeftPadded<Hcl.Attribute.Type> type = a.getPadding().getType();

                if (Boolean.TRUE.equals(style.getBodyContent().getColumnarAlignment())) {
                    List<Hcl.Attribute> groupAttributes = attributesInGroup(siblingAttributes, attribute);

                    int rightMostColumnOfAttributeKey = 0;
                    for (final Hcl.Attribute sibling : groupAttributes) {
                        rightMostColumnOfAttributeKey = Math.max(rightMostColumnOfAttributeKey, endColumn(sibling));
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

    private List<Hcl.Attribute> getSiblingAttributes(final Hcl parent) {
        List<Hcl.Attribute> allAttributes = new ArrayList<>();
        if (parent instanceof Hcl.Block) {
            for (final BodyContent bc : ((Hcl.Block) parent).getBody()) {
                if (bc instanceof Hcl.Attribute) {
                    allAttributes.add((Hcl.Attribute) bc);
                }
            }
        }else {
            for (final Expression expr : ((Hcl.ObjectValue) parent).getAttributes()) {
                if (expr instanceof Hcl.Attribute) {
                    allAttributes.add((Hcl.Attribute) expr);
                }
            }
        }
        return allAttributes;
    }

    // find group of attributes (attributes with no extra newlines) containing given attribute
    private List<Hcl.Attribute> attributesInGroup(List<Hcl.Attribute> siblings, Hcl.Attribute attribute) {
        boolean isAttributeMultiline = attribute.getValue().print(getCursor()).split("\r\n|\r|\n").length > 2;
       if (isAttributeMultiline) {
            return singletonList(attribute);
        }

        List<Hcl.Attribute> groupAttributes = new ArrayList<>();
        boolean groupFound = false;
        Hcl.Attribute perviousSibling = null;
        for (Hcl.Attribute sibling : siblings) {
            if (sibling.getType() == Hcl.Attribute.Type.Assignment) {
                boolean siblingPrefixHasNewLines = sibling.getPrefix().getWhitespace().split("\r\n|\r|\n").length > 2;
                boolean siblingIsMultiline = sibling.getValue().print(getCursor()).split("\r\n|\r|\n").length > 2;
                boolean previousSiblingIsMultiline = perviousSibling != null && perviousSibling.getValue().print(getCursor()).split("\r\n|\r|\n").length > 2;
                boolean newGroup  = siblingPrefixHasNewLines || previousSiblingIsMultiline || siblingIsMultiline;
                if (newGroup) {
                    if (groupFound) {
                        break;
                    }
                    groupAttributes.clear();
                }
                if (sibling.getId() == attribute.getId()) {
                    groupFound = true;
                }
                groupAttributes.add(sibling);
                perviousSibling = sibling;
            }
        }

        return groupAttributes;
    }

    private int endColumn(Hcl.Attribute attribute) {
        return (attribute.getPrefix().getIndent() + attribute.getName().print(getCursor())).length();
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
