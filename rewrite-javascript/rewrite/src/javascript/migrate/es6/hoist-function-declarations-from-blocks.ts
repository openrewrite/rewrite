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
import {JavaScriptVisitor, JS, raw, template} from "../../../javascript";
import {emptySpace, J, Type} from "../../../java";
import {create as produce} from "mutative";
import {randomId} from "../../../uuid";
import {Tree} from "../../../tree";
import {emptyMarkers, replaceMarkerByKind} from "../../../markers";
import {TabsAndIndentsVisitor} from "../../format";
import {getStyle, StyleKind, TabsAndIndentsStyle} from "../../style";

/**
 * Information about a function declaration found inside a control flow block.
 */
interface FunctionInfo {
    /** The function name */
    name: string;
    /** The method type for matching invocations */
    methodType: Type.Method | undefined;
    /** The block/control flow statement where the function is declared */
    declaredInScope: Tree;
    /** Whether the function is called outside its declaring scope */
    usedOutsideScope: boolean;
    /** The enclosing function or module where the `let` declaration should be added */
    hoistToScope: Tree;
}

/**
 * Converts function declarations inside blocks (if/while/for) to function expressions
 * to avoid strict mode errors when targeting ES5 or when the function is used outside
 * its declaring block.
 *
 * @example
 * // Before:
 * if (true) {
 *     function helper() { return 42; }
 * }
 * const result = helper();
 *
 * // After:
 * let helper;
 * if (true) {
 *     helper = function () { return 42; };
 * }
 * const result = helper();
 */
export class HoistFunctionDeclarationsFromBlocks extends Recipe {
    name = "org.openrewrite.javascript.migrate.es6.hoist-function-declarations-from-blocks";
    displayName = "Hoist function declarations from blocks";
    description = "Converts function declarations inside blocks (if/while/for) to function expressions to avoid strict mode errors when targeting ES5 or when the function is used outside its declaring block.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new HoistingVisitor();
    }
}

/**
 * Main visitor that orchestrates the three-pass transformation.
 */
class HoistingVisitor extends JavaScriptVisitor<ExecutionContext> {
    protected async visitJsCompilationUnit(cu: JS.CompilationUnit, p: ExecutionContext): Promise<J | undefined> {
        // Pass 1: Analyze which functions need hoisting
        const analyzer = new FunctionUsageAnalyzer();
        await analyzer.visit(cu, p);

        // Filter to only functions that are used outside their declaring scope
        const functionsToHoist = new Map<string, FunctionInfo>();
        for (const [name, info] of analyzer.functionsInBlocks) {
            if (info.usedOutsideScope) {
                functionsToHoist.set(name, info);
            }
        }

        if (functionsToHoist.size === 0) {
            return cu;
        }

        // Pass 2: Transform function declarations to assignments
        const transformer = new FunctionToAssignmentTransformer(functionsToHoist);
        const transformed = await transformer.visit(cu, p) as JS.CompilationUnit;

        // Pass 3: Add variable declarations at the top of each scope
        if (transformer.hoistedFunctions.size > 0) {
            const injector = new VariableDeclarationInjector(transformer.hoistedFunctions);
            return await injector.visit(transformed, p) as JS.CompilationUnit;
        }

        return transformed;
    }
}

/**
 * Pass 1: Analyzes the AST to find function declarations inside control flow blocks
 * and tracks whether they are used outside their declaring scope.
 */
class FunctionUsageAnalyzer extends JavaScriptVisitor<ExecutionContext> {
    /** Functions declared inside control flow blocks, keyed by name */
    readonly functionsInBlocks = new Map<string, FunctionInfo>();

    /** Stack of control flow blocks we're currently inside */
    private scopeStack: Tree[] = [];

    async visitBlock(block: J.Block, p: ExecutionContext): Promise<J | undefined> {
        const parent = this.cursor.parentTree()?.value;
        if (isControlFlowStatement(parent)) {
            this.scopeStack.push(block);
            const result = await super.visitBlock(block, p);
            this.scopeStack.pop();
            return result;
        }
        return super.visitBlock(block, p);
    }

    async visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): Promise<J | undefined> {
        const funcName = method.name?.simpleName;
        if (funcName) {
            const parent = this.cursor.parentTree()?.value;
            let declaringScope: Tree | undefined;

            // Check if we're inside a tracked control flow block
            if (this.scopeStack.length > 0) {
                declaringScope = this.scopeStack[this.scopeStack.length - 1];
            }
            // Or if we're a direct child of a control flow statement (no braces)
            else if (isControlFlowStatement(parent)) {
                declaringScope = parent;
            }

            if (declaringScope && !this.functionsInBlocks.has(funcName)) {
                this.functionsInBlocks.set(funcName, {
                    name: funcName,
                    methodType: method.methodType,
                    declaredInScope: declaringScope,
                    usedOutsideScope: false,
                    hoistToScope: this.findEnclosingScope()
                });
            }
        }
        return super.visitMethodDeclaration(method, p);
    }

    async visitMethodInvocation(invocation: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
        const funcName = invocation.name?.simpleName;
        // Only consider function calls (no select), not method calls
        if (funcName && !invocation.select && this.functionsInBlocks.has(funcName)) {
            const info = this.functionsInBlocks.get(funcName)!;
            // Match by method type name to avoid false positives
            if (invocation.methodType?.name === info.methodType?.name) {
                const inDeclaringScope = this.scopeStack.includes(info.declaredInScope);
                if (!inDeclaringScope) {
                    info.usedOutsideScope = true;
                }
            }
        }
        return super.visitMethodInvocation(invocation, p);
    }

    async visitIdentifier(identifier: J.Identifier, p: ExecutionContext): Promise<J | undefined> {
        const funcName = identifier.simpleName;
        if (funcName && this.functionsInBlocks.has(funcName)) {
            // Skip if this identifier is the function declaration's own name
            const parent = this.cursor.parentTree()?.value;
            if (parent?.kind === J.Kind.MethodDeclaration) {
                const methodDecl = parent as J.MethodDeclaration;
                if (methodDecl.name?.id === identifier.id) {
                    return super.visitIdentifier(identifier, p);
                }
            }

            // Skip if this is the name part of a method invocation (already handled by visitMethodInvocation)
            if (parent?.kind === J.Kind.MethodInvocation) {
                const invocation = parent as J.MethodInvocation;
                if (invocation.name?.id === identifier.id) {
                    return super.visitIdentifier(identifier, p);
                }
            }

            const info = this.functionsInBlocks.get(funcName)!;
            const inDeclaringScope = this.scopeStack.includes(info.declaredInScope);
            if (!inDeclaringScope) {
                info.usedOutsideScope = true;
            }
        }
        return super.visitIdentifier(identifier, p);
    }

    /** Finds the nearest enclosing function or module scope */
    private findEnclosingScope(): Tree {
        let current = this.cursor.parentTree();
        while (current) {
            const value = current.value;
            if (value.kind === J.Kind.MethodDeclaration ||
                value.kind === JS.Kind.ArrowFunction ||
                value.kind === JS.Kind.CompilationUnit) {
                return value;
            }
            current = current.parentTree();
        }
        return this.cursor.root.value;
    }
}

/**
 * Pass 2: Transforms function declarations inside control flow blocks
 * into function expression assignments.
 */
class FunctionToAssignmentTransformer extends JavaScriptVisitor<ExecutionContext> {
    /** Maps scope ID -> set of function names that need `let` declarations */
    readonly hoistedFunctions = new Map<string, Set<string>>();

    constructor(private readonly functionsToHoist: Map<string, FunctionInfo>) {
        super();
    }

    async visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): Promise<J | undefined> {
        const funcName = method.name?.simpleName;
        if (!funcName || !this.functionsToHoist.has(funcName)) {
            return super.visitMethodDeclaration(method, p);
        }

        const parent = this.cursor.parentTree()?.value;

        // Check if this function is in a control flow context that needs transformation
        const inControlFlow = isControlFlowStatement(parent) ||
            (parent?.kind === J.Kind.Block && isControlFlowStatement(this.cursor.parentTree()?.parentTree()?.value));

        if (!inControlFlow) {
            return super.visitMethodDeclaration(method, p);
        }

        return this.transformMethodToAssignment(method);
    }

    private async transformMethodToAssignment(methodDecl: J.MethodDeclaration): Promise<J.Assignment> {
        const funcName = methodDecl.name!.simpleName;
        const info = this.functionsToHoist.get(funcName)!;

        // Track that this function needs a `let` declaration in its enclosing scope
        const scopeId = info.hoistToScope.id;
        if (!this.hoistedFunctions.has(scopeId)) {
            this.hoistedFunctions.set(scopeId, new Set<string>());
        }
        this.hoistedFunctions.get(scopeId)!.add(funcName);

        // Create the assignment: funcName = function() {};
        const tempAssignment = await template`${raw(funcName)} = function() {};`.apply(methodDecl, this.cursor) as J.Assignment;

        // Copy the original function's properties to the new function expression
        return produce(tempAssignment, draft => {
            const funcExpr = draft.assignment.element;
            if (funcExpr.kind === JS.Kind.StatementExpression) {
                const stmtExpr = funcExpr as JS.StatementExpression;
                const methodNode = stmtExpr.statement as J.MethodDeclaration;
                (stmtExpr.statement as J.MethodDeclaration) = {
                    ...methodNode,
                    // Copy function body, parameters, return type
                    body: methodDecl.body,
                    parameters: methodDecl.parameters,
                    returnTypeExpression: methodDecl.returnTypeExpression,
                    // Copy async/other modifiers and annotations
                    leadingAnnotations: methodDecl.leadingAnnotations,
                    modifiers: methodDecl.modifiers,
                    // Copy generic type parameters
                    typeParameters: methodDecl.typeParameters,
                    // Copy markers (includes Generator marker for function*)
                    markers: methodDecl.markers
                };
            }
        })!;
    }

    /**
     * Add semicolon marker to RightPadded elements containing transformed assignments.
     */
    async visitRightPadded<T extends J | boolean>(rightPadded: J.RightPadded<T>, p: ExecutionContext): Promise<J.RightPadded<T>> {
        const result = await super.visitRightPadded(rightPadded, p);

        // Check if the element is an assignment that we transformed
        const element = result?.element;
        if (element && typeof element === 'object' && 'kind' in element && element.kind === J.Kind.Assignment) {
            const assignment = element as J.Assignment;
            // Check if this assignment's variable name is one we're hoisting
            if (assignment.variable.kind === J.Kind.Identifier) {
                const varName = (assignment.variable as J.Identifier).simpleName;
                if (this.functionsToHoist.has(varName)) {
                    return produce(result, draft => {
                        draft!.markers = replaceMarkerByKind(draft!.markers, {
                            kind: J.Markers.Semicolon,
                            id: randomId()
                        });
                    })!;
                }
            }
        }

        return result!;
    }
}

/**
 * Pass 3: Injects `let` variable declarations at the top of each scope
 * where hoisted functions need them.
 */
class VariableDeclarationInjector extends JavaScriptVisitor<ExecutionContext> {
    /** Tracks which scopes we've already processed */
    private processedScopes = new Set<string>();

    constructor(private readonly hoistedFunctions: Map<string, Set<string>>) {
        super();
    }

    async visitJsCompilationUnit(cu: JS.CompilationUnit, p: ExecutionContext): Promise<J | undefined> {
        const result = await super.visitJsCompilationUnit(cu, p) as JS.CompilationUnit;

        const funcNames = this.hoistedFunctions.get(result.id);
        if (funcNames && funcNames.size > 0 && !this.processedScopes.has(result.id)) {
            this.processedScopes.add(result.id);
            return this.prependDeclarations(result, funcNames, result.statements,
                (cu, newStatements) => ({...cu, statements: newStatements}), p);
        }

        return result;
    }

    async visitBlock(block: J.Block, p: ExecutionContext): Promise<J | undefined> {
        const result = await super.visitBlock(block, p) as J.Block;

        // Check if this block is the body of a function that needs declarations
        const parent = this.cursor.parentTree()?.value;

        // Handle regular function declarations
        if (parent?.kind === J.Kind.MethodDeclaration) {
            const funcNames = this.hoistedFunctions.get(parent.id);
            if (funcNames && funcNames.size > 0 && !this.processedScopes.has(parent.id)) {
                this.processedScopes.add(parent.id);
                return this.prependDeclarations(result, funcNames, result.statements,
                    (block, newStatements) => ({...block, statements: newStatements}), p);
            }
        }

        // Handle arrow functions: block is inside Lambda which is inside ArrowFunction
        if (parent?.kind === J.Kind.Lambda) {
            const grandparent = this.cursor.parentTree()?.parentTree()?.value;
            if (grandparent?.kind === JS.Kind.ArrowFunction) {
                const funcNames = this.hoistedFunctions.get(grandparent.id);
                if (funcNames && funcNames.size > 0 && !this.processedScopes.has(grandparent.id)) {
                    this.processedScopes.add(grandparent.id);
                    return this.prependDeclarations(result, funcNames, result.statements,
                        (block, newStatements) => ({...block, statements: newStatements}), p);
                }
            }
        }

        return result;
    }

    private async prependDeclarations<T extends J & { statements: J.RightPadded<J>[] }>(
        node: T,
        funcNames: Set<string>,
        statements: J.RightPadded<J>[],
        updateStatements: (node: T, statements: J.RightPadded<J>[]) => T,
        p: ExecutionContext
    ): Promise<T> {
        if (statements.length === 0) {
            return node;
        }

        // Generate `let funcName;` declarations for each hoisted function
        const declarations: J[] = [];
        for (const funcName of funcNames) {
            const decl = await template`let ${raw(funcName)};`.apply(statements[0].element, this.cursor) as J;
            if (decl) {
                declarations.push(decl);
            }
        }

        if (declarations.length === 0) {
            return node;
        }

        // Wrap declarations in RightPadded with semicolon markers
        const newStatements: J.RightPadded<J>[] = declarations.map((decl, i) => {
            const wrappedDecl = i > 0
                ? produce(decl, d => { d.prefix.whitespace = '\n'; })!
                : decl;

            return {
                kind: J.Kind.RightPadded,
                element: wrappedDecl,
                after: emptySpace,
                markers: replaceMarkerByKind(emptyMarkers, {
                    kind: J.Markers.Semicolon,
                    id: randomId()
                })
            } as J.RightPadded<J>;
        });

        // Ensure the first existing statement starts on a new line
        const updatedStatements = [...newStatements, ...statements];
        if (statements.length > 0) {
            updatedStatements[newStatements.length] = produce(
                updatedStatements[newStatements.length],
                s => { s.element.prefix.whitespace = '\n'; }
            )!;
        }

        const updatedNode = updateStatements(node, updatedStatements);

        // Apply tabs-and-indents formatting to fix indentation of the newly added statements
        const style = getStyle(StyleKind.TabsAndIndentsStyle, updatedNode) as TabsAndIndentsStyle;
        return (await new TabsAndIndentsVisitor(style).visit(updatedNode, p, this.cursor.parentTree()))!;
    }
}

/**
 * Checks if a node is a control flow statement that can contain
 * problematic function declarations.
 */
function isControlFlowStatement(node: Tree | undefined): boolean {
    if (!node) return false;
    return node.kind === J.Kind.If ||
        node.kind === J.Kind.IfElse ||
        node.kind === J.Kind.WhileLoop ||
        node.kind === J.Kind.DoWhileLoop ||
        node.kind === JS.Kind.ForInLoop ||
        node.kind === JS.Kind.ForOfLoop ||
        node.kind === J.Kind.ForLoop ||
        node.kind === J.Kind.ForEachLoop;
}
