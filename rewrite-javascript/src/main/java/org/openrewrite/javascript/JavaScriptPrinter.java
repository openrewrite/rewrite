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
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.Semicolon;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.javascript.marker.Computed;
import org.openrewrite.javascript.marker.DelegatedYield;
import org.openrewrite.javascript.marker.FunctionDeclaration;
import org.openrewrite.javascript.marker.Generator;
import org.openrewrite.javascript.marker.NonNullAssertion;
import org.openrewrite.javascript.marker.Optional;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.javascript.tree.JSX;
import org.openrewrite.javascript.tree.JsContainer;
import org.openrewrite.javascript.tree.JsLeftPadded;
import org.openrewrite.javascript.tree.JsRightPadded;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.Iterator;
import java.util.List;
import java.util.function.UnaryOperator;

import static java.util.Collections.singletonList;

/**
 * Prints JavaScript and TypeScript LSTs. This is a port of the TypeScript {@code JavaScriptPrinter}
 * in {@code rewrite/src/javascript/print.ts} and must print exactly what that printer does.
 */
public class JavaScriptPrinter<P> extends JavaScriptVisitor<PrintOutputCapture<P>> {

    private static final UnaryOperator<String> JAVA_SCRIPT_MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    @Override
    public J visitJsCompilationUnit(JS.CompilationUnit cu, PrintOutputCapture<P> p) {
        beforeSyntax(cu, p);
        visitRightPaddedLocal(cu.getPadding().getStatements(), "", p);
        visitSpace(cu.getEof(), p);
        afterSyntax(cu, p);
        return cu;
    }

    @Override
    public J visitAlias(JS.Alias alias, PrintOutputCapture<P> p) {
        beforeSyntax(alias, p);
        visitRightPadded(alias.getPadding().getPropertyName(), p);
        p.append("as");
        visit(alias.getAlias(), p);
        afterSyntax(alias, p);
        return alias;
    }

    @Override
    public J visitAwait(JS.Await await, PrintOutputCapture<P> p) {
        beforeSyntax(await, p);
        p.append("await");
        visit(await.getExpression(), p);
        afterSyntax(await, p);
        return await;
    }

    @Override
    public J visitBindingElement(JS.BindingElement binding, PrintOutputCapture<P> p) {
        beforeSyntax(binding, p);
        if (binding.getPadding().getPropertyName() != null) {
            visitRightPadded(binding.getPadding().getPropertyName(), p);
            p.append(":");
        }
        visit(binding.getName(), p);
        visitLeftPaddedLocal("=", binding.getPadding().getInitializer(), p);
        afterSyntax(binding, p);
        return binding;
    }

    @Override
    public J visitDelete(JS.Delete delete, PrintOutputCapture<P> p) {
        beforeSyntax(delete, p);
        p.append("delete");
        visit(delete.getExpression(), p);
        afterSyntax(delete, p);
        return delete;
    }

    @Override
    public J visitExpressionStatement(JS.ExpressionStatement statement, PrintOutputCapture<P> p) {
        beforeSyntax(statement, p);
        visit(statement.getExpression(), p);
        afterSyntax(statement, p);
        return statement;
    }

    @Override
    public J visitStatementExpression(JS.StatementExpression expression, PrintOutputCapture<P> p) {
        beforeSyntax(expression, p);
        visit(expression.getStatement(), p);
        afterSyntax(expression, p);
        return expression;
    }

    @Override
    public J visitSpread(JS.Spread spread, PrintOutputCapture<P> p) {
        beforeSyntax(spread, p);
        p.append("...");
        visit(spread.getExpression(), p);
        afterSyntax(spread, p);
        return spread;
    }

    @Override
    public J visitInferType(JS.InferType inferType, PrintOutputCapture<P> p) {
        beforeSyntax(inferType, p);
        visitLeftPaddedLocal("infer", inferType.getPadding().getTypeParameter(), p);
        afterSyntax(inferType, p);
        return inferType;
    }

    @Override
    public J visitJsxTag(JSX.Tag tag, PrintOutputCapture<P> p) {
        beforeSyntax(tag, p);
        p.append("<");
        visitSpace(tag.getPadding().getOpenName().getBefore(), p);
        visit(tag.getPadding().getOpenName().getElement(), p);
        visitContainerLocal("<", tag.getPadding().getTypeArguments(), ",", ">", p);
        visitSpace(tag.getAfterName(), p);
        visitRightPaddedLocal(tag.getPadding().getAttributes(), "", p);

        if (tag.getSelfClosing() != null) {
            visitSpace(tag.getSelfClosing(), p);
            p.append("/>");
        } else {
            p.append(">");
            JLeftPadded<J> closingName = tag.getPadding().getClosingName();
            if (tag.getChildren() != null && closingName != null) {
                visit(tag.getChildren(), p);
                p.append("</");
                visitSpace(closingName.getBefore(), p);
                visit(closingName.getElement(), p);
                visitSpace(tag.getAfterClosingName(), p);
                p.append(">");
            }
        }

        afterSyntax(tag, p);
        return tag;
    }

    @Override
    public J visitJsxAttribute(JSX.Attribute attribute, PrintOutputCapture<P> p) {
        beforeSyntax(attribute, p);
        visit(attribute.getKey(), p);
        visitLeftPaddedLocal("=", attribute.getPadding().getValue(), p);
        afterSyntax(attribute, p);
        return attribute;
    }

    @Override
    public J visitJsxSpreadAttribute(JSX.SpreadAttribute spreadAttribute, PrintOutputCapture<P> p) {
        beforeSyntax(spreadAttribute, p);
        p.append("{");
        visitSpace(spreadAttribute.getDots(), p);
        p.append("...");
        visitRightPaddedLocal(singletonList(spreadAttribute.getPadding().getExpression()), "}", p);
        p.append("}");
        afterSyntax(spreadAttribute, p);
        return spreadAttribute;
    }

    @Override
    public J visitJsxEmbeddedExpression(JSX.EmbeddedExpression embeddedExpression, PrintOutputCapture<P> p) {
        beforeSyntax(embeddedExpression, p);
        p.append("{");
        visitRightPaddedLocal(singletonList(embeddedExpression.getPadding().getExpression()), "}", p);
        p.append("}");
        afterSyntax(embeddedExpression, p);
        return embeddedExpression;
    }

    @Override
    public J visitJsxNamespacedName(JSX.NamespacedName namespacedName, PrintOutputCapture<P> p) {
        beforeSyntax(namespacedName, p);
        visit(namespacedName.getNamespace(), p);
        p.append(":");
        visitLeftPadded(namespacedName.getPadding().getName(), p);
        afterSyntax(namespacedName, p);
        return namespacedName;
    }

    @Override
    public J visitImportDeclaration(JS.Import jsImport, PrintOutputCapture<P> p) {
        visit(jsImport.getModifiers(), p);
        beforeSyntax(jsImport, p);
        p.append("import");
        visit(jsImport.getImportClause(), p);
        visitLeftPaddedLocal(jsImport.getImportClause() != null ? "from" : "", jsImport.getPadding().getModuleSpecifier(), p);
        visit(jsImport.getAttributes(), p);
        if (jsImport.getPadding().getInitializer() != null) {
            p.append("=");
            visitLeftPadded(jsImport.getPadding().getInitializer(), p);
        }
        afterSyntax(jsImport, p);
        return jsImport;
    }

    @Override
    public J visitImportClause(JS.ImportClause jsImportClause, PrintOutputCapture<P> p) {
        beforeSyntax(jsImportClause, p);
        if (jsImportClause.isTypeOnly()) {
            p.append("type");
        }
        if (jsImportClause.getPadding().getName() != null) {
            visitRightPadded(jsImportClause.getPadding().getName(), p);
            if (jsImportClause.getNamedBindings() != null) {
                p.append(",");
            }
        }
        visit(jsImportClause.getNamedBindings(), p);
        afterSyntax(jsImportClause, p);
        return jsImportClause;
    }

    @Override
    public J visitTypeTreeExpression(JS.TypeTreeExpression typeTreeExpression, PrintOutputCapture<P> p) {
        beforeSyntax(typeTreeExpression, p);
        visit(typeTreeExpression.getExpression(), p);
        afterSyntax(typeTreeExpression, p);
        return typeTreeExpression;
    }

    @Override
    public J visitNamespaceDeclaration(JS.NamespaceDeclaration namespaceDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(namespaceDeclaration, p);
        for (J.Modifier m : namespaceDeclaration.getModifiers()) {
            visitModifier(m, p);
        }
        visitSpace(namespaceDeclaration.getPadding().getKeywordType().getBefore(), p);
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
        visitRightPadded(namespaceDeclaration.getPadding().getName(), p);
        visit(namespaceDeclaration.getBody(), p);
        afterSyntax(namespaceDeclaration, p);
        return namespaceDeclaration;
    }

    @Override
    public J visitSatisfiesExpression(JS.SatisfiesExpression satisfiesExpression, PrintOutputCapture<P> p) {
        beforeSyntax(satisfiesExpression, p);
        visit(satisfiesExpression.getExpression(), p);
        visitLeftPaddedLocal("satisfies", satisfiesExpression.getPadding().getSatisfiesType(), p);
        afterSyntax(satisfiesExpression, p);
        return satisfiesExpression;
    }

    @Override
    public J visitVoid(JS.Void aVoid, PrintOutputCapture<P> p) {
        beforeSyntax(aVoid, p);
        p.append("void");
        visit(aVoid.getExpression(), p);
        afterSyntax(aVoid, p);
        return aVoid;
    }

    @Override
    public J visitYield(J.Yield yield, PrintOutputCapture<P> p) {
        beforeSyntax(yield, p);
        p.append("yield");
        DelegatedYield delegated = yield.getMarkers().findFirst(DelegatedYield.class).orElse(null);
        if (delegated != null) {
            visitSpace(delegated.getPrefix(), p);
            p.append("*");
        }
        visit(yield.getValue(), p);
        afterSyntax(yield, p);
        return yield;
    }

    @Override
    public J visitTry(J.Try tryable, PrintOutputCapture<P> p) {
        beforeSyntax(tryable, p);
        p.append("try");
        visit(tryable.getBody(), p);
        visit(tryable.getCatches(), p);
        visitLeftPaddedLocal("finally", tryable.getPadding().getFinally(), p);
        afterSyntax(tryable, p);
        return tryable;
    }

    @Override
    public J visitCatch(J.Try.Catch catch_, PrintOutputCapture<P> p) {
        beforeSyntax(catch_, p);
        p.append("catch");
        if (!catch_.getParameter().getTree().getVariables().isEmpty()) {
            visit(catch_.getParameter(), p);
        } else {
            beforeSyntaxMarkers(catch_.getParameter().getMarkers(), p);
            markersOnly(catch_.getParameter().getTree(), p);
            afterSyntax(catch_.getParameter(), p);
        }
        visit(catch_.getBody(), p);
        afterSyntax(catch_, p);
        return catch_;
    }

    @Override
    public J visitArrayDimension(J.ArrayDimension arrayDimension, PrintOutputCapture<P> p) {
        beforeSyntax(arrayDimension, p);
        p.append("[");
        visitRightPaddedLocalSingle(arrayDimension.getPadding().getIndex(), "]", p);
        afterSyntax(arrayDimension, p);
        return arrayDimension;
    }

    @Override
    public J visitArrayType(J.ArrayType arrayType, PrintOutputCapture<P> p) {
        beforeSyntax(arrayType, p);
        TypeTree type = arrayType;
        while (type instanceof J.ArrayType) {
            type = ((J.ArrayType) type).getElementType();
        }
        visit(type, p);
        visit(arrayType.getAnnotations(), p);

        JLeftPadded<Space> dimension = arrayType.getDimension();
        if (dimension != null) {
            visitSpace(dimension.getBefore(), p);
            p.append("[");
            visitSpace(dimension.getElement(), p);
            p.append("]");

            if (arrayType.getElementType() instanceof J.ArrayType) {
                printDimensions((J.ArrayType) arrayType.getElementType(), p);
            }
        }

        afterSyntax(arrayType, p);
        return arrayType;
    }

    private void printDimensions(J.ArrayType arrayType, PrintOutputCapture<P> p) {
        beforeSyntax(arrayType, p);
        visit(arrayType.getAnnotations(), p);
        JLeftPadded<Space> dimension = arrayType.getDimension();
        visitSpace(dimension == null ? Space.EMPTY : dimension.getBefore(), p);
        p.append("[");
        visitSpace(dimension == null ? Space.EMPTY : dimension.getElement(), p);
        p.append("]");

        if (arrayType.getElementType() instanceof J.ArrayType) {
            printDimensions((J.ArrayType) arrayType.getElementType(), p);
        }

        afterSyntax(arrayType, p);
    }

    @Override
    public J visitTernary(J.Ternary ternary, PrintOutputCapture<P> p) {
        beforeSyntax(ternary, p);
        visit(ternary.getCondition(), p);
        visitLeftPaddedLocal("?", ternary.getPadding().getTruePart(), p);
        visitLeftPaddedLocal(":", ternary.getPadding().getFalsePart(), p);
        afterSyntax(ternary, p);
        return ternary;
    }

    @Override
    public J visitThrow(J.Throw thrown, PrintOutputCapture<P> p) {
        beforeSyntax(thrown, p);
        p.append("throw");
        visit(thrown.getException(), p);
        afterSyntax(thrown, p);
        return thrown;
    }

    @Override
    public J visitIf(J.If iff, PrintOutputCapture<P> p) {
        beforeSyntax(iff, p);
        p.append("if");
        visit(iff.getIfCondition(), p);
        visitStatementLocal(iff.getPadding().getThenPart(), p);
        visit(iff.getElsePart(), p);
        afterSyntax(iff, p);
        return iff;
    }

    @Override
    public J visitElse(J.If.Else else_, PrintOutputCapture<P> p) {
        beforeSyntax(else_, p);
        p.append("else");
        visitStatementLocal(else_.getPadding().getBody(), p);
        afterSyntax(else_, p);
        return else_;
    }

    @Override
    public J visitDoWhileLoop(J.DoWhileLoop doWhileLoop, PrintOutputCapture<P> p) {
        beforeSyntax(doWhileLoop, p);
        p.append("do");
        visitStatementLocal(doWhileLoop.getPadding().getBody(), p);
        visitLeftPaddedLocal("while", doWhileLoop.getPadding().getWhileCondition(), p);
        afterSyntax(doWhileLoop, p);
        return doWhileLoop;
    }

    @Override
    public J visitWhileLoop(J.WhileLoop whileLoop, PrintOutputCapture<P> p) {
        beforeSyntax(whileLoop, p);
        p.append("while");
        visit(whileLoop.getCondition(), p);
        visitStatementLocal(whileLoop.getPadding().getBody(), p);
        afterSyntax(whileLoop, p);
        return whileLoop;
    }

    @Override
    public J visitInstanceOf(J.InstanceOf instanceOf, PrintOutputCapture<P> p) {
        beforeSyntax(instanceOf, p);
        visitRightPaddedLocalSingle(instanceOf.getPadding().getExpression(), "instanceof", p);
        visit(instanceOf.getClazz(), p);
        visit(instanceOf.getPattern(), p);
        afterSyntax(instanceOf, p);
        return instanceOf;
    }

    @Override
    public J visitLiteral(J.Literal literal, PrintOutputCapture<P> p) {
        beforeSyntax(literal, p);

        List<J.Literal.UnicodeEscape> unicodeEscapes = literal.getUnicodeEscapes();
        if (unicodeEscapes == null) {
            p.append(literal.getValueSource());
        } else if (literal.getValueSource() != null) {
            Iterator<J.Literal.UnicodeEscape> surrogateIter = unicodeEscapes.iterator();
            J.Literal.UnicodeEscape surrogate = surrogateIter.hasNext() ? surrogateIter.next() : null;
            int i = 0;

            if (surrogate != null && surrogate.getValueSourceIndex() == 0) {
                p.append("\\u").append(surrogate.getCodePoint());
                surrogate = surrogateIter.hasNext() ? surrogateIter.next() : null;
            }

            String valueSource = literal.getValueSource();
            for (int j = 0; j < valueSource.length(); j++) {
                p.append(valueSource.charAt(j));

                if (surrogate != null && surrogate.getValueSourceIndex() == ++i) {
                    while (surrogate != null && surrogate.getValueSourceIndex() == i) {
                        p.append("\\u").append(surrogate.getCodePoint());
                        surrogate = surrogateIter.hasNext() ? surrogateIter.next() : null;
                    }
                }
            }
        }

        afterSyntax(literal, p);
        return literal;
    }

    @Override
    public J visitScopedVariableDeclarations(JS.ScopedVariableDeclarations variableDeclarations, PrintOutputCapture<P> p) {
        beforeSyntax(variableDeclarations, p);
        for (J.Modifier m : variableDeclarations.getModifiers()) {
            visitModifier(m, p);
        }
        visitRightPaddedLocal(variableDeclarations.getPadding().getVariables(), ",", p);
        afterSyntax(variableDeclarations, p);
        return variableDeclarations;
    }

    @Override
    public J visitShebang(JS.Shebang shebang, PrintOutputCapture<P> p) {
        beforeSyntax(shebang, p);
        p.append(shebang.getText());
        afterSyntax(shebang, p);
        return shebang;
    }

    @Override
    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, PrintOutputCapture<P> p) {
        beforeSyntax(multiVariable, p);
        visit(multiVariable.getLeadingAnnotations(), p);
        for (J.Modifier m : multiVariable.getModifiers()) {
            visitModifier(m, p);
        }

        List<JRightPadded<J.VariableDeclarations.NamedVariable>> variables = multiVariable.getPadding().getVariables();
        for (int i = 0; i < variables.size(); i++) {
            JRightPadded<J.VariableDeclarations.NamedVariable> variable = variables.get(i);

            beforeSyntax(variable.getElement(), p);

            if (multiVariable.getVarargs() != null) {
                p.append("...");
            }

            visit(variable.getElement().getDeclarator(), p);
            // print non-null assertions or optional
            postVisit(variable.getElement(), p);

            visitSpace(variable.getAfter(), p);

            visit(multiVariable.getTypeExpression(), p);
            visitLeftPaddedLocal("=", variable.getElement().getPadding().getInitializer(), p);

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
        beforeSyntax(variable, p);
        visit(variable.getDeclarator(), p);
        visitLeftPaddedLocal("=", variable.getPadding().getInitializer(), p);
        afterSyntax(variable, p);
        return variable;
    }

    @Override
    public J visitIdentifier(J.Identifier ident, PrintOutputCapture<P> p) {
        visit(ident.getAnnotations(), p);
        beforeSyntax(ident, p);
        p.append(ident.getSimpleName());
        afterSyntax(ident, p);
        return ident;
    }

    @Override
    public J visitBlock(J.Block block, PrintOutputCapture<P> p) {
        beforeSyntax(block, p);

        if (block.isStatic()) {
            p.append("static");
            visitRightPadded(block.getPadding().getStatic(), p);
        }

        p.append("{");
        visitStatements(block.getPadding().getStatements(), p);
        visitSpace(block.getEnd(), p);
        p.append("}");

        afterSyntax(block, p);
        return block;
    }

    @Override
    public J visitTypeInfo(JS.TypeInfo typeInfo, PrintOutputCapture<P> p) {
        beforeSyntax(typeInfo, p);
        p.append(":");
        visit(typeInfo.getTypeIdentifier(), p);
        afterSyntax(typeInfo, p);
        return typeInfo;
    }

    @Override
    public J visitReturn(J.Return return_, PrintOutputCapture<P> p) {
        beforeSyntax(return_, p);
        p.append("return");
        visit(return_.getExpression(), p);
        afterSyntax(return_, p);
        return return_;
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
                keyword = "const";
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

        beforeSyntax(mod, p);
        p.append(keyword);
        afterSyntax(mod, p);
        return mod;
    }

    @Override
    public J visitFunctionCall(JS.FunctionCall functionCall, PrintOutputCapture<P> p) {
        beforeSyntax(functionCall, p);

        JRightPadded<Expression> function = functionCall.getPadding().getFunction();
        if (function != null) {
            visitRightPadded(function, p);
            if (function.getElement().getMarkers().findFirst(Optional.class).isPresent()) {
                p.append("?.");
            }
        }

        visitContainerLocal("<", functionCall.getPadding().getTypeParameters(), ",", ">", p);
        visitContainerLocal("(", functionCall.getPadding().getArguments(), ",", ")", p);

        afterSyntax(functionCall, p);
        return functionCall;
    }

    @Override
    public J visitFunctionType(JS.FunctionType functionType, PrintOutputCapture<P> p) {
        beforeSyntax(functionType, p);
        for (J.Modifier m : functionType.getModifiers()) {
            visitModifier(m, p);
        }
        if (functionType.isConstructorType()) {
            visitLeftPaddedLocal("new", functionType.getPadding().getConstructorType(), p);
        }
        printTypeParameters(functionType.getTypeParameters(), p);
        visitContainerLocal("(", functionType.getPadding().getParameters(), ",", ")", p);
        visitLeftPaddedLocal("=>", functionType.getPadding().getReturnType(), p);
        afterSyntax(functionType, p);
        return functionType;
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
        // a class kind has no visit method to be dispatched to when it is printed on its own
        if (tree instanceof J.ClassDeclaration.Kind) {
            visitClassDeclarationKind((J.ClassDeclaration.Kind) tree, p);
            return (J) tree;
        }
        return super.visit(tree, p);
    }

    private void visitClassDeclarationKind(J.ClassDeclaration.Kind classKind, PrintOutputCapture<P> p) {
        String kind = "";
        switch (classKind.getType()) {
            case Class:
                kind = "class";
                break;
            case Enum:
                kind = "enum";
                break;
            case Interface:
                kind = "interface";
                break;
            case Annotation:
                kind = "@interface";
                break;
            case Record:
                kind = "record";
                break;
        }

        visit(classKind.getAnnotations(), p);
        beforeSyntax(classKind, p);
        p.append(kind);
        afterSyntax(classKind, p);
    }

    @Override
    public J visitClassDeclaration(J.ClassDeclaration classDecl, PrintOutputCapture<P> p) {
        J.ClassDeclaration.Kind classKind = classDecl.getPadding().getKind();
        beforeSyntax(classDecl, p);
        visit(classDecl.getLeadingAnnotations(), p);
        for (J.Modifier m : classDecl.getModifiers()) {
            visitModifier(m, p);
        }
        visitClassDeclarationKind(classKind, p);
        visit(classDecl.getName(), p);
        visitContainerLocal("<", classDecl.getPadding().getTypeParameters(), ",", ">", p);
        visitContainerLocal("(", classDecl.getPadding().getPrimaryConstructor(), ",", ")", p);
        visitLeftPaddedLocal("extends", classDecl.getPadding().getExtends(), p);
        visitContainerLocal(classKind.getType() == J.ClassDeclaration.Kind.Type.Interface ? "extends" : "implements",
                classDecl.getPadding().getImplements(), ",", null, p);
        visit(classDecl.getBody(), p);
        afterSyntax(classDecl, p);
        return classDecl;
    }

    @Override
    public J visitMethodDeclaration(J.MethodDeclaration method, PrintOutputCapture<P> p) {
        beforeSyntax(method, p);
        visit(method.getLeadingAnnotations(), p);
        for (J.Modifier m : method.getModifiers()) {
            visitModifier(m, p);
        }
        visit(method.getAnnotations().getName().getAnnotations(), p);

        FunctionDeclaration function = method.getMarkers().findFirst(FunctionDeclaration.class).orElse(null);
        if (function != null) {
            visitSpace(function.getPrefix(), p);
            p.append("function");
        }

        Generator asterisk = method.getMarkers().findFirst(Generator.class).orElse(null);
        if (asterisk != null) {
            visitSpace(asterisk.getPrefix(), p);
            p.append("*");
        }

        visit(method.getName(), p);
        printTypeParameters(method.getPadding().getTypeParameters(), p);
        visitContainerLocal("(", method.getPadding().getParameters(), ",", ")", p);
        visit(method.getReturnTypeExpression(), p);
        visit(method.getBody(), p);
        afterSyntax(method, p);
        return method;
    }

    @Override
    public J visitComputedPropertyMethodDeclaration(JS.ComputedPropertyMethodDeclaration method, PrintOutputCapture<P> p) {
        beforeSyntax(method, p);
        visit(method.getLeadingAnnotations(), p);
        for (J.Modifier m : method.getModifiers()) {
            visitModifier(m, p);
        }

        Generator generator = method.getMarkers().findFirst(Generator.class).orElse(null);
        if (generator != null) {
            visitSpace(generator.getPrefix(), p);
            p.append("*");
        }

        visit(method.getName(), p);
        printTypeParameters(method.getTypeParameters(), p);
        visitContainerLocal("(", method.getPadding().getParameters(), ",", ")", p);
        visit(method.getReturnTypeExpression(), p);
        visit(method.getBody(), p);
        afterSyntax(method, p);
        return method;
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<P> p) {
        beforeSyntax(method, p);

        JRightPadded<Expression> select = method.getPadding().getSelect();
        if (method.getName().getSimpleName().isEmpty()) {
            if (select != null) {
                visitRightPadded(select, p);
            }
        } else {
            if (select != null) {
                visitRightPadded(select, p);
                p.append(select.getElement().getMarkers().findFirst(Optional.class).isPresent() ? "?." : ".");
            }
            visit(method.getName(), p);
        }

        visitContainerLocal("<", method.getPadding().getTypeParameters(), ",", ">", p);
        visitContainerLocal("(", method.getPadding().getArguments(), ",", ")", p);

        afterSyntax(method, p);
        return method;
    }

    @Override
    public J visitTypeParameter(J.TypeParameter typeParameter, PrintOutputCapture<P> p) {
        beforeSyntax(typeParameter, p);
        visit(typeParameter.getAnnotations(), p);
        for (J.Modifier m : typeParameter.getModifiers()) {
            visitModifier(m, p);
        }
        visit(typeParameter.getName(), p);

        JContainer<TypeTree> bounds = typeParameter.getPadding().getBounds();
        if (bounds != null) {
            visitSpace(bounds.getBefore(), p);
            JRightPadded<TypeTree> constraintType = bounds.getPadding().getElements().get(0);
            if (!(constraintType.getElement() instanceof J.Empty)) {
                p.append("extends");
                visitRightPadded(constraintType, p);
            } else {
                markersOnly(constraintType.getElement(), p);
            }

            JRightPadded<TypeTree> defaultType = bounds.getPadding().getElements().get(1);
            if (!(defaultType.getElement() instanceof J.Empty)) {
                p.append("=");
                visitRightPadded(defaultType, p);
            } else {
                markersOnly(defaultType.getElement(), p);
            }
        }

        afterSyntax(typeParameter, p);
        return typeParameter;
    }

    @Override
    public J visitArrowFunction(JS.ArrowFunction arrowFunction, PrintOutputCapture<P> p) {
        beforeSyntax(arrowFunction, p);
        visit(arrowFunction.getLeadingAnnotations(), p);
        for (J.Modifier m : arrowFunction.getModifiers()) {
            visitModifier(m, p);
        }
        printTypeParameters(arrowFunction.getTypeParameters(), p);

        J.Lambda lambda = arrowFunction.getLambda();
        beforeSyntaxMarkers(lambda.getMarkers(), p);

        J.Lambda.Parameters parameters = lambda.getParameters();
        if (parameters.isParenthesized()) {
            beforeSyntax(parameters, p);
            p.append("(");
            visitRightPaddedLocal(parameters.getPadding().getParameters(), ",", p);
            p.append(")");
        } else {
            beforeSyntaxMarkers(parameters.getMarkers(), p);
            visitRightPaddedLocal(parameters.getPadding().getParameters(), ",", p);
        }
        afterSyntax(parameters, p);

        visit(arrowFunction.getReturnTypeExpression(), p);

        visitSpace(lambda.getArrow(), p);
        p.append("=>");
        visit(lambda.getBody(), p);
        afterSyntax(lambda, p);

        afterSyntax(arrowFunction, p);
        return arrowFunction;
    }

    @Override
    public J visitConditionalType(JS.ConditionalType conditionalType, PrintOutputCapture<P> p) {
        beforeSyntax(conditionalType, p);
        visit(conditionalType.getCheckType(), p);
        visitLeftPaddedLocal("extends", conditionalType.getPadding().getCondition(), p);
        afterSyntax(conditionalType, p);
        return conditionalType;
    }

    @Override
    public J visitExpressionWithTypeArguments(JS.ExpressionWithTypeArguments type, PrintOutputCapture<P> p) {
        beforeSyntax(type, p);
        visit(type.getClazz(), p);
        visitContainerLocal("<", type.getPadding().getTypeArguments(), ",", ">", p);
        afterSyntax(type, p);
        return type;
    }

    @Override
    public J visitImportType(JS.ImportType importType, PrintOutputCapture<P> p) {
        beforeSyntax(importType, p);

        if (importType.isHasTypeof()) {
            p.append("typeof");
            visitRightPadded(importType.getPadding().getHasTypeof(), p);
        }

        p.append("import");
        visitContainerLocal("(", importType.getPadding().getArgumentAndAttributes(), ",", ")", p);
        visitLeftPaddedLocal(".", importType.getPadding().getQualifier(), p);
        visitContainerLocal("<", importType.getPadding().getTypeArguments(), ",", ">", p);

        afterSyntax(importType, p);
        return importType;
    }

    @Override
    public J visitTypeDeclaration(JS.TypeDeclaration typeDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(typeDeclaration, p);
        for (J.Modifier m : typeDeclaration.getModifiers()) {
            visitModifier(m, p);
        }
        visitLeftPaddedLocal("type", typeDeclaration.getPadding().getName(), p);
        printTypeParameters(typeDeclaration.getTypeParameters(), p);
        visitLeftPaddedLocal("=", typeDeclaration.getPadding().getInitializer(), p);
        afterSyntax(typeDeclaration, p);
        return typeDeclaration;
    }

    @Override
    public J visitUnknownSource(J.Unknown.Source source, PrintOutputCapture<P> p) {
        beforeSyntax(source, p);
        p.append(source.getText());
        afterSyntax(source, p);
        return source;
    }

    @Override
    public J visitLiteralType(JS.LiteralType literalType, PrintOutputCapture<P> p) {
        beforeSyntax(literalType, p);
        visit(literalType.getLiteral(), p);
        afterSyntax(literalType, p);
        return literalType;
    }

    @Override
    public J visitNamedImports(JS.NamedImports namedImports, PrintOutputCapture<P> p) {
        beforeSyntax(namedImports, p);
        visitContainerLocal("{", namedImports.getPadding().getElements(), ",", "}", p);
        afterSyntax(namedImports, p);
        return namedImports;
    }

    @Override
    public J visitImportSpecifier(JS.ImportSpecifier jis, PrintOutputCapture<P> p) {
        beforeSyntax(jis, p);
        if (jis.getImportType()) {
            visitLeftPaddedLocal("type", jis.getPadding().getImportType(), p);
        }
        visit(jis.getSpecifier(), p);
        afterSyntax(jis, p);
        return jis;
    }

    @Override
    public J visitExportDeclaration(JS.ExportDeclaration exportDeclaration, PrintOutputCapture<P> p) {
        for (J.Modifier m : exportDeclaration.getModifiers()) {
            visitModifier(m, p);
        }
        beforeSyntax(exportDeclaration, p);
        p.append("export");
        if (exportDeclaration.isTypeOnly()) {
            visitLeftPaddedLocal("type", exportDeclaration.getPadding().getTypeOnly(), p);
        }
        visit(exportDeclaration.getExportClause(), p);
        visitLeftPaddedLocal("from", exportDeclaration.getPadding().getModuleSpecifier(), p);
        visit(exportDeclaration.getAttributes(), p);
        afterSyntax(exportDeclaration, p);
        return exportDeclaration;
    }

    @Override
    public J visitExportAssignment(JS.ExportAssignment exportAssignment, PrintOutputCapture<P> p) {
        beforeSyntax(exportAssignment, p);
        p.append("export");
        visitLeftPaddedLocal(exportAssignment.isExportEquals() ? "=" : "default", exportAssignment.getPadding().getExpression(), p);
        afterSyntax(exportAssignment, p);
        return exportAssignment;
    }

    @Override
    public J visitIndexedAccessType(JS.IndexedAccessType indexedAccessType, PrintOutputCapture<P> p) {
        beforeSyntax(indexedAccessType, p);
        visit(indexedAccessType.getObjectType(), p);
        visit(indexedAccessType.getIndexType(), p);
        afterSyntax(indexedAccessType, p);
        return indexedAccessType;
    }

    @Override
    public J visitIndexedAccessTypeIndexType(JS.IndexedAccessType.IndexType indexType, PrintOutputCapture<P> p) {
        beforeSyntax(indexType, p);
        p.append("[");
        visitRightPadded(indexType.getPadding().getElement(), p);
        p.append("]");
        afterSyntax(indexType, p);
        return indexType;
    }

    @Override
    public J visitWithStatement(JS.WithStatement withStatement, PrintOutputCapture<P> p) {
        beforeSyntax(withStatement, p);
        p.append("with");
        visit(withStatement.getExpression(), p);
        visitRightPadded(withStatement.getPadding().getBody(), p);
        afterSyntax(withStatement, p);
        return withStatement;
    }

    @Override
    public J visitExportSpecifier(JS.ExportSpecifier exportSpecifier, PrintOutputCapture<P> p) {
        beforeSyntax(exportSpecifier, p);
        if (exportSpecifier.isTypeOnly()) {
            visitLeftPaddedLocal("type", exportSpecifier.getPadding().getTypeOnly(), p);
        }
        visit(exportSpecifier.getSpecifier(), p);
        afterSyntax(exportSpecifier, p);
        return exportSpecifier;
    }

    @Override
    public J visitNamedExports(JS.NamedExports namedExports, PrintOutputCapture<P> p) {
        beforeSyntax(namedExports, p);
        visitContainerLocal("{", namedExports.getPadding().getElements(), ",", "}", p);
        afterSyntax(namedExports, p);
        return namedExports;
    }

    @Override
    public J visitImportAttributes(JS.ImportAttributes importAttributes, PrintOutputCapture<P> p) {
        beforeSyntax(importAttributes, p);
        p.append(importAttributes.getToken() == JS.ImportAttributes.Token.With ? "with" : "assert");
        visitContainerLocal("{", importAttributes.getPadding().getElements(), ",", "}", p);
        afterSyntax(importAttributes, p);
        return importAttributes;
    }

    @Override
    public J visitImportAttribute(JS.ImportAttribute importAttribute, PrintOutputCapture<P> p) {
        beforeSyntax(importAttribute, p);
        visit(importAttribute.getName(), p);
        visitLeftPaddedLocal(":", importAttribute.getPadding().getValue(), p);
        afterSyntax(importAttribute, p);
        return importAttribute;
    }

    @Override
    public J visitImportTypeAttributes(JS.ImportTypeAttributes importTypeAttributes, PrintOutputCapture<P> p) {
        beforeSyntax(importTypeAttributes, p);
        p.append("{");
        visitRightPadded(importTypeAttributes.getPadding().getToken(), p);
        p.append(":");
        visitContainerLocal("{", importTypeAttributes.getPadding().getElements(), ",", "}", p);
        visitSpace(importTypeAttributes.getEnd(), p);
        p.append("}");
        afterSyntax(importTypeAttributes, p);
        return importTypeAttributes;
    }

    @Override
    public J visitArrayBindingPattern(JS.ArrayBindingPattern arrayBindingPattern, PrintOutputCapture<P> p) {
        beforeSyntax(arrayBindingPattern, p);
        visitContainerLocal("[", arrayBindingPattern.getPadding().getElements(), ",", "]", p);
        afterSyntax(arrayBindingPattern, p);
        return arrayBindingPattern;
    }

    @Override
    public J visitMappedType(JS.MappedType mappedType, PrintOutputCapture<P> p) {
        beforeSyntax(mappedType, p);
        p.append("{");

        if (mappedType.getPadding().getPrefixToken() != null) {
            visitLeftPadded(mappedType.getPadding().getPrefixToken(), p);
        }

        if (mappedType.isHasReadonly()) {
            visitLeftPaddedLocal("readonly", mappedType.getPadding().getHasReadonly(), p);
        }

        visitMappedTypeKeysRemapping(mappedType.getKeysRemapping(), p);

        if (mappedType.getPadding().getSuffixToken() != null) {
            visitLeftPadded(mappedType.getPadding().getSuffixToken(), p);
        }

        if (mappedType.isHasQuestionToken()) {
            visitLeftPaddedLocal("?", mappedType.getPadding().getHasQuestionToken(), p);
        }

        JContainer<TypeTree> valueType = mappedType.getPadding().getValueType();
        String colon = valueType.getElements().get(0) instanceof J.Empty ? "" : ":";
        visitContainerLocal(colon, valueType, "", "", p);

        p.append("}");
        afterSyntax(mappedType, p);
        return mappedType;
    }

    @Override
    public J visitMappedTypeKeysRemapping(JS.MappedType.KeysRemapping mappedTypeKeys, PrintOutputCapture<P> p) {
        beforeSyntax(mappedTypeKeys, p);
        p.append("[");
        visitRightPadded(mappedTypeKeys.getPadding().getTypeParameter(), p);

        if (mappedTypeKeys.getPadding().getNameType() != null) {
            p.append("as");
            visitRightPadded(mappedTypeKeys.getPadding().getNameType(), p);
        }

        p.append("]");
        afterSyntax(mappedTypeKeys, p);
        return mappedTypeKeys;
    }

    @Override
    public J visitMappedTypeParameter(JS.MappedType.Parameter parameter, PrintOutputCapture<P> p) {
        beforeSyntax(parameter, p);
        visit(parameter.getName(), p);
        visitLeftPaddedLocal("in", parameter.getPadding().getIterateType(), p);
        afterSyntax(parameter, p);
        return parameter;
    }

    @Override
    public J visitObjectBindingPattern(JS.ObjectBindingPattern objectBindingPattern, PrintOutputCapture<P> p) {
        beforeSyntax(objectBindingPattern, p);
        visit(objectBindingPattern.getLeadingAnnotations(), p);
        for (J.Modifier m : objectBindingPattern.getModifiers()) {
            visitModifier(m, p);
        }
        visit(objectBindingPattern.getTypeExpression(), p);
        visitContainerLocal("{", objectBindingPattern.getPadding().getBindings(), ",", "}", p);
        visitLeftPaddedLocal("=", objectBindingPattern.getPadding().getInitializer(), p);
        afterSyntax(objectBindingPattern, p);
        return objectBindingPattern;
    }

    @Override
    public J visitTaggedTemplateExpression(JS.TaggedTemplateExpression taggedTemplateExpression, PrintOutputCapture<P> p) {
        beforeSyntax(taggedTemplateExpression, p);
        if (taggedTemplateExpression.getPadding().getTag() != null) {
            visitRightPadded(taggedTemplateExpression.getPadding().getTag(), p);
        }
        visitContainerLocal("<", taggedTemplateExpression.getPadding().getTypeArguments(), ",", ">", p);
        visit(taggedTemplateExpression.getTemplateExpression(), p);
        afterSyntax(taggedTemplateExpression, p);
        return taggedTemplateExpression;
    }

    @Override
    public J visitTemplateExpression(JS.TemplateExpression templateExpression, PrintOutputCapture<P> p) {
        beforeSyntax(templateExpression, p);
        visit(templateExpression.getHead(), p);
        visitRightPaddedLocal(templateExpression.getPadding().getSpans(), "", p);
        afterSyntax(templateExpression, p);
        return templateExpression;
    }

    @Override
    public J visitTemplateExpressionSpan(JS.TemplateExpression.Span span, PrintOutputCapture<P> p) {
        beforeSyntax(span, p);
        visit(span.getExpression(), p);
        visit(span.getTail(), p);
        afterSyntax(span, p);
        return span;
    }

    @Override
    public J visitTuple(JS.Tuple tuple, PrintOutputCapture<P> p) {
        beforeSyntax(tuple, p);
        visitContainerLocal("[", tuple.getPadding().getElements(), ",", "]", p);
        afterSyntax(tuple, p);
        return tuple;
    }

    @Override
    public J visitTypeQuery(JS.TypeQuery typeQuery, PrintOutputCapture<P> p) {
        beforeSyntax(typeQuery, p);
        p.append("typeof");
        visit(typeQuery.getTypeExpression(), p);
        visitContainerLocal("<", typeQuery.getPadding().getTypeArguments(), ",", ">", p);
        afterSyntax(typeQuery, p);
        return typeQuery;
    }

    @Override
    public J visitTypeOf(JS.TypeOf typeOf, PrintOutputCapture<P> p) {
        beforeSyntax(typeOf, p);
        p.append("typeof");
        visit(typeOf.getExpression(), p);
        afterSyntax(typeOf, p);
        return typeOf;
    }

    @Override
    public J visitComputedPropertyName(JS.ComputedPropertyName computedPropertyName, PrintOutputCapture<P> p) {
        beforeSyntax(computedPropertyName, p);
        p.append("[");
        visitRightPaddedLocalSingle(computedPropertyName.getPadding().getExpression(), "]", p);
        afterSyntax(computedPropertyName, p);
        return computedPropertyName;
    }

    @Override
    public J visitTypeOperator(JS.TypeOperator typeOperator, PrintOutputCapture<P> p) {
        beforeSyntax(typeOperator, p);

        String keyword = "";
        if (typeOperator.getOperator() == JS.TypeOperator.Type.ReadOnly) {
            keyword = "readonly";
        } else if (typeOperator.getOperator() == JS.TypeOperator.Type.KeyOf) {
            keyword = "keyof";
        } else if (typeOperator.getOperator() == JS.TypeOperator.Type.Unique) {
            keyword = "unique";
        }

        p.append(keyword);
        visitLeftPadded(typeOperator.getPadding().getExpression(), p);

        afterSyntax(typeOperator, p);
        return typeOperator;
    }

    @Override
    public J visitTypePredicate(JS.TypePredicate typePredicate, PrintOutputCapture<P> p) {
        beforeSyntax(typePredicate, p);
        if (typePredicate.isAsserts()) {
            visitLeftPaddedLocal("asserts", typePredicate.getPadding().getAsserts(), p);
        }
        visit(typePredicate.getParameterName(), p);
        visitLeftPaddedLocal("is", typePredicate.getPadding().getExpression(), p);
        afterSyntax(typePredicate, p);
        return typePredicate;
    }

    @Override
    public J visitIndexSignatureDeclaration(JS.IndexSignatureDeclaration indexSignatureDeclaration, PrintOutputCapture<P> p) {
        beforeSyntax(indexSignatureDeclaration, p);
        for (J.Modifier m : indexSignatureDeclaration.getModifiers()) {
            visitModifier(m, p);
        }
        visitContainerLocal("[", indexSignatureDeclaration.getPadding().getParameters(), "", "]", p);
        visitLeftPaddedLocal(":", indexSignatureDeclaration.getPadding().getTypeExpression(), p);
        afterSyntax(indexSignatureDeclaration, p);
        return indexSignatureDeclaration;
    }

    @Override
    public J visitAnnotation(J.Annotation annotation, PrintOutputCapture<P> p) {
        beforeSyntax(annotation, p);
        p.append("@");
        visit(annotation.getAnnotationType(), p);
        visitContainerLocal("(", annotation.getPadding().getArguments(), ",", ")", p);
        afterSyntax(annotation, p);
        return annotation;
    }

    @Override
    public J visitNewArray(J.NewArray newArray, PrintOutputCapture<P> p) {
        beforeSyntax(newArray, p);
        visit(newArray.getTypeExpression(), p);
        visit(newArray.getDimensions(), p);
        visitContainerLocal("[", newArray.getPadding().getInitializer(), ",", "]", p);
        afterSyntax(newArray, p);
        return newArray;
    }

    @Override
    public J visitNewClass(J.NewClass newClass, PrintOutputCapture<P> p) {
        beforeSyntax(newClass, p);
        visitRightPaddedLocalSingle(newClass.getPadding().getEnclosing(), ".", p);
        visitSpace(newClass.getNew(), p);

        if (newClass.getClazz() != null) {
            p.append("new");
            visit(newClass.getClazz(), p);

            if (!newClass.getPadding().getArguments().getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                visitContainerLocal("(", newClass.getPadding().getArguments(), ",", ")", p);
            }
        }

        visit(newClass.getBody(), p);
        afterSyntax(newClass, p);
        return newClass;
    }

    @Override
    public J visitSwitch(J.Switch switch_, PrintOutputCapture<P> p) {
        beforeSyntax(switch_, p);
        p.append("switch");
        visit(switch_.getSelector(), p);
        visit(switch_.getCases(), p);
        afterSyntax(switch_, p);
        return switch_;
    }

    @Override
    public J visitCase(J.Case case_, PrintOutputCapture<P> p) {
        beforeSyntax(case_, p);

        J elem = case_.getCaseLabels().get(0);
        if (!(elem instanceof J.Identifier) || !"default".equals(((J.Identifier) elem).getSimpleName())) {
            p.append("case");
        }

        visitContainerLocal("", case_.getPadding().getCaseLabels(), ",", "", p);

        JContainer<Statement> statements = case_.getPadding().getStatements();
        visitSpace(statements.getBefore(), p);
        p.append(case_.getType() == J.Case.Type.Statement ? ":" : "->");

        visitStatements(statements.getPadding().getElements(), p);

        afterSyntax(case_, p);
        return case_;
    }

    @Override
    public J visitLabel(J.Label label, PrintOutputCapture<P> p) {
        beforeSyntax(label, p);
        visitRightPaddedLocalSingle(label.getPadding().getLabel(), ":", p);
        visit(label.getStatement(), p);
        afterSyntax(label, p);
        return label;
    }

    @Override
    public J visitContinue(J.Continue continueStatement, PrintOutputCapture<P> p) {
        beforeSyntax(continueStatement, p);
        p.append("continue");
        visit(continueStatement.getLabel(), p);
        afterSyntax(continueStatement, p);
        return continueStatement;
    }

    @Override
    public J visitBreak(J.Break breakStatement, PrintOutputCapture<P> p) {
        beforeSyntax(breakStatement, p);
        p.append("break");
        visit(breakStatement.getLabel(), p);
        afterSyntax(breakStatement, p);
        return breakStatement;
    }

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess, PrintOutputCapture<P> p) {
        beforeSyntax(fieldAccess, p);
        visit(fieldAccess.getTarget(), p);
        visitLeftPaddedLocal(".", fieldAccess.getPadding().getName(), p);
        afterSyntax(fieldAccess, p);
        return fieldAccess;
    }

    @Override
    public J visitTypeLiteral(JS.TypeLiteral typeLiteral, PrintOutputCapture<P> p) {
        beforeSyntax(typeLiteral, p);
        visit(typeLiteral.getMembers(), p);
        afterSyntax(typeLiteral, p);
        return typeLiteral;
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parens, PrintOutputCapture<P> p) {
        beforeSyntax(parens, p);
        p.append('(');
        visitRightPaddedLocalSingle(parens.getPadding().getTree(), ")", p);
        afterSyntax(parens, p);
        return parens;
    }

    @Override
    public J visitParameterizedType(J.ParameterizedType type, PrintOutputCapture<P> p) {
        beforeSyntax(type, p);
        visit(type.getClazz(), p);
        visitContainerLocal("<", type.getPadding().getTypeParameters(), ",", ">", p);
        afterSyntax(type, p);
        return type;
    }

    @Override
    public J visitAs(JS.As as_, PrintOutputCapture<P> p) {
        beforeSyntax(as_, p);
        visitRightPadded(as_.getPadding().getLeft(), p);
        p.append("as");
        visit(as_.getRight(), p);
        afterSyntax(as_, p);
        return as_;
    }

    @Override
    public J visitAssignment(J.Assignment assignment, PrintOutputCapture<P> p) {
        beforeSyntax(assignment, p);
        visit(assignment.getVariable(), p);
        visitLeftPaddedLocal("=", assignment.getPadding().getAssignment(), p);
        afterSyntax(assignment, p);
        return assignment;
    }

    @Override
    public J visitPropertyAssignment(JS.PropertyAssignment propertyAssignment, PrintOutputCapture<P> p) {
        beforeSyntax(propertyAssignment, p);
        for (J.Modifier m : propertyAssignment.getModifiers()) {
            visitModifier(m, p);
        }
        visitRightPadded(propertyAssignment.getPadding().getName(), p);

        if (propertyAssignment.getInitializer() != null) {
            // `{ a: b }` or `{ a = b }`, as opposed to the shorthand `{ a }`
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
    public J visitAssignmentOperation(J.AssignmentOperation assignOp, PrintOutputCapture<P> p) {
        String keyword = "";
        switch (assignOp.getOperator()) {
            case Addition:
                keyword = "+=";
                break;
            case Subtraction:
                keyword = "-=";
                break;
            case Multiplication:
                keyword = "*=";
                break;
            case Division:
                keyword = "/=";
                break;
            case Modulo:
                keyword = "%=";
                break;
            case BitAnd:
                keyword = "&=";
                break;
            case BitOr:
                keyword = "|=";
                break;
            case BitXor:
                keyword = "^=";
                break;
            case LeftShift:
                keyword = "<<=";
                break;
            case RightShift:
                keyword = ">>=";
                break;
            case UnsignedRightShift:
                keyword = ">>>=";
                break;
        }

        beforeSyntax(assignOp, p);
        visit(assignOp.getVariable(), p);
        visitSpace(assignOp.getPadding().getOperator().getBefore(), p);
        p.append(keyword);
        visit(assignOp.getAssignment(), p);
        afterSyntax(assignOp, p);
        return assignOp;
    }

    @Override
    public J visitAssignmentOperationExtensions(JS.AssignmentOperation assignOp, PrintOutputCapture<P> p) {
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

        beforeSyntax(assignOp, p);
        visit(assignOp.getVariable(), p);
        visitSpace(assignOp.getPadding().getOperator().getBefore(), p);
        p.append(keyword);
        visit(assignOp.getAssignment(), p);
        afterSyntax(assignOp, p);
        return assignOp;
    }

    @Override
    public J visitEnumValue(J.EnumValue enum_, PrintOutputCapture<P> p) {
        beforeSyntax(enum_, p);
        Computed computed = enum_.getName().getMarkers().findFirst(Computed.class).orElse(null);
        if (computed != null) {
            p.append("[");
        }
        visit(enum_.getName(), p);
        if (computed != null) {
            visitSpace(computed.getSuffix(), p);
            p.append("]");
        }

        J.NewClass initializer = enum_.getInitializer();
        if (initializer != null) {
            beforeSyntax(initializer, p);
            p.append("=");
            // There can be only one argument
            visitRightPadded(initializer.getPadding().getArguments().getPadding().getElements().get(0), p);
            afterSyntax(initializer, p);
        }

        afterSyntax(enum_, p);
        return enum_;
    }

    @Override
    public J visitEnumValueSet(J.EnumValueSet enums, PrintOutputCapture<P> p) {
        beforeSyntax(enums, p);
        visitRightPaddedLocal(enums.getPadding().getEnums(), ",", p);

        if (enums.isTerminatedWithSemicolon()) {
            p.append(",");
        }

        afterSyntax(enums, p);
        return enums;
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
                keyword = "||";
                break;
            case And:
                keyword = "&&";
                break;
        }

        beforeSyntax(binary, p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), p);
        p.append(keyword);
        visit(binary.getRight(), p);
        afterSyntax(binary, p);
        return binary;
    }

    @Override
    public J visitBinaryExtensions(JS.Binary binary, PrintOutputCapture<P> p) {
        beforeSyntax(binary, p);

        visit(binary.getLeft(), p);
        String keyword = "";
        switch (binary.getOperator()) {
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

        visitSpace(binary.getPadding().getOperator().getBefore(), p);
        p.append(keyword);

        visit(binary.getRight(), p);

        afterSyntax(binary, p);
        return binary;
    }

    @Override
    public J visitUnary(J.Unary unary, PrintOutputCapture<P> p) {
        beforeSyntax(unary, p);
        switch (unary.getOperator()) {
            case PreIncrement:
                p.append("++");
                visit(unary.getExpression(), p);
                break;
            case PreDecrement:
                p.append("--");
                visit(unary.getExpression(), p);
                break;
            case PostIncrement:
                visit(unary.getExpression(), p);
                visitSpace(unary.getPadding().getOperator().getBefore(), p);
                p.append("++");
                break;
            case PostDecrement:
                visit(unary.getExpression(), p);
                visitSpace(unary.getPadding().getOperator().getBefore(), p);
                p.append("--");
                break;
            case Positive:
                p.append('+');
                visit(unary.getExpression(), p);
                break;
            case Negative:
                p.append('-');
                visit(unary.getExpression(), p);
                break;
            case Complement:
                p.append('~');
                visit(unary.getExpression(), p);
                break;
            case Not:
            default:
                p.append('!');
                visit(unary.getExpression(), p);
        }
        afterSyntax(unary, p);
        return unary;
    }

    @Override
    public J visitUnion(JS.Union union, PrintOutputCapture<P> p) {
        beforeSyntax(union, p);
        visitRightPaddedLocal(union.getPadding().getTypes(), "|", p);
        afterSyntax(union, p);
        return union;
    }

    @Override
    public J visitIntersection(JS.Intersection intersection, PrintOutputCapture<P> p) {
        beforeSyntax(intersection, p);
        visitRightPaddedLocal(intersection.getPadding().getTypes(), "&", p);
        afterSyntax(intersection, p);
        return intersection;
    }

    @Override
    public J visitForLoop(J.ForLoop forLoop, PrintOutputCapture<P> p) {
        beforeSyntax(forLoop, p);
        p.append("for");
        J.ForLoop.Control ctrl = forLoop.getControl();
        beforeSyntax(ctrl, p);
        p.append('(');
        visitRightPaddedLocal(ctrl.getPadding().getInit(), ",", p);
        p.append(';');
        visitRightPaddedLocalSingle(ctrl.getPadding().getCondition(), ";", p);
        visitRightPaddedLocal(ctrl.getPadding().getUpdate(), ",", p);
        p.append(')');
        afterSyntax(ctrl, p);
        visitStatementLocal(forLoop.getPadding().getBody(), p);
        afterSyntax(forLoop, p);
        return forLoop;
    }

    @Override
    public J visitForOfLoop(JS.ForOfLoop forOfLoop, PrintOutputCapture<P> p) {
        beforeSyntax(forOfLoop, p);
        p.append("for");
        if (forOfLoop.getAwait() != null) {
            visitSpace(forOfLoop.getAwait(), p);
            p.append("await");
        }

        beforeSyntaxMarkers(forOfLoop.getLoop().getMarkers(), p);
        J.ForEachLoop.Control control = forOfLoop.getLoop().getControl();
        beforeSyntax(control, p);
        p.append('(');
        visitRightPadded(control.getPadding().getVariable(), p);
        p.append("of");
        visitRightPadded(control.getPadding().getIterable(), p);
        p.append(')');
        afterSyntax(control, p);
        visitRightPadded(forOfLoop.getLoop().getPadding().getBody(), p);
        afterSyntax(forOfLoop.getLoop(), p);
        afterSyntax(forOfLoop, p);
        return forOfLoop;
    }

    @Override
    public J visitForInLoop(JS.ForInLoop forInLoop, PrintOutputCapture<P> p) {
        beforeSyntax(forInLoop, p);
        p.append("for");

        J.ForEachLoop.Control control = forInLoop.getControl();
        beforeSyntax(control, p);
        p.append('(');
        visitRightPadded(control.getPadding().getVariable(), p);
        p.append("in");
        visitRightPadded(control.getPadding().getIterable(), p);
        p.append(')');
        afterSyntax(control, p);
        visitRightPadded(forInLoop.getPadding().getBody(), p);
        afterSyntax(forInLoop, p);
        return forInLoop;
    }

    @Override
    public <T extends J> J visitControlParentheses(J.ControlParentheses<T> controlParens, PrintOutputCapture<P> p) {
        beforeSyntax(controlParens, p);

        if (parentTree() instanceof J.TypeCast) {
            p.append('<');
            visitRightPaddedLocalSingle(controlParens.getPadding().getTree(), ">", p);
        } else {
            p.append('(');
            visitRightPaddedLocalSingle(controlParens.getPadding().getTree(), ")", p);
        }

        afterSyntax(controlParens, p);
        return controlParens;
    }

    @Override
    public @Nullable J postVisit(J tree, PrintOutputCapture<P> p) {
        for (Marker marker : tree.getMarkers().getMarkers()) {
            if (marker instanceof NonNullAssertion) {
                visitSpace(((NonNullAssertion) marker).getPrefix(), p);
                p.append("!");
            }
            if (marker instanceof Optional) {
                visitSpace(((Optional) marker).getPrefix(), p);
                Tree parent = parentTree();
                if (!(parent instanceof J.MethodInvocation) && !(parent instanceof JS.FunctionCall)) {
                    p.append("?");
                    if (parent instanceof J.ArrayAccess) {
                        p.append(".");
                    }
                }
            }
        }
        return tree;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
        if (marker instanceof Semicolon) {
            p.append(';');
        }
        if (marker instanceof TrailingComma) {
            p.append(',');
            visitSpace(((TrailingComma) marker).getSuffix(), p);
        }
        return (M) marker;
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
        visitSpace(space, p);
        return space;
    }

    private void visitSpace(@Nullable Space space, PrintOutputCapture<P> p) {
        if (space == null) {
            return;
        }
        p.append(space.getWhitespace());

        for (Comment comment : space.getComments()) {
            visitMarkers(comment.getMarkers(), p);
            printComment(comment, p);
            p.append(comment.getSuffix());
        }
    }

    private void printComment(Comment comment, PrintOutputCapture<P> p) {
        for (Marker marker : comment.getMarkers().getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), comment), JAVA_SCRIPT_MARKER_WRAPPER));
        }

        if (comment instanceof TextComment) {
            TextComment textComment = (TextComment) comment;
            p.append(textComment.isMultiline() ? "/*" + textComment.getText() + "*/" : "//" + textComment.getText());
        }

        for (Marker marker : comment.getMarkers().getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), comment), JAVA_SCRIPT_MARKER_WRAPPER));
        }
    }

    private void printTypeParameters(J.@Nullable TypeParameters typeParameters, PrintOutputCapture<P> p) {
        if (typeParameters != null) {
            visit(typeParameters.getAnnotations(), p);
            beforeSyntax(typeParameters.getPrefix(), typeParameters.getMarkers(), p);
            p.append("<");
            visitRightPaddedLocal(typeParameters.getPadding().getTypeParameters(), ",", p);
            p.append(">");
            afterSyntax(typeParameters.getMarkers(), p);
        }
    }

    private void visitStatements(List<JRightPadded<Statement>> statements, PrintOutputCapture<P> p) {
        boolean objectLiteral = getCursor().getValue() instanceof J.Block && parentTree() instanceof J.NewClass;

        for (int i = 0; i < statements.size(); i++) {
            visitStatementLocal(statements.get(i), p);
            if (i < statements.size() - 1 && objectLiteral) {
                p.append(",");
            }
        }
    }

    // A cursor handed in by a caller holds the padding between a tree and the tree enclosing it.
    private @Nullable Tree parentTree() {
        for (Cursor c = getCursor().getParent(); c != null; c = c.getParent()) {
            if (c.getValue() instanceof Tree) {
                return c.getValue();
            }
        }
        return null;
    }

    private void visitStatementLocal(@Nullable JRightPadded<Statement> paddedStat, PrintOutputCapture<P> p) {
        if (paddedStat != null) {
            visit(paddedStat.getElement(), p);
            visitSpace(paddedStat.getAfter(), p);
            visitMarkers(paddedStat.getMarkers(), p);
        }
    }

    private void beforeSyntax(J j, PrintOutputCapture<P> p) {
        beforeSyntax(j.getPrefix(), j.getMarkers(), p);
    }

    private void beforeSyntax(Space prefix, Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), JAVA_SCRIPT_MARKER_WRAPPER));
        }

        visitSpace(prefix, p);
        visitMarkers(markers, p);

        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), JAVA_SCRIPT_MARKER_WRAPPER));
        }
    }

    // A tree that stands for something left out of the source is still printed where a recipe marked it.
    private void markersOnly(J j, PrintOutputCapture<P> p) {
        beforeSyntaxMarkers(j.getMarkers(), p);
        afterSyntax(j, p);
    }

    // For a tree printed as part of another, whose prefix is not printed.
    private void beforeSyntaxMarkers(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), JAVA_SCRIPT_MARKER_WRAPPER));
        }
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), JAVA_SCRIPT_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(J j, PrintOutputCapture<P> p) {
        afterSyntax(j.getMarkers(), p);
    }

    private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), JAVA_SCRIPT_MARKER_WRAPPER));
        }
    }

    private void visitRightPaddedLocal(List<? extends JRightPadded<? extends J>> nodes, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            JRightPadded<? extends J> node = nodes.get(i);

            visit(node.getElement(), p);

            visitSpace(node.getAfter(), p);
            visitMarkers(node.getMarkers(), p);

            if (i < nodes.size() - 1) {
                p.append(suffixBetween);
            }
        }
    }

    private void visitRightPaddedLocalSingle(@Nullable JRightPadded<? extends J> node, String suffix, PrintOutputCapture<P> p) {
        if (node != null) {
            visit(node.getElement(), p);

            visitSpace(node.getAfter(), p);
            visitMarkers(node.getMarkers(), p);

            p.append(suffix);
        }
    }

    private void visitLeftPaddedLocal(@Nullable String prefix, @Nullable JLeftPadded<?> leftPadded, PrintOutputCapture<P> p) {
        if (leftPadded != null) {
            beforeSyntax(leftPadded.getBefore(), leftPadded.getMarkers(), p);

            p.append(prefix);

            Object element = leftPadded.getElement();
            if (element instanceof String) {
                p.append((String) element);
            } else if (element instanceof J) {
                visit((J) element, p);
            }

            afterSyntax(leftPadded.getMarkers(), p);
        }
    }

    private void visitContainerLocal(String before, @Nullable JContainer<? extends J> container, String suffixBetween, @Nullable String after, PrintOutputCapture<P> p) {
        if (container == null) {
            return;
        }

        beforeSyntax(container.getBefore(), container.getMarkers(), p);

        p.append(before);
        visitRightPaddedLocal(container.getPadding().getElements(), suffixBetween, p);
        afterSyntax(container.getMarkers(), p);

        p.append(after);
    }

    private void visitRightPadded(JRightPadded<?> right, PrintOutputCapture<P> p) {
        if (right.getElement() instanceof J) {
            visit((J) right.getElement(), p);
        }

        visitSpace(right.getAfter(), p);
        visitMarkers(right.getMarkers(), p);
    }

    private void visitLeftPadded(JLeftPadded<?> left, PrintOutputCapture<P> p) {
        setCursor(new Cursor(getCursor(), left));
        visitSpace(left.getBefore(), p);
        if (left.getElement() instanceof J) {
            visit((J) left.getElement(), p);
        } else if (left.getElement() instanceof Space) {
            visitSpace((Space) left.getElement(), p);
        }
        visitMarkers(left.getMarkers(), p);
        setCursor(getCursor().getParentOrThrow());
    }

    private void visitContainer(JContainer<? extends J> container, PrintOutputCapture<P> p) {
        setCursor(new Cursor(getCursor(), container));
        visitSpace(container.getBefore(), p);
        for (JRightPadded<? extends J> element : container.getPadding().getElements()) {
            visitRightPadded(element, p);
        }
        visitMarkers(container.getMarkers(), p);
        setCursor(getCursor().getParentOrThrow());
    }

    @Override
    public J visitArrayAccess(J.ArrayAccess arrayAccess, PrintOutputCapture<P> p) {
        beforeSyntax(arrayAccess, p);
        visit(arrayAccess.getIndexed(), p);
        visit(arrayAccess.getDimension(), p);
        afterSyntax(arrayAccess, p);
        return arrayAccess;
    }

    @Override
    public J visitTypeCast(J.TypeCast typeCast, PrintOutputCapture<P> p) {
        beforeSyntax(typeCast, p);
        visit(typeCast.getClazz(), p);
        visit(typeCast.getExpression(), p);
        afterSyntax(typeCast, p);
        return typeCast;
    }

    @Override
    public J visitParenthesizedTypeTree(J.ParenthesizedTypeTree parTree, PrintOutputCapture<P> p) {
        beforeSyntax(parTree, p);
        visit(parTree.getAnnotations(), p);
        visit(parTree.getParenthesizedType(), p);
        afterSyntax(parTree, p);
        return parTree;
    }

    @Override
    public J visitEmpty(J.Empty empty, PrintOutputCapture<P> p) {
        beforeSyntax(empty, p);
        afterSyntax(empty, p);
        return empty;
    }

    @Override
    public J visitUnknown(J.Unknown unknown, PrintOutputCapture<P> p) {
        beforeSyntax(unknown, p);
        visit(unknown.getSource(), p);
        afterSyntax(unknown, p);
        return unknown;
    }

    // What the TypeScript printer has no method for is printed as its base visitor walks it.

    @Override
    public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, PrintOutputCapture<P> p) {
        if (right != null) {
            visitRightPadded(right, p);
        }
        return right;
    }

    @Override
    public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JsRightPadded.Location loc, PrintOutputCapture<P> p) {
        if (right != null) {
            visitRightPadded(right, p);
        }
        return right;
    }

    @Override
    public <T> @Nullable JLeftPadded<T> visitLeftPadded(@Nullable JLeftPadded<T> left, JLeftPadded.Location loc, PrintOutputCapture<P> p) {
        if (left != null) {
            visitLeftPadded(left, p);
        }
        return left;
    }

    @Override
    public <T> @Nullable JLeftPadded<T> visitLeftPadded(@Nullable JLeftPadded<T> left, JsLeftPadded.Location loc, PrintOutputCapture<P> p) {
        if (left != null) {
            visitLeftPadded(left, p);
        }
        return left;
    }

    @Override
    public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container, JContainer.Location loc, PrintOutputCapture<P> p) {
        if (container != null) {
            visitContainer(container, p);
        }
        return container;
    }

    @Override
    public <J2 extends J> @Nullable JContainer<J2> visitContainer(@Nullable JContainer<J2> container, JsContainer.Location loc, PrintOutputCapture<P> p) {
        if (container != null) {
            visitContainer(container, p);
        }
        return container;
    }
}
