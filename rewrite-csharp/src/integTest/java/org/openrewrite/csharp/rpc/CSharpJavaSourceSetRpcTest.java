/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.csharp.rpc;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JavaSourceSet} rides on every source file in a Java source set, resource files included,
 * so it reaches the C# peer through {@code Xml$Document} even though C# never sees a Java
 * compilation unit. Without a codec on the C# side it resolves to {@code UnknownMarker}, which
 * consumes one message where Java sends many, and the queue then desynchronizes.
 */
@Tag("slow")
@Timeout(value = 180, unit = TimeUnit.SECONDS)
class CSharpJavaSourceSetRpcTest {

    @BeforeAll
    static void setUpFactory() {
        Path basePath = Paths.get(System.getProperty("user.dir"));
        Path[] searchPaths = {
          basePath.resolve("csharp"),
          basePath.resolve("rewrite-csharp/csharp"),
        };
        for (Path searchPath : searchPaths) {
            Path csproj = searchPath.resolve("OpenRewrite.Tool/OpenRewrite.Tool.csproj");
            if (csproj.toFile().exists()) {
                CSharpRewriteRpc.setFactory(
                  CSharpRewriteRpc.builder()
                    .csharpServerEntry(csproj.toAbsolutePath().normalize())
                    .log(Paths.get(System.getProperty("java.io.tmpdir"), "csharp-rpc-java-source-set.log")));
                return;
            }
        }
        throw new IllegalStateException("Could not find C# Rewrite project");
    }

    @AfterAll
    static void tearDown() {
        CSharpRewriteRpc.shutdownCurrent();
    }

    @Test
    void javaSourceSetMarkerAcrossRpcBoundary() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        Xml.Document document = XmlParser.builder().build()
          .parse(ctx, "<project><name>test</name></project>")
          .findFirst()
          .map(Xml.Document.class::cast)
          .orElseThrow();

        JavaType.FullyQualified a = JavaType.ShallowClass.build("com.example.A");
        JavaType.FullyQualified b = JavaType.ShallowClass.build("com.example.B");
        JavaSourceSet sourceSet = new JavaSourceSet(Tree.randomId(), "main",
          List.of(JavaType.ShallowClass.build("java.lang.String"), a, b),
          Map.of("com.example:example:1.0", List.of(a, b)));

        String printed = CSharpRewriteRpc.getOrStart()
          .print(document.withMarkers(document.getMarkers().add(sourceSet)));

        assertThat(printed).isEqualTo("<project><name>test</name></project>");
    }
}
