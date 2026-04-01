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
import {isSourceFile, Tree} from "../../tree";
import {MarkerPrinter as PrintMarkerPrinter, printer, PrintOutputCapture} from "../../print";
import {UUID} from "../../uuid";
import {extractSourcePath, withMetrics} from "./metrics";

export const enum MarkerPrinter {
    DEFAULT = "DEFAULT",
    FENCED = "FENCED",
    SANITIZED = "SANITIZED"
}

export class Print {
    constructor(private readonly treeId: UUID,
                private readonly sourceFileType: string,
                readonly markerPrinter: MarkerPrinter = MarkerPrinter.DEFAULT) {
    }

    static handle(connection: rpc.MessageConnection,
                  getObject: (id: string, sourceFileType: string) => any,
                  logger?: rpc.Logger,
                  metricsCsv?: string): void {
        connection.onRequest(
            new rpc.RequestType<Print, string, Error>("Print"),
            withMetrics<Print, string>(
                "Print",
                metricsCsv,
                (context) => async request => {
                    const tree: Tree = await getObject(request.treeId.toString(), request.sourceFileType);
                    context.target = extractSourcePath(tree);
                    if (logger) {
                        logger.log("Printing " + (isSourceFile(tree) ? tree.sourcePath : `tree of type ${tree.kind} in ${context.target}`));
                    }
                    const out = new PrintOutputCapture(PrintMarkerPrinter[request.markerPrinter]);
                    let result: string;
                    if (isSourceFile(tree)) {
                        result = await printer(tree).print(tree, out);
                    } else {
                        result = await printer(request.sourceFileType).print(tree, out);
                    }
                    return result;
                }
            )
        );
    }
}
