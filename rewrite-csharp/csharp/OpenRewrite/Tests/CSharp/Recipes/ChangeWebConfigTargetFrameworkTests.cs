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
using OpenRewrite.Core;
using OpenRewrite.CSharp.Recipes;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.CSharp.Recipes;

public class ChangeWebConfigTargetFrameworkTests(RpcFixture fixture) : RpcRewriteTest(fixture)
{
    private static SourceSpec WebConfig(string before, string? after = null, string sourcePath = "web.config") =>
        new(before, after, sourcePath, "org.openrewrite.xml.tree.Xml$Document");

    private static SourceSpec AppConfig(string before, string? after = null, string sourcePath = "app.config") =>
        new(before, after, sourcePath, "org.openrewrite.xml.tree.Xml$Document");

    [Fact]
    public void ChangeHttpRuntimeTargetFramework()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeWebConfigTargetFramework
            {
                OldVersion = "4.7.2",
                NewVersion = "4.8"
            }),
            WebConfig(
                """
                <configuration>
                  <system.web>
                    <httpRuntime targetFramework="4.7.2" />
                  </system.web>
                </configuration>
                """,
                """
                <configuration>
                  <system.web>
                    <httpRuntime targetFramework="4.8" />
                  </system.web>
                </configuration>
                """
            )
        );
    }

    [Fact]
    public void ChangeCompilationTargetFramework()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeWebConfigTargetFramework
            {
                OldVersion = "4.7.2",
                NewVersion = "4.8"
            }),
            WebConfig(
                """
                <configuration>
                  <system.web>
                    <compilation targetFramework="4.7.2" debug="true" />
                  </system.web>
                </configuration>
                """,
                """
                <configuration>
                  <system.web>
                    <compilation targetFramework="4.8" debug="true" />
                  </system.web>
                </configuration>
                """
            )
        );
    }

    [Fact]
    public void ChangeBothHttpRuntimeAndCompilationInOneRun()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeWebConfigTargetFramework
            {
                OldVersion = "4.6.2",
                NewVersion = "4.7.2"
            }),
            WebConfig(
                """
                <configuration>
                  <system.web>
                    <compilation targetFramework="4.6.2" debug="true" />
                    <httpRuntime targetFramework="4.6.2" />
                  </system.web>
                </configuration>
                """,
                """
                <configuration>
                  <system.web>
                    <compilation targetFramework="4.7.2" debug="true" />
                    <httpRuntime targetFramework="4.7.2" />
                  </system.web>
                </configuration>
                """
            )
        );
    }

    [Fact]
    public void AcceptsVersionWithVPrefix()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeWebConfigTargetFramework
            {
                OldVersion = "v4.7.2",
                NewVersion = "v4.8"
            }),
            WebConfig(
                """
                <configuration>
                  <system.web>
                    <httpRuntime targetFramework="4.7.2" />
                  </system.web>
                </configuration>
                """,
                """
                <configuration>
                  <system.web>
                    <httpRuntime targetFramework="4.8" />
                  </system.web>
                </configuration>
                """
            )
        );
    }

    [Fact]
    public void AlsoAppliesToAppConfig()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeWebConfigTargetFramework
            {
                OldVersion = "4.7.2",
                NewVersion = "4.8"
            }),
            AppConfig(
                """
                <configuration>
                  <system.web>
                    <httpRuntime targetFramework="4.7.2" />
                  </system.web>
                </configuration>
                """,
                """
                <configuration>
                  <system.web>
                    <httpRuntime targetFramework="4.8" />
                  </system.web>
                </configuration>
                """
            )
        );
    }

    [Fact]
    public void NoChangeWhenVersionDoesNotMatch()
    {
        RewriteRun(
            spec => spec.SetRecipe(new ChangeWebConfigTargetFramework
            {
                OldVersion = "4.6.2",
                NewVersion = "4.8"
            }),
            WebConfig(
                """
                <configuration>
                  <system.web>
                    <httpRuntime targetFramework="4.7.2" />
                  </system.web>
                </configuration>
                """
            )
        );
    }

    [Fact]
    public void NoChangeWhenAttributeAbsent()
    {
        // <httpRuntime> without a targetFramework attribute (uses runtime default).
        RewriteRun(
            spec => spec.SetRecipe(new ChangeWebConfigTargetFramework
            {
                OldVersion = "4.7.2",
                NewVersion = "4.8"
            }),
            WebConfig(
                """
                <configuration>
                  <system.web>
                    <httpRuntime maxRequestLength="4096" />
                  </system.web>
                </configuration>
                """
            )
        );
    }
}
