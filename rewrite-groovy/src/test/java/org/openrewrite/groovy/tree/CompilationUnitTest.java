/*
 * Copyright 2021 the original author or authors.
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

import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings({"GroovyUnusedAssignment", "GrUnnecessaryDefModifier", "GrMethodMayBeStatic"})
class CompilationUnitTest implements RewriteTest {

    @SuppressWarnings("GrPackage")
    @Test
    void packageDecl() {
        rewriteRun(
          groovy(
            """
              package org.openrewrite
              def a = 'hello'
              """
          )
        );
    }

    @Test
    void mixedImports() {
        rewriteRun(
          groovy(
            """
              def a = 'hello'
              import java.util.List
              List l = null
              """
          )
        );
    }

    @Test
    void shellScript() {
        rewriteRun(
          groovy(
            """
              #!/usr/bin/env groovy
              
              def a = 'hello'
              """
          )
        );
    }

    @Test
    void trailingComment() {
        rewriteRun(
          groovy(
            """
              // foo
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1974")
    @Test
    void topLevelExpression() {
        rewriteRun(
          groovy(
            """
              5
              """
          )
        );
    }

    @Test
    void scriptImportsCanBeAnywhere() {
        rewriteRun(
          spec -> spec.parser(GroovyParser.builder().compilerCustomizers(config -> {
              ImportCustomizer imports = new ImportCustomizer();
              imports.addStarImports("java.nio.file");
              config.addCompilationCustomizers(imports);
          })),
          groovy(
            """
              def p = Paths.get("abc")
              
              import java.io.File
              def f = new File(p.toFile(), "def")
              
              import java.io.InputStream
              f.withInputStream { InputStream io ->
                io.read()
              }
              """
          )
        );
    }

    @Test
    void shouldNotFailWhenImportCannotBeResolved() {
        rewriteRun(
          groovy(
            """
              import com.example.MyClass
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4704")
    @Test
    void addingToMaps() {
        rewriteRun(
          groovy(
                """
            class Pair {
                String foo
                String bar
                Pair(String foo, String bar) {
                    this.foo = foo
                    this.bar = bar
                }
            }
            class A {
                def foo(List l) {
                    l.add(new Pair("foo", "bar"))
                    l.add(new Pair("foo", "bar"))
                    l.add(new Pair("foo", "bar"))
                    l.add(new Pair("foo", "bar"))
                }
            }
            """
          )
        );
    }
}
