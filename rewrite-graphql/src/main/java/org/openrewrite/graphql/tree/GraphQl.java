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

        List<GraphQlRightPadded<Argument>> arguments;

        Space end;
        
        public List<Argument> getArguments() {
            return GraphQlRightPadded.getElements(arguments);
        }
        
        public Arguments withArguments(List<Argument> arguments) {
            return getPadding().withArguments(GraphQlRightPadded.withElements(this.arguments, arguments));
        }

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitArguments(this, p);
        }
        
        public Padding getPadding() {
            return new Padding(this);
        }
        
        @RequiredArgsConstructor
        public static class Padding {
            private final Arguments t;
            
            public List<GraphQlRightPadded<Argument>> getArguments() {
                return t.arguments;
            }
            
            public Arguments withArguments(List<GraphQlRightPadded<Argument>> arguments) {
                return t.arguments == arguments ? t : new Arguments(t.id, t.prefix, t.markers, arguments, t.end);
            }
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
        Space defaultValuePrefix;

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

        @Nullable
        Space operationTypesPrefix;

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
    @AllArgsConstructor
    class ObjectTypeDefinition implements TypeDefinition {
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
        @Nullable
        StringValue description;

        @With
        @Getter
        Name name;

        @With
        @Getter
        @Nullable
        Space implementsPrefix;

        @Nullable
        List<GraphQlRightPadded<NamedType>> implementsInterfaces;

        @With
        @Getter
        @Nullable
        List<Directive> directives;

        @With
        @Getter
        @Nullable
        Space fieldsPrefix;

        @With
        @Getter
        @Nullable
        List<FieldDefinition> fields;
        
        @Nullable
        public List<NamedType> getImplementsInterfaces() {
            return GraphQlRightPadded.getElements(implementsInterfaces);
        }
        
        public ObjectTypeDefinition withImplementsInterfaces(@Nullable List<NamedType> implementsInterfaces) {
            return getPadding().withImplementsInterfaces(GraphQlRightPadded.withElements(this.implementsInterfaces, implementsInterfaces));
        }
        
        @Override
        public ObjectTypeDefinition withId(UUID id) {
            return this.id == id ? this : new ObjectTypeDefinition(id, prefix, markers, description, name, implementsPrefix, implementsInterfaces, directives, fieldsPrefix, fields);
        }

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitObjectTypeDefinition(this, p);
        }
        
        public Padding getPadding() {
            return new Padding(this);
        }
        
        @RequiredArgsConstructor
        public static class Padding {
            private final ObjectTypeDefinition t;
            
            @Nullable
            public List<GraphQlRightPadded<NamedType>> getImplementsInterfaces() {
                return t.implementsInterfaces;
            }
            
            public ObjectTypeDefinition withImplementsInterfaces(@Nullable List<GraphQlRightPadded<NamedType>> implementsInterfaces) {
                return t.implementsInterfaces == implementsInterfaces ? t : new ObjectTypeDefinition(t.id, t.prefix, t.markers, t.description, t.name, t.implementsPrefix, implementsInterfaces, t.directives, t.fieldsPrefix, t.fields);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    class FieldDefinition implements GraphQl {
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
        @Nullable
        StringValue description;

        @With
        @Getter
        Name name;

        @Nullable
        List<GraphQlRightPadded<InputValueDefinition>> arguments;
        
        @Nullable
        public List<InputValueDefinition> getArguments() {
            return GraphQlRightPadded.getElements(arguments);
        }
        
        public FieldDefinition withArguments(@Nullable List<InputValueDefinition> arguments) {
            return getPadding().withArguments(GraphQlRightPadded.withElements(this.arguments, arguments));
        }
        
        @Override
        public FieldDefinition withId(UUID id) {
            return this.id == id ? this : new FieldDefinition(id, prefix, markers, description, name, arguments, argumentsEnd, type, directives);
        }
        
        @With
        @Getter
        @Nullable
        Space argumentsEnd;

        @With
        @Getter
        Type type;

        @With
        @Getter
        @Nullable
        List<Directive> directives;
        
        public Padding getPadding() {
            return new Padding(this);
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final FieldDefinition t;

            @Nullable
            public List<GraphQlRightPadded<InputValueDefinition>> getArguments() {
                return t.arguments;
            }

            public FieldDefinition withArguments(@Nullable List<GraphQlRightPadded<InputValueDefinition>> arguments) {
                return t.arguments == arguments ? t : new FieldDefinition(t.id, t.prefix, t.markers, t.description, t.name, arguments, t.argumentsEnd, t.type, t.directives);
            }
        }

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
        Space defaultValuePrefix;

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
    @AllArgsConstructor
    class InterfaceTypeDefinition implements TypeDefinition {
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
        @Nullable
        StringValue description;

        @With
        @Getter
        Name name;

        @With
        @Getter
        @Nullable
        Space implementsPrefix;

        @Nullable
        List<GraphQlRightPadded<NamedType>> implementsInterfaces;

        @With
        @Getter
        @Nullable
        List<Directive> directives;

        @With
        @Getter
        @Nullable
        Space fieldsPrefix;

        @With
        @Getter
        @Nullable
        List<FieldDefinition> fields;
        
        @Nullable
        public List<NamedType> getImplementsInterfaces() {
            return GraphQlRightPadded.getElements(implementsInterfaces);
        }
        
        public InterfaceTypeDefinition withImplementsInterfaces(@Nullable List<NamedType> implementsInterfaces) {
            return getPadding().withImplementsInterfaces(GraphQlRightPadded.withElements(this.implementsInterfaces, implementsInterfaces));
        }
        
        @Override
        public InterfaceTypeDefinition withId(UUID id) {
            return this.id == id ? this : new InterfaceTypeDefinition(id, prefix, markers, description, name, implementsPrefix, implementsInterfaces, directives, fieldsPrefix, fields);
        }

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitInterfaceTypeDefinition(this, p);
        }
        
        public Padding getPadding() {
            return new Padding(this);
        }
        
        @RequiredArgsConstructor
        public static class Padding {
            private final InterfaceTypeDefinition t;
            
            @Nullable
            public List<GraphQlRightPadded<NamedType>> getImplementsInterfaces() {
                return t.implementsInterfaces;
            }
            
            public InterfaceTypeDefinition withImplementsInterfaces(@Nullable List<GraphQlRightPadded<NamedType>> implementsInterfaces) {
                return t.implementsInterfaces == implementsInterfaces ? t : new InterfaceTypeDefinition(t.id, t.prefix, t.markers, t.description, t.name, t.implementsPrefix, implementsInterfaces, t.directives, t.fieldsPrefix, t.fields);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    class UnionTypeDefinition implements TypeDefinition {
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
        @Nullable
        StringValue description;

        @With
        @Getter
        Name name;

        @With
        @Getter
        @Nullable
        List<Directive> directives;

        @With
        @Getter
        @Nullable
        Space memberTypesPrefix;

        @Nullable
        List<GraphQlRightPadded<NamedType>> memberTypes;

        @Nullable
        public List<NamedType> getMemberTypes() {
            return GraphQlRightPadded.getElements(memberTypes);
        }
        
        public UnionTypeDefinition withMemberTypes(@Nullable List<NamedType> memberTypes) {
            return getPadding().withMemberTypes(GraphQlRightPadded.withElements(this.memberTypes, memberTypes));
        }
        
        @Override
        public UnionTypeDefinition withId(UUID id) {
            return this.id == id ? this : new UnionTypeDefinition(id, prefix, markers, description, name, directives, memberTypesPrefix, memberTypes);
        }

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitUnionTypeDefinition(this, p);
        }
        
        public Padding getPadding() {
            return new Padding(this);
        }
        
        @RequiredArgsConstructor
        public static class Padding {
            private final UnionTypeDefinition t;
            
            @Nullable
            public List<GraphQlRightPadded<NamedType>> getMemberTypes() {
                return t.memberTypes;
            }
            
            public UnionTypeDefinition withMemberTypes(@Nullable List<GraphQlRightPadded<NamedType>> memberTypes) {
                return t.memberTypes == memberTypes ? t : new UnionTypeDefinition(t.id, t.prefix, t.markers, t.description, t.name, t.directives, t.memberTypesPrefix, memberTypes);
            }
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

        @Nullable
        Space repeatablePrefix;

        boolean repeatable;

        @Nullable
        Space onPrefix;

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
        Space operationTypesPrefix;

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
    @AllArgsConstructor
    class ObjectTypeExtension implements Definition {
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
        Space typeKeywordPrefix;

        @With
        @Getter
        Name name;

        @With
        @Getter
        @Nullable
        Space implementsPrefix;

        @Nullable
        List<GraphQlRightPadded<NamedType>> implementsInterfaces;

        @With
        @Getter
        @Nullable
        List<Directive> directives;

        @With
        @Getter
        @Nullable
        Space fieldsPrefix;

        @With
        @Getter
        @Nullable
        List<FieldDefinition> fields;
        
        @Nullable
        public List<NamedType> getImplementsInterfaces() {
            return GraphQlRightPadded.getElements(implementsInterfaces);
        }
        
        public ObjectTypeExtension withImplementsInterfaces(@Nullable List<NamedType> implementsInterfaces) {
            return getPadding().withImplementsInterfaces(GraphQlRightPadded.withElements(this.implementsInterfaces, implementsInterfaces));
        }
        
        @Override
        public ObjectTypeExtension withId(UUID id) {
            return this.id == id ? this : new ObjectTypeExtension(id, prefix, markers, typeKeywordPrefix, name, implementsPrefix, implementsInterfaces, directives, fieldsPrefix, fields);
        }

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitObjectTypeExtension(this, p);
        }
        
        public Padding getPadding() {
            return new Padding(this);
        }
        
        @RequiredArgsConstructor
        public static class Padding {
            private final ObjectTypeExtension t;
            
            @Nullable
            public List<GraphQlRightPadded<NamedType>> getImplementsInterfaces() {
                return t.implementsInterfaces;
            }
            
            public ObjectTypeExtension withImplementsInterfaces(@Nullable List<GraphQlRightPadded<NamedType>> implementsInterfaces) {
                return t.implementsInterfaces == implementsInterfaces ? t : new ObjectTypeExtension(t.id, t.prefix, t.markers, t.typeKeywordPrefix, t.name, t.implementsPrefix, implementsInterfaces, t.directives, t.fieldsPrefix, t.fields);
            }
        }
    }
    
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    class InterfaceTypeExtension implements Definition {
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
        Space typeKeywordPrefix;

        @With
        @Getter
        Name name;

        @With
        @Getter
        @Nullable
        Space implementsPrefix;

        @Nullable
        List<GraphQlRightPadded<NamedType>> implementsInterfaces;

        @With
        @Getter
        @Nullable
        List<Directive> directives;

        @With
        @Getter
        @Nullable
        Space fieldsPrefix;

        @With
        @Getter
        @Nullable
        List<FieldDefinition> fields;
        
        @Nullable
        public List<NamedType> getImplementsInterfaces() {
            return GraphQlRightPadded.getElements(implementsInterfaces);
        }
        
        public InterfaceTypeExtension withImplementsInterfaces(@Nullable List<NamedType> implementsInterfaces) {
            return getPadding().withImplementsInterfaces(GraphQlRightPadded.withElements(this.implementsInterfaces, implementsInterfaces));
        }
        
        @Override
        public InterfaceTypeExtension withId(UUID id) {
            return this.id == id ? this : new InterfaceTypeExtension(id, prefix, markers, typeKeywordPrefix, name, implementsPrefix, implementsInterfaces, directives, fieldsPrefix, fields);
        }

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitInterfaceTypeExtension(this, p);
        }
        
        public Padding getPadding() {
            return new Padding(this);
        }
        
        @RequiredArgsConstructor
        public static class Padding {
            private final InterfaceTypeExtension t;
            
            @Nullable
            public List<GraphQlRightPadded<NamedType>> getImplementsInterfaces() {
                return t.implementsInterfaces;
            }
            
            public InterfaceTypeExtension withImplementsInterfaces(@Nullable List<GraphQlRightPadded<NamedType>> implementsInterfaces) {
                return t.implementsInterfaces == implementsInterfaces ? t : new InterfaceTypeExtension(t.id, t.prefix, t.markers, t.typeKeywordPrefix, t.name, t.implementsPrefix, implementsInterfaces, t.directives, t.fieldsPrefix, t.fields);
            }
        }
    }
    
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    class UnionTypeExtension implements Definition {
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
        Space typeKeywordPrefix;

        @With
        @Getter
        Name name;

        @With
        @Getter
        @Nullable
        List<Directive> directives;

        @With
        @Getter
        @Nullable
        Space memberTypesPrefix;

        @Nullable
        List<GraphQlRightPadded<NamedType>> memberTypes;

        @Nullable
        public List<NamedType> getMemberTypes() {
            return GraphQlRightPadded.getElements(memberTypes);
        }
        
        public UnionTypeExtension withMemberTypes(@Nullable List<NamedType> memberTypes) {
            return getPadding().withMemberTypes(GraphQlRightPadded.withElements(this.memberTypes, memberTypes));
        }
        
        @Override
        public UnionTypeExtension withId(UUID id) {
            return this.id == id ? this : new UnionTypeExtension(id, prefix, markers, typeKeywordPrefix, name, directives, memberTypesPrefix, memberTypes);
        }

        @Override
        public <P> GraphQl acceptGraphQl(GraphQlVisitor<P> v, P p) {
            return v.visitUnionTypeExtension(this, p);
        }
        
        public Padding getPadding() {
            return new Padding(this);
        }
        
        @RequiredArgsConstructor
        public static class Padding {
            private final UnionTypeExtension t;
            
            @Nullable
            public List<GraphQlRightPadded<NamedType>> getMemberTypes() {
                return t.memberTypes;
            }
            
            public UnionTypeExtension withMemberTypes(@Nullable List<GraphQlRightPadded<NamedType>> memberTypes) {
                return t.memberTypes == memberTypes ? t : new UnionTypeExtension(t.id, t.prefix, t.markers, t.typeKeywordPrefix, t.name, t.directives, t.memberTypesPrefix, memberTypes);
            }
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