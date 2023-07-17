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

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.style.Autodetect;
import org.openrewrite.xml.style.TabsAndIndentsStyle;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

import static java.util.Collections.singletonList;
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

        TabsAndIndentsStyle tabsStyle = Optional.ofNullable(doc.getStyle(TabsAndIndentsStyle.class))
                .orElseGet(() -> {
                    Autodetect.Detector detector = Autodetect.detector();
                    detector.sample(doc);
                    return NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(detector.build()));
                });
        assert tabsStyle != null;

        t = new NormalizeTabsOrSpacesVisitor<>(tabsStyle, stopAfter)
                .visit(t, p, cursor.fork());

        t = new TabsAndIndentsVisitor<>(tabsStyle, stopAfter)
                .visit(t, p, cursor.fork());

        t = new NormalizeLineBreaksVisitor<>(Optional.ofNullable(doc.getStyle(GeneralFormatStyle.class))
                .orElse(autodetectGeneralFormatStyle(doc)), stopAfter)
                .visit(t, p, cursor.fork());

        return t;
    }

    @Override
    public Xml.Document visitDocument(Xml.Document doc, P p) {
        Xml.Document t = (Xml.Document) new RemoveTrailingWhitespaceVisitor<>(stopAfter).visit(doc, p);

        t = (Xml.Document) new NormalizeFormatVisitor<>(stopAfter).visit(t, p);

        t = (Xml.Document) new MinimumViableSpacingVisitor<>(stopAfter).visit(t, p);

        t = (Xml.Document) new RemoveTrailingWhitespaceVisitor<>(stopAfter).visit(t, p);

        t = (Xml.Document) new LineBreaksVisitor<>(stopAfter).visit(t, p);

        TabsAndIndentsStyle tabsStyle = Optional.ofNullable(doc.getStyle(TabsAndIndentsStyle.class))
                .orElseGet(() -> {
                    Autodetect.Detector detector = Autodetect.detector();
                    detector.sample(doc);
                    return NamedStyles.merge(TabsAndIndentsStyle.class, singletonList(detector.build()));
                });
        assert tabsStyle != null;

        t = (Xml.Document) new NormalizeTabsOrSpacesVisitor<>(tabsStyle, stopAfter).visit(t, p);

        t = (Xml.Document) new TabsAndIndentsVisitor<>(tabsStyle, stopAfter).visit(t, p);

        t = (Xml.Document) new NormalizeLineBreaksVisitor<>(Optional.ofNullable(doc.getStyle(GeneralFormatStyle.class))
                .orElse(autodetectGeneralFormatStyle(doc)), stopAfter)
                .visit(t, p);

        assert t != null;
        return t;
    }
}
