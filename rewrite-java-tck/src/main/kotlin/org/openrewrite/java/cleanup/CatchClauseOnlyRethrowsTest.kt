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
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

interface CatchClauseOnlyRethrowsTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = CatchClauseOnlyRethrows()

    @Test
    fun rethrownButWithDifferentMessage() = assertUnchanged(
        before = """
            import java.io.FileReader;
            import java.io.IOException;
            
            class A {
                void foo() throws IOException {
                    try {
                        new FileReader("").read();
                    } catch (IOException e) {
                        throw new IOException("another message", e);
                    } catch(Exception e) {
                        throw new Exception("another message");
                    }
                }
            }
        """
    )

    @Test
    fun catchShouldBePreservedBecauseLessSpecificCatchFollows() = assertUnchanged(
        before = """
            import java.io.FileReader;
            import java.io.IOException;
            
            class A {
                void foo() throws IOException {
                    try {
                        new FileReader("").read();
                    } catch (IOException e) {
                        throw e;
                    } catch(Exception e) {
                        System.out.println(e.getMessage());
                    } catch(Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        """
    )

    @Test
    fun tryCanBeRemoved() = assertChanged(
        before = """
            import java.io.FileReader;
            import java.io.IOException;
            
            class A {
                void foo() throws IOException {
                    try {
                        new FileReader("").read();
                    } catch (IOException e) {
                        throw e;
                    }
                }
            }
        """,
        after = """
            import java.io.FileReader;
            import java.io.IOException;
            
            class A {
                void foo() throws IOException {
                    new FileReader("").read();
                }
            }
        """
    )

    @Test
    fun tryShouldBePreservedBecauseFinally() = assertChanged(
        before = """
            import java.io.FileReader;
            import java.io.IOException;
            
            class A {
                void foo() throws IOException {
                    try {
                        new FileReader("").read();
                    } catch (IOException e) {
                        throw e;
                    } finally {
                        // should be untouched since this might do something
                    }
                }
            }
        """,
        after = """
            import java.io.FileReader;
            import java.io.IOException;
            
            class A {
                void foo() throws IOException {
                    try {
                        new FileReader("").read();
                    } finally {
                        // should be untouched since this might do something
                    }
                }
            }
        """
    )

    @Test
    fun tryShouldBePreservedBecauseResources() = assertChanged(
        before = """
            import java.io.FileReader;
            import java.io.IOException;
            
            class A {
                void foo() throws IOException {
                    try(FileReader fr = new FileReader("")) {
                        fr.read();
                    } catch (IOException e) {
                        throw e;
                    }
                }
            }
        """,
        after = """
            import java.io.FileReader;
            import java.io.IOException;
            
            class A {
                void foo() throws IOException {
                    try(FileReader fr = new FileReader("")) {
                        fr.read();
                    }
                }
            }
        """
    )

    @Test
    fun wrappingAndRethrowingIsUnchanged() = assertUnchanged(
        before = """
            import java.io.FileReader;
            import java.io.IOException;
            
            class A {
                void foo() {
                    try {
                        new FileReader("").read();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        """
    )

    @Test
    fun loggingAndRethrowingIsUnchanged() = assertUnchanged(
        before = """
            import java.io.FileReader;
            import java.io.IOException;
            
            class A {
                void foo() throws IOException {
                    try {
                        new FileReader("").read();
                    } catch (IOException e) {
                        System.out.println("Oh no an exception!!");
                        throw e;
                    }
                }
            }
        """
    )
}
