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
package org.openrewrite.maven.cleanup;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.trait.MavenDependency;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.xml.tree.Xml;

public class ExplicitDependencyVersion extends Recipe {
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Getter
    final String displayName = "Add explicit dependency versions";

    @Getter
    final String description = "Add explicit dependency versions to POMs for reproducibility, as the `LATEST` and `RELEASE` version keywords are deprecated.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        LatestRelease latestRelease = new LatestRelease(null);
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (isDependencyTag() || isManagedDependencyTag()) {
                    String versionValue = getResolutionResult().getPom().getValue(t.getChildValue("version").orElse(null));
                    if ("LATEST".equalsIgnoreCase(versionValue) || "RELEASE".equalsIgnoreCase(versionValue)) {
                        String groupId = getResolutionResult().getPom().getValue(t.getChildValue("groupId").orElse(null));
                        String artifactId = getResolutionResult().getPom().getValue(t.getChildValue("artifactId").orElse(null));
                        if (groupId != null && artifactId != null) {
                            try {
                                String newerVersion = MavenDependency.findNewerVersion(
                                        groupId,
                                        artifactId,
                                        null, // current version is not valid semver
                                        getResolutionResult(),
                                        metadataFailures,
                                        latestRelease,
                                        ctx
                                );
                                if (newerVersion != null) {
                                    t = changeChildTagValue(t, "version", newerVersion, ctx);
                                    maybeUpdateModel();
                                }
                            } catch (MavenDownloadingException e) {
                                return e.warn(t);
                            }
                        }
                    }
                }
                return t;
            }
        };
    }
}
