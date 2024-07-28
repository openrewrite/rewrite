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
package org.openrewrite.maven.security;

import lombok.AllArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UseHttpsForRepositories extends Recipe {
    private static final XPathMatcher REPOSITORY_URL_MATCHER = new XPathMatcher("/project/repositories/repository/url");
    private static final XPathMatcher PLUGIN_REPOSITORY_URL_MATCHER = new XPathMatcher("/project/pluginRepositories/pluginRepository/url");
    private static final XPathMatcher DISTRIBUTION_MANAGEMENT_REPOSITORY_URL_MATCHER = new XPathMatcher("/project/distributionManagement/repository/url");
    private static final XPathMatcher DISTRIBUTION_MANAGEMENT_SNAPSHOT_REPOSITORY_URL_MATCHER = new XPathMatcher("/project/distributionManagement/snapshotRepository/url");

    @Override
    public String getDisplayName() {
        return "Use HTTPS for repositories";
    }

    @Override
    public String getDescription() {
        return "Use HTTPS for repository URLs.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("security", "CWE-829"));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isRepositoryUrlTag()) {
                    if (isInsecureTag(tag)) {
                        @SuppressWarnings("OptionalGetWithoutIsPresent")
                        String newValue = replaceInsecure(tag.getValue().get());
                        doAfterVisit(new ChangeTagValueVisitor<>(tag, newValue));
                    } else if (tag.getValue().map(v -> v.startsWith("$")).orElse(false)) {
                        String repositoryUrlProperty = tag.getValue().get();
                        doAfterVisit(new UpdateMavenPropertyToHttpsVisitor(
                            repositoryUrlProperty.substring(2, repositoryUrlProperty.length() - 1)
                        ));
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
        };
    }

    @AllArgsConstructor
    static class UpdateMavenPropertyToHttpsVisitor extends MavenIsoVisitor<ExecutionContext> {
        final String propertyName;

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isPropertyTag() && propertyName.equals(tag.getName()) && isInsecureTag(tag)) {
                @SuppressWarnings("OptionalGetWithoutIsPresent")
                String newValue = replaceInsecure(tag.getValue().get());
                doAfterVisit(new ChangeTagValueVisitor<>(tag, newValue));
            }
            return super.visitTag(tag, ctx);
        }
    }

    private static boolean isInsecureTag(Xml.Tag tag) {
        return tag.getValue().map(UseHttpsForRepositories::isInsecure).orElse(false);
    }

    @SuppressWarnings("HttpUrlsUsage")
    private static boolean isInsecure(String value) {
        return value.startsWith("http://") || value.startsWith("ftp://");
    }

    private static String replaceInsecure(String value) {
        return value.replaceAll("^http://(.*)", "https://$1")
            .replaceAll("^ftp://(.*)", "ftps://$1");
    }
}
