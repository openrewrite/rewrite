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
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

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
            description = "The new group ID to use. Defaults to the existing group ID.",
            example = "corp.internal.openrewrite.recipe",
            required = false)
    @Nullable
    String newGroupId;

    @Option(displayName = "New artifact ID",
            description = "The new artifact ID to use. Defaults to the existing artifact ID.",
            example = "my-new-maven-plugin",
            required = false)
    @Nullable
    String newArtifact;

    @Override
    public String getDisplayName() {
        return "Change Maven plugin group and artifact ID";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", newGroupId, newArtifact);
    }

    @Override
    public String getDescription() {
        return "Change the groupId and/or the artifactId of a specified Maven plugin.";
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
                    if (newArtifact != null) {
                        t = changeChildTagValue(t, "artifactId", newArtifact, ctx);
                    }
                    if (t != tag) {
                        maybeUpdateModel();
                    }
                }
                //noinspection ConstantConditions
                return t;
            }

            private Xml.Tag changeChildTagValue(Xml.Tag tag, String childTagName, String newValue, ExecutionContext ctx) {
                Optional<Xml.Tag> childTag = tag.getChild(childTagName);
                if (childTag.isPresent() && !newValue.equals(childTag.get().getValue().orElse(null))) {
                    tag = (Xml.Tag) new ChangeTagValueVisitor<>(childTag.get(), newValue).visitNonNull(tag, ctx);
                }
                return tag;
            }
        };
    }
}
