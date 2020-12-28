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
package org.openrewrite.xml.tree;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.XmlSourceVisitor;
import org.openrewrite.xml.internal.PrintXml;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

/**
 * The XML <a href="https://www.w3.org/TR/xml11/#syntax">spec</a>.
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface Xml extends Serializable, Tree {
    @Override
    default String print() {
        return new PrintXml().visit(this);
    }

    @Override
    default <R> R accept(SourceVisitor<R> v) {
        return v instanceof XmlSourceVisitor ?
                acceptXml((XmlSourceVisitor<R>) v) : v.defaultTo(null);
    }

    default <R> R acceptXml(XmlSourceVisitor<R> v) {
        return v.defaultTo(null);
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @JsonIgnoreProperties(value = "styles")
    class Document implements Xml, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        Path sourcePath;

        @With
        Prolog prolog;

        @With
        Tag root;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptXml(XmlSourceVisitor<R> v) {
            return v.visitDocument(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Prolog implements Xml {
        @EqualsAndHashCode.Include
        UUID id;

        @Nullable
        @With
        List<ProcessingInstruction> xmlDecls;

        @With
        List<Misc> misc;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptXml(XmlSourceVisitor<R> v) {
            return v.visitProlog(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class ProcessingInstruction implements Xml, Misc {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String name;

        @With
        List<Attribute> attributes;

        /**
         * Space before '&gt;'
         */
        @With
        String beforeTagDelimiterPrefix;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptXml(XmlSourceVisitor<R> v) {
            return v.visitProcessingInstruction(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Tag implements Xml, Content {
        @EqualsAndHashCode.Include
        UUID id;

        /**
         * XML does not allow space between the '&lt;' and tag name.
         */
        String name;

        public static Xml.Tag build(String tagSource) {
            return new XmlParser().parse(tagSource).get(0).getRoot();
        }

        public Tag withName(String name) {
            return new Tag(id, name, attributes, content,
                    closing == null ? null : closing.withName(name),
                    beforeTagDelimiterPrefix,
                    formatting,
                    markers);
        }

        public Tag withValue(String value) {
            CharData charData;
            if (content != null && content.get(0) instanceof CharData) {
                charData = ((CharData) content.get(0)).withText(value);
            } else {
                charData = new CharData(randomId(), false, value, Formatting.EMPTY, Markers.EMPTY);
            }
            return withContent(Collections.singletonList(charData));
        }

        @With
        List<Attribute> attributes;

        @Nullable
        List<? extends Content> content;

        @JsonIgnore
        public Optional<Tag> getChild(String name) {
            return content == null ? Optional.empty() : content.stream()
                    .filter(t -> t instanceof Xml.Tag)
                    .map(Tag.class::cast)
                    .filter(t -> t.getName().equals(name))
                    .findAny();
        }

        @JsonIgnore
        public List<Tag> getChildren(String name) {
            return content == null ? emptyList() : content.stream()
                    .filter(t -> t instanceof Xml.Tag)
                    .map(Tag.class::cast)
                    .filter(t -> t.getName().equals(name))
                    .collect(toList());
        }

        @JsonIgnore
        public List<Tag> getChildren() {
            return content == null ? emptyList() : content.stream()
                    .filter(t -> t instanceof Xml.Tag)
                    .map(Tag.class::cast)
                    .collect(toList());
        }

        /**
         * Locate an child tag with the given name and set its text value.
         *
         * @param childName The child tag to locate. This assumes there is one and only one.
         * @param text      The text value to set.
         * @return This tag.
         */
        public Xml.Tag withChildValue(String childName, String text) {
            return getChild(childName)
                    .map(tag -> this.withContent(
                            content == null ?
                                    null :
                                    content.stream()
                                            .map(content -> content == tag ?
                                                    ((Tag) content).withValue(text) :
                                                    content)
                                            .collect(toList())
                    ))
                    .orElse(this);
        }

        /**
         * @return If this tag's content is only character data, consider it the value.
         */
        @JsonIgnore
        public Optional<String> getValue() {
            if (content == null) {
                return Optional.empty();
            }
            if (content.size() != 1) {
                return Optional.empty();
            }
            if (content.get(0) instanceof Xml.CharData) {
                return Optional.ofNullable(((CharData) content.get(0)).getText());
            }
            return Optional.empty();
        }

        /**
         * A shortcut for {@link #getChild(String)} and {@link #getValue()}.
         *
         * @param name The name of the child element to look for.
         * @return The character data of the first child element matching the provided name, if any.
         */
        @JsonIgnore
        public Optional<String> getChildValue(String name) {
            return getChild(name).flatMap(Tag::getValue);
        }

        @JsonIgnore
        public Optional<Tag> getSibling(String name, Cursor cursor) {
            Xml.Tag parent = cursor.getParentOrThrow().getTree();
            if (parent == null) {
                return Optional.empty();
            }
            return parent.getChild(name);
        }

        public Tag withContent(List<? extends Content> content) {
            if (this.content == content) {
                return this;
            }

            Tag tag = new Tag(id, name, attributes, content, closing,
                    beforeTagDelimiterPrefix,
                    formatting,
                    markers);

            if (closing == null) {
                if (content != null && !content.isEmpty()) {
                    Formatting indentedClosingTagFormatting = formatting.withPrefix(
                            formatting.getPrefix().substring(Math.max(0,
                                    formatting.getPrefix().lastIndexOf('\n'))));

                    if (content.get(0) instanceof CharData) {
                        return tag.withClosing(new Closing(randomId(), name, "",
                                content.get(0).getPrefix().contains("\n") ?
                                        indentedClosingTagFormatting : Formatting.EMPTY,
                                Markers.EMPTY));
                    } else {
                        return tag.withClosing(new Closing(randomId(), name, "",
                                indentedClosingTagFormatting, Markers.EMPTY));
                    }
                }
            }

            return tag;
        }

        @With
        @Nullable
        Closing closing;

        /**
         * Space before '&gt;' or '/&gt;'
         */
        @With
        String beforeTagDelimiterPrefix;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptXml(XmlSourceVisitor<R> v) {
            return v.visitTag(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Closing implements Xml {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            String name;

            /**
             * Space before '&gt;'
             */
            @With
            String beforeTagDelimiterPrefix;

            @With
        Formatting formatting;

        @With
        Markers markers;
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Attribute implements Xml {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Ident key;

        @With
        Value value;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptXml(XmlSourceVisitor<R> v) {
            return v.visitAttribute(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Value implements Xml {
            public enum Quote {
                Double, Single
            }

            @EqualsAndHashCode.Include
            UUID id;

            @With
            Quote quote;

            @With
            String value;

            @With
        Formatting formatting;

        @With
        Markers markers;
        }

        @JsonIgnore
        public String getKeyAsString() {
            return key.getName();
        }

        @JsonIgnore
        public String getValueAsString() {
            return value.getValue();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class CharData implements Xml, Content {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        boolean cdata;

        @With
        String text;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptXml(XmlSourceVisitor<R> v) {
            return v.visitCharData(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Comment implements Xml, Content, Misc {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String text;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptXml(XmlSourceVisitor<R> v) {
            return v.visitComment(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class DocTypeDecl implements Xml, Misc {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Ident name;

        @With
        Ident externalId;

        @With
        List<Ident> internalSubset;

        @With
        @Nullable
        ExternalSubsets externalSubsets;

        /**
         * Space before '&gt;'.
         */
        @With
        String beforeTagDelimiterPrefix;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class ExternalSubsets implements Xml {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            List<Element> elements;

            @With
        Formatting formatting;

        @With
        Markers markers;
        }

        @Override
        public <R> R acceptXml(XmlSourceVisitor<R> v) {
            return v.visitDocTypeDecl(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Element implements Xml {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<Ident> subset;

        /**
         * Space before '&gt;'
         */
        @With
        String beforeTagDelimiterPrefix;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptXml(XmlSourceVisitor<R> v) {
            return v.visitElement(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Ident implements Xml {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String name;

        @With
        Formatting formatting;

        @With
        Markers markers;

        @Override
        public <R> R acceptXml(XmlSourceVisitor<R> v) {
            return v.visitIdent(this);
        }

        @Override
        public String toString() {
            return "Ident{" + name + "}";
        }
    }
}
