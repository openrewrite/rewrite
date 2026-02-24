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

import {Recipe} from "../../recipe";
import {produceAsync, TreeVisitor} from "../../visitor";
import {ExecutionContext} from "../../execution";
import {JavaScriptVisitor, JS} from "../index";
import {Expression, J, Statement} from "../../java";
import {create as produce, Draft} from "mutative";
import {SpacesStyle, styleFromSourceFile, StyleKind} from "../style";

/**
 * Import type categories for sorting order:
 * 1. Side-effect imports (no specifier): import 'module';
 * 2. Namespace imports: import * as foo from 'module';
 * 3. Default imports: import foo from 'module';
 * 4. Named imports: import { foo } from 'module';
 * 5. Type imports: import type { Foo } from 'module';
 */
enum ImportCategory {
    SideEffect = 0,
    Namespace = 1,
    Default = 2,
    Named = 3,
    Type = 4
}

export class OrderImports extends Recipe {
    readonly name = "org.openrewrite.javascript.cleanup.order-imports";
    readonly displayName = "Order imports";
    readonly description = "Sort imports by category and module path. Categories: side-effect, namespace, default, named, type. Within each category, imports are sorted alphabetically by module path. Named specifiers within each import are also sorted alphabetically.";


    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JavaScriptVisitor<ExecutionContext> {

            protected async visitJsCompilationUnit(cu: JS.CompilationUnit, p: ExecutionContext): Promise<J | undefined> {
                const importCount = this.countImports(cu);
                if (importCount === 0) {
                    return cu;
                }

                const imports = cu.statements.slice(0, importCount) as J.RightPadded<JS.Import>[];
                // For tree types, the padded value IS the element (intersection type)
                const originalImportPosition = Object.fromEntries(imports.map((item, i) => [item.id, i]));
                const restStatements = cu.statements.slice(importCount);

                // Get style for consistent brace spacing
                const spacesStyle = styleFromSourceFile(StyleKind.SpacesStyle, cu) as SpacesStyle | undefined;
                const useBraceSpaces = spacesStyle?.within.es6ImportExportBraces ?? false;

                // Sort named specifiers within each import
                const sortedSpecifiers = this.sortNamedSpecifiersWithinImports(imports, useBraceSpaces);

                // Sort imports by category and module path
                sortedSpecifiers.sort((a, b) => {

                    // First, compare by category
                    const categoryA = this.getImportCategory(a);
                    const categoryB = this.getImportCategory(b);
                    if (categoryA !== categoryB) {
                        return categoryA - categoryB;
                    }

                    // Within same category, sort by module path (case-insensitive)
                    const modulePathA = this.getModulePath(a).toLowerCase();
                    const modulePathB = this.getModulePath(b).toLowerCase();
                    const pathComparison = modulePathA.localeCompare(modulePathB);
                    if (pathComparison !== 0) {
                        return pathComparison;
                    }

                    // Tiebreaker: keep original order for stability
                    return originalImportPosition[a.id] - originalImportPosition[b.id];
                });

                const cuWithImportsSorted = await produceAsync(cu, async draft => {
                    draft.statements = [...sortedSpecifiers, ...restStatements];
                });

                return produce(cuWithImportsSorted!, draft => {
                    for (let i = 0; i < importCount; i++) {
                        // For tree types, the padded value IS the element (intersection type)
                        draft.statements[i].prefix.whitespace = i > 0 ? "\n" : "";
                    }
                });
            }

            /**
             * Determine the category of an import for sorting purposes.
             */
            private getImportCategory(import_: JS.Import): ImportCategory {
                // Type imports: import type { Foo } from 'module'
                if (import_.importClause?.typeOnly) {
                    return ImportCategory.Type;
                }

                // Side-effect imports: import 'module'
                if (import_.importClause === undefined) {
                    return ImportCategory.SideEffect;
                }

                // Namespace imports: import * as foo from 'module'
                if (import_.importClause.namedBindings?.kind === JS.Kind.Alias) {
                    const alias = import_.importClause.namedBindings as JS.Alias;
                    const propertyName = alias.propertyName;
                    if (propertyName.simpleName === "*") {
                        return ImportCategory.Namespace;
                    }
                }

                // Default imports (without named imports): import foo from 'module'
                if (import_.importClause.name && !import_.importClause.namedBindings) {
                    return ImportCategory.Default;
                }

                // Default with named imports or just named imports: import foo, { bar } from 'module' or import { foo } from 'module'
                return ImportCategory.Named;
            }

            /**
             * Extract the module path from an import statement.
             */
            private getModulePath(import_: JS.Import): string {
                const moduleSpec = import_.moduleSpecifier;
                if (moduleSpec?.kind === J.Kind.Literal) {
                    const literal = moduleSpec as J.Literal;
                    // Remove quotes from the value
                    return String(literal.value ?? '');
                }
                return '';
            }

            private countImports(cu: JS.CompilationUnit): number {
                let i = 0;
                while ((i < cu.statements.length) && (cu.statements[i].kind === JS.Kind.Import)) {
                    i++;
                }
                return i;
            }

            /**
             * Sort named specifiers within each import statement alphabetically.
             */
            private sortNamedSpecifiersWithinImports(imports: J.RightPadded<JS.Import>[], useBraceSpaces: boolean): J.RightPadded<JS.Import>[] {
                const ret = [];
                for (const importPadded of imports) {
                    const import_ = importPadded;
                    if (this.hasNamedImports(import_)) {
                        const importSorted = produce(import_, draft => {
                            const namedBindings = draft.importClause!.namedBindings as Draft<JS.NamedImports>;
                            let elements = namedBindings.elements.elements;

                            if (elements.length <= 1) {
                                return; // Nothing to sort
                            }

                            // Handle trailing comma
                            const trailingComma = elements.length > 0 &&
                                elements[elements.length - 1].padding.markers?.markers.find(m => m.kind === J.Markers.TrailingComma);
                            if (trailingComma) {
                                elements[elements.length - 1].padding.markers.markers =
                                    elements[elements.length - 1].padding.markers.markers.filter(m => m.kind !== J.Markers.TrailingComma);
                            }

                            // Sort by the imported name (not alias)
                            elements.sort((a, b) => {
                                const nameA = this.getSpecifierSortKey(a);
                                const nameB = this.getSpecifierSortKey(b);
                                return nameA.localeCompare(nameB);
                            });

                            // Normalize spacing based on es6ImportExportBraces style
                            const braceSpace = useBraceSpaces ? " " : "";
                            for (let i = 0; i < elements.length; i++) {
                                // For tree types, elements[i] IS the specifier with padding mixed in
                                if (i === 0) {
                                    // First element: space after opening brace based on style
                                    elements[i].prefix = {kind: J.Kind.Space, whitespace: braceSpace, comments: []};
                                } else {
                                    // Other elements: space after comma
                                    elements[i].prefix = {kind: J.Kind.Space, whitespace: ' ', comments: []};
                                }
                            }
                            // Last element: space before closing brace based on style
                            elements[elements.length - 1].padding.after = {kind: J.Kind.Space, whitespace: braceSpace, comments: []};

                            // Restore trailing comma to last element
                            if (trailingComma && elements.length > 0 &&
                                !elements[elements.length - 1].padding.markers.markers.find(m => m.kind === J.Markers.TrailingComma)) {
                                elements[elements.length - 1].padding.markers.markers.push(trailingComma);
                            }
                        });

                        // Merge the sorted import with padding
                        ret.push({
                            ...importSorted,
                            padding: importPadded.padding
                        } as J.RightPadded<JS.Import>);
                    } else {
                        ret.push(importPadded);
                    }
                }
                return ret;
            }

            /**
             * Check if an import has named imports that can be sorted.
             */
            private hasNamedImports(import_: JS.Import): boolean {
                if (import_.importClause?.namedBindings?.kind === JS.Kind.NamedImports) {
                    const namedImports = import_.importClause.namedBindings as JS.NamedImports;
                    return namedImports.elements.kind === J.Kind.Container &&
                        namedImports.elements.elements.length > 1;
                }
                return false;
            }

            /**
             * Get the sort key for an import specifier (the original name, not alias).
             */
            private getSpecifierSortKey(specifier: JS.ImportSpecifier): string {
                if (specifier.specifier.kind === JS.Kind.Alias) {
                    // import { foo as bar } - sort by 'foo'
                    const propertyName = (specifier.specifier as JS.Alias).propertyName;
                    return propertyName.simpleName;
                } else if (specifier.specifier.kind === J.Kind.Identifier) {
                    // import { foo } - sort by 'foo'
                    return (specifier.specifier as J.Identifier).simpleName;
                }
                return '';
            }
        }
    }
}
