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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.AutoFormatVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangePropertyValue extends Recipe {

    @Option(displayName = "Key",
            description = "The name of the property key whose value is to be changed.",
            example = "junit.version")
    String key;

    @Option(displayName = "Value",
            description = "Value to apply to the matching property.",
            example = "4.13")
    String newValue;

    @Option(displayName = "Add If Missing",
            description = "Add the property if it is missing from the pom file.",
            required = false,
            example = "false")
    @Nullable
    Boolean addIfMissing;

    @Override
    public String getDisplayName() {
        return "Change Maven project property value";
    }

    @Override
    public String getDescription() {
        return "Changes the specified Maven project property value leaving the key intact.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            final String key = ChangePropertyValue.this.key.replace("${", "").replace("}", "");

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document m = super.visitDocument(document, ctx);

                if (Boolean.TRUE.equals(addIfMissing)) {
                    Xml.Tag root = m.getRoot();
                    Optional<Xml.Tag> properties = root.getChild("properties");
                    if (!properties.isPresent()) {
                        Xml.Tag propertiesTag = Xml.Tag.build("<properties>\n<" + key + ">" + newValue + "</" + key + ">\n</properties>");
                        doAfterVisit(new AddToTagVisitor<>(root, propertiesTag, new MavenTagInsertionComparator(root.getChildren())));
                        doAfterVisit(new AutoFormatVisitor<>(propertiesTag));

                    } else if (!properties.get().getChildValue(key).isPresent()) {
                        Xml.Tag propertyTag = Xml.Tag.build("<" + key + ">" + newValue + "</" + key + ">");
                        doAfterVisit(new AddToTagVisitor<>(properties.get(), propertyTag,
                                new MavenTagInsertionComparator(properties.get().getChildren())));
                        doAfterVisit(new AutoFormatVisitor<>(propertyTag));
                    }

                }
                return m;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isPropertyTag() && key.equals(tag.getName()) &&
                        !newValue.equals(tag.getValue().orElse(null))) {
                    doAfterVisit(new ChangeTagValueVisitor<>(tag, newValue));
                }
                return super.visitTag(tag, ctx);
            }
        };
    }
}
