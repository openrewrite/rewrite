/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java;


import static org.openrewrite.java.Assertions.java;

import java.util.stream.Stream;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.test.RewriteTest;

public class Java11ParserTest implements RewriteTest {

  @ParameterizedTest
  @MethodSource("lambdaParameterVariableDeclarationsParams")
  void lambdaParameterVariableDeclarations(@Language("java") String java) {
    rewriteRun(java(java));
  }

  /**
   * Produces a stream of test java sources like
   * `public class Foo { public static void main(String[] args){ java.util.function.Consumer<Object> i = (j) -> {}; System.out.println("Hello World!"); } }`
   */
  static Stream<String> lambdaParameterVariableDeclarationsParams() {
    return Stream.of(
        "(var j)",
        "(Object j)",
        "j",
        "(j)",
        "vari",
        "(vari)"
    ).map(exp ->
        String.format(
            "public class Foo { public static void main(String[] args){ java.util.function.Consumer<Object> i = %s -> {}; System.out.println(\"Hello World!\"); } }",
            exp)
    );
  }
}
