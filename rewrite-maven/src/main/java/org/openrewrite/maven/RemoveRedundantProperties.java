/*
 * Copyright 2025 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Collections;

import static org.openrewrite.internal.StringUtils.isNullOrEmpty;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveRedundantProperties extends Recipe {
    @Option(displayName = "Property name",
            description = "Property name glob expression pattern used to match properties that should be checked.",
            example = "*.version",
            required = false)
    @Nullable
    String namePattern;

    @Option(displayName = "Only if values match",
            description = "Only remove the property if its value exactly matches the property value in the parent pom. " +
                    "Default `false`.",
            required = false)
    @Nullable
    Boolean onlyIfValuesMatch;

    public RemoveRedundantProperties(@Nullable String namePattern, @Nullable Boolean onlyIfValuesMatch) {
        this.namePattern = namePattern;
        this.onlyIfValuesMatch = onlyIfValuesMatch;
    }

    @Override
    public String getDisplayName() {
        return "Remove redundant properties";
    }

    @Override
    public String getDescription() {
        return "Remove properties when a parent POM specifies the same property.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.@Nullable Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (isMatchingPropertyTag(tag)) {
                    try {
                        String parentProperty = parentProperty(tag.getName(), ctx);
                        if (parentProperty == null) {
                            return t;
                        }
                        if (!Boolean.TRUE.equals(onlyIfValuesMatch) || valuesMatch(tag, parentProperty)) {
                            doAfterVisit(new RemoveContentVisitor<>(tag, true, true));
                            maybeUpdateModel();
                            return t;
                        }
                    } catch (MavenDownloadingException e) {
                        // just continue, try to do no harm on error
                        return t;
                    }
                }

                return t;
            }

            private boolean valuesMatch(Xml.Tag tag, String parentProperty) {
                return tag.getValue().map(tagValue -> tagValue.equals(parentProperty)).orElse(false);
            }

            private boolean isMatchingPropertyTag(Xml.Tag tag) {
                return isPropertyTag() &&
                        (isNullOrEmpty(namePattern) || matchesGlob(tag.getName(), namePattern));
            }

            private @Nullable String parentProperty(String propertyKey, ExecutionContext ctx) throws MavenDownloadingException {
                MavenResolutionResult mrr = getResolutionResult();
                MavenPomDownloader mpd = new MavenPomDownloader(
                        mrr.getProjectPoms(),
                        ctx,
                        mrr.getMavenSettings(),
                        mrr.getActiveProfiles()
                );

                ResolvedPom resolvedBarePom = mrr.getPom().getRequested()
                        .withProperties(Collections.emptyMap())
                        .withDependencies(Collections.emptyList())
                        .withDependencyManagement(Collections.emptyList())
                        .withPlugins(Collections.emptyList())
                        .withPluginManagement(Collections.emptyList())
                        .resolve(mrr.getActiveProfiles(), mpd, ctx);
                return resolvedBarePom.getProperties().get(propertyKey);
            }
        };
    }

}
