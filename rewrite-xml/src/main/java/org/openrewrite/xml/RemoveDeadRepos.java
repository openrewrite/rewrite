package org.openrewrite.xml;

import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.xml.tree.Xml;

public class IdentifyDeadRepos extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove dead repositories";
    }

    @Override
    public String getDescription() {
        return "Remove remote repositories that are not responding.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveDeadReposVisitor();
    }

    private static class RemoveDeadReposVisitor extends XmlIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getHttpSender();
            if (t.getName().equals("url")) {
                String url = t.getValue().orElse(null);
                if (url != null) {
                    try (HttpSender.Response response = httpSender.send(httpSender.get(url).build())) {
                        if (!response.isSuccessful()) {
                            // mark the repo url as "dead"
                            t = t.withMarkers(t.getMarkers().searchResult("dead"));
                        }
                    } catch (Exception e) {
                        // mark the repo url as "ambiguous"
                        t = t.withMarkers(t.getMarkers().searchResult("ambiguous"));
                    }

                }
            }
            return t;
        }
    }
}