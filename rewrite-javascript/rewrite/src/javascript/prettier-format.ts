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
import {JS} from './tree';
import {J} from '../java';
import {TreePrinters} from '../print';
import {JavaScriptParser} from './parser';
import {WhitespaceReconcilerVisitor} from './whitespace-reconciler';

/**
 * Options for Prettier formatting.
 */
export interface PrettierFormatOptions {
    /**
     * Tab width for indentation. Defaults to 2.
     */
    tabWidth?: number;

    /**
     * Use tabs instead of spaces. Defaults to false.
     */
    useTabs?: boolean;

    /**
     * Print semicolons at the ends of statements. Defaults to true.
     */
    semi?: boolean;

    /**
     * Use single quotes instead of double quotes. Defaults to false.
     */
    singleQuote?: boolean;

    /**
     * Print trailing commas wherever possible. Defaults to 'all'.
     */
    trailingComma?: 'all' | 'es5' | 'none';

    /**
     * Print width for line wrapping. Defaults to 80.
     */
    printWidth?: number;
}

/**
 * Formats a JavaScript/TypeScript AST using Prettier.
 *
 * This function:
 * 1. Prints the AST to a string
 * 2. Formats the string using Prettier
 * 3. Parses the formatted string back to an AST (without type attribution for performance)
 * 4. Reconciles the whitespace from the formatted AST back into the original AST
 *
 * The result preserves the original AST's structure, types, and markers while
 * applying Prettier's formatting rules for whitespace.
 *
 * @param sourceFile The source file to format
 * @param options Prettier formatting options
 * @returns The formatted source file with reconciled whitespace
 */
export async function prettierFormat(
    sourceFile: JS.CompilationUnit,
    options: PrettierFormatOptions = {}
): Promise<JS.CompilationUnit> {
    // Dynamically import prettier standalone (avoids dynamic import issues with Prettier 3.x)
    let prettier: typeof import('prettier/standalone');
    let prettierPlugins: any[];
    try {
        // Use require for better compatibility with Jest and CommonJS environments
        // eslint-disable-next-line @typescript-eslint/no-require-imports
        prettier = require('prettier/standalone');
        // eslint-disable-next-line @typescript-eslint/no-require-imports
        const parserBabel = require('prettier/plugins/babel');
        // eslint-disable-next-line @typescript-eslint/no-require-imports
        const parserTypescript = require('prettier/plugins/typescript');
        // eslint-disable-next-line @typescript-eslint/no-require-imports
        const parserEstree = require('prettier/plugins/estree');
        prettierPlugins = [parserBabel, parserTypescript, parserEstree];
    } catch (e) {
        throw new Error(
            'Prettier is not installed. Please install it with: npm install prettier'
        );
    }

    // Step 1: Print the AST to string
    const originalSource = await TreePrinters.print(sourceFile);

    // Step 2: Determine parser based on source path
    const parser = getParserForPath(sourceFile.sourcePath);

    // Step 3: Format with Prettier
    const prettierOptions = {
        parser,
        plugins: prettierPlugins,
        tabWidth: options.tabWidth ?? 2,
        useTabs: options.useTabs ?? false,
        semi: options.semi ?? true,
        singleQuote: options.singleQuote ?? false,
        trailingComma: options.trailingComma ?? 'all',
        printWidth: options.printWidth ?? 80,
    };

    const formattedSource = await prettier.format(originalSource, prettierOptions);

    // Step 4: Parse the formatted string (skip types for performance)
    const formattedParser = new JavaScriptParser({skipTypes: true});
    let formattedAst: JS.CompilationUnit | undefined;

    for await (const parsed of formattedParser.parse({
        sourcePath: sourceFile.sourcePath,
        text: formattedSource
    })) {
        formattedAst = parsed as JS.CompilationUnit;
        break;
    }

    if (!formattedAst) {
        // If parsing fails, return original unchanged
        console.warn('Failed to parse Prettier output, returning original');
        return sourceFile;
    }

    // Step 5: Reconcile whitespace from formatted AST to original AST
    const reconciler = new WhitespaceReconcilerVisitor();
    const result = await reconciler.reconcile(sourceFile, formattedAst);

    return result as JS.CompilationUnit;
}

/**
 * Formats a JavaScript/TypeScript expression or statement using Prettier.
 *
 * This wraps the expression in a minimal source file, formats it, and extracts
 * the result. Useful for formatting code snippets.
 *
 * @param node The node to format
 * @param options Prettier formatting options
 * @returns The formatted node with reconciled whitespace
 */
export async function prettierFormatNode<T extends J>(
    node: T,
    options: PrettierFormatOptions = {}
): Promise<T> {
    // Dynamically import prettier standalone (avoids dynamic import issues with Prettier 3.x)
    let prettier: typeof import('prettier/standalone');
    let prettierPlugins: any[];
    try {
        // Use require for better compatibility with Jest and CommonJS environments
        // eslint-disable-next-line @typescript-eslint/no-require-imports
        prettier = require('prettier/standalone');
        // eslint-disable-next-line @typescript-eslint/no-require-imports
        const parserBabel = require('prettier/plugins/babel');
        // eslint-disable-next-line @typescript-eslint/no-require-imports
        const parserEstree = require('prettier/plugins/estree');
        prettierPlugins = [parserBabel, parserEstree];
    } catch (e) {
        throw new Error(
            'Prettier is not installed. Please install it with: npm install prettier'
        );
    }

    // Print the node to string using the JS printer
    const originalSource = await TreePrinters.printer(JS.Kind.CompilationUnit).print(node);

    // Format with Prettier using babel parser for snippets
    const prettierOptions = {
        parser: 'babel',
        plugins: prettierPlugins,
        tabWidth: options.tabWidth ?? 2,
        useTabs: options.useTabs ?? false,
        semi: options.semi ?? true,
        singleQuote: options.singleQuote ?? false,
        trailingComma: options.trailingComma ?? 'all',
        printWidth: options.printWidth ?? 80,
    };

    const formattedSource = await prettier.format(originalSource, prettierOptions);

    // Parse the formatted string
    const formattedParser = new JavaScriptParser({skipTypes: true});
    let formattedAst: JS.CompilationUnit | undefined;

    for await (const parsed of formattedParser.parse({
        sourcePath: 'snippet.js',
        text: formattedSource
    })) {
        formattedAst = parsed as JS.CompilationUnit;
        break;
    }

    if (!formattedAst) {
        return node;
    }

    // Extract the first statement/expression from the formatted AST
    // This assumes the node was a single statement/expression
    const formattedNode = extractFirstNode(formattedAst);
    if (!formattedNode || formattedNode.kind !== node.kind) {
        return node;
    }

    // Reconcile whitespace
    const reconciler = new WhitespaceReconcilerVisitor();
    const result = await reconciler.reconcile(node, formattedNode as J);

    return result as T;
}

/**
 * Determines the Prettier parser to use based on file extension.
 */
function getParserForPath(path: string): string {
    const lower = path.toLowerCase();
    if (lower.endsWith('.tsx')) return 'typescript';
    if (lower.endsWith('.ts')) return 'typescript';
    if (lower.endsWith('.jsx')) return 'babel';
    if (lower.endsWith('.mjs')) return 'babel';
    if (lower.endsWith('.cjs')) return 'babel';
    return 'babel';
}

/**
 * Extracts the first meaningful node from a compilation unit.
 */
function extractFirstNode(cu: JS.CompilationUnit): J | undefined {
    if (cu.statements.length === 0) return undefined;
    return cu.statements[0].element;
}
