// noinspection DuplicatedCode

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
import '../tree';
import '../java';
import {JavaScriptVisitor, JS} from ".";
import {PrintOutputCapture, TreePrinters} from "../print";
import {Cursor, Tree} from "../tree";
import {Comment, emptySpace, J, JavaMarkers, Statement, TextComment, TrailingComma, TypeTree} from "../java";
import {Marker, Markers} from "../markers";
import Space = J.Space;
import NamespaceDeclaration = JS.NamespaceDeclaration;

export class JavaScriptPrinter extends JavaScriptVisitor<PrintOutputCapture> {

    JAVA_SCRIPT_MARKER_WRAPPER: (out: string) => string = (out) => `/*~~${out}${out.length === 0 ? "" : "~~"}>*/`;

    override async visitJsCompilationUnit(cu: JS.CompilationUnit, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(cu, p);

        await this.visitJsRightPaddedLocal(cu.statements, "", p);

        await this.visitSpace(cu.eof, p);
        await this.afterSyntax(cu, p);
        return cu;
    }

    // override async visitCompilationUnit(cu: J.CompilationUnit, p: PrintOutputCapture): Promise<J | undefined> {
    //     await this.beforeSyntax(cu, p);
    //
    //     //await this.visitJRightPadded(cu.getStatements(), "", p);
    //
    //     await this.visitSpace(cu.eof, p);
    //     await this.afterSyntax(cu, p);
    //     return cu;
    // }

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
        binding.initializer && await this.visitJsLeftPaddedLocal("=", binding.initializer, p);
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

    override async visitTrailingTokenStatement(statement: JS.TrailingTokenStatement, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(statement, p);
        await this.visitRightPadded(statement.expression, p);
        await this.afterSyntax(statement, p);
        return statement;
    }

    override async visitInferType(inferType: JS.InferType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(inferType, p);
        await this.visitJsLeftPaddedLocal("infer", inferType.typeParameter, p);
        await this.afterSyntax(inferType, p);
        return inferType;
    }

    override async visitJsImport(jsImport: JS.JsImport, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(jsImport, p);
        jsImport.modifiers.forEach(m => this.visitModifier(m, p));
        p.append("import");
        jsImport.importClause && await this.visit(jsImport.importClause, p);

        await this.visitJsLeftPaddedLocal(jsImport.importClause ? "from" : "", jsImport.moduleSpecifier, p);

        jsImport.attributes && await this.visit(jsImport.attributes, p);

        await this.afterSyntax(jsImport, p);
        return jsImport;
    }

    override async visitJsImportClause(jsImportClause: JS.JsImportClause, p: PrintOutputCapture): Promise<J | undefined> {
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
        namespaceDeclaration.modifiers.forEach(it => this.visitModifier(it, p));
        await this.visitSpace(namespaceDeclaration.keywordType.before, p);

        switch (namespaceDeclaration.keywordType.element) {
            case JS.NamespaceDeclaration.KeywordType.Namespace:
                p.append("namespace");
                break;
            case NamespaceDeclaration.KeywordType.Module:
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
        await this.visitJsLeftPaddedLocal("satisfies", satisfiesExpression.satisfiesType, p);
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

    override async visitJsYield(yield_: JS.Yield, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(yield_, p);

        p.append("yield");

        if (yield_.delegated) {
            await this.visitJsLeftPaddedLocal("*", yield_.delegated, p);
        }

        yield_.expression && await this.visit(yield_.expression, p);

        await this.afterSyntax(yield_, p);
        return yield_;
    }

    override async visitTry(try_: J.Try, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(try_, p);
        p.append("try");
        await this.visit(try_.body, p);
        await this.visitNodes(try_.catches, p);
        try_.finally && await this.visitJLeftPaddedLocal("finally", try_.finally, p);
        await this.afterSyntax(try_, p);
        return try_;
    }

    override async visitTryCatch(catch_: J.TryCatch, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(catch_, p);
        p.append("catch");
        if (catch_.parameter.tree.element.variables.length > 0) {
            await this.visit(catch_.parameter, p);
        }
        await this.visit(catch_.body, p);
        await this.afterSyntax(catch_, p);
        return catch_;
    }

    override async visitJSTry(jsTry: JS.JSTry, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(jsTry, p);
        p.append("try");
        await this.visit(jsTry.body, p);
        await this.visit(jsTry.catches, p);
        jsTry.finally && await this.visitJsLeftPaddedLocal("finally", jsTry.finally, p);
        await this.afterSyntax(jsTry, p);
        return jsTry;
    }

    override async visitJSCatch(jsCatch: JS.JSTry.JSCatch, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(jsCatch, p);
        p.append("catch");
        await this.visit(jsCatch.parameter, p);
        await this.visit(jsCatch.body, p);
        await this.afterSyntax(jsCatch, p);
        return jsCatch;
    }

    override async visitJSVariableDeclarations(multiVariable: JS.JSVariableDeclarations, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(multiVariable, p);
        await this.visitNodes(multiVariable.leadingAnnotations, p);
        multiVariable.modifiers.forEach(it => this.visitModifier(it, p));

        const variables = multiVariable.variables;
        for (let i = 0; i < variables.length; i++) {
            const variable = variables[i];
            await this.beforeSyntax(variable.element, p);
            if (multiVariable.varargs) {
                p.append("...");
            }

            await this.visit(variable.element.name, p);

            await this.visitSpace(variable.after, p);
            if (multiVariable.typeExpression) {
                await this.visit(multiVariable.typeExpression, p);
            }

            if (variable.element.initializer) {
                await this.visitJsLeftPaddedLocal("=", variable.element.initializer, p);
            }

            await this.afterSyntax(variable.element, p);
            if (i < variables.length - 1) {
                p.append(",");
            } else if (variable.markers.markers.find(m => m.kind === JavaMarkers.Semicolon)) {
                p.append(";");
            }
        }

        await this.afterSyntax(multiVariable, p);
        return multiVariable;
    }

    override async visitJSNamedVariable(variable: JS.JSVariableDeclarations.JSNamedVariable, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(variable, p);
        await this.visit(variable.name, p);
        variable.initializer && await this.visitJsLeftPaddedLocal("=", variable.initializer, p);
        await this.afterSyntax(variable, p);
        return variable;
    }

    override async visitArrayDimension(arrayDimension: J.ArrayDimension, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(arrayDimension, p);
        p.append("[");
        await this.visitJRightPaddedLocalSingle(arrayDimension.index, "]", p);
        await this.afterSyntax(arrayDimension, p);
        return arrayDimension;
    }

    override async visitArrayType(arrayType: J.ArrayType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(arrayType, p);
        let type: TypeTree = arrayType;

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
        await this.visitJLeftPaddedLocal("?", ternary.truePart, p);
        await this.visitJLeftPaddedLocal(":", ternary.falsePart, p);
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
        await this.visitJLeftPaddedLocal("while", doWhileLoop.whileCondition, p);
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
        await this.visitJRightPaddedLocalSingle(instanceOf.expression, "instanceof", p);
        await this.visit(instanceOf.clazz, p);
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

        variableDeclarations.modifiers.forEach(m => this.visitModifier(m, p));

        const scope = variableDeclarations.scope;
        if (scope) {
            await this.visitSpace(scope.before, p);

            switch (scope.element) {
                case JS.ScopedVariableDeclarations.Scope.Let:
                    p.append("let");
                    break;
                case JS.ScopedVariableDeclarations.Scope.Const:
                    p.append("const");
                    break;
                case JS.ScopedVariableDeclarations.Scope.Var:
                    p.append("var");
                    break;
                case JS.ScopedVariableDeclarations.Scope.Using:
                    p.append("using");
                    break;
                case JS.ScopedVariableDeclarations.Scope.Import:
                    p.append("import");
                    break;
            }
        }

        await this.visitJsRightPaddedLocal(variableDeclarations.variables, ",", p);

        await this.afterSyntax(variableDeclarations, p);
        return variableDeclarations;
    }

    override async visitVariableDeclarations(multiVariable: J.VariableDeclarations, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(multiVariable, p);
        await this.visitNodes(multiVariable.leadingAnnotations, p);

        multiVariable.modifiers.forEach(it => this.visitModifier(it, p));

        const variables = multiVariable.variables;
        for (let i = 0; i < variables.length; i++) {
            const variable = variables[i];

            await this.beforeSyntax(variable.element, p);

            if (multiVariable.varargs) {
                p.append("...");
            }

            await this.visit(variable.element.name, p);
            await this.visitSpace(variable.after, p);

            if (multiVariable.typeExpression) {
                await this.visit(multiVariable.typeExpression, p);
            }

            if (variable.element.initializer) {
                await this.visitJLeftPaddedLocal("=", variable.element.initializer, p);
            }

            await this.afterSyntax(variable.element, p);

            if (i < variables.length - 1) {
                p.append(",");
            } else if (variable.markers.markers.find(m => m.kind === JavaMarkers.Semicolon)) {
                p.append(";");
            }
        }

        await this.afterSyntax(multiVariable, p);
        return multiVariable;
    }

    override async visitVariable(variable: J.VariableDeclarations.NamedVariable, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(variable, p);
        await this.visit(variable.name, p);

        variable.initializer && await this.visitJLeftPaddedLocal("=", variable.initializer, p);

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

    override async visitFunctionDeclaration(functionDeclaration: JS.FunctionDeclaration, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(functionDeclaration, p);

        functionDeclaration.modifiers.forEach((m) => this.visitModifier(m, p));

        await this.visitJsLeftPaddedLocal("function", functionDeclaration.asteriskToken, p);

        await this.visitJsLeftPaddedLocal(functionDeclaration.asteriskToken ? "*" : "", functionDeclaration.name, p);

        const typeParameters = functionDeclaration.typeParameters;
        if (typeParameters) {
            await this.visitNodes(typeParameters.annotations, p);
            await this.visitSpace(typeParameters.prefix, p);
            await this.visitMarkers(typeParameters.markers, p);
            p.append("<");
            await this.visitJRightPaddedLocal(typeParameters.typeParameters, ",", p);
            p.append(">");
        }

        await this.visitJsContainerLocal("(", functionDeclaration.parameters, ",", ")", p);

        functionDeclaration.returnTypeExpression && await this.visit(functionDeclaration.returnTypeExpression, p);
        functionDeclaration.body && await this.visit(functionDeclaration.body, p);

        await this.afterSyntax(functionDeclaration, p);
        return functionDeclaration;
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
                keyword = "final";
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

    override async visitFunctionType(functionType: JS.FunctionType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(functionType, p);
        functionType.modifiers.forEach(m => this.visitModifier(m, p));

        if (functionType.constructorType) {
            await this.visitJsLeftPaddedLocal("new", functionType.constructorType, p);
        }

        const typeParameters = functionType.typeParameters;
        if (typeParameters) {
            await this.visitNodes(typeParameters.annotations, p);
            await this.visitSpace(typeParameters.prefix, p);
            await this.visitMarkers(typeParameters.markers, p);
            p.append("<");
            await this.visitJRightPaddedLocal(typeParameters.typeParameters, ",", p);
            p.append(">");
        }

        await this.visitJsContainerLocal("(", functionType.parameters, ",", ")", p);
        await this.visitJsLeftPaddedLocal("=>", functionType.returnType, p);

        await this.afterSyntax(functionType, p);
        return functionType;
    }

    override async visitClassDeclaration(classDecl: J.ClassDeclaration, p: PrintOutputCapture): Promise<J | undefined> {
        let kind = "";
        switch (classDecl.classKind.type) {
            case J.ClassType.Class:
                kind = "class";
                break;
            case J.ClassType.Enum:
                kind = "enum";
                break;
            case J.ClassType.Interface:
                kind = "interface";
                break;
            case J.ClassType.Annotation:
                kind = "@interface";
                break;
            case J.ClassType.Record:
                kind = "record";
                break;
        }

        await this.beforeSyntax(classDecl, p);
        await this.visitSpace(emptySpace, p);
        await this.visitNodes(classDecl.leadingAnnotations, p);
        classDecl.modifiers.forEach(m => this.visitModifier(m, p));
        await this.visitNodes(classDecl.classKind.annotations, p);
        await this.visitSpace(classDecl.classKind.prefix, p);
        p.append(kind);
        await this.visit(classDecl.name, p);
        classDecl.typeParameters && await this.visitJContainerLocal("<", classDecl.typeParameters, ",", ">", p);
        classDecl.primaryConstructor && await this.visitJContainerLocal("(", classDecl.primaryConstructor, ",", ")", p);
        classDecl.extends && await this.visitJLeftPaddedLocal("extends", classDecl.extends, p);
        classDecl.implements && await this.visitJContainerLocal(classDecl.classKind.type === J.ClassType.Interface ? "extends" : "implements",
            classDecl.implements, ",", null, p);
        await this.visit(classDecl.body, p);
        await this.afterSyntax(classDecl, p);

        return classDecl;
    }

    override async visitMethodDeclaration(method: J.MethodDeclaration, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(method, p);
        await this.visitSpace(emptySpace, p);
        await this.visitNodes(method.leadingAnnotations, p);
        method.modifiers.forEach(m => this.visitModifier(m, p));

        await this.visit(method.name, p);

        const typeParameters = method.typeParameters;
        if (typeParameters) {
            await this.visitNodes(typeParameters.annotations, p);
            await this.visitSpace(typeParameters.prefix, p);
            await this.visitMarkers(typeParameters.markers, p);
            p.append("<");
            await this.visitJRightPaddedLocal(typeParameters.typeParameters, ",", p);
            p.append(">");
        }

        await this.visitJContainerLocal("(", method.parameters, ",", ")", p);

        if (method.returnTypeExpression) {
            await this.visit(method.returnTypeExpression, p);
        }

        method.body && await this.visit(method.body, p);
        await this.afterSyntax(method, p);
        return method;
    }

    override async visitJSMethodDeclaration(method: JS.JSMethodDeclaration, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(method, p);
        await this.visitSpace(emptySpace, p);
        await this.visitNodes(method.leadingAnnotations, p);
        method.modifiers.forEach(it => this.visitModifier(it, p));

        await this.visit(method.name, p);

        const typeParameters = method.typeParameters;
        if (typeParameters) {
            await this.visitNodes(typeParameters.annotations, p);
            await this.visitSpace(typeParameters.prefix, p);
            await this.visitMarkers(typeParameters.markers, p);
            p.append("<");
            await this.visitJRightPaddedLocal(typeParameters.typeParameters, ",", p);
            p.append(">");
        }

        await this.visitJsContainerLocal("(", method.parameters, ",", ")", p);
        if (method.returnTypeExpression) {
            await this.visit(method.returnTypeExpression, p);
        }

        method.body && await this.visit(method.body, p);
        await this.afterSyntax(method, p);
        return method;
    }

    override async visitMethodInvocation(method: J.MethodInvocation, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(method, p);

        if (method.name.toString().length === 0) {
            method.select && await this.visitRightPadded(method.select, p);
        } else {
            method.select && await this.visitJRightPaddedLocalSingle(method.select, "", p);
            await this.visit(method.name, p);
        }

        method.typeParameters && await this.visitJContainerLocal("<", method.typeParameters, ",", ">", p);
        await this.visitJContainerLocal("(", method.arguments, ",", ")", p);

        await this.afterSyntax(method, p);
        return method;
    }

    override async visitTypeParameter(typeParameter: J.TypeParameter, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(typeParameter, p);
        await this.visitNodes(typeParameter.annotations, p);
        typeParameter.modifiers.forEach(m => this.visitModifier(m, p));
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
        arrowFunction.modifiers.forEach(m => this.visitModifier(m, p));

        const typeParameters = arrowFunction.typeParameters;
        if (typeParameters) {
            await this.visitNodes(typeParameters.annotations, p);
            await this.visitSpace(typeParameters.prefix, p);
            await this.visitMarkers(typeParameters.markers, p);
            p.append("<");
            await this.visitJRightPaddedLocal(typeParameters.typeParameters, ",", p);
            p.append(">");
        }

        if (arrowFunction.parameters.parenthesized) {
            await this.visitSpace(arrowFunction.parameters.prefix, p);
            p.append("(");
            await this.visitJRightPaddedLocal(arrowFunction.parameters.parameters, ",", p);
            p.append(")");
        } else {
            await this.visitJRightPaddedLocal(arrowFunction.parameters.parameters, ",", p);
        }

        if (arrowFunction.returnTypeExpression) {
            await this.visit(arrowFunction.returnTypeExpression, p);
        }

        await this.visitJsLeftPaddedLocal("=>", arrowFunction.body, p);

        await this.afterSyntax(arrowFunction, p);
        return arrowFunction;
    }

    override async visitConditionalType(conditionalType: JS.ConditionalType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(conditionalType, p);
        await this.visit(conditionalType.checkType, p);
        await this.visitJsContainerLocal("extends", conditionalType.condition, "", "", p);
        await this.afterSyntax(conditionalType, p);
        return conditionalType;
    }

    override async visitExpressionWithTypeArguments(type: JS.ExpressionWithTypeArguments, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(type, p);
        await this.visit(type.clazz, p);
        type.typeArguments && await this.visitJsContainerLocal("<", type.typeArguments, ",", ">", p);
        await this.afterSyntax(type, p);
        return type;
    }

    override async visitImportType(importType: JS.ImportType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(importType, p);

        if (importType.hasTypeof) {
            p.append("typeof");
            await this.visitRightPadded(importType.hasTypeof, p);
        }

        p.append("import");
        await this.visitJsContainerLocal("(", importType.argumentAndAttributes, ",", ")", p);
        importType.qualifier && await this.visitJsLeftPaddedLocal(".", importType.qualifier, p);
        importType.typeArguments && await this.visitJsContainerLocal("<", importType.typeArguments, ",", ">", p);

        await this.afterSyntax(importType, p);
        return importType;
    }

    override async visitTypeDeclaration(typeDeclaration: JS.TypeDeclaration, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(typeDeclaration, p);

        typeDeclaration.modifiers.forEach(m => this.visitModifier(m, p));

        await this.visitJsLeftPaddedLocal("type", typeDeclaration.name, p);

        const typeParameters = typeDeclaration.typeParameters;
        if (typeParameters) {
            await this.visitNodes(typeParameters.annotations, p);
            await this.visitSpace(typeParameters.prefix, p);
            await this.visitMarkers(typeParameters.markers, p);
            p.append("<");
            await this.visitJRightPaddedLocal(typeParameters.typeParameters, ",", p);
            p.append(">");
        }

        await this.visitJsLeftPaddedLocal("=", typeDeclaration.initializer, p);

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
        await this.visitJsContainerLocal("{", namedImports.elements, ",", "}", p);
        await this.afterSyntax(namedImports, p);
        return namedImports;
    }

    override async visitJsImportSpecifier(jis: JS.JsImportSpecifier, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(jis, p);

        if (jis.importType) {
            await this.visitJsLeftPaddedLocal("type", jis.importType, p);
        }

        await this.visit(jis.specifier, p);

        await this.afterSyntax(jis, p);
        return jis;
    }

    override async visitExportDeclaration(ed: JS.ExportDeclaration, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(ed, p);
        p.append("export");
        ed.modifiers.forEach(it => this.visitModifier(it, p));

        if (ed.typeOnly) {
            await this.visitJsLeftPaddedLocal("type", ed.typeOnly, p);
        }

        ed.exportClause && await this.visit(ed.exportClause, p);
        ed.moduleSpecifier && await this.visitJsLeftPaddedLocal("from", ed.moduleSpecifier, p);
        ed.attributes && await this.visit(ed.attributes, p);

        await this.afterSyntax(ed, p);
        return ed;
    }

    override async visitExportAssignment(es: JS.ExportAssignment, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(es, p);
        p.append("export");
        es.modifiers.forEach(it => this.visitModifier(it, p));

        if (es.exportEquals) {
            await this.visitJsLeftPaddedLocal("=", es.exportEquals, p);
        }

        es.expression && await this.visit(es.expression, p);
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

    override async visitIndexType(indexType: JS.IndexedAccessType.IndexType, p: PrintOutputCapture): Promise<J | undefined> {
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
        if (es.typeOnly) {
            await this.visitJsLeftPaddedLocal("type", es.typeOnly, p);
        }

        await this.visit(es.specifier, p);

        await this.afterSyntax(es, p);
        return es;
    }

    override async visitNamedExports(ne: JS.NamedExports, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(ne, p);
        await this.visitJsContainerLocal("{", ne.elements, ",", "}", p);
        await this.afterSyntax(ne, p);
        return ne;
    }

    override async visitImportAttributes(importAttributes: JS.ImportAttributes, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(importAttributes, p);

        p.append((importAttributes.token === JS.ImportAttributes.Token.With ? "with" : "assert"));
        await this.visitJsContainerLocal("{", importAttributes.elements, ",", "}", p);

        await this.afterSyntax(importAttributes, p);
        return importAttributes;
    }

    override async visitImportAttribute(importAttribute: JS.ImportAttribute, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(importAttribute, p);

        await this.visit(importAttribute.name, p);
        await this.visitJsLeftPaddedLocal(":", importAttribute.value, p);

        await this.afterSyntax(importAttribute, p);
        return importAttribute;
    }

    override async visitImportTypeAttributes(importAttributes: JS.ImportTypeAttributes, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(importAttributes, p);
        p.append("{");

        await this.visitRightPadded(importAttributes.token, p);
        p.append(":");
        await this.visitJsContainerLocal("{", importAttributes.elements, ",", "}", p);
        await this.visitSpace(importAttributes.end, p);

        p.append("}");
        await this.afterSyntax(importAttributes, p);
        return importAttributes;
    }

    override async visitArrayBindingPattern(abp: JS.ArrayBindingPattern, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(abp, p);

        await this.visitJsContainerLocal("[", abp.elements, ",", "]", p);

        await this.afterSyntax(abp, p);
        return abp;
    }

    override async visitMappedType(mappedType: JS.MappedType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(mappedType, p);
        p.append("{");

        if (mappedType.prefixToken) {
            await this.visitLeftPadded(mappedType.prefixToken, p);
        }

        if (mappedType.hasReadonly) {
            await this.visitJsLeftPaddedLocal("readonly", mappedType.hasReadonly, p);
        }

        await this.visitKeysRemapping(mappedType.keysRemapping, p);

        if (mappedType.suffixToken) {
            await this.visitLeftPadded(mappedType.suffixToken, p);
        }

        if (mappedType.hasQuestionToken) {
            await this.visitJsLeftPaddedLocal("?", mappedType.hasQuestionToken, p);
        }

        const colon = mappedType.valueType.elements[0].element.kind === J.Kind.Empty ? "" : ":";
        await this.visitJsContainerLocal(colon, mappedType.valueType, "", "", p);

        p.append("}");
        await this.afterSyntax(mappedType, p);
        return mappedType;
    }

    override async visitKeysRemapping(mappedTypeKeys: JS.MappedType.KeysRemapping, p: PrintOutputCapture): Promise<J | undefined> {
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

    override async visitMappedTypeParameter(mappedTypeParameter: JS.MappedType.MappedTypeParameter, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(mappedTypeParameter, p);
        await this.visit(mappedTypeParameter.name, p);
        await this.visitJsLeftPaddedLocal("in", mappedTypeParameter.iterateType, p);
        await this.afterSyntax(mappedTypeParameter, p);
        return mappedTypeParameter;
    }

    override async visitObjectBindingDeclarations(objectBindingDeclarations: JS.ObjectBindingDeclarations, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(objectBindingDeclarations, p);
        await this.visitNodes(objectBindingDeclarations.leadingAnnotations, p);
        objectBindingDeclarations.modifiers.forEach(m => this.visitModifier(m, p));

        objectBindingDeclarations.typeExpression && await this.visit(objectBindingDeclarations.typeExpression, p);
        await this.visitJsContainerLocal("{", objectBindingDeclarations.bindings, ",", "}", p);
        objectBindingDeclarations.initializer && await this.visitJsLeftPaddedLocal("=", objectBindingDeclarations.initializer, p);
        await this.afterSyntax(objectBindingDeclarations, p);
        return objectBindingDeclarations;
    }

    override async visitTaggedTemplateExpression(taggedTemplateExpression: JS.TaggedTemplateExpression, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(taggedTemplateExpression, p);
        taggedTemplateExpression.tag && await this.visitRightPadded(taggedTemplateExpression.tag, p);
        taggedTemplateExpression.typeArguments && await this.visitJsContainerLocal("<", taggedTemplateExpression.typeArguments, ",", ">", p);
        await this.visit(taggedTemplateExpression.templateExpression, p);
        await this.afterSyntax(taggedTemplateExpression, p);
        return taggedTemplateExpression;
    }

    override async visitTemplateExpression(templateExpression: JS.TemplateExpression, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(templateExpression, p);
        await this.visit(templateExpression.head, p);
        await this.visitJsRightPaddedLocal(templateExpression.templateSpans, "", p);
        await this.afterSyntax(templateExpression, p);
        return templateExpression;
    }

    override async visitTemplateSpan(value: JS.TemplateExpression.TemplateSpan, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(value, p);
        await this.visit(value.expression, p);
        await this.visit(value.tail, p);
        await this.afterSyntax(value, p);
        return value;
    }

    override async visitTuple(tuple: JS.Tuple, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(tuple, p);
        await this.visitJsContainerLocal("[", tuple.elements, ",", "]", p);
        await this.afterSyntax(tuple, p);
        return tuple;
    }

    override async visitTypeQuery(typeQuery: JS.TypeQuery, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(typeQuery, p);
        p.append("typeof");
        await this.visit(typeQuery.typeExpression, p);
        typeQuery.typeArguments && await this.visitJsContainerLocal("<", typeQuery.typeArguments, ",", ">", p);
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

        if (typePredicate.asserts) {
            await this.visitJsLeftPaddedLocal("asserts", typePredicate.asserts, p);
        }

        await this.visit(typePredicate.parameterName, p);
        typePredicate.expression && await this.visitJsLeftPaddedLocal("is", typePredicate.expression, p);

        await this.afterSyntax(typePredicate, p);
        return typePredicate;
    }

    override async visitIndexSignatureDeclaration(isd: JS.IndexSignatureDeclaration, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(isd, p);

        isd.modifiers.forEach(m => this.visitModifier(m, p));
        await this.visitJsContainerLocal("[", isd.parameters, "", "]", p);
        await this.visitJsLeftPaddedLocal(":", isd.typeExpression, p);

        await this.afterSyntax(isd, p);
        return isd;
    }

    override async visitAnnotation(annotation: J.Annotation, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(annotation, p);

        p.append("@");
        await this.visit(annotation.annotationType, p);
        annotation.arguments && await this.visitJContainerLocal("(", annotation.arguments, ",", ")", p);

        await this.afterSyntax(annotation, p);
        return annotation;
    }

    override async visitNewArray(newArray: J.NewArray, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(newArray, p);
        newArray.typeExpression && await this.visit(newArray.typeExpression, p);
        await this.visitNodes(newArray.dimensions, p);
        newArray.initializer && await this.visitJContainerLocal("[", newArray.initializer, ",", "]", p);
        await this.afterSyntax(newArray, p);
        return newArray;
    }

    override async visitNewClass(newClass: J.NewClass, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(newClass, p);
        newClass.enclosing && await this.visitJRightPaddedLocalSingle(newClass.enclosing, ".", p);
        await this.visitSpace(newClass.new, p);

        if (newClass.clazz) {
            p.append("new");
            await this.visit(newClass.clazz, p);

            if (!newClass.arguments.markers.markers.find(m => m.kind === JavaMarkers.OmitParentheses)) {
                await this.visitJContainerLocal("(", newClass.arguments, ",", ")", p);
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

        await this.visitJContainerLocal("", case_.caseLabels, ",", "", p);

        await this.visitSpace(case_.statements.before, p);
        p.append(case_.type === J.Case.Type.Statement ? ":" : "->");

        await this.visitStatements(case_.statements.elements, p);

        await this.afterSyntax(case_, p);
        return case_;
    }

    override async visitLabel(label: J.Label, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(label, p);
        await this.visitJRightPaddedLocalSingle(label.label, ":", p);
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

        await this.visitJLeftPaddedLocal(".", fieldAccess.name, p);
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
        await this.visitJRightPaddedLocalSingle(parens.tree, ")", p);
        await this.afterSyntax(parens, p);
        return parens;
    }

    override async visitParameterizedType(type: J.ParameterizedType, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(type, p);
        await this.visit(type.clazz, p);
        type.typeParameters && await this.visitJContainerLocal("<", type.typeParameters, ",", ">", p);
        await this.afterSyntax(type, p);
        return type;
    }

    override async visitAssignment(assignment: J.Assignment, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(assignment, p);
        await this.visit(assignment.variable, p);
        await this.visitJLeftPaddedLocal("=", assignment.assignment, p);
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

    override async visitJsAssignmentOperation(assignOp: JS.JsAssignmentOperation, p: PrintOutputCapture): Promise<J | undefined> {
        let keyword = "";
        switch (assignOp.operator.element) {
            case JS.JsAssignmentOperation.Type.QuestionQuestion:
                keyword = "??=";
                break;
            case JS.JsAssignmentOperation.Type.And:
                keyword = "&&=";
                break;
            case JS.JsAssignmentOperation.Type.Or:
                keyword = "||=";
                break;
            case JS.JsAssignmentOperation.Type.Power:
                keyword = "**";
                break;
            case JS.JsAssignmentOperation.Type.Exp:
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
        await this.visitJRightPaddedLocal(enums.enums, ",", p);

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

    override async visitJsBinary(binary: JS.JsBinary, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(binary, p);

        await this.visit(binary.left, p);
        let keyword = "";

        switch (binary.operator.element) {
            case JS.JsBinary.Type.As:
                keyword = "as";
                break;
            case JS.JsBinary.Type.IdentityEquals:
                keyword = "===";
                break;
            case JS.JsBinary.Type.IdentityNotEquals:
                keyword = "!==";
                break;
            case JS.JsBinary.Type.In:
                keyword = "in";
                break;
            case JS.JsBinary.Type.QuestionQuestion:
                keyword = "??";
                break;
            case JS.JsBinary.Type.Comma:
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

    override async visitJsUnary(unary: JS.Unary, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(unary, p);

        switch (unary.operator.element) {
            case JS.Unary.Type.Spread:
                await this.visitSpace(unary.operator.before, p);
                p.append("...");
                await this.visit(unary.expression, p);
                break;
            case JS.Unary.Type.Optional:
                await this.visit(unary.expression, p);
                await this.visitSpace(unary.operator.before, p);
                p.append("?");
                break;
            case JS.Unary.Type.Exclamation:
                await this.visit(unary.expression, p);
                await this.visitSpace(unary.operator.before, p);
                p.append("!");
                break;
            case JS.Unary.Type.QuestionDot:
                await this.visit(unary.expression, p);
                await this.visitSpace(unary.operator.before, p);
                p.append("?");
                break;
            case JS.Unary.Type.QuestionDotWithDot:
                await this.visit(unary.expression, p);
                await this.visitSpace(unary.operator.before, p);
                p.append("?.");
                break;
            case JS.Unary.Type.Asterisk:
                p.append("*");
                await this.visitSpace(unary.operator.before, p);
                await this.visit(unary.expression, p);
                break;
            default:
                break;
        }

        await this.afterSyntax(unary, p);
        return unary;
    }

    override async visitUnion(union: JS.Union, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(union, p);

        await this.visitJsRightPaddedLocal(union.types, "|", p);

        await this.afterSyntax(union, p);
        return union;
    }

    override async visitIntersection(intersection: JS.Intersection, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(intersection, p);

        await this.visitJsRightPaddedLocal(intersection.types, "&", p);

        await this.afterSyntax(intersection, p);
        return intersection;
    }

    override async visitForLoop(forLoop: J.ForLoop, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(forLoop, p);
        p.append("for");
        const ctrl = forLoop.control;
        await this.visitSpace(ctrl.prefix, p);
        p.append('(');
        await this.visitJRightPaddedLocal(ctrl.init, ",", p);
        p.append(';');
        ctrl.condition && await this.visitJRightPaddedLocalSingle(ctrl.condition, ";", p);
        await this.visitJRightPaddedLocal(ctrl.update, ",", p);
        p.append(')');
        await this.visitStatementLocal(forLoop.body, p);
        await this.afterSyntax(forLoop, p);
        return forLoop;
    }

    override async visitJSForOfLoop(loop: JS.JSForOfLoop, p: PrintOutputCapture): Promise<J | undefined> {
        await this.beforeSyntax(loop, p);
        p.append("for");
        if (loop.await) {
            await this.visitJsLeftPaddedLocal("await", loop.await, p);
        }

        const control = loop.control;
        await this.visitSpace(control.prefix, p);
        p.append('(');
        await this.visitRightPadded(control.variable, p);
        p.append("of");
        await this.visitRightPadded(control.iterable, p);
        p.append(')');
        await this.visitRightPadded(loop.body, p);
        await this.afterSyntax(loop, p);
        return loop;
    }

    override async visitJSForInLoop(loop: JS.JSForInLoop, p: PrintOutputCapture): Promise<J | undefined> {
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

    private async afterSyntax(j: J, p: PrintOutputCapture) {
        await this.afterSyntaxMarkers(j.markers, p);
    }

    private async afterSyntaxMarkers(markers: Markers, p: PrintOutputCapture) {
        for (const marker of markers.markers) {
            p.out.concat(p.markerPrinter.afterSyntax(marker, new Cursor(marker, this.cursor), this.JAVA_SCRIPT_MARKER_WRAPPER));
        }
    }

    private async beforeSyntax(j: J, p: PrintOutputCapture) {
        await this.beforeSyntaxExt(j.prefix, j.markers, p);
    }

    private async beforeSyntaxExt(prefix: Space, markers: Markers, p: PrintOutputCapture) {
        for (const marker of markers.markers) {
            p.out.concat(
                p.markerPrinter.beforePrefix(marker, new Cursor(marker, this.cursor), this.JAVA_SCRIPT_MARKER_WRAPPER)
            );
        }

        await this.visitSpace(prefix, p);
        await this.visitMarkers(markers, p);

        for (const marker of markers.markers) {
            p.out.concat(
                p.markerPrinter.beforeSyntax(marker, new Cursor(marker, this.cursor), this.JAVA_SCRIPT_MARKER_WRAPPER)
            );
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

    private async visitJRightPaddedLocal(nodes: J.RightPadded<J>[], suffixBetween: string, p: PrintOutputCapture) {
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

    private async visitJRightPaddedLocalSingle(node: J.RightPadded<J> | undefined, suffix: string, p: PrintOutputCapture) {
        if (node) {
            await this.visit(node.element, p);

            await this.visitSpace(node.after, p);
            await this.visitMarkers(node.markers, p);

            p.append(suffix);
        }
    }

    private async visitJsRightPaddedLocal(nodes: J.RightPadded<J>[], suffixBetween: string, p: PrintOutputCapture) {
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

    private async visitJLeftPaddedLocal(prefix: string | null, leftPadded: J.LeftPadded<J> | J.LeftPadded<boolean> | undefined, p: PrintOutputCapture) {
        if (leftPadded) {
            await this.beforeSyntaxExt(leftPadded.before, leftPadded.markers, p);

            if (prefix) {
                p.append(prefix);
            }

            if (typeof leftPadded.element !== 'boolean') {
                await this.visit(leftPadded.element, p);
            }

            await this.afterSyntaxMarkers(leftPadded.markers, p);
        }
    }

    private async visitJsLeftPaddedLocal(prefix: string | undefined, leftPadded: J.LeftPadded<J> | J.LeftPadded<boolean> | undefined, p: PrintOutputCapture) {
        if (leftPadded) {
            await this.beforeSyntaxExt(leftPadded.before, leftPadded.markers, p);

            if (prefix) {
                p.append(prefix);
            }

            if (typeof leftPadded.element !== 'boolean') {
                await this.visit(leftPadded.element, p);
            }

            await this.afterSyntaxMarkers(leftPadded.markers, p);
        }
    }

    private async visitJContainerLocal(before: string, container: J.Container<J> | undefined, suffixBetween: string, after: string | null, p: PrintOutputCapture) {
        if (!container) {
            return;
        }

        await this.beforeSyntaxExt(container.before, container.markers, p);

        p.append(before);
        await this.visitJRightPaddedLocal(container.elements, suffixBetween, p);
        await this.afterSyntaxMarkers(container.markers, p);

        p.append(after === null ? "" : after);
    }

    private async visitJsContainerLocal(before: string, container: J.Container<J> | undefined, suffixBetween: string, after: string | null, p: PrintOutputCapture) {
        if (container === undefined) {
            return;
        }

        await this.beforeSyntaxExt(container.before, container.markers, p);

        p.append(before);
        await this.visitJsRightPaddedLocal(container.elements, suffixBetween, p);
        await this.afterSyntaxMarkers(container.markers, p);

        p.append(after === null ? "" : after);
    }

    override async visitMarker<M extends Marker>(marker: M, p: PrintOutputCapture): Promise<M> {
        if (marker.kind === JavaMarkers.Semicolon) {
            p.append(';');
        }
        if (marker.kind === JavaMarkers.TrailingComma) {
            p.append(',');
            await this.visitSpace((marker as unknown as TrailingComma).suffix, p);
        }
        return marker;
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
            await this.visitJRightPaddedLocalSingle(controlParens.tree, ">", p);
        } else {
            p.append('(');
            await this.visitJRightPaddedLocalSingle(controlParens.tree, ")", p);
        }

        await this.afterSyntax(controlParens, p);
        return controlParens;
    }
}

TreePrinters.register(JS.Kind.CompilationUnit, new JavaScriptPrinter());
