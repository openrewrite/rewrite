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
package org.openrewrite.javascript;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.internal.LockFileParserBridge;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.ResolvedDependency;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class LockFileParserParityTest {

    @BeforeEach
    void setUp() {
        DependencyWorkspace.clearCache();
    }

    @Test
    void parityNpmTinyProject() throws Exception {
        String packageJson = "{\n" +
                "  \"name\": \"parity-tiny\",\n" +
                "  \"dependencies\": { \"is-even\": \"^1.0.0\" }\n" +
                "}\n";
        Set<String> javaSet = parseLockInJava(packageJson);
        Set<String> tsSet = parseMarkerViaRpc(packageJson);

        assertThat(javaSet).isEqualTo(tsSet);
        assertThat(javaSet).contains("is-even@1.0.0");
    }

    @Test
    void parityNpmExpressProject() throws Exception {
        // express pulls ~50 transitive deps — exercises the moderate case.
        String packageJson = "{\n" +
                "  \"name\": \"parity-express\",\n" +
                "  \"dependencies\": { \"express\": \"4.18.2\" }\n" +
                "}\n";
        Set<String> javaSet = parseLockInJava(packageJson);
        Set<String> tsSet = parseMarkerViaRpc(packageJson);

        assertThat(javaSet).isEqualTo(tsSet);
        // sanity: express + at least 30 transitive deps
        assertThat(javaSet.size()).isGreaterThan(30);
        assertThat(javaSet).anyMatch(s -> s.startsWith("express@"));
    }

    @Test
    void parityNpmExpressEnginesAndLicense() throws Exception {
        String packageJson = "{\n" +
                "  \"name\": \"parity-express\",\n" +
                "  \"dependencies\": { \"express\": \"4.18.2\" }\n" +
                "}\n";
        Path workspace = DependencyWorkspace.getOrCreateWorkspace(packageJson);
        Path lockPath = workspace.resolve("package-lock.json");
        String lockContent = new String(Files.readAllBytes(lockPath));

        ResolvedDependency javaExpress = LockFileParserBridge.parse(lockContent).getAll().stream()
                .filter(d -> "express".equals(d.getName())).findFirst().orElseThrow();

        PackageJsonParser parser = new PackageJsonParser();
        Parser.Input input = Parser.Input.fromString(workspace.resolve("package.json"), packageJson);
        SourceFile sf = parser.parseInputs(Collections.singletonList(input), null,
                new InMemoryExecutionContext(Throwable::printStackTrace)).findFirst().orElseThrow();
        NodeResolutionResult marker = sf.getMarkers().findFirst(NodeResolutionResult.class).orElseThrow();
        ResolvedDependency tsExpress = marker.getResolvedDependencies().stream()
                .filter(d -> "express".equals(d.getName())).findFirst().orElseThrow();

        assertThat(javaExpress.getLicense()).isEqualTo(tsExpress.getLicense());
        assertThat(javaExpress.getEngines()).isEqualTo(tsExpress.getEngines());
    }

    private static Set<String> parseLockInJava(String packageJson) throws Exception {
        Path workspace = DependencyWorkspace.getOrCreateWorkspace(packageJson);
        Path lockPath = workspace.resolve("package-lock.json");
        String lockContent = new String(Files.readAllBytes(lockPath));
        return LockFileParserBridge.parse(lockContent).getAll().stream()
                .map(d -> d.getName() + "@" + d.getVersion())
                .collect(Collectors.toSet());
    }

    private static Set<String> parseMarkerViaRpc(String packageJson) throws Exception {
        // The TS parser resolves the lock file relative to the package.json path on disk.
        // We must point it at the workspace where package-lock.json actually exists.
        Path workspace = DependencyWorkspace.getOrCreateWorkspace(packageJson);
        Path packageJsonPath = workspace.resolve("package.json");
        PackageJsonParser parser = new PackageJsonParser();
        Parser.Input input = Parser.Input.fromString(packageJsonPath, packageJson);
        SourceFile sf = parser.parseInputs(Collections.singletonList(input), null,
                new InMemoryExecutionContext(Throwable::printStackTrace)).findFirst().orElseThrow();
        NodeResolutionResult marker = sf.getMarkers().findFirst(NodeResolutionResult.class).orElseThrow();
        return marker.getResolvedDependencies().stream()
                .map(d -> d.getName() + "@" + d.getVersion())
                .collect(Collectors.toSet());
    }
}
