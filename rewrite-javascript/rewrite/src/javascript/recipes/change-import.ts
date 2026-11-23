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

import { Option, Recipe } from "../../recipe";
import { TreeVisitor } from "../../visitor";
import { ExecutionContext } from "../../execution";
import { JavaScriptVisitor, JS } from "../index";
import { maybeRebind } from "../binding";
import { J } from "../../java";

/**
 * Changes an import from one module to another, updating all type attributions.
 *
 * This recipe is useful for:
 * - Library migrations (e.g., moving `act` from `react-dom/test-utils` to `react`)
 * - Module restructuring (e.g., split packages)
 * - Renaming exported members
 *
 * @example
 * // Migrate act import from react-dom/test-utils to react
 * const recipe = new ChangeImport({
 *     oldModule: "react-dom/test-utils",
 *     oldMember: "act",
 *     newModule: "react"
 * });
 * // Before: import { act } from 'react-dom/test-utils';
 * // After:  import { act } from 'react';
 *
 * @example
 * // Rename a member, carrying the references that resolve to it
 * const recipe = new ChangeImport({
 *     oldModule: "lodash",
 *     oldMember: "extend",
 *     newModule: "lodash",
 *     newMember: "assign"
 * });
 * // Before: import { extend } from 'lodash';  extend({}, {});
 * // After:  import { assign } from 'lodash';  assign({}, {});
 * // Adding `newAlias: "extend"` instead binds it as `import { assign as extend }`.
 */
export class ChangeImport extends Recipe {
    readonly name = "org.openrewrite.javascript.change-import";
    readonly displayName = "Change import";
    readonly description = "Changes an import from one module/member to another, updating all type attributions.";

    @Option({
        displayName: "Old module",
        description: "The module to change imports from",
        example: "react-dom/test-utils"
    })
    oldModule!: string;

    @Option({
        displayName: "Old member",
        description: "The member to change (or 'default' for default imports, '*' for namespace imports)",
        example: "act"
    })
    oldMember!: string;

    @Option({
        displayName: "New module",
        description: "The module to change imports to",
        example: "react"
    })
    newModule!: string;

    @Option({
        displayName: "New member",
        description: "The new member name. If not specified, keeps the same member name.",
        example: "act",
        required: false
    })
    newMember?: string;

    @Option({
        displayName: "New alias",
        description: "The local name to bind the new member under. Defaults to the alias the import " +
            "already had, or to the new member name where it had none.",
        example: "act",
        required: false
    })
    newAlias?: string;

    constructor(options?: {
        oldModule?: string;
        oldMember?: string;
        newModule?: string;
        newMember?: string;
        newAlias?: string;
    }) {
        super(options);
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        const oldModule = this.oldModule;
        const oldMember = this.oldMember;
        const newModule = this.newModule;
        const newMember = this.newMember ?? oldMember;
        const newAlias = this.newAlias;

        return new class extends JavaScriptVisitor<ExecutionContext> {
            override async visitJsCompilationUnit(cu: JS.CompilationUnit, ctx: ExecutionContext): Promise<J | undefined> {
                maybeRebind(this, {
                    from: {module: oldModule, member: oldMember},
                    to: {module: newModule, member: newMember, alias: newAlias}
                });
                return super.visitJsCompilationUnit(cu, ctx);
            }
        }();
    }
}
