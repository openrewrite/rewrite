/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.internal;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.FileAttributes;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.jgit.api.ApplyResult;
import org.openrewrite.jgit.api.Git;
import org.openrewrite.jgit.lib.FileMode;
import org.openrewrite.jgit.lib.Repository;
import org.openrewrite.jgit.util.FileUtils;
import org.openrewrite.marker.GitTreeEntry;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.quark.Quark;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.test.RewriteTest.toRecipe;

class InMemoryDiffEntryTest {
    private final Path filePath = Path.of("com/netflix/MyJavaClass.java");

    private String ab(String which) {
        return which + "/" + filePath.toString().replace("\\", "/");
    }

    @Test
    void idempotent() {
        try (var diff = new InMemoryDiffEntry(
          Path.of("com/netflix/MyJavaClass.java"),
          Path.of("com/netflix/MyJavaClass.java"),
          null,
          "public class A {}",
          "public class A {}",
          emptySet()
        )) {
            assertThat(diff.getDiff()).isEmpty();
        }
    }

    @Test
    void ignoreWhitespace() {
        try (var diff = new InMemoryDiffEntry(
          Path.of("com/netflix/MyJavaClass.java"),
          Path.of("com/netflix/MyJavaClass.java"),
          null,
          "public class A {} ",
          "public class A {}",
          emptySet()
        )) {
            var diffNoWs = diff.getDiff(true);
            assertThat(
              """
                diff --git a/com/netflix/MyJavaClass.java b/com/netflix/MyJavaClass.java
                index 9bfeb36..efd7fa3 100644
                --- a/com/netflix/MyJavaClass.java
                +++ b/com/netflix/MyJavaClass.java
                """.trim()
            ).isEqualTo(StringUtils.trimIndent(diffNoWs));
        }
    }

    @Test
    void singleLineChange() {
        try (var result = new InMemoryDiffEntry(
          filePath, filePath, null,
          """
            public void test() {
               logger.infof("some %s", 1);
            }
            """,
          """
            public void test() {
               logger.info("some {}", 1);
            }
            """,
          Set.of(toRecipe().withName("logger.Fix")))) {
            assertThat(result.getDiff()).isEqualTo(
              """
                diff --git %s %s
                index 3490cbf..5d64ae4 100644
                --- %s
                +++ %s
                @@ -1,3 +1,3 @@ logger.Fix
                 public void test() {
                -   logger.infof("some %%s", 1);
                +   logger.info("some {}", 1);
                 }
                """.formatted(ab("a"), ab("b"), ab("a"), ab("b"))
            );
        }
    }

    @Test
    void multipleChangesMoreThanThreeLinesApart() {
        try (var result = new InMemoryDiffEntry(
          filePath, filePath, null,
          """
            public void test() {
               logger.infof("some %s", 1);
               System.out.println("1");
               System.out.println("2");
               System.out.println("3");
               System.out.println("4");
               System.out.println("5");
               System.out.println("6");
               System.out.println("7");
               System.out.println("8");
               logger.infof("some %s", 2);
            }
            """,
          """
            public void test() {
               logger.info("some {}", 1);
               System.out.println("1");
               System.out.println("2");
               System.out.println("3");
               System.out.println("4");
               System.out.println("5");
               System.out.println("6");
               System.out.println("7");
               System.out.println("8");
               logger.info("some %s", 2);
            }
            """,
          new LinkedHashSet<>() {{
              add(toRecipe().withName("logger.Fix1"));
              add(toRecipe().withName("logger.Fix2"));
          }}
        )) {
            assertThat(result.getDiff()).isEqualTo(
              """
                diff --git %s %s
                index c17f051..bb2dfba 100644
                --- %s
                +++ %s
                @@ -1,5 +1,5 @@ logger.Fix1, logger.Fix2
                 public void test() {
                -   logger.infof("some %%s", 1);
                +   logger.info("some {}", 1);
                    System.out.println("1");
                    System.out.println("2");
                    System.out.println("3");
                @@ -8,5 +8,5 @@
                    System.out.println("6");
                    System.out.println("7");
                    System.out.println("8");
                -   logger.infof("some %%s", 2);
                +   logger.info("some %%s", 2);
                 }
                """.formatted(ab("a"), ab("b"), ab("a"), ab("b"))
            );
        }
    }

    @Test
    void addFile() {
        try (var result = new InMemoryDiffEntry(
          null, filePath, null,
          "",
          """
            public void test() {
               logger.info("Hello Fred");
            }
            """,
          Set.of(toRecipe().withName("logger.Fix1")))) {
            assertThat(result.getDiff()).isEqualTo(
              """
                diff --git a/com/netflix/MyJavaClass.java b/com/netflix/MyJavaClass.java
                new file mode 100644
                index 0000000..efeb105
                --- /dev/null
                +++ b/com/netflix/MyJavaClass.java
                @@ -0,0 +1,3 @@ logger.Fix1
                +public void test() {
                +   logger.info("Hello Fred");
                +}
                """
            );
        }
    }

    @Test
    void deleteFile() {
        try (var result = new InMemoryDiffEntry(
          filePath, null, null,
          """
            public void test() {
               logger.info("Hello Fred");
            }
            """,
          "",
          Set.of(toRecipe().withName("logger.Fix1")))) {
            assertThat(result.getDiff()).isEqualTo(
              """
                diff --git a/com/netflix/MyJavaClass.java b/com/netflix/MyJavaClass.java
                deleted file mode 100644
                index efeb105..0000000
                --- a/com/netflix/MyJavaClass.java
                +++ /dev/null
                @@ -1,3 +0,0 @@ logger.Fix1
                -public void test() {
                -   logger.info("Hello Fred");
                -}
                """
            );
        }
    }

    @Disabled("Does not work with CI due to jgit shadowJar")
    @Test
    void executableFile() {
        try (var result = new InMemoryDiffEntry(
          filePath, null, null,
          """
            public void test() {
               logger.info("Hello Fred");
            }
            """,
          "",
          Set.of(toRecipe().withName("logger.Fix1")), FileMode.EXECUTABLE_FILE, FileMode.EXECUTABLE_FILE)) {
            assertThat(result.getDiff()).isEqualTo(
              """
                diff --git a/com/netflix/MyJavaClass.java b/com/netflix/MyJavaClass.java
                deleted file mode 100755
                index efeb105..0000000
                --- a/com/netflix/MyJavaClass.java
                +++ /dev/null
                @@ -1,3 +0,0 @@ logger.Fix1
                -public void test() {
                -   logger.info("Hello Fred");
                -}
                """
            );
        }
    }

    @Test
    void addBinary() {
        PlainText after = (PlainText) PlainTextParser.builder().build().parse("Hello, jon!").findFirst().get();
        after = after.withSourcePath(Paths.get("file.txt"))
          .withMarkers(after.getMarkers().add(new GitTreeEntry(randomId(), "0000000000000000000000000000000000000001", 0100644)));

        try (var entry = new InMemoryDiffEntry(null, after, null, null, Set.of(), true)) {
            assertThat(entry.getDiff()).isEqualTo("""
              diff --git a/file.txt b/file.txt
              new file mode 100644
              index 0000000000000000000000000000000000000000..06085efc592f6851a3f54f502f1a270db233ebf0
              GIT binary patch
              literal 11
              ScmeZB&B@8vQOL^AQv?7O<O8Vy
              
              literal 0
              HcmV?d00001
              
              
              """);
        }
    }

    @Test
    void deleteBinary() {
        PlainText before = (PlainText) PlainTextParser.builder().build().parse("Hello, jon!").findFirst().get();
        before = before.withSourcePath(Paths.get("file.txt"))
          .withMarkers(before.getMarkers().add(new GitTreeEntry(randomId(), "0000000000000000000000000000000000000001", 0100644)));

        try (var entry = new InMemoryDiffEntry(before, null, null, null, Set.of(), true)) {
            assertThat(entry.getDiff()).isEqualTo("""
              diff --git a/file.txt b/file.txt
              deleted file mode 100644
              index 0000000000000000000000000000000000000001..0000000000000000000000000000000000000000
              GIT binary patch
              literal 0
              HcmV?d00001
              
              literal 11
              ScmeZB&B@8vQOL^AQv?7O<O8Vy
              
              
              """);
        }
    }

    @Test
    void renameBinary() {
        PlainText before = (PlainText) PlainTextParser.builder().build().parse("Hello, jon!").findFirst().get();
        before = before.withSourcePath(Paths.get("file.txt"))
          .withMarkers(before.getMarkers().add(new GitTreeEntry(randomId(), "06085efc592f6851a3f54f502f1a270db233ebf0", 0100644)));
        PlainText after = before.withSourcePath(Paths.get("renamed.txt"));

        try (var entry = new InMemoryDiffEntry(before, after, null, null, Set.of(), true)) {
            assertThat(entry.getDiff()).isEqualTo("""
              diff --git a/file.txt b/renamed.txt
              similarity index 0%
              rename from file.txt
              rename to renamed.txt
              """);
        }
    }

    @Test
    void modifyBinary() {
        PlainText before = (PlainText) PlainTextParser.builder().build().parse("Hello, jon!\n").findFirst().get();
        before = before.withSourcePath(Paths.get("file.txt"))
          .withMarkers(before.getMarkers().add(new GitTreeEntry(randomId(), "0000000000000000000000000000000000000001", 0100644)));
        PlainText after = before.withText("Hello, jon.bak!\n");

        try (var entry = new InMemoryDiffEntry(before, after, null, null, Set.of(), true)) {
            assertThat(entry.getDiff()).isEqualTo("""
              diff --git a/file.txt b/file.txt
              index 0000000000000000000000000000000000000001..88292b7f3bb636b57b68867e3457ad3bcb85eb28 100644
              GIT binary patch
              literal 16
              XcmeZB&B@8vQOL^A(@RRsR^$QzE_wwI
              
              literal 12
              TcmeZB&B@8vQOL^AQ{(~w8kYmJ
              
              
              """);
        }
    }

    @Test
    void breakModifyBinary() {
        PlainText before = (PlainText) PlainTextParser.builder().build().parse("Hello, jon!\n").findFirst().get();
        before = before.withSourcePath(Paths.get("file.txt"))
          .withMarkers(before.getMarkers().add(new GitTreeEntry(randomId(), "0000000000000000000000000000000000000001", 0100644)));
        PlainText after = before.withSourcePath(Paths.get("renamed.txt"))
          .withText("Hello, jon.bak!\n");

        try (var entry = new InMemoryDiffEntry(before, after, null, null, Set.of(), true)) {
            assertThat(entry.getDiff()).isEqualTo("""
              diff --git a/file.txt b/renamed.txt
              similarity index 0%
              rename from file.txt
              rename to renamed.txt
              index 0000000000000000000000000000000000000001..88292b7f3bb636b57b68867e3457ad3bcb85eb28 100644
              GIT binary patch
              literal 16
              XcmeZB&B@8vQOL^A(@RRsR^$QzE_wwI
              
              literal 12
              TcmeZB&B@8vQOL^AQ{(~w8kYmJ
              
              
              """);
        }
    }

    @Test
    void quarkBinary() {
        Quark before = new Quark(randomId(), Paths.get("file.txt"), Markers.build(singletonList(new GitTreeEntry(randomId(), "0000000000000000000000000000000000000001", 0100644))), null, new FileAttributes(null, null, null, true, true, false, 0));
        PlainText after = PlainTextParser.builder().build().parse("Hello, jon!\n").findFirst().get()
          .withSourcePath(Paths.get("file.txt"))
          .withMarkers(before.getMarkers());

        try (var entry = new InMemoryDiffEntry(before, after, null, null, Set.of(), true)) {
            assertThat(entry.getDiff()).isEqualTo("""
              diff --git a/file.txt b/file.txt
              index 0000000000000000000000000000000000000001..cb9108edf7f482e8a7249097fc986c15e4fec69f 100644
              GIT binary patch
              literal 12
              TcmeZB&B@8vQOL^AQ{(~w8kYmJ
              
              literal 0
              HcmV?d00001
              
              
              """);
        }
    }

    @Test
    void unicodeDiff() {
        // Test that getDiff() correctly handles unicode content by using
        // the file's charset for decoding, not the platform default charset.
        String before = """
                public void test() {
                    String content = "ユーザー: \\"佐藤\\" — emoji: \uD83C\uDF89 — accented: café";
                    assertNotNull("Should generate UUID for unicode content", id);
                    assertEquals("UUID should be 36 characters", 36, id.length());
                }
                """;
        String after = """
                public void test() {
                    String content = "ユーザー: \\"佐藤\\" — emoji: \uD83C\uDF89 — accented: café";
                    assertNotNull(id, "Should generate UUID for unicode content");
                    assertEquals(36, id.length(), "UUID should be 36 characters");
                }
                """;

        try (InMemoryDiffEntry entry = new InMemoryDiffEntry(
                Path.of("Test.java"), Path.of("Test.java"), null,
                before, after, emptySet())) {
            String diff = entry.getDiff();
            assertThat(diff).isNotEmpty();
            // Verify the unicode context line is preserved in the diff
            assertThat(diff).contains("ユーザー");
            assertThat(diff).contains("🎉");
            assertThat(diff).contains("café");
            // Verify the changes are captured
            assertThat(diff).contains("-    assertNotNull(\"Should generate UUID for unicode content\", id);");
            assertThat(diff).contains("+    assertNotNull(id, \"Should generate UUID for unicode content\");");
        }
    }

    @Test
    void fencedMarkerPrinterIsApplied() {
        PlainText before = PlainTextParser.builder().build().parse("Hello").findFirst().get()
          .withSourcePath(Paths.get("file.txt"));

        SearchResult searchResult = new SearchResult(randomId(), null);
        PlainText after = before.withText("Hello World")
          .withMarkers(before.getMarkers().add(searchResult));

        try (var entry = new InMemoryDiffEntry(before, after, null, PrintOutputCapture.MarkerPrinter.FENCED, Set.of(), false)) {
            String diff = entry.getDiff();
            String expectedMarker = "{{" + searchResult.getId() + "}}";
            assertThat(diff).contains(expectedMarker);
            assertThat(diff).doesNotContain("~~>");
        }
    }

    @Test
    void fencedSearchResultOnFileWithTrailingNewline() {
        // Files with trailing newlines should not produce "\ No newline at end of file"
        // when fenced markers are applied. The closing fence must be placed before the
        // trailing newline, not after it.
        PlainText before = PlainTextParser.builder().build()
          .parse("line1\nline2\nline3\n").findFirst().get()
          .withSourcePath(Paths.get("file.txt"));

        SearchResult searchResult = new SearchResult(randomId(), null);
        PlainText after = before.withMarkers(before.getMarkers().add(searchResult));

        try (var entry = new InMemoryDiffEntry(before, after, null, PrintOutputCapture.MarkerPrinter.FENCED, Set.of(), false)) {
            String diff = entry.getDiff();
            assertThat(diff).doesNotContain("No newline at end of file");
            // The fenced markers should be present
            String expectedMarker = "{{" + searchResult.getId() + "}}";
            assertThat(diff).contains(expectedMarker);
        }
    }

    @Test
    void fencedSearchResultOnFileWithoutTrailingNewline() {
        // Files without trailing newlines should still produce "\ No newline at end of file"
        // on both sides of the diff, since neither before nor after ends with a newline.
        PlainText before = PlainTextParser.builder().build()
          .parse("line1\nline2").findFirst().get()
          .withSourcePath(Paths.get("file.txt"));

        SearchResult searchResult = new SearchResult(randomId(), null);
        PlainText after = before.withMarkers(before.getMarkers().add(searchResult));

        try (var entry = new InMemoryDiffEntry(before, after, null, PrintOutputCapture.MarkerPrinter.FENCED, Set.of(), false)) {
            String diff = entry.getDiff();
            String expectedMarker = "{{" + searchResult.getId() + "}}";
            assertThat(diff).contains(expectedMarker);
            assertThat(diff).contains("No newline at end of file");
        }
    }

    @Test
    void fencedSearchResultOnFileWithWindowsLineEndings() {
        // Windows-style line endings (\r\n) should be handled the same way —
        // the closing fence goes before the trailing \r\n.
        PlainText before = PlainTextParser.builder().build()
          .parse("line1\r\nline2\r\n").findFirst().get()
          .withSourcePath(Paths.get("file.txt"));

        SearchResult searchResult = new SearchResult(randomId(), null);
        PlainText after = before.withMarkers(before.getMarkers().add(searchResult));

        try (var entry = new InMemoryDiffEntry(before, after, null, PrintOutputCapture.MarkerPrinter.FENCED, Set.of(), false)) {
            String diff = entry.getDiff();
            assertThat(diff).doesNotContain("No newline at end of file");
            String expectedMarker = "{{" + searchResult.getId() + "}}";
            assertThat(diff).contains(expectedMarker);
        }
    }

    @Test
    void unicodeDiffCanBeAppliedByJgit() throws Exception {
        // Simulate the exact pipeline: InMemoryDiffEntry generates diff,
        // diff is converted to bytes, jgit ApplyCommand applies it to the file on disk.
        // This is the pipeline that fails for customer issue #1994.

        StringBuilder before = new StringBuilder();
        StringBuilder after = new StringBuilder();
        for (int i = 1; i <= 210; i++) {
            if (i == 203) {
                String line = "        String content = \"ユーザー: \\\"佐藤\\\" — emoji: \uD83C\uDF89 — accented: café\";\n";
                before.append(line);
                after.append(line);
            } else if (i == 204) {
                String line = "        String id = ContentIdentifierUtil.generate(content, util.getCorpBondNamespace());\n";
                before.append(line);
                after.append(line);
            } else if (i == 205) {
                before.append("        assertNotNull(\"Should generate UUID for unicode content\", id);\n");
                after.append("        assertNotNull(id, \"Should generate UUID for unicode content\");\n");
            } else if (i == 206) {
                before.append("        assertEquals(\"UUID should be 36 characters\", 36, id.length());\n");
                after.append("        assertEquals(36, id.length(), \"UUID should be 36 characters\");\n");
            } else {
                String line = "        line " + i + ";\n";
                before.append(line);
                after.append(line);
            }
        }

        String beforeStr = before.toString();
        String afterStr = after.toString();

        // Step 1: Generate diff using InMemoryDiffEntry (like the worker does)
        String diff;
        try (InMemoryDiffEntry entry = new InMemoryDiffEntry(
                Path.of("Test.java"), Path.of("Test.java"), null,
                beforeStr, afterStr, emptySet())) {
            diff = entry.getDiff();
        }
        assertThat(diff).isNotEmpty();
        assertThat(diff).contains("ユーザー");
        assertThat(diff).contains("🎉");

        // Step 2: Simulate SCM service applying the diff
        // Create a temp git repo with the original file
        File trash = Files.createTempDirectory("unicode-diff-test").toFile();
        try {
            Git git = Git.init().setDirectory(trash).call();
            Repository db = git.getRepository();

            // Write the original file
            File testFile = new File(trash, "Test.java");
            try (FileOutputStream fos = new FileOutputStream(testFile)) {
                fos.write(beforeStr.getBytes(StandardCharsets.UTF_8));
            }
            git.add().addFilepattern("Test.java").call();
            git.commit().setMessage("initial").call();

            // Apply the diff (simulating GitClient.applyPatch)
            byte[] diffBytes = diff.getBytes(StandardCharsets.UTF_8);
            ApplyResult result = git.apply()
                    .setPatch(new ByteArrayInputStream(diffBytes))
                    .call();

            assertThat(result.getUpdatedFiles()).isNotEmpty();

            // Verify the result
            String resultContent = new String(Files.readAllBytes(testFile.toPath()), StandardCharsets.UTF_8);
            assertThat(resultContent).contains("assertNotNull(id, \"Should generate UUID for unicode content\")");
            assertThat(resultContent).contains("assertEquals(36, id.length(), \"UUID should be 36 characters\")");
            assertThat(resultContent).contains("ユーザー");
            assertThat(resultContent).contains("🎉");
            assertThat(resultContent).contains("café");

            db.close();
        } finally {
            FileUtils.delete(trash, FileUtils.RECURSIVE | FileUtils.RETRY);
        }
    }

    @Test
    void fencedSearchResultPreservesLineCount() {
        // A search-only result (text unchanged, only markers added) on a file with a
        // trailing newline should not change the line count in the hunk header.
        PlainText before = PlainTextParser.builder().build()
          .parse("line1\nline2\nline3\n").findFirst().get()
          .withSourcePath(Paths.get("file.txt"));

        SearchResult searchResult = new SearchResult(randomId(), null);
        PlainText after = before.withMarkers(before.getMarkers().add(searchResult));

        try (var entry = new InMemoryDiffEntry(before, after, null, PrintOutputCapture.MarkerPrinter.FENCED, Set.of(), false)) {
            String diff = entry.getDiff();
            // Before has 3 lines, after should also have 3 lines (not 4)
            assertThat(diff).contains("@@ -1,3 +1,3 @@");
        }
    }
}
