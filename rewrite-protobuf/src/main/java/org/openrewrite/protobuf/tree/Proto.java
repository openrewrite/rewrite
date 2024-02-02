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
package org.openrewrite.protobuf.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.protobuf.ProtoVisitor;
import org.openrewrite.protobuf.internal.ProtoPrinter;

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface Proto extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptProto(v.adapt(ProtoVisitor.class), p);
    }

    @Nullable
    default <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(ProtoVisitor.class);
    }

    Space getPrefix();

    <P extends Proto> P withPrefix(Space prefix);

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Block implements Proto {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        List<ProtoRightPadded<Proto>> statements;

        @With
        @Getter
        Space end;

        public List<Proto> getStatements() {
            return ProtoRightPadded.getElements(statements);
        }

        public Block withStatements(List<Proto> statements) {
            return getPadding().withStatements(ProtoRightPadded.withElements(this.statements, statements));
        }

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitBlock(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Block t;

            public List<ProtoRightPadded<Proto>> getStatements() {
                return t.statements;
            }

            public Block withStatements(List<ProtoRightPadded<Proto>> statements) {
                return t.statements == statements ? t : new Block(t.id, t.prefix, t.markers, statements, t.end);
            }
        }
    }

    @Value
    @With
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    class Constant implements Proto {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Object value;
        String valueSource;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitConstant(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Document implements Proto, SourceFile {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Path sourcePath;

        @With
        @Getter
        @Nullable
        FileAttributes fileAttributes;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
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
        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        @With
        @Getter
        Syntax syntax;

        List<ProtoRightPadded<Proto>> body;

        public List<Proto> getBody() {
            return ProtoRightPadded.getElements(body);
        }

        public Document withBody(List<Proto> body) {
            return getPadding().withBody(ProtoRightPadded.withElements(this.body, body));
        }

        @With
        @Getter
        Space eof;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitDocument(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new ProtoPrinter<>();
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Document t;

            public List<ProtoRightPadded<Proto>> getBody() {
                return t.body;
            }

            public Document withBody(List<ProtoRightPadded<Proto>> body) {
                return t.body == body ? t : new Document(t.id, t.sourcePath, t.fileAttributes, t.prefix, t.markers, t.charsetName, t.charsetBomMarked, t.checksum, t.syntax, body, t.eof);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Empty implements Proto {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitEmpty(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Enum implements Proto {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Identifier name;
        Block body;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitEnum(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Extend implements Proto {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        FullIdentifier name;
        Block body;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitExtend(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Field implements FullName {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        /**
         * Required for a regular field, and required to be null for a oneof field.
         */
        @Nullable
        @With
        @Getter
        Keyword label;

        @With
        @Getter
        TypeTree type;

        ProtoRightPadded<Identifier> name;

        public Identifier getName() {
            return name.getElement();
        }

        public Field withName(Identifier fieldName) {
            return getPadding().withName(this.name.withElement(fieldName));
        }

        @With
        @Getter
        Constant number;

        @Nullable
        ProtoContainer<Option> options;

        @Nullable
        public List<Option> getOptions() {
            return options == null ? null : options.getElements();
        }

        public Field withOptions(List<Option> fieldOptions) {
            return getPadding().withOptions(ProtoContainer.withElementsNullable(this.options, fieldOptions));
        }

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitField(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Field t;

            public ProtoRightPadded<Identifier> getName() {
                return t.name;
            }

            public Field withName(ProtoRightPadded<Identifier> name) {
                return t.name == name ? t : new Field(t.id, t.prefix, t.markers, t.label, t.type, name, t.number, t.options);
            }

            @Nullable
            public ProtoContainer<Option> getOptions() {
                return t.options;
            }

            public Field withOptions(@Nullable ProtoContainer<Option> fieldOptions) {
                return t.options == fieldOptions ? t : new Field(t.id, t.prefix, t.markers, t.label, t.type, t.name, t.number, fieldOptions);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class ExtensionName implements FullName {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        ProtoRightPadded<FullIdentifier> extension;

        public FullIdentifier getExtension() {
            return extension.getElement();
        }

        public ExtensionName withExtension(FullIdentifier extension) {
            return getPadding().withExtension(this.extension.withElement(extension));
        }

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitExtensionName(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ExtensionName t;

            public ProtoRightPadded<FullIdentifier> getExtension() {
                return t.extension;
            }

            public ExtensionName withExtension(ProtoRightPadded<FullIdentifier> extension) {
                return t.extension == extension ? t : new ExtensionName(t.id, t.prefix, t.markers, extension);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class EnumField implements Proto {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        ProtoRightPadded<Identifier> name;

        public Identifier getName() {
            return name.getElement();
        }

        public EnumField withName(Identifier fieldName) {
            return getPadding().withName(this.name.withElement(fieldName));
        }

        @With
        @Getter
        Constant number;

        @Nullable
        ProtoContainer<Option> options;

        @Nullable
        public List<Option> getOptions() {
            return options == null ? null : options.getElements();
        }

        public EnumField withOptions(List<Option> fieldOptions) {
            return getPadding().withOptions(ProtoContainer.withElementsNullable(this.options, fieldOptions));
        }

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitEnumField(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final EnumField t;

            public ProtoRightPadded<Identifier> getName() {
                return t.name;
            }

            public EnumField withName(ProtoRightPadded<Identifier> name) {
                return t.name == name ? t : new EnumField(t.id, t.prefix, t.markers, name, t.number, t.options);
            }

            @Nullable
            public ProtoContainer<Option> getOptions() {
                return t.options;
            }

            public EnumField withOptions(@Nullable ProtoContainer<Option> options) {
                return t.options == options ? t : new EnumField(t.id, t.prefix, t.markers, t.name, t.number, options);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class FullIdentifier implements FullName, TypeTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @Nullable
        ProtoRightPadded<FullName> target;

        @Nullable
        public FullName getTarget() {
            return target == null ? null : target.getElement();
        }

        @Nullable
        public FullName withTarget(@Nullable FullName target) {
            if (target == null) {
                return getPadding().withTarget(null);
            }
            return getPadding().withTarget(this.target == null ?
                    ProtoRightPadded.build(target) :
                    this.target.withElement(target));
        }

        @With
        @Getter
        Identifier name;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitFullIdentifier(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final FullIdentifier t;

            @Nullable
            public ProtoRightPadded<FullName> getTarget() {
                return t.target;
            }

            public FullIdentifier withTarget(@Nullable ProtoRightPadded<FullName> target) {
                return t.target == target ? t : new FullIdentifier(t.id, t.prefix, t.markers, target, t.name);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Identifier implements Proto {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String name;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitIdentifier(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Import implements Proto {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @EqualsAndHashCode.Include
        @With
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        @Nullable
        Keyword modifier;

        ProtoRightPadded<StringLiteral> name;

        public StringLiteral getName() {
            return name.getElement();
        }

        public Import withName(StringLiteral name) {
            return getPadding().withName(this.name.withElement(name));
        }

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitImport(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Import t;

            public ProtoRightPadded<StringLiteral> getName() {
                return t.name;
            }

            public Import withName(ProtoRightPadded<StringLiteral> name) {
                return t.name == name ? t : new Import(t.id, t.prefix, t.markers, t.modifier, name);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Keyword implements Proto {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String keyword;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitKeyword(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class MapField implements Proto {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        ProtoRightPadded<Keyword> map;

        ProtoRightPadded<Keyword> keyType;

        public Keyword getKeyType() {
            return keyType.getElement();
        }

        public MapField withKeyType(Keyword keyType) {
            return getPadding().withKeyType(getPadding().getKeyType().withElement(keyType));
        }

        ProtoRightPadded<TypeTree> valueType;

        public TypeTree getValueType() {
            return valueType.getElement();
        }

        public MapField withValueType(TypeTree valueType) {
            return getPadding().withValueType(getPadding().getValueType().withElement(valueType));
        }

        ProtoRightPadded<Identifier> name;

        public Identifier getName() {
            return name.getElement();
        }

        public MapField withName(Identifier name) {
            return getPadding().withName(getPadding().getName().withElement(name));
        }

        @With
        @Getter
        Constant number;

        @Nullable
        ProtoContainer<Option> options;

        @Nullable
        public List<Option> getOptions() {
            return options == null ? null : options.getElements();
        }

        public MapField withOptions(@Nullable List<Option> options) {
            return getPadding().withOptions(ProtoContainer.withElementsNullable(this.options, options));
        }

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitMapField(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final MapField t;

            public ProtoRightPadded<Keyword> getMap() {
                return t.map;
            }

            public MapField withMap(ProtoRightPadded<Keyword> map) {
                return t.map == map ? t : new MapField(t.id, t.prefix, t.markers, map, t.keyType, t.valueType, t.name, t.number, t.options);
            }

            public ProtoRightPadded<Keyword> getKeyType() {
                return t.keyType;
            }

            public MapField withKeyType(ProtoRightPadded<Keyword> keyType) {
                return t.keyType == keyType ? t : new MapField(t.id, t.prefix, t.markers, t.map, keyType, t.valueType, t.name, t.number, t.options);
            }

            public ProtoRightPadded<TypeTree> getValueType() {
                return t.valueType;
            }

            public MapField withValueType(ProtoRightPadded<TypeTree> valueType) {
                return t.valueType == valueType ? t : new MapField(t.id, t.prefix, t.markers, t.map, t.keyType, valueType, t.name, t.number, t.options);
            }

            public ProtoRightPadded<Identifier> getName() {
                return t.name;
            }

            public MapField withName(ProtoRightPadded<Identifier> name) {
                return t.name == name ? t : new MapField(t.id, t.prefix, t.markers, t.map, t.keyType, t.valueType, name, t.number, t.options);
            }

            @Nullable
            public ProtoContainer<Option> getOptions() {
                return t.options;
            }

            public MapField withOptions(@Nullable ProtoContainer<Option> options) {
                return t.options == options ? t : new MapField(t.id, t.prefix, t.markers, t.map, t.keyType, t.valueType, t.name, t.number, options);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Message implements Proto {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Identifier name;
        Block body;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitMessage(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class OneOf implements Proto {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Identifier name;
        Block fields;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitOneOf(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Option implements Proto {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        ProtoRightPadded<FullName> name;

        public FullName getName() {
            return name.getElement();
        }

        public Field.Option withName(FullName name) {
            return getPadding().withName(getPadding().getName().withElement(name));
        }

        @With
        @Getter
        Constant assignment;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitOption(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Field.Option t;

            public ProtoRightPadded<FullName> getName() {
                return t.name;
            }

            public Field.Option withName(ProtoRightPadded<FullName> name) {
                return t.name == name ? t : new Field.Option(t.id, t.prefix, t.markers, name, t.assignment);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class OptionDeclaration implements Proto {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        ProtoRightPadded<FullName> name;

        @With
        @Getter
        Constant assignment;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitOptionDeclaration(this, p);
        }

        public FullName getName() {
            return name.getElement();
        }

        public OptionDeclaration withName(FullName name) {
            return getPadding().withName(this.name.withElement(name));
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final OptionDeclaration t;

            public ProtoRightPadded<FullName> getName() {
                return t.name;
            }

            public OptionDeclaration withName(ProtoRightPadded<FullName> name) {
                return t.name == name ? t : new OptionDeclaration(t.id, t.prefix, t.markers, name, t.assignment);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Package implements Proto {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        FullIdentifier name;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitPackage(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Primitive implements TypeTree {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Type type;

        public enum Type {
            DOUBLE, FLOAT, INT32, INT64, UINT32, UINT64, SINT32, SINT64, FIXED32, FIXED64, SFIXED32, SFIXED64, BOOL, STRING, BYTES
        }

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitPrimitive(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Range implements Proto {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        ProtoRightPadded<Constant> from;

        @With
        @Getter
        Constant to;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitRange(this, p);
        }

        public Constant getFrom() {
            return from.getElement();
        }

        public Range withFrom(Constant from) {
            return getPadding().withFrom(this.from.withElement(from));
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Range t;

            public ProtoRightPadded<Constant> getFrom() {
                return t.from;
            }

            public Range withFrom(ProtoRightPadded<Constant> from) {
                return t.from == from ? t : new Range(t.id, t.prefix, t.markers, from, t.to);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Reserved implements Proto {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        /**
         * Either a set of string literal {@link Constant} or a set of {@link Range}
         */
        @Nullable
        ProtoContainer<Proto> reservations;

        @Nullable
        public List<Proto> getReservations() {
            return reservations == null ? null : reservations.getElements();
        }

        public Reserved withReservations(@Nullable List<Proto> reservations) {
            return getPadding().withReservations(ProtoContainer.withElementsNullable(this.reservations, reservations));
        }

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitReserved(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Reserved t;

            @Nullable
            public ProtoContainer<Proto> getReservations() {
                return t.reservations;
            }

            public Reserved withReservations(@Nullable ProtoContainer<Proto> reservations) {
                return t.reservations == reservations ? t : new Reserved(t.id, t.prefix, t.markers, reservations);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class RpcInOut implements Proto {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @Nullable
        @With
        @Getter
        Keyword stream;

        ProtoRightPadded<FullName> type;

        public FullName getType() {
            return type.getElement();
        }

        public RpcInOut withType(FullName type) {
            return getPadding().withType(this.type.withElement(type));
        }

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitRpcInOut(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final RpcInOut t;

            @Nullable
            public ProtoRightPadded<FullName> getType() {
                return t.type;
            }

            public RpcInOut withType(@Nullable ProtoRightPadded<FullName> type) {
                return t.type == type ? t : new RpcInOut(t.id, t.prefix, t.markers, t.stream, type);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Rpc implements Proto {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Identifier name;
        RpcInOut request;
        Keyword returns;
        RpcInOut response;

        @Nullable
        Block body;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitRpc(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Service implements Proto {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Identifier name;
        Block body;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitService(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class StringLiteral implements Proto {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        boolean singleQuote;
        String literal;

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitStringLiteral(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Syntax implements Proto {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Space keywordSuffix;

        ProtoRightPadded<Constant> level;

        public Constant getLevel() {
            return level.getElement();
        }

        public Syntax withLevel(Constant level) {
            return getPadding().withLevel(this.level.withElement(level));
        }

        public int getLevelVersion() {
            return level.getElement().getValueSource().contains("2") ? 2 : 3;
        }

        @Override
        public <P> Proto acceptProto(ProtoVisitor<P> v, P p) {
            return v.visitSyntax(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Syntax t;

            public ProtoRightPadded<Constant> getLevel() {
                return t.level;
            }

            public Syntax withLevel(ProtoRightPadded<Constant> level) {
                return t.level == level ? t : new Syntax(t.id, t.prefix, t.markers, t.keywordSuffix, level);
            }
        }
    }
}
