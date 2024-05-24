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

import org.openrewrite.jgit.lib.FileMode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.RewriteTest.toRecipe;

class InMemoryDiffEntryTest {
    private final Path filePath = Paths.get("com/netflix/MyJavaClass.java");

    private String ab(String which) {
        return which + "/" + filePath.toString().replace("\\", "/");
    }

    @Test
    void idempotent() {
        try (var diff = new InMemoryDiffEntry(
          Paths.get("com/netflix/MyJavaClass.java"),
          Paths.get("com/netflix/MyJavaClass.java"),
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
          Paths.get("com/netflix/MyJavaClass.java"),
          Paths.get("com/netflix/MyJavaClass.java"),
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
}
