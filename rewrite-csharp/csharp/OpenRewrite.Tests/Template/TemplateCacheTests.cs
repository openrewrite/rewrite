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
using OpenRewrite.CSharp.Template;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.Template;

public class TemplateCacheTests
{
    [Fact]
    public void AnonymousCapturesShareOneCacheEntry()
    {
        for (var i = 0; i < 10; i++)
        {
            BuildPattern().GetTree();
        }

        Assert.Single(TemplateEngine.CacheKeys, k => k.Contains("WhereProbeA"));

        static CSharpPattern BuildPattern()
        {
            var source = Capture.Expression(type: "IEnumerable<T>", typeParameters: ["T"]);
            var predicate = Capture.Expression();
            return CSharpPattern.Expression($"{source}.WhereProbeA({predicate}).FirstProbeA()");
        }
    }

    [Fact]
    public void CapturesKeepTheirNamesInTheParsedTemplate()
    {
        var source = Capture.Expression();
        var predicate = Capture.Expression();
        var tree = CSharpPattern.Expression($"{source}.WhereProbeB({predicate}).FirstProbeB()").GetTree();

        var names = new PlaceholderCollector();
        names.Visit(tree, 0);

        Assert.Contains(source.Name, names.Found);
        Assert.Contains(predicate.Name, names.Found);
    }

    private sealed class PlaceholderCollector : JavaVisitor<int>
    {
        internal List<string> Found { get; } = [];

        public override J VisitIdentifier(Identifier identifier, int p)
        {
            if (Placeholder.FromPlaceholder(identifier.SimpleName) is { } name)
            {
                Found.Add(name);
            }
            return base.VisitIdentifier(identifier, p);
        }
    }
}
