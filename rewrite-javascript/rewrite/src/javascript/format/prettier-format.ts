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
import {JS} from '../tree';
import {J, Statement} from '../../java';
import {Cursor, Tree} from '../../tree';
import {TreePrinters} from '../../print';
import {JavaScriptParser} from '../parser';
import {WhitespaceReconciler} from './whitespace-reconciler';
import {randomId} from '../../uuid';
import {PrettierStyle, StyleKind} from '../style';
import {NamedStyles} from '../../style';
import {emptyMarkers, findMarker} from '../../markers';
import {NormalizeWhitespaceVisitor} from './normalize-whitespace-visitor';
import {MinimumViableSpacingVisitor} from './minimum-viable-spacing-visitor';
import {loadPrettierVersion} from './prettier-config-loader';
import {updateIfChanged} from "../../util";

/**
 * Loads Prettier for formatting.
 *
 * We use the main Prettier module (not standalone) because:
 * 1. It automatically handles parser resolution
 * 2. Works better with CommonJS (avoids ESM issues in Jest)
 * 3. Simpler - no need to manually load plugins
 */
async function loadPrettierFormatting(version?: string): Promise<typeof import('prettier')> {
    if (version) {
        // Ensure the version is installed and get it from cache
        return await loadPrettierVersion(version);
    }

    // Use bundled Prettier
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    return require('prettier');
}

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

    /**
     * Change when properties in objects are quoted.
     * - "as-needed" - Only add quotes around object properties where required.
     * - "consistent" - If at least one property in an object requires quotes, quote all properties.
     * - "preserve" - Respect the input use of quotes in object properties.
     * Defaults to "as-needed".
     */
    quoteProps?: 'as-needed' | 'consistent' | 'preserve';

    /**
     * The Prettier version to use (e.g., "3.4.2").
     * If specified, loads that version from cache or installs it.
     * If not specified, uses the bundled Prettier.
     */
    prettierVersion?: string;
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
 * @param stopAfter Optional node to stop formatting after. Once this node is exited,
 *                  no more whitespace changes are applied to subsequent nodes.
 * @returns The formatted source file with reconciled whitespace
 */
export async function prettierFormat(
    sourceFile: JS.CompilationUnit,
    options: PrettierFormatOptions = {},
    stopAfter?: J
): Promise<JS.CompilationUnit> {
    // Load Prettier - either specific version or bundled
    let prettier: typeof import('prettier');

    try {
        prettier = await loadPrettierFormatting(options.prettierVersion);
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
    // Using the main Prettier module - parsers are resolved automatically
    // Only set parser and filepath - pass through all other options without defaults
    // This lets Prettier use its own defaults for any unspecified options
    const prettierOptions: Record<string, unknown> = {
        parser,
        filepath: sourceFile.sourcePath,  // Important: tells Prettier the file type for proper formatting
        ...options,
    };
    // Remove our internal option that Prettier doesn't understand
    delete prettierOptions.prettierVersion;

    const formattedSource = await prettier.format(originalSource, prettierOptions);

    // Step 4: Parse the formatted string using parseOnly() for maximum performance
    // (bypasses TypeScript's type checker entirely)
    const formattedParser = new JavaScriptParser();
    const formattedAst = await formattedParser.parseOnly({
        sourcePath: sourceFile.sourcePath,
        text: formattedSource
    });

    if (!formattedAst) {
        console.warn('Prettier formatting: Failed to parse formatted output, returning original');
        return sourceFile;
    }

    // Step 5: Reconcile whitespace from formatted AST to original AST
    // Note: For subtree formatting with pruned trees, the structure may differ
    // (e.g., Prettier removes empty placeholder statements). In such cases,
    // we return the formatted AST directly and let the caller handle
    // subtree-level reconciliation.
    const reconciler = new WhitespaceReconciler();
    const formattedCu = formattedAst as JS.CompilationUnit;
    const result = reconciler.reconcile(sourceFile, formattedCu, undefined, stopAfter);

    // If reconciliation succeeded, return the reconciled original with updated whitespace
    // If it failed (structure mismatch), return the formatted AST for subtree reconciliation
    return reconciler.isCompatible() ? result as JS.CompilationUnit : formattedCu;
}

/**
 * Maps file extensions to Prettier parser names.
 */
const PRETTIER_PARSER_BY_EXTENSION: Record<string, string> = {
    '.ts': 'typescript',
    '.tsx': 'typescript',
    '.js': 'babel',
    '.jsx': 'babel',
    '.mjs': 'babel',
    '.cjs': 'babel',
};

/**
 * Determines the Prettier parser to use based on file extension.
 */
function getParserForPath(filePath: string): string {
    const lower = filePath.toLowerCase();
    for (const [ext, parser] of Object.entries(PRETTIER_PARSER_BY_EXTENSION)) {
        if (lower.endsWith(ext)) {
            return parser;
        }
    }
    return 'babel'; // Default parser
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
        markers: emptyMarkers,
        prefix: prefix,
        annotations: [],
        simpleName: "null",
        type: undefined,
        fieldType: undefined
    };
}

/**
 * Prunes a compilation unit for efficient Prettier formatting of a subtree,
 * and substitutes the (potentially modified) target at the path location.
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
 * @param target The (potentially modified) target to substitute at the path location
 * @returns A pruned copy of the compilation unit with the target substituted
 */
function pruneTreeForSubtree(cu: JS.CompilationUnit, path: PathSegment[], target: any): JS.CompilationUnit {
    return pruneNode(cu, path, 0, target) as JS.CompilationUnit;
}

/**
 * Recursively prunes a node, following the path, pruning J.Block#statements,
 * and substituting the target at the final location.
 */
function pruneNode(node: any, path: PathSegment[], pathIndex: number, target: any): any {
    if (pathIndex >= path.length) {
        // Reached the target location - substitute with the (potentially modified) target
        return target;
    }

    const segment = path[pathIndex];
    const value = node[segment.property];

    if (value == null) {
        return node;
    }

    // Handle J.Block#statements specially - prune siblings
    if (node.kind === J.Kind.Block && segment.property === 'statements' && segment.index !== undefined) {
        const statements = value as J.RightPadded<Statement>[];
        const targetIndex = segment.index;

        // Create pruned statements array:
        // - Prior siblings: replace with "null" placeholders (to maintain line length)
        // - Target: recurse into it (following the path through RightPadded.element)
        // - Following siblings: omit entirely
        const prunedStatements: J.RightPadded<Statement>[] = [];

        for (let i = 0; i <= targetIndex; i++) {
            if (i < targetIndex) {
                // Prior sibling - replace with "null" placeholder
                // Preserve the original prefix to maintain line positions
                // For tree types, the padded value IS the element (intersection type)
                const originalPrefix = statements[i].prefix;
                const placeholder = createNullPlaceholder(originalPrefix);
                // Use spread to merge placeholder with padding properties
                prunedStatements.push({
                    ...placeholder,
                    padding: statements[i].padding
                } as J.RightPadded<Statement>);
            } else {
                // Target - recurse into the RightPadded (path will handle .element)
                const prunedRightPadded = pruneNode(statements[i], path, pathIndex + 1, target);
                prunedStatements.push(prunedRightPadded);
            }
        }
        // Following siblings are omitted

        return updateIfChanged(node, {statements: prunedStatements});
    }

    // For other properties, just recurse without pruning
    if (Array.isArray(value) && segment.index !== undefined) {
        const childNode = value[segment.index];
        const prunedChild = pruneNode(childNode, path, pathIndex + 1, target);

        if (prunedChild !== childNode) {
            // Create a copy of the array with the updated element
            const newArray = [...value];
            newArray[segment.index] = prunedChild;
            return { ...node, [segment.property]: newArray };
        }
    } else if (!Array.isArray(value)) {
        const prunedChild = pruneNode(value, path, pathIndex + 1, target);

        if (prunedChild !== value) {
            return { ...node, [segment.property]: prunedChild };
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
 * @param stopAfter Optional node to stop formatting after
 * @returns The formatted subtree, or undefined if formatting failed
 */
export async function prettierFormatSubtree<T extends J>(
    target: T,
    cursor: Cursor,
    options: PrettierFormatOptions = {},
    stopAfter?: J
): Promise<T | undefined> {
    // Extract the path and compilation unit in a single cursor traversal
    const { compilationUnit: cu, path } = extractPathFromCursor(cursor, target);

    if (!cu) {
        return undefined;
    }

    // Prune the tree for efficient formatting and substitute the (potentially modified) target.
    // This ensures that if the visitor modified the target before calling autoFormat,
    // we format the modified content, not the original from the cursor.
    const prunedCu = pruneTreeForSubtree(cu, path, target);

    // Format the pruned compilation unit with Prettier
    const formattedPrunedCu = await prettierFormat(prunedCu, options);

    // Find the target node in the formatted tree using the path
    const formattedTarget = findByPath(formattedPrunedCu, path);
    if (!formattedTarget) {
        return undefined;
    }

    // Reconcile only the target subtree, optionally stopping after a specific node
    const reconciler = new WhitespaceReconciler();
    const reconciled = reconciler.reconcile(target as J, formattedTarget as J, undefined, stopAfter);

    return reconciled as T;
}

/**
 * Gets the PrettierStyle from the styles array or source file markers.
 *
 * @param tree The tree being formatted
 * @param cursor Optional cursor for walking up to find source file
 * @param styles Optional styles array to check first
 * @returns PrettierStyle if found, undefined otherwise
 */
export function getPrettierStyle(
    tree: Tree,
    cursor?: Cursor,
    styles?: NamedStyles<string>[]
): PrettierStyle | undefined {
    // First check the styles array
    if (styles) {
        const fromStyles = styles.find(s => (s as any).kind === StyleKind.PrettierStyle);
        if (fromStyles) {
            return fromStyles as unknown as PrettierStyle;
        }
    }

    // Then check for PrettierStyle marker on source file
    let sourceFile: JS.CompilationUnit | undefined;

    if (tree.kind === JS.Kind.CompilationUnit) {
        sourceFile = tree as JS.CompilationUnit;
    } else if (cursor) {
        // Walk up the cursor to find the compilation unit
        let current: Cursor | undefined = cursor;
        while (current) {
            if (current.value?.kind === JS.Kind.CompilationUnit) {
                sourceFile = current.value as JS.CompilationUnit;
                break;
            }
            current = current.parent;
        }
    }

    if (!sourceFile) {
        return undefined;
    }

    return findMarker(sourceFile, StyleKind.PrettierStyle) as PrettierStyle | undefined;
}

/**
 * Applies Prettier formatting to a tree.
 *
 * Configuration is resolved from the PrettierStyle marker on the source file.
 *
 * For compilation units, formats and reconciles the entire tree.
 * For subtrees, uses prettierFormatSubtree which prunes the tree for efficiency,
 * formats the pruned tree, and reconciles only the target subtree.
 *
 * @param tree The tree to format
 * @param prettierStyle The PrettierStyle containing config
 * @param p The visitor parameter
 * @param cursor Optional cursor for subtree formatting
 * @param stopAfter Optional tree to stop after
 * @returns The formatted tree
 */
export async function applyPrettierFormatting<R extends J, P>(
    tree: R,
    prettierStyle: PrettierStyle,
    p: P,
    cursor?: Cursor,
    stopAfter?: Tree
): Promise<R | undefined> {
    // Run only the essential visitors first
    const essentialVisitors = [
        new NormalizeWhitespaceVisitor(stopAfter),
        new MinimumViableSpacingVisitor(stopAfter),
    ];

    let t: R | undefined = tree;
    for (const visitor of essentialVisitors) {
        t = await visitor.visit(t, p, cursor);
        if (t === undefined) {
            return undefined;
        }
    }

    // If file is in .prettierignore, skip formatting entirely
    if (prettierStyle.ignored) {
        return t;
    }

    // Build options for Prettier
    // Pass through the entire resolved config - let Prettier use its own defaults for unspecified options
    const prettierOpts: PrettierFormatOptions = {
        ...prettierStyle.config as PrettierFormatOptions,
        prettierVersion: prettierStyle.prettierVersion,
    };

    try {
        if (t.kind === JS.Kind.CompilationUnit) {
            // Format and reconcile the entire compilation unit
            const formatted = await prettierFormat(t as unknown as JS.CompilationUnit, prettierOpts, stopAfter as J | undefined);
            return formatted as unknown as R;
        }

        if (!cursor) {
            // No cursor provided - can't use subtree formatting, return with essential formatting
            console.warn('Prettier formatting: No cursor provided for subtree, returning with essential formatting only');
            return t;
        }

        // Use prettierFormatSubtree for subtree formatting
        const formatted = await prettierFormatSubtree(t, cursor, prettierOpts, stopAfter as J | undefined);
        if (formatted) {
            return formatted as R;
        }

        // Subtree formatting failed, return with essential formatting applied
        console.warn('Prettier formatting: Subtree formatting failed, returning with essential formatting only');
        return t;
    } catch (e) {
        // If Prettier fails, return tree with essential formatting applied
        console.warn('Prettier formatting failed, returning with essential formatting only:', e);
        return t;
    }
}
