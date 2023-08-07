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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.semver.Semver;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeNamespaceValue extends Recipe {

    private static final String XMLNS_PREFIX = "xmlns";
    private static final String VERSION_PREFIX = "version";

    @Override
    public String getDisplayName() {
        return "Change XML Attribute of a specific resource version";
    }

    @Override
    public String getDescription() {
        return "Alters XML Attribute value within specified element of a specific resource versions.";
    }

    @Nullable
    @Option(displayName = "Element name",
            description = "The name of the element whose attribute's value is to be changed. Interpreted as an XPath Expression.",
            example = "property")
    String elementName;

    @Nullable
    @Option(displayName = "Old value",
            description = "Only change the property value if it matches the configured `oldValue`.",
            example = "newfoo.bar.attribute.value.string")
    String oldValue;

    @Option(displayName = "New value",
            description = "The new value to be used for the namespace.",
            example = "newfoo.bar.attribute.value.string")
    String newValue;

    @Nullable
    @Option(displayName = "Resource version",
            description = "The version of resource to change",
            example = "1.1")
    String versionMatcher;

    @Option(displayName = "Search All Namespaces",
            description = "Specify whether evaluate all namespaces",
            example = "true")
    boolean searchAllNamespaces;


    @JsonCreator
    public ChangeNamespaceValue(@Nullable @JsonProperty("elementName") String elementName, @Nullable @JsonProperty("oldValue") String oldValue,
                                @NonNull @JsonProperty("newValue") String newValue, @Nullable @JsonProperty("versionMatcher") String versionMatcher,
                                @NonNull @JsonProperty("searchAllNamespaces") boolean searchAllNamespaces) {
        this.elementName = elementName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.versionMatcher = versionMatcher;
        this.searchAllNamespaces = searchAllNamespaces;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlIsoVisitor<ExecutionContext>() {

            String version = null;

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                this.version = null;
                if (elementName == null || new XPathMatcher(elementName).matches(getCursor())) {
                    // find versions
                    t = t.withAttributes(ListUtils.map(t.getAttributes(), this::visitChosenElementAttribute));
                }

                if(this.version != null) {
                    //change namespace
                    t = t.withAttributes(ListUtils.map(t.getAttributes(), this::visitChosenElementAttribute));
                }
                return t;
            }

            public Xml.Attribute visitChosenElementAttribute(Xml.Attribute attribute) {
                boolean isXmlnsAttr = (searchAllNamespaces && attribute.getKeyAsString().startsWith(XMLNS_PREFIX) ||
                        !searchAllNamespaces && attribute.getKeyAsString().equals(XMLNS_PREFIX));
                boolean isVersionAttr = attribute.getKeyAsString().startsWith(VERSION_PREFIX);
                if(oldValue != null && (!isXmlnsAttr || !attribute.getValueAsString().equals(oldValue))) {
                    return attribute;
                }

                if(oldValue == null && (this.version == null && !isVersionAttr || this.version != null && !isXmlnsAttr)) {
                    return attribute;
                }

                if(this.version != null && Semver.validate(this.version, versionMatcher).isValid() || oldValue != null) {
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
