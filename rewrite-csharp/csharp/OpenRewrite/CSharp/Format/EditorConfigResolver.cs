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
namespace OpenRewrite.CSharp.Format;

/// <summary>
/// Resolves .editorconfig files hierarchically for C# source files.
/// Walks from the file's directory up to the project root, collecting and layering
/// .editorconfig settings (child overrides parent). Stops at <c>root = true</c>.
/// Caches resolved <see cref="CSharpFormatStyle"/> instances per directory.
/// </summary>
public class EditorConfigResolver
{
    private readonly string _projectRoot;
    private readonly Dictionary<string, CSharpFormatStyle> _cache = new();

    public EditorConfigResolver(string projectRoot)
    {
        _projectRoot = Path.GetFullPath(projectRoot);
    }

    /// <summary>
    /// Resolves the <see cref="CSharpFormatStyle"/> for the given absolute file path
    /// by collecting .editorconfig files from the file's directory up to the project root.
    /// Results are cached per directory.
    /// </summary>
    public CSharpFormatStyle Resolve(string absoluteFilePath)
    {
        var dir = Path.GetDirectoryName(Path.GetFullPath(absoluteFilePath))!;

        if (_cache.TryGetValue(dir, out var cached))
            return cached;

        var style = ResolveForDirectory(dir);
        _cache[dir] = style;
        return style;
    }

    private CSharpFormatStyle ResolveForDirectory(string dir)
    {
        // Collect .editorconfig files from dir up to project root
        var configs = CollectEditorConfigs(dir);

        // Merge: parent configs come first, child configs override
        var merged = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        foreach (var config in configs)
        {
            foreach (var (key, value) in config)
            {
                merged[key] = value;
            }
        }

        return BuildStyle(merged);
    }

    /// <summary>
    /// Collects applicable .editorconfig settings from directory upward.
    /// Returns a list ordered parent-first (so child can override parent when merged in order).
    /// </summary>
    private List<Dictionary<string, string>> CollectEditorConfigs(string startDir)
    {
        var configs = new List<(string dir, Dictionary<string, string> settings, bool isRoot)>();

        var current = startDir;
        while (current != null && IsWithinProjectRoot(current))
        {
            var editorConfigPath = Path.Combine(current, ".editorconfig");
            if (File.Exists(editorConfigPath))
            {
                var (settings, isRoot) = ParseEditorConfig(editorConfigPath);
                configs.Add((current, settings, isRoot));
                if (isRoot) break;
            }

            var parent = Path.GetDirectoryName(current);
            if (parent == current) break; // filesystem root
            current = parent;
        }

        // Reverse so parent comes first, child last (child overrides)
        configs.Reverse();
        return configs.Select(c => c.settings).ToList();
    }

    private static readonly StringComparison PathComparison =
        OperatingSystem.IsWindows() ? StringComparison.OrdinalIgnoreCase : StringComparison.Ordinal;

    private bool IsWithinProjectRoot(string dir)
    {
        var fullDir = Path.GetFullPath(dir);
        return fullDir.StartsWith(_projectRoot, PathComparison);
    }

    /// <summary>
    /// Parses an .editorconfig file, extracting settings from [*] and [*.cs] sections.
    /// Returns the merged settings and whether <c>root = true</c> was found.
    /// </summary>
    private static (Dictionary<string, string> settings, bool isRoot) ParseEditorConfig(string path)
    {
        var settings = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        var isRoot = false;
        var inApplicableSection = false;
        var inPreamble = true; // before any section header

        foreach (var rawLine in File.ReadLines(path))
        {
            var line = rawLine.Trim();

            // Skip empty lines and comments
            if (line.Length == 0 || line[0] == '#' || line[0] == ';')
                continue;

            // Section header
            if (line[0] == '[' && line[^1] == ']')
            {
                inPreamble = false;
                var pattern = line[1..^1].Trim();
                inApplicableSection = IsApplicableToCSharp(pattern);
                continue;
            }

            // Key = value
            var eqIndex = line.IndexOf('=');
            if (eqIndex < 0) continue;

            var key = line[..eqIndex].Trim();
            var rawValue = line[(eqIndex + 1)..].Trim();
            // Strip optional :severity suffix (e.g., "true:error", "all:suggestion")
            var colonIdx = rawValue.IndexOf(':');
            var value = colonIdx >= 0 ? rawValue[..colonIdx].Trim() : rawValue;

            // root = true can appear in preamble (before any section)
            if (inPreamble && key.Equals("root", StringComparison.OrdinalIgnoreCase) &&
                value.Equals("true", StringComparison.OrdinalIgnoreCase))
            {
                isRoot = true;
                continue;
            }

            if (inApplicableSection)
            {
                settings[key] = value;
            }
        }

        return (settings, isRoot);
    }

    /// <summary>
    /// Checks if a section pattern applies to C# files.
    /// Supports: *, *.cs, *.{cs,vb}, {*.cs}
    /// </summary>
    private static bool IsApplicableToCSharp(string pattern)
    {
        // Universal wildcard
        if (pattern == "*")
            return true;

        // Exact extension match
        if (pattern.Equals("*.cs", StringComparison.OrdinalIgnoreCase))
            return true;

        // Brace expansion: *.{cs,vb} or {*.cs,*.vb}
        if (pattern.Contains('{') && pattern.Contains('}'))
        {
            var braceStart = pattern.IndexOf('{');
            var braceEnd = pattern.IndexOf('}');
            if (braceStart < braceEnd)
            {
                var alternatives = pattern[(braceStart + 1)..braceEnd].Split(',');
                var prefix = pattern[..braceStart];
                var suffix = pattern[(braceEnd + 1)..];

                foreach (var alt in alternatives)
                {
                    var expanded = prefix + alt.Trim() + suffix;
                    if (expanded.Equals("*.cs", StringComparison.OrdinalIgnoreCase))
                        return true;
                }
            }
        }

        return false;
    }

    private static CSharpFormatStyle BuildStyle(Dictionary<string, string> settings)
    {
        var useTabs = GetBoolSetting(settings, "indent_style", "tab", false);
        var tabSize = GetIntSetting(settings, "tab_width", 4);
        // EditorConfig spec: indent_size = tab means use tab_width value
        var indentSize = settings.TryGetValue("indent_size", out var indSizeVal) &&
                         indSizeVal.Trim().Equals("tab", StringComparison.OrdinalIgnoreCase)
            ? tabSize
            : GetIntSetting(settings, "indent_size", 4);
        // If tab_width was not explicitly set, default it to indent_size
        if (!settings.ContainsKey("tab_width"))
            tabSize = indentSize;
        var newLine = GetNewLineSetting(settings);

        // Brace placement — parse csharp_new_line_before_open_brace
        var braceDefaults = true; // Roslyn default: all true (Allman)
        var bracesInTypes = braceDefaults;
        var bracesInMethods = braceDefaults;
        var bracesInProperties = braceDefaults;
        var bracesInAccessors = braceDefaults;
        var bracesInAnonymousMethods = braceDefaults;
        var bracesInAnonymousTypes = braceDefaults;
        var bracesInControlBlocks = braceDefaults;
        var bracesInLambdas = braceDefaults;
        var bracesInObjectCollectionArray = braceDefaults;
        var bracesInLocalFunctions = braceDefaults;

        if (settings.TryGetValue("csharp_new_line_before_open_brace", out var braceValue))
        {
            braceValue = braceValue.Trim().ToLowerInvariant();
            if (braceValue == "all")
            {
                // All true — already defaults
            }
            else if (braceValue == "none")
            {
                bracesInTypes = false;
                bracesInMethods = false;
                bracesInProperties = false;
                bracesInAccessors = false;
                bracesInAnonymousMethods = false;
                bracesInAnonymousTypes = false;
                bracesInControlBlocks = false;
                bracesInLambdas = false;
                bracesInObjectCollectionArray = false;
                bracesInLocalFunctions = false;
            }
            else
            {
                // Comma-separated list: only listed items are true
                var items = new HashSet<string>(
                    braceValue.Split(',').Select(s => s.Trim()),
                    StringComparer.OrdinalIgnoreCase);

                bracesInTypes = items.Contains("types");
                bracesInMethods = items.Contains("methods");
                bracesInProperties = items.Contains("properties");
                bracesInAccessors = items.Contains("accessors");
                bracesInAnonymousMethods = items.Contains("anonymous_methods");
                bracesInAnonymousTypes = items.Contains("anonymous_types");
                bracesInControlBlocks = items.Contains("control_blocks");
                bracesInLambdas = items.Contains("lambdas");
                bracesInObjectCollectionArray = items.Contains("object_collection_array_initializers");
                bracesInLocalFunctions = items.Contains("local_functions");
            }
        }

        // Indentation
        var indentBlock = GetBoolSetting(settings, "csharp_indent_block_contents", "true", true);
        var indentBraces = GetBoolSetting(settings, "csharp_indent_braces", "true", false);
        var indentSwitchCaseSection = GetBoolSetting(settings, "csharp_indent_case_contents", "true", true);
        var indentSwitchCaseSectionWhenBlock = GetBoolSetting(settings, "csharp_indent_case_contents_when_block", "true", true);
        var indentSwitchSection = GetBoolSetting(settings, "csharp_indent_switch_labels", "true", true);
        var labelPositioning = GetLabelPositioning(settings);

        // New line before keywords
        var newLineBeforeElse = GetBoolSetting(settings, "csharp_new_line_before_else", "true", true);
        var newLineBeforeCatch = GetBoolSetting(settings, "csharp_new_line_before_catch", "true", true);
        var newLineBeforeFinally = GetBoolSetting(settings, "csharp_new_line_before_finally", "true", true);
        var newLineForClausesInQuery = GetBoolSetting(settings, "csharp_new_line_between_query_expression_clauses", "true", true);
        var newLineForMembersInAnonymousTypes = GetBoolSetting(settings, "csharp_new_line_before_members_in_anonymous_types", "true", true);
        var newLineForMembersInObjectInit = GetBoolSetting(settings, "csharp_new_line_before_members_in_object_initializers", "true", true);

        // Spacing
        var spaceAfterCast = GetBoolSetting(settings, "csharp_space_after_cast", "true", false);
        var spaceAfterColonInBase = GetBoolSetting(settings, "csharp_space_after_colon_in_inheritance_clause", "true", true);
        var spaceAfterComma = GetBoolSetting(settings, "csharp_space_after_comma", "true", true);
        var spaceAfterControlFlow = GetBoolSetting(settings, "csharp_space_after_keywords_in_control_flow_statements", "true", true);
        var spaceAfterDot = GetBoolSetting(settings, "csharp_space_after_dot", "true", false);
        var spaceAfterMethodCallName = GetBoolSetting(settings, "csharp_space_between_method_call_name_and_opening_parenthesis", "true", false);
        var spaceAfterSemicolonInFor = GetBoolSetting(settings, "csharp_space_after_semicolon_in_for_statement", "true", true);
        var spaceBeforeColonInBase = GetBoolSetting(settings, "csharp_space_before_colon_in_inheritance_clause", "true", true);
        var spaceBeforeComma = GetBoolSetting(settings, "csharp_space_before_comma", "true", false);
        var spaceBeforeDot = GetBoolSetting(settings, "csharp_space_before_dot", "true", false);
        var spaceBeforeOpenSquare = GetBoolSetting(settings, "csharp_space_before_open_square_brackets", "true", false);
        var spaceBeforeSemicolonInFor = GetBoolSetting(settings, "csharp_space_before_semicolon_in_for_statement", "true", false);
        var spaceBetweenEmptyMethodCallParens = GetBoolSetting(settings, "csharp_space_between_method_call_empty_parameter_list_parentheses", "true", false);
        var spaceBetweenEmptyMethodDeclParens = GetBoolSetting(settings, "csharp_space_between_method_declaration_empty_parameter_list_parentheses", "true", false);
        var spaceBetweenEmptySquare = GetBoolSetting(settings, "csharp_space_between_empty_square_brackets", "true", false);
        // csharp_space_around_declaration_statements uses "ignore"/"false" not "true"/"false"
        var spacesIgnoreAroundVarDecl = GetBoolSetting(settings, "csharp_space_around_declaration_statements", "ignore", false);
        var spaceWithinCast = GetBoolSetting(settings, "csharp_space_within_cast_parentheses", "true", false);
        var spaceWithinExprParens = GetBoolSetting(settings, "csharp_space_within_expression_parentheses", "true", false);
        var spaceWithinMethodCallParens = GetBoolSetting(settings, "csharp_space_between_method_call_parameter_list_parentheses", "true", false);
        var spaceWithinMethodDeclParens = GetBoolSetting(settings, "csharp_space_between_method_declaration_parameter_list_parentheses", "true", false);
        var spaceWithinOtherParens = GetBoolSetting(settings, "csharp_space_between_parentheses", "true", false);
        var spaceWithinSquare = GetBoolSetting(settings, "csharp_space_within_square_brackets", "true", false);
        var spacingAfterMethodDeclName = GetBoolSetting(settings, "csharp_space_between_method_declaration_name_and_open_parenthesis", "true", false);
        var spacingAroundBinaryOp = GetBinaryOperatorSpacing(settings);

        // Wrapping
        var wrappingPreserveSingleLine = GetBoolSetting(settings, "csharp_preserve_single_line_blocks", "true", true);
        var wrappingKeepStatementsOnSingleLine = GetBoolSetting(settings, "csharp_preserve_single_line_statements", "true", true);

        return new CSharpFormatStyle(
            Guid.NewGuid(), useTabs, indentSize, tabSize, newLine,
            // Indentation
            indentBlock, indentBraces, indentSwitchCaseSection,
            indentSwitchCaseSectionWhenBlock, indentSwitchSection, labelPositioning,
            // Brace placement
            bracesInTypes, bracesInMethods, bracesInProperties, bracesInAccessors,
            bracesInAnonymousMethods, bracesInAnonymousTypes, bracesInControlBlocks,
            bracesInLambdas, bracesInObjectCollectionArray, bracesInLocalFunctions,
            // New line before keywords
            newLineBeforeElse, newLineBeforeCatch, newLineBeforeFinally,
            newLineForClausesInQuery, newLineForMembersInAnonymousTypes, newLineForMembersInObjectInit,
            // Spacing
            spaceAfterCast, spaceAfterColonInBase, spaceAfterComma, spaceAfterControlFlow,
            spaceAfterDot, spaceAfterMethodCallName, spaceAfterSemicolonInFor,
            spaceBeforeColonInBase, spaceBeforeComma, spaceBeforeDot, spaceBeforeOpenSquare,
            spaceBeforeSemicolonInFor, spaceBetweenEmptyMethodCallParens, spaceBetweenEmptyMethodDeclParens,
            spaceBetweenEmptySquare, spacesIgnoreAroundVarDecl, spaceWithinCast, spaceWithinExprParens,
            spaceWithinMethodCallParens, spaceWithinMethodDeclParens, spaceWithinOtherParens,
            spaceWithinSquare, spacingAfterMethodDeclName, spacingAroundBinaryOp,
            // Wrapping
            wrappingPreserveSingleLine, wrappingKeepStatementsOnSingleLine
        );
    }

    private static bool GetBoolSetting(Dictionary<string, string> settings, string key, string trueValue, bool defaultValue)
    {
        if (!settings.TryGetValue(key, out var value))
            return defaultValue;
        return value.Trim().Equals(trueValue, StringComparison.OrdinalIgnoreCase);
    }

    private static int GetIntSetting(Dictionary<string, string> settings, string key, int defaultValue)
    {
        if (!settings.TryGetValue(key, out var value))
            return defaultValue;
        return int.TryParse(value.Trim(), out var result) ? result : defaultValue;
    }

    private static string GetNewLineSetting(Dictionary<string, string> settings)
    {
        if (!settings.TryGetValue("end_of_line", out var value))
            return "\n";
        return value.Trim().ToLowerInvariant() switch
        {
            "crlf" => "\r\n",
            "cr" => "\r",
            _ => "\n"
        };
    }

    /// <summary>
    /// Parses csharp_indent_labels: flush_left=0 (LeftMost), one_less_than_current=1 (OneLess), no_change=2 (NoIndent).
    /// </summary>
    private static int GetLabelPositioning(Dictionary<string, string> settings)
    {
        if (!settings.TryGetValue("csharp_indent_labels", out var value))
            return 1; // OneLess (default)
        return value.Trim().ToLowerInvariant() switch
        {
            "flush_left" => 0,         // LeftMost
            "one_less_than_current" => 1, // OneLess
            "no_change" => 2,          // NoIndent
            _ => 1
        };
    }

    /// <summary>
    /// Parses csharp_space_around_binary_operators: before_and_after=0 (Single), ignore=1, none=2 (Remove).
    /// </summary>
    private static int GetBinaryOperatorSpacing(Dictionary<string, string> settings)
    {
        if (!settings.TryGetValue("csharp_space_around_binary_operators", out var value))
            return 0; // Single (default)
        return value.Trim().ToLowerInvariant() switch
        {
            "before_and_after" => 0, // Single
            "ignore" => 1,          // Ignore
            "none" => 2,            // Remove
            _ => 0
        };
    }
}
