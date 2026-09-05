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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.apache.commons.text.StringEscapeUtils;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.CommentService;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.WhitespaceValidationService;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.internal.WithPrefix;
import org.openrewrite.xml.internal.XmlPrinter;
import org.openrewrite.xml.internal.XmlWhitespaceValidationService;
import org.openrewrite.xml.service.XmlCommentService;

import java.lang.ref.SoftReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

/**
 * The XML <a href="https://www.w3.org/TR/xml11/#syntax">spec</a>.
 */
public interface Xml extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        //noinspection DataFlowIssue
        return (R) acceptXml(v.adapt(XmlVisitor.class), p);
    }

    default <P> @Nullable Xml acceptXml(XmlVisitor<P> v, P p) {
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

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Document implements Xml, SourceFileWithReferences {
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

        @With
        Tag root;

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
            } else if (CommentService.class.getName().equals(service.getName())) {
                return (T) new XmlCommentService();
            }
            return SourceFileWithReferences.super.service(service);
        }

        @Nullable
        @NonFinal
        transient SoftReference<References> references;

        @Override
        public References getReferences() {
            this.references = build(this.references);
            return Objects.requireNonNull(this.references.get());
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

        /**
         * The instruction's data, which the XML specification makes optional: {@code <?target?>}
         * carries a target but no data, so this is {@code null}.
         */
        @Nullable
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
            if (!name.equals(name.trim())) {
                throw new IllegalArgumentException("Tag name must not contain leading or trailing whitespace");
            }
            if (this.name.equals(name)) {
                return this;
            }
            return new Tag(id, prefixUnsafe, markers, name, attributes, content,
                    closing == null ? null : closing.withName(name),
                    beforeTagDelimiterPrefix, type);
        }

        public Tag withValue(String value) {
            CharData charData;
            if (content != null && content.get(0) instanceof CharData) {
                charData = ((CharData) content.get(0)).withText(value);
            } else {
                charData = new CharData(randomId(), "", Markers.EMPTY,
                        false, value, "");
            }
            return withContent(singletonList(charData));
        }

        @With
        List<Attribute> attributes;

        @Nullable
        List<? extends Content> content;

        public Optional<Tag> getChild(String name) {
            return content == null ? Optional.empty() : content.stream()
                    .filter(Xml.Tag.class::isInstance)
                    .map(Tag.class::cast)
                    .filter(t -> t.getName().equals(name))
                    .findAny();
        }

        public List<Tag> getChildren(String name) {
            return content == null ? emptyList() : content.stream()
                    .filter(Xml.Tag.class::isInstance)
                    .map(Tag.class::cast)
                    .filter(t -> t.getName().equals(name))
                    .collect(toList());
        }

        public List<Tag> getChildren() {
            return content == null ? emptyList() : content.stream()
                    .filter(Xml.Tag.class::isInstance)
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
                return Optional.of(((CharData) content.get(0)).getText());
            }
            if (content.stream().allMatch(Xml.CharData.class::isInstance)) {
                return Optional.of(content.stream()
                        .map(c -> ((CharData) c).getText())
                        .map(StringEscapeUtils::unescapeXml)
                        .collect(joining()));
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
                    beforeTagDelimiterPrefix, type);

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
         * XAML attestation: the resolved type of the element this tag denotes. Null for
         * plain XML documents and for unresolved XAML.
         *
         *   object element            &lt;Button ...&gt;         the control class
         *   property element          &lt;Button.Content&gt;     the property on the enclosing
         *                                                  element's type
         *   attached property element &lt;Grid.Row&gt; (inside a  the attached member on the
         *                             non-Grid child)      foreign owner type
         *
         * The name string stays verbatim (possibly xmlns-prefixed and/or dotted);
         * attestation resolves the parts. Distinguishing the three roles is this slot's
         * job, never a node-shape difference.
         */
        @With
        @Nullable
        JavaType type;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitTag(this, p);
        }

        @Override
        public String toString() {
            return "<" + name + attributes.stream().map(a -> " " + a.getKey().getName() + "=\"" + a.getValueAsString() + "\"")
                    .collect(joining("")) + ">";
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
        AttributeValue value;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitAttribute(this, p);
        }

        /**
         * The polymorphic attribute-value slot — THE structural change XAML requires
         * of the XML model. Plain XML attribute values are string leaves
         * ({@link Value}, unchanged); XAML attribute values may instead carry a parsed
         * expression tree ({@link ExpressionValue}) because markup extensions nest:
         *
         *   Text="hello"                               Value (exactly as today)
         *   Text="{Binding Path=FirstName}"            ExpressionValue(MarkupExtension)
         *   Text="{Binding Converter={StaticResource c}}"   nested extension tree
         *   Text="{}{not an extension}"                Value — a value starting with
         *                                              the {} escape is by definition
         *                                              plain text, never an extension
         *
         * XML documents are untouched: their values remain {@link Value} instances, so
         * existing serialized LSTs deserialize unchanged.
         */
        public interface AttributeValue extends Xml {
        }

        /**
         * A XAML attribute value holding a parsed expression rather than a raw string.
         * Quote style is preserved exactly as in {@link Value}; after captures the gap
         * between the end of the expression and the closing quote:
         *
         *   Text="{Binding X} "        after = " "  (WPF rejects trailing whitespace
         *                              at load — SR.WhitespaceAfterME — but files ship
         *                              with it, so the model must round-trip it)
         */
        @lombok.Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class ExpressionValue implements AttributeValue {
            @EqualsAndHashCode.Include
            UUID id;

            String prefixUnsafe;

            @Override
            public ExpressionValue withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            @Override
            public String getPrefix() {
                return prefixUnsafe;
            }

            Markers markers;
            Value.Quote quote;
            Expression expression;
            String after;
        }

        @lombok.Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class Value implements AttributeValue {
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

        /**
         * The raw string form of the value. For XAML expression values there is no
         * stored string (the tree is the source of truth); printing the expression is
         * the printer's job, so this returns empty for {@link ExpressionValue}.
         */
        public String getValueAsString() {
            return value instanceof Value ? ((Value) value).getValue() : "";
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
            if (StringUtils.isNotEmpty(afterText)) {
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
    @AllArgsConstructor(onConstructor_ = {@JsonCreator})
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
        String documentDeclaration;

        // Override lombok default getter to avoid backwards compatibility problems with old LSTs
        public String getDocumentDeclaration() {
            //noinspection ConstantValue
            if (documentDeclaration == null) {
                return "DOCTYPE";
            }
            return documentDeclaration;
        }

        @Nullable
        Ident externalId;

        List<Ident> internalSubset;

        @Nullable
        ExternalSubsets externalSubsets;

        /**
         * Space before '&gt;'.
         */
        String beforeTagDelimiterPrefix;

        public DocTypeDecl(UUID id, String prefix, Markers markers, Ident name, Ident externalId, List<Ident> internalSubset, ExternalSubsets externalSubsets, String beforeTagDelimiterPrefix) {
            this(id,
                    prefix,
                    markers,
                    name,
                    "DOCTYPE",
                    externalId,
                    internalSubset,
                    externalSubsets,
                    beforeTagDelimiterPrefix);
        }

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

        /**
         * XAML attestation: the resolved meaning of this name token. Null for plain XML
         * and for unresolved XAML. This slot is what gives tokens semantic identity
         * WITHOUT node-type proliferation:
         *
         *   attribute key      Mode           the Binding.Mode property
         *   attribute key      Click          the routed event
         *   directive key      x:Name         the XAML language directive
         *   extension name     Binding        the extension class (spelling verbatim,
         *                                     incl. the optional "Extension" suffix)
         *   path member        Customer       the CLR property (JavaType.Variable) —
         *                                     the hook that propagates ViewModel
         *                                     renames into XAML
         *
         * The name string is verbatim source, including any xmlns prefix and dots
         * (local:MyControl, Grid.Column); attestation resolves the parts. Never
         * normalized.
         */
        @Nullable
        JavaType type;

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
                    .collect(joining("")) + "%>";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class JspScriptlet implements Xml, Content {
        @EqualsAndHashCode.Include
        @With
        UUID id;

        @With
        String prefixUnsafe;

        @Override
        public JspScriptlet withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
        public String getPrefix() {
            return prefixUnsafe;
        }

        @With
        Markers markers;

        @With
        String content;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitJspScriptlet(this, p);
        }

        @Override
        public String toString() {
            return "<% " + content + " %>";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class JspExpression implements Xml, Content {
        @EqualsAndHashCode.Include
        @With
        UUID id;

        @With
        String prefixUnsafe;

        @Override
        public JspExpression withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
        public String getPrefix() {
            return prefixUnsafe;
        }

        @With
        Markers markers;

        @With
        String content;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitJspExpression(this, p);
        }

        @Override
        public String toString() {
            return "<%= " + content + " %>";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class JspDeclaration implements Xml, Content, Misc {
        @EqualsAndHashCode.Include
        @With
        UUID id;

        @With
        String prefixUnsafe;

        @Override
        public JspDeclaration withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
        public String getPrefix() {
            return prefixUnsafe;
        }

        @With
        Markers markers;

        @With
        String content;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitJspDeclaration(this, p);
        }

        @Override
        public String toString() {
            return "<%! " + content + " %>";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class JspComment implements Xml, Content, Misc {
        @EqualsAndHashCode.Include
        @With
        UUID id;

        @With
        String prefixUnsafe;

        @Override
        public JspComment withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
        public String getPrefix() {
            return prefixUnsafe;
        }

        @With
        Markers markers;

        @With
        String content;

        @Override
        public <P> Xml acceptXml(XmlVisitor<P> v, P p) {
            return v.visitJspComment(this, p);
        }

        @Override
        public String toString() {
            return "<%-- " + content + " --%>";
        }
    }

    // ========================================================================
    // XAML VALUE LAYER — the attribute-value / binding mini-language.
    //
    // Everything below exists to cover XAML (WPF, UWP/WinUI incl. x:Bind,
    // Xamarin.Forms / .NET MAUI, Avalonia, legacy Silverlight) on top of the XML
    // model. Plain XML documents never contain these nodes. Grammar was verified
    // against the platform sources (WPF MeScanner/MePullParser/PathParser/
    // GenericTypeNameParser, Avalonia BindingExpressionGrammar, MAUI
    // MarkupExpressionParser, XamlX SystemXamlMarkupExtensionParser).
    //
    // Modelling rules (same as the rest of this file):
    // - prefix = all whitespace left of the node's first token; interior gaps get
    //   named before* fields. Punctuation is implicit; an optional token's presence
    //   is encoded by its nullable before* field being non-null.
    // - Parse is purely syntactic: tree shape never depends on type resolution.
    //   Semantics attach afterwards as JavaType attestation. Keyword/enum value
    //   tokens (FindAncestor, TwoWay) are Literal/Ident nodes attested to enum
    //   members — never distinct node types.
    // - Literal text is the RAW source slice (quotes kept, backslash/caret escapes
    //   unresolved, leading {} escape kept). Any value text that does not conform
    //   to the closed grammar stays a verbatim Literal (e.g. MAUI expression
    //   bindings such as "{Binding Price * this.TaxRate}") — never a parse error.
    // ========================================================================

    /**
     * Anything legal in a XAML value position: an attribute value expression, a
     * markup-extension argument, a function-call argument. Implementations:
     * Literal, MarkupExtension, PropertyPath, Negation, TypeName.
     */
    interface Expression extends Xml {
    }

    /**
     * Verbatim uninterpreted text in a value position — the most common node.
     * text is the EXACT source slice: surrounding ' or " quotes kept, backslash
     * escapes (WPF/MAUI) and caret escapes (WPF indexer params) unresolved, leading
     * {} escape kept, interior whitespace kept. A helper (not a field) can expose
     * the cooked value per dialect unescaping rules. WPF, Avalonia, MAUI and XamlX
     * all store only the decoded+trimmed value; the raw slice is what makes this
     * model lossless.
     *
     * Covers, with semantic identity supplied by type attestation:
     *
     *   plain values          Text="hello"   Width="120.5"
     *   enum keyword tokens   Mode=TwoWay    {RelativeSource FindAncestor}'s
     *                         positional arg — attests to the enum member
     *   event handler names   Click="Save_Click" — attests to the handler method
     *   resource keys         {StaticResource accentBrush}
     *   quoted args           ConverterParameter='a, b'  (comma inert in quotes)
     *                         StringFormat='{}{0:N2}'
     *   mixed quoting         Text=abc'def'  (MAUI allows a quote mid-piece)
     *   embedded braces       {Binding Foo{Bar}} — balanced braces inside an
     *                         unquoted value are legal WPF v3-compat text
     *   x:Bind constants      {x:Bind Format(Value, 'N2')}'s second arg
     *   fallback              any value text outside the closed grammar
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Literal implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        @Override
        public Literal withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;
        String text;

        @Nullable
        JavaType type;
    }

    /**
     * A markup extension — ONE node type for every extension kind in every dialect:
     *
     *   WPF       {Binding} {StaticResource} {DynamicResource} {TemplateBinding}
     *             {RelativeSource} {x:Static} {x:Type} {x:Null} {x:Array}
     *   WinUI     {ThemeResource} {CustomResource} {x:Bind}
     *   MAUI/XF   {AppThemeBinding} {OnPlatform} {OnIdiom} {x:Reference}
     *   Avalonia  {CompiledBinding} {ReflectionBinding}
     *   any       third-party MarkupExtension subclass
     *
     * Which one it is = type attestation (xmlns resolution; WPF/MAUI try both the
     * literal name and name+"Extension" — typeName keeps the source spelling),
     * never a subclass.
     *
     * Token/field map:
     *
     *   { Binding  Path=FirstName , Mode=TwoWay }
     *   A B       C                D            E
     *
     *   A prefix (before the opening brace, or before the quote when quoted)
     *   B typeName.prefix (after the brace)
     *   C arguments.get(0).prefix
     *   D arguments.get(0).beforeComma (gap before the separating comma)
     *   E beforeClosingBrace
     *
     * Nesting is direct tree recursion — the reason this layer exists:
     *   {Binding Converter={StaticResource conv}, Path=A.B}
     * A nested extension may itself be QUOTED (WPF scanner token
     * QuotedMarkupExtension): Converter='{StaticResource conv}' — encoded by
     * non-null quote; the quote characters wrap the braces tightly.
     *
     * Ordering: WPF enforces positional-before-named; MAUI allows interleaving.
     * The order-preserving arguments list represents both; the invariant is a
     * dialect validation, not a shape.
     *
     * Implements Content: extension syntax is legal initialization text —
     *   &lt;TextBlock.Text&gt;{Binding Foo}&lt;/TextBlock.Text&gt;
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class MarkupExtension implements Expression, Content {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        @Override
        public MarkupExtension withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;

        /**
         * Non-null when this nested extension is quoted: Converter='{...}'
         */
        Attribute.Value.@Nullable Quote quote;

        /**
         * Binding, x:Static, local:MyExt — verbatim spelling incl. any Extension
         * suffix; resolved class in typeName.type.
         */
        Ident typeName;

        List<Argument> arguments;

        /**
         * Whitespace before the closing brace.
         */
        String beforeClosingBrace;
    }

    /**
     * One argument of a MarkupExtension or an x:Bind function call. Positional and
     * named forms are ONE node distinguished by name nullability — mirroring how
     * the WPF scanner itself disambiguates (a value run that hits = is re-labelled
     * PropertyName; there is no parser lookahead):
     *
     *   positional   {Binding FirstName}     name null, value PropertyPath
     *                {StaticResource key}    name null, value Literal
     *   named        Mode=TwoWay             name attests to Binding.Mode,
     *                                        value Literal attests to the enum
     *
     * beforeComma is the gap before the trailing comma separator and is null on
     * the last argument (the gap before the closing brace is
     * MarkupExtension.beforeClosingBrace).
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Argument implements Xml {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        @Override
        public Argument withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;

        @Nullable
        Ident name;

        /**
         * Whitespace before '='; null iff positional.
         */
        @Nullable
        String beforeEquals;

        Expression value;

        /**
         * Whitespace before the following ','; null on the last argument.
         */
        @Nullable
        String beforeComma;
    }

    /**
     * The property-path mini-language: the value of Path= / the positional path of
     * {Binding}, {x:Bind}, {TemplateBinding}, {CompiledBinding}, MAUI/XF bindings.
     * A flat sequence of segments; each segment records the ACCESSOR token that
     * precedes it. There is no whole-path stream flag — the caret is a segment
     * (Avalonia allows it mid-path and repeated).
     *
     *   Customer.Address[0].(Validation.Errors)[0].ErrorContent
     *   Property Property   Paren               Indexer Property
     *   NONE     DOT   Indexer(NONE)  DOT       NONE    DOT
     *
     * Special shapes (all verified against platform sources):
     *
     *   empty path       {Binding}            segments empty — bind to the source
     *   bare dot         Path=.               one Property, accessor DOT, empty name
     *   leading slash    /Items/Name          WPF collection-view current item
     *   leading dots     .Foo  ..Foo          WPF keeps leading dots in the member
     *                                         name (XLinq); name is verbatim
     *   Avalonia stream  Foo^.Bar^   ^   .^   Stream segments, repeatable
     *   null-conditional Foo?.Bar             accessor NULL_CONDITIONAL (Avalonia)
     *
     * Fallback: path text not matching this closed grammar (MAUI expression
     * bindings: {Binding Price * this.TaxRate}) stays a verbatim Literal. WPF trims
     * segment-interior whitespace when it parses; this model stores exactly what
     * the source had.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class PropertyPath implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        @Override
        public PropertyPath withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;
        List<Segment> segments;

        /**
         * A step in a PropertyPath. The accessor is the token PRECEDING the step;
         * the step's prefix is the whitespace before that token (or before the
         * step's first own token when NONE).
         *
         *   NONE              first segment, or one attaching directly:  Foo[0]
         *   DOT               .   member step (all dialects)
         *   SLASH             /   WPF collection-view current-item drill-in;
         *                         may precede an Indexer too:  /Items/[0]
         *   NULL_CONDITIONAL  ?.  Avalonia null-conditional member access
         */
        public interface Segment extends Xml {
            Accessor getAccessor();

            enum Accessor {
                NONE, DOT, SLASH, NULL_CONDITIONAL
            }
        }

        /**
         * A plain member step: Customer, Address. name.type attests to the
         * resolved CLR property (JavaType.Variable) — the hook that propagates
         * ViewModel renames into XAML. Exact under compiled bindings (x:Bind,
         * x:DataType); heuristic under reflection bindings (DataContext inference
         * ladder). In Avalonia a trailing member may resolve to a METHOD (command
         * binding) — same syntax, method attestation, no parentheses involved.
         */
        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class Property implements Segment {
            @EqualsAndHashCode.Include
            UUID id;

            String prefixUnsafe;

            @Override
            public Property withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            @Override
            public String getPrefix() {
                return prefixUnsafe;
            }

            Markers markers;
            Accessor accessor;
            Ident name;
        }

        /**
         * An indexer step:  [0]  [FirstName]  [a,b]  [(sys:Int32)42]  [15][16]
         * prefix is the gap before the opening bracket. Each parameter is an
         * optional parenthesized group followed by an optional value literal — the
         * paren group's meaning is three-way in WPF and resolved by ATTESTATION,
         * never by node shape:
         *
         *   [(sys:Int32)42]  paren = type       value = 42    typed indexer param
         *   [(2)]            paren = param idx  value absent  PathParameters[2]
         *   [(abc)]          paren = literal    value absent  the string "(abc)"
         *   [foo]            paren absent       value = foo
         *
         * Param values are verbatim Literals: WPF's indexer escape char is CARET
         * (escapes comma, parens, bracket and caret itself — a different escape
         * rule than the extension level's backslash), nested balanced brackets are
         * legal inside a value, and MAUI treats the whole bracket content as one
         * opaque string that may contain quoted commas (['x, y']) — the parser
         * splits params only on commas outside quotes/brackets, and the raw text
         * survives in the Literals regardless.
         */
        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class Indexer implements Segment {
            @EqualsAndHashCode.Include
            UUID id;

            String prefixUnsafe;

            @Override
            public Indexer withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            @Override
            public String getPrefix() {
                return prefixUnsafe;
            }

            Markers markers;
            Accessor accessor;
            List<Param> parameters;
            String beforeClosingBracket;

            /**
             * One indexer parameter.
             */
            @Value
            @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
            @With
            public static class Param implements Xml {
                @EqualsAndHashCode.Include
                UUID id;

                String prefixUnsafe;

                @Override
                public Param withPrefix(String prefix) {
                    return WithPrefix.onlyIfNotEqual(this, prefix);
                }

                @Override
                public String getPrefix() {
                    return prefixUnsafe;
                }

                Markers markers;

                @Nullable
                ParenGroup paren;

                @Nullable
                Literal value;

                /**
                 * Before the trailing ','; null on the last parameter.
                 */
                @Nullable
                String beforeComma;
            }
        }

        /**
         * A parenthesized path step — one grammar, several dialect meanings, all
         * resolved by attestation (WPF splits on the LAST dot, so a dotted owner
         * is legal):
         *
         *   (Grid.Row)              attached property      owner Grid, member Row
         *   (local:Owner.Prop)      prefixed attached      owner local:Owner
         *   (A.B.C)                 owner A.B, member C    (last-dot split)
         *   (Validation.Errors)[0]  attached + indexer step
         *   (Foo)                   WPF simple paren member / Avalonia type CAST —
         *                           owner null, member Foo; attestation decides
         *   (0)                     WPF parameter index into PathParameters
         *   (abc)                   WPF literal paren string
         *
         * Avalonia disambiguates cast-vs-attached the same lexical way (no dot
         * means cast), so shape-wise one node suffices; only the double-paren cast
         * form gets its own node (Cast).
         */
        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class Paren implements Segment {
            @EqualsAndHashCode.Include
            UUID id;

            String prefixUnsafe;

            @Override
            public Paren withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            @Override
            public String getPrefix() {
                return prefixUnsafe;
            }

            Markers markers;
            Accessor accessor;

            @Nullable
            Ident owner;

            /**
             * Whitespace before the owner/member dot; null when owner is null.
             */
            @Nullable
            String beforeDot;

            Ident member;
            String beforeClosingParen;
        }

        /**
         * The double-paren cast form — shared grammar of WinUI x:Bind and Avalonia
         * bindings (Avalonia allows it mid-path in ordinary {Binding}):
         *
         *   ((TextBox)obj).Text                    x:Bind
         *   $parent.((Button)DataContext).Tag      Avalonia
         *
         * prefix is before the OUTER paren; type is the inner (ns:Type) group; the
         * operand (member and optional indexer) sits inside the outer parens.
         */
        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class Cast implements Segment {
            @EqualsAndHashCode.Include
            UUID id;

            String prefixUnsafe;

            @Override
            public Cast withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            @Override
            public String getPrefix() {
                return prefixUnsafe;
            }

            Markers markers;
            Accessor accessor;
            ParenGroup type;
            PropertyPath operand;
            String beforeClosingParen;
        }

        /**
         * WinUI/UWP x:Bind function binding — the one dialect where a path step
         * takes arguments:
         *
         *   {x:Bind ViewModel.Format(Item.Value, 'N2'), Mode=OneWay}
         *
         * Arguments reuse Argument (always positional; values are PropertyPaths or
         * Literal constants). name.type attests to the resolved method. Gated on
         * the x:Bind context, which is syntactic (x: resolves via in-document
         * xmlns). NOT valid in WPF, MAUI, or Avalonia paths — Avalonia
         * method/command bindings are a bare identifier, never a call.
         */
        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class FunctionCall implements Segment {
            @EqualsAndHashCode.Include
            UUID id;

            String prefixUnsafe;

            @Override
            public FunctionCall withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            @Override
            public String getPrefix() {
                return prefixUnsafe;
            }

            Markers markers;
            Accessor accessor;
            Ident name;
            String beforeOpenParen;
            List<Argument> arguments;
            String beforeClosingParen;
        }

        /**
         * Avalonia element-name step:  {Binding #slider.Value}
         * WPF's ElementName= concept expressed inside the path grammar (root-only
         * in Avalonia), hence a distinct-grammar node. name.type attests to the
         * x:Name'd element's type. prefix is before the hash.
         */
        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class Element implements Segment {
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
            Accessor accessor;
            Ident name;
        }

        /**
         * Avalonia relative-source step (root-only):
         *
         *   $self
         *   $parent
         *   $parent[Border]              ancestorType only
         *   $parent[2]                   ancestorLevel only (an integer token is
         *                                parsed into ancestorLevel, not a type —
         *                                lexical: digits cannot start a type name)
         *   $parent[local:MyControl;1]   both, SEMICOLON separated
         *
         * Nullable before* fields encode which tokens are present. WPF's
         * equivalent ({RelativeSource FindAncestor, AncestorType={x:Type Border},
         * AncestorLevel=1}) needs NO nodes here — it is plain MarkupExtension
         * structure; only Avalonia gave the concept its own grammar. prefix is
         * before the dollar sign.
         */
        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class Relative implements Segment {
            @EqualsAndHashCode.Include
            UUID id;

            String prefixUnsafe;

            @Override
            public Relative withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            @Override
            public String getPrefix() {
                return prefixUnsafe;
            }

            Markers markers;
            Accessor accessor;
            Kind kind;

            @Nullable
            String beforeOpenBracket;

            @Nullable
            Ident ancestorType;

            @Nullable
            String beforeSemicolon;

            @Nullable
            Literal ancestorLevel;

            @Nullable
            String beforeClosingBracket;

            public enum Kind {
                SELF, PARENT
            }
        }

        /**
         * The Avalonia stream operator as a path STEP — it binds the value
         * produced so far (IObservable/Task), may appear mid-path and repeatedly,
         * so it cannot be a whole-path flag:
         *
         *   Activity^     [Property, Stream(NONE)]
         *   Foo^.Bar^     [Property, Stream(NONE), Property(DOT), Stream(NONE)]
         *   ^             [Stream(NONE)]        stream of the source itself
         *   .^            [Stream(DOT)]
         *
         * prefix is before the caret. Not valid in WPF/WinUI/MAUI.
         */
        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class Stream implements Segment {
            @EqualsAndHashCode.Include
            UUID id;

            String prefixUnsafe;

            @Override
            public Stream withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            @Override
            public String getPrefix() {
                return prefixUnsafe;
            }

            Markers markers;
            Accessor accessor;
        }
    }

    /**
     * Avalonia logical-negation prefix on a binding value: {Binding !IsEnabled}.
     * Wraps its operand, so the to-bool double-negation !!Items.Count is two
     * nested Negations — nesting matches the operator semantics and keeps this one
     * node type. Only legal as a leading run (Avalonia rejects mid-path bangs);
     * that is a parser/validation concern, not a shape. prefix is before the bang.
     * Not valid WPF/WinUI/MAUI grammar.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Negation implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        @Override
        public Negation withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;
        Expression operand;
    }

    /**
     * A XAML type-name with optional generic arguments and array subscripts —
     * XAML's generic syntax uses PARENTHESES, and WPF's GenericTypeNameParser
     * additionally accepts array subscripts folded onto the name:
     *
     *   x:TypeArguments="sys:String"
     *   x:TypeArguments="scg:List(sys:String)"
     *   x:TypeArguments="scg:Dictionary(sys:String, scg:List(sys:Int32))"  nests
     *   x:TypeArguments="sys:String[]"     subscript = "[]"
     *   x:TypeArguments="sys:Int32[,][]"   subscript = "[,][]" (verbatim; rank
     *                                      derivable; interior spaces "[ , ]" are
     *                                      legal and kept)
     *
     * Parsed as TypeName only where the grammar guarantees a type: x:TypeArguments
     * values (the x: prefix resolves syntactically via in-document xmlns).
     * Everywhere else type-valued tokens ({x:Type Grid}'s positional arg,
     * DataType="local:Person", cast/ancestor type tokens) are lexically ordinary
     * tokens and stay Literal/Ident with JavaType attestation — parse shape must
     * not depend on resolution.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class TypeName implements Expression {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        @Override
        public TypeName withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;

        /**
         * Verbatim, possibly xmlns-prefixed: scg:List — resolved type in name.type.
         */
        Ident name;

        /**
         * Null when non-generic.
         */
        @Nullable
        GenericArguments typeArguments;

        /**
         * Verbatim array subscript run incl. any interior whitespace; null if none.
         */
        @Nullable
        String subscript;

        /**
         * The parenthesized argument list:  ( sys:String , sys:Int32 )
         */
        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class GenericArguments implements Xml {
            @EqualsAndHashCode.Include
            UUID id;

            String prefixUnsafe;

            @Override
            public GenericArguments withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            @Override
            public String getPrefix() {
                return prefixUnsafe;
            }

            Markers markers;
            List<TypeArgument> arguments;
            String beforeClosingParen;
        }

        /**
         * One generic argument with its trailing-comma gap.
         */
        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class TypeArgument implements Xml {
            @EqualsAndHashCode.Include
            UUID id;

            String prefixUnsafe;

            @Override
            public TypeArgument withPrefix(String prefix) {
                return WithPrefix.onlyIfNotEqual(this, prefix);
            }

            @Override
            public String getPrefix() {
                return prefixUnsafe;
            }

            Markers markers;
            TypeName type;

            /**
             * Before the trailing ','; null on the last argument.
             */
            @Nullable
            String beforeComma;
        }
    }

    /**
     * A parenthesized single token — the shared shape for contexts where parens
     * wrap one uninterpreted token whose meaning is attestation's job:
     *
     *   indexer param prefix  [(sys:Int32)42]  [(2)]  [(abc)]  see Indexer.Param
     *   cast target type      ((TextBox)obj)                   see Cast
     *
     * token is a verbatim Literal (attestation resolves it to a type, a parameter
     * index, or nothing). Distinct from the Paren path segment, whose content has
     * owner-dot-member structure. prefix is before the opening paren.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ParenGroup implements Xml {
        @EqualsAndHashCode.Include
        UUID id;

        String prefixUnsafe;

        @Override
        public ParenGroup withPrefix(String prefix) {
            return WithPrefix.onlyIfNotEqual(this, prefix);
        }

        @Override
        public String getPrefix() {
            return prefixUnsafe;
        }

        Markers markers;
        Literal token;
        String beforeClosingParen;
    }
}
