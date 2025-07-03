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
import * as rpc from "vscode-jsonrpc/node";
import {MessageConnection} from "vscode-jsonrpc/node";
import {Cursor, isSourceFile, isTree, rootCursor, SourceFile, Tree} from "../tree";
import {Recipe, RecipeDescriptor, RecipeRegistry} from "../recipe";
import {SnowflakeId} from "@akashrajpurohit/snowflake-id";
import {
    Generate,
    GetObject,
    GetRecipes,
    GetRef,
    Parse,
    PrepareRecipe,
    PrepareRecipeResponse,
    Print,
    Visit,
    VisitResponse
} from "./request";
import {RpcObjectData, RpcObjectState, RpcReceiveQueue} from "./queue";
import {RpcRecipe} from "./recipe";
import {ExecutionContext} from "../execution";
import {InstallRecipes, InstallRecipesResponse} from "./request/install-recipes";
import {ParserInput} from "../parser";
import {randomId} from "../uuid";
import {ReferenceMap} from "./reference";
import {Writable} from "node:stream";
import {ObjectStore} from "./object-store";

export class RewriteRpc {
    private readonly snowflake = SnowflakeId();

    readonly localObjects: ObjectStore = new ObjectStore(this.snowflake);
    readonly remoteObjects: ObjectStore = new ObjectStore(this.snowflake);

    readonly remoteRefs: Map<number, any> = new Map();
    readonly localRefs: ReferenceMap = new ReferenceMap();

    constructor(readonly connection: MessageConnection = rpc.createMessageConnection(
                    new rpc.StreamMessageReader(process.stdin),
                    new rpc.StreamMessageWriter(process.stdout),
                ),
                private readonly options: {
                    batchSize?: number,
                    registry?: RecipeRegistry,
                    traceGetObjectOutput?: boolean,
                    traceGetObjectInput?: Writable,
                    recipeInstallDir?: string
                }) {
        const preparedRecipes: Map<String, Recipe> = new Map();
        const recipeCursors: WeakMap<Recipe, Cursor> = new WeakMap()

        // Need this indirection, otherwise `this` will be undefined when executed in the handlers.
        const getObject = (id: string) => this.getObject(id);
        const getCursor = (cursorIds: string[] | undefined) => this.getCursor(cursorIds);

        const registry = options.registry || new RecipeRegistry();

        Visit.handle(this.connection, this.localObjects, preparedRecipes, recipeCursors, getObject, getCursor);
        Generate.handle(this.connection, this.localObjects, preparedRecipes, recipeCursors, getObject);
        GetObject.handle(this.connection, this.remoteObjects, this.localObjects, this.localRefs, options?.batchSize || 100,
            !!options?.traceGetObjectOutput);
        GetRecipes.handle(this.connection, registry);
        GetRef.handle(this.connection, this.remoteRefs, this.localRefs, options?.batchSize || 100, !!options?.traceGetObjectOutput);
        PrepareRecipe.handle(this.connection, registry, preparedRecipes);
        Parse.handle(this.connection, this.localObjects);
        Print.handle(this.connection, getObject, getCursor);
        InstallRecipes.handle(this.connection, options.recipeInstallDir ?? ".rewrite", registry);

        this.connection.listen();
    }

    end(): RewriteRpc {
        this.connection.end();
        return this;
    }

    async getObject<P>(id: string): Promise<P> {
        const existingRemote = this.remoteObjects.get<P>(id);
        if (existingRemote) {
            return existingRemote;
        }

        const localObject = this.localObjects.get<P>(id);
        
        // Get the lastKnownId from remote ObjectStore using version tracking
        let lastKnownId: string | undefined;
        if (localObject) {
            const intrinsicId = (localObject as any)?.id;
            if (intrinsicId && typeof intrinsicId === 'string') {
                // For objects with intrinsic ID, get the last known version
                const lastVersion = this.remoteObjects.getCurrentVersion(intrinsicId);
                if (lastVersion) {
                    lastKnownId = `${intrinsicId}@${lastVersion}`;
                }
            }
        }

        const q = new RpcReceiveQueue(this.remoteRefs, () => {
            return this.connection.sendRequest(
                new rpc.RequestType<GetObject, RpcObjectData[], Error>("GetObject"),
                new GetObject(id, lastKnownId)
            );
        }, this.options.traceGetObjectInput, (refId: number) => this.getRef(refId));

        const remoteObject = await q.receive<P>(localObject);

        const eof = (await q.take());
        if (eof.state !== RpcObjectState.END_OF_OBJECT) {
            throw new Error(`Expected END_OF_OBJECT but got: ${eof.state}`);
        }

        this.remoteObjects.store(remoteObject, id);
        this.localObjects.store(remoteObject, id);

        return remoteObject;
    }

    async getCursor(cursorIds: string[] | undefined): Promise<Cursor> {
        let cursor = rootCursor();
        if (cursorIds) {
            for (let i = cursorIds.length - 1; i >= 0; i--) {
                const cursorObject = await this.getObject(cursorIds[i]);
                this.remoteObjects.store(cursorObject, cursorIds[i]);
                cursor = new Cursor(cursorObject, cursor);
            }
        }
        return cursor;
    }

    async parse(inputs: ParserInput[], relativeTo?: string): Promise<SourceFile[]> {
        const parsed: SourceFile[] = [];
        // FIXME properly handle multiple results
        for (const g of await this.connection.sendRequest(
            new rpc.RequestType<Parse, string[], Error>("Parse"),
            new Parse(randomId(), inputs, relativeTo)
        )) {
            parsed.push(await this.getObject(g));
        }
        return parsed;
    }

    async print(tree: SourceFile): Promise<string>;
    async print<T extends Tree>(tree: T, cursor?: Cursor): Promise<string> {
        if (!cursor && !isSourceFile(tree)) {
            throw new Error("Cursor is required for non-SourceFile trees");
        }
        return await this.connection.sendRequest(
            new rpc.RequestType<Print, string, Error>("Print"),
            new Print(this.localObjects.store(tree), this.getCursorIds(cursor))
        );
    }

    async recipes(): Promise<({ name: string } & RecipeDescriptor)[]> {
        return await this.connection.sendRequest(
            new rpc.RequestType0<({ name: string } & RecipeDescriptor)[], Error>("GetRecipes")
        );
    }

    async prepareRecipe(id: string, options?: any): Promise<Recipe> {
        const response = await this.connection.sendRequest(
            new rpc.RequestType<PrepareRecipe, PrepareRecipeResponse, Error>("PrepareRecipe"),
            new PrepareRecipe(id, options)
        );
        return new RpcRecipe(this, response.id, response.descriptor, response.editVisitor,
            response.scanVisitor);
    }

    async visit(tree: Tree, visitorName: string, p: any, cursor?: Cursor): Promise<Tree> {
        let response = await this.scan(tree, visitorName, p, cursor);
        if (response.modified) {
            return this.getObject(response.afterId!);
        }
        return tree;
    }

    scan(tree: Tree, visitorName: string, p: any, cursor?: Cursor): Promise<VisitResponse> {
        const treeId = this.localObjects.store(tree);
        const pId = this.localObjects.store(p);
        const cursorIds = this.getCursorIds(cursor);
        return this.connection.sendRequest(
            new rpc.RequestType<Visit, VisitResponse, Error>("Visit"),
            new Visit(visitorName, undefined, treeId, pId, cursorIds)
        );
    }

    async generate(remoteRecipeId: string, ctx: ExecutionContext): Promise<SourceFile[]> {
        const ctxId = this.localObjects.store(ctx);
        const generated: SourceFile[] = [];
        for (const g of await this.connection.sendRequest(
            new rpc.RequestType<Generate, string[], Error>("Generate"),
            new Generate(remoteRecipeId, ctxId)
        )) {
            generated.push(await this.getObject(g));
        }
        return generated;
    }

    installRecipes(recipes: string | { packageName: string, version?: string }): Promise<InstallRecipesResponse> {
        return this.connection.sendRequest(
            new rpc.RequestType<InstallRecipes, InstallRecipesResponse, Error>("InstallRecipes"),
            new InstallRecipes(recipes)
        );
    }

    getCursorIds(cursor: Cursor | undefined): string[] | undefined {
        if (cursor) {
            const cursorIds = [];
            for (const c of cursor.asArray()) {
                cursorIds.push(this.localObjects.store(c));
            }
            return cursorIds;
        }
    }

    private async getRef(refId: number): Promise<any> {
        const refData = await this.connection.sendRequest(
            new rpc.RequestType<{refId: string}, RpcObjectData, Error>("GetRef"),
            {refId: refId.toString()}
        );
        if (refData.state === RpcObjectState.DELETE) {
            throw new Error(`Reference ${refId} not found on remote`);
        }
        const ref = refData.value;
        this.remoteRefs.set(refId, ref);
        return ref;
    }
}

