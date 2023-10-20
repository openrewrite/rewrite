package org.openrewrite.xml;

import org.openrewrite.Recipe;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.tree.Xml;

public class RemoveEmptyXmlTagsRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove empty XML Tag";
    }

    @Override
    public String getDescription() {
        return "Removes XML tags that do not have attributes or children.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                doAfterVisit(new RemoveEmptyTagsVisitor<>());
                return super.visitTag(tag, ctx);
            }
        };
    }
}