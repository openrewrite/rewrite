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
/// Precondition visitor that matches .csproj files with an MSBuildProject marker.
/// Use with Preconditions.Check() to scope recipes to .NET projects.
/// </summary>
public class IsProjectFile : XmlVisitor<ExecutionContext>
{
    public override Xml.Xml VisitDocument(Document document, ExecutionContext ctx)
    {
        if (document.Markers.FindFirst<MSBuildProject>() != null)
        {
            return document.WithMarkers(
                document.Markers.Add(new SearchResult(Guid.NewGuid(), null)));
        }
        return document;
    }
}
