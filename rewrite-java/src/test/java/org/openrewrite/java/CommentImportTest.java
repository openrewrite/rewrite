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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class CommentImportTest implements RewriteTest {

    @Test
    void singleLineComment() {
        rewriteRun(
          spec -> spec.recipe(new CommentImport("java.util.UUID", "comment", null)),
          //language=java
          java("""
            // An existing comment
            import java.util.UUID;
                      
            class A {}
            """, """
            // An existing comment
            //comment
            //import java.util.UUID;
                      
            class A {}
            """)
        );
    }

    @Test
    void multiLineComment() {
        rewriteRun(
          spec -> spec.recipe(new CommentImport("java.util.UUID", "comment", true)),
          //language=java
          java("""
            // An existing comment
            import java.util.UUID;
                      
            class A {}
            """, """
            // An existing comment
            /*
            comment
            import java.util.UUID;
            */

            class A {}
            """)
        );
    }
}
