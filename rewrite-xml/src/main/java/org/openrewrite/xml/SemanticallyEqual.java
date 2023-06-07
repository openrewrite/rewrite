/*
 * Copyright 2021 the original author or authors.
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

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

/**
 * Recursively check the equality of XML tags and attributes.
 * Ignores comments. Ignores the order of attributes.
 */
public class SemanticallyEqual {

    private SemanticallyEqual() {
    }

    public static boolean areEqual(Xml first, Xml second) {
        SemanticallyEqualVisitor sme = new SemanticallyEqualVisitor();
        sme.visit(first, second);
        return sme.areEqual;
    }

    @SuppressWarnings("ConstantConditions")
    private static class SemanticallyEqualVisitor extends XmlVisitor<Xml> {

        boolean areEqual = true;

        @Override
        public Xml visitDocument(Xml.Document document, Xml other) {
            if(document == other) {
                return null;
            }
            if(!(other instanceof Xml.Document)) {
                areEqual = false;
                return null;
            }
            Xml.Document otherDocument = (Xml.Document)other;
            visitTag(document.getRoot(), otherDocument.getRoot());
            return null;
        }

        @Override
        public Xml visitTag(Xml.Tag tag, Xml other) {
            if(tag == other) {
                return null;
            }
            if(!(other instanceof Xml.Tag)) {
                areEqual = false;
                return null;
            }
            Xml.Tag otherTag = (Xml.Tag)other;
            if (!tag.getName().equals(otherTag.getName())) {
                areEqual = false;
                return null;
            }
            if(tag.getAttributes().size() != otherTag.getAttributes().size()) {
                areEqual = false;
                return null;
            }
            List<Xml.Attribute> theseAttributes = tag.getAttributes().stream()
                    .sorted(comparing(Xml.Attribute::getKeyAsString))
                    .collect(toList());
            List<Xml.Attribute> thoseAttributes = otherTag.getAttributes().stream()
                    .sorted(comparing(Xml.Attribute::getKeyAsString))
                    .collect(toList());
            for(int i = 0; i < theseAttributes.size(); i++) {
                visitAttribute(theseAttributes.get(i), thoseAttributes.get(i));
                if(!areEqual) {
                    return null;
                }
            }
            if(bothNullOrEmpty(tag.getContent(), otherTag.getContent())) {
                return null;
            } else if(eitherNullOrEmpty(tag.getContent(), otherTag.getContent())) {
                areEqual = false;
                return null;
            }
            List<Content> theseContents = tag.getContent().stream()
                    .filter(it -> it != null && !(it instanceof Xml.Comment))
                    .collect(toList());
            List<Content> thoseContents = otherTag.getContent().stream()
                    .filter(it -> it != null && !(it instanceof Xml.Comment))
                    .collect(toList());
            if(theseContents.size() != thoseContents.size()) {
                areEqual = false;
                return null;
            }
            for(int i = 0; i < theseContents.size(); i++) {
                visit(theseContents.get(i), thoseContents.get(i));
                if(!areEqual) {
                    return null;
                }
            }
            return null;
        }

        @Override
        public Xml visitAttribute(Xml.Attribute attribute, Xml other) {
            if(attribute == other) {
                return null;
            }
            if(!(other instanceof Xml.Attribute)) {
                areEqual = false;
                return null;
            }
            Xml.Attribute otherAttribute = (Xml.Attribute)other;
            if(!attribute.getKeyAsString().equals(otherAttribute.getKeyAsString())) {
                areEqual = false;
                return null;
            }
            if(!attribute.getValueAsString().equals(otherAttribute.getValueAsString())) {
                areEqual = false;
                return null;
            }
            return null;
        }

        @Override
        public Xml visitCharData(Xml.CharData charData, Xml other) {
            if(charData == other) {
                return null;
            }
            if(!(other instanceof Xml.CharData)) {
                areEqual = false;
                return null;
            }
            Xml.CharData otherChar = (Xml.CharData)other;
            if(!charData.getText().trim().equals(otherChar.getText().trim())) {
                areEqual = false;
                return null;
            }
            return null;
        }
    }
    private static boolean isNullOrEmpty(@Nullable List<?> a) {
        return a == null || a.isEmpty();
    }
    private static boolean bothNullOrEmpty(@Nullable List<?> a, @Nullable List<?> b) {
        return isNullOrEmpty(a) && isNullOrEmpty(b);
    }
    private static boolean eitherNullOrEmpty(@Nullable List<?> a, @Nullable List<?> b) {
        return isNullOrEmpty(a) || isNullOrEmpty(b);
    }
}
