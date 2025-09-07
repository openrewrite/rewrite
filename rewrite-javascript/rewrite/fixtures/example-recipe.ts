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
import {RecipeRegistry} from "@openrewrite/rewrite";
import {FindIdentifier} from "./search-recipe";
import {CreateText} from "./create-text";
import {ChangeText} from "./change-text";
import {ChangeVersion} from "./change-version";
import {RecipeWithRecipeList} from "./recipe-with-recipe-list";
import {ReplaceId} from "./replace-id";
import {FindIdentifierWithRemotePathPrecondition} from "./remote-path-precondition";
import {FindIdentifierWithPathPrecondition} from "./path-precondition";

export function activate(registry: RecipeRegistry) {
    registry.register(ChangeText);
    registry.register(CreateText);
    registry.register(ChangeVersion);
    registry.register(RecipeWithRecipeList);
    registry.register(ReplaceId);
    registry.register(FindIdentifier);
    registry.register(FindIdentifierWithRemotePathPrecondition);
    registry.register(FindIdentifierWithPathPrecondition);
}
