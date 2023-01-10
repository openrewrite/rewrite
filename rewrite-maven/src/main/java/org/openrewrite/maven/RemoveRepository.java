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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Objects;
import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveRepository extends Recipe {

    private static final XPathMatcher REPOS_MATCHER = new XPathMatcher("/project/repositories/repository");

    @Option(required = false, description = "Repository id")
    @Nullable
    private String id;

    @Option(description = "Repository URL")
    private String url;

    @Option(required = false, description = "Repository name")
    @Nullable
    private String repoName;

    @Option(required = false, description = "Repository layout")
    @Nullable
    private String layout;

    @Option(required = false, description = "Snapshots from the repository are available")
    @Nullable
    private Boolean snapshotsEnabled;

    @Option(required = false, description = "Snapshots checksum policy")
    @Nullable
    private String snapshotsChecksumPolicy;

    @Option(required = false, description = "Snapshots update policy policy")
    @Nullable
    private String snapshotsUpdatePolicy;

    @Option(required = false, description = "Releases from the repository are available")
    @Nullable
    private Boolean releasesEnabled;

    @Option(required = false, description = "Releases checksum policy")
    @Nullable
    private String releasesChecksumPolicy;

    @Option(required = false, description = "Releases update policy")
    @Nullable
    private String releasesUpdatePolicy;

    @Override
    public String getDisplayName() {
        return "Remove repository";
    }

    @Override
    public String getDescription() {
        return "Removes a matching Maven repository.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag repo = super.visitTag(tag, ctx);

                if (REPOS_MATCHER.matches(getCursor())) {
                    if (id == null && (isReleasesEqual(repo) && isSnapshotsEqual(repo)) &&
                                        url.equals(repo.getChildValue("url").orElse(null))) {
                        return null;
                    } else if (id != null && (id.equals(repo.getChildValue("id").orElse(null)) || (isReleasesEqual(repo) && isSnapshotsEqual(repo))) &&
                                url.equals(repo.getChildValue("url").orElse(null))) {
                        return null;
                    }
                }
                return repo;
            }
        };
    }

    private boolean isReleasesEqual(Xml.Tag repo) {
        Xml.Tag releases = repo.getChild("releases").orElse(null);
        if (releases == null) {
            return isNoReleases();
        } else {
            return Objects.equals(releasesEnabled == null ? null : String.valueOf(releasesEnabled.booleanValue()), releases.getChildValue("enabled").orElse(null))
                    && Objects.equals(releasesUpdatePolicy, releases.getChildValue("updatePolicy").orElse(null))
                    && Objects.equals(releasesChecksumPolicy, releases.getChildValue("checksumPolicy").orElse(null));
        }
    }

    private boolean isNoReleases() {
        return releasesEnabled == null && releasesUpdatePolicy == null && releasesChecksumPolicy == null;
    }

    private boolean isSnapshotsEqual(Xml.Tag repo) {
        Xml.Tag snapshots = repo.getChild("snapshots").orElse(null);
        if (snapshots == null) {
            return isNoSnapshots();
        } else {
            return Objects.equals(snapshotsEnabled == null ? null : String.valueOf(snapshotsEnabled.booleanValue()), snapshots.getChildValue("enabled").orElse(null))
                    && Objects.equals(snapshotsUpdatePolicy, snapshots.getChildValue("updatePolicy").orElse(null))
                    && Objects.equals(snapshotsChecksumPolicy, snapshots.getChildValue("checksumPolicy").orElse(null));
        }
    }

    private boolean isNoSnapshots() {
        return snapshotsEnabled == null && snapshotsUpdatePolicy == null && snapshotsChecksumPolicy == null;
    }

}
