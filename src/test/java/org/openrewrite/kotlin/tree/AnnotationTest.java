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
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.Issue;
import org.openrewrite.java.RemoveAnnotation;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.kotlin.tree.ParserAssertions.kotlin;

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

    @Test
    @ExpectedToFail
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/105")
    void fullyQualifiedCustomAnnotation() {
        rewriteRun(
          java(
            """
              package com.example.annotations;
                            
              import java.lang.annotation.Target;
              import static java.lang.annotation.ElementType.*;
                            
              @Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE})
              public @interface JavaAnnotationToRemove {
              }
              """
          ),
          // Works
          kotlin(
            """
              import com.example.annotations.JavaAnnotationToRemove

              @JavaAnnotationToRemove
              class A {
              }
              """),
          // Fails, despite being valid Kotlin code
          kotlin(
            """
              @com.example.annotations.JavaAnnotationToRemove
              class A {
              }
              """)
        );
    }

    @Test
    @ExpectedToFail
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/105")
    void fullyQualifieKotlinAnnotation() {
        rewriteRun(
          // Works, Deprecated is defined in kotlin package so no import needed
          kotlin(
            """
              @Deprecated
              class A {
              }
              """),
          // Fails, despite being valid Kotlin code
          kotlin(
            """
              @kotlin.Deprecated
              class A {
              }
              """)
        );
    }

    // Despite being recognized as an annotation, it is not removed (but it works fine with annotations from java.lang package)
    @Test
    @ExpectedToFail
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/105")
    void customAnnotationIsNotRemoved() {
        rewriteRun(
          spec -> spec.recipe(new RemoveAnnotation("@com.example.annotations.JavaAnnotationToRemove")),
          java(
            """
              package com.example.annotations;
                            
              import java.lang.annotation.Target;
              import static java.lang.annotation.ElementType.*;
                            
              @Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE})
              public @interface JavaAnnotationToRemove {
              }
              """
          ),
          kotlin(
            """
              import com.example.annotations.JavaAnnotationToRemove
                          
              @JavaAnnotationToRemove
              class A {
              }
              """
            , """
              class A {
              }
              """)
        );
    }

    @Test
    @ExpectedToFail
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/105")
    void arrayArgumentRemoval() {
        rewriteRun(
          spec -> spec.recipe(new RemoveAnnotation("@com.example.Test")),
          kotlin(
            """
              package com.example
                          
              @Target ( AnnotationTarget . LOCAL_VARIABLE )
              @Retention ( AnnotationRetention . SOURCE )
              annotation class Test ( val values : Array < String > ) {
              }
              """
          ),
          kotlin(
            """
              import com.example.Test
                          
              @Test( values = [ "a" , "b" , "c" ] )
              val a = 42
              """,
            """
              val a = 42
              """
          )
        );
    }
}
