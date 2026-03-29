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
using OpenRewrite.CSharp.Format;

namespace OpenRewrite.Tests.Format;

public class EditorConfigResolverTests : IDisposable
{
    private readonly string _tempDir;

    public EditorConfigResolverTests()
    {
        _tempDir = Path.Combine(Path.GetTempPath(), "editorconfig-tests-" + Guid.NewGuid().ToString("N")[..8]);
        Directory.CreateDirectory(_tempDir);
    }

    public void Dispose()
    {
        if (Directory.Exists(_tempDir))
            Directory.Delete(_tempDir, recursive: true);
    }

    private string CreateFile(string relativePath, string content)
    {
        var fullPath = Path.Combine(_tempDir, relativePath.Replace('/', Path.DirectorySeparatorChar));
        Directory.CreateDirectory(Path.GetDirectoryName(fullPath)!);
        File.WriteAllText(fullPath, content);
        return fullPath;
    }

    [Fact]
    public void DefaultStyleWhenNoEditorConfig()
    {
        var resolver = new EditorConfigResolver(_tempDir);
        var csFile = CreateFile("src/Program.cs", "");
        var style = resolver.Resolve(csFile);

        // Should return Roslyn/VS defaults (Allman style)
        Assert.False(style.UseTabs);
        Assert.Equal(4, style.IndentSize);
        Assert.True(style.NewLinesForBracesInTypes);
        Assert.True(style.NewLinesForBracesInMethods);
        Assert.True(style.NewLinesForBracesInControlBlocks);
        Assert.True(style.NewLineBeforeElse);
        Assert.True(style.NewLineBeforeCatch);
        Assert.True(style.NewLineBeforeFinally);
        Assert.True(style.WrappingPreserveSingleLine);
        Assert.True(style.WrappingKeepStatementsOnSingleLine);
    }

    [Fact]
    public void DetectsIndentStyleTabs()
    {
        CreateFile(".editorconfig", """
            root = true

            [*.cs]
            indent_style = tab
            indent_size = 4
            tab_width = 8
            """);
        var csFile = CreateFile("Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        Assert.True(style.UseTabs);
        Assert.Equal(4, style.IndentSize);
        Assert.Equal(8, style.TabSize);
    }

    [Fact]
    public void DetectsIndentStyleSpaces()
    {
        CreateFile(".editorconfig", """
            root = true

            [*.cs]
            indent_style = space
            indent_size = 2
            """);
        var csFile = CreateFile("Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        Assert.False(style.UseTabs);
        Assert.Equal(2, style.IndentSize);
        Assert.Equal(2, style.TabSize); // tab_width defaults to indent_size
    }

    [Fact]
    public void DetectsEndOfLineCrlf()
    {
        CreateFile(".editorconfig", """
            root = true

            [*.cs]
            end_of_line = crlf
            """);
        var csFile = CreateFile("Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        Assert.Equal("\r\n", style.NewLine);
    }

    [Fact]
    public void DetectsEndOfLineLf()
    {
        CreateFile(".editorconfig", """
            root = true

            [*.cs]
            end_of_line = lf
            """);
        var csFile = CreateFile("Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        Assert.Equal("\n", style.NewLine);
    }

    [Fact]
    public void NewLineBeforeOpenBraceAll()
    {
        CreateFile(".editorconfig", """
            root = true

            [*.cs]
            csharp_new_line_before_open_brace = all
            """);
        var csFile = CreateFile("Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        Assert.True(style.NewLinesForBracesInTypes);
        Assert.True(style.NewLinesForBracesInMethods);
        Assert.True(style.NewLinesForBracesInProperties);
        Assert.True(style.NewLinesForBracesInAccessors);
        Assert.True(style.NewLinesForBracesInAnonymousMethods);
        Assert.True(style.NewLinesForBracesInAnonymousTypes);
        Assert.True(style.NewLinesForBracesInControlBlocks);
        Assert.True(style.NewLinesForBracesInLambdaExpressionBody);
        Assert.True(style.NewLinesForBracesInObjectCollectionArrayInitializers);
        Assert.True(style.NewLinesForBracesInLocalFunctions);
    }

    [Fact]
    public void NewLineBeforeOpenBraceNone()
    {
        CreateFile(".editorconfig", """
            root = true

            [*.cs]
            csharp_new_line_before_open_brace = none
            """);
        var csFile = CreateFile("Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        Assert.False(style.NewLinesForBracesInTypes);
        Assert.False(style.NewLinesForBracesInMethods);
        Assert.False(style.NewLinesForBracesInProperties);
        Assert.False(style.NewLinesForBracesInAccessors);
        Assert.False(style.NewLinesForBracesInAnonymousMethods);
        Assert.False(style.NewLinesForBracesInAnonymousTypes);
        Assert.False(style.NewLinesForBracesInControlBlocks);
        Assert.False(style.NewLinesForBracesInLambdaExpressionBody);
        Assert.False(style.NewLinesForBracesInObjectCollectionArrayInitializers);
        Assert.False(style.NewLinesForBracesInLocalFunctions);
    }

    [Fact]
    public void NewLineBeforeOpenBraceCommaList()
    {
        CreateFile(".editorconfig", """
            root = true

            [*.cs]
            csharp_new_line_before_open_brace = methods, types, control_blocks
            """);
        var csFile = CreateFile("Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        Assert.True(style.NewLinesForBracesInTypes);
        Assert.True(style.NewLinesForBracesInMethods);
        Assert.True(style.NewLinesForBracesInControlBlocks);
        // Others should be false (not in list)
        Assert.False(style.NewLinesForBracesInProperties);
        Assert.False(style.NewLinesForBracesInAccessors);
        Assert.False(style.NewLinesForBracesInAnonymousMethods);
        Assert.False(style.NewLinesForBracesInAnonymousTypes);
        Assert.False(style.NewLinesForBracesInLambdaExpressionBody);
        Assert.False(style.NewLinesForBracesInObjectCollectionArrayInitializers);
        Assert.False(style.NewLinesForBracesInLocalFunctions);
    }

    [Fact]
    public void NewLineBeforeElseCatchFinally()
    {
        CreateFile(".editorconfig", """
            root = true

            [*.cs]
            csharp_new_line_before_else = false
            csharp_new_line_before_catch = false
            csharp_new_line_before_finally = true
            """);
        var csFile = CreateFile("Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        Assert.False(style.NewLineBeforeElse);
        Assert.False(style.NewLineBeforeCatch);
        Assert.True(style.NewLineBeforeFinally);
    }

    [Fact]
    public void HierarchicalEditorConfigChildOverridesParent()
    {
        CreateFile(".editorconfig", """
            root = true

            [*.cs]
            indent_size = 4
            csharp_new_line_before_open_brace = all
            """);
        CreateFile("src/.editorconfig", """
            [*.cs]
            indent_size = 2
            csharp_new_line_before_open_brace = none
            """);
        var csFile = CreateFile("src/Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        Assert.Equal(2, style.IndentSize);
        Assert.False(style.NewLinesForBracesInTypes);
        Assert.False(style.NewLinesForBracesInMethods);
    }

    [Fact]
    public void HierarchicalEditorConfigPartialOverride()
    {
        CreateFile(".editorconfig", """
            root = true

            [*.cs]
            indent_size = 4
            indent_style = tab
            csharp_new_line_before_open_brace = all
            """);
        CreateFile("src/.editorconfig", """
            [*.cs]
            indent_size = 2
            """);
        var csFile = CreateFile("src/Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        // Child overrides indent_size
        Assert.Equal(2, style.IndentSize);
        // Parent's values still apply for unoverridden settings
        Assert.True(style.UseTabs);
        Assert.True(style.NewLinesForBracesInTypes);
    }

    [Fact]
    public void RootTrueStopsUpwardSearch()
    {
        CreateFile(".editorconfig", """
            root = true

            [*.cs]
            indent_size = 8
            """);
        CreateFile("src/.editorconfig", """
            root = true

            [*.cs]
            indent_size = 2
            """);
        var csFile = CreateFile("src/Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        // The src/.editorconfig has root=true, so parent is not consulted
        Assert.Equal(2, style.IndentSize);
        // Other values should be defaults since src/.editorconfig doesn't set them
        Assert.True(style.NewLinesForBracesInTypes);
    }

    [Fact]
    public void CachesResolvedStylePerDirectory()
    {
        CreateFile(".editorconfig", """
            root = true

            [*.cs]
            indent_size = 4
            """);
        var file1 = CreateFile("src/A.cs", "");
        var file2 = CreateFile("src/B.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);

        var style1 = resolver.Resolve(file1);
        var style2 = resolver.Resolve(file2);

        // Same directory → same cached instance
        Assert.Same(style1, style2);
    }

    [Fact]
    public void DifferentDirectoriesGetDifferentInstances()
    {
        CreateFile(".editorconfig", """
            root = true

            [*.cs]
            indent_size = 4
            """);
        CreateFile("src/.editorconfig", """
            [*.cs]
            indent_size = 2
            """);
        var file1 = CreateFile("Program.cs", "");
        var file2 = CreateFile("src/Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);

        var style1 = resolver.Resolve(file1);
        var style2 = resolver.Resolve(file2);

        Assert.NotSame(style1, style2);
        Assert.Equal(4, style1.IndentSize);
        Assert.Equal(2, style2.IndentSize);
    }

    [Fact]
    public void WildcardSectionAppliesToCsFiles()
    {
        CreateFile(".editorconfig", """
            root = true

            [*]
            indent_style = tab
            indent_size = 4
            """);
        var csFile = CreateFile("Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        Assert.True(style.UseTabs);
        Assert.Equal(4, style.IndentSize);
    }

    [Fact]
    public void CsSpecificSectionOverridesWildcard()
    {
        CreateFile(".editorconfig", """
            root = true

            [*]
            indent_size = 4

            [*.cs]
            indent_size = 2
            """);
        var csFile = CreateFile("Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        Assert.Equal(2, style.IndentSize);
    }

    [Fact]
    public void CommentsAndBlankLinesAreIgnored()
    {
        CreateFile(".editorconfig", """
            # This is a comment
            root = true

            ; This is also a comment

            [*.cs]
            indent_size = 3
            """);
        var csFile = CreateFile("Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        Assert.Equal(3, style.IndentSize);
    }

    [Fact]
    public void IndentSizeTabUsesTabWidth()
    {
        CreateFile(".editorconfig", """
            root = true

            [*.cs]
            indent_style = tab
            indent_size = tab
            tab_width = 8
            """);
        var csFile = CreateFile("Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        Assert.True(style.UseTabs);
        Assert.Equal(8, style.IndentSize);
        Assert.Equal(8, style.TabSize);
    }

    [Fact]
    public void SeveritySuffixesAreStripped()
    {
        CreateFile(".editorconfig", """
            root = true

            [*.cs]
            csharp_new_line_before_open_brace = all:error
            csharp_new_line_before_else = false:suggestion
            """);
        var csFile = CreateFile("Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        Assert.True(style.NewLinesForBracesInTypes);
        Assert.True(style.NewLinesForBracesInMethods);
        Assert.False(style.NewLineBeforeElse);
    }

    [Fact]
    public void WrappingOptions()
    {
        CreateFile(".editorconfig", """
            root = true

            [*.cs]
            csharp_preserve_single_line_blocks = false
            csharp_preserve_single_line_statements = false
            """);
        var csFile = CreateFile("Program.cs", "");
        var resolver = new EditorConfigResolver(_tempDir);
        var style = resolver.Resolve(csFile);

        Assert.False(style.WrappingPreserveSingleLine);
        Assert.False(style.WrappingKeepStatementsOnSingleLine);
    }
}
