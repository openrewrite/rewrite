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
import {check, ExecutionContext, Recipe, RecipeMarketplace, TreeVisitor} from "@openrewrite/rewrite";
import {prepareJavaRecipe} from "@openrewrite/rewrite/rpc";
import {FindIdentifier} from "./search-recipe";

export async function activate(marketplace: RecipeMarketplace) {
    await marketplace.install(FindIdentifierGatedByJavaRecipe, []);
}

/**
 * Gates a JS editor on a Java recipe, the shape of `check(usesType(...), editor)` when the
 * gate is a recipe rather than a `RecipeRef`.
 */
class FindIdentifierGatedByJavaRecipe extends Recipe {
    name = "org.openrewrite.example.npm.find-identifier-gated-by-java-recipe";
    displayName = "Find identifier gated by a Java recipe";
    description = "Find identifiers in files a Java search recipe matches.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return check(
            await prepareJavaRecipe("org.openrewrite.text.Find", {find: "gate"}),
            new FindIdentifier({identifier: "x"}).editor()
        );
    }
}
