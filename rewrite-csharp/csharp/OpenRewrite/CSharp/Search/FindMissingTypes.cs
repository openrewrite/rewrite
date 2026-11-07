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
using OpenRewrite.Java;
using static OpenRewrite.CSharp.Recipes.Categories;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.CSharp.Search;

[Category, CSharp, Search]
public class FindMissingTypes : Recipe
{
    public override string DisplayName => "Find missing type information on C# LSTs";

    public override string Description =>
        "A diagnostic recipe that marks where a C# LST lacks type attribution, such as a method declaration or " +
        "invocation without a method type.";

    public override ITreeVisitor<ExecutionContext> GetVisitor() =>
        new Visitor<ExecutionContext>((j, message, _) => SearchResult.Found(j, message));

    private sealed class Visitor<P>(Func<J, string, P, J> missing) : CSharpVisitor<P>
    {
        /// <summary>The parser attributes types in the first branch only.</summary>
        public override J VisitConditionalDirective(ConditionalDirective directive, P p) =>
            directive.WithBranches(ListUtils.Map(directive.Branches, (i, b) => i == 0 ? VisitRightPadded(b, p) : b));

        public override J VisitMethodDeclaration(MethodDeclaration method, P p)
        {
            var md = base.VisitMethodDeclaration(method, p);
            return md is MethodDeclaration { MethodType: null } ? missing(md, "MethodDeclaration type is missing", p) : md;
        }

        public override J VisitMethodInvocation(MethodInvocation mi, P p)
        {
            var visited = base.VisitMethodInvocation(mi, p);
            return visited is MethodInvocation { MethodType: null } invocation && !BoundAtRunTime(invocation)
                ? missing(visited, "MethodInvocation type is missing", p)
                : visited;
        }

        /// <summary>
        /// <c>dynamic</c> maps to an unknown type. A call with a dynamic receiver or argument picks its method at run
        /// time.
        /// </summary>
        private static bool BoundAtRunTime(MethodInvocation mi) =>
            mi.Select?.Element.Type is JavaType.Unknown ||
            mi.Arguments.Elements.Any(a => a.Element.Type is JavaType.Unknown);
    }
}
