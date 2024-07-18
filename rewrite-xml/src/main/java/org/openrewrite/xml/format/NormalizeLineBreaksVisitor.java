/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

public class NormalizeLineBreaksVisitor<P> extends XmlIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final GeneralFormatStyle style;

    public NormalizeLineBreaksVisitor(GeneralFormatStyle style) {
        this(style, null);
    }

    public NormalizeLineBreaksVisitor(GeneralFormatStyle style, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;
    }

    @Override
    public @Nullable Xml visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (Xml) tree;
        }

        if (tree instanceof Xml) {
            Xml x = super.visit(tree, p);
            assert x != null;
            return x.withPrefix(normalizeNewLines(x.getPrefix(), style.isUseCRLFNewLines()));
        }

        return super.visit(tree, p);
    }

    private static String normalizeNewLines(String text, boolean useCrlf) {
        if (!text.contains("\n")) {
            return text;
        }
        StringBuilder normalized = new StringBuilder();
        char[] charArray = text.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (useCrlf && c == '\n' && (i == 0 || text.charAt(i - 1) != '\r')) {
                normalized.append('\r').append('\n');
            } else if (useCrlf || c != '\r') {
                normalized.append(c);
            }
        }
        return normalized.toString();
    }

    @Override
    public @Nullable Xml postVisit(Xml tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(Xml.Document.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }
}
