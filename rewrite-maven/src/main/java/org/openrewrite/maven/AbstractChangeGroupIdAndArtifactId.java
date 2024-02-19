/*
 * Copyright 2021 the original author or authors.
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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import lombok.EqualsAndHashCode;

abstract class AbstractChangeGroupIdAndArtifactId extends Recipe {
    @EqualsAndHashCode.Exclude
    protected transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Old groupId",
            description = "The old groupId to replace. The groupId is the first part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob expressions.",
            example = "org.openrewrite.recipe")
    String oldGroupId;

    @Option(displayName = "Old artifactId",
            description = "The old artifactId to replace. The artifactId is the second part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob expressions.",
            example = "rewrite-testing-frameworks")
    String oldArtifactId;

    @Option(displayName = "New groupId",
            description = "The new groupId to use. Defaults to the existing group id.",
            example = "corp.internal.openrewrite.recipe")
    String newGroupId;

    @Option(displayName = "New artifactId",
            description = "The new artifactId to use. Defaults to the existing artifact id.",
            example = "rewrite-testing-frameworks")
    String newArtifactId;

    protected AbstractChangeGroupIdAndArtifactId(String oldGroupId, String oldArtifactId, String newGroupId, String newArtifactId) {
        this.oldGroupId = oldGroupId;
        this.oldArtifactId = oldArtifactId;
        this.newGroupId = newGroupId;
        this.newArtifactId = newArtifactId;
    }

    protected abstract static class AbstractChangeGroupIdAndArtifactIdVisitor extends MavenVisitor<ExecutionContext> {
        protected Xml.Tag changeChildTagValue(final Xml.Tag tag, String childTagName, String newValue, ExecutionContext ctx) {
            return tag.getChild(childTagName).map(childTagValue -> {
                if (!newValue.equals(childTagValue.getValue().orElse(null))) {
                    return  (Xml.Tag) new ChangeTagValueVisitor<>(childTagValue, newValue).visitNonNull(tag, ctx);
                }
                return tag;
            }).orElse(tag);
        }
    }
}