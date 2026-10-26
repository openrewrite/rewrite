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
package org.openrewrite.csharp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * The RPC server the {@code csharpBuild} task builds. Tests start its assembly rather than its
 * project, which {@code dotnet run} would evaluate again for every process it starts.
 */
public class CSharpServer {

    private CSharpServer() {
    }

    /**
     * The module is the working directory under Gradle and the repository root when a run is
     * launched from there.
     */
    public static Path entry() {
        Path base = Paths.get(System.getProperty("user.dir"));
        return Stream.of(base.resolve("csharp"), base.resolve("rewrite-csharp/csharp"))
          .map(dir -> dir.resolve("OpenRewrite.Tool/bin/Debug/net10.0/OpenRewrite.Tool.dll"))
          .filter(Files::exists)
          .findFirst()
          .map(dll -> dll.toAbsolutePath().normalize())
          .orElseThrow(() -> new IllegalStateException(
            "Could not find the C# RPC server; build it with ./gradlew :rewrite-csharp:csharpBuild"));
    }
}
