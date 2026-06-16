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
    GetObject,
    GetMarketplace,
    GetMarketplaceResponseRow,
    toMarketplace,
    Parse,
    ParseProject,
    PrepareRecipe,
    PrepareRecipeResponse,
    Print,
    TraceGetObject,
    Visit,
    VisitResponse,
    BatchVisit
} from "./request";
import {RecipeMarketplace} from "../marketplace";
import {initializeMetricsCsv} from "./request/metrics";
import {RpcObjectData, RpcObjectState, RpcReceiveQueue} from "./queue";
import {RpcRecipe} from "./recipe";
import {ExecutionContext} from "../execution";
import {InstallRecipes, InstallRecipesResponse} from "./request/install-recipes";
import {ParserInput} from "../parser";
import {ReferenceMap} from "../reference";
import {GetLanguages} from "./request/get-languages";

export class RewriteRpc {
    /**
     * Key for the active {@link RewriteRpc} connection on {@link globalThis}.
     *
     * It deliberately lives on `globalThis` rather than as a `static` field:
     * a recipe package that bundles `@openrewrite/rewrite` (or resolves it from
     * its own `node_modules`) loads a *separate copy* of this module, with its
     * own class object and therefore its own statics. A `static` field set by
     * the host would be invisible to such a copy, so `RewriteRpc.get()` (e.g.
     * via `prepareJavaRecipe`) would return `undefined` and throw "no active
     * RewriteRpc connection" — surfacing during `InstallRecipes` as the
     * misleading "Ensure the constructor can be called without any arguments".
     * {@link Symbol.for} resolves to the same symbol across every module copy,
     * so all copies share the one active connection. See gh-7968.
     */
    private static readonly GLOBAL_KEY: symbol = Symbol.for("org.openrewrite.rpc.RewriteRpc.global");

    private readonly snowflake = SnowflakeId();

    readonly localObjects: Map<string, ((input: string) => any) | any> = new Map();
    /* A reverse map of the objects back to their IDs */
    private readonly localObjectIds = new IdentityMap();

    readonly remoteObjects: Map<string, any> = new Map();
    readonly remoteRefs: Map<number, any> = new Map();
    readonly localRefs: ReferenceMap = new ReferenceMap();

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

        const preparedRecipes: Map<String, Recipe> = new Map();
        const recipeCursors: WeakMap<Recipe, Cursor> = new WeakMap()

        // Need this indirection, otherwise `this` will be undefined when executed in the handlers.
        const getObject = (id: string, sourceFileType?: string) => this.getObject(id, sourceFileType);
        const getCursor = (cursorIds: string[] | undefined, sourceFileType?: string) => this.getCursor(cursorIds, sourceFileType);
        const traceGetObject = () => this.traceGetObject.send;

        const marketplace = options.marketplace || new RecipeMarketplace();

        Visit.handle(this.connection, this.localObjects, preparedRecipes, recipeCursors, getObject, getCursor, options.metricsCsv);
        BatchVisit.handle(this.connection, this.localObjects, preparedRecipes, recipeCursors, getObject, getCursor, options.metricsCsv);
        Generate.handle(this.connection, this.localObjects, preparedRecipes, recipeCursors, getObject, options.metricsCsv);
        GetObject.handle(this.connection, this.remoteObjects, this.localObjects,
            this.localRefs, options?.batchSize || 1000, traceGetObject, options.metricsCsv);
        GetMarketplace.handle(this.connection, marketplace, options.metricsCsv);
        GetLanguages.handle(this.connection, options.metricsCsv);
        PrepareRecipe.handle(this.connection, marketplace, preparedRecipes, options.metricsCsv);
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
        )

        // Clears local caches. Captured here so it can close over the
        // constructor-local `preparedRecipes` while still being callable
        // from the public `reset()` method on the class.
        const clearLocalState = () => {
            this.localObjects.clear();
            this.localObjectIds.clear();
            this.remoteObjects.clear();
            this.remoteRefs.clear();
            this.localRefs.clear();
            preparedRecipes.clear();
            this.remoteLanguages = undefined;
        };
        this.clearLocalState = clearLocalState;

        this.connection.onRequest(
            new rpc.RequestType0<boolean, Error>("Reset"),
            async () => {
                // Inbound Reset only clears local state — never sends a Reset
                // back to the originator. Mirrors the Java handler in
                // RewriteRpc.java around line 222.
                clearLocalState();
                return true;
            }
        )

        RewriteRpc.set(this);
        this.connection.listen();
    }

    private readonly clearLocalState!: () => void;

    /**
     * Reset both the remote and local RPC caches. Sends a `Reset` request to the
     * remote — which clears the remote's state without sending one back — and
     * then clears local caches. Use this between independent operations (e.g.
     * between tests) so accumulated objects and prepared recipes don't leak
     * across boundaries.
     */
    async reset(): Promise<void> {
        await this.connection.sendRequest(
            new rpc.RequestType0<boolean, Error>("Reset"),
        );
        this.clearLocalState();
    }

    static set(value: RewriteRpc) {
        (globalThis as any)[RewriteRpc.GLOBAL_KEY] = value;
    }

    static get(): RewriteRpc | undefined {
        return (globalThis as any)[RewriteRpc.GLOBAL_KEY];
    }

    end(): RewriteRpc {
        this.connection.end();
        return this;
    }

    async getObject<P>(id: string, sourceFileType?: string): Promise<P> {
        // Use the last synced state as the baseline for receiving diffs.
        // This must match what the remote used as its baseline when computing the diff.
        // Using localObjects here would be wrong if the local side modified the tree
        // (e.g., via a local recipe) since the remote doesn't know about those changes.
        const before = this.remoteObjects.get(id);

        const q = new RpcReceiveQueue(this.remoteRefs, sourceFileType, () => {
            return this.connection.sendRequest(
                new rpc.RequestType<GetObject, RpcObjectData[], Error>("GetObject"),
                new GetObject(id, sourceFileType),
            );
        }, this.logger, this.traceGetObject.receive);

        let remoteObject: P;
        try {
            remoteObject = await q.receive<P>(before as P);
        } catch (e) {
            // Reset our tracking of the remote state so the next interaction
            // forces a full object sync (ADD) instead of a delta (CHANGE).
            this.remoteObjects.delete(id);
            throw e;
        }

        const eof = (await q.take());
        if (eof.state !== RpcObjectState.END_OF_OBJECT) {
            RpcObjectData.logTrace(eof, this.traceGetObject.receive, this.logger);
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
