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
/// Finds usages of obsolete X509Certificate2 and X509Certificate constructors (SYSLIB0057).
/// These constructors that accept binary content or file paths are obsolete starting in .NET 9.
/// Use X509CertificateLoader methods instead.
/// </summary>
class X509CertificateConstructorsSearchRecipe : Recipe
{
    private static readonly HashSet<string> AffectedTypes = new()
    {
        "System.Security.Cryptography.X509Certificates.X509Certificate2",
        "System.Security.Cryptography.X509Certificates.X509Certificate"
    };

    public override string DisplayName =>
        "Find obsolete `X509Certificate2`/`X509Certificate` constructors (SYSLIB0057)";

    public override string Description =>
        "Finds usages of `X509Certificate2` and `X509Certificate` constructors that accept binary content " +
        "or file paths, which are obsolete in .NET 9 (SYSLIB0057). Use `X509CertificateLoader` methods instead.";

    public override IReadOnlySet<string> Tags => new HashSet<string> { "dotnet", "net9", "search", "cryptography" };

    public override JavaVisitor<Core.ExecutionContext> GetVisitor() => new Visitor();

    private class Visitor : CSharpVisitor<Core.ExecutionContext>
    {
        public override J VisitNewClass(NewClass newClass, Core.ExecutionContext ctx)
        {
            newClass = (NewClass)base.VisitNewClass(newClass, ctx);

            if (newClass.ConstructorType?.DeclaringType is JavaType.Class cls &&
                AffectedTypes.Contains(cls.FullyQualifiedName))
            {
                return AddWarnMarker(newClass,
                    "X509Certificate/X509Certificate2 constructors for binary and file content are obsolete " +
                    "in .NET 9 (SYSLIB0057). Use X509CertificateLoader methods instead.");
            }

            return newClass;
        }
    }
}
