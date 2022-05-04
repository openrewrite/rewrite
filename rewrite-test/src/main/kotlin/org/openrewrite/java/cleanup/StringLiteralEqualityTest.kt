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
@file:Suppress("StringEquality")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

@Suppress(
    "RedundantStringOperation",
    "ConstantConditions",
    "NewObjectEquality",
    "StatementWithEmptyBody",
    "LoopConditionNotUpdatedInsideLoop",
    "EqualsWithItself", "ResultOfMethodCallIgnored"
)
interface StringLiteralEqualityTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = StringLiteralEquality()

    @Test
    fun stringLiteralEqualityReplacedWithEquals() = assertChanged(
        before = """
            import java.util.List;
            class Test {
                public String getString() {
                    return "stringy";
                }

                public void method(String str) {
                    if (str == "test") ;
                    if ("test" == str) ;
                    if ("test" == "test") ;
                    if ("test" == new String("test")) ;
                    if ("test" == getString());
                    boolean flag = (str == "test");
                    while ("test" == str) {
                    }
                }
                
                public void findPeter(List<Friend> friends) {
                    friends.stream().filter(e -> e.name == "peter");
                }
                
                class Friend {
                    String name;
                }
            }
        """,
        after = """
            import java.util.List;
            class Test {
                public String getString() {
                    return "stringy";
                }

                public void method(String str) {
                    if ("test".equals(str)) ;
                    if ("test".equals(str)) ;
                    if ("test".equals("test")) ;
                    if ("test".equals(new String("test"))) ;
                    if ("test".equals(getString()));
                    boolean flag = ("test".equals(str));
                    while ("test".equals(str)) {
                    }
                }
                
                public void findPeter(List<Friend> friends) {
                    friends.stream().filter(e -> "peter".equals(e.name));
                }
                
                class Friend {
                    String name;
                }
            }
        """
    )

    @Test
    fun stringLiteralEqualityReplacedWithNotEquals() = assertChanged(
        before = """
            class Test {
                public String getString() {
                    return "stringy";
                }

                public void method(String str) {
                    if (str != "test") ;
                    if ("test" != str) ;
                    if ("test" != "test") ;
                    if ("test" != new String("test")) ;
                    if ("test" != getString());
                    boolean flag = (str != "test");
                    while ("test" != str) {
                    }
                }
            }
        """,
        after = """
            class Test {
                public String getString() {
                    return "stringy";
                }

                public void method(String str) {
                    if (!"test".equals(str)) ;
                    if (!"test".equals(str)) ;
                    if (!"test".equals("test")) ;
                    if (!"test".equals(new String("test"))) ;
                    if (!"test".equals(getString()));
                    boolean flag = (!"test".equals(str));
                    while (!"test".equals(str)) {
                    }
                }
            }
        """
    )

    @Test
    fun changeNotNeeded() = assertUnchanged(
        before = """
            class Test {
                public String getString() {
                    return "stringy";
                }

                public void method(String str0, String str1) {
                    if (str0 == new String("str0")) ;
                    if (str1 != new String("str1")) ;
                    if (str0 == str1) ;
                    if (getString() == str0) ;
                    if (str1 != str0) ;
                    if (getString() != str1) ;
                }
            }
        """
    )

}
