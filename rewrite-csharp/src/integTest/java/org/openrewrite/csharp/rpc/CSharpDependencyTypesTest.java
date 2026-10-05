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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.java.tree.JavaType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end test of the DependencyTypes RPC: the C# engine resolves a NuGet coordinate to
 * its assemblies, enumerates them into {@code JavaType}, and the Java side reconstructs the
 * result off the wire.
 */
@Timeout(value = 180, unit = TimeUnit.SECONDS)
class CSharpDependencyTypesTest {

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
                    .log(Paths.get(System.getProperty("java.io.tmpdir"), "csharp-rpc-exported-types.log"))
                );
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
    void enumeratesNewtonsoftJsonConvertOverRpc() {
        Path dll = Paths.get(System.getProperty("user.home"),
          ".nuget", "packages", "newtonsoft.json", "13.0.3", "lib", "net6.0", "Newtonsoft.Json.dll");
        assumeTrue(dll.toFile().exists(), "Newtonsoft.Json 13.0.3 not in the NuGet cache");

        CSharpRewriteRpc rpc = CSharpRewriteRpc.getOrStart();
        List<JavaType.FullyQualified> types = new ArrayList<>();
        Set<String> ownFqns = new LinkedHashSet<>();
        rpc.dependencyTypes(new Dependency("Newtonsoft.Json", "13.0.3", "net6.0"), ownFqns::addAll, types::add);

        // The own-FQN list is delivered before the types, so the writer's predicate is known up front.
        assertThat(ownFqns).contains("Newtonsoft.Json.JsonConvert");

        JavaType.Class jsonConvert = types.stream()
          .filter(JavaType.Class.class::isInstance)
          .map(JavaType.Class.class::cast)
          .filter(c -> "Newtonsoft.Json.JsonConvert".equals(c.getFullyQualifiedName()))
          .findFirst()
          .orElseThrow(() -> new AssertionError("Newtonsoft.Json.JsonConvert not enumerated"));

        assertThat(jsonConvert.getMethods())
          .extracting(JavaType.Method::getName)
          .contains("SerializeObject", "DeserializeObject");

        // Every enumerated top-level type has a distinct FQN (no writer-rejected duplicates).
        long distinct = types.stream()
          .filter(JavaType.Class.class::isInstance)
          .map(t -> ((JavaType.Class) t).getFullyQualifiedName())
          .distinct().count();
        long total = types.stream().filter(JavaType.Class.class::isInstance).count();
        assertThat(distinct).isEqualTo(total);
    }
}
