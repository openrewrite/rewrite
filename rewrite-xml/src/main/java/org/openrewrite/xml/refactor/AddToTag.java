package org.openrewrite.xml.refactor;

import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;

public class AddToTag extends ScopedXmlRefactorVisitor {
    private final Xml.Tag tagToAdd;

    public AddToTag(Xml.Tag scope, String tagSource) {
        super(scope.getId());
        this.tagToAdd = new XmlParser().parseTag(tagSource);
    }

    @Override
    public Xml visitTag(Xml.Tag tag) {
        Xml.Tag t = refactor(tag, super::visitTag);
        if (isScope()) {
            List<Content> content = t.getContent() == null ? new ArrayList<>() : new ArrayList<>(t.getContent());
            content.add(tagToAdd.withPrefix(formatter.findIndent(enclosingTag().getFormatting().getIndent(), tag).getPrefix()));
            t = t.withContent(content);
        }
        return t;
    }
}
