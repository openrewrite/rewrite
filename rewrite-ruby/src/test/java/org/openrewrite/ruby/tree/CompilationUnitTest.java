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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.ruby.Assertions.ruby;

public class CompilationUnitTest implements RewriteTest {

    @Test
    void dataSection() {
        rewriteRun(
          ruby(
            """
              puts DATA.read
              __END__
              name: value
              - a list
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getDataSection())
              .isNotNull()
              .satisfies(data -> assertThat(data.getText())
                .isEqualTo("__END__\nname: value\n- a list")))
          )
        );
    }

    @Test
    void emptyDataSection() {
        rewriteRun(
          ruby(
            """
              puts 1
              __END__
              """
          )
        );
    }

    @Test
    void noDataSection() {
        rewriteRun(
          ruby(
            "puts 1\n",
            spec -> spec.afterRecipe(cu -> assertThat(cu.getDataSection()).isNull())
          )
        );
    }

    /**
     * Sinatra keeps its inline templates after {@code __END__}, where a `@@` line is data rather
     * than Ruby.
     */
    @Test
    void inlineTemplates() {
        rewriteRun(
          ruby(
            """
              get('/') { erb :index }
              __END__

              @@ index
              %div.title Hello world
              """
          )
        );
    }

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

    /**
     * The magic comment makes Prism wrap the assignments that follow it; the tree keeps only the
     * assignments, and the comment round-trips as a comment.
     */
    @Test
    void shareableConstantValue() {
        rewriteRun(
          ruby(
            """
              # frozen_string_literal: true
              # shareable_constant_value: literal
              CONST = [1, 2, 3]
              OTHER = {a: 1}
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
