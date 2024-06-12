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
import org.intellij.lang.annotations.Language;
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
@EqualsAndHashCode(callSuper = false)
public class AddPluginRepository extends Recipe {
    private static final XPathMatcher PLUGIN_REPOS_MATCHER = new XPathMatcher("/project/pluginRepositories");

    @Option(example = "repo-id", displayName = "Plugin repository ID",
            description = "A unique name to describe the plugin repository.")
    String id;

    @Option(example = "http://myrepo.maven.com/repo", displayName = "Plugin repository URL",
            description = "The URL of the plugin repository.")
    String url;

    @Option(example = "My Great Repo Name", required = false,
            displayName = "Plugin repository name",
            description = "A display name for the plugin repository.")
    @Nullable
    String pluginRepoName;

    @Option(example = "default", required = false,
            displayName = "Plugin repository layout",
            description = "The Maven layout of the plugin repository.")
    @Nullable
    String layout;

    @Option(required = false,
            displayName = "Enable snapshots",
            description = "Snapshots from the plugin repository are available.")
    @Nullable
    Boolean snapshotsEnabled;

    @Option(example = "warn", required = false,
            displayName = "Snapshots checksum policy",
            description = "Governs whether snapshots require checksums.")
    @Nullable
    String snapshotsChecksumPolicy;

    @Option(example = "always", required = false,
            displayName = "Snapshots update policy",
            description = "The policy governing snapshot updating interval.")
    @Nullable
    String snapshotsUpdatePolicy;

    @Option(required = false,
            displayName = "Releases enabled",
            description = "Releases from the plugin repository are available")
    @Nullable
    Boolean releasesEnabled;

    @Option(example = "fail", required = false,
            displayName = "Releases checksum policy",
            description = "Governs whether releases require checksums.")
    @Nullable
    String releasesChecksumPolicy;

    @Option(example = "never", required = false,
            displayName = "Releases update policy",
            description = "The policy governing release updating interval.")
    @Nullable
    String releasesUpdatePolicy;

    @Override
    public String getDisplayName() {
        return "Add plugin repository";
    }

    @Override
    public String getDescription() {
        return "Adds a new Maven Plugin Repository or updates a matching plugin repository.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Tag root = document.getRoot();
                if (!root.getChild("pluginRepositories").isPresent()) {
                    document = (Xml.Document) new AddToTagVisitor<>(root, Xml.Tag.build("<pluginRepositories/>"))
                            .visitNonNull(document, ctx, getCursor().getParentOrThrow());
                }
                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag pluginRepositories = super.visitTag(tag, ctx);

                if (PLUGIN_REPOS_MATCHER.matches(getCursor())) {
                    Optional<Xml.Tag> maybePluginRepo = pluginRepositories.getChildren().stream()
                            .filter(pluginRepo ->
                                    "pluginRepository".equals(pluginRepo.getName())
                                            && (id.equals(pluginRepo.getChildValue("id").orElse(null)) || (isReleasesEqual(pluginRepo) && isSnapshotsEqual(pluginRepo)))
                                            && url.equals(pluginRepo.getChildValue("url").orElse(null))
                            )
                            .findAny();

                    if (maybePluginRepo.isPresent()) {
                        Xml.Tag pluginRepo = maybePluginRepo.get();
                        if (pluginRepoName != null && !Objects.equals(pluginRepoName, pluginRepo.getChildValue("name").orElse(null))) {
                            //noinspection OptionalGetWithoutIsPresent
                            pluginRepositories = (Xml.Tag) new ChangeTagValueVisitor<>(pluginRepo.getChild("name").get(), pluginRepoName)
                                    .visitNonNull(pluginRepositories, ctx, getCursor().getParentOrThrow());
                        }
                        if (pluginRepoName != null && layout != null && !Objects.equals(layout, pluginRepo.getChildValue("layout").orElse(null))) {
                            //noinspection OptionalGetWithoutIsPresent
                            pluginRepositories = (Xml.Tag) new ChangeTagValueVisitor<>(pluginRepo.getChild("layout").get(), pluginRepoName)
                                    .visitNonNull(pluginRepositories, ctx, getCursor().getParentOrThrow());
                            maybeUpdateModel();
                        }
                        if (!isReleasesEqual(pluginRepo)) {
                            Xml.Tag releases = pluginRepo.getChild("releases").orElse(null);
                            if (releases == null) {
                                pluginRepositories = (Xml.Tag) new AddToTagVisitor<>(pluginRepo, Xml.Tag.build(assembleReleases()))
                                        .visitNonNull(pluginRepositories, ctx, getCursor().getParentOrThrow());
                            } else {
                                pluginRepositories = (Xml.Tag) new RemoveContentVisitor<>(releases, true)
                                        .visitNonNull(pluginRepositories, ctx, getCursor().getParentOrThrow());
                                if (!isNoSnapshots()) {
                                    pluginRepositories = (Xml.Tag) new AddToTagVisitor<>(pluginRepo, Xml.Tag.build(assembleReleases()))
                                            .visitNonNull(pluginRepositories, ctx, getCursor().getParentOrThrow());
                                }
                            }
                            maybeUpdateModel();
                        }
                        if (!isSnapshotsEqual(pluginRepo)) {
                            Xml.Tag snapshots = pluginRepo.getChild("snapshots").orElse(null);
                            if (snapshots == null) {
                                pluginRepositories = (Xml.Tag) new AddToTagVisitor<>(pluginRepo, Xml.Tag.build(assembleSnapshots())).visitNonNull(pluginRepositories, ctx, getCursor().getParentOrThrow());
                            } else {
                                pluginRepositories = (Xml.Tag) new RemoveContentVisitor<>(snapshots, true).visitNonNull(pluginRepositories, ctx, getCursor().getParentOrThrow());
                                if (!isNoSnapshots()) {
                                    pluginRepositories = (Xml.Tag) new AddToTagVisitor<>(pluginRepo, Xml.Tag.build(assembleSnapshots())).visitNonNull(pluginRepositories, ctx, getCursor().getParentOrThrow());
                                }
                            }
                            maybeUpdateModel();
                        }
                    } else {
                        @Language("xml")
                        String sb = "<pluginRepository>\n" +
                                assembleTagWithValue("id", id) +
                                assembleTagWithValue("url", url) +
                                assembleTagWithValue("name", pluginRepoName) +
                                assembleTagWithValue("layout", layout) +
                                assembleReleases() +
                                assembleSnapshots() +
                                "</pluginRepository>\n";

                        Xml.Tag pluginRepoTag = Xml.Tag.build(sb);
                        pluginRepositories = (Xml.Tag) new AddToTagVisitor<>(pluginRepositories, pluginRepoTag).visitNonNull(pluginRepositories, ctx, getCursor().getParentOrThrow());
                        maybeUpdateModel();
                    }
                }
                return pluginRepositories;
            }
        };
    }

    private String assembleTagWithValue(String tag, @Nullable String value) {
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

    private boolean isReleasesEqual(Xml.Tag pluginRepo) {
        Xml.Tag releases = pluginRepo.getChild("releases").orElse(null);
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

    private boolean isSnapshotsEqual(Xml.Tag pluginRepo) {
        Xml.Tag snapshots = pluginRepo.getChild("snapshots").orElse(null);
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
