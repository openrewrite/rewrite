/*
 * Copyright 2026 the original author or authors.
 * Licensed under the Moderne Source Available License (the "License");
 * See https://docs.moderne.io/licensing/moderne-source-available-license
 */
using OpenRewrite.Core;
using OpenRewrite.CSharp;
using OpenRewrite.CSharp.Rpc;
using OpenRewrite.Java;
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Tests.Rpc;

public class ScanningAccumulatorPerCycleTest
{
    private sealed class RecordScans : ScanningRecipe<List<Guid>>
    {
        public List<Guid>? Generated;
        public List<Guid>? Edited;
        public override string DisplayName => "Record scans";
        public override string Description => "Records the files it scans.";
        public override List<Guid> GetInitialValue(ExecutionContext ctx) => [];
        public override ITreeVisitor<ExecutionContext> GetScanner(List<Guid> acc) => new RecordFile(acc);

        public override IEnumerable<SourceFile> Generate(List<Guid> acc, ExecutionContext ctx)
        {
            Generated = acc;
            return [];
        }

        public override ITreeVisitor<ExecutionContext> GetVisitor(List<Guid> acc)
        {
            Edited = acc;
            return ITreeVisitor<ExecutionContext>.Noop();
        }

        private sealed class RecordFile(List<Guid> scanned) : CSharpVisitor<ExecutionContext>
        {
            public override J? PreVisit(J tree, ExecutionContext ctx)
            {
                StopAfterPreVisit();
                scanned.Add(tree.Id);
                return tree;
            }
        }
    }

    private readonly RecordScans _recipe = new();
    private readonly ExecutionContext _ctx = new();
    private readonly J _cu = new CSharpParser().Parse("class A { }");

    [Fact]
    public async Task EachCycleScansIntoAFreshAccumulator()
    {
        var (server, prepared) = await Prepare();

        await Cycle(server, prepared);
        await Cycle(server, prepared);

        Assert.Single(_recipe.Generated!);
        Assert.Same(_recipe.Generated, _recipe.Edited);
    }

    private async Task Cycle(RewriteRpcServer server, PrepareRecipeResponse prepared)
    {
        server.VisitorFor(prepared.ScanVisitor!, _ctx).Visit(_cu, _ctx);
        await server.Generate(new GenerateRequest { Id = prepared.Id });
        server.VisitorFor(prepared.EditVisitor, _ctx).Visit(_cu, _ctx);
    }

    private async Task<(RewriteRpcServer, PrepareRecipeResponse)> Prepare()
    {
        var marketplace = new RecipeMarketplace();
        marketplace.Install(_recipe, new CategoryDescriptor("Test"));
        var server = new RewriteRpcServer(marketplace);
        var prepared = await server.PrepareRecipe(new PrepareRecipeRequest { Id = typeof(RecordScans).FullName! });
        return (server, prepared);
    }
}
