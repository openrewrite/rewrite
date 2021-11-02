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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Maven;
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
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangePropertyValueVisitor();
    }

    public ChangePropertyValue(String key, String newValue, @Nullable Boolean addIfMissing) {
        //Customizing lombok constructor to replace the property markers.
        //noinspection ConstantConditions
        if (key != null) {
            key = key.replace("${", "").replace("}", "");
        }
        this.key = key;
        this.newValue = newValue;
        this.addIfMissing = addIfMissing != null && addIfMissing;
    }

    private class ChangePropertyValueVisitor extends MavenVisitor {

        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            Maven m = super.visitMaven(maven, ctx);

            if (addIfMissing) {
                Xml.Tag root = m.getRoot();
                Optional<Xml.Tag> properties = root.getChild("properties");
                if (!properties.isPresent()) {
                    doAfterVisit(new AddToTagVisitor<>(root, Xml.Tag.build("<properties>\n<" + key + ">" + newValue + "</" + key + ">\n</properties>"),
                            new MavenTagInsertionComparator(root.getChildren())));
                    doAfterVisit(new AutoFormatVisitor<>(root));

                } else if (!properties.get().getChildValue(key).isPresent()) {
                    doAfterVisit(new AddToTagVisitor<>(properties.get(), Xml.Tag.build("<" + key + ">" + newValue + "</" + key + ">"),
                            new MavenTagInsertionComparator(properties.get().getChildren())));
                    doAfterVisit(new AutoFormatVisitor<>(properties.get()));
                }

            }
            return m;
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isPropertyTag() && key.equals(tag.getName()) &&
                    !newValue.equals(tag.getValue().orElse(null))) {
                doAfterVisit(new ChangeTagValueVisitor<>(tag, newValue));
            }
            return super.visitTag(tag, ctx);
        }
    }
}
