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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

import static org.openrewrite.xml.AddOrUpdateChild.addOrUpdateChild;
import static org.openrewrite.xml.FilterTagChildrenVisitor.filterTagChildren;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseMavenCompilerPluginReleaseConfiguration extends Recipe {
    private static final XPathMatcher PLUGINS_MATCHER = new XPathMatcher("/project//build//plugins");

    @Option(
            displayName = "Release version",
            description = "The new value for the release configuration. This recipe prefers ${java.version} if defined.",
            example = "11"
    )
    Integer releaseVersion;

    @Override
    public String getDisplayName() {
        return "Use Maven compiler plugin release configuration";
    }

    @Override
    public String getDescription() {
        return "Replaces any explicit `source` or `target` configuration (if present) on the `maven-compiler-plugin` with " +
                "`release`, and updates the `release` value if needed. Will not downgrade the Java version if the current version is higher.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (!isPluginTag("org.apache.maven.plugins", "maven-compiler-plugin")) {
                    return t;
                }
                Optional<Xml.Tag> maybeCompilerPluginConfig = t.getChild("configuration");
                if (!maybeCompilerPluginConfig.isPresent()) {
                    return t;
                }
                Xml.Tag compilerPluginConfig = maybeCompilerPluginConfig.get();
                Optional<String> source = compilerPluginConfig.getChildValue("source");
                Optional<String> target = compilerPluginConfig.getChildValue("target");
                Optional<String> release = compilerPluginConfig.getChildValue("release");
                if (!source.isPresent() && !target.isPresent() && !release.isPresent()) {
                    return t; // Do not introduce a new tag if none of the values are present
                }
                if (currentNewerThanProposed(source) ||
                        currentNewerThanProposed(target) ||
                        currentNewerThanProposed(release)) {
                    return t;
                }

                Xml.Tag updated = filterTagChildren(t, compilerPluginConfig,
                        child -> !("source".equals(child.getName()) || "target".equals(child.getName())));
                String releaseVersionValue = hasJavaVersionProperty(getCursor().firstEnclosingOrThrow(Xml.Document.class)) ?
                        "${java.version}" : releaseVersion.toString();
                return addOrUpdateChild(updated, compilerPluginConfig,
                        Xml.Tag.build("<release>" + releaseVersionValue + "</release>"), getCursor().getParentOrThrow());
            }
        };
    }

    private boolean currentNewerThanProposed(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String> config) {
        if (!config.isPresent()) {
            return false;
        }
        try {
            float currentVersion = Float.parseFloat(config.get());
            float proposedVersion = Float.parseFloat(releaseVersion.toString());
            return proposedVersion < currentVersion;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean hasJavaVersionProperty(Xml.Document xml) {
        return xml.getMarkers().findFirst(MavenResolutionResult.class)
                .map(r -> r.getPom().getProperties().get("java.version") != null)
                .orElse(false);
    }
}
