package org.openrewrite.xml;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.tree.Xml;

public class RemoveDeadRepos extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove dead repositories";
    }

    @Override
    public String getDescription() {
        return "Remove repos marked as dead in Maven POM files.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveDeadReposVisitor();
    }

    private static class RemoveDeadReposVisitor extends XmlIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            Xml.Tag newTag = t;
            if (t.getName().equals("repository")) {
                newTag = t.getChild("url").flatMap(Xml.Tag::getValue).map(url -> {
                    String urlKey = IdentifyUnreachableRepos.httpOrHttps(url);
                    if (IdentifyUnreachableRepos.KNOWN_DEFUNCT.containsKey(urlKey)) {
                        if (IdentifyUnreachableRepos.KNOWN_DEFUNCT.get(urlKey) == null) {
                            return null;
                        }
                        return t.withChildValue("url", IdentifyUnreachableRepos.KNOWN_DEFUNCT.get(urlKey));
                    }
                    return t;
                }).orElse(t);
            }
            return newTag;
        }

    }
}