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
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeProjectVersion extends Recipe {
    // there are several implicitly defined version properties that we should never attempt to update
    private static final Collection<String> implicitlyDefinedVersionProperties = Arrays.asList(
            "${version}", "${project.version}", "${pom.version}", "${project.parent.version}"
    );

    @Override
    public String getDisplayName() {
        return "Change Maven Project Version";
    }

    @Override
    public String getDescription() {
        return "Change the project version of a Maven pom.xml. Identifies the project to be changed by its groupId and artifactId. " +
               "If the version is defined as a property, this recipe will only change the property value if the property exists within the same pom.";
    }

    @Option(displayName = "GroupId",
            description = "The groupId of the maven project to change it's version. This can be a glob expression.",
            example = "org.openrewrite")
    String groupId;

    @Option(displayName = "ArtifactId",
            description = "The artifactId of the maven project to change it's version. This can be a glob expression.",
            example = "rewrite-maven")
    String artifactId;

    @Option(displayName = "New version",
            description = "The new version to replace the maven project version.",
            example = "8.4.2")
    String newVersion;

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
                        assert versionTag.isPresent() && versionTag.get().getValue().isPresent();
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
                    }
                }
                return t;
            }
        };
    }
}
