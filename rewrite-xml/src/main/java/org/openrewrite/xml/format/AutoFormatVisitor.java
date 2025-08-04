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
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.Style;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.style.Autodetect;
import org.openrewrite.xml.style.TabsAndIndentsStyle;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

import static org.openrewrite.xml.format.AutodetectGeneralFormatStyle.autodetectGeneralFormatStyle;

public class AutoFormatVisitor<P> extends XmlVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    public AutoFormatVisitor() {
        this(null);
    }

    public AutoFormatVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public Xml visit(@Nullable Tree tree, P p, Cursor cursor) {
        Xml.Document doc = (tree instanceof Xml.Document) ?
                (Xml.Document) tree :
                cursor.firstEnclosingOrThrow(Xml.Document.class);

        Xml t = new NormalizeFormatVisitor<>(stopAfter).visit(tree, p, cursor.fork());

        t = new MinimumViableSpacingVisitor<>(stopAfter).visit(t, p, cursor.fork());

        t = new RemoveTrailingWhitespaceVisitor<>(stopAfter).visit(t, p, cursor.fork());

        t = new LineBreaksVisitor<>(stopAfter).visit(t, p, cursor.fork());

        TabsAndIndentsStyle tabsStyle =
                Style.from(TabsAndIndentsStyle.class, doc,
                        () -> Autodetect.detector().sample(doc).build().getStyle(TabsAndIndentsStyle.class));
        t = new NormalizeTabsOrSpacesVisitor<>(tabsStyle, stopAfter)
                .visit(t, p, cursor.fork());

        t = new TabsAndIndentsVisitor<>(tabsStyle, stopAfter)
                .visit(t, p, cursor.fork());

        return new NormalizeLineBreaksVisitor<>(Optional.ofNullable(Style.from(GeneralFormatStyle.class, doc))
                .orElse(autodetectGeneralFormatStyle(doc)), stopAfter)
                .visit(t, p, cursor.fork());
    }

    @Override
    public Xml.Document visitDocument(Xml.Document doc, P p) {
        Xml.Document t = (Xml.Document) new RemoveTrailingWhitespaceVisitor<>(stopAfter).visit(doc, p);

        t = (Xml.Document) new NormalizeFormatVisitor<>(stopAfter).visit(t, p);

        t = (Xml.Document) new MinimumViableSpacingVisitor<>(stopAfter).visit(t, p);

        t = (Xml.Document) new RemoveTrailingWhitespaceVisitor<>(stopAfter).visit(t, p);

        t = (Xml.Document) new LineBreaksVisitor<>(stopAfter).visit(t, p);

        TabsAndIndentsStyle tabsStyle = Optional.ofNullable(Style.from(TabsAndIndentsStyle.class, doc))
                .orElseGet(() -> Autodetect.detector().sample(doc).build().getStyle(TabsAndIndentsStyle.class));
        assert tabsStyle != null;

        t = (Xml.Document) new NormalizeTabsOrSpacesVisitor<>(tabsStyle, stopAfter).visit(t, p);

        t = (Xml.Document) new TabsAndIndentsVisitor<>(tabsStyle, stopAfter).visit(t, p);

        t = (Xml.Document) new NormalizeLineBreaksVisitor<>(Optional.ofNullable(Style.from(GeneralFormatStyle.class, doc))
                .orElse(autodetectGeneralFormatStyle(doc)), stopAfter)
                .visit(t, p);

        assert t != null;
        return t;
    }
}
