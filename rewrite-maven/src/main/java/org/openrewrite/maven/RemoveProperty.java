/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveProperty extends Recipe {

    @Option(displayName = "Property name",
            description = "Key name of the property to remove.",
            example = "junit.version")
    String propertyName;

    @Override
    public String getDisplayName() {
        return "Remove Maven project property";
    }

    @Override
    public String getDescription() {
        return "Removes the specified Maven project property from the pom.xml.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemovePropertyVisitor();
    }

    private class RemovePropertyVisitor extends MavenVisitor<ExecutionContext> {
        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isPropertyTag() && propertyName.equals(tag.getName())) {
                doAfterVisit(new RemoveContentVisitor<>(tag, true));
                maybeUpdateModel();
            }
            return super.visitTag(tag, ctx);
        }
    }
}
