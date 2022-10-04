package org.openrewrite.xml;

import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.xml.tree.Xml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IdentifyUnreachableRepos extends Recipe {

    public static final Map<String, String> KNOWN_DEFUNCT;
    static {
        Map<String, String> knownDefunct = new HashMap<>();
        knownDefunct.put("maven.glassfish.org/content/groups/public", "maven.java.net");
        KNOWN_DEFUNCT = Collections.unmodifiableMap(knownDefunct);
    }

    protected static String httpOrHttps(String url) {
        if (url.startsWith("http://")) {
            return url.substring(7);
        }
        if (url.startsWith("https://")) {
            return url.substring(8);
        }
        return url;
    }

    @Override
    public String getDisplayName() {
        return "Identify potentially dead repositories";
    }

    @Override
    public String getDescription() {
        return "Identify remote repositories that are not responding to an HTTP/HTTPS request.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new IdentifyDeadReposVisitor();
    }

    private static class IdentifyDeadReposVisitor extends XmlIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getHttpSender();
            if (t.getName().equals("url")) {
                String url = t.getValue().orElse(null);
                if (url != null) {
                    String urlKey = httpOrHttps(url);
                    if (KNOWN_DEFUNCT.containsKey(urlKey)) {
                        // if we know of a replacement, mark it as "replacement"
                        if (KNOWN_DEFUNCT.get(urlKey) != null) {
                            return t.withMarkers(t.getMarkers().searchResult("replacement"));
                        }
                        // otherwise, mark it as (potentially) "dead"
                        return t.withMarkers(t.getMarkers().searchResult("dead"));
                    }
                    try (HttpSender.Response response = httpSender.send(httpSender.get(url).build())) {
                        if (!response.isSuccessful()) {
                            return t.withMarkers(t.getMarkers().searchResult("dead"));
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        // mark the repo url as "ambiguous"
                        t = t.withMarkers(t.getMarkers().searchResult("ambiguous"));
                    }

                }
            }
            return t;
        }
    }
}