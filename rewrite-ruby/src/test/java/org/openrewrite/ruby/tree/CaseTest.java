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

public class CaseTest implements RewriteTest {

    @Test
    void caseSelect() {
        rewriteRun(
          ruby(
            """
              response = gets
              case response
              when /^[nN]/, /^$/
                  return false
              when /^[yY]/
                  return true
              end
              """
          )
        );
    }

    @Test
    void caseSelectElse() {
        rewriteRun(
          ruby(
            """
              response = gets
              case response
              when /^[yY]/
                  return true
              else
                  return false
              end
              """
          )
        );
    }

    @Test
    void casePatternMatching() {
        rewriteRun(
          ruby(
            """
              case %i[a b c d]
                  in Symbol then puts "Single"
                  else puts "Unable to match input."
              end
              """
          )
        );
    }
}
