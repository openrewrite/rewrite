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

    @Nullable
    @Override
    public Xml visit(@Nullable Tree tree, P p) {
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


    @Nullable
    @Override
    public Xml postVisit(Xml tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(Xml.Document.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }
}
