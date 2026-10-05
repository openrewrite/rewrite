/*
 * Copyright 2026 the original author or authors.
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
import {check, ExecutionContext, not, Option, Recipe, TreeVisitor} from "@openrewrite/rewrite";
import {isVendoredOrBundled} from "@openrewrite/rewrite/javascript";
import {FindIdentifier} from "./search-recipe";

export class FindIdentifierOutsideVendoredOrBundled extends Recipe {
    name = "org.openrewrite.example.javascript.find-identifier-outside-vendored-or-bundled"
    displayName = "Find identifier outside vendored or bundled code";
    description = "Find identifiers, skipping vendored, bundled and build-output files.";

    @Option({
        displayName: "Identifier",
        description: "The identifier to find."
    })
    identifier!: string;

    constructor(options: { identifier: string }) {
        super(options);
    }

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return check(
            not(isVendoredOrBundled()),
            new FindIdentifier({identifier: this.identifier}).editor()
        );
    }
}
