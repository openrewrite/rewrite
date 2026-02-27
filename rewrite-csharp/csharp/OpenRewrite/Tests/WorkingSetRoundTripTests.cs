using Microsoft.CodeAnalysis;
using OpenRewrite.CSharp;

namespace OpenRewrite.Tests;

/// <summary>
/// Round-trip tests that parse each working-set solution/project and verify
/// that printing the LST back produces the original source code.
///
/// Each solution/project discovered by <see cref="WorkingSetDiscovery"/> becomes
/// an individual test case. There are two ways to run them:
///
/// 1. [Theory] + [MemberData] — runs all projects dynamically
/// 2. Individual [Fact] tests — run one project at a time for fast iteration
/// </summary>
public class WorkingSetRoundTripTests
{
    private const int TimeoutMinutes = 10;

    #region Dynamic discovery (Theory)

    public static IEnumerable<object[]> AllProjects()
    {
        if (!Directory.Exists(WorkingSetDiscovery.WorkingSetRoot))
            yield break;

        foreach (var (displayName, projectDir, projectFile) in WorkingSetDiscovery.DiscoverAll())
        {
            yield return [displayName, projectFile, projectDir];
        }
    }

    [Theory]
    [MemberData(nameof(AllProjects))]
    public async Task ParseAndPrintRoundTrip(string displayName, string projectFile, string projectDir)
    {
        await RunRoundTrip(displayName, projectFile, projectDir);
    }

    #endregion

    #region Individual project tests (Fact)

    // -- AI Library --

    [Fact]
    public async Task SemanticKernel() => await RunRoundTripByRelativePath(
        @"AI Library\microsoft\semantic-kernel\dotnet",
        "SK-dotnet.slnx");

    // -- Algorithm --

    [Fact]
    public async Task WaveFunctionCollapse() => await RunRoundTripByRelativePath(
        @"Algorithm\mxgmn\WaveFunctionCollapse",
        "WaveFunctionCollapse.csproj");

    // -- Architecture Sample --

    [Fact]
    public async Task ModularMonolith_Build() => await RunRoundTripByRelativePath(
        @"Architecture Sample\kgrzybek\modular-monolith-with-ddd\build",
        "_build.csproj");

    [Fact]
    public async Task ModularMonolith_Src() => await RunRoundTripByRelativePath(
        @"Architecture Sample\kgrzybek\modular-monolith-with-ddd\src",
        "CompanyName.MyMeetings.sln");

    // -- CLI Tool --

    [Fact]
    public async Task BBDown() => await RunRoundTripByRelativePath(
        @"CLI Tool\nilaoda\BBDown",
        "BBDown.sln");

    // -- Desktop App --

    [Fact]
    public async Task FlowLauncher() => await RunRoundTripByRelativePath(
        @"Desktop App\Flow-Launcher\Flow.Launcher",
        "Flow.Launcher.sln");

    [Fact]
    public async Task BulkCrapUninstaller() => await RunRoundTripByRelativePath(
        @"Desktop App\Klocman\Bulk-Crap-Uninstaller\source",
        "BulkCrapUninstaller.sln");

    [Fact]
    public async Task ScreenToGif() => await RunRoundTripByRelativePath(
        @"Desktop App\NickeManarin\ScreenToGif",
        "GifRecorder.sln");

    [Fact]
    public async Task ShareX() => await RunRoundTripByRelativePath(
        @"Desktop App\ShareX\ShareX",
        "ShareX.sln");

    [Fact]
    public async Task YoutubeDownloader() => await RunRoundTripByRelativePath(
        @"Desktop App\Tyrrrz\YoutubeDownloader",
        "YoutubeDownloader.sln");

    // -- Developer Tool --

    [Fact]
    public async Task ILSpy_Installer() => await RunRoundTripByRelativePath(
        @"Developer Tool\icsharpcode\ILSpy",
        "ILSpy.Installer.sln");

    [Fact]
    public async Task ILSpy() => await RunRoundTripByRelativePath(
        @"Developer Tool\icsharpcode\ILSpy",
        "ILSpy.sln");

    [Fact]
    public async Task ILSpy_VSExtensions() => await RunRoundTripByRelativePath(
        @"Developer Tool\icsharpcode\ILSpy",
        "ILSpy.VSExtensions.sln");

    // -- Game Framework --

    [Fact]
    public async Task MonoGame_Build() => await RunRoundTripByRelativePath(
        @"Game Framework\MonoGame\MonoGame",
        "Build.sln");

    [Fact]
    public async Task MonoGame_Android() => await RunRoundTripByRelativePath(
        @"Game Framework\MonoGame\MonoGame",
        "MonoGame.Framework.Android.sln");

    [Fact]
    public async Task MonoGame_DesktopGL() => await RunRoundTripByRelativePath(
        @"Game Framework\MonoGame\MonoGame",
        "MonoGame.Framework.DesktopGL.sln");

    [Fact]
    public async Task MonoGame_iOS() => await RunRoundTripByRelativePath(
        @"Game Framework\MonoGame\MonoGame",
        "MonoGame.Framework.iOS.sln");

    [Fact]
    public async Task MonoGame_Native() => await RunRoundTripByRelativePath(
        @"Game Framework\MonoGame\MonoGame",
        "MonoGame.Framework.Native.sln");

    [Fact]
    public async Task MonoGame_WindowsDX() => await RunRoundTripByRelativePath(
        @"Game Framework\MonoGame\MonoGame",
        "MonoGame.Framework.WindowsDX.sln");

    [Fact]
    public async Task MonoGame_ToolsLinux() => await RunRoundTripByRelativePath(
        @"Game Framework\MonoGame\MonoGame",
        "MonoGame.Tools.Linux.sln");

    [Fact]
    public async Task MonoGame_ToolsMac() => await RunRoundTripByRelativePath(
        @"Game Framework\MonoGame\MonoGame",
        "MonoGame.Tools.Mac.sln");

    [Fact]
    public async Task MonoGame_ToolsWindows() => await RunRoundTripByRelativePath(
        @"Game Framework\MonoGame\MonoGame",
        "MonoGame.Tools.Windows.sln");

    // -- Library --

    [Fact]
    public async Task Polly() => await RunRoundTripByRelativePath(
        @"Library\App-vNext\Polly",
        "Polly.slnx");

    [Fact]
    public async Task Dapper() => await RunRoundTripByRelativePath(
        @"Library\DapperLib\Dapper",
        "Dapper.sln");

    // -- Media Server --

    [Fact]
    public async Task Jellyfin() => await RunRoundTripByRelativePath(
        @"Media Server\jellyfin\jellyfin",
        "Jellyfin.sln");

    // -- Network Tool --

    [Fact]
    public async Task Netch() => await RunRoundTripByRelativePath(
        @"Network Tool\netchx\netch",
        "Netch.sln");

    // -- System Tool --

    [Fact]
    public async Task WinSW() => await RunRoundTripByRelativePath(
        @"System Tool\winsw\winsw\src",
        "WinSW.sln");

    // -- Template --

    [Fact]
    public async Task CleanArchitecture() => await RunRoundTripByRelativePath(
        @"Template\ardalis\CleanArchitecture",
        "Clean.Architecture.slnx");

    [Fact]
    public async Task CfBuildpackTemplate() => await RunRoundTripByRelativePath(
        @"Template\macsux\cf-buildpack-template",
        "MyBuildpack.sln");

    // -- UI Framework --

    [Fact]
    public async Task MaterialDesignInXaml() => await RunRoundTripByRelativePath(
        @"UI Framework\MaterialDesignInXAML\MaterialDesignInXamlToolkit",
        "MaterialDesignToolkit.Full.slnx");

    // -- Web API --

    [Fact]
    public async Task BitwardenServer() => await RunRoundTripByRelativePath(
        @"Web API\bitwarden\server",
        "bitwarden-server.sln");

    [Fact]
    public async Task Jackett() => await RunRoundTripByRelativePath(
        @"Web API\Jackett\Jackett\src",
        "Jackett.sln");

    [Fact]
    public void SwitchTuple_Standalone()
    {
        var source = """
            using System;
            class C
            {
                void M(int a, int b)
                {
                    switch (a, b)
                    {
                        case (1, 2):
                            break;
                        default:
                            break;
                    }
                }
            }
            """;

        var parser = new CSharpParser();
        var cu = parser.Parse(source, "Test.cs");
        var printer = new CSharpPrinter<int>();
        var printed = printer.Print(cu);

        Assert.Equal(source, printed);
    }

    [Fact]
    public void GenericMethodGroup_Standalone()
    {
        var source = """
            class C
            {
                void M()
                {
                    var x = items.Select<string>;
                    var y = Foo.Bar<int, string>.Baz();
                }
            }
            """;

        var parser = new CSharpParser();
        var cu = parser.Parse(source, "Test.cs");
        var printer = new CSharpPrinter<int>();
        var printed = printer.Print(cu);

        Assert.Equal(source, printed);
    }

    [Fact]
    public void PointerAccess_Standalone()
    {
        var source = """
            class C
            {
                unsafe void M(int* p)
                {
                    p->ToString();
                }
            }
            """;

        var parser = new CSharpParser();
        var cu = parser.Parse(source, "Test.cs");
        var printer = new CSharpPrinter<int>();
        var printed = printer.Print(cu);

        Assert.Equal(source, printed);
    }

    [Fact]
    public void SemicolonOnNewLine_Standalone()
    {
        var source = "class C\r\n{\r\n    void M()\r\n    {\r\n        var x = new object()\r\n        ;\r\n    }\r\n}";

        var parser = new CSharpParser();
        var cu = parser.Parse(source, "Test.cs");
        var printer = new CSharpPrinter<int>();
        var printed = printer.Print(cu);

        Assert.Equal(source, printed);
    }

    [Fact]
    public void CollectionInitializer_TrailingComma_Standalone()
    {
        var source = """
            class C
            {
                int[] M() => [
                    new Foo()
                    {
                        X = [1],
                    },
                ];
            }
            """;

        var parser = new CSharpParser();
        var cu = parser.Parse(source, "Test.cs");
        var printer = new CSharpPrinter<int>();
        var printed = printer.Print(cu);

        Assert.Equal(source, printed);
    }

    [Fact]
    public void PreprocessorConditional_Standalone()
    {
        // Test with actual Dapper DateTimeOnlyTests.cs file using semantic model (like SolutionParser)
        var filePath = @"C:\Projects\moderneinc\moderne-cli\working-set-csharp\Library\DapperLib\Dapper\tests\Dapper.Tests\DateTimeOnlyTests.cs";
        if (!File.Exists(filePath))
        {
            Console.WriteLine("SKIP: file not found");
            return;
        }

        var source = File.ReadAllText(filePath);

        // First, test without semantic model (should pass)
        var parser = new CSharpParser();
        var cu = parser.Parse(source, "DateTimeOnlyTests.cs");
        var printer = new CSharpPrinter<int>();
        var printed = printer.Print(cu);
        Console.WriteLine($"Without semantic model: {(printed == source ? "PASS" : "FAIL")}");

        // Now test with a semantic model that has preprocessor symbols (like MSBuildWorkspace does)
        var parseOptions = new Microsoft.CodeAnalysis.CSharp.CSharpParseOptions()
            .WithPreprocessorSymbols("NET6_0_OR_GREATER", "NETCOREAPP");
        var syntaxTree = Microsoft.CodeAnalysis.CSharp.CSharpSyntaxTree.ParseText(
            source, options: parseOptions, path: filePath);
        var references = new[] {
            Microsoft.CodeAnalysis.MetadataReference.CreateFromFile(typeof(object).Assembly.Location)
        };
        var compilation = Microsoft.CodeAnalysis.CSharp.CSharpCompilation.Create(
            "TestAssembly", [syntaxTree], references);
        var semanticModel = compilation.GetSemanticModel(syntaxTree);

        var cu2 = parser.Parse(source, "DateTimeOnlyTests.cs", semanticModel);
        var printed2 = printer.Print(cu2);
        Console.WriteLine($"With semantic model: {(printed2 == source ? "PASS" : "FAIL")}");

        if (printed2 != source)
        {
            for (int i = 0; i < Math.Min(source.Length, printed2.Length); i++)
            {
                if (source[i] != printed2[i])
                {
                    var start = Math.Max(0, i - 30);
                    var end = Math.Min(source.Length, i + 50);
                    Console.WriteLine($"First diff at char {i}:");
                    Console.WriteLine($"Expected: ...{source[start..end].Replace("\r", "\\r").Replace("\n", "\\n")}...");
                    var endP = Math.Min(printed2.Length, i + 50);
                    Console.WriteLine($"Actual:   ...{printed2[start..endP].Replace("\r", "\\r").Replace("\n", "\\n")}...");
                    break;
                }
            }
        }

        Assert.Equal(source, printed2);
    }

    /// <summary>
    /// Isolated test: parse a single file with standalone CSharpParser (no MSBuildWorkspace).
    /// Useful for debugging individual file parsing/printing issues.
    /// </summary>
    [Fact]
    public void Extensibility_Standalone()
    {
        var filePath = @"C:\Projects\moderneinc\moderne-cli\working-set-csharp\Library\App-vNext\Polly\src\Snippets\Docs\Extensibility.cs";
        if (!File.Exists(filePath))
        {
            Console.WriteLine("SKIP: file not found");
            return;
        }

        var source = File.ReadAllText(filePath);
        var parser = new CSharpParser();
        var cu = parser.Parse(source, "Extensibility.cs");

        // Dump the AST structure
        Console.WriteLine("=== AST Structure ===");
        DumpTree(cu, 0);
        Console.WriteLine("=== END AST ===");

        var printer = new CSharpPrinter<int>();
        var printed = printer.Print(cu);

        Console.WriteLine("=== PRINTED OUTPUT ===");
        Console.WriteLine(printed);
        Console.WriteLine("=== END OUTPUT ===");

        Assert.Equal(source, printed);
    }

    [Fact]
    public void PreprocessorFieldInitializer_Standalone()
    {
        // Minimal reproduction of Munger.cs #if !DEBUG field initializer pattern
        var source = "class C\r\n{\r\n    private static bool ignoreMissingAspects\r\n#if !DEBUG\r\n        = true\r\n#endif\r\n        ;\r\n}";

        // Debug: trace the preprocessor transformation
        var symbols = PreprocessorSourceTransformer.ExtractSymbols(source);
        Console.WriteLine($"Extracted symbols: [{string.Join(", ", symbols)}]");

        var directiveLines = PreprocessorSourceTransformer.GetDirectivePositions(source);
        Console.WriteLine($"Directive lines: {directiveLines.Count}");
        foreach (var d in directiveLines)
            Console.WriteLine($"  Line {d.LineNumber}: {d.Kind} group={d.GroupId} text=\"{d.Text.Replace("\r", "\\r")}\"");

        var directiveLineToIndex = new Dictionary<int, int>();
        for (int idx = 0; idx < directiveLines.Count; idx++)
            directiveLineToIndex[directiveLines[idx].LineNumber] = idx;

        var permutations = PreprocessorSourceTransformer.GenerateUniquePermutations(
            source, symbols, directiveLineToIndex);
        Console.WriteLine($"Permutations: {permutations.Count}");
        for (int i = 0; i < permutations.Count; i++)
        {
            var (cleanSource, definedSymbols) = permutations[i];
            Console.WriteLine($"\n=== Branch {i} symbols=[{string.Join(",", definedSymbols)}] ===");
            Console.WriteLine(cleanSource.Replace("\r", "\\r").Replace("\n", "\\n\n"));
        }

        PreprocessorSourceTransformer.ComputeActiveBranchIndices(directiveLines, permutations);
        Console.WriteLine("\nActive branch indices:");
        foreach (var d in directiveLines)
            Console.WriteLine($"  Line {d.LineNumber}: {d.Kind} ActiveBranch={d.ActiveBranchIndex}");

        var parser = new CSharpParser();
        var cu = parser.Parse(source, "Test.cs");

        // Debug: inspect the AST structure
        Console.WriteLine($"\nOuter CU type: {cu.GetType().Name}");
        Console.WriteLine($"Outer CU members: {cu.Members.Count}");
        foreach (var member in cu.Members)
        {
            Console.WriteLine($"  Member type: {member.GetType().Name}");
            if (member is ConditionalDirective cd)
            {
                Console.WriteLine($"  DirectiveLines: {cd.DirectiveLines.Count}");
                Console.WriteLine($"  Branches: {cd.Branches.Count}");

                // Print each branch's output
                for (int b = 0; b < cd.Branches.Count; b++)
                {
                    var branchPrinter = new CSharpPrinter<int>();
                    var branchOutput = branchPrinter.Print(cd.Branches[b].Element);
                    Console.WriteLine($"\n  === Branch {b} output ===");
                    Console.WriteLine(branchOutput.Replace("\r", "\\r").Replace("\n", "\\n\n"));
                }
            }
        }

        var printer = new CSharpPrinter<int>();
        var printed = printer.Print(cu);

        if (printed != source)
        {
            Console.WriteLine("\n=== EXPECTED ===");
            Console.WriteLine(source.Replace("\r", "\\r").Replace("\n", "\\n\n"));
            Console.WriteLine("=== ACTUAL ===");
            Console.WriteLine(printed.Replace("\r", "\\r").Replace("\n", "\\n\n"));
        }

        Assert.Equal(source, printed);
    }

    [Fact]
    public void PreprocessorElifChain_Standalone()
    {
        // Minimal reproduction of PlatformInfo.cs — long #elif chain with most branches inactive
        var source = "class C\r\n{\r\n    int M()\r\n    {\r\n#if A\r\n        return 1;\r\n#elif B\r\n        return 2;\r\n#elif C\r\n        return 3;\r\n#else\r\n        return 4;\r\n#endif\r\n    }\r\n}";

        var parser = new CSharpParser();
        var cu = parser.Parse(source, "Test.cs");
        var printer = new CSharpPrinter<int>();
        var printed = printer.Print(cu);

        if (printed != source)
        {
            Console.WriteLine("=== EXPECTED ===");
            Console.WriteLine(source.Replace("\r", "\\r").Replace("\n", "\\n\n"));
            Console.WriteLine("=== ACTUAL ===");
            Console.WriteLine(printed.Replace("\r", "\\r").Replace("\n", "\\n\n"));
        }

        Assert.Equal(source, printed);
    }

    [Fact]
    public void PreprocessorNestedIfElif_Standalone()
    {
        // Minimal reproduction of GraphicsExtensions.cs — nested #if with #elif branches
        var source = "#if OPENGL\r\n#if DESKTOPGL\r\nusing MonoGame.OpenGL;\r\n#elif ANGLE\r\nusing OpenTK.Graphics;\r\n#endif\r\n#endif\r\n\r\nnamespace Ns\r\n{\r\n}";

        var parser = new CSharpParser();
        var cu = parser.Parse(source, "Test.cs");
        var printer = new CSharpPrinter<int>();
        var printed = printer.Print(cu);

        if (printed != source)
        {
            Console.WriteLine("=== EXPECTED ===");
            Console.WriteLine(source.Replace("\r", "\\r").Replace("\n", "\\n\n"));
            Console.WriteLine("=== ACTUAL ===");
            Console.WriteLine(printed.Replace("\r", "\\r").Replace("\n", "\\n\n"));
        }

        Assert.Equal(source, printed);
    }

    [Fact]
    public void PreprocessorIfTrue_Standalone()
    {
        // Minimal reproduction of Microphone.OpenAL.cs — #if true //DESKTOPGL with #else branch
        var source = "class C\r\n{\r\n    void M()\r\n    {\r\n#if true //DESKTOPGL\r\n        DoDesktop();\r\n#else\r\n        DoFallback();\r\n#endif\r\n    }\r\n}";

        var parser = new CSharpParser();
        var cu = parser.Parse(source, "Test.cs");
        var printer = new CSharpPrinter<int>();
        var printed = printer.Print(cu);

        if (printed != source)
        {
            Console.WriteLine("=== EXPECTED ===");
            Console.WriteLine(source.Replace("\r", "\\r").Replace("\n", "\\n\n"));
            Console.WriteLine("=== ACTUAL ===");
            Console.WriteLine(printed.Replace("\r", "\\r").Replace("\n", "\\n\n"));
        }

        Assert.Equal(source, printed);
    }

    [Fact]
    public void PreprocessorElseBranch_Standalone()
    {
        // Minimal reproduction of Microphone.OpenAL.cs — #else branch with content
        var source = "class C\r\n{\r\n    void M()\r\n    {\r\n#if OPENAL\r\n        DoOpenAL();\r\n#else\r\n        DoFallback();\r\n#endif\r\n    }\r\n}";

        var parser = new CSharpParser();
        var cu = parser.Parse(source, "Test.cs");
        var printer = new CSharpPrinter<int>();
        var printed = printer.Print(cu);

        if (printed != source)
        {
            Console.WriteLine("=== EXPECTED ===");
            Console.WriteLine(source.Replace("\r", "\\r").Replace("\n", "\\n\n"));
            Console.WriteLine("=== ACTUAL ===");
            Console.WriteLine(printed.Replace("\r", "\\r").Replace("\n", "\\n\n"));
        }

        Assert.Equal(source, printed);
    }

    [Fact]
    public void PreprocessorNestedIfElif_GraphicsExtensions()
    {
        // Faithful reproduction of GraphicsExtensions.cs structure:
        // - Nested #if OPENGL / #if DESKTOPGL || GLES / #elif ANGLE / #endif / #endif
        // - Multiple directive groups with 8+ symbols (triggers MaxPermutations cap)
        // - The #elif ANGLE branch content was being lost
        var source =
            "using System;\r\n" +
            "\r\n" +
            "#if OPENGL\r\n" +
            "#if DESKTOPGL || GLES\r\n" +
            "using MonoGame.OpenGL;\r\n" +
            "#elif ANGLE\r\n" +
            "using OpenTK.Graphics;\r\n" +
            "#endif\r\n" +
            "#endif\r\n" +
            "\r\n" +
            "namespace Ns\r\n" +
            "{\r\n" +
            "    static class C\r\n" +
            "    {\r\n" +
            "#if OPENGL\r\n" +
            "        void OpenGLMethod() { }\r\n" +
            "#if WINDOWS || DESKTOPGL\r\n" +
            "        void WinDesktopMethod() { }\r\n" +
            "#endif\r\n" +
            "#if MONOMAC\r\n" +
            "        void MonoMacMethod() { }\r\n" +
            "#endif\r\n" +
            "#if WINDOWS || DESKTOPGL || ANGLE\r\n" +
            "        void WinDesktopAngleMethod() { }\r\n" +
            "#endif\r\n" +
            "#if IOS || ANDROID\r\n" +
            "        void MobileMethod() { }\r\n" +
            "#else\r\n" +
            "        void DesktopMethod() { }\r\n" +
            "#endif\r\n" +
            "#if !IOS && !ANDROID && !ANGLE\r\n" +
            "        void NotMobileNotAngle() { }\r\n" +
            "#endif\r\n" +
            "#endif\r\n" +
            "    }\r\n" +
            "}";

        var parser = new CSharpParser();
        var cu = parser.Parse(source, "Test.cs");
        var printer = new CSharpPrinter<int>();
        var printed = printer.Print(cu);

        if (printed != source)
        {
            Console.WriteLine("=== EXPECTED ===");
            Console.WriteLine(source.Replace("\r", "\\r").Replace("\n", "\\n\n"));
            Console.WriteLine("=== ACTUAL ===");
            Console.WriteLine(printed.Replace("\r", "\\r").Replace("\n", "\\n\n"));

            // Show permutations being generated
            var symbols = PreprocessorSourceTransformer.ExtractSymbols(source);
            Console.WriteLine($"\nSymbols: {string.Join(", ", symbols)}");
            var permutations = PreprocessorSourceTransformer.GenerateUniquePermutations(source, symbols);
            Console.WriteLine($"Permutations: {permutations.Count}");
            for (int i = 0; i < permutations.Count; i++)
            {
                var (cleanSource, definedSymbols) = permutations[i];
                Console.WriteLine($"  [{i}] defined={string.Join(",", definedSymbols.OrderBy(s => s))}");
                var lines = cleanSource.Split('\n');
                for (int ln = 0; ln < lines.Length; ln++)
                {
                    if (lines[ln].Contains("//DIRECTIVE:"))
                        Console.WriteLine($"    L{ln}: {lines[ln].TrimEnd()}");
                }
            }
        }

        Assert.Equal(source, printed);
    }

    private static void DumpTree(object? node, int indent)
    {
        if (node == null) return;
        var prefix = new string(' ', indent * 2);
        var typeName = node.GetType().Name;

        if (node is Java.J j && node is not Java.ExpressionStatement)
        {
            var pfx = j.Prefix.Whitespace.Replace("\n", "\\n").Replace("\r", "\\r");
            if (pfx.Length > 40) pfx = pfx[..40] + "...";
            Console.WriteLine($"{prefix}{typeName} prefix=\"{pfx}\"");

            // Recurse into known children
            if (node is Java.Block block)
            {
                foreach (var stmt in block.Statements)
                {
                    DumpTree(stmt.Element, indent + 1);
                }
            }
            else if (node is OpenRewrite.CSharp.CompilationUnit cu2)
            {
                foreach (var member in cu2.Members)
                {
                    DumpTree(member, indent + 1);
                }
            }
            else if (node is Java.ClassDeclaration cd)
            {
                if (cd.Body != null)
                    DumpTree(cd.Body, indent + 1);
            }
            else if (node is Java.MethodDeclaration md)
            {
                if (md.Body != null)
                    DumpTree(md.Body, indent + 1);
            }
        }
        else
        {
            Console.WriteLine($"{prefix}{typeName}");
        }
    }

    #endregion

    #region Helpers

    private static async Task RunRoundTripByRelativePath(string relativeDir, string fileName)
    {
        var projectDir = Path.Combine(WorkingSetDiscovery.WorkingSetRoot, relativeDir);
        var projectFile = Path.Combine(projectDir, fileName);
        var displayName = Path.Combine(relativeDir, fileName).Replace('\\', '/');

        if (!File.Exists(projectFile))
        {
            Console.WriteLine($"SKIP: Project file not found: {projectFile}");
            return;
        }

        await RunRoundTrip(displayName, projectFile, projectDir);
    }

    private static async Task RunRoundTrip(string displayName, string projectFile, string projectDir)
    {
        var rootDir = projectDir;

        Console.WriteLine($"Parsing: {displayName}");
        Console.WriteLine($"  File: {projectFile}");
        Console.WriteLine($"  Root: {rootDir}");

        var parser = new SolutionParser();

        using var cts = new CancellationTokenSource(TimeSpan.FromMinutes(TimeoutMinutes));
        Solution solution;
        try
        {
            solution = await parser.LoadAsync(projectFile, cts.Token);
        }
        catch (OperationCanceledException)
        {
            Assert.Fail($"Timed out loading {displayName} after {TimeoutMinutes} minutes");
            return;
        }
        catch (Exception ex)
        {
            Assert.Fail($"Failed to load {displayName}: {ex.Message}");
            return;
        }

        Console.WriteLine($"  Projects in solution: {solution.Projects.Count()}");

        var projectPaths = GetProjectPaths(solution, projectFile);
        Console.WriteLine($"  Parsing {projectPaths.Count} project(s)");

        var printer = new CSharpPrinter<int>();
        var totalFiles = 0;
        var failures = new List<string>();

        foreach (var projPath in projectPaths)
        {
            List<CompilationUnit> compilationUnits;
            try
            {
                compilationUnits = parser.ParseProject(solution, projPath, rootDir);
            }
            catch (Exception ex)
            {
                var msg = $"Failed to parse project {Path.GetFileName(projPath)}: {ex.Message}";
                Console.WriteLine($"  PARSE ERROR: {msg}");
                failures.Add(msg);
                continue;
            }

            Console.WriteLine($"  Project {Path.GetFileName(projPath)}: {compilationUnits.Count} files");
            totalFiles += compilationUnits.Count;

            foreach (var cu in compilationUnits)
            {
                var sourcePath = cu.SourcePath;
                string printed;
                try
                {
                    printed = printer.Print(cu);
                }
                catch (Exception ex)
                {
                    var msg = $"Print failed for {sourcePath}: {ex.Message}";
                    Console.WriteLine($"    PRINT ERROR: {msg}");
                    Console.WriteLine($"    Stack trace: {ex.StackTrace}");
                    failures.Add(msg + "\n" + ex.StackTrace);
                    continue;
                }

                // Read original source to compare
                var fullPath = Path.Combine(rootDir, sourcePath.Replace('/', Path.DirectorySeparatorChar));
                if (!File.Exists(fullPath))
                {
                    Console.WriteLine($"    SKIP: Original file not found: {sourcePath}");
                    continue;
                }

                var originalSource = await File.ReadAllTextAsync(fullPath, cts.Token);

                if (printed != originalSource)
                {
                    var (line, col, context) = FindFirstDifference(originalSource, printed);
                    var msg = $"Round-trip mismatch in {sourcePath} at line {line}, col {col}:\n{context}";
                    Console.WriteLine($"    MISMATCH: {sourcePath} at line {line}, col {col}");
                    Console.WriteLine(context);
                    // Show full output for small files to aid debugging
                    if (printed.Length < 2000)
                    {
                        Console.WriteLine($"    --- FULL PRINTED OUTPUT ({printed.Length} chars) ---");
                        Console.WriteLine(printed);
                        Console.WriteLine("    --- END FULL OUTPUT ---");
                    }
                    failures.Add(msg);
                }
            }
        }

        Console.WriteLine($"\n  Total files parsed: {totalFiles}");
        Console.WriteLine($"  Failures: {failures.Count}");

        if (failures.Count > 0)
        {
            Assert.Fail($"{failures.Count} round-trip failure(s) in {displayName}:\n\n" +
                         string.Join("\n\n", failures.Take(10)));
        }

        Assert.True(totalFiles > 0, $"No files were parsed from {displayName}");
    }

    private static List<string> GetProjectPaths(Solution solution, string originalFile)
    {
        if (originalFile.EndsWith(".csproj", StringComparison.OrdinalIgnoreCase))
        {
            return solution.Projects
                .Where(p => p.FilePath != null)
                .Select(p => p.FilePath!)
                .ToList();
        }

        return solution.Projects
            .Where(p => p.FilePath != null)
            .Select(p => p.FilePath!)
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .ToList();
    }

    private static (int Line, int Col, string Context) FindFirstDifference(string expected, string actual)
    {
        var line = 1;
        var col = 1;
        var minLen = Math.Min(expected.Length, actual.Length);

        for (var i = 0; i < minLen; i++)
        {
            if (expected[i] != actual[i])
            {
                return (line, col, FormatDiffContext(expected, actual, i));
            }

            if (expected[i] == '\n')
            {
                line++;
                col = 1;
            }
            else
            {
                col++;
            }
        }

        if (expected.Length != actual.Length)
        {
            return (line, col,
                $"Strings differ in length: expected {expected.Length} chars, actual {actual.Length} chars.\n" +
                $"Expected ends with: ...{Escape(expected[Math.Max(0, expected.Length - 40)..])}|\n" +
                $"Actual ends with:   ...{Escape(actual[Math.Max(0, actual.Length - 40)..])  }|");
        }

        return (0, 0, "Strings are identical");
    }

    private static string FormatDiffContext(string expected, string actual, int diffPos)
    {
        var contextStart = Math.Max(0, diffPos - 30);
        var contextEnd = Math.Min(Math.Min(expected.Length, actual.Length), diffPos + 30);

        var expSnippet = Escape(expected[contextStart..contextEnd]);
        var actSnippet = Escape(actual[contextStart..contextEnd]);

        var pointer = new string(' ', diffPos - contextStart) + "^";

        return $"Expected: ...{expSnippet}...\n" +
               $"Actual:   ...{actSnippet}...\n" +
               $"               {pointer}";
    }

    private static string Escape(string s) =>
        s.Replace("\r", "\\r").Replace("\n", "\\n").Replace("\t", "\\t");

    #endregion
}
