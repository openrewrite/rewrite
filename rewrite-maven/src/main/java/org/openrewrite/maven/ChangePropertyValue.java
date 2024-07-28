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
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangePropertyValue extends Recipe {

    @Option(displayName = "Key",
            description = "The name of the property key whose value is to be changed.",
            example = "junit.version")
    String key;

    @Option(displayName = "Value",
            description = "Value to apply to the matching property.",
            example = "4.13")
    String newValue;

    @Option(displayName = "Add if missing",
            description = "Add the property if it is missing from the pom file.",
            required = false)
    @Nullable
    Boolean addIfMissing;

    @Option(displayName = "Trust parent POM",
            description = "Even if the parent defines a property with the same key, trust it even if the value isn't the same. " +
                          "Useful when you want to wait for the parent to have its value changed first. The parent is not trusted by default.",
            required = false)
    @Nullable
    Boolean trustParent;

    @Override
    public String getDisplayName() {
        return "Change Maven project property value";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s=%s`", key, newValue);
    }

    @Override
    public String getDescription() {
        return "Changes the specified Maven project property value leaving the key intact.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                String currentValue = getResolutionResult().getPom().getProperties().get(key);
                boolean trust = Boolean.TRUE.equals(trustParent);
                if (!trust && !newValue.equals(currentValue)) {
                    return SearchResult.found(document);
                } else if (trust) {
                    String myValue = getResolutionResult().getPom().getRequested().getProperties().get(key);
                    if (myValue != null && !myValue.equals(newValue)) {
                        return SearchResult.found(document);
                    }
                }
                return document;
            }
        }, new MavenIsoVisitor<ExecutionContext>() {
            final String propertyName = key.replace("${", "").replace("}", "");

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document d = super.visitDocument(document, ctx);
                if (Boolean.TRUE.equals(addIfMissing)) {
                    doAfterVisit(new AddProperty(key, newValue, true, false).getVisitor());
                }
                return d;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isPropertyTag() && propertyName.equals(tag.getName()) &&
                    !newValue.equals(tag.getValue().orElse(null))) {
                    doAfterVisit(new ChangeTagValueVisitor<>(tag, newValue));
                    maybeUpdateModel();
                }
                return super.visitTag(tag, ctx);
            }
        });
    }
}
