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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.tree.ParserAssertions.kotlin;

class ClassDeclarationTest implements RewriteTest {

    @Test
    void multipleClassDeclarationsInOneCompilationUnit() {
        rewriteRun(
          kotlin(
            """
                package some.package.name
                class A {}
                class B {}
            """
          )
        );
    }

    @Test
    void empty() {
        rewriteRun(
          kotlin(
            """
                class A
                class B
            """
          )
        );
    }

    @Test
    void classImplements() {
        rewriteRun(
          kotlin(
            """
                interface A
                interface B
                class C : A , B
                class D : B , A
            """
          )
        );
    }

    @Test
    void classExtends() {
        rewriteRun(
          kotlin(
            """
                open class A
                class B : A ( )
            """
          )
        );
    }

    @Test
    void extendsAndImplementsInMixedOrder() {
        rewriteRun(
          kotlin(
            """
            interface A
            interface B
            open class C

            class D : A , C ( ) , B
            """
          )
        );
    }

    @Test
    void innerClass() {
        rewriteRun(
          kotlin(
            """
              interface C {
                  class Inner {
                  }
              }
              """
          )
        );
    }

    @Test
    void modifierOrdering() {
        rewriteRun(
          kotlin(
            "public abstract class A"
          )
        );
    }

    @Test
    void annotationClass() {
        rewriteRun(
          kotlin(
            "annotation class A"
          )
        );
    }

    @Test
    void enumClass() {
        rewriteRun(
          kotlin(
            "enum class A"
          )
        );
    }

    @Test
    void annotation() {
        rewriteRun(
          kotlin(
            """
                public @Deprecated("message 0") abstract @Suppress("") class Test
                
                @Deprecated("message 1")
                @Suppress("")
                class A
                
                @Suppress("unused", "unchecked")
                @Deprecated("message 2")
                class B
            """
          )
        );
    }

    @Test
    void quotedIdentifier() {
        rewriteRun(
          kotlin(
            "class `Quoted id here`"
          )
        );
    }

    @Test
    void typeArguments() {
        rewriteRun(
          kotlin(
            "open class B<T> {}"
          )
        );
    }

    @Test
    void singleBoundedTypeParameters() {
        rewriteRun(
          kotlin(
            """
                  interface A
                  interface B
                  
                  class KotlinTypeGoat<T: A, S: B>
              """
          )
        );
    }

    @Disabled
    @Test
    void multipleBounds() {
        rewriteRun(
          kotlin(
            """
                  interface A
                  interface B
                  interface C
                  interface D
                  
                  class KotlinTypeGoat < T , S > where S : A , T : D , S : B , T : C
              """
          )
        );
    }
}
