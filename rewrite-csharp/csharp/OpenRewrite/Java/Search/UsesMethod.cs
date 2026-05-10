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
/// Match files using a specific method by walking the tree for
/// <see cref="MethodInvocation"/> nodes and consulting
/// <see cref="MethodMatcher"/>.
///
/// Mirrors <c>org.openrewrite.java.search.HasMethod</c>. Used as the
/// LocalVisitor bundled with the <see cref="RecipeRef"/> returned by
/// <c>UsesMethod</c> / <c>FindMethods</c> so unit tests without an
/// active RPC connection still see real filtering.
///
/// The <c>methodPattern</c> follows the OpenRewrite method-pattern
/// syntax: <c>&lt;receiver-type&gt; &lt;method-name&gt;(&lt;args&gt;)</c>
/// — e.g. <c>"*..* tostring(..)"</c> or
/// <c>"java.util.Collections emptyList()"</c>.
/// </summary>
public class UsesMethod : JavaVisitor<ExecutionContext>
{
    private readonly MethodMatcher _matcher;
    private bool _found;

    public UsesMethod(string methodPattern, bool matchOverrides = false)
    {
        _matcher = new MethodMatcher(methodPattern, matchOverrides);
    }

    public override J? Visit(Tree? tree, ExecutionContext ctx)
    {
        if (tree is SourceFile sf)
        {
            _found = false;
            base.Visit(tree, ctx);
            if (_found)
            {
                return SearchResult.Found(sf) as J;
            }
            return sf as J;
        }
        return base.Visit(tree, ctx);
    }

    public override J VisitMethodInvocation(MethodInvocation mi, ExecutionContext ctx)
    {
        if (!_found && _matcher.Matches(mi))
        {
            _found = true;
        }
        return base.VisitMethodInvocation(mi, ctx);
    }
}
