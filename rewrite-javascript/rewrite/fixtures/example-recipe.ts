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
import {JavaScript, RecipeMarketplace} from "../src";
import {FindIdentifier} from "./search-recipe";
import {CreateText} from "./create-text";
import {ChangeText} from "./change-text";
import {ChangeVersion} from "./change-version";
import {RecipeWithRecipeList} from "./recipe-with-recipe-list";
import {ReplaceId} from "./replace-id";
import {FindIdentifierWithRemotePathPrecondition} from "./remote-path-precondition";
import {FindIdentifierWithPathPrecondition} from "./path-precondition";
import {MarkTypes} from "./mark-types";
import {MarkPrimitiveTypes} from "./mark-primitive-types";
import {MarkClassTypes} from "./mark-class-types";
import {ScanningEditor} from "./scanning-editor";

export async function activate(marketplace: RecipeMarketplace): Promise<void> {
    await marketplace.install(ChangeText, JavaScript);
    await marketplace.install(CreateText, JavaScript);
    await marketplace.install(ChangeVersion, JavaScript);
    await marketplace.install(RecipeWithRecipeList, JavaScript);
    await marketplace.install(ReplaceId, JavaScript);
    await marketplace.install(FindIdentifier, JavaScript);
    await marketplace.install(FindIdentifierWithRemotePathPrecondition, JavaScript);
    await marketplace.install(FindIdentifierWithPathPrecondition, JavaScript);
    await marketplace.install(MarkTypes, JavaScript);
    await marketplace.install(MarkPrimitiveTypes, JavaScript);
    await marketplace.install(MarkClassTypes, JavaScript);
    await marketplace.install(ScanningEditor, JavaScript);
}
