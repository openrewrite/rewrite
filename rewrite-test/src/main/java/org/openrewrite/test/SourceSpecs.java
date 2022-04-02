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
import org.openrewrite.java.tree.J;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.text.PlainText;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.yaml.tree.Yaml;

import java.util.function.Consumer;

public interface SourceSpecs extends Iterable<SourceSpec<?>> {
    default SourceSpecs dir(String dir, SourceSpecs... sources) {
        return dir(dir, s -> {
        }, sources);
    }

    default SourceSpecs dir(String dir, Consumer<SourceSpec<SourceFile>> spec, SourceSpecs... sources) {
        return new Dir(dir, spec, sources);
    }

    default SourceSpecs java(@Language("java") String before) {
        return java(before, s -> {
        });
    }

    default SourceSpecs java(@Language("java") String before, Consumer<SourceSpec<J.CompilationUnit>> spec) {
        SourceSpec<J.CompilationUnit> java = new SourceSpec<>(J.CompilationUnit.class, null, before, null);
        spec.accept(java);
        return java;
    }

    default SourceSpecs java(@Language("java") String before, @Language("java") String after) {
        return java(before, after, s -> {
        });
    }

    default SourceSpecs java(@Language("java") String before, @Language("java") String after,
                             Consumer<SourceSpec<J.CompilationUnit>> spec) {
        SourceSpec<J.CompilationUnit> java = new SourceSpec<>(J.CompilationUnit.class, null, before, after);
        spec.accept(java);
        return java;
    }

    default SourceSpecs pomXml(@Language("xml") String before) {
        return pomXml(before, s -> {
        });
    }

    default SourceSpecs pomXml(@Language("xml") String before, Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> maven = new SourceSpec<>(Xml.Document.class, "maven", before, null);
        spec.accept(maven);
        return maven;
    }

    default SourceSpecs pomXml(@Language("xml") String before, @Language("xml") String after) {
        return pomXml(before, after, s -> {
        });
    }

    default SourceSpecs pomXml(@Language("xml") String before, @Language("xml") String after,
                               Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> maven = new SourceSpec<>(Xml.Document.class, "maven", before, after);
        spec.accept(maven);
        return maven;
    }

    default SourceSpecs yaml(@Language("yml") String before) {
        return yaml(before, s -> {
        });
    }

    default SourceSpecs yaml(@Language("yml") String before, Consumer<SourceSpec<Yaml.Documents>> spec) {
        SourceSpec<Yaml.Documents> yaml = new SourceSpec<>(Yaml.Documents.class, null, before, null);
        spec.accept(yaml);
        return yaml;
    }

    default SourceSpecs yaml(@Language("yml") String before, @Language("yml") String after) {
        return yaml(before, after, s -> {
        });
    }

    default SourceSpecs yaml(@Language("yml") String before, @Language("yml") String after,
                             Consumer<SourceSpec<Yaml.Documents>> spec) {
        SourceSpec<Yaml.Documents> yaml = new SourceSpec<>(Yaml.Documents.class, null, before, after);
        spec.accept(yaml);
        return yaml;
    }

    default SourceSpecs properties(@Language("properties") String before) {
        return properties(before, s -> {
        });
    }

    default SourceSpecs properties(@Language("properties") String before, Consumer<SourceSpec<PlainText>> spec) {
        SourceSpec<PlainText> properties = new SourceSpec<>(PlainText.class, null, before, null);
        spec.accept(properties);
        return properties;
    }

    default SourceSpecs properties(@Language("properties") String before, @Language("properties") String after) {
        return properties(before, after, s -> {
        });
    }

    default SourceSpecs properties(@Language("properties") String before, @Language("properties") String after,
                             Consumer<SourceSpec<Properties.File>> spec) {
        SourceSpec<Properties.File> properties = new SourceSpec<>(Properties.File.class, null, before, after);
        spec.accept(properties);
        return properties;
    }

    default SourceSpecs text(String before) {
        return text(before, s -> {
        });
    }

    default SourceSpecs text(String before, Consumer<SourceSpec<PlainText>> spec) {
        SourceSpec<PlainText> text = new SourceSpec<>(PlainText.class, null, before, null);
        spec.accept(text);
        return text;
    }

    default SourceSpecs text(String before, String after) {
        return text(before, after, s -> {
        });
    }

    default SourceSpecs text(String before, String after,
                                   Consumer<SourceSpec<PlainText>> spec) {
        SourceSpec<PlainText> text = new SourceSpec<>(PlainText.class, null, before, after);
        spec.accept(text);
        return text;
    }

    default SourceSpecs mavenProject(String project, SourceSpecs... sources) {
        return dir(project, spec -> spec.java().project(project), sources);
    }

    default SourceSpecs srcMainJava(SourceSpecs... javaSources) {
        return dir("src/main/java", spec -> spec.java().sourceSet("main"), javaSources);
    }

    default SourceSpecs srcMainResources(SourceSpecs... resources) {
        return dir("src/main/resources", spec -> spec.java().sourceSet("main"), resources);
    }

    default SourceSpecs srcTestJava(SourceSpecs... javaSources) {
        return dir("src/test/java", spec -> spec.java().sourceSet("test"), javaSources);
    }

    default SourceSpecs srcTestResources(SourceSpecs... resources) {
        return dir("src/test/resources", spec -> spec.java().sourceSet("test"), resources);
    }
}
