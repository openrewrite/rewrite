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
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatchApplyTest {
    @Test
    void findsShiftedHunksAndPreservesContextWhitespace() {
        ZipArchive archive = archive("prefix\nfirst \t\nold\nlast\n");
        PatchApply.apply(PatchParse.parsePatchFile("""
          diff --git a/file b/file
          --- a/file
          +++ b/file
          @@ -1,3 +1,4 @@
           first
          -old
          +new
          +extra
           last
          """), archive, "root/", "1.0.0");
        assertThat(text(archive)).isEqualTo("prefix\nfirst \t\nnew\nextra\nlast\n");
    }

    @Test
    void preservesTheNoNewlineMarker() {
        ZipArchive archive = archive("old");
        PatchApply.apply(PatchParse.parsePatchFile("""
          diff --git a/file b/file
          --- a/file
          +++ b/file
          @@ -1 +1 @@
          -old
          \\ No newline at end of file
          +new
          \\ No newline at end of file
          """), archive, "root/", "1.0.0");
        assertThat(text(archive)).isEqualTo("new");
    }

    @Test
    void missingFilesStayNativeErrorsAndDifferentTextIsAnUnmatchedHunk() {
        java.util.List<java.util.Map<String, Object>> effects = PatchParse.parsePatchFile("""
          diff --git a/file b/file
          --- a/file
          +++ b/file
          @@ -1 +1 @@
          -old
          +new
          """);
        assertThatThrownBy(() -> PatchApply.apply(effects, new ZipArchive(), "root/", "1.0.0"))
                .isInstanceOf(ReportError.class).hasMessageContaining("ENOENT");
        assertThatThrownBy(() -> PatchApply.apply(effects, archive("different"), "root/", "1.0.0"))
                .isInstanceOf(PatchApply.UnmatchedHunk.class);
    }

    @Test
    void aSymlinkTargetIsExplicitlyUnknownRatherThanChecksummedAsLinkText() {
        ZipArchive archive = new ZipArchive();
        archive.mkdirp("root");
        archive.symlink("target", "root/file");
        assertThatThrownBy(() -> PatchApply.apply(PatchParse.parsePatchFile("""
          diff --git a/file b/file
          --- a/file
          +++ b/file
          @@ -1 +1 @@
          -target
          +wrong
          """), archive, "root/", "1.0.0"))
                .isInstanceOf(org.openrewrite.javascript.internal.lock.CannotKnow.class)
                .hasMessageContaining("symbolic link");
    }

    private static ZipArchive archive(String text) {
        ZipArchive archive = new ZipArchive();
        archive.mkdirp("root");
        archive.writeFile("root/file", text.getBytes(StandardCharsets.UTF_8), 0644);
        return archive;
    }

    private static String text(ZipArchive archive) {
        return new String(archive.entry("root/file").data, StandardCharsets.UTF_8);
    }
}
