package org.openrewrite.xml;

import org.openrewrite.xml.tree.Xml;

public class RemoveEmptyTagsVisitor<P> extends XmlVisitor<P> {
    @Override
    public Xml visitTag(Xml.Tag tag, P p) {
        Xml.Tag t = (Xml.Tag) super.visitTag(tag, p);

        if (isEmptyTag(t) || (t.getClosing() != null && t.getClosing().isSelfClosing(t))) {
            return null; // Remove tags that are either empty or self-closing.
        }

        return t;
    }

    private boolean isEmptyTag(Xml.Tag tag) {
        return tag != null && (tag.getContent() == null || tag.getContent().isEmpty()) && tag.getAttributes().isEmpty();
    }
}