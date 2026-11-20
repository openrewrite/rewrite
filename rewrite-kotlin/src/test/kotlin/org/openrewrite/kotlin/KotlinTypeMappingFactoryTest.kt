/*
 * Copyright 2026 the original author or authors.
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
@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)
package org.openrewrite.kotlin

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.internal.DefaultJavaTypeFactory
import org.openrewrite.java.internal.JavaTypeCache
import org.openrewrite.java.tree.Flag
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.JavaType.FullyQualified.Kind
import org.openrewrite.kotlin.tree.K
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer
import javax.tools.ToolProvider

class KotlinTypeMappingFactoryTest {

    @Test
    fun classesReachedThroughACompiledClassComeFromTheFactory(@TempDir classes: Path) {
        compileKotlin(classes, """
            package fixture
            interface Iface
            annotation class Marker
            @Marker class Impl : Iface
        """.trimIndent())
        Files.delete(classes.resolve("fixture/Iface.class"))
        Files.delete(classes.resolve("fixture/Marker.class"))
        val iface = premade("fixture.Iface", Kind.Interface)
        val marker = premade("fixture.Marker", Kind.Annotation)

        val impl = fieldType(parse(classes, "class A { val impl: fixture.Impl? = null }", iface, marker))

        assertThat(impl.interfaces).singleElement().isSameAs(iface)
        assertThat(impl.annotations).singleElement().isSameAs(marker)
    }

    @Test
    fun missingClassesKeepTheirNameExceptAsSupertypes(@TempDir classes: Path) {
        compileKotlin(classes, """
            package fixture
            open class Base
            open class MissingBase
            interface MissingIface
            annotation class MissingMarker
            class MissingType
            @MissingMarker class Impl : Base(), MissingIface { fun missing(): MissingType? = null }
            class Sub : MissingBase()
            class Sub2 : MissingIface
        """.trimIndent())
        for (name in listOf("MissingBase", "MissingIface", "MissingMarker", "MissingType")) {
            Files.delete(classes.resolve("fixture/$name.class"))
        }

        val cu = parse(
            classes,
            "class A { val impl: fixture.Impl? = null; val sub: fixture.Sub? = null; val sub2: fixture.Sub2? = null }"
        )
        val types = fieldTypes(cu)
        val impl = types.getValue("impl")

        val missing = impl.methods.single { it.name == "missing" }.returnType
        assertThat(missing).isInstanceOf(JavaType.Class::class.java)
        assertThat((missing as JavaType.Class).fullyQualifiedName).isEqualTo("fixture.MissingType")
        assertThat(missing.flagsBitMap).isZero()

        assertThat(impl.annotations).extracting<String> { it.fullyQualifiedName }
            .containsExactly("fixture.MissingMarker")

        // Without its class file a missing class cannot be told from a missing interface, so neither is kept.
        assertThat(types.getValue("sub").supertype).isNull()
        assertThat(types.getValue("sub2").supertype).isNull()
        assertThat(types.getValue("sub2").interfaces).isEmpty()
        assertThat(impl.supertype!!.fullyQualifiedName).isEqualTo("fixture.Base")
        assertThat(impl.interfaces).isEmpty()
    }

    @Test
    fun thrownExceptionsComeFromTheFactoryMethodBeforeTheClassFile(@TempDir classes: Path) {
        compileJava(classes, "Thrower", """
            package fixture;
            public class Thrower {
                public void risky(java.util.List<String> names) throws java.io.IOException {}
            }
        """.trimIndent())
        val served = premade("fixture.ServedException", Kind.Class)
        val thrower = premade("fixture.Thrower", Kind.Class)
        val risky = JavaType.Method(
            null, Flag.Public.bitMask, null, "risky", null, arrayOf("names"), null, null, null, null, null
        )
        val listOfString = premade("java.util.List", Kind.Interface)
        risky.unsafeSet(thrower, JavaType.Primitive.Void, listOf(listOfString), listOf(served), null)
        thrower.unsafeSet(null, null, null, null, null, null, listOf(risky))

        val cu = parse(classes, "fun f(t: fixture.Thrower) { t.risky(listOf(\"a\")) }", thrower)
        val invocation = invocationType(cu, "risky")

        assertThat(invocation.thrownExceptions).singleElement().isSameAs(served)
    }

    @Test
    fun thrownExceptionsAreReadWhileTheDeclaringTypeIsStillBeingBuilt(@TempDir classes: Path) {
        compileJava(classes, "Thrower", """
            package fixture;
            public class Thrower {
                public Thrower() throws java.io.IOException {}
            }
        """.trimIndent())

        // The constructor call is the first mention of the class, so building its method type builds the class,
        // whose member list holds this very method still under construction.
        val cu = parse(classes, "fun f() { fixture.Thrower() }")
        var constructor: JavaType.Method? = null
        object : KotlinIsoVisitor<Int>() {
            override fun visitNewClass(newClass: J.NewClass, p: Int): J.NewClass {
                constructor = newClass.methodType
                return super.visitNewClass(newClass, p)
            }
        }.visit(cu, 0)

        assertThat(constructor!!.thrownExceptions).extracting<String> { (it as JavaType.Class).fullyQualifiedName }
            .containsExactly("java.io.IOException")
    }

    @Test
    fun thrownExceptionsMissingFromTheClasspathKeepTheirName(@TempDir classes: Path) {
        compileJava(classes, "MissingException", "package fixture; public class MissingException extends Exception {}")
        compileJava(classes, "Thrower", """
            package fixture;
            public class Thrower {
                public void risky() throws MissingException {}
            }
        """.trimIndent())
        Files.delete(classes.resolve("fixture/MissingException.class"))

        val invocation = invocationType(parse(classes, "fun f(t: fixture.Thrower) { t.risky() }"), "risky")

        assertThat(invocation.thrownExceptions).extracting<String> { (it as JavaType.Class).fullyQualifiedName }
            .containsExactly("fixture.MissingException")
    }

    private fun premade(fqn: String, kind: Kind) =
        JavaType.Class(null, Flag.Public.bitMask, fqn, kind, null, null, null, null, null, null, null)

    private fun parse(classes: Path, source: String, vararg premade: JavaType.Class): K.CompilationUnit {
        val served = premade.associateBy { it.fullyQualifiedName }
        val factory = object : DefaultJavaTypeFactory(JavaTypeCache()) {
            override fun computeClass(fqn: String, flags: Long, kind: Kind, initializer: Consumer<JavaType.Class>) =
                served[fqn] ?: super.computeClass(fqn, flags, kind, initializer)
        }
        val parser = KotlinParser.builder().classpath(listOf(classes)).typeFactory(factory).build()
        return parser.parse(InMemoryExecutionContext { throw it }, source).findFirst().get() as K.CompilationUnit
    }

    private fun fieldType(cu: K.CompilationUnit): JavaType.Class = fieldTypes(cu).values.single()

    private fun fieldTypes(cu: K.CompilationUnit): Map<String, JavaType.Class> {
        val types = LinkedHashMap<String, JavaType.Class>()
        object : KotlinIsoVisitor<Int>() {
            override fun visitVariableDeclarations(
                multiVariable: J.VariableDeclarations, p: Int
            ): J.VariableDeclarations {
                val variable = multiVariable.variables[0]
                types[variable.simpleName] = variable.type as JavaType.Class
                return super.visitVariableDeclarations(multiVariable, p)
            }
        }.visit(cu, 0)
        return types
    }

    private fun invocationType(cu: K.CompilationUnit, name: String): JavaType.Method {
        var type: JavaType.Method? = null
        object : KotlinIsoVisitor<Int>() {
            override fun visitMethodInvocation(method: J.MethodInvocation, p: Int): J.MethodInvocation {
                if (method.simpleName == name) type = method.methodType
                return super.visitMethodInvocation(method, p)
            }
        }.visit(cu, 0)
        return type!!
    }

    private fun compileKotlin(classes: Path, source: String) {
        val result = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("Fixture.kt", source))
            inheritClassPath = true
            messageOutputStream = System.err
        }.compile()
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        result.outputDirectory.walkTopDown().filter { it.isFile }.forEach {
            val target = classes.resolve(it.relativeTo(result.outputDirectory).path)
            Files.createDirectories(target.parent)
            Files.copy(it.toPath(), target)
        }
    }

    private fun compileJava(classes: Path, name: String, source: String) {
        val file = classes.resolve("$name.java")
        Files.writeString(file, source)
        val exit = ToolProvider.getSystemJavaCompiler()
            .run(null, null, null, "-d", classes.toString(), "-cp", classes.toString(), file.toString())
        assertThat(exit).isZero()
    }
}
