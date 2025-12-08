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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangePluginGroupIdAndArtifactId extends Recipe {
    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Old group ID",
            description = "The old group ID to replace. The group ID is the first part of a plugin coordinate 'com.google.guava:guava:VERSION'. Supports glob expressions.",
            example = "org.openrewrite.recipe")
    String oldGroupId;

    @Option(displayName = "Old artifact ID",
            description = "The old artifactId to replace. The artifact ID is the second part of a plugin coordinate 'com.google.guava:guava:VERSION'. Supports glob expressions.",
            example = "my-deprecated-maven-plugin")
    String oldArtifactId;

    @Option(displayName = "New group ID",
            description = "The new group ID to use.",
            example = "corp.internal.openrewrite.recipe",
            required = false)
    @Nullable
    String newGroupId;

    @Option(displayName = "New artifact ID",
            description = "The new artifact ID to use.",
            example = "my-new-maven-plugin",
            required = false)
    @Nullable
    String newArtifactId;

    @Option(displayName = "New version",
            description = "An exact version number.",
            example = "29.0",
            required = false)
    @Nullable
    String newVersion;

    @Override
    public String getDisplayName() {
        return "Change Maven plugin group and artifact ID";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", newGroupId, newArtifactId);
    }

    @Override
    public String getDescription() {
        return "Change the groupId and/or the artifactId of a specified Maven plugin. Optionally update the plugin version. " +
                "This recipe does not perform any validation and assumes all values passed are valid.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (isPluginTag(oldGroupId, oldArtifactId)) {
                    if (newGroupId != null) {
                        t = changeChildTagValue(t, "groupId", newGroupId, ctx);
                    }
                    if (newArtifactId != null) {
                        t = changeChildTagValue(t, "artifactId", newArtifactId, ctx);
                    }
                    if (newVersion != null) {
                        t = changeChildTagValue(t, "version", newVersion, ctx);
                    }
                    if (t != tag) {
                        maybeUpdateModel();
                    }
                }
                //noinspection ConstantConditions
                return t;
            }
        };
    }
}
