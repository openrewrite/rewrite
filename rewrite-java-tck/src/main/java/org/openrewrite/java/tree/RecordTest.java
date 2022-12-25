/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.MinimumJava17;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@MinimumJava17
public class RecordTest implements RewriteTest {

    @Test
    void javaRecord() {
        rewriteRun(
          java(
            """
              public record JavaRecord(String name, @Deprecated int age) {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2455")
    @Test
    void compactConstructor() {
        rewriteRun(
          java(
            """
              public record JavaRecord(String name, @Deprecated int age) {
                  public JavaRecord {
                      java.util.Objects.requireNonNull(name);
                  }
              }
              """
          )
        );
    }
}
