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

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.internal.WithPrefix;
import org.openrewrite.xml.internal.XmlPrinter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

/**
 * The XML <a href="https://www.w3.org/TR/xml11/#syntax">spec</a>.
 */
public interface Xml extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptXml(v.adapt(XmlVisitor.class), p);
    }

    @Nullable
    default <P> Xml acceptXml(XmlVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(XmlVisitor.class);
    }

    String getPrefix();

    Xml withPrefix(String prefix);

    /**
     * @param prefix The new prefix
     * @return An XML AST with the new prefix set, even if the old and new prefix pass a
     * string equality check. The receiver is unchanged if the old and new prefix pass a
     * referential equality check.
     */
    Xml withPrefixUnsafe(String prefix);

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    class Document implements Xml, SourceFile {
        @Getter
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Path sourcePath;

        @With
        String prefixUnsafe;

        public Document withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        @Getter
        @With
        Markers markers;

        @Nullable // for backwards compatibility
        @With(AccessLevel.PRIVATE)
        String charsetName;

        @With
        @Getter
        boolean charsetBomMarked;

        @With
        @Getter
        @Nullable
        Checksum checksum;

        @With
        @Getter
        @Nullable
        FileAttributes fileAttributes;

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        @Getter
        @With
        Prolog prolog;

        @Getter
        @With
        Tag root;

        @Getter
        String eof;

        public Document withEof(String eof) {
            if (this.eof.equals(eof)) {
                return this;
            }
            return new Document(id, sourcePath, prefixUnsafe, markers, charsetName, charsetBomMarked, checksum, fileAttributes, prolog, root, eof);
        }

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitDocument(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new XmlPrinter<>();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Prolog implements Xml {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        public Prolog withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;

        @Nullable
        XmlDecl xmlDecl;

        List<Misc> misc;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitProlog(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class XmlDecl implements Xml, Misc {
        @EqualsAndHashCode.Include
        UUID id;
        String prefixUnsafe;

        public XmlDecl withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;
        String name;
        List<Attribute> attributes;

        /**
         * Space before '&gt;'
         */
        String beforeTagDelimiterPrefix;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitXmlDecl(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ProcessingInstruction implements Xml, Content, Misc {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        public ProcessingInstruction withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;
        String name;
        CharData processingInstructions;

        /**
         * Space before '&gt;'
         */
        String beforeTagDelimiterPrefix;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitProcessingInstruction(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Tag implements Xml, Content {
        @EqualsAndHashCode.Include
        @With
        UUID id;

        @With
        String prefixUnsafe;

        public Tag withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        @With
        Markers markers;

        /**
         * XML does not allow space between the '&lt;' and tag name.
         */
        String name;

        public static Xml.Tag build(@Language("xml") String tagSource) {
            return new XmlParser().parse(tagSource)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Could not parse as XML"))
                    .getRoot();
        }

        public Tag withName(String name) {
            return new Tag(id, prefixUnsafe, markers, name, attributes, content,
                    closing == null ? null : closing.withName(name),
                    beforeTagDelimiterPrefix);
        }

        public Tag withValue(String value) {
            CharData charData;
            if (content != null && content.get(0) instanceof CharData) {
                charData = ((CharData) content.get(0)).withText(value);
            } else {
                charData = new CharData(randomId(), "", Markers.EMPTY,
                        false, value, "");
            }
            return withContent(Collections.singletonList(charData));
        }

        @With
        List<Attribute> attributes;

        @Nullable
        List<? extends Content> content;

        public Optional<Tag> getChild(String name) {
            return content == null ? Optional.empty() : content.stream()
                    .filter(t -> t instanceof Xml.Tag)
                    .map(Tag.class::cast)
                    .filter(t -> t.getName().equals(name))
                    .findAny();
        }

        public List<Tag> getChildren(String name) {
            return content == null ? emptyList() : content.stream()
                    .filter(t -> t instanceof Xml.Tag)
                    .map(Tag.class::cast)
                    .filter(t -> t.getName().equals(name))
                    .collect(toList());
        }

        public List<Tag> getChildren() {
            return content == null ? emptyList() : content.stream()
                    .filter(t -> t instanceof Xml.Tag)
                    .map(Tag.class::cast)
                    .collect(toList());
        }

        /**
         * Locate a child tag with the given name and set its text value.
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
        public Optional<String> getChildValue(String name) {
            return getChild(name).flatMap(Tag::getValue);
        }

        public Optional<Tag> getSibling(String name, Cursor cursor) {
            if (cursor.getParent() == null) {
                return Optional.empty();
            }
            Xml.Tag parent = cursor.getParent().getValue();
            return parent.getChild(name);
        }

        public Tag withContent(@Nullable List<? extends Content> content) {
            if (this.content == content) {
                return this;
            }

            Tag tag = new Tag(id, prefixUnsafe, markers, name, attributes, content, closing,
                    beforeTagDelimiterPrefix);

            if (closing == null) {
                if (content != null && !content.isEmpty()) {
                    // TODO test this
                    String indentedClosingTagPrefix = prefixUnsafe.substring(Math.max(0, prefixUnsafe.lastIndexOf('\n')));

                    if (content.get(0) instanceof CharData) {
                        return tag.withClosing(new Closing(randomId(),
                                content.get(0).getPrefix().contains("\n") ?
                                        indentedClosingTagPrefix : "",
                                Markers.EMPTY,
                                name, ""));
                    } else {
                        return tag.withClosing(new Closing(randomId(),
                                indentedClosingTagPrefix, Markers.EMPTY,
                                name, ""));
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

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitTag(this, p);
        }

        @Override
        public String toString() {
            return "<" + name + attributes.stream().map(a -> " " + a.getKey().getName() + "=\"" + a.getValueAsString() + "\"")
                    .collect(Collectors.joining("")) + ">";
        }

        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class Closing implements Xml {
            @EqualsAndHashCode.Include
            UUID id;

            String prefixUnsafe;

            public Closing withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            public String getPrefix() {
                return prefixUnsafe;
            }

            Markers markers;
            String name;

            /**
             * Space before '&gt;'
             */
            String beforeTagDelimiterPrefix;

            @Override
            public String toString() {
                return "</" + name + ">";
            }
        }
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Attribute implements Xml {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        public Attribute withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;
        Ident key;
        String beforeEquals;
        Value value;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitAttribute(this, p);
        }

        @lombok.Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class Value implements Xml {
            public enum Quote {
                Double, Single
            }

            @EqualsAndHashCode.Include
            UUID id;

            String prefixUnsafe;

            public Value withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            public String getPrefix() {
                return prefixUnsafe;
            }

            Markers markers;
            Quote quote;
            String value;
        }

        public String getKeyAsString() {
            return key.getName();
        }

        public String getValueAsString() {
            return value.getValue();
        }

        @Override
        public String toString() {
            return getKeyAsString() + "=" + getValueAsString();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CharData implements Xml, Content {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        public CharData withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;
        boolean cdata;
        String text;
        String afterText;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitCharData(this, p);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("text = \"").append(text).append("\"");
            if (afterText != null && !afterText.isEmpty()) {
                sb.append(" afterText = \"").append(afterText).append("\"");
            }
            return sb.toString();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Comment implements Xml, Content, Misc {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        public Comment withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;
        String text;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitComment(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class DocTypeDecl implements Xml, Misc {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        public DocTypeDecl withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;
        Ident name;
        Ident externalId;
        List<Ident> internalSubset;

        @Nullable
        ExternalSubsets externalSubsets;

        /**
         * Space before '&gt;'.
         */
        String beforeTagDelimiterPrefix;

        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class ExternalSubsets implements Xml {
            @EqualsAndHashCode.Include
            UUID id;

            String prefixUnsafe;

            public ExternalSubsets withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            public String getPrefix() {
                return prefixUnsafe;
            }

            Markers markers;
            List<Element> elements;
        }

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitDocTypeDecl(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Element implements Xml {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        public Element withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;
        List<Ident> subset;

        /**
         * Space before '&gt;'
         */
        String beforeTagDelimiterPrefix;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitElement(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Ident implements Xml {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        public Ident withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;
        String name;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitIdent(this, p);
        }

        @Override
        public String toString() {
            return "Ident{" + name + "}";
        }
    }
}
