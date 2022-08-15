/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface RenameLocalVariablesToCamelCaseTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = RenameLocalVariablesToCamelCase()

    @Test
    fun renameAllCapsAcronyms(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                void test() {
                    String ID;
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    String id;
                }
            }
        """
    )

    @Test
    fun renameLocalVariables(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                int DoNoTChange;

                public int addTen(int rename_one) {
                    double RenameTwo = 2.0;
                    float __rename__three__ = 2.0;
                    long _Rename__Four = 2.0;
                    return rename_one + RenameTwo + __rename__three__ + _Rename__Four + 10;
                }
            }
        """,
        after = """
            class Test {
                int DoNoTChange;

                public int addTen(int renameOne) {
                    double renameTwo = 2.0;
                    float renameThree = 2.0;
                    long renameFour = 2.0;
                    return renameOne + renameTwo + renameThree + renameFour + 10;
                }
            }
        """
    )

    @Test
    fun doNotChangeStaticImports(jp: JavaParser) = assertUnchanged(
        jp,
        dependsOn = arrayOf("""
            class B {
                public static int _staticImport_ = 0;
            }
        """),
        before = """
            import static B._staticImport_;

            class Test {
                public int addTen(int testValue) {
                    _staticImport_++;
                    return testValue;
                }
            }
        """
    )

    @Test
    fun doNotChangeInheritedFields(jp: JavaParser) = assertUnchanged(
        jp,
        dependsOn = arrayOf("""
            class A {
                public int _inheritedField_ = 0;
            }
        """),
        before = """
            class Test extends A {
                public int addTen(int testValue) {
                    _inheritedField_++;
                    return testValue;
                }
            }
        """
    )

    @Test
    fun doNotChangeIfToNameExists(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                int DoNoTChange;

                public int addTen(int test_value) {
                    int testValue = 10;
                    return test_value + testValue;
                }
            }
        """
    )

    @Test
    fun doNotChangeCatch(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                int DoNoTChange;

                public int addTen(String value) {
                    try {
                        Integer.valueOf(value);
                    // Rule does not apply to catch variables with 1 character.
                    } catch (Exception E){
                        throw new NumberFormatException("Test", E);
                    }
                    return DoNoTChange + 10;
                }
            }
        """
    )

    @Test
    fun doNotForLoop(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                int DoNoTChange;

                public void addTen() {
                    // Rule does not apply to for loop control variables.
                    for (int do_not_change = 0; do_not_change < 10; do_not_change++) {
                       DoNoTChange += do_not_change; 
                    }
                }
            }
        """
    )

    @Test
    fun doNotRenameConstantVariable(jp: JavaParser) = assertUnchanged(
            jp,
            before = """
            import java.util.ArrayList;
            import java.util.List;
            class Test {
                public List<String> testFoo() {
                    return new ArrayList<>() {
                        private final int DO_NOT_CHANGE = 1;
                   
                    };
                    
                }
            }

        """
    )
}
