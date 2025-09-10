import * as rpc from "vscode-jsonrpc/node";

export class GetLanguages {
    static handle(connection: rpc.MessageConnection): void {
        connection.onRequest(new rpc.RequestType0<string[], Error>("GetLanguages"), async () => {
            // Include all languages you want this server to support receiving from a remote peer
            const languages: string[] = [
                "org.openrewrite.text.PlainText",
                "org.openrewrite.json.tree.Json$Document",
                "org.openrewrite.java.tree.J$CompilationUnit",
                "org.openrewrite.javascript.tree.JS$CompilationUnit",
            ];
            return languages;
        });
    }
}
