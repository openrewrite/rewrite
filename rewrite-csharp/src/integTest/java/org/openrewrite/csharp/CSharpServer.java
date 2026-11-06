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

/**
 * The RPC server the {@code csharpBuild} task builds. Tests start its assembly rather than its
 * project, which {@code dotnet run} would evaluate again for every process it starts.
 */
public final class CSharpServer {

    private CSharpServer() {
    }

    public static Path entry() {
        Path assembly = CSharpPrinterParityTest.csharpDir().resolve("OpenRewrite.Tool/bin/Debug/net10.0/OpenRewrite.Tool.dll");
        if (!Files.exists(assembly)) {
            throw new IllegalStateException("No C# RPC server at " + assembly +
                                            "; build it with ./gradlew :rewrite-csharp:csharpBuild");
        }
        return assembly;
    }
}
