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
    // Dynamically load prettier standalone
    // Using require() for compatibility with Jest/CommonJS environments
    let prettier: typeof import('prettier/standalone');
    let prettierPlugins: any[];
    try {
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
        console.error('Failed to load Prettier:', e);
        throw new Error(
            `Prettier is not installed or failed to load. Please install it with: npm install prettier. Error: ${e}`
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
    const formattedAst = await formattedParser.parseOne({
        sourcePath: sourceFile.sourcePath,
        text: formattedSource
    }) as JS.CompilationUnit;

    // Step 5: Reconcile whitespace from formatted AST to original AST
    // Note: For subtree formatting with pruned trees, the structure may differ
    // (e.g., Prettier removes empty placeholder statements). In such cases,
    // we return the formatted AST directly and let the caller handle
    // subtree-level reconciliation.
    const reconciler = new WhitespaceReconcilerVisitor();
    const result = await reconciler.reconcile(sourceFile, formattedAst);

    // If reconciliation succeeded, return the reconciled original with updated whitespace
    // If it failed (structure mismatch), return the formatted AST for subtree reconciliation
    return reconciler.isCompatible() ? result as JS.CompilationUnit : formattedAst;
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
