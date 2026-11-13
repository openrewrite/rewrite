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
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.marker.AlreadyIncremented;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.tree.Xml;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@EqualsAndHashCode(callSuper = false)
public class IncrementProjectVersion extends ScanningRecipe<Map<GroupArtifact, IncrementProjectVersion.VersionChange>> {

    @Option(displayName = "Group",
            description = "The group ID of the Maven project to change its version. This can be a glob expression.",
            example = "org.openrewrite")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The artifact ID of the Maven project to change its version. This can be a glob expression.",
            example = "*")
    String artifactId;

    @Option(displayName = "Semver digit",
            description = "`MAJOR` increments the first digit, `MINOR` increments the second digit, and `PATCH` " +
                          "increments the third digit.",
            example = "PATCH")
    SemverDigit digit;

    public enum SemverDigit {
        MAJOR,
        MINOR,
        PATCH
    }

    @Value
    public static class VersionChange {
        String oldVersion;
        String newVersion;
    }

    String displayName = "Increment Maven project version";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", groupId, artifactId, digit);
    }

    String description = "Increase Maven project version by incrementing either the major, minor, or patch version as defined by " +
               "[semver](https://semver.org/). Other versioning schemes are not supported.";

    @Override
    public Map<GroupArtifact, VersionChange> getInitialValue(ExecutionContext ctx) {
        return new HashMap<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<GroupArtifact, VersionChange> acc) {
        final Pattern SEMVER_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)\\.?(\\d+)?(-.+)?$");

        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (!isProjectTag() || t.getMarkers().findFirst(AlreadyIncremented.class).isPresent()) {
                    return t;
                }
                ResolvedPom resolvedPom = getResolutionResult().getPom();
                if (!(matchesGlob(resolvedPom.getValue(t.getChildValue("groupId").orElse(null)), groupId) &&
                      matchesGlob(resolvedPom.getValue(t.getChildValue("artifactId").orElse(null)), artifactId))) {
                    return t;
                }
                Optional<Xml.Tag> versionTag = t.getChild("version");
                if (!(versionTag.isPresent() && versionTag.get().getValue().isPresent())) {
                    return t;
                }
                String versionTagValue = versionTag.get().getValue().get();
                String oldVersion = resolvedPom.getValue(versionTagValue);
                if (oldVersion == null) {
                    return t;
                }
                String newVersion = incrementSemverDigit(oldVersion);
                if (newVersion.equals(oldVersion)) {
                    return t;
                }
                acc.put(new GroupArtifact(resolvedPom.getGroupId(), resolvedPom.getArtifactId()),
                        new VersionChange(oldVersion, newVersion));
                return t;
            }

            private String incrementSemverDigit(String oldVersion) {
                Matcher m = SEMVER_PATTERN.matcher(oldVersion);
                if (!m.matches()) {
                    return oldVersion;
                }
                String major = m.group(1);
                String minor = m.group(2);
                String patch = m.group(3);
                // Semver does not have a concept of a fourth number, but it is common enough to support
                String fourth = m.group(4);
                String extra = m.group(5);
                switch (digit) {
                    case MAJOR:
                        major = String.valueOf(Integer.parseInt(major) + 1);
                        minor = "0";
                        patch = "0";
                        break;
                    case MINOR:
                        minor = String.valueOf(Integer.parseInt(minor) + 1);
                        patch = "0";
                        break;
                    case PATCH:
                        patch = String.valueOf(Integer.parseInt(patch) + 1);
                        break;
                }
                if (fourth == null) {
                    fourth = "";
                } else {
                    fourth = ".0";
                }
                if (extra == null) {
                    extra = "";
                }
                return major + "." + minor + "." + patch + fourth + extra;
            }
        };

    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Map<GroupArtifact, VersionChange> acc) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (isProjectTag() || isParentTag()) {
                    return incrementProjectVersion(t);
                }
                if (isDependencyLikeTag()) {
                    return incrementSiblingVersion(t);
                }
                return t;
            }

            private Xml.Tag incrementProjectVersion(Xml.Tag t) {
                if (t.getMarkers().findFirst(AlreadyIncremented.class).isPresent()) {
                    return t;
                }
                ResolvedPom resolvedPom = getResolutionResult().getPom();
                VersionChange change = isProjectTag() ?
                        acc.get(new GroupArtifact(resolvedPom.getGroupId(), resolvedPom.getArtifactId())) :
                        acc.get(groupArtifact(t));
                if (change == null || change.getNewVersion().equals(t.getChildValue("version").orElse(null))) {
                    return t;
                }
                return t.withChildValue("version", change.getNewVersion())
                        .withMarkers(t.getMarkers().add(new AlreadyIncremented(randomId())));
            }

            private Xml.Tag incrementSiblingVersion(Xml.Tag t) {
                VersionChange change = acc.get(groupArtifact(t));
                if (change == null || !change.getOldVersion().equals(t.getChildValue("version").orElse(null))) {
                    return t;
                }
                return t.withChildValue("version", change.getNewVersion());
            }

            private GroupArtifact groupArtifact(Xml.Tag t) {
                ResolvedPom resolvedPom = getResolutionResult().getPom();
                return new GroupArtifact(
                        resolvedPom.getValue(t.getChildValue("groupId").orElse(null)),
                        resolvedPom.getValue(t.getChildValue("artifactId").orElse(null)));
            }
        };
    }
}
