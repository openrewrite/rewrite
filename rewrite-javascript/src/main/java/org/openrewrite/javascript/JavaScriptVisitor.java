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

    public J visitCompilationUnit(JS.CompilationUnit cu, P p) {
        JS.CompilationUnit c = cu;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.getPadding().withImports(ListUtils.map(c.getPadding().getImports(), t -> visitRightPadded(t, JRightPadded.Location.IMPORT, p)));
        c = c.getPadding().withStatements(ListUtils.map(c.getPadding().getStatements(), e -> visitRightPadded(e, JRightPadded.Location.BLOCK_STATEMENT, p)));
        c = c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
        return c;
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
        a = a.withAlias(requireNonNull(visitAndCast(a.getAlias(), p)));
        return a;
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

        a = a.withParameters(
                a.getParameters().withPrefix(
                        visitSpace(a.getParameters().getPrefix(), Space.Location.LAMBDA_PARAMETERS_PREFIX, p)
                )
        );
        a = a.withParameters(
                a.getParameters().getPadding().withParameters(
                        requireNonNull(ListUtils.map(a.getParameters().getPadding().getParameters(),
                                param -> visitRightPadded(param, JRightPadded.Location.LAMBDA_PARAM, p)
                        ))
                )
        );
        a = a.withParameters(requireNonNull(visitAndCast(a.getParameters(), p)));
        a = a.withReturnTypeExpression(visitAndCast(a.getReturnTypeExpression(), p));
        a = a.getPadding().withBody(requireNonNull(visitLeftPadded(a.getPadding().getBody(), JsLeftPadded.Location.LAMBDA_ARROW, p)));
        a = a.withType(visitType(a.getType(), p));
        return a;
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
        a = a.withExpression(requireNonNull(visitAndCast(a.getExpression(), p)));
        return a;
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
        b = b.withVariableType((JavaType.Variable) visitType(b.getVariableType(), p));
        return b;
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
        t = t.withType(visitType(t.getType(), p));
        return t;
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
        d = d.withType(visitType(d.getType(), p));
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
        es = es.withExpression((Expression) requireNonNull(expression));
        return es;
    }

    public J visitTrailingTokenStatement(JS.TrailingTokenStatement statement, P p) {
        JS.TrailingTokenStatement st = statement;
        st = st.withPrefix(visitSpace(st.getPrefix(), JsSpace.Location.TRAILING_TOKEN_PREFIX, p));
        st = st.withMarkers(visitMarkers(st.getMarkers(), p));
        Statement temp = (Statement) visitStatement(st, p);
        if (!(temp instanceof JS.TrailingTokenStatement)) {
            return temp;
        } else {
            st = (JS.TrailingTokenStatement) temp;
        }
        st = st.getPadding().withExpression(requireNonNull(visitRightPadded(st.getPadding().getExpression(), JsRightPadded.Location.TRAILING_TOKEN_EXPRESSION, p)));
        st = st.withType(visitType(st.getType(), p));
        return st;
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
        ta = ta.withType(visitType(ta.getType(), p));
        return ta;
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
        f = f.getPadding().withReturnType((requireNonNull(visitLeftPadded(f.getPadding().getReturnType(), JsLeftPadded.Location.FUNCTION_TYPE_RETURN_TYPE, p))));
        f = f.withType(visitType(f.getType(), p));
        return f;
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
        t = t.withType(visitType(t.getType(), p));
        return t;
    }

    public J visitJsBinary(JS.JsBinary binary, P p) {
        JS.JsBinary b = binary;
        b = b.withPrefix(visitSpace(b.getPrefix(), JsSpace.Location.BINARY_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitExpression(b, p);
        if (!(temp instanceof JS.JsBinary)) {
            return temp;
        } else {
            b = (JS.JsBinary) temp;
        }
        b = b.withLeft(requireNonNull(visitAndCast(b.getLeft(), p)));
        b = b.getPadding().withOperator(requireNonNull(visitLeftPadded(b.getPadding().getOperator(), JsLeftPadded.Location.BINARY_OPERATOR, p)));
        b = b.withRight(requireNonNull(visitAndCast(b.getRight(), p)));
        b = b.withType(visitType(b.getType(), p));
        return b;
    }

    public J visitJsImport(JS.JsImport jsImport, P p) {
        JS.JsImport i = jsImport;
        i = i.withPrefix(visitSpace(i.getPrefix(), JsSpace.Location.IMPORT_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        Statement temp = (Statement) visitStatement(i, p);
        if (!(temp instanceof JS.JsImport)) {
            return temp;
        } else {
            i = (JS.JsImport) temp;
        }
        i = i.withModifiers(requireNonNull(ListUtils.map(i.getModifiers(), e -> visitAndCast(e, p))));
        i = i.withImportClause(visitAndCast(i.getImportClause(), p));
        i = i.getPadding().withModuleSpecifier(requireNonNull(visitLeftPadded(i.getPadding().getModuleSpecifier(), JsLeftPadded.Location.JS_IMPORT_MODULE_SPECIFIER, p)));
        i = i.withAttributes(visitAndCast(i.getAttributes(), p));
        return i;
    }

    public J visitJsImportClause(JS.JsImportClause jsImportClause, P p) {
        JS.JsImportClause i = jsImportClause;
        i = i.withPrefix(visitSpace(i.getPrefix(), JsSpace.Location.JS_IMPORT_CLAUSE_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.getPadding().withName(visitRightPadded(i.getPadding().getName(), JsRightPadded.Location.JS_IMPORT_CLAUSE_NAME, p));
        i = i.withNamedBindings(visitAndCast(i.getNamedBindings(), p));
        return i;
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
        ne = ne.withType(visitType(ne.getType(), p));
        return ne;
    }

    public J visitJsImportSpecifier(JS.JsImportSpecifier jis, P p) {
        JS.JsImportSpecifier i = jis;
        i = i.withPrefix(visitSpace(i.getPrefix(), JsSpace.Location.JS_IMPORT_SPECIFIER_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        Expression temp = (Expression) visitExpression(i, p);
        if (!(temp instanceof JS.JsImportSpecifier)) {
            return temp;
        } else {
            i = (JS.JsImportSpecifier) temp;
        }
        i = i.getPadding().withImportType(requireNonNull(visitLeftPadded(i.getPadding().getImportType(), JsLeftPadded.Location.JS_IMPORT_SPECIFIER_IMPORT_TYPE, p)));
        i = i.withSpecifier(requireNonNull(visitAndCast(i.getSpecifier(), p)));
        i = i.withType(visitType(i.getType(), p));
        return i;
    }

    public J visitImportAttributes(JS.ImportAttributes importAttributes, P p) {
        JS.ImportAttributes i = importAttributes;
        i = i.withPrefix(visitSpace(i.getPrefix(), JsSpace.Location.JS_IMPORT_ATTRIBUTES_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.getPadding().withElements(requireNonNull(visitContainer(i.getPadding().getElements(), JsContainer.Location.JS_IMPORT_ATTRIBUTES_ELEMENTS, p)));
        return i;
    }

    public J visitImportTypeAttributes(JS.ImportTypeAttributes importTypeAttributes, P p) {
        JS.ImportTypeAttributes i = importTypeAttributes;
        i = i.withPrefix(visitSpace(i.getPrefix(), JsSpace.Location.JS_IMPORT_TYPE_ATTRIBUTES_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.getPadding().withToken(requireNonNull(visitRightPadded(i.getPadding().getToken(), JsRightPadded.Location.JS_IMPORT_TYPE_ATTRIBUTES_TOKEN, p)));
        i = i.getPadding().withElements(requireNonNull(visitContainer(i.getPadding().getElements(), JsContainer.Location.JS_IMPORT_TYPE_ATTRIBUTES_ELEMENTS, p)));
        i = i.withEnd(requireNonNull(visitSpace(i.getEnd(), JsSpace.Location.JS_IMPORT_TYPE_ATTRIBUTES_END_SUFFIX, p)));
        return i;
    }

    public J visitImportAttribute(JS.ImportAttribute importAttribute, P p) {
        JS.ImportAttribute i = importAttribute;
        i = i.withPrefix(visitSpace(i.getPrefix(), JsSpace.Location.JS_IMPORT_ATTRIBUTE_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.withName(requireNonNull(visitAndCast(i.getName(), p)));
        i = i.getPadding().withValue(requireNonNull(visitLeftPadded(i.getPadding().getValue(), JsLeftPadded.Location.JS_IMPORT_ATTRIBUTE_VALUE, p)));
        return i;
    }

    public J visitLiteralType(JS.LiteralType literalType, P p) {
        JS.LiteralType type = literalType;
        type = type.withPrefix(visitSpace(type.getPrefix(), JsSpace.Location.LITERAL_TYPE_PREFIX, p));
        type = type.withMarkers(visitMarkers(type.getMarkers(), p));
        type = type.withLiteral(requireNonNull(visitAndCast(type.getLiteral(), p)));
        return type;
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
        t = t.getPadding().withValueType(requireNonNull(visitContainer(t.getPadding().getValueType(), JsContainer.Location.MAPPED_TYPE_VALUE_TYPE, p)));
        return t;
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

    public J visitMappedTypeMappedTypeParameter(JS.MappedType.MappedTypeParameter mappedTypeParameter, P p) {
        JS.MappedType.MappedTypeParameter m = mappedTypeParameter;
        m = m.withPrefix(visitSpace(m.getPrefix(), JsSpace.Location.MAPPED_TYPE_MAPPED_TYPE_PARAMETER_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Statement temp = (Statement) visitStatement(m, p);
        if (!(temp instanceof JS.MappedType.MappedTypeParameter)) {
            return temp;
        } else {
            m = (JS.MappedType.MappedTypeParameter) temp;
        }
        m = m.withName(requireNonNull(visitAndCast(m.getName(), p)));
        m = m.getPadding().withIterateType(requireNonNull(visitLeftPadded(m.getPadding().getIterateType(), JsLeftPadded.Location.MAPPED_TYPE_MAPPED_TYPE_PARAMETER_ITERATE, p)));
        return m;
    }

    public J visitObjectBindingDeclarations(JS.ObjectBindingDeclarations objectBindingDeclarations, P p) {
        JS.ObjectBindingDeclarations o = objectBindingDeclarations;
        o = o.withPrefix(visitSpace(o.getPrefix(), JsSpace.Location.OBJECT_BINDING_DECLARATIONS_PREFIX, p));
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        Expression temp = (Expression) visitExpression(o, p);
        if (!(temp instanceof JS.ObjectBindingDeclarations)) {
            return temp;
        } else {
            o = (JS.ObjectBindingDeclarations) temp;
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
        pa = pa.withInitializer(visitAndCast(pa.getInitializer(), p));
        return pa;
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
        sa = sa.getPadding().withSatisfiesType(requireNonNull(visitLeftPadded(sa.getPadding().getSatisfiesType(), JsLeftPadded.Location.SATISFIES_EXPRESSION_TYPE, p)));
        return sa;
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
        vd = vd.getPadding().withScope(visitLeftPadded(vd.getPadding().getScope(), JsLeftPadded.Location.SCOPED_VARIABLE_DECLARATIONS_SCOPE, p));
        vd = vd.getPadding().withVariables(requireNonNull(ListUtils.map(vd.getPadding().getVariables(), e -> visitRightPadded(e, JsRightPadded.Location.SCOPED_VARIABLE_DECLARATIONS_VARIABLE, p))));
        return vd;
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
        se = se.withStatement((Statement) requireNonNull(statement));
        return se;
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
        ta = ta.withTemplateExpression(requireNonNull(visitAndCast(ta.getTemplateExpression(), p)));
        return ta;
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
        te = te.withType(visitType(te.getType(), p));
        return te;
    }

    public J visitTemplateExpressionSpan(JS.TemplateExpression.Span span, P p) {
        JS.TemplateExpression.Span s = span;
        s = s.withPrefix(visitSpace(s.getPrefix(), JsSpace.Location.TEMPLATE_EXPRESSION_SPAN_PREFIX, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withExpression(requireNonNull(visitAndCast(s.getExpression(), p)));
        s = s.withTail(requireNonNull(visitAndCast(s.getTail(), p)));
        s = s.withTail(s.getTail());
        return s;
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
        t = t.withType(visitType(t.getType(), p));
        return t;
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
        t = t.withType(visitType(t.getType(), p));
        return t;
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
        t = t.getPadding().withExpression(requireNonNull(visitLeftPadded(t.getPadding().getExpression(), JsLeftPadded.Location.TYPE_OPERATOR_EXPRESSION, p)));
        return t;
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
        t = t.getPadding().withExpression(visitLeftPadded(t.getPadding().getExpression(), JsLeftPadded.Location.TYPE_PREDICATE_EXPRESSION, p));
        return t;
    }

    public J visitUnary(JS.Unary unary, P p) {
        JS.Unary u = unary;
        u = u.withPrefix(visitSpace(u.getPrefix(), Space.Location.UNARY_PREFIX, p));
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        Statement temp = (Statement) visitStatement(u, p);
        if (!(temp instanceof JS.Unary)) {
            return temp;
        } else {
            u = (JS.Unary) temp;
        }
        Expression temp2 = (Expression) visitExpression(u, p);
        if (!(temp2 instanceof JS.Unary)) {
            return temp2;
        } else {
            u = (JS.Unary) temp2;
        }
        u = u.getPadding().withOperator(requireNonNull(visitLeftPadded(u.getPadding().getOperator(), JLeftPadded.Location.UNARY_OPERATOR, p)));
        u = u.withExpression(requireNonNull(visitAndCast(u.getExpression(), p)));
        u = u.withType(visitType(u.getType(), p));
        return u;
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
        u = u.withType(visitType(u.getType(), p));
        return u;
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
        u = u.withType(visitType(u.getType(), p));
        return u;
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
        ed = ed.withAttributes(visitAndCast(ed.getAttributes(), p));
        return ed;
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
        es = es.getPadding().withExpression(requireNonNull(visitLeftPadded(es.getPadding().getExpression(), JsLeftPadded.Location.EXPORT_ASSIGNMENT_EXPRESSION, p)));
        return es;
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
        ne = ne.withType(visitType(ne.getType(), p));
        return ne;
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
        es = es.withType(visitType(es.getType(), p));
        return es;
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
        iat = iat.withType(visitType(iat.getType(), p));
        return iat;
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
        iatit = iatit.withType(visitType(iatit.getType(), p));
        return iatit;
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
        a = a.withTypeExpression(visitTypeName(a.getTypeExpression(), p));
        return a;
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
        pt = pt.withClazz(visitAndCast(pt.getClazz(), p));
        if (pt.getPadding().getTypeParameters() != null) {
            pt = pt.getPadding().withTypeParameters(visitContainer(pt.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, p));
        }
        pt = pt.getPadding().withTypeParameters(visitTypeNames(pt.getPadding().getTypeParameters(), p));
        pt = pt.withType(visitType(pt.getType(), p));
        return pt;
    }

    @Override
    public <N extends NameTree> N visitTypeName(N nameTree, P p) {
        return nameTree;
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
        t = t.withType(visitType(t.getType(), p));
        return t;
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
        t = t.withType(visitType(t.getType(), p));
        return t;
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
        v = v.withExpression(requireNonNull(visitAndCast(v.getExpression(), p)));
        return v;
    }

    public J visitYield(JS.Yield yield, P p) {
        JS.Yield y = yield;
        y = y.withPrefix(visitSpace(y.getPrefix(), JsSpace.Location.YIELD_PREFIX, p));
        y = y.withMarkers(visitMarkers(y.getMarkers(), p));
        Expression temp = (Expression) visitExpression(y, p);
        if (!(temp instanceof JS.Yield)) {
            return temp;
        } else {
            y = (JS.Yield) temp;
        }
        y = y.getPadding().withDelegated(requireNonNull(this.visitLeftPadded(y.getPadding().getDelegated(), JsLeftPadded.Location.JS_YIELD_DELEGATED, p)));
        y = y.withExpression(visitAndCast(y.getExpression(), p));
        return y;
    }

    public J visitTypeInfo(JS.TypeInfo typeInfo, P p) {
        JS.TypeInfo ti = typeInfo;
        ti = ti.withPrefix(visitSpace(ti.getPrefix(), JsSpace.Location.YIELD_PREFIX, p));
        ti = ti.withMarkers(visitMarkers(ti.getMarkers(), p));
        Expression temp = (Expression) visitExpression(ti, p);
        if (!(temp instanceof JS.TypeInfo)) {
            return temp;
        } else {
            ti = (JS.TypeInfo) temp;
        }
        ti = ti.withTypeIdentifier(requireNonNull(visitAndCast(ti.getTypeIdentifier(), p)));
        return ti;
    }

    public J visitJSVariableDeclarations(JS.JSVariableDeclarations multiVariable, P p) {
        JS.JSVariableDeclarations m = multiVariable.withPrefix(this.visitSpace(multiVariable.getPrefix(), JsSpace.Location.JSVARIABLE_DECLARATIONS_PREFIX, p));
        m = m.withMarkers(this.visitMarkers(m.getMarkers(), p));
        Statement temp = (Statement) this.visitStatement(m, p);
        if (!(temp instanceof JS.JSVariableDeclarations)) {
            return temp;
        } else {
            m = (JS.JSVariableDeclarations) temp;
            m = m.withLeadingAnnotations(requireNonNull(ListUtils.map(m.getLeadingAnnotations(), a -> this.visitAndCast(a, p))));
            m = m.withModifiers(requireNonNull(ListUtils.map(m.getModifiers(), e -> (J.Modifier) this.visitAndCast(e, p))));
            m = m.withTypeExpression(this.visitAndCast(m.getTypeExpression(), p));
            m = m.withTypeExpression(m.getTypeExpression() == null ? null : this.visitTypeName(m.getTypeExpression(), p));
            m = m.withVarargs(m.getVarargs() == null ? null : this.visitSpace(m.getVarargs(), Space.Location.VARARGS, p));
            m = m.getPadding().withVariables(requireNonNull(ListUtils.map(m.getPadding().getVariables(), t -> this.visitRightPadded(t, JsRightPadded.Location.JSNAMED_VARIABLE, p))));
            return m;
        }
    }

    public J visitJSVariableDeclarationsJSNamedVariable(JS.JSVariableDeclarations.JSNamedVariable variable, P p) {
        JS.JSVariableDeclarations.JSNamedVariable v = variable.withPrefix(this.visitSpace(variable.getPrefix(), JsSpace.Location.JSVARIABLE_PREFIX, p));
        v = v.withMarkers(this.visitMarkers(v.getMarkers(), p));
        v = v.withName(requireNonNull(this.visitAndCast(v.getName(), p)));
        v = v.withDimensionsAfterName(requireNonNull(ListUtils.map(v.getDimensionsAfterName(), dim -> requireNonNull(dim).withBefore(this.visitSpace(dim.getBefore(), Space.Location.DIMENSION_PREFIX, p)).withElement(this.visitSpace((Space) dim.getElement(), Space.Location.DIMENSION, p)))));
        if (v.getPadding().getInitializer() != null) {
            v = v.getPadding().withInitializer(this.visitLeftPadded(v.getPadding().getInitializer(), JsLeftPadded.Location.JSVARIABLE_INITIALIZER, p));
        }

        v = v.withVariableType((JavaType.Variable) this.visitType(v.getVariableType(), p));
        return v;
    }

    public J visitJSMethodDeclaration(JS.JSMethodDeclaration method, P p) {
        JS.JSMethodDeclaration m = method;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.METHOD_DECLARATION_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Statement temp = (Statement) visitStatement(m, p);
        if (!(temp instanceof JS.JSMethodDeclaration)) {
            return temp;
        } else {
            m = (JS.JSMethodDeclaration) temp;
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
        if (m.getPadding().getDefaultValue() != null) {
            m = m.getPadding().withDefaultValue(visitLeftPadded(m.getPadding().getDefaultValue(), JLeftPadded.Location.METHOD_DECLARATION_DEFAULT_VALUE, p));
        }
        m = m.withMethodType((JavaType.Method) visitType(m.getMethodType(), p));
        return m;
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
        ns = ns.withBody(visitAndCast(ns.getBody(), p));
        return ns;
    }

    public J visitFunctionDeclaration(JS.FunctionDeclaration functionDeclaration, P p) {
        JS.FunctionDeclaration f = functionDeclaration;
        f = f.withPrefix(visitSpace(f.getPrefix(), JsSpace.Location.FUNCTION_DECLARATION_PREFIX, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        Expression temp = (Expression) visitExpression(f, p);
        if (!(temp instanceof JS.FunctionDeclaration)) {
            return temp;
        } else {
            f = (JS.FunctionDeclaration) temp;
        }

        f = f.withModifiers(requireNonNull(ListUtils.map(f.getModifiers(), e -> visitAndCast(e, p))));
        f = f.getPadding().withAsteriskToken(requireNonNull(visitLeftPadded(f.getPadding().getAsteriskToken(), JsLeftPadded.Location.FUNCTION_DECLARATION_ASTERISK_TOKEN, p)));
        f = f.getPadding().withName(requireNonNull(visitLeftPadded(f.getPadding().getName(), JsLeftPadded.Location.FUNCTION_DECLARATION_NAME, p)));
        f = f.withTypeParameters(visitAndCast(f.getTypeParameters(), p));
        f = f.getPadding().withParameters(requireNonNull(visitContainer(f.getPadding().getParameters(), JsContainer.Location.FUNCTION_DECLARATION_PARAMETERS, p)));
        f = f.withReturnTypeExpression(visitAndCast(f.getReturnTypeExpression(), p));
        f = f.withBody(visitAndCast(f.getBody(), p));
        f = f.withType(visitType(f.getType(), p));
        return f;
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
        t = t.withType(visitType(t.getType(), p));
        return t;
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
        isd = isd.withType(visitType(isd.getType(), p));
        return isd;
    }

    public J visitJSForOfLoop(JS.JSForOfLoop jsForOfLoop, P p) {
        JS.JSForOfLoop f = jsForOfLoop;
        f = f.withPrefix(visitSpace(f.getPrefix(), JsSpace.Location.FOR_OF_LOOP_PREFIX, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        Statement temp = (Statement) visitStatement(f, p);
        if (!(temp instanceof JS.JSForOfLoop)) {
            return temp;
        } else {
            f = (JS.JSForOfLoop) temp;
        }
        f = f.getPadding().withAwait(requireNonNull(visitLeftPadded(f.getPadding().getAwait(), JsLeftPadded.Location.FOR_OF_AWAIT, p)));
        f = f.withControl(requireNonNull(visitAndCast(f.getControl(), p)));
        f = f.getPadding().withBody(requireNonNull(visitRightPadded(f.getPadding().getBody(), JsRightPadded.Location.FOR_BODY, p)));
        return f;
    }

    public J visitJSForInLoop(JS.JSForInLoop jsForInLoop, P p) {
        JS.JSForInLoop f = jsForInLoop;
        f = f.withPrefix(visitSpace(f.getPrefix(), JsSpace.Location.FOR_IN_LOOP_PREFIX, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        Statement temp = (Statement) visitStatement(f, p);
        if (!(temp instanceof JS.JSForInLoop)) {
            return temp;
        } else {
            f = (JS.JSForInLoop) temp;
        }
        f = f.withControl(requireNonNull(visitAndCast(f.getControl(), p)));
        f = f.getPadding().withBody(requireNonNull(visitRightPadded(f.getPadding().getBody(), JsRightPadded.Location.FOR_BODY, p)));
        return f;
    }

    public J visitJSForInOfLoopControl(JS.JSForInOfLoopControl jsForInOfLoopControl, P p) {
        JS.JSForInOfLoopControl c = jsForInOfLoopControl;
        c = c.withPrefix(visitSpace(c.getPrefix(), JsSpace.Location.FOR_LOOP_CONTROL_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.getPadding().withVariable(requireNonNull(visitRightPadded(c.getPadding().getVariable(), JsRightPadded.Location.FOR_CONTROL_VAR, p)));
        c = c.getPadding().withIterable(requireNonNull(visitRightPadded(c.getPadding().getIterable(), JsRightPadded.Location.FOR_CONTROL_ITER, p)));
        return c;
    }

    public J visitJSTry(JS.JSTry jsTry, P p) {
        JS.JSTry t = jsTry;
        t = t.withPrefix(visitSpace(t.getPrefix(), JsSpace.Location.JSTRY_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Statement temp = (Statement) visitStatement(t, p);
        if (!(temp instanceof JS.JSTry)) {
            return temp;
        } else {
            t = (JS.JSTry) temp;
        }

        t = t.withBody(requireNonNull(visitAndCast(t.getBody(), p)));
        t = t.withCatches(requireNonNull(visitAndCast(t.getCatches(), p)));
        if (t.getPadding().getFinally() != null) {
            t = t.getPadding().withFinally(requireNonNull(visitLeftPadded(t.getPadding().getFinally(), JsLeftPadded.Location.JSTRY_FINALLY, p)));
        }
        return t;
    }

    public J visitJSTryJSCatch(JS.JSTry.JSCatch jsCatch, P p) {
        JS.JSTry.JSCatch c = jsCatch;
        c = c.withPrefix(visitSpace(c.getPrefix(), JsSpace.Location.JSCATCH_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));

        c = c.withParameter(requireNonNull(visitAndCast(c.getParameter(), p)));
        c = c.withBody(requireNonNull(visitAndCast(c.getBody(), p)));
        return c;
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
        c = c.getPadding().withElements(requireNonNull(visitContainer(c.getPadding().getElements(), JsContainer.Location.ARRAY_BINDING_PATTERN_ELEMENTS, p)));
        return c;
    }

    public J visitJsAssignmentOperation(JS.JsAssignmentOperation assignOp, P p) {
        JS.JsAssignmentOperation a = assignOp;
        a = a.withPrefix(visitSpace(a.getPrefix(), JsSpace.Location.ASSIGNMENT_OPERATION_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Statement temp = (Statement) visitStatement(a, p);
        if (!(temp instanceof JS.JsAssignmentOperation)) {
            return temp;
        } else {
            a = (JS.JsAssignmentOperation) temp;
        }
        Expression temp2 = (Expression) visitExpression(a, p);
        if (!(temp2 instanceof JS.JsAssignmentOperation)) {
            return temp2;
        } else {
            a = (JS.JsAssignmentOperation) temp2;
        }
        a = a.withVariable(requireNonNull(visitAndCast(a.getVariable(), p)));
        a = a.getPadding().withOperator(requireNonNull(visitLeftPadded(a.getPadding().getOperator(), JsLeftPadded.Location.ASSIGNMENT_OPERATION_OPERATOR, p)));
        a = a.withAssignment(requireNonNull(visitAndCast(a.getAssignment(), p)));
        a = a.withType(visitType(a.getType(), p));
        return a;
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
        a = a.withType(visitType(a.getType(), p));
        return a;
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
        w = w.getPadding().withBody(requireNonNull(visitRightPadded(w.getPadding().getBody(), JsRightPadded.Location.WITH_BODY, p)));
        return w;
    }
}
