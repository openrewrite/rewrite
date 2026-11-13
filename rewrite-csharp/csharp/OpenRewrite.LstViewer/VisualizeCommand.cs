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
using System.ComponentModel;
using System.Text;
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using Spectre.Console;
using Spectre.Console.Cli;

namespace OpenRewrite.LstViewer;

internal sealed class VisualizeSettings : CommandSettings
{
    [CommandArgument(0, "[file]")]
    [Description("File holding the C# to visualize. Omit it to use --code or to read stdin.")]
    public string? File { get; init; }

    [CommandOption("-c|--code <CSHARP>")]
    [Description("The C# to visualize, given inline.")]
    public string? Code { get; init; }

    [CommandOption("-w|--wrap <MODE>")]
    [Description("How to make the snippet a compilation unit: auto, none, member, statement or expression.")]
    [DefaultValue("auto")]
    public string Wrap { get; init; } = "auto";

    [CommandOption("-f|--focus <LST_TYPE>")]
    [Description("Start the outline at the first node of this LST type, e.g. InstanceOf or Cs.UsingDirective.")]
    public string? Focus { get; init; }

    [CommandOption("--full-tree")]
    [Description("Outline the whole compilation unit, including any wrapper added around the snippet.")]
    public bool FullTree { get; init; }

    [CommandOption("-t|--types")]
    [Description("Add a column with the JavaType attested on each node.")]
    public bool Types { get; init; }

    [CommandOption("--no-semantic")]
    [Description("Parse without a Roslyn semantic model: faster, but nothing is type-attested.")]
    public bool NoSemantic { get; init; }

    [CommandOption("--framework <TFM>")]
    [Description("Reference assemblies for attestation: net9, net10, net48, aspnet9 or aspnet10.")]
    [DefaultValue("net9")]
    public string Framework { get; init; } = "net9";

    [CommandOption("--width <CHARS>")]
    [Description("Characters of source to show per row before truncating.")]
    [DefaultValue(100)]
    public int Width { get; init; }

    [CommandOption("--no-color")]
    [Description("Plain output. The node span is delimited with « and » instead of inverted.")]
    public bool NoColor { get; init; }

    public override ValidationResult Validate() => Width > 0
        ? ValidationResult.Success()
        : ValidationResult.Error("--width must be greater than zero.");
}

internal sealed class VisualizeCommand : AsyncCommand<VisualizeSettings>
{
    private const string UnlocatableNote = "not printed";
    private const char EmptyNodeCaret = '\u2038';
    private const char Ellipsis = '\u2026';
    private const string SpanOpen = "\u00ab";
    private const string SpanClose = "\u00bb";
    private const string LooseOpen = "\u2039";
    private const string LooseClose = "\u203a";

    private sealed record OutlineRow(
        string Tree,
        string Property,
        string Type,
        string SourceMarkup,
        int SourceWidth,
        bool IsMarker,
        bool Approximate,
        bool Unlocatable);

    protected override async Task<int> ExecuteAsync(CommandContext context, VisualizeSettings settings,
        CancellationToken cancellation)
    {
        if (!Snippet.TryParseWrap(settings.Wrap, out var wrap))
        {
            await Console.Error.WriteLineAsync(
                $"Unknown --wrap mode '{settings.Wrap}'. Use auto, none, member, statement or expression.");
            return 1;
        }

        var code = ReadInput(settings);
        if (string.IsNullOrWhiteSpace(code))
        {
            await Console.Error.WriteLineAsync(
                "No C# to visualize. Pass a file, use --code, or pipe the snippet in on stdin.");
            return 1;
        }

        var parsed = await Snippet.ParseAsync(code, wrap, settings.Framework, !settings.NoSemantic, cancellation);

        J? focus = null;
        if (settings.Focus != null)
        {
            focus = LstOutline.Find(parsed.Tree, settings.Focus);
            if (focus == null)
            {
                await Console.Error.WriteLineAsync($"No node of type '{settings.Focus}' in the parsed snippet.");
                return 1;
            }
        }
        else if (!settings.FullTree)
        {
            focus = WrapperBody(parsed);
        }

        var outline = LstOutline.Of(parsed.Tree, focus);
        var console = CreateConsole(settings);

        var invertSpans = !settings.NoColor && console.Profile.Capabilities.Ansi;
        var rows = Flatten(outline, settings, invertSpans);
        console.Profile.Width = Math.Max(console.Profile.Width, EstimateWidth(rows, settings));

        WriteDiagnostics(console, parsed, outline.Source, settings);
        WriteTable(console, rows, settings);
        WriteLegend(console, rows, invertSpans);
        return 0;
    }

    private static string? ReadInput(VisualizeSettings settings)
    {
        if (settings.Code != null)
        {
            return settings.Code;
        }
        if (settings.File != null)
        {
            return File.ReadAllText(settings.File);
        }
        return Console.IsInputRedirected ? Console.In.ReadToEnd() : null;
    }

    private static J? WrapperBody(ParsedSnippet parsed) => parsed.Wrap switch
    {
        WrapMode.Member => (LstOutline.Find(parsed.Tree, nameof(ClassDeclaration)) as ClassDeclaration)?.Body,
        WrapMode.Statement => (LstOutline.Find(parsed.Tree, nameof(MethodDeclaration)) as MethodDeclaration)?.Body,
        WrapMode.Expression => (LstOutline.Find(parsed.Tree, nameof(NamedVariable)) as NamedVariable)?.Initializer?.Element,
        _ => null
    };

    private static void WriteDiagnostics(IAnsiConsole console, ParsedSnippet parsed, string reprinted,
        VisualizeSettings settings)
    {
        if (parsed.Wrap != WrapMode.None)
        {
            console.Write(new Panel(new Text(parsed.Source.TrimEnd()))
                .Header($" [bold]{parsed.Wrap.ToString().ToLowerInvariant()}[/] wrap ")
                .Border(BoxBorder.Rounded)
                .BorderColor(Color.Grey));
        }

        foreach (var error in parsed.SyntaxErrors.Take(5))
        {
            console.MarkupLine($"[yellow]syntax error[/] {Markup.Escape(error.GetMessage())} "
                               + $"[grey]at {error.Location.GetLineSpan().StartLinePosition}[/]");
        }

        if (reprinted != parsed.Source)
        {
            console.MarkupLine("[red]the LST does not print back to the source it was parsed from — "
                               + "the parser lost information[/]");
            console.MarkupLine($"[grey]first difference at offset {FirstDifference(parsed.Source, reprinted)}[/]");
        }

        if (settings is { Types: true, NoSemantic: true })
        {
            console.MarkupLine("[yellow]--types with --no-semantic: nothing is attested[/]");
        }
    }

    private static int FirstDifference(string left, string right)
    {
        var shared = Math.Min(left.Length, right.Length);
        for (var i = 0; i < shared; i++)
        {
            if (left[i] != right[i])
            {
                return i;
            }
        }
        return shared;
    }

    private static List<OutlineRow> Flatten(LstOutline outline, VisualizeSettings settings, bool invertSpans)
    {
        var rows = new List<OutlineRow>();
        var glyphs = SourceGlyphs.Of(outline.Source);
        Append(outline.Root, "", true, true);
        return rows;

        void Append(LstOutlineNode node, string indent, bool isLast, bool isRoot)
        {
            var connector = isRoot ? "" : isLast ? "└─ " : "├─ ";
            var unlocatable = !node.IsMarker && node.Start < 0;
            var source = node.IsMarker
                ? Styled(node.MarkerState, "blue italic")
                : unlocatable
                    ? Styled(UnlocatableNote, "grey")
                    : SpanMarkup(glyphs, node.Start, node.Length, settings.Width, invertSpans,
                        node.IsApproximate);

            rows.Add(new OutlineRow(
                indent + connector + node.Type,
                node.Property + PaddingTag(node.Padding),
                node.AttestedType ?? "",
                source,
                node.IsMarker ? node.MarkerState.Length : unlocatable ? UnlocatableNote.Length : settings.Width + 2,
                node.IsMarker,
                !node.IsMarker && node.IsApproximate,
                unlocatable));

            var childIndent = isRoot ? "" : indent + (isLast ? "   " : "│  ");
            for (var i = 0; i < node.Children.Count; i++)
            {
                Append(node.Children[i], childIndent, i == node.Children.Count - 1, false);
            }
        }
    }

    private static string Styled(string text, string style) =>
        text.Length == 0 ? "" : $"[{style}]{Markup.Escape(text)}[/]";

    private static string PaddingTag(PaddingKind padding) => padding switch
    {
        PaddingKind.RightPadded => " (RP)",
        PaddingKind.LeftPadded => " (LP)",
        PaddingKind.Container => " (JC)",
        _ => ""
    };

    private static IAnsiConsole CreateConsole(VisualizeSettings settings) =>
        AnsiConsole.Create(new AnsiConsoleSettings
        {
            Ansi = settings.NoColor ? AnsiSupport.No : AnsiSupport.Detect,
            ColorSystem = settings.NoColor ? ColorSystemSupport.NoColors : ColorSystemSupport.Detect,
            Out = new AnsiConsoleOutput(Console.Out)
        });

    private static int EstimateWidth(List<OutlineRow> rows, VisualizeSettings settings) =>
        rows.Count == 0
            ? 0
            : rows.Max(row => row.Tree.Length)
              + rows.Max(row => row.Property.Length)
              + (settings.Types ? rows.Max(row => row.Type.Length) : 0)
              + rows.Max(row => row.SourceWidth)
              + 24;

    private static void WriteTable(IAnsiConsole console, List<OutlineRow> rows, VisualizeSettings settings)
    {
        var table = new Table().Border(TableBorder.Rounded).BorderColor(Color.Grey);
        table.AddColumn(new TableColumn("[bold]Tree[/]").NoWrap());
        table.AddColumn(new TableColumn("[bold]Parent property[/]").NoWrap());
        if (settings.Types)
        {
            table.AddColumn(new TableColumn("[bold]Attested type[/]").NoWrap());
        }
        table.AddColumn(new TableColumn("[bold]Source[/]").NoWrap());

        foreach (var row in rows)
        {
            var cells = new List<string>
            {
                row.IsMarker ? Styled(row.Tree, "blue italic") : Markup.Escape(row.Tree),
                row.IsMarker ? Styled(row.Property, "blue italic") : Markup.Escape(row.Property)
            };
            if (settings.Types)
            {
                cells.Add(Markup.Escape(row.Type));
            }
            cells.Add(row.SourceMarkup);
            table.AddRow(cells.Select(cell => new Markup(cell)).ToArray());
        }

        console.Write(table);
    }

    private static void WriteLegend(IAnsiConsole console, List<OutlineRow> rows, bool invertSpans)
    {
        console.MarkupLine("[grey](RP) in a JRightPadded   (LP) in a JLeftPadded   (JC) in a JContainer   "
                           + "[blue italic]markers in blue[/][/]");
        console.MarkupLine(invertSpans
            ? "[grey]The inverted span is the source a node owns, its prefix whitespace included.[/]"
            : "[grey]The span between « and » is the source a node owns, its prefix whitespace included.[/]");
        console.MarkupLine($"[grey]A node that covers no text is marked with {EmptyNodeCaret} "
                           + "where it sits.[/]");
        if (rows.Any(row => row.Approximate))
        {
            console.MarkupLine(invertSpans
                ? "[grey]An underlined span is approximate: CSharpPrinter writes that node inline, so its extent "
                  + "is derived from its neighbours and may be wider than the text it owns.[/]"
                : $"[grey]A span between {LooseOpen} and {LooseClose} is approximate: CSharpPrinter writes that "
                  + "node inline, so its extent is derived from its neighbours and may be wider than the text it "
                  + "owns.[/]");
        }
        if (rows.Any(row => row.Unlocatable))
        {
            console.MarkupLine($"[grey]'{UnlocatableNote}' means the node contributes no text to this source at "
                               + "all, so it has nowhere to point.[/]");
        }
    }

    private static string SpanMarkup(SourceGlyphs glyphs, int start, int length, int width, bool invert,
        bool approximate)
    {
        if (start < 0 || glyphs.Count == 0)
        {
            return "";
        }

        var end = start + length;
        var first = glyphs.IndexAtOrAfter(start);
        var last = length == 0 ? first - 1 : glyphs.IndexAtOrAfter(end) - 1;
        var caret = last < first ? first : -1;
        var anchor = caret >= 0 ? caret : first;
        var from = WindowStart(glyphs.Count, anchor, caret >= 0 ? anchor : last, width);
        var to = Math.Min(glyphs.Count, from + width);

        var builder = new StringBuilder();
        var open = false;

        void Mark(bool wanted)
        {
            if (wanted == open)
            {
                return;
            }
            builder.Append(Delimiter(wanted, invert, approximate));
            open = wanted;
        }

        if (from > 0)
        {
            builder.Append(Ellipsis);
        }

        for (var i = from; i < to; i++)
        {
            if (i == caret)
            {
                Mark(true);
                builder.Append(EmptyNodeCaret);
            }

            var offset = glyphs.OffsetAt(i);
            Mark(caret < 0 && offset >= start && offset < end);

            var glyph = glyphs.GlyphAt(i);
            builder.Append(glyph switch { '[' => "[[", ']' => "]]", _ => glyph.ToString() });
        }

        if (caret >= to)
        {
            Mark(true);
            builder.Append(EmptyNodeCaret);
        }

        Mark(false);
        if (to < glyphs.Count)
        {
            builder.Append(Ellipsis);
        }
        return builder.ToString();
    }

    private static string Delimiter(bool opening, bool invert, bool approximate) => invert
        ? opening ? approximate ? "[underline]" : "[invert]" : "[/]"
        : opening
            ? approximate ? LooseOpen : SpanOpen
            : approximate ? LooseClose : SpanClose;

    private sealed class SourceGlyphs
    {
        private readonly char[] _glyphs;
        private readonly int[] _offsets;

        private SourceGlyphs(char[] glyphs, int[] offsets)
        {
            _glyphs = glyphs;
            _offsets = offsets;
        }

        public int Count => _glyphs.Length;

        public char GlyphAt(int index) => _glyphs[index];

        public int OffsetAt(int index) => _offsets[index];

        public int IndexAtOrAfter(int offset)
        {
            var found = Array.BinarySearch(_offsets, offset);
            return found >= 0 ? found : ~found;
        }

        public static SourceGlyphs Of(string source)
        {
            var glyphs = new List<char>(source.Length);
            var offsets = new List<int>(source.Length);
            var run = -1;

            for (var i = 0; i < source.Length; i++)
            {
                if (char.IsWhiteSpace(source[i]))
                {
                    if (run < 0)
                    {
                        run = i;
                    }
                    continue;
                }

                if (run >= 0)
                {
                    glyphs.Add(' ');
                    offsets.Add(run);
                    run = -1;
                }
                glyphs.Add(source[i]);
                offsets.Add(i);
            }

            if (run >= 0)
            {
                glyphs.Add(' ');
                offsets.Add(run);
            }
            return new SourceGlyphs(glyphs.ToArray(), offsets.ToArray());
        }
    }

    private static int WindowStart(int total, int first, int last, int width)
    {
        if (total <= width || first < 0)
        {
            return 0;
        }
        var spanWidth = last - first + 1;
        var from = spanWidth >= width ? first : first - (width - spanWidth) / 2;
        return Math.Clamp(from, 0, total - width);
    }
}
