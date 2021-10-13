package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface AddSerialVersionUidToSerializableTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = AddSerialVersionUidToSerializable()

    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build()

    @Test
    fun doNothingNotSerializable() = assertUnchanged(
        before = """
            public class Example {
                private String fred;
                private int numberOfFreds;
            }
        """.trimIndent()
    )

    @Test
    fun addSerialVersionUID() = assertChanged(
        before = """
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private String fred;
                private int numberOfFreds;
            }
        """.trimIndent(),
        after = """
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private static final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """.trimIndent()
    )

    @Test
    fun fixSerialVersionUIDModifiers() = assertChanged(
        before = """
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """.trimIndent(),
        after = """
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private static final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """.trimIndent()
    )

    @Test
    fun fixSerialVersionUIDNoModifiers() = assertChanged(
        before = """
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """.trimIndent(),
        after = """
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private static final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """.trimIndent()
    )

    @Test
    fun fixSerialVersionUIDNoModifiersWrongType() = assertChanged(
        before = """
            import java.io.Serializable;

            public class Example implements Serializable {
                Long serialVersionUID = 1L;
                private String fred;
                private int numberOfFreds;
            }
        """.trimIndent(),
        after = """
            import java.io.Serializable;

            public class Example implements Serializable {
                private static final long serialVersionUID = 1L;
                private String fred;
                private int numberOfFreds;
            }
        """.trimIndent()
    )

    @Test
    fun uidAlreadyPresent() = assertUnchanged(
        before = """
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private static final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """.trimIndent()
    )

    @Test
    fun doNotAlterAnInterface() = assertUnchanged(
        before = """
            import java.io.Serializable;
                        
            public interface Example extends Serializable {
            }
        """.trimIndent()
    )

    @Test
    fun doNotAlterAnException() = assertUnchanged(
        before = """
            import java.io.Serializable;
                        
            public class MyException extends Exception implements Serializable {
            }
        """.trimIndent()
    )

    @Test
    fun doNotAlterARuntimeException() = assertUnchanged(
        before = """
            import java.io.Serializable;
                        
            public class MyException extends RuntimeException implements Serializable {
            }
        """.trimIndent()
    )

}