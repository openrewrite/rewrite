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
    MSBuildProject,
    MSBuildProjectReference,
    MSBuildResolvedPackage,
    ReferenceMap,
    RpcObjectData,
    RpcObjectState,
    RpcReceiveQueue,
    RpcSendQueue
} from "../../src/rpc";

const {ADD, CHANGE, NO_CHANGE, END_OF_OBJECT} = RpcObjectState;

// A Java host sends this marker on the Xml document of every .csproj file, so a JavaScript recipe
// run over a repository that also holds .NET projects receives it.
describe("MSBuildProject over RPC", () => {

    const MSB = "org.openrewrite.csharp.marker.MSBuildProject";

    function resolvedPackage(name: string, resolvedVersion: string, depth: number,
                             fields: Partial<MSBuildResolvedPackage> = {}): MSBuildResolvedPackage {
        return {
            kind: `${MSB}$ResolvedPackage`,
            name,
            resolvedVersion,
            dependencies: [],
            depth,
            type: "package",
            compileAssemblies: [],
            runtimeAssemblies: [],
            frameworkAssemblies: [],
            buildFiles: [],
            buildMultiTargetingFiles: [],
            contentFiles: [],
            runtimeTargets: [],
            resourceAssemblies: [],
            analyzerAssemblies: [],
            hasInstallScripts: false,
            hasXdtTransforms: false,
            hasLegacyContentFolder: false,
            dependencyRanges: {},
            ...fields
        };
    }

    function projectReference(include: string, name: string | undefined, depth: number,
                              fields: Partial<MSBuildProjectReference> = {}): MSBuildProjectReference {
        return {
            kind: `${MSB}$ProjectReference`,
            include,
            name,
            depth,
            projectReferences: [],
            resolvedPackages: [],
            ...fields
        };
    }

    // The same marker as the one the Java wire below was captured from
    function marker(): MSBuildProject {
        const logging = resolvedPackage("Microsoft.Extensions.Logging.Abstractions", "8.0.0", 1, {
            compileAssemblies: ["lib/net8.0/Microsoft.Extensions.Logging.Abstractions.dll"],
            runtimeAssemblies: ["lib/net8.0/Microsoft.Extensions.Logging.Abstractions.dll"],
            buildFiles: ["buildTransitive/net6.0/Microsoft.Extensions.Logging.Abstractions.targets"],
            analyzerAssemblies: ["analyzers/dotnet/roslyn4.4/cs/Microsoft.Extensions.Logging.Generators.dll"]
        });
        const serilog = resolvedPackage("Serilog.Extensions.Logging", "8.0.0", 0, {
            dependencies: [logging],
            compileAssemblies: ["lib/net8.0/Serilog.Extensions.Logging.dll"],
            resourceAssemblies: ["lib/net8.0/de/Serilog.Extensions.Logging.resources.dll"],
            hasInstallScripts: true,
            hasLegacyContentFolder: true,
            dependencyRanges: {"Microsoft.Extensions.Logging.Abstractions": "[8.0.0, )"}
        });
        const helpers = projectReference("../Ombi.Helpers/Ombi.Helpers.csproj", "Ombi.Helpers", 1, {
            resolvedPackages: [logging]
        });
        const core = projectReference("../Ombi.Core/Ombi.Core.csproj", "Ombi.Core", 0, {
            projectReferences: [helpers],
            resolvedPackages: [serilog]
        });
        return {
            kind: MSB,
            id: "3c1f9e7a-2b4d-4f6e-8a1c-5d7e9f0b2a4c",
            sdk: "Microsoft.NET.Sdk.Web",
            properties: {
                TargetFramework: {kind: `${MSB}$PropertyValue`, value: "net8.0", definedIn: "src/Ombi/Ombi.csproj"},
                SerilogVersion: {kind: `${MSB}$PropertyValue`, value: "8.0.0", definedIn: "Directory.Build.props"},
                LangVersion: {kind: `${MSB}$PropertyValue`, value: "latest"}
            },
            packageSources: [{kind: `${MSB}$PackageSource`, key: "nuget.org", url: "https://api.nuget.org/v3/index.json"}],
            targetFrameworks: [{
                kind: `${MSB}$TargetFramework`,
                targetFramework: "net8.0",
                packageReferences: [
                    {
                        kind: `${MSB}$PackageReference`,
                        include: "Serilog.Extensions.Logging",
                        requestedVersion: "$(SerilogVersion)",
                        resolvedVersion: "8.0.0"
                    },
                    {kind: `${MSB}$PackageReference`, include: "Unversioned"}
                ],
                resolvedPackages: [serilog, logging],
                projectReferences: [core, helpers]
            }]
        };
    }

    function roundTrip<T>(value: T): { received: T, next: RpcObjectState } {
        const sq = new RpcSendQueue(new ReferenceMap(), undefined, false);
        const batch = sq.generate(value, undefined);
        const rq = new RpcReceiveQueue(new Map(), undefined, [batch], undefined, false);
        const received = rq.receive<T>(undefined);
        // Every message of the object must have been consumed, or later objects desynchronize
        const next = rq.take();
        return {received, next: next.state};
    }

    // What Java's RpcSendQueue emits for marker() above, captured from the Java class. Every nested
    // type travels property by property after an ADD naming it, the properties map as a list of
    // keys and a list of values, and the resolved packages and project references by ref, so the
    // shared and repeated instances travel once and then as a bare ref.
    const javaWire: RpcObjectData[] = [
        {state: ADD, valueType: MSB},
        {state: ADD, value: "3c1f9e7a-2b4d-4f6e-8a1c-5d7e9f0b2a4c"},
        {state: ADD, value: "Microsoft.NET.Sdk.Web"},
        // properties, keys
        {state: ADD},
        {state: CHANGE, value: [-1, -1, -1]},
        {state: ADD},
        {state: ADD, value: "TargetFramework"},
        {state: ADD},
        {state: ADD, value: "SerilogVersion"},
        {state: ADD},
        {state: ADD, value: "LangVersion"},
        // properties, values
        {state: ADD},
        {state: CHANGE, value: [-1, -1, -1]},
        {state: ADD, valueType: `${MSB}$PropertyValue`},
        {state: ADD, value: "net8.0"},
        {state: ADD, value: "src/Ombi/Ombi.csproj"},
        {state: ADD, valueType: `${MSB}$PropertyValue`},
        {state: ADD, value: "8.0.0"},
        {state: ADD, value: "Directory.Build.props"},
        {state: ADD, valueType: `${MSB}$PropertyValue`},
        {state: ADD, value: "latest"},
        {state: NO_CHANGE},
        // packageSources
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD, valueType: `${MSB}$PackageSource`},
        {state: ADD, value: "nuget.org"},
        {state: ADD, value: "https://api.nuget.org/v3/index.json"},
        // targetFrameworks
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD, valueType: `${MSB}$TargetFramework`},
        {state: ADD, value: "net8.0"},
        // packageReferences
        {state: ADD},
        {state: CHANGE, value: [-1, -1]},
        {state: ADD, valueType: `${MSB}$PackageReference`},
        {state: ADD, value: "Serilog.Extensions.Logging"},
        {state: ADD, value: "$(SerilogVersion)"},
        {state: ADD, value: "8.0.0"},
        {state: ADD, valueType: `${MSB}$PackageReference`},
        {state: ADD, value: "Unversioned"},
        {state: NO_CHANGE},
        {state: NO_CHANGE},
        // resolvedPackages, as refs
        {state: ADD},
        {state: CHANGE, value: [-1, -1]},
        {state: ADD, valueType: `${MSB}$ResolvedPackage`, ref: 1},
        {state: ADD, value: "Serilog.Extensions.Logging"},
        {state: ADD, value: "8.0.0"},
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD, valueType: `${MSB}$ResolvedPackage`, ref: 2},
        {state: ADD, value: "Microsoft.Extensions.Logging.Abstractions"},
        {state: ADD, value: "8.0.0"},
        {state: ADD},
        {state: CHANGE, value: []},
        {state: ADD, value: 1},
        {state: ADD, value: "package"},
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD},
        {state: ADD, value: "lib/net8.0/Microsoft.Extensions.Logging.Abstractions.dll"},
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD},
        {state: ADD, value: "lib/net8.0/Microsoft.Extensions.Logging.Abstractions.dll"},
        {state: ADD},
        {state: CHANGE, value: []},
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD},
        {state: ADD, value: "buildTransitive/net6.0/Microsoft.Extensions.Logging.Abstractions.targets"},
        {state: ADD},
        {state: CHANGE, value: []},
        {state: ADD},
        {state: CHANGE, value: []},
        {state: ADD},
        {state: CHANGE, value: []},
        {state: ADD},
        {state: CHANGE, value: []},
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD},
        {state: ADD, value: "analyzers/dotnet/roslyn4.4/cs/Microsoft.Extensions.Logging.Generators.dll"},
        {state: ADD, value: false},
        {state: ADD, value: false},
        {state: ADD, value: false},
        {state: ADD},
        {state: CHANGE, value: []},
        {state: ADD},
        {state: CHANGE, value: []},
        {state: ADD, value: 0},
        {state: ADD, value: "package"},
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD},
        {state: ADD, value: "lib/net8.0/Serilog.Extensions.Logging.dll"},
        {state: ADD},
        {state: CHANGE, value: []},
        {state: ADD},
        {state: CHANGE, value: []},
        {state: ADD},
        {state: CHANGE, value: []},
        {state: ADD},
        {state: CHANGE, value: []},
        {state: ADD},
        {state: CHANGE, value: []},
        {state: ADD},
        {state: CHANGE, value: []},
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD},
        {state: ADD, value: "lib/net8.0/de/Serilog.Extensions.Logging.resources.dll"},
        {state: ADD},
        {state: CHANGE, value: []},
        {state: ADD, value: true},
        {state: ADD, value: false},
        {state: ADD, value: true},
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD},
        {state: ADD, value: "Microsoft.Extensions.Logging.Abstractions"},
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD},
        {state: ADD, value: "[8.0.0, )"},
        {state: ADD, ref: 2},
        {state: ADD},
        // projectReferences, as refs
        {state: CHANGE, value: [-1, -1]},
        {state: ADD, valueType: `${MSB}$ProjectReference`, ref: 3},
        {state: ADD, value: "../Ombi.Core/Ombi.Core.csproj"},
        {state: ADD, value: "Ombi.Core"},
        {state: ADD, value: 0},
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD, valueType: `${MSB}$ProjectReference`, ref: 4},
        {state: ADD, value: "../Ombi.Helpers/Ombi.Helpers.csproj"},
        {state: ADD, value: "Ombi.Helpers"},
        {state: ADD, value: 1},
        {state: ADD},
        {state: CHANGE, value: []},
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD, ref: 2},
        {state: ADD},
        {state: CHANGE, value: [-1]},
        {state: ADD, ref: 1},
        {state: ADD, ref: 4},
        {state: END_OF_OBJECT}
    ];

    test("receives the messages the Java codec sends", () => {
        const rq = new RpcReceiveQueue(new Map(), undefined,
            [structuredClone(javaWire)], undefined, false);
        const received = rq.receive<MSBuildProject>(undefined);

        expect(rq.take().state).toBe(END_OF_OBJECT);
        expect(received).toEqual(marker());

        const tf = received.targetFrameworks[0];
        const [serilog, logging] = tf.resolvedPackages;
        const [core, helpers] = tf.projectReferences;
        // Instances Java shares arrive shared
        expect(serilog.dependencies[0]).toBe(logging);
        expect(core.resolvedPackages[0]).toBe(serilog);
        expect(core.projectReferences[0]).toBe(helpers);
        expect(helpers.resolvedPackages[0]).toBe(logging);
    });

    test("sends the messages the Java codec receives", () => {
        const sq = new RpcSendQueue(new ReferenceMap(), undefined, false);
        const sent = sq.generate(marker(), undefined);

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

    test("round trips every field and consumes exactly the messages sent", () => {
        const {received, next} = roundTrip(marker());

        expect(next).toBe(END_OF_OBJECT);
        expect(received).toEqual(marker());
        expect(received.properties.LangVersion.definedIn).toBeUndefined();
        expect(received.targetFrameworks[0].packageReferences[1].requestedVersion).toBeUndefined();
        expect(received.targetFrameworks[0].resolvedPackages[0].dependencyRanges)
            .toEqual({"Microsoft.Extensions.Logging.Abstractions": "[8.0.0, )"});
    });

    test("cyclic package and project graphs close onto the populated instances", () => {
        const a = resolvedPackage("A", "1.0.0", 0);
        const b = resolvedPackage("B", "1.0.0", 1, {dependencies: [a]});
        a.dependencies.push(b);
        const p = projectReference("../P/P.csproj", "P", 0, {resolvedPackages: [a]});
        const q = projectReference("../Q/Q.csproj", "Q", 1, {projectReferences: [p], resolvedPackages: [b]});
        p.projectReferences.push(q);
        const cyclic: MSBuildProject = {
            ...marker(),
            targetFrameworks: [{
                kind: `${MSB}$TargetFramework`,
                targetFramework: "net8.0",
                packageReferences: [],
                resolvedPackages: [a, b],
                projectReferences: [p, q]
            }]
        };

        const {received, next} = roundTrip(cyclic);

        expect(next).toBe(END_OF_OBJECT);
        const tf = received.targetFrameworks[0];
        const [receivedA, receivedB] = tf.resolvedPackages;
        expect(receivedA.dependencies[0]).toBe(receivedB);
        expect(receivedB.dependencies[0]).toBe(receivedA);
        expect(receivedB.name).toBe("B");
        const [receivedP, receivedQ] = tf.projectReferences;
        expect(receivedP.projectReferences[0]).toBe(receivedQ);
        expect(receivedQ.projectReferences[0]).toBe(receivedP);
        expect(receivedQ.resolvedPackages[0]).toBe(receivedB);
        expect(receivedQ.include).toBe("../Q/Q.csproj");
    });

    test("absent optional fields round trip", () => {
        const sparse: MSBuildProject = {
            kind: MSB,
            id: "0d6c9a57-54a0-4b8a-a2a8-5a0f5b1e1c3a",
            properties: {},
            packageSources: [],
            targetFrameworks: [{
                kind: `${MSB}$TargetFramework`,
                targetFramework: "net48",
                packageReferences: [],
                resolvedPackages: [],
                projectReferences: [projectReference("../Lib/Lib.csproj", undefined, 0)]
            }]
        };

        const {received, next} = roundTrip(sparse);

        expect(next).toBe(END_OF_OBJECT);
        expect(received).toEqual(sparse);
        expect(received.sdk).toBeUndefined();
        expect(received.targetFrameworks[0].projectReferences[0].name).toBeUndefined();
    });

    test("a changed marker applies onto the one received before", () => {
        const sendRefs = new ReferenceMap();
        const receiveRefs = new Map<number, any>();
        const before = marker();
        const first = new RpcReceiveQueue(receiveRefs, undefined,
            [new RpcSendQueue(sendRefs, undefined, false).generate(before, undefined)], undefined, false)
            .receive<MSBuildProject>(undefined);

        const after: MSBuildProject = {
            ...before,
            properties: {
                ...before.properties,
                SerilogVersion: {...before.properties.SerilogVersion, value: "9.0.0"},
                Nullable: {kind: `${MSB}$PropertyValue`, value: "enable", definedIn: "Directory.Build.props"}
            },
            packageSources: [...before.packageSources,
                {kind: `${MSB}$PackageSource`, key: "internal", url: "https://nuget.example.com/v3/index.json"}]
        };
        const rq = new RpcReceiveQueue(receiveRefs, undefined,
            [new RpcSendQueue(sendRefs, undefined, false).generate(after, before)], undefined, false);
        const second = rq.receive<MSBuildProject>(first);

        expect(rq.take().state).toBe(END_OF_OBJECT);
        expect(second).toEqual(after);
        expect(Object.keys(second.properties)).toEqual(["TargetFramework", "SerilogVersion", "LangVersion", "Nullable"]);
        // What did not change is the instance received before
        expect(second.properties.TargetFramework).toBe(first.properties.TargetFramework);
        expect(second.targetFrameworks[0].resolvedPackages[0]).toBe(first.targetFrameworks[0].resolvedPackages[0]);
    });

    test("the marker in a source file's markers leaves the queue in sync", () => {
        const markers: Markers = {
            kind: MarkersKind.Markers,
            id: "5f2f4b0a-3c0e-4e59-8d59-3f4f1c7a7e21",
            markers: [marker()]
        };

        const sq = new RpcSendQueue(new ReferenceMap(), undefined, false);
        const batch = sq.generate(markers, undefined);
        const rq = new RpcReceiveQueue(new Map(), undefined, [batch], undefined, false);
        const received = rq.receiveMarkers(undefined);

        expect((received.markers[0] as MSBuildProject).targetFrameworks[0].resolvedPackages[0].name)
            .toBe("Serilog.Extensions.Logging");
        expect(rq.take().state).toBe(END_OF_OBJECT);
    });
});
