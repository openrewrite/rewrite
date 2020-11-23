package org.openrewrite.xml;

import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;

public class RemoveContent {
    private RemoveContent() {
    }

    public static class Scoped extends XmlRefactorVisitor {
        private final Content scope;

        public Scoped(Content tag) {
            this.scope = tag;
        }

        @Override
        public Xml visitTag(Xml.Tag tag) {
            for (Content content : tag.getContent()) {
                if (scope.isScope(content)) {
                    List<Content> contents = new ArrayList<>(tag.getContent());
                    contents.remove(content);
                    return tag.withContent(contents);
                }
            }

            return super.visitTag(tag);
        }
    }
}
