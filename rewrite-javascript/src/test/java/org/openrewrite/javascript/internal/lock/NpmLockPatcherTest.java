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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.javascript.internal.lock.LockEditSet.EntryMetadata;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
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

    // --- leaf add (Phase B increment 1) ----------------------------------

    private static PackageEdit leafAdd(String scope) {
        return PackageEdit.builder()
                .name("left-pad")
                .oldVersion("")
                .newVersion("1.3.0")
                .newResolved("https://registry.npmjs.org/left-pad/-/left-pad-1.3.0.tgz")
                .newIntegrity("sha512-XI5MPzVNApjAyhQzphX8BkmKsKUxD4LdyK24iZeQGinBN9yTQT3bFlCBy/aVx2HrNcqQGsdot8ghrjyrvMCoEA==")
                .scope(scope)
                .importerDir(null)
                .kind(PackageEdit.Kind.ADD)
                .metadata(LockEditSet.EntryMetadata.builder()
                        .license("WTFPL")
                        .deprecated("use String.prototype.padStart()")
                        .dev("devDependencies".equals(scope) ? Boolean.TRUE : null)
                        .build())
                .build();
    }

    @Test
    void leafAddV3IsByteExact() {
        String out = new NpmLockPatcher().patch(editSet("add-leaf", leafAdd("dependencies")));
        assertThat(out).isEqualTo(golden("add-leaf/after"));
    }

    @Test
    void leafAddV2DualMapIsByteExact() {
        String out = new NpmLockPatcher().patch(editSet("add-leaf-v2", leafAdd("dependencies")));
        assertThat(out).isEqualTo(golden("add-leaf-v2/after"));
    }

    @Test
    void leafAddDevScopeIsByteExact() {
        String out = new NpmLockPatcher().patch(editSet("add-leaf-dev", leafAdd("devDependencies")));
        assertThat(out).isEqualTo(golden("add-leaf-dev/after"));
    }

    // --- object/array metadata leaf add (Phase B increment 1-follow) -----

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(String source) {
        try {
            return MAPPER.readTree(source);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static PackageEdit.PackageEditBuilder metaAdd(String name, String version, String resolved,
                                                          String integrity) {
        return PackageEdit.builder()
                .name(name)
                .oldVersion("")
                .newVersion(version)
                .newResolved(resolved)
                .newIntegrity(integrity)
                .scope("dependencies")
                .importerDir(null)
                .kind(PackageEdit.Kind.ADD);
    }

    @Test
    void enginesLeafAddIsByteExact() {
        PackageEdit add = metaAdd("is-number", "7.0.0",
                "https://registry.npmjs.org/is-number/-/is-number-7.0.0.tgz",
                "sha512-41Cifkg6e8TylSpdtTpeLVMqvSBEVzTttHvERD741+pnZ8ANv0004MRL43QKPDlK9cGvNp6NZWZUBlbGXYxxng==")
                .metadata(EntryMetadata.builder()
                        .license("MIT")
                        .engines(singletonMap("node", ">=0.12.0"))
                        .build())
                .build();
        assertThat(new NpmLockPatcher().patch(editSet("add-meta-engines", add)))
                .isEqualTo(golden("add-meta-engines/after"));
    }

    @Test
    void binObjectLeafAddIsByteExact() {
        PackageEdit add = metaAdd("he", "1.2.0",
                "https://registry.npmjs.org/he/-/he-1.2.0.tgz",
                "sha512-F/1DnUGPopORZi0ni+CvrCgHQ5FyEAHRLSApuYWMmrbSwoN2Mn/7k+Gl38gJnR7yyDZk6WLXwiGod1JOWNDKGw==")
                .metadata(EntryMetadata.builder()
                        .license("MIT")
                        .bin(json("{\"he\": \"bin/he\"}"))
                        .build())
                .build();
        assertThat(new NpmLockPatcher().patch(editSet("add-meta-bin", add)))
                .isEqualTo(golden("add-meta-bin/after"));
    }

    /** os (array, groups with scalars) + hasInstallScript + license before engines (object, groups last). */
    @Test
    void richMetadataLeafAddV3IsByteExact() {
        assertThat(new NpmLockPatcher().patch(editSet("add-meta-rich", fseventsAdd())))
                .isEqualTo(golden("add-meta-rich/after"));
    }

    @Test
    void richMetadataLeafAddV2LegacyTreeStaysMinimal() {
        assertThat(new NpmLockPatcher().patch(editSet("add-meta-rich-v2", fseventsAdd())))
                .isEqualTo(golden("add-meta-rich-v2/after"));
    }

    private static PackageEdit fseventsAdd() {
        return metaAdd("fsevents", "2.3.3",
                "https://registry.npmjs.org/fsevents/-/fsevents-2.3.3.tgz",
                "sha512-5xoDfX+fL7faATnagmWPpbFtwh/R77WmMMqqHGS65C3vvB0YHrgF+B1YmZ3441tMj5n63k0212XNoJwzlhffQw==")
                .metadata(EntryMetadata.builder()
                        .license("MIT")
                        .hasInstallScript(true)
                        .os(singletonList("darwin"))
                        .engines(singletonMap("node", "^8.16.0 || ^10.6.0 || >=11.0.0"))
                        .build())
                .build();
    }

    /** npm records a string {@code funding} as the normalized object {@code {url}}, sorted after engines. */
    @Test
    void fundingLeafAddIsByteExact() {
        PackageEdit add = metaAdd("escape-string-regexp", "5.0.0",
                "https://registry.npmjs.org/escape-string-regexp/-/escape-string-regexp-5.0.0.tgz",
                "sha512-/veY75JbMK4j1yjvuUxuVsiS/hr/4iHs9FTT6cgTexxdE0Ly/glccBAkloH/DofkjRbZU3bnoj38mOmhkZ0lHw==")
                .metadata(EntryMetadata.builder()
                        .license("MIT")
                        .engines(singletonMap("node", ">=12"))
                        .funding(json("{\"url\": \"https://github.com/sponsors/sindresorhus\"}"))
                        .build())
                .build();
        assertThat(new NpmLockPatcher().patch(editSet("add-meta-funding", add)))
                .isEqualTo(golden("add-meta-funding/after"));
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

    // --- entry flags -------------------------------------------------------

    private static String flagLock(String flagsSource) {
        return "{\n" +
                "  \"name\": \"t\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"requires\": true,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\n" +
                "      \"name\": \"t\",\n" +
                "      \"version\": \"1.0.0\",\n" +
                "      \"devDependencies\": {\n" +
                "        \"is-odd\": \"^3.0.1\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"node_modules/is-odd\": {\n" +
                "      \"version\": \"3.0.1\",\n" +
                "      \"resolved\": \"https://registry.npmjs.org/is-odd/-/is-odd-3.0.1.tgz\",\n" +
                "      \"integrity\": \"sha512-x\",\n" +
                flagsSource +
                "      \"license\": \"MIT\"\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
    }

    private static PackageEdit flagsOnlyBump(EntryMetadata metadata) {
        return PackageEdit.builder()
                .name("is-odd")
                .oldVersion("3.0.1")
                .newVersion("3.0.1")
                .scope("devDependencies")
                .importerDir(null)
                .metadata(metadata)
                .build();
    }

    @Test
    void flagsOnlyBumpAddsDevFlag() {
        LockEditSet set = new LockEditSet(flagLock(""), Paths.get("package-lock.json"), PackageManager.Npm,
                "{\"devDependencies\":{\"is-odd\":\"^3.0.1\"}}",
                singletonList(flagsOnlyBump(EntryMetadata.builder().flagsChanged(true).dev(true).build())));
        assertThat(new NpmLockPatcher().patch(set))
                .isEqualTo(flagLock("      \"dev\": true,\n"));
    }

    @Test
    void flagsOnlyBumpExactSetsFlagMembers() {
        // dev is cleared and optional written in one exact-set pass; untouched members keep their bytes.
        LockEditSet set = new LockEditSet(flagLock("      \"dev\": true,\n"),
                Paths.get("package-lock.json"), PackageManager.Npm,
                "{\"devDependencies\":{\"is-odd\":\"^3.0.1\"}}",
                singletonList(flagsOnlyBump(EntryMetadata.builder().flagsChanged(true).optional(true).build())));
        String out = new NpmLockPatcher().patch(set);
        assertThat(out).contains("\"integrity\": \"sha512-x\",\n      \"license\": \"MIT\",\n      \"optional\": true\n");
        assertThat(out).doesNotContain("\"dev\"");
    }

    @Test
    void nestedBumpRewritesOnlyTheNestedEntry() {
        String lock = "{\n" +
                "  \"name\": \"t\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"requires\": true,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\n" +
                "      \"name\": \"t\",\n" +
                "      \"version\": \"1.0.0\",\n" +
                "      \"dependencies\": {\n" +
                "        \"a\": \"^1.0.0\",\n" +
                "        \"b\": \"^2.0.0\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"node_modules/a\": {\n" +
                "      \"version\": \"1.0.0\",\n" +
                "      \"resolved\": \"https://registry.npmjs.org/a/-/a-1.0.0.tgz\",\n" +
                "      \"integrity\": \"sha512-a1\",\n" +
                "      \"dependencies\": {\n" +
                "        \"b\": \"^1.0.0\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"node_modules/a/node_modules/b\": {\n" +
                "      \"version\": \"1.0.0\",\n" +
                "      \"resolved\": \"https://registry.npmjs.org/b/-/b-1.0.0.tgz\",\n" +
                "      \"integrity\": \"sha512-b1\"\n" +
                "    },\n" +
                "    \"node_modules/b\": {\n" +
                "      \"version\": \"2.0.0\",\n" +
                "      \"resolved\": \"https://registry.npmjs.org/b/-/b-2.0.0.tgz\",\n" +
                "      \"integrity\": \"sha512-b2\"\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
        PackageEdit edit = PackageEdit.builder()
                .name("b")
                .oldVersion("1.0.0")
                .newVersion("1.0.1")
                .newResolved("https://registry.npmjs.org/b/-/b-1.0.1.tgz")
                .newIntegrity("sha512-b11")
                .scope("dependencies")
                .importerDir(null)
                .nestedUnder("a")
                .build();
        LockEditSet set = new LockEditSet(lock, Paths.get("package-lock.json"), PackageManager.Npm,
                "{\"dependencies\":{\"a\":\"^1.0.0\",\"b\":\"^2.0.0\"}}", singletonList(edit));
        assertThat(new NpmLockPatcher().patch(set)).isEqualTo(lock
                .replace("b-1.0.0.tgz", "b-1.0.1.tgz")
                .replace("\"version\": \"1.0.0\",\n      \"resolved\": \"https://registry.npmjs.org/b/",
                        "\"version\": \"1.0.1\",\n      \"resolved\": \"https://registry.npmjs.org/b/")
                .replace("sha512-b1\"", "sha512-b11\""));
    }

    @Test
    void promotionClearsDevInV2LegacyTree() {
        String lock = "{\n" +
                "  \"name\": \"t\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"lockfileVersion\": 2,\n" +
                "  \"requires\": true,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\n" +
                "      \"name\": \"t\",\n" +
                "      \"version\": \"1.0.0\",\n" +
                "      \"devDependencies\": {\n" +
                "        \"is-even\": \"^1.0.0\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"node_modules/is-even\": {\n" +
                "      \"version\": \"1.0.0\",\n" +
                "      \"resolved\": \"https://registry.npmjs.org/is-even/-/is-even-1.0.0.tgz\",\n" +
                "      \"integrity\": \"sha512-e\",\n" +
                "      \"dev\": true\n" +
                "    }\n" +
                "  },\n" +
                "  \"dependencies\": {\n" +
                "    \"is-even\": {\n" +
                "      \"version\": \"1.0.0\",\n" +
                "      \"resolved\": \"https://registry.npmjs.org/is-even/-/is-even-1.0.0.tgz\",\n" +
                "      \"integrity\": \"sha512-e\",\n" +
                "      \"dev\": true\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
        PackageEdit edit = PackageEdit.builder()
                .name("is-even")
                .oldVersion("1.0.0")
                .newVersion("1.0.0")
                .scope("dependencies")
                .importerDir(null)
                .kind(PackageEdit.Kind.PROMOTION)
                .metadata(EntryMetadata.builder().flagsChanged(true).build())
                .build();
        LockEditSet set = new LockEditSet(lock, Paths.get("package-lock.json"), PackageManager.Npm,
                "{\"dependencies\":{\"is-even\":\"^1.0.0\"}}", singletonList(edit));
        String out = new NpmLockPatcher().patch(set);
        // The importer edge is written into a fresh dependencies scope and both trees' dev flags clear.
        assertThat(out).contains("\"dependencies\": {\n        \"is-even\": \"^1.0.0\"\n      },\n      \"devDependencies\"");
        assertThat(out).doesNotContain("\"dev\": true");
    }
}
