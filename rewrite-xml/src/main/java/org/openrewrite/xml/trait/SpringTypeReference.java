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
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.TypeReference;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Value
class SpringTypeReference implements TypeReference {
    Cursor cursor;

    @Override
    public Tree getTree() {
        return TypeReference.super.getTree();
    }

    @Override
    public String getName() {
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

    @Override
    public TreeVisitor<Tree, ExecutionContext> renameTo(String name) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof Xml.Attribute) {
                    return ((Xml.Attribute) tree).withValue(((Xml.Attribute) tree).getValue().withValue(name));
                }
                if (tree instanceof Xml.Tag) {
                    return ((Xml.Tag) tree).withValue(name);
                }
                return super.visit(tree, ctx);
            }
        };
    }

    static class Matcher extends SimpleTraitMatcher<SpringTypeReference> {
        private final Pattern typeReference = Pattern.compile("(?:[a-zA-Z_][a-zA-Z0-9_]*\\.)+[A-Z*][a-zA-Z0-9_]*(?:<[a-zA-Z0-9_,?<> ]*>)?");
        private final XPathMatcher classXPath = new XPathMatcher("//@class");
        private final XPathMatcher typeXPath = new XPathMatcher("//@type");
        private final XPathMatcher tags = new XPathMatcher("//value");

        @Override
        protected @Nullable SpringTypeReference test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof Xml.Attribute) {
                Xml.Attribute attrib = (Xml.Attribute) value;
                if (classXPath.matches(cursor) || typeXPath.matches(cursor)) {
                    if (typeReference.matcher(attrib.getValueAsString()).matches()) {
                        return new SpringTypeReference(cursor);
                    }
                }
            } else if (value instanceof Xml.Tag) {
                Xml.Tag tag = (Xml.Tag) value;
                if (tags.matches(cursor)) {
                    if (tag.getValue().isPresent() && typeReference.matcher(tag.getValue().get()).matches()) {
                        return new SpringTypeReference(cursor);
                    }
                }
            }
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static class Provider implements TypeReference.Provider {

        @Override
        public Set<TypeReference> getTypeReferences(SourceFile sourceFile) {
            Set<TypeReference> typeReferences = new HashSet<>();
            new Matcher().asVisitor(reference -> {
                typeReferences.add(reference);
                return reference.getTree();
            }).visit(sourceFile, 0);
            return typeReferences;
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
