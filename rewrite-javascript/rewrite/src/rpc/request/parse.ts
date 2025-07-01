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
import {ExecutionContext} from "../../execution";
import {UUID} from "node:crypto";
import {Parser, ParserInput, Parsers} from "../../parser";
import {SourceFile} from "../../tree";
import {LRUCache} from "lru-cache";

export class Parse {
    constructor(private readonly id: string,
                private readonly inputs: ParserInput[],
                private readonly relativeTo?: string) {
    }

    static handle(connection: rpc.MessageConnection,
                  localObjects: LRUCache<string, any>): void {
        const ongoingRequests = new Map<string, AsyncGenerator<SourceFile>>();

        connection.onRequest(new rpc.RequestType<Parse, UUID[], Error>("Parse"), async (request) => {
            let parser: Parser | undefined = Parsers.createParser("javascript", {
                ctx: new ExecutionContext(),
                relativeTo: request.relativeTo
            });

            if (parser) {
                const requestId = request.id;

                // Check if we already have an ongoing request for this ID
                let generator = ongoingRequests.get(requestId);
                if (!generator) {
                    generator = parser.parse(...request.inputs);
                    ongoingRequests.set(requestId, generator);
                }

                const batch: string[] = [];
                const batchSize = 10;

                for (let i = 0; i < batchSize; i++) {
                    const result = await generator.next();

                    if (result.done) {
                        // Generator is exhausted, clean up
                        ongoingRequests.delete(requestId);
                        break;
                    } else {
                        // Store the generated object and add its ID to batch
                        const sourceFile = result.value;
                        localObjects.set(sourceFile.id.toString(), sourceFile);
                        batch.push(sourceFile.id);
                    }
                }

                return batch;
            }
            return [];
        });
    }
}
