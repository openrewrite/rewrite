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

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

class MethodInvocationTest implements RewriteTest {
    @Test
    void gradle() {
        rewriteRun(
          groovy(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation 'org.hibernate:hibernate-core:3.6.7.Final'
                  api 'com.google.guava:guava:23.0'
                  testImplementation 'junit:junit:4.+'
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/4615")
    void gradleWithParentheses() {
        rewriteRun(
          groovy(
            """
              plugins {
                  id 'java-library'
              }
              def version = (rootProject.jobName.startsWith('a')) ? "latest.release" : "3.0"
              """
          )
        );
    }

    @Test
    void emptyArgsWithParens() {
        rewriteRun(
          groovy("mavenCentral()")
        );
    }

    @Test
    void noParentheses() {
        rewriteRun(
          groovy(
            """
              class SomeObject {}
              def foo(String a, int b, SomeObject c, String d) {}
              foo "a", 3, new SomeObject(), "d"
              """
          )
        );
    }

    @Test
    @SuppressWarnings("GroovyVariableNotAssigned")
    void nullSafeDereference() {
        rewriteRun(
          groovy(
            """
              Map m
              m?.clear()
              """
          )
        );
    }

    @Test
    void mapLiteralFirstArgument() {
        rewriteRun(
          groovy(
            """
              foo(["foo" : "bar"])
              """
          )
        );
    }

    @Test
    void namedArgumentsInDeclaredOrder() {
        rewriteRun(
          groovy(
            """
              def acceptsNamedArguments (Map a, int i) { }
              acceptsNamedArguments(foo: "bar", 1)
              """
          )
        );
    }

    @Test
    void namedArgumentsAfterPositionalArguments() {
        rewriteRun(
          groovy(
            """
              def acceptsNamedArguments (Map a, int i) { }
              acceptsNamedArguments(1, foo: "bar")
              """
          )
        );
    }

    @Test
    void namedArgumentBeforeClosure() {
        rewriteRun(
          groovy(
            """
              def acceptsNamedArguments(Map a, int i, Closure c) {}
              acceptsNamedArguments(1, foo: "bar") { }
              """
          )
        );
    }

    @Test
    void namedArgumentsBeforeClosure() {
        rewriteRun(
          groovy(
            """
              def acceptsNamedArguments(Map a, Closure c) {}
              acceptsNamedArguments(foo: "bar", bar: "baz") { }
              """
          )
        );
    }

    @Test
    void namedArgumentsBetweenPositionalArguments() {
        rewriteRun(
          groovy(
            """
              def acceptsNamedArguments(Map a, int n, int m) { }
              acceptsNamedArguments(1, foo: "bar", 2)
              """
          )
        );
    }

    @Test
    void namedArgumentsAllOverThePlace() {
        rewriteRun(
          groovy(
            """
              def acceptsNamedArguments(Map a, int n, int m) { }
              acceptsNamedArguments(1, foo: "bar", 2, bar: "baz")
              """
          )
        );
    }

    @Test
    void useClassAsArgument() {
        rewriteRun(
          groovy(
            """
              foo(String)
              """
          )
        );
    }

    @Test
    void useClassAsArgumentJavaStyle() {
        rewriteRun(
          groovy(
            """
              foo(String    .class)
              """
          )
        );
    }

    @Test
    @SuppressWarnings("GroovyAssignabilityCheck")
    void closureWithImplicitParameter() {
        rewriteRun(
          groovy(
            """
              def acceptsClosure(Closure cl) {}
              acceptsClosure {
                  println(it)
              }
              """
          )
        );
    }

    @Test
    void closureInObjectInObject() {
        rewriteRun(
          groovy(
            """
              class Test {
                Test child = new Test()
                def acceptsClosure(Closure cl) {}
              }
              
              new Test().child.acceptsClosure {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4116")
    @Test
    void closureWithGString() {
        rewriteRun(
          groovy(
            """
            { x -> "${x.y}" }
            """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4766")
    @Test
    void gradleFileWithMultipleClosuresWithoutParentheses() {
        rewriteRun(
          groovy(
            """
              copySpec {
                  from { 'src/main/webapp' } { exclude "**/*.jpg" }
                  rename '(.+)-staging(.+)', '$1$2'
              }
              """
          )
        );
    }

    @Test
    void multipleClosureArgumentsWithoutParentheses() {
        rewriteRun(
          groovy(
            """
              def foo(Closure a, Closure b, Closure c) {}
              foo {     }    {        } {
              }
              """
          )
        );
    }

    @Test
    void multipleClosureArgumentsWithParentheses() {
        rewriteRun(
          groovy(
            """
              def foo(Closure a, Closure b, Closure c) {}
              foo({ }, { }, {
              })
              """
          )
        );
    }

    @Test
    void multipleArgumentsWithClosuresAndNonClosuresWithoutParentheses() {
        rewriteRun(
          groovy(
            """
              def foo(String a, Closure b, Closure c, String d) {}
              foo "a", { },            {
              }, "d"
              """
          )
        );
    }

    @Test
    void trailingClosures() {
        rewriteRun(
          groovy(
            """
              def foo(String a, int b, String c, Closure d, Closure e, Closure f) {}
              foo("bar", 3, "baz") {       }           { } {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1236")
    @Test
    @SuppressWarnings("GroovyAssignabilityCheck")
    void closureWithNamedParameter() {
        rewriteRun(
          groovy(
            """
              def acceptsClosure(Closure cl) {}
              acceptsClosure { foo ->
                  println(foo)
              }
              """
          )
        );
    }

    @Test
    void closureWithNamedParameterAndType() {
        rewriteRun(
          groovy(
            """
              def acceptsClosure(Closure cl) {}
              acceptsClosure { String foo ->
                  println(foo)
              }
              """
          )
        );
    }

    @Test
    void closureArgumentInParens() {
        rewriteRun(
          groovy(
            """
              def acceptsClosure(Closure cl) {}
              acceptsClosure({})
              """
          )
        );
    }

    @Test
    void closureArgumentAfterEmptyParens() {
        rewriteRun(
          groovy(
            """
              def acceptsClosure(Closure cl) {}
              acceptsClosure ( /* () */ ) { /* {} */ }
              """
          )
        );
    }

    @Test
    void closureReturn() {
        rewriteRun(
          groovy(
            """
              foo {
                  return
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4055")
    @Test
    void chainOfMethodInvocations() {
        rewriteRun(
          groovy(
            """
              Micronaut.build(args)
                      .banner(false)
                      .propertySources(PropertySource.of("my-config", [name: "MyApp"]))
                      .environments("prod") // Only prod
                      .overrideConfig("custom-config.yml") // Load custom config
                      .packages("com.company")
                      .mainClass(Application)
                      .start()
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2552")
    @Test
    void closureInvocation() {
        rewriteRun(
          groovy(
            """
              def closure = {}
              closure()
              closure.call()
              """
          )
        );
    }

    @Test
    void staticMethodInvocation() {
        rewriteRun(
          groovy(
            """
              class StringUtils {
                static boolean isEmpty(String value) {
                  return value == null || value.isEmpty()
                }
              
                static void main(String[] args) {
                  isEmpty("")
                }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4703")
    @Test
    void insideParenthesesSimple() {
        rewriteRun(
          groovy(
            """
              ((a.invoke "b" ))
              """
          )
        );
    }

    @Test
    void lotOfSpacesAroundConstantWithParentheses() {
        rewriteRun(
          groovy(
            """
              (  ( (    "x"         )        ).toString()       )
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4703")
    @Test
    void insideParentheses() {
        rewriteRun(
          groovy(
            """              
              static def foo(Map map) {
                  ((map.containsKey("foo"))
                      && ((map.get("foo")).equals("bar")))
              }
              """
          )
        );
    }

    @Test
    void topLevelCallWithoutReceiver() {
        rewriteRun(
          groovy(
            """
              from('timer:groovy?period=1000')
                  .setBody()
                      .constant('Hello Camel K')
                  .setBody()
                      .simple('body - header.RandomValue')
              """
          )
        );
    }

    @Test
    void insideParenthesesWithNewline() {
        rewriteRun(
          groovy(
            """              
              static def foo(Map map) {
                  ((
                  map.containsKey("foo"))
                      && ((map.get("foo")).equals("bar")))
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4703")
    @Test
    void insideParenthesesWithoutNewLineAndEscapedMethodName() {
        rewriteRun(
          groovy(
            """
              static def foo(Map someMap) {((((((someMap.get("(bar")))) ).'equals' "baz" )   )      }
              """
          )
        );
    }

    @Test
    void insideFourParenthesesAndEnters() {
        rewriteRun(
          groovy(
            """
              ((((
                something(a)
              ))))
              """
          )
        );
    }

    @Test
    void insideFourParenthesesAndEntersWithCommentWithParentheses() {
        rewriteRun(
          groovy(
            """
              ((/* comment :-) ((
              */((
                // :-((
                /*)*/something(a)
              ))))
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/5729")
    void closureChainedCall() {
        rewriteRun(
          groovy(
            """
              foo { }()
              """
          )
        );
    }

    @Test
    void closureCallWithArgs() {
        rewriteRun(
          groovy(
            """
              def bar(closure) {
                  return { x -> println x }
              }
              bar { }("hello")
              """
          )
        );
    }

    @Test
    void multipleChainedCalls() {
        rewriteRun(
          groovy(
            """
              def chain() {
                  return { { println "nested" } }
              }
              chain()()
              """
          )
        );
    }

    @Test
    void closureWithParamCall() {
        rewriteRun(
          groovy(
            """
              def create(x) {
                  return { y -> println '$x $y' }
              }
              create("hello") { }("world")
              """
          )
        );
    }

    @Test
    void nestedMethodClosureCall() {
        rewriteRun(
          groovy(
            """
              def outer() {
                  return { inner() }
              }
              def inner() {
                  return { println "inner" }
              }
              outer()()
              """
          )
        );
    }

    @Test
    void variableClosureCall() {
        rewriteRun(
          groovy(
            """
              def factory() {
                  return { x -> x * 2 }
              }
              def closure = factory()
              closure(5)
              """
          )
        );
    }

    @Test
    void closureComplexArgs() {
        rewriteRun(
          groovy(
            """
              def builder(closure) {
                  return { a, b -> closure(a + b) }
              }
              builder { println it }(10, 20)
              """
          )
        );
    }

    @Test
    void safeNavigationClosureCall() {
        rewriteRun(
          groovy(
            """
              def maybe() {
                  return Math.random() > 0.5 ? { println "yes" } : null
              }
              maybe()?.call()
              """
          )
        );
    }

}
