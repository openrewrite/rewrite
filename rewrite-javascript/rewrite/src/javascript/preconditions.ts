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
import {UsesMethod, UsesType} from "./search";
import {ExecutionContext} from "../execution";
import {TreeVisitor} from "../visitor";
import {IsSourceFile} from "../search";
import {RpcRecipe} from "../rpc/recipe";

export function hasSourcePath(filePattern: string): Promise<RpcRecipe> | TreeVisitor<any, ExecutionContext> {
    return RewriteRpc.get() ? RewriteRpc.get()!.prepareRecipe("org.openrewrite.FindSourceFiles", {
        filePattern
    }) : new IsSourceFile(filePattern);
}

export function usesMethod(methodPattern: string, matchOverrides: boolean = false): Promise<RpcRecipe> | TreeVisitor<any, ExecutionContext> {
    return RewriteRpc.get() ? RewriteRpc.get()!.prepareRecipe("org.openrewrite.java.search.HasMethod", {
        methodPattern,
        matchOverrides
    }) : new UsesMethod(methodPattern);
}

export function usesType(fullyQualifiedType: string): Promise<RpcRecipe> | TreeVisitor<any, ExecutionContext> {
    return RewriteRpc.get() ? RewriteRpc.get()!.prepareRecipe("org.openrewrite.java.search.HasType", {
        fullyQualifiedType,
        checkAssignability: false
    }) : new UsesType(fullyQualifiedType);
}
