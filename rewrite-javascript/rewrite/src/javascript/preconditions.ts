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
import {RewriteRpc} from "../rpc";
import {Recipe} from "../recipe";

export async function hasSourcePath(filePattern: string): Promise<Recipe> {
    return RewriteRpc.get().prepareRecipe("org.openrewrite.FindSourceFiles", {filePattern})
}

export async function usesMethod(methodMatcher: string, matchOverrides: boolean = false): Promise<Recipe> {
    return RewriteRpc.get().prepareRecipe("org.openrewrite.java.search.UsesMethod", {methodMatcher, matchOverrides})
}

export async function usesType(fullyQualifiedType: string): Promise<Recipe> {
    return RewriteRpc.get().prepareRecipe("org.openrewrite.java.search.UsesType", {fullyQualifiedType})
}
