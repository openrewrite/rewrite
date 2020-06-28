package org.openrewrite.xml.search;

import org.openrewrite.Tree;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlSourceVisitor;
import org.openrewrite.xml.tree.Xml;

public class FindTag extends XmlSourceVisitor<Xml.Tag> {
    private final XPathMatcher xPathMatcher;

    public FindTag(String xpath) {
        this.xPathMatcher = new XPathMatcher(xpath);
        setCursoringOn();
    }

    @Override
    public Xml.Tag defaultTo(Tree t) {
        return null;
    }

    @Override
    public Xml.Tag visitTag(Xml.Tag tag) {
        if(xPathMatcher.matches(getCursor())) {
            return tag;
        }
        return super.visitTag(tag);
    }
}
