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
/// Finds usages of AuthenticationManager which is not supported in .NET 9 (SYSLIB0009).
/// Methods will no-op or throw PlatformNotSupportedException.
/// </summary>
class AuthenticationManagerSearchRecipe : Recipe
{
    private const string AuthenticationManagerFqn = "System.Net.AuthenticationManager";

    public override string DisplayName => "Find `AuthenticationManager` usage (SYSLIB0009)";

    public override string Description =>
        "Finds usages of `AuthenticationManager` which is not supported in .NET 9 (SYSLIB0009). " +
        "Methods will no-op or throw `PlatformNotSupportedException`.";

    public override IReadOnlySet<string> Tags => new HashSet<string> { "dotnet", "net9", "search", "networking" };

    public override JavaVisitor<Core.ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<Core.ExecutionContext>
    {
        private const string TodoMessage =
            "AuthenticationManager is not supported in .NET 9 (SYSLIB0009). " +
            "Methods will no-op or throw PlatformNotSupportedException. " +
            "Use HttpClientHandler or CredentialCache for authentication.";

        public override J VisitFieldAccess(FieldAccess fa, Core.ExecutionContext ctx)
        {
            fa = (FieldAccess)base.VisitFieldAccess(fa, ctx);

            if (fa.Target is Identifier id &&
                id.Type is JavaType.Class cls &&
                cls.FullyQualifiedName == AuthenticationManagerFqn)
            {
                return AddWarnMarker(fa, TodoMessage);
            }

            return fa;
        }

        public override J VisitMethodInvocation(MethodInvocation mi, Core.ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);

            if (IsMethodCall(mi.MethodType, AuthenticationManagerFqn, mi.Name.SimpleName))
            {
                return AddWarnMarker(mi, TodoMessage);
            }

            return mi;
        }
    }
}
