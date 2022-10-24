package org.openrewrite.maven;

/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import lombok.AllArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

public class IdentifyUnreachableRepos extends Recipe {
    private static final XPathMatcher REPOSITORY_URL_MATCHER = new XPathMatcher("/project/repositories/repository/url");
    private static final XPathMatcher PLUGIN_REPOSITORY_URL_MATCHER = new XPathMatcher("/project/pluginRepositories/pluginRepository/url");
    private static final XPathMatcher DISTRIBUTION_MANAGEMENT_REPOSITORY_URL_MATCHER = new XPathMatcher("/project/distributionManagement/repository/url");
    private static final XPathMatcher DISTRIBUTION_MANAGEMENT_SNAPSHOT_REPOSITORY_URL_MATCHER = new XPathMatcher("/project/distributionManagement/snapshotRepository/url");

    public static final Map<String, String> KNOWN_DEFUNCT;
    public static final Set<String> KNOWN_ACTIVE;

    static {
        // KNOWN_DEFUNCT is a growing list containing repositories that are known to be defunct
        Map<String, String> knownDefunct = new HashMap<>();
        knownDefunct.put("maven.glassfish.org/content/groups/public", "maven.java.net");
        knownDefunct.put("abcmaven.com", null);
        KNOWN_DEFUNCT = Collections.unmodifiableMap(knownDefunct);

        //KNOWN_ACTIVE is a (growing) list containing repositories that are known to always be active
        Set<String> knownNonDefunct = new HashSet<>();
        KNOWN_ACTIVE = Collections.unmodifiableSet(knownNonDefunct);
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
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UnreachableRepoMarkerVisitor();
    }

    private static class UnreachableRepoMarkerVisitor extends MavenIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            // if the tag is <...repository>, accordingly mark the repo URL if it is unreachable
            if (isRepositoryUrlTag()) {
                // get the url subtag
                String url = tag.getValue().orElse(null);
                if (url != null) {
                    if (url.startsWith("$")) {
                        String repositoryUrlProperty = tag.getValue().get();
                        doAfterVisit(new MarkPropertyLink(
                                repositoryUrlProperty.substring(2, repositoryUrlProperty.length() - 1)
                        ));
                        return tag;
                    } else {
                        // mark the url subtag
                        return mark(tag, ctx);
                    }
                }
            }
            return super.visitTag(tag, ctx);
        }

        private boolean isRepositoryUrlTag() {
            return REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    PLUGIN_REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    DISTRIBUTION_MANAGEMENT_REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    DISTRIBUTION_MANAGEMENT_SNAPSHOT_REPOSITORY_URL_MATCHER.matches(getCursor());
        }
    }


    @AllArgsConstructor
    private static class MarkPropertyLink extends MavenIsoVisitor<ExecutionContext> {
        @NonNull
        final String propertyName;

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isPropertyTag() && propertyName.equals(tag.getName())) {
                return mark(tag, ctx);
            }
            return super.visitTag(tag, ctx);
        }
    }

    private static Xml.Tag mark(Xml.Tag tag, ExecutionContext ctx) {
        HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getHttpSender();
        String url = tag.getValue().orElse(null);
        if (url != null) {
            String urlKey = httpOrHttps(url);
            if (KNOWN_ACTIVE.contains(urlKey)) {
                return tag;
            }
            if (KNOWN_DEFUNCT.containsKey(urlKey)) {
                // if we know of a replacement, mark it as "replacement"
                if (KNOWN_DEFUNCT.get(urlKey) != null) {
                    return tag.withMarkers(tag.getMarkers().searchResult("replacement"));
                }
                // otherwise, mark it as (potentially) "dead"
                if (KNOWN_DEFUNCT.get(urlKey) == null) {
                    return tag.withMarkers(tag.getMarkers().searchResult("dead"));
                }
            }
            try (HttpSender.Response response = httpSender.send(httpSender.get(url).build())) {
                if (!response.isSuccessful()) {
                    return tag.withMarkers(tag.getMarkers().searchResult("dead"));
                }
            } catch (Exception e) {
                // mark the repo url as "ambiguous"
                tag = tag.withMarkers(tag.getMarkers().searchResult("ambiguous"));
            }

        }
        return tag;
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
}

