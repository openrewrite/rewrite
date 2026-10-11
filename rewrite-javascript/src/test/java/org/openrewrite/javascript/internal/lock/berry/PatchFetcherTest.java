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

import java.util.Base64;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class PatchFetcherTest {

    @Test
    void patchesASortedCopyOfTheSourceArchive() {
        // the tarball holds package.json, z.js, lib/b.js and a.js in that order
        // the hunk's header names a line above line4
        byte[] tgz = Base64.getDecoder().decode("H4sIAAAAAAAAE+2VwQ6CMAyGOfMUZmczOmAj4W2GWQyIA5l6wPjuhik" +
                "xQRMTA8NIv8PfZZf28rW13OzkVgX1vdLCVNobGQAAEce2AsCwAkTJ8939MyZC4a1g7EHecTJH2XjgpNcPciFa7hVJyYG" +
                "syVk1Jq80SQmjQIFc/bnHQybm4X3Q0sJM1eOj/xAO/AcWJei/C1pUfNH0/pd5FmQT7YAv/Beco/8uyND/RdP7L+e8//z" +
                "1/rMI/XdBmWvF/C5Dm5HN2Ca3KWwmuCcQBEH+ihuIWtWdABQAAA==");
        String patch = "diff --git a/a.js b/a.js\n--- a/a.js\n+++ b/a.js\n@@ -1,3 +1,3 @@\n line4\n-line5\n+LINE5\n" +
                       " line6\ndiff --git a/lib/new.js b/lib/new.js\nnew file mode 100644\n--- /dev/null\n" +
                       "+++ b/lib/new.js\n@@ -0,0 +1,1 @@\n+module.exports = 1;\n";
        Locator locator = StructUtils.makeLocator(StructUtils.makeIdent(null, "q"),
                "patch:q@file:./q-1.0.0.tgz#./q.patch");
        ZipArchive patched = PatchFetcher.patchPackage(locator, Cache.packageFs(tgz, locator),
                Collections.singletonList(new PatchUtils.PatchFile(patch, false)), "1.0.0", Configuration.defaults());
        // recorded with Yarn 4.10.3: the checksum of q@patch:q@file:./q-1.0.0.tgz#./q.patch
        assertThat(Cache.checksum(patched, locator, Configuration.defaults())).isEqualTo("10c0/3f30b8a7cc053e44bd" +
                "1e94da5c6f35265648b597bc05926ef96da7eb2bae400d63bccd50f3e1083820d3abe508a43568ba0c2b3fed5223eb91ac4" +
                "df89f3690eb");
    }

    @Test
    void hashesTheBuiltinPatchOfTheReleaseThatRuns() {
        // recorded: plugin-compat's resolve patch changed in 4.17.1
        assertThat(builtinPatchHash("4.10.3")).isEqualTo("c3c19d");
        assertThat(builtinPatchHash("4.18.1")).isEqualTo("9bd1a5");
    }

    private static String builtinPatchHash(String release) {
        String text = CompatPatches.getBuiltinPatch("compat/resolve", YarnRelease.of(release));
        return PatchUtils.makePatchHash(Collections.singletonList(new PatchUtils.PatchFile(text, true)), "1.22.13");
    }
}
