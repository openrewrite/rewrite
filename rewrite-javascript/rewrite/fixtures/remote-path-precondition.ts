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
import {check, ExecutionContext, Option, Recipe, TreeVisitor} from "@openrewrite/rewrite";
import {FindIdentifier} from "./search-recipe";
import {hasSourcePath} from "@openrewrite/rewrite/javascript";

/**
 * A recipe that finds identifiers in files matching a specific path
 */
export class FindIdentifierWithRemotePathPrecondition extends Recipe {
    name = "org.openrewrite.example.javascript.remote-find-identifier-with-path"
    displayName = "Find identifier with remotely defined path precondition";
    description = "Find identifiers in files with a specific path.";

    @Option({
        displayName: "Required Path",
        description: "The source path that must match."
    })
    requiredPath!: string;

    @Option({
        displayName: "Identifier",
        description: "The identifier to find."
    })
    identifier!: string;

    constructor(options: { requiredPath: string, identifier: string }) {
        super(options);
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        // Use the check function to apply the precondition
        return check(
            hasSourcePath(this.requiredPath),
            new FindIdentifier({identifier: this.identifier}).editor()
        );
    }
}
