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
import {TreeVisitor} from "../visitor";
import {ExecutionContext} from "../execution";
import {JavaScriptVisitor, JS} from "../javascript";
import {J} from "../java";
import {produce} from "immer";

export class OrderImports extends Recipe {
    name = "org.openrewrite.OrderImports";
    displayName = "Order imports";
    description = "Sort top-level imports alphabetically within groups: no qualifier, asterisk, multiple, single.";


    get editor(): TreeVisitor<any, ExecutionContext> {
        return new class extends JavaScriptVisitor<ExecutionContext> {

            protected async visitJsCompilationUnit(cu: JS.CompilationUnit, p: ExecutionContext): Promise<J | undefined> {
                return produce(cu, draft => {
                    const importCount = this.countImports(cu);
                    const importsToSort = draft.statements.slice(0, importCount);
                    const restStatements = cu.statements.slice(importCount);
                    const importsSorted = importsToSort.sort((a, b) => {
                        const aImport = a.element as JS.Import;
                        const bImport = b.element as JS.Import;

                        const noSpecifier = (aImport.importClause == undefined ? 1 : 0) - (bImport.importClause == undefined ? 1 : 0);
                        if (noSpecifier != 0) {
                            return -noSpecifier;
                        }
                        const asterisk = this.isAsteriskImport(aImport) - this.isAsteriskImport(bImport);
                        if (asterisk != 0) {
                            return -asterisk;
                        }
                        const multipleImport = this.isMultipleImport(aImport) - this.isMultipleImport(bImport);
                        if (multipleImport != 0) {
                            return -multipleImport;
                        }
                        const comparedSpecifiers = this.compareStringArrays(this.extractImportSpecifierNames(aImport), this.extractImportSpecifierNames(bImport));
                        if (comparedSpecifiers != 0) {
                            return comparedSpecifiers;
                        }
                        return 0; // TODO, have some tiebreakers, 0 might actually confuse the sorting algorithm
                    });
                    draft.statements = [...importsSorted, ...restStatements];
                    for (let i = 0; i < importsSorted.length; i++) {
                        draft.statements[i].element.prefix.whitespace = i > 0 ? "\n" : "";
                    }
                    // TODO deal with prefixes
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
                            if (namedImports.elements.kind == J.Kind.JContainer) {
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
                            if (namedImports.elements.kind == J.Kind.JContainer) {
                                const elements = namedImports.elements.elements;
                                for (let i = 0; i < elements.length; i++) {
                                    if (elements[i].element.kind == JS.Kind.Alias) {
                                        const alias = elements[i].element as JS.Alias;
                                        names.push(alias.propertyName.element.simpleName);
                                    } else if (elements[i].element.kind == JS.Kind.ImportSpecifier) {
                                        const importSpecifier = elements[i].element as JS.ImportSpecifier;
                                        if (importSpecifier.specifier.kind == J.Kind.Identifier) {
                                            names.push((importSpecifier.specifier as J.Identifier).simpleName);
                                        }
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
        }
    }
}
