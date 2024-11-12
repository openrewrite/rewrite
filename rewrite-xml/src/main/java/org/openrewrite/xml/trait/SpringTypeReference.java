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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.reference.Reference;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Value
class SpringTypeReference implements Reference {
    Cursor cursor;
    Kind kind;

    @Override
    public Tree getTree() {
        return Reference.super.getTree();
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public String getValue() {
        if (getTree() instanceof Xml.Attribute) {
            Xml.Attribute attribute = (Xml.Attribute) getTree();
            return attribute.getValueAsString();
        } else if (getTree() instanceof Xml.Tag) {
            Xml.Tag tag = (Xml.Tag) getTree();
            if (tag.getValue().isPresent()) {
                return tag.getValue().get();
            }
        }
        throw new IllegalArgumentException("getTree() must be an Xml.Attribute or Xml.Tag: " + getTree().getClass());
    }

    @Override
    public boolean supportsRename() {
        return true;
    }

    static class Matcher extends SimpleTraitMatcher<SpringTypeReference> {
        private final Pattern typeReference = Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(?:\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");
        private final XPathMatcher classXPath = new XPathMatcher("//@class");
        private final XPathMatcher typeXPath = new XPathMatcher("//@type");
        private final XPathMatcher tags = new XPathMatcher("//value");

        @Override
        protected @Nullable SpringTypeReference test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof Xml.Attribute) {
                Xml.Attribute attrib = (Xml.Attribute) value;
                if (classXPath.matches(cursor) || typeXPath.matches(cursor)) {
                    String stringVal = attrib.getValueAsString();
                    if (typeReference.matcher(stringVal).matches()) {
                        return new SpringTypeReference(cursor, determineKind(stringVal));
                    }
                }
            } else if (value instanceof Xml.Tag) {
                Xml.Tag tag = (Xml.Tag) value;
                if (tags.matches(cursor)) {
                    String stringVal = tag.getValue().get();
                    if (tag.getValue().isPresent() && typeReference.matcher(stringVal).matches()) {
                        return new SpringTypeReference(cursor, determineKind(stringVal));
                    }
                }
            }
            return null;
        }

        Kind determineKind(String value) {
            return Character.isUpperCase(value.charAt(value.lastIndexOf('.')+1)) ? Kind.TYPE : Kind.PACKAGE;
        }
    }

    @SuppressWarnings("unused")
    public static class Provider implements Reference.Provider {

        @Override
        public Set<Reference> getTypeReferences(SourceFile sourceFile) {
            Set<Reference> references = new HashSet<>();
            new Matcher().asVisitor(reference -> {
                references.add(reference);
                return reference.getTree();
            }).visit(sourceFile, 0);
            return references;
        }

        @Override
        public boolean isAcceptable(SourceFile sourceFile) {
            if (sourceFile instanceof Xml.Document) {
                Xml.Document doc = (Xml.Document) sourceFile;
                for (Xml.Attribute attrib : doc.getRoot().getAttributes()) {
                    if (attrib.getKeyAsString().equals("xsi:schemaLocation") && attrib.getValueAsString().contains("www.springframework.org/schema/beans")) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
