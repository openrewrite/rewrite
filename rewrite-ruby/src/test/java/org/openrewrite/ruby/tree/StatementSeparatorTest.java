/*
 * Copyright 2025 the original author or authors.
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

/**
 * `;` separates statements. One that follows a statement is a marker on that statement; one with
 * nothing in front of it is an empty statement.
 */
public class StatementSeparatorTest implements RewriteTest {

    @Test
    void statementsOnOneLine() {
        rewriteRun(
          ruby(
            """
              a; b; c
              """
          )
        );
    }

    @Test
    void noSpaceAroundSeparator() {
        rewriteRun(
          ruby(
            """
              a;b
              """
          )
        );
    }

    @Test
    void spaceBeforeSeparator() {
        rewriteRun(
          ruby(
            """
              a ; b
              """
          )
        );
    }

    @Test
    void trailingSeparator() {
        rewriteRun(
          ruby(
            """
              a;
              b;
              """
          )
        );
    }

    @Test
    void repeatedSeparators() {
        rewriteRun(
          ruby(
            """
              a;;b
              ;
              c ;;
              """
          )
        );
    }

    @Test
    void separatorAfterMethodHeader() {
        rewriteRun(
          ruby(
            """
              def to_s; name end
              """
          )
        );
    }

    @Test
    void emptyMethodBody() {
        rewriteRun(
          ruby(
            """
              def x; end
              """
          )
        );
    }

    @Test
    void emptyMethodBodyWithParameters() {
        rewriteRun(
          ruby(
            """
              def x(a, b); end
              """
          )
        );
    }

    @Test
    void separatorsThroughoutMethodBody() {
        rewriteRun(
          ruby(
            """
              def compute(a); b = a * 2; b + 1; end
              """
          )
        );
    }

    @Test
    void classBody() {
        rewriteRun(
          ruby(
            """
              class Permission < Base; end
              """
          )
        );
    }

    @Test
    void classBodyWithStatements() {
        rewriteRun(
          ruby(
            """
              class Permission; attr_reader :name; def to_s; name end; end
              """
          )
        );
    }

    @Test
    void moduleBody() {
        rewriteRun(
          ruby(
            """
              module Empty; end
              """
          )
        );
    }

    @Test
    void singletonClassBody() {
        rewriteRun(
          ruby(
            """
              class << self; def zero; 0 end; end
              """
          )
        );
    }

    @Test
    void classMethodBody() {
        rewriteRun(
          ruby(
            """
              def self.up; add_column :users, :admin, :boolean end
              """
          )
        );
    }

    @Test
    void ifBody() {
        rewriteRun(
          ruby(
            """
              if x; y end
              """
          )
        );
    }

    @Test
    void ifElseBody() {
        rewriteRun(
          ruby(
            """
              if x; y; else; z; end
              """
          )
        );
    }

    @Test
    void unlessBody() {
        rewriteRun(
          ruby(
            """
              unless x; y end
              """
          )
        );
    }

    @Test
    void whileBody() {
        rewriteRun(
          ruby(
            """
              while x; y end
              """
          )
        );
    }

    @Test
    void untilBody() {
        rewriteRun(
          ruby(
            """
              until x; y; end
              """
          )
        );
    }

    @Test
    void forBody() {
        rewriteRun(
          ruby(
            """
              for i in list; p i; end
              """
          )
        );
    }

    @Test
    void blockBody() {
        rewriteRun(
          ruby(
            """
              list.each do |i|; p i; end
              list.each { |i|; p i }
              """
          )
        );
    }

    @Test
    void beginBody() {
        rewriteRun(
          ruby(
            """
              begin; a; end
              """
          )
        );
    }

    @Test
    void rescueBody() {
        rewriteRun(
          ruby(
            """
              begin; a; rescue StandardError => e; b; ensure; c; end
              """
          )
        );
    }

    @Test
    void caseBody() {
        rewriteRun(
          ruby(
            """
              case x; when 1; "one"; when 2 then "two"; else; "many"; end
              """
          )
        );
    }

    @Test
    void separatorAfterComment() {
        rewriteRun(
          ruby(
            """
              a # trailing
              ; b
              """
          )
        );
    }

    @Test
    void lambdaBody() {
        rewriteRun(
          ruby(
            """
              f = ->(x) { p x; x + 1 }
              """
          )
        );
    }

    @Test
    void beginEndBlocks() {
        rewriteRun(
          ruby(
            """
              BEGIN { a; b }
              END { c; d }
              """
          )
        );
    }
}
