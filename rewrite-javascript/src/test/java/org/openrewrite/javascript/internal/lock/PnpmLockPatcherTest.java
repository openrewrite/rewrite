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

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.javascript.internal.lock.LockEditSet.WriteThroughMetadata;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PnpmLockPatcherTest {

    private static final String MS_213 =
            "sha512-6FlzubTLZG3J2a/NVCAleEhjzq5oxgHyaCU9yYXvcLsvoVaHJq/s5xXI6/XXP6tz7R9xAOtHnSO/tXtF3WRTlA==";
    private static final String IS_NUMBER_700 =
            "sha512-41Cifkg6e8TylSpdtTpeLVMqvSBEVzTttHvERD741+pnZ8ANv0004MRL43QKPDlK9cGvNp6NZWZUBlbGXYxxng==";
    private static final String IS_NUMBER_600 =
            "sha512-Wu1VHeILBK8KAWJUAiSZQX94GmOE45Rg6/538fKwiloUu21KncEkYGPqob2oSZ5mUT73vLGrHQjKw3KMPwfDzg==";
    private static final String IS_ODD_301 =
            "sha512-CQpnWPrDwmP1+SMHXZhtLtJv90yiyVfluGsX5iNCVkrhQtU3TQHsUWPG9wkdk9Lgd5yNpAg9jQEo90CBaXgWMA==";

    private static Map<String, String> engines(String value) {
        Map<String, String> engines = new LinkedHashMap<>();
        engines.put("node", value);
        return engines;
    }

    private static String bump(String scenario, PackageEdit edit) {
        LockEditSet edits = new LockEditSet(
                read("/lock/pnpm/" + scenario + "/before"),
                Paths.get("pnpm-lock.yaml"),
                PackageManager.Pnpm,
                read("/lock/pnpm/" + scenario + "/pkg-after"),
                singletonList(edit));
        return new PnpmLockPatcher().patch(edits);
    }

    @Test
    void v9MetadataBump() {
        String patched = bump("v9", PackageEdit.builder()
                .name("ms").oldVersion("2.1.2").newVersion("2.1.3")
                .newIntegrity(MS_213).scope("dependencies").build());
        assertThat(patched).isEqualTo(read("/lock/pnpm/v9/after"));
    }

    @Test
    void v9EnginesWriteThroughBump() {
        Map<String, String> engines = new LinkedHashMap<>();
        engines.put("node", ">=0.12.0");
        String patched = bump("v9-engines", PackageEdit.builder()
                .name("is-number").oldVersion("6.0.0").newVersion("7.0.0")
                .newIntegrity(IS_NUMBER_700).scope("dependencies")
                .writeThroughMetadata(WriteThroughMetadata.builder().engines(engines).build())
                .build());
        assertThat(patched).isEqualTo(read("/lock/pnpm/v9-engines/after"));
    }

    @Test
    void v6MetadataBump() {
        String patched = bump("v6", PackageEdit.builder()
                .name("ms").oldVersion("2.1.2").newVersion("2.1.3")
                .newIntegrity(MS_213).scope("dependencies").build());
        assertThat(patched).isEqualTo(read("/lock/pnpm/v6/after"));
    }

    @Test
    void v9WorkspaceMemberBump() {
        LockEditSet edits = new LockEditSet(
                read("/lock/pnpm/v9-ws/before"),
                Paths.get("packages/app/pnpm-lock.yaml"),
                PackageManager.Pnpm,
                read("/lock/pnpm/v9-ws/pkg-app-after"),
                singletonList(PackageEdit.builder()
                        .name("ms").oldVersion("2.1.2").newVersion("2.1.3")
                        .newIntegrity(MS_213).scope("dependencies").importerDir("packages/app").build()));
        assertThat(new PnpmLockPatcher().patch(edits)).isEqualTo(read("/lock/pnpm/v9-ws/after"));
    }

    @Test
    void v9OrphanRemoval() {
        String patched = bump("v9-rm", PackageEdit.builder()
                .name("ms").oldVersion("2.1.2").newVersion(null).scope("dependencies").build());
        assertThat(patched).isEqualTo(read("/lock/pnpm/v9-rm/after"));
    }

    @Test
    void v9NonLeafRemovalGCsPrivateTransitiveByteExact() {
        // Removing debug (non-leaf) must also drop its now-orphaned private transitive ms; golden recorded
        // from a real `pnpm install --lockfile-only` (pnpm 11.2.2).
        String patched = bump("v9-rm-nonleaf", PackageEdit.builder()
                .name("debug").oldVersion("4.3.4").newVersion(null).scope("dependencies").build());
        assertThat(patched).isEqualTo(read("/lock/pnpm/v9-rm-nonleaf/after"));
    }

    @Test
    void v9LeafAddByteExact() {
        LockEditSet edits = new LockEditSet(read("/lock/pnpm/add-leaf/before"), Paths.get("pnpm-lock.yaml"),
                PackageManager.Pnpm, read("/lock/pnpm/add-leaf/pkg-after"),
                singletonList(PackageEdit.builder()
                        .name("is-number").oldVersion("").newVersion("7.0.0").newIntegrity(IS_NUMBER_700)
                        .scope("dependencies").kind(PackageEdit.Kind.ADD)
                        .writeThroughMetadata(WriteThroughMetadata.builder().engines(engines(">=0.12.0")).build())
                        .build()));
        assertThat(new PnpmLockPatcher().patch(edits)).isEqualTo(read("/lock/pnpm/add-leaf/after"));
    }

    @Test
    void v9CleanClosureAddByteExact() {
        LockEditSet edits = new LockEditSet(read("/lock/pnpm/add-closure/before"), Paths.get("pnpm-lock.yaml"),
                PackageManager.Pnpm, read("/lock/pnpm/add-closure/pkg-after"),
                Arrays.asList(
                        PackageEdit.builder()
                                .name("is-odd").oldVersion("").newVersion("3.0.1").newIntegrity(IS_ODD_301)
                                .newDependencies(Collections.singletonMap("is-number", "^6.0.0"))
                                .scope("dependencies").kind(PackageEdit.Kind.ADD)
                                .writeThroughMetadata(WriteThroughMetadata.builder().engines(engines(">=4")).build())
                                .build(),
                        PackageEdit.builder()
                                .name("is-number").oldVersion("").newVersion("6.0.0").newIntegrity(IS_NUMBER_600)
                                .scope("dependencies").kind(PackageEdit.Kind.ADD)
                                .writeThroughMetadata(WriteThroughMetadata.builder().engines(engines(">=0.10.0")).build())
                                .build()));
        assertThat(new PnpmLockPatcher().patch(edits)).isEqualTo(read("/lock/pnpm/add-closure/after"));
    }

    @Test
    void v6AddFailsLoud() {
        assertThatThrownBy(() -> {
            LockEditSet edits = new LockEditSet(read("/lock/pnpm/v6/before"), Paths.get("pnpm-lock.yaml"),
                    PackageManager.Pnpm, "{\"dependencies\":{\"is-number\":\"^7.0.0\"}}",
                    singletonList(PackageEdit.builder()
                            .name("is-number").oldVersion("").newVersion("7.0.0").newIntegrity(IS_NUMBER_700)
                            .scope("dependencies").kind(PackageEdit.Kind.ADD).build()));
            new PnpmLockPatcher().patch(edits);
        }).isInstanceOfSatisfying(EngineFailure.class,
                e -> assertThat(e.failure.getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED));
    }

    @Test
    void licenseWriteThroughFailsLoud() {
        assertThatThrownBy(() -> bump("v9", PackageEdit.builder()
                .name("ms").oldVersion("2.1.2").newVersion("2.1.3").newIntegrity(MS_213).scope("dependencies")
                .writeThroughMetadata(WriteThroughMetadata.builder().license("MIT").build()).build()))
                .isInstanceOfSatisfying(EngineFailure.class, e -> {
                    assertThat(e.failure.getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
                    assertThat(e.failure.getDetail()).contains("license");
                });
    }

    @Test
    void deprecatedWriteThroughFailsLoud() {
        assertThatThrownBy(() -> bump("v9", PackageEdit.builder()
                .name("ms").oldVersion("2.1.2").newVersion("2.1.3").newIntegrity(MS_213).scope("dependencies")
                .writeThroughMetadata(WriteThroughMetadata.builder().deprecated("no longer maintained").build()).build()))
                .isInstanceOfSatisfying(EngineFailure.class,
                        e -> assertThat(e.failure.getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED));
    }

    @Test
    void blockStyleResolutionDefersResolution() {
        String before = "lockfileVersion: '9.0'\n\n" +
                "importers:\n\n" +
                "  .:\n" +
                "    dependencies:\n" +
                "      ms:\n" +
                "        specifier: 2.1.2\n" +
                "        version: 2.1.2\n\n" +
                "packages:\n\n" +
                "  ms@2.1.2:\n" +
                "    resolution:\n" +
                "      integrity: sha512-OLD\n\n" +
                "snapshots:\n\n" +
                "  ms@2.1.2: {}\n";
        LockEditSet edits = new LockEditSet(before, Paths.get("pnpm-lock.yaml"), PackageManager.Pnpm,
                "{\"dependencies\":{\"ms\":\"2.1.3\"}}",
                singletonList(PackageEdit.builder()
                        .name("ms").oldVersion("2.1.2").newVersion("2.1.3").newIntegrity(MS_213)
                        .scope("dependencies").build()));
        assertThatThrownBy(() -> new PnpmLockPatcher().patch(edits))
                .isInstanceOfSatisfying(EngineFailure.class, e -> {
                    // The lock is valid; this patcher just cannot rewrite integrity inside a block-style resolution,
                    // so it defers to a real resolver rather than reporting the lock as malformed.
                    assertThat(e.failure.getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
                    assertThat(e.failure.getDetail()).contains("flow-scalar");
                });
    }

    @Test
    void roundTripV9PreservesBytes() {
        LockEditSet edits = new LockEditSet(read("/lock/pnpm/v9/before"), Paths.get("pnpm-lock.yaml"),
                PackageManager.Pnpm, "{}", Collections.<PackageEdit>emptyList());
        assertThat(new PnpmLockPatcher().patch(edits)).isEqualTo(read("/lock/pnpm/v9/before"));
    }

    @Test
    void roundTripV6PreservesBytes() {
        LockEditSet edits = new LockEditSet(read("/lock/pnpm/v6/before"), Paths.get("pnpm-lock.yaml"),
                PackageManager.Pnpm, "{}", Collections.<PackageEdit>emptyList());
        assertThat(new PnpmLockPatcher().patch(edits)).isEqualTo(read("/lock/pnpm/v6/before"));
    }

    @Test
    void v5_4FailsLoud() {
        assertThatThrownBy(() -> patchOnly("v5_4", PackageEdit.builder()
                .name("ms").oldVersion("2.1.2").newVersion("2.1.3").scope("dependencies").build()))
                .isInstanceOfSatisfying(EngineFailure.class,
                        e -> assertThat(e.failure.getReason()).isEqualTo(Reason.UNSUPPORTED_LOCKFILE_VERSION));
    }

    @Test
    void peerProviderFailsLoud() {
        assertThatThrownBy(() -> patchOnly("v9-peer", PackageEdit.builder()
                .name("react").oldVersion("18.2.0").newVersion("18.3.1")
                .newIntegrity("sha512-fake").scope("dependencies").build()))
                .isInstanceOfSatisfying(EngineFailure.class,
                        e -> assertThat(e.failure.getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED));
    }

    @Test
    void transitivelyReferencedBumpFailsLoud() {
        assertThatThrownBy(() -> patchOnly("v9-transitive", PackageEdit.builder()
                .name("is-number").oldVersion("6.0.0").newVersion("7.0.0")
                .newIntegrity(IS_NUMBER_700).scope("dependencies").build()))
                .isInstanceOfSatisfying(EngineFailure.class,
                        e -> assertThat(e.failure.getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED));
    }

    @Test
    void linkEntryUnsupported() {
        LockEditSet edits = new LockEditSet(read("/lock/pnpm/v9-link/before"), Paths.get("pnpm-lock.yaml"),
                PackageManager.Pnpm, "{}", singletonList(PackageEdit.builder()
                        .name("@ws/lib").oldVersion("link:../lib").newVersion("1.0.1")
                        .scope("dependencies").importerDir("packages/app").build()));
        assertThatThrownBy(() -> new PnpmLockPatcher().patch(edits))
                .isInstanceOfSatisfying(EngineFailure.class,
                        e -> assertThat(e.failure.getReason()).isEqualTo(Reason.UNSUPPORTED_ENTRY_TYPE));
    }

    private static String patchOnly(String scenario, PackageEdit edit) {
        return new PnpmLockPatcher().patch(new LockEditSet(read("/lock/pnpm/" + scenario + "/before"),
                Paths.get("pnpm-lock.yaml"), PackageManager.Pnpm, "{}", singletonList(edit)));
    }

    private static String read(String resource) {
        try (InputStream in = PnpmLockPatcherTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing test resource " + resource);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
