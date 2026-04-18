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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.csharp.CSharpParser;
import org.openrewrite.csharp.marker.MSBuildProject;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.tree.ParseError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.openrewrite.csharp.Assertions.csharp;
import static org.openrewrite.csharp.Assertions.csproj;

/**
 * Integration tests for C# project parsing via RPC.
 * Simple round-trip tests use rewriteRun; project-structure tests use direct RPC.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class CSharpParseProjectTest implements RewriteTest {

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
                    .log(Paths.get(System.getProperty("java.io.tmpdir"), "csharp-rpc-project.log"))
                );
                return;
            }
        }
        throw new IllegalStateException("Could not find C# Rewrite project");
    }

    // ---- Round-trip tests via rewriteRun ----

    @Test
    void parseSimpleProject() {
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
          ),
          csharp(
            """
              namespace Test
              {
                  public class Helper
                  {
                      public string GetMessage() => "hello";
                  }
              }
              """
          )
        );
    }

    @Test
    void parseProjectWithNuGetReference() {
        rewriteRun(
          spec -> spec.parser(CSharpParser.builder().assemblyReferences("Newtonsoft.Json@13.0.3")),
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <PropertyGroup>
                  <TargetFramework>net10.0</TargetFramework>
                </PropertyGroup>
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                </ItemGroup>
              </Project>
              """
          ),
          csharp(
            """
              using Newtonsoft.Json;
              
              namespace Test
              {
                  public class Program
                  {
                      public string Serialize(object obj)
                      {
                          return JsonConvert.SerializeObject(obj);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void parseProjectWithPartialClasses() {
        rewriteRun(
          csharp(
            """
              namespace Models
              {
                  public partial class Person
                  {
                      public string FirstName { get; set; }
                      public string LastName { get; set; }
                  }
              }
              """
          ),
          csharp(
            """
              namespace Models
              {
                  public partial class Person
                  {
                      public string GetFullName()
                      {
                          return FirstName + " " + LastName;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void csprojMarkerHasProjectMetadata() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <PropertyGroup>
                  <TargetFramework>net10.0</TargetFramework>
                </PropertyGroup>
                <ItemGroup>
                  <PackageReference Include="Newtonsoft.Json" Version="13.0.3" />
                </ItemGroup>
              </Project>
              """,
            spec -> spec.beforeRecipe(doc -> {
                MSBuildProject marker = doc.getMarkers()
                  .findFirst(MSBuildProject.class)
                  .orElseThrow(() -> new AssertionError("MSBuildProject marker not found"));
                assertThat(marker.getSdk()).isEqualTo("Microsoft.NET.Sdk");
                assertThat(marker.getTargetFrameworks()).hasSize(1);
                assertThat(marker.getTargetFrameworks().getFirst().getTargetFramework()).isEqualTo("net10.0");
                assertThat(marker.getTargetFrameworks().getFirst().getPackageReferences()).hasSize(1);
                assertThat(marker.getTargetFrameworks().getFirst().getPackageReferences().getFirst().getInclude())
                  .isEqualTo("Newtonsoft.Json");
            })
          )
        );
    }

    @Test
    void generatedFilesExcludedFromLst() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <PropertyGroup>
                  <TargetFramework>net10.0</TargetFramework>
                </PropertyGroup>
              </Project>
              """
          ),
          csharp(
            """
              namespace Test
              {
                  public class Program
                  {
                  }
              }
              """,
            spec -> spec.beforeRecipe(doc ->
              assertThat(doc.getSourcePath().toString()).doesNotContain("obj"))
          )
        );
    }

    @Test
    void parseProjectRelativePaths() {
        rewriteRun(
          csproj(
            """
              <Project Sdk="Microsoft.NET.Sdk">
                <PropertyGroup>
                  <TargetFramework>net10.0</TargetFramework>
                </PropertyGroup>
              </Project>
              """
          ),
          csharp(
            """
              namespace MyApp
              {
                  public class Program
                  {
                  }
              }
              """
          )
        );
    }

    @Test
    void xmlDocComment() {
        rewriteRun(
          csharp(
            """
              namespace Test
              {
                  public class Foo
                  {
                      /// <inheritdoc />
                      public void Bar()
                      {
                      }
                  }
              }
              """
          )
        );
    }

    // ---- Full working set sweep ----

    @Tag("workingSet-full")
    @Test
    @Timeout(value = 3600, unit = TimeUnit.SECONDS)
    void parseWorkingSetSolution() throws IOException {
        String rootProperty = System.getProperty("workingSetRoot");
        assumeTrue(rootProperty != null, "System property 'workingSetRoot' not set");
        Path workingSetRoot = Paths.get(rootProperty);
        assumeTrue(Files.isDirectory(workingSetRoot),
          "Working set root not found: " + workingSetRoot);

        List<Path> solutionFiles;
        try (var walk = Files.walk(workingSetRoot)) {
            solutionFiles = walk
              .filter(p -> {
                  String name = p.getFileName().toString().toLowerCase();
                  return name.endsWith(".sln") || name.endsWith(".slnx");
              })
              .sorted()
              .toList();
        }

        System.out.println("Found " + solutionFiles.size() + " solution files under " + workingSetRoot);
        assumeTrue(!solutionFiles.isEmpty(), "No .sln/.slnx files found under " + workingSetRoot);

        int totalFiles = 0;
        int totalParseErrors = 0;
        int totalSolutions = 0;
        int failedSolutions = 0;

        for (int i = 0; i < solutionFiles.size(); i++) {
            Path solutionPath = solutionFiles.get(i);
            Path rootDir = solutionPath.getParent();
            String relative = workingSetRoot.relativize(solutionPath).toString();

            System.out.println("\n[" + (i + 1) + "/" + solutionFiles.size() + "] Parsing: " + relative);
            System.out.flush();

            CSharpRewriteRpc.shutdownCurrent();
            CSharpRewriteRpc rpc = CSharpRewriteRpc.getOrStart();

            var ctx = new InMemoryExecutionContext(t -> {
                System.err.println("  Execution error: " + t.getMessage());
                t.printStackTrace(System.err);
            });

            try {
                List<SourceFile> sourceFiles = rpc.parseSolution(solutionPath, rootDir, ctx).toList();

                int parseErrors = 0;
                for (SourceFile sf : sourceFiles) {
                    if (sf instanceof ParseError pe) {
                        parseErrors++;
                        System.err.println("  PARSE ERROR: " + sf.getSourcePath());
                        System.err.println("    " + pe.getText());
                    }
                }

                totalFiles += sourceFiles.size();
                totalParseErrors += parseErrors;
                totalSolutions++;

                System.out.println("  OK: " + sourceFiles.size() + " files" +
                                   (parseErrors > 0 ? ", " + parseErrors + " parse errors" : ""));
            } catch (Exception e) {
                failedSolutions++;
                System.err.println("  FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }
            System.out.flush();
            System.err.flush();
        }

        CSharpRewriteRpc.shutdownCurrent();

        System.out.println("\n========== SUMMARY ==========");
        System.out.println("Solutions found:  " + solutionFiles.size());
        System.out.println("Solutions parsed: " + totalSolutions);
        System.out.println("Solutions failed: " + failedSolutions);
        System.out.println("Total files:      " + totalFiles);
        System.out.println("Parse errors:     " + totalParseErrors);
        System.out.println("=============================");

        assertThat(failedSolutions).as("Solutions that failed to parse").isZero();
        assertThat(totalParseErrors).as("Total parse errors across all solutions").isZero();
    }
}
