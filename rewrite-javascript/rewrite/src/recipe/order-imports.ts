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

import {Recipe} from "../recipe";
import {produceAsync, TreeVisitor} from "../visitor";
import {ExecutionContext} from "../execution";
import {JavaScriptVisitor, JS} from "../javascript";
import {J} from "../java";
import {Draft, produce} from "immer";
import {AutoformatVisitor} from "../javascript/format";

export class OrderImports extends Recipe {
    name = "org.openrewrite.OrderImports";
    displayName = "Order imports";
    description = "Sort top-level imports alphabetically within groups: no qualifier, asterisk, multiple, single.";


    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JavaScriptVisitor<ExecutionContext> {

            protected async visitJsCompilationUnit(cu: JS.CompilationUnit, p: ExecutionContext): Promise<J | undefined> {
                const importCount = this.countImports(cu);
                const imports = cu.statements.slice(0, importCount) as J.RightPadded<JS.Import>[];
                const originalImportPosition = Object.fromEntries(imports.map((item, i) => [item.element.id, i]));
                const restStatements = cu.statements.slice(importCount);
                const sortedImports = await this.sortNamesWithinEachLine(imports);
                sortedImports.sort((aPadded, bPadded) => {
                    const a = aPadded.element;
                    const b = bPadded.element;

                    const noSpecifier = (a.importClause == undefined ? 1 : 0) - (b.importClause == undefined ? 1 : 0);
                    if (noSpecifier != 0) {
                        return -noSpecifier;
                    }
                    const asterisk = this.isAsteriskImport(a) - this.isAsteriskImport(b);
                    if (asterisk != 0) {
                        return -asterisk;
                    }
                    const multipleImport = this.isMultipleImport(a) - this.isMultipleImport(b);
                    if (multipleImport != 0) {
                        return -multipleImport;
                    }
                    const comparedSpecifiers = this.compareStringArrays(this.extractImportSpecifierNames(a), this.extractImportSpecifierNames(b));
                    if (comparedSpecifiers != 0) {
                        return comparedSpecifiers;
                    }
                    // Tiebreaker, keep the sort stable
                    return originalImportPosition[aPadded.element.id] - originalImportPosition[bPadded.element.id];
                });
                const cuWithImportsSorted = await produceAsync(cu, async draft => {
                    draft.statements = [...sortedImports, ...restStatements];
                });
                return produce(cuWithImportsSorted, draft => {
                    for (let i = 0; i < importCount; i++) {
                       draft.statements[i].element.prefix.whitespace = i > 0 ? "\n" : "";
                    }
                    // TODO deal with comments in the whitespace around imports
                });
            }

            private isAsteriskImport(import_: JS.Import): 0 | 1 {
                if (import_.importClause != undefined) {
                    if (import_.importClause.namedBindings != undefined) {
                        if (import_.importClause.namedBindings.kind == JS.Kind.Alias) {
                            return (import_.importClause.namedBindings as JS.Alias).propertyName.element.simpleName == "*" ? 1 : 0;
                        }
                    }
                }
                return 0;
            }

            private isMultipleImport(import_: JS.Import): 0 | 1 {
                if (import_.importClause != undefined) {
                    if (import_.importClause.namedBindings != undefined) {
                        if (import_.importClause.namedBindings.kind == JS.Kind.NamedImports) {
                            const namedImports = import_.importClause.namedBindings as JS.NamedImports;
                            if (namedImports.elements.kind == J.Kind.Container) {
                                return namedImports.elements.elements.length > 1 ? 1 : 0;
                            }
                        }
                    }
                }
                return 0;
            }

            private extractImportSpecifierNames(import_: JS.Import): string[] {
                const names: string[] = [];
                if (import_.importClause != undefined) {
                    if (import_.importClause.namedBindings != undefined) {
                        if (import_.importClause.namedBindings.kind == JS.Kind.NamedImports) {
                            const namedImports = import_.importClause.namedBindings as JS.NamedImports;
                            if (namedImports.elements.kind == J.Kind.Container) {
                                const elements = namedImports.elements.elements;
                                for (let i = 0; i < elements.length; i++) {
                                    const importSpecifier = elements[i].element as JS.ImportSpecifier;
                                    if (importSpecifier.specifier.kind == J.Kind.Identifier) {
                                        names.push((importSpecifier.specifier as J.Identifier).simpleName);
                                    } else if (importSpecifier.specifier.kind == JS.Kind.Alias) {
                                        names.push((importSpecifier.specifier as JS.Alias).propertyName.element.simpleName);
                                    } else {
                                        throw new Error("Unknown kind " + elements[i].kind);
                                    }
                                }
                            }
                        } else if (import_.importClause.namedBindings.kind == JS.Kind.Alias) {
                            const alias = import_.importClause.namedBindings as JS.Alias;
                            names.push(alias.propertyName.element.simpleName);
                            if (alias.alias.kind == J.Kind.Identifier) {
                                names.push((alias.alias as J.Identifier).simpleName)
                            }
                        }
                    }
                }
                return names;
            }

            private compareStringArrays(a: string[], b: string[]): number {
                let i = 0;
                while (i < a.length && i < b.length) {
                    const comparison = a[i].localeCompare(b[i]);
                    if (comparison !== 0) {
                        return comparison;
                    }
                    i++;
                }
                if (i < a.length) {
                    return 1;
                } else if (i < b.length) {
                    return -1;
                }
                return 0;
            }

            private countImports(cu: JS.CompilationUnit): number {
                let i = 0;
                while ((i < cu.statements.length) && (cu.statements[i].element.kind === JS.Kind.Import)) {
                    i++;
                }
                return i;
            }

            private async sortNamesWithinEachLine(imports: J.RightPadded<JS.Import>[]): Promise<J.RightPadded<JS.Import>[]> {
                const ret = [];
                for (const importPadded of imports) {
                    const import_ = importPadded.element;
                    if (this.isMultipleImport(import_) == 1) {
                        const importSorted = produce(import_, draft => {
                            let elements = (draft.importClause!.namedBindings as Draft<JS.NamedImports>).elements.elements;
                            const trailingComma = elements.length > 0 && elements[elements.length - 1].markers?.markers.find(m => m.kind === J.Markers.TrailingComma);
                            if (trailingComma) {
                                elements[elements.length - 1].markers.markers = elements[elements.length - 1].markers.markers.filter(m => m.kind !== J.Markers.TrailingComma);
                            }
                            elements.sort((a, b) => {
                                const namesExtracted: string[] = [a.element, b.element].map(expr => {
                                    const is = expr as JS.ImportSpecifier;
                                    if (is.specifier.kind == JS.Kind.Alias) {
                                        return (is.specifier as JS.Alias).propertyName.element.simpleName;
                                    } else if (is.specifier.kind == J.Kind.Identifier) {
                                        return (is.specifier as J.Identifier).simpleName;
                                    } else {
                                        throw new Error("Unsupported kind " + expr.kind);
                                    }
                                });
                                return namesExtracted[0].localeCompare(namesExtracted[1]);
                            });
                            if (trailingComma && elements.length > 0 && !elements[elements.length - 1].markers.markers.find(m => m.kind === J.Markers.TrailingComma)) {
                                elements[elements.length - 1].markers.markers.push(trailingComma);
                            }
                        });
                        const formatted = await new AutoformatVisitor().visit(importSorted, {}) as JS.Import;
                        ret.push(produce(importPadded, draft => {
                           draft.element = formatted;
                        }));
                    } else {
                        ret.push(importPadded);
                    }
                }
                return ret;
            }
        }
    }
}
