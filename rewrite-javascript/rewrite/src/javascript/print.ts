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

    override visitJsCompilationUnit(cu: JS.CompilationUnit, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(cu, p);
    
        this.visitRightPaddedLocal(cu.statements, "", p);
    
        this.visitSpace(cu.eof, p);
        this.afterSyntax(cu, p);
        return cu;
    }

    override visitAlias(alias: JS.Alias, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(alias, p);
        this.visitRightPadded(alias.propertyName, p);
        p.append("as");
        this.visit(alias.alias, p);
        this.afterSyntax(alias, p);
        return alias;
    }

    override visitAwait(awaitExpr: JS.Await, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(awaitExpr, p);
        p.append("await");
        this.visit(awaitExpr.expression, p);
        this.afterSyntax(awaitExpr, p);
        return awaitExpr;
    }

    override visitBindingElement(binding: JS.BindingElement, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(binding, p);
        if (binding.propertyName) {
            this.visitRightPadded(binding.propertyName, p);
            p.append(":");
        }
        this.visit(binding.name, p);
        binding.initializer && this.visitLeftPaddedLocal("=", binding.initializer, p);
        this.afterSyntax(binding, p);
        return binding;
    }

    override visitDelete(del: JS.Delete, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(del, p);
        p.append("delete");
        this.visit(del.expression, p);
        this.afterSyntax(del, p);
        return del;
    }

    override visitExpressionStatement(statement: JS.ExpressionStatement, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(statement, p);
        this.visit(statement.expression, p);
        this.afterSyntax(statement, p);
        return statement;
    }

    override visitStatementExpression(statementExpression: JS.StatementExpression, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(statementExpression, p);
        this.visit(statementExpression.statement, p);
        this.afterSyntax(statementExpression, p);
        return statementExpression;
    }

    override visitSpread(spread: JS.Spread, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(spread, p);
        p.append("...");
        this.visit(spread.expression, p);
        this.afterSyntax(spread, p);
        return spread;
    }

    override visitInferType(inferType: JS.InferType, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(inferType, p);
        this.visitLeftPaddedLocal("infer", inferType.typeParameter, p);
        this.afterSyntax(inferType, p);
        return inferType;
    }

    override visitJsxTag(element: JSX.Tag, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(element, p);
        // Print < first, then the space after < (openName.before), then the tag name
        p.append("<");
        this.visitSpace(element.openName.before, p);
        this.visit(element.openName.element, p);
        if (element.typeArguments) {
            this.visitContainerLocal("<", element.typeArguments, ",", ">", p);
        }
        this.visitSpace(element.afterName, p);
        this.visitRightPaddedLocal(element.attributes, "", p);
    
        if (element.selfClosing) {
            this.visitSpace(element.selfClosing, p);
            p.append("/>");
        } else {
            p.append(">");
            if (element.children) {
                for (let i = 0; i < element.children.length; i++) {
                    this.visit(element.children[i], p)
                }
                // Print </ first, then the space after </ (closingName.before), then the tag name
                p.append("</");
                this.visitSpace(element.closingName!.before, p);
                this.visit(element.closingName!.element, p);
                this.visitSpace(element.afterClosingName, p);
                p.append(">");
            }
        }
    
        this.afterSyntax(element, p);
        return element;
    }

    override visitJsxAttribute(attribute: JSX.Attribute, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(attribute, p);
        this.visit(attribute.key, p);
        if (attribute.value) {
            p.append("=");
            this.visit(attribute.value.element, p);
        }
        this.afterSyntax(attribute, p);
        return attribute;
    }

    override visitJsxSpreadAttribute(spread: JSX.SpreadAttribute, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(spread, p);
        p.append("{");
        this.visitSpace(spread.dots, p);
        p.append("...");
        this.visitRightPaddedLocal([spread.expression], "}", p);
        p.append("}");
        this.afterSyntax(spread, p);
        return spread;
    }

    override visitJsxEmbeddedExpression(expr: JSX.EmbeddedExpression, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(expr, p);
        p.append("{");
        if (expr.expression) {
            this.visitRightPaddedLocal([expr.expression], "}", p);
        }
        p.append("}");
        this.afterSyntax(expr, p);
        return expr;
    }

    override visitJsxNamespacedName(ns: JSX.NamespacedName, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(ns, p);
        this.visit(ns.namespace, p);
        p.append(":");
        this.visitLeftPadded(ns.name, p);
        this.afterSyntax(ns, p);
        return ns;
    }

    override visitImportDeclaration(jsImport: JS.Import, p: PrintOutputCapture): J | undefined {
    
        for (const it of jsImport.modifiers) {
            this.visitDefined(it, p);
        }
        this.beforeSyntax(jsImport, p);
    
        p.append("import");
    
        jsImport.importClause && this.visit(jsImport.importClause, p);
    
        this.visitLeftPaddedLocal(jsImport.importClause ? "from" : "", jsImport.moduleSpecifier, p);
    
        jsImport.attributes && this.visit(jsImport.attributes, p);
    
        if (jsImport.initializer) {
            p.append("=");
            this.visitLeftPadded(jsImport.initializer, p);
        }
    
        this.afterSyntax(jsImport, p);
        return jsImport;
    }

    override visitImportClause(jsImportClause: JS.ImportClause, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(jsImportClause, p);
    
        if (jsImportClause.typeOnly) {
            p.append("type");
        }
    
        if (jsImportClause.name) {
            this.visitRightPadded(jsImportClause.name, p);
    
            if (jsImportClause.namedBindings) {
                p.append(",");
            }
        }
    
        jsImportClause.namedBindings && this.visit(jsImportClause.namedBindings, p);
    
        this.afterSyntax(jsImportClause, p);
    
        return jsImportClause;
    }

    override visitTypeTreeExpression(typeTreeExpression: JS.TypeTreeExpression, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(typeTreeExpression, p);
        this.visit(typeTreeExpression.expression, p);
        this.afterSyntax(typeTreeExpression, p);
        return typeTreeExpression;
    }

    override visitNamespaceDeclaration(namespaceDeclaration: JS.NamespaceDeclaration, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(namespaceDeclaration, p);
        for (const it of namespaceDeclaration.modifiers) {
            this.visitModifier(it, p);
        }
        this.visitSpace(namespaceDeclaration.keywordType.before, p);
    
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
    
        this.visitRightPadded(namespaceDeclaration.name, p);
    
        if (namespaceDeclaration.body) {
            this.visit(namespaceDeclaration.body, p);
        }
    
        this.afterSyntax(namespaceDeclaration, p);
        return namespaceDeclaration;
    }

    override visitSatisfiesExpression(satisfiesExpression: JS.SatisfiesExpression, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(satisfiesExpression, p);
        this.visit(satisfiesExpression.expression, p);
        this.visitLeftPaddedLocal("satisfies", satisfiesExpression.satisfiesType, p);
        this.afterSyntax(satisfiesExpression, p);
        return satisfiesExpression;
    }

    override visitVoid(aVoid: JS.Void, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(aVoid, p);
        p.append("void");
        this.visit(aVoid.expression, p);
        this.afterSyntax(aVoid, p);
        return aVoid;
    }

    override visitYield(aYield: J.Yield, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(aYield, p);
    
        p.append("yield");
    
        const delegated = findMarker<DelegatedYield>(aYield, JS.Markers.DelegatedYield);
        if (delegated) {
            this.visitSpace(delegated.prefix, p);
            p.append("*");
        }
    
        aYield.value && this.visit(aYield.value, p);
    
        this.afterSyntax(aYield, p);
        return aYield;
    }

    override visitTry(aTry: J.Try, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(aTry, p);
        p.append("try");
        this.visit(aTry.body, p);
        this.visitNodes(aTry.catches, p);
        aTry.finally && this.visitLeftPaddedLocal("finally", aTry.finally, p);
        this.afterSyntax(aTry, p);
        return aTry;
    }

    override visitTryCatch(aCatch: J.Try.Catch, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(aCatch, p);
        p.append("catch");
        if (aCatch.parameter.tree.element.variables.length > 0) {
            this.visit(aCatch.parameter, p);
        }
        this.visit(aCatch.body, p);
        this.afterSyntax(aCatch, p);
        return aCatch;
    }

    override visitArrayDimension(arrayDimension: J.ArrayDimension, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(arrayDimension, p);
        p.append("[");
        this.visitRightPaddedLocalSingle(arrayDimension.index, "]", p);
        this.afterSyntax(arrayDimension, p);
        return arrayDimension;
    }

    override visitArrayType(arrayType: J.ArrayType, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(arrayType, p);
        let type: TypedTree = arrayType;
    
        while (type.kind === J.Kind.ArrayType) {
            type = (type as J.ArrayType).elementType;
        }
    
        this.visit(type, p);
        this.visitNodes(arrayType.annotations, p);
    
        if (arrayType.dimension) {
            this.visitSpace(arrayType.dimension.before, p);
            p.append("[");
            this.visitSpace(arrayType.dimension.element, p);
            p.append("]");
    
            if (arrayType.elementType.kind === J.Kind.ArrayType) {
                this.printDimensions(arrayType.elementType as J.ArrayType, p);
            }
        }
    
        this.afterSyntax(arrayType, p);
        return arrayType;
    }

    private printDimensions(arrayType: J.ArrayType, p: PrintOutputCapture) {
        this.beforeSyntax(arrayType, p);
        this.visitNodes(arrayType.annotations, p);
        this.visitSpace(arrayType.dimension?.before ?? emptySpace, p);
    
        p.append("[");
        this.visitSpace(arrayType.dimension?.element ?? emptySpace, p);
        p.append("]");
    
        if (arrayType.elementType.kind === J.Kind.ArrayType) {
            this.printDimensions(arrayType.elementType as J.ArrayType, p);
        }
    
        this.afterSyntax(arrayType, p);
    }

    override visitTernary(ternary: J.Ternary, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(ternary, p);
        this.visit(ternary.condition, p);
        this.visitLeftPaddedLocal("?", ternary.truePart, p);
        this.visitLeftPaddedLocal(":", ternary.falsePart, p);
        this.afterSyntax(ternary, p);
        return ternary;
    }

    override visitThrow(thrown: J.Throw, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(thrown, p);
        p.append("throw");
        this.visit(thrown.exception, p);
        this.afterSyntax(thrown, p);
        return thrown;
    }

    override visitIf(iff: J.If, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(iff, p);
        p.append("if");
        this.visit(iff.ifCondition, p);
        this.visitStatementLocal(iff.thenPart, p);
        iff.elsePart && this.visit(iff.elsePart, p);
        this.afterSyntax(iff, p);
        return iff;
    }

    override visitElse(else_: J.If.Else, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(else_, p);
        p.append("else");
        this.visitStatementLocal(else_.body, p);
        this.afterSyntax(else_, p);
        return else_;
    }

    override visitDoWhileLoop(doWhileLoop: J.DoWhileLoop, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(doWhileLoop, p);
        p.append("do");
        this.visitStatementLocal(doWhileLoop.body, p);
        this.visitLeftPaddedLocal("while", doWhileLoop.whileCondition, p);
        this.afterSyntax(doWhileLoop, p);
        return doWhileLoop;
    }

    override visitWhileLoop(whileLoop: J.WhileLoop, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(whileLoop, p);
        p.append("while");
        this.visit(whileLoop.condition, p);
        this.visitStatementLocal(whileLoop.body, p);
        this.afterSyntax(whileLoop, p);
        return whileLoop;
    }

    override visitInstanceOf(instanceOf: J.InstanceOf, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(instanceOf, p);
        this.visitRightPaddedLocalSingle(instanceOf.expression, "instanceof", p);
        this.visit(instanceOf.class, p);
        instanceOf.pattern && this.visit(instanceOf.pattern, p);
        this.afterSyntax(instanceOf, p);
        return instanceOf;
    }

    override visitLiteral(literal: J.Literal, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(literal, p);
    
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
    
        this.afterSyntax(literal, p);
        return literal;
    }

    override visitScopedVariableDeclarations(variableDeclarations: JS.ScopedVariableDeclarations, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(variableDeclarations, p);
    
        for (const m of variableDeclarations.modifiers) {
            this.visitModifier(m, p);
        }
    
        this.visitRightPaddedLocal(variableDeclarations.variables, ",", p);
    
        this.afterSyntax(variableDeclarations, p);
        return variableDeclarations;
    }

    override visitShebang(shebang: JS.Shebang, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(shebang, p);
        p.append(shebang.text);
        this.afterSyntax(shebang, p);
        return shebang;
    }

    override visitVariableDeclarations(multiVariable: J.VariableDeclarations, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(multiVariable, p);
        this.visitNodes(multiVariable.leadingAnnotations, p);
    
        for (const it of multiVariable.modifiers) {
            this.visitModifier(it, p);
        }
    
        const variables = multiVariable.variables;
        for (let i = 0; i < variables.length; i++) {
            const variable = variables[i];
    
            this.beforeSyntax(variable.element, p);
    
            if (multiVariable.varargs) {
                p.append("...");
            }
    
            this.visit(variable.element.name, p);
            // print non-null assertions or optional
            this.postVisit(variable.element, p);
    
            this.visitSpace(variable.after, p);
    
            if (multiVariable.typeExpression) {
                this.visit(multiVariable.typeExpression, p);
            }
    
            if (variable.element.initializer) {
                this.visitLeftPaddedLocal("=", variable.element.initializer, p);
            }
    
            this.afterSyntax(variable.element, p);
    
            if (i < variables.length - 1) {
                p.append(",");
            } else if (findMarker(variable, J.Markers.Semicolon)) {
                p.append(";");
            }
        }
    
        this.afterSyntax(multiVariable, p);
        return multiVariable;
    }

    override visitVariable(variable: J.VariableDeclarations.NamedVariable, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(variable, p);
        this.visit(variable.name, p);
    
        variable.initializer && this.visitLeftPaddedLocal("=", variable.initializer, p);
    
        this.afterSyntax(variable, p);
        return variable;
    }

    override visitIdentifier(ident: J.Identifier, p: PrintOutputCapture): J | undefined {
        this.visitSpace(emptySpace, p);
        this.visitNodes(ident.annotations, p);
        this.beforeSyntax(ident, p);
        p.append(ident.simpleName);
        this.afterSyntax(ident, p);
        return ident;
    }

    override visitBlock(block: J.Block, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(block, p);
    
        if (block.static.element) {
            p.append("static");
            this.visitRightPadded(block.static, p);
        }
    
        p.append("{");
        this.visitStatements(block.statements, p);
        this.visitSpace(block.end, p);
        p.append("}");
    
        this.afterSyntax(block, p);
        return block;
    }

    override visitTypeInfo(typeInfo: JS.TypeInfo, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(typeInfo, p);
        p.append(":");
        this.visit(typeInfo.typeIdentifier, p);
        this.afterSyntax(typeInfo, p);
        return typeInfo;
    }

    override visitReturn(return_: J.Return, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(return_, p);
        p.append("return");
        return_.expression && this.visit(return_.expression, p);
        this.afterSyntax(return_, p);
        return return_;
    }

    override visitModifier(mod: J.Modifier, p: PrintOutputCapture): J | undefined {
        this.visitNodes(mod.annotations, p);
    
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
    
        this.beforeSyntax(mod, p);
        p.append(keyword);
        this.afterSyntax(mod, p);
        return mod;
    }

    override visitFunctionCall(functionCall: JS.FunctionCall, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(functionCall, p);
    
        if (functionCall.function) {
            this.visitRightPadded(functionCall.function, p);
            if (functionCall.function.element.markers.markers.find(m => m.kind === JS.Markers.Optional)) {
                p.append("?.");
            }
        }
    
        functionCall.typeParameters && this.visitContainerLocal("<", functionCall.typeParameters, ",", ">", p);
        this.visitContainerLocal("(", functionCall.arguments, ",", ")", p);
    
        this.afterSyntax(functionCall, p);
        return functionCall;
    }

    override visitFunctionType(functionType: JS.FunctionType, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(functionType, p);
        for (const m of functionType.modifiers) {
            this.visitModifier(m, p);
        }
        if (functionType.constructorType.element) {
            this.visitLeftPaddedLocal("new", functionType.constructorType, p);
        }
        const typeParameters = functionType.typeParameters;
        if (typeParameters) {
            this.visitNodes(typeParameters.annotations, p);
            this.visitSpace(typeParameters.prefix, p);
            this.visitMarkers(typeParameters.markers, p);
            p.append("<");
            this.visitRightPaddedLocal(typeParameters.typeParameters, ",", p);
            p.append(">");
        }
    
        this.visitContainerLocal("(", functionType.parameters, ",", ")", p);
        this.visitLeftPaddedLocal("=>", functionType.returnType, p);
    
        this.afterSyntax(functionType, p);
        return functionType;
    }

    override visitClassDeclaration(classDecl: J.ClassDeclaration, p: PrintOutputCapture): J | undefined {
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
    
        this.beforeSyntax(classDecl, p);
        this.visitSpace(emptySpace, p);
        this.visitNodes(classDecl.leadingAnnotations, p);
        for (const m of classDecl.modifiers) {
            this.visitModifier(m, p);
        }
        this.visitNodes(classDecl.classKind.annotations, p);
        this.visitSpace(classDecl.classKind.prefix, p);
        p.append(kind);
        this.visit(classDecl.name, p);
        classDecl.typeParameters && this.visitContainerLocal("<", classDecl.typeParameters, ",", ">", p);
        classDecl.primaryConstructor && this.visitContainerLocal("(", classDecl.primaryConstructor, ",", ")", p);
        classDecl.extends && this.visitLeftPaddedLocal("extends", classDecl.extends, p);
        classDecl.implements && this.visitContainerLocal(classDecl.classKind.type === J.ClassDeclaration.Kind.Type.Interface ? "extends" : "implements",
            classDecl.implements, ",", null, p);
        this.visit(classDecl.body, p);
        this.afterSyntax(classDecl, p);
    
        return classDecl;
    }

    override visitMethodDeclaration(method: J.MethodDeclaration, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(method, p);
        this.visitSpace(emptySpace, p);
        this.visitNodes(method.leadingAnnotations, p);
        for (const m of method.modifiers) {
            this.visitModifier(m, p);
        }
    
        let m;
        if ((m = findMarker<FunctionDeclaration>(method, JS.Markers.FunctionDeclaration))) {
            this.visitSpace(m.prefix, p);
            p.append("function");
        }
    
        const asterisk = findMarker<Generator>(method, JS.Markers.Generator);
        if (asterisk) {
            this.visitSpace(asterisk.prefix, p);
            p.append("*");
        }
    
        this.visit(method.name, p);
    
        const typeParameters = method.typeParameters;
        if (typeParameters) {
            this.visitNodes(typeParameters.annotations, p);
            this.visitSpace(typeParameters.prefix, p);
            this.visitMarkers(typeParameters.markers, p);
            p.append("<");
            this.visitRightPaddedLocal(typeParameters.typeParameters, ",", p);
            p.append(">");
        }
    
        this.visitContainerLocal("(", method.parameters, ",", ")", p);
    
        if (method.returnTypeExpression) {
            this.visit(method.returnTypeExpression, p);
        }
    
        method.body && this.visit(method.body, p);
        this.afterSyntax(method, p);
        return method;
    }

    override visitComputedPropertyMethodDeclaration(method: JS.ComputedPropertyMethodDeclaration, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(method, p);
        this.visitSpace(emptySpace, p);
        this.visitNodes(method.leadingAnnotations, p);
        for (const it of method.modifiers) {
            this.visitModifier(it, p);
        }
    
        const generator = findMarker<Generator>(method, JS.Markers.Generator);
        if (generator) {
            this.visitSpace(generator.prefix, p);
            p.append("*");
        }
    
        this.visit(method.name, p);
    
        const typeParameters = method.typeParameters;
        if (typeParameters) {
            this.visitNodes(typeParameters.annotations, p);
            this.visitSpace(typeParameters.prefix, p);
            this.visitMarkers(typeParameters.markers, p);
            p.append("<");
            this.visitRightPaddedLocal(typeParameters.typeParameters, ",", p);
            p.append(">");
        }
    
        this.visitContainerLocal("(", method.parameters, ",", ")", p);
        if (method.returnTypeExpression) {
            this.visit(method.returnTypeExpression, p);
        }
    
        method.body && this.visit(method.body, p);
        this.afterSyntax(method, p);
        return method;
    }

    override visitMethodInvocation(method: J.MethodInvocation, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(method, p);
    
        if (method.name.simpleName.length === 0) {
            method.select && this.visitRightPadded(method.select, p);
        } else {
            if (method.select) {
                this.visitRightPadded(method.select, p);
                if (!method.select.element.markers.markers.find(m => m.kind === JS.Markers.Optional)) {
                    p.append(".");
                } else {
                    p.append("?.");
                }
            }
            this.visit(method.name, p);
        }
    
        method.typeParameters && this.visitContainerLocal("<", method.typeParameters, ",", ">", p);
        this.visitContainerLocal("(", method.arguments, ",", ")", p);
    
        this.afterSyntax(method, p);
        return method;
    }

    override visitTypeParameter(typeParameter: J.TypeParameter, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(typeParameter, p);
        this.visitNodes(typeParameter.annotations, p);
        for (const m of typeParameter.modifiers) {
            this.visitModifier(m, p);
        }
        this.visit(typeParameter.name, p);
    
        const bounds = typeParameter.bounds;
        if (bounds) {
            this.visitSpace(bounds.before, p);
            const constraintType = bounds.elements[0];
    
            if (!(constraintType.element.kind === J.Kind.Empty)) {
                p.append("extends");
                this.visitRightPadded(constraintType, p);
            }
    
            const defaultType = bounds.elements[1];
            if (!(defaultType.element.kind === J.Kind.Empty)) {
                p.append("=");
                this.visitRightPadded(defaultType, p);
            }
        }
    
        this.afterSyntax(typeParameter, p);
        return typeParameter;
    }

    override visitArrowFunction(arrowFunction: JS.ArrowFunction, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(arrowFunction, p);
        this.visitNodes(arrowFunction.leadingAnnotations, p);
        for (const m of arrowFunction.modifiers) {
            this.visitModifier(m, p);
        }
    
        const typeParameters = arrowFunction.typeParameters;
        if (typeParameters) {
            this.visitNodes(typeParameters.annotations, p);
            this.visitSpace(typeParameters.prefix, p);
            this.visitMarkers(typeParameters.markers, p);
            p.append("<");
            this.visitRightPaddedLocal(typeParameters.typeParameters, ",", p);
            p.append(">");
        }
    
        const lambda = arrowFunction.lambda;
    
        if (lambda.parameters.parenthesized) {
            this.visitSpace(lambda.parameters.prefix, p);
            p.append("(");
            this.visitRightPaddedLocal(lambda.parameters.parameters, ",", p);
            p.append(")");
        } else {
            this.visitRightPaddedLocal(lambda.parameters.parameters, ",", p);
        }
    
        if (arrowFunction.returnTypeExpression) {
            this.visit(arrowFunction.returnTypeExpression, p);
        }
    
        this.visitSpace(lambda.arrow, p);
        p.append("=>");
        this.visit(lambda.body, p);
    
        this.afterSyntax(arrowFunction, p);
        return arrowFunction;
    }

    override visitConditionalType(conditionalType: JS.ConditionalType, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(conditionalType, p);
        this.visit(conditionalType.checkType, p);
        this.visitLeftPaddedLocal("extends", conditionalType.condition, p);
        this.afterSyntax(conditionalType, p);
        return conditionalType;
    }

    override visitExpressionWithTypeArguments(type: JS.ExpressionWithTypeArguments, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(type, p);
        this.visit(type.clazz, p);
        type.typeArguments && this.visitContainerLocal("<", type.typeArguments, ",", ">", p);
        this.afterSyntax(type, p);
        return type;
    }

    override visitImportType(importType: JS.ImportType, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(importType, p);
    
        if (importType.hasTypeof.element) {
            p.append("typeof");
            this.visitRightPadded(importType.hasTypeof, p);
        }
    
        p.append("import");
        this.visitContainerLocal("(", importType.argumentAndAttributes, ",", ")", p);
        importType.qualifier && this.visitLeftPaddedLocal(".", importType.qualifier, p);
        importType.typeArguments && this.visitContainerLocal("<", importType.typeArguments, ",", ">", p);
    
        this.afterSyntax(importType, p);
        return importType;
    }

    override visitTypeDeclaration(typeDeclaration: JS.TypeDeclaration, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(typeDeclaration, p);
    
        for (const m of typeDeclaration.modifiers) {
            this.visitModifier(m, p);
        }
    
        this.visitLeftPaddedLocal("type", typeDeclaration.name, p);
    
        const typeParameters = typeDeclaration.typeParameters;
        if (typeParameters) {
            this.visitNodes(typeParameters.annotations, p);
            this.visitSpace(typeParameters.prefix, p);
            this.visitMarkers(typeParameters.markers, p);
            p.append("<");
            this.visitRightPaddedLocal(typeParameters.typeParameters, ",", p);
            p.append(">");
        }
    
        this.visitLeftPaddedLocal("=", typeDeclaration.initializer, p);
    
        this.afterSyntax(typeDeclaration, p);
        return typeDeclaration;
    }

    override visitLiteralType(literalType: JS.LiteralType, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(literalType, p);
        this.visit(literalType.literal, p);
        this.afterSyntax(literalType, p);
        return literalType;
    }

    override visitNamedImports(namedImports: JS.NamedImports, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(namedImports, p);
        this.visitContainerLocal("{", namedImports.elements, ",", "}", p);
        this.afterSyntax(namedImports, p);
        return namedImports;
    }

    override visitImportSpecifier(jis: JS.ImportSpecifier, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(jis, p);
    
        if (jis.importType.element) {
            this.visitLeftPaddedLocal("type", jis.importType, p);
        }
    
        this.visit(jis.specifier, p);
    
        this.afterSyntax(jis, p);
        return jis;
    }

    override visitExportDeclaration(ed: JS.ExportDeclaration, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(ed, p);
        p.append("export");
        for (const it of ed.modifiers) {
            this.visitModifier(it, p);
        }
    
        if (ed.typeOnly.element) {
            this.visitLeftPaddedLocal("type", ed.typeOnly, p);
        }
    
        ed.exportClause && this.visit(ed.exportClause, p);
        ed.moduleSpecifier && this.visitLeftPaddedLocal("from", ed.moduleSpecifier, p);
        ed.attributes && this.visit(ed.attributes, p);
    
        this.afterSyntax(ed, p);
        return ed;
    }

    override visitExportAssignment(es: JS.ExportAssignment, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(es, p);
        p.append("export");
        this.visitLeftPaddedLocal(es.exportEquals ? "=" : "default", es.expression, p);
        this.afterSyntax(es, p);
        return es;
    }

    override visitIndexedAccessType(iat: JS.IndexedAccessType, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(iat, p);
    
        this.visit(iat.objectType, p);
        // expect that this element is printed accordingly
        // <space_before>[<inner_space_before>index<inner_right_padded_suffix_space>]<right_padded_suffix_space>
        this.visit(iat.indexType, p);
    
        this.afterSyntax(iat, p);
        return iat;
    }

    override visitIndexedAccessTypeIndexType(indexType: JS.IndexedAccessType.IndexType, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(indexType, p);
    
        p.append("[");
        this.visitRightPadded(indexType.element, p);
        p.append("]");
    
        this.afterSyntax(indexType, p);
        return indexType;
    }

    override visitWithStatement(withStatement: JS.WithStatement, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(withStatement, p);
        p.append("with");
        this.visit(withStatement.expression, p);
        this.visitRightPadded(withStatement.body, p);
        this.afterSyntax(withStatement, p);
        return withStatement;
    }

    override visitExportSpecifier(es: JS.ExportSpecifier, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(es, p);
        if (es.typeOnly.element) {
            this.visitLeftPaddedLocal("type", es.typeOnly, p);
        }
    
        this.visit(es.specifier, p);
    
        this.afterSyntax(es, p);
        return es;
    }

    override visitNamedExports(ne: JS.NamedExports, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(ne, p);
        this.visitContainerLocal("{", ne.elements, ",", "}", p);
        this.afterSyntax(ne, p);
        return ne;
    }

    override visitImportAttributes(importAttributes: JS.ImportAttributes, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(importAttributes, p);
    
        p.append((importAttributes.token === JS.ImportAttributes.Token.With ? "with" : "assert"));
        this.visitContainerLocal("{", importAttributes.elements, ",", "}", p);
    
        this.afterSyntax(importAttributes, p);
        return importAttributes;
    }

    override visitImportAttribute(importAttribute: JS.ImportAttribute, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(importAttribute, p);
    
        this.visit(importAttribute.name, p);
        this.visitLeftPaddedLocal(":", importAttribute.value, p);
    
        this.afterSyntax(importAttribute, p);
        return importAttribute;
    }

    override visitImportTypeAttributes(importAttributes: JS.ImportTypeAttributes, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(importAttributes, p);
        p.append("{");
    
        this.visitRightPadded(importAttributes.token, p);
        p.append(":");
        this.visitContainerLocal("{", importAttributes.elements, ",", "}", p);
        this.visitSpace(importAttributes.end, p);
    
        p.append("}");
        this.afterSyntax(importAttributes, p);
        return importAttributes;
    }

    override visitArrayBindingPattern(abp: JS.ArrayBindingPattern, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(abp, p);
        this.visitContainerLocal("[", abp.elements, ",", "]", p);
        this.afterSyntax(abp, p);
        return abp;
    }

    override visitMappedType(mappedType: JS.MappedType, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(mappedType, p);
        p.append("{");
    
        if (mappedType.prefixToken) {
            this.visitLeftPadded(mappedType.prefixToken, p);
        }
    
        if (mappedType.hasReadonly.element) {
            this.visitLeftPaddedLocal("readonly", mappedType.hasReadonly, p);
        }
    
        this.visitMappedTypeKeysRemapping(mappedType.keysRemapping, p);
    
        if (mappedType.suffixToken) {
            this.visitLeftPadded(mappedType.suffixToken, p);
        }
    
        if (mappedType.hasQuestionToken.element) {
            this.visitLeftPaddedLocal("?", mappedType.hasQuestionToken, p);
        }
    
        const colon = mappedType.valueType.elements[0].element.kind === J.Kind.Empty ? "" : ":";
        this.visitContainerLocal(colon, mappedType.valueType, "", "", p);
    
        p.append("}");
        this.afterSyntax(mappedType, p);
        return mappedType;
    }

    override visitMappedTypeKeysRemapping(mappedTypeKeys: JS.MappedType.KeysRemapping, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(mappedTypeKeys, p);
        p.append("[");
        this.visitRightPadded(mappedTypeKeys.typeParameter, p);
    
        if (mappedTypeKeys.nameType) {
            p.append("as");
            this.visitRightPadded(mappedTypeKeys.nameType, p);
        }
    
        p.append("]");
        this.afterSyntax(mappedTypeKeys, p);
        return mappedTypeKeys;
    }

    override visitMappedTypeParameter(mappedTypeParameter: JS.MappedType.Parameter, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(mappedTypeParameter, p);
        this.visit(mappedTypeParameter.name, p);
        this.visitLeftPaddedLocal("in", mappedTypeParameter.iterateType, p);
        this.afterSyntax(mappedTypeParameter, p);
        return mappedTypeParameter;
    }

    override visitObjectBindingPattern(objectBindingPattern: JS.ObjectBindingPattern, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(objectBindingPattern, p);
        this.visitNodes(objectBindingPattern.leadingAnnotations, p);
        for (const m of objectBindingPattern.modifiers) {
            this.visitModifier(m, p);
        }
    
        objectBindingPattern.typeExpression && this.visit(objectBindingPattern.typeExpression, p);
        this.visitContainerLocal("{", objectBindingPattern.bindings, ",", "}", p);
        objectBindingPattern.initializer && this.visitLeftPaddedLocal("=", objectBindingPattern.initializer, p);
        this.afterSyntax(objectBindingPattern, p);
        return objectBindingPattern;
    }

    override visitTaggedTemplateExpression(taggedTemplateExpression: JS.TaggedTemplateExpression, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(taggedTemplateExpression, p);
        taggedTemplateExpression.tag && this.visitRightPadded(taggedTemplateExpression.tag, p);
        taggedTemplateExpression.typeArguments && this.visitContainerLocal("<", taggedTemplateExpression.typeArguments, ",", ">", p);
        this.visit(taggedTemplateExpression.templateExpression, p);
        this.afterSyntax(taggedTemplateExpression, p);
        return taggedTemplateExpression;
    }

    override visitTemplateExpression(templateExpression: JS.TemplateExpression, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(templateExpression, p);
        this.visit(templateExpression.head, p);
        this.visitRightPaddedLocal(templateExpression.spans, "", p);
        this.afterSyntax(templateExpression, p);
        return templateExpression;
    }

    override visitTemplateExpressionSpan(value: JS.TemplateExpression.Span, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(value, p);
        this.visit(value.expression, p);
        this.visit(value.tail, p);
        this.afterSyntax(value, p);
        return value;
    }

    override visitTuple(tuple: JS.Tuple, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(tuple, p);
        this.visitContainerLocal("[", tuple.elements, ",", "]", p);
        this.afterSyntax(tuple, p);
        return tuple;
    }

    override visitTypeQuery(typeQuery: JS.TypeQuery, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(typeQuery, p);
        p.append("typeof");
        this.visit(typeQuery.typeExpression, p);
        typeQuery.typeArguments && this.visitContainerLocal("<", typeQuery.typeArguments, ",", ">", p);
        this.afterSyntax(typeQuery, p);
        return typeQuery;
    }

    override visitTypeOf(typeOf: JS.TypeOf, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(typeOf, p);
        p.append("typeof");
        this.visit(typeOf.expression, p);
        this.afterSyntax(typeOf, p);
        return typeOf;
    }

    protected visitComputedPropertyName(computedPropertyName: JS.ComputedPropertyName, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(computedPropertyName, p);
        p.append("[");
        this.visitRightPaddedLocalSingle(computedPropertyName.expression, "]", p);
        this.afterSyntax(computedPropertyName, p);
        return computedPropertyName;
    }

    override visitTypeOperator(typeOperator: JS.TypeOperator, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(typeOperator, p);
    
        let keyword = "";
        if (typeOperator.operator === JS.TypeOperator.Type.ReadOnly) {
            keyword = "readonly";
        } else if (typeOperator.operator === JS.TypeOperator.Type.KeyOf) {
            keyword = "keyof";
        } else if (typeOperator.operator === JS.TypeOperator.Type.Unique) {
            keyword = "unique";
        }
    
        p.append(keyword);
    
        this.visitLeftPadded(typeOperator.expression, p);
    
        this.afterSyntax(typeOperator, p);
        return typeOperator;
    }

    override visitTypePredicate(typePredicate: JS.TypePredicate, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(typePredicate, p);
    
        if (typePredicate.asserts.element) {
            this.visitLeftPaddedLocal("asserts", typePredicate.asserts, p);
        }
    
        this.visit(typePredicate.parameterName, p);
        typePredicate.expression && this.visitLeftPaddedLocal("is", typePredicate.expression, p);
    
        this.afterSyntax(typePredicate, p);
        return typePredicate;
    }

    override visitIndexSignatureDeclaration(isd: JS.IndexSignatureDeclaration, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(isd, p);
    
        for (const m of isd.modifiers) {
            this.visitModifier(m, p);
        }
        this.visitContainerLocal("[", isd.parameters, "", "]", p);
        this.visitLeftPaddedLocal(":", isd.typeExpression, p);
    
        this.afterSyntax(isd, p);
        return isd;
    }

    override visitAnnotation(annotation: J.Annotation, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(annotation, p);
    
        p.append("@");
        this.visit(annotation.annotationType, p);
        annotation.arguments && this.visitContainerLocal("(", annotation.arguments, ",", ")", p);
    
        this.afterSyntax(annotation, p);
        return annotation;
    }

    override visitNewArray(newArray: J.NewArray, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(newArray, p);
        newArray.typeExpression && this.visit(newArray.typeExpression, p);
        this.visitNodes(newArray.dimensions, p);
        newArray.initializer && this.visitContainerLocal("[", newArray.initializer, ",", "]", p);
        this.afterSyntax(newArray, p);
        return newArray;
    }

    override visitNewClass(newClass: J.NewClass, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(newClass, p);
        newClass.enclosing && this.visitRightPaddedLocalSingle(newClass.enclosing, ".", p);
        this.visitSpace(newClass.new, p);
    
        if (newClass.class) {
            p.append("new");
            this.visit(newClass.class, p);
    
            if (!newClass.arguments.markers.markers.find(m => m.kind === J.Markers.OmitParentheses)) {
                this.visitContainerLocal("(", newClass.arguments, ",", ")", p);
            }
        }
    
        newClass.body && this.visit(newClass.body, p);
        this.afterSyntax(newClass, p);
        return newClass;
    }

    override visitSwitch(switch_: J.Switch, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(switch_, p);
        p.append("switch");
        this.visit(switch_.selector, p);
        this.visit(switch_.cases, p);
        this.afterSyntax(switch_, p);
        return switch_;
    }

    override visitCase(case_: J.Case, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(case_, p);
    
        const elem = case_.caseLabels.elements[0].element;
        if (elem.kind !== J.Kind.Identifier || (elem as J.Identifier).simpleName !== "default") {
            p.append("case");
        }
    
        this.visitContainerLocal("", case_.caseLabels, ",", "", p);
    
        this.visitSpace(case_.statements.before, p);
        p.append(case_.type === J.Case.Type.Statement ? ":" : "->");
    
        this.visitStatements(case_.statements.elements, p);
    
        this.afterSyntax(case_, p);
        return case_;
    }

    override visitLabel(label: J.Label, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(label, p);
        this.visitRightPaddedLocalSingle(label.label, ":", p);
        this.visit(label.statement, p);
        this.afterSyntax(label, p);
        return label;
    }

    override visitContinue(continueStatement: J.Continue, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(continueStatement, p);
        p.append("continue");
        continueStatement.label && this.visit(continueStatement.label, p);
        this.afterSyntax(continueStatement, p);
        return continueStatement;
    }

    override visitBreak(breakStatement: J.Break, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(breakStatement, p);
        p.append("break");
        breakStatement.label && this.visit(breakStatement.label, p);
        this.afterSyntax(breakStatement, p);
        return breakStatement;
    }

    override visitFieldAccess(fieldAccess: J.FieldAccess, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(fieldAccess, p);
        this.visit(fieldAccess.target, p);
    
        this.visitLeftPaddedLocal(".", fieldAccess.name, p);
        this.afterSyntax(fieldAccess, p);
        return fieldAccess;
    }

    override visitTypeLiteral(tl: JS.TypeLiteral, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(tl, p);
    
        this.visit(tl.members, p);
    
        this.afterSyntax(tl, p);
        return tl;
    }

    override visitParentheses<T extends J>(parens: J.Parentheses<T>, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(parens, p);
        p.append('(');
        this.visitRightPaddedLocalSingle(parens.tree, ")", p);
        this.afterSyntax(parens, p);
        return parens;
    }

    override visitParameterizedType(type: J.ParameterizedType, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(type, p);
        this.visit(type.class, p);
        type.typeParameters && this.visitContainerLocal("<", type.typeParameters, ",", ">", p);
        this.afterSyntax(type, p);
        return type;
    }

    override visitAs(as_: JS.As, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(as_, p);
        this.visitRightPadded(as_.left, p);
        p.append("as");
        this.visit(as_.right, p);
        this.afterSyntax(as_, p);
    
        return as_;
    }

    override visitAssignment(assignment: J.Assignment, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(assignment, p);
        this.visit(assignment.variable, p);
        this.visitLeftPaddedLocal("=", assignment.assignment, p);
        this.afterSyntax(assignment, p);
        return assignment;
    }

    override visitPropertyAssignment(propertyAssignment: JS.PropertyAssignment, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(propertyAssignment, p);
    
        this.visitRightPadded(propertyAssignment.name, p);
    
        if (propertyAssignment.initializer) {
            // if the property is not null, we should print it like `{ a: b }`
            // otherwise, it is a shorthand assignment where we have stuff like `{ a }` only
            if (propertyAssignment.assigmentToken === JS.PropertyAssignment.Token.Colon) {
                p.append(':');
            } else if (propertyAssignment.assigmentToken === JS.PropertyAssignment.Token.Equals) {
                p.append('=');
            }
            this.visit(propertyAssignment.initializer, p);
        }
    
        this.afterSyntax(propertyAssignment, p);
        return propertyAssignment;
    }

    override visitAssignmentOperation(assignOp: J.AssignmentOperation, p: PrintOutputCapture): J | undefined {
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
    
        this.beforeSyntax(assignOp, p);
        this.visit(assignOp.variable, p);
        this.visitSpace(assignOp.operator.before, p);
        p.append(keyword);
        this.visit(assignOp.assignment, p);
        this.afterSyntax(assignOp, p);
    
        return assignOp;
    }

    override visitAssignmentOperationExtensions(assignOp: JS.AssignmentOperation, p: PrintOutputCapture): J | undefined {
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
    
        this.beforeSyntax(assignOp, p);
        this.visit(assignOp.variable, p);
        this.visitSpace(assignOp.operator.before, p);
        p.append(keyword);
        this.visit(assignOp.assignment, p);
        this.afterSyntax(assignOp, p);
    
        return assignOp;
    }

    override visitEnumValue(enum_: J.EnumValue, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(enum_, p);
        this.visit(enum_.name, p);
    
        const initializer = enum_.initializer;
        if (initializer) {
            this.visitSpace(initializer.prefix, p);
            p.append("=");
            // There can be only one argument
            const expression = initializer.arguments.elements[0];
            this.visitRightPadded(expression, p);
            return enum_;
        }
    
        this.afterSyntax(enum_, p);
        return enum_;
    }

    override visitEnumValueSet(enums: J.EnumValueSet, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(enums, p);
        this.visitRightPaddedLocal(enums.enums, ",", p);
    
        if (enums.terminatedWithSemicolon) {
            p.append(",");
        }
    
        this.afterSyntax(enums, p);
        return enums;
    }

    override visitBinary(binary: J.Binary, p: PrintOutputCapture): J | undefined {
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
    
        this.beforeSyntax(binary, p);
        this.visit(binary.left, p);
        this.visitSpace(binary.operator.before, p);
        p.append(keyword);
        this.visit(binary.right, p);
        this.afterSyntax(binary, p);
    
        return binary;
    }

    override visitBinaryExtensions(binary: JS.Binary, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(binary, p);
    
        this.visit(binary.left, p);
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
    
        this.visitSpace(binary.operator.before, p);
        p.append(keyword);
    
        this.visit(binary.right, p);
    
        this.afterSyntax(binary, p);
        return binary;
    }

    override visitUnary(unary: J.Unary, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(unary, p);
        switch (unary.operator.element) {
            case J.Unary.Type.PreIncrement:
                p.append("++");
                this.visit(unary.expression, p);
                break;
            case J.Unary.Type.PreDecrement:
                p.append("--");
                this.visit(unary.expression, p);
                break;
            case J.Unary.Type.PostIncrement:
                this.visit(unary.expression, p);
                this.visitSpace(unary.operator.before, p);
                p.append("++");
                break;
            case J.Unary.Type.PostDecrement:
                this.visit(unary.expression, p);
                this.visitSpace(unary.operator.before, p);
                p.append("--");
                break;
            case J.Unary.Type.Positive:
                p.append('+');
                this.visit(unary.expression, p);
                break;
            case J.Unary.Type.Negative:
                p.append('-');
                this.visit(unary.expression, p);
                break;
            case J.Unary.Type.Complement:
                p.append('~');
                this.visit(unary.expression, p);
                break;
            case J.Unary.Type.Not:
            default:
                p.append('!');
                this.visit(unary.expression, p);
        }
        this.afterSyntax(unary, p);
        return unary;
    }

    override visitUnion(union: JS.Union, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(union, p);
    
        this.visitRightPaddedLocal(union.types, "|", p);
    
        this.afterSyntax(union, p);
        return union;
    }

    override visitIntersection(intersection: JS.Intersection, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(intersection, p);
    
        this.visitRightPaddedLocal(intersection.types, "&", p);
    
        this.afterSyntax(intersection, p);
        return intersection;
    }

    override visitForLoop(forLoop: J.ForLoop, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(forLoop, p);
        p.append("for");
        const ctrl = forLoop.control;
        this.visitSpace(ctrl.prefix, p);
        p.append('(');
        this.visitRightPaddedLocal(ctrl.init, ",", p);
        p.append(';');
        ctrl.condition && this.visitRightPaddedLocalSingle(ctrl.condition, ";", p);
        this.visitRightPaddedLocal(ctrl.update, ",", p);
        p.append(')');
        this.visitStatementLocal(forLoop.body, p);
        this.afterSyntax(forLoop, p);
        return forLoop;
    }

    override visitForOfLoop(loop: JS.ForOfLoop, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(loop, p);
        p.append("for");
        if (loop.await) {
            this.visitSpace(loop.await, p);
            p.append("await");
        }
    
        const control = loop.loop.control;
        this.visitSpace(control.prefix, p);
        p.append('(');
        this.visitRightPadded(control.variable, p);
        p.append("of");
        this.visitRightPadded(control.iterable, p);
        p.append(')');
        this.visitRightPadded(loop.loop.body, p);
        this.afterSyntax(loop, p);
        return loop;
    }

    override visitForInLoop(loop: JS.ForInLoop, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(loop, p);
        p.append("for");
    
        const control = loop.control;
        this.visitSpace(control.prefix, p);
        p.append('(');
        this.visitRightPadded(control.variable, p);
        p.append("in");
        this.visitRightPadded(control.iterable, p);
        p.append(')');
        this.visitRightPadded(loop.body, p);
        this.afterSyntax(loop, p);
        return loop;
    }

    // ---- print utils

    private visitStatements(statements: J.RightPadded<Statement>[], p: PrintOutputCapture) {
        const objectLiteral =
            this.getParentCursor(0)?.value.kind === J.Kind.Block &&
            this.getParentCursor(1)?.value.kind === J.Kind.NewClass;
    
        for (let i = 0; i < statements.length; i++) {
            const paddedStat = statements[i];
            this.visitStatementLocal(paddedStat, p);
            if (i < statements.length - 1 && objectLiteral) {
                p.append(",");
            }
        }
    }

    private visitStatementLocal(paddedStat: J.RightPadded<Statement> | undefined, p: PrintOutputCapture) {
        if (paddedStat) {
            this.visit(paddedStat.element, p);
            this.visitSpace(paddedStat.after, p);
            this.visitMarkers(paddedStat.markers, p);
        }
    }

    private getParentCursor(levels: number): Cursor | undefined {
        let cursor: Cursor | undefined = this.cursor;
        for (let i = 0; i < levels && cursor; i++) {
            cursor = cursor.parent;
        }
    
        return cursor;
    }

    protected afterSyntax(j: J, p: PrintOutputCapture) {
        this.afterSyntaxMarkers(j.markers, p);
    }

    private afterSyntaxMarkers(markers: Markers, p: PrintOutputCapture) {
        for (const marker of markers.markers) {
            p.append(p.markerPrinter.afterSyntax(marker, new Cursor(marker, this.cursor), this.JAVA_SCRIPT_MARKER_WRAPPER));
        }
    }

    protected beforeSyntax(j: J, p: PrintOutputCapture) {
        this.beforeSyntaxExt(j.prefix, j.markers, p);
    }

    private beforeSyntaxExt(prefix: J.Space, markers: Markers, p: PrintOutputCapture) {
        for (const marker of markers.markers) {
            p.append(p.markerPrinter.beforePrefix(marker, new Cursor(marker, this.cursor), this.JAVA_SCRIPT_MARKER_WRAPPER));
        }
    
        this.visitSpace(prefix, p);
        this.visitMarkers(markers, p);
    
        for (const marker of markers.markers) {
            p.append(p.markerPrinter.beforeSyntax(marker, new Cursor(marker, this.cursor), this.JAVA_SCRIPT_MARKER_WRAPPER));
        }
    }

    override visitSpace(space: J.Space, p: PrintOutputCapture): J.Space {
        p.append(space.whitespace!);
    
        const comments = space.comments;
        for (let i = 0; i < comments.length; i++) {
            const comment = comments[i];
            this.visitMarkers(comment.markers, p);
            this.printComment(comment, this.cursor, p);
            p.append(comment.suffix);
        }
    
        return space;
    }

    private visitRightPaddedLocal(nodes: J.RightPadded<J>[], suffixBetween: string, p: PrintOutputCapture) {
        for (let i = 0; i < nodes.length; i++) {
            const node = nodes[i];
    
            this.visit(node.element, p);
    
            this.visitSpace(node.after, p);
            this.visitMarkers(node.markers, p);
    
            if (i < nodes.length - 1) {
                p.append(suffixBetween);
            }
        }
    }

    public visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, p: PrintOutputCapture): J.RightPadded<T> {
        if (isTree(right.element)) {
            this.visit(right.element, p);
        }
    
        this.visitSpace(right.after, p);
        this.visitMarkers(right.markers, p);
        return right;
    }

    private visitRightPaddedLocalSingle(node: J.RightPadded<J> | undefined, suffix: string, p: PrintOutputCapture) {
        if (node) {
            this.visit(node.element, p);
    
            this.visitSpace(node.after, p);
            this.visitMarkers(node.markers, p);
    
            p.append(suffix);
        }
    }

    private visitLeftPaddedLocal(prefix: string | undefined, leftPadded: J.LeftPadded<J> | J.LeftPadded<boolean> | J.LeftPadded<string> | undefined, p: PrintOutputCapture) {
        if (leftPadded) {
            this.beforeSyntaxExt(leftPadded.before, leftPadded.markers, p);
    
            if (prefix) {
                p.append(prefix);
            }
    
            if (typeof leftPadded.element === 'string') {
                p.append(leftPadded.element);
            } else if (typeof leftPadded.element !== 'boolean') {
                this.visit(leftPadded.element, p);
            }
    
            this.afterSyntaxMarkers(leftPadded.markers, p);
        }
    }

    private visitContainerLocal(before: string, container: J.Container<J> | undefined, suffixBetween: string, after: string | null, p: PrintOutputCapture) {
        if (!container) {
            return;
        }
    
        this.beforeSyntaxExt(container.before, container.markers, p);
    
        p.append(before);
        this.visitRightPaddedLocal(container.elements, suffixBetween, p);
        this.afterSyntaxMarkers(container.markers, p);
    
        p.append(after === null ? "" : after);
    }

    override visitMarker<M extends Marker>(marker: M, p: PrintOutputCapture): M {
        if (marker.kind === J.Markers.Semicolon) {
            p.append(';');
        }
        if (marker.kind === J.Markers.TrailingComma) {
            p.append(',');
            this.visitSpace((marker as unknown as TrailingComma).suffix, p);
        }
        return marker;
    }

    protected preVisit(tree: J, p: PrintOutputCapture): J | undefined {
        // Note: Spread is now handled as JS.Spread AST element via visitSpread
        return tree;
    }

    protected postVisit(tree: J, p: PrintOutputCapture): J | undefined {
        for (const marker of tree.markers.markers) {
            if (marker.kind === JS.Markers.NonNullAssertion) {
                this.visitSpace((marker as NonNullAssertion).prefix, p);
                p.append("!");
            }
            if (marker.kind === JS.Markers.Optional) {
                this.visitSpace((marker as Optional).prefix, p);
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

    private visitNodes<T extends Tree>(nodes: T[] | undefined, p: PrintOutputCapture): void {
        if (nodes) {
            for (const node of nodes) {
                this.visit(node, p);
            }
        }
    }

    override visitControlParentheses<T extends J>(controlParens: J.ControlParentheses<T>, p: PrintOutputCapture): J | undefined {
        this.beforeSyntax(controlParens, p);
    
        if (this.getParentCursor(1)?.value.kind === J.Kind.TypeCast) {
            p.append('<');
            this.visitRightPaddedLocalSingle(controlParens.tree, ">", p);
        } else {
            p.append('(');
            this.visitRightPaddedLocalSingle(controlParens.tree, ")", p);
        }
    
        this.afterSyntax(controlParens, p);
        return controlParens;
    }
}

TreePrinters.register(JS.Kind.CompilationUnit, () => new JavaScriptPrinter());
