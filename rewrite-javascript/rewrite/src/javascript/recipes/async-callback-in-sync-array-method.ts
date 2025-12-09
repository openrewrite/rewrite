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
import {ExecutionContext} from "../../execution";
import {TreeVisitor} from "../../visitor";
import {JavaScriptVisitor} from "../visitor";
import {J} from "../../java";
import {JS} from "../tree";
import {Type} from "../../java";
import {markupWarn} from "../../markers";

/**
 * Array methods that don't await async callbacks.
 * When an async function is passed as a callback to these methods,
 * the returned Promise is not awaited, leading to bugs.
 */
const SYNC_ARRAY_METHODS = new Set([
    'some',      // Returns first truthy value, but Promise is always truthy
    'every',     // Returns first falsy value, but Promise is always truthy
    'find',      // Returns first truthy value, but Promise is always truthy
    'findIndex', // Returns first truthy value, but Promise is always truthy
    'filter',    // Filters based on truthy values, but Promise is always truthy
    'forEach',   // Ignores return values entirely, async callbacks won't be awaited
]);

/**
 * Check if a type is a Promise type.
 * Looks for types like Promise<T>, PromiseLike<T>, or the Promise class itself.
 */
function isPromiseType(type?: Type): boolean {
    if (!type) return false;

    // Check for Class type with Promise name
    if (Type.isClass(type)) {
        const fqn = type.fullyQualifiedName;
        return fqn === 'Promise' ||
            fqn === 'PromiseLike' ||
            fqn.endsWith('.Promise') ||
            fqn.endsWith('.PromiseLike');
    }

    // Check for Parameterized type (e.g., Promise<boolean>)
    if (Type.isParameterized(type)) {
        return isPromiseType(type.type);
    }

    // Check for Union type (e.g., Promise<T> | undefined)
    if (Type.isUnion(type)) {
        return type.bounds.some(b => isPromiseType(b));
    }

    return false;
}

/**
 * Check if an arrow function has an async modifier.
 * In JavaScript, async is represented as a LanguageExtension modifier with keyword="async"
 */
function hasAsyncModifier(arrowFunc: JS.ArrowFunction): boolean {
    return arrowFunc.modifiers.some(m =>
        m.type === J.ModifierType.Async ||
        (m.type === J.ModifierType.LanguageExtension && m.keyword === 'async')
    );
}

/**
 * Check if a function type returns a Promise.
 */
function functionTypeReturnsPromise(funcType: Type): boolean {
    if (!Type.isFunctionType(funcType)) return false;

    const clazz = funcType as Type.Class;
    if (clazz.typeParameters && clazz.typeParameters.length > 0) {
        // First type parameter is typically R (return type) in TypeScript function types
        // It's a GenericTypeVariable with bounds containing the actual type
        const returnTypeParam = clazz.typeParameters[0];

        // Check if it's directly a Promise type
        if (isPromiseType(returnTypeParam)) {
            return true;
        }

        // Check if it's a GenericTypeVariable with bounds
        if (Type.isGenericTypeVariable(returnTypeParam)) {
            const bounds = returnTypeParam.bounds;
            if (bounds && bounds.some(b => isPromiseType(b))) {
                return true;
            }
        }
    }
    return false;
}

/**
 * Check if a callback argument returns a Promise.
 * This checks:
 * 1. Explicit async modifier on arrow functions
 * 2. Type annotation indicating Promise return type
 * 3. Function references whose type indicates Promise return
 */
function callbackReturnsPromise(arg: any): boolean {
    // Handle RightPadded wrapper
    const element = arg?.element ?? arg;

    if (!element) return false;

    // Check if it's an arrow function
    if (element.kind === JS.Kind.ArrowFunction) {
        const arrowFunc = element as JS.ArrowFunction;

        // Check for async modifier
        if (hasAsyncModifier(arrowFunc)) {
            return true;
        }

        // Check if return type expression indicates Promise
        const returnType = arrowFunc.returnTypeExpression;
        if (returnType) {
            // Check if the return type expression is an identifier named Promise
            if (returnType.kind === J.Kind.Identifier) {
                const id = returnType as J.Identifier;
                if (id.simpleName === 'Promise' || id.simpleName === 'PromiseLike') {
                    return true;
                }
            }
            // Check if it's a parameterized type like Promise<boolean>
            if (returnType.kind === J.Kind.ParameterizedType) {
                const pt = returnType as J.ParameterizedType;
                // ParameterizedType has 'clazz' property which is the base type
                const baseType = (pt as any).clazz;
                if (baseType?.kind === J.Kind.Identifier) {
                    const clazz = baseType as J.Identifier;
                    if (clazz.simpleName === 'Promise' || clazz.simpleName === 'PromiseLike') {
                        return true;
                    }
                }
            }
        }

        // Check type attribution if available
        const funcType = (arrowFunc as any).type;
        if (funcType && functionTypeReturnsPromise(funcType)) {
            return true;
        }
    }

    // Check if it's a function reference (Identifier) with type attribution
    if (element.kind === J.Kind.Identifier) {
        const identifier = element as J.Identifier;
        const funcType = identifier.type;
        if (funcType && functionTypeReturnsPromise(funcType)) {
            return true;
        }
    }

    return false;
}

/**
 * Detects async callbacks passed to synchronous array methods.
 *
 * This is a common bug pattern in JavaScript/TypeScript where async functions
 * are passed to array methods like `.some()`, `.every()`, `.find()`, etc.
 * These methods don't await the promises returned by async callbacks, leading
 * to bugs where Promise objects are treated as truthy values.
 *
 * Example of buggy code:
 * ```typescript
 * // BUG: .some() doesn't await, so any Promise is truthy
 * const hasAdmin = users.some(async user => {
 *     return await checkPermission(user, 'admin');
 * });
 * // hasAdmin is ALWAYS true because Promise objects are truthy!
 *
 * // CORRECT: Use a for loop with await
 * let hasAdmin = false;
 * for (const user of users) {
 *     if (await checkPermission(user, 'admin')) {
 *         hasAdmin = true;
 *         break;
 *     }
 * }
 * ```
 *
 * This recipe reports occurrences but doesn't auto-fix because the correct
 * fix depends on the context (could use for...of loop, Promise.all, etc.).
 */
export class AsyncCallbackInSyncArrayMethod extends Recipe {
    readonly name = "org.openrewrite.javascript.cleanup.async-callback-in-sync-array-method";
    readonly displayName: string = "Detect async callbacks in synchronous array methods";
    readonly description: string = "Detects async callbacks passed to array methods like .some(), .every(), .filter() which don't await promises. This is a common bug where Promise objects are always truthy.";
    readonly tags = ["javascript", "typescript", "async", "bug", "cleanup"];

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JavaScriptVisitor<ExecutionContext> {
            override async visitMethodInvocation(method: J.MethodInvocation, ctx: ExecutionContext): Promise<J | undefined> {
                let m = await super.visitMethodInvocation(method, ctx) as J.MethodInvocation;

                const methodName = m.name?.simpleName;
                if (!methodName || !SYNC_ARRAY_METHODS.has(methodName)) {
                    return m;
                }

                // Check the arguments for async callbacks
                const args = m.arguments?.elements;
                if (!args || args.length === 0) {
                    return m;
                }

                const firstArg = args[0];
                if (callbackReturnsPromise(firstArg)) {
                    return markupWarn(
                        m,
                        `Async callback passed to .${methodName}()`,
                        `Array methods like .${methodName}() don't await async callbacks, so Promises are treated as truthy values. Consider using a for...of loop with await instead.`
                    );
                }

                return m;
            }
        }();
    }
}
