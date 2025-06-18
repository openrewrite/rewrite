/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {JavaScriptVisitor} from './visitor';
import {J} from '../java';
import {Cursor, isTree, Tree} from "../tree";

/**
 * A visitor that compares two AST trees in lock step.
 * It takes another `J` instance as context and visits both trees simultaneously.
 * The visit operation is aborted when the nodes don't match.
 */
export class JavaScriptComparatorVisitor extends JavaScriptVisitor<J> {
    /**
     * Flag indicating whether the trees match so far
     */
    private match: boolean = true;

    /**
     * Creates a new comparator visitor.
     */
    constructor() {
        super();
    }

    /**
     * Compares two AST trees.
     * 
     * @param tree1 The first tree to compare
     * @param tree2 The second tree to compare
     * @returns true if the trees match, false otherwise
     */
    async compare(tree1: J, tree2: J): Promise<boolean> {
        this.match = true;
        await this.visit(tree1, tree2);
        return this.match;
    }

    /**
     * Checks if two nodes have the same kind.
     * 
     * @param j The node being visited
     * @param other The other node to compare with
     * @returns true if the nodes have the same kind, false otherwise
     */
    protected hasSameKind(j: J, other: J): boolean {
        return j.kind === other.kind;
    }

    /**
     * Aborts the visit operation by setting the match flag to false.
     */
    private abort(): void {
        this.match = false;
    }

    override async visit<R extends J>(j: Tree, p: J, parent?: Cursor): Promise<R | undefined> {
        // If we've already found a mismatch, abort further processing
        if (!this.match) {
            return j as R;
        }

        // Check if the nodes have the same kind
        if (!this.hasSameKind(j as J, p)) {
            this.abort();
            return j as R;
        }

        if (j.kind === J.Kind.Identifier) {
            return await this.visitIdentifier(j as J.Identifier, p) as R | undefined;
        }

        for (const key of Object.keys(j)) {
            const value = (j as any)[key];
            const otherValue = (p as any)[key] as J;

            if (isTree(value) && isTree(otherValue)) {
                await this.visit(value, otherValue, parent);
            } else if (["value", "valueSource", "simpleName", "terminatedWithSemicolon", "parenthesized", "codePoint", "keyword", "implicit", "text", "typeOnly", "exportEquals", "multiline", "element"].includes(key)) {
                if (value !== otherValue) {
                    this.abort();
                }
            } else if (value && [J.Kind.LeftPadded, J.Kind.RightPadded, J.Kind.Container].includes(value.kind)) {
                await this.visit(value, otherValue, parent);
            } else if (Array.isArray(value) && Array.isArray(otherValue)) { // TODO check key name?
                if (value.length !== otherValue.length) {
                    this.abort();
                } else {
                    // Visit each element in the arrays
                    for (let i = 0; i < value.length; i++) {
                        await this.visit(value[i], otherValue[i] as J, parent);
                    }
                }
            }
        }
        return j as R;
    }

    override async visitIdentifier(identifier: J.Identifier, other: J): Promise<J | undefined> {
        if (!this.match || other.kind !== J.Kind.Identifier) {
            this.abort();
            return identifier;
        }

        const otherIdentifier = other as J.Identifier;
        if (identifier.simpleName !== otherIdentifier.simpleName) {
            this.abort();
        }

        return identifier;
    }
}
