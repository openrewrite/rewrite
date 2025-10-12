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
    Generate, GenerateResponse,
    GetObject,
    GetRecipes,
    Parse,
    PrepareRecipe,
    PrepareRecipeResponse,
    Print,
    Visit,
    VisitResponse
} from "./request";
import {initializeMetricsCsv} from "./request/metrics";
import {RpcObjectData, RpcObjectState, RpcReceiveQueue} from "./queue";
import {RpcRecipe} from "./recipe";
import {ExecutionContext} from "../execution";
import {InstallRecipes, InstallRecipesResponse} from "./request/install-recipes";
import {ParserInput} from "../parser";
import {ReferenceMap} from "../reference";
import {Writable} from "node:stream";
import {GetLanguages} from "./request/get-languages";

export class RewriteRpc {
    private static _global?: RewriteRpc;

    private readonly snowflake = SnowflakeId();

    readonly localObjects: Map<string, ((input: string) => any) | any> = new Map();
    /* A reverse map of the objects back to their IDs */
    private readonly localObjectIds = new IdentityMap();

    readonly remoteObjects: Map<string, any> = new Map();
    readonly remoteRefs: Map<number, any> = new Map();
    readonly localRefs: ReferenceMap = new ReferenceMap();

    private remoteLanguages?: string[];

    constructor(readonly connection: MessageConnection = rpc.createMessageConnection(
                    new rpc.StreamMessageReader(process.stdin),
                    new rpc.StreamMessageWriter(process.stdout),
                ),
                private readonly options: {
                    batchSize?: number,
                    registry?: RecipeRegistry,
                    logger?: rpc.Logger,
                    metricsCsv?: string,
                    traceGetObjectOutput?: boolean,
                    traceGetObjectInput?: Writable,
                    recipeInstallDir?: string
                }) {
        // Initialize metrics CSV file if configured
        initializeMetricsCsv(options.metricsCsv, options.logger);

        const preparedRecipes: Map<String, Recipe> = new Map();
        const recipeCursors: WeakMap<Recipe, Cursor> = new WeakMap()

        // Need this indirection, otherwise `this` will be undefined when executed in the handlers.
        const getObject = (id: string, sourceFileType?: string) => this.getObject(id, sourceFileType);
        const getCursor = (cursorIds: string[] | undefined, sourceFileType?: string) => this.getCursor(cursorIds, sourceFileType);

        const registry = options.registry || new RecipeRegistry();

        Visit.handle(this.connection, this.localObjects, preparedRecipes, recipeCursors, getObject, getCursor, options.metricsCsv);
        Generate.handle(this.connection, this.localObjects, preparedRecipes, recipeCursors, getObject, options.metricsCsv);
        GetObject.handle(this.connection, this.remoteObjects, this.localObjects,
            this.localRefs, options?.batchSize || 200, !!options?.traceGetObjectOutput, options.metricsCsv);
        GetRecipes.handle(this.connection, registry, options.metricsCsv);
        GetLanguages.handle(this.connection, options.metricsCsv);
        PrepareRecipe.handle(this.connection, registry, preparedRecipes, options.metricsCsv);
        Parse.handle(this.connection, this.localObjects, options.metricsCsv);
        Print.handle(this.connection, getObject, options.metricsCsv);
        InstallRecipes.handle(this.connection, options.recipeInstallDir ?? ".rewrite", registry, options.logger, options.metricsCsv);

        this.connection.listen();
    }

    static set(value: RewriteRpc) {
        this._global = value;
    }

    static get(): RewriteRpc | undefined {
        return this._global;
    }

    end(): RewriteRpc {
        this.connection.end();
        return this;
    }

    async getObject<P>(id: string, sourceFileType?: string): Promise<P> {
        const localObject = this.localObjects.get(id);

        const q = new RpcReceiveQueue(this.remoteRefs, sourceFileType, () => {
            return this.connection.sendRequest(
                new rpc.RequestType<GetObject, RpcObjectData[], Error>("GetObject"),
                new GetObject(id, sourceFileType)
            );
        }, this.options.traceGetObjectInput);

        const remoteObject = await q.receive<P>(localObject);

        const eof = (await q.take());
        if (eof.state !== RpcObjectState.END_OF_OBJECT) {
            throw new Error(`Expected END_OF_OBJECT but got: ${eof.state}`);
        }

        this.remoteObjects.set(id, remoteObject);
        this.localObjects.set(id, remoteObject);

        return remoteObject;
    }

    async getCursor(cursorIds: string[] | undefined, sourceFileType?: string): Promise<Cursor> {
        let cursor = rootCursor();
        if (cursorIds) {
            for (let i = cursorIds.length - 1; i >= 0; i--) {
                const cursorObject = await this.getObject(cursorIds[i], sourceFileType);
                this.remoteObjects.set(cursorIds[i], cursorObject);
                cursor = new Cursor(cursorObject, cursor);
            }
        }
        return cursor;
    }

    async parse(inputs: ParserInput[], sourceFileType: string, relativeTo?: string): Promise<SourceFile[]> {
        const parsed: SourceFile[] = [];
        for (const g of await this.connection.sendRequest(
            new rpc.RequestType<Parse, string[], Error>("Parse"),
            new Parse(inputs, relativeTo)
        )) {
            parsed.push(await this.getObject(g, sourceFileType));
        }
        return parsed;
    }

    async print(tree: SourceFile): Promise<string>;
    async print(tree: Tree, cursor: Cursor): Promise<string>;
    async print(tree: Tree, cursor?: Cursor): Promise<string> {
        if (!cursor && !isSourceFile(tree)) {
            throw new Error("Cursor is required for non-SourceFile trees");
        }
        this.localObjects.set(tree.id.toString(), tree);
        return await this.connection.sendRequest(
            new rpc.RequestType<Print, string, Error>("Print"),
            new Print(tree.id, isSourceFile(tree) ? tree.kind :
                cursor!.firstEnclosing(t => isSourceFile(t))!.kind)
        );
    }

    async languages(): Promise<string[]> {
        if (!this.remoteLanguages) {
            this.remoteLanguages = await this.connection.sendRequest(
                new rpc.RequestType0<string[], Error>("GetLanguages")
            );
        }
        return this.remoteLanguages;
    }

    async recipes(): Promise<({ name: string } & RecipeDescriptor)[]> {
        return await this.connection.sendRequest(
            new rpc.RequestType0<({ name: string } & RecipeDescriptor)[], Error>("GetRecipes")
        );
    }

    async prepareRecipe(id: string, options?: any): Promise<RpcRecipe> {
        const response = await this.connection.sendRequest(
            new rpc.RequestType<PrepareRecipe, PrepareRecipeResponse, Error>("PrepareRecipe"),
            new PrepareRecipe(id, options)
        );
        return new RpcRecipe(this, response.id, response.descriptor, response.editVisitor,
            response.scanVisitor);
    }

    async visit(tree: Tree, visitorName: string, p: any, cursor?: Cursor): Promise<Tree> {
        this.localObjects.set(tree.id.toString(), tree);
        const pId = this.localObject(p);
        const cursorIds = this.getCursorIds(cursor);

        const sourceFileType = isSourceFile(tree) ? tree.kind :
            cursor!.firstEnclosing(t => isSourceFile(t))!.kind;

        const response = await this.connection.sendRequest(
            new rpc.RequestType<Visit, VisitResponse, Error>("Visit"),
            new Visit(visitorName, sourceFileType, undefined, tree.id.toString(), pId, cursorIds)
        );
        return response.modified ? this.getObject(tree.id.toString(), sourceFileType) : tree;
    }

    async generate(remoteRecipeId: string, ctx: ExecutionContext): Promise<SourceFile[]> {
        const ctxId = this.localObject(ctx);
        const generated: SourceFile[] = [];
        const response = await this.connection.sendRequest(
            new rpc.RequestType<Generate, GenerateResponse, Error>("Generate"),
            new Generate(remoteRecipeId, ctxId)
        );
        for (let i = 0; i < response.ids.length; i++) {
            generated.push(await this.getObject(response.ids[i], response.sourceFileTypes[i]));
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
