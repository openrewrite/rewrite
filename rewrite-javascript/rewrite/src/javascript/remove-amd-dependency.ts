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
import {J} from "../java";
import {JavaScriptVisitor} from "./visitor";
import {amdBlockOf, DEFAULT_AMD_CALLEES, dependencyNames, parameterNames, withoutDependencyAt} from "./amd";
import {references} from "./bind-module";

/**
 * The AMD counterpart to `RemoveImport`: drops a dependency the block's body no longer names,
 * along with the parameter bound to it. A dependency the factory takes no parameter for is
 * loaded for its side effects and stays, matching `bindAmd`'s own read of such a block.
 */
export class RemoveAmdDependency<P> extends JavaScriptVisitor<P> {
    constructor(readonly module: string, readonly callees: readonly string[] = DEFAULT_AMD_CALLEES) {
        super();
    }

    override async visitMethodInvocation(m: J.MethodInvocation, p: P): Promise<J | undefined> {
        const visited = await super.visitMethodInvocation(m, p) as J.MethodInvocation;
        const block = amdBlockOf(visited, this.callees);
        if (block === undefined) {
            return visited;
        }
        const index = dependencyNames(block).indexOf(this.module);
        if (index < 0) {
            return visited;
        }
        const binding = parameterNames(block)[index];
        if (binding === undefined || await references(block, binding, true)) {
            return visited;
        }
        return withoutDependencyAt(visited, block, index);
    }
}
