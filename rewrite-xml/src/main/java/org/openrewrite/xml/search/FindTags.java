package org.openrewrite.xml.search;

import org.openrewrite.Tree;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlSourceVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class FindTags extends XmlSourceVisitor<List<Xml.Tag>> {
    private final XPathMatcher xPathMatcher;

    public FindTags(String xpath) {
        this.xPathMatcher = new XPathMatcher(xpath);
        setCursoringOn();
    }

    @Override
    public List<Xml.Tag> defaultTo(Tree t) {
        return emptyList();
    }

    @Override
    public List<Xml.Tag> visitTag(Xml.Tag tag) {
        if(xPathMatcher.matches(getCursor())) {
            return singletonList(tag);
        }
        return super.visitTag(tag);
    }
}
