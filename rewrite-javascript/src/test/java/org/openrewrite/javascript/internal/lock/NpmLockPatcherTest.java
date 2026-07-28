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
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NpmLockPatcherTest {

    private static final String ODD_301_RESOLVED = "https://registry.npmjs.org/is-odd/-/is-odd-3.0.1.tgz";
    private static final String ODD_301_INTEGRITY =
            "sha512-CQpnWPrDwmP1+SMHXZhtLtJv90yiyVfluGsX5iNCVkrhQtU3TQHsUWPG9wkdk9Lgd5yNpAg9jQEo90CBaXgWMA==";

    private static String golden(String path) {
        URL url = NpmLockPatcherTest.class.getClassLoader().getResource("lock/npm/" + path);
        assertThat(url).as("missing golden lock/npm/%s", path).isNotNull();
        try {
            return new String(Files.readAllBytes(Paths.get(url.toURI())), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            throw new UncheckedIOException(new IOException("cannot read golden " + path, e));
        }
    }

    private static LockEditSet editSet(String scenario, PackageEdit edit) {
        return new LockEditSet(
                golden(scenario + "/before"),
                Paths.get("package-lock.json"),
                PackageManager.Npm,
                golden(scenario + "/pkg-after"),
                singletonList(edit));
    }

    private static PackageEdit oddBump(@Nullable String importerDir) {
        return PackageEdit.builder()
                .name("is-odd")
                .oldVersion("3.0.0")
                .newVersion("3.0.1")
                .newResolved(ODD_301_RESOLVED)
                .newIntegrity(ODD_301_INTEGRITY)
                .scope("dependencies")
                .oldConstraint("3.0.0")
                .importerDir(importerDir)
                .build();
    }

    @Test
    void v3MetadataBumpIsByteExact() {
        String out = new NpmLockPatcher().patch(editSet("v3", oddBump(null)));
        assertThat(out).isEqualTo(golden("v3/after"));
    }

    @Test
    void v2DualMapBumpIsByteExact() {
        String out = new NpmLockPatcher().patch(editSet("v2", oddBump(null)));
        assertThat(out).isEqualTo(golden("v2/after"));
    }

    @Test
    void workspaceMemberBumpIsByteExact() {
        String out = new NpmLockPatcher().patch(editSet("workspace", oddBump("packages/foo")));
        assertThat(out).isEqualTo(golden("workspace/after"));
    }

    @Test
    void leafRemovalIsByteExact() {
        PackageEdit remove = PackageEdit.builder()
                .name("left-pad")
                .oldVersion("1.3.0")
                .newVersion(null)
                .scope("dependencies")
                .oldConstraint("1.3.0")
                .importerDir(null)
                .build();
        String out = new NpmLockPatcher().patch(editSet("remove-leaf", remove));
        assertThat(out).isEqualTo(golden("remove-leaf/after"));
    }

    @Test
    void orphanRemovalIsByteExact() {
        PackageEdit remove = PackageEdit.builder()
                .name("is-odd")
                .oldVersion("3.0.0")
                .newVersion(null)
                .scope("dependencies")
                .oldConstraint("3.0.0")
                .importerDir(null)
                .build();
        String out = new NpmLockPatcher().patch(editSet("remove-orphan", remove));
        assertThat(out).isEqualTo(golden("remove-orphan/after"));
    }

    @Test
    void v1LockfileVersionFailsLoud() {
        LockEditSet set = new LockEditSet(
                "{\n  \"lockfileVersion\": 1,\n  \"dependencies\": {}\n}\n",
                Paths.get("package-lock.json"), PackageManager.Npm,
                "{\"dependencies\":{\"is-odd\":\"3.0.1\"}}", singletonList(oddBump(null)));
        assertThatThrownBy(() -> new NpmLockPatcher().patch(set))
                .isInstanceOfSatisfying(EngineFailure.class,
                        ef -> assertThat(ef.failure.getReason()).isEqualTo(Reason.UNSUPPORTED_LOCKFILE_VERSION));
    }

    @Test
    void linkEntryFailsLoud() {
        String lock = "{\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\"name\": \"x\", \"dependencies\": {\"foo\": \"1.0.0\"}},\n" +
                "    \"node_modules/foo\": {\"resolved\": \"packages/foo\", \"link\": true}\n" +
                "  }\n" +
                "}\n";
        PackageEdit edit = PackageEdit.builder()
                .name("foo").oldVersion("1.0.0").newVersion("1.0.1")
                .newResolved("https://registry.npmjs.org/foo/-/foo-1.0.1.tgz")
                .newIntegrity("sha512-x")
                .scope("dependencies").oldConstraint("1.0.0").importerDir(null)
                .build();
        LockEditSet set = new LockEditSet(lock, Paths.get("package-lock.json"), PackageManager.Npm,
                "{\"dependencies\":{\"foo\":\"1.0.1\"}}", singletonList(edit));
        assertThatThrownBy(() -> new NpmLockPatcher().patch(set))
                .isInstanceOfSatisfying(EngineFailure.class,
                        ef -> assertThat(ef.failure.getReason()).isEqualTo(Reason.UNSUPPORTED_ENTRY_TYPE));
    }

    @Test
    void nonObjectRootFailsLoud() {
        LockEditSet set = new LockEditSet("[]\n", Paths.get("package-lock.json"), PackageManager.Npm,
                "{\"dependencies\":{\"is-odd\":\"3.0.1\"}}", singletonList(oddBump(null)));
        assertThatThrownBy(() -> new NpmLockPatcher().patch(set))
                .isInstanceOfSatisfying(EngineFailure.class,
                        ef -> assertThat(ef.failure.getReason()).isEqualTo(Reason.MALFORMED_LOCK));
    }
}
