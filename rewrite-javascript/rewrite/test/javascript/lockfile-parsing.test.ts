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
import {describe, test, expect} from "@jest/globals";
import {
    NodeResolutionResult,
    NodeResolutionResultQueries,
    PackageJsonParser,
    PackageManager,
    ResolvedDependency
} from "../../src/javascript";
import * as path from "path";

const fixturesDir = path.join(__dirname, "fixtures", "lockfiles");

/**
 * Tests for lock file parsing across different package managers.
 *
 * The test fixtures use a simple dependency tree:
 * - is-odd@3.0.1 (direct dependency)
 *   - is-number@6.0.0
 * - is-even@1.0.0 (dev dependency)
 *   - is-odd@0.1.2 (different version!)
 *     - is-number@3.0.0
 *       - kind-of@3.2.2
 *         - is-buffer@1.1.6
 *
 * This tests:
 * 1. Direct dependencies are resolved
 * 2. Transitive dependencies are resolved
 * 3. Multiple versions of the same package (is-odd, is-number) are handled
 * 4. Each package's own dependencies are captured
 */
describe("Lock file parsing", () => {
    /**
     * Common assertions that should pass for all package managers
     */
    function assertCommonExpectations(
        resolvedDeps: ResolvedDependency[],
        packageManagerName: string
    ) {
        // Should have resolved dependencies
        expect(resolvedDeps.length).toBeGreaterThan(0);

        // Direct dependency: is-odd@3.0.1
        const isOdd3 = resolvedDeps.find(d => d.name === "is-odd" && d.version === "3.0.1");
        expect(isOdd3).toBeDefined();
        expect(isOdd3!.dependencies).toBeDefined();
        expect(isOdd3!.dependencies!.some(d => d.name === "is-number")).toBe(true);

        // is-number@6.0.0 (dependency of is-odd@3.0.1)
        const isNumber6 = resolvedDeps.find(d => d.name === "is-number" && d.version === "6.0.0");
        expect(isNumber6).toBeDefined();

        // Dev dependency chain: is-even -> is-odd@0.1.2 -> is-number@3.0.0
        const isEven = resolvedDeps.find(d => d.name === "is-even" && d.version === "1.0.0");
        expect(isEven).toBeDefined();
        expect(isEven!.dependencies).toBeDefined();
        expect(isEven!.dependencies!.some(d => d.name === "is-odd")).toBe(true);

        // Multiple versions of is-odd (3.0.1 and 0.1.2)
        const isOddVersions = resolvedDeps.filter(d => d.name === "is-odd");
        expect(isOddVersions.length).toBe(2);
        const versions = isOddVersions.map(d => d.version).sort();
        expect(versions).toEqual(["0.1.2", "3.0.1"]);

        // Multiple versions of is-number (6.0.0 and 3.0.0)
        const isNumberVersions = resolvedDeps.filter(d => d.name === "is-number");
        expect(isNumberVersions.length).toBe(2);
        const numberVersions = isNumberVersions.map(d => d.version).sort();
        expect(numberVersions).toEqual(["3.0.0", "6.0.0"]);

        // Deep transitive: kind-of and is-buffer
        const kindOf = resolvedDeps.find(d => d.name === "kind-of");
        expect(kindOf).toBeDefined();
        expect(kindOf!.version).toBe("3.2.2");

        const isBuffer = resolvedDeps.find(d => d.name === "is-buffer");
        expect(isBuffer).toBeDefined();
        expect(isBuffer!.version).toBe("1.1.6");
    }

    /**
     * Helper to parse package.json using PackageJsonParser and extract the marker.
     * Returns null if parsing fails (e.g., CLI not available for pnpm/yarn).
     */
    async function parseAndGetMarker(fixtureSubDir: string): Promise<NodeResolutionResult | null> {
        const dir = path.join(fixturesDir, fixtureSubDir);
        const packageJsonPath = path.join(dir, "package.json");

        const parser = new PackageJsonParser({
            relativeTo: dir
        });

        const results: any[] = [];
        for await (const result of parser.parse(packageJsonPath)) {
            results.push(result);
        }

        if (results.length !== 1) return null;
        const sourceFile = results[0];

        // Find the NodeResolutionResult marker
        const marker = sourceFile.markers.markers.find(
            (m: any) => m.kind === "org.openrewrite.javascript.marker.NodeResolutionResult"
        ) as NodeResolutionResult | undefined;

        return marker || null;
    }

    /**
     * Helper that skips the test if marker couldn't be created (e.g., CLI not available).
     */
    async function getMarkerOrSkip(fixtureSubDir: string): Promise<NodeResolutionResult> {
        const marker = await parseAndGetMarker(fixtureSubDir);
        if (!marker || marker.resolvedDependencies.length === 0) {
            // Skip test if we couldn't resolve dependencies (CLI not available or packages not installed)
            return null as any; // Will be caught by the conditional skip
        }
        return marker;
    }

    describe("npm (package-lock.json)", () => {
        test("should parse all dependencies from package-lock.json", async () => {
            const marker = await parseAndGetMarker("npm");
            expect(marker).not.toBeNull();
            assertCommonExpectations(marker!.resolvedDependencies, "npm");
        });

        test("should set packageManager to Npm", async () => {
            const marker = await parseAndGetMarker("npm");
            expect(marker).not.toBeNull();
            expect(marker!.packageManager).toBe(PackageManager.Npm);
        });

        test("should capture license information", async () => {
            const marker = await parseAndGetMarker("npm");
            expect(marker).not.toBeNull();

            // Use the resolved property from direct dependency
            const isOdd = marker!.dependencies.find(d => d.name === "is-odd")?.resolved;
            expect(isOdd).toBeDefined();
            expect(isOdd!.license).toBe("MIT");
        });

        test("should capture engine requirements", async () => {
            const marker = await parseAndGetMarker("npm");
            expect(marker).not.toBeNull();

            // Get all versions of is-odd to find the one with node>=4
            const isOddVersions = NodeResolutionResultQueries.getAllResolvedVersions(marker!, "is-odd");
            const isOdd3 = isOddVersions.find(d => d.version === "3.0.1");
            expect(isOdd3).toBeDefined();
            expect(isOdd3!.engines).toEqual({node: ">=4"});

            // Also verify the older version has different engines
            const isOdd0 = isOddVersions.find(d => d.version === "0.1.2");
            expect(isOdd0).toBeDefined();
            expect(isOdd0!.engines).toEqual({node: ">=0.10.0"});
        });

        test("should find correct transitive dependency version via resolved property", async () => {
            const marker = await parseAndGetMarker("npm");
            expect(marker).not.toBeNull();

            // Navigate: is-odd (direct dep) -> is-number (transitive)
            // is-odd@3.0.1 depends on is-number@6.0.0
            const isOdd = marker!.dependencies.find(d => d.name === "is-odd")?.resolved;
            expect(isOdd).toBeDefined();
            expect(isOdd!.version).toBe("3.0.1");

            const isNumber = isOdd!.dependencies?.find(d => d.name === "is-number")?.resolved;
            expect(isNumber).toBeDefined();
            expect(isNumber!.version).toBe("6.0.0");
        });

        test("dependencies should have resolved property linked to ResolvedDependency", async () => {
            const marker = await parseAndGetMarker("npm");
            expect(marker).not.toBeNull();

            // Check that direct dependencies from package.json have resolved linked
            const isOddDep = marker!.dependencies.find(d => d.name === "is-odd");
            expect(isOddDep).toBeDefined();
            expect(isOddDep!.resolved).toBeDefined();
            expect(isOddDep!.resolved!.name).toBe("is-odd");
            expect(isOddDep!.resolved!.version).toBe("3.0.1");

            // Dev dependencies should also have resolved linked
            const isEvenDep = marker!.devDependencies.find(d => d.name === "is-even");
            expect(isEvenDep).toBeDefined();
            expect(isEvenDep!.resolved).toBeDefined();
            expect(isEvenDep!.resolved!.name).toBe("is-even");
            expect(isEvenDep!.resolved!.version).toBe("1.0.0");
        });

        test("transitive dependencies in ResolvedDependency should also have resolved linked", async () => {
            const marker = await parseAndGetMarker("npm");
            expect(marker).not.toBeNull();

            // Get is-odd@3.0.1 resolved dependency
            const isOdd3 = marker!.resolvedDependencies.find(
                d => d.name === "is-odd" && d.version === "3.0.1"
            );
            expect(isOdd3).toBeDefined();

            // Its dependency on is-number should have resolved linked
            const isNumberDep = isOdd3!.dependencies!.find(d => d.name === "is-number");
            expect(isNumberDep).toBeDefined();
            expect(isNumberDep!.resolved).toBeDefined();
            expect(isNumberDep!.resolved!.name).toBe("is-number");
            // Should be is-number@6.0.0 (the version is-odd@3.0.1 uses)
            expect(isNumberDep!.resolved!.version).toBe("6.0.0");
        });

        test("navigating dependency tree via resolved property", async () => {
            const marker = await parseAndGetMarker("npm");
            expect(marker).not.toBeNull();

            // Navigate from is-odd@3.0.1 -> is-number@6.0.0 using resolved
            const isOddDep = marker!.dependencies.find(d => d.name === "is-odd");
            const isOdd = isOddDep!.resolved!;
            expect(isOdd.version).toBe("3.0.1");

            // Follow to is-number
            const isNumberDep = isOdd.dependencies!.find(d => d.name === "is-number");
            const isNumber = isNumberDep!.resolved!;
            expect(isNumber.name).toBe("is-number");
            expect(isNumber.version).toBe("6.0.0");

            // is-number@6.0.0 has no dependencies, it's a leaf
            expect(isNumber.dependencies).toBeUndefined();
        });
    });

    describe("bun (bun.lock)", () => {
        test("should parse all dependencies from bun.lock", async () => {
            const marker = await parseAndGetMarker("bun");
            expect(marker).not.toBeNull();
            assertCommonExpectations(marker!.resolvedDependencies, "bun");
        });

        test("should set packageManager to Bun", async () => {
            const marker = await parseAndGetMarker("bun");
            expect(marker).not.toBeNull();
            expect(marker!.packageManager).toBe(PackageManager.Bun);
        });
    });

    // pnpm and yarn tests require CLI to be available and packages installed
    // They will be skipped if the marker has no resolved dependencies
    describe("pnpm (pnpm-lock.yaml)", () => {
        test("should parse all dependencies from pnpm-lock.yaml", async () => {
            const marker = await getMarkerOrSkip("pnpm");
            if (!marker) return; // Skip if node_modules not available
            assertCommonExpectations(marker.resolvedDependencies, "pnpm");
        });

        test("should set packageManager to Pnpm", async () => {
            const marker = await getMarkerOrSkip("pnpm");
            if (!marker) return; // Skip if node_modules not available
            expect(marker.packageManager).toBe(PackageManager.Pnpm);
        });

        test("dependencies should resolve to correct version using path-based resolution", async () => {
            const marker = await getMarkerOrSkip("pnpm");
            if (!marker) return; // Skip if node_modules not available

            // is-odd@^3.0.1 should resolve to is-odd@3.0.1 (not 0.1.2)
            const isOddDep = marker.dependencies.find(d => d.name === "is-odd");
            expect(isOddDep).toBeDefined();
            expect(isOddDep!.resolved).toBeDefined();
            expect(isOddDep!.resolved!.version).toBe("3.0.1");

            // is-even's dependency on is-odd@^0.1.2 should resolve to 0.1.2
            const isEvenResolved = marker.resolvedDependencies.find(
                d => d.name === "is-even" && d.version === "1.0.0"
            );
            expect(isEvenResolved).toBeDefined();
            const nestedIsOdd = isEvenResolved!.dependencies!.find(d => d.name === "is-odd");
            expect(nestedIsOdd).toBeDefined();
            expect(nestedIsOdd!.resolved).toBeDefined();
            expect(nestedIsOdd!.resolved!.version).toBe("0.1.2");
        });

        test("should capture license and engine information from node_modules", async () => {
            const marker = await getMarkerOrSkip("pnpm");
            if (!marker) return; // Skip if node_modules not available

            // Use getAllResolvedVersions to get the specific version we want
            const isOddVersions = NodeResolutionResultQueries.getAllResolvedVersions(marker, "is-odd");
            const isOdd3 = isOddVersions.find(d => d.version === "3.0.1");
            expect(isOdd3).toBeDefined();
            expect(isOdd3!.license).toBe("MIT");
            expect(isOdd3!.engines).toEqual({node: ">=4"});
        });
    });

    describe("yarn classic (yarn.lock v1)", () => {
        test("should parse all dependencies from yarn.lock", async () => {
            const marker = await getMarkerOrSkip("yarn");
            if (!marker) return; // Skip if node_modules not available
            assertCommonExpectations(marker.resolvedDependencies, "yarn");
        });

        test("should set packageManager to YarnClassic", async () => {
            const marker = await getMarkerOrSkip("yarn");
            if (!marker) return; // Skip if node_modules not available
            expect(marker.packageManager).toBe(PackageManager.YarnClassic);
        });

        test("dependencies should resolve to correct version using path-based resolution", async () => {
            const marker = await getMarkerOrSkip("yarn");
            if (!marker) return; // Skip if node_modules not available

            // is-odd@^3.0.1 should resolve to is-odd@3.0.1 (not 0.1.2)
            const isOddDep = marker.dependencies.find(d => d.name === "is-odd");
            expect(isOddDep).toBeDefined();
            expect(isOddDep!.resolved).toBeDefined();
            expect(isOddDep!.resolved!.version).toBe("3.0.1");

            // is-even's dependency on is-odd@^0.1.2 should resolve to 0.1.2
            const isEvenResolved = marker.resolvedDependencies.find(
                d => d.name === "is-even" && d.version === "1.0.0"
            );
            expect(isEvenResolved).toBeDefined();
            const nestedIsOdd = isEvenResolved!.dependencies!.find(d => d.name === "is-odd");
            expect(nestedIsOdd).toBeDefined();
            expect(nestedIsOdd!.resolved).toBeDefined();
            expect(nestedIsOdd!.resolved!.version).toBe("0.1.2");
        });

        test("should capture license and engine information from node_modules", async () => {
            const marker = await getMarkerOrSkip("yarn");
            if (!marker) return; // Skip if node_modules not available

            // Use getAllResolvedVersions to get the specific version we want
            const isOddVersions = NodeResolutionResultQueries.getAllResolvedVersions(marker, "is-odd");
            const isOdd3 = isOddVersions.find(d => d.version === "3.0.1");
            expect(isOdd3).toBeDefined();
            expect(isOdd3!.license).toBe("MIT");
            expect(isOdd3!.engines).toEqual({node: ">=4"});

            // Also check the older version has different engines
            const isOdd0 = isOddVersions.find(d => d.version === "0.1.2");
            expect(isOdd0).toBeDefined();
            expect(isOdd0!.engines).toEqual({node: ">=0.10.0"});
        });
    });

    describe("yarn berry (yarn.lock v2+)", () => {
        test("should parse all dependencies from yarn.lock", async () => {
            const marker = await getMarkerOrSkip("yarn-berry");
            if (!marker) return; // Skip if CLI not available
            assertCommonExpectations(marker.resolvedDependencies, "yarn-berry");
        });

        test("should set packageManager to YarnBerry", async () => {
            const marker = await getMarkerOrSkip("yarn-berry");
            if (!marker) return; // Skip if CLI not available
            expect(marker.packageManager).toBe(PackageManager.YarnBerry);
        });
    });
});
