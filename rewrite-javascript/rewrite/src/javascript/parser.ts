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
import ts from 'typescript';
import {
    Comment,
    emptyContainer,
    emptySpace,
    Expression,
    J,
    JavaType,
    NameTree,
    Statement,
    TextComment,
    TrailingComma,
    TypeTree,
    VariableDeclarator,
} from '../java';
import {Asterisk, DelegatedYield, FunctionDeclaration, JS, JSX, NonNullAssertion, Optional, Spread} from '.';
import {
    emptyMarkers,
    markers,
    Markers,
    MarkersKind,
    NamedStyles,
    ParseExceptionResult,
    Parser,
    ParserInput,
    parserInputFile,
    parserInputRead,
    ParserOptions,
    Parsers,
    randomId,
    SourceFile,
    SourcePath
} from "..";
import {
    binarySearch,
    checkSyntaxErrors,
    compareTextSpans,
    getNextSibling,
    getPreviousSibling,
    hasFlowAnnotation,
    isStatement,
    isValidSurrogateRange,
    TextSpan
} from "./parser-utils";
import {JavaScriptTypeMapping} from "./type-mapping";
import {produce} from "immer";
import Kind = JS.Kind;
import ComputedPropertyName = JS.ComputedPropertyName;
import Attribute = JSX.Attribute;
import SpreadAttribute = JSX.SpreadAttribute;

export interface JavaScriptParserOptions extends ParserOptions {
    styles?: NamedStyles[]
}

export class JavaScriptParser extends Parser {

    private readonly compilerOptions: ts.CompilerOptions;
    private readonly styles?: NamedStyles[];
    private oldProgram?: ts.Program;

    constructor(
        {
            ctx,
            relativeTo,
            styles
        }: JavaScriptParserOptions = {},
        private readonly sourceFileCache: Map<String, ts.SourceFile> = new Map()
    ) {
        super({ctx, relativeTo});
        this.compilerOptions = {
            target: ts.ScriptTarget.Latest,
            module: ts.ModuleKind.CommonJS,
            allowJs: true,
            esModuleInterop: true,
            experimentalDecorators: true,
            emitDecoratorMetadata: true,
            jsx: ts.JsxEmit.Preserve
        };
        this.styles = styles;
    }

    // noinspection JSUnusedGlobalSymbols
    reset(): this {
        this.sourceFileCache.clear();
        this.oldProgram = undefined;
        return this;
    }

    async parse(...inputs: ParserInput[]): Promise<SourceFile[]> {
        const inputFiles = new Map<SourcePath, ParserInput>();

        // Populate inputFiles map and remove from cache if necessary
        for (const input of inputs) {
            const sourcePath = parserInputFile(input);
            inputFiles.set(sourcePath, input);
            // Remove from cache if previously cached
            this.sourceFileCache.delete(sourcePath);
        }

        // Create a new CompilerHost within parseInputs
        const host = ts.createCompilerHost(this.compilerOptions);

        // Override getSourceFile
        host.getSourceFile = (fileName, languageVersion, onError) => {
            // Check if the SourceFile is in the cache
            let sourceFile = this.sourceFileCache.get(fileName);
            if (sourceFile) {
                return sourceFile;
            }

            // Read the file content
            let sourceText: string | undefined;

            // For input files
            const input = inputFiles.get(fileName);
            if (input) {
                sourceText = parserInputRead(input);
            } else {
                // For dependency files
                sourceText = ts.sys.readFile(fileName);
            }

            if (sourceText !== undefined) {
                sourceFile = ts.createSourceFile(fileName, sourceText, languageVersion, true);
                // Cache the SourceFile if it's a dependency
                if (!input) {
                    this.sourceFileCache.set(fileName, sourceFile);
                }
                return sourceFile;
            }

            if (onError) onError(`File not found: ${fileName}`);
            return undefined;
        };

        // Override fileExists
        host.fileExists = (fileName) => {
            return inputFiles.has(fileName) || ts.sys.fileExists(fileName);
        };

        // Override readFile
        host.readFile = (fileName) => {
            const input = inputFiles.get(fileName);
            return input ? parserInputRead(input) : ts.sys.readFile(fileName);
        };

        // Create a new Program, passing the oldProgram for incremental parsing
        const program = ts.createProgram([...inputFiles.keys()], this.compilerOptions, host, this.oldProgram);

        // Update the oldProgram reference
        this.oldProgram = program;

        const typeChecker = program.getTypeChecker();

        const result: SourceFile[] = [];
        for (const input of inputFiles.values()) {
            const filePath = parserInputFile(input);
            const sourceFile = program.getSourceFile(filePath);
            if (!sourceFile) {
                result.push(this.error(input, new Error('Parser returned undefined')));
                continue;
            }

            if (hasFlowAnnotation(sourceFile)) {
                result.push(this.error(input, new FlowSyntaxNotSupportedError("Flow syntax not supported")));
                continue;
            }

            const syntaxErrors = checkSyntaxErrors(program, sourceFile);
            if (syntaxErrors.length > 0) {
                let errors = syntaxErrors.map(e => `${e[0]} [${e[1]}]`).join('; ');
                result.push(this.error(input, new SyntaxError(`Compiler error(s): ${errors}`)))
                continue;
            }

            try {
                result.push(produce(
                    new JavaScriptParserVisitor(sourceFile, this.relativePath(input), typeChecker)
                        .visit(sourceFile) as SourceFile,
                    draft => {
                        if (this.styles) {
                            draft.markers.markers = draft.markers.markers.concat(this.styles);
                        }
                    }));
            } catch (error) {
                result.push(this.error(input, error instanceof Error ? error : new Error('Parser threw unknown error: ' + error)));
            }
        }

        return result;
    }
}

// we use this instead of `ts.SyntaxKind[node.kind]` because the numeric values are not unique, and we want
// the first one rather than the last one, as the last ones are things like `FirstToken`, `LastToken`, etc.
const visitMethodMap = new Map<number, string>();
for (const [key, value] of Object.entries(ts.SyntaxKind)) {
    if (typeof value === 'number' && !visitMethodMap.has(value)) {
        visitMethodMap.set(value, 'visit' + key);
    }
}

// noinspection JSUnusedGlobalSymbols
export class JavaScriptParserVisitor {
    private readonly typeMapping: JavaScriptTypeMapping;

    constructor(
        private readonly sourceFile: ts.SourceFile,
        private readonly sourcePath: string,
        typeChecker: ts.TypeChecker) {
        this.typeMapping = new JavaScriptTypeMapping(typeChecker);
    }

    visit = (node: ts.Node): any => {
        const member = this[(visitMethodMap.get(node.kind) as keyof JavaScriptParserVisitor)];
        if (typeof member === 'function') {
            return member.bind(this)(node as any);
        } else {
            return this.visitUnknown(node);
        }
    }

    convert = <T extends J>(node: ts.Node): T => {
        return this.visit(node) as T;
    }

    detectBOMAndTextEncoding(content: String): { hasBom: boolean; encoding: string | undefined } {
        const BOM_UTF8 = "\uFEFF"; // BOM for UTF-8
        const BOM_UTF16_LE = [0xFF, 0xFE]; // BOM for UTF-16 Little Endian

        // Detect BOM
        const hasUtf8Bom = content.startsWith(BOM_UTF8);
        const hasUtf16LeBom = content.charCodeAt(0) === BOM_UTF16_LE[0] && content.charCodeAt(1) === BOM_UTF16_LE[1];

        if (hasUtf8Bom) {
            return {hasBom: true, encoding: 'utf8'};
        } else if (hasUtf16LeBom) {
            return {hasBom: true, encoding: 'utf16le'};
        }

        return {hasBom: false, encoding: undefined};
    }

    visitSourceFile(node: ts.SourceFile): JS.CompilationUnit {

        let bomAndTextEncoding = this.detectBOMAndTextEncoding(node.getFullText());

        let prefix = this.prefix(node);
        if (bomAndTextEncoding.hasBom) {
            // If a node full text has a BOM marker, it becomes a part of the prefix, so we remove it
            if (bomAndTextEncoding.encoding === 'utf8') {
                prefix = produce(prefix, draft => {
                    draft.whitespace = prefix.whitespace!.slice(1);
                });
            } else if (bomAndTextEncoding.encoding === 'utf16le') {
                prefix = produce(prefix, draft => {
                    draft.whitespace = prefix.whitespace!.slice(2);
                });
            }
        }

        return {
            kind: JS.Kind.CompilationUnit,
            id: randomId(),
            prefix: prefix,
            markers: emptyMarkers,
            sourcePath: this.sourcePath,
            charsetName: bomAndTextEncoding.encoding,
            charsetBomMarked: bomAndTextEncoding.hasBom,
            statements: this.semicolonPaddedStatementList(node.statements),
            eof: this.prefix(node.endOfFileToken)
        };
    }

    private semicolonPaddedStatementList(statements: ts.NodeArray<ts.Statement>) {
        return [...statements].map(n => {
            const j: Statement = this.convert(n);
            if (j.kind === J.Kind.Unknown) {
                // in case of `J.Unknown` its source will already contain any `;`
                return this.rightPadded(j, emptySpace, emptyMarkers);
            }
            return this.rightPadded(j, this.semicolonPrefix(n), (n => {
                const last = n.getChildAt(n.getChildCount(this.sourceFile) - 1, this.sourceFile);
                return last?.kind === ts.SyntaxKind.SemicolonToken ? markers({
                    kind: J.Markers.Semicolon,
                    id: randomId()
                }) : emptyMarkers;
            })?.(n));
        });
    }

    visitUnknown(node: ts.Node): J.Unknown {
        return {
            kind: J.Kind.Unknown,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            source: {
                kind: J.Kind.UnknownSource,
                id: randomId(),
                prefix: emptySpace,
                markers: markers({
                    kind: MarkersKind.ParseExceptionResult,
                    parserType: "JavaScriptParser",
                    exceptionType: "Error",
                    message: "Unsupported AST element: " + node,
                    treeType: visitMethodMap.get(node.kind)!.substring(5)
                } as ParseExceptionResult),
                text: node.getFullText()
            }
        };
    }

    private mapModifiers(node: ts.VariableDeclarationList | ts.VariableStatement | ts.ClassDeclaration | ts.PropertyDeclaration
        | ts.FunctionDeclaration | ts.ParameterDeclaration | ts.MethodDeclaration | ts.EnumDeclaration | ts.InterfaceDeclaration
        | ts.PropertySignature | ts.ConstructorDeclaration | ts.ModuleDeclaration | ts.GetAccessorDeclaration | ts.SetAccessorDeclaration
        | ts.ArrowFunction | ts.IndexSignatureDeclaration | ts.TypeAliasDeclaration | ts.ExportDeclaration | ts.ExportAssignment | ts.FunctionExpression
        | ts.ConstructorTypeNode | ts.TypeParameterDeclaration | ts.ImportDeclaration | ts.ImportEqualsDeclaration): J.Modifier[] {
        if (ts.isVariableStatement(node) || ts.isModuleDeclaration(node) || ts.isClassDeclaration(node) || ts.isEnumDeclaration(node)
            || ts.isInterfaceDeclaration(node) || ts.isPropertyDeclaration(node) || ts.isPropertySignature(node) || ts.isParameter(node)
            || ts.isMethodDeclaration(node) || ts.isConstructorDeclaration(node) || ts.isArrowFunction(node)
            || ts.isIndexSignatureDeclaration(node) || ts.isTypeAliasDeclaration(node) || ts.isExportDeclaration(node)
            || ts.isFunctionDeclaration(node) || ts.isFunctionExpression(node) || ts.isConstructorTypeNode(node) || ts.isTypeParameterDeclaration(node) || ts.isImportDeclaration(node) || ts.isImportEqualsDeclaration(node)) {
            return node.modifiers ? node.modifiers?.filter(ts.isModifier).map(this.mapModifier) : [];
        } else if (ts.isExportAssignment(node)) {
            const defaultModifier = this.findChildNode(node, ts.SyntaxKind.DefaultKeyword);
            return [
                ...node.modifiers ? node.modifiers?.filter(ts.isModifier).map(this.mapModifier) : [],
                ...defaultModifier && ts.isModifier(defaultModifier) ? [this.mapModifier(defaultModifier)] : []
            ]
        } else if (ts.isVariableDeclarationList(node)) {
            let modifier: string | undefined;
            if ((node.flags & ts.NodeFlags.Let) !== 0) {
                modifier = "let";
            } else if ((node.flags & ts.NodeFlags.Const) !== 0) {
                modifier = "const";
            } else {
                modifier = "var";
            }
            return modifier ? [{
                kind: J.Kind.Modifier,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                keyword: "let",
                type: J.ModifierType.LanguageExtension,
                annotations: []
            }] : [];
        } else if (ts.isGetAccessorDeclaration(node)) {
            return (node.modifiers ? node.modifiers?.filter(ts.isModifier).map(this.mapModifier) : []).concat({
                kind: J.Kind.Modifier,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.GetKeyword)!),
                markers: emptyMarkers,
                keyword: 'get',
                type: J.ModifierType.LanguageExtension,
                annotations: []
            });
        } else if (ts.isSetAccessorDeclaration(node)) {
            return (node.modifiers ? node.modifiers?.filter(ts.isModifier).map(this.mapModifier) : []).concat({
                kind: J.Kind.Modifier,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.SetKeyword)!),
                markers: emptyMarkers,
                keyword: 'set',
                type: J.ModifierType.LanguageExtension,
                annotations: []
            });
        }
        throw new Error(`Cannot get modifiers from ${node}`);
    }

    private mapModifier = (node: ts.Modifier | ts.ModifierLike): J.Modifier => {
        let kind: J.ModifierType;
        switch (node.kind) {
            case ts.SyntaxKind.PublicKeyword:
                kind = J.ModifierType.Public;
                break;
            case ts.SyntaxKind.PrivateKeyword:
                kind = J.ModifierType.Private;
                break;
            case ts.SyntaxKind.ProtectedKeyword:
                kind = J.ModifierType.Protected;
                break;
            case ts.SyntaxKind.StaticKeyword:
                kind = J.ModifierType.Static;
                break;
            case ts.SyntaxKind.AbstractKeyword:
                kind = J.ModifierType.Abstract;
                break;
            default:
                kind = J.ModifierType.LanguageExtension;
        }
        return {
            kind: J.Kind.Modifier,
            id: randomId(),
            prefix: this.prefix(node!),
            markers: emptyMarkers,
            keyword: kind === J.ModifierType.LanguageExtension ? node.getText() : undefined,
            type: kind,
            annotations: []
        };
    }

    private rightPadded<T extends J | boolean>(t: T, trailing: J.Space, markers?: Markers): J.RightPadded<T> {
        return {
            kind: J.Kind.RightPadded,
            element: t,
            after: trailing,
            markers: markers ?? emptyMarkers
        };
    }

    private rightPaddedList<N extends ts.Node, T extends J>(nodes: N[], trailing: (node: N) => J.Space, markers?: (node: N) => Markers): J.RightPadded<T>[] {
        return nodes.map(n => this.rightPadded(this.convert(n), trailing(n), markers?.(n)));
    }

    private rightPaddedSeparatedList<N extends ts.Node, T extends J>(nodes: N[], markers?: (nodes: N[], i: number) => Markers): J.RightPadded<T>[] {
        if (nodes.length === 0) {
            return [];
        }
        const ts: J.RightPadded<T>[] = [];

        for (let i = 0; i < nodes.length - 1; i += 2) {
            // FIXME right padding and trailing comma
            ts.push(this.rightPadded(
                this.convert(nodes[i]),
                this.prefix(nodes[i + 1]),
                markers ? markers(nodes, i) : emptyMarkers
            ));
        }
        if ((nodes.length & 1) === 1) {
            ts.push(this.rightPadded(this.convert(nodes[nodes.length - 1]), emptySpace, markers ? markers(nodes, nodes.length - 1) : emptyMarkers));
        }

        return ts;
    }

    private leftPadded<T extends J | J.Space | number | string | boolean>(before: J.Space, t: T, markers?: Markers): J.LeftPadded<T> {
        return {
            kind: J.Kind.LeftPadded,
            before: before,
            element: t,
            markers: markers ?? emptyMarkers
        };
    }

    private semicolonPrefix = (node: ts.Node) => {
        const last = node.getChildren(this.sourceFile).slice(-1)[0];
        return last?.kind === ts.SyntaxKind.SemicolonToken ? this.prefix(last) : emptySpace;
    }

    private keywordPrefix = (token: ts.PunctuationSyntaxKind, findSibling: (node: ts.Node) => ts.Node | undefined) => (node: ts.Node): J.Space => {
        const last = findSibling(node);
        return last?.kind === token ? this.prefix(last) : emptySpace;
    }

    visitClassDeclaration(node: ts.ClassDeclaration): J.ClassDeclaration {
        return {
            kind: J.Kind.ClassDeclaration,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: this.mapDecorators(node),
            modifiers: this.mapModifiers(node),
            classKind: {
                kind: J.Kind.ClassDeclarationKind,
                id: randomId(),
                prefix: node.modifiers ? this.suffix(node.modifiers[node.modifiers.length - 1]) : this.prefix(node),
                markers: emptyMarkers,
                annotations: [],
                type: J.ClassDeclaration.Kind.Type.Class
            },
            name: node.name ? this.convert(node.name) : this.mapIdentifier(node, ""),
            typeParameters: this.mapTypeParametersAsContainer(node),
            primaryConstructor: undefined, // FIXME primary constructor
            extends: this.mapExtends(node),
            implements: this.mapImplements(node),
            body: {
                kind: J.Kind.Block,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBraceToken)!),
                markers: emptyMarkers,
                static: this.rightPadded(false, emptySpace, emptyMarkers),
                statements: node.members.map((ce: ts.ClassElement) => this.rightPadded(
                    this.convert(ce),
                    ce.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken ? this.prefix(ce.getLastToken()!) : emptySpace,
                    ce.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken ? markers({
                        kind: J.Markers.Semicolon,
                        id: randomId()
                    }) : emptyMarkers
                )),
                end: this.prefix(node.getLastToken()!)
            },
            type: this.mapType(node)
        };
    }

    private mapExtends(node: ts.ClassDeclaration | ts.ClassExpression): J.LeftPadded<TypeTree> | undefined {
        if (node.heritageClauses == undefined || node.heritageClauses.length == 0) {
            return undefined;
        }
        for (let heritageClause of node.heritageClauses) {
            if (heritageClause.token == ts.SyntaxKind.ExtendsKeyword) {
                const expression = this.visit(heritageClause.types[0]);
                return this.leftPadded<TypeTree>(this.prefix(heritageClause.getFirstToken()!), {
                    kind: JS.Kind.TypeTreeExpression,
                    id: randomId(),
                    prefix: emptySpace,
                    markers: emptyMarkers,
                    expression: expression
                } as JS.TypeTreeExpression);
            }
        }
        return undefined;
    }

    private mapInterfaceExtends(node: ts.InterfaceDeclaration): J.Container<TypeTree> | undefined {
        if (node.heritageClauses == undefined || node.heritageClauses.length == 0) {
            return undefined;
        }
        for (let heritageClause of node.heritageClauses) {
            if ((heritageClause.token == ts.SyntaxKind.ExtendsKeyword)) {
                const _extends: J.RightPadded<TypeTree>[] = [];
                for (let type of heritageClause.types) {
                    _extends.push(this.rightPadded(this.visit(type), this.suffix(type)));
                }
                return _extends.length > 0 ? {
                    kind: J.Kind.Container,
                    before: this.prefix(heritageClause.getFirstToken()!),
                    elements: _extends,
                    markers: emptyMarkers
                } : undefined;
            }
        }
        return undefined;
    }

    private mapImplements(node: ts.ClassDeclaration | ts.ClassExpression): J.Container<TypeTree> | undefined {
        if (node.heritageClauses == undefined || node.heritageClauses.length == 0) {
            return undefined;
        }
        for (let heritageClause of node.heritageClauses) {
            if (heritageClause.token == ts.SyntaxKind.ImplementsKeyword) {
                const _implements: J.RightPadded<TypeTree>[] = [];
                for (let type of heritageClause.types) {
                    _implements.push(this.rightPadded(this.visit(type), this.suffix(type)));
                }
                return _implements.length > 0 ? {
                    kind: J.Kind.Container,
                    before: this.prefix(heritageClause.getFirstToken()!),
                    elements: _implements,
                    markers: emptyMarkers
                } : undefined;
            }
        }
        return undefined;
    }

    visitNumericLiteral(node: ts.NumericLiteral): J.Literal {
        return this.mapLiteral(node, node.text); // FIXME value not in AST
    }

    visitTrueKeyword(node: ts.TrueLiteral): J.Literal {
        return this.mapLiteral(node, true);
    }

    visitNumberKeyword(node: ts.Node): J.Identifier {
        return this.mapIdentifier(node, 'number');
    }

    visitBooleanKeyword(node: ts.Node): J.Identifier {
        return this.mapIdentifier(node, 'boolean');
    }

    visitStringKeyword(node: ts.Node): J.Identifier {
        return this.mapIdentifier(node, 'string');
    }

    visitUndefinedKeyword(node: ts.Node): J.Identifier {
        return this.mapIdentifier(node, 'undefined');
    }

    visitAnyKeyword(node: ts.Node): J.Identifier {
        return this.mapIdentifier(node, 'any');
    }

    visitIntrinsicKeyword(node: ts.Node): J.Identifier {
        return this.mapIdentifier(node, 'intrinsic');
    }

    visitObjectKeyword(node: ts.Node): J.Identifier {
        return this.mapIdentifier(node, 'object');
    }

    visitUnknownKeyword(node: ts.Node): J.Identifier {
        return this.mapIdentifier(node, 'unknown');
    }

    visitVoidKeyword(node: ts.Node): J.Identifier {
        return this.mapIdentifier(node, 'void');
    }

    visitFalseKeyword(node: ts.FalseLiteral): J.Literal {
        return this.mapLiteral(node, false);
    }

    visitNullKeyword(node: ts.NullLiteral): J.Literal {
        return this.mapLiteral(node, null);
    }

    visitNeverKeyword(node: ts.Node): J.Identifier {
        return this.mapIdentifier(node, 'never');
    }

    visitSymbolKeyword(node: ts.Node): J.Identifier {
        return this.mapIdentifier(node, 'symbol');
    }

    visitBigIntKeyword(node: ts.Node): J.Identifier {
        return this.mapIdentifier(node, 'bigint');
    }

    private mapLiteral(node: ts.LiteralExpression | ts.TrueLiteral | ts.FalseLiteral | ts.NullLiteral | ts.Identifier
        | ts.TemplateHead | ts.TemplateMiddle | ts.TemplateTail | ts.JsxText, value: any): J.Literal {

        let valueSource = node.getText();
        if (!isValidSurrogateRange(valueSource)) {
            // TODO: Fix to prevent ingestion failure for invalid surrogate pairs. Should be reworked with J.Literal.UnicodeEscape
            throw new InvalidSurrogatesNotSupportedError();
        }

        return {
            kind: J.Kind.Literal,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            value: value,
            valueSource: valueSource,
            type: this.mapPrimitiveType(node)
        };
    }

    visitBigIntLiteral(node: ts.BigIntLiteral): J.Literal {
        return this.mapLiteral(node, node.text); // FIXME value not in AST
    }

    visitStringLiteral(node: ts.StringLiteral): J.Literal {
        return this.mapLiteral(node, node.text); // FIXME value not in AST
    }

    visitRegularExpressionLiteral(node: ts.RegularExpressionLiteral): J.Literal {
        return this.mapLiteral(node, node.text); // FIXME value not in AST
    }

    visitNoSubstitutionTemplateLiteral(node: ts.NoSubstitutionTemplateLiteral): J.Literal {
        return this.mapLiteral(node, node.text); // FIXME value not in AST
    }

    visitTemplateHead(node: ts.TemplateHead): J.Literal {
        return this.mapLiteral(node, node.text);
    }

    visitTemplateMiddle(node: ts.TemplateMiddle): J.Literal {
        return this.mapLiteral(node, node.text);
    }

    visitTemplateTail(node: ts.TemplateTail): J.Literal {
        return this.mapLiteral(node, node.text);
    }

    visitIdentifier(node: ts.Identifier): J.Identifier {
        return this.mapIdentifier(node, node.text);
    }

    private mapIdentifier(node: ts.Node, name: string, withType: boolean = true): J.Identifier {
        let type = withType ? this.mapType(node) : undefined;
        return {
            kind: J.Kind.Identifier,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            annotations: [], // FIXME decorators
            simpleName: name,
            type: type?.kind === JavaType.Kind.Variable ? (type as JavaType.Variable).type : type,
            fieldType: type?.kind === JavaType.Kind.Variable ? type as JavaType.Variable : undefined
        };
    }

    visitThisKeyword(node: ts.ThisExpression): J.Identifier {
        return this.mapIdentifier(node, 'this');
    }

    visitPrivateIdentifier(node: ts.PrivateIdentifier): J.Identifier {
        return this.mapIdentifier(node, node.text);
    }

    visitQualifiedName(node: ts.QualifiedName): J.FieldAccess {
        return {
            kind: J.Kind.FieldAccess,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            target: this.visit(node.left),
            name: this.leftPadded(this.suffix(node.left), this.convert(node.right)),
            type: this.mapType(node)
        };
    }

    visitComputedPropertyName(node: ts.ComputedPropertyName): JS.ComputedPropertyName {
        return {
            kind: JS.Kind.ComputedPropertyName,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            expression: this.rightPadded(this.convert(node.expression), this.suffix(node.expression))
        };
    }

    visitTypeParameter(node: ts.TypeParameterDeclaration): J.TypeParameter {
        return {
            kind: J.Kind.TypeParameter,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            annotations: [],
            modifiers: this.mapModifiers(node),
            name: this.visit(node.name),
            bounds: (node.constraint || node.default) && {
                kind: J.Kind.Container,
                before: this.prefix(this.findChildNode(node, ts.SyntaxKind.ExtendsKeyword) ?? this.findChildNode(node, ts.SyntaxKind.EqualsToken)!),
                elements: [
                    node.constraint ? this.rightPadded(this.visit(node.constraint), this.suffix(node.constraint)) : this.rightPadded(this.newEmpty(), emptySpace),
                    node.default ? this.rightPadded(this.visit(node.default), this.suffix(node.default)) : this.rightPadded(this.newEmpty(), emptySpace)
                ],
                markers: emptyMarkers
            }
        };
    }

    visitParameter(node: ts.ParameterDeclaration): J.VariableDeclarations {
        return {
            kind: J.Kind.VariableDeclarations,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: this.mapDecorators(node),
            modifiers: this.mapModifiers(node),
            typeExpression: this.mapTypeInfo(node),
            variables: [this.rightPadded(
                {
                    kind: J.Kind.NamedVariable,
                    id: randomId(),
                    prefix: node.dotDotDotToken ? this.prefix(node.dotDotDotToken) : this.prefix(node.name),
                    markers: emptyMarkers,
                    name: produce(this.convert<VariableDeclarator>(node.name), draft => {
                        draft.markers = this.maybeAddOptionalMarker(draft, node);
                        if (node.dotDotDotToken) {
                            draft.markers.markers.push({
                                kind: JS.Markers.Spread,
                                id: randomId(),
                                prefix: this.prefix(node.name)
                            } as Spread);
                        }
                    }),
                    dimensionsAfterName: [],
                    initializer: node.initializer && this.leftPadded(this.prefix(node.getChildAt(node.getChildren().indexOf(node.initializer) - 1)), this.visit(node.initializer)),
                    variableType: this.mapVariableType(node)
                },
                this.suffix(node.name)
            )]
        };
    }

    visitDecorator(node: ts.Decorator): J.Annotation | J.Unknown {
        let annotationType: NameTree | TypeTree;
        let _arguments: J.Container<Expression> | undefined = undefined;

        if (ts.isCallExpression(node.expression)) {
            annotationType = {
                kind: JS.Kind.ExpressionWithTypeArguments,
                id: randomId(),
                prefix: emptySpace,
                markers: emptyMarkers,
                clazz: this.convert<J>(node.expression.expression) as Expression,
                typeArguments: node.expression.typeArguments && this.mapTypeArguments(this.suffix(node.expression.expression), node.expression.typeArguments)
            } as JS.ExpressionWithTypeArguments;
            _arguments = this.mapCommaSeparatedList(node.expression.getChildren(this.sourceFile).slice(-3))
        } else if (ts.isIdentifier(node.expression)) {
            annotationType = this.convert(node.expression);
        } else if (ts.isPropertyAccessExpression(node.expression)) {
            annotationType = this.convert(node.expression);
        } else if (ts.isParenthesizedExpression(node.expression)) {
            annotationType = {
                kind: JS.Kind.TypeTreeExpression,
                id: randomId(),
                prefix: this.prefix(node.expression),
                markers: emptyMarkers,
                expression: this.convert(node.expression) as Expression
            } as JS.TypeTreeExpression;
        } else {
            return this.visitUnknown(node);
        }

        return {
            kind: J.Kind.Annotation,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            annotationType: annotationType,
            arguments: _arguments
        };
    }

    visitPropertySignature(node: ts.PropertySignature) {
        // FIXME We are mapping the literals in things like type ascii = { " ": 32; "!": 33; } to
        //  named variables, which is not a good use of this construct.
        return {
            kind: J.Kind.VariableDeclarations,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: [], // no decorators allowed
            modifiers: this.mapModifiers(node),
            typeExpression: this.mapTypeInfo(node),
            variables: [this.rightPadded(
                {
                    kind: J.Kind.NamedVariable,
                    id: randomId(),
                    prefix: this.prefix(node.name),
                    markers: emptyMarkers,
                    name: produce(this.convert<VariableDeclarator>(node.name), draft => {
                        draft.markers = this.maybeAddOptionalMarker(draft, node);
                    }) as VariableDeclarator,
                    dimensionsAfterName: [],
                    variableType: this.mapVariableType(node)
                },
                emptySpace
            )]
        };
    }

    visitPropertyDeclaration(node: ts.PropertyDeclaration): J.VariableDeclarations {
        return {
            kind: J.Kind.VariableDeclarations,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: this.mapDecorators(node),
            modifiers: this.mapModifiers(node),
            typeExpression: this.mapTypeInfo(node),
            variables: [this.rightPadded(
                {
                    kind: J.Kind.NamedVariable,
                    id: randomId(),
                    prefix: this.prefix(node.name),
                    markers: emptyMarkers,
                    name: produce(this.convert<VariableDeclarator>(node.name), draft => {
                        draft.markers = this.maybeAddOptionalMarker(draft, node);

                        // This will be mutually exclusive with the optional token
                        if (node.exclamationToken) {
                            draft.markers.markers.push({
                                kind: JS.Markers.NonNullAssertion,
                                id: randomId(),
                                prefix: this.suffix(node.name)
                            } as NonNullAssertion);
                        }
                    }),
                    dimensionsAfterName: [],
                    initializer: node.initializer && this.leftPadded(this.prefix(node.getChildAt(node.getChildren().indexOf(node.initializer) - 1)), this.visit(node.initializer)),
                    variableType: this.mapVariableType(node)
                } as J.VariableDeclarations.NamedVariable,
                emptySpace
            )]
        };
    }

    visitMethodSignature(node: ts.MethodSignature): J.MethodDeclaration | JS.ComputedPropertyMethodDeclaration {
        const prefix = this.prefix(node);

        if (ts.isComputedPropertyName(node.name)) {
            return {
                kind: JS.Kind.ComputedPropertyMethodDeclaration,
                id: randomId(),
                prefix: prefix,
                markers: emptyMarkers,
                leadingAnnotations: [], // no decorators allowed
                modifiers: [], // no modifiers allowed
                typeParameters: this.mapTypeParametersAsObject(node),
                returnTypeExpression: this.mapTypeInfo(node),
                name: produce(this.convert<ComputedPropertyName>(node.name), draft => {
                    draft.markers = this.maybeAddOptionalMarker(draft, node);
                }),
                parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
                methodType: this.mapMethodType(node)
            } as JS.ComputedPropertyMethodDeclaration;
        }

        let name: J.Identifier = !node.name
            ? this.mapIdentifier(node, "")
            : ts.isStringLiteral(node.name)
                ? this.mapIdentifier(node.name, node.name.getText())
                : this.visit(node.name);

        return {
            kind: J.Kind.MethodDeclaration,
            id: randomId(),
            prefix: prefix,
            markers: emptyMarkers,
            leadingAnnotations: [], // no decorators allowed
            modifiers: [], // no modifiers allowed
            typeParameters: this.mapTypeParametersAsObject(node),
            returnTypeExpression: this.mapTypeInfo(node),
            nameAnnotations: [],
            name: produce(name, draft => {
                draft.markers = this.maybeAddOptionalMarker(draft, node);
            }),
            parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
            methodType: this.mapMethodType(node)
        } as J.MethodDeclaration;
    }

    visitMethodDeclaration(node: ts.MethodDeclaration): J.MethodDeclaration | JS.ComputedPropertyMethodDeclaration {
        const prefix = this.prefix(node);
        const markers = produce(emptyMarkers, draft => {
            if (node.asteriskToken) {
                draft.markers.push({
                    kind: JS.Markers.Asterisk,
                    id: randomId(),
                    prefix: this.prefix(node.asteriskToken)
                } as Asterisk);
            }
        });

        let name: Expression = node.name ? this.visit(node.name) : this.mapIdentifier(node, "");
        name = produce(name, draft => {
            draft.markers = this.maybeAddOptionalMarker(draft, node);
        });

        if (ts.isComputedPropertyName(node.name)) {
            return {
                kind: JS.Kind.ComputedPropertyMethodDeclaration,
                id: randomId(),
                prefix: prefix,
                markers: markers,
                leadingAnnotations: this.mapDecorators(node),
                modifiers: this.mapModifiers(node),
                typeParameters: this.mapTypeParametersAsObject(node),
                returnTypeExpression: this.mapTypeInfo(node),
                nameAnnotations: [],
                name: name as ComputedPropertyName,
                parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
                body: node.body && this.convert<J.Block>(node.body),
                methodType: this.mapMethodType(node)
            } as JS.ComputedPropertyMethodDeclaration;
        }

        return {
            kind: J.Kind.MethodDeclaration,
            id: randomId(),
            prefix: prefix,
            markers: markers,
            leadingAnnotations: this.mapDecorators(node),
            modifiers: this.mapModifiers(node),
            typeParameters: this.mapTypeParametersAsObject(node),
            returnTypeExpression: this.mapTypeInfo(node),
            nameAnnotations: [],
            name: name as J.Identifier,
            parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
            body: node.body && this.convert<J.Block>(node.body),
            methodType: this.mapMethodType(node)
        } as J.MethodDeclaration;
    }

    private mapTypeInfo(node: ts.MethodDeclaration | ts.PropertyDeclaration | ts.VariableDeclaration | ts.ParameterDeclaration
        | ts.PropertySignature | ts.MethodSignature | ts.ArrowFunction | ts.CallSignatureDeclaration | ts.GetAccessorDeclaration
        | ts.FunctionDeclaration | ts.ConstructSignatureDeclaration | ts.FunctionExpression | ts.NamedTupleMember): JS.TypeInfo | undefined {
        return node.type && {
            kind: JS.Kind.TypeInfo,
            id: randomId(),
            prefix: this.prefix(node.getChildAt(node.getChildren().indexOf(node.type) - 1)),
            markers: emptyMarkers,
            typeIdentifier: this.visit(node.type)
        };
    }

    visitClassStaticBlockDeclaration(node: ts.ClassStaticBlockDeclaration): J.Block {
        return {
            kind: J.Kind.Block,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            static: this.rightPadded(true, this.prefix(this.findChildNode(node.body, ts.SyntaxKind.OpenBraceToken)!), emptyMarkers),
            statements: node.body.statements.map(ce => this.rightPadded(
                this.convert(ce),
                ce.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken ? this.prefix(ce.getLastToken()!) : emptySpace,
                ce.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken ? markers({
                    kind: J.Markers.Semicolon,
                    id: randomId()
                }) : emptyMarkers
            )),
            end: this.prefix(node.getLastToken()!)
        };
    }

    visitConstructor(node: ts.ConstructorDeclaration): J.MethodDeclaration {
        // using string literal for the following case: class A { "constructor"() {} }
        const constructorKeyword = node.getChildren()
            .find(n => (n.kind === ts.SyntaxKind.ConstructorKeyword) || ((n.kind === ts.SyntaxKind.StringLiteral) && (n.getText().includes("constructor"))))!;
        return {
            kind: J.Kind.MethodDeclaration,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: this.mapDecorators(node),
            modifiers: this.mapModifiers(node),
            nameAnnotations: [],
            name: this.mapIdentifier(constructorKeyword, constructorKeyword.getText()),
            parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
            body: node.body && this.convert<J.Block>(node.body),
            methodType: this.mapMethodType(node)
        };
    }

    visitGetAccessor(node: ts.GetAccessorDeclaration): J.MethodDeclaration | JS.ComputedPropertyMethodDeclaration {
        const name = this.visit(node.name);
        if (ts.isComputedPropertyName(node.name)) {
            return {
                kind: JS.Kind.ComputedPropertyMethodDeclaration,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                leadingAnnotations: this.mapDecorators(node),
                modifiers: this.mapModifiers(node),
                returnTypeExpression: this.mapTypeInfo(node),
                name: name as ComputedPropertyName,
                parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
                body: node.body && this.convert<J.Block>(node.body),
                methodType: this.mapMethodType(node)
            } as JS.ComputedPropertyMethodDeclaration;
        }

        return {
            kind: J.Kind.MethodDeclaration,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: this.mapDecorators(node),
            modifiers: this.mapModifiers(node),
            returnTypeExpression: this.mapTypeInfo(node),
            nameAnnotations: [],
            name: name,
            parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
            body: node.body && this.convert<J.Block>(node.body),
            methodType: this.mapMethodType(node)
        } as J.MethodDeclaration;
    }

    visitSetAccessor(node: ts.SetAccessorDeclaration): J.MethodDeclaration | JS.ComputedPropertyMethodDeclaration {
        const name = this.visit(node.name);
        if (ts.isComputedPropertyName(node.name)) {
            return {
                kind: JS.Kind.ComputedPropertyMethodDeclaration,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                leadingAnnotations: this.mapDecorators(node),
                modifiers: this.mapModifiers(node),
                name: name as ComputedPropertyName,
                parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
                body: node.body && this.convert<J.Block>(node.body),
                methodType: this.mapMethodType(node)
            } as JS.ComputedPropertyMethodDeclaration;
        }

        return {
            kind: J.Kind.MethodDeclaration,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: this.mapDecorators(node),
            modifiers: this.mapModifiers(node),
            nameAnnotations: [],
            name: name,
            parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
            body: node.body && this.convert<J.Block>(node.body),
            methodType: this.mapMethodType(node)
        } as J.MethodDeclaration;
    }

    visitCallSignature(node: ts.CallSignatureDeclaration): J.MethodDeclaration {
        return {
            kind: J.Kind.MethodDeclaration,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: [],
            modifiers: [],
            typeParameters: this.mapTypeParametersAsObject(node),
            returnTypeExpression: this.mapTypeInfo(node),
            nameAnnotations: [],
            name: {
                kind: J.Kind.Identifier,
                id: randomId(),
                prefix: emptySpace/* this.prefix(node.getChildren().find(n => n.kind === ts.SyntaxKind.OpenBraceToken)!) */,
                markers: emptyMarkers,
                annotations: [], // FIXME decorators
                simpleName: "",
            },
            parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
            methodType: this.mapMethodType(node)
        };
    }

    visitConstructSignature(node: ts.ConstructSignatureDeclaration): J.MethodDeclaration {
        return {
            kind: J.Kind.MethodDeclaration,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: [], // no decorators allowed
            modifiers: [], // no modifiers allowed
            typeParameters: this.mapTypeParametersAsObject(node),
            returnTypeExpression: this.mapTypeInfo(node),
            nameAnnotations: [],
            name: {
                kind: J.Kind.Identifier,
                id: randomId(),
                prefix: emptySpace,
                markers: emptyMarkers,
                annotations: [],
                simpleName: 'new'
            },
            parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
            methodType: this.mapMethodType(node)
        };
    }

    visitIndexSignature(node: ts.IndexSignatureDeclaration): JS.IndexSignatureDeclaration {
        return {
            kind: JS.Kind.IndexSignatureDeclaration,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            modifiers: this.mapModifiers(node),
            parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node, ts.SyntaxKind.OpenBracketToken)),
            typeExpression: this.leftPadded(this.prefix(node.getChildAt(node.getChildren().indexOf(node.type) - 1)), this.convert(node.type)),
            type: this.mapType(node)
        };
    }

    visitTypePredicate(node: ts.TypePredicateNode): JS.TypePredicate {
        return {
            kind: JS.Kind.TypePredicate,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            asserts: node.assertsModifier ? this.leftPadded(this.prefix(node.assertsModifier), true) : this.leftPadded(emptySpace, false),
            parameterName: this.visit(node.parameterName),
            expression: node.type && this.leftPadded(this.suffix(node.parameterName), this.convert(node.type)),
            type: this.mapType(node)
        };
    }

    visitTypeReference(node: ts.TypeReferenceNode): J.ParameterizedType {
        if (node.typeArguments) {
            return {
                kind: J.Kind.ParameterizedType,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                class: this.visit(node.typeName),
                typeParameters: this.mapTypeArguments(this.suffix(node.typeName), node.typeArguments),
                type: this.mapType(node)
            }
        }
        return this.visit(node.typeName);
    }

    visitFunctionType(node: ts.FunctionTypeNode): JS.FunctionType {
        return {
            kind: JS.Kind.FunctionType,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            modifiers: [],
            constructorType: this.leftPadded(emptySpace, false),
            typeParameters: this.mapTypeParametersAsObject(node),
            parameters: {
                kind: J.Kind.Container,
                before: this.prefix(node.getChildAt(node.getChildren().findIndex(n => n.pos === node.parameters.pos) - 1)),
                elements: node.parameters.length == 0 ?
                    [this.rightPadded(this.newEmpty(), this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseParenToken)!))]
                    : node.parameters.map(p => this.rightPadded(this.visit(p), this.suffix(p)))
                        .concat(node.parameters.hasTrailingComma ? this.rightPadded(this.newEmpty(), this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseParenToken)!)) : []),
                markers: emptyMarkers
            },
            returnType: this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.EqualsGreaterThanToken)!), this.convert(node.type))
        };
    }

    visitConstructorType(node: ts.ConstructorTypeNode): JS.FunctionType {
        return {
            kind: JS.Kind.FunctionType,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            modifiers: this.mapModifiers(node),
            constructorType: this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.NewKeyword)!), true),
            typeParameters: this.mapTypeParametersAsObject(node),
            parameters: {
                kind: J.Kind.Container,
                before: this.prefix(node.getChildAt(node.getChildren().findIndex(n => n.pos === node.parameters.pos) - 1)),
                elements: node.parameters.length == 0 ?
                    [this.rightPadded(this.newEmpty(), this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseParenToken)!))]
                    : node.parameters.map(p => this.rightPadded(this.visit(p), this.suffix(p)))
                        .concat(node.parameters.hasTrailingComma ? this.rightPadded(this.newEmpty(), this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseParenToken)!)) : []),
                markers: emptyMarkers
            },
            returnType: this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.EqualsGreaterThanToken)!), this.convert(node.type))
        };
    }

    visitTypeQuery(node: ts.TypeQueryNode): JS.TypeQuery {
        return {
            kind: JS.Kind.TypeQuery,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            typeExpression: this.convert(node.exprName),
            typeArguments: node.typeArguments && this.mapTypeArguments(this.suffix(node.exprName), node.typeArguments),
            type: this.mapType(node)
        }
    }

    visitTypeLiteral(node: ts.TypeLiteralNode): JS.TypeLiteral {
        return {
            kind: JS.Kind.TypeLiteral,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            members: {
                kind: J.Kind.Block,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBraceToken)!),
                markers: emptyMarkers,
                static: this.rightPadded(false, emptySpace),
                statements: node.members.map(te => ({
                    kind: J.Kind.RightPadded,
                    element: this.convert(te),
                    after: (te.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken) || (te.getLastToken()?.kind === ts.SyntaxKind.CommaToken) ? this.prefix(te.getLastToken()!) : emptySpace,
                    markers: (te.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken) || (te.getLastToken()?.kind === ts.SyntaxKind.CommaToken) ? markers(this.convertToken(te.getLastToken())!) : emptyMarkers
                })),
                end: this.prefix(node.getLastToken()!)
            },
            type: this.mapType(node)
        };
    }

    visitArrayType(node: ts.ArrayTypeNode): J.ArrayType {
        return {
            kind: J.Kind.ArrayType,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            elementType: this.convert(node.elementType),
            dimension: this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBracketToken)!), this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseBracketToken)!)),
            type: this.mapType(node)!
        }
    }

    visitTupleType(node: ts.TupleTypeNode): JS.Tuple {
        return {
            kind: JS.Kind.Tuple,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            elements: {
                kind: J.Kind.Container,
                before: emptySpace,
                elements: node.elements.length > 0 ?
                    node.elements.map(p => this.rightPadded(this.convert(p), this.suffix(p)))
                        .concat(node.elements.hasTrailingComma ? this.rightPadded(this.newEmpty(), this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseBracketToken)!)) : [])
                    : [this.rightPadded(this.newEmpty(this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseBracketToken)!)), emptySpace)],
                markers: emptyMarkers
            },
            type: this.mapType(node)
        };
    }

    /**
     * This can only exist in the context of a TupleTypeNode, and so printing
     * of the Optional marker will be handled specially in the printer for {@link JS.Tuple}.
     * @param node The optional type node.
     */
    visitOptionalType(node: ts.OptionalTypeNode): JS {
        const type = this.visit(node.type) as JS;
        return produce(type, draft => {
            draft.markers.markers.push({
                kind: JS.Markers.Optional,
                id: randomId(),
                prefix: this.suffix(node.type)
            } as Optional);
        });
    }

    visitRestType(node: ts.RestTypeNode): Expression {
        return produce(this.convert<Expression>(node.type), draft => {
            draft.markers.markers.push({
                kind: JS.Markers.Spread,
                id: randomId(),
                prefix: this.prefix(node)
            } as Spread);
        });
    }

    visitUnionType(node: ts.UnionTypeNode): JS.Union {
        const initialBar = getPreviousSibling(node.types[0]);
        return {
            kind: JS.Kind.Union,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            types: [
                ...(initialBar?.kind === ts.SyntaxKind.BarToken ? [this.rightPadded<Expression>(this.newEmpty(), this.prefix(initialBar))] : []),
                ...this.rightPaddedList<ts.Node, Expression>([...node.types], (n) => this.keywordPrefix(ts.SyntaxKind.BarToken, getNextSibling)(n))
            ],
            type: this.mapType(node)
        };
    }

    visitIntersectionType(node: ts.IntersectionTypeNode): JS.Intersection {
        const initialAmpersand = getPreviousSibling(node.types[0]);
        return {
            kind: JS.Kind.Intersection,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            types: [
                ...(initialAmpersand?.kind === ts.SyntaxKind.AmpersandToken ? [this.rightPadded<Expression>(this.newEmpty(), this.prefix(initialAmpersand))] : []),
                ...this.rightPaddedList<ts.Node, Expression>([...node.types], (n) => this.keywordPrefix(ts.SyntaxKind.AmpersandToken, getNextSibling)(n))
            ],
            type: this.mapType(node)
        };
    }

    visitConditionalType(node: ts.ConditionalTypeNode): JS.ConditionalType {
        return {
            kind: JS.Kind.ConditionalType,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            checkType: this.visit(node.checkType),
            condition: {
                kind: J.Kind.LeftPadded,
                before: this.prefix(this.findChildNode(node, ts.SyntaxKind.ExtendsKeyword)!),
                element: {
                    kind: J.Kind.Ternary,
                    id: randomId(),
                    prefix: emptySpace,
                    markers: emptyMarkers,
                    condition: this.convert(node.extendsType),
                    truePart: this.leftPadded(this.suffix(node.extendsType), this.convert(node.trueType)),
                    falsePart: this.leftPadded(this.suffix(node.trueType), this.convert(node.falseType)),
                    type: this.mapType(node)
                },
                markers: emptyMarkers
            },
            type: this.mapType(node)
        };
    }

    visitInferType(node: ts.InferTypeNode): JS.InferType {
        return {
            kind: JS.Kind.InferType,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            typeParameter: this.leftPadded(emptySpace, this.convert(node.typeParameter)),
            type: this.mapType(node)
        };
    }

    visitParenthesizedType(node: ts.ParenthesizedTypeNode): J.ParenthesizedTypeTree {
        return {
            kind: J.Kind.ParenthesizedTypeTree,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            annotations: [],
            parenthesizedType: {
                kind: J.Kind.Parentheses,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                tree: this.rightPadded(this.convert(node.type), this.prefix(node.getLastToken()!))
            }
        };
    }

    visitThisType(node: ts.ThisTypeNode): J.Identifier {
        return this.mapIdentifier(node, 'this');
    }

    visitTypeOperator(node: ts.TypeOperatorNode): JS.TypeOperator {
        function mapTypeOperator(operator: ts.SyntaxKind.KeyOfKeyword | ts.SyntaxKind.UniqueKeyword | ts.SyntaxKind.ReadonlyKeyword): JS.TypeOperator.Type | undefined {
            switch (operator) {
                case ts.SyntaxKind.KeyOfKeyword:
                    return JS.TypeOperator.Type.KeyOf;
                case ts.SyntaxKind.ReadonlyKeyword:
                    return JS.TypeOperator.Type.ReadOnly;
                case ts.SyntaxKind.UniqueKeyword:
                    return JS.TypeOperator.Type.Unique;
            }
        }

        return {
            kind: JS.Kind.TypeOperator,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            operator: mapTypeOperator(node.operator)!,
            expression: this.leftPadded(this.prefix(node.type), this.visit(node.type))
        };
    }

    visitIndexedAccessType(node: ts.IndexedAccessTypeNode): JS.IndexedAccessType {
        return {
            kind: JS.Kind.IndexedAccessType,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            objectType: this.convert(node.objectType),
            indexType: {
                kind: JS.Kind.IndexedAccessTypeIndexType,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBracketToken)!),
                markers: emptyMarkers,
                element: this.rightPadded(this.convert(node.indexType), this.suffix(node.indexType)),
                type: this.mapType(node.indexType)
            } as JS.IndexedAccessType.IndexType,
            type: this.mapType(node)
        };
    }

    visitMappedType(node: ts.MappedTypeNode): JS.MappedType {
        function hasPrefixToken(readonlyToken?: ts.ReadonlyKeyword | ts.PlusToken | ts.MinusToken): boolean {
            return !!(readonlyToken && (readonlyToken.kind === ts.SyntaxKind.PlusToken || readonlyToken.kind === ts.SyntaxKind.MinusToken));
        }

        function hasSuffixToken(questionToken?: ts.QuestionToken | ts.PlusToken | ts.MinusToken): boolean {
            return !!(questionToken && (questionToken.kind === ts.SyntaxKind.PlusToken || questionToken.kind === ts.SyntaxKind.MinusToken));
        }

        return {
            kind: JS.Kind.MappedType,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            prefixToken: hasPrefixToken(node.readonlyToken) ? this.leftPadded(
                this.prefix(node.readonlyToken!),
                {
                    kind: J.Kind.Literal,
                    id: randomId(),
                    prefix: this.prefix(node.readonlyToken!),
                    markers: emptyMarkers,
                    value: undefined, // FIXME verify
                    valueSource: node.readonlyToken!.getText(),
                    type: this.mapPrimitiveType(node.readonlyToken!)
                } as J.Literal
            ) : undefined,
            hasReadonly: node.readonlyToken ? this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.ReadonlyKeyword)!), true) : this.leftPadded(emptySpace, false),
            keysRemapping: {
                kind: JS.Kind.MappedTypeKeysRemapping,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBracketToken)!),
                markers: emptyMarkers,
                typeParameter: this.rightPadded(
                    {
                        kind: Kind.MappedTypeParameter,
                        id: randomId(),
                        prefix: this.prefix(node.typeParameter),
                        markers: emptyMarkers,
                        name: this.visit(node.typeParameter.name),
                        iterateType: this.leftPadded(this.suffix(node.typeParameter.name), this.visit(node.typeParameter.constraint!)),
                    },
                    this.suffix(node.typeParameter)),
                nameType: node.nameType && this.rightPadded(this.visit(node.nameType), this.suffix(node.nameType))
            },
            suffixToken: hasSuffixToken(node.questionToken) ? this.leftPadded(
                this.prefix(node.questionToken!),
                {
                    kind: J.Kind.Literal,
                    id: randomId(),
                    prefix: this.prefix(node.questionToken!),
                    markers: emptyMarkers,
                    value: undefined, // FIXME verify
                    valueSource: node.questionToken!.getText(),
                    type: this.mapPrimitiveType(node.questionToken!)
                } as J.Literal
            ) : undefined,
            hasQuestionToken: node.questionToken ? this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.QuestionToken)!), true) : this.leftPadded(emptySpace, false),
            valueType: node.type ? {
                kind: J.Kind.Container,
                before: this.prefix(this.findChildNode(node, ts.SyntaxKind.ColonToken)!),
                elements: [this.rightPadded(this.visit(node.type), this.suffix(node.type)),
                    this.findChildNode(node, ts.SyntaxKind.SemicolonToken) ?
                        this.rightPadded(this.newEmpty(emptySpace, markers({
                            kind: J.Markers.Semicolon,
                            id: randomId()
                        })), this.prefix(node.getLastToken()!))
                        : this.rightPadded(this.newEmpty(), this.prefix(node.getLastToken()!))
                ],
                markers: emptyMarkers
            } : {
                kind: J.Kind.Container,
                before: emptySpace,
                elements: [this.findChildNode(node, ts.SyntaxKind.SemicolonToken) ?
                    this.rightPadded(this.newEmpty(this.prefix(this.findChildNode(node, ts.SyntaxKind.SemicolonToken)!), markers({
                        kind: J.Markers.Semicolon,
                        id: randomId()
                    })), this.prefix(node.getLastToken()!))
                    : this.rightPadded(this.newEmpty(), this.prefix(node.getLastToken()!))
                ],
                markers: emptyMarkers
            },
            type: this.mapType(node)
        };
    }

    visitLiteralType(node: ts.LiteralTypeNode): JS.LiteralType {
        return {
            kind: JS.Kind.LiteralType,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            literal: this.visit(node.literal),
            type: this.mapType(node)!,
        };
    }

    // FIXME these should not be mapped as VariableDeclarations since the names can never be accessed
    //  and this would potentially just trip up flow analyses. The names exist purely for documentation purposes,
    //  they has no semantics. See https://stackoverflow.com/questions/63629315/what-are-named-or-labeled-tuples-in-typescript.
    visitNamedTupleMember(node: ts.NamedTupleMember) {
        return {
            kind: J.Kind.VariableDeclarations,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: [],
            modifiers: [],
            typeExpression: this.mapTypeInfo(node),
            variables: [this.rightPadded(
                {
                    kind: J.Kind.NamedVariable,
                    id: randomId(),
                    prefix: emptySpace,
                    markers: emptyMarkers,
                    name: produce(this.convert<J.Identifier>(node.name), draft => {
                        draft.markers = this.maybeAddOptionalMarker(draft, node);
                        if (node.dotDotDotToken) {
                            draft.markers.markers.push({
                                kind: JS.Markers.Spread,
                                id: randomId(),
                                prefix: this.prefix(node.dotDotDotToken)
                            } as Spread);
                        }
                    }),
                    dimensionsAfterName: [],
                    variableType: this.mapVariableType(node),
                } as J.VariableDeclarations.NamedVariable,
                this.suffix(node.name)
            )],
        };
    }

    visitTemplateLiteralType(node: ts.TemplateLiteralTypeNode): JS.TemplateExpression {
        return {
            kind: JS.Kind.TemplateExpression,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            head: this.visit(node.head),
            spans: node.templateSpans.map(s => this.rightPadded(this.visit(s), this.suffix(s))),
            type: this.mapType(node)
        }
    }

    visitTemplateLiteralTypeSpan(node: ts.TemplateLiteralTypeSpan): JS.TemplateExpression.Span {
        return {
            kind: JS.Kind.TemplateExpressionSpan,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            expression: this.convert(node.type),
            tail: this.visit(node.literal)
        }
    }

    visitImportType(node: ts.ImportTypeNode): JS.ImportType {
        let importTypeAttributes = undefined;
        if (node.attributes) {
            const openBraceIndex = node.attributes.getChildren().findIndex(n => n.kind === ts.SyntaxKind.OpenBraceToken);
            const attributes = this.mapCommaSeparatedList<JS.ImportAttribute>(node.attributes.getChildren(this.sourceFile).slice(openBraceIndex, openBraceIndex + 3));
            importTypeAttributes = {
                kind: JS.Kind.ImportTypeAttributes,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBraceToken)!),
                markers: emptyMarkers,
                token: this.rightPadded(
                    this.mapIdentifier(this.findChildNode(node, node.attributes.token)!,
                        ts.SyntaxKind.WithKeyword === node.attributes.token ? "with" : "assert"),
                    this.prefix(this.findChildNode(node, ts.SyntaxKind.ColonToken)!)),
                elements: attributes,
                end: this.suffix(node.attributes),
            }
        }

        return {
            kind: JS.Kind.ImportType,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            hasTypeof: node.isTypeOf ? this.rightPadded(true, this.suffix(this.findChildNode(node, ts.SyntaxKind.TypeOfKeyword)!)) : this.rightPadded(false, emptySpace),
            argumentAndAttributes: {
                kind: J.Kind.Container,
                before: this.suffix(this.findChildNode(node, ts.SyntaxKind.ImportKeyword)!),
                elements: [this.rightPadded(this.visit(node.argument), this.suffix(node.argument))].concat(importTypeAttributes ? [this.rightPadded(importTypeAttributes, this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseParenToken)!))] : []),
                markers: emptyMarkers
            },
            qualifier: node.qualifier && this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.DotToken)!), this.visit(node.qualifier)),
            typeArguments: node.typeArguments && this.mapTypeArguments(this.prefix(this.findChildNode(node, ts.SyntaxKind.LessThanToken)!), node.typeArguments),
            type: this.mapType(node)
        };
    }

    visitObjectBindingPattern(node: ts.ObjectBindingPattern): JS.ObjectBindingDeclarations {
        return {
            kind: JS.Kind.ObjectBindingDeclarations,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: [],
            modifiers: [],
            bindings: this.mapCommaSeparatedList(node.getChildren(this.sourceFile)),
            initializer: undefined
        };
    }

    visitArrayBindingPattern(node: ts.ArrayBindingPattern): JS.ArrayBindingPattern {
        return {
            kind: JS.Kind.ArrayBindingPattern,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            elements: this.mapCommaSeparatedList(node.getChildren(this.sourceFile)),
            type: this.mapType(node)
        };
    }

    visitBindingElement(node: ts.BindingElement): JS.BindingElement {
        return {
            kind: JS.Kind.BindingElement,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            propertyName: node.propertyName && this.rightPadded(this.convert<J.Identifier>(node.propertyName), this.suffix(node.propertyName)),
            name: produce(this.convert<Expression>(node.name), draft => {
                if (node.dotDotDotToken) {
                    draft.markers.markers.push({
                        kind: JS.Markers.Spread,
                        id: randomId(),
                        prefix: this.prefix(node.dotDotDotToken)
                    } as Spread);
                }
            }),
            initializer: node.initializer && this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.EqualsToken)!), this.convert<Expression>(node.initializer)),
            variableType: this.mapVariableType(node)
        };
    }

    visitArrayLiteralExpression(node: ts.ArrayLiteralExpression): J.NewArray {
        return {
            kind: J.Kind.NewArray,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            dimensions: [],
            initializer: this.mapCommaSeparatedList(node.getChildren(this.sourceFile)),
            type: this.mapType(node)
        };
    }

    visitObjectLiteralExpression(node: ts.ObjectLiteralExpression): J.NewClass {
        return {
            kind: J.Kind.NewClass,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            new: emptySpace,
            arguments: emptyContainer(),
            body: this.convertPropertyAssignments(node.getChildren(this.sourceFile).slice(-3)),
            constructorType: this.mapMethodType(node)
        };
    }

    private convertPropertyAssignments(nodes: ts.Node[]): J.Block {
        const prefix = this.prefix(nodes[0]);
        let statementList = nodes[1] as ts.SyntaxList;

        const statements: J.RightPadded<Statement>[] = this.rightPaddedSeparatedList(
            [...statementList.getChildren(this.sourceFile)],
            (nodes, i) => i == nodes.length - 2 && nodes[i + 1].kind === ts.SyntaxKind.CommaToken ? markers({
                kind: J.Markers.TrailingComma,
                id: randomId(),
                suffix: this.prefix(nodes[i + 1])
            } as TrailingComma) : emptyMarkers
        );

        return {
            kind: J.Kind.Block,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            static: this.rightPadded(false, emptySpace),
            statements,
            end: this.prefix(nodes[nodes.length - 1])
        };
    }

    visitPropertyAccessExpression(node: ts.PropertyAccessExpression): J.FieldAccess {
        return {
            kind: J.Kind.FieldAccess,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            target: produce(this.convert<Expression>(node.expression), draft => {
                if (node.questionDotToken) {
                    draft.markers.markers.push({
                        kind: JS.Markers.Optional,
                        id: randomId(),
                        prefix: this.suffix(node.expression)
                    } as Optional);
                }
            }),
            name: this.leftPadded(this.prefix(node.getChildAt(1, this.sourceFile)), this.convert(node.name)),
            type: this.mapType(node)
        };
    }

    visitElementAccessExpression(node: ts.ElementAccessExpression): J.ArrayAccess {
        return {
            kind: J.Kind.ArrayAccess,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            indexed: produce(this.convert<Expression>(node.expression), draft => {
                if (node.questionDotToken) {
                    draft.markers.markers.push({
                        kind: JS.Markers.Optional,
                        id: randomId(),
                        prefix: this.suffix(node.expression)
                    } as Optional);
                }
            }),
            dimension: {
                kind: J.Kind.ArrayDimension,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBracketToken)!),
                markers: emptyMarkers,
                index: this.rightPadded(this.convert(node.argumentExpression), this.suffix(node.argumentExpression))
            }
        };
    }

    visitCallExpression(node: ts.CallExpression): J.MethodInvocation {
        const prefix = this.prefix(node);
        const typeArguments = node.typeArguments && this.mapTypeArguments(this.prefix(this.findChildNode(node, ts.SyntaxKind.LessThanToken)!), node.typeArguments);

        let select: J.RightPadded<Expression> | undefined;
        let name: J.Identifier = {
            kind: J.Kind.Identifier,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            annotations: [],
            simpleName: "",
            type: undefined,
            fieldType: undefined
        };

        if (ts.isIdentifier(node.expression) && !node.questionDotToken) {
            select = undefined;
            name = this.convert(node.expression);
        } else if (node.questionDotToken) {
            select = this.rightPadded(
                produce(this.convert<Expression>(node.expression), draft => {
                    if (node.questionDotToken) {
                        draft.markers.markers.push({
                            kind: JS.Markers.Optional,
                            id: randomId(),
                            prefix: this.suffix(node.expression)
                        } as Optional);
                    }
                }),
                emptySpace
            )
        } else {
            select = this.rightPadded(this.visit(node.expression), this.suffix(node.expression))
        }

        return {
            kind: J.Kind.MethodInvocation,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            select,
            typeParameters: typeArguments,
            name,
            arguments: this.mapCommaSeparatedList(node.getChildren(this.sourceFile).slice(-3)),
            methodType: this.mapMethodType(node)
        }
    }

    visitSuperKeyword(node: ts.KeywordToken<ts.SyntaxKind.SuperKeyword>) {
        return this.mapIdentifier(node, node.getText());
    }

    visitNewExpression(node: ts.NewExpression): J.NewClass {
        return {
            kind: J.Kind.NewClass,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            new: emptySpace,
            class: node.typeArguments ? {
                kind: J.Kind.ParameterizedType,
                id: randomId(),
                prefix: emptySpace,
                markers: emptyMarkers,
                class: {
                    kind: JS.Kind.TypeTreeExpression,
                    id: randomId(),
                    prefix: emptySpace,
                    markers: emptyMarkers,
                    expression: this.visit(node.expression),
                },
                typeParameters: this.mapTypeArguments(this.prefix(this.findChildNode(node, ts.SyntaxKind.LessThanToken)!), node.typeArguments),
                type: undefined
            } as J.ParameterizedType : {
                kind: JS.Kind.TypeTreeExpression,
                id: randomId(),
                prefix: emptySpace,
                markers: emptyMarkers,
                expression: this.visit(node.expression),
            } as JS.TypeTreeExpression,
            arguments: node.arguments ?
                this.mapCommaSeparatedList(this.getParameterListNodes(node)) : {
                    ...emptyContainer<Expression>(),
                    markers: markers({
                        kind: J.Markers.OmitParentheses,
                        id: randomId()
                    })
                },
            constructorType: this.mapMethodType(node)
        };
    }

    visitTaggedTemplateExpression(node: ts.TaggedTemplateExpression): JS.TaggedTemplateExpression {
        return {
            kind: JS.Kind.TaggedTemplateExpression,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            tag: this.rightPadded(this.visit(node.tag), this.suffix(node.tag)),
            typeArguments: node.typeArguments && this.mapTypeArguments(emptySpace, node.typeArguments),
            templateExpression: this.convert(node.template),
            type: this.mapType(node)
        }
    }

    visitTypeAssertionExpression(node: ts.TypeAssertion): J.TypeCast {
        return {
            kind: J.Kind.TypeCast,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            class: {
                kind: J.Kind.ControlParentheses,
                id: randomId(),
                prefix: this.prefix(node.getFirstToken()!),
                markers: emptyMarkers,
                tree: this.rightPadded(this.convert(node.type), this.prefix(node.getChildAt(2, this.sourceFile)))
            },
            expression: this.convert(node.expression),
        };
    }

    visitParenthesizedExpression(node: ts.ParenthesizedExpression): J.Parentheses<Expression> {
        return {
            kind: J.Kind.Parentheses,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            tree: this.rightPadded(this.convert(node.expression), this.prefix(node.getLastToken()!))
        };
    }

    visitArrowFunction(node: ts.ArrowFunction): JS.ArrowFunction {
        const openParenToken = this.findChildNode(node, ts.SyntaxKind.OpenParenToken);
        const isParenthesized = openParenToken != undefined;
        return {
            kind: JS.Kind.ArrowFunction,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: [],
            modifiers: this.mapModifiers(node),
            typeParameters: node.typeParameters && this.mapTypeParametersAsObject(node),
            lambda: {
                kind: J.Kind.Lambda,
                id: randomId(),
                markers: emptyMarkers,
                parameters: {
                    kind: J.Kind.LambdaParameters,
                    id: randomId(),
                    prefix: isParenthesized ? this.prefix(openParenToken) : emptySpace,
                    markers: emptyMarkers,
                    parenthesized: isParenthesized,
                    parameters: node.parameters.length > 0 ?
                        node.parameters.map(p => this.rightPadded(this.convert(p), this.suffix(p)))
                            .concat(node.parameters.hasTrailingComma ? this.rightPadded(this.newEmpty(), this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseParenToken)!)) : []) :
                        isParenthesized ? [this.rightPadded(this.newEmpty(), this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseParenToken)!))] : [],
                },
                arrow: this.prefix(node.equalsGreaterThanToken),
                body: this.convert(node.body),
                type: this.mapType(node)
            } as J.Lambda,
            returnTypeExpression: this.mapTypeInfo(node)
        };
    }

    visitDeleteExpression(node: ts.DeleteExpression): JS.Delete {
        return {
            kind: JS.Kind.Delete,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            expression: this.convert(node.expression)
        };
    }

    visitTypeOfExpression(node: ts.TypeOfExpression): JS.TypeOf {
        return {
            kind: JS.Kind.TypeOf,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            expression: this.convert(node.expression),
            type: this.mapType(node)
        };
    }

    visitVoidExpression(node: ts.VoidExpression): JS.Void {
        return {
            kind: JS.Kind.Void,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            expression: this.convert(node.expression)
        };
    }

    visitAwaitExpression(node: ts.AwaitExpression): JS.Await {
        return {
            kind: JS.Kind.Await,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            expression: this.convert(node.expression),
            type: this.mapType(node)
        };
    }

    visitPrefixUnaryExpression(node: ts.PrefixUnaryExpression): J.Unary | J.Unknown {
        let unaryOperator: J.Unary.Type | undefined;
        switch (node.operator) {
            case ts.SyntaxKind.PlusToken:
                unaryOperator = J.Unary.Type.Positive;
                break;
            case ts.SyntaxKind.MinusToken:
                unaryOperator = J.Unary.Type.Negative;
                break;
            case ts.SyntaxKind.ExclamationToken:
                unaryOperator = J.Unary.Type.Not;
                break;
            case ts.SyntaxKind.PlusPlusToken:
                unaryOperator = J.Unary.Type.PreIncrement;
                break;
            case ts.SyntaxKind.MinusMinusToken:
                unaryOperator = J.Unary.Type.PreDecrement;
                break;
            case ts.SyntaxKind.TildeToken:
                unaryOperator = J.Unary.Type.Complement;
        }

        if (unaryOperator === undefined) {
            return this.visitUnknown(node);
        }

        return {
            kind: J.Kind.Unary,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            operator: this.leftPadded(this.prefix(node.getFirstToken()!), unaryOperator),
            expression: this.convert(node.operand),
            type: this.mapType(node)
        };
    }

    visitPostfixUnaryExpression(node: ts.PostfixUnaryExpression): J.Unary | J.Unknown {
        let unaryOperator: J.Unary.Type | undefined;
        switch (node.operator) {
            case ts.SyntaxKind.PlusPlusToken:
                unaryOperator = J.Unary.Type.PostIncrement;
                break;
            case ts.SyntaxKind.MinusMinusToken:
                unaryOperator = J.Unary.Type.PostDecrement;
                break;
        }

        if (unaryOperator === undefined) {
            return this.visitUnknown(node);
        }

        return {
            kind: J.Kind.Unary,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            operator: this.leftPadded(this.suffix(node.operand), unaryOperator),
            expression: this.convert(node.operand),
            type: this.mapType(node),
        };
    }

    visitBinaryExpression(node: ts.BinaryExpression): J.Binary | J.AssignmentOperation | JS.AssignmentOperation | J.Assignment | JS.Binary | J.InstanceOf | J.Unknown {
        if (node.operatorToken.kind === ts.SyntaxKind.EqualsToken) {
            // assignment is also represented as `ts.BinaryExpression`
            return {
                kind: J.Kind.Assignment,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                variable: this.convert(node.left),
                assignment: this.leftPadded(this.suffix(node.left), this.convert(node.right)),
                type: this.mapType(node)
            };
        }

        let binaryOperator: J.Binary.Type | JS.Binary.Type | undefined;
        switch (node.operatorToken.kind) {
            case ts.SyntaxKind.EqualsEqualsEqualsToken:
                binaryOperator = JS.Binary.Type.IdentityEquals;
                break;
            case ts.SyntaxKind.ExclamationEqualsEqualsToken:
                binaryOperator = JS.Binary.Type.IdentityNotEquals;
                break;
            case ts.SyntaxKind.QuestionQuestionToken:
                binaryOperator = JS.Binary.Type.QuestionQuestion;
                break;
            case ts.SyntaxKind.InKeyword:
                binaryOperator = JS.Binary.Type.In;
                break;
            case ts.SyntaxKind.CommaToken:
                binaryOperator = JS.Binary.Type.Comma;
                break;
        }

        if (binaryOperator !== undefined) {
            return {
                kind: JS.Kind.Binary,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                left: this.convert(node.left),
                operator: this.leftPadded(this.prefix(node.operatorToken), binaryOperator as JS.Binary.Type),
                right: this.convert(node.right),
                type: this.mapType(node),
            };
        }

        if (node.operatorToken.kind === ts.SyntaxKind.InstanceOfKeyword) {
            return {
                kind: J.Kind.InstanceOf,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                expression: this.rightPadded(this.convert(node.left), this.prefix(node.operatorToken)),
                class: this.convert(node.right),
                type: this.mapType(node),
            };
        }

        binaryOperator = this.mapBinaryOperator(node);
        if (binaryOperator === undefined) {
            let assignmentOperation;

            switch (node.operatorToken.kind) {
                case ts.SyntaxKind.QuestionQuestionEqualsToken:
                    assignmentOperation = JS.AssignmentOperation.Type.QuestionQuestion;
                    break;
                case ts.SyntaxKind.AmpersandAmpersandEqualsToken:
                    assignmentOperation = JS.AssignmentOperation.Type.And;
                    break;
                case ts.SyntaxKind.BarBarEqualsToken:
                    assignmentOperation = JS.AssignmentOperation.Type.Or;
                    break;
                case ts.SyntaxKind.AsteriskAsteriskToken:
                    assignmentOperation = JS.AssignmentOperation.Type.Power;
                    break;
                case ts.SyntaxKind.AsteriskAsteriskEqualsToken:
                    assignmentOperation = JS.AssignmentOperation.Type.Exp;
                    break;
            }

            if (assignmentOperation !== undefined) {
                return {
                    kind: JS.Kind.AssignmentOperation,
                    id: randomId(),
                    prefix: this.prefix(node),
                    markers: emptyMarkers,
                    variable: this.convert(node.left),
                    operator: this.leftPadded(this.prefix(node.operatorToken), assignmentOperation),
                    assignment: this.convert(node.right),
                    type: this.mapType(node),
                }
            }

            assignmentOperation = this.mapAssignmentOperation(node);
            if (assignmentOperation === undefined) {
                return this.visitUnknown(node);
            }
            return {
                kind: J.Kind.AssignmentOperation,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                variable: this.convert(node.left),
                operator: this.leftPadded(this.prefix(node.operatorToken), assignmentOperation),
                assignment: this.convert(node.right),
                type: this.mapType(node)
            }
        }

        return {
            kind: J.Kind.Binary,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            left: this.convert(node.left),
            operator: this.leftPadded(this.prefix(node.operatorToken), binaryOperator),
            right: this.convert(node.right),
            type: this.mapType(node)
        }
    }

    private mapBinaryOperator(node: ts.BinaryExpression): J.Binary.Type | undefined {
        switch (node.operatorToken.kind) {
            case ts.SyntaxKind.PlusToken:
                return J.Binary.Type.Addition;
            case ts.SyntaxKind.MinusToken:
                return J.Binary.Type.Subtraction;
            case ts.SyntaxKind.AsteriskToken:
                return J.Binary.Type.Multiplication;
            case ts.SyntaxKind.SlashToken:
                return J.Binary.Type.Division;
            case ts.SyntaxKind.PercentToken:
                return J.Binary.Type.Modulo;
            case ts.SyntaxKind.LessThanLessThanToken:
                return J.Binary.Type.LeftShift;
            case ts.SyntaxKind.GreaterThanGreaterThanToken:
                return J.Binary.Type.RightShift;
            case ts.SyntaxKind.GreaterThanGreaterThanGreaterThanToken:
                return J.Binary.Type.UnsignedRightShift;
            // case ts.SyntaxKind.LessThanLessThanEqualsToken:
            //     return J.Binary.Type.LeftShiftEquals;
            // case ts.SyntaxKind.GreaterThanGreaterThanEqualsToken:
            //     return J.Binary.Type.RightShiftEquals;

            case ts.SyntaxKind.AmpersandToken:
                return J.Binary.Type.BitAnd;
            // case ts.SyntaxKind.AmpersandEqualsToken:
            //     return J.Binary.Type.BitwiseAndEquals;
            case ts.SyntaxKind.BarToken:
                return J.Binary.Type.BitOr;
            // case ts.SyntaxKind.BarEqualsToken:
            //     return J.Binary.Type.BitwiseOrEquals;
            case ts.SyntaxKind.CaretToken:
                return J.Binary.Type.BitXor;
            // case ts.SyntaxKind.CaretEqualsToken:
            //     return J.Binary.Type.BitwiseXorEquals;

            case ts.SyntaxKind.EqualsEqualsToken:
                return J.Binary.Type.Equal;
            // case ts.SyntaxKind.EqualsEqualsEqualsToken:
            //     return J.Binary.Type.StrictEquals;
            case ts.SyntaxKind.ExclamationEqualsToken:
                return J.Binary.Type.NotEqual;
            // case ts.SyntaxKind.ExclamationEqualsEqualsToken:
            //     return J.Binary.Type.StrictNotEquals;
            case ts.SyntaxKind.LessThanToken:
                return J.Binary.Type.LessThan;
            case ts.SyntaxKind.LessThanEqualsToken:
                return J.Binary.Type.LessThanOrEqual;
            case ts.SyntaxKind.GreaterThanToken:
                return J.Binary.Type.GreaterThan;
            case ts.SyntaxKind.GreaterThanEqualsToken:
                return J.Binary.Type.GreaterThanOrEqual;

            case ts.SyntaxKind.AmpersandAmpersandToken:
                return J.Binary.Type.And;
            case ts.SyntaxKind.BarBarToken:
                return J.Binary.Type.Or;
            // case ts.SyntaxKind.BarBarEqualsToken:
            //     return J.Binary.Type.OrEquals;
            // case ts.SyntaxKind.AmpersandEqualsToken:
            //     return J.Binary.Type.AndEquals;
        }
        return undefined;
    }

    private mapAssignmentOperation(node: ts.BinaryExpression): J.AssignmentOperation.Type | undefined {
        switch (node.operatorToken.kind) {
            case ts.SyntaxKind.PlusEqualsToken:
                return J.AssignmentOperation.Type.Addition;
            case ts.SyntaxKind.MinusEqualsToken:
                return J.AssignmentOperation.Type.Subtraction;
            case ts.SyntaxKind.AsteriskEqualsToken:
                return J.AssignmentOperation.Type.Multiplication;
            case ts.SyntaxKind.SlashEqualsToken:
                return J.AssignmentOperation.Type.Division;
            case ts.SyntaxKind.PercentEqualsToken:
                return J.AssignmentOperation.Type.Modulo;
            case ts.SyntaxKind.LessThanLessThanEqualsToken:
                return J.AssignmentOperation.Type.LeftShift;
            case ts.SyntaxKind.GreaterThanGreaterThanEqualsToken:
                return J.AssignmentOperation.Type.RightShift;
            case ts.SyntaxKind.GreaterThanGreaterThanGreaterThanEqualsToken:
                return J.AssignmentOperation.Type.UnsignedRightShift;
            case ts.SyntaxKind.AmpersandEqualsToken:
                return J.AssignmentOperation.Type.BitAnd;
            case ts.SyntaxKind.BarEqualsToken:
                return J.AssignmentOperation.Type.BitOr;
            case ts.SyntaxKind.CaretEqualsToken:
                return J.AssignmentOperation.Type.BitXor;

            // case ts.SyntaxKind.AmpersandAmpersandEqualsToken:
            //     return J.AssignmentOperation.Type.And;
            // case ts.SyntaxKind.BarBarEqualsToken:
            //     return J.AssignmentOperation.Type.Or;
            // case ts.SyntaxKind.BarBarEqualsToken:
            //     return J.Binary.Type.OrEquals;
            // case ts.SyntaxKind.AmpersandEqualsToken:
            //     return J.Binary.Type.AndEquals;
        }
        return undefined;
    }

    visitConditionalExpression(node: ts.ConditionalExpression): J.Ternary {
        return {
            kind: J.Kind.Ternary,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            condition: this.convert(node.condition),
            truePart: this.leftPadded(this.suffix(node.condition), this.convert(node.whenTrue)),
            falsePart: this.leftPadded(this.suffix(node.whenTrue), this.convert(node.whenFalse)),
            type: this.mapType(node)
        };
    }

    visitTemplateExpression(node: ts.TemplateExpression): JS.TemplateExpression {
        return {
            kind: JS.Kind.TemplateExpression,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            head: this.visit(node.head),
            spans: node.templateSpans.map(s => this.rightPadded(this.visit(s), this.suffix(s))),
            type: this.mapType(node)
        }
    }

    visitYieldExpression(node: ts.YieldExpression): J.Yield {
        return {
            kind: J.Kind.Yield,
            id: randomId(),
            prefix: this.prefix(node),
            markers: node.asteriskToken ?
                markers({
                    kind: JS.Markers.DelegatedYield,
                    id: randomId(),
                    prefix: this.prefix(node.asteriskToken)
                } as DelegatedYield) : emptyMarkers,
            value: node.expression && this.visit(node.expression),
            implicit: false
        };
    }

    visitSpreadElement(node: ts.SpreadElement): Expression {
        return produce(this.convert<Expression>(node.expression), draft => {
            draft.markers.markers.push({
                kind: JS.Markers.Spread,
                id: randomId(),
                prefix: this.prefix(node)
            } as Spread);
        });
    }

    visitClassExpression(node: ts.ClassExpression): JS.StatementExpression {
        return {
            kind: JS.Kind.StatementExpression,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            statement: {
                kind: J.Kind.ClassDeclaration,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                leadingAnnotations: this.mapDecorators(node),
                modifiers: [],
                classKind: {
                    kind: J.Kind.ClassDeclarationKind,
                    id: randomId(),
                    prefix: node.modifiers ? this.suffix(node.modifiers[node.modifiers.length - 1]) : this.prefix(node),
                    markers: emptyMarkers,
                    annotations: [],
                    type: J.ClassDeclaration.Kind.Type.Class
                },
                name: node.name ? this.convert(node.name) : this.mapIdentifier(node, ""),
                typeParameters: this.mapTypeParametersAsContainer(node),
                extends: this.mapExtends(node),
                implements: this.mapImplements(node),
                body: {
                    kind: J.Kind.Block,
                    id: randomId(),
                    prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBraceToken)!),
                    markers: emptyMarkers,
                    static: this.rightPadded(false, emptySpace),
                    statements: node.members.map(ce => ({
                        kind: J.Kind.RightPadded,
                        element: this.convert(ce),
                        after: ce.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken ? this.prefix(ce.getLastToken()!) : emptySpace,
                        markers: ce.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken ? markers({
                            kind: J.Markers.Semicolon,
                            id: randomId()
                        }) : emptyMarkers
                    })),
                    end: this.prefix(node.getLastToken()!)
                },
                type: this.mapType(node)
            } as J.ClassDeclaration,
        }
    }

    visitOmittedExpression(node: ts.OmittedExpression) {
        return this.newEmpty(this.prefix(node));
    }

    visitExpressionWithTypeArguments(node: ts.ExpressionWithTypeArguments) {
        if (node.typeArguments) {
            return {
                kind: JS.Kind.ExpressionWithTypeArguments,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                clazz: this.visit(node.expression),
                typeArguments: this.mapTypeArguments(this.suffix(node.expression), node.typeArguments),
                type: this.mapType(node)
            }
        }
        return this.visit(node.expression);
    }

    visitAsExpression(node: ts.AsExpression): JS.Binary {
        return {
            kind: JS.Kind.Binary,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            left: this.convert(node.expression),
            operator: this.leftPadded(this.prefix(node.getChildAt(1, this.sourceFile)), JS.Binary.Type.As),
            right: this.convert(node.type),
            type: this.mapType(node),
        };
    }

    visitNonNullExpression(node: ts.NonNullExpression): Expression {
        return produce(this.visit(node.expression) as Expression, draft => {
            draft.markers.markers.push({
                kind: JS.Markers.NonNullAssertion,
                id: randomId(),
                prefix: this.suffix(node.expression)
            } as NonNullAssertion);
        });
    }

    visitMetaProperty(node: ts.MetaProperty): J.FieldAccess {
        return {
            kind: J.Kind.FieldAccess,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            target: node.keywordToken === ts.SyntaxKind.NewKeyword ? this.mapIdentifier(node, 'new') : this.mapIdentifier(node, 'import'),
            name: this.leftPadded(this.prefix(node.getChildAt(1, this.sourceFile)), this.convert(node.name)),
            type: this.mapType(node)
        };
    }

    visitSyntheticExpression(node: ts.SyntheticExpression): J.Unknown {
        // SyntheticExpression is a special type of node used internally by the TypeScript compiler
        return this.visitUnknown(node);
    }

    visitSatisfiesExpression(node: ts.SatisfiesExpression): JS.SatisfiesExpression {
        return {
            kind: JS.Kind.SatisfiesExpression,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            expression: this.visit(node.expression),
            satisfiesType: this.leftPadded(this.suffix(node.expression), this.visit(node.type)),
            type: this.mapType(node)
        };
    }

    visitTemplateSpan(node: ts.TemplateSpan): JS.TemplateExpression.Span {
        return {
            kind: JS.Kind.TemplateExpressionSpan,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            expression: this.convert(node.expression),
            tail: this.visit(node.literal)
        }
    }

    visitSemicolonClassElement(node: ts.SemicolonClassElement): J.Empty {
        return this.newEmpty(this.semicolonPrefix(node));
    }

    visitBlock(node: ts.Block): J.Block {
        return {
            kind: J.Kind.Block,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            static: this.rightPadded(false, emptySpace),
            statements: this.semicolonPaddedStatementList(node.statements),
            end: this.prefix(node.getLastToken()!)
        };
    }

    visitEmptyStatement(node: ts.EmptyStatement): J.Empty {
        return this.newEmpty(this.prefix(node));
    }

    visitVariableStatement(node: ts.VariableStatement) {
        return produce(this.visitVariableDeclarationList(node.declarationList), draft => {
            draft.modifiers = this.mapModifiers(node).concat(draft.modifiers);
            draft.prefix = this.prefix(node);
        });
    }

    visitExpressionStatement(node: ts.ExpressionStatement): Statement {
        const expression = this.visit(node.expression) as Expression;
        if (isStatement(expression)) {
            return expression as Statement;
        }
        return {
            kind: JS.Kind.ExpressionStatement,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            expression: expression
        } as JS.ExpressionStatement;
    }

    visitIfStatement(node: ts.IfStatement): J.If {
        const semicolonAfterThen = (node.thenStatement.getChildAt(node.thenStatement.getChildCount() - 1)?.kind === ts.SyntaxKind.SemicolonToken);
        const semicolonAfterElse = (node.elseStatement?.getChildAt(node.elseStatement.getChildCount() - 1)?.kind === ts.SyntaxKind.SemicolonToken);
        return {
            kind: J.Kind.If,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            ifCondition: {
                kind: J.Kind.ControlParentheses,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                markers: emptyMarkers,
                tree: this.rightPadded(this.visit(node.expression), this.suffix(node.expression))
            },
            thenPart: this.rightPadded(
                this.convert(node.thenStatement),
                semicolonAfterThen ? this.prefix(node.thenStatement.getLastToken()!) : emptySpace,
                semicolonAfterThen ? markers({kind: J.Markers.Semicolon, id: randomId()}) : emptyMarkers
            ),
            elsePart: node.elseStatement && {
                kind: J.Kind.IfElse,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.ElseKeyword)!),
                markers: emptyMarkers,
                body: this.rightPadded(
                    this.convert(node.elseStatement),
                    semicolonAfterElse ? this.prefix(node.elseStatement.getLastToken()!) : emptySpace,
                    semicolonAfterElse ? markers({kind: J.Markers.Semicolon, id: randomId()}) : emptyMarkers
                )
            }
        };
    }

    visitDoStatement(node: ts.DoStatement): J.DoWhileLoop {
        return {
            kind: J.Kind.DoWhileLoop,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            body: this.rightPadded(this.visit(node.statement),
                this.semicolonPrefix(node.statement),
                node.statement.getChildAt(node.statement.getChildCount() - 1)?.kind === ts.SyntaxKind.SemicolonToken ? markers({
                    kind: J.Markers.Semicolon,
                    id: randomId()
                }) : emptyMarkers),
            whileCondition: this.leftPadded(
                this.prefix(this.findChildNode(node, ts.SyntaxKind.WhileKeyword)!),
                {
                    kind: J.Kind.ControlParentheses,
                    id: randomId(),
                    prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                    markers: emptyMarkers,
                    tree: this.rightPadded(this.visit(node.expression), this.suffix(node.expression))
                }
            )
        };
    }

    visitWhileStatement(node: ts.WhileStatement): J.WhileLoop {
        return {
            kind: J.Kind.WhileLoop,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            condition: {
                kind: J.Kind.ControlParentheses,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                markers: emptyMarkers,
                tree: this.rightPadded(this.visit(node.expression), this.suffix(node.expression))
            },
            body: this.rightPadded(
                this.convert(node.statement),
                this.semicolonPrefix(node.statement),
                node.statement.getChildAt(node.statement.getChildCount() - 1)?.kind === ts.SyntaxKind.SemicolonToken ? markers({
                    kind: J.Markers.Semicolon,
                    id: randomId()
                }) : emptyMarkers
            ),
        };
    }

    visitForStatement(node: ts.ForStatement): J.ForLoop {
        return {
            kind: J.Kind.ForLoop,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            control: {
                kind: J.Kind.ForLoopControl,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                markers: emptyMarkers,
                init: [node.initializer ?
                    (ts.isVariableDeclarationList(node.initializer) ? this.rightPadded(this.visit(node.initializer), emptySpace) :
                        this.rightPadded(ts.isStatement(node.initializer) ? this.visit(node.initializer) : {
                            kind: JS.Kind.ExpressionStatement,
                            id: randomId(),
                            prefix: emptySpace,
                            markers: emptyMarkers,
                            expression: this.visit(node.initializer)
                        }, this.suffix(node.initializer))) :
                    this.rightPadded(this.newEmpty(), this.suffix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!))],
                condition: node.condition ? this.rightPadded(this.visit(node.condition), this.suffix(node.condition)) :
                    this.rightPadded(this.newEmpty(), this.suffix(this.findChildNode(node, ts.SyntaxKind.SemicolonToken)!)),
                update: [node.incrementor ? this.rightPadded(ts.isStatement(node.incrementor) ? this.visit(node.incrementor) : {
                        kind: JS.Kind.ExpressionStatement,
                        id: randomId(),
                        prefix: emptySpace,
                        markers: emptyMarkers,
                        expression: this.visit(node.incrementor)
                    }, this.suffix(node.incrementor)) :
                    this.rightPadded(this.newEmpty(this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseParenToken)!)), emptySpace)]
            },
            body: this.rightPadded(
                this.convert(node.statement),
                this.semicolonPrefix(node.statement),
                node.statement.getChildAt(node.statement.getChildCount() - 1)?.kind === ts.SyntaxKind.SemicolonToken ? markers({
                    kind: J.Markers.Semicolon,
                    id: randomId()
                }) : emptyMarkers
            )
        };
    }

    visitForInStatement(node: ts.ForInStatement): JS.ForInLoop {
        return {
            kind: JS.Kind.ForInLoop,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            control: {
                kind: J.Kind.ForEachLoopControl,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                markers: emptyMarkers,
                variable: this.rightPadded(this.visit(node.initializer), this.suffix(node.initializer)),
                iterable: this.rightPadded(this.visit(node.expression), this.suffix(node.expression))
            },
            body: this.rightPadded(
                this.convert(node.statement),
                this.semicolonPrefix(node.statement),
                node.statement.getChildAt(node.statement.getChildCount() - 1)?.kind === ts.SyntaxKind.SemicolonToken ? markers({
                    kind: J.Markers.Semicolon,
                    id: randomId()
                }) : emptyMarkers
            )
        };
    }

    visitForOfStatement(node: ts.ForOfStatement): JS.ForOfLoop {
        return {
            kind: JS.Kind.ForOfLoop,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            await: node.awaitModifier && this.prefix(node.awaitModifier),
            loop: {
                kind: J.Kind.ForEachLoop,
                id: randomId(),
                prefix: emptySpace,
                markers: emptyMarkers,
                control: {
                    kind: J.Kind.ForEachLoopControl,
                    id: randomId(),
                    prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                    markers: emptyMarkers,
                    variable: this.rightPadded(this.visit(node.initializer), this.suffix(node.initializer)),
                    iterable: this.rightPadded(this.visit(node.expression), this.suffix(node.expression))
                },
                body: this.rightPadded(
                    this.convert(node.statement),
                    this.semicolonPrefix(node.statement),
                    node.statement.getChildAt(node.statement.getChildCount() - 1)?.kind === ts.SyntaxKind.SemicolonToken ? markers({
                        kind: J.Markers.Semicolon,
                        id: randomId()
                    }) : emptyMarkers
                )
            }
        };
    }

    visitContinueStatement(node: ts.ContinueStatement): J.Continue {
        return {
            kind: J.Kind.Continue,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            label: node.label && this.visit(node.label)
        };
    }

    visitBreakStatement(node: ts.BreakStatement): J.Break {
        return {
            kind: J.Kind.Break,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            label: node.label && this.visit(node.label)
        };
    }

    visitReturnStatement(node: ts.ReturnStatement): J.Return {
        return {
            kind: J.Kind.Return,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            expression: node.expression && this.convert<Expression>(node.expression)
        };
    }

    visitWithStatement(node: ts.WithStatement): JS.WithStatement {
        return {
            kind: JS.Kind.WithStatement,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            expression: {
                kind: J.Kind.ControlParentheses,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                markers: emptyMarkers,
                tree: this.rightPadded(this.visit(node.expression), this.suffix(node.expression))
            },
            body: this.rightPadded(
                this.convert(node.statement),
                this.semicolonPrefix(node.statement),
                node.statement.getChildAt(node.statement.getChildCount() - 1)?.kind === ts.SyntaxKind.SemicolonToken ? markers({
                    kind: J.Markers.Semicolon,
                    id: randomId()
                }) : emptyMarkers
            ),
        };
    }

    visitSwitchStatement(node: ts.SwitchStatement): J.Switch {
        return {
            kind: J.Kind.Switch,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            selector: {
                kind: J.Kind.ControlParentheses,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                markers: emptyMarkers,
                tree: this.rightPadded(this.visit(node.expression), this.suffix(node.expression))
            },
            cases: this.visit(node.caseBlock)
        };
    }

    visitLabeledStatement(node: ts.LabeledStatement): J.Label {
        return {
            kind: J.Kind.Label,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            label: this.rightPadded(this.visit(node.label), this.suffix(node.label)),
            statement: {
                kind: JS.Kind.TrailingTokenStatement,
                id: randomId(),
                prefix: emptySpace,
                markers: emptyMarkers,
                expression: this.rightPadded(
                    this.visit(node.statement),
                    this.semicolonPrefix(node.statement),
                    node.statement.getChildAt(node.statement.getChildCount() - 1)?.kind === ts.SyntaxKind.SemicolonToken ? markers({
                        kind: J.Markers.Semicolon,
                        id: randomId()
                    }) : emptyMarkers
                ),
                type: this.mapType(node.statement)
            } as JS.TrailingTokenStatement
        };
    }

    visitThrowStatement(node: ts.ThrowStatement): J.Throw {
        return {
            kind: J.Kind.Throw,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            exception: this.visit(node.expression)
        };
    }

    visitTryStatement(node: ts.TryStatement): J.Try {
        return {
            kind: J.Kind.Try,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            body: this.visit(node.tryBlock),
            catches: node.catchClause ? [this.visit(node.catchClause)] : [],
            finally: node.finallyBlock && this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.FinallyKeyword)!), this.visit(node.finallyBlock))
        };
    }

    visitDebuggerStatement(node: ts.DebuggerStatement): JS.ExpressionStatement {
        return {
            kind: JS.Kind.ExpressionStatement,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            expression: this.mapIdentifier(node, 'debugger')
        };
    }

    visitVariableDeclaration(node: ts.VariableDeclaration): J.VariableDeclarations.NamedVariable {
        return {
            kind: J.Kind.NamedVariable,
            id: randomId(),
            prefix: this.prefix(node),
            markers: produce(emptyMarkers, draft => {
                if (node.exclamationToken) {
                    draft.markers.push({
                        kind: JS.Markers.NonNullAssertion,
                        id: randomId(),
                        prefix: this.suffix(node.name)
                    } as NonNullAssertion);
                }
            }),
            name: this.visit(node.name),
            dimensionsAfterName: [],
            initializer: node.initializer && this.leftPadded(this.prefix(node.getChildAt(node.getChildCount(this.sourceFile) - 2)), this.visit(node.initializer)),
            variableType: this.mapVariableType(node),
        } as J.VariableDeclarations.NamedVariable;
    }

    visitVariableDeclarationList(node: ts.VariableDeclarationList): JS.ScopedVariableDeclarations {
        let kind = node.getFirstToken();

        // to parse the declaration case: await using db = ...
        let modifier;
        if (kind?.kind === ts.SyntaxKind.AwaitKeyword) {
            modifier = {
                kind: J.Kind.Modifier,
                id: randomId(),
                prefix: this.prefix(kind),
                markers: emptyMarkers,
                keyword: 'await',
                type: J.ModifierType.LanguageExtension,
                annotations: []
            };
            kind = node.getChildAt(1);
        }

        return {
            kind: JS.Kind.ScopedVariableDeclarations,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            modifiers: modifier ? [modifier] : [],
            scope: this.leftPadded(
                kind ? this.prefix(kind) : this.prefix(node),
                kind?.kind === ts.SyntaxKind.LetKeyword
                    ? JS.ScopedVariableDeclarations.Scope.Let
                    : kind?.kind === ts.SyntaxKind.ConstKeyword
                        ? JS.ScopedVariableDeclarations.Scope.Const
                        : kind?.kind === ts.SyntaxKind.UsingKeyword
                            ? JS.ScopedVariableDeclarations.Scope.Using
                            : JS.ScopedVariableDeclarations.Scope.Var
            ),
            variables: node.declarations.map((declaration) => {
                // FIXME this is suspect... we are creating a whole VariableDeclarations for each declaration?
                return this.rightPadded(
                    {
                        kind: J.Kind.VariableDeclarations,
                        id: randomId(),
                        prefix: this.prefix(declaration),
                        markers: emptyMarkers,
                        leadingAnnotations: [],
                        modifiers: [],
                        typeExpression: this.mapTypeInfo(declaration),
                        variables: [this.rightPadded(this.visit(declaration), emptySpace)]
                    },
                    this.suffix(declaration)
                );
            })
        };
    }

    visitFunctionDeclaration(node: ts.FunctionDeclaration): J.MethodDeclaration {
        return this.mapFunctionDeclaration(node);
    }

    visitFunctionExpression(node: ts.FunctionExpression): JS.StatementExpression {
        return {
            kind: JS.Kind.StatementExpression,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            statement: this.mapFunctionDeclaration(node)
        };
    }

    private mapFunctionDeclaration(node: ts.FunctionDeclaration | ts.FunctionExpression): J.MethodDeclaration {
        return {
            kind: J.Kind.MethodDeclaration,
            id: randomId(),
            prefix: this.prefix(node),
            markers: produce(emptyMarkers, draft => {
                draft.markers.push({
                    kind: JS.Markers.FunctionDeclaration,
                    id: randomId(),
                    prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.FunctionKeyword)!)
                } as FunctionDeclaration);

                if (node.asteriskToken) {
                    draft.markers.push({
                        kind: JS.Markers.Asterisk,
                        id: randomId(),
                        prefix: this.prefix(node.asteriskToken)
                    } as Asterisk);
                }
            }),
            leadingAnnotations: [],
            nameAnnotations: [],
            modifiers: this.mapModifiers(node),
            name: node.name ? this.visit(node.name) : {
                kind: J.Kind.Identifier,
                id: randomId(),
                prefix: emptySpace,
                markers: emptyMarkers,
                annotations: [],
                simpleName: "",
                type: undefined,
                fieldType: undefined
            },
            typeParameters: this.mapTypeParametersAsObject(node),
            parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
            returnTypeExpression: this.mapTypeInfo(node),
            body: node.body && this.convert<J.Block>(node.body),
            methodType: this.mapMethodType(node)
        };
    }

    private getParameterListNodes(node: ts.SignatureDeclarationBase | ts.NewExpression, openToken: ts.SyntaxKind = ts.SyntaxKind.OpenParenToken) {
        const children = node.getChildren(this.sourceFile);
        for (let i = 0; i < children.length; i++) {
            if (children[i].kind === openToken) {
                return children.slice(i, i + 3);
            }
        }
        return [];
    }

    visitInterfaceDeclaration(node: ts.InterfaceDeclaration): J.ClassDeclaration {
        return {
            kind: J.Kind.ClassDeclaration,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: [],
            modifiers: this.mapModifiers(node),
            classKind: {
                kind: J.Kind.ClassDeclarationKind,
                id: randomId(),
                prefix: node.modifiers ? this.suffix(node.modifiers[node.modifiers.length - 1]) : this.prefix(node),
                markers: emptyMarkers,
                annotations: [],
                type: J.ClassDeclaration.Kind.Type.Interface
            },
            name: node.name ? this.convert(node.name) : this.mapIdentifier(node, ""),
            typeParameters: this.mapTypeParametersAsContainer(node),
            implements: this.mapInterfaceExtends(node),
            body: {
                kind: J.Kind.Block,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBraceToken)!),
                markers: emptyMarkers,
                static: this.rightPadded(false, emptySpace),
                statements: node.members.map(te => ({
                    kind: J.Kind.RightPadded,
                    element: this.convert(te),
                    after: (te.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken) || (te.getLastToken()?.kind === ts.SyntaxKind.CommaToken) ? this.prefix(te.getLastToken()!) : emptySpace,
                    markers: (te.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken) || (te.getLastToken()?.kind === ts.SyntaxKind.CommaToken) ? markers(this.convertToken(te.getLastToken())!) : emptyMarkers
                })),
                end: this.prefix(node.getLastToken()!)
            },
            type: this.mapType(node)
        };
    }

    visitTypeAliasDeclaration(node: ts.TypeAliasDeclaration): JS.TypeDeclaration {
        return {
            kind: JS.Kind.TypeDeclaration,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            modifiers: this.mapModifiers(node),
            name: this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.TypeKeyword)!), this.visit(node.name)),
            typeParameters: node.typeParameters && this.mapTypeParametersAsObject(node),
            initializer: this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.EqualsToken)!), this.convert(node.type)),
            type: this.mapType(node)
        };
    }

    visitEnumDeclaration(node: ts.EnumDeclaration): J.ClassDeclaration {
        return {
            kind: J.Kind.ClassDeclaration,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: [],
            modifiers: this.mapModifiers(node),
            classKind: {
                kind: J.Kind.ClassDeclarationKind,
                id: randomId(),
                prefix: node.modifiers ? this.suffix(node.modifiers[node.modifiers.length - 1]) : this.prefix(node),
                markers: emptyMarkers,
                annotations: [],
                type: J.ClassDeclaration.Kind.Type.Enum
            },
            name: node.name ? this.convert(node.name) : this.mapIdentifier(node, ""),
            body: {
                kind: J.Kind.Block,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBraceToken)!),
                markers: emptyMarkers,
                static: this.rightPadded(false, emptySpace),
                statements: [this.rightPadded(
                    {
                        kind: J.Kind.EnumValueSet,
                        id: randomId(),
                        prefix: emptySpace,
                        markers: emptyMarkers,
                        enums: node.members.map(em => this.rightPadded(this.visit(em), this.suffix(em))),
                        terminatedWithSemicolon: node.members.hasTrailingComma
                    },
                    emptySpace)],
                end: this.prefix(node.getLastToken()!)
            },
            type: this.mapType(node) as JavaType.Class
        };
    }

    visitModuleDeclaration(node: ts.ModuleDeclaration): JS.NamespaceDeclaration {
        const body = node.body && this.visit(node.body as ts.Node);

        let namespaceKeyword = this.findChildNode(node, ts.SyntaxKind.NamespaceKeyword) ?? this.findChildNode(node, ts.SyntaxKind.ModuleKeyword);
        let keywordType: JS.NamespaceDeclaration.KeywordType;
        if (namespaceKeyword == undefined) {
            keywordType = JS.NamespaceDeclaration.KeywordType.Empty;
        } else if (namespaceKeyword?.kind === ts.SyntaxKind.NamespaceKeyword) {
            keywordType = JS.NamespaceDeclaration.KeywordType.Namespace;
        } else {
            keywordType = JS.NamespaceDeclaration.KeywordType.Module;
        }
        if (body?.kind === JS.Kind.NamespaceDeclaration) {
            return {
                kind: JS.Kind.NamespaceDeclaration,
                id: randomId(),
                prefix: emptySpace,
                markers: emptyMarkers,
                modifiers: this.mapModifiers(node),
                keywordType: this.leftPadded(
                    namespaceKeyword ? this.prefix(namespaceKeyword) : emptySpace,
                    keywordType
                ),
                name: this.rightPadded(
                    (body.name.element.kind === J.Kind.FieldAccess)
                        ? this.remapFieldAccess(body.name.element, node.name)
                        : {
                            kind: J.Kind.FieldAccess,
                            id: randomId(),
                            prefix: emptySpace,
                            markers: emptyMarkers,
                            target: this.visit(node.name),
                            name: {
                                kind: J.Kind.LeftPadded,
                                before: this.suffix(node.name),
                                element: body.name.element as J.Identifier,
                                markers: emptyMarkers
                            },
                            type: undefined
                        },
                    body.name.after
                ),
                body: body.body
            };
        } else {
            return {
                kind: JS.Kind.NamespaceDeclaration,
                id: randomId(),
                prefix: node.parent.kind === ts.SyntaxKind.ModuleBlock ? this.prefix(node) : emptySpace,
                markers: emptyMarkers,
                modifiers: this.mapModifiers(node),
                keywordType: this.leftPadded(
                    namespaceKeyword ? this.prefix(namespaceKeyword) : emptySpace,
                    keywordType
                ),
                name: this.rightPadded(this.convert(node.name), this.suffix(node.name)),
                body
            };
        }
    }

    private remapFieldAccess(fa: J.FieldAccess, name: ts.ModuleName): J.FieldAccess {
        if (fa.target.kind === J.Kind.Identifier) {
            return {
                kind: J.Kind.FieldAccess,
                id: randomId(),
                prefix: emptySpace,
                markers: emptyMarkers,
                target: {
                    kind: J.Kind.FieldAccess,
                    id: randomId(),
                    prefix: emptySpace,
                    markers: emptyMarkers,
                    target: this.visit(name),
                    name: this.leftPadded(
                        this.suffix(name),
                        fa.target
                    ),
                    type: undefined
                } as J.FieldAccess,
                name: fa.name,
                type: undefined
            };
        }

        return {
            kind: J.Kind.FieldAccess,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            target: this.remapFieldAccess(fa.target as J.FieldAccess, name),
            name: fa.name,
            type: undefined
        };
    }

    visitModuleBlock(node: ts.ModuleBlock): J.Block {
        return {
            kind: J.Kind.Block,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            static: this.rightPadded(false, emptySpace),
            statements: this.semicolonPaddedStatementList(node.statements),
            end: this.prefix(node.getLastToken()!)
        };
    }

    visitCaseBlock(node: ts.CaseBlock): J.Block {
        return {
            kind: J.Kind.Block,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            static: this.rightPadded(false, emptySpace),
            statements: node.clauses.map(clause =>
                this.rightPadded(
                    this.visit(clause),
                    this.suffix(clause)
                )),
            end: this.prefix(node.getLastToken()!)
        }
    }

    visitNamespaceExportDeclaration(node: ts.NamespaceExportDeclaration) {
        return {
            kind: JS.Kind.NamespaceDeclaration,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            modifiers: [
                {
                    kind: J.Kind.Modifier,
                    id: randomId(),
                    prefix: emptySpace,
                    markers: emptyMarkers,
                    keyword: 'export',
                    type: J.ModifierType.LanguageExtension,
                    annotations: []
                },
                {
                    kind: J.Kind.Modifier,
                    id: randomId(),
                    prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.AsKeyword)!),
                    markers: emptyMarkers,
                    keyword: 'as',
                    type: J.ModifierType.LanguageExtension,
                    annotations: []
                }
            ],
            keywordType: this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.NamespaceKeyword)!), JS.NamespaceDeclaration.KeywordType.Namespace),
            name: this.rightPadded(this.convert(node.name), this.suffix(node.name)),
        };
    }

    visitImportEqualsDeclaration(node: ts.ImportEqualsDeclaration): JS.ScopedVariableDeclarations {
        const kind = this.findChildNode(node, ts.SyntaxKind.ImportKeyword)!;

        return {
            kind: JS.Kind.ScopedVariableDeclarations,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            modifiers: this.mapModifiers(node),
            scope: this.leftPadded(
                this.prefix(kind),
                JS.ScopedVariableDeclarations.Scope.Import
            ),
            variables: [
                this.rightPadded({
                    kind: J.Kind.VariableDeclarations,
                    id: randomId(),
                    prefix: emptySpace,
                    markers: emptyMarkers,
                    leadingAnnotations: [],
                    modifiers: node.isTypeOnly ? [{
                        kind: J.Kind.Modifier,
                        id: randomId(),
                        prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.TypeKeyword)!),
                        markers: emptyMarkers,
                        keyword: "type",
                        type: J.ModifierType.LanguageExtension,
                        annotations: []
                    }] : [],
                    variables: [this.rightPadded({
                        kind: J.Kind.NamedVariable,
                        id: randomId(),
                        prefix: emptySpace,
                        markers: emptyMarkers,
                        name: this.visit(node.name),
                        dimensionsAfterName: [],
                        initializer: this.leftPadded(this.suffix(node.name), this.visit(node.moduleReference)),
                        variableType: this.mapVariableType(node)
                    }, emptySpace)]
                }, emptySpace)
            ]
        }
    }

    visitImportKeyword(node: ts.ImportExpression): J.Identifier {
        // this is used for dynamic imports as in `await import('foo')`
        return this.mapIdentifier(node, 'import');
    }

    visitImportDeclaration(node: ts.ImportDeclaration): JS.Import {
        return {
            kind: JS.Kind.Import,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            importClause: node.importClause && this.visit(node.importClause),
            moduleSpecifier: this.leftPadded(node.importClause ? this.prefix(this.findChildNode(node, ts.SyntaxKind.FromKeyword)!) : emptySpace, this.visit(node.moduleSpecifier)),
            attributes: node.attributes && this.visit(node.attributes)
        };
    }

    visitImportClause(node: ts.ImportClause): JS.ImportClause {
        return {
            kind: JS.Kind.ImportClause,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            typeOnly: node.isTypeOnly,
            name: node.name && this.rightPadded(this.visit(node.name), this.suffix(node.name)),
            namedBindings: node.namedBindings && this.visit(node.namedBindings)
        };
    }

    visitNamespaceImport(node: ts.NamespaceImport): JS.Alias {
        return {
            kind: JS.Kind.Alias,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            propertyName: this.rightPadded(this.mapIdentifier(node, "*"), this.prefix(this.findChildNode(node, ts.SyntaxKind.AsKeyword)!)),
            alias: this.visit(node.name)
        };
    }

    visitNamedImports(node: ts.NamedImports): JS.NamedImports {
        return {
            kind: JS.Kind.NamedImports,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            elements: this.mapCommaSeparatedList(node.getChildren(this.sourceFile)),
            type: undefined
        };
    }

    visitImportSpecifier(node: ts.ImportSpecifier): JS.ImportSpecifier {
        return {
            kind: JS.Kind.ImportSpecifier,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            importType: this.leftPadded(
                node.isTypeOnly ? this.prefix(this.findChildNode(node, ts.SyntaxKind.TypeKeyword)!) : emptySpace,
                node.isTypeOnly
            ),
            specifier: node.propertyName
                ? {
                    kind: JS.Kind.Alias,
                    id: randomId(),
                    prefix: this.prefix(node.propertyName),
                    markers: emptyMarkers,
                    propertyName: this.rightPadded(this.convert(node.propertyName), this.suffix(node.propertyName)),
                    alias: this.convert(node.name)
                } as JS.Alias
                : this.convert(node.name) as J.Identifier,
            type: this.mapType(node),
        };
    }

    visitExportAssignment(node: ts.ExportAssignment): JS.ExportAssignment {
        return {
            kind: JS.Kind.ExportAssignment,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            exportEquals: !!node.isExportEquals,
            expression: this.leftPadded(
                this.prefix(this.findChildNode(node, node.isExportEquals ? ts.SyntaxKind.EqualsToken : ts.SyntaxKind.DefaultKeyword)!),
                this.visit(node.expression)
            )
        };
    }

    visitExportDeclaration(node: ts.ExportDeclaration): JS.ExportDeclaration {
        return {
            kind: JS.Kind.ExportDeclaration,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            modifiers: this.mapModifiers(node),
            typeOnly: this.leftPadded(node.isTypeOnly ? this.prefix(this.findChildNode(node, ts.SyntaxKind.TypeKeyword)!) : emptySpace, node.isTypeOnly),
            exportClause: node.exportClause ? this.visit(node.exportClause) : this.mapIdentifier(this.findChildNode(node, ts.SyntaxKind.AsteriskToken)!, "*"),
            moduleSpecifier: node.moduleSpecifier && this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.FromKeyword)!), this.visit(node.moduleSpecifier)),
            attributes: node.attributes && this.visit(node.attributes)
        };
    }

    visitNamedExports(node: ts.NamedExports): JS.NamedExports {
        return {
            kind: JS.Kind.NamedExports,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            elements: this.mapCommaSeparatedList(node.getChildren()),
            type: this.mapType(node)
        };
    }

    visitNamespaceExport(node: ts.NamespaceExport): JS.Alias {
        return {
            kind: JS.Kind.Alias,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            propertyName: this.rightPadded(this.mapIdentifier(this.findChildNode(node, ts.SyntaxKind.AsteriskToken)!, "*"), this.prefix(this.findChildNode(node, ts.SyntaxKind.AsKeyword)!)),
            alias: this.visit(node.name)
        }
    }

    visitExportSpecifier(node: ts.ExportSpecifier): JS.ExportSpecifier {
        return {
            kind: JS.Kind.ExportSpecifier,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            typeOnly: this.leftPadded(node.isTypeOnly ? this.prefix(this.findChildNode(node, ts.SyntaxKind.TypeKeyword)!) : emptySpace, node.isTypeOnly),
            specifier: node.propertyName
                ? {
                    kind: JS.Kind.Alias,
                    id: randomId(),
                    prefix: this.prefix(node.propertyName),
                    markers: emptyMarkers,
                    propertyName: this.rightPadded(this.convert(node.propertyName), this.suffix(node.propertyName)),
                    alias: this.convert(node.name)
                } as JS.Alias
                : this.convert<JS.Alias>(node.name),
            type: this.mapType(node)
        };
    }

    visitMissingDeclaration(node: ts.MissingDeclaration) {
        return this.visitUnknown(node);
    }

    visitExternalModuleReference(node: ts.ExternalModuleReference) {
        return {
            kind: J.Kind.MethodInvocation,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            name: this.mapIdentifier(node, "require"),
            arguments: {
                kind: J.Kind.Container,
                before: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                elements: [this.rightPadded(this.visit(node.expression), this.suffix(node.expression))],
                markers: emptyMarkers
            },
            methodType: this.mapMethodType(node)
        }
    }

    visitJsxText(node: ts.JsxText): J.Literal {
        return this.mapLiteral(node, node.text);
    }

    visitJsxElement(node: ts.JsxElement): JSX.Tag {
        const attrs = node.openingElement.attributes.properties;
        return {
            kind: JS.Kind.JsxTag,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            openName: this.leftPadded(this.prefix(node.openingElement), this.visit(node.openingElement.tagName)),
            afterName: attrs.length === 0 ?
                this.prefix(this.findChildNode(node.openingElement, ts.SyntaxKind.GreaterThanToken)!) :
                emptySpace,
            attributes:
                this.mapJsxAttributes<Attribute | SpreadAttribute>(
                    attrs,
                    this.prefix(this.findChildNode(node.openingElement, ts.SyntaxKind.GreaterThanToken)!),
                    () => emptyMarkers
                ),
            children: this.mapJsxChildren<JSX.EmbeddedExpression | JSX.Tag | J.Identifier | J.Literal>(node.children),
            closingName: this.leftPadded(this.prefix(node.closingElement.tagName), this.visit(node.closingElement.tagName)),
            afterClosingName: this.suffix(node.closingElement.tagName)
        };
    }

    visitJsxSelfClosingElement(node: ts.JsxSelfClosingElement): JSX.Tag {
        const attrs = node.attributes.properties;
        return {
            kind: JS.Kind.JsxTag,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            openName: this.leftPadded(this.prefix(node.tagName), this.visit(node.tagName)),
            afterName: attrs.length === 0 ?
                this.prefix(this.findChildNode(node, ts.SyntaxKind.GreaterThanToken)!) :
                emptySpace,
            attributes:
                this.mapJsxAttributes<Attribute | SpreadAttribute>(
                    attrs,
                    this.prefix(this.findChildNode(node, ts.SyntaxKind.GreaterThanToken)!),
                    () => emptyMarkers
                ),
            selfClosing: this.prefix(this.findChildNode(node, ts.SyntaxKind.SlashToken)!),
        };
    }

    visitJsxFragment(node: ts.JsxFragment): JSX.Tag {
        return {
            kind: JS.Kind.JsxTag,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            openName: this.leftPadded(this.prefix(node.openingFragment), this.newEmpty()),
            afterName: this.prefix(this.findChildNode(node.openingFragment, ts.SyntaxKind.GreaterThanToken)!),
            attributes: [],
            children: this.mapJsxChildren<JSX.EmbeddedExpression | JSX.Tag | J.Identifier | J.Literal>(node.children),
            closingName: this.leftPadded(emptySpace, this.newEmpty()),
            afterClosingName: emptySpace
        };
    }

    visitJsxAttribute(node: ts.JsxAttribute): JSX.Attribute {
        return {
            kind: JS.Kind.JsxAttribute,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            key: this.visit(node.name),
            value: node.initializer
                ? this.leftPadded(
                    this.prefix(this.findChildNode(node, ts.SyntaxKind.EqualsToken)!),
                    this.visit(node.initializer)
                )
                : undefined
        };
    }

    visitJsxSpreadAttribute(node: ts.JsxSpreadAttribute): JSX.SpreadAttribute {
        return {
            kind: JS.Kind.JsxSpreadAttribute,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            dots: this.prefix(this.findChildNode(node, ts.SyntaxKind.DotDotDotToken)!),
            expression: this.rightPadded(this.visit(node.expression),
                this.suffix(node.expression))
        };
    }

    visitJsxExpression(node: ts.JsxExpression): JSX.EmbeddedExpression {
        return {
            kind: JS.Kind.JsxEmbeddedExpression,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            expression: this.rightPadded(
                node.expression ?
                    this.convert<Expression>(node.expression) :
                    this.newEmpty(),
                this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseBraceToken)!)
            )
        };
    }

    visitJsxNamespacedName(node: ts.JsxNamespacedName): JSX.NamespacedName {
        return {
            kind: JS.Kind.JsxNamespacedName,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            namespace: this.mapIdentifier(node.namespace, node.namespace.getText()),
            name: this.leftPadded(
                this.prefix(this.findChildNode(node, ts.SyntaxKind.ColonToken)!),
                this.mapIdentifier(node.name, node.name.getText())
            )
        };
    }

    visitCaseClause(node: ts.CaseClause): J.Case {
        return {
            kind: J.Kind.Case,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            type: J.Case.Type.Statement,
            caseLabels: {
                kind: J.Kind.Container,
                before: this.prefix(node.expression),
                elements: [this.rightPadded(
                    this.visit(node.expression),
                    this.suffix(node.expression)
                )],
                markers: emptyMarkers
            },
            statements: {
                kind: J.Kind.Container,
                before: this.prefix(node),
                elements: this.semicolonPaddedStatementList(node.statements),
                markers: emptyMarkers
            },
        };
    }

    visitDefaultClause(node: ts.DefaultClause): J.Case {
        return {
            kind: J.Kind.Case,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            type: J.Case.Type.Statement,
            caseLabels: {
                kind: J.Kind.Container,
                before: this.prefix(node),
                elements: [this.rightPadded(this.mapIdentifier(node, 'default'), this.suffix(this.findChildNode(node, ts.SyntaxKind.DefaultKeyword)!))],
                markers: emptyMarkers
            },
            statements: {
                kind: J.Kind.Container,
                before: this.prefix(node),
                elements: this.semicolonPaddedStatementList(node.statements),
                markers: emptyMarkers
            },
        };
    }

    visitHeritageClause(node: ts.HeritageClause) {
        return this.convert(node.types[0]);
    }

    visitCatchClause(node: ts.CatchClause): J.Try.Catch {
        return {
            kind: J.Kind.TryCatch,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            parameter: node.variableDeclaration ?
                {
                    kind: J.Kind.ControlParentheses,
                    id: randomId(),
                    prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                    markers: emptyMarkers,
                    tree: this.rightPadded(
                        {
                            kind: J.Kind.VariableDeclarations,
                            id: randomId(),
                            prefix: this.prefix(node.variableDeclaration),
                            markers: emptyMarkers,
                            leadingAnnotations: [],
                            modifiers: [],
                            typeExpression: this.mapTypeInfo(node.variableDeclaration),
                            variables: [this.rightPadded(this.visit(node.variableDeclaration), emptySpace)]
                        },
                        this.suffix(node.variableDeclaration))
                } :
                // should return empty variables list to handle: try { } catch { }
                {
                    kind: J.Kind.ControlParentheses,
                    id: randomId(),
                    prefix: emptySpace,
                    markers: emptyMarkers,
                    tree: this.rightPadded({
                        kind: J.Kind.VariableDeclarations,
                        id: randomId(),
                        prefix: emptySpace,
                        markers: emptyMarkers,
                        leadingAnnotations: [],
                        modifiers: [],
                        variables: []
                    }, emptySpace)
                },
            body: this.visit(node.block),
        }
    }

    visitImportAttributes(node: ts.ImportAttributes): JS.ImportAttributes {
        const openBraceIndex = node.getChildren().findIndex(n => n.kind === ts.SyntaxKind.OpenBraceToken);
        const elements = this.mapCommaSeparatedList(node.getChildren(this.sourceFile).slice(openBraceIndex, openBraceIndex + 3));
        return {
            kind: JS.Kind.ImportAttributes,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            token: ts.SyntaxKind.WithKeyword === node.token ? JS.ImportAttributes.Token.With : JS.ImportAttributes.Token.Assert,
            elements: elements,
        };
    }

    visitImportAttribute(node: ts.ImportAttribute): JS.ImportAttribute {
        return {
            kind: JS.Kind.ImportAttribute,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            name: this.visit(node.name),
            value: this.leftPadded(this.suffix(node.name), this.visit(node.value)),
        };
    }

    visitPropertyAssignment(node: ts.PropertyAssignment): JS.PropertyAssignment {
        return {
            kind: JS.Kind.PropertyAssignment,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            name: this.rightPadded(this.visit(node.name), this.suffix(node.name)),
            assigmentToken: JS.PropertyAssignment.Token.Colon,
            initializer: this.visit(node.initializer)
        };
    }

    visitShorthandPropertyAssignment(node: ts.ShorthandPropertyAssignment): JS.PropertyAssignment {
        return {
            kind: JS.Kind.PropertyAssignment,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            name: this.rightPadded(this.visit(node.name), this.suffix(node.name)),
            assigmentToken: JS.PropertyAssignment.Token.Equals,
            initializer: node.objectAssignmentInitializer && this.visit(node.objectAssignmentInitializer)
        };
    }

    visitSpreadAssignment(node: ts.SpreadAssignment): JS.PropertyAssignment {
        return {
            kind: JS.Kind.PropertyAssignment,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            name: this.rightPadded(
                produce(this.convert<Expression>(node.expression), draft => {
                    draft.markers.markers.push({
                        kind: JS.Markers.Spread,
                        id: randomId(),
                        prefix: this.prefix(node)
                    } as Spread);
                }),
                this.suffix(node.expression)
            ),
            assigmentToken: JS.PropertyAssignment.Token.Empty,
        };
    }

    visitEnumMember(node: ts.EnumMember) {
        return {
            kind: J.Kind.EnumValue,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            annotations: [],
            name: node.name ? ts.isStringLiteral(node.name) ? this.mapIdentifier(node.name, node.name.getText()) : this.convert(node.name) : this.mapIdentifier(node, ""),
            initializer: node.initializer && {
                kind: J.Kind.NewClass,
                id: randomId(),
                prefix: this.suffix(node.name),
                markers: emptyMarkers,
                new: emptySpace,
                arguments: {
                    kind: J.Kind.Container,
                    before: emptySpace,
                    elements: [this.rightPadded(this.visit(node.initializer), emptySpace)],
                    markers: emptyMarkers
                },
                constructorType: this.mapMethodType(node)
            }
        }
    }

    visitBundle(node: ts.Bundle) {
        return this.visitUnknown(node);
    }

    visitJSDocTypeExpression(node: ts.JSDocTypeExpression) {
        return this.visitUnknown(node);
    }

    visitJSDocNameReference(node: ts.JSDocNameReference) {
        return this.visitUnknown(node);
    }

    visitJSDocMemberName(node: ts.JSDocMemberName) {
        return this.visitUnknown(node);
    }

    visitJSDocAllType(node: ts.JSDocAllType) {
        return this.visitUnknown(node);
    }

    visitJSDocUnknownType(node: ts.JSDocUnknownType) {
        return this.visitUnknown(node);
    }

    visitJSDocNullableType(node: ts.JSDocNullableType) {
        return this.visitUnknown(node);
    }

    visitJSDocNonNullableType(node: ts.JSDocNonNullableType) {
        return this.visitUnknown(node);
    }

    visitJSDocOptionalType(node: ts.JSDocOptionalType) {
        return this.visitUnknown(node);
    }

    visitJSDocFunctionType(node: ts.JSDocFunctionType) {
        return this.visitUnknown(node);
    }

    visitJSDocVariadicType(node: ts.JSDocVariadicType) {
        return this.visitUnknown(node);
    }

    visitJSDocNamepathType(node: ts.JSDocNamepathType) {
        return this.visitUnknown(node);
    }

    visitJSDoc(node: ts.JSDoc) {
        return this.visitUnknown(node);
    }

    visitJSDocType(node: ts.JSDocType) {
        return this.visitUnknown(node);
    }

    visitJSDocText(node: ts.JSDocText) {
        return this.visitUnknown(node);
    }

    visitJSDocTypeLiteral(node: ts.JSDocTypeLiteral) {
        return this.visitUnknown(node);
    }

    visitJSDocSignature(node: ts.JSDocSignature) {
        return this.visitUnknown(node);
    }

    visitJSDocLink(node: ts.JSDocLink) {
        return this.visitUnknown(node);
    }

    visitJSDocLinkCode(node: ts.JSDocLinkCode) {
        return this.visitUnknown(node);
    }

    visitJSDocLinkPlain(node: ts.JSDocLinkPlain) {
        return this.visitUnknown(node);
    }

    visitJSDocTag(node: ts.JSDocTag) {
        return this.visitUnknown(node);
    }

    visitJSDocAugmentsTag(node: ts.JSDocAugmentsTag) {
        return this.visitUnknown(node);
    }

    visitJSDocImplementsTag(node: ts.JSDocImplementsTag) {
        return this.visitUnknown(node);
    }

    visitJSDocAuthorTag(node: ts.JSDocAuthorTag) {
        return this.visitUnknown(node);
    }

    visitJSDocDeprecatedTag(node: ts.JSDocDeprecatedTag) {
        return this.visitUnknown(node);
    }

    visitJSDocClassTag(node: ts.JSDocClassTag) {
        return this.visitUnknown(node);
    }

    visitJSDocPublicTag(node: ts.JSDocPublicTag) {
        return this.visitUnknown(node);
    }

    visitJSDocPrivateTag(node: ts.JSDocPrivateTag) {
        return this.visitUnknown(node);
    }

    visitJSDocProtectedTag(node: ts.JSDocProtectedTag) {
        return this.visitUnknown(node);
    }

    visitJSDocReadonlyTag(node: ts.JSDocReadonlyTag) {
        return this.visitUnknown(node);
    }

    visitJSDocOverrideTag(node: ts.JSDocOverrideTag) {
        return this.visitUnknown(node);
    }

    visitJSDocCallbackTag(node: ts.JSDocCallbackTag) {
        return this.visitUnknown(node);
    }

    visitJSDocOverloadTag(node: ts.JSDocOverloadTag) {
        return this.visitUnknown(node);
    }

    visitJSDocEnumTag(node: ts.JSDocEnumTag) {
        return this.visitUnknown(node);
    }

    visitJSDocParameterTag(node: ts.JSDocParameterTag) {
        return this.visitUnknown(node);
    }

    visitJSDocReturnTag(node: ts.JSDocReturnTag) {
        return this.visitUnknown(node);
    }

    visitJSDocThisTag(node: ts.JSDocThisTag) {
        return this.visitUnknown(node);
    }

    visitJSDocTypeTag(node: ts.JSDocTypeTag) {
        return this.visitUnknown(node);
    }

    visitJSDocTemplateTag(node: ts.JSDocTemplateTag) {
        return this.visitUnknown(node);
    }

    visitJSDocTypedefTag(node: ts.JSDocTypedefTag) {
        return this.visitUnknown(node);
    }

    visitJSDocSeeTag(node: ts.JSDocSeeTag) {
        return this.visitUnknown(node);
    }

    visitJSDocPropertyTag(node: ts.JSDocPropertyTag) {
        return this.visitUnknown(node);
    }

    visitJSDocThrowsTag(node: ts.JSDocThrowsTag) {
        return this.visitUnknown(node);
    }

    visitJSDocSatisfiesTag(node: ts.JSDocSatisfiesTag) {
        return this.visitUnknown(node);
    }

    visitJSDocImportTag(node: ts.JSDocImportTag) {
        return this.visitUnknown(node);
    }

    visitSyntaxList(node: ts.SyntaxList) {
        return this.visitUnknown(node);
    }

    visitNotEmittedStatement(node: ts.NotEmittedStatement) {
        return this.visitUnknown(node);
    }

    visitPartiallyEmittedExpression(node: ts.PartiallyEmittedExpression) {
        return this.visitUnknown(node);
    }

    visitCommaListExpression(node: ts.CommaListExpression) {
        return this.visitUnknown(node);
    }

    visitSyntheticReferenceExpression(node: ts.Node) {
        return this.visitUnknown(node);
    }

    private _seenTriviaSpans: TextSpan[] = [];

    private prefix(node: ts.Node, consume: boolean = true): J.Space {
        if (node.getFullStart() == node.getStart()) {
            return emptySpace;
        }

        if (consume) {
            const nodeStart = node.getFullStart();
            const span: TextSpan = [nodeStart, node.getStart()];
            let idx = binarySearch(this._seenTriviaSpans, span, compareTextSpans);
            if (idx >= 0)
                return emptySpace;
            idx = ~idx;
            if (idx > 0 && this._seenTriviaSpans[idx - 1][1] > span[0])
                return emptySpace;
            this._seenTriviaSpans.splice(idx, 0, span);
        }
        return prefixFromNode(node, this.sourceFile);
    }

    private suffix = (node: ts.Node, consume: boolean = true): J.Space => {
        return this.prefix(getNextSibling(node)!, consume);
    }

    private mapType(node: ts.Node): JavaType | undefined {
        return this.typeMapping.type(node);
    }

    private mapPrimitiveType(node: ts.Node): JavaType.Primitive {
        return this.typeMapping.primitiveType(node);
    }

    private mapVariableType(node: ts.NamedDeclaration): JavaType.Variable | undefined {
        return this.typeMapping.variableType(node);
    }

    private mapMethodType(node: ts.Node): JavaType.Method | undefined {
        return this.typeMapping.methodType(node);
    }

    private mapCommaSeparatedList<T extends J>(nodes: readonly ts.Node[]): J.Container<T> {
        return this.mapToContainer(nodes, this.trailingComma(nodes));
    }

    private mapTypeArguments(prefix: J.Space, nodes: readonly ts.Node[]): J.Container<Expression> {
        if (nodes.length === 0) {
            return emptyContainer();
        }

        const args = nodes.map(node =>
            this.rightPadded(
                this.visit(node),
                this.suffix(node),
                emptyMarkers
            ))
        return {
            kind: J.Kind.Container,
            before: prefix,
            elements: args,
            markers: emptyMarkers
        };
    }

    private trailingComma = (nodes: readonly ts.Node[]) => (ns: readonly ts.Node[], i: number) => {
        const last = i === ns.length - 2;
        return last ? markers({
            kind: J.Markers.TrailingComma,
            id: randomId(),
            suffix: this.prefix(nodes[2], false)
        } as TrailingComma) : emptyMarkers;
    }

    private mapToContainer<T extends J>(nodes: readonly ts.Node[], markers?: (ns: readonly ts.Node[], i: number) => Markers): J.Container<T> {
        if (nodes.length === 0) {
            return emptyContainer();
        }
        const prefix = this.prefix(nodes[0]);
        const args: J.RightPadded<T>[] = this.mapToRightPaddedList(nodes[1].getChildren(this.sourceFile), this.prefix(nodes[2]), markers);
        return {
            kind: J.Kind.Container,
            before: prefix,
            elements: args,
            markers: emptyMarkers
        };
    }

    private mapToRightPaddedList<T extends J>(elementList: readonly ts.Node[], lastAfter: J.Space, markers?: (ns: readonly ts.Node[], i: number) => Markers): J.RightPadded<T>[] {
        let childCount = elementList.length;

        const args: J.RightPadded<T>[] = [];
        if (childCount === 0) {
            args.push(this.rightPadded(
                this.newEmpty() as T,
                lastAfter,
                emptyMarkers
            ));
        } else {
            for (let i = 0; i < childCount - 1; i += 2) {
                // FIXME right padding and trailing comma
                // const last = i === childCount - 2;
                args.push(this.rightPadded(
                    this.visit(elementList[i]),
                    this.prefix(elementList[i + 1]),
                    markers ? markers(elementList, i) : emptyMarkers
                ));
            }
            if ((childCount & 1) === 1) {
                args.push(this.rightPadded(this.visit(elementList[childCount - 1]), lastAfter));
            }
        }
        return args;
    }

    private mapJsxChildren<T extends J>(elementList: readonly ts.Node[]): T[] {
        let childCount = elementList.length;

        const args: T[] = [];
        if (childCount === 0) {
            args.push(this.newEmpty() as T);
        } else {
            for (let i = 0; i < childCount; i++) {
                args.push(this.visit(elementList[i]));
            }
        }
        return args;
    }

    private mapJsxAttributes<T extends J>(elementList: readonly ts.Node[], lastAfter: J.Space, markers?: (ns: readonly ts.Node[], i: number) => Markers): J.RightPadded<T>[] {
        let childCount = elementList.length;
        if (childCount === 0) {
            return [];
        } else {
            const args: J.RightPadded<T>[] = [];
            for (let i = 0; i < childCount; i++) {
                const node = elementList[i];
                const isLast = i === childCount - 1;
                args.push(this.rightPadded(
                    this.visit(node),
                    isLast ? lastAfter : emptySpace,
                    markers ? markers(elementList, i) : emptyMarkers
                ));
            }
            return args;
        }
    }

    private mapDecorators(node: ts.ClassDeclaration | ts.FunctionDeclaration | ts.MethodDeclaration | ts.ConstructorDeclaration | ts.ParameterDeclaration | ts.PropertyDeclaration | ts.SetAccessorDeclaration | ts.GetAccessorDeclaration | ts.ClassExpression): J.Annotation[] {
        return node.modifiers?.filter(ts.isDecorator)?.map(this.convert<J.Annotation>) ?? [];
    }

    private mapTypeParametersAsContainer(node: ts.ClassDeclaration | ts.InterfaceDeclaration | ts.ClassExpression): J.Container<J.TypeParameter> | undefined {
        return node.typeParameters &&
            {
                kind: J.Kind.Container,
                before: this.prefix(this.findChildNode(node, ts.SyntaxKind.LessThanToken)!),
                elements: this.mapTypeParametersList(node.typeParameters)
                    .concat(node.typeParameters.hasTrailingComma ? this.rightPadded<J.TypeParameter>(
                        {
                            kind: J.Kind.TypeParameter,
                            id: randomId(),
                            prefix: emptySpace,
                            markers: emptyMarkers,
                            annotations: [],
                            modifiers: [],
                            name: this.newEmpty(),
                        },
                        this.prefix(this.findChildNode(node, ts.SyntaxKind.GreaterThanToken)!)) : []),
                markers: emptyMarkers
            };
    }

    private mapTypeParametersAsObject(node: ts.MethodDeclaration | ts.MethodSignature | ts.FunctionDeclaration
        | ts.CallSignatureDeclaration | ts.ConstructSignatureDeclaration | ts.FunctionExpression | ts.ArrowFunction | ts.TypeAliasDeclaration | ts.FunctionTypeNode | ts.ConstructorTypeNode): J.TypeParameters | undefined {
        const typeParameters = node.typeParameters;
        if (!typeParameters) return undefined;

        return {
            kind: J.Kind.TypeParameters,
            id: randomId(),
            prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.LessThanToken)!),
            markers: emptyMarkers,
            annotations: [],
            typeParameters: typeParameters.length == 0 ?
                [this.rightPadded({
                    kind: J.Kind.TypeParameter,
                    id: randomId(),
                    prefix: emptySpace,
                    markers: emptyMarkers,
                    annotations: [],
                    modifiers: [],
                    name: this.newEmpty(),
                }, this.prefix(this.findChildNode(node, ts.SyntaxKind.GreaterThanToken)!))]
                : typeParameters.map(tp => this.rightPadded(this.visit(tp), this.suffix(tp)))
                    .concat(typeParameters.hasTrailingComma ? this.rightPadded(
                        {
                            kind: J.Kind.TypeParameter,
                            id: randomId(),
                            prefix: emptySpace,
                            markers: emptyMarkers,
                            annotations: [],
                            modifiers: [],
                            name: this.newEmpty(),
                        },
                        this.prefix(this.findChildNode(node, ts.SyntaxKind.GreaterThanToken)!)) : []),
        };
    }

    private mapTypeParametersList(typeParamsNodeArray: ts.NodeArray<ts.TypeParameterDeclaration>): J.RightPadded<J.TypeParameter>[] {
        return typeParamsNodeArray.map(tp => this.rightPadded<J.TypeParameter>(this.visit(tp), this.suffix(tp)));
    }

    private findChildNode(node: ts.Node, kind: ts.SyntaxKind): ts.Node | undefined {
        for (let i = 0; i < node.getChildCount(this.sourceFile); i++) {
            if (node.getChildAt(i, this.sourceFile).kind === kind) {
                return node.getChildAt(i, this.sourceFile);
            }
        }
        return undefined;
    }

    private convertToken(token?: ts.Node) {
        if (token?.kind === ts.SyntaxKind.CommaToken) {
            return {
                kind: J.Markers.TrailingComma,
                id: randomId(),
                suffix: emptySpace
            };
        }
        if (token?.kind === ts.SyntaxKind.SemicolonToken) return {kind: J.Markers.Semicolon, id: randomId()};
        return undefined;
    }

    private newEmpty(prefix: J.Space = emptySpace, markers?: Markers): J.Empty {
        return {kind: J.Kind.Empty, id: randomId(), prefix: prefix, markers: markers ?? emptyMarkers};
    }

    private maybeAddOptionalMarker(t: {
        markers: Markers
    }, node: ts.MethodSignature | ts.MethodDeclaration | ts.ParameterDeclaration | ts.PropertySignature | ts.PropertyDeclaration | ts.NamedTupleMember): Markers {
        return produce(t.markers, draft => {
            if (node.questionToken) {
                draft.markers.push({
                    kind: JS.Markers.Optional,
                    id: randomId(),
                    prefix: this.suffix(node.name)
                } as Optional);
            }
        });
    }
}

function prefixFromNode(node: ts.Node, sourceFile: ts.SourceFile): J.Space {
    const comments: Comment[] = [];
    const text = sourceFile.getFullText();
    const nodeStart = node.getFullStart();

    // FIXME merge with whitespace from previous sibling
    // let previousSibling = getPreviousSibling(node);
    let leadingWhitespacePos = node.getStart();

    // Step 1: Use forEachLeadingCommentRange to extract comments
    ts.forEachLeadingCommentRange(text, nodeStart, (pos, end, kind) => {
        leadingWhitespacePos = Math.min(leadingWhitespacePos, pos);

        const isMultiline = kind === ts.SyntaxKind.MultiLineCommentTrivia;
        const commentStart = isMultiline ? pos + 2 : pos + 2;  // Skip `/*` or `//`
        const commentEnd = isMultiline ? end - 2 : end;  // Exclude closing `*/` or nothing for `//`

        // Step 2: Capture suffix (whitespace after the comment)
        let suffixEnd = end;
        while (suffixEnd < text.length && (text[suffixEnd] === ' ' || text[suffixEnd] === '\t' || text[suffixEnd] === '\n' || text[suffixEnd] === '\r')) {
            suffixEnd++;
        }

        const commentBody = text.slice(commentStart, commentEnd);  // Extract comment body
        const suffix = text.slice(end, suffixEnd);  // Extract suffix (whitespace after comment)

        comments.push({
            kind: J.Kind.TextComment,
            multiline: isMultiline,
            text: commentBody,
            suffix: suffix,
            markers: emptyMarkers
        } as TextComment);
    });

    // Step 3: Extract leading whitespace (before the first comment)
    let whitespace = '';
    if (leadingWhitespacePos > nodeStart) {
        whitespace = text.slice(nodeStart, leadingWhitespacePos);
    }

    // Step 4: Return the Space object with comments and leading whitespace
    return {kind: J.Kind.Space, comments: comments, whitespace: whitespace.length > 0 ? whitespace : ""};
}

class FlowSyntaxNotSupportedError extends SyntaxError {
    constructor(message: string = "Flow syntax is not supported") {
        super(message);
        this.name = "FlowSyntaxNotSupportedError";
    }
}

class InvalidSurrogatesNotSupportedError extends SyntaxError {
    constructor(message: string = "String literal contains invalid surrogate pairs, that is not supported") {
        super(message);
        this.name = "InvalidSurrogatesNotSupportedError";
    }
}

Parsers.registerParser("javascript", JavaScriptParser);
