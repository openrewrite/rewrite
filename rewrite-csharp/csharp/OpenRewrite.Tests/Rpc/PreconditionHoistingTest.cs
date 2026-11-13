/*
 * Copyright 2026 the original author or authors.
 * Licensed under the Moderne Source Available License (the "License");
 * See https://docs.moderne.io/licensing/moderne-source-available-license
 */
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Rpc;
using OpenRewrite.Java;
using static OpenRewrite.Core.Preconditions;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Rpc;

public class PreconditionHoistingTest
{
    private static readonly Precondition CsLanguageGate = new()
    {
        VisitorName = "org.openrewrite.rpc.internal.FindTreesOfType",
        VisitorOptions = new() { ["type"] = "org.openrewrite.csharp.tree.Cs" }
    };

    // Matches nothing in-process, so a visitor gated on it runs only once the gate is hoisted
    private static readonly RecipeRef NeverMatchingJavaSearch = new("org.openrewrite.FindSourceFiles",
        new Dictionary<string, object?> { ["filePattern"] = "**/never-matches.cs" },
        ITreeVisitor<ExecutionContext>.Noop());

    private sealed class MarkFile : CSharpVisitor<ExecutionContext>
    {
        public override J? PreVisit(J tree, ExecutionContext ctx)
        {
            StopAfterPreVisit();
            return SearchResult.Found(tree);
        }
    }

    private sealed class EditGatedOnJavaSearch : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Edit gated on a Java search";
        public override string Description => "Marks files a Java search matches.";
        public override ITreeVisitor<ExecutionContext> GetVisitor() => Check(NeverMatchingJavaSearch, new MarkFile());
    }

    [Fact]
    public async Task JavaSearchPreconditionOfAnEditorRunsOnlyOnTheHost()
    {
        var (server, prepared) = await Prepare(new EditGatedOnJavaSearch());

        Assert.Equivalent(new[]
        {
            new Precondition
            {
                VisitorName = "org.openrewrite.FindSourceFiles",
                VisitorOptions = new() { ["filePattern"] = "**/never-matches.cs" }
            },
            CsLanguageGate
        }, prepared.EditPreconditions, strict: true);
        var cu = Parse();
        Assert.NotSame(cu, server.VisitorFor(prepared.EditVisitor, new ExecutionContext()).Visit(cu, new ExecutionContext()));
    }

    private sealed class JavaFind(string find) : global::OpenRewrite.Core.Recipe, IDelegatesTo
    {
        public override string DisplayName => "Java find";
        public override string Description => "Delegates to a Java recipe.";
        public string JavaRecipeName => "org.openrewrite.text.Find";
        public Dictionary<string, object?> Options => new() { ["find"] = find };
    }

    private sealed class EditGatedOnJavaRecipe : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Edit gated on a Java recipe";
        public override string Description => "Marks files a Java recipe matches.";
        public override ITreeVisitor<ExecutionContext> GetVisitor() => Check(new JavaFind("TODO"), new MarkFile());
    }

    private sealed class EditGatedOnEitherJavaRecipe : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Edit gated on either Java recipe";
        public override string Description => "Marks files either Java recipe matches.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() =>
            Check(Or(new JavaFind("TODO"), new JavaFind("FIXME")), new MarkFile());
    }

    [Fact]
    public async Task JavaRecipeUsedAsAPreconditionGoesToTheHost()
    {
        var (_, alone) = await Prepare(new EditGatedOnJavaRecipe());
        var (_, inOr) = await Prepare(new EditGatedOnEitherJavaRecipe());

        Assert.Equivalent(new[] { FindJava("TODO"), CsLanguageGate }, alone.EditPreconditions, strict: true);

        Assert.Equivalent(new[]
        {
            new Precondition { Op = "or", Operands = [FindJava("TODO"), FindJava("FIXME")] },
            CsLanguageGate
        }, inOr.EditPreconditions, strict: true);
    }

    private static Precondition FindJava(string find) => new()
    {
        VisitorName = "org.openrewrite.text.Find",
        VisitorOptions = new() { ["find"] = find }
    };

    private sealed class ScanGatedOnJavaSearch : ScanningRecipe<List<J>>
    {
        public readonly List<J> Scanned = [];
        public override string DisplayName => "Scan gated on a Java search";
        public override string Description => "Records files a Java search matches.";
        public override List<J> GetInitialValue(ExecutionContext ctx) => Scanned;

        public override ITreeVisitor<ExecutionContext> GetScanner(List<J> acc) =>
            Check(NeverMatchingJavaSearch, new RecordFile(acc));

        private sealed class RecordFile(List<J> scanned) : CSharpVisitor<ExecutionContext>
        {
            public override J? PreVisit(J tree, ExecutionContext ctx)
            {
                StopAfterPreVisit();
                scanned.Add(tree);
                return tree;
            }
        }
    }

    [Fact]
    public async Task JavaSearchPreconditionOfAScannerRunsOnlyOnTheHost()
    {
        var recipe = new ScanGatedOnJavaSearch();
        var (server, prepared) = await Prepare(recipe);

        Assert.Equivalent(new[]
        {
            new Precondition
            {
                VisitorName = "org.openrewrite.FindSourceFiles",
                VisitorOptions = new() { ["filePattern"] = "**/never-matches.cs" }
            },
            CsLanguageGate
        }, prepared.ScanPreconditions, strict: true);
        server.VisitorFor(prepared.ScanVisitor!, new ExecutionContext()).Visit(Parse(), new ExecutionContext());
        Assert.Single(recipe.Scanned);
    }

    private sealed class EditGatedOnCSharpVisitor : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Edit gated on a C# visitor";
        public override string Description => "Marks files a C# visitor matches.";

        public override ITreeVisitor<ExecutionContext> GetVisitor() =>
            Check(ITreeVisitor<ExecutionContext>.Noop(), new MarkFile());
    }

    [Fact]
    public async Task PreconditionOnlyThePeerCanRunStaysInThePeer()
    {
        var (server, prepared) = await Prepare(new EditGatedOnCSharpVisitor());

        Assert.Equivalent(new[] { CsLanguageGate }, prepared.EditPreconditions, strict: true);
        var cu = Parse();
        Assert.Same(cu, server.VisitorFor(prepared.EditVisitor, new ExecutionContext()).Visit(cu, new ExecutionContext()));
    }

    private static async Task<(RewriteRpcServer, PrepareRecipeResponse)> Prepare(global::OpenRewrite.Core.Recipe recipe)
    {
        var marketplace = new RecipeMarketplace();
        marketplace.Install(recipe, new CategoryDescriptor("Test"));
        var server = new RewriteRpcServer(marketplace);
        var prepared = await server.PrepareRecipe(new PrepareRecipeRequest { Id = recipe.GetType().FullName! });
        return (server, prepared);
    }

    private static J Parse() => new CSharpParser().Parse("class A { }");
}
