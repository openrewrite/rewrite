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
import {Recipe} from "../recipe";
import {SnowflakeId} from "@akashrajpurohit/snowflake-id";
import {
    Generate,
    GenerateResponse,
    GetMarketplace,
    GetMarketplaceResponseRow,
    GetObject,
    Parse,
    ParseProject,
    PrepareRecipe,
    PrepareRecipeResponse,
    Print,
    toMarketplace,
    TraceGetObject,
    Visit,
    VisitResponse
} from "./request";
import {RecipeMarketplace} from "../marketplace";
import {initializeMetricsCsv} from "./request/metrics";
import {RpcRawMessage, RpcReceiveQueue} from "./queue";
import {RpcRecipe} from "./recipe";
import {ExecutionContext} from "../execution";
import {InstallRecipes, InstallRecipesResponse} from "./request/install-recipes";
import {ParserInput} from "../parser";
import {ReferenceMap} from "../reference";
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

    private readonly preparedRecipes: Map<String, Recipe> = new Map();
    private readonly recipeCursors: WeakMap<Recipe, Cursor> = new WeakMap();

    private remoteLanguages?: string[];
    private readonly logger?: rpc.Logger;
    private traceGetObject: TraceGetObject = {receive: false, send: false};

    constructor(readonly connection: MessageConnection = rpc.createMessageConnection(
                    new rpc.StreamMessageReader(process.stdin),
                    new rpc.StreamMessageWriter(process.stdout),
                ),
                options: {
                    batchSize?: number,
                    marketplace?: RecipeMarketplace,
                    logger?: rpc.Logger,
                    metricsCsv?: string,
                    recipeInstallDir?: string
                }) {
        // Initialize metrics CSV file if configured
        initializeMetricsCsv(options.metricsCsv, options.logger);
        this.logger = options.logger;

        // Need this indirection, otherwise `this` will be undefined when executed in the handlers.
        const getObject = (id: string, sourceFileType?: string) => this.getObject(id, sourceFileType);
        const getCursor = (cursorIds: string[] | undefined, sourceFileType?: string) => this.getCursor(cursorIds, sourceFileType);
        const traceGetObject = () => this.traceGetObject.send;

        const marketplace = options.marketplace || new RecipeMarketplace();

        Visit.handle(this.connection, this.localObjects, this.preparedRecipes, this.recipeCursors, getObject, getCursor, options.metricsCsv);
        Generate.handle(this.connection, this.localObjects, this.preparedRecipes, this.recipeCursors, getObject, options.metricsCsv);
        GetObject.handle(this.connection, this.remoteObjects, this.localObjects,
            this.localRefs, options?.batchSize || 1000, traceGetObject, options.metricsCsv);
        GetMarketplace.handle(this.connection, marketplace, options.metricsCsv);
        GetLanguages.handle(this.connection, options.metricsCsv);
        PrepareRecipe.handle(this.connection, marketplace, this.preparedRecipes, options.metricsCsv);
        Parse.handle(this.connection, this.localObjects, options.metricsCsv);
        ParseProject.handle(this.connection, this.localObjects, options.metricsCsv);
        Print.handle(this.connection, getObject, options.logger, options.metricsCsv);
        InstallRecipes.handle(this.connection, options.recipeInstallDir ?? ".rewrite", marketplace, options.logger, options.metricsCsv);

        this.connection.onRequest(
            new rpc.RequestType<TraceGetObject, boolean, Error>("TraceGetObject"),
            async (request) => {
                this.traceGetObject = request;
                return true;
            }
        );

        this.connection.onRequest(
            new rpc.RequestType0<boolean, Error>("Reset"),
            async () => {
                this.reset();
                return true;
            }
        );

        RewriteRpc.set(this);
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

    /**
     * Resets all caches. Used for benchmarking to ensure a clean state between runs.
     */
    reset(): void {
        this.localObjects.clear();
        this.localObjectIds.clear();
        this.remoteObjects.clear();
        this.remoteRefs.clear();
        this.localRefs.clear();
        this.preparedRecipes.clear();
        // WeakMap doesn't have clear(), but since preparedRecipes is cleared,
        // the recipes will be garbage collected and so will their cursor entries
        this.remoteLanguages = undefined;

        // Trigger garbage collection if available (requires --expose-gc flag)
        if (typeof global.gc === 'function') {
            global.gc();
        }
    }

    private static totalItems = 0;
    private static totalCalls = 0;

    static getStats(): {calls: number, items: number, avgItemsPerCall: number} {
        return {
            calls: RewriteRpc.totalCalls,
            items: RewriteRpc.totalItems,
            avgItemsPerCall: RewriteRpc.totalCalls > 0 ? Math.round(RewriteRpc.totalItems / RewriteRpc.totalCalls) : 0
        };
    }

    async getObject<P>(id: string, sourceFileType?: string): Promise<P> {
        // Fetch data in batches - each batch is an array of compact arrays: [[state, valueType, value, ref?, trace?], ...]
        // Keep fetching until we receive END_OF_OBJECT
        const batches: RpcRawMessage[][] = [];
        const request = new GetObject(id, sourceFileType);
        const requestType = new rpc.RequestType<GetObject, RpcRawMessage[], Error>("GetObject");

        let batchCount = 0;
        const maxBatches = 100000; // Safety limit to prevent infinite loops

        while (true) {
            if (batchCount >= maxBatches) {
                throw new Error(`Exceeded max batch limit (${maxBatches}) for object ${id}`);
            }

            const batch = await this.connection.sendRequest(requestType, request);
            RewriteRpc.totalCalls++;
            batchCount++;

            if (batch.length === 0) {
                throw new Error(`Empty batch received for object ${id} after ${batchCount} batches`);
            }

            batches.push(batch);

            // Check if the last element in this batch is END_OF_OBJECT
            if (RpcReceiveQueue.isComplete(batch)) {
                break;
            }
        }

        // Create queue with batches - no copying needed
        const q = new RpcReceiveQueue(batches, this.remoteRefs, sourceFileType);

        // Deserialize synchronously - q.receive() will look up the codec from
        // the RpcCodecs registry based on the object's kind and sourceFileType
        const localObject = this.localObjects.get(id);
        const remoteObject = q.receive<P>(localObject);

        // Verify END_OF_OBJECT was reached
        q.verifyComplete();

        this.remoteObjects.set(id, remoteObject);
        this.localObjects.set(id, remoteObject);

        return remoteObject!;
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
        const sourceFile = isSourceFile(tree) ? tree : cursor!.firstEnclosing(t => isSourceFile(t))!;
        return await this.connection.sendRequest(
            new rpc.RequestType<Print, string, Error>("Print"),
            new Print(tree.id, sourceFile.kind)
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

    async marketplace(): Promise<RecipeMarketplace> {
        const rows = await this.connection.sendRequest(
            new rpc.RequestType0<GetMarketplaceResponseRow[], Error>("GetMarketplace")
        );
        return await toMarketplace(rows);
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

    clear(): void {
        this.objectMap = new WeakMap<any, string>();
        this.primitiveMap.clear();
    }
}
