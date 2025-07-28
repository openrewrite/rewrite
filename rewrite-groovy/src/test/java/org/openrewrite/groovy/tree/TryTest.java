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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings({"GroovyUnusedCatchParameter", "GroovyUnusedAssignment"})
class TryTest implements RewriteTest {

    @Test
    void severalCatchBlocks() {
        rewriteRun(
          groovy(
            """
              try {
              } catch (RuntimeException e) {
              } catch (Exception e) {
              }
              """
          )
        );
    }

    @Test
    void catchOmittingType() {
        rewriteRun(
          groovy(
            """
              try {
              } catch (all) {
              }
              """
          )
        );
    }

    @Test
    void modifier() {
        rewriteRun(
          groovy(
            """
              try {
              } catch (final RuntimeException e) {
              }
              """
          )
        );
    }

    @Test
    void tryFinally() {
        rewriteRun(
          groovy(
            """
              try {
              } finally {
              }
              """
          )
        );
    }

    @Test
    void tryCatchFinally() {
        rewriteRun(
          groovy(
            """
             try {
             } catch (Exception e) {
             } finally {
                 def a = ""
             }
             """
          )
        );
    }

    @ExpectedToFail
    @Issue("https://github.com/openrewrite/rewrite/issues/1944")
    @Test
    void multiCatch() {
        rewriteRun(
          groovy(
            """
              try {
              } catch (IOException | UncheckedIOException e) {
              }
              """
          )
        );
    }

    @ExpectedToFail
    @Issue("https://github.com/openrewrite/rewrite/issues/1945")
    @Test
    void tryWithResource() {
        rewriteRun(
          groovy(
            """
              try(ByteArrayInputStream a = new ByteArrayInputStream("".getBytes())) {
              } catch (Exception e) {
              }
              """
          )
        );
    }

    @ExpectedToFail
    @Issue("https://github.com/openrewrite/rewrite/issues/1945")
    @Test
    void tryWithResources() {
        rewriteRun(
          groovy(
            """
              try(ByteArrayInputStream a = new ByteArrayInputStream("".getBytes()); ByteArrayInputStream b = new ByteArrayInputStream("".getBytes())) {
              } catch (Exception e) {
              }
              """
          )
        );
    }

    @Test
    void whitespace() {
        rewriteRun(
          groovy(
            """
              try     {
              
              }    catch(RuntimeException e)      {
              
              }catch (     final         Exception          e) {
              
              }finally{
                  // some comment
              }
              """
          )
        );
    }
}
