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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

/**
 * pnpm 11.2.2's pkg-manifest/utils/test/convertEnginesRuntimeToDependencies.test.ts, and the runtimeOnFail override
 * as small-runtime-on-fail-ignore records it.
 */
class PkgManifestUtilsTest {

    @Test
    void convertEnginesRuntimeToDependenciesSkipsRuntimeEntriesWithoutAVersion() {
        Map<String, @Nullable Object> manifest = obj("{'devEngines':{'runtime':{'name':'node','onFail':'download'}}}");
        PkgManifestUtils.convertEnginesRuntimeToDependencies(manifest, "devEngines", "devDependencies");
        assertThat(manifest).doesNotContainKey("devDependencies");
    }

    @Test
    void applyRuntimeOnFailOverrideDownloadSkipsRuntimeEntriesWithoutAVersion() {
        Map<String, @Nullable Object> manifest = obj("{'devEngines':{'runtime':{'name':'node'}}}");
        PkgManifestUtils.applyRuntimeOnFailOverride(manifest, "download");
        assertThat(json(manifest)).isEqualTo("{'devEngines':{'runtime':{'name':'node','onFail':'download'}}}");
    }

    @Test
    void anOverrideOtherThanDownloadDropsTheRuntimeDependency() {
        Map<String, @Nullable Object> manifest = obj("{'dependencies':{'wrappy':'1.0.2'},'devDependencies':{" +
                                                     "'node':'runtime:22.11.0'},'devEngines':{'runtime':{" +
                                                     "'name':'node','version':'22.11.0','onFail':'download'}}}");
        PkgManifestUtils.applyRuntimeOnFailOverride(manifest, "ignore");
        assertThat(json(manifest)).isEqualTo("{'dependencies':{'wrappy':'1.0.2'},'devDependencies':{}," +
                                             "'devEngines':{'runtime':{'name':'node','version':'22.11.0'," +
                                             "'onFail':'ignore'}}}");
    }

    @Test
    void onlyADownloadRuntimeIsConverted() {
        Map<String, @Nullable Object> manifest = obj("{'engines':{'runtime':[{'name':'deno','version':'2'," +
                                                     "'onFail':'warn'},{'name':'bun','version':'1.2.0'," +
                                                     "'onFail':'download'}]}}");
        PkgManifestUtils.convertEnginesRuntimeToDependencies(manifest, "engines", "dependencies");
        assertThat(json(manifest.get("dependencies"))).isEqualTo("{'bun':'runtime:1.2.0'}");
    }

    @Test
    void aDeclaredDependencyOfTheRuntimesNameWins() {
        Map<String, @Nullable Object> manifest = obj("{'devDependencies':{'node':'^22'},'devEngines':{'runtime':{" +
                                                     "'name':'node','version':'22.11.0','onFail':'download'}}}");
        PkgManifestUtils.convertEnginesRuntimeToDependencies(manifest, "devEngines", "devDependencies");
        assertThat(json(manifest.get("devDependencies"))).isEqualTo("{'node':'^22'}");
    }

    /**
     * A null runtime entry, and a string one under runtimeOnFail, as pnpm 11.2.2 failed on both on 2026-10-08.
     */
    @Test
    void runtimeEntriesPnpmCannotReadAreJavaScriptsTypeErrors() {
        assertThatThrownBy(() -> PkgManifestUtils.convertEnginesRuntimeToDependencies(
          obj("{'devEngines':{'runtime':[null]}}"), "devEngines", "devDependencies"))
          .isInstanceOf(JsError.class)
          .hasMessage("Cannot read properties of null (reading 'name')");
        assertThatThrownBy(() -> PkgManifestUtils.applyRuntimeOnFailOverride(obj("{'devEngines':{'runtime':'node'}}"),
          "ignore"))
          .isInstanceOf(JsError.class)
          .hasMessage("Cannot create property 'onFail' on string 'node'");
    }
}
