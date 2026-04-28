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
using OpenRewrite.Xml;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.CSharp.Recipes;

/// <summary>
/// Removes <c>&lt;DotNetCliToolReference&gt;</c> entries (matching by glob on the
/// Include attribute) from .csproj files. CLI tool references are obsolete starting
/// with .NET Core 3.0 — they were the netcoreapp2.x mechanism for shipping per-project
/// CLI tools, and have since been replaced by global / local tools and SDK-built-in
/// commands (e.g. <c>dotnet watch</c>).
/// </summary>
public class RemoveDotNetCliToolReference : ScanningRecipe<DotNetBuildContext>
{
    public override string DisplayName => "Remove DotNetCliToolReference";

    public override string Description =>
        "Removes a `<DotNetCliToolReference>` element from .csproj files. " +
        "Use `*` to remove every CLI tool reference.";

    [Option(DisplayName = "Tool name",
        Description = "The CLI tool package name to remove. Supports glob patterns. " +
                      "Use `*` to remove all CLI tool references.",
        Example = "Microsoft.DotNet.Watcher.Tools")]
    public string ToolName { get; set; } = "";

    public override DotNetBuildContext GetInitialValue(ExecutionContext ctx) => DotNetBuildContext.GetOrCreate(ctx);

    public override ITreeVisitor<ExecutionContext> GetScanner(DotNetBuildContext acc) => new BuildContextScanner();

    public override ITreeVisitor<ExecutionContext> GetVisitor(DotNetBuildContext acc)
    {
        return Preconditions.Check(
            new IsProjectFile(),
            new RemoveDotNetCliToolReferenceVisitor(ToolName));
    }
}
