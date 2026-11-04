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
import {CsvDataTableStore, DataTableStore, InMemoryDataTableStore} from "../../data-table";
import {withMetrics} from "./metrics";

/**
 * Conveys a data table store's configuration from the host; rows never cross the wire.
 * Mirrors Java's {@code SetDataTableStore}.
 */
export type SetDataTableStore = SetDataTableStore.NoOp | SetDataTableStore.Csv;

export namespace SetDataTableStore {
    export interface NoOp {
        readonly kind: "NOOP";
    }

    export interface Csv {
        readonly kind: "CSV";
        readonly outputDir: string;
        readonly prefixColumns?: { [key: string]: string };
        readonly suffixColumns?: { [key: string]: string };
    }

    export function toDataTableStore(request: SetDataTableStore): DataTableStore {
        if (request.kind === "CSV" && request.outputDir) {
            return new CsvDataTableStore(
                request.outputDir,
                request.prefixColumns ?? {},
                request.suffixColumns ?? {});
        }
        return new InMemoryDataTableStore();
    }

    export function handle(connection: rpc.MessageConnection,
                           install: (store: DataTableStore) => void,
                           metricsCsv?: string): void {
        connection.onRequest(
            new rpc.RequestType<SetDataTableStore, boolean, Error>("SetDataTableStore"),
            withMetrics<SetDataTableStore, boolean>(
                "SetDataTableStore",
                metricsCsv,
                (_context) => async (request) => {
                    install(toDataTableStore(request));
                    return true;
                }
            )
        );
    }
}
