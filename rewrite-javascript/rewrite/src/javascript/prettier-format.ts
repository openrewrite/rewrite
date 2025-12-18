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
import {J, Statement} from '../java';
import {Cursor} from '../tree';
import {TreePrinters} from '../print';
import {JavaScriptParser} from './parser';
import {WhitespaceReconcilerVisitor} from './whitespace-reconciler';
import {produce} from 'immer';
import {randomId} from '../uuid';

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

/**
 * Represents a segment of the path from root to a target node.
 */
interface PathSegment {
    /** The property name containing the child */
    property: string;
    /** For array properties, the index of the element */
    index?: number;
}

/**
 * Result of extracting a path from cursor.
 */
interface PathExtractionResult {
    /** The compilation unit (root of the tree) */
    compilationUnit: JS.CompilationUnit | undefined;
    /** The path from root to target */
    path: PathSegment[];
}

/**
 * Extracts the path from a CompilationUnit to a target node using the cursor.
 * Returns the path segments in order from root to target.
 *
 * @param cursor The cursor, which may not include the target (e.g., when passing cursor.parent)
 * @param target The target node we're looking for
 */
function extractPathFromCursor(cursor: Cursor, target: any): PathExtractionResult {
    const pathNodes = cursor.asArray().reverse(); // root to target
    const segments: PathSegment[] = [];
    let compilationUnit: JS.CompilationUnit | undefined;

    // Helper to check if two nodes are the same (by identity or ID)
    const isSameNode = (a: any, b: any): boolean => {
        if (a === b) return true;
        if (a && b && typeof a === 'object' && typeof b === 'object' && 'id' in a && 'id' in b) {
            return a.id === b.id;
        }
        return false;
    };

    // Helper to find a child in a parent and return the segment
    const findChildInParent = (parent: any, child: any): PathSegment | undefined => {
        if (!parent || typeof parent !== 'object') return undefined;

        for (const key of Object.keys(parent)) {
            const value = (parent as any)[key];
            if (value == null) continue;

            if (Array.isArray(value)) {
                for (let idx = 0; idx < value.length; idx++) {
                    const item = value[idx];
                    if (isSameNode(item, child)) {
                        return { property: key, index: idx };
                    }
                }
            } else if (isSameNode(value, child)) {
                return { property: key };
            }
        }
        return undefined;
    };

    for (let i = 0; i < pathNodes.length - 1; i++) {
        const parent = pathNodes[i];
        const child = pathNodes[i + 1];

        // Check if this node is the CompilationUnit
        if (parent?.kind === JS.Kind.CompilationUnit) {
            compilationUnit = parent as JS.CompilationUnit;
        }

        const segment = findChildInParent(parent, child);
        if (segment) {
            segments.push(segment);
        }
    }

    // Check the last node for CompilationUnit
    const lastNode = pathNodes[pathNodes.length - 1];
    if (lastNode?.kind === JS.Kind.CompilationUnit) {
        compilationUnit = lastNode as JS.CompilationUnit;
    }

    // If the cursor doesn't include the target, add the final segment
    // This handles the case when autoFormat is called with cursor.parent
    if (lastNode && !isSameNode(lastNode, target)) {
        const finalSegment = findChildInParent(lastNode, target);
        if (finalSegment) {
            segments.push(finalSegment);
        }
    }

    return { compilationUnit, path: segments };
}

/**
 * Creates a "null" identifier placeholder for use in pruned trees.
 * Using "null" instead of an empty statement ensures Prettier sees similar
 * line lengths and doesn't collapse multi-line code to single-line.
 */
function createNullPlaceholder(prefix: J.Space): J.Identifier {
    return {
        kind: J.Kind.Identifier,
        id: randomId(),
        markers: { kind: "org.openrewrite.marker.Markers", id: randomId(), markers: [] },
        prefix: prefix,
        annotations: [],
        simpleName: "null",
        type: undefined,
        fieldType: undefined
    };
}

/**
 * Prunes a compilation unit for efficient Prettier formatting of a subtree.
 *
 * For J.Block#statements along the path to the target:
 * - Prior siblings are replaced with "null" identifier placeholders (to maintain line length)
 * - Following siblings are omitted entirely
 *
 * This optimization reduces the amount of code Prettier needs to process
 * while maintaining approximate line positions so Prettier doesn't collapse
 * multi-line code.
 *
 * @param cu The compilation unit to prune
 * @param path The path from root to the target subtree
 * @returns A pruned copy of the compilation unit
 */
function pruneTreeForSubtree(cu: JS.CompilationUnit, path: PathSegment[]): JS.CompilationUnit {
    return pruneNode(cu, path, 0) as JS.CompilationUnit;
}

/**
 * Recursively prunes a node, following the path and pruning J.Block#statements.
 */
function pruneNode(node: any, path: PathSegment[], pathIndex: number): any {
    if (pathIndex >= path.length) {
        // Reached the target - return as-is
        return node;
    }

    const segment = path[pathIndex];
    const value = node[segment.property];

    if (value == null) {
        return node;
    }

    // Handle J.Block#statements specially
    if (node.kind === J.Kind.Block && segment.property === 'statements' && segment.index !== undefined) {
        const statements = value as J.RightPadded<Statement>[];
        const targetIndex = segment.index;

        // Create pruned statements array:
        // - Prior siblings: replace with "null" placeholders (to maintain line length)
        // - Target: recurse into it
        // - Following siblings: omit entirely
        const prunedStatements: J.RightPadded<Statement>[] = [];

        for (let i = 0; i <= targetIndex; i++) {
            if (i < targetIndex) {
                // Prior sibling - replace with "null" placeholder
                // Preserve the original prefix to maintain line positions
                const originalPrefix = statements[i].element.prefix;
                const placeholder = createNullPlaceholder(originalPrefix);
                prunedStatements.push({
                    kind: J.Kind.RightPadded,
                    element: placeholder,
                    after: statements[i].after,
                    markers: statements[i].markers
                } as J.RightPadded<Statement>);
            } else {
                // Target - recurse into it
                const targetStatement = statements[i];
                const prunedElement = pruneNode(targetStatement.element, path, pathIndex + 1);
                prunedStatements.push({
                    ...targetStatement,
                    element: prunedElement
                });
            }
        }
        // Following siblings are omitted

        return produce(node, (draft: any) => {
            draft.statements = prunedStatements;
        });
    }

    // For other properties, just recurse without pruning
    if (Array.isArray(value) && segment.index !== undefined) {
        const childNode = value[segment.index];
        const prunedChild = pruneNode(childNode, path, pathIndex + 1);

        if (prunedChild !== childNode) {
            return produce(node, (draft: any) => {
                draft[segment.property][segment.index!] = prunedChild;
            });
        }
    } else if (!Array.isArray(value)) {
        const prunedChild = pruneNode(value, path, pathIndex + 1);

        if (prunedChild !== value) {
            return produce(node, (draft: any) => {
                draft[segment.property] = prunedChild;
            });
        }
    }

    return node;
}

/**
 * Finds a node in a tree by following a path of segments.
 * Used to locate the target node in the formatted tree.
 *
 * For block statements, the target is always at the last index since
 * following siblings are omitted during pruning.
 */
function findByPath(tree: any, path: PathSegment[]): any {
    let current = tree;

    for (const segment of path) {
        if (current == null) return undefined;

        const value = current[segment.property];
        if (value == null) return undefined;

        if (Array.isArray(value) && segment.index !== undefined) {
            // For block statements, target is always at the last index
            // since following siblings are omitted during pruning
            const isBlockStatements = current.kind === J.Kind.Block && segment.property === 'statements';
            const index = isBlockStatements ? value.length - 1 : segment.index;
            const item = value[index];
            if (item == null) return undefined;
            current = item;
        } else {
            current = value;
        }
    }

    return current;
}

/**
 * Formats a subtree of a JavaScript/TypeScript AST using Prettier.
 *
 * This function is optimized for formatting a small part of a larger tree:
 * 1. Extracts the path from compilation unit to target
 * 2. Prunes the tree (replaces siblings with placeholders)
 * 3. Formats the pruned tree with Prettier
 * 4. Finds the target in the formatted tree
 * 5. Reconciles only the target subtree's whitespace
 *
 * @param target The subtree to format
 * @param cursor The cursor pointing to or near the target
 * @param options Prettier formatting options
 * @returns The formatted subtree, or undefined if formatting failed
 */
export async function prettierFormatSubtree<T extends J>(
    target: T,
    cursor: Cursor,
    options: PrettierFormatOptions = {}
): Promise<T | undefined> {
    // Extract the path and compilation unit in a single cursor traversal
    const { compilationUnit: cu, path } = extractPathFromCursor(cursor, target);

    if (!cu) {
        return undefined;
    }

    // Prune the tree for efficient formatting
    const prunedCu = pruneTreeForSubtree(cu, path);

    // Format the pruned compilation unit with Prettier
    const formattedPrunedCu = await prettierFormat(prunedCu, options);

    // Find the target node in the formatted tree using the path
    const formattedTarget = findByPath(formattedPrunedCu, path);
    if (!formattedTarget) {
        return undefined;
    }

    // Reconcile only the target subtree
    const reconciler = new WhitespaceReconcilerVisitor();
    const reconciled = await reconciler.reconcile(target as J, formattedTarget as J);

    return reconciled as T;
}
