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
import * as ts from 'typescript';
import {
    emptySpace,
    J,
    JavaMarkers,
    JavaType,
    NameTree,
    Comment,
    Statement,
    TrailingComma,
    emptyContainer
} from '../java';
import {Expression, JS, TypedTree, TypeTree} from '.';
import {
    emptyMarkers,
    ExecutionContext, markers,
    Markers,
    MarkersKind,
    ParseError,
    ParseExceptionResult,
    Parser,
    ParserInput,
    ParserSourceReader,
    randomId,
    SourceFile
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
} from "./parserUtils";
import {JavaScriptTypeMapping} from "./typeMapping";
import path from "node:path";
import {produce} from "immer";

export class JavaScriptParser extends Parser<JS.CompilationUnit> {

    private readonly compilerOptions: ts.CompilerOptions;
    private readonly sourceFileCache: Map<string, ts.SourceFile> = new Map();
    private oldProgram: ts.Program | undefined;

    constructor() {
        super();
        this.compilerOptions = {
            target: ts.ScriptTarget.Latest,
            module: ts.ModuleKind.CommonJS,
            allowJs: true,
            esModuleInterop: true,
            experimentalDecorators: true,
            emitDecoratorMetadata: true
        };
    }

    reset(): this {
        this.sourceFileCache.clear();
        this.oldProgram = undefined;
        return this;
    }

    parseProgramSources(program: ts.Program, relativeTo: string | undefined, ctx: ExecutionContext): Iterable<SourceFile> {
        const typeChecker = program.getTypeChecker();

        const result: SourceFile[] = [];
        for (const filePath of program.getRootFileNames()) {
            const sourceFile = program.getSourceFile(filePath)!;
            const input = new ParserInput(filePath, undefined, false, () => Buffer.from(ts.sys.readFile(filePath)!));
            try {
                const parsed = new JavaScriptParserVisitor(this, sourceFile, typeChecker).visit(sourceFile) as SourceFile;
                result.push(parsed.withSourcePath(relativeTo != undefined ? path.relative(relativeTo, input.path) : input.path));
            } catch (error) {
                result.push(ParseError.build(this, input, relativeTo, ctx, error instanceof Error ? error : new Error('Parser threw unknown error: ' + error), undefined));
            }
        }
        return result;
    }

    async parse(...sourcePaths: ParserInput[]): Promise<JS.CompilationUnit[]> {
        return sourcePaths.map(sourcePath => {
            return {
                ...new ParseJavaScriptReader(sourcePath).parse(),
                sourcePath: this.relativePath(sourcePath)
            };
        });
    }

    parse(...inputs: ParserInput[]): Promise<JS.CompilationUnit[]> {
        const inputFiles = new Map<string, ParserInput>();

        // Populate inputFiles map and remove from cache if necessary
        for (const input of inputs) {
            inputFiles.set(input.path, input);
            // Remove from cache if previously cached
            this.sourceFileCache.delete(input.path);
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
                sourceText = input.text.toString('utf8');
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
            return input
                ? input.source().toString('utf8')
                : ts.sys.readFile(fileName);
        };

        // Create a new Program, passing the oldProgram for incremental parsing
        const program = ts.createProgram([...inputFiles.keys()], this.compilerOptions, host, this.oldProgram);

        // Update the oldProgram reference
        this.oldProgram = program;

        const typeChecker = program.getTypeChecker();

        const result: SourceFile[] = [];
        for (const input of inputFiles.values()) {
            const filePath = input.path;
            const sourceFile = program.getSourceFile(filePath);
            if (!sourceFile) {
                result.push(ParseError.build(this, input, this.relativeTo, this.ctx, new Error('Parser returned undefined'), undefined));
                continue;
            }

            if (hasFlowAnnotation(sourceFile)) {
                result.push(ParseError.build(this, input, this.relativeTo, this.ctx, new FlowSyntaxNotSupportedError(`Flow syntax not supported: ${input.path}`), undefined));
                continue;
            }

            const syntaxErrors = checkSyntaxErrors(program, sourceFile);
            if (syntaxErrors.length > 0) {
                let errors = syntaxErrors.map(e => `${e[0]} [${e[1]}]`).join('; ');
                result.push(ParseError.build(this, input, this.relativeTo, this.ctx, new SyntaxError(`Compiler error(s) for ${sourceFile.fileName}: ${errors}`), undefined))
                continue;
            }

            try {
                const parsed = new JavaScriptParserVisitor(this, sourceFile, typeChecker).visit(sourceFile) as SourceFile;
                result.push(parsed.withSourcePath(this.relativeTo != undefined ? path.relative(this.relativeTo, input.path) : input.path));
            } catch (error) {
                result.push(ParseError.build(this, input, this.relativeTo, this.ctx, error instanceof Error ? error : new Error('Parser threw unknown error: ' + error), undefined));
            }
        }
        return result;
    }
}

class ParseJavaScriptReader extends ParserSourceReader {
    constructor(sourcePath: ParserInput) {
        super(sourcePath);
    }

    parse(): Omit<JS.CompilationUnit, "sourcePath"> {
        return {
            kind: JS.Kind.CompilationUnit,
            id: randomId(),
            markers: emptyMarkers,
            prefix: emptySpace,
            imports: [],
            statements: [],
            eof: emptySpace
        };
    }

    accept(path: string): boolean {
        return path.endsWith('.ts') || path.endsWith('.tsx') || path.endsWith('.js') || path.endsWith('.jsx');
    }

    sourcePathFromSourceText(prefix: string, sourceCode: string): string {
        return prefix + "/source.ts";
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
        private readonly parser: Parser<JS.CompilationUnit>,
        private readonly sourceFile: ts.SourceFile,
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
            sourcePath: this.sourceFile.fileName,
            charsetName: bomAndTextEncoding.encoding,
            charsetBomMarked: bomAndTextEncoding.hasBom,
            imports: [],
            statements: this.semicolonPaddedStatementList(node.statements),
            eof: this.prefix(node.endOfFileToken)
        };
    }

    private semicolonPaddedStatementList(statements: ts.NodeArray<ts.Statement>) {
        return [...statements].map(n => {
            const j: Statement = this.convert(n);
            if (j.kind == J.Kind.Unknown) {
                // in case of `J.Unknown` its source will already contain any `;`
                return this.rightPadded(j, emptySpace, emptyMarkers);
            }
            return this.rightPadded(j, this.semicolonPrefix(n), (n => {
                const last = n.getChildAt(n.getChildCount(this.sourceFile) - 1, this.sourceFile);
                return last?.kind == ts.SyntaxKind.SemicolonToken ? {
                    kind: MarkersKind.Markers,
                    id: randomId(),
                    markers: [{kind: "org.openrewrite.java.marker.Semicolon", id: randomId()}]
                } : emptyMarkers;
            })?.(n));
        });
    }

    visitUnknown(node: ts.Node) {
        return {
            kind: J.Kind.Unknown,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            source: {
                kind: J.Kind.UnknownSource,
                id: randomId(),
                prefix: emptySpace,
                markers: {
                    kind: MarkersKind,
                    id: randomId(),
                    markers: [
                        ParseExceptionResult.build(
                            this.parser,
                            new Error("Unsupported AST element: " + node)
                        ).withTreeType(visitMethodMap.get(node.kind)!.substring(5))
                    ]
                },
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
            keyword: kind == J.ModifierType.LanguageExtension ? node.getText() : undefined,
            type: kind,
            annotations: []
        };
    }

    private rightPadded<T extends J | boolean>(t: T, trailing: J.Space, markers?: Markers): J.RightPadded<T> {
        return {
            kind: J.Kind.JRightPadded,
            element: t,
            after: trailing,
            markers: markers ?? emptyMarkers
        };
    }

    // private rightPaddedList<N extends ts.Node, T extends J>(nodes: ts.NodeArray<N>, trailing: (node: N) => Space, markers?: (node: N) => Markers): J.RightPadded<T>[] {
    //     return nodes.map(n => this.rightPadded(this.convert(n), trailing(n), markers?.(n)));
    // }

    private rightPaddedList<N extends ts.Node, T extends J>(nodes: N[], trailing: (node: N) => Space, markers?: (node: N) => Markers): J.RightPadded<T>[] {
        return nodes.map(n => this.rightPadded(this.convert(n), trailing(n), markers?.(n)));
    }

    private rightPaddedSeparatedList<N extends ts.Node, T extends J>(nodes: N[], separator: ts.PunctuationSyntaxKind, markers?: (nodes: N[], i: number) => Markers): J.RightPadded<T>[] {
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

    private leftPadded<T extends J | J.Space | number | boolean>(before: J.Space, t: T, markers?: Markers): J.LeftPadded<T> {
        return {
            kind: J.Kind.JLeftPadded,
            before: before,
            element: t,
            markers: markers ?? emptyMarkers
        };
    }

    private leftPaddedList<N extends ts.Node, T extends J>(before: (node: N) => J.Space, nodes: ts.NodeArray<N>, markers?: (node: N) => Markers): J.LeftPadded<T>[] {
        return nodes.map(n => this.leftPadded(before(n), this.convert(n), markers?.(n)));
    }

    private semicolonPrefix = (node: ts.Node) => {
        const last = node.getChildren(this.sourceFile).slice(-1)[0];
        return last?.kind == ts.SyntaxKind.SemicolonToken ? this.prefix(last) : emptySpace;
    }

    private keywordPrefix = (token: ts.PunctuationSyntaxKind, findSibling: (node: ts.Node) => ts.Node | undefined) => (node: ts.Node): J.Space => {
        const last = findSibling(node);
        return last?.kind == token ? this.prefix(last) : emptySpace;
    }

    visitClassDeclaration(node: ts.ClassDeclaration) {
        return {
            kind: J.Kind.ClassDeclaration,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: this.mapDecorators(node),
            modifiers: this.mapModifiers(node),
            kindKeyword: {
                kind: J.Kind.ClassDeclarationKind,
                id: randomId(),
                prefix: node.modifiers ? this.suffix(node.modifiers[node.modifiers.length - 1]) : this.prefix(node),
                markers: emptyMarkers,
                annotations: [],
                classType: J.ClassType.Class
            },
            name: node.name ? this.convert(node.name) : this.mapIdentifier(node, ""),
            typeParameters: this.mapTypeParametersAsJContainer(node),
            primaryConstructor: undefined, // FIXME primary constructor
            extends: this.mapExtends(node),
            implements: this.mapImplements(node),
            permits: undefined,
            body: {
                kind: J.Kind.Block,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBraceToken)!),
                markers: emptyMarkers,
                statik: this.rightPadded(false, emptySpace, emptyMarkers),
                statements: node.members.map((ce: ts.ClassElement) => this.rightPadded(
                    this.convert(ce),
                    ce.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken ? this.prefix(ce.getLastToken()!) : emptySpace,
                    ce.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken ? {
                        kind: MarkersKind.Markers,
                        id: randomId(),
                        markers: [{kind: "org.openrewrite.java.marker.Semicolon", id: randomId()}]
                    } : emptyMarkers
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
                return this.leftPadded(this.prefix(heritageClause.getFirstToken()!), {
                    kind: JS.Kind.TypeTreeExpression,
                    id: randomId(),
                    prefix: emptySpace,
                    markers: emptyMarkers,
                    expression: expression
                });
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
                    kind: J.Kind.JContainer,
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
                    kind: J.Kind.JContainer,
                    before: this.prefix(heritageClause.getFirstToken()!),
                    elements: _implements,
                    markers: emptyMarkers
                } : undefined;
            }
        }
        return undefined;
    }

    visitNumericLiteral(node: ts.NumericLiteral) {
        return this.mapLiteral(node, node.text); // FIXME value not in AST
    }

    visitTrueKeyword(node: ts.TrueLiteral) {
        return this.mapLiteral(node, true);
    }

    visitNumberKeyword(node: ts.Node) {
        return this.mapIdentifier(node, 'number');
    }

    visitBooleanKeyword(node: ts.Node) {
        return this.mapIdentifier(node, 'boolean');
    }

    visitStringKeyword(node: ts.Node) {
        return this.mapIdentifier(node, 'string');
    }

    visitUndefinedKeyword(node: ts.Node) {
        return this.mapIdentifier(node, 'undefined');
    }

    visitAnyKeyword(node: ts.Node) {
        return this.mapIdentifier(node, 'any');
    }

    visitIntrinsicKeyword(node: ts.Node) {
        return this.mapIdentifier(node, 'intrinsic');
    }

    visitObjectKeyword(node: ts.Node) {
        return this.mapIdentifier(node, 'object');
    }

    visitUnknownKeyword(node: ts.Node) {
        return this.mapIdentifier(node, 'unknown');
    }

    visitVoidKeyword(node: ts.Node) {
        return this.mapIdentifier(node, 'void');
    }

    visitFalseKeyword(node: ts.FalseLiteral) {
        return this.mapLiteral(node, false);
    }

    visitundefinedKeyword(node: ts.undefinedLiteral) {
        return this.mapLiteral(node, undefined);
    }

    visitNeverKeyword(node: ts.Node) {
        return this.mapIdentifier(node, 'never');
    }

    visitSymbolKeyword(node: ts.Node) {
        return this.mapIdentifier(node, 'symbol');
    }

    visitBigIntKeyword(node: ts.Node) {
        return this.mapIdentifier(node, 'bigint');
    }

    private mapLiteral(node: ts.LiteralExpression | ts.TrueLiteral | ts.FalseLiteral | ts.undefinedLiteral | ts.Identifier
        | ts.TemplateHead | ts.TemplateMiddle | ts.TemplateTail, value: any): J.Literal {

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

    visitBigIntLiteral(node: ts.BigIntLiteral) {
        return this.mapLiteral(node, node.text); // FIXME value not in AST
    }

    visitStringLiteral(node: ts.StringLiteral) {
        return this.mapLiteral(node, node.text); // FIXME value not in AST
    }

    visitJsxText(node: ts.JsxText) {
        return this.visitUnknown(node);
    }

    visitRegularExpressionLiteral(node: ts.RegularExpressionLiteral) {
        return this.mapLiteral(node, node.text); // FIXME value not in AST
    }

    visitNoSubstitutionTemplateLiteral(node: ts.NoSubstitutionTemplateLiteral) {
        return this.mapLiteral(node, node.text); // FIXME value not in AST
    }

    visitTemplateHead(node: ts.TemplateHead) {
        return this.mapLiteral(node, node.text);
    }

    visitTemplateMiddle(node: ts.TemplateMiddle) {
        return this.mapLiteral(node, node.text);
    }

    visitTemplateTail(node: ts.TemplateTail) {
        return this.mapLiteral(node, node.text);
    }

    visitIdentifier(node: ts.Identifier) {
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
            type: type?.kind instanceof JavaType.Variable ? type.type : type,
            fieldType: type instanceof JavaType.Variable ? type : undefined
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

    visitComputedPropertyName(node: ts.ComputedPropertyName): J.NewArray {
        // using a `J.NewArray` is a bit of a trick; in the TS Compiler AST there is no array for this
        return {
            kind: J.Kind.NewArray,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            typeExpression: undefined,
            dimensions: [],
            initializer: {
                kind: J.Kind.JContainer,
                before: emptySpace,
                elements: [this.rightPadded(this.convert(node.expression), this.suffix(node.expression))],
                markers: emptyMarkers
            },
            type: this.mapType(node)
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
            bounds: (node.constraint || node.default) ?
                {
                    kind: J.Kind.JContainer,
                    before: this.prefix(this.findChildNode(node, ts.SyntaxKind.ExtendsKeyword) ?? this.findChildNode(node, ts.SyntaxKind.EqualsToken)!),
                    elements: [
                        node.constraint ? this.rightPadded(this.visit(node.constraint), this.suffix(node.constraint)) : this.rightPadded(this.newJEmpty(), emptySpace),
                        node.default ? this.rightPadded(this.visit(node.default), this.suffix(node.default)) : this.rightPadded(this.newJEmpty(), emptySpace)
                    ],
                    markers: emptyMarkers
                } : undefined
        };
    }

    visitParameter(node: ts.ParameterDeclaration) {
        if (node.questionToken) {
            return {
                kind: JS.Kind.JSVariableDeclarations,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                leadingAnnotations: this.mapDecorators(node),
                modifiers: this.mapModifiers(node),
                typeInfo: this.mapTypeInfo(node),
                varargs: undefined,
                variables: [
                    this.rightPadded(
                        {
                            kind: JS.Kind.JSNamedVariable,
                            id: randomId(),
                            prefix: this.prefix(node.name),
                            markers: emptyMarkers,
                            element: this.getOptionalUnary(node),
                            annotations: [],
                            initializer: node.initializer &&
                                this.leftPadded(
                                    this.prefix(
                                        node.getChildAt(
                                            node.getChildren().indexOf(node.initializer) - 1
                                        )
                                    ),
                                    this.visit(node.initializer)
                                ),
                            variableType: this.mapVariableType(node)
                        },
                        this.suffix(node.name)
                    )
                ]
            };
        }

        if (node.dotDotDotToken) {
            return {
                kind: JS.Kind.JSVariableDeclarations,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                leadingAnnotations: this.mapDecorators(node),
                modifiers: this.mapModifiers(node),
                typeExpression: this.mapTypeInfo(node),
                variables: [this.rightPadded(
                    {
                        kind: JS.Kind.JSNamedVariable,
                        id: randomId(),
                        prefix: emptySpace,
                        markers: emptyMarkers,
                        name: {
                            kind: JS.Kind.Unary,
                            id: randomId(),
                            prefix: emptySpace,
                            markers: emptyMarkers,
                            operator: this.leftPadded(this.prefix(node.dotDotDotToken), JS.Unary.Type.Spread),
                            expression: this.visit(node.name),
                            type: this.mapType(node)
                        },
                        dimensions: [],
                        initializer: node.initializer && this.leftPadded(this.prefix(node.getChildAt(node.getChildren().indexOf(node.initializer) - 1)), this.visit(node.initializer)),
                        variableType: this.mapVariableType(node)
                    },
                    this.suffix(node.name)
                )]
            };
        }

        const nameExpression = this.visit(node.name)

        if (nameExpression.kind == J.Kind.Identifier) {
            return {
                kind: J.Kind.VariableDeclarations,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                leadingAnnotations: this.mapDecorators(node),
                modifiers: this.mapModifiers(node),
                typeExpression: this.mapTypeInfo(node),
                dimensionsBeforeName: [],
                variables: [this.rightPadded(
                    {
                        kind: J.Kind.NamedVariable,
                        id: randomId(),
                        prefix: this.prefix(node.name),
                        markers: emptyMarkers,
                        name: nameExpression,
                        dimensionsAfterName: [],
                        initializer: node.initializer && this.leftPadded(this.prefix(node.getChildAt(node.getChildren().indexOf(node.initializer) - 1)), this.visit(node.initializer)),
                        variableType: this.mapVariableType(node)
                    },
                    this.suffix(node.name)
                )]
            };
        }

        return {
            kind: JS.Kind.JSVariableDeclarations,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: this.mapDecorators(node),
            modifiers: this.mapModifiers(node),
            typeExpression: this.mapTypeInfo(node),
            variables: [this.rightPadded(
                {
                    kind: JS.Kind.JSNamedVariable,
                    id: randomId(),
                    prefix: this.prefix(node.name),
                    markers: emptyMarkers,
                    name: nameExpression,
                    dimensionsAfterName: [],
                    initializer: node.initializer ? this.leftPadded(this.prefix(node.getChildAt(node.getChildren().indexOf(node.initializer) - 1)), this.visit(node.initializer)) : undefined,
                    variableType: this.mapVariableType(node)
                },
                this.suffix(node.name)
            )]
        };
    }

    visitDecorator(node: ts.Decorator) {
        let annotationType: NameTree;
        let _arguments: J.Container<Expression> | undefined = undefined;

        if (ts.isCallExpression(node.expression)) {
            annotationType = ({
                kind: JS.Kind.ExpressionWithTypeArguments,
                id: randomId(),
                prefix: emptySpace,
                markers: emptyMarkers,
                clazz: this.convert(node.expression.expression),
                typeArguments: node.expression.typeArguments && this.mapTypeArguments(this.suffix(node.expression.expression), node.expression.typeArguments)
            });
            _arguments = this.mapCommaSeparatedList(node.expression.getChildren(this.sourceFile).slice(-3))
        } else if (ts.isIdentifier(node.expression)) {
            annotationType = this.convert(node.expression);
        } else if (ts.isPropertyAccessExpression(node.expression)) {
            annotationType = this.convert(node.expression);
        } else if (ts.isParenthesizedExpression(node.expression)) {
            annotationType = ({
                kind: JS.Kind.TypeTreeExpression,
                id: randomId(),
                prefix: this.prefix(node.expression),
                markers: emptyMarkers,
                expression: this.convert(node.expression)
            });
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
        const prefix = this.prefix(node);

        if (node.questionToken) {
            return {
                kind: JS.Kind.JSVariableDeclarations,
                id: randomId(),
                prefix: prefix,
                markers: emptyMarkers,
                leadingAnnotations: [], // no decorators allowed
                modifiers: this.mapModifiers(node),
                typeExpression: this.mapTypeInfo(node),
                variables: [this.rightPadded(
                    {
                        kind: JS.Kind.JSNamedVariable,
                        id: randomId(),
                        prefix: this.prefix(node.name),
                        markers: emptyMarkers,
                        name: this.getOptionalUnary(node),
                        dimensionsAfterName: [],
                        variableType: this.mapVariableType(node)
                    },
                    emptySpace
                )]
            };
        }

        const nameExpression = this.visit(node.name)

        if (nameExpression.kind == J.Kind.Identifier) {
            return {
                kind: J.Kind.VariableDeclarations,
                id: randomId(),
                prefix: prefix,
                markers: emptyMarkers,
                leadingAnnotations: [], // no decorators allowed
                modifiers: this.mapModifiers(node),
                typeExpression: this.mapTypeInfo(node),
                dimensionsBeforeName: [],
                variables: [this.rightPadded(
                    {
                        kind: J.Kind.NamedVariable,
                        id: randomId(),
                        prefix: this.prefix(node.name),
                        markers: emptyMarkers,
                        name: nameExpression,
                        dimensionsAfterName: [],
                        variableType: this.mapVariableType(node)
                    },
                    emptySpace
                )]
            };
        } else {
            return {
                kind: JS.Kind.JSVariableDeclarations,
                id: randomId(),
                prefix: prefix,
                markers: emptyMarkers,
                leadingAnnotations: [], // no decorators allowed
                modifiers: this.mapModifiers(node),
                typeExpression: this.mapTypeInfo(node),
                variables: [this.rightPadded(
                    {
                        kind: JS.Kind.JSNamedVariable,
                        id: randomId(),
                        prefix: this.prefix(node.name),
                        markers: emptyMarkers,
                        name: nameExpression,
                        dimensionsAfterName: [],
                        variableType: this.mapVariableType(node)
                    },
                    emptySpace
                )]
            };
        }
    }

    visitPropertyDeclaration(node: ts.PropertyDeclaration) {
        const prefix = this.prefix(node);

        if (node.questionToken) {
            return {
                kind: JS.Kind.JSVariableDeclarations,
                id: randomId(),
                prefix: prefix,
                markers: emptyMarkers,
                leadingAnnotations: this.mapDecorators(node),
                modifiers: this.mapModifiers(node),
                typeExpression: this.mapTypeInfo(node),
                variables: [this.rightPadded(
                    {
                        kind: JS.Kind.JSNamedVariable,
                        id: randomId(),
                        prefix: this.prefix(node.name),
                        markers: emptyMarkers,
                        name: this.getOptionalUnary(node),
                        dimensionsAfterName: [],
                        initializer: node.initializer && this.leftPadded(this.prefix(node.getChildAt(node.getChildren().indexOf(node.initializer) - 1)), this.visit(node.initializer)),
                        variableType: this.mapVariableType(node)
                    },
                    emptySpace
                )]
            };
        }

        if (node.exclamationToken) {
            return {
                kind: JS.Kind.JSVariableDeclarations,
                id: randomId(),
                prefix: prefix,
                markers: emptyMarkers,
                leadingAnnotations: this.mapDecorators(node),
                modifiers: this.mapModifiers(node),
                typeExpression: this.mapTypeInfo(node),
                variables: [this.rightPadded(
                    {
                        kind: JS.Kind.JSNamedVariable,
                        id: randomId(),
                        prefix: this.prefix(node.name),
                        markers: emptyMarkers,
                        name: {
                            kind: JS.Kind.Unary,
                            id: randomId(),
                            prefix: emptySpace,
                            markers: emptyMarkers,
                            operator: this.leftPadded(this.suffix(node.name), JS.Unary.Type.Exclamation),
                            expression: this.visit(node.name),
                            type: this.mapType(node)
                        },
                        dimensionsAfterName: [],
                        initializer: node.initializer && this.leftPadded(this.prefix(node.getChildAt(node.getChildren().indexOf(node.initializer) - 1)), this.visit(node.initializer)),
                        variableType: this.mapVariableType(node)
                    },
                    emptySpace
                )]
            };
        }

        const nameExpression = this.visit(node.name)

        if (nameExpression.kind == J.Kind.Identifier) {
            return {
                kind: J.Kind.VariableDeclarations,
                id: randomId(),
                prefix: prefix,
                markers: emptyMarkers,
                leadingAnnotations: this.mapDecorators(node),
                modifiers: this.mapModifiers(node),
                typeExpression: this.mapTypeInfo(node),
                dimensionsBeforeName: [],
                variables: [this.rightPadded(
                    {
                        kind: J.Kind.NamedVariable,
                        id: randomId(),
                        prefix: this.prefix(node.name),
                        markers: emptyMarkers,
                        name: nameExpression,
                        dimensionsAfterName: [],
                        initializer: node.initializer && this.leftPadded(this.prefix(node.getChildAt(node.getChildren().indexOf(node.initializer) - 1)), this.visit(node.initializer)),
                        variableType: this.mapVariableType(node)
                    },
                    this.suffix(node.name)
                )]
            };
        }

        return {
            kind: JS.Kind.JSVariableDeclarations,
            id: randomId(),
            prefix: prefix,
            markers: emptyMarkers,
            leadingAnnotations: this.mapDecorators(node),
            modifiers: this.mapModifiers(node),
            typeExpression: this.mapTypeInfo(node),
            variables: [this.rightPadded(
                {
                    kind: JS.Kind.JSNamedVariable,
                    id: randomId(),
                    prefix: this.prefix(node.name),
                    markers: emptyMarkers,
                    name: nameExpression,
                    dimensionsAfterName: [],
                    initializer: node.initializer && this.leftPadded(this.prefix(node.getChildAt(node.getChildren().indexOf(node.initializer) - 1)), this.visit(node.initializer)),
                    variableType: this.mapVariableType(node)
                },
                emptySpace
            )]
        };
    }

    visitMethodSignature(node: ts.MethodSignature) {
        const prefix = this.prefix(node);

        if (node.questionToken) {
            return {
                kind: JS.Kind.JSMethodDeclaration,
                id: randomId(),
                prefix: prefix,
                markers: emptyMarkers,
                leadingAnnotations: [], // no decorators allowed
                modifiers: [], // no modifiers allowed
                typeParameters: this.mapTypeParametersAsObject(node),
                returnTypeExpression: this.mapTypeInfo(node),
                name: this.getOptionalUnary(node),
                parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
                methodType: this.mapMethodType(node)
            };
        }

        if (ts.isComputedPropertyName(node.name)) {
            return {
                kind: JS.Kind.JSMethodDeclaration,
                id: randomId(),
                prefix: prefix,
                markers: emptyMarkers,
                leadingAnnotations: [], // no decorators allowed
                modifiers: [], // no modifiers allowed
                typeParameters: this.mapTypeParametersAsObject(node),
                returnTypeExpression: this.mapTypeInfo(node),
                name: this.convert(node.name),
                parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
                methodType: this.mapMethodType(node)
            };
        }

        const name: J.Identifier = !node.name
            ? this.mapIdentifier(node, "")
            : ts.isStringLiteral(node.name)
                ? this.mapIdentifier(node.name, node.name.getText())
                : this.visit(node.name);

        return {
            kind: J.Kind.MethodDeclaration,
            id: randomId(),
            prefix: prefix,
            markers: emptyMarkers,
            decorators: [], // no decorators allowed
            modifiers: [], // no modifiers allowed
            typeParameters: this.mapTypeParametersAsObject(node),
            returnTypeExpression: this.mapTypeInfo(node),
            nameAnnotations: [],
            name: name,
            parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
            methodType: this.mapMethodType(node)
        };
    }

    visitMethodDeclaration(node: ts.MethodDeclaration) {
        const prefix = this.prefix(node);

        if (node.questionToken || node.asteriskToken) {
            let methodName = node.questionToken ? this.getOptionalUnary(node) : this.visit(node.name);

            if (node.asteriskToken) {
                methodName = {
                    kind: JS.Kind.Unary,
                    id: randomId(),
                    prefix: this.prefix(node.asteriskToken),
                    markers: emptyMarkers,
                    operator: this.leftPadded(this.prefix(node.name), JS.Unary.Type.Asterisk),
                    expression: methodName,
                    type: this.mapType(node)
                }
            }

            return {
                kind: JS.Kind.JSMethodDeclaration,
                id: randomId(),
                prefix: prefix,
                markers: emptyMarkers,
                leadingAnnotations: this.mapDecorators(node),
                modifiers: this.mapModifiers(node),
                typeParameters: this.mapTypeParametersAsObject(node),
                returnTypeExpression: this.mapTypeInfo(node),
                name: methodName,
                parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
                body: node.body && this.convert<J.Block>(node.body),
                methodType: this.mapMethodType(node)
            };
        }

        const name = node.name ? this.visit(node.name) : this.mapIdentifier(node, "");
        if (!(name.kind == J.Kind.Identifier)) {
            return {
                kind: JS.Kind.JSMethodDeclaration,
                id: randomId(),
                prefix: prefix,
                markers: emptyMarkers,
                leadingAnnotations: this.mapDecorators(node),
                modifiers: this.mapModifiers(node),
                typeParameters: this.mapTypeParametersAsObject(node),
                returnTypeExpression: this.mapTypeInfo(node),
                name: name,
                parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
                body: node.body && this.convert<J.Block>(node.body),
                methodType: this.mapMethodType(node)
            };
        }

        return {
            kind: J.Kind.MethodDeclaration,
            id: randomId(),
            prefix: prefix,
            markers: emptyMarkers,
            leadingAnnotations: this.mapDecorators(node),
            modifiers: this.mapModifiers(node),
            typeParameters: this.mapTypeParametersAsObject(node),
            returnTypeExpression: this.mapTypeInfo(node),
            nameAnnotations: [],
            name: name,
            parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
            body: node.body && this.convert<J.Block>(node.body),
            methodType: this.mapMethodType(node)
        };
    }

    private mapTypeInfo(node: ts.MethodDeclaration | ts.PropertyDeclaration | ts.VariableDeclaration | ts.ParameterDeclaration
        | ts.PropertySignature | ts.MethodSignature | ts.ArrowFunction | ts.CallSignatureDeclaration | ts.GetAccessorDeclaration
        | ts.FunctionDeclaration | ts.ConstructSignatureDeclaration | ts.FunctionExpression | ts.NamedTupleMember): TypeTree | undefined {
        return node.type && {
            kind: JS.Kind.TypeInfo,
            id: randomId(),
            prefix: this.prefix(node.getChildAt(node.getChildren().indexOf(node.type) - 1)),
            markers: emptyMarkers,
            typeIdentifier: this.visit(node.type)
        };
    }

    visitClassStaticBlockDeclaration(node: ts.ClassStaticBlockDeclaration) {
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
                    kind: JavaMarkers.Semicolon,
                    id: randomId()
                }) : emptyMarkers
            )),
            end: this.prefix(node.getLastToken()!)
        };
    }

    visitConstructor(node: ts.ConstructorDeclaration) {
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

    visitGetAccessor(node: ts.GetAccessorDeclaration) {
        const name = this.visit(node.name);
        if (!(name.kind == J.Kind.Identifier)) {
            return {
                kind: JS.Kind.JSMethodDeclaration,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                leadingAnnotations: this.mapDecorators(node),
                modifiers: this.mapModifiers(node),
                returnTypeExpression: this.mapTypeInfo(node),
                name: name,
                parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
                body: node.body && this.convert<J.Block>(node.body),
                methodType: this.mapMethodType(node)
            };
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
        };
    }

    visitSetAccessor(node: ts.SetAccessorDeclaration) {
        const name = this.visit(node.name);
        if (!(name.kind == J.Kind.Identifier)) {
            return {
                kind: JS.Kind.JSMethodDeclaration,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                leadingAnnotations: this.mapDecorators(node),
                modifiers: this.mapModifiers(node),
                name: name,
                parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
                body: node.body && this.convert<J.Block>(node.body),
                methodType: this.mapMethodType(node)
            };
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
        };
    }

    visitCallSignature(node: ts.CallSignatureDeclaration) {
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
                prefix: emptySpace/* this.prefix(node.getChildren().find(n => n.kind == ts.SyntaxKind.OpenBraceToken)!) */,
                markers: emptyMarkers,
                annotations: [], // FIXME decorators
                simpleName: "",
            },
            parameters: this.mapCommaSeparatedList(this.getParameterListNodes(node)),
            methodType: this.mapMethodType(node)
        };
    }

    visitConstructSignature(node: ts.ConstructSignatureDeclaration) {
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

    visitIndexSignature(node: ts.IndexSignatureDeclaration) {
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

    visitTypePredicate(node: ts.TypePredicateNode) {
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

    visitTypeReference(node: ts.TypeReferenceNode) {
        if (node.typeArguments) {
            return {
                kind: J.Kind.ParameterizedType,
                id: randomId(),
                prefix: this.prefix(node),
                markers: emptyMarkers,
                clazz: this.visit(node.typeName),
                typeParameters: this.mapTypeArguments(this.suffix(node.typeName), node.typeArguments),
                type: this.mapType(node)
            }
        }
        return this.visit(node.typeName);
    }

    visitFunctionType(node: ts.FunctionTypeNode) {
        return JS.newFunctionType(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            [],
            this.leftPadded(emptySpace, false),
            this.mapTypeParametersAsObject(node),
            {
                kind: J.Kind.JContainer,
                before: this.prefix(node.getChildAt(node.getChildren().findIndex(n => n.pos === node.parameters.pos) - 1)),
                elements: node.parameters.length == 0 ?
                    [this.rightPadded(this.newJEmpty(), this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseParenToken)!))]
                    : node.parameters.map(p => this.rightPadded(this.visit(p), this.suffix(p)))
                        .concat(node.parameters.hasTrailingComma ? this.rightPadded(this.newJEmpty(), this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseParenToken)!)) : []),
                markers: emptyMarkers
            },
            this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.EqualsGreaterThanToken)!), this.convert(node.type)),
            undefined);
    }

    visitConstructorType(node: ts.ConstructorTypeNode) {
        return JS.newFunctionType(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.mapModifiers(node),
            this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.NewKeyword)!), true),
            this.mapTypeParametersAsObject(node),
            {
                kind: J.Kind.JContainer,
                before: this.prefix(node.getChildAt(node.getChildren().findIndex(n => n.pos === node.parameters.pos) - 1)),
                elements: node.parameters.length == 0 ?
                    [this.rightPadded(this.newJEmpty(), this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseParenToken)!))]
                    : node.parameters.map(p => this.rightPadded(this.visit(p), this.suffix(p)))
                        .concat(node.parameters.hasTrailingComma ? this.rightPadded(this.newJEmpty(), this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseParenToken)!)) : []),
                markers: emptyMarkers
            },
            this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.EqualsGreaterThanToken)!), this.convert(node.type)),
            undefined);
    }

    visitTypeQuery(node: ts.TypeQueryNode) {
        return JS.newTypeQuery(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.convert(node.exprName),
            node.typeArguments ? this.mapTypeArguments(this.suffix(node.exprName), node.typeArguments) : undefined,
            this.mapType(node)
        )
    }

    visitTypeLiteral(node: ts.TypeLiteralNode) {
        return JS.newTypeLiteral(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            J.newBlock(
                randomId(),
                this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBraceToken)!),
                emptyMarkers,
                this.rightPadded(false, emptySpace),
                node.members.map(te => ({
                    kind: J.Kind.JRightPadded,
                    element: this.convert(te),
                    after: (te.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken) || (te.getLastToken()?.kind === ts.SyntaxKind.CommaToken) ? this.prefix(te.getLastToken()!) : emptySpace,
                    markers: (te.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken) || (te.getLastToken()?.kind === ts.SyntaxKind.CommaToken) ? markers(this.convertToken(te.getLastToken())!) : emptyMarkers
                })),
                this.prefix(node.getLastToken()!)
            ),
            this.mapType(node)
        );
    }

    visitArrayType(node: ts.ArrayTypeNode) {
        return J.newArrayType(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.convert(node.elementType),
            undefined,
            this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBracketToken)!), this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseBracketToken)!)),
            this.mapType(node)!
        )
    }

    visitTupleType(node: ts.TupleTypeNode) {
        return JS.newTuple(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            {
                kind: J.Kind.JContainer,
                before: emptySpace,
                elements: node.elements.length > 0 ?
                    node.elements.map(p => this.rightPadded(this.convert(p), this.suffix(p)))
                        .concat(node.elements.hasTrailingComma ? this.rightPadded(this.newJEmpty(), this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseBracketToken)!)) : [])
                    : [this.rightPadded(this.newJEmpty(this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseBracketToken)!)), emptySpace)],
                markers: emptyMarkers
            },
            this.mapType(node)
        );
    }

    visitOptionalType(node: ts.OptionalTypeNode) {
        return JS.newUnary(
            randomId(),
            emptySpace,
            emptyMarkers,
            this.leftPadded(this.suffix(node.type), JS.Unary.Type.Optional),
            this.visit(node.type),
            this.mapType(node)
        );
    }

    visitRestType(node: ts.RestTypeNode) {
        return JS.newUnary(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.leftPadded(emptySpace, JS.Unary.Type.Spread),
            this.convert(node.type),
            this.mapType(node)
        );
    }

    visitUnionType(node: ts.UnionTypeNode) {
        const initialBar = getPreviousSibling(node.types[0]);
        return JS.newUnion(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            [
                ...(initialBar?.kind == ts.SyntaxKind.BarToken ? [this.rightPadded<Expression>(this.newJEmpty(), this.prefix(initialBar))] : []),
                ...this.rightPaddedList<ts.Node, Expression>([...node.types], (n) => this.keywordPrefix(ts.SyntaxKind.BarToken, getNextSibling)(n))
            ],
            this.mapType(node),
        );
    }

    visitIntersectionType(node: ts.IntersectionTypeNode) {
        const initialAmpersand = getPreviousSibling(node.types[0]);
        return JS.newIntersection(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            [
                ...(initialAmpersand?.kind == ts.SyntaxKind.AmpersandToken ? [this.rightPadded<Expression>(this.newJEmpty(), this.prefix(initialAmpersand))] : []),
                ...this.rightPaddedList<ts.Node, Expression>([...node.types], (n) => this.keywordPrefix(ts.SyntaxKind.AmpersandToken, getNextSibling)(n))
            ],
            this.mapType(node),
        );
    }

    visitConditionalType(node: ts.ConditionalTypeNode) {
        return JS.newConditionalType(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.visit(node.checkType),
            {
                kind: J.Kind.JContainer,
                before: this.prefix(this.findChildNode(node, ts.SyntaxKind.ExtendsKeyword)!),
                elements: [this.rightPadded(
                    J.newTernary(
                        randomId(),
                        emptySpace,
                        emptyMarkers,
                        this.convert(node.extendsType),
                        this.leftPadded(this.suffix(node.extendsType), this.convert(node.trueType)),
                        this.leftPadded(this.suffix(node.trueType), this.convert(node.falseType)),
                        this.mapType(node)),
                    emptySpace
                )],
                markers: emptyMarkers
            },
            this.mapType(node)
        );
    }

    visitInferType(node: ts.InferTypeNode) {
        return JS.newInferType(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.leftPadded(emptySpace, this.convert(node.typeParameter)),
            this.mapType(node)
        );
    }

    visitParenthesizedType(node: ts.ParenthesizedTypeNode) {
        return J.newParenthesizedTypeTree(
            randomId(),
            emptySpace,
            emptyMarkers,
            [],
            J.newParentheses(
                randomId(),
                this.prefix(node),
                emptyMarkers,
                this.rightPadded(this.convert(node.type), this.prefix(node.getLastToken()!))
            )
        );
    }

    visitThisType(node: ts.ThisTypeNode) {
        return this.mapIdentifier(node, 'this');
    }

    visitTypeOperator(node: ts.TypeOperatorNode) {
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

        return JS.newTypeOperator(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            mapTypeOperator(node.operator)!,
            this.leftPadded(this.prefix(node.type), this.visit(node.type))
        );
    }

    visitIndexedAccessType(node: ts.IndexedAccessTypeNode) {
        return JS.newIndexedAccessType(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.convert(node.objectType),
            JS.newIndexedAccessType.IndexType(
                randomId(),
                this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBracketToken)!),
                emptyMarkers,
                this.rightPadded(this.convert(node.indexType), this.suffix(node.indexType)),
                this.mapType(node.indexType)
            ),
            this.mapType(node)
        );
    }

    visitMappedType(node: ts.MappedTypeNode) {
        function hasPrefixToken(readonlyToken?: ts.ReadonlyKeyword | ts.PlusToken | ts.MinusToken): boolean {
            return !!(readonlyToken && (readonlyToken.kind == ts.SyntaxKind.PlusToken || readonlyToken.kind == ts.SyntaxKind.MinusToken));
        }

        function hasSuffixToken(questionToken?: ts.QuestionToken | ts.PlusToken | ts.MinusToken): boolean {
            return !!(questionToken && (questionToken.kind == ts.SyntaxKind.PlusToken || questionToken.kind == ts.SyntaxKind.MinusToken));
        }

        return JS.newMappedType(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            hasPrefixToken(node.readonlyToken) ? this.leftPadded(this.prefix(node.readonlyToken!),
                J.newLiteral(
                    randomId(),
                    this.prefix(node.readonlyToken!),
                    emptyMarkers,
                    undefined,
                    node.readonlyToken!.getText(),
                    undefined,
                    this.mapPrimitiveType(node.readonlyToken!)
                )) : undefined,
            node.readonlyToken ? this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.ReadonlyKeyword)!), true) : this.leftPadded(emptySpace, false),
            JS.newMappedTypeKeysRemapping(
                randomId(),
                this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBracketToken)!),
                emptyMarkers,
                this.rightPadded(
                    JS.newMappedType.MappedTypeParameter(
                        randomId(),
                        this.prefix(node.typeParameter),
                        emptyMarkers,
                        this.visit(node.typeParameter.name),
                        this.leftPadded(this.suffix(node.typeParameter.name), this.visit(node.typeParameter.constraint!))
                    ),
                    this.suffix(node.typeParameter)),
                node.nameType ? this.rightPadded(this.visit(node.nameType), this.suffix(node.nameType)) : undefined,
            ),
            hasSuffixToken(node.questionToken) ? this.leftPadded(this.prefix(node.questionToken!),
                J.newLiteral(
                    randomId(),
                    this.prefix(node.questionToken!),
                    emptyMarkers,
                    undefined,
                    node.questionToken!.getText(),
                    undefined,
                    this.mapPrimitiveType(node.questionToken!)
                )
            ) : undefined,
            node.questionToken ? this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.QuestionToken)!), true) : this.leftPadded(emptySpace, false),
            node.type ? {
                kind: J.Kind.JContainer,
                before: this.prefix(this.findChildNode(node, ts.SyntaxKind.ColonToken)!),
                elements: [this.rightPadded(this.visit(node.type), this.suffix(node.type)),
                    this.findChildNode(node, ts.SyntaxKind.SemicolonToken) ?
                        this.rightPadded(this.newJEmpty(emptySpace, markers({
                            kind: JavaMarkers.Semicolon,
                            id: randomId()
                        })), this.prefix(node.getLastToken()!))
                        : this.rightPadded(this.newJEmpty(), this.prefix(node.getLastToken()!))
                ],
                markers: emptyMarkers
            } : {
                kind: J.Kind.JContainer,
                before: emptySpace,
                elements: [this.findChildNode(node, ts.SyntaxKind.SemicolonToken) ?
                    this.rightPadded(this.newJEmpty(this.prefix(this.findChildNode(node, ts.SyntaxKind.SemicolonToken)!), markers({
                        kind: JavaMarkers.Semicolon,
                        id: randomId()
                    })), this.prefix(node.getLastToken()!))
                    : this.rightPadded(this.newJEmpty(), this.prefix(node.getLastToken()!))
                ],
                markers: emptyMarkers
            },
            this.mapType(node)
        );
    }

    visitLiteralType(node: ts.LiteralTypeNode) {
        return JS.newLiteralType(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.visit(node.literal),
            this.mapType(node)!
        );
    }

    visitNamedTupleMember(node: ts.NamedTupleMember) {
        if (node.questionToken) {
            return JS.newJSVariableDeclarations(
                randomId(),
                this.prefix(node),
                emptyMarkers,
                [],
                [],
                this.mapTypeInfo(node),
                undefined,
                [this.rightPadded(
                    JS.newJSVariableDeclarations.JSNamedVariable(
                        randomId(),
                        this.prefix(node.name),
                        emptyMarkers,
                        this.getOptionalUnary(node),
                        [],
                        undefined,
                        this.mapVariableType(node)
                    ),
                    this.suffix(node.name)
                )]
            );
        }

        if (node.dotDotDotToken) {
            return JS.newJSVariableDeclarations(
                randomId(),
                this.prefix(node),
                emptyMarkers,
                [],
                [],
                this.mapTypeInfo(node),
                undefined,
                [this.rightPadded(
                    JS.newJSVariableDeclarations.JSNamedVariable(
                        randomId(),
                        emptySpace,
                        emptyMarkers,
                        JS.newUnary(
                            randomId(),
                            emptySpace,
                            emptyMarkers,
                            this.leftPadded(emptySpace, JS.Unary.Type.Spread),
                            this.visit(node.name),
                            this.mapType(node)
                        ),
                        [],
                        undefined,
                        this.mapVariableType(node)
                    ),
                    this.suffix(node.name)
                )]
            );
        }

        return J.newVariableDeclarations(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            [],
            [],
            this.mapTypeInfo(node),
            undefined,
            [this.rightPadded(
                J.newNamedVariable(
                    randomId(),
                    this.prefix(node.name),
                    emptyMarkers,
                    this.visit(node.name),
                    [],
                    undefined,
                    this.mapVariableType(node)
                ),
                this.suffix(node.name)
            )]
        );
    }

    visitTemplateLiteralType(node: ts.TemplateLiteralTypeNode) {
        return JS.newTemplateExpression(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.visit(node.head),
            node.templateSpans.map(s => this.rightPadded(this.visit(s), this.suffix(s))),
            this.mapType(node)
        )
    }

    visitTemplateLiteralTypeSpan(node: ts.TemplateLiteralTypeSpan) {
        return JS.newTemplateExpression.TemplateSpan(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.convert(node.type),
            this.visit(node.literal)
        )
    }

    visitImportType(node: ts.ImportTypeNode) {
        let importTypeAttributes = undefined;
        if (node.attributes) {
            const openBraceIndex = node.attributes.getChildren().findIndex(n => n.kind === ts.SyntaxKind.OpenBraceToken);
            const attributes = this.mapCommaSeparatedList<JS.ImportAttribute>(node.attributes.getChildren(this.sourceFile).slice(openBraceIndex, openBraceIndex + 3));
            importTypeAttributes = JS.newImportTypeAttributes(
                randomId(),
                this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBraceToken)!),
                emptyMarkers,
                this.rightPadded(
                    this.mapIdentifier(this.findChildNode(node, node.attributes.token)!,
                        ts.SyntaxKind.WithKeyword === node.attributes.token ? "with" : "assert"),
                    this.prefix(this.findChildNode(node, ts.SyntaxKind.ColonToken)!)),
                attributes,
                this.suffix(node.attributes)
            )
        }

        return JS.newImportType(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            node.isTypeOf ? this.rightPadded(true, this.suffix(this.findChildNode(node, ts.SyntaxKind.TypeOfKeyword)!)) : this.rightPadded(false, emptySpace),
            {
                kind: J.Kind.JContainer,
                before: this.suffix(this.findChildNode(node, ts.SyntaxKind.ImportKeyword)!),
                elements: [this.rightPadded(this.visit(node.argument), this.suffix(node.argument))].concat(importTypeAttributes ? [this.rightPadded(importTypeAttributes, this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseParenToken)!))] : []),
                markers: emptyMarkers
            },
            node.qualifier ? this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.DotToken)!), this.visit(node.qualifier)) : undefined,
            node.typeArguments ? this.mapTypeArguments(this.prefix(this.findChildNode(node, ts.SyntaxKind.LessThanToken)!), node.typeArguments) : undefined,
            this.mapType(node)
        );
    }

    visitObjectBindingPattern(node: ts.ObjectBindingPattern) {
        return JS.newObjectBindingDeclarations(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            [],
            [],
            undefined,
            this.mapCommaSeparatedList(node.getChildren(this.sourceFile)),
            undefined
        );
    }

    visitArrayBindingPattern(node: ts.ArrayBindingPattern) {
        return JS.newArrayBindingPattern(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.mapCommaSeparatedList(node.getChildren(this.sourceFile)),
            this.mapType(node)
        );
    }

    visitBindingElement(node: ts.BindingElement) {
        return JS.newBindingElement(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            node.propertyName ? this.rightPadded(this.convert<J.Identifier>(node.propertyName), this.suffix(node.propertyName)) : undefined,
            node.dotDotDotToken ? JS.newUnary(
                randomId(),
                this.prefix(node.dotDotDotToken),
                emptyMarkers,
                this.leftPadded(emptySpace, JS.Unary.Type.Spread),
                this.convert<Expression>(node.name),
                undefined
            ) : this.convert<TypedTree>(node.name),
            node.initializer ? this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.EqualsToken)!), this.convert<Expression>(node.initializer)) : undefined,
            this.mapVariableType(node),
        );
    }

    visitArrayLiteralExpression(node: ts.ArrayLiteralExpression) {
        return J.newNewArray(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            undefined,
            [],
            this.mapCommaSeparatedList(node.getChildren(this.sourceFile)),
            this.mapType(node)
        );
    }

    visitObjectLiteralExpression(node: ts.ObjectLiteralExpression) {
        return J.newNewClass(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            undefined,
            emptySpace,
            undefined,
            emptyContainer(),
            this.convertPropertyAssignments(node.getChildren(this.sourceFile).slice(-3)),
            this.mapMethodType(node)
        );
    }

    private convertPropertyAssignments(nodes: ts.Node[]): J.Block {
        const prefix = this.prefix(nodes[0]);
        let statementList = nodes[1] as ts.SyntaxList;

        const statements: J.RightPadded<Statement>[] = this.rightPaddedSeparatedList(
            [...statementList.getChildren(this.sourceFile)],
            ts.SyntaxKind.CommaToken,
            (nodes, i) => i == nodes.length - 2 && nodes[i + 1].kind == ts.SyntaxKind.CommaToken ? markers({
                kind: JavaMarkers.TrailingComma,
                id: randomId(),
                suffix: this.prefix(nodes[i + 1])
            } as TrailingComma) : emptyMarkers
        );

        return J.newBlock(
            randomId(),
            prefix,
            emptyMarkers,
            this.rightPadded(false, emptySpace),
            statements,
            this.prefix(nodes[nodes.length - 1])
        );
    }

    visitPropertyAccessExpression(node: ts.PropertyAccessExpression) {
        return J.newFieldAccess(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            node.questionDotToken ?
                JS.newUnary(
                    randomId(),
                    emptySpace,
                    emptyMarkers,
                    this.leftPadded(this.suffix(node.expression), JS.Unary.Type.QuestionDot),
                    this.visit(node.expression),
                    this.mapType(node)
                ) : this.convert(node.expression),
            this.leftPadded(this.prefix(node.getChildAt(1, this.sourceFile)), this.convert(node.name)),
            this.mapType(node)
        );
    }

    visitElementAccessExpression(node: ts.ElementAccessExpression) {
        return J.newArrayAccess(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            node.questionDotToken ?
                JS.newUnary(
                    randomId(),
                    emptySpace,
                    emptyMarkers,
                    this.leftPadded(this.suffix(node.expression), JS.Unary.Type.QuestionDotWithDot),
                    this.visit(node.expression),
                    this.mapType(node)
                ) :
                this.convert(node.expression),
            J.newArrayDimension(
                randomId(),
                this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBracketToken)!),
                emptyMarkers,
                this.rightPadded(this.convert(node.argumentExpression), this.suffix(node.argumentExpression))
            ),
            this.mapType(node)
        );
    }

    visitCallExpression(node: ts.CallExpression) {
        const prefix = this.prefix(node);
        const typeArguments = node.typeArguments ? this.mapTypeArguments(this.prefix(this.findChildNode(node, ts.SyntaxKind.LessThanToken)!), node.typeArguments) : undefined;

        let select: J.RightPadded<Expression> | undefined;
        let name: J.Identifier = J.newIdentifier(randomId(), emptySpace, emptyMarkers, [], "", undefined, undefined);

        if (ts.isIdentifier(node.expression) && !node.questionDotToken) {
            select = undefined;
            name = this.convert(node.expression);
        } else if (node.questionDotToken) {
            select = this.rightPadded(JS.newUnary(
                    randomId(),
                    emptySpace,
                    emptyMarkers,
                    this.leftPadded(this.suffix(node.expression), JS.Unary.Type.QuestionDotWithDot),
                    this.visit(node.expression),
                    this.mapType(node)
                ),
                emptySpace
            )
        } else {
            select = this.rightPadded(this.visit(node.expression), this.suffix(node.expression))
        }

        return J.newMethodInvocation(
            randomId(),
            prefix,
            emptyMarkers,
            select,
            typeArguments,
            name,
            this.mapCommaSeparatedList(node.getChildren(this.sourceFile).slice(-3)),
            this.mapMethodType(node)
        )
    }

    visitSuperKeyword(node: ts.KeywordToken<ts.SyntaxKind.SuperKeyword>) {
        return this.mapIdentifier(node, node.getText());
    }

    visitNewExpression(node: ts.NewExpression) {
        return J.newNewClass(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            undefined,
            emptySpace,
            node.typeArguments ? J.newParameterizedType(
                randomId(),
                emptySpace,
                emptyMarkers,
                new JS.TypeTreeExpression(randomId(), emptySpace, emptyMarkers, this.visit(node.expression)),
                this.mapTypeArguments(this.prefix(this.findChildNode(node, ts.SyntaxKind.LessThanToken)!), node.typeArguments),
                undefined
            ) : new JS.TypeTreeExpression(randomId(), emptySpace, emptyMarkers, this.visit(node.expression)),
            node.arguments ? this.mapCommaSeparatedList(this.getParameterListNodes(node)) : J.Container.empty<Expression>().withMarkers(markers({
                kind: JavaMarkers.OmitParentheses,
                id: randomId()
            })),
            undefined,
            this.mapMethodType(node)
        );
    }

    visitTaggedTemplateExpression(node: ts.TaggedTemplateExpression) {
        return JS.newTaggedTemplateExpression(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.rightPadded(this.visit(node.tag), this.suffix(node.tag)),
            node.typeArguments ? this.mapTypeArguments(emptySpace, node.typeArguments) : undefined,
            this.convert(node.template),
            this.mapType(node)
        )
    }

    visitTypeAssertionExpression(node: ts.TypeAssertion) {
        return J.newTypeCast(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            J.newControlParentheses(
                randomId(),
                this.prefix(node.getFirstToken()!),
                emptyMarkers,
                this.rightPadded(this.convert(node.type), this.prefix(node.getChildAt(2, this.sourceFile)))
            ),
            this.convert(node.expression)
        );
    }

    visitParenthesizedExpression(node: ts.ParenthesizedExpression) {
        return {
            kind: J.Kind.Parentheses,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            tree: this.rightPadded(this.convert(node.expression), this.prefix(node.getLastToken()!))
        };
    }

    visitFunctionExpression(node: ts.FunctionExpression) {
        return JS.newFunctionDeclaration(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.mapModifiers(node),
            this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.FunctionKeyword)!), !!node.asteriskToken),
            this.leftPadded(node.asteriskToken ? this.prefix(node.asteriskToken) : emptySpace, node.name ? this.visit(node.name) : J.newIdentifier(randomId(), emptySpace, emptyMarkers, [], "", undefined, undefined)),
            this.mapTypeParametersAsObject(node),
            this.mapCommaSeparatedList(this.getParameterListNodes(node)),
            this.mapTypeInfo(node),
            this.convert(node.body),
            this.mapMethodType(node)
        );
    }

    visitArrowFunction(node: ts.ArrowFunction) {
        const openParenToken = this.findChildNode(node, ts.SyntaxKind.OpenParenToken);
        const isParenthesized = openParenToken != undefined;
        return {
            kind: JS.Kind.ArrowFunction,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            leadingAnnotations: [],
            modifiers: this.mapModifiers(node),
            typeParameters: node.typeParameters ? this.mapTypeParametersAsObject(node) : undefined,
            parameters: {
                kind: J.Kind.LambdaParameters,
                id: randomId(),
                prefix: isParenthesized ? this.prefix(openParenToken) : emptySpace,
                markers: emptyMarkers,
                parenthesized: isParenthesized,
                parameters: node.parameters.length > 0 ?
                    node.parameters.map(p => this.rightPadded(this.convert(p), this.suffix(p)))
                        .concat(node.parameters.hasTrailingComma ? this.rightPadded(this.newJEmpty(), this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseParenToken)!)) : []) :
                    isParenthesized ? [this.rightPadded(this.newJEmpty(), this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseParenToken)!))] : [],
            },
            returnTypeExpression: this.mapTypeInfo(node),
            body: this.leftPadded(this.prefix(node.equalsGreaterThanToken), this.convert(node.body)),
            type: this.mapType(node)
        };
    }

    visitDeleteExpression(node: ts.DeleteExpression) {
        return {
            kind: JS.Kind.Delete,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            expression: this.convert(node.expression),
            type: this.mapType(node)
        };
    }

    visitTypeOfExpression(node: ts.TypeOfExpression) {
        return {
            kind: JS.Kind.TypeOf,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            expression: this.convert(node.expression),
            type: this.mapType(node)
        };
    }

    visitVoidExpression(node: ts.VoidExpression) {
        return {
            kind: JS.Kind.Void,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            expression: this.convert(node.expression)
        };
    }

    visitAwaitExpression(node: ts.AwaitExpression) {
        return {
            kind: JS.Kind.Await,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            expression: this.convert(node.expression),
            type: this.mapType(node)
        };
    }

    visitPrefixUnaryExpression(node: ts.PrefixUnaryExpression) {
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

    visitPostfixUnaryExpression(node: ts.PostfixUnaryExpression) {
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

        return J.newUnary(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.leftPadded(this.suffix(node.operand), unaryOperator),
            this.convert(node.operand),
            this.mapType(node)
        );
    }

    visitBinaryExpression(node: ts.BinaryExpression) {
        if (node.operatorToken.kind == ts.SyntaxKind.EqualsToken) {
            // assignment is also represented as `ts.BinaryExpression`
            return J.newAssignment(
                randomId(),
                this.prefix(node),
                emptyMarkers,
                this.convert(node.left),
                this.leftPadded(this.suffix(node.left), this.convert(node.right)),
                this.mapType(node)
            );
        }

        let binaryOperator: J.Binary.Type | JS.JsBinary.Type | undefined;
        switch (node.operatorToken.kind) {
            case ts.SyntaxKind.EqualsEqualsEqualsToken:
                binaryOperator = JS.JsBinary.Type.IdentityEquals;
                break;
            case ts.SyntaxKind.ExclamationEqualsEqualsToken:
                binaryOperator = JS.JsBinary.Type.IdentityNotEquals;
                break;
            case ts.SyntaxKind.QuestionQuestionToken:
                binaryOperator = JS.JsBinary.Type.QuestionQuestion;
                break;
            case ts.SyntaxKind.InKeyword:
                binaryOperator = JS.JsBinary.Type.In;
                break;
            case ts.SyntaxKind.CommaToken:
                binaryOperator = JS.JsBinary.Type.Comma;
                break;
        }

        if (binaryOperator !== undefined) {
            return JS.newJsBinary(
                randomId(),
                this.prefix(node),
                emptyMarkers,
                this.convert(node.left),
                this.leftPadded(this.prefix(node.operatorToken), binaryOperator as JS.JsBinary.Type),
                this.convert(node.right),
                this.mapType(node)
            );
        }

        if (node.operatorToken.kind == ts.SyntaxKind.InstanceOfKeyword) {
            return J.newInstanceOf(
                randomId(),
                this.prefix(node),
                emptyMarkers,
                this.rightPadded(this.convert(node.left), this.prefix(node.operatorToken)),
                this.convert(node.right),
                undefined,
                this.mapType(node)
            );
        }

        binaryOperator = this.mapBinaryOperator(node);
        if (binaryOperator === undefined) {
            let assignmentOperation;

            switch (node.operatorToken.kind) {
                case ts.SyntaxKind.QuestionQuestionEqualsToken:
                    assignmentOperation = JS.JsAssignmentOperation.Type.QuestionQuestion;
                    break;
                case ts.SyntaxKind.AmpersandAmpersandEqualsToken:
                    assignmentOperation = JS.JsAssignmentOperation.Type.And;
                    break;
                case ts.SyntaxKind.BarBarEqualsToken:
                    assignmentOperation = JS.JsAssignmentOperation.Type.Or;
                    break;
                case ts.SyntaxKind.AsteriskAsteriskToken:
                    assignmentOperation = JS.JsAssignmentOperation.Type.Power;
                    break;
                case ts.SyntaxKind.AsteriskAsteriskEqualsToken:
                    assignmentOperation = JS.JsAssignmentOperation.Type.Exp;
                    break;
            }

            if (assignmentOperation !== undefined) {
                return JS.newJsAssignmentOperation(
                    randomId(),
                    this.prefix(node),
                    emptyMarkers,
                    this.convert(node.left),
                    this.leftPadded(this.prefix(node.operatorToken), assignmentOperation),
                    this.convert(node.right),
                    this.mapType(node)
                )
            }

            assignmentOperation = this.mapAssignmentOperation(node);
            if (assignmentOperation === undefined) {
                return this.visitUnknown(node);
            }
            return J.newAssignmentOperation(
                randomId(),
                this.prefix(node),
                emptyMarkers,
                this.convert(node.left),
                this.leftPadded(this.prefix(node.operatorToken), assignmentOperation),
                this.convert(node.right),
                this.mapType(node)
            )
        }

        return J.newBinary(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.convert(node.left),
            this.leftPadded(this.prefix(node.operatorToken), binaryOperator),
            this.convert(node.right),
            this.mapType(node)
        )
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

    visitConditionalExpression(node: ts.ConditionalExpression) {
        return J.newTernary(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.convert(node.condition),
            this.leftPadded(this.suffix(node.condition), this.convert(node.whenTrue)),
            this.leftPadded(this.suffix(node.whenTrue), this.convert(node.whenFalse)),
            this.mapType(node)
        );
    }

    visitTemplateExpression(node: ts.TemplateExpression) {
        return JS.newTemplateExpression(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.visit(node.head),
            node.templateSpans.map(s => this.rightPadded(this.visit(s), this.suffix(s))),
            this.mapType(node)
        )
    }

    visitYieldExpression(node: ts.YieldExpression) {
        return JS.newYield(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            node.asteriskToken ? this.leftPadded(this.prefix(node.asteriskToken), true) : this.leftPadded(emptySpace, false),
            node.expression ? this.visit(node.expression) : undefined,
            this.mapType(node)
        );
    }

    visitSpreadElement(node: ts.SpreadElement) {
        return JS.newUnary(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.leftPadded(emptySpace, JS.Unary.Type.Spread),
            this.convert(node.expression),
            this.mapType(node)
        );
    }

    visitClassExpression(node: ts.ClassExpression) {
        return JS.newStatementExpression(
            randomId(),
            J.newClassDeclaration(
                randomId(),
                this.prefix(node),
                emptyMarkers,
                this.mapDecorators(node),
                [], //this.mapModifiers(node),
                J.newClassDeclarationKind(
                    randomId(),
                    node.modifiers ? this.suffix(node.modifiers[node.modifiers.length - 1]) : this.prefix(node),
                    emptyMarkers,
                    [],
                    J.ClassType.Class
                ),
                node.name ? this.convert(node.name) : this.mapIdentifier(node, ""),
                this.mapTypeParametersAsJContainer(node),
                undefined, // FIXME primary constructor
                this.mapExtends(node),
                this.mapImplements(node),
                undefined,
                J.newBlock(
                    randomId(),
                    this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBraceToken)!),
                    emptyMarkers,
                    this.rightPadded(false, emptySpace),
                    node.members.map(ce => ({
                        kind: J.Kind.JRightPadded,
                        element: this.convert(ce),
                        after: ce.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken ? this.prefix(ce.getLastToken()!) : emptySpace,
                        markers: ce.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken ? markers({
                            kind: JavaMarkers.Semicolon,
                            id: randomId()
                        }) : emptyMarkers
                    })),
                    this.prefix(node.getLastToken()!)
                ),
                this.mapType(node)
            )
        )
    }

    visitOmittedExpression(node: ts.OmittedExpression) {
        return this.newJEmpty(this.prefix(node));
    }

    visitExpressionWithTypeArguments(node: ts.ExpressionWithTypeArguments) {
        if (node.typeArguments) {
            return JS.newExpressionWithTypeArguments(
                randomId(),
                this.prefix(node),
                emptyMarkers,
                this.visit(node.expression),
                this.mapTypeArguments(this.suffix(node.expression), node.typeArguments),
                this.mapType(node)
            )
        }
        return this.visit(node.expression);
    }

    visitAsExpression(node: ts.AsExpression) {
        return JS.newJsBinary(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.convert(node.expression),
            this.leftPadded(this.prefix(node.getChildAt(1, this.sourceFile)), JS.JsBinary.Type.As),
            this.convert(node.type),
            this.mapType(node)
        );
    }

    visitNonundefinedExpression(node: ts.NonundefinedExpression) {
        return JS.newUnary(
            randomId(),
            emptySpace,
            emptyMarkers,
            this.leftPadded(this.suffix(node.expression), JS.Unary.Type.Exclamation),
            this.visit(node.expression),
            this.mapType(node)
        )
    }

    visitMetaProperty(node: ts.MetaProperty) {
        return J.newFieldAccess(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            node.keywordToken === ts.SyntaxKind.NewKeyword ? this.mapIdentifier(node, 'new') : this.mapIdentifier(node, 'import'),
            this.leftPadded(this.prefix(node.getChildAt(1, this.sourceFile)), this.convert(node.name)),
            this.mapType(node)
        );
    }

    visitSyntheticExpression(node: ts.SyntheticExpression) {
        // SyntheticExpression is a special type of node used internally by the TypeScript compiler
        return this.visitUnknown(node);
    }

    visitSatisfiesExpression(node: ts.SatisfiesExpression) {
        return JS.newSatisfiesExpression(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.visit(node.expression),
            this.leftPadded(this.suffix(node.expression), this.visit(node.type)),
            this.mapType(node)
        );
    }

    visitTemplateSpan(node: ts.TemplateSpan) {
        return JS.newTemplateExpression.TemplateSpan(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.convert(node.expression),
            this.visit(node.literal)
        )
    }

    visitSemicolonClassElement(node: ts.SemicolonClassElement) {
        return this.newJEmpty(this.semicolonPrefix(node));
    }

    visitBlock(node: ts.Block) {
        return J.newBlock(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.rightPadded(false, emptySpace),
            this.semicolonPaddedStatementList(node.statements),
            this.prefix(node.getLastToken()!)
        );
    }

    visitEmptyStatement(node: ts.EmptyStatement) {
        return this.newJEmpty(this.prefix(node));
    }

    visitVariableStatement(node: ts.VariableStatement) {
        const declaration = this.visitVariableDeclarationList(node.declarationList);
        return declaration.withModifiers(this.mapModifiers(node).concat(declaration.modifiers)).withPrefix(this.prefix(node));
    }

    visitExpressionStatement(node: ts.ExpressionStatement): Statement {
        const expression = this.visit(node.expression) as Expression;
        if (isStatement(expression)) {
            return expression as Statement;
        }
        return JS.newExpressionStatement(
            randomId(),
            expression
        )
    }

    visitIfStatement(node: ts.IfStatement) {
        const semicolonAfterThen = (node.thenStatement.getChildAt(node.thenStatement.getChildCount() - 1)?.kind == ts.SyntaxKind.SemicolonToken);
        const semicolonAfterElse = (node.elseStatement?.getChildAt(node.elseStatement.getChildCount() - 1)?.kind == ts.SyntaxKind.SemicolonToken);
        return J.newIf(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            J.newControlParentheses(
                randomId(),
                this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                emptyMarkers,
                this.rightPadded(this.visit(node.expression), this.suffix(node.expression))
            ),
            this.rightPadded(
                this.convert(node.thenStatement),
                semicolonAfterThen ? this.prefix(node.thenStatement.getLastToken()!) : emptySpace,
                semicolonAfterThen ? markers({kind: JavaMarkers.Semicolon, id: randomId()}) : emptyMarkers
            ),
            node.elseStatement ? J.newIfElse(
                randomId(),
                this.prefix(this.findChildNode(node, ts.SyntaxKind.ElseKeyword)!),
                emptyMarkers,
                this.rightPadded(
                    this.convert(node.elseStatement),
                    semicolonAfterElse ? this.prefix(node.elseStatement.getLastToken()!) : emptySpace,
                    semicolonAfterElse ? markers({kind: JavaMarkers.Semicolon, id: randomId()}) : emptyMarkers
                )
            ) : undefined
        );
    }

    visitDoStatement(node: ts.DoStatement) {
        return J.newDoWhileLoop(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.rightPadded(this.visit(node.statement),
                this.semicolonPrefix(node.statement),
                node.statement.getChildAt(node.statement.getChildCount() - 1)?.kind == ts.SyntaxKind.SemicolonToken ? markers({
                    kind: JavaMarkers.Semicolon,
                    id: randomId()
                }) : emptyMarkers),
            this.leftPadded(
                this.prefix(this.findChildNode(node, ts.SyntaxKind.WhileKeyword)!),
                J.newControlParentheses(
                    randomId(),
                    this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                    emptyMarkers,
                    this.rightPadded(this.visit(node.expression), this.suffix(node.expression))
                )
            )
        );
    }

    visitWhileStatement(node: ts.WhileStatement) {
        return J.newWhileLoop(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            J.newControlParentheses(
                randomId(),
                this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                emptyMarkers,
                this.rightPadded(this.visit(node.expression), this.suffix(node.expression))
            ),
            this.rightPadded(
                this.convert(node.statement),
                this.semicolonPrefix(node.statement),
                node.statement.getChildAt(node.statement.getChildCount() - 1)?.kind == ts.SyntaxKind.SemicolonToken ? markers({
                    kind: JavaMarkers.Semicolon,
                    id: randomId()
                }) : emptyMarkers
            )
        );
    }

    visitForStatement(node: ts.ForStatement) {
        return J.newForLoop(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            J.newForLoopControl(
                randomId(),
                this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                emptyMarkers,
                [node.initializer ?
                    (ts.isVariableDeclarationList(node.initializer) ? this.rightPadded(this.visit(node.initializer), emptySpace) :
                        this.rightPadded(ts.isStatement(node.initializer) ? this.visit(node.initializer) : new ExpressionStatement(randomId(), this.visit(node.initializer)), this.suffix(node.initializer))) :
                    this.rightPadded(this.newJEmpty(), this.suffix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!))],  // to handle for (/*_*/; ; );
                node.condition ? this.rightPadded(this.visit(node.condition), this.suffix(node.condition)) :
                    this.rightPadded(this.newJEmpty(), this.suffix(this.findChildNode(node, ts.SyntaxKind.SemicolonToken)!)),  // to handle for ( ;/*_*/; );
                [node.incrementor ? this.rightPadded(ts.isStatement(node.incrementor) ? this.visit(node.incrementor) : new ExpressionStatement(randomId(), this.visit(node.incrementor)), this.suffix(node.incrementor)) :
                    this.rightPadded(this.newJEmpty(this.prefix(this.findChildNode(node, ts.SyntaxKind.CloseParenToken)!)), emptySpace)],  // to handle for ( ; ;/*_*/);
            ),
            this.rightPadded(
                this.convert(node.statement),
                this.semicolonPrefix(node.statement),
                node.statement.getChildAt(node.statement.getChildCount() - 1)?.kind == ts.SyntaxKind.SemicolonToken ? markers({
                    kind: JavaMarkers.Semicolon,
                    id: randomId()
                }) : emptyMarkers
            )
        );
    }

    visitForInStatement(node: ts.ForInStatement) {
        return {
            kind: JS.Kind.JSForInLoop,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            control: {
                kind: JS.Kind.JSForInOfLoopControl,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                markers: emptyMarkers,
                variable: this.rightPadded(this.visit(node.initializer), this.suffix(node.initializer)),
                iterable: this.rightPadded(this.visit(node.expression), this.suffix(node.expression))
            },
            body: this.rightPadded(
                this.convert(node.statement),
                this.semicolonPrefix(node.statement),
                node.statement.getChildAt(node.statement.getChildCount() - 1)?.kind == ts.SyntaxKind.SemicolonToken ? markers({
                    kind: JavaMarkers.Semicolon,
                    id: randomId()
                }) : emptyMarkers
            )
        };
    }

    visitForOfStatement(node: ts.ForOfStatement) {
        return JS.newJSForOfLoop(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            node.awaitModifier ? this.leftPadded(this.prefix(node.awaitModifier), true) : this.leftPadded(emptySpace, false),
            {
                kind: JS.Kind.JSForInOfLoopControl,
                id: randomId(),
                prefix: this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                markers: emptyMarkers,
                variable: this.rightPadded(this.visit(node.initializer), this.suffix(node.initializer)),
                iterable: this.rightPadded(this.visit(node.expression), this.suffix(node.expression))
            },
            this.rightPadded(
                this.convert(node.statement),
                this.semicolonPrefix(node.statement),
                node.statement.getChildAt(node.statement.getChildCount() - 1)?.kind == ts.SyntaxKind.SemicolonToken ? markers({
                    kind: JavaMarkers.Semicolon,
                    id: randomId()
                }) : emptyMarkers
            )
        );
    }

    visitContinueStatement(node: ts.ContinueStatement) {
        return J.newContinue(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            node.label ? this.visit(node.label) : undefined
        );
    }

    visitBreakStatement(node: ts.BreakStatement) {
        return J.newBreak(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            node.label ? this.visit(node.label) : undefined
        );
    }

    visitReturnStatement(node: ts.ReturnStatement) {
        return J.newReturn(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            node.expression ? this.convert<Expression>(node.expression) : undefined
        );
    }

    visitWithStatement(node: ts.WithStatement) {
        return JS.newWithStatement(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            J.newControlParentheses(
                randomId(),
                this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                emptyMarkers,
                this.rightPadded(this.visit(node.expression), this.suffix(node.expression))
            ),
            this.rightPadded(
                this.convert(node.statement),
                this.semicolonPrefix(node.statement),
                node.statement.getChildAt(node.statement.getChildCount() - 1)?.kind == ts.SyntaxKind.SemicolonToken ? markers({
                    kind: JavaMarkers.Semicolon,
                    id: randomId()
                }) : emptyMarkers
            )
        );
    }

    visitSwitchStatement(node: ts.SwitchStatement) {
        return J.newSwitch(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            J.newControlParentheses(
                randomId(),
                this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                emptyMarkers,
                this.rightPadded(this.visit(node.expression), this.suffix(node.expression))
            ),
            this.visit(node.caseBlock)
        );
    }

    visitLabeledStatement(node: ts.LabeledStatement) {
        return J.newLabel(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.rightPadded(this.visit(node.label), this.suffix(node.label)),
            JS.newTrailingTokenStatement(
                randomId(),
                emptySpace,
                emptyMarkers,
                this.rightPadded(
                    this.visit(node.statement),
                    this.semicolonPrefix(node.statement),
                    node.statement.getChildAt(node.statement.getChildCount() - 1)?.kind == ts.SyntaxKind.SemicolonToken ? markers({
                        kind: JavaMarkers.Semicolon,
                        id: randomId()
                    }) : emptyMarkers
                ),
                this.mapType(node.statement)
            )
        );
    }

    visitThrowStatement(node: ts.ThrowStatement) {
        return J.newThrow(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.visit(node.expression)
        );
    }

    visitTryStatement(node: ts.TryStatement) {
        if (node.catchClause?.variableDeclaration?.name && !ts.isIdentifier(node.catchClause?.variableDeclaration?.name)) {
            return JS.newJSTry(
                randomId(),
                this.prefix(node),
                emptyMarkers,
                this.visit(node.tryBlock),
                this.visit(node.catchClause),
                node.finallyBlock ? this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.FinallyKeyword)!), this.visit(node.finallyBlock)) : undefined
            );
        }

        return J.newTry(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            undefined,
            this.visit(node.tryBlock),
            node.catchClause ? [this.visit(node.catchClause)] : [],
            node.finallyBlock ? this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.FinallyKeyword)!), this.visit(node.finallyBlock)) : undefined
        );
    }

    visitDebuggerStatement(node: ts.DebuggerStatement) {
        return new ExpressionStatement(
            randomId(),
            this.mapIdentifier(node, 'debugger')
        );
    }

    visitVariableDeclaration(node: ts.VariableDeclaration) {
        const nameExpression = this.visit(node.name);

        if (nameExpression instanceof J.Identifier && !node.exclamationToken) {
            return J.newVariableDeclarations.NamedVariable(
                randomId(),
                this.prefix(node),
                emptyMarkers,
                nameExpression,
                [],
                node.initializer ? this.leftPadded(this.prefix(node.getChildAt(node.getChildCount(this.sourceFile) - 2)), this.visit(node.initializer)) : undefined,
                this.mapVariableType(node)
            );
        }

        return JS.newJSVariableDeclarations.JSNamedVariable(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            node.exclamationToken ? JS.newUnary(
                randomId(),
                emptySpace,
                emptyMarkers,
                this.leftPadded(
                    this.suffix(node.name),
                    JS.Unary.Type.Exclamation
                ),
                nameExpression,
                this.mapType(node)
            ) : nameExpression,
            [],
            node.initializer ? this.leftPadded(this.prefix(node.getChildAt(node.getChildCount(this.sourceFile) - 2)), this.visit(node.initializer)) : undefined,
            this.mapVariableType(node)
        );
    }

    visitVariableDeclarationList(node: ts.VariableDeclarationList) {
        let kind = node.getFirstToken();

        // to parse the declaration case: await using db = ...
        let modifier;
        if (kind?.kind === ts.SyntaxKind.AwaitKeyword) {
            modifier = J.newModifier(
                randomId(),
                this.prefix(kind),
                emptyMarkers,
                'await',
                J.ModifierType.LanguageExtension,
                []
            );
            kind = node.getChildAt(1);
        }
        return JS.newScopedVariableDeclarations(
            randomId(),
            emptySpace,
            emptyMarkers,
            modifier ? [modifier] : [],
            this.leftPadded(
                kind ? this.prefix(kind) : this.prefix(node),
                kind?.kind === ts.SyntaxKind.LetKeyword
                    ? JS.ScopedVariableDeclarations.Scope.Let
                    : kind?.kind === ts.SyntaxKind.ConstKeyword
                        ? JS.ScopedVariableDeclarations.Scope.Const
                        : kind?.kind === ts.SyntaxKind.UsingKeyword
                            ? JS.ScopedVariableDeclarations.Scope.Using
                            : JS.ScopedVariableDeclarations.Scope.Var
            ),
            node.declarations.map((declaration) => {
                const declarationExpression = this.visit(declaration);

                return this.rightPadded(
                    JS.isJavaScript(declarationExpression)
                        ? JS.newJSVariableDeclarations(
                            randomId(),
                            this.prefix(declaration),
                            emptyMarkers,
                            [], // FIXME decorators?
                            [], // FIXME modifiers?
                            this.mapTypeInfo(declaration),
                            undefined, // FIXME varargs
                            [this.rightPadded(declarationExpression as JS.JSVariableDeclarations.JSNamedVariable, emptySpace)]
                        )
                        : J.newVariableDeclarations(
                            randomId(),
                            this.prefix(declaration),
                            emptyMarkers,
                            [], // FIXME decorators?
                            [], // FIXME modifiers?
                            this.mapTypeInfo(declaration),
                            undefined, // FIXME varargs
                            [],
                            [this.rightPadded(declarationExpression, emptySpace)]
                        ),
                    this.suffix(declaration)
                );
            })
        );
    }

    visitFunctionDeclaration(node: ts.FunctionDeclaration) {
        return JS.newFunctionDeclaration(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.mapModifiers(node),
            this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.FunctionKeyword)!), !!node.asteriskToken),
            this.leftPadded(node.asteriskToken ? this.prefix(node.asteriskToken) : emptySpace, node.name ? this.visit(node.name) : J.newIdentifier(randomId(), emptySpace, emptyMarkers, [], "", undefined, undefined)),
            this.mapTypeParametersAsObject(node),
            this.mapCommaSeparatedList(this.getParameterListNodes(node)),
            this.mapTypeInfo(node),
            node.body ? this.convert(node.body) : undefined,
            this.mapMethodType(node)
        );
    }

    private getParameterListNodes(node: ts.SignatureDeclarationBase | ts.NewExpression, openToken: ts.SyntaxKind = ts.SyntaxKind.OpenParenToken) {
        const children = node.getChildren(this.sourceFile);
        for (let i = 0; i < children.length; i++) {
            if (children[i].kind == openToken) {
                return children.slice(i, i + 3);
            }
        }
        return [];
    }

    visitInterfaceDeclaration(node: ts.InterfaceDeclaration) {
        return J.newClassDeclaration(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            [], // interface has no decorators
            this.mapModifiers(node),
            J.newClassDeclarationKind(
                randomId(),
                node.modifiers ? this.suffix(node.modifiers[node.modifiers.length - 1]) : this.prefix(node),
                emptyMarkers,
                [],
                J.ClassType.Interface
            ),
            node.name ? this.convert(node.name) : this.mapIdentifier(node, ""),
            this.mapTypeParametersAsJContainer(node),
            undefined, // interface has no constructor
            undefined, // implements should be used
            this.mapInterfaceExtends(node), // interface extends modeled as implements
            undefined,
            J.newBlock(
                randomId(),
                this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBraceToken)!),
                emptyMarkers,
                this.rightPadded(false, emptySpace),
                node.members.map(te => ({
                    kind: J.Kind.JRightPadded,
                    element: this.convert(te),
                    after: (te.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken) || (te.getLastToken()?.kind === ts.SyntaxKind.CommaToken) ? this.prefix(te.getLastToken()!) : emptySpace,
                    markers: (te.getLastToken()?.kind === ts.SyntaxKind.SemicolonToken) || (te.getLastToken()?.kind === ts.SyntaxKind.CommaToken) ? markers(this.convertToken(te.getLastToken())!) : emptyMarkers
                })),
                this.prefix(node.getLastToken()!)
            ),
            this.mapType(node)
        );
    }

    visitTypeAliasDeclaration(node: ts.TypeAliasDeclaration) {
        return JS.newTypeDeclaration(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.mapModifiers(node),
            this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.TypeKeyword)!), this.visit(node.name)),
            node.typeParameters ? this.mapTypeParametersAsObject(node) : undefined,
            this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.EqualsToken)!), this.convert(node.type)),
            this.mapType(node)
        );
    }

    visitEnumDeclaration(node: ts.EnumDeclaration) {
        return J.newClassDeclaration(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            [], // enum has no decorators
            this.mapModifiers(node),
            J.newClassDeclarationKind(
                randomId(),
                node.modifiers ? this.suffix(node.modifiers[node.modifiers.length - 1]) : this.prefix(node),
                emptyMarkers,
                [],
                J.ClassType.Enum
            ),
            node.name ? this.convert(node.name) : this.mapIdentifier(node, ""),
            undefined, // enum has no type parameters
            undefined, // enum has no constructor
            undefined, // enum can't extend smth.
            undefined, // enum can't implement smth.
            undefined,
            J.newBlock(
                randomId(),
                this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenBraceToken)!),
                emptyMarkers,
                this.rightPadded(false, emptySpace),
                [this.rightPadded(
                    J.newEnumValueSet(
                        randomId(),
                        emptySpace,
                        emptyMarkers,
                        node.members.map(em => this.rightPadded(this.visit(em), this.suffix(em))),
                        node.members.hasTrailingComma),
                    emptySpace)],
                this.prefix(node.getLastToken()!)
            ),
            this.mapType(node) as JavaType.Class
        );
    }

    visitModuleDeclaration(node: ts.ModuleDeclaration) {
        const body = node.body ? this.visit(node.body as ts.Node) : undefined;

        let namespaceKeyword = this.findChildNode(node, ts.SyntaxKind.NamespaceKeyword) ?? this.findChildNode(node, ts.SyntaxKind.ModuleKeyword);
        let keywordType: JS.NamespaceDeclaration.KeywordType;
        if (namespaceKeyword == undefined) {
            keywordType = JS.NamespaceDeclaration.KeywordType.Empty;
        } else if (namespaceKeyword?.kind === ts.SyntaxKind.NamespaceKeyword) {
            keywordType = JS.NamespaceDeclaration.KeywordType.Namespace;
        } else {
            keywordType = JS.NamespaceDeclaration.KeywordType.Module;
        }
        if (body instanceof JS.NamespaceDeclaration) {
            return JS.newNamespaceDeclaration(
                randomId(),
                emptySpace,
                emptyMarkers,
                this.mapModifiers(node),
                this.leftPadded(
                    namespaceKeyword ? this.prefix(namespaceKeyword) : emptySpace,
                    keywordType
                ),
                this.rightPadded(
                    (body.name instanceof J.FieldAccess)
                        ? this.remapFieldAccess(body.name, node.name)
                        : J.newFieldAccess(
                            randomId(),
                            emptySpace,
                            emptyMarkers,
                            this.visit(node.name),
                            {
                                kind: J.Kind.JLeftPadded,
                                before: this.suffix(node.name),
                                element: body.name as J.Identifier,
                                markers: emptyMarkers
                            },
                            undefined
                        ),
                    body.padding.name.after
                ),
                body.body
            );
        } else {
            return JS.newNamespaceDeclaration(
                randomId(),
                node.parent.kind === ts.SyntaxKind.ModuleBlock ? this.prefix(node) : emptySpace,
                emptyMarkers,
                this.mapModifiers(node),
                this.leftPadded(
                    namespaceKeyword ? this.prefix(namespaceKeyword) : emptySpace,
                    keywordType
                ),
                this.rightPadded(this.convert(node.name), this.suffix(node.name)), // J.FieldAccess
                body // J.Block
            );
        }
    }

    private remapFieldAccess(fa: J.FieldAccess, name: ts.ModuleName): J.FieldAccess {
        if (fa.target instanceof J.Identifier) {
            return J.newFieldAccess(
                randomId(),
                emptySpace,
                emptyMarkers,
                J.newFieldAccess(
                    randomId(),
                    emptySpace,
                    emptyMarkers,
                    this.visit(name),
                    this.leftPadded(
                        this.suffix(name),
                        fa.target
                    ),
                    undefined
                ),
                fa.padding.name,
                undefined
            );
        }

        return J.newFieldAccess(
            randomId(),
            emptySpace,
            emptyMarkers,
            this.remapFieldAccess(fa.target as J.FieldAccess, name),
            fa.padding.name,
            undefined
        );
    }

    visitModuleBlock(node: ts.ModuleBlock) {
        return J.newBlock(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.rightPadded(false, emptySpace),
            this.semicolonPaddedStatementList(node.statements),
            this.prefix(node.getLastToken()!)
        );
    }

    visitCaseBlock(node: ts.CaseBlock) {
        return J.newBlock(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.rightPadded(false, emptySpace),
            node.clauses.map(clause =>
                this.rightPadded(
                    this.visit(clause),
                    this.suffix(clause)
                )),
            this.prefix(node.getLastToken()!)
        )
    }

    visitNamespaceExportDeclaration(node: ts.NamespaceExportDeclaration) {
        return JS.newNamespaceDeclaration(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            [
                J.newModifier(
                    randomId(),
                    emptySpace,
                    emptyMarkers,
                    'export',
                    J.ModifierType.LanguageExtension,
                    []
                ),
                J.newModifier(
                    randomId(),
                    this.prefix(this.findChildNode(node, ts.SyntaxKind.AsKeyword)!),
                    emptyMarkers,
                    'as',
                    J.ModifierType.LanguageExtension,
                    []
                )
            ],
            this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.NamespaceKeyword)!), JS.NamespaceDeclaration.KeywordType.Namespace),
            this.rightPadded(this.convert(node.name), this.suffix(node.name)),
            undefined
        );
    }

    visitImportEqualsDeclaration(node: ts.ImportEqualsDeclaration) {
        const kind = this.findChildNode(node, ts.SyntaxKind.ImportKeyword)!;

        return JS.newScopedVariableDeclarations(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.mapModifiers(node),
            this.leftPadded(
                this.prefix(kind),
                JS.ScopedVariableDeclarations.Scope.Import
            ),
            [
                this.rightPadded(J.newVariableDeclarations(
                    randomId(),
                    emptySpace,
                    emptyMarkers,
                    [],
                    node.isTypeOnly ? [J.newModifier(
                        randomId(),
                        this.prefix(this.findChildNode(node, ts.SyntaxKind.TypeKeyword)!),
                        emptyMarkers,
                        "type",
                        J.ModifierType.LanguageExtension,
                        []
                    )] : [],
                    undefined,
                    undefined,
                    [],
                    [this.rightPadded(J.newVariableDeclarations.NamedVariable(
                        randomId(),
                        emptySpace,
                        emptyMarkers,
                        this.visit(node.name),
                        [],
                        this.leftPadded(this.suffix(node.name), this.visit(node.moduleReference)),
                        this.mapVariableType(node)
                    ), emptySpace)]
                ), emptySpace)
            ]
        )
    }

    visitImportKeyword(node: ts.ImportExpression) {
        // this is used for dynamic imports as in `await import('foo')`
        return this.mapIdentifier(node, 'import');
    }

    visitImportDeclaration(node: ts.ImportDeclaration) {
        return JS.newJsImport(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.mapModifiers(node),
            node.importClause ? this.visit(node.importClause) : undefined,
            this.leftPadded(node.importClause ? this.prefix(this.findChildNode(node, ts.SyntaxKind.FromKeyword)!) : emptySpace, this.visit(node.moduleSpecifier)),
            node.attributes ? this.visit(node.attributes) : undefined
        );
    }

    visitImportClause(node: ts.ImportClause) {
        return JS.newJsImportClause(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            node.isTypeOnly,
            node.name ? this.rightPadded(this.visit(node.name), this.suffix(node.name)) : undefined,
            node.namedBindings ? this.visit(node.namedBindings) : undefined
        );
    }

    visitNamespaceImport(node: ts.NamespaceImport) {
        return {
            kind: JS.Kind.Alias,
            id: randomId(),
            prefix: this.prefix(node),
            markers: emptyMarkers,
            propertyName: this.rightPadded(this.mapIdentifier(node, "*"), this.prefix(this.findChildNode(node, ts.SyntaxKind.AsKeyword)!)),
            alias: this.visit(node.name)
        };
    }

    visitNamedImports(node: ts.NamedImports) {
        return JS.newNamedImports(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.mapCommaSeparatedList(node.getChildren(this.sourceFile)),
            undefined
        );
    }

    visitImportSpecifier(node: ts.ImportSpecifier) {
        return JS.newJsImportSpecifier(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.leftPadded(
                node.isTypeOnly ? this.prefix(this.findChildNode(node, ts.SyntaxKind.TypeKeyword)!) : emptySpace,
                node.isTypeOnly
            ),
            node.propertyName
                ? JS.newAlias(
                    randomId(),
                    this.prefix(node.propertyName),
                    emptyMarkers,
                    this.rightPadded(this.convert(node.propertyName), this.suffix(node.propertyName)),
                    this.convert(node.name)
                )
                : this.convert(node.name),
            this.mapType(node)
        );
    }

    visitExportAssignment(node: ts.ExportAssignment) {
        return JS.newExportAssignment(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.mapModifiers(node),
            this.leftPadded(node.isExportEquals ? this.prefix(this.findChildNode(node, ts.SyntaxKind.EqualsToken)!) : emptySpace, (!!node.isExportEquals)),
            this.visit(node.expression)
        );
    }

    visitExportDeclaration(node: ts.ExportDeclaration) {
        return JS.newExportDeclaration(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.mapModifiers(node),
            this.leftPadded(node.isTypeOnly ? this.prefix(this.findChildNode(node, ts.SyntaxKind.TypeKeyword)!) : emptySpace, node.isTypeOnly),
            node.exportClause ? this.visit(node.exportClause) : this.mapIdentifier(this.findChildNode(node, ts.SyntaxKind.AsteriskToken)!, "*"),
            node.moduleSpecifier ? this.leftPadded(this.prefix(this.findChildNode(node, ts.SyntaxKind.FromKeyword)!), this.visit(node.moduleSpecifier)) : undefined,
            node.attributes ? this.visit(node.attributes) : undefined
        );
    }

    visitNamedExports(node: ts.NamedExports) {
        return JS.newNamedExports(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.mapCommaSeparatedList(node.getChildren()),
            this.mapType(node)
        );
    }

    visitNamespaceExport(node: ts.NamespaceExport) {
        return JS.newAlias(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.rightPadded(this.mapIdentifier(this.findChildNode(node, ts.SyntaxKind.AsteriskToken)!, "*"), this.prefix(this.findChildNode(node, ts.SyntaxKind.AsKeyword)!)),
            this.visit(node.name)
        )
    }

    visitExportSpecifier(node: ts.ExportSpecifier) {
        return JS.newExportSpecifier(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.leftPadded(node.isTypeOnly ? this.prefix(this.findChildNode(node, ts.SyntaxKind.TypeKeyword)!) : emptySpace, node.isTypeOnly),
            node.propertyName
                ? JS.newAlias(
                    randomId(),
                    this.prefix(node.propertyName),
                    emptyMarkers,
                    this.rightPadded(this.convert(node.propertyName), this.suffix(node.propertyName)),
                    this.convert(node.name)
                )
                : this.convert(node.name),
            this.mapType(node)
        );
    }

    visitMissingDeclaration(node: ts.MissingDeclaration) {
        return this.visitUnknown(node);
    }

    visitExternalModuleReference(node: ts.ExternalModuleReference) {
        return J.newMethodInvocation(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            undefined,
            undefined,
            this.mapIdentifier(node, "require"),
            J.newJContainer(
                this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                [this.rightPadded(this.visit(node.expression), this.suffix(node.expression))],
                emptyMarkers
            ),
            this.mapMethodType(node)
        )
    }

    visitJsxElement(node: ts.JsxElement) {
        return this.visitUnknown(node);
    }

    visitJsxSelfClosingElement(node: ts.JsxSelfClosingElement) {
        return this.visitUnknown(node);
    }

    visitJsxOpeningElement(node: ts.JsxOpeningElement) {
        return this.visitUnknown(node);
    }

    visitJsxClosingElement(node: ts.JsxClosingElement) {
        return this.visitUnknown(node);
    }

    visitJsxFragment(node: ts.JsxFragment) {
        return this.visitUnknown(node);
    }

    visitJsxOpeningFragment(node: ts.JsxOpeningFragment) {
        return this.visitUnknown(node);
    }

    visitJsxClosingFragment(node: ts.JsxClosingFragment) {
        return this.visitUnknown(node);
    }

    visitJsxAttribute(node: ts.JsxAttribute) {
        return this.visitUnknown(node);
    }

    visitJsxAttributes(node: ts.JsxAttributes) {
        return this.visitUnknown(node);
    }

    visitJsxSpreadAttribute(node: ts.JsxSpreadAttribute) {
        return this.visitUnknown(node);
    }

    visitJsxExpression(node: ts.JsxExpression) {
        return this.visitUnknown(node);
    }

    visitJsxNamespacedName(node: ts.JsxNamespacedName) {
        return this.visitUnknown(node);
    }

    visitCaseClause(node: ts.CaseClause) {
        return J.newCase(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            J.Case.Type.Statement,
            {
                kind: J.Kind.JContainer,
                before: this.prefix(node.expression),
                elements: [this.rightPadded(
                    this.visit(node.expression),
                    this.suffix(node.expression)
                )],
                markers: emptyMarkers
            },
            {
                kind: J.Kind.JContainer,
                before: this.prefix(node),
                elements: this.semicolonPaddedStatementList(node.statements),
                markers: emptyMarkers
            },
            undefined,
            undefined
        );
    }

    visitDefaultClause(node: ts.DefaultClause) {
        return J.newCase(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            J.Case.Type.Statement,
            {
                kind: J.Kind.JContainer,
                before: this.prefix(node),
                elements: [this.rightPadded(this.mapIdentifier(node, 'default'), this.suffix(this.findChildNode(node, ts.SyntaxKind.DefaultKeyword)!))],
                markers: emptyMarkers
            },
            {
                kind: J.Kind.JContainer,
                before: this.prefix(node),
                elements: this.semicolonPaddedStatementList(node.statements),
                markers: emptyMarkers
            },
            undefined,
            undefined
        );
    }

    visitHeritageClause(node: ts.HeritageClause) {
        return this.convert(node.types[0]);
    }

    visitCatchClause(node: ts.CatchClause) {
        if (node.variableDeclaration?.name && !ts.isIdentifier(node.variableDeclaration?.name)) {
            return JS.newJSTryJSCatch(
                randomId(),
                this.prefix(node),
                emptyMarkers,
                J.newControlParentheses(
                    randomId(),
                    this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                    emptyMarkers,
                    this.rightPadded(
                        JS.newJSVariableDeclarations(
                            randomId(),
                            this.prefix(node.variableDeclaration),
                            emptyMarkers,
                            [],
                            [],
                            this.mapTypeInfo(node.variableDeclaration),
                            undefined,
                            [this.rightPadded(this.visit(node.variableDeclaration), emptySpace)]
                        ),
                        this.suffix(node.variableDeclaration))
                ),
                this.visit(node.block)
            )
        }

        return J.newTryCatch(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            node.variableDeclaration ?
                J.newControlParentheses(
                    randomId(),
                    this.prefix(this.findChildNode(node, ts.SyntaxKind.OpenParenToken)!),
                    emptyMarkers,
                    this.rightPadded(
                        J.newVariableDeclarations(
                            randomId(),
                            this.prefix(node.variableDeclaration),
                            emptyMarkers,
                            [],
                            [],
                            this.mapTypeInfo(node.variableDeclaration),
                            undefined,
                            [],
                            [this.rightPadded(this.visit(node.variableDeclaration), emptySpace)]
                        ),
                        this.suffix(node.variableDeclaration))
                ) :
                // should return empty variables list to handle: try { } catch { }
                J.newControlParentheses(
                    randomId(),
                    emptySpace,
                    emptyMarkers,
                    this.rightPadded(J.newVariableDeclarations(randomId(), emptySpace, emptyMarkers, [], [], undefined, undefined, [], []), emptySpace)
                ),
            this.visit(node.block)
        )
    }

    visitImportAttributes(node: ts.ImportAttributes) {
        const openBraceIndex = node.getChildren().findIndex(n => n.kind === ts.SyntaxKind.OpenBraceToken);
        const elements = this.mapCommaSeparatedList(node.getChildren(this.sourceFile).slice(openBraceIndex, openBraceIndex + 3));
        return JS.newImportAttributes(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            ts.SyntaxKind.WithKeyword === node.token ? JS.ImportAttributes.Token.With : JS.ImportAttributes.Token.Assert,
            elements
        );
    }

    visitImportAttribute(node: ts.ImportAttribute) {
        return JS.newImportAttribute(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.visit(node.name),
            this.leftPadded(this.suffix(node.name), this.visit(node.value))
        );
    }

    visitPropertyAssignment(node: ts.PropertyAssignment) {
        return JS.newPropertyAssignment(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.rightPadded(this.visit(node.name), this.suffix(node.name)),
            JS.PropertyAssignment.Token.Colon,
            this.visit(node.initializer)
        );
    }

    visitShorthandPropertyAssignment(node: ts.ShorthandPropertyAssignment) {
        return JS.newPropertyAssignment(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.rightPadded(this.visit(node.name), this.suffix(node.name)),
            JS.PropertyAssignment.Token.Equals,
            node.objectAssignmentInitializer ? this.visit(node.objectAssignmentInitializer) : undefined
        );
    }

    visitSpreadAssignment(node: ts.SpreadAssignment) {
        return JS.newPropertyAssignment(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            this.rightPadded(
                JS.newUnary(
                    randomId(),
                    emptySpace,
                    emptyMarkers,
                    this.leftPadded(
                        emptySpace,
                        JS.Unary.Type.Spread
                    ),
                    this.visit(node.expression),
                    this.mapType(node.expression)
                ),
                this.suffix(node.expression)
            ),
            JS.PropertyAssignment.Token.Empty,
            undefined
        );
    }

    visitEnumMember(node: ts.EnumMember) {
        return J.newEnumValue(
            randomId(),
            this.prefix(node),
            emptyMarkers,
            [],
            node.name ? ts.isStringLiteral(node.name) ? this.mapIdentifier(node.name, node.name.getText()) : this.convert(node.name) : this.mapIdentifier(node, ""),
            node.initializer ? J.newNewClass(
                randomId(),
                this.suffix(node.name),
                emptyMarkers,
                undefined,
                emptySpace,
                undefined,
                {
                    kind: J.Kind.JContainer,
                    before: emptySpace,
                    elements: [this.rightPadded(this.visit(node.initializer), emptySpace)],
                    markers: emptyMarkers
                },
                undefined,
                this.mapMethodType(node)
            ) : undefined
        )
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

    visitJSDocundefinedableType(node: ts.JSDocundefinedableType) {
        return this.visitUnknown(node);
    }

    visitJSDocNonundefinedableType(node: ts.JSDocNonundefinedableType) {
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
        // return Space.format(this.sourceFile.text, node.getFullStart(), node.getFullStart() + node.getLeadingTriviaWidth());
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
            kind: J.Kind.JContainer,
            before: prefix,
            elements: args,
            markers: emptyMarkers
        };
    }

    private trailingComma = (nodes: readonly ts.Node[]) => (ns: readonly ts.Node[], i: number) => {
        const last = i === ns.length - 2;
        return last ? markers({
            kind: JavaMarkers.TrailingComma,
            id: randomId(),
            suffix: this.prefix(nodes[2], false)
        } as TrailingComma) : emptyMarkers;
    }

    private mapToContainer<T extends J>(nodes: readonly ts.Node[], markers?: (ns: readonly ts.Node[], i: number) => Markers): J.Container<T> {
        if (nodes.length === 0) {
            return emptyContainer();
        }
        const prefix = this.prefix(nodes[0]);
        const args: J.RightPadded<T>[] = this.mapToRightPaddedList(nodes[1] as ts.SyntaxList, this.prefix(nodes[2]), markers);
        return {
            kind: J.Kind.JContainer,
            before: prefix,
            elements: args,
            markers: emptyMarkers
        };
    }

    private mapToRightPaddedList<T extends J>(node: ts.SyntaxList, lastAfter: J.Space, markers?: (ns: readonly ts.Node[], i: number) => Markers): J.RightPadded<T>[] {
        let elementList = node.getChildren(this.sourceFile);
        let childCount = elementList.length;

        const args: J.RightPadded<T>[] = [];
        if (childCount === 0) {
            args.push(this.rightPadded(
                this.newJEmpty() as T,
                lastAfter,
                emptyMarkers
            ));
        } else {
            for (let i = 0; i < childCount - 1; i += 2) {
                // FIXME right padding and trailing comma
                const last = i === childCount - 2;
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

    private mapDecorators(node: ts.ClassDeclaration | ts.FunctionDeclaration | ts.MethodDeclaration | ts.ConstructorDeclaration | ts.ParameterDeclaration | ts.PropertyDeclaration | ts.SetAccessorDeclaration | ts.GetAccessorDeclaration | ts.ClassExpression): J.Annotation[] {
        return node.modifiers?.filter(ts.isDecorator)?.map(this.convert<J.Annotation>) ?? [];
    }

    private mapTypeParametersAsJContainer(node: ts.ClassDeclaration | ts.InterfaceDeclaration | ts.ClassExpression): J.Container<J.TypeParameter> | undefined {
        return node.typeParameters
            ? {
                kind: J.Kind.JContainer,
                before: this.prefix(this.findChildNode(node, ts.SyntaxKind.LessThanToken)!),
                elements: this.mapTypeParametersList(node.typeParameters)
                    .concat(node.typeParameters.hasTrailingComma ? this.rightPadded(
                        J.newTypeParameter(randomId(), emptySpace, emptyMarkers, [], [], this.newJEmpty(), undefined),
                        this.prefix(this.findChildNode(node, ts.SyntaxKind.GreaterThanToken)!)) : []),
                markers: emptyMarkers
            }
            : undefined;
    }

    private mapTypeParametersAsObject(node: ts.MethodDeclaration | ts.MethodSignature | ts.FunctionDeclaration
        | ts.CallSignatureDeclaration | ts.ConstructSignatureDeclaration | ts.FunctionExpression | ts.ArrowFunction | ts.TypeAliasDeclaration | ts.FunctionTypeNode | ts.ConstructorTypeNode): J.TypeParameters | undefined {
        const typeParameters = node.typeParameters;
        if (!typeParameters) return undefined;

        return J.newTypeParameters(
            randomId(),
            this.prefix(this.findChildNode(node, ts.SyntaxKind.LessThanToken)!),
            emptyMarkers,
            [],
            typeParameters.length == 0 ?
                [this.rightPadded(J.newTypeParameter(randomId(), emptySpace, emptyMarkers, [], [], this.newJEmpty(), undefined), this.prefix(this.findChildNode(node, ts.SyntaxKind.GreaterThanToken)!))]
                : typeParameters.map(tp => this.rightPadded(this.visit(tp), this.suffix(tp)))
                    .concat(typeParameters.hasTrailingComma ? this.rightPadded(
                        J.newTypeParameter(randomId(), emptySpace, emptyMarkers, [], [], this.newJEmpty(), undefined),
                        this.prefix(this.findChildNode(node, ts.SyntaxKind.GreaterThanToken)!)) : []),
        );
    }

    private mapTypeParametersList(typeParamsNodeArray: ts.NodeArray<ts.TypeParameterDeclaration>): J.RightPadded<J.TypeParameter>[] {
        return typeParamsNodeArray.map(tp => this.rightPadded<J.TypeParameter>(this.visit(tp), this.suffix(tp)));
    }

    private findChildNode(node: ts.Node, kind: ts.SyntaxKind): ts.Node | undefined {
        for (let i = 0; i < node.getChildCount(this.sourceFile); i++) {
            if (node.getChildAt(i, this.sourceFile).kind == kind) {
                return node.getChildAt(i, this.sourceFile);
            }
        }
        return undefined;
    }

    private convertToken(token?: ts.Node) {
        if (token?.kind === ts.SyntaxKind.CommaToken) return {
            kind: JavaMarkers.TrailingComma,
            id: randomId(),
            space: emptySpace
        };
        if (token?.kind === ts.SyntaxKind.SemicolonToken) return {kind: JavaMarkers.Semicolon, id: randomId()};
        return undefined;
    }

    private newJEmpty(prefix: J.Space = emptySpace, markers?: Markers) {
        return {kind: J.Kind.Empty, id: randomId(), prefix: prefix, markers: markers ?? emptyMarkers};
    }

    private getOptionalUnary(node: ts.MethodSignature | ts.MethodDeclaration | ts.ParameterDeclaration | ts.PropertySignature | ts.PropertyDeclaration | ts.NamedTupleMember) {
        return JS.newUnary(
            randomId(),
            emptySpace,
            emptyMarkers,
            this.leftPadded(this.suffix(node.name), JS.Unary.Type.Optional),
            this.visit(node.name),
            this.mapType(node)
        );
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

        comments.push(J.newTextComment(isMultiline, commentBody, suffix, emptyMarkers));
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
