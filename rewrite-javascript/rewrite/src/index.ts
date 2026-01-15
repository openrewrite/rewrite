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
import {JavaScript, RecipeMarketplace} from "./marketplace";
import {RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "./rpc";
import {updateIfChanged} from "./util";
import {MarkersKind, ParseExceptionResult} from "./markers";

export * from "./data-table";
export * from "./execution";
export * from "./markers";
export * from "./style";
export * from "./print";
export * from "./tree";
export * from "./visitor";
export * from "./parser";
export * from "./parse-error";
export * from "./preconditions";
export * from "./uuid";
export * from "./util";
export * from "./recipe";
export * from "./run";
export * from "./marketplace";

// Install all recipes in this package
export async function activate(marketplace: RecipeMarketplace): Promise<void> {
    const {
        AddDependency,
        AsyncCallbackInSyncArrayMethod,
        AutoFormat,
        UpgradeDependencyVersion,
        UpgradeTransitiveDependencyVersion,
        OrderImports,
        ChangeImport
    } = await import("./javascript/recipes/index.js");
    await marketplace.install(AddDependency, JavaScript);
    await marketplace.install(AsyncCallbackInSyncArrayMethod, JavaScript);
    await marketplace.install(AutoFormat, JavaScript);
    await marketplace.install(UpgradeDependencyVersion, JavaScript);
    await marketplace.install(UpgradeTransitiveDependencyVersion, JavaScript);
    await marketplace.install(OrderImports, JavaScript);
    await marketplace.install(ChangeImport, JavaScript);

    const {FindDependency, Search} = await import("./javascript/search/index.js");
    await marketplace.install(FindDependency, Search);

    const {
        UseObjectPropertyShorthand,
        PreferOptionalChain,
        AddParseIntRadix,
        Cleanup
    } = await import("./javascript/cleanup/index.js");
    await marketplace.install(UseObjectPropertyShorthand, Cleanup);
    await marketplace.install(PreferOptionalChain, Cleanup);
    await marketplace.install(AddParseIntRadix, Cleanup);

    const {
        ExportAssignmentToExportDefault,
        MigrateTypeScript
    } = await import("./javascript/migrate/typescript/index.js");
    await marketplace.install(ExportAssignmentToExportDefault, MigrateTypeScript);

    const {
        HoistFunctionDeclarationsFromBlocks,
        ModernizeOctalEscapeSequences,
        ModernizeOctalLiterals,
        RemoveDuplicateObjectKeys,
        MigrateES6
    } = await import("./javascript/migrate/es6/index.js");
    await marketplace.install(HoistFunctionDeclarationsFromBlocks, MigrateES6);
    await marketplace.install(ModernizeOctalEscapeSequences, MigrateES6);
    await marketplace.install(ModernizeOctalLiterals, MigrateES6);
    await marketplace.install(RemoveDuplicateObjectKeys, MigrateES6);
}

RpcCodecs.registerCodec(MarkersKind.ParseExceptionResult, {
    async rpcSend(after: ParseExceptionResult, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.parserType);
        await q.getAndSend(after, a => a.exceptionType);
        await q.getAndSend(after, a => a.message);
        await q.getAndSend(after, a => a.treeType);
    },
    async rpcReceive(before: ParseExceptionResult, q: RpcReceiveQueue): Promise<ParseExceptionResult> {
        return updateIfChanged(before, {
            id: await q.receive(before.id),
            parserType: await q.receive(before.parserType),
            exceptionType: await q.receive(before.exceptionType),
            message: await q.receive(before.message),
            treeType: await q.receive(before.treeType),
        });
    }
});
