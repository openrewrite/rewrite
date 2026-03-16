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

namespace OpenRewrite.Tests.DataTable;

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
        var ctx = new Core.ExecutionContext();
        var row = new TextMatch { SourcePath = "Foo.cs", LineNumber = 42, Match = "hello" };

        store.AcceptRows(true);
        store.InsertRow(table, ctx, row);

        Assert.Single(store.Rows);
        Assert.True(store.Rows.ContainsKey("org.openrewrite.table.TextMatches"));
        Assert.Single(store.Rows["org.openrewrite.table.TextMatches"]);
        Assert.Equal(row, store.Rows["org.openrewrite.table.TextMatches"][0]);
    }

    [Fact]
    public void SuppressesRowsWhenNotAccepting()
    {
        var store = new InMemoryDataTableStore();
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
            "Matches found by a text search.");
        var ctx = new Core.ExecutionContext();

        store.InsertRow(table, ctx, new TextMatch { SourcePath = "Foo.cs", LineNumber = 1, Match = "x" });

        Assert.Empty(store.Rows);
    }

    [Fact]
    public void MultipleRowsSameTable()
    {
        var store = new InMemoryDataTableStore();
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
            "Matches found by a text search.");
        var ctx = new Core.ExecutionContext();

        store.AcceptRows(true);
        store.InsertRow(table, ctx, new TextMatch { SourcePath = "A.cs", LineNumber = 1, Match = "a" });
        store.InsertRow(table, ctx, new TextMatch { SourcePath = "B.cs", LineNumber = 2, Match = "b" });

        Assert.Equal(2, store.Rows["org.openrewrite.table.TextMatches"].Count);
    }

    [Fact]
    public void TracksDataTableDescriptors()
    {
        var store = new InMemoryDataTableStore();
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
            "Matches found by a text search.");
        var ctx = new Core.ExecutionContext();

        store.AcceptRows(true);
        store.InsertRow(table, ctx, new TextMatch { SourcePath = "A.cs", LineNumber = 1, Match = "a" });

        Assert.Single(store.DataTables);
        Assert.True(store.DataTables.ContainsKey("org.openrewrite.table.TextMatches"));
        Assert.Equal("Text Matches", store.DataTables["org.openrewrite.table.TextMatches"].DisplayName);
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
            var ctx = new Core.ExecutionContext();

            store.AcceptRows(true);
            store.InsertRow(table, ctx, new TextMatch { SourcePath = "Foo.cs", LineNumber = 42, Match = "hello" });
            store.InsertRow(table, ctx, new TextMatch { SourcePath = "Bar.cs", LineNumber = 7, Match = "world" });

            var csvPath = Path.Combine(outputDir, "org.openrewrite.table.TextMatches.csv");
            Assert.True(File.Exists(csvPath));

            var lines = File.ReadAllLines(csvPath);
            Assert.Equal(3, lines.Length);
            Assert.Equal("Source Path,Line Number,Match", lines[0]);
            Assert.Equal("Foo.cs,42,hello", lines[1]);
            Assert.Equal("Bar.cs,7,world", lines[2]);
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
            var ctx = new Core.ExecutionContext();

            store.AcceptRows(true);
            store.InsertRow(table, ctx,
                new TextMatch { SourcePath = "path,with,commas.cs", LineNumber = 1, Match = "has \"quotes\"" });
            store.InsertRow(table, ctx,
                new TextMatch { SourcePath = "normal.cs", LineNumber = 2, Match = "line1\nline2" });

            var csvPath = Path.Combine(outputDir, "org.openrewrite.table.TextMatches.csv");
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
    public void SuppressesRowsWhenNotAccepting()
    {
        var outputDir = Path.Combine(Path.GetTempPath(), "datatable-test-" + Guid.NewGuid());
        try
        {
            var store = new CsvDataTableStore(outputDir);
            var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
                "Matches found by a text search.");
            var ctx = new Core.ExecutionContext();

            store.InsertRow(table, ctx, new TextMatch { SourcePath = "Foo.cs", LineNumber = 1, Match = "x" });

            var csvPath = Path.Combine(outputDir, "org.openrewrite.table.TextMatches.csv");
            Assert.False(File.Exists(csvPath));
            Assert.Empty(store.RowCounts);
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
            var ctx = new Core.ExecutionContext();

            store.AcceptRows(true);
            store.InsertRow(table, ctx, new TextMatch { SourcePath = "A.cs", LineNumber = 1, Match = "a" });
            store.InsertRow(table, ctx, new TextMatch { SourcePath = "B.cs", LineNumber = 2, Match = "b" });

            Assert.Single(store.TableNames);
            Assert.Contains("org.openrewrite.table.TextMatches", store.TableNames);
            Assert.Equal(2, store.RowCounts["org.openrewrite.table.TextMatches"]);
        }
        finally
        {
            if (Directory.Exists(outputDir)) Directory.Delete(outputDir, true);
        }
    }
}

public class DataTableTests
{
    [Fact]
    public void InsertRowCreatesDefaultStore()
    {
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
            "Matches found by a text search.");
        var ctx = new Core.ExecutionContext();

        table.InsertRow(ctx, new TextMatch { SourcePath = "Foo.cs", LineNumber = 1, Match = "x" });

        var store = ctx.GetMessage<InMemoryDataTableStore>(DataTable<TextMatch>.DataTableStoreKey);
        Assert.NotNull(store);
        // Default store does not accept rows until AcceptRows(true) is called
        Assert.Empty(store.Rows);
    }

    [Fact]
    public void InsertRowDelegatesToAcceptingStore()
    {
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
            "Matches found by a text search.");
        var ctx = new Core.ExecutionContext();

        var store = new InMemoryDataTableStore();
        store.AcceptRows(true);
        ctx.PutMessage(DataTable<TextMatch>.DataTableStoreKey, store);

        table.InsertRow(ctx, new TextMatch { SourcePath = "Foo.cs", LineNumber = 1, Match = "x" });

        Assert.Single(store.Rows);
        Assert.Equal("Foo.cs", ((TextMatch)store.Rows["org.openrewrite.table.TextMatches"][0]).SourcePath);
    }

    [Fact]
    public void InsertRowUsesExistingStore()
    {
        var table = new DataTable<TextMatch>("org.openrewrite.table.TextMatches", "Text Matches",
            "Matches found by a text search.");
        var ctx = new Core.ExecutionContext();
        var store = new InMemoryDataTableStore();
        store.AcceptRows(true);
        ctx.PutMessage(DataTable<TextMatch>.DataTableStoreKey, store);

        table.InsertRow(ctx, new TextMatch { SourcePath = "Foo.cs", LineNumber = 1, Match = "x" });

        Assert.Single(store.Rows);
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
