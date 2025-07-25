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

class DelegationTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/145")
    @Test
    void delegationByMap() {
        rewriteRun(
          kotlin(
            """
              class Foo (map : Map<String , Any?>) {
                  val bar : String by map
                  val baz : Int by map
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/269")
    @Test
    void delegationToProperty() {
        rewriteRun(
          kotlin(
            """
              var topLevelInt: Int = 0
              class ClassWithDelegate(val anotherClassInt: Int)
              
              class MyClass(var memberInt: Int, val anotherClassInstance: ClassWithDelegate) {
                  var delegatedToMember: Int by this::memberInt
                  var delegatedToTopLevel: Int by ::topLevelInt
              
                  val delegatedToAnotherClass: Int by anotherClassInstance::anotherClassInt
              }
              var MyClass.extDelegated: Int by ::topLevelInt
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/269")
    @Test
    void classWithDelegation() {
        rewriteRun(
          kotlin(
            """
              class Test(base: Collection<Any>) :  Collection< Any >   by    base
              """
          )
        );
    }

    @Test
    void delegationByObservable() {
        rewriteRun(
          kotlin(
            """
              import kotlin.properties.Delegates
              
              class User {
                  var name: String by Delegates.observable("<no name>") {
                      prop, old, new ->
                      println("$old -> $new")
                  }
              }
              """
          )
        );
    }
}
