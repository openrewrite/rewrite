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
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangePluginGroupIdAndArtifactId extends AbstractChangeGroupIdAndArtifactId {

    public ChangePluginGroupIdAndArtifactId(String oldGroupId, String oldArtifactId, String newGroupId, String newArtifactId) {
        super(oldGroupId, oldArtifactId, newGroupId, newArtifactId);
    }

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
        return "Change the groupId and/or the artifactId of a specified Maven plugin.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return new AbstractChangeGroupIdAndArtifactIdVisitor() {
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
