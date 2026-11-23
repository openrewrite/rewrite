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
import {Recipe} from "@openrewrite/rewrite";
import {ChangeText} from "./change-text";

/**
 * A composite whose recipeList contains a child left without its required `text` option. Used to
 * verify that PrepareRecipe validates required options recursively through the whole tree, so a
 * broken child is rejected rather than silently prepared.
 */
export class CompositeWithInvalidChild extends Recipe {
    name = "org.openrewrite.example.text.composite-with-invalid-child";
    displayName = "Composite with an invalid child";
    description = "A composite whose child is missing a required option.";

    async recipeList(): Promise<Recipe[]> {
        return [new ChangeText({} as { text: string })];
    }
}
