import {RpcCodec, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "./rpc";
import {DATA_TABLES_KEY} from "./data-table";

export class ExecutionContext {
    readonly kind: string = "org.openrewrite.ExecutionContext"

    constructor(public readonly messages: { [key: string | symbol]: any } = {}) {
    }
}

const executionContextCodec: RpcCodec<ExecutionContext> = {
    async rpcSend(after: ExecutionContext, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, ctx => {
            const withoutDataTables = {
                ...ctx.messages
            }
            delete withoutDataTables[DATA_TABLES_KEY];
        });

        // Map<DataTable<?>, List<?>> dt = after.getMessage(DATA_TABLES);
        // q.getAndSendList(after, sendWholeList(dt == null ? null : dt.keySet()), DataTable::getName, null);
        // if (dt != null) {
        //     for (List<?> rowSet : dt.values()) {
        //         q.getAndSendList(after, sendWholeList(rowSet),
        //                 row -> Integer.toString(System.identityHashCode(row)),
        //                 null);
        //     }
        // }

        const dt = after.messages[DATA_TABLES_KEY];
        await q.getAndSendList(after, sendWholeList(Object.keys(dt)), name => name);
        if (dt) {
            for (const rowSet of Object.values(dt)) {
                await q.getAndSendList(after, sendWholeList(rowSet),
                    row => {
                    });
            }
        }
    },

    async rpcReceive(before: ExecutionContext, q: RpcReceiveQueue): Promise<ExecutionContext> {
        return before;
    }
}

function rowId(row: any): any {
    if(row[Symbol("org.openrewrite.dataTable.rowId")]) {

    }
}

function sendWholeList<T>(list: T[] | undefined): (ctx: ExecutionContext) => T[] | undefined {
    let retrievedAfter = false;
    return () => {
        if (!retrievedAfter) {
            retrievedAfter = true;
            return !list ? undefined : [...list];
        }
        return undefined;
    };
}

RpcCodecs.registerCodec("org.openrewrite.ExecutionContext", executionContextCodec);
