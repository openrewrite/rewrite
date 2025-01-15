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
package org.openrewrite.xml.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.style.TabsAndIndentsStyle;
import org.openrewrite.xml.tree.Xml;

public class TabsAndIndentsVisitor<P> extends XmlIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final TabsAndIndentsStyle style;

    public TabsAndIndentsVisitor(TabsAndIndentsStyle style) {
        this(style, null);
    }

    public TabsAndIndentsVisitor(TabsAndIndentsStyle style, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;
    }

    @Override
    public Xml preVisit(Xml tree, P p) {
        Xml x = super.preVisit(tree, p);
        if (x != null) {
            String prefix = x.getPrefix();
            if (prefix.contains("\n")) {
                int indentMultiple = (int) getCursor().getPathAsStream().filter(Xml.Tag.class::isInstance).count() - 1;
                if (getCursor().getValue() instanceof Xml.Attribute ||
                        getCursor().getValue() instanceof Xml.CharData ||
                        getCursor().getValue() instanceof Xml.Comment ||
                        getCursor().getValue() instanceof Xml.ProcessingInstruction) {
                    indentMultiple++;
                }

                StringBuilder shiftedPrefixBuilder = new StringBuilder(prefix.substring(0, prefix.lastIndexOf('\n') + 1));
                for (int i = 0; i < indentMultiple; i++) {
                    if(style.getUseTabCharacter()) {
                        shiftedPrefixBuilder.append("\t");
                    } else {
                        for(int j = 0; j < (x instanceof Xml.Attribute ? style.getContinuationIndentSize() : style.getIndentSize()); j++) {
                            shiftedPrefixBuilder.append(" ");
                        }
                    }
                }

                String shiftedPrefix = shiftedPrefixBuilder.toString();
                if (!shiftedPrefix.equals(prefix)) {
                    return x.withPrefix(shiftedPrefix);
                }
            }
        }
        return x;
    }

    @Override
    public @Nullable Xml postVisit(Xml tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(Xml.Document.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Override
    public @Nullable Xml visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (Xml) tree;
        }
        return super.visit(tree, p);
    }
}
