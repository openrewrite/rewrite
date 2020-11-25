package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.RefactorVisitorTest

interface UseGettersAndSettersTest : RefactorVisitorTest {

    @Test
    fun shouldChangePublicFieldIntoGetterAndSetter(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { cu -> UseGettersAndSetters().apply { setClassType("A") } },
            before =
            """
                public class A {
                    public String foo;
                }
            """,
            after =
            """
                public class A {
                    private String foo;

                    public String getFoo() {
                        return foo;
                    }

                    public void setFoo(String value) {
                        foo = value;
                    }
                }
            """
    )

    @Test
    fun shouldUpdateUsagesInDependentClass(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { cu -> UseGettersAndSetters().apply { setClassType("A") } },
            before =
            """
                public class A {
                    public String foo;
                }
                
                public class B {
                    public void useFoo() {
                        A a = new A();
                        a.foo = "bar";
                        String x = a.foo;
                        
                        System.out.println("hi" + a.foo);
                    }
                }
            """,
            after =
            """
                public class A {
                    private String foo;

                    public String getFoo() {
                        return foo;
                    }

                    public void setFoo(String value) {
                        foo = value;
                    }
                }
                
                public class B {
                    public void useFoo() {
                        A a = new A();
                        a.setFoo("bar");
                        String x = a.getFoo();
                        
                        System.out.println("hi" + a.getFoo());
                    }
                }
            """
    )

    @Test
    fun shouldNotAffectClassNotTargeted(jp: JavaParser) = assertUnchanged(
            jp,
            visitorsMapped = listOf { cu -> UseGettersAndSetters().apply { setClassType("B") } },
            before =
            """
                public class A {
                    public String foo;
                }                    
            """
    )
}