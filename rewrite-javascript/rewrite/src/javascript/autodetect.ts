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

import {NamedStyles, Style} from "../style";
import {randomId} from "../uuid";
import {SourceFile} from "../tree";
import {JS} from "./tree";
import {JavaScriptVisitor} from "./visitor";
import {J} from "../java";
import {
    IntelliJ,
    SpacesStyle,
    StyleKind,
    TabsAndIndentsStyle,
    WrappingAndBracesStyle,
    WrappingAndBracesStyleDetailKind
} from "./style";
import {UUID} from "../uuid";

/**
 * Auto-detected styles for JavaScript/TypeScript code.
 * Focuses on key formatting variations where projects differ:
 * - Tabs vs spaces
 * - Indent size (2, 4, etc.)
 * - Spaces within ES6 import/export braces
 */
export interface Autodetect extends NamedStyles<typeof StyleKind.Autodetect> {
    readonly kind: typeof StyleKind.Autodetect;
    readonly name: "org.openrewrite.javascript.Autodetect";
    readonly displayName: "Auto-detected";
    readonly description: "Automatically detect styles from a repository's existing code.";
}

export function autodetect(id: UUID, styles: Style[]): Autodetect {
    return {
        kind: StyleKind.Autodetect,
        id,
        name: "org.openrewrite.javascript.Autodetect",
        displayName: "Auto-detected",
        description: "Automatically detect styles from a repository's existing code.",
        tags: [],
        styles
    };
}

export namespace Autodetect {
    export function detector(): Detector {
        return new Detector();
    }
}

/**
 * Collects formatting statistics from source files and builds auto-detected styles.
 */
export class Detector {
    private readonly tabsAndIndentsStats = new TabsAndIndentsStatistics();
    private readonly spacesStats = new SpacesStatistics();
    private readonly wrappingAndBracesStats = new WrappingAndBracesStatistics();

    /**
     * Sample a source file to collect formatting statistics.
     */
    async sample(sourceFile: SourceFile): Promise<void> {
        if (sourceFile.kind === JS.Kind.CompilationUnit) {
            await this.sampleJavaScript(sourceFile as JS.CompilationUnit);
        }
    }

    /**
     * Sample a JavaScript/TypeScript compilation unit.
     */
    async sampleJavaScript(cu: JS.CompilationUnit): Promise<void> {
        await new FindIndentVisitor(this.tabsAndIndentsStats).visit(cu, {});
        await new FindSpacesVisitor(this.spacesStats).visit(cu, {});
        await new FindWrappingAndBracesVisitor(this.wrappingAndBracesStats).visit(cu, {});
    }

    /**
     * Build the auto-detected styles from collected statistics.
     */
    build(): Autodetect {
        return autodetect(randomId(), [
            this.tabsAndIndentsStats.getTabsAndIndentsStyle(),
            this.spacesStats.getSpacesStyle(),
            this.getWrappingAndBracesStyle(),
        ]);
    }

    getTabsAndIndentsStyle(): TabsAndIndentsStyle {
        return this.tabsAndIndentsStats.getTabsAndIndentsStyle();
    }

    getSpacesStyle(): SpacesStyle {
        return this.spacesStats.getSpacesStyle();
    }

    getWrappingAndBracesStyle(): WrappingAndBracesStyle {
        return this.wrappingAndBracesStats.getWrappingAndBracesStyle();
    }
}

// ============================================================================
// Statistics Classes
// ============================================================================

/**
 * Tracks indentation patterns to detect tabs vs spaces and indent size.
 */
class TabsAndIndentsStatistics {
    private totalSpaceIndents = 0;
    private totalTabIndents = 0;

    // Track all observed indent sizes to compute GCD
    private observedIndents: number[] = [];

    recordSpaceIndent(spaceCount: number): void {
        this.totalSpaceIndents++;
        if (spaceCount > 0) {
            this.observedIndents.push(spaceCount);
        }
    }

    recordTabIndent(): void {
        this.totalTabIndents++;
    }

    getTabsAndIndentsStyle(): TabsAndIndentsStyle {
        // Determine if using tabs or spaces
        const useTabs = this.totalTabIndents > this.totalSpaceIndents;

        // Find indent size by computing GCD of all observed indents
        // This correctly handles 2-space files where we see 2, 4, 6, 8... (all multiples of 2)
        let detectedIndentSize = 4; // Default
        if (this.observedIndents.length > 0) {
            // Compute GCD of all observed indents
            let gcd = this.observedIndents[0];
            for (let i = 1; i < this.observedIndents.length; i++) {
                gcd = this.computeGcd(gcd, this.observedIndents[i]);
                if (gcd === 1) break; // Can't get smaller than 1
            }
            // Only use common indent sizes (2, 4, 8)
            if (gcd === 2 || gcd === 4 || gcd === 8) {
                detectedIndentSize = gcd;
            } else if (gcd > 0 && gcd % 4 === 0) {
                detectedIndentSize = 4;
            } else if (gcd > 0 && gcd % 2 === 0) {
                detectedIndentSize = 2;
            }
        }

        return {
            kind: StyleKind.TabsAndIndentsStyle,
            useTabCharacter: useTabs,
            tabSize: 4,
            indentSize: detectedIndentSize,
            continuationIndent: detectedIndentSize * 2,
            keepIndentsOnEmptyLines: false,
            indentChainedMethods: true,
            indentAllChainedCallsInAGroup: false
        };
    }

    private computeGcd(a: number, b: number): number {
        while (b !== 0) {
            const temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }
}

/**
 * Tracks spacing patterns around braces and other constructs.
 */
class SpacesStatistics {
    // Track spaces within ES6 import/export braces: { a } vs {a}
    es6ImportExportBracesWithSpace = 0;
    es6ImportExportBracesWithoutSpace = 0;

    // Track spaces within object literal braces: { foo: 1 } vs {foo: 1}
    objectLiteralBracesWithSpace = 0;
    objectLiteralBracesWithoutSpace = 0;

    // Track spaces within object literal type braces: { foo: string } vs {foo: string}
    objectLiteralTypeBracesWithSpace = 0;
    objectLiteralTypeBracesWithoutSpace = 0;

    getSpacesStyle(): SpacesStyle {
        // Use TypeScript defaults as base since most modern JS/TS projects use similar conventions
        // TypeScript defaults include afterTypeReferenceColon: true which is commonly expected
        const defaults = IntelliJ.TypeScript.spaces();

        return {
            ...defaults,
            within: {
                ...defaults.within,
                es6ImportExportBraces: this.es6ImportExportBracesWithSpace > this.es6ImportExportBracesWithoutSpace,
                objectLiteralBraces: this.objectLiteralBracesWithSpace > this.objectLiteralBracesWithoutSpace,
                objectLiteralTypeBraces: this.objectLiteralTypeBracesWithSpace > this.objectLiteralTypeBracesWithoutSpace
            }
        };
    }
}

/**
 * Tracks wrapping and braces patterns for simple (empty) blocks and methods.
 */
class WrappingAndBracesStatistics {
    // Track simple blocks (not method/function bodies): {} vs {\n}
    simpleBlocksOnOneLine = 0;
    simpleBlocksOnMultipleLines = 0;

    // Track simple method/function bodies: {} vs {\n}
    simpleMethodsOnOneLine = 0;
    simpleMethodsOnMultipleLines = 0;

    recordSimpleBlock(isOnOneLine: boolean): void {
        if (isOnOneLine) {
            this.simpleBlocksOnOneLine++;
        } else {
            this.simpleBlocksOnMultipleLines++;
        }
    }

    recordSimpleMethod(isOnOneLine: boolean): void {
        if (isOnOneLine) {
            this.simpleMethodsOnOneLine++;
        } else {
            this.simpleMethodsOnMultipleLines++;
        }
    }

    getWrappingAndBracesStyle(): WrappingAndBracesStyle {
        return {
            kind: StyleKind.WrappingAndBracesStyle,
            ifStatement: {
                kind: WrappingAndBracesStyleDetailKind.WrappingAndBracesStyleIfStatement,
                elseOnNewLine: false
            },
            keepWhenReformatting: {
                kind: WrappingAndBracesStyleDetailKind.WrappingAndBracesStyleKeepWhenReformatting,
                simpleBlocksInOneLine: this.simpleBlocksOnOneLine > this.simpleBlocksOnMultipleLines,
                simpleMethodsInOneLine: this.simpleMethodsOnOneLine > this.simpleMethodsOnMultipleLines
            }
        };
    }
}

// ============================================================================
// Visitor Classes for Collecting Statistics
// ============================================================================

/**
 * Detects indentation patterns by examining block contents.
 */
class FindIndentVisitor extends JavaScriptVisitor<any> {
    constructor(private stats: TabsAndIndentsStatistics) {
        super();
    }

    protected async visitBlock(block: J.Block, p: any): Promise<J | undefined> {
        // Check indentation of statements in the block
        // With intersection types, stmt IS the statement with padding mixed in
        for (const stmt of block.statements) {
            const whitespace = stmt.prefix?.whitespace;
            if (whitespace) {
                this.analyzeIndent(whitespace);
            }
        }
        return super.visitBlock(block, p);
    }

    private analyzeIndent(whitespace: string): void {
        const newlineIndex = whitespace.lastIndexOf('\n');
        if (newlineIndex < 0) return;

        const indent = whitespace.substring(newlineIndex + 1);
        if (indent.length === 0) return;

        // Check first character to determine type
        if (indent[0] === '\t') {
            this.stats.recordTabIndent();
        } else if (indent[0] === ' ') {
            // Count consecutive spaces
            let spaceCount = 0;
            for (const char of indent) {
                if (char === ' ') spaceCount++;
                else break;
            }
            if (spaceCount > 0) {
                this.stats.recordSpaceIndent(spaceCount);
            }
        }
    }
}

/**
 * Detects spacing patterns in imports and exports.
 */
class FindSpacesVisitor extends JavaScriptVisitor<any> {
    constructor(private stats: SpacesStatistics) {
        super();
    }

    protected async visitImportDeclaration(import_: JS.Import, p: any): Promise<J | undefined> {
        // Check ES6 import braces spacing: import { a } from 'x' vs import {a} from 'x'
        if (import_.importClause?.namedBindings?.kind === JS.Kind.NamedImports) {
            const namedImports = import_.importClause.namedBindings as JS.NamedImports;
            if (namedImports.elements.elements.length > 0) {
                // With intersection types, firstElement IS the specifier with padding mixed in
                const firstElement = namedImports.elements.elements[0];
                const hasSpaceAfterOpenBrace = firstElement.prefix?.whitespace?.includes(' ') ?? false;

                const lastElement = namedImports.elements.elements[namedImports.elements.elements.length - 1];
                const hasSpaceBeforeCloseBrace = lastElement.padding.after?.whitespace?.includes(' ') ?? false;

                if (hasSpaceAfterOpenBrace || hasSpaceBeforeCloseBrace) {
                    this.stats.es6ImportExportBracesWithSpace++;
                } else {
                    this.stats.es6ImportExportBracesWithoutSpace++;
                }
            }
        }
        return super.visitImportDeclaration(import_, p);
    }

    protected async visitExportDeclaration(export_: JS.ExportDeclaration, p: any): Promise<J | undefined> {
        // Check ES6 export braces spacing
        if (export_.exportClause?.kind === JS.Kind.NamedExports) {
            const namedExports = export_.exportClause as JS.NamedExports;
            if (namedExports.elements.elements.length > 0) {
                // With intersection types, firstElement IS the specifier with padding mixed in
                const firstElement = namedExports.elements.elements[0];
                const hasSpaceAfterOpenBrace = firstElement.prefix?.whitespace?.includes(' ') ?? false;

                const lastElement = namedExports.elements.elements[namedExports.elements.elements.length - 1];
                const hasSpaceBeforeCloseBrace = lastElement.padding.after?.whitespace?.includes(' ') ?? false;

                if (hasSpaceAfterOpenBrace || hasSpaceBeforeCloseBrace) {
                    this.stats.es6ImportExportBracesWithSpace++;
                } else {
                    this.stats.es6ImportExportBracesWithoutSpace++;
                }
            }
        }
        return super.visitExportDeclaration(export_, p);
    }

    protected async visitNewClass(newClass: J.NewClass, p: any): Promise<J | undefined> {
        // Only handle object literals (NewClass with no class/constructor)
        if (!newClass.class && newClass.body && newClass.body.statements.length > 0) {
            const stmts = newClass.body.statements;

            // Check if single-line (no newlines in any element prefix or in end)
            // With intersection types, stmt IS the statement with padding mixed in
            const isMultiLine = stmts.some(s => s.prefix?.whitespace?.includes('\n')) ||
                newClass.body.end?.whitespace?.includes('\n');

            if (!isMultiLine) {
                const firstElement = stmts[0];
                const hasSpaceAfterOpenBrace = firstElement.prefix?.whitespace?.includes(' ') ?? false;

                // For object literals, the space before } is in body.end, not in last statement's after
                const hasSpaceBeforeCloseBrace = newClass.body.end?.whitespace?.includes(' ') ?? false;

                if (hasSpaceAfterOpenBrace || hasSpaceBeforeCloseBrace) {
                    this.stats.objectLiteralBracesWithSpace++;
                } else {
                    this.stats.objectLiteralBracesWithoutSpace++;
                }
            }
        }
        return super.visitNewClass(newClass, p);
    }

    protected async visitTypeLiteral(typeLiteral: JS.TypeLiteral, p: any): Promise<J | undefined> {
        // Check type literal braces spacing: { foo: string } vs {foo: string}
        if (typeLiteral.members && typeLiteral.members.statements.length > 0) {
            const stmts = typeLiteral.members.statements;

            // Check if single-line (no newlines in any element prefix or in end)
            // With intersection types, stmt IS the statement with padding mixed in
            const isMultiLine = stmts.some(s => s.prefix?.whitespace?.includes('\n')) ||
                typeLiteral.members.end?.whitespace?.includes('\n');

            if (!isMultiLine) {
                const firstElement = stmts[0];
                const hasSpaceAfterOpenBrace = firstElement.prefix?.whitespace?.includes(' ') ?? false;

                // For type literals, the space before } is in members.end, not in last statement's after
                const hasSpaceBeforeCloseBrace = typeLiteral.members.end?.whitespace?.includes(' ') ?? false;

                if (hasSpaceAfterOpenBrace || hasSpaceBeforeCloseBrace) {
                    this.stats.objectLiteralTypeBracesWithSpace++;
                } else {
                    this.stats.objectLiteralTypeBracesWithoutSpace++;
                }
            }
        }
        return super.visitTypeLiteral(typeLiteral, p);
    }
}

/**
 * Detects wrapping and braces patterns for simple (empty) blocks.
 */
class FindWrappingAndBracesVisitor extends JavaScriptVisitor<any> {
    constructor(private stats: WrappingAndBracesStatistics) {
        super();
    }

    protected async visitBlock(block: J.Block, p: any): Promise<J | undefined> {
        // Check if this is a simple block (empty or contains only J.Empty)
        // With intersection types, stmt IS the statement with padding mixed in
        const isSimple = block.statements.length === 0 ||
            (block.statements.length === 1 && block.statements[0].kind === J.Kind.Empty);

        if (isSimple) {
            // Determine if block is on one line by checking for newlines
            const hasNewlineInEnd = block.end?.whitespace?.includes('\n') ?? false;
            const hasNewlineInStatements = block.statements.length > 0 &&
                (block.statements[0].prefix?.whitespace?.includes('\n') ||
                 block.statements[0].padding.after?.whitespace?.includes('\n'));

            const isOnOneLine = !hasNewlineInEnd && !hasNewlineInStatements;

            // Determine parent kind to classify as block or method
            const parent = this.cursor.parent?.value;
            const isMethodOrFunctionBody = parent?.kind === J.Kind.Lambda ||
                parent?.kind === J.Kind.MethodDeclaration;

            if (isMethodOrFunctionBody) {
                this.stats.recordSimpleMethod(isOnOneLine);
            } else {
                // Skip object literals and type literals
                if (parent?.kind !== J.Kind.NewClass && parent?.kind !== JS.Kind.TypeLiteral) {
                    this.stats.recordSimpleBlock(isOnOneLine);
                }
            }
        }

        return super.visitBlock(block, p);
    }
}
