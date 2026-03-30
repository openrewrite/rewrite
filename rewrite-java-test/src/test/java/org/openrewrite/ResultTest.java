/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.DeserializationError;
import org.openrewrite.marker.Markers;
import org.openrewrite.quark.Quark;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;

import java.nio.file.Paths;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;

class ResultTest implements RewriteTest {

    @Test
    void noChangesNewList() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      System.out.println("Hello, world!");
                  }
              }
              """,
            spec -> spec.afterRecipe(before -> {
                var after = (J.CompilationUnit) new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.Block visitBlock(J.Block block, Integer p) {
                        // intentional inappropriate creation of a new list
                        List<Statement> statements = ListUtils.concat(block.getStatements(),
                          new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY));
                        return block
                          .withStatements(statements)
                          .withStatements(ListUtils.map(statements, (n, s) -> n == 0 ? s : null));
                    }
                }.visitNonNull(before, 0);

                var unknownSourceFileChangeException = assertThrows(UnknownSourceFileChangeException.class, () -> new Result(before, after));
                assertThat(unknownSourceFileChangeException.getDiff()).contains("+class Test /*~~>*/{");
            })
          )
        );
    }

    @Test
    void deserializationError() {
        PlainText before = PlainText.builder().sourcePath(Paths.get("foo.txt")).text("foo").build();
        new Result(
          before,
          new Quark(
            randomId(),
            before.getSourcePath(),
            Markers.build(singletonList(new DeserializationError(randomId(), "Foo", new RuntimeException()))),
            before.getChecksum(),
            before.getFileAttributes()
          )
        );
    }
}
