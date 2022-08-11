/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.test;

import org.intellij.lang.annotations.Language;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.quark.Quark;
import org.openrewrite.quark.QuarkParser;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.util.function.Consumer;

@SuppressWarnings("unused")
public interface SourceSpecs extends Iterable<SourceSpec<?>> {
    default SourceSpecs dir(String dir, SourceSpecs... sources) {
        return dir(dir, s -> {
        }, sources);
    }

    default SourceSpecs dir(String dir, Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... sources) {
        return new Dir(dir, spec, sources);
    }

    default SourceSpecs java(@Language("java") @Nullable String before) {
        return java(before, s -> {
        });
    }

    default ParserSupplier javaParserSupplier() {
        return new ParserSupplier(J.CompilationUnit.class, "java", () -> JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(true)
                .build());
    }

    default SourceSpecs java(@Language("java") @Nullable String before, Consumer<SourceSpec<J.CompilationUnit>> spec) {
        SourceSpec<J.CompilationUnit> java = new SourceSpec<>(J.CompilationUnit.class, null, javaParserSupplier(), before, null);
        spec.accept(java);
        return java;
    }

    default SourceSpecs java(@Language("java") @Nullable String before, @Language("java") String after) {
        return java(before, after, s -> {
        });
    }

    default SourceSpecs java(@Language("java") @Nullable String before, @Language("java") String after,
                             Consumer<SourceSpec<J.CompilationUnit>> spec) {
        SourceSpec<J.CompilationUnit> java = new SourceSpec<>(J.CompilationUnit.class, null, javaParserSupplier(), before, after);
        spec.accept(java);
        return java;
    }

    default SourceSpecs other(@Nullable String before) {
        return other(before, s -> {
        });
    }

    default SourceSpecs other(@Nullable String before, Consumer<SourceSpec<Quark>> spec) {
        SourceSpec<Quark> quark = new SourceSpec<>(Quark.class, null, new ParserSupplier(Quark.class, "other", QuarkParser::new), before, null);
        spec.accept(quark);
        return quark;
    }

    default SourceSpecs text(@Nullable String before) {
        return text(before, s -> {
        });
    }

    default SourceSpecs text(@Nullable String before, Consumer<SourceSpec<PlainText>> spec) {
        SourceSpec<PlainText> text = new SourceSpec<>(PlainText.class, null, new ParserSupplier(PlainText.class, "text", PlainTextParser::new), before, null);
        spec.accept(text);
        return text;
    }

    default SourceSpecs text(@Nullable String before, String after) {
        return text(before, after, s -> {
        });
    }

    default SourceSpecs text(@Nullable String before, String after,
                             Consumer<SourceSpec<PlainText>> spec) {
        SourceSpec<PlainText> text = new SourceSpec<>(PlainText.class, null, new ParserSupplier(PlainText.class, "text", PlainTextParser::new), before, after);
        spec.accept(text);
        return text;
    }

    default SourceSpecs mavenProject(String project, Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... sources) {
        return dir(project, spec, sources);
    }

    default SourceSpecs mavenProject(String project, SourceSpecs... sources) {
        return mavenProject(project, spec -> spec.java().project(project), sources);
    }

    default SourceSpecs srcMainJava(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... javaSources) {
        return dir("src/main/java", spec, javaSources);
    }

    default SourceSpecs srcMainJava(SourceSpecs... javaSources) {
        return srcMainJava(spec -> spec.java().sourceSet("main"), javaSources);
    }

    default SourceSpecs srcMainResources(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... resources) {
        return dir("src/main/resources", spec, resources);
    }

    default SourceSpecs srcMainResources(SourceSpecs... resources) {
        return srcMainResources(spec -> spec.java().sourceSet("main"), resources);
    }

    default SourceSpecs srcTestJava(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... javaSources) {
        return dir("src/test/java", spec, javaSources);
    }

    default SourceSpecs srcTestJava(SourceSpecs... javaSources) {
        return srcTestJava(spec -> spec.java().sourceSet("test"), javaSources);
    }

    default SourceSpecs srcTestResources(Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... resources) {
        return dir("src/test/resources", spec, resources);
    }

    default SourceSpecs srcTestResources(SourceSpecs... resources) {
        return srcTestResources(spec -> spec.java().sourceSet("test"), resources);
    }
}
