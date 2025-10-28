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
import {JavaScriptVisitor, JS} from "../../../javascript";
import {J} from "../../../java";
import {produce} from "immer";

export class ExportAssignmentToExportDefault extends Recipe {
    name = "org.openrewrite.javascript.migrate.typescript.export-assignment-to-export-default";
    displayName = "Convert `export =` to `export default`";
    description = "Converts TypeScript `export =` syntax to ES module `export default` syntax for compatibility with ECMAScript modules.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JavaScriptVisitor<ExecutionContext> {

            protected async visitExportAssignment(exportAssignment: JS.ExportAssignment, p: ExecutionContext): Promise<J | undefined> {
                // Only transform export = to export default
                if (exportAssignment.exportEquals) {
                    return produce(exportAssignment, draft => {
                        draft.exportEquals = false;
                    });
                }
                return exportAssignment;
            }
        }
    }
}
