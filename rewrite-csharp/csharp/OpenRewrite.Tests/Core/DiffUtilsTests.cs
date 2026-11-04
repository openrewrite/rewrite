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
namespace OpenRewrite.Tests.Core;

public class DiffUtilsTests
{
    [Fact]
    public void IdenticalInputsProduceEmptyDiff()
    {
        var result = OpenRewrite.Core.DiffUtils.UnifiedDiff("hello\nworld\n", "hello\nworld\n", "test.cs");
        Assert.Equal("", result);
    }

    [Fact]
    public void SingleLineChange()
    {
        var before = "line1\nline2\nline3\n";
        var after = "line1\nmodified\nline3\n";
        var result = OpenRewrite.Core.DiffUtils.UnifiedDiff(before, after, "test.cs");

        Assert.Contains("--- a/test.cs", result);
        Assert.Contains("+++ b/test.cs", result);
        Assert.Contains("-line2", result);
        Assert.Contains("+modified", result);
        // Context lines should be present
        Assert.Contains(" line1", result);
        Assert.Contains(" line3", result);
    }

    [Fact]
    public void AddedLines()
    {
        var before = "line1\nline3\n";
        var after = "line1\nline2\nline3\n";
        var result = OpenRewrite.Core.DiffUtils.UnifiedDiff(before, after, "test.cs");

        Assert.Contains("+line2", result);
        Assert.DoesNotContain("-line", result.Replace("--- a/test.cs", ""));
    }

    [Fact]
    public void RemovedLines()
    {
        var before = "line1\nline2\nline3\n";
        var after = "line1\nline3\n";
        var result = OpenRewrite.Core.DiffUtils.UnifiedDiff(before, after, "test.cs");

        Assert.Contains("-line2", result);
    }

    [Fact]
    public void MultipleHunks()
    {
        // Changes far enough apart to produce separate hunks (>6 lines apart with 3-line context)
        var lines = new List<string>();
        for (var i = 0; i < 20; i++)
            lines.Add($"line{i}");

        var beforeLines = new List<string>(lines);
        var afterLines = new List<string>(lines);
        afterLines[2] = "changed2";
        afterLines[17] = "changed17";

        var before = string.Join("\n", beforeLines) + "\n";
        var after = string.Join("\n", afterLines) + "\n";
        var result = OpenRewrite.Core.DiffUtils.UnifiedDiff(before, after, "test.cs");

        Assert.Contains("-line2", result);
        Assert.Contains("+changed2", result);
        Assert.Contains("-line17", result);
        Assert.Contains("+changed17", result);
        // Should have two @@ headers
        var hhCount = result.Split("@@").Length - 1;
        Assert.True(hhCount >= 4, $"Expected at least 2 hunk headers (4 @@ markers), got {hhCount / 2} hunks");
    }

    [Fact]
    public void EmptyBeforeProducesAllAdditions()
    {
        var result = OpenRewrite.Core.DiffUtils.UnifiedDiff("", "line1\nline2\n", "test.cs");

        Assert.Contains("+line1", result);
        Assert.Contains("+line2", result);
    }

    [Fact]
    public void EmptyAfterProducesAllDeletions()
    {
        var result = OpenRewrite.Core.DiffUtils.UnifiedDiff("line1\nline2\n", "", "test.cs");

        Assert.Contains("-line1", result);
        Assert.Contains("-line2", result);
    }
}
