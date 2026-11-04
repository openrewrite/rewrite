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
using OpenRewrite.CSharp.Rpc;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// A <c>Print</c> or <c>Visit</c> request for a tree that is not a whole source file names the
/// ancestors of that tree, so that what is printed or visited sees where it is.
/// </summary>
public class CursorRebuildTest
{
    [Fact]
    public async Task AncestorsAreFetchedOutermostFirstAndNestedInnermostLast()
    {
        var sourceFile = Guid.NewGuid().ToString();
        var parent = Guid.NewGuid().ToString();
        var fetched = new List<string>();

        // as Java sends it: the parent, its padding, the source file, then the root
        var cursor = await RewriteRpcServer.RebuildCursorAsync([parent, "1789", sourceFile, "1790"], id =>
        {
            fetched.Add(id);
            return Task.FromResult<object>(id);
        });

        Assert.Equal([sourceFile, parent], fetched);
        Assert.Equal(parent, cursor.Value);
        Assert.Equal(sourceFile, cursor.Parent!.Value);
        Assert.True(cursor.Parent.Parent!.IsRoot);
    }

    [Fact]
    public async Task NoIdsLeaveARootCursor()
    {
        var cursor = await RewriteRpcServer.RebuildCursorAsync(null, _ => throw new InvalidOperationException());

        Assert.True(cursor.IsRoot);
    }

    [Fact]
    public void AVisitorSeesTheCursorItIsGiven()
    {
        var cu = new CSharpParser().Parse("class C { }", sourcePath: "C.cs");
        var classDeclaration = (ClassDeclaration)cu.Members[0].Element;
        var seen = new SeenFrom();

        ((ITreeVisitor<int>)seen).Visit(classDeclaration, 0, new Cursor(new Cursor(), cu));

        Assert.Same(cu, seen.Parent);
    }

    private class SeenFrom : CSharpVisitor<int>
    {
        public object? Parent { get; private set; }

        public override J VisitClassDeclaration(ClassDeclaration classDeclaration, int p)
        {
            Parent = Cursor.Parent!.Value;
            return classDeclaration;
        }
    }
}
