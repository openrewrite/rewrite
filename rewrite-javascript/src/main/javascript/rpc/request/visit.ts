import * as rpc from "vscode-jsonrpc/node";
import {Recipe} from "../../recipe";
import {Cursor, Tree} from "../../tree";
import {TreeVisitor} from "../../visitor";

export interface VisitResponse {
    modified: boolean
}

export class Visit {
    private readonly visitor: string
    private readonly visitorOptions: Map<string, any> | undefined
    private readonly treeId: string
    private readonly p: string
    private readonly cursor: string[] | undefined

    constructor(visitor: string, visitorOptions: Map<string, any> | undefined, treeId: string, p: string, cursor: string[] | undefined) {
        this.visitor = visitor
        this.visitorOptions = visitorOptions
        this.treeId = treeId
        this.p = p
        this.cursor = cursor
    }

    static handle(connection: rpc.MessageConnection,
                  localObjects: Map<string, any>,
                  preparedRecipes: Map<String, Recipe>,
                  recipeCursors: WeakMap<Recipe, Cursor>,
                  getObject: (id: string) => any,
                  getCursor: (cursorIds: string[] | undefined) => Promise<Cursor>): void {
        connection.onRequest(new rpc.RequestType<Visit, VisitResponse, Error>("Visit"), async (request) => {
            const before: Tree = getObject(request.treeId);
            localObjects.set(before.id.toString(), before);

            const visitor: TreeVisitor<any, any> = Reflect.construct(
                // "as any" bypasses strict type checking
                (globalThis as any)[request.visitor],
                request.visitorOptions ? Array.from(request.visitorOptions.values()) : [])

            const after = await visitor.visit(before, getObject(request.p), await getCursor(request.cursor));
            if (!after) {
                localObjects.delete(before.id.toString());
            } else {
                localObjects.set(after.id.toString(), after);
            }

            return {modified: before !== after}
        });
    }
}
