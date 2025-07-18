/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.graphql.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.graphql.GraphQlVisitor;
import org.openrewrite.graphql.internal.GraphQlPrinter;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.Markers;

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public interface GraphQl extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptGraphQl(v.adapt(GraphQlVisitor.class), p);
    }

    default <P> @Nullable GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(GraphQlVisitor.class);
    }

    Space getPrefix();

    Markers getMarkers();

    default List<Comment> getComments() {
        return getPrefix().getComments();
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class Document implements GraphQl, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        Path sourcePath;

        @Nullable
        FileAttributes fileAttributes;

        Space prefix;
        Markers markers;
        
        @Nullable
        String charsetName;

        boolean charsetBomMarked;

        @Nullable
        Checksum checksum;

        List<Definition> definitions;

        Space eof;

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitDocument(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new GraphQlPrinter<>();
        }
    }

    interface Definition extends GraphQl {
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class OperationDefinition implements Definition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        OperationType operationType;

        @Nullable
        Name name;

        @Nullable
        List<VariableDefinition> variableDefinitions;

        @Nullable
        Space variableDefinitionsEnd;

        @Nullable
        List<Directive> directives;

        SelectionSet selectionSet;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitOperationDefinition(this, p);
        }
    }

    enum OperationType {
        QUERY,
        MUTATION,
        SUBSCRIPTION
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class SelectionSet implements GraphQl {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<Selection> selections;
        Space end;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitSelectionSet(this, p);
        }
    }

    interface Selection extends GraphQl {
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class Field implements Selection {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        Name alias;

        Name name;

        @Nullable
        Arguments arguments;

        @Nullable
        List<Directive> directives;

        @Nullable
        SelectionSet selectionSet;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitField(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class Name implements GraphQl {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String value;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitName(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class Arguments implements GraphQl {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<Argument> arguments;

        Space end;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitArguments(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class Argument implements GraphQl {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        GraphQlRightPadded<Name> name;
        Value value;
        
        public Name getName() {
            return name.getElement();
        }
        
        public Argument withName(Name name) {
            return getPadding().withName(this.name.withElement(name));
        }

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitArgument(this, p);
        }
        
        public Padding getPadding() {
            return new Padding(this);
        }
        
        @RequiredArgsConstructor
        public static class Padding {
            private final Argument t;
            
            public GraphQlRightPadded<Name> getName() {
                return t.name;
            }
            
            public Argument withName(GraphQlRightPadded<Name> name) {
                return t.name == name ? t : new Argument(t.id, t.prefix, t.markers, name, t.value);
            }
        }
    }

    interface Value extends GraphQl {
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class StringValue implements Value {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String value;
        boolean block;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitStringValue(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class IntValue implements Value {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String value;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitIntValue(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class FloatValue implements Value {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String value;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitFloatValue(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class BooleanValue implements Value {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        boolean value;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitBooleanValue(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class NullValue implements Value {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitNullValue(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class EnumValue implements Value {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String name;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitEnumValue(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class ListValue implements Value {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<Value> values;

        Space end;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitListValue(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class ObjectValue implements Value {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<ObjectField> fields;
        Space end;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitObjectValue(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class ObjectField implements GraphQl {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        GraphQlRightPadded<Name> name;
        Value value;
        
        public Name getName() {
            return name.getElement();
        }
        
        public ObjectField withName(Name name) {
            return getPadding().withName(this.name.withElement(name));
        }

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitObjectField(this, p);
        }
        
        public Padding getPadding() {
            return new Padding(this);
        }
        
        @RequiredArgsConstructor
        public static class Padding {
            private final ObjectField t;
            
            public GraphQlRightPadded<Name> getName() {
                return t.name;
            }
            
            public ObjectField withName(GraphQlRightPadded<Name> name) {
                return t.name == name ? t : new ObjectField(t.id, t.prefix, t.markers, name, t.value);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class Variable implements Value {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Name name;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitVariable(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class VariableDefinition implements GraphQl {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Variable variable;
        Type type;

        @Nullable
        Value defaultValue;

        @Nullable
        List<Directive> directives;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitVariableDefinition(this, p);
        }
    }

    interface Type extends GraphQl {
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class NamedType implements Type {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Name name;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitNamedType(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class ListType implements Type {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Type type;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitListType(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class NonNullType implements Type {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Type type;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitNonNullType(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class Directive implements GraphQl {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Name name;

        @Nullable
        Arguments arguments;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitDirective(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class FragmentDefinition implements Definition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Name name;
        Space onPrefix;
        NamedType typeCondition;

        @Nullable
        List<Directive> directives;

        SelectionSet selectionSet;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitFragmentDefinition(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class FragmentSpread implements Selection {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        Name name;

        @Nullable
        List<Directive> directives;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitFragmentSpread(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class InlineFragment implements Selection {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        Space onPrefix;
        
        @Nullable
        NamedType typeCondition;

        @Nullable
        List<Directive> directives;

        SelectionSet selectionSet;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitInlineFragment(this, p);
        }
    }

    // Type System Definitions
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class SchemaDefinition implements Definition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        StringValue description;

        @Nullable
        List<Directive> directives;

        List<RootOperationTypeDefinition> operationTypes;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitSchemaDefinition(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class RootOperationTypeDefinition implements GraphQl {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        OperationType operationType;
        NamedType type;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitRootOperationTypeDefinition(this, p);
        }
    }

    interface TypeDefinition extends Definition {
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class ScalarTypeDefinition implements TypeDefinition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        StringValue description;

        Name name;

        @Nullable
        List<Directive> directives;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitScalarTypeDefinition(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class ObjectTypeDefinition implements TypeDefinition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        StringValue description;

        Name name;

        @Nullable
        List<NamedType> implementsInterfaces;

        @Nullable
        List<Directive> directives;

        @Nullable
        Space fieldsPrefix;

        @Nullable
        List<FieldDefinition> fields;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitObjectTypeDefinition(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class FieldDefinition implements GraphQl {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        StringValue description;

        Name name;

        @Nullable
        List<InputValueDefinition> arguments;
        
        @Nullable
        Space argumentsEnd;

        Type type;

        @Nullable
        List<Directive> directives;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitFieldDefinition(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class InputValueDefinition implements GraphQl {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        StringValue description;

        Name name;
        Type type;

        @Nullable
        Value defaultValue;

        @Nullable
        List<Directive> directives;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitInputValueDefinition(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class InterfaceTypeDefinition implements TypeDefinition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        StringValue description;

        Name name;

        @Nullable
        List<NamedType> implementsInterfaces;

        @Nullable
        List<Directive> directives;

        @Nullable
        Space fieldsPrefix;

        @Nullable
        List<FieldDefinition> fields;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitInterfaceTypeDefinition(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class UnionTypeDefinition implements TypeDefinition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        StringValue description;

        Name name;

        @Nullable
        List<Directive> directives;

        @Nullable
        List<NamedType> memberTypes;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitUnionTypeDefinition(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class EnumTypeDefinition implements TypeDefinition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        StringValue description;

        Name name;

        @Nullable
        List<Directive> directives;

        @Nullable
        List<EnumValueDefinition> values;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitEnumTypeDefinition(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class EnumValueDefinition implements GraphQl {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        StringValue description;

        Name enumValue;

        @Nullable
        List<Directive> directives;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitEnumValueDefinition(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class InputObjectTypeDefinition implements TypeDefinition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        StringValue description;

        Name name;

        @Nullable
        List<Directive> directives;

        @Nullable
        List<InputValueDefinition> fields;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitInputObjectTypeDefinition(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class DirectiveDefinition implements Definition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Nullable
        StringValue description;

        Name name;

        @Nullable
        List<InputValueDefinition> arguments;
        
        @Nullable
        Space argumentsEnd;

        boolean repeatable;

        List<GraphQlRightPadded<DirectiveLocationValue>> locations;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitDirectiveDefinition(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class SchemaExtension implements Definition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        
        Space typeKeywordPrefix;

        @Nullable
        List<Directive> directives;

        @Nullable
        List<RootOperationTypeDefinition> operationTypes;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitSchemaExtension(this, p);
        }
    }
    
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class ScalarTypeExtension implements Definition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        
        Space typeKeywordPrefix;

        Name name;

        @Nullable
        List<Directive> directives;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitScalarTypeExtension(this, p);
        }
    }
    
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class ObjectTypeExtension implements Definition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        
        Space typeKeywordPrefix;

        Name name;

        @Nullable
        List<NamedType> implementsInterfaces;

        @Nullable
        List<Directive> directives;

        @Nullable
        Space fieldsPrefix;

        @Nullable
        List<FieldDefinition> fields;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitObjectTypeExtension(this, p);
        }
    }
    
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class InterfaceTypeExtension implements Definition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        
        Space typeKeywordPrefix;

        Name name;

        @Nullable
        List<NamedType> implementsInterfaces;

        @Nullable
        List<Directive> directives;

        @Nullable
        Space fieldsPrefix;

        @Nullable
        List<FieldDefinition> fields;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitInterfaceTypeExtension(this, p);
        }
    }
    
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class UnionTypeExtension implements Definition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        
        Space typeKeywordPrefix;

        Name name;

        @Nullable
        List<Directive> directives;

        @Nullable
        List<NamedType> memberTypes;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitUnionTypeExtension(this, p);
        }
    }
    
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class EnumTypeExtension implements Definition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        
        Space typeKeywordPrefix;

        Name name;

        @Nullable
        List<Directive> directives;

        @Nullable
        List<EnumValueDefinition> values;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitEnumTypeExtension(this, p);
        }
    }
    
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class InputObjectTypeExtension implements Definition {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        
        Space typeKeywordPrefix;

        Name name;

        @Nullable
        List<Directive> directives;

        @Nullable
        List<InputValueDefinition> fields;

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitInputObjectTypeExtension(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class DirectiveLocationValue implements GraphQl {
        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        Markers markers;
        DirectiveLocation location;
        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitDirectiveLocationValue(this, p);
        }
    }
    
    enum DirectiveLocation {
        // Executable directive locations
        QUERY,
        MUTATION,
        SUBSCRIPTION,
        FIELD,
        FRAGMENT_DEFINITION,
        FRAGMENT_SPREAD,
        INLINE_FRAGMENT,
        VARIABLE_DEFINITION,
        
        // Type system directive locations
        SCHEMA,
        SCALAR,
        OBJECT,
        FIELD_DEFINITION,
        ARGUMENT_DEFINITION,
        INTERFACE,
        UNION,
        ENUM,
        ENUM_VALUE,
        INPUT_OBJECT,
        INPUT_FIELD_DEFINITION
    }
}