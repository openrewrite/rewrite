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

using System.Text.Json;
using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;
using OpenRewrite.CSharp.Rpc;

namespace OpenRewrite.Tests.Core;

public record TextMatch
{
    [Column(DisplayName = "Source Path", Description = "The path of the source file")]
    public string SourcePath { get; init; } = "";

    [Column(DisplayName = "Line Number", Description = "The line number of the match")]
    public int LineNumber { get; init; }

    [Column(DisplayName = "Match", Description = "The matched text")]
    public string Match { get; init; } = "";
}

public class InMemoryDataTableStoreTests
{
    [Fact]
    public void InsertRowsWhenAccepting()
    {
        var store = new InMemoryDataTableStore();
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
            "Matches found by a text search.");
        var ctx = new OpenRewrite.Core.ExecutionContext();
        var row = new TextMatch { SourcePath = "Foo.cs", LineNumber = 42, Match = "hello" };

        store.InsertRow(table, ctx, row);

        Assert.Single(store.GetDataTables());
        var rows = store.GetRows("org.openrewrite.table.TextMatches", "Text Matches").ToList();
        Assert.Single(rows);
        Assert.Equal(row, rows[0]);
    }

    [Fact]
    public void AlwaysAcceptsRows()
    {
        var store = new InMemoryDataTableStore();
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
            "Matches found by a text search.");
        var ctx = new OpenRewrite.Core.ExecutionContext();

        store.InsertRow(table, ctx, new TextMatch { SourcePath = "Foo.cs", LineNumber = 1, Match = "x" });

        Assert.Single(store.GetDataTables());
    }

    [Fact]
    public void MultipleRowsSameTable()
    {
        var store = new InMemoryDataTableStore();
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
            "Matches found by a text search.");
        var ctx = new OpenRewrite.Core.ExecutionContext();

        store.InsertRow(table, ctx, new TextMatch { SourcePath = "A.cs", LineNumber = 1, Match = "a" });
        store.InsertRow(table, ctx, new TextMatch { SourcePath = "B.cs", LineNumber = 2, Match = "b" });

        Assert.Equal(2, store.GetRows("org.openrewrite.table.TextMatches", "Text Matches").Count());
    }

    [Fact]
    public void TracksDataTableDescriptors()
    {
        var store = new InMemoryDataTableStore();
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
            "Matches found by a text search.");
        var ctx = new OpenRewrite.Core.ExecutionContext();

        store.InsertRow(table, ctx, new TextMatch { SourcePath = "A.cs", LineNumber = 1, Match = "a" });

        var dataTables = store.GetDataTables();
        Assert.Single(dataTables);
        var dt = dataTables[0] as DataTable<TextMatch>;
        Assert.NotNull(dt);
        Assert.Equal("Text Matches", dt!.Descriptor.DisplayName);
    }
}

public class CsvDataTableStoreTests
{
    [Fact]
    public void WritesHeaderAndDataRows()
    {
        var outputDir = Path.Combine(Path.GetTempPath(), "datatable-test-" + Guid.NewGuid());
        try
        {
            var store = new CsvDataTableStore(outputDir);
            var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
                "Matches found by a text search.");
            var ctx = new OpenRewrite.Core.ExecutionContext();

            store.InsertRow(table, ctx, new TextMatch { SourcePath = "Foo.cs", LineNumber = 42, Match = "hello" });
            store.InsertRow(table, ctx, new TextMatch { SourcePath = "Bar.cs", LineNumber = 7, Match = "world" });

            var fileKey = CsvDataTableStore.FileKey(table);
            var csvPath = Path.Combine(outputDir, fileKey + ".csv");
            Assert.True(File.Exists(csvPath));

            var lines = File.ReadAllLines(csvPath);
            Assert.Equal(6, lines.Length);
            Assert.StartsWith("# @name", lines[0]);
            Assert.StartsWith("# @instanceName", lines[1]);
            Assert.StartsWith("# @group", lines[2]);
            // Header uses field names (matches the Java writer for shared files).
            Assert.Equal("SourcePath,LineNumber,Match", lines[3]);
            Assert.Equal("Foo.cs,42,hello", lines[4]);
            Assert.Equal("Bar.cs,7,world", lines[5]);
        }
        finally
        {
            if (Directory.Exists(outputDir)) Directory.Delete(outputDir, true);
        }
    }

    [Fact]
    public void EscapesCsvSpecialCharacters()
    {
        var outputDir = Path.Combine(Path.GetTempPath(), "datatable-test-" + Guid.NewGuid());
        try
        {
            var store = new CsvDataTableStore(outputDir);
            var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
                "Matches found by a text search.");
            var ctx = new OpenRewrite.Core.ExecutionContext();

            store.InsertRow(table, ctx,
                new TextMatch { SourcePath = "path,with,commas.cs", LineNumber = 1, Match = "has \"quotes\"" });
            store.InsertRow(table, ctx,
                new TextMatch { SourcePath = "normal.cs", LineNumber = 2, Match = "line1\nline2" });

            var fileKey = CsvDataTableStore.FileKey(table);
            var csvPath = Path.Combine(outputDir, fileKey + ".csv");
            var content = File.ReadAllText(csvPath);
            // Row with commas and quotes
            Assert.Contains("\"path,with,commas.cs\",1,\"has \"\"quotes\"\"\"", content);
            // Row with embedded newline — value is quoted
            Assert.Contains("normal.cs,2,\"line1\nline2\"", content);
        }
        finally
        {
            if (Directory.Exists(outputDir)) Directory.Delete(outputDir, true);
        }
    }

    [Fact]
    public void AlwaysAcceptsRows()
    {
        var outputDir = Path.Combine(Path.GetTempPath(), "datatable-test-" + Guid.NewGuid());
        try
        {
            var store = new CsvDataTableStore(outputDir);
            var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
                "Matches found by a text search.");
            var ctx = new OpenRewrite.Core.ExecutionContext();

            store.InsertRow(table, ctx, new TextMatch { SourcePath = "Foo.cs", LineNumber = 1, Match = "x" });

            var fileKey = CsvDataTableStore.FileKey(table);
            var csvPath = Path.Combine(outputDir, fileKey + ".csv");
            Assert.True(File.Exists(csvPath));
            Assert.Equal(1, store.RowCounts[fileKey]);
        }
        finally
        {
            if (Directory.Exists(outputDir)) Directory.Delete(outputDir, true);
        }
    }

    [Fact]
    public void ExposesRowCountsAndTableNames()
    {
        var outputDir = Path.Combine(Path.GetTempPath(), "datatable-test-" + Guid.NewGuid());
        try
        {
            var store = new CsvDataTableStore(outputDir);
            var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
                "Matches found by a text search.");
            var ctx = new OpenRewrite.Core.ExecutionContext();

            store.InsertRow(table, ctx, new TextMatch { SourcePath = "A.cs", LineNumber = 1, Match = "a" });
            store.InsertRow(table, ctx, new TextMatch { SourcePath = "B.cs", LineNumber = 2, Match = "b" });

            var fileKey = CsvDataTableStore.FileKey(table);
            Assert.Single(store.TableKeys);
            Assert.Contains(fileKey, store.TableKeys);
            Assert.Equal(2, store.RowCounts[fileKey]);
        }
        finally
        {
            if (Directory.Exists(outputDir)) Directory.Delete(outputDir, true);
        }
    }

    [Fact]
    public void WritesPrefixAndSuffixColumns()
    {
        var outputDir = Path.Combine(Path.GetTempPath(), "datatable-test-" + Guid.NewGuid());
        try
        {
            var store = new CsvDataTableStore(outputDir,
                new Dictionary<string, string> { ["repositoryOrigin"] = "github.com/acme/widgets" },
                new Dictionary<string, string> { ["organization"] = "Acme" });
            var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
                "Matches found by a text search.");
            var ctx = new OpenRewrite.Core.ExecutionContext();

            store.InsertRow(table, ctx, new TextMatch { SourcePath = "Foo.cs", LineNumber = 42, Match = "hello" });

            var fileKey = CsvDataTableStore.FileKey(table);
            var lines = File.ReadAllLines(Path.Combine(outputDir, fileKey + ".csv"));
            Assert.Equal("repositoryOrigin,SourcePath,LineNumber,Match,organization", lines[3]);
            Assert.Equal("github.com/acme/widgets,Foo.cs,42,hello,Acme", lines[4]);
        }
        finally
        {
            if (Directory.Exists(outputDir)) Directory.Delete(outputDir, true);
        }
    }

    [Fact]
    public void DecidesHeaderByFileExistenceAcrossStores()
    {
        var outputDir = Path.Combine(Path.GetTempPath(), "datatable-test-" + Guid.NewGuid());
        try
        {
            var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
                "Matches found by a text search.");
            var ctx = new OpenRewrite.Core.ExecutionContext();

            // Two store instances on one dir simulate the Java host + an RPC runtime sharing one file.
            new CsvDataTableStore(outputDir)
                .InsertRow(table, ctx, new TextMatch { SourcePath = "A.cs", LineNumber = 1, Match = "a" });
            new CsvDataTableStore(outputDir)
                .InsertRow(table, ctx, new TextMatch { SourcePath = "B.cs", LineNumber = 2, Match = "b" });

            var fileKey = CsvDataTableStore.FileKey(table);
            var lines = File.ReadAllLines(Path.Combine(outputDir, fileKey + ".csv"));
            // 3 comment lines + 1 header + 2 data rows
            Assert.Equal(6, lines.Length);
            Assert.Equal("SourcePath,LineNumber,Match", lines[3]);
            Assert.Equal("A.cs,1,a", lines[4]);
            Assert.Equal("B.cs,2,b", lines[5]);
        }
        finally
        {
            if (Directory.Exists(outputDir)) Directory.Delete(outputDir, true);
        }
    }
}

public class SetDataTableStoreTests
{
    [Fact]
    public void CsvReconstructsRawCsvStoreAtOutputDir()
    {
        var outputDir = Path.Combine(Path.GetTempPath(), "datatable-test-" + Guid.NewGuid());
        try
        {
            var store = new SetDataTableStoreRequest.Csv
            {
                OutputDir = outputDir,
                PrefixColumns = new Dictionary<string, string> { ["repositoryOrigin"] = "acme" },
                SuffixColumns = new Dictionary<string, string> { ["organization"] = "Acme" }
            }.ToDataTableStore();

            var csv = Assert.IsType<CsvDataTableStore>(store);
            Assert.Equal("acme", csv.PrefixColumns["repositoryOrigin"]);
            Assert.Equal("Acme", csv.SuffixColumns["organization"]);

            var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
                "Matches found by a text search.");
            csv.InsertRow(table, new OpenRewrite.Core.ExecutionContext(),
                new TextMatch { SourcePath = "Foo.cs", LineNumber = 1, Match = "x" });

            var fileKey = CsvDataTableStore.FileKey(table);
            var lines = File.ReadAllLines(Path.Combine(outputDir, fileKey + ".csv"));
            Assert.Equal("repositoryOrigin,SourcePath,LineNumber,Match,organization", lines[3]);
            Assert.Equal("acme,Foo.cs,1,x,Acme", lines[4]);
        }
        finally
        {
            if (Directory.Exists(outputDir)) Directory.Delete(outputDir, true);
        }
    }

    [Fact]
    public void NoopKeepsRowsInMemory()
    {
        var store = new SetDataTableStoreRequest.NoOp().ToDataTableStore();
        Assert.IsType<InMemoryDataTableStore>(store);
    }

    [Fact]
    public void UnderSpecifiedCsvFallsBackToInMemory()
    {
        var store = new SetDataTableStoreRequest.Csv().ToDataTableStore();
        Assert.IsType<InMemoryDataTableStore>(store);
    }

    [Fact]
    public void DeserializesCsvWireFormByKindDiscriminator()
    {
        const string json =
            """{"kind":"CSV","outputDir":"/tmp/dt","prefixColumns":{"repositoryOrigin":"acme"},"suffixColumns":{}}""";
        var request = JsonSerializer.Deserialize<SetDataTableStoreRequest>(json, RpcJson.Options);
        var csv = Assert.IsType<SetDataTableStoreRequest.Csv>(request);
        Assert.Equal("/tmp/dt", csv.OutputDir);
        Assert.Equal("acme", csv.PrefixColumns!["repositoryOrigin"]);
    }

    [Fact]
    public void DeserializesNoOpWireForm()
    {
        var request = JsonSerializer.Deserialize<SetDataTableStoreRequest>(
            """{"kind":"NOOP"}""", RpcJson.Options);
        Assert.IsType<SetDataTableStoreRequest.NoOp>(request);
    }
}

public class DataTableTests
{
    [Fact]
    public void InsertRowCreatesDefaultStore()
    {
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
            "Matches found by a text search.");
        var ctx = new OpenRewrite.Core.ExecutionContext();

        table.InsertRow(ctx, new TextMatch { SourcePath = "Foo.cs", LineNumber = 1, Match = "x" });

        var store = ctx.GetMessage<InMemoryDataTableStore>(DataTable<TextMatch>.DataTableStoreKey);
        Assert.NotNull(store);
        Assert.Single(store!.GetDataTables());
    }

    [Fact]
    public void InsertRowDelegatesToStore()
    {
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
            "Matches found by a text search.");
        var ctx = new OpenRewrite.Core.ExecutionContext();

        var store = new InMemoryDataTableStore();
        ctx.PutMessage(DataTable<TextMatch>.DataTableStoreKey, store);

        table.InsertRow(ctx, new TextMatch { SourcePath = "Foo.cs", LineNumber = 1, Match = "x" });

        var rows = store.GetRows("org.openrewrite.table.TextMatches", "Text Matches").ToList();
        Assert.Single(rows);
        Assert.Equal("Foo.cs", ((TextMatch)rows[0]).SourcePath);
    }

    [Fact]
    public void InsertRowUsesExistingStore()
    {
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
            "Matches found by a text search.");
        var ctx = new OpenRewrite.Core.ExecutionContext();
        var store = new InMemoryDataTableStore();
        ctx.PutMessage(DataTable<TextMatch>.DataTableStoreKey, store);

        table.InsertRow(ctx, new TextMatch { SourcePath = "Foo.cs", LineNumber = 1, Match = "x" });

        Assert.Single(store.GetDataTables());
    }

    [Fact]
    public void DescriptorReflectsColumnAttributes()
    {
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
            "Matches found by a text search.");

        var descriptor = table.Descriptor;

        Assert.Equal("org.openrewrite.table.TextMatches", descriptor.Name);
        Assert.Equal("Text Matches", descriptor.DisplayName);
        Assert.Equal("Matches found by a text search.", descriptor.Description);
        Assert.Equal(3, descriptor.Columns.Count);
        Assert.Equal("SourcePath", descriptor.Columns[0].Name);
        Assert.Equal("Source Path", descriptor.Columns[0].DisplayName);
        Assert.Equal("The path of the source file", descriptor.Columns[0].Description);
    }
}

public class FileKeyMatchesJavaTests
{
    // fileKey must match the Java host's exactly (incl. the sha256[..4] suffix), or a shared table resolves to two files.

    [Fact]
    public void DefaultTableUsesBareFullyQualifiedName()
    {
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text matches",
            "Lines matching a text search.");
        Assert.Equal("org.openrewrite.table.TextMatches", CsvDataTableStore.FileKey(table));
    }

    [Fact]
    public void CustomInstanceNameAppendsSanitizedSuffix()
    {
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text matches",
            "Lines matching a text search.") { InstanceName = "Custom run" };
        Assert.Equal("org.openrewrite.table.TextMatches--custom-run-6251", CsvDataTableStore.FileKey(table));
    }

    [Fact]
    public void GroupTakesPrecedenceAndIsSanitized()
    {
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text matches",
            "Lines matching a text search.") { InstanceName = "Custom run", Group = "team-alpha" };
        Assert.Equal("org.openrewrite.table.TextMatches--team-alpha-29c4", CsvDataTableStore.FileKey(table));
    }

    [Fact]
    public void GroupEqualToNameUsesBareName()
    {
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text matches",
            "Lines matching a text search.") { Group = "org.openrewrite.table.TextMatches" };
        Assert.Equal("org.openrewrite.table.TextMatches", CsvDataTableStore.FileKey(table));
    }
}
