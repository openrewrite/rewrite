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

namespace OpenRewrite.Tests.Core;

/// <summary>
/// <c>SearchResult.Found</c> resolves <c>WithMarkers</c> reflectively and used to return the node
/// unchanged when the type had none — the same silent no-op <c>Markup.AddMarker</c> and
/// <c>J.SetPrefix</c> already throw for. <c>ParseError</c> was the one in-tree type without the
/// wither, so marking a parse failure quietly reported nothing.
/// </summary>
public class SearchResultTests
{
    [Fact]
    public void FoundOnParseErrorAttachesTheMarker()
    {
        var error = ParseError.Build("broken.cs", "class {", new Exception("boom"));

        var found = SearchResult.Found(error, "failed to parse");

        Assert.NotSame(error, found);
        var marker = Assert.Single(found.Markers.MarkerList.OfType<SearchResult>());
        Assert.Equal("failed to parse", marker.Description);
        Assert.Equal(error.Id, found.Id);
        Assert.Equal(error.SourcePath, found.SourcePath);
        Assert.Equal(error.Text, found.Text);
    }

    [Fact]
    public void FoundOnJNodeAttachesTheMarker()
    {
        var empty = new Empty(Guid.NewGuid(), Space.Empty, Markers.Empty);

        var found = SearchResult.Found(empty, "here");

        var marker = Assert.Single(found.Markers.MarkerList.OfType<SearchResult>());
        Assert.Equal("here", marker.Description);
        Assert.Equal(empty.Id, found.Id);
    }

    [Fact]
    public void FoundThrowsWhenTheNodeHasNoWithMarkers()
    {
        var ex = Assert.Throws<InvalidOperationException>(
            () => SearchResult.Found(new WitherlessTree()));
        Assert.Contains("WithMarkers", ex.Message);
        Assert.Contains(nameof(WitherlessTree), ex.Message);
    }

    /// <summary>A non-J node that declares no withers, standing in for a future modelling gap.</summary>
    private sealed class WitherlessTree : global::OpenRewrite.Core.Tree
    {
        public Guid Id { get; } = Guid.NewGuid();
        public Markers Markers => Markers.Empty;
        public global::OpenRewrite.Core.Tree WithId(Guid id) => this;
    }
}
