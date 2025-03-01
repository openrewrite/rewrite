/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.javascript.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.Semicolon;
import org.openrewrite.java.tree.*;
import org.openrewrite.javascript.JavaScriptVisitor;
import org.openrewrite.javascript.markers.*;
import org.openrewrite.javascript.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.function.UnaryOperator;

@SuppressWarnings("SameParameterValue")
public class JavaScriptPrinter<P> extends JavaScriptVisitor<PrintOutputCapture<P>> {

    private static final UnaryOperator<String> JAVA_SCRIPT_MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    private final JavaScriptJavaPrinter delegate = new JavaScriptJavaPrinter();

    @Override
    public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
        if (!(tree instanceof JS)) {
            // re-route printing to the java printer
            return delegate.visit(tree, p);
        } else {
            return super.visit(tree, p);
        }
    }

    @Override
    public void setCursor(@Nullable Cursor cursor) {
        super.setCursor(cursor);
        delegate.internalSetCursor(cursor);
    }

    private void internalSetCursor(@Nullable Cursor cursor) {
        super.setCursor(cursor);
    }

    @Override
    public J visitCompilationUnit(JS.CompilationUnit cu, PrintOutputCapture<P> p) {
        beforeSyntax(cu, Space.Location.COMPILATION_UNIT_PREFIX, p);

        visitRightPadded(cu.getPadding().getStatements(), JRightPadded.Location.LANGUAGE_EXTENSION, "", p);

        visitSpace(cu.getEof(), Space.Location.COMPILATION_UNIT_EOF, p);
        afterSyntax(cu, p);
        return cu;
    }

    @Override
    public J visitAlias(JS.Alias alias, PrintOutputCapture<P> p) {
        beforeSyntax(alias, JsSpace.Location.ALIAS_PREFIX, p);
        visitRightPadded(alias.getPadding().getPropertyName(), JsRightPadded.Location.ALIAS_PROPERTY_NAME, p);
        p.append("as");
        visit(alias.getAlias(), p);
        afterSyntax(alias, p);
        return alias;
    }

    @Override
    public J visitArrowFunction(JS.ArrowFunction arrowFunction, PrintOutputCapture<P> p) {
        beforeSyntax(arrowFunction, JsSpace.Location.ARROW_FUNCTION_PREFIX, p);
        visit(arrowFunction.getLeadingAnnotations(), p);
        arrowFunction.getModifiers().forEach(m -> delegate.visitModifier(m, p));

        J.TypeParameters typeParameters = arrowFunction.getTypeParameters();
        if (typeParameters != null) {
            visit(typeParameters.getAnnotations(), p);
            visitSpace(typeParameters.getPrefix(), Space.Location.TYPE_PARAMETERS, p);
            visitMarkers(typeParameters.getMarkers(), p);
            p.append("<");
            visitRightPadded(typeParameters.getPadding().getTypeParameters(), JRightPadded.Location.TYPE_PARAMETER, ",", p);
            p.append(">");
        }

        if (arrowFunction.getParameters().isParenthesized()) {
            visitSpace(arrowFunction.getParameters().getPrefix(), Space.Location.LAMBDA_PARAMETERS_PREFIX, p);
            p.append('(');
            visitRightPadded(arrowFunction.getParameters().getPadding().getParams(), JRightPadded.Location.LAMBDA_PARAM, ",", p);
            p.append(')');
        } else {
            visitRightPadded(arrowFunction.getParameters().getPadding().getParams(), JRightPadded.Location.LAMBDA_PARAM, ",", p);
        }

        if (arrowFunction.getReturnTypeExpression() != null) {
            visit(arrowFunction.getReturnTypeExpression(), p);
        }

        visitLeftPadded("=>", arrowFunction.getPadding().getBody(), JsLeftPadded.Location.LAMBDA_ARROW, p);

        afterSyntax(arrowFunction, p);
        return arrowFunction;
    }

    @Override
    public J visitAwait(JS.Await await, PrintOutputCapture<P> p) {
        beforeSyntax(await, JsSpace.Location.AWAIT_PREFIX, p);
        p.append("await");
        visit(await.getExpression(), p);
        afterSyntax(await, p);
        return await;
    }

    @Override
    public J visitBindingElement(JS.BindingElement binding, PrintOutputCapture<P> p) {
        beforeSyntax(binding, JsSpace.Location.BINDING_ELEMENT_PREFIX, p);
        if (binding.getPropertyName() != null) {
            visitRightPadded(binding.getPadding().getPropertyName(), JsRightPadded.Location.BINDING_ELEMENT_PROPERTY_NAME, p);
            p.append(":");
        }
        visit(binding.getName(), p);
        visitLeftPadded("=", binding.getPadding().getInitializer(), JsLeftPadded.Location.BINDING_ELEMENT_INITIALIZER, p);
        afterSyntax(binding, p);
        return binding;
    }

    @Override
    public J visitConditionalType(JS.ConditionalType conditionalType, PrintOutputCapture<P> p) {
        beforeSyntax(conditionalType, JsSpace.Location.CONDITIONAL_TYPE_PREFIX, p);
        visit(conditionalType.getCheckType(), p);
        visitContainer("extends", conditionalType.getPadding().getCondition(), JsContainer.Location.CONDITIONAL_TYPE_CONDITION, "", "", p);
        afterSyntax(conditionalType, p);
        return conditionalType;
    }

    @Override
    public J visitDefaultType(JS.DefaultType defaultType, PrintOutputCapture<P> p) {
        beforeSyntax(defaultType, JsSpace.Location.DEFAULT_TYPE_PREFIX, p);
        visit(defaultType.getLeft(), p);
        visitSpace(defaultType.getBeforeEquals(), Space.Location.ASSIGNMENT_OPERATION_PREFIX, p);
        p.append("=");
        visit(defaultType.getRight(), p);
        afterSyntax(defaultType, p);
        return defaultType;
    }

    @Override
    public J visitDelete(JS.Delete delete, PrintOutputCapture<P> p) {
        beforeSyntax(delete, JsSpace.Location.DELETE_PREFIX, p);
        p.append("delete");
        visit(delete.getExpression(), p);
        afterSyntax(delete, p);
        return delete;
    }

    @Override
    public J visitExpressionWithTypeArguments(JS.ExpressionWithTypeArguments type, PrintOutputCapture<P> p) {
        beforeSyntax(type, JsSpace.Location.EXPR_WITH_TYPE_ARG_PREFIX, p);
        visit(type.getClazz(), p);
        visitContainer("<", type.getPadding().getTypeArguments(), JsContainer.Location.EXPR_WITH_TYPE_ARG_PARAMETERS, ",", ">", p);
        afterSyntax(type, p);
        return type;
    }

    @Override
    public J visitTrailingTokenStatement(JS.TrailingTokenStatement statement, PrintOutputCapture<P> p) {
        beforeSyntax(statement, JsSpace.Location.TRAILING_TOKEN_PREFIX, p);
        visitRightPadded(statement.getPadding().getExpression(), JsRightPadded.Location.TRAILING_TOKEN_EXPRESSION, p);
        afterSyntax(statement, p);
        return statement;
    }

    @Override
    public J visitExport(JS.Export export, PrintOutputCapture<P> p) {
        beforeSyntax(export, JsSpace.Location.EXPORT_PREFIX, p);
        p.append("export");

        boolean printBrackets = export.getPadding().getExports() != null && export.getPadding().getExports().getMarkers().findFirst(Braces.class).isPresent();
        visitContainer(printBrackets ? "{" : "", export.getPadding().getExports(), JsContainer.Location.FUNCTION_TYPE_PARAMETERS, ",", printBrackets ? "}" : "", p);

        if (export.getFrom() != null) {
            visitSpace(export.getFrom(), Space.Location.LANGUAGE_EXTENSION, p);
            p.append("from");
        }

        visit(export.getTarget(), p);
        visitLeftPadded("default", export.getPadding().getInitializer(), JsLeftPadded.Location.IMPORT_INITIALIZER, p);
        afterSyntax(export, p);
        return export;
    }

    @Override
    public J visitFunctionType(JS.FunctionType functionType, PrintOutputCapture<P> p) {
        beforeSyntax(functionType, JsSpace.Location.FUNCTION_TYPE_PREFIX, p);
        functionType.getModifiers().forEach(m -> delegate.visitModifier(m, p));

        if (functionType.isConstructorType()) {
            visitLeftPaddedBoolean("new", functionType.getPadding().getConstructorType(), JsLeftPadded.Location.FUNCTION_TYPE_CONSTRUCTOR, p);
        }
        J.TypeParameters typeParameters = functionType.getTypeParameters();
        if (typeParameters != null) {
            visit(typeParameters.getAnnotations(), p);
            visitSpace(typeParameters.getPrefix(), Space.Location.TYPE_PARAMETERS, p);
            visitMarkers(typeParameters.getMarkers(), p);
            p.append("<");
            visitRightPadded(typeParameters.getPadding().getTypeParameters(), JRightPadded.Location.TYPE_PARAMETER, ",", p);
            p.append(">");
        }
        visitContainer("(", functionType.getPadding().getParameters(), JsContainer.Location.FUNCTION_TYPE_PARAMETERS, ",", ")", p);
        visitLeftPadded("=>", functionType.getPadding().getReturnType(), JsLeftPadded.Location.FUNCTION_TYPE_RETURN_TYPE, p);

        afterSyntax(functionType, p);
        return functionType;
    }

    @Override
    public J visitInferType(JS.InferType inferType, PrintOutputCapture<P> p) {
        beforeSyntax(inferType, JsSpace.Location.INFER_TYPE_PREFIX, p);
        visitLeftPadded("infer", inferType.getPadding().getTypeParameter(), JsLeftPadded.Location.INFER_TYPE_PARAMETER, p);
        afterSyntax(inferType, p);
        return inferType;
    }

    @Override
    public J visitImportType(JS.ImportType importType, PrintOutputCapture<P> p) {
        beforeSyntax(importType, JsSpace.Location.IMPORT_TYPE_PREFIX, p);
        if (importType.isHasTypeof()) {
            p.append("typeof");
            visitRightPadded(importType.getPadding().getHasTypeof(), JsRightPadded.Location.IMPORT_TYPE_TYPEOF, p);
        }
        p.append("import");
        visitContainer("(", importType.getPadding().getArgumentAndAttributes(), JsContainer.Location.IMPORT_TYPE_ARGUMENTS_AND_ATTRIBUTES, ",", ")", p);
        visitLeftPadded(".", importType.getPadding().getQualifier(), JsLeftPadded.Location.IMPORT_TYPE_QUALIFIER, p);
        visitContainer("<", importType.getPadding().getTypeArguments(), JsContainer.Location.IMPORT_TYPE_TYPE_ARGUMENTS, ",", ">", p);
        afterSyntax(importType, p);
        return importType;
    }

    @Override
    public J visitJsImport(JS.JsImport jsImport, PrintOutputCapture<P> p) {
        beforeSyntax(jsImport, JsSpace.Location.IMPORT_PREFIX, p);

        jsImport.getModifiers().forEach(m -> delegate.visitModifier(m, p));

        p.append("import");

        visit(jsImport.getImportClause(), p);

        visitLeftPadded(jsImport.getImportClause() == null ? "" : "from", jsImport.getPadding().getModuleSpecifier(), JsLeftPadded.Location.JS_IMPORT_MODULE_SPECIFIER, p);

        visit(jsImport.getAttributes(), p);

        afterSyntax(jsImport, p);
        return jsImport;
    }

    @Override
    public J visitJsImportClause(JS.JsImportClause jsImportClause, PrintOutputCapture<P> p) {
        beforeSyntax(jsImportClause, JsSpace.Location.JS_IMPORT_CLAUSE_PREFIX, p);

        if (jsImportClause.isTypeOnly()) {
            p.append("type");
        }

        JRightPadded<J.Identifier> name = jsImportClause.getPadding().getName();
        visitRightPadded(name, JsRightPadded.Location.JS_IMPORT_CLAUSE_NAME, p);

        if (name!= null && jsImportClause.getNamedBindings() != null) {
            p.append(",");
        }

        visit(jsImportClause.getNamedBindings(), p);

        afterSyntax(jsImportClause, p);

        return jsImportClause;
    }

    @Override
    public J visitNamedImports(JS.NamedImports namedImports, PrintOutputCapture<P> p) {
        beforeSyntax(namedImports, JsSpace.Location.NAMED_IMPORTS_PREFIX, p);
        visitContainer("{", namedImports.getPadding().getElements(), JsContainer.Location.NAMED_IMPORTS_ELEMENTS, ",", "}", p);
        afterSyntax(namedImports, p);
        return namedImports;
    }

    @Override
    public J visitJsImportSpecifier(JS.JsImportSpecifier jis, PrintOutputCapture<P> p) {
        beforeSyntax(jis, JsSpace.Location.JS_IMPORT_SPECIFIER_PREFIX, p);
        if (jis.getImportType()) {
            visitLeftPaddedBoolean("type", jis.getPadding().getImportType(), JsLeftPadded.Location.JS_IMPORT_SPECIFIER_IMPORT_TYPE, p);
        }

        visit(jis.getSpecifier(), p);

        afterSyntax(jis, p);
        return jis;
    }

    @Override
    public J visitImportAttributes(JS.ImportAttributes importAttributes, PrintOutputCapture<P> p) {
        beforeSyntax(importAttributes, JsSpace.Location.JS_IMPORT_ATTRIBUTES_PREFIX, p);

        p.append(importAttributes.getToken().name().toLowerCase());

        visitContainer("{", importAttributes.getPadding().getElements(), JsContainer.Location.JS_IMPORT_ATTRIBUTES_ELEMENTS, ",", "}", p);

        afterSyntax(importAttributes, p);
        return importAttributes;
    }

    @Override
    public J visitImportTypeAttributes(JS.ImportTypeAttributes importAttributes, PrintOutputCapture<P> p) {
        beforeSyntax(importAttributes, JsSpace.Location.JS_IMPORT_TYPE_ATTRIBUTES_PREFIX, p);
        p.append("{");

        visitRightPadded(importAttributes.getPadding().getToken(), JsRightPadded.Location.JS_IMPORT_TYPE_ATTRIBUTES_TOKEN, p);
        p.append(":");
        visitContainer("{", importAttributes.getPadding().getElements(), JsContainer.Location.JS_IMPORT_TYPE_ATTRIBUTES_ELEMENTS, ",", "}", p);
        visitSpace(importAttributes.getEnd(), JsSpace.Location.JS_IMPORT_TYPE_ATTRIBUTES_END_SUFFIX, p);

        p.append("}");
        afterSyntax(importAttributes, p);
        return importAttributes;
    }

    @Override
    public J visitImportAttribute(JS.ImportAttribute importAttribute, PrintOutputCapture<P> p) {
        beforeSyntax(importAttribute, JsSpace.Location.JS_IMPORT_ATTRIBUTE_PREFIX, p);

        visit(importAttribute.getName(), p);

        visitLeftPadded(":", importAttribute.getPadding().getValue(), JsLeftPadded.Location.JS_IMPORT_ATTRIBUTE_VALUE, p);

        afterSyntax(importAttribute, p);
        return importAttribute;
    }

    @Override
    public J visitJsBinary(JS.JsBinary binary, PrintOutputCapture<P> p) {
        beforeSyntax(binary, JsSpace.Location.BINARY_PREFIX, p);

        visit(binary.getLeft(), p);
        String keyword = "";
        switch (binary.getOperator()) {
            case As:
                keyword = "as";
                break;
            case IdentityEquals:
                keyword = "===";
                break;
            case IdentityNotEquals:
                keyword = "!==";
                break;
            case In:
                keyword = "in";
                break;
            case QuestionQuestion:
                keyword = "??";
                break;
            case Comma:
                keyword = ",";
                break;
        }

        visitSpace(binary.getPadding().getOperator().getBefore(), JsSpace.Location.BINARY_PREFIX, p);
        p.append(keyword);

        visit(binary.getRight(), p);

        afterSyntax(binary, p);
        return binary;
    }

    @Override
    public J visitJsAssignmentOperation(JS.JsAssignmentOperation assignOp, PrintOutputCapture<P> p) {
        String keyword = "";
        switch (assignOp.getOperator()) {
            case QuestionQuestion:
                keyword = "??=";
                break;
            case And:
                keyword = "&&=";
                break;
            case Or:
                keyword = "||=";
                break;
            case Power:
                keyword = "**";
                break;
            case Exp:
                keyword = "**=";
                break;
        }
        beforeSyntax(assignOp, JsSpace.Location.ASSIGNMENT_OPERATION_PREFIX, p);
        visit(assignOp.getVariable(), p);
        visitSpace(assignOp.getPadding().getOperator().getBefore(), JsSpace.Location.ASSIGNMENT_OPERATION_OPERATOR, p);
        p.append(keyword);
        visit(assignOp.getAssignment(), p);
        afterSyntax(assignOp, p);
        return assignOp;
    }

    @Override
    public J visitTypeTreeExpression(JS.TypeTreeExpression typeTreeExpression, PrintOutputCapture<P> p) {
        beforeSyntax(typeTreeExpression, JsSpace.Location.TYPE_TREE_EXPRESSION_PREFIX, p);
        visit(typeTreeExpression.getExpression(), p);
        afterSyntax(typeTreeExpression, p);
        return typeTreeExpression;
    }

    @Override
    public J visitPropertyAssignment(JS.PropertyAssignment propertyAssignment, PrintOutputCapture<P> p) {
        beforeSyntax(propertyAssignment, JsSpace.Location.PROPERTY_ASSIGNMENT_PREFIX, p);

        visitRightPadded(propertyAssignment.getPadding().getName(), JsRightPadded.Location.PROPERTY_ASSIGNMENT_NAME, p);
        if (propertyAssignment.getInitializer() != null) {
            // if property is not null, we should print it like `{ a: b }`
            // otherwise it is a shorthanded assignment where we have stuff like `{ a }` only
            if (propertyAssignment.getAssigmentToken() == JS.PropertyAssignment.AssigmentToken.Colon) {
                p.append(':');
            } else if (propertyAssignment.getAssigmentToken() == JS.PropertyAssignment.AssigmentToken.Equals) {
                p.append('=');
            }
            visit(propertyAssignment.getInitializer(), p);
        }

        afterSyntax(propertyAssignment, p);
        return propertyAssignment;
    }

    @Override
    public J visitNamespaceDeclaration(JS.NamespaceDeclaration namespaceDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(namespaceDeclaration, JsSpace.Location.NAMESPACE_DECLARATION_PREFIX, p);
        namespaceDeclaration.getModifiers().forEach(it -> delegate.visitModifier(it, p));
        visitSpace(namespaceDeclaration.getPadding().getKeywordType().getBefore(), JsSpace.Location.NAMESPACE_DECLARATION_KEYWORD_PREFIX, p);
        switch (namespaceDeclaration.getKeywordType()) {
            case Namespace:
                p.append("namespace");
                break;
            case Module:
                p.append("module");
                break;
            default:
                break;
        }
        this.visitRightPadded(namespaceDeclaration.getPadding().getName(), JsRightPadded.Location.NAMESPACE_DECLARATION_NAME, p);
        if (namespaceDeclaration.getBody() != null) {
            visit(namespaceDeclaration.getBody(), p);
        }
        afterSyntax(namespaceDeclaration, p);
        return namespaceDeclaration;
    }

    @Override
    public J visitScopedVariableDeclarations(JS.ScopedVariableDeclarations variableDeclarations, PrintOutputCapture<P> p) {
        beforeSyntax(variableDeclarations, JsSpace.Location.SCOPED_VARIABLE_DECLARATIONS_PREFIX, p);
        variableDeclarations.getModifiers().forEach(m -> delegate.visitModifier(m, p));

        JLeftPadded<JS.ScopedVariableDeclarations.Scope> scope = variableDeclarations.getPadding().getScope();
        if (scope != null) {
            visitSpace(scope.getBefore(), JsSpace.Location.SCOPED_VARIABLE_DECLARATIONS_SCOPE_PREFIX, p);
            switch (scope.getElement()) {
                case Let:
                    p.append("let");
                    break;
                case Const:
                    p.append("const");
                    break;
                case Var:
                    p.append("var");
                    break;
                case Using:
                    p.append("using");
                    break;
                case Import:
                    p.append("import");
                    break;
            }
        }

        visitRightPadded(variableDeclarations.getPadding().getVariables(), JsRightPadded.Location.SCOPED_VARIABLE_DECLARATIONS_VARIABLE, ",", p);

        afterSyntax(variableDeclarations, p);
        return variableDeclarations;
    }

    @Override
    public J visitLiteralType(JS.LiteralType literalType, PrintOutputCapture<P> p) {
        beforeSyntax(literalType, JsSpace.Location.LITERAL_TYPE_PREFIX, p);
        visit(literalType.getLiteral(), p);
        afterSyntax(literalType, p);
        return literalType;
    }

    @Override
    public J visitMappedType(JS.MappedType mappedType, PrintOutputCapture<P> p) {
        beforeSyntax(mappedType, JsSpace.Location.MAPPED_TYPE_PREFIX, p);
        p.append("{");

        if (mappedType.getPrefixToken() != null) {
            visitLeftPadded(mappedType.getPadding().getPrefixToken(), JsLeftPadded.Location.MAPPED_TYPE_PREFIX_TOKEN, p);
        }

        if (mappedType.isHasReadonly()) {
            visitLeftPaddedBoolean("readonly", mappedType.getPadding().getHasReadonly(), JsLeftPadded.Location.MAPPED_TYPE_READONLY, p);
        }

        visitMappedTypeKeysRemapping(mappedType.getKeysRemapping(), p);

        if (mappedType.getSuffixToken() != null) {
            visitLeftPadded(mappedType.getPadding().getSuffixToken(), JsLeftPadded.Location.MAPPED_TYPE_SUFFIX_TOKEN, p);
        }

        if (mappedType.isHasQuestionToken()) {
            visitLeftPaddedBoolean("?", mappedType.getPadding().getHasQuestionToken(), JsLeftPadded.Location.MAPPED_TYPE_QUESTION_TOKEN, p);
        }

        String colon = mappedType.getValueType().get(0) instanceof J.Empty ? "" : ":";
        visitContainer(colon, mappedType.getPadding().getValueType(), JsContainer.Location.MAPPED_TYPE_VALUE_TYPE, "", "", p);

        p.append("}");
        afterSyntax(mappedType, p);
        return mappedType;
    }

    @Override
    public J visitMappedTypeKeysRemapping(JS.MappedType.KeysRemapping mappedTypeKeys, PrintOutputCapture<P> p) {
        beforeSyntax(mappedTypeKeys, JsSpace.Location.MAPPED_TYPE_KEYS_REMAPPING_PREFIX, p);
        p.append("[");
        this.visitRightPadded(mappedTypeKeys.getPadding().getTypeParameter(), JsRightPadded.Location.MAPPED_TYPE_KEYS_REMAPPING_TYPE_PARAMETER, p);

        if (mappedTypeKeys.getNameType() != null) {
            p.append("as");
            this.visitRightPadded(mappedTypeKeys.getPadding().getNameType(), JsRightPadded.Location.MAPPED_TYPE_KEYS_REMAPPING_NAME_TYPE, p);
        }

        p.append("]");
        afterSyntax(mappedTypeKeys, p);
        return mappedTypeKeys;
    }

    @Override
    public J visitMappedTypeMappedTypeParameter(JS.MappedType.MappedTypeParameter mappedTypeParameter, PrintOutputCapture<P> p) {
        beforeSyntax(mappedTypeParameter, JsSpace.Location.MAPPED_TYPE_MAPPED_TYPE_PARAMETER_PREFIX, p);
        visit(mappedTypeParameter.getName(), p);
        this.visitLeftPadded("in", mappedTypeParameter.getPadding().getIterateType(), JsLeftPadded.Location.MAPPED_TYPE_MAPPED_TYPE_PARAMETER_ITERATE, p);
        afterSyntax(mappedTypeParameter, p);
        return mappedTypeParameter;
    }

    @Override
    public J visitObjectBindingDeclarations(JS.ObjectBindingDeclarations objectBindingDeclarations, PrintOutputCapture<P> p) {
        beforeSyntax(objectBindingDeclarations, Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
        visit(objectBindingDeclarations.getLeadingAnnotations(), p);
        objectBindingDeclarations.getModifiers().forEach(m -> delegate.visitModifier(m, p));

        visit(objectBindingDeclarations.getTypeExpression(), p);
        visitContainer("{", objectBindingDeclarations.getPadding().getBindings(), JsContainer.Location.BINDING_ELEMENT, ",", "}", p);
        visitLeftPadded("=", objectBindingDeclarations.getPadding().getInitializer(), JsLeftPadded.Location.BINDING_ELEMENT_INITIALIZER, p);
        afterSyntax(objectBindingDeclarations, p);
        return objectBindingDeclarations;
    }

    @Override
    public J visitSatisfiesExpression(JS.SatisfiesExpression satisfiesExpression, PrintOutputCapture<P> p) {
        beforeSyntax(satisfiesExpression, JsSpace.Location.SATISFIES_EXPRESSION_PREFIX, p);
        visit(satisfiesExpression.getExpression(), p);
        visitLeftPadded("satisfies", satisfiesExpression.getPadding().getSatisfiesType(), JsLeftPadded.Location.SATISFIES_EXPRESSION_TYPE, p);
        afterSyntax(satisfiesExpression, p);
        return satisfiesExpression;
    }

    @Override
    public J visitTaggedTemplateExpression(JS.TaggedTemplateExpression taggedTemplateExpression, PrintOutputCapture<P> p) {
        beforeSyntax(taggedTemplateExpression, JsSpace.Location.TEMPLATE_EXPRESSION_PREFIX, p);
        visitRightPadded(taggedTemplateExpression.getPadding().getTag(), JsRightPadded.Location.TEMPLATE_EXPRESSION_TAG, p);
        visitContainer("<", taggedTemplateExpression.getPadding().getTypeArguments(), JsContainer.Location.TEMPLATE_EXPRESSION_TYPE_ARG_PARAMETERS, ",", ">", p);
        visit(taggedTemplateExpression.getTemplateExpression(), p);
        afterSyntax(taggedTemplateExpression, p);
        return taggedTemplateExpression;
    }

    @Override
    public J visitTemplateExpression(JS.TemplateExpression templateExpression, PrintOutputCapture<P> p) {
        beforeSyntax(templateExpression, JsSpace.Location.TEMPLATE_EXPRESSION_PREFIX, p);
        visit(templateExpression.getHead(), p);
        visitRightPadded(templateExpression.getPadding().getTemplateSpans(), JsRightPadded.Location.TEMPLATE_EXPRESSION_TEMPLATE_SPAN, "", p);
        afterSyntax(templateExpression, p);
        return templateExpression;
    }

    @Override
    public J visitTemplateExpressionTemplateSpan(JS.TemplateExpression.TemplateSpan value, PrintOutputCapture<P> p) {
        beforeSyntax(value, JsSpace.Location.TEMPLATE_EXPRESSION_SPAN_PREFIX, p);
        visit(value.getExpression(), p);
        visit(value.getTail(), p);
        afterSyntax(value, p);
        return value;
    }

    @Override
    public J visitTuple(JS.Tuple tuple, PrintOutputCapture<P> p) {
        beforeSyntax(tuple, JsSpace.Location.TUPLE_PREFIX, p);
        visitContainer("[", tuple.getPadding().getElements(), JsContainer.Location.TUPLE_ELEMENT, ",", "]", p);
        afterSyntax(tuple, p);
        return tuple;
    }

    @Override
    public J visitTypeDeclaration(JS.TypeDeclaration typeDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(typeDeclaration, JsSpace.Location.TYPE_DECLARATION_PREFIX, p);
        typeDeclaration.getModifiers().forEach(m -> delegate.visitModifier(m, p));

        visitLeftPadded("type", typeDeclaration.getPadding().getName(), JsLeftPadded.Location.TYPE_DECLARATION_NAME, p);
        J.TypeParameters typeParameters = typeDeclaration.getTypeParameters();
        if (typeParameters != null) {
            visit(typeParameters.getAnnotations(), p);
            visitSpace(typeParameters.getPrefix(), Space.Location.TYPE_PARAMETERS, p);
            visitMarkers(typeParameters.getMarkers(), p);
            p.append("<");
            visitRightPadded(typeParameters.getPadding().getTypeParameters(), JRightPadded.Location.TYPE_PARAMETER, ",", p);
            p.append(">");
        }
        visitLeftPadded("=", typeDeclaration.getPadding().getInitializer(), JsLeftPadded.Location.TYPE_DECLARATION_INITIALIZER, p);
        afterSyntax(typeDeclaration, p);
        return typeDeclaration;
    }

    @Override
    public J visitTypeOf(JS.TypeOf typeOf, PrintOutputCapture<P> p) {
        beforeSyntax(typeOf, JsSpace.Location.TYPEOF_PREFIX, p);
        p.append("typeof");
        visit(typeOf.getExpression(), p);
        afterSyntax(typeOf, p);
        return typeOf;
    }

    @Override
    public J visitTypeQuery(JS.TypeQuery typeQuery, PrintOutputCapture<P> p) {
        beforeSyntax(typeQuery, JsSpace.Location.TYPE_QUERY_PREFIX, p);
        p.append("typeof");
        visit(typeQuery.getTypeExpression(), p);
        visitContainer("<", typeQuery.getPadding().getTypeArguments(), JsContainer.Location.TYPE_QUERY_TYPE_ARGUMENTS, ",", ">", p);
        afterSyntax(typeQuery, p);
        return typeQuery;
    }

    @Override
    public J visitTypeOperator(JS.TypeOperator typeOperator, PrintOutputCapture<P> p) {
        beforeSyntax(typeOperator, JsSpace.Location.TYPE_OPERATOR_PREFIX, p);

        String keyword = "";
        if (typeOperator.getOperator() == JS.TypeOperator.Type.ReadOnly) {
            keyword = "readonly";
        } else if (typeOperator.getOperator() == JS.TypeOperator.Type.KeyOf) {
            keyword = "keyof";
        } else if (typeOperator.getOperator() == JS.TypeOperator.Type.Unique) {
            keyword = "unique";
        }

        p.append(keyword);

        visitLeftPadded(typeOperator.getPadding().getExpression(), JsLeftPadded.Location.TYPE_OPERATOR_EXPRESSION, p);

        afterSyntax(typeOperator, p);
        return typeOperator;
    }

    @Override
    public J visitTypePredicate(JS.TypePredicate typePredicate, PrintOutputCapture<P> p) {
        beforeSyntax(typePredicate, JsSpace.Location.TYPE_PREDICATE_PREFIX, p);
        if (typePredicate.isAsserts()) {
            visitLeftPaddedBoolean("asserts", typePredicate.getPadding().getAsserts(), JsLeftPadded.Location.TYPE_PREDICATE_ASSERTS, p);
        }
        visit(typePredicate.getParameterName(), p);
        visitLeftPadded("is", typePredicate.getPadding().getExpression(), JsLeftPadded.Location.TYPE_PREDICATE_EXPRESSION, p);
        afterSyntax(typePredicate, p);
        return typePredicate;
    }

    @Override
    public J visitUnary(JS.Unary unary, PrintOutputCapture<P> p) {
        beforeSyntax(unary, Space.Location.UNARY_PREFIX, p);
        switch (unary.getOperator()) {
            case Spread:
                visitSpace(unary.getPadding().getOperator().getBefore(), Space.Location.UNARY_OPERATOR, p);
                p.append("...");
                visit(unary.getExpression(), p);
                break;
            case Optional:
                visit(unary.getExpression(), p);
                visitSpace(unary.getPadding().getOperator().getBefore(), Space.Location.UNARY_OPERATOR, p);
                p.append("?");
                break;
            case Exclamation:
                visit(unary.getExpression(), p);
                visitSpace(unary.getPadding().getOperator().getBefore(), Space.Location.UNARY_OPERATOR, p);
                p.append("!");
                break;
            case QuestionDot:
                visit(unary.getExpression(), p);
                visitSpace(unary.getPadding().getOperator().getBefore(), Space.Location.UNARY_OPERATOR, p);
                p.append("?");
                break;
            case QuestionDotWithDot:
                visit(unary.getExpression(), p);
                visitSpace(unary.getPadding().getOperator().getBefore(), Space.Location.UNARY_OPERATOR, p);
                p.append("?.");
                break;
            case Asterisk:
                p.append("*");
                visitSpace(unary.getPadding().getOperator().getBefore(), Space.Location.UNARY_OPERATOR, p);
                visit(unary.getExpression(), p);
            default:
                break;
        }
        afterSyntax(unary, p);
        return unary;
    }

    @Override
    public J visitUnion(JS.Union union, PrintOutputCapture<P> p) {
        beforeSyntax(union, JsSpace.Location.UNION_PREFIX, p);

        visitRightPadded(union.getPadding().getTypes(), JsRightPadded.Location.UNION_TYPE, "|", p);

        afterSyntax(union, p);
        return union;
    }

    @Override
    public J visitIntersection(JS.Intersection intersection, PrintOutputCapture<P> p) {
        beforeSyntax(intersection, JsSpace.Location.INTERSECTION_PREFIX, p);

        visitRightPadded(intersection.getPadding().getTypes(), JsRightPadded.Location.INTERSECTION_TYPE, "&", p);

        afterSyntax(intersection, p);
        return intersection;
    }

    @Override
    public J visitVoid(JS.Void aVoid, PrintOutputCapture<P> p) {
        beforeSyntax(aVoid, JsSpace.Location.VOID_PREFIX, p);
        p.append("void");
        visit(aVoid.getExpression(), p);
        afterSyntax(aVoid, p);
        return aVoid;
    }

    @Override
    public J visitYield(JS.Yield yield, PrintOutputCapture<P> p) {
        beforeSyntax(yield, JsSpace.Location.YIELD_PREFIX, p);

        p.append("yield");

        if (yield.isDelegated()) {
            visitLeftPaddedBoolean("*", yield.getPadding().getDelegated(), JsLeftPadded.Location.JS_YIELD_DELEGATED, p);
        }

        visit(yield.getExpression(), p);

        afterSyntax(yield, p);
        return yield;
    }

    @Override
    public J visitTypeInfo(JS.TypeInfo typeInfo, PrintOutputCapture<P> p) {
        beforeSyntax(typeInfo, JsSpace.Location.TYPE_INFO_PREFIX, p);

        p.append(":");

        visit(typeInfo.getTypeIdentifier(), p);

        afterSyntax(typeInfo, p);

        return typeInfo;
    }

    @Override
    public J visitJSVariableDeclarations(JS.JSVariableDeclarations multiVariable, PrintOutputCapture<P> p) {
        beforeSyntax(multiVariable, JsSpace.Location.JSVARIABLE_DECLARATIONS_PREFIX, p);
        visit(multiVariable.getLeadingAnnotations(), p);
        multiVariable.getModifiers().forEach(it -> delegate.visitModifier(it, p));

        List<JRightPadded<JS.JSVariableDeclarations.JSNamedVariable>> variables = multiVariable.getPadding().getVariables();
        for (int i = 0; i < variables.size(); i++) {
            JRightPadded<JS.JSVariableDeclarations.JSNamedVariable> variable = variables.get(i);
            beforeSyntax(variable.getElement(), JsSpace.Location.JSVARIABLE_PREFIX, p);
            if (multiVariable.getVarargs() != null) {
                p.append("...");
            }

            visit(variable.getElement().getName(), p);

            visitSpace(variable.getAfter(), JsSpace.Location.JSNAMED_VARIABLE_SUFFIX, p);
            if (multiVariable.getTypeExpression() != null) {
                visit(multiVariable.getTypeExpression(), p);
            }

            if (variable.getElement().getInitializer() != null) {
                JavaScriptPrinter.this.visitLeftPadded("=",
                        variable.getElement().getPadding().getInitializer(), JsLeftPadded.Location.JSVARIABLE_INITIALIZER, p);
            }

            afterSyntax(variable.getElement(), p);
            if (i < variables.size() - 1) {
                p.append(",");
            } else if (variable.getMarkers().findFirst(Semicolon.class).isPresent()) {
                p.append(";");
            }
        }

        afterSyntax(multiVariable, p);
        return multiVariable;
    }

    @Override
    public J visitJSVariableDeclarationsJSNamedVariable(JS.JSVariableDeclarations.JSNamedVariable variable, PrintOutputCapture<P> p) {
        beforeSyntax(variable, JsSpace.Location.JSVARIABLE_PREFIX, p);
        visit(variable.getName(), p);
        JLeftPadded<Expression> initializer = variable.getPadding().getInitializer();
        visitLeftPadded("=", initializer, JsLeftPadded.Location.JSVARIABLE_INITIALIZER, p);
        afterSyntax(variable, p);
        return variable;
    }

    @Override
    public J visitJSMethodDeclaration(JS.JSMethodDeclaration method, PrintOutputCapture<P> p) {
        beforeSyntax(method, JsSpace.Location.JSMETHOD_DECLARATION_PREFIX, p);
        visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
        visit(method.getLeadingAnnotations(), p);
        method.getModifiers().forEach(it -> delegate.visitModifier(it, p));

        visit(method.getName(), p);

        J.TypeParameters typeParameters = method.getTypeParameters();
        if (typeParameters != null) {
            visit(typeParameters.getAnnotations(), p);
            visitSpace(typeParameters.getPrefix(), Space.Location.TYPE_PARAMETERS, p);
            visitMarkers(typeParameters.getMarkers(), p);
            p.append("<");
            visitRightPadded(typeParameters.getPadding().getTypeParameters(), JRightPadded.Location.TYPE_PARAMETER, ",", p);
            p.append(">");
        }

        visitContainer("(", method.getPadding().getParameters(), JsContainer.Location.JSMETHOD_DECLARATION_PARAMETERS, ",", ")", p);
        if (method.getReturnTypeExpression() != null) {
            visit(method.getReturnTypeExpression(), p);
        }

        visit(method.getBody(), p);
        afterSyntax(method, p);
        return method;
    }

    @Override
    public J visitFunctionDeclaration(JS.FunctionDeclaration functionDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(functionDeclaration, JsSpace.Location.FUNCTION_DECLARATION_PREFIX, p);
        functionDeclaration.getModifiers().forEach(m -> delegate.visitModifier(m, p));

        visitLeftPaddedBoolean("function", functionDeclaration.getPadding().getAsteriskToken(), JsLeftPadded.Location.FUNCTION_DECLARATION_ASTERISK_TOKEN, p);

        visitLeftPadded(functionDeclaration.hasAsteriskToken() ? "*" : "", functionDeclaration.getPadding().getName(), JsLeftPadded.Location.FUNCTION_DECLARATION_NAME, p);

        J.TypeParameters typeParameters = functionDeclaration.getTypeParameters();
        if (typeParameters != null) {
            visit(typeParameters.getAnnotations(), p);
            visitSpace(typeParameters.getPrefix(), Space.Location.TYPE_PARAMETERS, p);
            visitMarkers(typeParameters.getMarkers(), p);
            p.append("<");
            visitRightPadded(typeParameters.getPadding().getTypeParameters(), JRightPadded.Location.TYPE_PARAMETER, ",", p);
            p.append(">");
        }

        visitContainer("(", functionDeclaration.getPadding().getParameters(), JsContainer.Location.FUNCTION_DECLARATION_PARAMETERS, ",", ")", p);
        if (functionDeclaration.getReturnTypeExpression() != null) {
            visit(functionDeclaration.getReturnTypeExpression(), p);
        }

        if (functionDeclaration.getBody() != null) {
            visit(functionDeclaration.getBody(), p);
        }

        afterSyntax(functionDeclaration, p);
        return functionDeclaration;
    }

    @Override
    public J visitTypeLiteral(JS.TypeLiteral tl, PrintOutputCapture<P> p) {
        beforeSyntax(tl, JsSpace.Location.TYPE_LITERAL_PREFIX, p);

        visit(tl.getMembers(), p);

        afterSyntax(tl, p);
        return tl;
    }

    @Override
    public J visitIndexSignatureDeclaration(JS.IndexSignatureDeclaration isd, PrintOutputCapture<P> p) {
        beforeSyntax(isd, JsSpace.Location.INDEXED_SIGNATURE_DECLARATION_PREFIX, p);

        isd.getModifiers().forEach(m -> delegate.visitModifier(m, p));
        visitContainer("[", isd.getPadding().getParameters(), JsContainer.Location.INDEXED_SIGNATURE_DECLARATION_PARAMETERS, "", "]", p);
        visitLeftPadded(":", isd.getPadding().getTypeExpression(), JsLeftPadded.Location.INDEXED_SIGNATURE_DECLARATION_TYPE_EXPRESSION, p);

        afterSyntax(isd, p);
        return isd;
    }



    @Override
    public J visitJSForOfLoop(JS.JSForOfLoop loop, PrintOutputCapture<P> p) {
        beforeSyntax(loop, JsSpace.Location.FOR_OF_LOOP_PREFIX, p);
        p.append("for");
        if (loop.isAwait()) {
            visitLeftPaddedBoolean("await", loop.getPadding().getAwait(), JsLeftPadded.Location.FOR_OF_AWAIT, p);
        }

        JS.JSForInOfLoopControl control = loop.getControl();
        visitSpace(control.getPrefix(), JsSpace.Location.FOR_LOOP_CONTROL_PREFIX, p);
        p.append('(');
        visitRightPadded(control.getPadding().getVariable(), JsRightPadded.Location.FOR_CONTROL_VAR, p);
        p.append("of");
        visitRightPadded(control.getPadding().getIterable(), JsRightPadded.Location.FOR_CONTROL_ITER, p);
        p.append(')');
        visitRightPadded(loop.getPadding().getBody(), JsRightPadded.Location.FOR_BODY, p);
        afterSyntax(loop, p);
        return loop;
    }

    @Override
    public J visitJSForInLoop(JS.JSForInLoop loop, PrintOutputCapture<P> p) {
        beforeSyntax(loop, JsSpace.Location.FOR_IN_LOOP_PREFIX, p);
        p.append("for");

        JS.JSForInOfLoopControl control = loop.getControl();
        visitSpace(control.getPrefix(), JsSpace.Location.FOR_LOOP_CONTROL_PREFIX, p);
        p.append('(');
        visitRightPadded(control.getPadding().getVariable(), JsRightPadded.Location.FOR_CONTROL_VAR, p);
        p.append("in");
        visitRightPadded(control.getPadding().getIterable(), JsRightPadded.Location.FOR_CONTROL_ITER, p);
        p.append(')');
        visitRightPadded(loop.getPadding().getBody(), JsRightPadded.Location.FOR_BODY, p);
        afterSyntax(loop, p);
        return loop;
    }

    @Override
    public J visitJSTry(JS.JSTry jsTry, PrintOutputCapture<P> p) {
        beforeSyntax(jsTry, JsSpace.Location.JSTRY_PREFIX, p);
        p.append("try");
        visit(jsTry.getBody(), p);
        visit(jsTry.getCatches(), p);
        visitLeftPadded("finally", jsTry.getPadding().getFinallie(), JsLeftPadded.Location.JSTRY_FINALLY, p);
        afterSyntax(jsTry, p);
        return jsTry;
    }

    @Override
    public J visitJSTryJSCatch(JS.JSTry.JSCatch jsCatch, PrintOutputCapture<P> p) {
        beforeSyntax(jsCatch, JsSpace.Location.JSCATCH_PREFIX, p);
        p.append("catch");
        visit(jsCatch.getParameter(), p);
        visit(jsCatch.getBody(), p);
        afterSyntax(jsCatch, p);
        return jsCatch;
    }

    @Override
    public J visitArrayBindingPattern(JS.ArrayBindingPattern abp, PrintOutputCapture<P> p) {
        beforeSyntax(abp, JsSpace.Location.ARRAY_BINDING_PATTERN_PREFIX, p);

        visitContainer("[", abp.getPadding().getElements(), JsContainer.Location.ARRAY_BINDING_PATTERN_ELEMENTS, "," , "]", p);

        afterSyntax(abp, p);
        return abp;
    }

    @Override
    public J visitExportDeclaration(JS.ExportDeclaration ed, PrintOutputCapture<P> p) {
        beforeSyntax(ed, JsSpace.Location.EXPORT_DECLARATION_PREFIX, p);
        p.append("export");
        ed.getModifiers().forEach(it -> delegate.visitModifier(it, p));
        if (ed.isTypeOnly()) {
            visitLeftPaddedBoolean("type", ed.getPadding().getTypeOnly(), JsLeftPadded.Location.EXPORT_DECLARATION_TYPE_ONLY, p);
        }
        visit(ed.getExportClause(), p);
        visitLeftPadded("from", ed.getPadding().getModuleSpecifier(), JsLeftPadded.Location.EXPORT_DECLARATION_MODULE_SPECIFIER, p);
        visit(ed.getAttributes(), p);
        afterSyntax(ed, p);
        return ed;
    }

    @Override
    public J visitExportAssignment(JS.ExportAssignment es, PrintOutputCapture<P> p) {
        beforeSyntax(es, JsSpace.Location.EXPORT_ASSIGNMENT_PREFIX, p);
        p.append("export");
        es.getModifiers().forEach(it -> delegate.visitModifier(it, p));
        if (es.isExportEquals()) {
            visitLeftPaddedBoolean("=", es.getPadding().getExportEquals(), JsLeftPadded.Location.EXPORT_ASSIGNMENT_EXPORT_EQUALS, p);
        }
        visit(es.getExpression(), p);
        afterSyntax(es, p);
        return es;
    }

    @Override
    public J visitNamedExports(JS.NamedExports ne, PrintOutputCapture<P> p) {
        beforeSyntax(ne, JsSpace.Location.NAMED_EXPORTS_PREFIX, p);
        visitContainer("{", ne.getPadding().getElements(), JsContainer.Location.NAMED_EXPORTS_ELEMENTS, ",", "}", p);
        afterSyntax(ne, p);
        return ne;
    }

    @Override
    public J visitExportSpecifier(JS.ExportSpecifier es, PrintOutputCapture<P> p) {
        beforeSyntax(es, JsSpace.Location.EXPORT_SPECIFIER_PREFIX, p);
        if (es.isTypeOnly()) {
            visitLeftPaddedBoolean("type", es.getPadding().getTypeOnly(), JsLeftPadded.Location.EXPORT_SPECIFIER_TYPE_ONLY, p);
        }

        visit(es.getSpecifier(), p);

        afterSyntax(es, p);
        return es;
    }

    @Override
    public J visitIndexedAccessType(JS.IndexedAccessType iat, PrintOutputCapture<P> p) {
        beforeSyntax(iat, JsSpace.Location.INDEXED_ACCESS_TYPE_PREFIX, p);

        visit(iat.getObjectType(), p);
        // expect that this element is printed accordingly
        // <space_before>[<inner_space_before>index<inner_right_padded_suffix_space>]<right_padded_suffix_space>
        visit(iat.getIndexType(), p);

        afterSyntax(iat, p);

        return iat;
    }

    @Override
    public J visitIndexedAccessTypeIndexType(JS.IndexedAccessType.IndexType iatit, PrintOutputCapture<P> p) {
        beforeSyntax(iatit, JsSpace.Location.INDEXED_ACCESS_TYPE_INDEX_TYPE_PREFIX, p);

        p.append("[");

        visitRightPadded(iatit.getPadding().getElement(), JsRightPadded.Location.INDEXED_ACCESS_TYPE_INDEX_TYPE_ELEMENT, p);

        p.append("]");

        afterSyntax(iatit, p);

        return iatit;
    }

    @Override
    public J visitWithStatement(JS.WithStatement withStatement, PrintOutputCapture<P> p) {
        beforeSyntax(withStatement, JsSpace.Location.WITH_PREFIX, p);
        p.append("with");
        visit(withStatement.getExpression(), p);
        visitRightPadded(withStatement.getPadding().getBody(), JsRightPadded.Location.WITH_BODY, p);
        afterSyntax(withStatement, p);
        return withStatement;
    }

    private class JavaScriptJavaPrinter extends JavaPrinter<P> {

        @Override
        public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
            if (tree instanceof JS) {
                // re-route printing back up to javascript
                return JavaScriptPrinter.this.visit(tree, p);
            } else {
                return super.visit(tree, p);
            }
        }

        @Override
        public void setCursor(@Nullable Cursor cursor) {
            super.setCursor(cursor);
            JavaScriptPrinter.this.internalSetCursor(cursor);
        }

        public void internalSetCursor(@Nullable Cursor cursor) {
            super.setCursor(cursor);
        }

        @Override
        public J visitEnumValue(J.EnumValue enum_, PrintOutputCapture<P> p) {
            beforeSyntax(enum_, Space.Location.ENUM_VALUE_PREFIX, p);
            visit(enum_.getName(), p);

            J.NewClass initializer = enum_.getInitializer();
            if (initializer != null) {
                visitSpace(initializer.getPrefix(), Space.Location.NEW_CLASS_PREFIX, p);
                p.append("=");
                // there can be only one argument
                Expression expression = initializer.getArguments().get(0);
                visit(expression, p);
                return enum_;
            }

            afterSyntax(enum_, p);
            return enum_;
        }

        @Override
        public J visitEnumValueSet(J.EnumValueSet enums, PrintOutputCapture<P> p) {
            beforeSyntax(enums, Space.Location.ENUM_VALUE_SET_PREFIX, p);
            visitRightPadded(enums.getPadding().getEnums(), JRightPadded.Location.ENUM_VALUE, ",", p);
            if (enums.isTerminatedWithSemicolon()) {
                p.append(',');
            }
            afterSyntax(enums, p);
            return enums;
        }

        @Override
        public J visitAnnotation(J.Annotation annotation, PrintOutputCapture<P> p) {
            beforeSyntax(annotation, Space.Location.ANNOTATION_PREFIX, p);
            if (!annotation.getMarkers().findFirst(Keyword.class).isPresent()) {
                p.append("@");
            }
            visit(annotation.getAnnotationType(), p);
            visitContainer("(", annotation.getPadding().getArguments(), JContainer.Location.ANNOTATION_ARGUMENTS, ",", ")", p);
            afterSyntax(annotation, p);
            return annotation;
        }

        @Override
        public J visitBinary(J.Binary binary, PrintOutputCapture<P> p) {
            String keyword = "";
            switch (binary.getOperator()) {
                case Addition:
                    keyword = "+";
                    break;
                case Subtraction:
                    keyword = "-";
                    break;
                case Multiplication:
                    keyword = "*";
                    break;
                case Division:
                    keyword = "/";
                    break;
                case Modulo:
                    keyword = "%";
                    break;
                case LessThan:
                    keyword = "<";
                    break;
                case GreaterThan:
                    keyword = ">";
                    break;
                case LessThanOrEqual:
                    keyword = "<=";
                    break;
                case GreaterThanOrEqual:
                    keyword = ">=";
                    break;
                case Equal:
                    keyword = "==";
                    break;
                case NotEqual:
                    keyword = "!=";
                    break;
                case BitAnd:
                    keyword = "&";
                    break;
                case BitOr:
                    keyword = "|";
                    break;
                case BitXor:
                    keyword = "^";
                    break;
                case LeftShift:
                    keyword = "<<";
                    break;
                case RightShift:
                    keyword = ">>";
                    break;
                case UnsignedRightShift:
                    keyword = ">>>";
                    break;
                case Or:
                    keyword = binary.getMarkers().findFirst(Comma.class).isPresent() ? "," : "||";
                    break;
                case And:
                    keyword = "&&";
                    break;
            }
            beforeSyntax(binary, Space.Location.BINARY_PREFIX, p);
            visit(binary.getLeft(), p);
            visitSpace(binary.getPadding().getOperator().getBefore(), Space.Location.BINARY_OPERATOR, p);
            p.append(keyword);
            visit(binary.getRight(), p);
            afterSyntax(binary, p);
            return binary;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, PrintOutputCapture<P> p) {
            beforeSyntax(fieldAccess, Space.Location.FIELD_ACCESS_PREFIX, p);
            visit(fieldAccess.getTarget(), p);
            PostFixOperator postFixOperator = fieldAccess.getMarkers().findFirst(PostFixOperator.class).orElse(null);

            visitLeftPadded(postFixOperator != null ? "?." : ".", fieldAccess.getPadding().getName(), JLeftPadded.Location.FIELD_ACCESS_NAME, p);
            afterSyntax(fieldAccess, p);
            return fieldAccess;
        }

        @Override
        public J visitCatch(J.Try.Catch catch_, PrintOutputCapture<P> p) {
            beforeSyntax(catch_, Space.Location.CATCH_PREFIX, p);
            p.append("catch");
            if (!catch_.getParameter().getTree().getVariables().isEmpty()) {
                visit(catch_.getParameter(), p);
            }
            visit(catch_.getBody(), p);
            afterSyntax(catch_, p);
            return catch_;
        }

        @Override
        public J visitImport(J.Import import_, PrintOutputCapture<P> p) {
            beforeSyntax(import_, Space.Location.IMPORT_PREFIX, p);
            p.append("import");
            visit(import_.getAlias(), p);
            visitSpace(import_.getQualid().getPrefix(), Space.Location.LANGUAGE_EXTENSION, p);
            p.append("from");
            visitLeftPadded(import_.getQualid().getPadding().getName(), JLeftPadded.Location.LANGUAGE_EXTENSION, p);
            afterSyntax(import_, p);
            return import_;
        }

        @Override
        public J visitMethodDeclaration(J.MethodDeclaration method, PrintOutputCapture<P> p) {
            beforeSyntax(method, Space.Location.METHOD_DECLARATION_PREFIX, p);
            visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
            visit(method.getLeadingAnnotations(), p);
            method.getModifiers().forEach(it -> delegate.visitModifier(it, p));

            visit(method.getName(), p);

            J.TypeParameters typeParameters = method.getAnnotations().getTypeParameters();
            if (typeParameters != null) {
                visit(typeParameters.getAnnotations(), p);
                visitSpace(typeParameters.getPrefix(), Space.Location.TYPE_PARAMETERS, p);
                visitMarkers(typeParameters.getMarkers(), p);
                p.append("<");
                visitRightPadded(typeParameters.getPadding().getTypeParameters(), JRightPadded.Location.TYPE_PARAMETER, ",", p);
                p.append(">");
            }

            visitContainer("(", method.getPadding().getParameters(), JContainer.Location.METHOD_DECLARATION_PARAMETERS, ",", ")", p);
            if (method.getReturnTypeExpression() != null) {
                visit(method.getReturnTypeExpression(), p);
            }

            visit(method.getBody(), p);
            afterSyntax(method, p);
            return method;
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<P> p) {
            beforeSyntax(method, Space.Location.METHOD_INVOCATION_PREFIX, p);
            if (method.getName().toString().isEmpty()) {
                visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, p);
            } else {
                visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT, ".", p);
                visit(method.getName(), p);
            }
            visitContainer("<", method.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
            visitContainer("(", method.getPadding().getArguments(), JContainer.Location.METHOD_INVOCATION_ARGUMENTS, ",", ")", p);
            afterSyntax(method, p);
            return method;
        }

        @Override
        public J visitModifier(J.Modifier mod, PrintOutputCapture<P> p) {
            visit(mod.getAnnotations(), p);
            String keyword;
            switch (mod.getType()) {
                case Default:
                    keyword = "default";
                    break;
                case Public:
                    keyword = "public";
                    break;
                case Protected:
                    keyword = "protected";
                    break;
                case Private:
                    keyword = "private";
                    break;
                case Abstract:
                    keyword = "abstract";
                    break;
                case Async:
                    keyword = "async";
                    break;
                case Static:
                    keyword = "static";
                    break;
                case Final:
                    keyword = "final";
                    break;
                case Native:
                    keyword = "native";
                    break;
                case NonSealed:
                    keyword = "non-sealed";
                    break;
                case Sealed:
                    keyword = "sealed";
                    break;
                case Strictfp:
                    keyword = "strictfp";
                    break;
                case Synchronized:
                    keyword = "synchronized";
                    break;
                case Transient:
                    keyword = "transient";
                    break;
                case Volatile:
                    keyword = "volatile";
                    break;
                default:
                    keyword = mod.getKeyword();
            }
            beforeSyntax(mod, Space.Location.MODIFIER_PREFIX, p);
            p.append(keyword);
            afterSyntax(mod, p);
            return mod;
        }

        @Override
        public J visitNewArray(J.NewArray newArray, PrintOutputCapture<P> p) {
            beforeSyntax(newArray, Space.Location.NEW_ARRAY_PREFIX, p);
            visit(newArray.getTypeExpression(), p);
            visit(newArray.getDimensions(), p);
            visitContainer("[", newArray.getPadding().getInitializer(), JContainer.Location.NEW_ARRAY_INITIALIZER, ",", "]", p);
            afterSyntax(newArray, p);
            return newArray;
        }

        @Override
        public J visitNewClass(J.NewClass newClass, PrintOutputCapture<P> p) {
            beforeSyntax(newClass, Space.Location.NEW_CLASS_PREFIX, p);
            visitRightPadded(newClass.getPadding().getEnclosing(), JRightPadded.Location.NEW_CLASS_ENCLOSING, ".", p);
            visitSpace(newClass.getNew(), Space.Location.NEW_PREFIX, p);
            if (newClass.getClazz() != null) {
                p.append("new");
                visit(newClass.getClazz(), p);
                if (!newClass.getPadding().getArguments().getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                    visitContainer("(", newClass.getPadding().getArguments(), JContainer.Location.NEW_CLASS_ARGUMENTS, ",", ")", p);
                }
            }
            visit(newClass.getBody(), p);
            afterSyntax(newClass, p);
            return newClass;
        }

        @Override
        public <T extends J> J visitControlParentheses(J.ControlParentheses<T> controlParens, PrintOutputCapture<P> p) {
            beforeSyntax(controlParens, Space.Location.CONTROL_PARENTHESES_PREFIX, p);
            if (getCursor().getParentTreeCursor().getValue() instanceof J.TypeCast) {
                p.append('<');
                visitRightPadded(controlParens.getPadding().getTree(), JRightPadded.Location.PARENTHESES, ">", p);
            } else {
                p.append('(');
                visitRightPadded(controlParens.getPadding().getTree(), JRightPadded.Location.PARENTHESES, ")", p);
            }
            afterSyntax(controlParens, p);
            return controlParens;
        }

        @Override
        protected void visitStatements(List<JRightPadded<Statement>> statements, JRightPadded.Location location, PrintOutputCapture<P> p) {
            boolean objectLiteral = getCursor().getParent(0).getValue() instanceof J.Block &&
                                    getCursor().getParent(1).getValue() instanceof J.NewClass;
            for (int i = 0; i < statements.size(); i++) {
                JRightPadded<Statement> paddedStat = statements.get(i);
                visitStatement(paddedStat, location, p);
                if (i < statements.size() - 1 && objectLiteral) {
                    p.append(',');
                }
            }
        }

        @Override
        public J visitTypeCast(J.TypeCast typeCast, PrintOutputCapture<P> p) {
            beforeSyntax(typeCast, Space.Location.TYPE_CAST_PREFIX, p);

            visit(typeCast.getClazz(), p);
            visit(typeCast.getExpression(), p);

            afterSyntax(typeCast, p);
            return typeCast;
        }

        @Override
        public J visitTypeParameter(J.TypeParameter typeParameter, PrintOutputCapture<P> p) {
            beforeSyntax(typeParameter, Space.Location.TYPE_PARAMETERS_PREFIX, p);
            visit(typeParameter.getAnnotations(), p);
            typeParameter.getModifiers().forEach(m -> delegate.visitModifier(m, p));
            visit(typeParameter.getName(), p);

            JContainer<TypeTree> bounds = typeParameter.getPadding().getBounds();
            if (bounds != null) {
                visitSpace(bounds.getBefore(), JContainer.Location.TYPE_BOUNDS.getBeforeLocation(), p);
                JRightPadded<TypeTree> constraintType = bounds.getPadding().getElements().get(0);

                if (!(constraintType.getElement() instanceof J.Empty)) {
                    p.append("extends");
                    this.visitRightPadded(constraintType, JContainer.Location.TYPE_BOUNDS.getElementLocation(), p);
                }

                JRightPadded<TypeTree> defaultType = bounds.getPadding().getElements().get(1);
                if (!(defaultType.getElement() instanceof J.Empty)) {
                    p.append("=");
                    this.visitRightPadded(defaultType, JContainer.Location.TYPE_BOUNDS.getElementLocation(), p);
                }
            }

            afterSyntax(typeParameter, p);
            return typeParameter;
        }

        @Override
        public J visitVariableDeclarations(J.VariableDeclarations multiVariable, PrintOutputCapture<P> p) {
            beforeSyntax(multiVariable, Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
            visit(multiVariable.getLeadingAnnotations(), p);
            multiVariable.getModifiers().forEach(it -> delegate.visitModifier(it, p));

            List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = multiVariable.getPadding().getVariables();
            for (int i = 0; i < variables.size(); i++) {
                JRightPadded<J.VariableDeclarations.NamedVariable> variable = variables.get(i);
                beforeSyntax(variable.getElement(), Space.Location.VARIABLE_PREFIX, p);
                if (multiVariable.getVarargs() != null) {
                    p.append("...");
                }

                visit(variable.getElement().getName(), p);

                visitSpace(variable.getAfter(), Space.Location.NAMED_VARIABLE_SUFFIX, p);
                if (multiVariable.getTypeExpression() != null) {
                    visit(multiVariable.getTypeExpression(), p);
                }

                if (variable.getElement().getInitializer() != null) {
                    JavaScriptPrinter.this.visitLeftPadded("=",
                            variable.getElement().getPadding().getInitializer(), JLeftPadded.Location.VARIABLE_INITIALIZER, p);
                }

                afterSyntax(variable.getElement(), p);
                if (i < variables.size() - 1) {
                    p.append(",");
                } else if (variable.getMarkers().findFirst(Semicolon.class).isPresent()) {
                    p.append(";");
                }
            }

            afterSyntax(multiVariable, p);
            return multiVariable;
        }

        @Override
        public J visitVariable(J.VariableDeclarations.NamedVariable variable, PrintOutputCapture<P> p) {
            beforeSyntax(variable, Space.Location.VARIABLE_PREFIX, p);
            visit(variable.getName(), p);
            JLeftPadded<Expression> initializer = variable.getPadding().getInitializer();
            visitLeftPadded("=", initializer, JLeftPadded.Location.VARIABLE_INITIALIZER, p);
            afterSyntax(variable, p);
            return variable;
        }

        protected void visitStatement(@Nullable JRightPadded<Statement> paddedStat, JRightPadded.Location location, PrintOutputCapture<P> p) {
            if (paddedStat != null) {
                visit(paddedStat.getElement(), p);
                visitSpace(paddedStat.getAfter(), location.getAfterLocation(), p);
                visitMarkers(paddedStat.getMarkers(), p);
            }
        }

        @Override
        public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
            if (marker instanceof Semicolon) {
                p.append(';');
            }
            return super.visitMarker(marker, p);
        }
    }

    protected void beforeSyntax(J j, JsSpace.Location loc, PrintOutputCapture<P> p) {
        beforeSyntax(j.getPrefix(), j.getMarkers(), loc, p);
    }

    private void beforeSyntax(Space prefix, Markers markers, JsSpace.@Nullable Location loc, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), JAVA_SCRIPT_MARKER_WRAPPER));
        }
        if (loc != null) {
            visitSpace(prefix, loc, p);
        }
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), JAVA_SCRIPT_MARKER_WRAPPER));
        }
    }

    protected void beforeSyntax(J j, Space.Location loc, PrintOutputCapture<P> p) {
        beforeSyntax(j.getPrefix(), j.getMarkers(), loc, p);
    }

    protected void beforeSyntax(Space prefix, Markers markers, Space.@Nullable Location loc, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.out.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), JAVA_SCRIPT_MARKER_WRAPPER));
        }
        if (loc != null) {
            visitSpace(prefix, loc, p);
        }
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.out.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), JAVA_SCRIPT_MARKER_WRAPPER));
        }
    }

    protected void afterSyntax(J j, PrintOutputCapture<P> p) {
        afterSyntax(j.getMarkers(), p);
    }

    protected void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.out.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), JAVA_SCRIPT_MARKER_WRAPPER));
        }
    }

    protected void visitContainer(String before, @Nullable JContainer<? extends J> container, JsContainer.Location location,
                                  String suffixBetween, @Nullable String after, PrintOutputCapture<P> p) {
        if (container == null) {
            return;
        }
        visitSpace(container.getBefore(), location.getBeforeLocation(), p);
        p.append(before);
        visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), suffixBetween, p);
        p.append(after == null ? "" : after);
    }

    protected void visitRightPadded(JRightPadded<? extends J> node, JsRightPadded.Location location, String suffixBetween, PrintOutputCapture<P> p) {
        visit(node.getElement(), p);
        p.append(suffixBetween);
        visitSpace(node.getAfter(), location.getAfterLocation(), p);
        afterSyntax(node.getMarkers(), p);
    }

    protected void visitLeftPadded(@Nullable String prefix, @Nullable JLeftPadded<? extends J> leftPadded, JsLeftPadded.Location location, PrintOutputCapture<P> p) {
        if (leftPadded != null) {
            beforeSyntax(leftPadded.getBefore(), leftPadded.getMarkers(), location.getBeforeLocation(), p);
            if (prefix != null) {
                p.append(prefix);
            }
            visit(leftPadded.getElement(), p);
            afterSyntax(leftPadded.getMarkers(), p);
        }
    }

    protected void visitLeftPadded(@Nullable String prefix, @Nullable JLeftPadded<? extends J> leftPadded, JLeftPadded.Location location, PrintOutputCapture<P> p) {
        if (leftPadded != null) {
            beforeSyntax(leftPadded.getBefore(), leftPadded.getMarkers(), location.getBeforeLocation(), p);
            if (prefix != null) {
                p.append(prefix);
            }
            visit(leftPadded.getElement(), p);
            afterSyntax(leftPadded.getMarkers(), p);
        }
    }

    protected void visitLeftPaddedBoolean(@Nullable String prefix, @Nullable JLeftPadded<Boolean> leftPadded, JsLeftPadded.Location location, PrintOutputCapture<P> p) {
        if (leftPadded != null) {
            beforeSyntax(leftPadded.getBefore(), leftPadded.getMarkers(), location.getBeforeLocation(), p);
            if (prefix != null) {
                p.append(prefix);
            }
            afterSyntax(leftPadded.getMarkers(), p);
        }
    }

    protected void visitRightPadded(List<? extends JRightPadded<? extends J>> nodes, JsRightPadded.Location location, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            JRightPadded<? extends J> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), location.getAfterLocation(), p);
            visitMarkers(node.getMarkers(), p);
            if (i < nodes.size() - 1) {
                p.append(suffixBetween);
            }
        }
    }

    protected void visitRightPadded(List<? extends JRightPadded<? extends J>> nodes, JRightPadded.Location location, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            JRightPadded<? extends J> node = nodes.get(i);
            JavaScriptPrinter.this.visit(node.getElement(), p);
            visitSpace(node.getAfter(), location.getAfterLocation(), p);
            visitMarkers(node.getMarkers(), p);
            if (i < nodes.size() - 1) {
                p.append(suffixBetween);
            }
        }
    }

    @Override
    public Space visitSpace(Space space, JsSpace.Location loc, PrintOutputCapture<P> p) {
        return delegate.visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
        return delegate.visitSpace(space, loc, p);
    }

    @Override
    public Markers visitMarkers(@Nullable Markers markers, PrintOutputCapture<P> pPrintOutputCapture) {
        return delegate.visitMarkers(markers, pPrintOutputCapture);
    }
}
