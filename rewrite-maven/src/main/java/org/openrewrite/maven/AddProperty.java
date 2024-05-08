/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.TagNameComparator;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddProperty extends Recipe {

    @Option(displayName = "Key",
            description = "The name of the property key to be added.",
            example = "junit.version")
    String key;

    @Option(displayName = "Value",
            description = "The value of property to be added.",
            example = "4.13")
    String value;

    @Option(displayName = "Preserve existing value",
            description = "Preserve previous value if the property already exists in the pom file.",
            required = false)
    @Nullable
    Boolean preserveExistingValue;

    @Option(displayName = "Trust parent POM",
            description = "If the parent defines a property with the same key, trust it even if the value isn't the same. " +
                          "Useful when you want to wait for the parent to have its value changed first. The parent is not trusted by default.",
            required = false)
    @Nullable
    Boolean trustParent;

    @Override
    public String getDisplayName() {
        return "Add Maven project property";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s=%s`", key, value);
    }

    @Override
    public String getDescription() {
        return "Add a new property to the Maven project property. " +
               "Prefers to add the property to the parent if the project has multiple modules.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddPropertyVisitor(
                key.replace("${", "").replace("}", ""), value, preserveExistingValue, trustParent);
    }
}

@Value
@EqualsAndHashCode(callSuper = false)
class AddPropertyVisitor extends MavenIsoVisitor<ExecutionContext> {
    String key;
    String value;
    @Nullable Boolean preserveExistingValue;
    @Nullable Boolean trustParent;

    @Override
    public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
        String parentValue = getResolutionResult().getPom().getRequested().getProperties().get(key);
        if ((Boolean.TRUE.equals(trustParent) && (parentValue == null || value.equals(parentValue)))
            || value.equals(getResolutionResult().getPom().getProperties().get(key))) {
            return document;
        }

        // If there is a parent pom in the same project, update the property there instead
        if (document.getRoot().getChild("parent")
                .flatMap(tag -> tag.getChild("relativePath"))
                .flatMap(Xml.Tag::getValue)
                .isPresent()) {
            if (Boolean.TRUE.equals(preserveExistingValue)) {
                return document;
            }
            // If the property is expected to be in the parent, there's no need for it in the child pom
            return (Xml.Document) new RemoveProperty(key).getVisitor()
                    .visitNonNull(document, ctx);
        }

        Xml.Document d = super.visitDocument(document, ctx);
        Xml.Tag root = d.getRoot();
        Optional<Xml.Tag> properties = root.getChild("properties");
        if (!properties.isPresent()) {
            Xml.Tag propertiesTag = Xml.Tag.build("<properties>\n<" + key + ">" + value + "</" + key + ">\n</properties>");
            d = (Xml.Document) new AddToTagVisitor<ExecutionContext>(root, propertiesTag, new MavenTagInsertionComparator(root.getChildren())).visitNonNull(d, ctx);
        } else if (!properties.get().getChildValue(key).isPresent()) {
            Xml.Tag propertyTag = Xml.Tag.build("<" + key + ">" + value + "</" + key + ">");
            d = (Xml.Document) new AddToTagVisitor<>(properties.get(), propertyTag, new TagNameComparator()).visitNonNull(d, ctx);
        }
        if (d != document) {
            maybeUpdateModel();
        }
        return d;
    }

    @Override
    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
        if (!Boolean.TRUE.equals(preserveExistingValue)
            && isPropertyTag() && key.equals(tag.getName())
            && !value.equals(tag.getValue().orElse(null))) {
            doAfterVisit(new ChangeTagValueVisitor<>(tag, value));
        }
        return super.visitTag(tag, ctx);
    }
}
