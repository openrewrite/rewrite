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
import {ParserInput, Parsers} from "../../parser";
import {randomId} from "../../uuid";
import {produce} from "immer";
import {SourceFile} from "../../tree";

export class Parse {
    constructor(private readonly inputs: ParserInput[],
                private readonly relativeTo?: string) {
    }

    static handle(connection: rpc.MessageConnection,
                  localObjects: Map<string, ((input: string) => any) | any>): void {
        connection.onRequest(new rpc.RequestType<Parse, UUID[], Error>("Parse"), async (request) => {
            let parser = Parsers.createParser("javascript", {
                ctx: new ExecutionContext(),
                relativeTo: request.relativeTo
            });

            if (!parser) {
                return [];
            }
            const generator = parser.parse(...request.inputs);
            const result: string[] = [];

            for (let i = 0; i < request.inputs.length; i++) {
                const id = randomId();
                localObjects.set(id, async (id: string) => {
                    let sourceFile: SourceFile = (await generator.next()).value;
                    return produce(sourceFile, (draft) => {draft.id = id;});
                });
                result.push(id);
            }

            return result;
        });
    }
}
