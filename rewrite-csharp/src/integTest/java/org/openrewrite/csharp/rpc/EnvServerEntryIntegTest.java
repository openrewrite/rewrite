/*
 * Copyright 2025 the original author or authors.
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.openrewrite.csharp.Assertions.csharp;

/**
 * Proves the REWRITE_DOTNET_RPC_SERVER env-var fallback: the factory intentionally does
 * NOT set csharpServerEntry, so the only way the C# RPC server can launch is via the
 * env-var path added to {@code CSharpRewriteRpc.Builder.get()}. Runs only when the env
 * var is set (point it at OpenRewrite.Tool.csproj).
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
@EnabledIfEnvironmentVariable(named = "REWRITE_DOTNET_RPC_SERVER", matches = ".+")
class EnvServerEntryIntegTest implements RewriteTest {

    @BeforeAll
    static void setUpFactory() {
        // No csharpServerEntry on purpose — exercise the REWRITE_DOTNET_RPC_SERVER fallback.
        CSharpRewriteRpc.setFactory(
          CSharpRewriteRpc.builder()
            .log(Paths.get(System.getProperty("java.io.tmpdir"), "csharp-rpc-envserver.log")));
    }

    @Test
    void parseViaEnvServerEntry() {
        rewriteRun(
          csharp(
            """
              namespace Test
              {
                  public class Program
                  {
                      public static void Main(string[] args)
                      {
                      }
                  }
              }
              """
          )
        );
    }
}
