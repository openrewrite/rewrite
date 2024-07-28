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
package org.openrewrite.maven.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenTagInsertionComparator;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class ExplicitPluginGroupId extends Recipe {
    @Override
    public String getDisplayName() {
        return "Add explicit `groupId` to Maven plugins";
    }

    @Override
    public String getDescription() {
        return "Add the default `<groupId>org.apache.maven.plugins</groupId>` to plugins for clarity.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (isPluginTag() && !t.getChild("groupId").isPresent()) {
                    Xml.Tag groupIdTag = Xml.Tag.build("<groupId>org.apache.maven.plugins</groupId>");
                    t = (Xml.Tag) new AddToTagVisitor<>(t, groupIdTag, new MavenTagInsertionComparator(t.getChildren()))
                            .visitNonNull(t, ctx, getCursor().getParentOrThrow());
                    maybeUpdateModel();
                }
                return t;
            }
        };
    }
}
