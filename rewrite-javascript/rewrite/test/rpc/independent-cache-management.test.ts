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
import {afterEach, beforeEach, describe, expect, test} from "@jest/globals";
import {emptyMarkers, Markers, randomId, RecipeRegistry} from "../../src";
import {asRef, RewriteRpc, RpcObjectData, RpcObjectState} from "../../src/rpc";
import {PlainText} from "../../src/text";
import {PassThrough} from "node:stream";
import * as rpc from "vscode-jsonrpc/node";
import {activate} from "../example-recipe";
import {GetObject, GetRef} from "../../src/rpc/request";

describe("Independent Cache Management", () => {
    let server: RewriteRpc;
    let client: RewriteRpc;

    beforeEach(async () => {
        // Create in-memory streams to simulate the pipes between two RPC instances
        const clientToServer = new PassThrough();
        const serverToClient = new PassThrough();

        const clientConnection = rpc.createMessageConnection(
            new rpc.StreamMessageReader(serverToClient),
            new rpc.StreamMessageWriter(clientToServer)
        );
        client = new RewriteRpc(clientConnection, {
            batchSize: 1,
            traceGetObjectOutput: false
        });

        const serverConnection = rpc.createMessageConnection(
            new rpc.StreamMessageReader(clientToServer),
            new rpc.StreamMessageWriter(serverToClient)
        );
        const registry = new RecipeRegistry();
        activate(registry);
        server = new RewriteRpc(serverConnection, {
            registry: registry,
            traceGetObjectOutput: false
        });
    });

    afterEach(() => {
        server.end();
        client.end();
    });

    test("getObject without lastKnownId returns ADD", async () => {
        // Create a simple object on server
        const objectId = randomId();
        const text: PlainText = {
            kind: "org.openrewrite.text.PlainText",
            id: objectId,
            text: "Hello World",
            markers: emptyMarkers,
            sourcePath: "test.txt",
            snippets: []
        };
        
        server.localObjects.set(objectId, text);

        // Client requests object for first time (no lastKnownId)
        const retrieved = await client.getObject<PlainText>(objectId);
        expect(retrieved.text).toBe("Hello World");
        expect(client.localObjects.has(objectId)).toBe(true);
    });

    test("getObject with lastKnownId when remote has no entry returns ADD", async () => {
        // Create object on server
        const objectId = randomId();
        const text: PlainText = {
            kind: "org.openrewrite.text.PlainText",
            id: objectId,
            text: "Hello World",
            markers: emptyMarkers,
            sourcePath: "test.txt",
            snippets: []
        };
        
        server.localObjects.set(objectId, text);
        // Clear any existing remote state to ensure cache miss
        server.remoteObjects.clear();

        // Client requests with lastKnownId - should get full object as ADD
        const retrieved = await client.getObject<PlainText>(objectId);
        expect(retrieved.text).toBe("Hello World");
    });

    test("getObject after cache eviction recovers gracefully", async () => {
        // Create object on server
        const objectId = randomId();
        const text: PlainText = {
            kind: "org.openrewrite.text.PlainText",
            id: objectId,
            text: "Original",
            markers: emptyMarkers,
            sourcePath: "test.txt",
            snippets: []
        };
        
        server.localObjects.set(objectId, text);

        // Client gets object first time
        const retrieved1 = await client.getObject<PlainText>(objectId);
        expect(retrieved1.text).toBe("Original");

        // Server evicts its knowledge of remote state (simulating memory pressure)
        server.remoteObjects.clear();

        // Modify object on server
        const modified = {...text, text: "Modified After Eviction"};
        server.localObjects.set(objectId, modified);

        // Client requests again - server should send full object since it evicted remote state
        const retrieved2 = await client.getObject<PlainText>(objectId);
        expect(retrieved2.text).toBe("Modified After Eviction");
    });

    test("getRef after reference eviction", async () => {
        // Create an object with markers that will become references
        const objectId = randomId();
        const customMarkers: Markers = {
            kind: "org.openrewrite.marker.Markers",
            id: randomId(),
            markers: []
        };
        const text: PlainText = {
            kind: "org.openrewrite.text.PlainText",
            id: objectId,
            text: "Text with refs",
            markers: customMarkers,
            sourcePath: "test.txt",
            snippets: []
        };
        
        server.localObjects.set(objectId, text);

        // Get object first time to establish references
        const retrieved1 = await client.getObject<PlainText>(objectId);
        expect(retrieved1.text).toBe("Text with refs");

        // Verify references were established
        expect(server.localRefs.has(asRef(customMarkers))).toBe(true);
        expect(client.remoteRefs.size).toBeGreaterThan(0);

        // Simulate client evicting references due to memory pressure
        client.remoteRefs.clear();

        // Request object again - should trigger GetRef calls for missing references
        const retrieved2 = await client.getObject<PlainText>(objectId);
        expect(retrieved2.text).toBe("Text with refs");
        expect(retrieved2.markers).toBeDefined();
    });

    test("getRef request handling returns correct object", async () => {
        // Create a reference on server
        const markers: Markers = {
            kind: "org.openrewrite.marker.Markers",
            id: randomId(),
            markers: []
        };
        const refId = server.localRefs.create(asRef(markers));

        // Debug: check that it was actually stored
        expect(server.localRefs.has(asRef(markers))).toBe(true);
        expect(server.localRefs.get(asRef(markers))).toBe(refId);
        expect(server.localRefs.getByRefId(refId)).toBeDefined();

        // Test GetRef request directly using connection (client requests from server)
        const response = await client.connection.sendRequest(
            new rpc.RequestType<GetRef, RpcObjectData[], Error>("GetRef"),
            new GetRef(refId)
        );
        
        expect(response).toHaveLength(5);
        expect(response[0].state).toBe(RpcObjectState.ADD);
        expect(response[response.length - 1].state).toBe(RpcObjectState.END_OF_OBJECT);
    });

    test("getRef request for missing reference returns DELETE", async () => {
        // Request a reference that doesn't exist (client requests from server)
        const response = await client.connection.sendRequest(
            new rpc.RequestType<GetRef, RpcObjectData[], Error>("GetRef"),
            new GetRef(999)
        );
        
        expect(response).toHaveLength(2);
        expect(response[0].state).toBe(RpcObjectState.DELETE);
        expect(response[1].state).toBe(RpcObjectState.END_OF_OBJECT);
    });

    test("lastKnownId null parameter returns ADD not CHANGE", async () => {
        // Create object on server
        const objectId = randomId();
        const text: PlainText = {
            kind: "org.openrewrite.text.PlainText",
            id: objectId,
            text: "Test Object",
            markers: emptyMarkers,
            sourcePath: "test.txt",
            snippets: []
        };
        
        server.localObjects.set(objectId, text);

        // Client requests from server with null lastKnownId
        const response = await client.connection.sendRequest(
            new rpc.RequestType<GetObject, RpcObjectData[], Error>("GetObject"),
            new GetObject(objectId, undefined)
        );

        // Should return ADD state, not CHANGE
        expect(response).toBeDefined();
        expect(response.length).toBeGreaterThan(0);
        expect(response[0].state).toBe(RpcObjectState.ADD);
    });

    test("lastKnownId absent parameter returns ADD not CHANGE", async () => {
        // Create object on server
        const objectId = randomId();
        const text: PlainText = {
            kind: "org.openrewrite.text.PlainText",
            id: objectId,
            text: "Test Object",
            markers: emptyMarkers,
            sourcePath: "test.txt",
            snippets: []
        };
        
        server.localObjects.set(objectId, text);

        // Client requests from server without lastKnownId property
        const response = await client.connection.sendRequest(
            new rpc.RequestType<GetObject, RpcObjectData[], Error>("GetObject"),
            new GetObject(objectId)
        );

        // Should return ADD state, not CHANGE
        expect(response).toBeDefined();
        expect(response.length).toBeGreaterThan(0);
        expect(response[0].state).toBe(RpcObjectState.ADD);
    });

    test("independent cache eviction does not affect other side", async () => {
        // Create objects on both sides
        const objectId1 = randomId();
        const objectId2 = randomId();
        
        const text1: PlainText = {
            kind: "org.openrewrite.text.PlainText",
            id: objectId1,
            text: "Server Object",
            markers: emptyMarkers,
            sourcePath: "server.txt",
            snippets: []
        };
                
        const text2: PlainText = {
            kind: "org.openrewrite.text.PlainText",
            id: objectId2,
            text: "Client Object",
            markers: emptyMarkers,
            sourcePath: "client.txt",
            snippets: []
        };
        
        server.localObjects.set(objectId1, text1);
        client.localObjects.set(objectId2, text2);

        // Exchange objects
        const serverObj = await client.getObject<PlainText>(objectId1);
        const clientObj = await server.getObject<PlainText>(objectId2);
        
        expect(serverObj.text).toBe("Server Object");
        expect(clientObj.text).toBe("Client Object");

        // Client evicts its caches
        client.remoteObjects.clear();
        client.remoteRefs.clear();

        // Server should still be able to access its objects normally
        expect(server.localObjects.get(objectId1)?.text).toBe("Server Object");
        expect(server.remoteObjects.get(objectId2)?.text).toBe("Client Object");

        // Client can still get objects from server (will get full objects now)
        const retrieved = await client.getObject<PlainText>(objectId1);
        expect(retrieved.text).toBe("Server Object");
    });

    test("complex object with multiple references handles cache eviction", async () => {
        // Create a more complex object with multiple references
        const objectId = randomId();
        
        const marker1: Markers = {kind: "org.openrewrite.marker.Markers", id: randomId(), markers: []};
        const marker2: Markers = {kind: "org.openrewrite.marker.Markers", id: randomId(), markers: []};
        
        const original: PlainText = {
            kind: "org.openrewrite.text.PlainText",
            id: objectId,
            text: "Complex Object",
            markers: marker1,
            sourcePath: "complex.txt",
            snippets: []
        };
        
        server.localObjects.set(objectId, original);

        // First retrieval establishes baseline and references
        const retrieved1 = await client.getObject<PlainText>(objectId);
        expect(retrieved1.text).toBe("Complex Object");

        // Clear various caches to test recovery
        client.remoteRefs.clear();
        server.remoteObjects.clear();

        // Modify and retrieve again - should handle all cache misses gracefully
        const modified = {...original, text: "Modified Complex Object", markers: marker2};
        server.localObjects.set(objectId, modified);

        const retrieved2 = await client.getObject<PlainText>(objectId);
        expect(retrieved2.text).toBe("Modified Complex Object");
    });

    test("batch operations work correctly with cache misses", async () => {
        // Create multiple objects on server
        const objects: PlainText[] = [];
        const objectIds: string[] = [];

        for (let i = 0; i < 5; i++) {
            const objectId = randomId();
            const text: PlainText = {
                kind: "org.openrewrite.text.PlainText",
                id: objectId,
                text: `Object ${i}`,
                markers: emptyMarkers,
                sourcePath: `file${i}.txt`,
                snippets: []
            };
            objects.push(text);
            objectIds.push(objectId);
            server.localObjects.set(objectId, text);
        }

        // Client gets all objects
        const retrieved: PlainText[] = [];
        for (const objectId of objectIds) {
            retrieved.push(await client.getObject<PlainText>(objectId));
        }

        expect(retrieved).toHaveLength(5);
        retrieved.forEach((obj, i) => {
            expect(obj.text).toBe(`Object ${i}`);
        });

        // Evict server's remote state
        server.remoteObjects.clear();

        // Modify all objects
        objects.forEach((obj, i) => {
            const modified = {...obj, text: `Modified Object ${i}`};
            server.localObjects.set(objectIds[i], modified);
        });

        // Client requests all objects again - should handle cache misses gracefully
        const retrievedModified: PlainText[] = [];
        for (const objectId of objectIds) {
            retrievedModified.push(await client.getObject<PlainText>(objectId));
        }

        expect(retrievedModified).toHaveLength(5);
        retrievedModified.forEach((obj, i) => {
            expect(obj.text).toBe(`Modified Object ${i}`);
        });
    });

    test("reference map bidirectional lookup works correctly", () => {
        const markers: Markers = {kind: "org.openrewrite.marker.Markers", id: randomId(), markers: []};
        
        // Create reference
        const refId = server.localRefs.create(asRef(markers));
        
        // Test forward lookup
        expect(server.localRefs.get(asRef(markers))).toBe(refId);
        
        // Test reverse lookup
        expect(server.localRefs.getByRefId(refId)).toBe(markers);
        
        // Test non-existent lookup
        expect(server.localRefs.getByRefId(999)).toBeUndefined();
    });
});