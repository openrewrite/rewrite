/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class CompilationUnitTest implements RewriteTest {

    /**
     * A shebang naming a wrapper rather than ruby itself, as `Rakefile` and `bin/` scripts do.
     */
    @Test
    void nonRubyShebang() {
        rewriteRun(
          ruby(
            """
              #!/usr/bin/env rake
              # Add your own tasks in files placed in lib/tasks.

              require File.expand_path('../config/application', __FILE__)
              """
          )
        );
    }

    @Test
    void singleBlockStatementIsAnArray() {
        rewriteRun(
          ruby(
            """
              [{
                "type" => "git_source",
                "host" => "github.com",
              }]
              """
          )
        );
    }
}
