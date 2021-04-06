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
package org.openrewrite.java.internal.typegeneration

import org.assertj.core.api.Assertions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.objectweb.asm.*
import org.objectweb.asm.util.ASMifier
import org.objectweb.asm.util.TraceClassVisitor
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.marker.GitProvenance
import java.io.PrintWriter
import java.lang.StringBuilder
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

interface TypeExtractionVisitorTest : JavaRecipeTest {

    @Test
    fun printClass() {

        //val reader = ClassReader("org.openrewrite.internal.lang.Nullable")

        //val reader = ClassReader("org.jetbrains.annotations.NotNull")
        //val reader = ClassReader("org.openrewrite.marker.Markable")
        val reader = ClassReader("org.openrewrite.marker.Markers")
        //val reader = ClassReader("org.openrewrite.SourceFile")
        //val reader = ClassReader("io.micrometer.core.instrument.Timer\$Builder")
        //val reader = ClassReader("org.openrewrite.marker.GitProvenance")

        val writer = PrintWriter(System.out)

        val classWriter = skippingVisitor(TraceClassVisitor(ClassWriter(0), writer))
        System.out.println("ASM: ")
        reader.accept(classWriter, 0)
        writer.flush()

    }

    @Test
    fun testCompliationUsingExtractedTypes(jp : JavaParser.Builder<*, *>) {
        @Language("java") val source = """
                package org.example;
                
                import org.openrewrite.internal.StringUtils;
                import java.util.SortedMap;
                import java.util.TreeMap;
                
                public class Example {
                    public void foo() {
                        SortedMap<Integer, Long> sortedMap = new TreeMap<>();
                        sortedMap.put(1, 2);
                        StringUtils.mostCommonIndent(sortedMap);
                    }
                }
        """.trimIndent()

        //First parser uses the runtime classpath.
        val originalAst  = jp
            .logCompilationWarningsAndErrors(true)
            .build()
            .parse(InMemoryExecutionContext { obj: Throwable -> obj.printStackTrace() }, source)[0]

        val typeInformation = TypeInformation()
        TypeExtractionVisitor().visit(originalAst, typeInformation)
        typeInformation.typesAsByteCode

        //typeInformation.typeMap.keys.stream().map { t -> t.fullyQualifiedName }.forEach(this::printAsm)

        val compilationUnit  = jp
            .logCompilationWarningsAndErrors(true)
            .build()
            .parse(InMemoryExecutionContext { obj: Throwable -> obj.printStackTrace() }, source)[0]

        Assertions.assertThat(compilationUnit).isNotNull
        //typeInformation.getTypesAsByteCode()

    }
    @Test
    fun testTypeExtraction() {
        val rewriteRoot: Path = Paths.get(
            TypeExtractionVisitorTest::class.java.getResource("./").toURI()
        ).resolve("../../../../../../../../../").normalize()

        val inputs = Arrays.asList(
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/lang/Nullable.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/lang/NullUtils.java"),
            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/MetricsHelper.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/ListUtils.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/StringUtils.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/PropertyPlaceholderHelper.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/internal/RecipeIntrospectionUtils.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/Tree.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/ExecutionContext.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/InMemoryExecutionContext.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/marker/Marker.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/marker/Markers.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/style/Style.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/DeclarativeNamedStyles.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/style/NamedStyles.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/TreePrinter.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/Option.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/OptionDescriptor.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/RecipeDescriptor.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/Result.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/SourceFile.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/Recipe.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/Validated.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/DeclarativeRecipe.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/ResourceLoader.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/YamlResourceLoader.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/ClasspathScanningLoader.java"),
//            rewriteRoot.resolve("rewrite-core/src/main/java/org/openrewrite/config/RecipeIntrospectionException.java")
        )

        val compliationUnit  = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build()
            .parse(inputs, null, InMemoryExecutionContext { obj: Throwable -> obj.printStackTrace() })[0]

        val typeInformation = TypeInformation()
        TypeExtractionVisitor()
            .visit(compliationUnit, typeInformation)

        typeInformation.getTypesAsByteCode()

        //typeInformation.typeMap.keys.stream().map { t -> t.fullyQualifiedName }.forEach(this::printAsm)

    }

    fun printAsm(className : String) {
        val names = className.split(".")

        var cName = ""
        for(name in names.reversed()) {
            if (!cName.isEmpty() && name[0].isUpperCase()) {
                cName = name + "$" + cName
            } else if (!cName.isEmpty()) {
                cName = name + "." + cName
            } else {
                cName = name
            }
        }
        ASMifier.main(arrayOf(cName))

    }
    fun skippingVisitor(visitor : ClassVisitor) : ClassVisitor {
        return object : ClassVisitor(Opcodes.ASM9, visitor) {
            override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
                return null
            }

            override fun visitField(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                value: Any?,
            ): FieldVisitor {
                return object: FieldVisitor(Opcodes.ASM9, super.visitField(access, name, descriptor, signature, value)) {
                    override fun visitTypeAnnotation(
                        typeRef: Int,
                        typePath: TypePath?,
                        descriptor: String?,
                        visible: Boolean,
                    ): AnnotationVisitor? {
                        return null
                    }

                    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
                        return null
                    }
                }
            }

            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?,
            ): MethodVisitor? {
                return null
            }
        }
    }
}