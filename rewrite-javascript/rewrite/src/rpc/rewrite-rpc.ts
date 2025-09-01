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
import {ReferenceMap} from "../reference";
import {Writable} from "node:stream";

export class RewriteRpc {
    private readonly snowflake = SnowflakeId();

    readonly localObjects: Map<string, ((input: string) => any) | any> = new Map();
    /* A reverse map of the objects back to their IDs */
    private readonly localObjectIds = new IdentityMap();

    readonly remoteObjects: Map<string, any> = new Map();
    readonly remoteRefs: Map<number, any> = new Map();
    readonly localRefs: ReferenceMap = new ReferenceMap();

    constructor(readonly connection: MessageConnection = rpc.createMessageConnection(
                    new rpc.StreamMessageReader(process.stdin),
                    new rpc.StreamMessageWriter(process.stdout),
                ),
                private readonly options: {
                    batchSize?: number,
                    registry?: RecipeRegistry,
                    logger?: rpc.Logger,
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
        GetObject.handle(this.connection, this.remoteObjects, this.localObjects,
            this.localRefs, options?.batchSize || 200, !!options?.traceGetObjectOutput);
        GetRecipes.handle(this.connection, registry);
        PrepareRecipe.handle(this.connection, registry, preparedRecipes);
        Parse.handle(this.connection, this.localObjects);
        Print.handle(this.connection, getObject, getCursor);
        InstallRecipes.handle(this.connection, options.recipeInstallDir ?? ".rewrite", registry, options.logger);

        this.connection.listen();
    }

    end(): RewriteRpc {
        this.connection.end();
        return this;
    }

    async getObject<P>(id: string): Promise<P> {
        const localObject = this.localObjects.get(id);
        const lastKnownId = localObject ? id : undefined;

        const q = new RpcReceiveQueue(this.remoteRefs, () => {
            return this.connection.sendRequest(
                new rpc.RequestType<GetObject, RpcObjectData[], Error>("GetObject"),
                new GetObject(id, lastKnownId)
            );
        }, this.options.traceGetObjectInput);

        const remoteObject = await q.receive<P>(this.localObjects.get(id));

        const eof = (await q.take());
        if (eof.state !== RpcObjectState.END_OF_OBJECT) {
            throw new Error(`Expected END_OF_OBJECT but got: ${eof.state}`);
        }

        this.remoteObjects.set(id, remoteObject);
        this.localObjects.set(id, remoteObject);

        return remoteObject;
    }

    async getCursor(cursorIds: string[] | undefined): Promise<Cursor> {
        let cursor = rootCursor();
        if (cursorIds) {
            for (let i = cursorIds.length - 1; i >= 0; i--) {
                const cursorObject = await this.getObject(cursorIds[i]);
                this.remoteObjects.set(cursorIds[i], cursorObject);
                cursor = new Cursor(cursorObject, cursor);
            }
        }
        return cursor;
    }

    async parse(inputs: ParserInput[], relativeTo?: string): Promise<SourceFile[]> {
        const parsed: SourceFile[] = [];
        for (const g of await this.connection.sendRequest(
            new rpc.RequestType<Parse, string[], Error>("Parse"),
            new Parse(inputs, relativeTo)
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
        this.localObjects.set(tree.id.toString(), tree);
        return await this.connection.sendRequest(
            new rpc.RequestType<Print, string, Error>("Print"),
            new Print(tree.id, this.getCursorIds(cursor))
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
            return this.getObject(tree.id.toString());
        }
        return tree;
    }

    scan(tree: Tree, visitorName: string, p: any, cursor?: Cursor): Promise<VisitResponse> {
        this.localObjects.set(tree.id.toString(), tree);
        const pId = this.localObject(p);
        const cursorIds = this.getCursorIds(cursor);
        return this.connection.sendRequest(
            new rpc.RequestType<Visit, VisitResponse, Error>("Visit"),
            new Visit(visitorName, undefined, tree.id.toString(), pId, cursorIds)
        );
    }

    async generate(remoteRecipeId: string, ctx: ExecutionContext): Promise<SourceFile[]> {
        const ctxId = this.localObject(ctx);
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

    private localObject<P>(obj: P): string {
        let id = this.localObjectIds.get(obj);
        if (!id) {
            id = this.snowflake.generate();
            this.localObjects.set(id, obj);
            this.localObjectIds.set(obj, id);
        }
        return id
    }

    getCursorIds(cursor: Cursor | undefined): string[] | undefined {
        if (cursor) {
            const cursorIds = [];
            for (const c of cursor.asArray()) {
                let id: string;
                if (isTree(c)) {
                    id = (c as Tree).id.toString();
                    this.localObjects.set(id, c);
                } else {
                    id = this.localObject(c);
                }
                cursorIds.push(id);
            }
            return cursorIds
        }
    }
}

class IdentityMap {
    constructor(private objectMap = new WeakMap<any, string>(),
                private readonly primitiveMap = new Map<any, string>()) {
    }

    set(key: any, value: any): void {
        if (typeof key === 'object' && key !== null) {
            this.objectMap.set(key, value);
        } else {
            this.primitiveMap.set(key, value);
        }
    }

    get(key: any): string | undefined {
        if (typeof key === 'object' && key !== null) {
            return this.objectMap.get(key);
        } else {
            return this.primitiveMap.get(key);
        }
    }

    has(key: any): boolean {
        if (typeof key === 'object' && key !== null) {
            return this.objectMap.has(key);
        } else {
            return this.primitiveMap.has(key);
        }
    }
}
