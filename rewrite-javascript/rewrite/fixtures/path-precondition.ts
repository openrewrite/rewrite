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
import {
    check,
    ExecutionContext,
    foundSearchResult,
    isSourceFile,
    Option,
    Recipe,
    TreeVisitor
} from "@openrewrite/rewrite";
import {FindIdentifier} from "./search-recipe";

/**
 * A visitor that adds a search result if the source file has a specific path
 */
export class PathPreconditionVisitor extends TreeVisitor<any, ExecutionContext> {
    constructor(private readonly requiredPath: string) {
        super();
    }

    async visit<R extends any>(tree: any, _: ExecutionContext): Promise<R | undefined> {
        if (isSourceFile(tree) && tree.sourcePath === this.requiredPath) {
            // Add a search result to indicate the precondition passed
            return foundSearchResult(tree) as R;
        }
        // Return unchanged to indicate the precondition failed
        return tree as R;
    }
}

/**
 * A recipe that finds identifiers in files matching a specific path
 */
export class FindIdentifierWithPathPrecondition extends Recipe {
    name = "org.openrewrite.example.javascript.find-identifier-with-path"
    displayName = "Find identifier with path precondition";
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
        const precondition = new PathPreconditionVisitor(this.requiredPath);
        const findIdentifier = new FindIdentifier({identifier: this.identifier});

        // Use the check function to apply the precondition
        return await check(precondition, findIdentifier.editor());
    }
}

/**
 * A recipe that conditionally finds identifiers based on a boolean flag
 */
export class ConditionalFindIdentifier extends Recipe {
    name = "org.openrewrite.example.javascript.conditional-find-identifier"
    displayName = "Conditionally find identifier";
    description = "Find identifiers based on a boolean condition.";

    @Option({
        displayName: "Should Search",
        description: "Whether to search for identifiers or not."
    })
    shouldSearch!: boolean;

    @Option({
        displayName: "Identifier",
        description: "The identifier to find."
    })
    identifier!: string;

    constructor(options: { shouldSearch: boolean, identifier: string }) {
        super(options);
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        const findIdentifier = new FindIdentifier({identifier: this.identifier});

        // Use the check function with a boolean condition
        return check(this.shouldSearch, findIdentifier.editor());
    }
}
