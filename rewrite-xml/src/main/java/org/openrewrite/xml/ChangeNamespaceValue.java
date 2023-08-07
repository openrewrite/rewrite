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
package org.openrewrite.xml;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeNamespaceValue extends Recipe {

    @Override
    public String getDisplayName() {
        return "Change XML Attribute of a specific resource version";
    }

    @Override
    public String getDescription() {
        return "Alters XML Attribute value within specified element of a specific resource versions.";
    }

    @Option(displayName = "Element name",
            description = "The name of the element whose attribute's value is to be changed. Interpreted as an XPath Expression.",
            example = "property")
    String elementName;

    @Option(displayName = "New value",
            description = "The new value to be used for key specified by `attributeName`, Set to null if you want to remove the attribute.",
            example = "newfoo.bar.attribute.value.string")
    String newValue;

    @Option(displayName = "Resource version",
            description = "The version of resource to change",
            example = "1.1")
    String versionMatcher;


    @JsonCreator
    public ChangeNamespaceValue(@NonNull @JsonProperty("elementName") String elementName,
                                @NonNull @JsonProperty("newValue") String newValue,
                                @NonNull @JsonProperty("versionMatcher") String versionMatcher) {
        this.elementName = elementName;
        this.newValue = newValue;
        this.versionMatcher = versionMatcher;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {

            String version = null;

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                this.version = null;
                if (new XPathMatcher(elementName).matches(getCursor())) {
                    // find versions
                    t = t.withAttributes(ListUtils.map(t.getAttributes(), this::visitChosenElementAttribute));
                }

                if (this.version != null) {
                    //change namespace
                    t = t.withAttributes(ListUtils.map(t.getAttributes(), this::visitChosenElementAttribute));
                }
                return t;
            }

            public boolean matchesVersion() {
                String[] versions = versionMatcher.split(",");
                double dVersion = Double.parseDouble(this.version);
                for (String splitVersion : versions) {
                    boolean checkGreaterThan = false;
                    double dVersionExpected;
                    if (splitVersion.endsWith("+")) {
                        splitVersion = splitVersion.substring(0, splitVersion.length() - 1);
                        checkGreaterThan = true;
                    }
                    try {
                        dVersionExpected = Double.parseDouble(splitVersion);
                    } catch (NumberFormatException e) {
                        return false;
                    }
                    if (!checkGreaterThan && dVersionExpected == dVersion || checkGreaterThan && dVersionExpected <= dVersion) {
                        return true;
                    }
                }
                return false;
            }

            public Xml.Attribute visitChosenElementAttribute(Xml.Attribute attribute) {
                if (this.version == null && !attribute.getKeyAsString().equals("version") ||
                    this.version != null && !attribute.getKeyAsString().equals("xmlns")) {
                    return attribute;
                }

                if (this.version != null && matchesVersion()) {
                    return attribute.withValue(
                            new Xml.Attribute.Value(attribute.getId(),
                                    "",
                                    attribute.getMarkers(),
                                    attribute.getValue().getQuote(),
                                    newValue));
                } else {
                    this.version = attribute.getValueAsString();
                    return attribute;
                }
            }
        };
    }

}
