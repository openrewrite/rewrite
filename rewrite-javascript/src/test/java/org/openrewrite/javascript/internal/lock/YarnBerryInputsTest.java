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
package org.openrewrite.javascript.internal.lock;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.lock.berry.RegistrySource;
import org.openrewrite.javascript.internal.lock.berry.YarnBerryLockRegenerator;
import org.openrewrite.javascript.internal.lock.berry.YarnBerryLockRegenerator.Inputs;
import org.openrewrite.javascript.internal.registry.NodeRegistries;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class YarnBerryInputsTest {

    /**
     * Yarn 4.10.3 run in a member reads every .yarnrc.yml from there up (open-member-yarnrc-run-in-member), not a
     * sibling's.
     */
    @Test
    void memberEditPlacesManifestsAndReadsTheRcFilesFromTheMemberUpToTheRepository() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"a\",\"version\":\"2.0.0\"}")
          .lock("__metadata:\n  version: 8\n")
          .packageJsonPath(Paths.get("repo/app/packages/a/package.json"))
          .manifest(Paths.get("repo/app/package.json"), "{\"private\":true,\"workspaces\":[\"packages/*\"]}")
          .manifest(Paths.get("repo/app/packages/a/package.json"), "{\"name\":\"a\",\"version\":\"1.0.0\"}")
          .packageManagerFile(Paths.get("repo/package.json"), "{\"name\":\"above\"}")
          .packageManagerFile(Paths.get("repo/app/.yarnrc.yml"), "compressionLevel: 0\n")
          .packageManagerFile(Paths.get("repo/.yarnrc.yml"), "enableTelemetry: false\n")
          .packageManagerFile(Paths.get("repo/app/packages/a/.yarnrc.yml"), "npmRegistryServer: x\n")
          .packageManagerFile(Paths.get("repo/app/packages/b/.yarnrc.yml"), "npmRegistryServer: y\n")
          .packageManagerFile(Paths.get("repo/app/.npmrc"), "registry=https://example.com/\n")
          .build();

        Inputs inputs = YarnBerryInputs.from(LockDirectory.of(request), Host.UNKNOWN, Instant.EPOCH, null, null);

        assertThat(inputs.getLock()).isEqualTo("__metadata:\n  version: 8\n");
        assertThat(inputs.getManifests()).containsOnlyKeys("", "packages/a");
        assertThat(inputs.getManifests().get("packages/a")).isEqualTo("{\"name\":\"a\",\"version\":\"2.0.0\"}");
        assertThat(inputs.getRunsIn()).isEqualTo("packages/a");
        assertThat(inputs.getConfigFiles()).containsOnlyKeys(".yarnrc.yml", "../.yarnrc.yml", "packages/a/.yarnrc.yml",
          "../package.json");
        assertThat(inputs.getConfigFiles().get(".yarnrc.yml")).isEqualTo("compressionLevel: 0\n");
        assertThat(inputs.getPaths()).isNull();
        assertThat(inputs.getEnv()).isNull();
        assertThat(inputs.getNow()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void thePathsUnderTheRootAreTheProjectsPaths() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\"}")
          .lock("")
          .packageJsonPath(Paths.get("repo/package.json"))
          .projectPaths(new HashSet<>(Arrays.asList(Paths.get("repo/package.json"), Paths.get("repo/.yarnrc.yml"),
            Paths.get("repo/packages/a/package.json"), Paths.get("other/.yarnrc.yml"))))
          .build();

        Inputs inputs = YarnBerryInputs.from(LockDirectory.of(request), Host.UNKNOWN, Instant.EPOCH, null, null);

        assertThat(inputs.getPaths()).containsExactlyInAnyOrder("package.json", ".yarnrc.yml",
          "packages/a/package.json");
        assertThat(inputs.getConfigFiles()).isEmpty();
    }

    /**
     * The host's registries are the home directory's .yarnrc.yml ({@code user-yarnrc-registry-bump},
     * {@code user-yarnrc-scoped-add}); npm's default, the host's answer when nothing names a registry, stays unnamed.
     */
    @Test
    void theHostsRegistriesAreTheHomeDirectorysYarnrc() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\"}")
          .lock("")
          .packageJsonPath(Paths.get("package.json"))
          .build();
        Map<String, NodeRegistry> scopes = new LinkedHashMap<>();
        scopes.put("@types", registry("https://registry.npmmirror.com/"));

        Inputs mirrored = YarnBerryInputs.from(LockDirectory.of(request), Host.UNKNOWN, Instant.EPOCH,
          new NodeRegistries(registry("https://registry.npmmirror.com/"), scopes, null, null, null), null);
        Inputs unnamed = YarnBerryInputs.from(LockDirectory.of(request), Host.UNKNOWN, Instant.EPOCH,
          new NodeRegistries(registry("https://registry.npmjs.org/"), new LinkedHashMap<>(), null, null, null), null);

        assertThat(mirrored.getHomeFiles()).containsOnlyKeys(".yarnrc.yml");
        assertThat(mirrored.getHomeFiles().get(".yarnrc.yml")).isEqualTo(
          "npmRegistryServer: \"https://registry.npmmirror.com/\"\n" +
          "npmScopes:\n  \"types\":\n    npmRegistryServer: \"https://registry.npmmirror.com/\"\n");
        assertThat(mirrored.getConfigFiles()).isEmpty();
        assertThat(unnamed.getHomeFiles().get(".yarnrc.yml")).isEmpty();
    }

    /**
     * The host's credentials are the default registry's npmRegistries entry and each scope's own settings, where
     * NpmHttpUtils.getAuthenticationHeader finds them (NpmHttpUtilsTest.sendsTheCredentialsYarnSends).
     */
    @Test
    void theHostsCredentialsAreTheHomeDirectorysYarnrc() {
        Map<String, NodeRegistry> scopes = new LinkedHashMap<>();
        scopes.put("@corp", new NodeRegistry("@corp", "https://corp.example.com/", "corp-token", null, null, null,
          false, null, true, false));
        NodeRegistry registry = new NodeRegistry(null, "https://r.example/npm/", null, "u", "p", null, true, null,
          true, false);

        NodeRegistries user = new NodeRegistries(registry, scopes, null, null, null);
        assertThat(YarnBerryInputs.userYarnrc(user, user)).isEqualTo(
          "npmRegistryServer: \"https://r.example/npm/\"\nnpmRegistries:\n  \"//r.example/npm\":\n" +
          "    npmAuthIdent: \"u:p\"\n    npmAlwaysAuth: true\nnpmScopes:\n  \"corp\":\n" +
          "    npmRegistryServer: \"https://corp.example.com/\"\n    npmAuthToken: \"corp-token\"\n");
    }

    /**
     * A registry only the project's .yarnrc.yml names keeps the credentials the host holds for it as an npmRegistries
     * entry, while the registry the package comes from stays the project's choice.
     */
    @Test
    void theHostsCredentialsForARegistryTheProjectNamesAreTheHomeDirectorysToo() {
        Request request = Request.builder()
          .packageJson("{\"name\":\"p\",\"version\":\"1.0.0\",\"packageManager\":\"yarn@4.10.3\"," +
                       "\"dependencies\":{\"@corp/a\":\"^1.0.0\"}}")
          .lock("__metadata:\n  version: 8\n  cacheKey: 10c0\n\n\"p@workspace:.\":\n  version: 0.0.0-use.local\n" +
                "  resolution: \"p@workspace:.\"\n  languageName: unknown\n  linkType: soft\n")
          .packageJsonPath(Paths.get("package.json"))
          .packageManagerFile(Paths.get(".yarnrc.yml"),
            "npmScopes:\n  corp:\n    npmRegistryServer: \"https://corp.example.com\"\n")
          .build();
        NodeRegistries user = new NodeRegistries(registry("https://registry.npmjs.org/"), new LinkedHashMap<>(),
          null, null, null);
        Map<String, NodeRegistry> scopes = new LinkedHashMap<>();
        scopes.put("@corp", new NodeRegistry("@corp", "https://corp.example.com/", "corp-token", null, null, null,
          false, null, true, false));
        NodeRegistries credentials = new NodeRegistries(registry("https://registry.npmjs.org/"), scopes, null, null,
          null);

        Inputs inputs = YarnBerryInputs.from(LockDirectory.of(request), Host.UNKNOWN, Instant.EPOCH, user,
          credentials);
        Map<String, @Nullable String> asked = new LinkedHashMap<>();
        String error = new YarnBerryLockRegenerator(inputs, new RegistrySource() {
            @Override
            public @Nullable String packument(String registry, String name, @Nullable String authorization) {
                asked.put(registry + " " + name, authorization);
                return null;
            }

            @Override
            public byte @Nullable [] tarball(String url, String name, String version,
                                             @Nullable String authorization) {
                return null;
            }
        }).regenerate().getErrorMessage();

        assertThat(inputs.getHomeFiles().get(".yarnrc.yml")).isEqualTo(
          "npmRegistries:\n  \"//corp.example.com\":\n    npmAuthToken: \"corp-token\"\n");
        assertThat(asked).as(String.valueOf(error)).containsExactly(entry("https://corp.example.com @corp/a", "Bearer corp-token"));
    }

    private static NodeRegistry registry(String url) {
        return new NodeRegistry(null, url, null, null, null, null, false, null, true, false);
    }
}
