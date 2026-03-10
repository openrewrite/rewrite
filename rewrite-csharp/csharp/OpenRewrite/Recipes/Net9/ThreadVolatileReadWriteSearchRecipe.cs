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
/// Finds usages of Thread.VolatileRead and Thread.VolatileWrite which are obsolete
/// in .NET 9 (SYSLIB0054). Use Volatile.Read or Volatile.Write instead.
/// </summary>
class ThreadVolatileReadWriteSearchRecipe : Recipe
{
    private static readonly HashSet<string> ObsoleteMethods = new()
    {
        "VolatileRead",
        "VolatileWrite"
    };

    public override string DisplayName =>
        "Find `Thread.VolatileRead`/`VolatileWrite` usage (SYSLIB0054)";

    public override string Description =>
        "Finds usages of `Thread.VolatileRead` and `Thread.VolatileWrite` which are obsolete in .NET 9 " +
        "(SYSLIB0054). Use `Volatile.Read` or `Volatile.Write` instead.";

    public override IReadOnlySet<string> Tags => new HashSet<string> { "dotnet", "net9", "search", "threading" };

    public override JavaVisitor<Core.ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<Core.ExecutionContext>
    {
        public override J VisitMethodInvocation(MethodInvocation mi, Core.ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);

            if (ObsoleteMethods.Contains(mi.Name.SimpleName))
            {
                var isMatch = IsMethodCall(mi.MethodType, "System.Threading.Thread", mi.Name.SimpleName);

                if (!isMatch && mi.MethodType == null && mi.Select != null)
                {
                    isMatch = GetExpressionTypeFqn(mi.Select.Element) == "System.Threading.Thread";
                }

                if (isMatch)
                {
                    var replacement = mi.Name.SimpleName == "VolatileRead" ? "Volatile.Read" : "Volatile.Write";
                    return AddWarnMarker(mi,
                        $"Thread.{mi.Name.SimpleName} is obsolete in .NET 9 (SYSLIB0054). " +
                        $"Use {replacement} instead.");
                }
            }

            return mi;
        }
    }
}
