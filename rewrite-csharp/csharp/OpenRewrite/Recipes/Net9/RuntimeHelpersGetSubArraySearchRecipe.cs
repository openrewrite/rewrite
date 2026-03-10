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
/// Finds calls to RuntimeHelpers.GetSubArray() which returns a different type in .NET 9.
/// Code that depends on the runtime type of the returned array may break.
/// </summary>
class RuntimeHelpersGetSubArraySearchRecipe : Recipe
{
    private const string RuntimeHelpersFqn = "System.Runtime.CompilerServices.RuntimeHelpers";

    public override string DisplayName => "Find `RuntimeHelpers.GetSubArray` return type change";

    public override string Description =>
        "Finds calls to `RuntimeHelpers.GetSubArray()` which may return a different array type in .NET 9. " +
        "Code that depends on the runtime type of the returned array may break.";

    public override IReadOnlySet<string> Tags => new HashSet<string> { "dotnet", "net9", "search" };

    public override JavaVisitor<Core.ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<Core.ExecutionContext>
    {
        private const string TodoMessage =
            "RuntimeHelpers.GetSubArray may return a different array type in .NET 9. " +
            "Code that checks the runtime type of the returned array or casts it may break.";

        public override J VisitMethodInvocation(MethodInvocation mi, Core.ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);

            if (mi.Name.SimpleName != "GetSubArray")
                return mi;

            var isMatch = IsMethodCall(mi.MethodType, RuntimeHelpersFqn, "GetSubArray");

            if (!isMatch && mi.MethodType == null && mi.Select != null)
            {
                isMatch = GetExpressionTypeFqn(mi.Select.Element) == RuntimeHelpersFqn;
            }

            if (isMatch)
            {
                return AddWarnMarker(mi, TodoMessage);
            }

            return mi;
        }
    }
}
