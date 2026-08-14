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

public class RescueTest implements RewriteTest {

    @Test
    void rescueExceptionUnnamed() {
        rewriteRun(
          ruby(
            """
              begin
                x = factorial(1)
              rescue Exception
                puts "Try again with a value >= 1"
              end
              
              content
              """
          )
        );
    }

    @Test
    void iterationRescue() {
        rewriteRun(
          ruby(
            """
              5.times do |i|
                puts i
              rescue Exception
                puts "Try again with a value >= 1"
              end
              
              content
              """
          )
        );
    }

    @Test
    void rescueExceptionUnnamedMultipleStatements() {
        rewriteRun(
          ruby(
            """
              begin
                x = factorial(1)
              rescue Exception
                puts "Try again with a value >= 1"
                puts "Or try something else"
              end
              
              content
              """
          )
        );
    }

    @Test
    void rescueExceptionNamed() {
        rewriteRun(
          ruby(
            """
              begin
                x = factorial(1)
              rescue Exception => ex
                puts "Try again with a value >= 1"
              end
              
              content
              """
          )
        );
    }

    @Test
    void retry() {
        rewriteRun(
          ruby(
            """
              begin
                x = factorial(1)
              rescue Exception
                retry
              end
              
              content
              """
          )
        );
    }

    @Test
    void multipleRescuesWithTypesAndNames() {
        rewriteRun(
          ruby(
            """
              begin
                x = factorial(1)
              rescue ArgumentError => ex
                puts "Try again with a value >= 1"
              rescue TypeError => ex
                puts "Try again with an integer"
              end
              
              content
              """
          )
        );
    }

    @Test
    void multipleTypesInOneRescue() {
        rewriteRun(
          ruby(
            """
              begin
                x = factorial(1)
              rescue ArgumentError, TypeError => ex
                puts "Try again"
              end
              
              content
              """
          )
        );
    }

    @Test
    void ensure() {
        rewriteRun(
          ruby(
            """
              begin
                x = factorial(1)
              rescue Exception
                puts "Try again with a value >= 1"
              ensure
                puts "Ensured"
              end
              
              content
              """
          )
        );
    }

    @Test
    void elseClause() {
        rewriteRun(
          ruby(
            """
              begin
                x = factorial(1)
              rescue Exception
                puts "Try again with a value >= 1"
              else
                puts "Else"
              end
              
              content
              """
          )
        );
    }

    /**
     * The `begin` is written out even though the enclosing `def` would also supply an `end`.
     */
    @Test
    void explicitBeginInsideMethod() {
        rewriteRun(
          ruby(
            """
              def deliver_mail(mail)
                begin
                  mail.deliver
                rescue Exception => e
                  puts e
                end
              end
              """
          )
        );
    }

    @Test
    void rescueOnMethodDef() {
        rewriteRun(
          ruby(
            """
              def sum(a, b)
                  a + b
              rescue Exception
                  puts "Not reachable"
              end
              
              content
              """
          )
        );
    }

    @Test
    void rescueOnClassMethod() {
        rewriteRun(
          ruby(
            """
              def self.get_proxied_source(raw_source)
                return raw_source
              rescue Excon::Error::Socket, Excon::Error::Timeout => e
                raw_source
                1
              end
              
              content
              """
          )
        );
    }
}
