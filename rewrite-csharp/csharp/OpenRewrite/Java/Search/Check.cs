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
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Java.Search;

/// <summary>
/// A visitor wrapper that runs a precondition check before the actual visitor.
/// If the precondition modifies the tree (indicating a match), the inner visitor runs.
/// Otherwise the tree is returned unchanged.
/// </summary>
public class Check : JavaVisitor<ExecutionContext>
{
    public ITreeVisitor<ExecutionContext> Precondition { get; }
    public JavaVisitor<ExecutionContext> Visitor { get; }

    internal Check(ITreeVisitor<ExecutionContext> precondition, JavaVisitor<ExecutionContext> visitor)
    {
        Precondition = precondition;
        Visitor = visitor;
    }

    public override J? PreVisit(J tree, ExecutionContext ctx)
    {
        StopAfterPreVisit();
        var result = Precondition.Visit(tree, ctx);
        if (result != tree)
        {
            return (J?)Visitor.Visit(tree, ctx);
        }
        return tree;
    }
}
