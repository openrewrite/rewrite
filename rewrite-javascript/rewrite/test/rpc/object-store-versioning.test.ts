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
import {ObjectStore} from "../../src/rpc/object-store";
import {SnowflakeId} from "@akashrajpurohit/snowflake-id";
import {randomId} from "../../src";
import {PlainText} from "../../src/text";

// Mock object without intrinsic ID
class MockData {
    constructor(public readonly value: string) {}
}

describe("ObjectStore Versioning", () => {

    test("ObjectStore creates composite IDs for objects with intrinsic IDs", () => {
        const snowflake = SnowflakeId();
        const store = new ObjectStore(snowflake);

        const tree: PlainText = {
            kind: PlainText.Kind.PlainText,
            id: randomId(),
            sourcePath: "test.txt",
            charsetName: "UTF-8",
            charsetBomMarked: false,
            checksum: undefined,
            fileAttributes: undefined,
            markers: {kind: "org.openrewrite.marker.Markers", id: randomId(), markers: []},
            text: "original content",
            snippets: []
        };
        const compositeId = store.store(tree);

        // Should create composite ID: <uuid>@<snowflake-id>
        expect(compositeId).toMatch(/^[\w-]+@\d+$/);
        expect(store.get(compositeId)).toBe(tree);

        // Version should be tracked
        const version = store.getCurrentVersion(tree.id);
        expect(version).toBeTruthy();
        expect(compositeId).toBe(`${tree.id}@${version}`);
    });

    test("ObjectStore creates simple IDs for objects without intrinsic IDs", () => {
        const snowflake = SnowflakeId();
        const store = new ObjectStore(snowflake);

        const data = new MockData("test data");
        const id = store.store(data);

        // Should create simple snowflake ID (no @ symbol)
        expect(id).not.toContain("@");
        expect(id).toMatch(/^\d+$/);
        expect(store.get(id)).toBe(data);
    });

    test("ObjectStore updates version when storing modified object with same intrinsic ID", () => {
        const snowflake = SnowflakeId();
        const store = new ObjectStore(snowflake);

        const treeId = randomId();
        const originalTree: PlainText = {
            kind: PlainText.Kind.PlainText,
            id: treeId,
            sourcePath: "test.txt",
            charsetName: "UTF-8",
            charsetBomMarked: false,
            checksum: undefined,
            fileAttributes: undefined,
            markers: {kind: "org.openrewrite.marker.Markers", id: randomId(), markers: []},
            text: "original",
            snippets: []
        };
        const originalCompositeId = store.store(originalTree);
        const originalVersion = store.getCurrentVersion(treeId);

        // Simulate modification - new object instance but same intrinsic ID
        const modifiedTree: PlainText = {
            ...originalTree,
            text: "modified"
        };
        const modifiedCompositeId = store.store(modifiedTree);
        const newVersion = store.getCurrentVersion(treeId);

        // Should have different versions
        expect(originalVersion).toBeTruthy();
        expect(newVersion).toBeTruthy();
        expect(newVersion).not.toBe(originalVersion);

        // Composite IDs should be different
        expect(originalCompositeId).not.toBe(modifiedCompositeId);
        expect(modifiedCompositeId).toBe(`${treeId}@${newVersion}`);

        // Should store the new version
        expect(store.get(modifiedCompositeId)).toBe(modifiedTree);
    });

    test("ObjectStore tracks versions across multiple stores", () => {
        const snowflake = SnowflakeId();
        const localStore = new ObjectStore(snowflake);
        const remoteStore = new ObjectStore(snowflake);

        const treeId = randomId();
        const tree1: PlainText = {
            kind: PlainText.Kind.PlainText,
            id: treeId,
            sourcePath: "test.txt",
            charsetName: "UTF-8",
            charsetBomMarked: false,
            checksum: undefined,
            fileAttributes: undefined,
            markers: {kind: "org.openrewrite.marker.Markers", id: randomId(), markers: []},
            text: "version 1",
            snippets: []
        };

        // Store in local
        const localId1 = localStore.store(tree1);
        const localVersion1 = localStore.getCurrentVersion(treeId);
        
        // Store same object in remote (simulating sync)
        remoteStore.store(tree1, localId1);
        expect(remoteStore.getCurrentVersion(treeId)).toBe(localVersion1);

        // Create modified version
        const tree2: PlainText = {
            ...tree1,
            text: "version 2"
        };
        
        // Store modified version in local
        const localId2 = localStore.store(tree2);
        const localVersion2 = localStore.getCurrentVersion(treeId);
        
        // Versions should be different
        expect(localVersion2).not.toBe(localVersion1);
        expect(localId2).toBe(`${treeId}@${localVersion2}`);
        
        // Remote still has old version
        expect(remoteStore.getCurrentVersion(treeId)).toBe(localVersion1);
    });
});