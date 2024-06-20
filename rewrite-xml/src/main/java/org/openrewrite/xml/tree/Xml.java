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
import org.apache.commons.text.StringEscapeUtils;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.WhitespaceValidationService;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.internal.WithPrefix;
import org.openrewrite.xml.internal.XmlPrinter;
import org.openrewrite.xml.internal.XmlWhitespaceValidationService;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
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

    static boolean isNamespaceDefinitionAttribute(String name) {
        return name.startsWith("xmlns");
    }

    static String getAttributeNameForPrefix(String namespacePrefix) {
        return namespacePrefix.isEmpty() ? "xmlns" : "xmlns:" + namespacePrefix;
    }

    /**
     * Extract the namespace prefix from a namespace definition attribute name (xmlns* attributes).
     *
     * @param name the attribute name or null if not a namespace definition attribute
     * @return the namespace prefix
     */
    static @Nullable String extractPrefixFromNamespaceDefinition(String name) {
        if (!isNamespaceDefinitionAttribute(name)) {
            return null;
        }
        return "xmlns".equals(name) ? "" : extractLocalName(name);
    }

    /**
     * Extract the namespace prefix from a tag or attribute name.
     *
     * @param name the tag or attribute name
     * @return the namespace prefix (empty string for the default namespace)
     */
    static String extractNamespacePrefix(String name) {
        int colon = name.indexOf(':');
        return colon == -1 ? "" : name.substring(0, colon);
    }

    /**
     * Extract the local name from a tag or attribute name.
     *
     * @param name the tag or attribute name
     * @return the local name
     */
    static String extractLocalName(String name) {
        int colon = name.indexOf(':');
        return colon == -1 ? name : name.substring(colon + 1);
    }

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    class Document implements Xml, SourceFile {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Path sourcePath;

        @With
        String prefixUnsafe;

        @Override
        public Document withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
        public String getPrefix() {
            return prefixUnsafe;
        }

        @With
        Markers markers;

        @Nullable // for backwards compatibility
        @With(AccessLevel.PRIVATE)
        String charsetName;

        @With
        boolean charsetBomMarked;

        @With
        @Nullable
        Checksum checksum;

        @With
        @Nullable
        FileAttributes fileAttributes;

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Xml.Document withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        @With
        Prolog prolog;

        Tag root;

        public Document withRoot(Tag root) {
            if (this.root == root) {
                return this;
            }
            return new Document(id, sourcePath, prefixUnsafe, markers, charsetName, charsetBomMarked, checksum, fileAttributes, prolog, root, eof);
        }

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

        @SuppressWarnings("unchecked")
        @Override
        public <S, T extends S> T service(Class<S> service) {
            if (WhitespaceValidationService.class.getName().equals(service.getName())) {
                return (T) new XmlWhitespaceValidationService();
            }
            return SourceFile.super.service(service);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Prolog implements Xml {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        @Override
        public Prolog withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;

        @Nullable
        XmlDecl xmlDecl;

        List<Misc> misc;

        List<JspDirective> jspDirectives;

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

        @Override
        public XmlDecl withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
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

        @Override
        public ProcessingInstruction withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
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

    @SuppressWarnings("unused")
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Tag implements Xml, Content {
        @EqualsAndHashCode.Include
        @With
        UUID id;

        @With
        String prefixUnsafe;

        /**
         * The map returned by this method is a view of the Tag's attributes.
         * Modifying the map will NOT modify the tag's attributes.
         *
         * @return a map of namespace prefixes (without the <code>xmlns</code> prefix) to URIs for this tag.
         */
        public Map<String, String> getNamespaces() {
            final Map<String, String> namespaces = new LinkedHashMap<>(attributes.size());
            if (!attributes.isEmpty()) {
                for (Attribute attribute : attributes) {
                    if(isNamespaceDefinitionAttribute(attribute.getKeyAsString())) {
                        namespaces.put(
                                extractPrefixFromNamespaceDefinition(attribute.getKeyAsString()),
                                attribute.getValueAsString());
                    }
                }
            }
            return namespaces;
        }

        /**
         * Gets a map containing all namespaces defined in the current scope, including all parent scopes.
         *
         * @param cursor     the cursor to search from
         * @return a map containing all namespaces defined in the current scope, including all parent scopes.
         */
        public Map<String, String> getAllNamespaces(Cursor cursor) {
            Map<String, String> namespaces = getNamespaces();
            while (cursor != null) {
                Xml.Tag enclosing = cursor.firstEnclosing(Xml.Tag.class);
                if (enclosing != null) {
                    for (Map.Entry<String, String> ns : enclosing.getNamespaces().entrySet()) {
                        if (namespaces.containsValue(ns.getKey())) {
                            throw new IllegalStateException(java.lang.String.format("Cannot have two namespaces with the same prefix (%s): '%s' and '%s'", ns.getKey(), namespaces.get(ns.getKey()), ns.getValue()));
                        }
                        namespaces.put(ns.getKey(), ns.getValue());
                    }
                }
                cursor = cursor.getParent();
            }

            return namespaces;
        }

        public Tag withNamespaces(Map<String, String> namespaces) {
            Map<String, String> currentNamespaces = getNamespaces();
            if (currentNamespaces.equals(namespaces)) {
                return this;
            }

            List<Xml.Attribute> attributes = this.attributes;
            if (attributes.isEmpty()) {
                for (Map.Entry<String, String> ns : namespaces.entrySet()) {
                    String key = getAttributeNameForPrefix(ns.getKey());
                    attributes = ListUtils.concat(attributes, new Xml.Attribute(
                            randomId(),
                            "",
                            Markers.EMPTY,
                            new Xml.Ident(
                                    randomId(),
                                    "",
                                    Markers.EMPTY,
                                    key
                            ),
                            "",
                            new Xml.Attribute.Value(
                                    randomId(),
                                    "",
                                    Markers.EMPTY,
                                    Xml.Attribute.Value.Quote.Double, ns.getValue()
                            )
                    ));
                }
            } else {
                Map<String, Xml.Attribute> attributeByKey = attributes.stream()
                        .collect(Collectors.toMap(
                                Attribute::getKeyAsString,
                                a -> a
                        ));

                for (Map.Entry<String, String> ns : namespaces.entrySet()) {
                    String key = getAttributeNameForPrefix(ns.getKey());
                    if (attributeByKey.containsKey(key)) {
                        Xml.Attribute attribute = attributeByKey.get(key);
                        if (!ns.getValue().equals(attribute.getValueAsString())) {
                            ListUtils.map(attributes, a -> a.getKeyAsString().equals(key)
                                    ? attribute.withValue(new Xml.Attribute.Value(randomId(), "", Markers.EMPTY, Xml.Attribute.Value.Quote.Double, ns.getValue()))
                                    : a
                            );
                        }
                    } else {
                        attributes = ListUtils.concat(attributes, new Xml.Attribute(
                                randomId(),
                                " ",
                                Markers.EMPTY,
                                new Xml.Ident(
                                        randomId(),
                                        "",
                                        Markers.EMPTY,
                                        key
                                ),
                                "",
                                new Xml.Attribute.Value(
                                        randomId(),
                                        "",
                                        Markers.EMPTY,
                                        Xml.Attribute.Value.Quote.Double, ns.getValue()
                                )
                        ));
                    }
                }
            }

            return new Tag(id, prefixUnsafe, markers, name, attributes, content, closing,
                    beforeTagDelimiterPrefix);
        }

        @Override
        public Tag withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
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
                    .map(Xml.Document.class::cast)
                    .orElseThrow(() -> new IllegalArgumentException("Could not parse as XML"))
                    .getRoot();
        }

        public Tag withName(String name) {
            if(!name.equals(name.trim())) {
                throw new IllegalArgumentException("Tag name must not contain leading or trailing whitespace");
            }
            if(this.name.equals(name)) {
                return this;
            }
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
            if (content.size() == 1 && content.get(0) instanceof Xml.CharData) {
                return Optional.ofNullable(((CharData) content.get(0)).getText());
            }
            if (content.stream().allMatch(c -> c instanceof Xml.CharData)) {
                return Optional.of(content.stream()
                        .map(c -> ((CharData) c).getText())
                        .map(StringEscapeUtils::unescapeXml)
                        .collect(Collectors.joining()));
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

        /**
         * @return The local name for this tag, without any namespace prefix.
         */
        public String getLocalName() {
            return extractLocalName(name);
        }

        /**
         * @return The namespace prefix for this tag, if any.
         */
        public Optional<String> getNamespacePrefix() {
            String extractedNamespacePrefix = extractNamespacePrefix(name);
            return Optional.ofNullable(StringUtils.isNotEmpty(extractedNamespacePrefix) ? extractedNamespacePrefix : null);
        }

        /**
         * @return The namespace URI for this tag, if any.
         */
        public Optional<String> getNamespaceUri(Cursor cursor) {
            Optional<String> maybeNamespacePrefix = getNamespacePrefix();
            return maybeNamespacePrefix.flatMap(s -> Optional.ofNullable(getAllNamespaces(cursor).get(s)));
        }

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

            @Override
            public Closing withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            @Override
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
            public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
                return v.visitTagClosing(this, p);
            }

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

        @Override
        public Attribute withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
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

            @Override
            public Value withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            @Override
            public String getPrefix() {
                return prefixUnsafe;
            }

            Markers markers;
            Quote quote;
            String value;

            @Override
            public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
                return v.visitAttributeValue(this, p);
            }
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

        @Override
        public CharData withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
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

        @Override
        public Comment withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;
        String text;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitComment(this, p);
        }

        @Override
        public String toString() {
            return "<!--" + text + "-->";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class DocTypeDecl implements Xml, Misc {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        @Override
        public DocTypeDecl withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;
        Ident name;

        @Nullable
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

            @Override
            public ExternalSubsets withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            @Override
            public String getPrefix() {
                return prefixUnsafe;
            }

            Markers markers;
            List<Element> elements;

            @Override
            public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
                return v.visitDocTypeDeclExternalSubsets(this, p);
            }

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

        @Override
        public Element withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
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

        @Override
        public Ident withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
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

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class JspDirective implements Xml, Content {
        @EqualsAndHashCode.Include
        @With
        UUID id;

        @With
        String prefixUnsafe;

        @Override
        public JspDirective withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
        public String getPrefix() {
            return prefixUnsafe;
        }

        @With
        Markers markers;

        @With
        String beforeTypePrefix;

        String type;

        public JspDirective withType(String type) {
            return new JspDirective(id, prefixUnsafe, markers, beforeTypePrefix, type, attributes,
                    beforeDirectiveEndPrefix);
        }

        @With
        List<Attribute> attributes;

        /**
         * Space before '%&gt;'
         */
        @With
        String beforeDirectiveEndPrefix;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitJspDirective(this, p);
        }

        @Override
        public String toString() {
            return "<%@ " + type + attributes.stream().map(a -> " " + a.getKey().getName() + "=\"" + a.getValueAsString() + "\"")
                    .collect(Collectors.joining("")) + "%>";
        }
    }
}
