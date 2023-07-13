/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings({"RedundantSuppression", "RedundantNullableReturnType"})
class AnnotationTest implements RewriteTest {

    @Test
    void fileScope() {
        rewriteRun(
          kotlin(
            """
              @file : Suppress ( "DEPRECATION_ERROR" , "RedundantUnitReturnType" )

              class A
              """
          )
        );
    }

    @Test
    void multipleFileScope() {
        rewriteRun(
          kotlin(
            """
              @file : A ( "1" )
              @file : A ( "2" )
              @file : A ( "3" )

              @Repeatable
              annotation class A ( val s : String )
              """
          )
        );
    }

    @Test
    void annotationWithDefaultArgument() {
        rewriteRun(
          kotlin(
            """
              @SuppressWarnings ( "ConstantConditions" , "unchecked" )
              class A
              """
          )
        );
    }

    @Test
    void arrayArgument() {
        rewriteRun(
          kotlin(
            """
              @Target ( AnnotationTarget . LOCAL_VARIABLE )
              @Retention ( AnnotationRetention . SOURCE )
              annotation class Test ( val values : Array < String > ) {
              }
              """
          ),
          kotlin(
            """
              @Test( values = [ "a" , "b" , "c" ] )
              val a = 42
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/80")
    @Test
    void jvmNameAnnotation() {
        rewriteRun(
          kotlin(
            """
              import kotlin.jvm.JvmName
              @get : JvmName ( "getCount" )
              val count : Int ?
                  get ( ) = 1
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/156")
    @Test
    void annotationUseSiteTargetAnnotationOnly() {
        rewriteRun(
          kotlin(
            """
              @Repeatable
              annotation class A (val s : String)
              class TestA {
                  @get : A("1")
                  @set : A("2")
                  var name: String = ""
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/156")
    @Test
    void annotationUseSiteTarget() {
        rewriteRun(
          kotlin("""
              @Repeatable
              annotation class A (val s : String)
          """),
          kotlin(
            """
              class TestA {
                  @get : A("1")
                  public
                  @set : A("2")
                  var name: String = ""
              }
              """
          ),
          kotlin(
            """
              class TestB {
                  public
                  @get : A("1")
                  @set : A("2")
                  var name: String = ""
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/173")
    @Test
    void constructorParameterWithAnnotation() {
        rewriteRun(
          kotlin(
            """
              annotation class Ann
              class Example(
                  @get : Ann
                  val bar : String
                  ) {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/173")
    @Test
    void getUseSiteOnConstructorParams() {
        rewriteRun(
          kotlin("@Repeatable annotation class A"),
          kotlin(
            """
              class Example( @get : A @set : A var foo: String, @get : A val bar: String)
              """
          )
        );
    }

    @Test
    void conditionalParameter() {
        rewriteRun(
          kotlin(
            """
              annotation class A ( val s : String )
              @A( if ( true ) "1" else "2" )
              class Test
              """
          )
        );
    }
}
