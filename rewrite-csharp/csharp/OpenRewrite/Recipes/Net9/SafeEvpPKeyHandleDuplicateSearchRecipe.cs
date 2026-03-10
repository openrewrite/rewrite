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
/// Finds calls to SafeEvpPKeyHandle.DuplicateHandle(). In .NET 9, this method now
/// increments the reference count (up-refs) the handle instead of creating a deep copy,
/// which may affect handle lifetime management.
/// </summary>
class SafeEvpPKeyHandleDuplicateSearchRecipe : Recipe
{
    private const string SafeEvpPKeyHandleFqn = "System.Security.Cryptography.SafeEvpPKeyHandle";

    public override string DisplayName => "Find `SafeEvpPKeyHandle.DuplicateHandle` up-ref change";

    public override string Description =>
        "Finds calls to `SafeEvpPKeyHandle.DuplicateHandle()`. In .NET 9, this method now " +
        "increments the reference count instead of creating a deep copy, which may affect handle lifetime.";

    public override IReadOnlySet<string> Tags => new HashSet<string> { "dotnet", "net9", "search", "cryptography" };

    public override JavaVisitor<Core.ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<Core.ExecutionContext>
    {
        private const string TodoMessage =
            "SafeEvpPKeyHandle.DuplicateHandle now up-refs the handle in .NET 9 instead of deep copying. " +
            "The original and duplicate now share the same underlying key. " +
            "Disposing one may affect the other.";

        public override J VisitMethodInvocation(MethodInvocation mi, Core.ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);

            if (mi.Name.SimpleName != "DuplicateHandle")
                return mi;

            var isMatch = IsMethodCall(mi.MethodType, SafeEvpPKeyHandleFqn, "DuplicateHandle");

            if (!isMatch && mi.MethodType == null && mi.Select != null)
            {
                isMatch = GetExpressionTypeFqn(mi.Select.Element) == SafeEvpPKeyHandleFqn;
            }

            if (isMatch)
            {
                return AddWarnMarker(mi, TodoMessage);
            }

            return mi;
        }
    }
}
