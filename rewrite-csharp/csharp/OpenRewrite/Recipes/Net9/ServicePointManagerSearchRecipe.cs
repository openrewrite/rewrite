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
/// Finds usages of ServicePointManager which is fully obsolete in .NET 9 (SYSLIB0014).
/// Settings on ServicePointManager don't affect SslStream or HttpClient.
/// </summary>
class ServicePointManagerSearchRecipe : Recipe
{
    private const string ServicePointManagerFqn = "System.Net.ServicePointManager";

    public override string DisplayName => "Find `ServicePointManager` usage (SYSLIB0014)";

    public override string Description =>
        "Finds usages of `ServicePointManager` which is fully obsolete in .NET 9 (SYSLIB0014). " +
        "Settings on `ServicePointManager` don't affect `SslStream` or `HttpClient`.";

    public override IReadOnlySet<string> Tags => new HashSet<string> { "dotnet", "net9", "search", "networking" };

    public override JavaVisitor<Core.ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<Core.ExecutionContext>
    {
        private const string TodoMessage =
            "ServicePointManager is fully obsolete in .NET 9 (SYSLIB0014). " +
            "Its settings don't affect SslStream or HttpClient. " +
            "Configure TLS settings directly on HttpClientHandler or SslStream instead.";

        public override J VisitFieldAccess(FieldAccess fa, Core.ExecutionContext ctx)
        {
            fa = (FieldAccess)base.VisitFieldAccess(fa, ctx);

            if (fa.Target is Identifier id &&
                id.Type is JavaType.Class cls &&
                cls.FullyQualifiedName == ServicePointManagerFqn)
            {
                return AddWarnMarker(fa, TodoMessage);
            }

            return fa;
        }

        public override J VisitMethodInvocation(MethodInvocation mi, Core.ExecutionContext ctx)
        {
            mi = (MethodInvocation)base.VisitMethodInvocation(mi, ctx);

            if (IsMethodCall(mi.MethodType, ServicePointManagerFqn, mi.Name.SimpleName))
            {
                return AddWarnMarker(mi, TodoMessage);
            }

            return mi;
        }
    }
}
