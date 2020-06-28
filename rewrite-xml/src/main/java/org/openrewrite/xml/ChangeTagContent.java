package org.openrewrite.xml;

import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.List;

public class ChangeTagContent extends XmlRefactorVisitor {
    private final Xml.Tag scope;
    private final List<Content> content;

    public ChangeTagContent(Xml.Tag scope, List<Content> content) {
        this.scope = scope;
        this.content = content;
    }

    @Override
    public Xml visitTag(Xml.Tag tag) {
        Xml.Tag t = refactor(tag, super::visitTag);
        if (scope.isScope(t)) {
            t = t.withContent(content);
        }
        return t;
    }
}
