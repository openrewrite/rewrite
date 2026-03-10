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
using OpenRewrite.Java;
using static OpenRewrite.Java.J;
using static OpenRewrite.Recipes.Net9.Net9RecipeHelpers;

namespace OpenRewrite.Recipes.Net9;

/// <summary>
/// Finds calls to BinaryReader.ReadString() which returns "\uFFFD" on malformed UTF-8
/// sequences in .NET 9 instead of the previous behavior.
/// </summary>
class BinaryReaderReadStringSearchRecipe : Recipe
{
    public override string DisplayName => "Find `BinaryReader.ReadString` behavior change";

    public override string Description =>
        "Finds calls to `BinaryReader.ReadString()` which now returns the Unicode replacement character " +
        "(\\uFFFD) for malformed UTF-8 byte sequences in .NET 9, instead of the previous behavior. " +
        "Verify your code handles the replacement character correctly.";

    public override IReadOnlySet<string> Tags => new HashSet<string> { "dotnet", "net9", "search" };

    public override JavaVisitor<Core.ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<Core.ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, Core.ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);

            if (mi.Name.SimpleName == "ReadString")
            {
                var isMatch = IsMethodCall(mi.MethodType, "System.IO.BinaryReader", "ReadString");

                if (!isMatch && mi.MethodType == null && mi.Select != null)
                {
                    isMatch = GetExpressionTypeFqn(mi.Select.Element) == "System.IO.BinaryReader";
                }

                if (isMatch)
                {
                    return AddWarnMarker(mi,
                        "BinaryReader.ReadString now returns \\uFFFD for malformed UTF-8 sequences in .NET 9. " +
                        "Verify your code handles the replacement character correctly.");
                }
            }

            return mi;
        }
    }
}
