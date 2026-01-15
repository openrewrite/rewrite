/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.*;
import org.openrewrite.xml.tree.Xml;

import static org.openrewrite.Validated.required;
import static org.openrewrite.Validated.test;
import static org.openrewrite.internal.StringUtils.isBlank;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeExclusion extends Recipe {

    @Option(displayName = "Old groupId",
            description = "The old groupId to replace. Supports glob expressions.",
            example = "org.springframework")
    String oldGroupId;

    @Option(displayName = "Old artifactId",
            description = "The old artifactId to replace. Supports glob expressions.",
            example = "spring-web*")
    String oldArtifactId;

    @Option(displayName = "New groupId",
            description = "The new groupId to use. Defaults to the existing group id.",
            example = "org.springframework.boot",
            required = false)
    @Nullable
    String newGroupId;

    @Option(displayName = "New artifactId",
            description = "The new artifactId to use. Defaults to the existing artifact id.",
            example = "spring-boot-starter-web",
            required = false)
    @Nullable
    String newArtifactId;

    @Override
    public String getDisplayName() {
        return "Change Maven dependency exclusion";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", oldGroupId, oldArtifactId);
    }

    @Override
    public String getDescription() {
        return "Modify Maven dependency exclusions, changing the group ID, artifact Id, or both. " +
                "Useful when an excluded dependency has been renamed and references to it must be updated.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        validated = validated.and(required("newGroupId", newGroupId).or(required("newArtifactId", newArtifactId)));
        return validated.and(test(
                "coordinates",
                "newGroupId OR newArtifactId must be different from before",
                this,
                r -> {
                    boolean sameGroupId = isBlank(r.newGroupId) || r.oldGroupId.equals(r.newGroupId);
                    boolean sameArtifactId = isBlank(r.newArtifactId) || r.oldArtifactId.equals(r.newArtifactId);
                    return !(sameGroupId && sameArtifactId);
                }
        ));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if ("exclusion".equals(t.getName())) {
                    if (matchesGlob(t.getChildValue("groupId").orElse(null), oldGroupId) &&
                        matchesGlob(t.getChildValue("artifactId").orElse(null), oldArtifactId)) {
                        if (newGroupId != null) {
                            t = changeChildTagValue(t, "groupId", newGroupId, ctx);
                        }
                        if (newArtifactId != null) {
                            t = changeChildTagValue(t, "artifactId", newArtifactId, ctx);
                        }
                    }
                }
                if (t != tag) {
                    maybeUpdateModel();
                }
                return t;
            }
        };
    }
}
