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
@file:Suppress("ConstantConditions", "AnonymousHasLambdaAlternative", "ResultOfMethodCallIgnored")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.Tree
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.marker.JavaVersion
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

interface RenamePrivateFieldsToCamelCaseTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(RenamePrivateFieldsToCamelCase())
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2461")
    @Test
    fun upperSnakeToLowerCamel() = rewriteRun(
        java("""
            class Test {
                private String D_TYPE_CONNECT = "";
            }
        """,
            """
            class Test {
                private String dTypeConnect = "";
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2294")
    @Test
    fun nameConflict() = rewriteRun(
        java("""
            class A {
                private final String _val = "";
                private void a() {
                    if (true) {
                        Thread t = new Thread(){
                            public void run() {
                                String val = _val;
                            }
                        };
                    }
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2285")
    @Test
    fun doesNotRenameAssociatedIdentifiers() = rewriteRun(
        java("""
            class A {
                private static String MY_STRING = "VAR";
                void doSomething() {
                    MY_STRING.toLowerCase();
                    AB.INNER_STRING.toLowerCase();
                }
            
                private static class AB {
                    private static String INNER_STRING = "var";
                    void doSomething() {
                        MY_STRING.toLowerCase();
                    }
                }
            }
        """,
        """
            class A {
                private static String myString = "VAR";
                void doSomething() {
                    myString.toLowerCase();
                    AB.INNER_STRING.toLowerCase();
                }
            
                private static class AB {
                    private static String INNER_STRING = "var";
                    void doSomething() {
                        myString.toLowerCase();
                    }
                }
            }
        """)
    )

    @Test
    fun doNotChangeStaticImports() = rewriteRun(
        java("""
            class B {
                public static int _staticImport_ = 0;
            }
        """),
        java("""
            import static B._staticImport_;

            class Test {
                private int member = _staticImport_;
            }
        """)
    )

    @Test
    fun doNotChangeInheritedFields() = rewriteRun(
        java("""
            class A {
                public int _inheritedField_ = 0;
            }
        """),
        java("""
            class Test extends A {
                private int _inheritedField_ = super._inheritedField_;
            }
        """,
        """
            class Test extends A {
                private int inheritedField = super._inheritedField_;
            }
        """)
    )

    @Test
    fun doNotChangeIfToNameExists() = rewriteRun(
        java("""
            class Test {
                int test_value;

                public int addTen(int testValue) {
                    return test_value + testValue;
                }
            }
        """)
    )

    @Test
    fun doNotChangeExistsInOnlyOneMethod() = rewriteRun(
       java("""
            class Test {
                private int DoNoTChange;
                
                public int addTwenty(String doNoTChange) {
                    return DoNoTChange + 20;
                }
                public int addTen(String value) {
                    return DoNoTChange + 10;
                }
            }
        """)
    )

    @Test
    fun renamePrivateMembers() = rewriteRun(
       java("""
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
        """
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
        """)
    )

    @Test
    fun renameWithFieldAccess() = rewriteRun(
       java("""
            class ClassWithPublicField {
                public int publicField = 10;
            }
        """),
        java("""
            class Test {
                private ClassWithPublicField DoChange = new ClassWithPublicField();

                public int getTen() {
                    return DoChange.publicField;
                }
            }
        """,
        """
            class Test {
                private ClassWithPublicField doChange = new ClassWithPublicField();

                public int getTen() {
                    return doChange.publicField;
                }
            }
        """)
    )

    @Test
    fun doNotRenameInnerClassesMembers() = rewriteRun(
       java("""
            class Test {
                private int test = new InnerClass().DoNotChange + new InnerClass().DoNotChange2;
                
                private class InnerClass{
                    public int DoNotChange = 10;
                    private int DoNotChange2 = 10;
                }
            }
       """)
    )

    @Test
    fun renameUsageInInnerClasses() = rewriteRun(
        java("""
            class Test {
                private int DoChange = 10;
                
                private class InnerClass{
                    private int test = DoChange + 1;
                }
            }
        """,
        """
            class Test {
                private int doChange = 10;
                
                private class InnerClass{
                    private int test = doChange + 1;
                }
            }
        """)
    )

    @Test
    fun doNotRenameAnonymousInnerClasses() = rewriteRun(
        java("""
                interface Book{}
            """),
        java("""
                class B {
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
        """)
    )

    @Test
    fun handleStaticMethods() = rewriteRun(
       java("""
                class A {
                    private int _variable;
                    public static A getInstance(){
                        A a = new A();
                        a._variable = 12;
                        return a;
                    }
                }
        """,
        """
                class A {
                    private int variable;
                    public static A getInstance(){
                        A a = new A();
                        a.variable = 12;
                        return a;
                    }
                }
        """)
    )

    @Test
    fun renameFinalMembers() = rewriteRun(
        java("""
                class A {
                    private final int _final_variable;
                    private static int _static_variable;
                    private static final int DO_NOT_CHANGE;
                }
        """,
        """
                class A {
                    private final int finalVariable;
                    private static int staticVariable;
                    private static final int DO_NOT_CHANGE;
                }
        """)
    )

    @Test
    fun doNotChangeWhenSameMethodParam() = rewriteRun(
        java("""
                class A {
                    private int _variable;
                    public void getInstance(int _variable) {
                        this._variable = _variable;
                    }
                }
        """)
    )

    @Test
    fun renameWhenSameMethodExists() = rewriteRun(
        java("""
                class A {
                    private boolean _hasMethod;
                    public boolean hasMethod() {
                        return _hasMethod;
                    }
                }
        """,
        """
                class A {
                    private boolean hasMethod;
                    public boolean hasMethod() {
                        return hasMethod;
                    }
                }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2526")
    @Test
    fun recordCompactConstructor() = rewriteRun(
        { spec -> spec.beforeRecipe { sf ->
            val javaRuntimeVersion = System.getProperty("java.runtime.version")
            val javaVendor = System.getProperty("java.vm.vendor")
            if (JavaVersion(Tree.randomId(), javaRuntimeVersion, javaVendor, javaRuntimeVersion, javaRuntimeVersion).majorVersion != 17) {
                spec.recipe(Recipe.noop())
            }
        }},
        java("""
            public record MyRecord(
               boolean bar,
               String foo
            ) {
               public MyRecord {
                  if (foo == null) {
                      foo = "defaultValue";
                  }
              }
            }
        """)
    )
}
