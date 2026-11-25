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
using OpenRewrite.CSharp;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Java.Search;

/// <summary>
/// Matches generated C# files; see <see cref="GeneratedCode.IsGenerated"/>.
///
/// Mirrors <c>org.openrewrite.csharp.search.FindGeneratedCode</c>; keep the two in sync. Used as the
/// LocalVisitor bundled with the <see cref="RecipeRef"/> returned by <see cref="Preconditions.IsGeneratedCode"/>.
/// </summary>
public class IsGeneratedCode : TreeVisitor<Tree, ExecutionContext>
{
    public override Tree? Visit(Tree? tree, ExecutionContext ctx) =>
        tree is CompilationUnit cu && GeneratedCode.IsGenerated(cu) ? SearchResult.Added(cu) : tree;
}
