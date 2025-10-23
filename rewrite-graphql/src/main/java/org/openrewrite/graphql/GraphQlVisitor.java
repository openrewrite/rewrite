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
package org.openrewrite.graphql;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.TreeVisitor;
import org.openrewrite.graphql.tree.GraphQl;
import org.openrewrite.graphql.tree.GraphQlRightPadded;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Marker;

import java.util.List;

import org.openrewrite.graphql.tree.Space;

public class GraphQlVisitor<P> extends TreeVisitor<GraphQl, P> {
    
    public @Nullable Space visitSpace(@Nullable Space space, P p) {
        return space;
    }
    
    public <T extends GraphQl> GraphQlRightPadded<T> visitRightPadded(@Nullable GraphQlRightPadded<T> right, P p) {
        if (right == null) {
            return null;
        }
        setCursor(new Cursor(getCursor(), right));
        T t = (T) visit(right.getElement(), p);
        setCursor(getCursor().getParent());
        if (t == null) {
            return null;
        }
        return right.withElement(t);
    }
    
    public <T extends GraphQl> List<GraphQlRightPadded<T>> visitRightPadded(@Nullable List<GraphQlRightPadded<T>> right, P p) {
        if (right == null) {
            return null;
        }
        return ListUtils.map(right, r -> visitRightPadded(r, p));
    }

    public @Nullable GraphQl visitDocument(GraphQl.Document document, P p) {
        GraphQl.Document d = document;
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withDefinitions(ListUtils.map(d.getDefinitions(), def -> (GraphQl.Definition) visit(def, p)));
        return d;
    }

    public @Nullable GraphQl visitOperationDefinition(GraphQl.OperationDefinition operationDef, P p) {
        GraphQl.OperationDefinition o = operationDef;
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        o = o.withName(visitAndCast(o.getName(), p));
        o = o.withVariableDefinitions(ListUtils.map(o.getVariableDefinitions(), v -> visitAndCast(v, p)));
        o = o.withDirectives(ListUtils.map(o.getDirectives(), d -> visitAndCast(d, p)));
        o = o.withSelectionSet(visitAndCast(o.getSelectionSet(), p));
        return o;
    }

    public @Nullable GraphQl visitSelectionSet(GraphQl.SelectionSet selectionSet, P p) {
        GraphQl.SelectionSet s = selectionSet;
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withSelections(ListUtils.map(s.getSelections(), sel -> (GraphQl.Selection) visit(sel, p)));
        return s;
    }

    public @Nullable GraphQl visitField(GraphQl.Field field, P p) {
        GraphQl.Field f = field;
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = f.withAlias(visitAndCast(f.getAlias(), p));
        f = f.withName(visitAndCast(f.getName(), p));
        f = f.withArguments(visitAndCast(f.getArguments(), p));
        f = f.withDirectives(ListUtils.map(f.getDirectives(), d -> visitAndCast(d, p)));
        f = f.withSelectionSet(visitAndCast(f.getSelectionSet(), p));
        return f;
    }

    public @Nullable GraphQl visitName(GraphQl.Name name, P p) {
        GraphQl.Name n = name;
        n = n.withMarkers(visitMarkers(n.getMarkers(), p));
        return n;
    }

    public @Nullable GraphQl visitArguments(GraphQl.Arguments arguments, P p) {
        GraphQl.Arguments a = arguments;
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.getPadding().withArguments(visitRightPadded(a.getPadding().getArguments(), p));
        return a;
    }

    public @Nullable GraphQl visitArgument(GraphQl.Argument argument, P p) {
        GraphQl.Argument a = argument;
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.getPadding().withName(visitRightPadded(a.getPadding().getName(), p));
        a = a.withValue((GraphQl.Value) visit(a.getValue(), p));
        return a;
    }

    public @Nullable GraphQl visitStringValue(GraphQl.StringValue stringValue, P p) {
        GraphQl.StringValue s = stringValue;
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        return s;
    }

    public @Nullable GraphQl visitIntValue(GraphQl.IntValue intValue, P p) {
        GraphQl.IntValue i = intValue;
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        return i;
    }

    public @Nullable GraphQl visitFloatValue(GraphQl.FloatValue floatValue, P p) {
        GraphQl.FloatValue f = floatValue;
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        return f;
    }

    public @Nullable GraphQl visitBooleanValue(GraphQl.BooleanValue booleanValue, P p) {
        GraphQl.BooleanValue b = booleanValue;
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        return b;
    }

    public @Nullable GraphQl visitNullValue(GraphQl.NullValue nullValue, P p) {
        GraphQl.NullValue n = nullValue;
        n = n.withMarkers(visitMarkers(n.getMarkers(), p));
        return n;
    }

    public @Nullable GraphQl visitEnumValue(GraphQl.EnumValue enumValue, P p) {
        GraphQl.EnumValue e = enumValue;
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        return e;
    }

    public @Nullable GraphQl visitListValue(GraphQl.ListValue listValue, P p) {
        GraphQl.ListValue l = listValue;
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        l = l.withValues(ListUtils.map(l.getValues(), v -> (GraphQl.Value) visit(v, p)));
        return l;
    }

    public @Nullable GraphQl visitObjectValue(GraphQl.ObjectValue objectValue, P p) {
        GraphQl.ObjectValue o = objectValue;
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        o = o.withFields(ListUtils.map(o.getFields(), f -> visitAndCast(f, p)));
        // Space fields don't need to be visited as they have no nested elements
        return o;
    }

    public @Nullable GraphQl visitObjectField(GraphQl.ObjectField objectField, P p) {
        GraphQl.ObjectField o = objectField;
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        o = o.getPadding().withName(visitRightPadded(o.getPadding().getName(), p));
        o = o.withValue((GraphQl.Value) visit(o.getValue(), p));
        return o;
    }

    public @Nullable GraphQl visitVariable(GraphQl.Variable variable, P p) {
        GraphQl.Variable v = variable;
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        v = v.withName(visitAndCast(v.getName(), p));
        return v;
    }

    public @Nullable GraphQl visitVariableDefinition(GraphQl.VariableDefinition variableDef, P p) {
        GraphQl.VariableDefinition v = variableDef;
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        v = v.withVariable(visitAndCast(v.getVariable(), p));
        v = v.withType((GraphQl.Type) visit(v.getType(), p));
        v = v.withDefaultValuePrefix(visitSpace(v.getDefaultValuePrefix(), p));
        v = v.withDefaultValue((GraphQl.Value) visit(v.getDefaultValue(), p));
        v = v.withDirectives(ListUtils.map(v.getDirectives(), d -> visitAndCast(d, p)));
        return v;
    }

    public @Nullable GraphQl visitNamedType(GraphQl.NamedType namedType, P p) {
        GraphQl.NamedType n = namedType;
        n = n.withMarkers(visitMarkers(n.getMarkers(), p));
        n = n.withName(visitAndCast(n.getName(), p));
        return n;
    }

    public @Nullable GraphQl visitListType(GraphQl.ListType listType, P p) {
        GraphQl.ListType l = listType;
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        l = l.withType((GraphQl.Type) visit(l.getType(), p));
        return l;
    }

    public @Nullable GraphQl visitNonNullType(GraphQl.NonNullType nonNullType, P p) {
        GraphQl.NonNullType n = nonNullType;
        n = n.withMarkers(visitMarkers(n.getMarkers(), p));
        n = n.withType((GraphQl.Type) visit(n.getType(), p));
        return n;
    }

    public @Nullable GraphQl visitDirective(GraphQl.Directive directive, P p) {
        GraphQl.Directive d = directive;
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withName(visitAndCast(d.getName(), p));
        d = d.withArguments(visitAndCast(d.getArguments(), p));
        return d;
    }

    public @Nullable GraphQl visitFragmentDefinition(GraphQl.FragmentDefinition fragmentDef, P p) {
        GraphQl.FragmentDefinition f = fragmentDef;
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = f.withName(visitAndCast(f.getName(), p));
        f = f.withOnPrefix(visitSpace(f.getOnPrefix(), p));
        f = f.withTypeCondition(visitAndCast(f.getTypeCondition(), p));
        f = f.withDirectives(ListUtils.map(f.getDirectives(), d -> visitAndCast(d, p)));
        f = f.withSelectionSet(visitAndCast(f.getSelectionSet(), p));
        return f;
    }

    public @Nullable GraphQl visitFragmentSpread(GraphQl.FragmentSpread fragmentSpread, P p) {
        GraphQl.FragmentSpread f = fragmentSpread;
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = f.withName(visitAndCast(f.getName(), p));
        f = f.withDirectives(ListUtils.map(f.getDirectives(), d -> visitAndCast(d, p)));
        return f;
    }

    public @Nullable GraphQl visitInlineFragment(GraphQl.InlineFragment inlineFragment, P p) {
        GraphQl.InlineFragment i = inlineFragment;
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.withOnPrefix(visitSpace(i.getOnPrefix(), p));
        i = i.withTypeCondition(visitAndCast(i.getTypeCondition(), p));
        i = i.withDirectives(ListUtils.map(i.getDirectives(), d -> visitAndCast(d, p)));
        i = i.withSelectionSet(visitAndCast(i.getSelectionSet(), p));
        return i;
    }

    public @Nullable GraphQl visitSchemaDefinition(GraphQl.SchemaDefinition schemaDef, P p) {
        GraphQl.SchemaDefinition s = schemaDef;
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withDescription(visitAndCast(s.getDescription(), p));
        s = s.withDirectives(ListUtils.map(s.getDirectives(), d -> visitAndCast(d, p)));
        s = s.withOperationTypesPrefix(visitSpace(s.getOperationTypesPrefix(), p));
        s = s.withOperationTypes(ListUtils.map(s.getOperationTypes(), o -> visitAndCast(o, p)));
        return s;
    }

    public @Nullable GraphQl visitRootOperationTypeDefinition(GraphQl.RootOperationTypeDefinition rootOpType, P p) {
        GraphQl.RootOperationTypeDefinition r = rootOpType;
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        r = r.withType(visitAndCast(r.getType(), p));
        return r;
    }

    public @Nullable GraphQl visitScalarTypeDefinition(GraphQl.ScalarTypeDefinition scalarType, P p) {
        GraphQl.ScalarTypeDefinition s = scalarType;
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withDescription(visitAndCast(s.getDescription(), p));
        s = s.withName(visitAndCast(s.getName(), p));
        s = s.withDirectives(ListUtils.map(s.getDirectives(), d -> visitAndCast(d, p)));
        return s;
    }

    public @Nullable GraphQl visitObjectTypeDefinition(GraphQl.ObjectTypeDefinition objectType, P p) {
        GraphQl.ObjectTypeDefinition o = objectType;
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        o = o.withDescription(visitAndCast(o.getDescription(), p));
        o = o.withName(visitAndCast(o.getName(), p));
        o = o.withImplementsPrefix(visitSpace(o.getImplementsPrefix(), p));
        o = o.getPadding().withImplementsInterfaces(visitRightPadded(o.getPadding().getImplementsInterfaces(), p));
        o = o.withDirectives(ListUtils.map(o.getDirectives(), d -> visitAndCast(d, p)));
        o = o.withFields(ListUtils.map(o.getFields(), f -> visitAndCast(f, p)));
        return o;
    }

    public @Nullable GraphQl visitFieldDefinition(GraphQl.FieldDefinition fieldDef, P p) {
        GraphQl.FieldDefinition f = fieldDef;
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = f.withDescription(visitAndCast(f.getDescription(), p));
        f = f.withName(visitAndCast(f.getName(), p));
        f = f.getPadding().withArguments(visitRightPadded(f.getPadding().getArguments(), p));
        // Space fields don't need to be visited as they have no nested elements
        f = f.withType((GraphQl.Type) visit(f.getType(), p));
        f = f.withDirectives(ListUtils.map(f.getDirectives(), d -> visitAndCast(d, p)));
        return f;
    }

    public @Nullable GraphQl visitInputValueDefinition(GraphQl.InputValueDefinition inputValueDef, P p) {
        GraphQl.InputValueDefinition i = inputValueDef;
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.withDescription(visitAndCast(i.getDescription(), p));
        i = i.withName(visitAndCast(i.getName(), p));
        i = i.withType((GraphQl.Type) visit(i.getType(), p));
        i = i.withDefaultValuePrefix(visitSpace(i.getDefaultValuePrefix(), p));
        i = i.withDefaultValue((GraphQl.Value) visit(i.getDefaultValue(), p));
        i = i.withDirectives(ListUtils.map(i.getDirectives(), d -> visitAndCast(d, p)));
        return i;
    }

    public @Nullable GraphQl visitInterfaceTypeDefinition(GraphQl.InterfaceTypeDefinition interfaceType, P p) {
        GraphQl.InterfaceTypeDefinition i = interfaceType;
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.withDescription(visitAndCast(i.getDescription(), p));
        i = i.withName(visitAndCast(i.getName(), p));
        i = i.withImplementsPrefix(visitSpace(i.getImplementsPrefix(), p));
        i = i.getPadding().withImplementsInterfaces(visitRightPadded(i.getPadding().getImplementsInterfaces(), p));
        i = i.withDirectives(ListUtils.map(i.getDirectives(), d -> visitAndCast(d, p)));
        i = i.withFields(ListUtils.map(i.getFields(), f -> visitAndCast(f, p)));
        return i;
    }

    public @Nullable GraphQl visitUnionTypeDefinition(GraphQl.UnionTypeDefinition unionType, P p) {
        GraphQl.UnionTypeDefinition u = unionType;
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        u = u.withDescription(visitAndCast(u.getDescription(), p));
        u = u.withName(visitAndCast(u.getName(), p));
        u = u.withDirectives(ListUtils.map(u.getDirectives(), d -> visitAndCast(d, p)));
        u = u.withMemberTypesPrefix(visitSpace(u.getMemberTypesPrefix(), p));
        u = u.getPadding().withMemberTypes(visitRightPadded(u.getPadding().getMemberTypes(), p));
        return u;
    }

    public @Nullable GraphQl visitEnumTypeDefinition(GraphQl.EnumTypeDefinition enumType, P p) {
        GraphQl.EnumTypeDefinition e = enumType;
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withDescription(visitAndCast(e.getDescription(), p));
        e = e.withName(visitAndCast(e.getName(), p));
        e = e.withDirectives(ListUtils.map(e.getDirectives(), d -> visitAndCast(d, p)));
        e = e.withValues(ListUtils.map(e.getValues(), v -> visitAndCast(v, p)));
        return e;
    }

    public @Nullable GraphQl visitEnumValueDefinition(GraphQl.EnumValueDefinition enumValueDef, P p) {
        GraphQl.EnumValueDefinition e = enumValueDef;
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withDescription(visitAndCast(e.getDescription(), p));
        e = e.withEnumValue(visitAndCast(e.getEnumValue(), p));
        e = e.withDirectives(ListUtils.map(e.getDirectives(), d -> visitAndCast(d, p)));
        return e;
    }

    public @Nullable GraphQl visitInputObjectTypeDefinition(GraphQl.InputObjectTypeDefinition inputObjectType, P p) {
        GraphQl.InputObjectTypeDefinition i = inputObjectType;
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.withDescription(visitAndCast(i.getDescription(), p));
        i = i.withName(visitAndCast(i.getName(), p));
        i = i.withDirectives(ListUtils.map(i.getDirectives(), d -> visitAndCast(d, p)));
        i = i.withFields(ListUtils.map(i.getFields(), f -> visitAndCast(f, p)));
        return i;
    }

    public @Nullable GraphQl visitDirectiveDefinition(GraphQl.DirectiveDefinition directiveDef, P p) {
        GraphQl.DirectiveDefinition d = directiveDef;
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withDescription(visitAndCast(d.getDescription(), p));
        d = d.withName(visitAndCast(d.getName(), p));
        d = d.withArguments(ListUtils.map(d.getArguments(), a -> visitAndCast(a, p)));
        d = d.withArgumentsEnd(visitSpace(d.getArgumentsEnd(), p));
        d = d.withRepeatablePrefix(visitSpace(d.getRepeatablePrefix(), p));
        d = d.withOnPrefix(visitSpace(d.getOnPrefix(), p));
        d = d.withLocations(visitRightPadded(d.getLocations(), p));
        return d;
    }
    
    public @Nullable GraphQl visitSchemaExtension(GraphQl.SchemaExtension schemaExt, P p) {
        GraphQl.SchemaExtension s = schemaExt;
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withDirectives(ListUtils.map(s.getDirectives(), d -> visitAndCast(d, p)));
        s = s.withOperationTypesPrefix(visitSpace(s.getOperationTypesPrefix(), p));
        s = s.withOperationTypes(ListUtils.map(s.getOperationTypes(), o -> visitAndCast(o, p)));
        return s;
    }
    
    public @Nullable GraphQl visitScalarTypeExtension(GraphQl.ScalarTypeExtension scalarExt, P p) {
        GraphQl.ScalarTypeExtension s = scalarExt;
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withName(visitAndCast(s.getName(), p));
        s = s.withDirectives(ListUtils.map(s.getDirectives(), d -> visitAndCast(d, p)));
        return s;
    }
    
    public @Nullable GraphQl visitObjectTypeExtension(GraphQl.ObjectTypeExtension objectExt, P p) {
        GraphQl.ObjectTypeExtension o = objectExt;
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        o = o.withName(visitAndCast(o.getName(), p));
        o = o.withImplementsPrefix(visitSpace(o.getImplementsPrefix(), p));
        o = o.getPadding().withImplementsInterfaces(visitRightPadded(o.getPadding().getImplementsInterfaces(), p));
        o = o.withDirectives(ListUtils.map(o.getDirectives(), d -> visitAndCast(d, p)));
        o = o.withFields(ListUtils.map(o.getFields(), f -> visitAndCast(f, p)));
        return o;
    }
    
    public @Nullable GraphQl visitInterfaceTypeExtension(GraphQl.InterfaceTypeExtension interfaceExt, P p) {
        GraphQl.InterfaceTypeExtension i = interfaceExt;
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.withName(visitAndCast(i.getName(), p));
        i = i.withImplementsPrefix(visitSpace(i.getImplementsPrefix(), p));
        i = i.getPadding().withImplementsInterfaces(visitRightPadded(i.getPadding().getImplementsInterfaces(), p));
        i = i.withDirectives(ListUtils.map(i.getDirectives(), d -> visitAndCast(d, p)));
        i = i.withFields(ListUtils.map(i.getFields(), f -> visitAndCast(f, p)));
        return i;
    }
    
    public @Nullable GraphQl visitUnionTypeExtension(GraphQl.UnionTypeExtension unionExt, P p) {
        GraphQl.UnionTypeExtension u = unionExt;
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        u = u.withName(visitAndCast(u.getName(), p));
        u = u.withDirectives(ListUtils.map(u.getDirectives(), d -> visitAndCast(d, p)));
        u = u.withMemberTypesPrefix(visitSpace(u.getMemberTypesPrefix(), p));
        u = u.getPadding().withMemberTypes(visitRightPadded(u.getPadding().getMemberTypes(), p));
        return u;
    }
    
    public @Nullable GraphQl visitEnumTypeExtension(GraphQl.EnumTypeExtension enumExt, P p) {
        GraphQl.EnumTypeExtension e = enumExt;
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withName(visitAndCast(e.getName(), p));
        e = e.withDirectives(ListUtils.map(e.getDirectives(), d -> visitAndCast(d, p)));
        e = e.withValues(ListUtils.map(e.getValues(), v -> visitAndCast(v, p)));
        return e;
    }
    
    public @Nullable GraphQl visitInputObjectTypeExtension(GraphQl.InputObjectTypeExtension inputExt, P p) {
        GraphQl.InputObjectTypeExtension i = inputExt;
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.withName(visitAndCast(i.getName(), p));
        i = i.withDirectives(ListUtils.map(i.getDirectives(), d -> visitAndCast(d, p)));
        i = i.withFields(ListUtils.map(i.getFields(), f -> visitAndCast(f, p)));
        return i;
    }
    
    public @Nullable GraphQl visitDirectiveLocationValue(GraphQl.DirectiveLocationValue locationValue, P p) {
        GraphQl.DirectiveLocationValue l = locationValue;
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        return l;
    }
}