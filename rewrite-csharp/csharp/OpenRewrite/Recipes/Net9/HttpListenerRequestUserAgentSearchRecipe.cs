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
/// Finds accesses to HttpListenerRequest.UserAgent which is now nullable in .NET 9.
/// Code that previously assumed UserAgent was non-null may throw NullReferenceException.
/// </summary>
class HttpListenerRequestUserAgentSearchRecipe : Recipe
{
    public override string DisplayName => "Find `HttpListenerRequest.UserAgent` nullable change";

    public override string Description =>
        "Finds accesses to `HttpListenerRequest.UserAgent` which changed from `string` to `string?` in .NET 9. " +
        "Code that assumes `UserAgent` is non-null may throw `NullReferenceException`.";

    public override IReadOnlySet<string> Tags => new HashSet<string> { "dotnet", "net9", "search", "networking" };

    public override JavaVisitor<Core.ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<Core.ExecutionContext>
    {
        public override J VisitFieldAccess(FieldAccess fa, Core.ExecutionContext ctx)
        {
            fa = (FieldAccess)base.VisitFieldAccess(fa, ctx);

            if (fa.Name.Element.SimpleName == "UserAgent")
            {
                var targetType = GetExpressionTypeFqn(fa.Target);
                if (targetType == "System.Net.HttpListenerRequest")
                {
                    return AddWarnMarker(fa,
                        "HttpListenerRequest.UserAgent is now nullable (string?) in .NET 9. " +
                        "Add null checks before using this property.");
                }
            }

            return fa;
        }
    }
}
