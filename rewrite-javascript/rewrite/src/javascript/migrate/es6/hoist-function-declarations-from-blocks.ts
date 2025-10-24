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

import {Recipe} from "../../../recipe";
import {TreeVisitor} from "../../../visitor";
import {ExecutionContext} from "../../../execution";
import {IntelliJ, JavaScriptVisitor, JS, template} from "../../../javascript";
import {emptySpace, J} from "../../../java";
import {produce} from "immer";
import {randomId} from "../../../uuid";
import {Tree} from "../../../tree";
import {emptyMarkers, setMarkerByKind} from "../../../markers";
import {TabsAndIndentsVisitor} from "../../format";

export class HoistFunctionDeclarationsFromBlocks extends Recipe {
    name = "org.openrewrite.javascript.migrate.es6.hoist-function-declarations-from-blocks";
    displayName = "Hoist function declarations from blocks";
    description = "Converts function declarations inside blocks (if/while/for) to function expressions to avoid strict mode errors when targeting ES5 or when the function is used outside its declaring block.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {

        return new class extends JavaScriptVisitor<ExecutionContext> {
            protected async visitJsCompilationUnit(cu: JS.CompilationUnit, p: ExecutionContext): Promise<J | undefined> {
                // First pass: analyze which functions need hoisting
                const functionsInBlocks = new Map<string, FunctionInfo>();

                await new class extends JavaScriptVisitor<ExecutionContext> {
                    private scopeStack: Tree[] = [];

                    async visitBlock(block: J.Block, p: ExecutionContext): Promise<J | undefined> {
                        const parent = this.cursor.parentTree()?.value;
                        if (isControlFlowStatement(parent)) {
                            this.scopeStack.push(block);
                            const result = await super.visitBlock(block, p);
                            this.scopeStack.pop();
                            return result;
                        }
                        return await super.visitBlock(block, p);
                    }

                    async visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): Promise<J | undefined> {
                        const funcName = method.name?.simpleName;
                        if (funcName) {
                            const parent = this.cursor.parentTree()?.value;
                            let declaringScope: Tree | undefined;

                            // Check if in a block that's tracked
                            if (this.scopeStack.length > 0) {
                                declaringScope = this.scopeStack[this.scopeStack.length - 1];
                            }
                            // Check if direct child of control flow statement
                            else if (isControlFlowStatement(parent)) {
                                declaringScope = parent;
                            }

                            if (declaringScope && !functionsInBlocks.has(funcName)) {
                                // Find the enclosing scope where we should hoist (function or module)
                                const hoistToScope = this.findEnclosingScope();

                                functionsInBlocks.set(funcName, {
                                    name: funcName,
                                    methodType: method.methodType,
                                    declaredInScope: declaringScope,
                                    usedOutsideScope: false,
                                    hoistToScope: hoistToScope
                                });
                            }
                        }
                        return await super.visitMethodDeclaration(method, p);
                    }

                    async visitMethodInvocation(invocation: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
                        const funcName = invocation.name?.simpleName;
                        if (funcName && functionsInBlocks.has(funcName)) {
                            const info = functionsInBlocks.get(funcName)!;
                            if (invocation.methodType?.name === info.methodType?.name) {
                                const inDeclaringScope = this.scopeStack.includes(info.declaredInScope);
                                if (!inDeclaringScope) {
                                    info.usedOutsideScope = true;
                                }
                            }
                        }
                        return await super.visitMethodInvocation(invocation, p);
                    }

                    private findEnclosingScope(): Tree {
                        // Walk up the cursor to find the nearest function or module scope
                        let current = this.cursor.parentTree();
                        while (current) {
                            const value = current.value;
                            if (value.kind === J.Kind.MethodDeclaration) {
                                return value;
                            }
                            if (value.kind === JS.Kind.CompilationUnit) {
                                return value;
                            }
                            current = current.parentTree();
                        }
                        // Fall back to root
                        return this.cursor.root.value;
                    }
                }().visit(cu, p);

                // Filter to functions that need hoisting
                const functionsToHoist = new Map<string, FunctionInfo>();
                functionsInBlocks.forEach((info, name) => {
                    if (info.usedOutsideScope) {
                        functionsToHoist.set(name, info);
                    }
                });

                if (functionsToHoist.size === 0) {
                    return cu;
                }

                // Second pass: transform the functions
                const hoistedFunctions = new Map<string, Set<string>>();  // Group by hoist scope ID

                const transformed = await new class extends JavaScriptVisitor<ExecutionContext> {
                    private async transformMethodToAssignment(methodDecl: J.MethodDeclaration): Promise<J.Assignment> {
                        const funcName = methodDecl.name?.simpleName!;
                        const info = functionsToHoist.get(funcName)!;

                        // Add to the set for this scope (using scope ID as key)
                        const scopeId = info.hoistToScope.id;
                        if (!hoistedFunctions.has(scopeId)) {
                            hoistedFunctions.set(scopeId, new Set<string>());
                        }
                        hoistedFunctions.get(scopeId)!.add(funcName);

                        // Use template to create the assignment properly
                        const tempAssignment = await template`${funcName} = function() {};`.apply(this.cursor, methodDecl) as J.Assignment;
                        return produce(tempAssignment, assignDraft => {
                            const funcExpr = assignDraft.assignment.element;
                            if (funcExpr.kind === JS.Kind.StatementExpression) {
                                assignDraft.assignment.element = produce(funcExpr as JS.StatementExpression, stmtExprDraft => {
                                    const methodNode = stmtExprDraft.statement as J.MethodDeclaration;
                                    stmtExprDraft.statement = produce(methodNode, methodDraftNode => {
                                        methodDraftNode.body = methodDecl.body;
                                        methodDraftNode.parameters = methodDecl.parameters;
                                    });
                                });
                            }
                        });
                    }

                    async visitRightPadded<T extends J | boolean>(rightPadded: J.RightPadded<T>, p: ExecutionContext): Promise<J.RightPadded<T>> {
                        const result = await super.visitRightPadded(rightPadded, p);

                        // Check if child set message to add semicolon
                        if (this.cursor.messages.get("ADD_SEMICOLON")) {
                            return produce(result, draft => {
                                draft.markers = setMarkerByKind(draft.markers, {
                                    kind: J.Markers.Semicolon,
                                    id: randomId()
                                });
                            });
                        }

                        return result;
                    }

                    async visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): Promise<J | undefined> {
                        const funcName = method.name?.simpleName;
                        if (!funcName || !functionsToHoist.has(funcName)) {
                            return await super.visitMethodDeclaration(method, p);
                        }

                        const parent = this.cursor.parentTree()?.value;

                        // Check if this is a function declaration as a direct child of a control flow statement
                        if (isControlFlowStatement(parent)) {
                            // Set message on parent cursor so visitRightPadded can see it
                            this.cursor.parentTree()?.messages.set("ADD_SEMICOLON", true);
                            return await this.transformMethodToAssignment(method);
                        }

                        // Check if this is inside a block that's inside a control flow statement
                        if (parent?.kind === J.Kind.Block) {
                            const grandparent = this.cursor.parentTree()?.parentTree()?.value;
                            if (isControlFlowStatement(grandparent)) {
                                // Set message on parent cursor so visitRightPadded can see it
                                this.cursor.parentTree()?.messages.set("ADD_SEMICOLON", true);
                                return await this.transformMethodToAssignment(method);
                            }
                        }

                        return await super.visitMethodDeclaration(method, p);
                    }
                }().visit(cu, p) as JS.CompilationUnit;

                // Third pass: Add variable declarations at the top of each scope
                if (hoistedFunctions.size > 0) {
                    return await new class extends JavaScriptVisitor<ExecutionContext> {
                        private processedScopes = new Set<string>();

                        async visitJsCompilationUnit(cu: JS.CompilationUnit, p: ExecutionContext): Promise<J | undefined> {
                            const result = await super.visitJsCompilationUnit(cu, p) as JS.CompilationUnit;

                            const funcNames = hoistedFunctions.get(result.id);
                            if (funcNames && funcNames.size > 0 && !this.processedScopes.has(result.id)) {
                                this.processedScopes.add(result.id);
                                return await this.prependDeclarations(result, funcNames, result.statements,
                                    (cu, newStatements) => produce(cu, draft => { draft.statements = newStatements; }));
                            }

                            return result;
                        }

                        async visitBlock(block: J.Block, p: ExecutionContext): Promise<J | undefined> {
                            const result = await super.visitBlock(block, p) as J.Block;

                            // Check if this is a function body by looking at the parent
                            const parent = this.cursor.parentTree()?.value;
                            if (parent?.kind === J.Kind.MethodDeclaration) {
                                const funcNames = hoistedFunctions.get(parent.id);
                                if (funcNames && funcNames.size > 0 && !this.processedScopes.has(parent.id)) {
                                    this.processedScopes.add(parent.id);
                                    return await this.prependDeclarations(result, funcNames, result.statements,
                                        (block, newStatements) => produce(block, draft => { draft.statements = newStatements; }));
                                }
                            }

                            return result;
                        }

                        private async prependDeclarations<T extends J & { statements: J.RightPadded<J>[] }>(
                            node: T,
                            funcNames: Set<string>,
                            statements: J.RightPadded<J>[],
                            updateStatements: (node: T, statements: J.RightPadded<J>[]) => T
                        ): Promise<T> {
                            if (statements.length === 0) {
                                return node;
                            }

                            // Template all declarations
                            const declarations: J[] = [];
                            for (const funcName of funcNames) {
                                const decl = await template`let ${funcName};`.apply(this.cursor, statements[0].element) as J;
                                if (decl) {
                                    declarations.push(decl);
                                }
                            }

                            if (declarations.length === 0) {
                                return node;
                            }

                            // Create RightPadded wrappers for the declarations
                            const newStatements: J.RightPadded<J>[] = [];
                            for (let i = 0; i < declarations.length; i++) {
                                const decl = declarations[i];
                                const wrappedDecl = produce(decl, d => {
                                    if (i > 0) {
                                        // Add newline prefix for subsequent declarations
                                        d.prefix.whitespace = '\n';
                                    }
                                });

                                // Create a RightPadded with empty after space and Semicolon marker
                                const rightPadded: J.RightPadded<J> = {
                                    kind: J.Kind.RightPadded,
                                    element: wrappedDecl,
                                    after: emptySpace,
                                    markers: setMarkerByKind(emptyMarkers, {
                                        kind: J.Markers.Semicolon,
                                        id: randomId()
                                    })
                                };
                                newStatements.push(rightPadded);
                            }

                            // Ensure the first existing statement has a newline prefix
                            const updatedStatements = [...newStatements, ...statements];
                            if (updatedStatements.length > newStatements.length) {
                                updatedStatements[newStatements.length] = produce(updatedStatements[newStatements.length], s => {
                                    s.element.prefix.whitespace = '\n';
                                });
                            }

                            let updatedNode = updateStatements(node, updatedStatements);
                            return (await new TabsAndIndentsVisitor(IntelliJ.TypeScript.tabsAndIndents()).visit(updatedNode, p, this.cursor.parentTree()!))!;
                        }
                    }().visit(transformed, p) as JS.CompilationUnit;
                }

                return transformed;
            }
        }();
    }
}

interface FunctionInfo {
    name: string;
    methodType: any;
    declaredInScope: Tree;
    usedOutsideScope: boolean;
    hoistToScope: Tree;  // The function/module where declaration should be added
}

// Helper method to check if a node is a control flow statement
function isControlFlowStatement(node: Tree): boolean {
    return node?.kind === J.Kind.If ||
        node?.kind === J.Kind.IfElse ||
        node?.kind === J.Kind.WhileLoop ||
        node?.kind === J.Kind.DoWhileLoop ||
        node?.kind === JS.Kind.ForInLoop ||
        node?.kind === JS.Kind.ForOfLoop ||
        node?.kind === J.Kind.ForLoop ||
        node?.kind === J.Kind.ForEachLoop;
}
