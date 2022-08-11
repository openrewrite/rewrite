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

interface RenamePrivateFieldsToCamelCaseTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = RenamePrivateFieldsToCamelCase()

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
                private int member = _staticImport_;
            }
        """
    )

    @Test
    fun doNotChangeInheritedFields(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf("""
            class A {
                public int _inheritedField_ = 0;
            }
        """),
        before = """
            class Test extends A {
                private int _inheritedField_ = super._inheritedField_;
            }
        """,
        after = """
            class Test extends A {
                private int inheritedField = super._inheritedField_;
            }
        """
    )

    @Test
    fun doNotChangeIfToNameExists(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                int test_value;

                public int addTen(int testValue) {
                    return test_value + testValue;
                }
            }
        """
    )

    @Test
    fun doNotChangeExistsInOnlyOneMethod(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                private int DoNoTChange;
                
                public int addTwenty(String doNoTChange) {
                    return DoNoTChange + 20;
                }
                public int addTen(String value) {
                    return DoNoTChange + 10;
                }
            }
        """
    )

    @Test
    fun renamePrivateMembers(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                private int DoChange = 10;
                public int DoNotChangePublicMember;
                int DoNotChangeDefaultMember;

                public int getTen() {
                    return DoChange;
                }

                public int getTwenty() {
                    return this.DoChange * 2;
                }

                public int getThirty() {
                    return DoChange * 3;
                }
            }
        """,
        after = """
            class Test {
                private int doChange = 10;
                public int DoNotChangePublicMember;
                int DoNotChangeDefaultMember;

                public int getTen() {
                    return doChange;
                }

                public int getTwenty() {
                    return this.doChange * 2;
                }

                public int getThirty() {
                    return doChange * 3;
                }
            }
        """
    )

    @Test
    fun renameWithFieldAccess(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf("""
            class ClassWithPublicField {
                public int publicField = 10;
            }
        """),
        before = """
            class Test {
                private ClassWithPublicField DoChange = new ClassWithPublicField();

                public int getTen() {
                    return DoChange.publicField;
                }
            }
        """,
        after = """
            class Test {
                private ClassWithPublicField doChange = new ClassWithPublicField();

                public int getTen() {
                    return doChange.publicField;
                }
            }
        """
    )

    @Test
    fun doNotRenameInnerClassesMembers(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                private int test = new InnerClass().DoNotChange + new InnerClass().DoNotChange2;
                
                private class InnerClass{
                    public int DoNotChange = 10;
                    private int DoNotChange2 = 10;
                }
            }
        """
    )

    @Test
    fun renameUsageInInnerClasses(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                private int DoChange = 10;
                
                private class InnerClass{
                    private int test = DoChange + 1;
                }
            }
        """,
        after = """
            class Test {
                private int doChange = 10;
                
                private class InnerClass{
                    private int test = doChange + 1;
                }
            }
        """
    )

    @Test
    fun doNotRenameAnonymousInnerClasses(jp: JavaParser) = assertUnchanged(
        jp,
        dependsOn = arrayOf("""
                interface Book{}
            """),
        before = """
                class B (){
                    B(){
                        new Book() {
                          private String DoChange;

                          @Override
                          public String toString() {
                            return DoChange;
                          }
                        };
                    }
                }
        """
    )

    @Test
    fun handleStaticMethods(jp: JavaParser) = assertChanged(
        jp,
        before = """
                class A (){
                    private int _variable;
                    public static A getInstance(){
                        A a = new A();
                        a._variable = 12;
                        return a;
                    }
                }
        """,
        after = """
                class A (){
                    private int variable;
                    public static A getInstance(){
                        A a = new A();
                        a.variable = 12;
                        return a;
                    }
                }
        """
    )

    @Test
    fun renameFinalMembers(jp: JavaParser) = assertChanged(
        jp,
        before = """
                class A (){
                    private final int _final_variable;
                    private static int _static_variable;
                    private static final int DO_NOT_CHANGE;
                }
        """,
        after = """
                class A (){
                    private final int finalVariable;
                    private static int staticVariable;
                    private static final int DO_NOT_CHANGE;
                }
        """
    )

    @Test
    fun doNotChangeWhenSameMethodParam(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
                class A (){
                    private int _variable;
                    public void getInstance(int _variable){
                        this._variable = _variable;
                    }
                }
        """)

    @Test
    fun renameWhenSameMethodExists(jp: JavaParser) = assertChanged(
        jp,
        before = """
                class A (){
                    private boolean _hasMethod;
                    public boolean hasMethod(){
                        return _hasMethod;
                    }
                }
        """,
        after = """
                class A (){
                    private boolean hasMethod;
                    public boolean hasMethod(){
                        return hasMethod;
                    }
                }
        """
    )
}
