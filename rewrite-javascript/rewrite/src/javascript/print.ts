// noinspection DuplicatedCode

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
import {JS, JSX} from "./tree";
import {JavaScriptVisitor} from "./visitor";
import {PrintOutputCapture, TreePrinters} from "../print";
import {Cursor, isTree, Tree} from "../tree";
import {Comment, emptySpace, J, Statement, TextComment, TrailingComma, TypedTree} from "../java";
import {findMarker, Marker, Markers} from "../markers";
import {DelegatedYield, FunctionDeclaration, Generator, NonNullAssertion, Optional} from "./markers";

export class JavaScriptPrinter extends JavaScriptVisitor<PrintOutputCapture> {

    JAVA_SCRIPT_MARKER_WRAPPER: (out: string) => string = (out) => `/*~~${out}${out.length === 0 ? "" : "~~"}>*/`;

    override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(cu, p);

        await this.visitRightPaddedLocal(cu.statements, "", p);

        await this.visitSpace(cu.eof, p);
        await this.afterSyntax(cu, p);
        return cu;
    }

    override async visitAlias(alias: JS.Alias, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(alias, p);
        await this.visitRightPadded(alias.propertyName, p);
        p.append("as");
        await this.visit(alias.alias, p);
        await this.afterSyntax(alias, p);
        return alias;
    }

    override async visitAwait(awaitExpr: JS.Await, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(awaitExpr, p);
        p.append("await");
        await this.visit(awaitExpr.expression, p);
        await this.afterSyntax(awaitExpr, p);
        return awaitExpr;
    }

    override async visitBindingElement(binding: JS.BindingElement, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(binding, p);
        if (binding.propertyName) {
            await this.visitRightPadded(binding.propertyName, p);
            p.append(":");
        }
        await this.visit(binding.name, p);
        binding.initializer && await this.visitLeftPaddedLocal("=", binding.initializer, p);
        await this.afterSyntax(binding, p);
        return binding;
    }

    override async visitDelete(del: JS.Delete, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(del, p);
        p.append("delete");
        await this.visit(del.expression, p);
        await this.afterSyntax(del, p);
        return del;
    }

    override async visitExpressionStatement(statement: JS.ExpressionStatement, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(statement, p);
        await this.visit(statement.expression, p);
        await this.afterSyntax(statement, p);
        return statement;
    }

    override async visitStatementExpression(statementExpression: JS.StatementExpression, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(statementExpression, p);
        await this.visit(statementExpression.statement, p);
        await this.afterSyntax(statementExpression, p);
        return statementExpression;
    }

    override async visitSpread(spread: JS.Spread, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(spread, p);
        p.append("...");
        await this.visit(spread.expression, p);
        await this.afterSyntax(spread, p);
        return spread;
    }

    override async visitInferType(inferType: JS.InferType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(inferType, p);
        await this.visitLeftPaddedLocal("infer", inferType.typeParameter, p);
        await this.afterSyntax(inferType, p);
        return inferType;
    }

    override async visitJsxTag(element: JSX.Tag, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(element, p);
        // Print < first, then the space after < (openName.before), then the tag name
        p.append("<");
        await this.visitSpace(element.openName.before, p);
        await this.visit(element.openName.element, p);
        if (element.typeArguments) {
            await this.visitContainerLocal("<", element.typeArguments, ",", ">", p);
        }
        await this.visitSpace(element.afterName, p);
        await this.visitRightPaddedLocal(element.attributes, "", p);

        if (element.selfClosing) {
            await this.visitSpace(element.selfClosing, p);
            p.append("/>");
        } else {
            p.append(">");
            if (element.children) {
                for (let i = 0; i < element.children.length; i++) {
                    await this.visit(element.children[i], p)
                }
                // Print </ first, then the space after </ (closingName.before), then the tag name
                p.append("</");
                await this.visitSpace(element.closingName!.before, p);
                await this.visit(element.closingName!.element, p);
                await this.visitSpace(element.afterClosingName, p);
                p.append(">");
            }
        }

        await this.afterSyntax(element, p);
        return element;
    }

    override async visitJsxAttribute(attribute: JSX.Attribute, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(attribute, p);
        await this.visit(attribute.key, p);
        if (attribute.value) {
            p.append("=");
            await this.visit(attribute.value.element, p);
        }
        await this.afterSyntax(attribute, p);
        return attribute;
    }

    override async visitJsxSpreadAttribute(spread: JSX.SpreadAttribute, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(spread, p);
        p.append("{");
        await this.visitSpace(spread.dots, p);
        p.append("...");
        await this.visitRightPaddedLocal([spread.expression], "}", p);
        p.append("}");
        await this.afterSyntax(spread, p);
        return spread;
    }

    override async visitJsxEmbeddedExpression(expr: JSX.EmbeddedExpression, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(expr, p);
        p.append("{");
        if (expr.expression) {
            await this.visitRightPaddedLocal([expr.expression], "}", p);
        }
        p.append("}");
        await this.afterSyntax(expr, p);
        return expr;
    }

    override async visitJsxNamespacedName(ns: JSX.NamespacedName, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(ns, p);
        await this.visit(ns.namespace, p);
        p.append(":");
        await this.visitLeftPadded(ns.name, p);
        await this.afterSyntax(ns, p);
        return ns;
    }

    override async visitImportDeclaration(jsImport: JS.Import, p: PrintOutputCapture): Promise<J | undefined> {

        for (const it of jsImport.modifiers) {
            await this.visitDefined(it, p);
        }
        await this.beforeSyntax(jsImport, p);

        p.append("import");

        jsImport.importClause && await this.visit(jsImport.importClause, p);

        await this.visitLeftPaddedLocal(jsImport.importClause ? "from" : "", jsImport.moduleSpecifier, p);

        jsImport.attributes && await this.visit(jsImport.attributes, p);

        if (jsImport.initializer) {
            p.append("=");
            await this.visitLeftPadded(jsImport.initializer, p);
        }

        await this.afterSyntax(jsImport, p);
        return jsImport;
    }

    override async visitImportClause(jsImportClause: JS.ImportClause, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(jsImportClause, p);

        if (jsImportClause.typeOnly) {
            p.append("type");
        }

        if (jsImportClause.name) {
            await this.visitRightPadded(jsImportClause.name, p);

            if (jsImportClause.namedBindings) {
                p.append(",");
            }
        }

        jsImportClause.namedBindings && await this.visit(jsImportClause.namedBindings, p);

        await this.afterSyntax(jsImportClause, p);

        return jsImportClause;
    }

    override async visitTypeTreeExpression(typeTreeExpression: JS.TypeTreeExpression, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(typeTreeExpression, p);
        await this.visit(typeTreeExpression.expression, p);
        await this.afterSyntax(typeTreeExpression, p);
        return typeTreeExpression;
    }

    override async visitNamespaceDeclaration(namespaceDeclaration: JS.NamespaceDeclaration, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(namespaceDeclaration, p);
        for (const it of namespaceDeclaration.modifiers) {
            await this.visitModifier(it, p);
        }
        await this.visitSpace(namespaceDeclaration.keywordType.before, p);

        switch (namespaceDeclaration.keywordType.element) {
            case JS.NamespaceDeclaration.KeywordType.Namespace:
                p.append("namespace");
                break;
            case JS.NamespaceDeclaration.KeywordType.Module:
                p.append("module");
                break;
            default:
                break;
        }

        await this.visitRightPadded(namespaceDeclaration.name, p);

        if (namespaceDeclaration.body) {
            await this.visit(namespaceDeclaration.body, p);
        }

        await this.afterSyntax(namespaceDeclaration, p);
        return namespaceDeclaration;
    }

    override async visitSatisfiesExpression(satisfiesExpression: JS.SatisfiesExpression, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(satisfiesExpression, p);
        await this.visit(satisfiesExpression.expression, p);
        await this.visitLeftPaddedLocal("satisfies", satisfiesExpression.satisfiesType, p);
        await this.afterSyntax(satisfiesExpression, p);
        return satisfiesExpression;
    }

    override async visitVoid(aVoid: JS.Void, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(aVoid, p);
        p.append("void");
        await this.visit(aVoid.expression, p);
        await this.afterSyntax(aVoid, p);
        return aVoid;
    }

    override async visitYield(aYield: J.Yield, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(aYield, p);

        p.append("yield");

        const delegated = findMarker<DelegatedYield>(aYield, JS.Markers.DelegatedYield);
        if (delegated) {
            await this.visitSpace(delegated.prefix, p);
            p.append("*");
        }

        aYield.value && await this.visit(aYield.value, p);

        await this.afterSyntax(aYield, p);
        return aYield;
    }

    override async visitTry(aTry: J.Try, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(aTry, p);
        p.append("try");
        await this.visit(aTry.body, p);
        await this.visitNodes(aTry.catches, p);
        aTry.finally && await this.visitLeftPaddedLocal("finally", aTry.finally, p);
        await this.afterSyntax(aTry, p);
        return aTry;
    }

    override async visitTryCatch(aCatch: J.Try.Catch, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(aCatch, p);
        p.append("catch");
        if (aCatch.parameter.tree.element.variables.length > 0) {
            await this.visit(aCatch.parameter, p);
        }
        await this.visit(aCatch.body, p);
        await this.afterSyntax(aCatch, p);
        return aCatch;
    }

    override async visitArrayDimension(arrayDimension: J.ArrayDimension, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(arrayDimension, p);
        p.append("[");
        await this.visitRightPaddedLocalSingle(arrayDimension.index, "]", p);
        await this.afterSyntax(arrayDimension, p);
        return arrayDimension;
    }

    override async visitArrayType(arrayType: J.ArrayType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(arrayType, p);
        let type: TypedTree = arrayType;

        while (type.kind === J.Kind.ArrayType) {
            type = (type as J.ArrayType).elementType;
        }

        await this.visit(type, p);
        await this.visitNodes(arrayType.annotations, p);

        if (arrayType.dimension) {
            await this.visitSpace(arrayType.dimension.before, p);
            p.append("[");
            await this.visitSpace(arrayType.dimension.element, p);
            p.append("]");

            if (arrayType.elementType.kind === J.Kind.ArrayType) {
                await this.printDimensions(arrayType.elementType as J.ArrayType, p);
            }
        }

        await this.afterSyntax(arrayType, p);
        return arrayType;
    }

    private async printDimensions(arrayType: J.ArrayType, p: PrintOutputCapture) {
        await this.beforeSyntax(arrayType, p);
        await this.visitNodes(arrayType.annotations, p);
        await this.visitSpace(arrayType.dimension?.before ?? emptySpace, p);

        p.append("[");
        await this.visitSpace(arrayType.dimension?.element ?? emptySpace, p);
        p.append("]");

        if (arrayType.elementType.kind === J.Kind.ArrayType) {
            await this.printDimensions(arrayType.elementType as J.ArrayType, p);
        }

        await this.afterSyntax(arrayType, p);
    }

    override async visitTernary(ternary: J.Ternary, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(ternary, p);
        await this.visit(ternary.condition, p);
        await this.visitLeftPaddedLocal("?", ternary.truePart, p);
        await this.visitLeftPaddedLocal(":", ternary.falsePart, p);
        await this.afterSyntax(ternary, p);
        return ternary;
    }

    override async visitThrow(thrown: J.Throw, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(thrown, p);
        p.append("throw");
        await this.visit(thrown.exception, p);
        await this.afterSyntax(thrown, p);
        return thrown;
    }

    override async visitIf(iff: J.If, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(iff, p);
        p.append("if");
        await this.visit(iff.ifCondition, p);
        await this.visitStatementLocal(iff.thenPart, p);
        iff.elsePart && await this.visit(iff.elsePart, p);
        await this.afterSyntax(iff, p);
        return iff;
    }

    override async visitElse(else_: J.If.Else, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(else_, p);
        p.append("else");
        await this.visitStatementLocal(else_.body, p);
        await this.afterSyntax(else_, p);
        return else_;
    }

    override async visitDoWhileLoop(doWhileLoop: J.DoWhileLoop, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(doWhileLoop, p);
        p.append("do");
        await this.visitStatementLocal(doWhileLoop.body, p);
        await this.visitLeftPaddedLocal("while", doWhileLoop.whileCondition, p);
        await this.afterSyntax(doWhileLoop, p);
        return doWhileLoop;
    }

    override async visitWhileLoop(whileLoop: J.WhileLoop, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(whileLoop, p);
        p.append("while");
        await this.visit(whileLoop.condition, p);
        await this.visitStatementLocal(whileLoop.body, p);
        await this.afterSyntax(whileLoop, p);
        return whileLoop;
    }

    override async visitInstanceOf(instanceOf: J.InstanceOf, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(instanceOf, p);
        await this.visitRightPaddedLocalSingle(instanceOf.expression, "instanceof", p);
        await this.visit(instanceOf.class, p);
        instanceOf.pattern && await this.visit(instanceOf.pattern, p);
        await this.afterSyntax(instanceOf, p);
        return instanceOf;
    }

    override async visitLiteral(literal: J.Literal, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(literal, p);

        const unicodeEscapes = literal.unicodeEscapes;
        if (!unicodeEscapes) {
            p.append(literal.valueSource!);
        } else if (literal.valueSource) {
            const surrogateIter = unicodeEscapes[Symbol.iterator]();
            let surrogate = surrogateIter.next().value ?? null;
            let i = 0;

            if (surrogate && surrogate.valueSourceIndex === 0) {
                p.append("\\u").append(surrogate.codePoint);
                surrogate = surrogateIter.next().value ?? null;
            }

            const valueSource = literal.valueSource;
            for (let j = 0; j < valueSource.length; j++) {
                const c = valueSource[j];
                p.append(c);

                if (surrogate && surrogate.valueSourceIndex === ++i) {
                    while (surrogate && surrogate.valueSourceIndex === i) {
                        p.append("\\u").append(surrogate.codePoint);
                        surrogate = surrogateIter.next().value ?? null;
                    }
                }
            }
        }

        await this.afterSyntax(literal, p);
        return literal;
    }

    override async visitScopedVariableDeclarations(variableDeclarations: JS.ScopedVariableDeclarations, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(variableDeclarations, p);

        for (const m of variableDeclarations.modifiers) {
            await this.visitModifier(m, p);
        }

        await this.visitRightPaddedLocal(variableDeclarations.variables, ",", p);

        await this.afterSyntax(variableDeclarations, p);
        return variableDeclarations;
    }

    override async visitShebang(shebang: JS.Shebang, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(shebang, p);
        p.append(shebang.text);
        await this.afterSyntax(shebang, p);
        return shebang;
    }

    override async visitVariableDeclarations(multiVariable: J.VariableDeclarations, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(multiVariable, p);
        await this.visitNodes(multiVariable.leadingAnnotations, p);

        for (const it of multiVariable.modifiers) {
            await this.visitModifier(it, p);
        }

        const variables = multiVariable.variables;
        for (let i = 0; i < variables.length; i++) {
            const variable = variables[i];

            await this.beforeSyntax(variable.element, p);

            if (multiVariable.varargs) {
                p.append("...");
            }

            await this.visit(variable.element.name, p);
            // print non-null assertions or optional
            await this.postVisit(variable.element, p);

            await this.visitSpace(variable.after, p);

            if (multiVariable.typeExpression) {
                await this.visit(multiVariable.typeExpression, p);
            }

            if (variable.element.initializer) {
                await this.visitLeftPaddedLocal("=", variable.element.initializer, p);
            }

            await this.afterSyntax(variable.element, p);

            if (i < variables.length - 1) {
                p.append(",");
            } else if (findMarker(variable, J.Markers.Semicolon)) {
                p.append(";");
            }
        }

        await this.afterSyntax(multiVariable, p);
        return multiVariable;
    }

    override async visitVariable(variable: J.VariableDeclarations.NamedVariable, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(variable, p);
        await this.visit(variable.name, p);

        variable.initializer && await this.visitLeftPaddedLocal("=", variable.initializer, p);

        await this.afterSyntax(variable, p);
        return variable;
    }

    override async visitIdentifier(ident: J.Identifier, p: PrintOutputCapture): Promise<J | undefined> {
        await this.visitSpace(emptySpace, p);
        await this.visitNodes(ident.annotations, p);
        await this.beforeSyntax(ident, p);
        p.append(ident.simpleName);
        await this.afterSyntax(ident, p);
        return ident;
    }

    override async visitBlock(block: J.Block, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(block, p);

        if (block.static.element) {
            p.append("static");
            await this.visitRightPadded(block.static, p);
        }

        p.append("{");
        await this.visitStatements(block.statements, p);
        await this.visitSpace(block.end, p);
        p.append("}");

        await this.afterSyntax(block, p);
        return block;
    }

    override async visitTypeInfo(typeInfo: JS.TypeInfo, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(typeInfo, p);
        p.append(":");
        await this.visit(typeInfo.typeIdentifier, p);
        await this.afterSyntax(typeInfo, p);
        return typeInfo;
    }

    override async visitReturn(return_: J.Return, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(return_, p);
        p.append("return");
        return_.expression && await this.visit(return_.expression, p);
        await this.afterSyntax(return_, p);
        return return_;
    }

    override async visitModifier(mod: J.Modifier, p: PrintOutputCapture): Promise<J | undefined> {
        await this.visitNodes(mod.annotations, p);

        let keyword: string;
        switch (mod.type) {
            case J.ModifierType.Default:
                keyword = "default";
                break;
            case J.ModifierType.Public:
                keyword = "public";
                break;
            case J.ModifierType.Protected:
                keyword = "protected";
                break;
            case J.ModifierType.Private:
                keyword = "private";
                break;
            case J.ModifierType.Abstract:
                keyword = "abstract";
                break;
            case J.ModifierType.Async:
                keyword = "async";
                break;
            case J.ModifierType.Static:
                keyword = "static";
                break;
            case J.ModifierType.Final:
                keyword = "const";
                break;
            case J.ModifierType.Native:
                keyword = "native";
                break;
            case J.ModifierType.NonSealed:
                keyword = "non-sealed";
                break;
            case J.ModifierType.Sealed:
                keyword = "sealed";
                break;
            case J.ModifierType.Strictfp:
                keyword = "strictfp";
                break;
            case J.ModifierType.Synchronized:
                keyword = "synchronized";
                break;
            case J.ModifierType.Transient:
                keyword = "transient";
                break;
            case J.ModifierType.Volatile:
                keyword = "volatile";
                break;
            default:
                keyword = mod.keyword ?? "";
        }

        await this.beforeSyntax(mod, p);
        p.append(keyword);
        await this.afterSyntax(mod, p);
        return mod;
    }

    override async visitFunctionCall(functionCall: JS.FunctionCall, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(functionCall, p);

        if (functionCall.function) {
            await this.visitRightPadded(functionCall.function, p);
            if (functionCall.function.element.markers.markers.find(m => m.kind === JS.Markers.Optional)) {
                p.append("?.");
            }
        }

        functionCall.typeParameters && await this.visitContainerLocal("<", functionCall.typeParameters, ",", ">", p);
        await this.visitContainerLocal("(", functionCall.arguments, ",", ")", p);

        await this.afterSyntax(functionCall, p);
        return functionCall;
    }

    override async visitFunctionType(functionType: JS.FunctionType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(functionType, p);
        for (const m of functionType.modifiers) {
            await this.visitModifier(m, p);
        }
        if (functionType.constructorType.element) {
            await this.visitLeftPaddedLocal("new", functionType.constructorType, p);
        }
        const typeParameters = functionType.typeParameters;
        if (typeParameters) {
            await this.visitNodes(typeParameters.annotations, p);
            await this.visitSpace(typeParameters.prefix, p);
            await this.visitMarkers(typeParameters.markers, p);
            p.append("<");
            await this.visitRightPaddedLocal(typeParameters.typeParameters, ",", p);
            p.append(">");
        }

        await this.visitContainerLocal("(", functionType.parameters, ",", ")", p);
        await this.visitLeftPaddedLocal("=>", functionType.returnType, p);

        await this.afterSyntax(functionType, p);
        return functionType;
    }

    override async visitClassDeclaration(classDecl: J.ClassDeclaration, p: PrintOutputCapture): Promise<J | undefined> {
        let kind = "";
        switch (classDecl.classKind.type) {
            case J.ClassDeclaration.Kind.Type.Class:
                kind = "class";
                break;
            case J.ClassDeclaration.Kind.Type.Enum:
                kind = "enum";
                break;
            case J.ClassDeclaration.Kind.Type.Interface:
                kind = "interface";
                break;
            case J.ClassDeclaration.Kind.Type.Annotation:
                kind = "@interface";
                break;
            case J.ClassDeclaration.Kind.Type.Record:
                kind = "record";
                break;
        }

        await this.beforeSyntax(classDecl, p);
        await this.visitSpace(emptySpace, p);
        await this.visitNodes(classDecl.leadingAnnotations, p);
        for (const m of classDecl.modifiers) {
            await this.visitModifier(m, p);
        }
        await this.visitNodes(classDecl.classKind.annotations, p);
        await this.visitSpace(classDecl.classKind.prefix, p);
        p.append(kind);
        await this.visit(classDecl.name, p);
        classDecl.typeParameters && await this.visitContainerLocal("<", classDecl.typeParameters, ",", ">", p);
        classDecl.primaryConstructor && await this.visitContainerLocal("(", classDecl.primaryConstructor, ",", ")", p);
        classDecl.extends && await this.visitLeftPaddedLocal("extends", classDecl.extends, p);
        classDecl.implements && await this.visitContainerLocal(classDecl.classKind.type === J.ClassDeclaration.Kind.Type.Interface ? "extends" : "implements",
            classDecl.implements, ",", null, p);
        await this.visit(classDecl.body, p);
        await this.afterSyntax(classDecl, p);

        return classDecl;
    }

    override async visitMethodDeclaration(method: J.MethodDeclaration, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(method, p);
        await this.visitSpace(emptySpace, p);
        await this.visitNodes(method.leadingAnnotations, p);
        for (const m of method.modifiers) {
            await this.visitModifier(m, p);
        }

        let m;
        if ((m = findMarker<FunctionDeclaration>(method, JS.Markers.FunctionDeclaration))) {
            await this.visitSpace(m.prefix, p);
            p.append("function");
        }

        const asterisk = findMarker<Generator>(method, JS.Markers.Generator);
        if (asterisk) {
            await this.visitSpace(asterisk.prefix, p);
            p.append("*");
        }

        await this.visit(method.name, p);

        const typeParameters = method.typeParameters;
        if (typeParameters) {
            await this.visitNodes(typeParameters.annotations, p);
            await this.visitSpace(typeParameters.prefix, p);
            await this.visitMarkers(typeParameters.markers, p);
            p.append("<");
            await this.visitRightPaddedLocal(typeParameters.typeParameters, ",", p);
            p.append(">");
        }

        await this.visitContainerLocal("(", method.parameters, ",", ")", p);

        if (method.returnTypeExpression) {
            await this.visit(method.returnTypeExpression, p);
        }

        method.body && await this.visit(method.body, p);
        await this.afterSyntax(method, p);
        return method;
    }

    override async visitComputedPropertyMethodDeclaration(method: JS.ComputedPropertyMethodDeclaration, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(method, p);
        await this.visitSpace(emptySpace, p);
        await this.visitNodes(method.leadingAnnotations, p);
        for (const it of method.modifiers) {
            await this.visitModifier(it, p);
        }

        const generator = findMarker<Generator>(method, JS.Markers.Generator);
        if (generator) {
            await this.visitSpace(generator.prefix, p);
            p.append("*");
        }

        await this.visit(method.name, p);

        const typeParameters = method.typeParameters;
        if (typeParameters) {
            await this.visitNodes(typeParameters.annotations, p);
            await this.visitSpace(typeParameters.prefix, p);
            await this.visitMarkers(typeParameters.markers, p);
            p.append("<");
            await this.visitRightPaddedLocal(typeParameters.typeParameters, ",", p);
            p.append(">");
        }

        await this.visitContainerLocal("(", method.parameters, ",", ")", p);
        if (method.returnTypeExpression) {
            await this.visit(method.returnTypeExpression, p);
        }

        method.body && await this.visit(method.body, p);
        await this.afterSyntax(method, p);
        return method;
    }

    override async visitMethodInvocation(method: J.MethodInvocation, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(method, p);

        if (method.name.simpleName.length === 0) {
            method.select && await this.visitRightPadded(method.select, p);
        } else {
            if (method.select) {
                await this.visitRightPadded(method.select, p);
                if (!method.select.element.markers.markers.find(m => m.kind === JS.Markers.Optional)) {
                    p.append(".");
                } else {
                    p.append("?.");
                }
            }
            await this.visit(method.name, p);
        }

        method.typeParameters && await this.visitContainerLocal("<", method.typeParameters, ",", ">", p);
        await this.visitContainerLocal("(", method.arguments, ",", ")", p);

        await this.afterSyntax(method, p);
        return method;
    }

    override async visitTypeParameter(typeParameter: J.TypeParameter, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(typeParameter, p);
        await this.visitNodes(typeParameter.annotations, p);
        for (const m of typeParameter.modifiers) {
            await this.visitModifier(m, p);
        }
        await this.visit(typeParameter.name, p);

        const bounds = typeParameter.bounds;
        if (bounds) {
            await this.visitSpace(bounds.before, p);
            const constraintType = bounds.elements[0];

            if (!(constraintType.element.kind === J.Kind.Empty)) {
                p.append("extends");
                await this.visitRightPadded(constraintType, p);
            }

            const defaultType = bounds.elements[1];
            if (!(defaultType.element.kind === J.Kind.Empty)) {
                p.append("=");
                await this.visitRightPadded(defaultType, p);
            }
        }

        await this.afterSyntax(typeParameter, p);
        return typeParameter;
    }

    override async visitArrowFunction(arrowFunction: JS.ArrowFunction, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(arrowFunction, p);
        await this.visitNodes(arrowFunction.leadingAnnotations, p);
        for (const m of arrowFunction.modifiers) {
            await this.visitModifier(m, p);
        }

        const typeParameters = arrowFunction.typeParameters;
        if (typeParameters) {
            await this.visitNodes(typeParameters.annotations, p);
            await this.visitSpace(typeParameters.prefix, p);
            await this.visitMarkers(typeParameters.markers, p);
            p.append("<");
            await this.visitRightPaddedLocal(typeParameters.typeParameters, ",", p);
            p.append(">");
        }

        const lambda = arrowFunction.lambda;

        if (lambda.parameters.parenthesized) {
            await this.visitSpace(lambda.parameters.prefix, p);
            p.append("(");
            await this.visitRightPaddedLocal(lambda.parameters.parameters, ",", p);
            p.append(")");
        } else {
            await this.visitRightPaddedLocal(lambda.parameters.parameters, ",", p);
        }

        if (arrowFunction.returnTypeExpression) {
            await this.visit(arrowFunction.returnTypeExpression, p);
        }

        await this.visitSpace(lambda.arrow, p);
        p.append("=>");
        await this.visit(lambda.body, p);

        await this.afterSyntax(arrowFunction, p);
        return arrowFunction;
    }

    override async visitConditionalType(conditionalType: JS.ConditionalType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(conditionalType, p);
        await this.visit(conditionalType.checkType, p);
        await this.visitLeftPaddedLocal("extends", conditionalType.condition, p);
        await this.afterSyntax(conditionalType, p);
        return conditionalType;
    }

    override async visitExpressionWithTypeArguments(type: JS.ExpressionWithTypeArguments, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(type, p);
        await this.visit(type.clazz, p);
        type.typeArguments && await this.visitContainerLocal("<", type.typeArguments, ",", ">", p);
        await this.afterSyntax(type, p);
        return type;
    }

    override async visitImportType(importType: JS.ImportType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(importType, p);

        if (importType.hasTypeof.element) {
            p.append("typeof");
            await this.visitRightPadded(importType.hasTypeof, p);
        }

        p.append("import");
        await this.visitContainerLocal("(", importType.argumentAndAttributes, ",", ")", p);
        importType.qualifier && await this.visitLeftPaddedLocal(".", importType.qualifier, p);
        importType.typeArguments && await this.visitContainerLocal("<", importType.typeArguments, ",", ">", p);

        await this.afterSyntax(importType, p);
        return importType;
    }

    override async visitTypeDeclaration(typeDeclaration: JS.TypeDeclaration, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(typeDeclaration, p);

        for (const m of typeDeclaration.modifiers) {
            await this.visitModifier(m, p);
        }

        await this.visitLeftPaddedLocal("type", typeDeclaration.name, p);

        const typeParameters = typeDeclaration.typeParameters;
        if (typeParameters) {
            await this.visitNodes(typeParameters.annotations, p);
            await this.visitSpace(typeParameters.prefix, p);
            await this.visitMarkers(typeParameters.markers, p);
            p.append("<");
            await this.visitRightPaddedLocal(typeParameters.typeParameters, ",", p);
            p.append(">");
        }

        await this.visitLeftPaddedLocal("=", typeDeclaration.initializer, p);

        await this.afterSyntax(typeDeclaration, p);
        return typeDeclaration;
    }

    override async visitLiteralType(literalType: JS.LiteralType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(literalType, p);
        await this.visit(literalType.literal, p);
        await this.afterSyntax(literalType, p);
        return literalType;
    }

    override async visitNamedImports(namedImports: JS.NamedImports, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(namedImports, p);
        await this.visitContainerLocal("{", namedImports.elements, ",", "}", p);
        await this.afterSyntax(namedImports, p);
        return namedImports;
    }

    override async visitImportSpecifier(jis: JS.ImportSpecifier, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(jis, p);

        if (jis.importType.element) {
            await this.visitLeftPaddedLocal("type", jis.importType, p);
        }

        await this.visit(jis.specifier, p);

        await this.afterSyntax(jis, p);
        return jis;
    }

    override async visitExportDeclaration(ed: JS.ExportDeclaration, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(ed, p);
        p.append("export");
        for (const it of ed.modifiers) {
            await this.visitModifier(it, p);
        }

        if (ed.typeOnly.element) {
            await this.visitLeftPaddedLocal("type", ed.typeOnly, p);
        }

        ed.exportClause && await this.visit(ed.exportClause, p);
        ed.moduleSpecifier && await this.visitLeftPaddedLocal("from", ed.moduleSpecifier, p);
        ed.attributes && await this.visit(ed.attributes, p);

        await this.afterSyntax(ed, p);
        return ed;
    }

    override async visitExportAssignment(es: JS.ExportAssignment, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(es, p);
        p.append("export");
        await this.visitLeftPaddedLocal(es.exportEquals ? "=" : "default", es.expression, p);
        await this.afterSyntax(es, p);
        return es;
    }

    override async visitIndexedAccessType(iat: JS.IndexedAccessType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(iat, p);

        await this.visit(iat.objectType, p);
        // expect that this element is printed accordingly
        // <space_before>[<inner_space_before>index<inner_right_padded_suffix_space>]<right_padded_suffix_space>
        await this.visit(iat.indexType, p);

        await this.afterSyntax(iat, p);
        return iat;
    }

    override async visitIndexedAccessTypeIndexType(indexType: JS.IndexedAccessType.IndexType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(indexType, p);

        p.append("[");
        await this.visitRightPadded(indexType.element, p);
        p.append("]");

        await this.afterSyntax(indexType, p);
        return indexType;
    }

    override async visitWithStatement(withStatement: JS.WithStatement, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(withStatement, p);
        p.append("with");
        await this.visit(withStatement.expression, p);
        await this.visitRightPadded(withStatement.body, p);
        await this.afterSyntax(withStatement, p);
        return withStatement;
    }

    override async visitExportSpecifier(es: JS.ExportSpecifier, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(es, p);
        if (es.typeOnly.element) {
            await this.visitLeftPaddedLocal("type", es.typeOnly, p);
        }

        await this.visit(es.specifier, p);

        await this.afterSyntax(es, p);
        return es;
    }

    override async visitNamedExports(ne: JS.NamedExports, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(ne, p);
        await this.visitContainerLocal("{", ne.elements, ",", "}", p);
        await this.afterSyntax(ne, p);
        return ne;
    }

    override async visitImportAttributes(importAttributes: JS.ImportAttributes, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(importAttributes, p);

        p.append((importAttributes.token === JS.ImportAttributes.Token.With ? "with" : "assert"));
        await this.visitContainerLocal("{", importAttributes.elements, ",", "}", p);

        await this.afterSyntax(importAttributes, p);
        return importAttributes;
    }

    override async visitImportAttribute(importAttribute: JS.ImportAttribute, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(importAttribute, p);

        await this.visit(importAttribute.name, p);
        await this.visitLeftPaddedLocal(":", importAttribute.value, p);

        await this.afterSyntax(importAttribute, p);
        return importAttribute;
    }

    override async visitImportTypeAttributes(importAttributes: JS.ImportTypeAttributes, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(importAttributes, p);
        p.append("{");

        await this.visitRightPadded(importAttributes.token, p);
        p.append(":");
        await this.visitContainerLocal("{", importAttributes.elements, ",", "}", p);
        await this.visitSpace(importAttributes.end, p);

        p.append("}");
        await this.afterSyntax(importAttributes, p);
        return importAttributes;
    }

    override async visitArrayBindingPattern(abp: JS.ArrayBindingPattern, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(abp, p);
        await this.visitContainerLocal("[", abp.elements, ",", "]", p);
        await this.afterSyntax(abp, p);
        return abp;
    }

    override async visitMappedType(mappedType: JS.MappedType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(mappedType, p);
        p.append("{");

        if (mappedType.prefixToken) {
            await this.visitLeftPadded(mappedType.prefixToken, p);
        }

        if (mappedType.hasReadonly.element) {
            await this.visitLeftPaddedLocal("readonly", mappedType.hasReadonly, p);
        }

        await this.visitMappedTypeKeysRemapping(mappedType.keysRemapping, p);

        if (mappedType.suffixToken) {
            await this.visitLeftPadded(mappedType.suffixToken, p);
        }

        if (mappedType.hasQuestionToken.element) {
            await this.visitLeftPaddedLocal("?", mappedType.hasQuestionToken, p);
        }

        const colon = mappedType.valueType.elements[0].element.kind === J.Kind.Empty ? "" : ":";
        await this.visitContainerLocal(colon, mappedType.valueType, "", "", p);

        p.append("}");
        await this.afterSyntax(mappedType, p);
        return mappedType;
    }

    override async visitMappedTypeKeysRemapping(mappedTypeKeys: JS.MappedType.KeysRemapping, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(mappedTypeKeys, p);
        p.append("[");
        await this.visitRightPadded(mappedTypeKeys.typeParameter, p);

        if (mappedTypeKeys.nameType) {
            p.append("as");
            await this.visitRightPadded(mappedTypeKeys.nameType, p);
        }

        p.append("]");
        await this.afterSyntax(mappedTypeKeys, p);
        return mappedTypeKeys;
    }

    override async visitMappedTypeParameter(mappedTypeParameter: JS.MappedType.Parameter, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(mappedTypeParameter, p);
        await this.visit(mappedTypeParameter.name, p);
        await this.visitLeftPaddedLocal("in", mappedTypeParameter.iterateType, p);
        await this.afterSyntax(mappedTypeParameter, p);
        return mappedTypeParameter;
    }

    override async visitObjectBindingPattern(objectBindingPattern: JS.ObjectBindingPattern, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(objectBindingPattern, p);
        await this.visitNodes(objectBindingPattern.leadingAnnotations, p);
        for (const m of objectBindingPattern.modifiers) {
            await this.visitModifier(m, p);
        }

        objectBindingPattern.typeExpression && await this.visit(objectBindingPattern.typeExpression, p);
        await this.visitContainerLocal("{", objectBindingPattern.bindings, ",", "}", p);
        objectBindingPattern.initializer && await this.visitLeftPaddedLocal("=", objectBindingPattern.initializer, p);
        await this.afterSyntax(objectBindingPattern, p);
        return objectBindingPattern;
    }

    override async visitTaggedTemplateExpression(taggedTemplateExpression: JS.TaggedTemplateExpression, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(taggedTemplateExpression, p);
        taggedTemplateExpression.tag && await this.visitRightPadded(taggedTemplateExpression.tag, p);
        taggedTemplateExpression.typeArguments && await this.visitContainerLocal("<", taggedTemplateExpression.typeArguments, ",", ">", p);
        await this.visit(taggedTemplateExpression.templateExpression, p);
        await this.afterSyntax(taggedTemplateExpression, p);
        return taggedTemplateExpression;
    }

    override async visitTemplateExpression(templateExpression: JS.TemplateExpression, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(templateExpression, p);
        await this.visit(templateExpression.head, p);
        await this.visitRightPaddedLocal(templateExpression.spans, "", p);
        await this.afterSyntax(templateExpression, p);
        return templateExpression;
    }

    override async visitTemplateExpressionSpan(value: JS.TemplateExpression.Span, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(value, p);
        await this.visit(value.expression, p);
        await this.visit(value.tail, p);
        await this.afterSyntax(value, p);
        return value;
    }

    override async visitTuple(tuple: JS.Tuple, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(tuple, p);
        await this.visitContainerLocal("[", tuple.elements, ",", "]", p);
        await this.afterSyntax(tuple, p);
        return tuple;
    }

    override async visitTypeQuery(typeQuery: JS.TypeQuery, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(typeQuery, p);
        p.append("typeof");
        await this.visit(typeQuery.typeExpression, p);
        typeQuery.typeArguments && await this.visitContainerLocal("<", typeQuery.typeArguments, ",", ">", p);
        await this.afterSyntax(typeQuery, p);
        return typeQuery;
    }

    override async visitTypeOf(typeOf: JS.TypeOf, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(typeOf, p);
        p.append("typeof");
        await this.visit(typeOf.expression, p);
        await this.afterSyntax(typeOf, p);
        return typeOf;
    }

    protected async visitComputedPropertyName(computedPropertyName: JS.ComputedPropertyName, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(computedPropertyName, p);
        p.append("[");
        await this.visitRightPaddedLocalSingle(computedPropertyName.expression, "]", p);
        await this.afterSyntax(computedPropertyName, p);
        return computedPropertyName;
    }

    override async visitTypeOperator(typeOperator: JS.TypeOperator, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(typeOperator, p);

        let keyword = "";
        if (typeOperator.operator === JS.TypeOperator.Type.ReadOnly) {
            keyword = "readonly";
        } else if (typeOperator.operator === JS.TypeOperator.Type.KeyOf) {
            keyword = "keyof";
        } else if (typeOperator.operator === JS.TypeOperator.Type.Unique) {
            keyword = "unique";
        }

        p.append(keyword);

        await this.visitLeftPadded(typeOperator.expression, p);

        await this.afterSyntax(typeOperator, p);
        return typeOperator;
    }

    override async visitTypePredicate(typePredicate: JS.TypePredicate, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(typePredicate, p);

        if (typePredicate.asserts.element) {
            await this.visitLeftPaddedLocal("asserts", typePredicate.asserts, p);
        }

        await this.visit(typePredicate.parameterName, p);
        typePredicate.expression && await this.visitLeftPaddedLocal("is", typePredicate.expression, p);

        await this.afterSyntax(typePredicate, p);
        return typePredicate;
    }

    override async visitIndexSignatureDeclaration(isd: JS.IndexSignatureDeclaration, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(isd, p);

        for (const m of isd.modifiers) {
            await this.visitModifier(m, p);
        }
        await this.visitContainerLocal("[", isd.parameters, "", "]", p);
        await this.visitLeftPaddedLocal(":", isd.typeExpression, p);

        await this.afterSyntax(isd, p);
        return isd;
    }

    override async visitAnnotation(annotation: J.Annotation, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(annotation, p);

        p.append("@");
        await this.visit(annotation.annotationType, p);
        annotation.arguments && await this.visitContainerLocal("(", annotation.arguments, ",", ")", p);

        await this.afterSyntax(annotation, p);
        return annotation;
    }

    override async visitNewArray(newArray: J.NewArray, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(newArray, p);
        newArray.typeExpression && await this.visit(newArray.typeExpression, p);
        await this.visitNodes(newArray.dimensions, p);
        newArray.initializer && await this.visitContainerLocal("[", newArray.initializer, ",", "]", p);
        await this.afterSyntax(newArray, p);
        return newArray;
    }

    override async visitNewClass(newClass: J.NewClass, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(newClass, p);
        newClass.enclosing && await this.visitRightPaddedLocalSingle(newClass.enclosing, ".", p);
        await this.visitSpace(newClass.new, p);

        if (newClass.class) {
            p.append("new");
            await this.visit(newClass.class, p);

            if (!newClass.arguments.markers.markers.find(m => m.kind === J.Markers.OmitParentheses)) {
                await this.visitContainerLocal("(", newClass.arguments, ",", ")", p);
            }
        }

        newClass.body && await this.visit(newClass.body, p);
        await this.afterSyntax(newClass, p);
        return newClass;
    }

    override async visitSwitch(switch_: J.Switch, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(switch_, p);
        p.append("switch");
        await this.visit(switch_.selector, p);
        await this.visit(switch_.cases, p);
        await this.afterSyntax(switch_, p);
        return switch_;
    }

    override async visitCase(case_: J.Case, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(case_, p);

        const elem = case_.caseLabels.elements[0].element;
        if (elem.kind !== J.Kind.Identifier || (elem as J.Identifier).simpleName !== "default") {
            p.append("case");
        }

        await this.visitContainerLocal("", case_.caseLabels, ",", "", p);

        await this.visitSpace(case_.statements.before, p);
        p.append(case_.type === J.Case.Type.Statement ? ":" : "->");

        await this.visitStatements(case_.statements.elements, p);

        await this.afterSyntax(case_, p);
        return case_;
    }

    override async visitLabel(label: J.Label, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(label, p);
        await this.visitRightPaddedLocalSingle(label.label, ":", p);
        await this.visit(label.statement, p);
        await this.afterSyntax(label, p);
        return label;
    }

    override async visitContinue(continueStatement: J.Continue, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(continueStatement, p);
        p.append("continue");
        continueStatement.label && await this.visit(continueStatement.label, p);
        await this.afterSyntax(continueStatement, p);
        return continueStatement;
    }

    override async visitBreak(breakStatement: J.Break, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(breakStatement, p);
        p.append("break");
        breakStatement.label && await this.visit(breakStatement.label, p);
        await this.afterSyntax(breakStatement, p);
        return breakStatement;
    }

    override async visitFieldAccess(fieldAccess: J.FieldAccess, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(fieldAccess, p);
        await this.visit(fieldAccess.target, p);

        await this.visitLeftPaddedLocal(".", fieldAccess.name, p);
        await this.afterSyntax(fieldAccess, p);
        return fieldAccess;
    }

    override async visitTypeLiteral(tl: JS.TypeLiteral, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(tl, p);

        await this.visit(tl.members, p);

        await this.afterSyntax(tl, p);
        return tl;
    }

    override async visitParentheses<T extends J>(parens: J.Parentheses<T>, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(parens, p);
        p.append('(');
        await this.visitRightPaddedLocalSingle(parens.tree, ")", p);
        await this.afterSyntax(parens, p);
        return parens;
    }

    override async visitParameterizedType(type: J.ParameterizedType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(type, p);
        await this.visit(type.class, p);
        type.typeParameters && await this.visitContainerLocal("<", type.typeParameters, ",", ">", p);
        await this.afterSyntax(type, p);
        return type;
    }

    override async visitAs(as_: JS.As, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(as_, p);
        await this.visitRightPadded(as_.left, p);
        p.append("as");
        await this.visit(as_.right, p);
        await this.afterSyntax(as_, p);

        return as_;
    }

    override async visitAssignment(assignment: J.Assignment, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(assignment, p);
        await this.visit(assignment.variable, p);
        await this.visitLeftPaddedLocal("=", assignment.assignment, p);
        await this.afterSyntax(assignment, p);
        return assignment;
    }

    override async visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(propertyAssignment, p);

        await this.visitRightPadded(propertyAssignment.name, p);

        if (propertyAssignment.initializer) {
            // if the property is not null, we should print it like `{ a: b }`
            // otherwise, it is a shorthand assignment where we have stuff like `{ a }` only
            if (propertyAssignment.assigmentToken === JS.PropertyAssignment.Token.Colon) {
                p.append(':');
            } else if (propertyAssignment.assigmentToken === JS.PropertyAssignment.Token.Equals) {
                p.append('=');
            }
            await this.visit(propertyAssignment.initializer, p);
        }

        await this.afterSyntax(propertyAssignment, p);
        return propertyAssignment;
    }

    override async visitAssignmentOperation(assignOp: J.AssignmentOperation, p: PrintOutputCapture): Promise<J | undefined> {
        let keyword = "";
        switch (assignOp.operator.element) {
            case J.AssignmentOperation.Type.Addition:
                keyword = "+=";
                break;
            case J.AssignmentOperation.Type.Subtraction:
                keyword = "-=";
                break;
            case J.AssignmentOperation.Type.Multiplication:
                keyword = "*=";
                break;
            case J.AssignmentOperation.Type.Division:
                keyword = "/=";
                break;
            case J.AssignmentOperation.Type.Modulo:
                keyword = "%=";
                break;
            case J.AssignmentOperation.Type.BitAnd:
                keyword = "&=";
                break;
            case J.AssignmentOperation.Type.BitOr:
                keyword = "|=";
                break;
            case J.AssignmentOperation.Type.BitXor:
                keyword = "^=";
                break;
            case J.AssignmentOperation.Type.LeftShift:
                keyword = "<<=";
                break;
            case J.AssignmentOperation.Type.RightShift:
                keyword = ">>=";
                break;
            case J.AssignmentOperation.Type.UnsignedRightShift:
                keyword = ">>>=";
                break;
        }

        await this.beforeSyntax(assignOp, p);
        await this.visit(assignOp.variable, p);
        await this.visitSpace(assignOp.operator.before, p);
        p.append(keyword);
        await this.visit(assignOp.assignment, p);
        await this.afterSyntax(assignOp, p);

        return assignOp;
    }

    override async visitAssignmentOperationExtensions(assignOp: JS.AssignmentOperation, p: PrintOutputCapture): Promise<J | undefined> {
        let keyword = "";
        switch (assignOp.operator.element) {
            case JS.AssignmentOperation.Type.QuestionQuestion:
                keyword = "??=";
                break;
            case JS.AssignmentOperation.Type.And:
                keyword = "&&=";
                break;
            case JS.AssignmentOperation.Type.Or:
                keyword = "||=";
                break;
            case JS.AssignmentOperation.Type.Power:
                keyword = "**";
                break;
            case JS.AssignmentOperation.Type.Exp:
                keyword = "**=";
                break;
        }

        await this.beforeSyntax(assignOp, p);
        await this.visit(assignOp.variable, p);
        await this.visitSpace(assignOp.operator.before, p);
        p.append(keyword);
        await this.visit(assignOp.assignment, p);
        await this.afterSyntax(assignOp, p);

        return assignOp;
    }

    override async visitEnumValue(enum_: J.EnumValue, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(enum_, p);
        await this.visit(enum_.name, p);

        const initializer = enum_.initializer;
        if (initializer) {
            await this.visitSpace(initializer.prefix, p);
            p.append("=");
            // There can be only one argument
            const expression = initializer.arguments.elements[0];
            await this.visitRightPadded(expression, p);
            return enum_;
        }

        await this.afterSyntax(enum_, p);
        return enum_;
    }

    override async visitEnumValueSet(enums: J.EnumValueSet, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(enums, p);
        await this.visitRightPaddedLocal(enums.enums, ",", p);

        if (enums.terminatedWithSemicolon) {
            p.append(",");
        }

        await this.afterSyntax(enums, p);
        return enums;
    }

    override async visitBinary(binary: J.Binary, p: PrintOutputCapture): Promise<J | undefined> {
        let keyword = "";
        switch (binary.operator.element) {
            case J.Binary.Type.Addition:
                keyword = "+";
                break;
            case J.Binary.Type.Subtraction:
                keyword = "-";
                break;
            case J.Binary.Type.Multiplication:
                keyword = "*";
                break;
            case J.Binary.Type.Division:
                keyword = "/";
                break;
            case J.Binary.Type.Modulo:
                keyword = "%";
                break;
            case J.Binary.Type.LessThan:
                keyword = "<";
                break;
            case J.Binary.Type.GreaterThan:
                keyword = ">";
                break;
            case J.Binary.Type.LessThanOrEqual:
                keyword = "<=";
                break;
            case J.Binary.Type.GreaterThanOrEqual:
                keyword = ">=";
                break;
            case J.Binary.Type.Equal:
                keyword = "==";
                break;
            case J.Binary.Type.NotEqual:
                keyword = "!=";
                break;
            case J.Binary.Type.BitAnd:
                keyword = "&";
                break;
            case J.Binary.Type.BitOr:
                keyword = "|";
                break;
            case J.Binary.Type.BitXor:
                keyword = "^";
                break;
            case J.Binary.Type.LeftShift:
                keyword = "<<";
                break;
            case J.Binary.Type.RightShift:
                keyword = ">>";
                break;
            case J.Binary.Type.UnsignedRightShift:
                keyword = ">>>";
                break;
            case J.Binary.Type.Or:
                keyword = "||";
                break;
            case J.Binary.Type.And:
                keyword = "&&";
                break;
        }

        await this.beforeSyntax(binary, p);
        await this.visit(binary.left, p);
        await this.visitSpace(binary.operator.before, p);
        p.append(keyword);
        await this.visit(binary.right, p);
        await this.afterSyntax(binary, p);

        return binary;
    }

    override async visitBinaryExtensions(binary: JS.Binary, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(binary, p);

        await this.visit(binary.left, p);
        let keyword = "";

        switch (binary.operator.element) {
            case JS.Binary.Type.IdentityEquals:
                keyword = "===";
                break;
            case JS.Binary.Type.IdentityNotEquals:
                keyword = "!==";
                break;
            case JS.Binary.Type.In:
                keyword = "in";
                break;
            case JS.Binary.Type.QuestionQuestion:
                keyword = "??";
                break;
            case JS.Binary.Type.Comma:
                keyword = ",";
                break;
        }

        await this.visitSpace(binary.operator.before, p);
        p.append(keyword);

        await this.visit(binary.right, p);

        await this.afterSyntax(binary, p);
        return binary;
    }

    override async visitUnary(unary: J.Unary, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(unary, p);
        switch (unary.operator.element) {
            case J.Unary.Type.PreIncrement:
                p.append("++");
                await this.visit(unary.expression, p);
                break;
            case J.Unary.Type.PreDecrement:
                p.append("--");
                await this.visit(unary.expression, p);
                break;
            case J.Unary.Type.PostIncrement:
                await this.visit(unary.expression, p);
                await this.visitSpace(unary.operator.before, p);
                p.append("++");
                break;
            case J.Unary.Type.PostDecrement:
                await this.visit(unary.expression, p);
                await this.visitSpace(unary.operator.before, p);
                p.append("--");
                break;
            case J.Unary.Type.Positive:
                p.append('+');
                await this.visit(unary.expression, p);
                break;
            case J.Unary.Type.Negative:
                p.append('-');
                await this.visit(unary.expression, p);
                break;
            case J.Unary.Type.Complement:
                p.append('~');
                await this.visit(unary.expression, p);
                break;
            case J.Unary.Type.Not:
            default:
                p.append('!');
                await this.visit(unary.expression, p);
        }
        await this.afterSyntax(unary, p);
        return unary;
    }

    override async visitUnion(union: JS.Union, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(union, p);

        await this.visitRightPaddedLocal(union.types, "|", p);

        await this.afterSyntax(union, p);
        return union;
    }

    override async visitIntersection(intersection: JS.Intersection, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(intersection, p);

        await this.visitRightPaddedLocal(intersection.types, "&", p);

        await this.afterSyntax(intersection, p);
        return intersection;
    }

    override async visitForLoop(forLoop: J.ForLoop, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(forLoop, p);
        p.append("for");
        const ctrl = forLoop.control;
        await this.visitSpace(ctrl.prefix, p);
        p.append('(');
        await this.visitRightPaddedLocal(ctrl.init, ",", p);
        p.append(';');
        ctrl.condition && await this.visitRightPaddedLocalSingle(ctrl.condition, ";", p);
        await this.visitRightPaddedLocal(ctrl.update, ",", p);
        p.append(')');
        await this.visitStatementLocal(forLoop.body, p);
        await this.afterSyntax(forLoop, p);
        return forLoop;
    }

    override async visitForOfLoop(loop: JS.ForOfLoop, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(loop, p);
        p.append("for");
        if (loop.await) {
            await this.visitSpace(loop.await, p);
            p.append("await");
        }

        const control = loop.loop.control;
        await this.visitSpace(control.prefix, p);
        p.append('(');
        await this.visitRightPadded(control.variable, p);
        p.append("of");
        await this.visitRightPadded(control.iterable, p);
        p.append(')');
        await this.visitRightPadded(loop.loop.body, p);
        await this.afterSyntax(loop, p);
        return loop;
    }

    override async visitForInLoop(loop: JS.ForInLoop, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(loop, p);
        p.append("for");

        const control = loop.control;
        await this.visitSpace(control.prefix, p);
        p.append('(');
        await this.visitRightPadded(control.variable, p);
        p.append("in");
        await this.visitRightPadded(control.iterable, p);
        p.append(')');
        await this.visitRightPadded(loop.body, p);
        await this.afterSyntax(loop, p);
        return loop;
    }

    // ---- print utils

    private async visitStatements(statements: J.RightPadded<Statement>[], p: PrintOutputCapture) {
        const objectLiteral =
            this.getParentCursor(0)?.value.kind === J.Kind.Block &&
            this.getParentCursor(1)?.value.kind === J.Kind.NewClass;

        for (let i = 0; i < statements.length; i++) {
            const paddedStat = statements[i];
            await this.visitStatementLocal(paddedStat, p);
            if (i < statements.length - 1 && objectLiteral) {
                p.append(",");
            }
        }
    }

    private async visitStatementLocal(paddedStat: J.RightPadded<Statement> | undefined, p: PrintOutputCapture) {
        if (paddedStat) {
            await this.visit(paddedStat.element, p);
            await this.visitSpace(paddedStat.after, p);
            await this.visitMarkers(paddedStat.markers, p);
        }
    }

    private getParentCursor(levels: number): Cursor | undefined {
        let cursor: Cursor | undefined = this.cursor;
        for (let i = 0; i < levels && cursor; i++) {
            cursor = cursor.parent;
        }

        return cursor;
    }

    protected async afterSyntax(j: J, p: PrintOutputCapture) {
        await this.afterSyntaxMarkers(j.markers, p);
    }

    private async afterSyntaxMarkers(markers: Markers, p: PrintOutputCapture) {
        for (const marker of markers.markers) {
            p.append(p.markerPrinter.afterSyntax(marker, new Cursor(marker, this.cursor), this.JAVA_SCRIPT_MARKER_WRAPPER));
        }
    }

    protected async beforeSyntax(j: J, p: PrintOutputCapture) {
        await this.beforeSyntaxExt(j.prefix, j.markers, p);
    }

    private async beforeSyntaxExt(prefix: J.Space, markers: Markers, p: PrintOutputCapture) {
        for (const marker of markers.markers) {
            p.append(p.markerPrinter.beforePrefix(marker, new Cursor(marker, this.cursor), this.JAVA_SCRIPT_MARKER_WRAPPER));
        }

        await this.visitSpace(prefix, p);
        await this.visitMarkers(markers, p);

        for (const marker of markers.markers) {
            p.append(p.markerPrinter.beforeSyntax(marker, new Cursor(marker, this.cursor), this.JAVA_SCRIPT_MARKER_WRAPPER));
        }
    }

    override async visitSpace(space: J.Space, p: PrintOutputCapture): Promise<J.Space> {
        p.append(space.whitespace!);

        const comments = space.comments;
        for (let i = 0; i < comments.length; i++) {
            const comment = comments[i];
            await this.visitMarkers(comment.markers, p);
            this.printComment(comment, this.cursor, p);
            p.append(comment.suffix);
        }

        return space;
    }

    private async visitRightPaddedLocal(nodes: J.RightPadded<J>[], suffixBetween: string, p: PrintOutputCapture) {
        for (let i = 0; i < nodes.length; i++) {
            const node = nodes[i];

            await this.visit(node.element, p);

            await this.visitSpace(node.after, p);
            await this.visitMarkers(node.markers, p);

            if (i < nodes.length - 1) {
                p.append(suffixBetween);
            }
        }
    }

    public async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, p: PrintOutputCapture): Promise<J.RightPadded<T>> {
        if (isTree(right.element)) {
            await this.visit(right.element, p);
        }

        await this.visitSpace(right.after, p);
        await this.visitMarkers(right.markers, p);
        return right;
    }

    private async visitRightPaddedLocalSingle(node: J.RightPadded<J> | undefined, suffix: string, p: PrintOutputCapture) {
        if (node) {
            await this.visit(node.element, p);

            await this.visitSpace(node.after, p);
            await this.visitMarkers(node.markers, p);

            p.append(suffix);
        }
    }

    private async visitLeftPaddedLocal(prefix: string | undefined, leftPadded: J.LeftPadded<J> | J.LeftPadded<boolean> | J.LeftPadded<string> | undefined, p: PrintOutputCapture) {
        if (leftPadded) {
            await this.beforeSyntaxExt(leftPadded.before, leftPadded.markers, p);

            if (prefix) {
                p.append(prefix);
            }

            if (typeof leftPadded.element === 'string') {
                p.append(leftPadded.element);
            } else if (typeof leftPadded.element !== 'boolean') {
                await this.visit(leftPadded.element, p);
            }

            await this.afterSyntaxMarkers(leftPadded.markers, p);
        }
    }

    private async visitContainerLocal(before: string, container: J.Container<J> | undefined, suffixBetween: string, after: string | null, p: PrintOutputCapture) {
        if (!container) {
            return;
        }

        await this.beforeSyntaxExt(container.before, container.markers, p);

        p.append(before);
        await this.visitRightPaddedLocal(container.elements, suffixBetween, p);
        p.append(after === null ? "" : after);
        await this.afterSyntaxMarkers(container.markers, p);
    }

    override async visitMarker<M extends Marker>(marker: M, p: PrintOutputCapture): Promise<M> {
        if (marker.kind === J.Markers.Semicolon) {
            p.append(';');
        }
        if (marker.kind === J.Markers.TrailingComma) {
            p.append(',');
            await this.visitSpace((marker as unknown as TrailingComma).suffix, p);
        }
        return marker;
    }

    protected async preVisit(tree: J, p: PrintOutputCapture): Promise<J | undefined> {
        // Note: Spread is now handled as JS.Spread AST element via visitSpread
        return tree;
    }

    protected async postVisit(tree: J, p: PrintOutputCapture): Promise<J | undefined> {
        for (const marker of tree.markers.markers) {
            if (marker.kind === JS.Markers.NonNullAssertion) {
                await this.visitSpace((marker as NonNullAssertion).prefix, p);
                p.append("!");
            }
            if (marker.kind === JS.Markers.Optional) {
                await this.visitSpace((marker as Optional).prefix, p);
                if (this.cursor.parent?.value?.kind !== J.Kind.MethodInvocation &&
                    this.cursor.parent?.value?.kind !== JS.Kind.FunctionCall) {
                    p.append("?");
                    if (this.cursor.parent?.value?.kind === J.Kind.ArrayAccess) {
                        p.append(".");
                    }
                }
            }
        }
        return tree;
    }

    private printComment(comment: Comment, cursor: Cursor, p: PrintOutputCapture): void {
        for (const marker of comment.markers.markers) {
            p.append(p.markerPrinter.beforeSyntax(marker, new Cursor(this, cursor), this.JAVA_SCRIPT_MARKER_WRAPPER));
        }

        if (comment.kind === J.Kind.TextComment) {
            const textComment = comment as TextComment;
            p.append(textComment.multiline ? `/*${textComment.text}*/` : `//${textComment.text}`);
        }

        for (const marker of comment.markers.markers) {
            p.append(p.markerPrinter.afterSyntax(marker, new Cursor(this, cursor), this.JAVA_SCRIPT_MARKER_WRAPPER));
        }
    }

    private async visitNodes<T extends Tree>(nodes: T[] | undefined, p: PrintOutputCapture): Promise<void> {
        if (nodes) {
            for (const node of nodes) {
                await this.visit(node, p);
            }
        }
    }

    override async visitControlParentheses<T extends J>(controlParens: J.ControlParentheses<T>, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(controlParens, p);

        if (this.getParentCursor(1)?.value.kind === J.Kind.TypeCast) {
            p.append('<');
            await this.visitRightPaddedLocalSingle(controlParens.tree, ">", p);
        } else {
            p.append('(');
            await this.visitRightPaddedLocalSingle(controlParens.tree, ")", p);
        }

        await this.afterSyntax(controlParens, p);
        return controlParens;
    }
}

TreePrinters.register(JS.Kind.CompilationUnit, () => new JavaScriptPrinter());
