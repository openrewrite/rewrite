/*
 * Copyright 2026 the original author or authors.
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
import {Markers, MarkersKind} from "../../src";
import {
    PythonDependency,
    PythonResolutionResult,
    PythonResolvedDependency,
    ReferenceMap,
    RpcObjectData,
    RpcObjectState,
    RpcReceiveQueue,
    RpcSendQueue
} from "../../src/rpc";

const {ADD, CHANGE, NO_CHANGE, END_OF_OBJECT} = RpcObjectState;

// A Java host sends this marker on requirements.txt, setup.cfg, Pipfile and pyproject.toml files,
// so a JavaScript recipe run over a repository holding Python project files receives it.
describe("PythonResolutionResult over RPC", () => {

    const PRR = "org.openrewrite.python.marker.PythonResolutionResult";

    function marker(): PythonResolutionResult {
        const urllib3: PythonResolvedDependency = {
            kind: `${PRR}$ResolvedDependency`,
            name: "urllib3",
            version: "2.2.1",
            source: "registry+https://pypi.org/simple",
            dependencies: []
        };
        const requests: PythonResolvedDependency = {
            kind: `${PRR}$ResolvedDependency`,
            name: "requests",
            version: "2.31.0",
            source: "registry+https://pypi.org/simple",
            dependencies: [urllib3]
        };
        const requestsDep: PythonDependency = {
            kind: `${PRR}$Dependency`,
            name: "requests",
            versionConstraint: ">=2.28",
            extras: ["security"],
            marker: "python_version >= '3.8'",
            resolved: requests
        };
        return {
            kind: PRR,
            id: "7b6f6f7e-6e0b-4a53-9a6e-2f0f4d3b8c11",
            name: "my-app",
            version: "1.0.0",
            description: "A Python project",
            license: "MIT",
            path: "pyproject.toml",
            requiresPython: ">=3.10",
            buildBackend: "hatchling.build",
            buildRequires: [{kind: `${PRR}$Dependency`, name: "hatchling"}],
            dependencies: [requestsDep],
            optionalDependencies: {socks: [{name: "PySocks", versionConstraint: ">=1.5.6"}]},
            dependencyGroups: {dev: [{name: "pytest"}]},
            constraintDependencies: [{kind: `${PRR}$Dependency`, name: "urllib3", versionConstraint: "<3"}],
            overrideDependencies: [],
            resolvedDependencies: [requests, urllib3],
            packageManager: "Uv",
            sourceIndexes: [{
                kind: `${PRR}$SourceIndex`,
                name: "pypi",
                url: "https://pypi.org/simple",
                defaultIndex: true
            }]
        };
    }

    async function roundTrip<T>(value: T): Promise<{ received: T, next: RpcObjectState }> {
        const sq = new RpcSendQueue(new ReferenceMap(), undefined, false);
        const batch = await sq.generate(value, undefined);
        const rq = new RpcReceiveQueue(new Map(), undefined, async () => batch, undefined, false);
        const received = await rq.receive<T>(undefined);
        // Every message of the object must have been consumed, or later objects desynchronize
        const next = await rq.take();
        return {received, next: next.state};
    }

    // What Java's RpcSendQueue emits for the same marker as marker() above: every nested
    // Dependency, ResolvedDependency and SourceIndex travels property by property after an
    // ADD naming its type, so the receiver needs a codec for each type or it stops reading
    // part way through and every later message is read against the wrong field.
    const javaWire: RpcObjectData[] = [
        {state: ADD, valueType: PRR},
        {state: ADD, value: "7b6f6f7e-6e0b-4a53-9a6e-2f0f4d3b8c11"},
        {state: ADD, value: "my-app"},
        {state: ADD, value: "1.0.0"},
        {state: ADD, value: "A Python project"},
        {state: ADD, value: "MIT"},
        {state: ADD, value: "pyproject.toml"},
        {state: ADD, value: ">=3.10"},
        {state: ADD, value: "hatchling.build"},
        // buildRequires
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD, valueType: `${PRR}$Dependency`, ref: 1},
        {state: ADD, value: "hatchling"},
        {state: NO_CHANGE},
        {state: NO_CHANGE},
        {state: NO_CHANGE},
        {state: NO_CHANGE},
        // dependencies
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD, valueType: `${PRR}$Dependency`, ref: 2},
        {state: ADD, value: "requests"},
        {state: ADD, value: ">=2.28"},
        {state: ADD, value: ["security"]},
        {state: ADD, value: "python_version >= '3.8'"},
        {state: ADD, valueType: `${PRR}$ResolvedDependency`, ref: 3},
        {state: ADD, value: "requests"},
        {state: ADD, value: "2.31.0"},
        {state: ADD, value: "registry+https://pypi.org/simple"},
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD, valueType: `${PRR}$ResolvedDependency`, ref: 4},
        {state: ADD, value: "urllib3"},
        {state: ADD, value: "2.2.1"},
        {state: ADD, value: "registry+https://pypi.org/simple"},
        {state: ADD},
        {state: CHANGE, value: []},
        // optionalDependencies and dependencyGroups travel as plain values
        {state: ADD, value: {socks: [{name: "PySocks", versionConstraint: ">=1.5.6"}]}},
        {state: ADD, value: {dev: [{name: "pytest"}]}},
        // constraintDependencies
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD, valueType: `${PRR}$Dependency`, ref: 5},
        {state: ADD, value: "urllib3"},
        {state: ADD, value: "<3"},
        {state: NO_CHANGE},
        {state: NO_CHANGE},
        {state: NO_CHANGE},
        // overrideDependencies
        {state: ADD},
        {state: CHANGE, value: []},
        // resolvedDependencies, both already sent through dependencies[0].resolved
        {state: ADD},
        {state: CHANGE, value: [-1, -1]},
        {state: ADD, ref: 3},
        {state: ADD, ref: 4},
        // packageManager
        {state: ADD, value: "Uv"},
        // sourceIndexes
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD, valueType: `${PRR}$SourceIndex`},
        {state: ADD, value: "pypi"},
        {state: ADD, value: "https://pypi.org/simple"},
        {state: ADD, value: true},
        {state: END_OF_OBJECT}
    ];

    test("receives the messages the Java codec sends", async () => {
        const rq = new RpcReceiveQueue(new Map(), undefined,
            async () => structuredClone(javaWire), undefined, false);
        const received = await rq.receive<PythonResolutionResult>(undefined);

        expect((await rq.take()).state).toBe(END_OF_OBJECT);
        expect(received.path).toBe("pyproject.toml");
        expect(received.buildRequires.map(d => d.name)).toEqual(["hatchling"]);
        expect(received.dependencies[0].resolved).toBe(received.resolvedDependencies[0]);
        expect(received.resolvedDependencies[0].dependencies![0]).toBe(received.resolvedDependencies[1]);
        expect(received.constraintDependencies[0].versionConstraint).toBe("<3");
        expect(received.optionalDependencies).toEqual({socks: [{name: "PySocks", versionConstraint: ">=1.5.6"}]});
        expect(received.packageManager).toBe("Uv");
        expect(received.sourceIndexes![0].defaultIndex).toBe(true);
    });

    test("sends the messages the Java codec receives", async () => {
        const sq = new RpcSendQueue(new ReferenceMap(), undefined, false);
        const sent = await sq.generate(marker(), undefined);

        // Ref ids only have to agree within one side's stream; Java numbers from 1, TypeScript from 0
        const refIds = new Map<number, number>();
        const normalized = JSON.parse(JSON.stringify(sent)).map((d: RpcObjectData) => {
            if (d.ref !== undefined) {
                if (!refIds.has(d.ref)) {
                    refIds.set(d.ref, refIds.size + 1);
                }
                d.ref = refIds.get(d.ref);
            }
            return d;
        });
        expect(normalized).toEqual(javaWire);
    });

    test("round trips every field and consumes exactly the messages sent", async () => {
        const {received, next} = await roundTrip(marker());

        expect(next).toBe(RpcObjectState.END_OF_OBJECT);
        expect(received.kind).toBe(PRR);
        expect(received.id).toBe("7b6f6f7e-6e0b-4a53-9a6e-2f0f4d3b8c11");
        expect(received.name).toBe("my-app");
        expect(received.version).toBe("1.0.0");
        expect(received.description).toBe("A Python project");
        expect(received.license).toBe("MIT");
        expect(received.path).toBe("pyproject.toml");
        expect(received.requiresPython).toBe(">=3.10");
        expect(received.buildBackend).toBe("hatchling.build");
        expect(received.buildRequires.map(d => d.name)).toEqual(["hatchling"]);
        expect(received.constraintDependencies.map(d => d.versionConstraint)).toEqual(["<3"]);
        expect(received.overrideDependencies).toEqual([]);
        expect(received.optionalDependencies).toEqual({socks: [{name: "PySocks", versionConstraint: ">=1.5.6"}]});
        expect(received.dependencyGroups).toEqual({dev: [{name: "pytest"}]});
        expect(received.packageManager).toBe("Uv");
        expect(received.sourceIndexes).toEqual([{
            kind: `${PRR}$SourceIndex`,
            name: "pypi",
            url: "https://pypi.org/simple",
            defaultIndex: true
        }]);

        const dep = received.dependencies[0];
        expect(dep.kind).toBe(`${PRR}$Dependency`);
        expect(dep.name).toBe("requests");
        expect(dep.versionConstraint).toBe(">=2.28");
        expect(dep.extras).toEqual(["security"]);
        expect(dep.marker).toBe("python_version >= '3.8'");

        // `resolved` travels as a ref, so it resolves to the instance the resolution list holds
        expect(dep.resolved).toBe(received.resolvedDependencies[0]);
        expect(dep.resolved!.name).toBe("requests");
        expect(dep.resolved!.source).toBe("registry+https://pypi.org/simple");
        expect(dep.resolved!.dependencies![0]).toBe(received.resolvedDependencies[1]);
        expect(received.resolvedDependencies[1].name).toBe("urllib3");
    });

    test("a cyclic resolved-dependency graph closes onto the populated instance", async () => {
        const a: PythonResolvedDependency = {kind: `${PRR}$ResolvedDependency`, name: "a", version: "1.0.0", dependencies: []};
        const b: PythonResolvedDependency = {kind: `${PRR}$ResolvedDependency`, name: "b", version: "1.0.0", dependencies: [a]};
        a.dependencies!.push(b);
        const cyclic: PythonResolutionResult = {
            ...marker(),
            dependencies: [{kind: `${PRR}$Dependency`, name: "a", versionConstraint: "==1.0.0", resolved: a}],
            resolvedDependencies: [a, b]
        };

        const {received, next} = await roundTrip(cyclic);

        expect(next).toBe(RpcObjectState.END_OF_OBJECT);
        const receivedA = received.dependencies[0].resolved!;
        const receivedB = receivedA.dependencies![0];
        expect(receivedB.name).toBe("b");
        expect(receivedB.dependencies![0].name).toBe("a");
        expect(receivedB.dependencies![0]).toBe(receivedA);
    });

    test("absent optional fields round trip", async () => {
        const sparse: PythonResolutionResult = {
            kind: PRR,
            id: "0d6c9a57-54a0-4b8a-a2a8-5a0f5b1e1c3a",
            path: "requirements.txt",
            buildRequires: [],
            dependencies: [{kind: `${PRR}$Dependency`, name: "flask"}],
            optionalDependencies: {},
            dependencyGroups: {},
            constraintDependencies: [],
            overrideDependencies: [],
            resolvedDependencies: []
        };

        const {received, next} = await roundTrip(sparse);

        expect(next).toBe(RpcObjectState.END_OF_OBJECT);
        expect(received.name).toBeUndefined();
        expect(received.packageManager).toBeUndefined();
        expect(received.dependencies[0].name).toBe("flask");
        expect(received.dependencies[0].versionConstraint).toBeUndefined();
        expect(received.dependencies[0].resolved).toBeUndefined();
        // Java always sends sourceIndexes as a list, empty when it holds none
        expect(received.sourceIndexes).toEqual([]);
    });

    test("the marker in a source file's markers leaves the queue in sync", async () => {
        const markers: Markers = {
            kind: MarkersKind.Markers,
            id: "5f2f4b0a-3c0e-4e59-8d59-3f4f1c7a7e21",
            markers: [marker()]
        };

        const sq = new RpcSendQueue(new ReferenceMap(), undefined, false);
        const batch = await sq.generate(markers, undefined);
        const rq = new RpcReceiveQueue(new Map(), undefined, async () => batch, undefined, false);
        const received = await rq.receiveMarkers(undefined);

        expect((received.markers[0] as PythonResolutionResult).dependencies[0].resolved!.name).toBe("requests");
        expect((await rq.take()).state).toBe(RpcObjectState.END_OF_OBJECT);
    });
});
