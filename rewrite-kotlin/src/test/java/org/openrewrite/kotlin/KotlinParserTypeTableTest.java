/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

/**
 * Verifies that TypeTable-sourced classpath entries work with the Kotlin K2 compiler.
 */
class KotlinParserTypeTableTest implements RewriteTest {

    @Test
    void typeTableClasspathWithCoroutinesViaClasspathFromResources() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder()
            .classpathFromResources(new InMemoryExecutionContext(), "kotlinx-coroutines-core-jvm")),
          kotlin(
            """
              import kotlinx.coroutines.channels.ReceiveChannel

              fun <T> pollChannel(channel: ReceiveChannel<T>): T? {
                  @Suppress("DEPRECATION")
                  return channel.poll()
              }
              """
          )
        );
    }
}
