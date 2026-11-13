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
package org.openrewrite.javascript.internal.lock.berry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Yarn's packages/yarnpkg-core/tests/Manifest.test.ts (the publishConfig cases aside: that field never reaches a
 * lock and is not ported), and the fields a lockfile entry and a registry document give.
 */
class ManifestTest {

    @Test
    void shouldHandleByteOrderMarkCharacter() {
        assertThat(Manifest.fromText("\uFEFF{\"name\":\"foo\"}").name.name).isEqualTo("foo");
    }

    @Test
    void shouldHandleWorkspacesSetToNull() {
        assertThatCode(() -> Manifest.fromText("{\"workspaces\":null}")).doesNotThrowAnyException();
    }

    @Test
    void shouldAddAScriptsFieldIfAScriptWasNewlyAdded() {
        Manifest manifest = Manifest.fromText("{}");
        manifest.scripts.put("foo", "bar");
        assertThat(manifest.exportTo(true).get("scripts")).isEqualTo(Collections.singletonMap("foo", "bar"));
    }

    @Test
    void shouldRemoveTheScriptsFieldIfTheLastScriptWasDeleted() {
        Manifest manifest = Manifest.fromText("{ \"scripts\": { \"foo\": \"bar\" } }");
        manifest.scripts.remove("foo");
        assertThat(manifest.exportTo(true)).doesNotContainKey("scripts");
    }

    @Test
    void shouldRespectChangesToScriptsPreservingExistingOrder() {
        Manifest manifest = Manifest.fromText("{ \"scripts\": { \"zzz-first\": \"first\", \"aaa-last\": \"last\" } }");
        manifest.scripts.put("zzz-first", "changed first");
        manifest.scripts.remove("aaa-last");
        manifest.scripts.put("start", "node index.js");
        @SuppressWarnings("unchecked") Map<String, Object> scripts =
                (Map<String, Object>) manifest.exportTo(true).get("scripts");
        assertThat(new ArrayList<>(scripts.entrySet()).toString())
                .isEqualTo("[zzz-first=changed first, start=node index.js]");
    }

    @Test
    void shouldPreserveBinIfAStringWasSpecified() {
        assertThat(Manifest.fromText("{ \"name\": \"name\", \"bin\": \"./bin.js\" }").exportTo(true).get("bin"))
                .isEqualTo("./bin.js");
    }

    @Test
    void shouldPreserveBinMapIfAHashWasSpecified() {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("bin1", "./bin1.js");
        expected.put("bin2", "./bin2.js");
        assertThat(Manifest.fromText("{ \"name\": \"name\", \"bin\": { \"bin1\": \"./bin1.js\", " +
                                     "\"bin2\": \"./bin2.js\" } }").exportTo(true).get("bin")).isEqualTo(expected);
    }

    @Test
    void shouldRemoveBinIfAnEmptyPathWasSpecified() {
        assertThat(Manifest.fromText("{ \"name\": \"name\", \"bin\": \"\" }").exportTo(true)).doesNotContainKey("bin");
    }

    @Test
    void shouldRemoveEntriesFromBinMapIfAnEmptyPathWasSpecified() {
        assertThat(Manifest.fromText("{ \"name\": \"name\", \"bin\": { \"bin1\": \" \", \"bin2\": \"./bin2.js\" } }")
                .exportTo(true).get("bin")).isEqualTo(Collections.singletonMap("bin2", "./bin2.js"));
    }

    @Test
    void conditionsJoinListsAndKeepNegation() {
        Manifest manifest = Manifest.fromText("{\"os\": [\"darwin\", \"linux\"], \"cpu\": [\"x64\", \"arm64\"]}");
        assertThat(manifest.getConditions()).isEqualTo("(os=darwin | os=linux) & (cpu=x64 | cpu=arm64)");
        assertThat(Manifest.fromText("{\"os\": [\"!win32\"], \"libc\": [\"glibc\"]}").getConditions())
                .isEqualTo("!os=win32 & libc=glibc");
        assertThat(Manifest.fromText("{\"os\": \"linux\"}").getConditions()).isNull();
    }

    @Test
    void anInvalidPeerRangeIsAStar() {
        Manifest manifest = Manifest.fromText("{\"peerDependencies\": {\"a\": \"not a range\", \"b\": \"^1.0.0\"}}");
        assertThat(manifest.exportTo(false).get("peerDependencies").toString()).isEqualTo("{a=*, b=^1.0.0}");
    }

    @Test
    void optionalDependenciesJoinTheDependenciesWithAnOptionalMeta() {
        Manifest manifest = Manifest.fromText("{\"dependencies\": {\"a\": \"1\"}, " +
                                              "\"optionalDependencies\": {\"b\": \"2\"}}");
        Map<String, Object> exported = manifest.exportTo(false);
        assertThat(exported.get("dependencies").toString()).isEqualTo("{a=1, b=2}");
        assertThat(exported.get("dependenciesMeta").toString()).isEqualTo("{b={optional=true}}");
    }

    @Test
    void aLockfileSpellsBooleansAsStrings() {
        Manifest manifest = new Manifest();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("optional", "true");
        manifest.load(Collections.singletonMap("peerDependenciesMeta", Collections.singletonMap("a", meta)), true);
        assertThat(manifest.peerDependenciesMeta.get("a").get("optional")).isEqualTo(true);
        manifest.load(Collections.singletonMap("peerDependenciesMeta", Collections.singletonMap("a", meta)), false);
        assertThat(manifest.peerDependenciesMeta.get("a")).isEmpty();
    }
}
