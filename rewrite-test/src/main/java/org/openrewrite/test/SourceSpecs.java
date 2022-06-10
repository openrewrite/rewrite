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
import org.openrewrite.groovy.tree.G;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.json.tree.Json;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.protobuf.tree.Proto;
import org.openrewrite.quark.Quark;
import org.openrewrite.text.PlainText;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Paths;
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

    default SourceSpecs java(@Language("java") @Nullable String before, Consumer<SourceSpec<J.CompilationUnit>> spec) {
        SourceSpec<J.CompilationUnit> java = new SourceSpec<>(J.CompilationUnit.class, null, before, null);
        spec.accept(java);
        return java;
    }

    default SourceSpecs java(@Language("java") @Nullable String before, @Language("java") String after) {
        return java(before, after, s -> {
        });
    }

    default SourceSpecs java(@Language("java") @Nullable String before, @Language("java") String after,
                             Consumer<SourceSpec<J.CompilationUnit>> spec) {
        SourceSpec<J.CompilationUnit> java = new SourceSpec<>(J.CompilationUnit.class, null, before, after);
        spec.accept(java);
        return java;
    }

    default SourceSpecs plainText(@Nullable String before) {
        return plainText(before, s -> {
        });
    }

    default SourceSpecs plainText(@Nullable String before, Consumer<SourceSpec<PlainText>> spec) {
        SourceSpec<PlainText> plainText = new SourceSpec<>(PlainText.class, null, before, null);
        spec.accept(plainText);
        return plainText;
    }

    default SourceSpecs plainText(@Nullable String before, String after) {
        return plainText(before, after, s -> {
        });
    }

    default SourceSpecs plainText(@Nullable String before, String after, Consumer<SourceSpec<PlainText>> spec) {
        SourceSpec<PlainText> plainText = new SourceSpec<>(PlainText.class, null, before, after);
        spec.accept(plainText);
        return plainText;
    }

    default SourceSpecs pomXml(@Language("xml") @Nullable String before) {
        return pomXml(before, s -> {
        });
    }

    default SourceSpecs pomXml(@Language("xml") @Nullable String before, Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> maven = new SourceSpec<>(Xml.Document.class, "maven", before, null);
        maven.path("pom.xml");
        spec.accept(maven);
        return maven;
    }

    default SourceSpecs pomXml(@Language("xml") @Nullable String before, @Language("xml") String after) {
        return pomXml(before, after, s -> {
        });
    }

    default SourceSpecs pomXml(@Language("xml") @Nullable String before, @Language("xml") String after,
                               Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> maven = new SourceSpec<>(Xml.Document.class, "maven", before, after);
        maven.path("pom.xml");
        spec.accept(maven);
        return maven;
    }

    default SourceSpecs buildGradle(@Language("gradle") @Nullable String before) {
        return buildGradle(before, s -> {
        });
    }

    default SourceSpecs buildGradle(@Language("gradle") @Nullable String before, Consumer<SourceSpec<G.CompilationUnit>> spec) {
        SourceSpec<G.CompilationUnit> gradle = new SourceSpec<>(G.CompilationUnit.class, "gradle", before, null);
        gradle.path(Paths.get("build.gradle"));
        spec.accept(gradle);
        return gradle;
    }

    default SourceSpecs buildGradle(@Language("gradle") @Nullable String before, @Language("gradle") String after) {
        return buildGradle(before, after, s -> {
        });
    }

    default SourceSpecs buildGradle(@Language("gradle") @Nullable String before, @Language("gradle") String after,
                                    Consumer<SourceSpec<G.CompilationUnit>> spec) {
        SourceSpec<G.CompilationUnit> gradle = new SourceSpec<>(G.CompilationUnit.class, "gradle", before, after);
        gradle.path("build.gradle");
        spec.accept(gradle);
        return gradle;
    }

    default SourceSpecs xml(@Language("xml") @Nullable String before) {
        return xml(before, s -> {
        });
    }

    default SourceSpecs xml(@Language("xml") @Nullable String before, Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> xml = new SourceSpec<>(Xml.Document.class, null, before, null);
        spec.accept(xml);
        return xml;
    }

    default SourceSpecs xml(@Language("xml") @Nullable String before, @Language("yml") String after) {
        return xml(before, after, s -> {
        });
    }

    default SourceSpecs xml(@Language("xml") @Nullable String before, @Language("yml") String after,
                             Consumer<SourceSpec<Xml.Document>> spec) {
        SourceSpec<Xml.Document> xml = new SourceSpec<>(Xml.Document.class, null, before, after);
        spec.accept(xml);
        return xml;
    }

    default SourceSpecs yaml(@Language("yml") @Nullable String before) {
        return yaml(before, s -> {
        });
    }

    default SourceSpecs yaml(@Language("yml") @Nullable String before, Consumer<SourceSpec<Yaml.Documents>> spec) {
        SourceSpec<Yaml.Documents> yaml = new SourceSpec<>(Yaml.Documents.class, null, before, null);
        spec.accept(yaml);
        return yaml;
    }

    default SourceSpecs yaml(@Language("yml") @Nullable String before, @Language("yml") String after) {
        return yaml(before, after, s -> {
        });
    }

    default SourceSpecs yaml(@Language("yml") @Nullable String before, @Language("yml") String after,
                             Consumer<SourceSpec<Yaml.Documents>> spec) {
        SourceSpec<Yaml.Documents> yaml = new SourceSpec<>(Yaml.Documents.class, null, before, after);
        spec.accept(yaml);
        return yaml;
    }

    default SourceSpecs properties(@Language("properties") @Nullable String before) {
        return properties(before, s -> {
        });
    }

    default SourceSpecs properties(@Language("properties") @Nullable String before, Consumer<SourceSpec<PlainText>> spec) {
        SourceSpec<PlainText> properties = new SourceSpec<>(PlainText.class, null, before, null);
        spec.accept(properties);
        return properties;
    }

    default SourceSpecs properties(@Language("properties") @Nullable String before, @Language("properties") String after) {
        return properties(before, after, s -> {
        });
    }

    default SourceSpecs properties(@Language("properties") @Nullable String before, @Language("properties") String after,
                                   Consumer<SourceSpec<Properties.File>> spec) {
        SourceSpec<Properties.File> properties = new SourceSpec<>(Properties.File.class, null, before, after);
        spec.accept(properties);
        return properties;
    }

    default SourceSpecs json(@Language("json") @Nullable String before) {
        return json(before, s -> {
        });
    }

    default SourceSpecs json(@Language("json") @Nullable String before, Consumer<SourceSpec<Json.Document>> spec) {
        SourceSpec<Json.Document> json = new SourceSpec<>(Json.Document.class, null, before, null);
        spec.accept(json);
        return json;
    }

    default SourceSpecs json(@Language("json") @Nullable String before, @Language("json") String after) {
        return json(before, after, s -> {
        });
    }

    default SourceSpecs json(@Language("json") @Nullable String before, @Language("json") String after,
                             Consumer<SourceSpec<Json.Document>> spec) {
        SourceSpec<Json.Document> json = new SourceSpec<>(Json.Document.class, null, before, after);
        spec.accept(json);
        return json;
    }

    default SourceSpecs proto(@Language("protobuf") @Nullable String before) {
        return proto(before, s -> {
        });
    }

    default SourceSpecs proto(@Language("protobuf") @Nullable String before, Consumer<SourceSpec<Proto.Document>> spec) {
        SourceSpec<Proto.Document> proto = new SourceSpec<>(Proto.Document.class, null, before, null);
        spec.accept(proto);
        return proto;
    }

    default SourceSpecs proto(@Language("protobuf") @Nullable String before, @Language("protobuf") String after) {
        return proto(before, after, s -> {
        });
    }

    default SourceSpecs proto(@Language("protobuf") @Nullable String before, @Language("protobuf") String after,
                              Consumer<SourceSpec<Proto.Document>> spec) {
        SourceSpec<Proto.Document> proto = new SourceSpec<>(Proto.Document.class, null, before, after);
        spec.accept(proto);
        return proto;
    }

    default SourceSpecs groovy(@Language("groovy") @Nullable String before) {
        return groovy(before, s -> {
        });
    }

    default SourceSpecs groovy(@Language("groovy") @Nullable String before, Consumer<SourceSpec<G.CompilationUnit>> spec) {
        SourceSpec<G.CompilationUnit> groovy = new SourceSpec<>(G.CompilationUnit.class, null, before, null);
        spec.accept(groovy);
        return groovy;
    }

    default SourceSpecs groovy(@Language("groovy") @Nullable String before, @Language("groovy") String after) {
        return groovy(before, after, s -> {
        });
    }

    default SourceSpecs groovy(@Language("groovy") @Nullable String before, @Language("groovy") String after,
                               Consumer<SourceSpec<G.CompilationUnit>> spec) {
        SourceSpec<G.CompilationUnit> groovy = new SourceSpec<>(G.CompilationUnit.class, null, before, after);
        spec.accept(groovy);
        return groovy;
    }

    default SourceSpecs hcl(@Nullable String before) {
        return hcl(before, s -> {
        });
    }

    default SourceSpecs hcl(@Nullable String before, Consumer<SourceSpec<Hcl.ConfigFile>> spec) {
        SourceSpec<Hcl.ConfigFile> hcl = new SourceSpec<>(Hcl.ConfigFile.class, null, before, null);
        spec.accept(hcl);
        return hcl;
    }

    default SourceSpecs hcl(@Nullable String before, String after) {
        return hcl(before, after, s -> {
        });
    }

    default SourceSpecs hcl(@Nullable String before, String after, Consumer<SourceSpec<Hcl.ConfigFile>> spec) {
        SourceSpec<Hcl.ConfigFile> hcl = new SourceSpec<>(Hcl.ConfigFile.class, null, before, after);
        spec.accept(hcl);
        return hcl;
    }

    default SourceSpecs other(@Nullable String before) {
        return other(before, s -> {
        });
    }

    default SourceSpecs other(@Nullable String before, Consumer<SourceSpec<Quark>> spec) {
        SourceSpec<Quark> quark = new SourceSpec<>(Quark.class, null, before, null);
        spec.accept(quark);
        return quark;
    }

    default SourceSpecs text(@Nullable String before) {
        return text(before, s -> {
        });
    }

    default SourceSpecs text(@Nullable String before, Consumer<SourceSpec<PlainText>> spec) {
        SourceSpec<PlainText> text = new SourceSpec<>(PlainText.class, null, before, null);
        spec.accept(text);
        return text;
    }

    default SourceSpecs text(@Nullable String before, String after) {
        return text(before, after, s -> {
        });
    }

    default SourceSpecs text(@Nullable String before, String after,
                             Consumer<SourceSpec<PlainText>> spec) {
        SourceSpec<PlainText> text = new SourceSpec<>(PlainText.class, null, before, after);
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
