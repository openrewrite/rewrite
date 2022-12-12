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
package org.openrewrite.maven;

import lombok.AllArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoveDeadRepos extends Recipe {
    private static final XPathMatcher REPOSITORY_URL_MATCHER = new XPathMatcher("/project/repositories/repository");
    private static final XPathMatcher PLUGIN_REPOSITORY_URL_MATCHER = new XPathMatcher("/project/pluginRepositories/pluginRepository");
    private static final XPathMatcher DISTRIBUTION_MANAGEMENT_REPOSITORY_URL_MATCHER = new XPathMatcher("/project/distributionManagement/repository");
    private static final XPathMatcher DISTRIBUTION_MANAGEMENT_SNAPSHOT_REPOSITORY_URL_MATCHER = new XPathMatcher("/project/distributionManagement/snapshotRepository");

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

    static private final Set<String> propNamesToSearch = new HashSet<>();
    static private final Map<String, String> propNamesToValues = new HashMap<>();

    private static class RemoveDeadReposVisitor extends MavenIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isRepositoryTag()) {
                String url = tag.getChild("url").flatMap(Xml.Tag::getValue).orElse(null);
                if (url != null) {
                    // if the url is a property, check if the property's URL is in KNOWN_DEFUNCT; if so,
                    // replace/remove the property (if the property is removed, remove the repo as well)
                    if (url.matches("\\$\\{([^}]+)}")) {
                        String propertyName = url.substring(2, url.length() - 1);
                        doAfterVisit(new CheckPropertyLink(propertyName));
                        return tag;
                    } else if (url.matches(".*\\$\\{([^}]+)}.*")) {
                        // add all the property names found in getProperties(url) to propNamesToSearch
                        List<String> propertyNames = getProperties(url);
                        propNamesToSearch.addAll(propertyNames);
                        doAfterVisit(new GetProperties());
                        doAfterVisit(new RemoveDeadReposVisitorWithPropsInURL());
                        return tag;
                    }
                    // otherwise, simply check if the repo URL is in KNOWN_DEFUNCT; accordingly replace the
                    // repo's URL or delete this particular repo tag entirely
                    else {
                        return maybeDeleteOrReplaceUrlAndOrRepo(tag, url, false);
                    }
                }
            }
            return super.visitTag(tag, ctx);
        }

        private boolean isRepositoryTag() {
            return REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    PLUGIN_REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    DISTRIBUTION_MANAGEMENT_REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    DISTRIBUTION_MANAGEMENT_SNAPSHOT_REPOSITORY_URL_MATCHER.matches(getCursor());
        }

    }

    private static class RemoveDeadReposVisitorWithPropsInURL extends MavenIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isRepositoryTag()) {
                String url = tag.getChild("url").flatMap(Xml.Tag::getValue).orElse(null);
                if (url != null && url.matches(".*\\$\\{([^}]+)}.*") && !url.matches("\\$\\{([^}]+)}")) {
                    List<String> propertyNames = getProperties(url);
                    if (propertyNames.isEmpty()) {
                        return tag;
                    }
                    // replace the ith property name with the ith property value
                    for (String propertyName : propertyNames) {
                        url = url.replace("${" + propertyName + "}", propNamesToValues.get(propertyName));
                    }
                    return maybeDeleteOrReplaceUrlAndOrRepo(tag, url, false);
                }
            }
            return super.visitTag(tag, ctx);
        }

        private boolean isRepositoryTag() {
            return REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    PLUGIN_REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    DISTRIBUTION_MANAGEMENT_REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    DISTRIBUTION_MANAGEMENT_SNAPSHOT_REPOSITORY_URL_MATCHER.matches(getCursor());
        }

    }

    @AllArgsConstructor
    private static class CheckPropertyLink extends MavenIsoVisitor<ExecutionContext> {
        final String propertyName;

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isPropertyTag() && propertyName.equals(tag.getName())) {
                Xml.Tag newPropTag = maybeDeleteOrReplaceUrlAndOrRepo(tag, tag.getValue().orElse(""), true);
                if (newPropTag == null) {
                    doAfterVisit(new DeleteReposWithProperty(propertyName));
                }
                return newPropTag;
            }
            return super.visitTag(tag, ctx);
        }
    }

    @AllArgsConstructor
    private static class GetProperties extends MavenIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            // if propertyNames contains tag.getName(), set the mapping from tag.getName() to tag.getValue() in propNamesToValues
            if (isPropertyTag() && propNamesToSearch.contains(tag.getName())) {
                propNamesToValues.put(tag.getName(), tag.getValue().orElse(""));
            }
            return super.visitTag(tag, ctx);
        }
    }

    @AllArgsConstructor
    private static class DeleteReposWithProperty extends MavenIsoVisitor<ExecutionContext> {
        @NonNull
        final String propertyName;

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            // if the url tag within a repository tag uses property propertyName, delete the repository tag
            if (isRepositoryTag() &&
                    propertyName.equals(
                            tag.getChildValue("url")
                                    .map(u -> u.substring(2, u.length() - 1))
                                    .orElse(null))) {
                return null;
            }
            return super.visitTag(tag, ctx);
        }

        private boolean isRepositoryTag() {
            return REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    PLUGIN_REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    DISTRIBUTION_MANAGEMENT_REPOSITORY_URL_MATCHER.matches(getCursor()) ||
                    DISTRIBUTION_MANAGEMENT_SNAPSHOT_REPOSITORY_URL_MATCHER.matches(getCursor());
        }
    }


    private static Xml.Tag maybeDeleteOrReplaceUrlAndOrRepo(Xml.Tag tag, String url, boolean replacingPropertyOnly) {
        String urlKey = IdentifyUnreachableRepos.httpOrHttps(url);
        if (IdentifyUnreachableRepos.KNOWN_ACTIVE.contains(urlKey)) {
            return tag;
        }
        if (IdentifyUnreachableRepos.KNOWN_DEFUNCT.containsKey(urlKey)) {
            if (IdentifyUnreachableRepos.KNOWN_DEFUNCT.get(urlKey) == null) {
                return null;
            }
            // we assume here that https:// is a valid prefix for the replacement URL
            if (replacingPropertyOnly) {
                return tag.withValue("https://" + IdentifyUnreachableRepos.KNOWN_DEFUNCT.get(urlKey));
            }
            return tag.withChildValue("url", "https://" + IdentifyUnreachableRepos.KNOWN_DEFUNCT.get(urlKey));
        }
        return tag;
    }

    private static List<String> getProperties(String url) {
        List<String> props = new LinkedList<>();
        Matcher m = Pattern.compile("(?=(\\$\\{([^}]+)}))").matcher(url);
        while(m.find()) {
            props.add(m.group(1).substring(2, m.group(1).length() - 1));
        }
        return props;
    }
}
