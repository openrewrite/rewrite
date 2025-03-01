import * as rpc from "vscode-jsonrpc/node";
import {MessageConnection} from "vscode-jsonrpc/node";
import {Cursor, isTree, rootCursor, Tree} from "../tree";
import {Recipe} from "../recipe";
import {SnowflakeId} from "@akashrajpurohit/snowflake-id";
import {Generate, GetObject, getRecipes, PrepareRecipe, Print, Visit, VisitResponse} from "./request";
import {RpcObjectData, RpcObjectState, RpcReceiveQueue} from "./queue";
import {RpcCodecs} from "./codec";

export class RewriteRpc {
    private readonly snowflake = SnowflakeId();

    private readonly connection: MessageConnection = rpc.createMessageConnection(
        new rpc.StreamMessageReader(process.stdin),
        new rpc.StreamMessageWriter(process.stdout))

    private readonly localObjects: Map<string, any> = new Map();
    /* A reverse map of the objects back to their IDs */
    private readonly localObjectIds: WeakMap<any, string> = new WeakMap()

    private readonly remoteObjects: Map<string, any> = new Map();
    private readonly remoteRefs: Map<number, any> = new Map();

    constructor() {
        const preparedRecipes: Map<String, Recipe> = new Map();
        const recipeCursors: WeakMap<Recipe, Cursor> = new WeakMap()

        Visit.handle(this.connection, this.localObjects,
            preparedRecipes, recipeCursors, this.getObject,
            this.getCursor);
        Generate.handle(this.connection, this.localObjects,
            preparedRecipes, recipeCursors, this.getObject);
        GetObject.handle(this.connection, this.remoteObjects, this.localObjects);
        getRecipes(this.connection);
        PrepareRecipe.handle(this.connection, preparedRecipes);
        Print.handle(this.connection, this.getObject, this.getCursor);

        this.connection.listen();
    }

    async getObject(id: string): Promise<any> {
        const q = new RpcReceiveQueue(this.remoteRefs, () => {
            return this.connection.sendRequest(
                new rpc.RequestType<GetObject, RpcObjectData[], Error>("GetObject"),
                new GetObject(id)
            );
        });

        let remoteObject = q.receive(this.localObjects.get(id), (before: any) => {
            return RpcCodecs.getCodecForInstance(before)?.rpcReceive(before, q) ?? before;
        });

        if ((await q.take()).state !== RpcObjectState.END_OF_OBJECT) {
            throw new Error("Expected END_OF_OBJECT");
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
                cursor = new Cursor(cursor, cursorObject);
            }
        }
        return cursor;
    }

    visit(tree: Tree, visitorName: string, p: any, cursor: Cursor | undefined): Promise<Tree> {
        return this.scan(tree, visitorName, p, cursor).then(response => {
            if (response.modified) {
                return this.getObject(tree.id.toString());
            }
            return tree;
        });
    }

    scan(tree: Tree, visitorName: string, p: any, cursor: Cursor | undefined): Promise<VisitResponse> {
        this.localObjects.set(tree.id.toString(), tree);
        const pId = this.localObject(p);
        const cursorIds = this.getCursorIds(cursor);
        return this.connection.sendRequest(
            new rpc.RequestType<Visit, VisitResponse, Error>("Visit"),
            new Visit(visitorName, undefined, tree.id.toString(), pId, cursorIds)
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

    private getCursorIds(cursor: Cursor | undefined): string[] | undefined {
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
