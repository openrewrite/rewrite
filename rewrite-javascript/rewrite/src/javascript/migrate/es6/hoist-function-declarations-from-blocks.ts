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
import {produceAsync, TreeVisitor} from "../../../visitor";
import {ExecutionContext} from "../../../execution";
import {JavaScriptVisitor, JS, template} from "../../../javascript";
import {J} from "../../../java";
import {produce} from "immer";
import {randomId} from "../../../uuid";

interface FunctionInfo {
    name: string;
    methodType: any;
    declaredInBlock: J.Block;
    usedOutsideBlock: boolean;
}

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
                    private blockStack: J.Block[] = [];

                    async visitBlock(block: J.Block, p: ExecutionContext): Promise<J | undefined> {
                        const parent = this.cursor.parentTree()?.value;
                        if (parent?.kind === J.Kind.If ||
                            parent?.kind === J.Kind.WhileLoop ||
                            parent?.kind === J.Kind.DoWhileLoop ||
                            parent?.kind === JS.Kind.ForInLoop ||
                            parent?.kind === JS.Kind.ForOfLoop ||
                            parent?.kind === J.Kind.ForLoop ||
                            parent?.kind === J.Kind.ForEachLoop) {

                            this.blockStack.push(block);
                            const result = await super.visitBlock(block, p);
                            this.blockStack.pop();
                            return result;
                        }
                        return await super.visitBlock(block, p);
                    }

                    async visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): Promise<J | undefined> {
                        const funcName = method.name?.simpleName;
                        if (funcName && this.blockStack.length > 0) {
                            const declaredInBlock = this.blockStack[this.blockStack.length - 1];
                            if (!functionsInBlocks.has(funcName)) {
                                functionsInBlocks.set(funcName, {
                                    name: funcName,
                                    methodType: method.methodType,
                                    declaredInBlock: declaredInBlock,
                                    usedOutsideBlock: false
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
                                const inDeclaringBlock = this.blockStack.includes(info.declaredInBlock);
                                if (!inDeclaringBlock) {
                                    info.usedOutsideBlock = true;
                                }
                            }
                        }
                        return await super.visitMethodInvocation(invocation, p);
                    }
                }().visit(cu, p);

                // Filter to functions that need hoisting
                const functionsToHoist = new Set<string>();
                functionsInBlocks.forEach((info, name) => {
                    if (info.usedOutsideBlock) {
                        functionsToHoist.add(name);
                    }
                });

                if (functionsToHoist.size === 0) {
                    return cu;
                }

                // Second pass: transform the functions
                const hoistedFunctions = new Set<string>();

                const transformed = await new class extends JavaScriptVisitor<ExecutionContext> {
                    private blockStack: J.Block[] = [];

                    async visitBlock(block: J.Block, p: ExecutionContext): Promise<J | undefined> {
                        const parent = this.cursor.parentTree()?.value;
                        const isControlFlowBlock = parent?.kind === J.Kind.If ||
                            parent?.kind === J.Kind.WhileLoop ||
                            parent?.kind === J.Kind.DoWhileLoop ||
                            parent?.kind === J.Kind.ForLoop ||
                            parent?.kind === JS.Kind.ForInLoop ||
                            parent?.kind === JS.Kind.ForOfLoop ||
                            parent?.kind === J.Kind.ForEachLoop;

                        if (isControlFlowBlock) {
                            this.blockStack.push(block);

                            // Transform the block's statements to replace function declarations with assignments
                            const transformedBlock = await produceAsync(block, async draft => {
                                const newStatements = [];
                                for (const stmt of draft.statements) {
                                    const method = stmt.element;
                                    if (method.kind === J.Kind.MethodDeclaration) {
                                        const methodDecl = method as J.MethodDeclaration;
                                        const funcName = methodDecl.name?.simpleName;

                                        if (funcName && functionsToHoist.has(funcName)) {
                                            hoistedFunctions.add(funcName);

                                            // Use template to create the assignment properly
                                            const tempAssignment = await template`${funcName} = function() {};`.apply(this.cursor, methodDecl) as J.Assignment;
                                            const assignment = produce(tempAssignment, assignDraft => {
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

                                            // Create new RightPadded with Semicolon marker
                                            newStatements.push(produce(stmt, s => {
                                                s.element = assignment;
                                                // Add Semicolon marker
                                                if (!s.markers.markers.some((m: any) => m.kind === J.Markers.Semicolon)) {
                                                    s.markers.markers = [...s.markers.markers, {
                                                        kind: J.Markers.Semicolon,
                                                        id: randomId()
                                                    }];
                                                }
                                            }));
                                        } else {
                                            newStatements.push(stmt);
                                        }
                                    } else {
                                        newStatements.push(stmt);
                                    }
                                }
                                draft.statements = newStatements;
                            });

                            const result = await super.visitBlock(transformedBlock, p);
                            this.blockStack.pop();
                            return result;
                        }
                        return await super.visitBlock(block, p);
                    }
                }().visit(cu, p) as JS.CompilationUnit;

                // Add variable declarations at the top
                if (hoistedFunctions.size > 0) {
                    // Use a visitor to prepend declarations with proper cursor context
                    return await new class extends JavaScriptVisitor<ExecutionContext> {
                        private declarationsAdded = false;

                        async visitJsCompilationUnit(cu: JS.CompilationUnit, p: ExecutionContext): Promise<J | undefined> {
                            const result = await super.visitJsCompilationUnit(cu, p) as JS.CompilationUnit;

                            if (!this.declarationsAdded && result.statements.length > 0) {
                                this.declarationsAdded = true;

                                // Template all declarations
                                const declarations: J[] = [];
                                for (const funcName of hoistedFunctions) {
                                    const decl = await template`let ${funcName};`.apply(this.cursor, result.statements[0].element) as J;
                                    if (decl) {
                                        declarations.push(decl);
                                    }
                                }

                                // Prepend declarations with proper spacing
                                return produce(result, draft => {
                                    // Create RightPadded wrappers for the declarations
                                    const newStatements = declarations.map((decl, index) => {
                                        // Wrap the declaration in a RightPadded with proper spacing
                                        const wrappedDecl = produce(decl, d => {
                                            if (index > 0) {
                                                // Add newline prefix for subsequent declarations
                                                d.prefix = produce(d.prefix, p => {
                                                    p.whitespace = '\n';
                                                });
                                            }
                                        });

                                        // Create a RightPadded with empty after space
                                        return {
                                            element: wrappedDecl,
                                            after: { kind: 'Space', whitespace: '', comments: [], markers: [] },
                                            markers: {
                                                kind: 'Markers',
                                                id: randomId(),
                                                markers: [{
                                                    kind: J.Markers.Semicolon,
                                                    id: randomId()
                                                }]
                                            }
                                        } as any;
                                    });

                                    // Ensure the first existing statement has a newline prefix
                                    const existingStatements = draft.statements.map((stmt, index) => {
                                        if (index === 0) {
                                            return produce(stmt, s => {
                                                s.element = produce(s.element, e => {
                                                    e.prefix = produce(e.prefix, p => {
                                                        p.whitespace = '\n';
                                                    });
                                                });
                                            });
                                        }
                                        return stmt;
                                    });

                                    draft.statements = [...newStatements, ...existingStatements];
                                });
                            }

                            return result;
                        }
                    }().visit(transformed, p) as JS.CompilationUnit;
                }

                return transformed;
            }
        }();
    }
}
