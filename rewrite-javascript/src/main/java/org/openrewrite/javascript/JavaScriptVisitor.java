/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.javascript.tree.*;
import org.openrewrite.marker.Markers;

import java.util.List;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("unused")
public class JavaScriptVisitor<P> extends JavaVisitor<P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof JS.CompilationUnit;
    }

    @Override
    public String getLanguage() {
        return "org/openrewrite/javascript";
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, P p) {
        throw new UnsupportedOperationException("JS has a different structure for its compilation unit. See JS.CompilationUnit.");
    }

    public J visitJsCompilationUnit(JS.CompilationUnit cu, P p) {
        JS.CompilationUnit c = cu;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.getPadding().withImports(ListUtils.map(c.getPadding().getImports(), t -> visitRightPadded(t, JRightPadded.Location.IMPORT, p)));
        c = c.getPadding().withStatements(ListUtils.map(c.getPadding().getStatements(), e -> visitRightPadded(e, JRightPadded.Location.BLOCK_STATEMENT, p)));
        return c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
    }

    public J visitAlias(JS.Alias alias, P p) {
        JS.Alias a = alias;
        a = a.withPrefix(visitSpace(a.getPrefix(), JsSpace.Location.ALIAS_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));

        Expression temp = (Expression) visitExpression(a, p);
        if (!(temp instanceof JS.Alias)) {
            return temp;
        } else {
            a = (JS.Alias) temp;
        }

        a = a.getPadding().withPropertyName(requireNonNull(visitRightPadded(a.getPadding().getPropertyName(), JsRightPadded.Location.ALIAS_PROPERTY_NAME, p)));
        return a.withAlias(requireNonNull(visitAndCast(a.getAlias(), p)));
    }

    public J visitArrowFunction(JS.ArrowFunction arrowFunction, P p) {
        JS.ArrowFunction a = arrowFunction;
        a = a.withPrefix(visitSpace(a.getPrefix(), JsSpace.Location.ARROW_FUNCTION_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));

        Expression temp = (Expression) visitExpression(a, p);
        if (!(temp instanceof JS.ArrowFunction)) {
            return temp;
        } else {
            a = (JS.ArrowFunction) temp;
        }

        a = a.withLeadingAnnotations(requireNonNull(ListUtils.map(a.getLeadingAnnotations(), ann -> visitAndCast(ann, p))));
        a = a.withModifiers(requireNonNull(ListUtils.map(a.getModifiers(), m -> visitAndCast(m, p))));
        a = a.withTypeParameters(visitAndCast(a.getTypeParameters(), p));
        a = a.withLambda(requireNonNull(visitAndCast(a.getLambda(), p)));
        return a.withReturnTypeExpression(visitAndCast(a.getReturnTypeExpression(), p));
    }

    public J visitAwait(JS.Await await, P p) {
        JS.Await a = await;
        a = a.withPrefix(visitSpace(a.getPrefix(), JsSpace.Location.AWAIT_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Expression temp = (Expression) visitExpression(a, p);
        if (!(temp instanceof JS.Await)) {
            return temp;
        } else {
            a = (JS.Await) temp;
        }
        return a.withExpression(requireNonNull(visitAndCast(a.getExpression(), p)));
    }

    public J visitBindingElement(JS.BindingElement binding, P p) {
        JS.BindingElement b = binding;
        b = b.withPrefix(visitSpace(b.getPrefix(), JsSpace.Location.BINDING_ELEMENT_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        b = b.withPropertyName(visitAndCast(b.getPropertyName(), p));
        b = b.withName(requireNonNull(visitAndCast(b.getName(), p)));
        if (b.getPadding().getInitializer() != null) {
            b = b.getPadding().withInitializer(visitLeftPadded(b.getPadding().getInitializer(),
                    JsLeftPadded.Location.BINDING_ELEMENT_INITIALIZER, p));
        }
        return b.withVariableType((JavaType.Variable) visitType(b.getVariableType(), p));
    }

    public J visitConditionalType(JS.ConditionalType conditionalType, P p) {
        JS.ConditionalType t = conditionalType;
        t = t.withPrefix(visitSpace(t.getPrefix(), JsSpace.Location.CONDITIONAL_TYPE_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        t = t.withCheckType(requireNonNull(visitAndCast(t.getCheckType(), p)));
        if (t.getCheckType() instanceof NameTree) {
            t = t.withCheckType((Expression) visitTypeName((NameTree) t.getCheckType(), p));
        }
        t = t.getPadding().withCondition(requireNonNull(visitLeftPadded(t.getPadding().getCondition(),
                JsLeftPadded.Location.CONDITIONAL_TYPE_CONDITION, p)));
        return t.withType(visitType(t.getType(), p));
    }

    public J visitDelete(JS.Delete delete, P p) {
        JS.Delete d = delete;
        d = d.withPrefix(visitSpace(d.getPrefix(), JsSpace.Location.DELETE_PREFIX, p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        Expression temp = (Expression) visitExpression(d, p);
        if (!(temp instanceof JS.Delete)) {
            return temp;
        } else {
            d = (JS.Delete) temp;
        }
        return d;
    }

    public J visitExpressionStatement(JS.ExpressionStatement statement, P p) {
        JS.ExpressionStatement es = statement;
        es = es.withMarkers(visitMarkers(es.getMarkers(), p));
        Statement temp = (Statement) visitStatement(es, p);
        if (!(temp instanceof JS.ExpressionStatement)) {
            return temp;
        } else {
            es = (JS.ExpressionStatement) temp;
        }
        J expression = visit(es.getExpression(), p);
        return es.withExpression((Expression) requireNonNull(expression));
    }

    public J visitExpressionWithTypeArguments(JS.ExpressionWithTypeArguments expressionWithTypeArguments, P p) {
        JS.ExpressionWithTypeArguments ta = expressionWithTypeArguments;
        ta = ta.withPrefix(visitSpace(ta.getPrefix(), JsSpace.Location.EXPR_WITH_TYPE_ARG_PREFIX, p));
        ta = ta.withMarkers(visitMarkers(ta.getMarkers(), p));
        Expression temp = (Expression) visitExpression(ta, p);
        if (!(temp instanceof JS.ExpressionWithTypeArguments)) {
            return temp;
        } else {
            ta = (JS.ExpressionWithTypeArguments) temp;
        }
        ta = ta.withClazz(requireNonNull(visitAndCast(ta.getClazz(), p)));
        if (ta.getPadding().getTypeArguments() != null) {
            ta = ta.getPadding().withTypeArguments(visitContainer(ta.getPadding().getTypeArguments(), JsContainer.Location.EXPR_WITH_TYPE_ARG_PARAMETERS, p));
        }
        return ta.withType(visitType(ta.getType(), p));
    }

    public J visitFunctionCall(JS.FunctionCall functionCall, P p) {
        JS.FunctionCall f = functionCall;
        f = f.withPrefix(visitSpace(f.getPrefix(), JsSpace.Location.FUNCTION_CALL_PREFIX, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        Expression temp2 = (Expression) visitExpression(f, p);
        if (!(temp2 instanceof JS.FunctionCall)) {
            return temp2;
        } else {
            f = (JS.FunctionCall) temp2;
        }
        if (f.getPadding().getFunction() != null && f.getPadding().getFunction().getElement() instanceof NameTree &&
                f.getMethodType() != null && f.getMethodType().hasFlags(Flag.Static)) {
            //noinspection unchecked
            f = f.getPadding().withFunction(
                    (JRightPadded<Expression>) (JRightPadded<?>)
                            visitTypeName((JRightPadded<NameTree>) (JRightPadded<?>) f.getPadding().getFunction(), p));
        }
        if (f.getPadding().getFunction() != null) {
            f = f.getPadding().withFunction(visitRightPadded(f.getPadding().getFunction(), JsRightPadded.Location.FUNCTION_CALL_FUNCTION, p));
        }
        if (f.getPadding().getTypeParameters() != null) {
            f = f.getPadding().withTypeParameters(visitContainer(f.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, p));
        }
        f = f.getPadding().withTypeParameters(visitTypeNames(f.getPadding().getTypeParameters(), p));
        f = f.getPadding().withArguments(visitContainer(f.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, p));
        return f.withMethodType((JavaType.Method) visitType(f.getMethodType(), p));
    }

    public J visitFunctionType(JS.FunctionType functionType, P p) {
        JS.FunctionType f = functionType;
        f = f.withPrefix(visitSpace(f.getPrefix(), JsSpace.Location.FUNCTION_TYPE_PREFIX, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        Expression temp = (Expression) visitExpression(f, p);
        if (!(temp instanceof JS.FunctionType)) {
            return temp;
        } else {
            f = (JS.FunctionType) temp;
        }
        f = f.withModifiers(requireNonNull(ListUtils.map(f.getModifiers(), e -> visitAndCast(e, p))));
        f = f.getPadding().withConstructorType(requireNonNull(visitLeftPadded(f.getPadding().getConstructorType(), JsLeftPadded.Location.FUNCTION_TYPE_CONSTRUCTOR, p)));
        f = f.withTypeParameters(visitAndCast(f.getTypeParameters(), p));
        f = f.getPadding().withParameters(requireNonNull(visitContainer(f.getPadding().getParameters(), JsContainer.Location.FUNCTION_TYPE_PARAMETERS, p)));
        return f.getPadding().withReturnType((requireNonNull(visitLeftPadded(f.getPadding().getReturnType(), JsLeftPadded.Location.FUNCTION_TYPE_RETURN_TYPE, p))));
    }


    public J visitInferType(JS.InferType inferType, P p) {
        JS.InferType t = inferType;
        t = t.withPrefix(visitSpace(t.getPrefix(), JsSpace.Location.INFER_TYPE_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof JS.InferType)) {
            return temp;
        } else {
            t = (JS.InferType) temp;
        }
        t = t.getPadding().withTypeParameter(requireNonNull(visitLeftPadded(t.getPadding().getTypeParameter(), JsLeftPadded.Location.INFER_TYPE_PARAMETER, p)));
        return t.withType(visitType(t.getType(), p));
    }

    public J visitBinaryExtensions(JS.Binary binary, P p) {
        JS.Binary b = binary;
        b = b.withPrefix(visitSpace(b.getPrefix(), JsSpace.Location.BINARY_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitExpression(b, p);
        if (!(temp instanceof JS.Binary)) {
            return temp;
        } else {
            b = (JS.Binary) temp;
        }
        b = b.withLeft(requireNonNull(visitAndCast(b.getLeft(), p)));
        b = b.getPadding().withOperator(requireNonNull(visitLeftPadded(b.getPadding().getOperator(), JsLeftPadded.Location.BINARY_OPERATOR, p)));
        b = b.withRight(requireNonNull(visitAndCast(b.getRight(), p)));
        return b.withType(visitType(b.getType(), p));
    }

    public J visitImportDeclaration(JS.Import jsImport, P p) {
        JS.Import i = jsImport;
        i = i.withPrefix(visitSpace(i.getPrefix(), JsSpace.Location.IMPORT_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        Statement temp = (Statement) visitStatement(i, p);
        if (!(temp instanceof JS.Import)) {
            return temp;
        } else {
            i = (JS.Import) temp;
        }
        i = i.withModifiers(ListUtils.map(i.getModifiers(), e -> visitAndCast(e, p)));
        i = i.withImportClause(visitAndCast(i.getImportClause(), p));
        i = i.getPadding().withModuleSpecifier(visitLeftPadded(i.getPadding().getModuleSpecifier(), JsLeftPadded.Location.IMPORT_MODULE_SPECIFIER, p));
        i = i.withAttributes(visitAndCast(i.getAttributes(), p));
        return i.getPadding().withInitializer(visitLeftPadded(i.getPadding().getInitializer(), JsLeftPadded.Location.IMPORT_INITIALIZER, p));
    }

    public J visitImportClause(JS.ImportClause jsImportClause, P p) {
        JS.ImportClause i = jsImportClause;
        i = i.withPrefix(visitSpace(i.getPrefix(), JsSpace.Location.IMPORT_CLAUSE_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.getPadding().withName(visitRightPadded(i.getPadding().getName(), JsRightPadded.Location.JS_IMPORT_CLAUSE_NAME, p));
        return i.withNamedBindings(visitAndCast(i.getNamedBindings(), p));
    }

    public J visitNamedImports(JS.NamedImports namedImports, P p) {
        JS.NamedImports ne = namedImports;
        ne = ne.withPrefix(visitSpace(ne.getPrefix(), JsSpace.Location.NAMED_IMPORTS_PREFIX, p));
        ne = ne.withMarkers(visitMarkers(ne.getMarkers(), p));
        Expression temp = (Expression) visitExpression(ne, p);
        if (!(temp instanceof JS.NamedImports)) {
            return temp;
        } else {
            ne = (JS.NamedImports) temp;
        }
        ne = ne.getPadding().withElements(requireNonNull(visitContainer(ne.getPadding().getElements(), JsContainer.Location.NAMED_IMPORTS_ELEMENTS, p)));
        return ne.withType(visitType(ne.getType(), p));
    }

    public J visitImportSpecifier(JS.ImportSpecifier jis, P p) {
        JS.ImportSpecifier i = jis;
        i = i.withPrefix(visitSpace(i.getPrefix(), JsSpace.Location.IMPORT_SPECIFIER_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        Expression temp = (Expression) visitExpression(i, p);
        if (!(temp instanceof JS.ImportSpecifier)) {
            return temp;
        } else {
            i = (JS.ImportSpecifier) temp;
        }
        i = i.getPadding().withImportType(requireNonNull(visitLeftPadded(i.getPadding().getImportType(), JsLeftPadded.Location.IMPORT_SPECIFIER_IMPORT_TYPE, p)));
        i = i.withSpecifier(requireNonNull(visitAndCast(i.getSpecifier(), p)));
        return i.withType(visitType(i.getType(), p));
    }

    public J visitImportAttributes(JS.ImportAttributes importAttributes, P p) {
        JS.ImportAttributes i = importAttributes;
        i = i.withPrefix(visitSpace(i.getPrefix(), JsSpace.Location.IMPORT_ATTRIBUTES_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        return i.getPadding().withElements(requireNonNull(visitContainer(i.getPadding().getElements(), JsContainer.Location.JS_IMPORT_ATTRIBUTES_ELEMENTS, p)));
    }

    public J visitImportTypeAttributes(JS.ImportTypeAttributes importTypeAttributes, P p) {
        JS.ImportTypeAttributes i = importTypeAttributes;
        i = i.withPrefix(visitSpace(i.getPrefix(), JsSpace.Location.IMPORT_TYPE_ATTRIBUTES_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.getPadding().withToken(requireNonNull(visitRightPadded(i.getPadding().getToken(), JsRightPadded.Location.JS_IMPORT_TYPE_ATTRIBUTES_TOKEN, p)));
        i = i.getPadding().withElements(requireNonNull(visitContainer(i.getPadding().getElements(), JsContainer.Location.JS_IMPORT_TYPE_ATTRIBUTES_ELEMENTS, p)));
        return i.withEnd(requireNonNull(visitSpace(i.getEnd(), JsSpace.Location.IMPORT_TYPE_ATTRIBUTES_END_SUFFIX, p)));
    }

    public J visitImportAttribute(JS.ImportAttribute importAttribute, P p) {
        JS.ImportAttribute i = importAttribute;
        i = i.withPrefix(visitSpace(i.getPrefix(), JsSpace.Location.IMPORT_ATTRIBUTE_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.withName(requireNonNull(visitAndCast(i.getName(), p)));
        return i.getPadding().withValue(requireNonNull(visitLeftPadded(i.getPadding().getValue(), JsLeftPadded.Location.IMPORT_ATTRIBUTE_VALUE, p)));
    }

    public J visitLiteralType(JS.LiteralType literalType, P p) {
        JS.LiteralType type = literalType;
        type = type.withPrefix(visitSpace(type.getPrefix(), JsSpace.Location.LITERAL_TYPE_PREFIX, p));
        type = type.withMarkers(visitMarkers(type.getMarkers(), p));
        return type.withLiteral(requireNonNull(visitAndCast(type.getLiteral(), p)));
    }

    public J visitMappedType(JS.MappedType mappedType, P p) {
        JS.MappedType t = mappedType;
        t = t.withPrefix(visitSpace(t.getPrefix(), JsSpace.Location.MAPPED_TYPE_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof JS.MappedType)) {
            return temp;
        } else {
            t = (JS.MappedType) temp;
        }
        t = t.getPadding().withPrefixToken(visitLeftPadded(t.getPadding().getPrefixToken(), JsLeftPadded.Location.MAPPED_TYPE_PREFIX_TOKEN, p));
        t = t.getPadding().withHasReadonly(requireNonNull(visitLeftPadded(t.getPadding().getHasReadonly(), JsLeftPadded.Location.MAPPED_TYPE_READONLY, p)));
        t = t.withKeysRemapping(requireNonNull(visitAndCast(t.getKeysRemapping(), p)));
        t = t.getPadding().withSuffixToken(visitLeftPadded(t.getPadding().getSuffixToken(), JsLeftPadded.Location.MAPPED_TYPE_SUFFIX_TOKEN, p));
        t = t.getPadding().withHasQuestionToken(requireNonNull(visitLeftPadded(t.getPadding().getHasQuestionToken(), JsLeftPadded.Location.MAPPED_TYPE_QUESTION_TOKEN, p)));
        return t.getPadding().withValueType(requireNonNull(visitContainer(t.getPadding().getValueType(), JsContainer.Location.MAPPED_TYPE_VALUE_TYPE, p)));
    }

    public J visitMappedTypeKeysRemapping(JS.MappedType.KeysRemapping mappedTypeKeys, P p) {
        JS.MappedType.KeysRemapping m = mappedTypeKeys;
        m = m.withPrefix(visitSpace(m.getPrefix(), JsSpace.Location.MAPPED_TYPE_KEYS_REMAPPING_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Statement temp = (Statement) visitStatement(m, p);
        if (!(temp instanceof JS.MappedType.KeysRemapping)) {
            return temp;
        } else {
            m = (JS.MappedType.KeysRemapping) temp;
        }
        m = m.getPadding().withTypeParameter(requireNonNull(visitRightPadded(m.getPadding().getTypeParameter(), JsRightPadded.Location.MAPPED_TYPE_KEYS_REMAPPING_TYPE_PARAMETER, p)));
        if (m.getNameType() != null) {
            m = m.getPadding().withNameType(requireNonNull(visitRightPadded(m.getPadding().getNameType(), JsRightPadded.Location.MAPPED_TYPE_KEYS_REMAPPING_NAME_TYPE, p)));
        }
        return m;
    }

    public J visitMappedTypeParameter(JS.MappedType.Parameter parameter, P p) {
        JS.MappedType.Parameter m = parameter;
        m = m.withPrefix(visitSpace(m.getPrefix(), JsSpace.Location.MAPPED_TYPE_MAPPED_TYPE_PARAMETER_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Statement temp = (Statement) visitStatement(m, p);
        if (!(temp instanceof JS.MappedType.Parameter)) {
            return temp;
        } else {
            m = (JS.MappedType.Parameter) temp;
        }
        m = m.withName(requireNonNull(visitAndCast(m.getName(), p)));
        return m.getPadding().withIterateType(requireNonNull(visitLeftPadded(m.getPadding().getIterateType(), JsLeftPadded.Location.MAPPED_TYPE_MAPPED_TYPE_PARAMETER_ITERATE, p)));
    }

    public J visitObjectBindingPattern(JS.ObjectBindingPattern objectBindingPattern, P p) {
        JS.ObjectBindingPattern o = objectBindingPattern;
        o = o.withPrefix(visitSpace(o.getPrefix(), JsSpace.Location.OBJECT_BINDING_DECLARATIONS_PREFIX, p));
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        Expression temp = (Expression) visitExpression(o, p);
        if (!(temp instanceof JS.ObjectBindingPattern)) {
            return temp;
        } else {
            o = (JS.ObjectBindingPattern) temp;
        }
        o = o.withLeadingAnnotations(requireNonNull(ListUtils.map(o.getLeadingAnnotations(), a -> visitAndCast(a, p))));
        o = o.withModifiers(requireNonNull(ListUtils.map(o.getModifiers(), e -> visitAndCast(e, p))));
        o = o.withTypeExpression(visitAndCast(o.getTypeExpression(), p));
        o = o.withTypeExpression(o.getTypeExpression() == null ?
                null :
                visitTypeName(o.getTypeExpression(), p));
        o = o.getPadding().withBindings(requireNonNull(visitContainer(o.getPadding().getBindings(), JsContainer.Location.BINDING_ELEMENT, p)));
        if (o.getPadding().getInitializer() != null) {
            o = o.getPadding().withInitializer(visitLeftPadded(o.getPadding().getInitializer(),
                    JsLeftPadded.Location.BINDING_ELEMENT_INITIALIZER, p));
        }
        return o;
    }

    public J visitPropertyAssignment(JS.PropertyAssignment propertyAssignment, P p) {
        JS.PropertyAssignment pa = propertyAssignment;
        pa = pa.withPrefix(visitSpace(pa.getPrefix(), JsSpace.Location.PROPERTY_ASSIGNMENT_PREFIX, p));
        pa = pa.withMarkers(visitMarkers(pa.getMarkers(), p));
        Statement temp = (Statement) visitStatement(pa, p);
        if (!(temp instanceof JS.PropertyAssignment)) {
            return temp;
        } else {
            pa = (JS.PropertyAssignment) temp;
        }
        pa = pa.getPadding().withName(requireNonNull(visitRightPadded(pa.getPadding().getName(), JsRightPadded.Location.PROPERTY_ASSIGNMENT_NAME, p)));
        return pa.withInitializer(visitAndCast(pa.getInitializer(), p));
    }

    public J visitSatisfiesExpression(JS.SatisfiesExpression satisfiesExpression, P p) {
        JS.SatisfiesExpression sa = satisfiesExpression;
        sa = sa.withPrefix(visitSpace(sa.getPrefix(), JsSpace.Location.SATISFIES_EXPRESSION_PREFIX, p));
        sa = sa.withMarkers(visitMarkers(sa.getMarkers(), p));
        Expression temp = (Expression) visitExpression(sa, p);
        if (!(temp instanceof JS.SatisfiesExpression)) {
            return temp;
        } else {
            sa = (JS.SatisfiesExpression) temp;
        }
        sa = sa.withExpression(requireNonNull(visitAndCast(sa.getExpression(), p)));
        return sa.getPadding().withSatisfiesType(requireNonNull(visitLeftPadded(sa.getPadding().getSatisfiesType(), JsLeftPadded.Location.SATISFIES_EXPRESSION_TYPE, p)));
    }

    public J visitScopedVariableDeclarations(JS.ScopedVariableDeclarations scopedVariableDeclarations, P p) {
        JS.ScopedVariableDeclarations vd = scopedVariableDeclarations;
        vd = vd.withPrefix(visitSpace(vd.getPrefix(), JsSpace.Location.SCOPED_VARIABLE_DECLARATIONS_PREFIX, p));
        vd = vd.withMarkers(visitMarkers(vd.getMarkers(), p));
        Statement temp = (Statement) visitStatement(vd, p);
        if (!(temp instanceof JS.ScopedVariableDeclarations)) {
            return temp;
        } else {
            vd = (JS.ScopedVariableDeclarations) temp;
        }
        vd = vd.withModifiers(requireNonNull(ListUtils.map(vd.getModifiers(), e -> visitAndCast(e, p))));
        return vd.getPadding().withVariables(requireNonNull(ListUtils.map(vd.getPadding().getVariables(), e -> visitRightPadded(e, JsRightPadded.Location.SCOPED_VARIABLE_DECLARATIONS_VARIABLE, p))));
    }

    public J visitStatementExpression(JS.StatementExpression expression, P p) {
        JS.StatementExpression se = expression;
        Expression temp = (Expression) visitExpression(se, p);
        if (!(temp instanceof JS.StatementExpression)) {
            return temp;
        } else {
            se = (JS.StatementExpression) temp;
        }
        J statement = visit(se.getStatement(), p);
        return se.withStatement((Statement) requireNonNull(statement));
    }

    public J visitTaggedTemplateExpression(JS.TaggedTemplateExpression taggedTemplateExpression, P p) {
        JS.TaggedTemplateExpression ta = taggedTemplateExpression;
        ta = ta.withPrefix(visitSpace(ta.getPrefix(), JsSpace.Location.TAGGED_TEMPLATE_EXPRESSION_PREFIX, p));
        ta = ta.withMarkers(visitMarkers(ta.getMarkers(), p));
        Expression temp = (Expression) visitExpression(ta, p);
        if (!(temp instanceof JS.TaggedTemplateExpression)) {
            return temp;
        } else {
            ta = (JS.TaggedTemplateExpression) temp;
        }
        ta = ta.getPadding().withTag(visitRightPadded(ta.getPadding().getTag(), JsRightPadded.Location.TEMPLATE_EXPRESSION_TAG, p));
        if (ta.getPadding().getTypeArguments() != null) {
            ta = ta.getPadding().withTypeArguments(visitContainer(ta.getPadding().getTypeArguments(), JsContainer.Location.TEMPLATE_EXPRESSION_TYPE_ARG_PARAMETERS, p));
        }
        return ta.withTemplateExpression(requireNonNull(visitAndCast(ta.getTemplateExpression(), p)));
    }

    public J visitTemplateExpression(JS.TemplateExpression templateExpression, P p) {
        JS.TemplateExpression te = templateExpression;
        te = te.withPrefix(visitSpace(te.getPrefix(), JsSpace.Location.TEMPLATE_EXPRESSION_PREFIX, p));
        te = te.withMarkers(visitMarkers(te.getMarkers(), p));
        Expression temp = (Expression) visitExpression(te, p);
        if (!(temp instanceof JS.TemplateExpression)) {
            return temp;
        } else {
            te = (JS.TemplateExpression) temp;
        }
        te = te.withHead(requireNonNull(visitAndCast(te.getHead(), p)));
        te = te.getPadding().withSpans(requireNonNull(ListUtils.map(te.getPadding().getSpans(), t -> this.visitRightPadded(t, JsRightPadded.Location.TEMPLATE_EXPRESSION_TEMPLATE_SPAN, p))));
        return te.withType(visitType(te.getType(), p));
    }

    public J visitTemplateExpressionSpan(JS.TemplateExpression.Span span, P p) {
        JS.TemplateExpression.Span s = span;
        s = s.withPrefix(visitSpace(s.getPrefix(), JsSpace.Location.TEMPLATE_EXPRESSION_SPAN_PREFIX, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withExpression(requireNonNull(visitAndCast(s.getExpression(), p)));
        s = s.withTail(requireNonNull(visitAndCast(s.getTail(), p)));
        return s.withTail(s.getTail());
    }

    public J visitTuple(JS.Tuple tuple, P p) {
        JS.Tuple t = tuple;
        t = t.withPrefix(visitSpace(t.getPrefix(), JsSpace.Location.TUPLE_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof JS.Tuple)) {
            return temp;
        } else {
            t = (JS.Tuple) temp;
        }
        t = t.getPadding().withElements(requireNonNull(visitContainer(t.getPadding().getElements(), JsContainer.Location.TUPLE_ELEMENT, p)));
        return t.withType(visitType(t.getType(), p));
    }

    public J visitTypeDeclaration(JS.TypeDeclaration typeDeclaration, P p) {
        JS.TypeDeclaration t = typeDeclaration;
        t = t.withPrefix(visitSpace(t.getPrefix(), JsSpace.Location.TYPE_DECLARATION_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Statement temp = (Statement) visitStatement(t, p);
        if (!(temp instanceof JS.TypeDeclaration)) {
            return temp;
        } else {
            t = (JS.TypeDeclaration) temp;
        }
        t = t.withModifiers(requireNonNull(ListUtils.map(t.getModifiers(), e -> visitAndCast(e, p))));
        t = t.getPadding().withName(requireNonNull(visitLeftPadded(t.getPadding().getName(), JsLeftPadded.Location.TYPE_DECLARATION_NAME, p)));
        t = t.withTypeParameters(visitAndCast(t.getTypeParameters(), p));
        t = t.getPadding().withInitializer(requireNonNull(visitLeftPadded(t.getPadding().getInitializer(),
                JsLeftPadded.Location.TYPE_DECLARATION_INITIALIZER, p)));
        return t.withType(visitType(t.getType(), p));
    }

    public J visitComputedPropertyName(JS.ComputedPropertyName computedPropertyName, P p) {
        JS.ComputedPropertyName t = computedPropertyName;
        t = t.withPrefix(visitSpace(t.getPrefix(), JsSpace.Location.COMPUTED_PROPERTY_NAME_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof JS.ComputedPropertyName)) {
            return temp;
        } else {
            t = (JS.ComputedPropertyName) temp;
        }
        return t.getPadding().withExpression(requireNonNull(visitRightPadded(t.getPadding().getExpression(), JsRightPadded.Location.COMPUTED_PROPERTY_NAME_SUFFIX, p)));
    }

    public J visitTypeOperator(JS.TypeOperator typeOperator, P p) {
        JS.TypeOperator t = typeOperator;
        t = t.withPrefix(visitSpace(t.getPrefix(), JsSpace.Location.TYPE_OPERATOR_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof JS.TypeOperator)) {
            return temp;
        } else {
            t = (JS.TypeOperator) temp;
        }
        return t.getPadding().withExpression(requireNonNull(visitLeftPadded(t.getPadding().getExpression(), JsLeftPadded.Location.TYPE_OPERATOR_EXPRESSION, p)));
    }

    public J visitTypePredicate(JS.TypePredicate typePredicate, P p) {
        JS.TypePredicate t = typePredicate;
        t = t.withPrefix(visitSpace(t.getPrefix(), JsSpace.Location.TYPE_PREDICATE_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof JS.TypePredicate)) {
            return temp;
        } else {
            t = (JS.TypePredicate) temp;
        }
        t = t.getPadding().withAsserts(requireNonNull(visitLeftPadded(t.getPadding().getAsserts(), JsLeftPadded.Location.TYPE_PREDICATE_ASSERTS, p)));
        t = t.withParameterName(requireNonNull(visitAndCast(t.getParameterName(), p)));
        return t.getPadding().withExpression(visitLeftPadded(t.getPadding().getExpression(), JsLeftPadded.Location.TYPE_PREDICATE_EXPRESSION, p));
    }

    public J visitUnion(JS.Union union, P p) {
        JS.Union u = union;
        u = u.withPrefix(visitSpace(u.getPrefix(), JsSpace.Location.UNION_PREFIX, p));
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        Expression temp = (Expression) visitExpression(u, p);
        if (!(temp instanceof JS.Union)) {
            return temp;
        } else {
            u = (JS.Union) temp;
        }
        u = u.getPadding().withTypes(requireNonNull(ListUtils.map(u.getPadding().getTypes(), t -> visitRightPadded(t, JsRightPadded.Location.UNION_TYPE, p))));
        return u.withType(visitType(u.getType(), p));
    }

    public J visitIntersection(JS.Intersection intersection, P p) {
        JS.Intersection u = intersection;
        u = u.withPrefix(visitSpace(u.getPrefix(), JsSpace.Location.INTERSECTION_PREFIX, p));
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        Expression temp = (Expression) visitExpression(u, p);
        if (!(temp instanceof JS.Intersection)) {
            return temp;
        } else {
            u = (JS.Intersection) temp;
        }
        u = u.getPadding().withTypes(requireNonNull(ListUtils.map(u.getPadding().getTypes(), t -> visitRightPadded(t, JsRightPadded.Location.INTERSECTION_TYPE, p))));
        return u.withType(visitType(u.getType(), p));
    }

    public J visitExportDeclaration(JS.ExportDeclaration exportDeclaration, P p) {
        JS.ExportDeclaration ed = exportDeclaration;
        ed = ed.withPrefix(visitSpace(ed.getPrefix(), JsSpace.Location.EXPORT_DECLARATION_PREFIX, p));
        ed = ed.withMarkers(visitMarkers(ed.getMarkers(), p));
        Statement temp = (Statement) visitStatement(ed, p);
        if (!(temp instanceof JS.ExportDeclaration)) {
            return temp;
        } else {
            ed = (JS.ExportDeclaration) temp;
        }

        ed = ed.withModifiers(requireNonNull(ListUtils.map(ed.getModifiers(), e -> visitAndCast(e, p))));
        ed = ed.getPadding().withTypeOnly(requireNonNull(visitLeftPadded(ed.getPadding().getTypeOnly(), JsLeftPadded.Location.EXPORT_DECLARATION_TYPE_ONLY, p)));
        ed = ed.withExportClause(visitAndCast(ed.getExportClause(), p));
        ed = ed.getPadding().withModuleSpecifier(visitLeftPadded(ed.getPadding().getModuleSpecifier(), JsLeftPadded.Location.EXPORT_DECLARATION_MODULE_SPECIFIER, p));
        return ed.withAttributes(visitAndCast(ed.getAttributes(), p));
    }

    public J visitExportAssignment(JS.ExportAssignment exportAssignment, P p) {
        JS.ExportAssignment es = exportAssignment;
        es = es.withPrefix(visitSpace(es.getPrefix(), JsSpace.Location.EXPORT_ASSIGNMENT_PREFIX, p));
        es = es.withMarkers(visitMarkers(es.getMarkers(), p));
        Statement temp = (Statement) visitStatement(es, p);
        if (!(temp instanceof JS.ExportAssignment)) {
            return temp;
        } else {
            es = (JS.ExportAssignment) temp;
        }
        return es.getPadding().withExpression(requireNonNull(visitLeftPadded(es.getPadding().getExpression(), JsLeftPadded.Location.EXPORT_ASSIGNMENT_EXPRESSION, p)));
    }

    public J visitNamedExports(JS.NamedExports namedExports, P p) {
        JS.NamedExports ne = namedExports;
        ne = ne.withPrefix(visitSpace(ne.getPrefix(), JsSpace.Location.NAMED_EXPORTS_PREFIX, p));
        ne = ne.withMarkers(visitMarkers(ne.getMarkers(), p));
        Expression temp = (Expression) visitExpression(ne, p);
        if (!(temp instanceof JS.NamedExports)) {
            return temp;
        } else {
            ne = (JS.NamedExports) temp;
        }
        ne = ne.getPadding().withElements(requireNonNull(visitContainer(ne.getPadding().getElements(), JsContainer.Location.NAMED_EXPORTS_ELEMENTS, p)));
        return ne.withType(visitType(ne.getType(), p));
    }

    public J visitExportSpecifier(JS.ExportSpecifier exportSpecifier, P p) {
        JS.ExportSpecifier es = exportSpecifier;
        es = es.withPrefix(visitSpace(es.getPrefix(), JsSpace.Location.EXPORT_SPECIFIER_PREFIX, p));
        es = es.withMarkers(visitMarkers(es.getMarkers(), p));
        Expression temp = (Expression) visitExpression(es, p);
        if (!(temp instanceof JS.ExportSpecifier)) {
            return temp;
        } else {
            es = (JS.ExportSpecifier) temp;
        }
        es = es.getPadding().withTypeOnly(requireNonNull(visitLeftPadded(es.getPadding().getTypeOnly(), JsLeftPadded.Location.EXPORT_SPECIFIER_TYPE_ONLY, p)));
        es = es.withSpecifier(requireNonNull(visitAndCast(es.getSpecifier(), p)));
        return es.withType(visitType(es.getType(), p));
    }

    public J visitIndexedAccessType(JS.IndexedAccessType indexedAccessType, P p) {
        JS.IndexedAccessType iat = indexedAccessType;
        iat = iat.withPrefix(visitSpace(iat.getPrefix(), JsSpace.Location.INDEXED_ACCESS_TYPE_PREFIX, p));
        iat = iat.withMarkers(visitMarkers(iat.getMarkers(), p));
        Expression temp = (Expression) visitExpression(iat, p);
        if (!(temp instanceof JS.IndexedAccessType)) {
            return temp;
        } else {
            iat = (JS.IndexedAccessType) temp;
        }
        iat = iat.withObjectType(requireNonNull(visitAndCast(iat.getObjectType(), p)));
        iat = iat.withIndexType(requireNonNull(visitAndCast(iat.getIndexType(), p)));
        return iat.withType(visitType(iat.getType(), p));
    }

    public J visitIndexedAccessTypeIndexType(JS.IndexedAccessType.IndexType indexedAccessTypeIndexType, P p) {
        JS.IndexedAccessType.IndexType iatit = indexedAccessTypeIndexType;
        iatit = iatit.withPrefix(visitSpace(iatit.getPrefix(), JsSpace.Location.INDEXED_ACCESS_TYPE_INDEX_TYPE_PREFIX, p));
        iatit = iatit.withMarkers(visitMarkers(iatit.getMarkers(), p));
        Expression temp = (Expression) visitExpression(iatit, p);
        if (!(temp instanceof JS.IndexedAccessType.IndexType)) {
            return temp;
        } else {
            iatit = (JS.IndexedAccessType.IndexType) temp;
        }
        iatit = iatit.getPadding().withElement(requireNonNull(visitRightPadded(iatit.getPadding().getElement(), JsRightPadded.Location.INDEXED_ACCESS_TYPE_INDEX_TYPE_ELEMENT, p)));
        return iatit.withType(visitType(iatit.getType(), p));
    }

    // TODO: remove me. Requires changes from rewrite-java.
    @Override
    public J visitAnnotatedType(J.AnnotatedType annotatedType, P p) {
        J.AnnotatedType a = annotatedType;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ANNOTATED_TYPE_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Expression temp = (Expression) visitExpression(a, p);
        if (!(temp instanceof J.AnnotatedType)) {
            return temp;
        } else {
            a = (J.AnnotatedType) temp;
        }
        a = a.withAnnotations(requireNonNull(ListUtils.map(a.getAnnotations(), e -> visitAndCast(e, p))));
        //noinspection DataFlowIssue
        a = a.withTypeExpression(visitAndCast(a.getTypeExpression(), p));
        return a.withTypeExpression(visitTypeName(a.getTypeExpression(), p));
    }

    @Override
    public J visitParameterizedType(J.ParameterizedType type, P p) {
        J.ParameterizedType pt = type;
        pt = pt.withPrefix(visitSpace(pt.getPrefix(), Space.Location.PARAMETERIZED_TYPE_PREFIX, p));
        pt = pt.withMarkers(visitMarkers(pt.getMarkers(), p));
        Expression temp = (Expression) visitExpression(pt, p);
        if (!(temp instanceof J.ParameterizedType)) {
            return temp;
        } else {
            pt = (J.ParameterizedType) temp;
        }
        pt = pt.withClazz(requireNonNull(visitAndCast(pt.getClazz(), p)));
        if (pt.getPadding().getTypeParameters() != null) {
            pt = pt.getPadding().withTypeParameters(visitContainer(pt.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, p));
        }
        pt = pt.getPadding().withTypeParameters(visitTypeNames(pt.getPadding().getTypeParameters(), p));
        return pt.withType(visitType(pt.getType(), p));
    }

    private <N extends NameTree> @Nullable JLeftPadded<N> visitTypeName(@Nullable JLeftPadded<N> nameTree, P p) {
        return nameTree == null ? null : nameTree.withElement(visitTypeName(nameTree.getElement(), p));
    }

    private <N extends NameTree> @Nullable JRightPadded<N> visitTypeName(@Nullable JRightPadded<N> nameTree, P p) {
        return nameTree == null ? null : nameTree.withElement(visitTypeName(nameTree.getElement(), p));
    }

    private <J2 extends J> @Nullable JContainer<J2> visitTypeNames(@Nullable JContainer<J2> nameTrees, P p) {
        if (nameTrees == null) {
            return null;
        }
        @SuppressWarnings("unchecked") List<JRightPadded<J2>> js = ListUtils.map(nameTrees.getPadding().getElements(),
                t -> requireNonNull(t).getElement() instanceof NameTree ? (JRightPadded<J2>) visitTypeName((JRightPadded<NameTree>) t, p) : t);
        return js == nameTrees.getPadding().getElements() ? nameTrees : JContainer.build(nameTrees.getBefore(), requireNonNull(js), Markers.EMPTY);
    }

    public Space visitSpace(Space space, JsSpace.Location loc, P p) {
        return visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
    }

    public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JsRightPadded.Location loc, P p) {
        return super.visitRightPadded(right, JRightPadded.Location.LANGUAGE_EXTENSION, p);
    }

    public <T> @Nullable JLeftPadded<T> visitLeftPadded(@Nullable JLeftPadded<T> left, JsLeftPadded.Location loc, P p) {
        return super.visitLeftPadded(left, JLeftPadded.Location.LANGUAGE_EXTENSION, p);
    }

    public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container, JsContainer.Location loc, P p) {
        return super.visitContainer(container, JContainer.Location.LANGUAGE_EXTENSION, p);
    }

    public J visitTypeOf(JS.TypeOf typeOf, P p) {
        JS.TypeOf t = typeOf;
        t = t.withPrefix(visitSpace(t.getPrefix(), JsSpace.Location.TYPEOF_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof JS.TypeOf)) {
            return temp;
        } else {
            t = (JS.TypeOf) temp;
        }
        t = t.withExpression(requireNonNull(visitAndCast(t.getExpression(), p)));
        return t.withType(visitType(t.getType(), p));
    }

    public J visitTypeQuery(JS.TypeQuery typeQuery, P p) {
        JS.TypeQuery t = typeQuery;
        t = t.withPrefix(visitSpace(t.getPrefix(), JsSpace.Location.TYPE_QUERY_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof JS.TypeQuery)) {
            return temp;
        } else {
            t = (JS.TypeQuery) temp;
        }
        t = t.withTypeExpression(requireNonNull(visitAndCast(t.getTypeExpression(), p)));
        if (t.getPadding().getTypeArguments() != null) {
            t = t.getPadding().withTypeArguments(visitContainer(t.getPadding().getTypeArguments(), JsContainer.Location.TYPE_QUERY_TYPE_ARGUMENTS, p));
        }
        return t.withType(visitType(t.getType(), p));
    }

    public J visitVoid(JS.Void aVoid, P p) {
        JS.Void v = aVoid;
        v = v.withPrefix(visitSpace(v.getPrefix(), JsSpace.Location.VOID_PREFIX, p));
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        Expression temp = (Expression) visitExpression(v, p);
        if (!(temp instanceof JS.Void)) {
            return temp;
        } else {
            v = (JS.Void) temp;
        }
        return v.withExpression(requireNonNull(visitAndCast(v.getExpression(), p)));
    }

    public J visitTypeInfo(JS.TypeInfo typeInfo, P p) {
        JS.TypeInfo ti = typeInfo;
        ti = ti.withPrefix(visitSpace(ti.getPrefix(), JsSpace.Location.TYPE_INFO_PREFIX, p));
        ti = ti.withMarkers(visitMarkers(ti.getMarkers(), p));
        Expression temp = (Expression) visitExpression(ti, p);
        if (!(temp instanceof JS.TypeInfo)) {
            return temp;
        } else {
            ti = (JS.TypeInfo) temp;
        }
        return ti.withTypeIdentifier(requireNonNull(visitAndCast(ti.getTypeIdentifier(), p)));
    }

    public J visitComputedPropertyMethodDeclaration(JS.ComputedPropertyMethodDeclaration method, P p) {
        JS.ComputedPropertyMethodDeclaration m = method;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.METHOD_DECLARATION_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Statement temp = (Statement) visitStatement(m, p);
        if (!(temp instanceof JS.ComputedPropertyMethodDeclaration)) {
            return temp;
        } else {
            m = (JS.ComputedPropertyMethodDeclaration) temp;
        }
        m = m.withLeadingAnnotations(requireNonNull(ListUtils.map(m.getLeadingAnnotations(), a -> visitAndCast(a, p))));
        m = m.withModifiers(requireNonNull(ListUtils.map(m.getModifiers(), e -> visitAndCast(e, p))));
        m = m.withTypeParameters(visitAndCast(m.getTypeParameters(), p));
        m = m.withReturnTypeExpression(visitAndCast(m.getReturnTypeExpression(), p));
        m = m.withReturnTypeExpression(
                m.getReturnTypeExpression() == null ?
                        null :
                        visitTypeName(m.getReturnTypeExpression(), p));
        m = m.withName(requireNonNull(this.visitAndCast(m.getName(), p)));
        m = m.getPadding().withParameters(requireNonNull(visitContainer(m.getPadding().getParameters(), JContainer.Location.METHOD_DECLARATION_PARAMETERS, p)));
        m = m.withBody(visitAndCast(m.getBody(), p));
        return m.withMethodType((JavaType.Method) visitType(m.getMethodType(), p));
    }

    public J visitNamespaceDeclaration(JS.NamespaceDeclaration namespaceDeclaration, P p) {
        JS.NamespaceDeclaration ns = namespaceDeclaration;
        ns = ns.withPrefix(visitSpace(ns.getPrefix(), JsSpace.Location.NAMESPACE_DECLARATION_PREFIX, p));
        ns = ns.withMarkers(visitMarkers(ns.getMarkers(), p));
        Statement temp = (Statement) visitStatement(ns, p);
        if (!(temp instanceof JS.NamespaceDeclaration)) {
            return temp;
        } else {
            ns = (JS.NamespaceDeclaration) temp;
        }
        ns = ns.withModifiers(requireNonNull(ListUtils.map(ns.getModifiers(), m -> visitAndCast(m, p))));
        ns = ns.getPadding().withKeywordType(requireNonNull(visitLeftPadded(ns.getPadding().getKeywordType(), JsLeftPadded.Location.NAMESPACE_DECLARATION_KEYWORD_TYPE, p)));
        ns = ns.getPadding().withName(requireNonNull(visitRightPadded(ns.getPadding().getName(), JsRightPadded.Location.NAMESPACE_DECLARATION_NAME, p)));
        return ns.withBody(visitAndCast(ns.getBody(), p));
    }

    public J visitTypeLiteral(JS.TypeLiteral typeLiteral, P p) {
        JS.TypeLiteral t = typeLiteral;
        t = t.withPrefix(visitSpace(t.getPrefix(), JsSpace.Location.TYPE_LITERAL_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));

        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof JS.TypeLiteral)) {
            return temp;
        } else {
            t = (JS.TypeLiteral) temp;
        }

        t = t.withMembers(requireNonNull(visitAndCast(t.getMembers(), p)));
        return t.withType(visitType(t.getType(), p));
    }

    public J visitImportType(JS.ImportType importType, P p) {
        JS.ImportType t = importType;
        t = t.withPrefix(visitSpace(t.getPrefix(), JsSpace.Location.IMPORT_TYPE_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof JS.ImportType)) {
            return temp;
        } else {
            t = (JS.ImportType) temp;
        }
        t = t.getPadding().withHasTypeof(requireNonNull(visitRightPadded(t.getPadding().getHasTypeof(), JsRightPadded.Location.IMPORT_TYPE_TYPEOF, p)));
        t = t.getPadding().withArgumentAndAttributes(requireNonNull(visitContainer(t.getPadding().getArgumentAndAttributes(), JsContainer.Location.IMPORT_TYPE_ARGUMENTS_AND_ATTRIBUTES, p)));
        t = t.getPadding().withQualifier(visitLeftPadded(t.getPadding().getQualifier(), JsLeftPadded.Location.IMPORT_TYPE_QUALIFIER, p));
        if (t.getPadding().getTypeArguments() != null) {
            t = t.getPadding().withTypeArguments(visitContainer(t.getPadding().getTypeArguments(), JsContainer.Location.IMPORT_TYPE_TYPE_ARGUMENTS, p));
        }
        return t;
    }

    public J visitIndexSignatureDeclaration(JS.IndexSignatureDeclaration indexSignatureDeclaration, P p) {
        JS.IndexSignatureDeclaration isd = indexSignatureDeclaration;
        isd = isd.withPrefix(visitSpace(isd.getPrefix(), JsSpace.Location.INDEXED_SIGNATURE_DECLARATION_PREFIX, p));
        isd = isd.withMarkers(visitMarkers(isd.getMarkers(), p));

        Statement temp = (Statement) visitStatement(isd, p);
        if (!(temp instanceof JS.IndexSignatureDeclaration)) {
            return temp;
        } else {
            isd = (JS.IndexSignatureDeclaration) temp;
        }

        isd = isd.withModifiers(requireNonNull(ListUtils.map(isd.getModifiers(), e -> visitAndCast(e, p))));
        isd = isd.getPadding().withParameters(requireNonNull(visitContainer(isd.getPadding().getParameters(), JsContainer.Location.INDEXED_SIGNATURE_DECLARATION_PARAMETERS, p)));
        isd = isd.getPadding().withTypeExpression(requireNonNull(visitLeftPadded(isd.getPadding().getTypeExpression(), JsLeftPadded.Location.INDEXED_SIGNATURE_DECLARATION_TYPE_EXPRESSION, p)));
        return isd.withType(visitType(isd.getType(), p));
    }

    public J visitForOfLoop(JS.ForOfLoop forOfLoop, P p) {
        JS.ForOfLoop f = forOfLoop;
        f = f.withPrefix(visitSpace(f.getPrefix(), JsSpace.Location.FOR_OF_LOOP_PREFIX, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        Statement temp = (Statement) visitStatement(f, p);
        if (!(temp instanceof JS.ForOfLoop)) {
            return temp;
        } else {
            f = (JS.ForOfLoop) temp;
        }
        return f.withLoop(requireNonNull(visitAndCast(f.getLoop(), p)));
    }

    public J visitForInLoop(JS.ForInLoop forInLoop, P p) {
        JS.ForInLoop f = forInLoop;
        f = f.withPrefix(visitSpace(f.getPrefix(), JsSpace.Location.FOR_IN_LOOP_PREFIX, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        Statement temp = (Statement) visitStatement(f, p);
        if (!(temp instanceof JS.ForInLoop)) {
            return temp;
        } else {
            f = (JS.ForInLoop) temp;
        }
        f = f.withControl(requireNonNull(visitAndCast(f.getControl(), p)));
        return f.getPadding().withBody(requireNonNull(visitRightPadded(f.getPadding().getBody(), JsRightPadded.Location.FOR_BODY, p)));
    }

    public J visitArrayBindingPattern(JS.ArrayBindingPattern arrayBindingPattern, P p) {
        JS.ArrayBindingPattern c = arrayBindingPattern;
        c = c.withPrefix(visitSpace(c.getPrefix(), JsSpace.Location.ARRAY_BINDING_PATTERN_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        Expression temp = (Expression) visitExpression(c, p);
        if (!(temp instanceof JS.ArrayBindingPattern)) {
            return temp;
        } else {
            c = (JS.ArrayBindingPattern) temp;
        }
        return c.getPadding().withElements(requireNonNull(visitContainer(c.getPadding().getElements(), JsContainer.Location.ARRAY_BINDING_PATTERN_ELEMENTS, p)));
    }

    public J visitAs(JS.As as_, P p) {
        JS.As b = as_;
        b = b.withPrefix(visitSpace(b.getPrefix(), JsSpace.Location.AS_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitExpression(b, p);
        if (!(temp instanceof JS.As)) {
            return temp;
        } else {
            b = (JS.As) temp;
        }
        b = b.getPadding().withLeft(requireNonNull(visitRightPadded(b.getPadding().getLeft(), JsRightPadded.Location.AS_LEFT, p)));
        b = b.withRight(requireNonNull(visitAndCast(b.getRight(), p)));
        return b.withType(visitType(b.getType(), p));
    }

    public J visitAssignmentOperationExtensions(JS.AssignmentOperation assignOp, P p) {
        JS.AssignmentOperation a = assignOp;
        a = a.withPrefix(visitSpace(a.getPrefix(), JsSpace.Location.ASSIGNMENT_OPERATION_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Statement temp = (Statement) visitStatement(a, p);
        if (!(temp instanceof JS.AssignmentOperation)) {
            return temp;
        } else {
            a = (JS.AssignmentOperation) temp;
        }
        Expression temp2 = (Expression) visitExpression(a, p);
        if (!(temp2 instanceof JS.AssignmentOperation)) {
            return temp2;
        } else {
            a = (JS.AssignmentOperation) temp2;
        }
        a = a.withVariable(requireNonNull(visitAndCast(a.getVariable(), p)));
        a = a.getPadding().withOperator(requireNonNull(visitLeftPadded(a.getPadding().getOperator(), JsLeftPadded.Location.ASSIGNMENT_OPERATION_OPERATOR, p)));
        a = a.withAssignment(requireNonNull(visitAndCast(a.getAssignment(), p)));
        return a.withType(visitType(a.getType(), p));
    }

    public J visitTypeTreeExpression(JS.TypeTreeExpression typeTreeExpression, P p) {
        JS.TypeTreeExpression a = typeTreeExpression;
        a = a.withPrefix(visitSpace(a.getPrefix(), JsSpace.Location.TYPE_TREE_EXPRESSION_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Expression temp = (Expression) visitExpression(a, p);
        if (!(temp instanceof JS.TypeTreeExpression)) {
            return temp;
        } else {
            a = (JS.TypeTreeExpression) temp;
        }

        a = a.withExpression(requireNonNull(visitAndCast(a.getExpression(), p)));
        return a.withType(visitType(a.getType(), p));
    }

    public J visitWithStatement(JS.WithStatement withStatement, P p) {
        JS.WithStatement w = withStatement;
        w = w.withPrefix(visitSpace(w.getPrefix(), JsSpace.Location.WITH_PREFIX, p));
        w = w.withMarkers(visitMarkers(w.getMarkers(), p));
        Statement temp = (Statement) visitStatement(w, p);
        if (!(temp instanceof JS.WithStatement)) {
            return temp;
        } else {
            w = (JS.WithStatement) temp;
        }

        w = w.withExpression(requireNonNull(visitAndCast(w.getExpression(), p)));
        return w.getPadding().withBody(requireNonNull(visitRightPadded(w.getPadding().getBody(), JsRightPadded.Location.WITH_BODY, p)));
    }

    public J visitJsxTag(JSX.Tag tag, P p) {
        JSX.Tag t = tag;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));

        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof JSX.Tag)) {
            return temp;
        } else {
            t = (JSX.Tag) temp;
        }

        t = t.getPadding().withOpenName(requireNonNull(visitLeftPadded(t.getPadding().getOpenName(), JLeftPadded.Location.LANGUAGE_EXTENSION, p)));
        if (t.getTypeArguments() != null) {
            t = t.withTypeArguments(visitContainer(t.getTypeArguments(), JContainer.Location.TYPE_PARAMETERS, p));
        }
        t = t.withAfterName(visitSpace(t.getAfterName(), Space.Location.LANGUAGE_EXTENSION, p));
        t = t.getPadding().withAttributes(requireNonNull(ListUtils.map(t.getPadding().getAttributes(), attr -> visitRightPadded(attr, JRightPadded.Location.LANGUAGE_EXTENSION, p))));

        if (t.isSelfClosing()) {
            t = t.withSelfClosing(visitSpace(requireNonNull(t.getSelfClosing()), Space.Location.LANGUAGE_EXTENSION, p));
        } else if (t.hasChildren()) {
            t = t.withChildren(ListUtils.map(t.getChildren(), child -> visitAndCast(child, p)));
            t = t.getPadding().withClosingName(requireNonNull(visitLeftPadded(t.getPadding().getClosingName(), JLeftPadded.Location.LANGUAGE_EXTENSION, p)));
            t = t.withAfterClosingName(visitSpace(requireNonNull(t.getAfterClosingName()), Space.Location.LANGUAGE_EXTENSION, p));
        }

        return t.withType(visitType(t.getType(), p));
    }

    public J visitJsxAttribute(JSX.Attribute attribute, P p) {
        JSX.Attribute a = attribute;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withKey(requireNonNull(visitAndCast(a.getKey(), p)));
        if (a.getPadding().getValue() != null) {
            a = a.getPadding().withValue(visitLeftPadded(a.getPadding().getValue(), JLeftPadded.Location.LANGUAGE_EXTENSION, p));
        }

        return a;
    }

    public J visitJsxSpreadAttribute(JSX.SpreadAttribute spreadAttribute, P p) {
        JSX.SpreadAttribute s = spreadAttribute;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withDots(visitSpace(s.getDots(), Space.Location.LANGUAGE_EXTENSION, p));
        return s.getPadding().withExpression(requireNonNull(visitRightPadded(s.getPadding().getExpression(), JRightPadded.Location.LANGUAGE_EXTENSION, p)));
    }

    public J visitJsxEmbeddedExpression(JSX.EmbeddedExpression embeddedExpression, P p) {
        JSX.EmbeddedExpression e = embeddedExpression;
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));

        Expression temp = (Expression) visitExpression(e, p);
        if (!(temp instanceof JSX.EmbeddedExpression)) {
            return temp;
        } else {
            e = (JSX.EmbeddedExpression) temp;
        }

        return e.getPadding().withExpression(requireNonNull(visitRightPadded(e.getPadding().getExpression(), JRightPadded.Location.LANGUAGE_EXTENSION, p)));
    }

    public J visitJsxNamespacedName(JSX.NamespacedName namespacedName, P p) {
        JSX.NamespacedName n = namespacedName;
        n = n.withPrefix(visitSpace(n.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        n = n.withMarkers(visitMarkers(n.getMarkers(), p));
        n = n.withNamespace(requireNonNull(visitAndCast(n.getNamespace(), p)));
        return n.getPadding().withName(requireNonNull(visitLeftPadded(n.getPadding().getName(), JLeftPadded.Location.LANGUAGE_EXTENSION, p)));
    }
}
