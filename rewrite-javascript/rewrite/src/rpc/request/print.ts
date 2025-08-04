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
import {Cursor, isSourceFile, Tree} from "../../tree";
import {MarkerPrinter as PrintMarkerPrinter, printer, PrintOutputCapture} from "../../print";
import {UUID} from "../../uuid";

export const enum MarkerPrinter {
    DEFAULT = "DEFAULT",
    FENCED = "FENCED",
    SANITIZED = "SANITIZED"
}

export class Print {
    constructor(private readonly treeId: UUID, private readonly cursor?: string[], readonly markerPrinter: MarkerPrinter = MarkerPrinter.DEFAULT) {
    }

    static handle(connection: rpc.MessageConnection,
                  getObject: (id: string) => any,
                  getCursor: (cursorIds: string[] | undefined) => Promise<Cursor>): void {
        connection.onRequest(new rpc.RequestType<Print, string, Error>("Print"), async request => {
            const tree: Tree = await getObject(request.treeId.toString());
            const out = new PrintOutputCapture(PrintMarkerPrinter[request.markerPrinter]);
            if (isSourceFile(tree)) {
                return await printer(tree).print(tree, out);
            } else {
                const cursor = await getCursor(request.cursor);
                return await printer(cursor).print(tree, out);
            }
        });
    }
}
