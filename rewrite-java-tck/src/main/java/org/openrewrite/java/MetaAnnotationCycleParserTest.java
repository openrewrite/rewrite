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
package org.openrewrite.java;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Markup;
import org.openrewrite.tree.ParseError;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lives in the TCK so it runs against every javac-driven parser version
 * (Java 8/11/17/21/25) via each module's {@code compatibilityTest} suite.
 * <p>
 * Reproducer for the type-mapping recursion cycle that fires when a
 * classpath-loaded annotation has an element method annotated with itself
 * (e.g. Spring's {@code @AliasFor}). javac's {@code ClassReader} populates the
 * loaded {@code MethodSymbol}'s declaration attributes eagerly, completing the
 * cycle before type mapping runs; in-source declarations defer annotation
 * attribute resolution so the cycle never closes there.
 */
class MetaAnnotationCycleParserTest {

    @Test
    void parsesImportOfSelfAnnotatedAnnotation(@TempDir Path tempDir) throws Exception {
        // Mirrors the shape of Spring's @AliasFor: an annotation whose element methods
        // are themselves annotated with the annotation, AND the meta-annotations carry
        // explicit attribute values so compound.values is non-empty and the
        // annotationType -> methodDeclarationType lookup actually closes the cycle.
        @Language("java")
        String selfAnnotation = "package x;\n" +
                "import java.lang.annotation.Retention;\n" +
                "import java.lang.annotation.RetentionPolicy;\n" +
                "@Retention(RetentionPolicy.RUNTIME)\n" +
                "public @interface Self {\n" +
                "    @Self(value = \"attribute\", attribute = \"value\") String value() default \"\";\n" +
                "    @Self(value = \"value\", attribute = \"attribute\") String attribute() default \"\";\n" +
                "}\n";

        Path jar = compileToJar(tempDir, "x", "Self", selfAnnotation);

        @Language("java")
        String userSource = "package y;\n" +
                "import x.Self;\n" +
                "@Self\n" +
                "public class UsesSelf {\n" +
                "}\n";

        List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .classpath(Collections.singletonList(jar))
                .build()
                .parse(userSource)
                .collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        SourceFile sf = parsed.get(0);
        if (sf instanceof ParseError) {
            // Before the computeMethod migration: javac's ClassReader eagerly populates the
            // meta-annotated MethodSymbol's declarationAttributes, listAnnotations ->
            // annotationType -> methodDeclarationType re-enters methodFor on the same
            // signature, and the supplier recurses unbounded -> StackOverflowError.
            throw new AssertionError("Parse failed (expected on unmigrated code): "
                    + ((ParseError) sf).getMarkers().findFirst(Markup.Error.class)
                    .map(Markup.Error::getDetail).orElse("<no detail>"));
        }
        J.CompilationUnit cu = (J.CompilationUnit) sf;
        assertThat(cu.getClasses()).hasSize(1);
        assertThat(cu.getClasses().get(0).getType().getFullyQualifiedName()).isEqualTo("y.UsesSelf");
    }

    private static Path compileToJar(Path tempDir, String pkg, String simpleName, String source) throws IOException {
        Path srcDir = tempDir.resolve("src");
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);

        Path pkgDir = srcDir.resolve(pkg.replace('.', '/'));
        Files.createDirectories(pkgDir);
        Path src = pkgDir.resolve(simpleName.concat(".java"));
        Files.write(src, source.getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int rc = compiler.run(null, null, null,
                "-d", classesDir.toString(),
                src.toString());
        assertThat(rc).isEqualTo(0);

        Path jar = tempDir.resolve("self.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar));
             Stream<Path> walk = Files.walk(classesDir)) {
            walk.filter(p -> p.toString().endsWith(".class")).forEach(classFile -> {
                try {
                    String entry = classesDir.relativize(classFile).toString().replace('\\', '/');
                    jos.putNextEntry(new JarEntry(entry));
                    jos.write(Files.readAllBytes(classFile));
                    jos.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        return jar;
    }
}
