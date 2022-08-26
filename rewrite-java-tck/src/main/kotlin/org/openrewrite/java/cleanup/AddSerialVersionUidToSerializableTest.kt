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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.java.Assertions.java
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

interface AddSerialVersionUidToSerializableTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(AddSerialVersionUidToSerializable())
    }

    @Test
    fun doNothingNotSerializable() = rewriteRun(
        java("""
            public class Example {
                private String fred;
                private int numberOfFreds;
            }
        """)
    )

    @Test
    fun addSerialVersionUID() = rewriteRun(
        java("""
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private String fred;
                private int numberOfFreds;
            }
        """,
        """
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private static final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """)
    )

    @Test
    fun fixSerialVersionUIDModifiers() = rewriteRun(
        java("""
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """,
        """
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private static final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """)
    )

    @Test
    fun fixSerialVersionUIDNoModifiers() = rewriteRun(
        java("""
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """,
        """
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private static final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """)
    )

    @Test
    fun fixSerialVersionUIDNoModifiersWrongType() = rewriteRun(
        java("""
            import java.io.Serializable;

            public class Example implements Serializable {
                Long serialVersionUID = 1L;
                private String fred;
                private int numberOfFreds;
            }
        """,
        """
            import java.io.Serializable;

            public class Example implements Serializable {
                private static final long serialVersionUID = 1L;
                private String fred;
                private int numberOfFreds;
            }
        """)
    )

    @Test
    fun uidAlreadyPresent() = rewriteRun(
        java("""
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private static final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """)
    )

    @Test
    fun methodDeclarationsAreNotVisited() = rewriteRun(
        java("""
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private String fred;
                private int numberOfFreds;
                void doSomething() {
                    int serialVersionUID = 1;
                }
            }
        """,
        """
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private static final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
                void doSomething() {
                    int serialVersionUID = 1;
                }
            }
        """)
    )

    @Test
    fun doNotAlterAnInterface() = rewriteRun(
        java("""
            import java.io.Serializable;
                        
            public interface Example extends Serializable {
            }
        """)
    )

    @Test
    fun doNotAlterAnException() = rewriteRun(
        java("""
            import java.io.Serializable;
                        
            public class MyException extends Exception implements Serializable {
            }
        """)
    )

    @Test
    fun doNotAlterARuntimeException() = rewriteRun(
        java("""
            import java.io.Serializable;
                        
            public class MyException extends RuntimeException implements Serializable {
            }
        """)
    )

    @Test
    fun serializableInnerClass() = rewriteRun(
        java("""
            import java.io.Serializable;
            public class Outer {
                public static class Inner implements Serializable {
                
                }
            }
        """,
        """
            import java.io.Serializable;
            public class Outer {
                public static class Inner implements Serializable {
                    private static final long serialVersionUID = 1;
                
                }
            }
        """)
    )
}
