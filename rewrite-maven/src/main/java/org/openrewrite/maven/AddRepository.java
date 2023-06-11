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
public class AddRepository extends Recipe {

    private static final XPathMatcher REPOS_MATCHER = new XPathMatcher("/project/repositories");

    @Option(description = "Repository id")
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
        return "Add Repository";
    }

    @Override
    public String getDescription() {
        return "Adds a new Maven Repository or Update a matching repository.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Tag root = document.getRoot();
                if (!root.getChild("repositories").isPresent()) {
                    document = (Xml.Document) new AddToTagVisitor<>(root, Xml.Tag.build("<repositories/>"))
                            .visitNonNull(document, ctx, getCursor().getParentOrThrow());
                }
                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag repositories = super.visitTag(tag, ctx);

                if (REPOS_MATCHER.matches(getCursor())) {
                    Optional<Xml.Tag> maybeRepo = repositories.getChildren().stream()
                            .filter(repo ->
                                    "repository".equals(repo.getName()) &&
                                            (id.equals(repo.getChildValue("id").orElse(null)) || (isReleasesEqual(repo) && isSnapshotsEqual(repo))) &&
                                            url.equals(repo.getChildValue("url").orElse(null))
                            )
                            .findAny();

                    if (maybeRepo.isPresent()) {
                        Xml.Tag repo = maybeRepo.get();
                        if (repoName != null && !Objects.equals(repoName, repo.getChildValue("name").orElse(null))) {
                            if (repoName == null) {
                                repositories = (Xml.Tag) new RemoveContentVisitor<>(repo.getChild("name").get(), true).visitNonNull(repositories, ctx, getCursor().getParentOrThrow());
                            } else {
                                repositories = (Xml.Tag) new ChangeTagValueVisitor<>(repo.getChild("name").get(), repoName).visitNonNull(repositories, ctx, getCursor().getParentOrThrow());
                            }
                        }
                        if (layout != null && !Objects.equals(layout, repo.getChildValue("layout").orElse(null))) {
                            if (layout == null) {
                                repositories = (Xml.Tag) new RemoveContentVisitor<>(repo.getChild("layout").get(), true).visitNonNull(repositories, ctx, getCursor().getParentOrThrow());
                            } else {
                                repositories = (Xml.Tag) new ChangeTagValueVisitor<>(repo.getChild("layout").get(), repoName).visitNonNull(repositories, ctx, getCursor().getParentOrThrow());
                            }
                            maybeUpdateModel();
                        }
                        if (!isReleasesEqual(repo)) {
                            Xml.Tag releases = repo.getChild("releases").orElse(null);
                            if (releases == null) {
                                repositories = (Xml.Tag) new AddToTagVisitor<>(repo, Xml.Tag.build(assembleReleases())).visitNonNull(repositories, ctx, getCursor().getParentOrThrow());
                            } else {
                                repositories = (Xml.Tag) new RemoveContentVisitor<>(releases, true).visitNonNull(repositories, ctx, getCursor().getParentOrThrow());
                                if (!isNoSnapshots()) {
                                    repositories = (Xml.Tag) new AddToTagVisitor<>(repo, Xml.Tag.build(assembleReleases())).visitNonNull(repositories, ctx, getCursor().getParentOrThrow());
                                }
                            }
                            maybeUpdateModel();
                        }
                        if (!isSnapshotsEqual(repo)) {
                            Xml.Tag snapshots = repo.getChild("snapshots").orElse(null);
                            if (snapshots == null) {
                                repositories = (Xml.Tag) new AddToTagVisitor<>(repo, Xml.Tag.build(assembleSnapshots())).visitNonNull(repositories, ctx, getCursor().getParentOrThrow());
                            } else {
                                repositories = (Xml.Tag) new RemoveContentVisitor<>(snapshots, true).visitNonNull(repositories, ctx, getCursor().getParentOrThrow());
                                if (!isNoSnapshots()) {
                                    repositories = (Xml.Tag) new AddToTagVisitor<>(repo, Xml.Tag.build(assembleSnapshots())).visitNonNull(repositories, ctx, getCursor().getParentOrThrow());
                                }
                            }
                            maybeUpdateModel();
                        }
                    } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append("<repository>\n");
                        sb.append(assembleTagWithValue("id", id));
                        sb.append(assembleTagWithValue("url", url));
                        sb.append(assembleTagWithValue("name", repoName));
                        sb.append(assembleTagWithValue("layout", layout));
                        sb.append(assembleReleases());
                        sb.append(assembleSnapshots());
                        sb.append("</repository>\n");

                        Xml.Tag repoTag = Xml.Tag.build(sb.toString());
                        repositories = (Xml.Tag) new AddToTagVisitor<>(repositories, repoTag).visitNonNull(repositories, ctx, getCursor().getParentOrThrow());
                        maybeUpdateModel();
                    }
                }
                return repositories;
            }
        };
    }

    private String assembleTagWithValue(String tag, String value) {
        StringBuilder sb = new StringBuilder();
        if (value != null) {
            sb.append("<");
            sb.append(tag);
            sb.append(">");
            sb.append(value);
            sb.append("</");
            sb.append(tag);
            sb.append(">\n");
        }
        return sb.toString();
    }

    private String assembleReleases() {
        StringBuilder sb = new StringBuilder();
        if (releasesUpdatePolicy != null || releasesEnabled != null || releasesChecksumPolicy != null) {
            sb.append("<releases>");
            if (releasesEnabled != null) {
                sb.append(assembleTagWithValue("enabled", String.valueOf(releasesEnabled.booleanValue())));
            }
            if (releasesUpdatePolicy != null) {
                sb.append(assembleTagWithValue("updatePolicy", releasesUpdatePolicy));
            }
            if (releasesChecksumPolicy != null) {
                sb.append(assembleTagWithValue("checksumPolicy", releasesChecksumPolicy));
            }
            sb.append("</releases>\n");
        }
        return sb.toString();
    }

    private String assembleSnapshots() {
        StringBuilder sb = new StringBuilder();
        if (snapshotsEnabled != null || snapshotsChecksumPolicy != null || snapshotsUpdatePolicy != null) {
            sb.append("<snapshots>");
            if (snapshotsEnabled != null) {
                sb.append(assembleTagWithValue("enabled", String.valueOf(snapshotsEnabled.booleanValue())));
            }
            if (snapshotsUpdatePolicy != null) {
                sb.append(assembleTagWithValue("updatePolicy", snapshotsUpdatePolicy));
            }
            if (snapshotsChecksumPolicy != null) {
                sb.append(assembleTagWithValue("checksumPolicy", snapshotsChecksumPolicy));
            }
            sb.append("</snapshots>\n");
        }
        return sb.toString();
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
