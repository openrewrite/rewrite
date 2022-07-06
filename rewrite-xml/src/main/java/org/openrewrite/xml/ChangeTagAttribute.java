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
package org.openrewrite.xml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeTagAttribute extends Recipe {

    @Override
    public String getDisplayName() {
        return "Change XML Attribute";
    }

    @Override
    public String getDescription() {
        return "Alters XML Attribute value within specified element.";
    }

    @Option(displayName = "Element name",
            description = "The name of the element whose attribute's value is to be changed. Interpreted as an XPath Expression.",
            example = "property")
    String elementName;

    @Option(displayName = "Attribute name",
            description = "The name of the attribute whose value is to be changed.",
            example = "name")
    String attributeName;

    @Option(displayName = "New value",
            description = "The new value to be used for key specified by `attributeName`.",
            example = "newfoo.bar.attribute.value.string")
    String newValue;

    @Option(displayName = "Old value",
            example = "foo.bar.attribute.value.string",
            required = false,
            description = "Only change the property value if it matches the configured `oldValue`.")
    @Nullable
    String oldValue;

    @Option(displayName = "File matcher",
            description = "If provided only matching files will be modified. This is a glob expression.",
            required = false,
            example = "'**/application-*.xml'")
    @Nullable
    String fileMatcher;

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        }
        return null;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangeTagAttributeVisitor<>(new XPathMatcher(elementName), attributeName, oldValue, newValue);
    }
}
