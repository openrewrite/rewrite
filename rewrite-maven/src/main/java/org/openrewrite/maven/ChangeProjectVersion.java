/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeProjectVersion extends Recipe {
    // there are several implicitly defined version properties that we should never attempt to update
    private static final Collection<String> implicitlyDefinedVersionProperties = Arrays.asList(
            "${version}", "${project.version}", "${pom.version}", "${project.parent.version}"
    );

    @Option(displayName = "Group",
            description = "The group ID of the maven project to change its version. This can be a glob expression.",
            example = "org.openrewrite")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The artifact ID of the maven project to change its version. This can be a glob expression.",
            example = "*")
    String artifactId;

    @Option(displayName = "New version",
            description = "The new version to replace the maven project version.",
            example = "8.4.2")
    String newVersion;

    @Option(displayName = "Override Parent Version",
            description = "This flag can be set to explicitly override the inherited parent version. Default `false`.",
            required = false)
    @Nullable
    Boolean overrideParentVersion;

    @Override
    public String getDisplayName() {
        return "Change Maven Project Version";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", groupId, artifactId, newVersion);
    }

    @Override
    public String getDescription() {
        return "Change the project version of a Maven pom.xml. Identifies the project to be changed by its groupId and artifactId. " +
               "If the version is defined as a property, this recipe will only change the property value if the property exists within the same pom.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return new MavenIsoVisitor<ExecutionContext>() {
            private final XPathMatcher PROJECT_MATCHER = new XPathMatcher("/project");

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (PROJECT_MATCHER.matches(getCursor())) {
                    ResolvedPom resolvedPom = getResolutionResult().getPom();

                    if (matchesGlob(resolvedPom.getValue(t.getChildValue("groupId").orElse(null)), groupId) &&
                        matchesGlob(resolvedPom.getValue(t.getChildValue("artifactId").orElse(null)), artifactId)) {
                        Optional<Xml.Tag> versionTag = t.getChild("version");
                        if (versionTag.isPresent() && versionTag.get().getValue().isPresent()) {
                            String versionTagValue = versionTag.get().getValue().get();
                            String oldVersion = resolvedPom.getValue(versionTagValue);
                            assert oldVersion != null;

                            if (!oldVersion.equals(newVersion)) {
                                if (versionTagValue.startsWith("${") && !implicitlyDefinedVersionProperties.contains(versionTagValue)) {
                                    doAfterVisit(new ChangePropertyValue(versionTagValue.substring(2, versionTagValue.length() - 1), newVersion, false, false).getVisitor());
                                } else {
                                    doAfterVisit(new ChangeTagValueVisitor<>(versionTag.get(), newVersion));
                                }
                                maybeUpdateModel();
                            }
                        } else if (Boolean.TRUE.equals(overrideParentVersion)) {
                            // if the version is not present and the override parent version is set,
                            // add a new explicit version tag
                            Xml.Tag newVersionTag = Xml.Tag.build("<version>" + newVersion + "</version>");
                            doAfterVisit(new AddToTagVisitor<>(t, newVersionTag, new MavenTagInsertionComparator(t.getChildren())));
                            maybeUpdateModel();
                        }
                    }
                }
                return t;
            }
        };
    }
}
