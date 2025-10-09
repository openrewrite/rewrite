import * as rpc from "vscode-jsonrpc/node";
import {withMetrics0} from "./metrics";

export class GetLanguages {
    static handle(connection: rpc.MessageConnection, metricsCsv?: string): void {
        const target = {target: ''};
        connection.onRequest(new rpc.RequestType0<string[], Error>("GetLanguages"), withMetrics0<string[]>("GetLanguages", target, metricsCsv)(async () => {
            // Include all languages you want this server to support receiving from a remote peer
            return [
                "org.openrewrite.text.PlainText",
                "org.openrewrite.json.tree.Json$Document",
                "org.openrewrite.java.tree.J$CompilationUnit",
                "org.openrewrite.javascript.tree.JS$CompilationUnit",
            ];
        }));
    }
}
