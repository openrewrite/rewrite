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

namespace OpenRewrite.CSharp;

/// <summary>
/// A reusable scanner visitor that captures build-related XML documents
/// (csproj, props, targets, nuget.config, etc.) as raw LSTs into a
/// <see cref="DotNetBuildContext"/> stored in the <see cref="ExecutionContext"/>.
///
/// Used automatically by <see cref="Recipes.CsProjRecipe"/> in its scan phase.
/// Can also be composed into custom scanners for recipes that extend
/// <see cref="ScanningRecipe{T}"/> directly.
/// </summary>
public class BuildContextScanner : XmlVisitor<ExecutionContext>
{
    public override Xml.Xml VisitDocument(Document document, ExecutionContext ctx)
    {
        DotNetBuildContext.GetOrCreate(ctx).CaptureIfBuildFile(document);
        return document;
    }
}
