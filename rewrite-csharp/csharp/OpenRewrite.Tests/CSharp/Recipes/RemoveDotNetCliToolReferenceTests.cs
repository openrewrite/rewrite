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
using OpenRewrite.CSharp.Recipes;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.CSharp.Recipes;

public class RemoveDotNetCliToolReferenceTests : RewriteTest
{
    [Fact]
    public void RemoveSpecificCliTool()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveDotNetCliToolReference
            {
                ToolName = "Microsoft.DotNet.Watcher.Tools"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <DotNetCliToolReference Include="Microsoft.VisualStudio.Web.CodeGeneration.Tools" Version="2.0.2" />
                    <DotNetCliToolReference Include="Microsoft.DotNet.Watcher.Tools" Version="2.1.0-preview1-27567" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <DotNetCliToolReference Include="Microsoft.VisualStudio.Web.CodeGeneration.Tools" Version="2.0.2" />
                  </ItemGroup>
                </Project>
                """
            )
        );
    }

    [Fact]
    public void RemoveAllCliToolsViaWildcard()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveDotNetCliToolReference
            {
                ToolName = "*"
            }),
            CsProj(
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                  <ItemGroup>
                    <DotNetCliToolReference Include="Microsoft.VisualStudio.Web.CodeGeneration.Tools" Version="2.0.2" />
                    <DotNetCliToolReference Include="Microsoft.DotNet.Watcher.Tools" Version="2.1.0-preview1-27567" />
                  </ItemGroup>
                </Project>
                """,
                """
                <Project Sdk="Microsoft.NET.Sdk">
                  <PropertyGroup>
                    <TargetFramework>net10.0</TargetFramework>
                  </PropertyGroup>
                </Project>
                """
            )
        );
    }
}
