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

import org.openrewrite.xml.tree.Xml;

public class ChangeXMLAttributeVisitor<P> extends XmlVisitor<P> {

    private final String elementName;
    private final String attributeName;
    private final String oldValue;
    private final String newValue;

    public ChangeXMLAttributeVisitor(String elementName, String attributeName, String oldValue, String newValue) {
        this.elementName = elementName;
        this.attributeName = attributeName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public Xml visitTag(Xml.Tag tag, P p) {
        Xml.Tag t = tag;
        if (tag.getName().equals(elementName)) {
            t.getAttributes().forEach( a -> this.visitAttribute(a, p));
        }
        t = (Xml.Tag)super.visitTag(t, p);
        return t;
    }

    @Override
    public Xml visitAttribute(Xml.Attribute attribute, P p) {

        if(!attribute.getKeyAsString().equals(attributeName)) {
            return attribute;
        }
        if(!attribute.getValueAsString().startsWith(oldValue)) {
            return attribute;
        }
        String changedValue = attribute.getValueAsString().replace(oldValue, newValue);
        Xml.Attribute a = attribute;
        return a.withValue(
                new Xml.Attribute.Value(attribute.getId(),
                                        "",
                                        attribute.getMarkers(),
                                        attribute.getValue().getQuote(),
                                        changedValue));
    }
}
