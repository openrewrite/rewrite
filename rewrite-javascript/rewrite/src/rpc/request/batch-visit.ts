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
import {Recipe} from "../../recipe";
import {Cursor, Tree} from "../../tree";
import {TreeVisitor} from "../../visitor";
import {MarkersKind, SearchResult} from "../../markers";
import {Visit} from "./visit";
import {withMetrics} from "./metrics";

export interface BatchVisitItem {
    visitor: string
    visitorOptions?: Map<string, any>
}

export interface BatchVisitResult {
    modified: boolean
    deleted: boolean
    hasNewMessages: boolean
    searchResultIds: string[]
}

export interface BatchVisitResponse {
    results: BatchVisitResult[]
}

export interface BatchVisitRequest {
    sourceFileType: string
    treeId: string
    p: string
    cursor?: string[]
    visitors: BatchVisitItem[]
}

function collectSearchResultIds(tree: Tree | null | undefined): Set<string> {
    const ids = new Set<string>();
    if (!tree) return ids;

    new class extends TreeVisitor<any, Set<string>> {
        protected async visitMarker(marker: any, ctx: Set<string>): Promise<any> {
            if (marker && marker.kind === MarkersKind.SearchResult) {
                ctx.add((marker as SearchResult).id.toString());
            }
            return super.visitMarker(marker, ctx);
        }
    }().reduce(tree, ids);
    return ids;
}

export class BatchVisit {
    static handle(connection: rpc.MessageConnection,
                  localObjects: Map<string, any>,
                  preparedRecipes: Map<String, Recipe>,
                  recipeCursors: WeakMap<Recipe, Cursor>,
                  getObject: (id: string, sourceFileType?: string) => any,
                  getCursor: (cursorIds: string[] | undefined, sourceFileType?: string) => Promise<Cursor>,
                  metricsCsv?: string): void {
        connection.onRequest(
            new rpc.RequestType<BatchVisitRequest, BatchVisitResponse, Error>("BatchVisit"),
            withMetrics<BatchVisitRequest, BatchVisitResponse>(
                "BatchVisit",
                metricsCsv,
                (_context) => async (request) => {
                    const p = await getObject(request.p, undefined);
                    let tree: Tree = await getObject(request.treeId, request.sourceFileType);
                    const cursor = await getCursor(request.cursor, request.sourceFileType);

                    const results: BatchVisitResult[] = [];
                    const knownIds = collectSearchResultIds(tree);

                    for (const item of request.visitors) {
                        // Instantiate and run visitor
                        const visitor = await Visit.instantiateVisitor(
                            {visitor: item.visitor, visitorOptions: item.visitorOptions},
                            preparedRecipes, recipeCursors, p);
                        const after = await visitor.visit(tree, p, cursor);

                        const modified = after !== tree;
                        const deleted = after == null;

                        // Diff SearchResult IDs against the running set
                        let searchResultIds: string[];
                        if (deleted) {
                            searchResultIds = [];
                        } else {
                            const afterIds = collectSearchResultIds(after);
                            searchResultIds = [...afterIds].filter(id => !knownIds.has(id));
                            for (const id of searchResultIds) knownIds.add(id);
                        }

                        results.push({modified, deleted, hasNewMessages: false, searchResultIds});

                        if (deleted) {
                            localObjects.delete(request.treeId);
                            break;
                        }

                        if (modified) {
                            tree = after;
                        }
                    }

                    // Store final tree in localObjects
                    if (tree != null) {
                        localObjects.set(tree.id.toString(), tree);
                        if (tree.id.toString() !== request.treeId) {
                            localObjects.set(request.treeId, tree);
                        }
                    }

                    return {results};
                }
            )
        );
    }
}
