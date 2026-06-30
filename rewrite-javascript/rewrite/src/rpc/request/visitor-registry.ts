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
import type {TreeVisitor} from "../../visitor";

export type VisitorConstructor = new (...args: any[]) => TreeVisitor<any, any>;

const VISITOR_REGISTRY: Record<string, VisitorConstructor> = {};

/**
 * Registers a visitor so it can be invoked directly by its fully-qualified (Java-style)
 * name over RPC, rather than as part of a prepared recipe. This lets a language service on
 * the Java side (e.g. {@code JavaScriptAutoFormatService}) delegate to a TypeScript visitor
 * by name. Language modules register their visitors from their own RPC setup, keeping the
 * core RPC layer free of language-specific imports.
 */
export function registerVisitor(name: string, ctor: VisitorConstructor): void {
    VISITOR_REGISTRY[name] = ctor;
}

export function lookupVisitor(name: string): VisitorConstructor | undefined {
    return VISITOR_REGISTRY[name];
}
