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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Yarn's packages/plugin-patch/tests/parsePatchFile.test.ts, each result as {@code JSON.stringify} of what Yarn
 * 4.10.3's parse.ts returns for it.
 */
class PatchParseTest {

    private static final String PATCH = "diff --git a/banana.ts b/banana.ts\n" +
                                        "index 2de83dd..842652c 100644\n" +
                                        "--- a/banana.ts\n" +
                                        "+++ b/banana.ts\n" +
                                        "@@ -1,5 +1,5 @@\n" +
                                        " this\n" +
                                        " is\n" +
                                        "\n" +
                                        "-a\n" +
                                        "+\n" +
                                        " file\n";

    // the same text as PATCH in Yarn's test
    private static final String ACCIDENTAL_BLANK_LINE = PATCH;

    private static final String PARSED = "[{\"type\":\"patch\",\"semverExclusivity\":null,\"path\":\"banana.ts\"," +
                                         "\"hunks\":[{\"header\":{\"original\":{\"start\":1,\"length\":5}," +
                                         "\"patched\":{\"start\":1,\"length\":5}},\"parts\":[{\"type\":\"context\"," +
                                         "\"lines\":[\"this\",\"is\",\"\"],\"noNewlineAtEndOfFile\":false}," +
                                         "{\"type\":\"deletion\",\"lines\":[\"a\"],\"noNewlineAtEndOfFile\":false}," +
                                         "{\"type\":\"insertion\",\"lines\":[\"\"],\"noNewlineAtEndOfFile\":false}," +
                                         "{\"type\":\"context\",\"lines\":[\"file\"]," +
                                         "\"noNewlineAtEndOfFile\":false}]}],\"beforeHash\":\"2de83dd\"," +
                                         "\"afterHash\":\"842652c\"}]";

    private static String withHeader(String header) {
        return PATCH.replace("@@ -1,5 +1,5 @@", header);
    }

    @Test
    void worksForASimpleCase() {
        assertThat(Js.jsonStringify(PatchParse.parsePatchFile(PATCH))).isEqualTo(PARSED);
    }

    @Test
    void failsWhenThePatchFileHasInvalidHeaders() {
        assertThatThrownBy(() -> PatchParse.parsePatchFile(withHeader("@@ -1,5 +1,4 @@")))
                .hasMessage("hunk header integrity check failed (expected @@ +5 +4 @@, got @@ +5 +5 @@)");
        assertThatThrownBy(() -> PatchParse.parsePatchFile(withHeader("@@ -1,4 +1,5 @@")))
                .hasMessage("hunk header integrity check failed (expected @@ +4 +5 @@, got @@ +5 +5 @@)");
        assertThatThrownBy(() -> PatchParse.parsePatchFile(withHeader("@@ -1,0 +1,5 @@")))
                .hasMessage("hunk header integrity check failed (expected @@ +0 +5 @@, got @@ +5 +5 @@)");
        assertThatThrownBy(() -> PatchParse.parsePatchFile(withHeader("@@ -1,5 +1,0 @@")))
                .hasMessage("hunk header integrity check failed (expected @@ +5 +0 @@, got @@ +5 +5 @@)");
        assertThatThrownBy(() -> PatchParse.parsePatchFile(withHeader("@@ -1,5 +1,5@@")))
                .hasMessage("Bad header line: '@@ -1,5 +1,5@@'");
    }

    @Test
    void isOkWhenBlankLinesAreAccidentallyCreated() {
        assertThat(Js.jsonStringify(PatchParse.parsePatchFile(ACCIDENTAL_BLANK_LINE))).isEqualTo(PARSED);
    }

    @Test
    void canHandleFilesWithCrlfLineBreaks() {
        String crlfLineBreaks = ("diff --git a/banana.ts b/banana.ts\n" +
                                 "new file mode 100644\n" +
                                 "index 0000000..3e1267f\n" +
                                 "--- /dev/null\n" +
                                 "+++ b/banana.ts\n" +
                                 "@@ -0,0 +1 @@\n" +
                                 "+this is a new file\n").replace("\n", "\r\n");
        assertThat(Js.jsonStringify(PatchParse.parsePatchFile(crlfLineBreaks))).isEqualTo(
                "[{\"type\":\"file " +
                "creation\",\"semverExclusivity\":null,\"hunk\":{\"header\":{\"original\":{\"start\":1,\"length\":0}," +
                "\"patched\":{\"start\":1,\"length\":1}},\"parts\":[{\"type\":\"insertion\",\"lines\":[\"this is a " +
                "new " +
                "file\\r\"],\"noNewlineAtEndOfFile\":false}]},\"path\":\"banana.ts\",\"mode\":420," +
                "\"hash\":\"3e1267f\"}]");
    }

    @Test
    void works() {
        String modeChangeAndModifyAndRename = "diff --git a/numbers.txt b/banana.txt\n" +
                                              "old mode 100644\n" +
                                              "new mode 100755\n" +
                                              "similarity index 96%\n" +
                                              "rename from numbers.txt\n" +
                                              "rename to banana.txt\n" +
                                              "index fbf1785..92d2c5f\n" +
                                              "--- a/numbers.txt\n" +
                                              "+++ b/banana.txt\n" +
                                              "@@ -1,4 +1,4 @@\n" +
                                              "-one\n" +
                                              "+ne\n" +
                                              "\n" +
                                              " two\n" +
                                              "\n";
        assertThat(Js.jsonStringify(PatchParse.parsePatchFile(modeChangeAndModifyAndRename))).isEqualTo(
                "[{\"type\":\"rename\",\"semverExclusivity\":null,\"fromPath\":\"numbers.txt\"," +
                "\"toPath\":\"banana.txt\"},{\"type\":\"mode " +
                "change\",\"semverExclusivity\":null,\"path\":\"banana.txt\",\"oldMode\":420,\"newMode\":493}," +
                "{\"type\":\"patch\",\"semverExclusivity\":null,\"path\":\"banana.txt\"," +
                "\"hunks\":[{\"header\":{\"original\":{\"start\":1,\"length\":4},\"patched\":{\"start\":1," +
                "\"length\":4}},\"parts\":[{\"type\":\"deletion\",\"lines\":[\"one\"]," +
                "\"noNewlineAtEndOfFile\":false},{\"type\":\"insertion\",\"lines\":[\"ne\"]," +
                "\"noNewlineAtEndOfFile\":false},{\"type\":\"context\",\"lines\":[\"\",\"two\",\"\"]," +
                "\"noNewlineAtEndOfFile\":false}]}],\"beforeHash\":\"fbf1785\",\"afterHash\":\"92d2c5f\"}]");
    }
}
