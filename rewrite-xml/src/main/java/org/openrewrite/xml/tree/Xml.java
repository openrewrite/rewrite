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
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.Metadata;
import org.openrewrite.xml.XmlSourceVisitor;
import org.openrewrite.xml.internal.XmlPrintVisitor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.openrewrite.Tree.randomId;

/**
 * The XML <a href="https://www.w3.org/TR/xml11/#syntax">spec</a>.
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface Xml extends Serializable, Tree {
    @Override
    default String print() {
        return new XmlPrintVisitor().visit(this);
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
    class Document implements Xml, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        String sourcePath;

        @With
        Map<Metadata, String> metadata;

        @With
        Prolog prolog;

        @With
        Tag root;

        @With
        Formatting formatting;

        public Refactor<Document, Xml> refactor() {
            return new Refactor<>(this);
        }

        @Override
        public <R> R acceptXml(XmlSourceVisitor<R> v) {
            return v.visitDocument(this);
        }

        @Override
        public String getFileType() {
            return "XML";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Prolog implements Xml {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        ProcessingInstruction xmlDecl;

        @With
        List<Misc> misc;

        @With
        Formatting formatting;

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

        public Tag withName(String name) {
            return new Tag(id, name, attributes, content,
                    closing == null ? null : closing.withName(name),
                    beforeTagDelimiterPrefix,
                    formatting);
        }

        @With
        List<Attribute> attributes;

        List<Content> content;

        public Tag withContent(List<Content> content) {
            Tag tag = new Tag(id, name, attributes, content, closing,
                    beforeTagDelimiterPrefix,
                    formatting);

            if(closing == null) {
                if (content != null && !content.isEmpty()) {
                    Formatting indentedClosingTagFormatting = formatting.withPrefix(
                            formatting.getPrefix().substring(Math.max(0,
                                    formatting.getPrefix().lastIndexOf('\n'))));

                    if(content.get(0) instanceof CharData) {
                        return tag.withClosing(new Closing(randomId(), name, "",
                                content.get(0).getFormatting().getPrefix().contains("\n") ?
                                        indentedClosingTagFormatting : Formatting.EMPTY));
                    }
                    else {
                        return tag.withClosing(new Closing(randomId(), name, "",
                                indentedClosingTagFormatting));
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
        }

        public String getKeyAsString() {
            return key.getName();
        }

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
