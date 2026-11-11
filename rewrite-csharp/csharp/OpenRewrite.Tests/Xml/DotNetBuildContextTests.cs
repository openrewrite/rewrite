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
using OpenRewrite.CSharp;
using OpenRewrite.Xml;

namespace OpenRewrite.Tests.Xml;

public class DotNetBuildContextTests : IDisposable
{
    private readonly string temp = Path.Combine(Path.GetTempPath(), Guid.NewGuid().ToString());
    public DotNetBuildContextTests() => Directory.CreateDirectory(temp);
    public void Dispose() => Directory.Delete(temp, true);

    [Theory]
    [InlineData(false, false, false)]
    [InlineData(false, true, false)]
    [InlineData(true, false, false)]
    [InlineData(true, true, false)]
    [InlineData(false, false, true)]
    [InlineData(false, true, true)]
    [InlineData(true, false, true)]
    [InlineData(true, true, true)]
    public void RejectsEscapeBeforeAnyWrites(bool document, bool absolute, bool existing)
    {
        var root = Path.Combine(temp, "root");
        var sentinel = Path.Combine(temp, "sentinel.props");
        if (existing) File.WriteAllText(sentinel, "original");
        var context = new DotNetBuildContext();
        context.Capture("safe/nested.props", "safe");
        Capture(context, document, absolute ? sentinel : "../sentinel.props");
        var error = Record.Exception(() => context.MaterializeAll(root));
        if (existing) Assert.Equal("original", File.ReadAllText(sentinel));
        else Assert.False(File.Exists(sentinel), "Materialization created an external sentinel");
        Assert.False(Directory.Exists(root), "Materialization partially wrote the safe entry");
        Assert.IsType<ArgumentException>(error);
    }

    // Windows forms are conservative policy checks on Linux, not Windows-native proof.
    [Theory]
    [InlineData("C:/outside.props")]
    [InlineData("C:outside.props")]
    [InlineData(@"\\server\share\outside.props")]
    [InlineData(@"nested\..\outside.props")]
    [InlineData("nested/../outside.props")]
    [InlineData("")]
    [InlineData("bad\0.props")]
    public void RejectsInvalidDestinationWithoutFilesystemWrites(string path)
    {
        var root = Path.Combine(temp, "root");
        Assert.Throws<ArgumentException>(() => DotNetBuildContext.ResolveDestination(root, path));
        Assert.False(Directory.Exists(root));
    }


    [Theory]
    [InlineData("disk")]
    [InlineData("document")]
    [InlineData("scan")]
    public void MaterializesNativeNestedCapturePaths(string capture)
    {
        const string content = "<Project><!-- native path --></Project>";
        var path = Path.Combine("nested space", "Build.props");
        var context = new DotNetBuildContext();
        if (capture == "disk") context.Capture(path, content);
        else if (capture == "document") context.CaptureIfBuildFile(new XmlParser().Parse(content, path));
        else
        {
            var source = Path.Combine(temp, "source");
            Directory.CreateDirectory(Path.Combine(source, "nested space"));
            File.WriteAllText(Path.Combine(source, path), content);
            context.CaptureFromDisk(source);
        }
        var root = Path.Combine(temp, "root");
        context.MaterializeAll(root);
        Assert.Equal(content, File.ReadAllText(Path.Combine(root, path)));
    }

    [Fact]
    public void PublicDocumentsCannotBypassValidation()
    {
        var context = new DotNetBuildContext();
        context.Documents["../outside.props"] = new XmlParser().Parse("<Project />");
        Assert.Throws<ArgumentException>(() => context.MaterializeAll(Path.Combine(temp, "root")));
        Assert.False(File.Exists(Path.Combine(temp, "outside.props")));
    }

    [Fact]
    public void PreservesPrintingPrecedenceExclusionAndRelativeRoots()
    {
        var context = new DotNetBuildContext();
        context.Capture("nested space/Build.props", "disk");
        var doc = new XmlParser().Parse("<Project><!-- retained --></Project>", "nested space/build.props");
        context.CaptureIfBuildFile(doc);
        context.Capture("skip.props", "skip");
        context.Capture("../excluded.props", "excluded");
        var root = Path.GetRelativePath(Directory.GetCurrentDirectory(), Path.Combine(temp, "root")) + Path.DirectorySeparatorChar;
        context.MaterializeAll(root, "../EXCLUDED.props");
        Assert.Equal(XmlParser.Print(doc), File.ReadAllText(Path.Combine(root, "nested space/build.props")));
        Assert.DoesNotContain("disk", Directory.GetFiles(root, "*", SearchOption.AllDirectories).Select(File.ReadAllText));
        Assert.Throws<ArgumentException>(() => context.MaterializeAll(root, "SKIP.PROPS"));
    }

    [Fact]
    public void ExclusionSkipsBothStoresCaseInsensitively()
    {
        var context = new DotNetBuildContext();
        context.Capture("SKIP.props", "disk");
        context.CaptureIfBuildFile(new XmlParser().Parse("<Project />", "skip.props"));
        context.Capture("keep.props", "keep");
        var root = Path.Combine(temp, "root");
        context.MaterializeAll(root, "Skip.Props");
        Assert.Equal(new[] { Path.Combine(root, "keep.props") }, Directory.GetFiles(root));
    }

    [Fact]
    public void CanonicalDestinationStaysWithinRoot()
    {
        var context = new DotNetBuildContext();
        context.Capture("nested/./build.props", "safe");
        var root = Path.Combine(temp, "root");
        context.MaterializeAll(root + Path.DirectorySeparatorChar);
        Assert.Equal("safe", File.ReadAllText(Path.Combine(root, "nested/build.props")));
    }

    private static void Capture(DotNetBuildContext context, bool document, string path)
    {
        if (document) context.CaptureIfBuildFile(new XmlParser().Parse("<Project />", path));
        else context.Capture(path, "replacement");
    }
}
