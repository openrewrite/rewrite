package org.openrewrite.xml.format;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.style.TabsAndIndentsStyle;
import org.openrewrite.xml.tree.Xml;

public class NormalizeTabsOrSpacesVisitor<P> extends XmlIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final TabsAndIndentsStyle style;

    public NormalizeTabsOrSpacesVisitor(TabsAndIndentsStyle style) {
        this(style, null);
    }

    public NormalizeTabsOrSpacesVisitor(TabsAndIndentsStyle style, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;
    }

    @NotNull
    private String normalizeAfterFirstNewline(String text) {
        int firstNewline = text.indexOf('\n');
        if (firstNewline >= 0 && firstNewline != text.length() - 1) {
            return text.substring(0, firstNewline + 1) + normalize(text.substring(firstNewline + 1));
        }
        return text;
    }

    private String normalize(String text) {
        if (!StringUtils.isNullOrEmpty(text)) {
            if (style.getUseTabCharacter() ? text.contains(" ") : text.contains("\t")) {
                StringBuilder textBuilder = new StringBuilder();
                int consecutiveSpaces = 0;
                boolean inMargin = true;
                char[] charArray = text.toCharArray();
                outer:
                for (int i = 0; i < charArray.length; i++) {
                    char c = charArray[i];
                    if (c == '\n' || c == '\r') {
                        inMargin = true;
                        consecutiveSpaces = 0;
                        textBuilder.append(c);
                    } else if (!inMargin) {
                        textBuilder.append(c);
                    } else if (style.getUseTabCharacter() && c == ' ') {
                        int j = i + 1;
                        for (; j < charArray.length && j < style.getTabSize(); j++) {
                            if (charArray[j] != ' ') {
                                continue outer;
                            }
                        }
                        i = j + 1;
                        textBuilder.append('\t');
                    } else if (!style.getUseTabCharacter() && c == '\t') {
                        for (int j = 0; j < style.getTabSize() - (consecutiveSpaces % style.getTabSize()); j++) {
                            textBuilder.append(' ');
                        }
                        consecutiveSpaces = 0;
                    } else if (Character.isWhitespace(c)) {
                        consecutiveSpaces++;
                        textBuilder.append(c);
                    } else {
                        inMargin = false;
                        textBuilder.append(c);
                    }
                }
                return textBuilder.toString();
            }
        }

        return text;
    }

    @Nullable
    @Override
    public Xml visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (Xml) tree;
        }

        if (tree instanceof Xml) {
            Xml x = super.visit(tree, p);
            assert x != null;
            return x.withPrefix(normalizeAfterFirstNewline(x.getPrefix()));
        }

        return super.visit(tree, p);
    }

    @Nullable
    @Override
    public Xml postVisit(Xml tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(Xml.Document.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }
}
