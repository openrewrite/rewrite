/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.xml.trait;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.trait.Reference;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public class SpringXmlReference {

    static class Matcher extends SimpleTraitMatcher<XmlReference> {
        private final Pattern referencePattern = Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(?:\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");
        private final XPathMatcher classXPath = new XPathMatcher("//@class");
        private final XPathMatcher typeXPath = new XPathMatcher("//@type");
        private final XPathMatcher keyTypeXPath = new XPathMatcher("//@key-type");
        private final XPathMatcher valueTypeXPath = new XPathMatcher("//@value-type");
        private final XPathMatcher tags = new XPathMatcher("//value");

        @Override
        protected @Nullable XmlReference test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof Xml.Attribute) {
                Xml.Attribute attrib = (Xml.Attribute) value;
                if (classXPath.matches(cursor) || typeXPath.matches(cursor) || keyTypeXPath.matches(cursor) || valueTypeXPath.matches(cursor)) {
                    String stringVal = attrib.getValueAsString();
                    if (referencePattern.matcher(stringVal).matches()) {
                        return new XmlReference(cursor, determineKind(stringVal));
                    }
                }
            } else if (value instanceof Xml.Tag) {
                Xml.Tag tag = (Xml.Tag) value;
                if (tags.matches(cursor)) {
                    Optional<String> stringVal = tag.getValue();
                    if (stringVal.isPresent() && referencePattern.matcher(stringVal.get()).matches()) {
                        return new XmlReference(cursor, determineKind(stringVal.get()));
                    }
                }
            }
            return null;
        }

        Reference.Kind determineKind(String value) {
            return Character.isUpperCase(value.charAt(value.lastIndexOf('.') + 1)) ? Reference.Kind.TYPE : Reference.Kind.PACKAGE;
        }
    }

    @SuppressWarnings("unused")
    public static class Provider implements Reference.Provider {

        @Override
        public Set<Reference> getReferences(SourceFile sourceFile) {
            Set<Reference> references = new HashSet<>();
            new SpringXmlReference.Matcher().asVisitor(reference -> {
                references.add(reference);
                return reference.getTree();
            }).visit(sourceFile, 0);
            return references;
        }

        @Override
        public boolean isAcceptable(SourceFile sourceFile) {
            if (sourceFile instanceof Xml.Document) {
                Xml.Document doc = (Xml.Document) sourceFile;
                //noinspection ConstantValue
                if (doc.getRoot() != null) {
                    for (Xml.Attribute attrib : doc.getRoot().getAttributes()) {
                        if (attrib.getKeyAsString().equals("xsi:schemaLocation") && attrib.getValueAsString().contains("www.springframework.org/schema/beans")) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }
}
