namespace OpenRewrite.CSharp;

/// <summary>
/// Preprocesses C# source text containing conditional directives (#if/#elif/#else/#endif)
/// to produce clean source permutations for multi-parse.
///
/// Each permutation defines a different set of preprocessor symbols, causing different
/// branches to be active. Clean sources replace directive lines and excluded code with
/// empty lines to preserve line count for the line-level interleaving printer.
/// </summary>
public static class PreprocessorSourceTransformer
{
    private const int MaxPermutations = 64;

    /// <summary>
    /// Extracts the set of external preprocessor symbols referenced in #if/#elif conditions.
    /// Excludes symbols that are #define'd or #undef'd within the file itself.
    /// </summary>
    public static HashSet<string> ExtractSymbols(string source)
    {
        var symbols = new HashSet<string>();
        var defined = new HashSet<string>();
        var undefined = new HashSet<string>();

        foreach (var line in EnumerateLines(source))
        {
            var trimmed = line.TrimStart();
            if (!trimmed.StartsWith('#')) continue;

            var afterHash = trimmed[1..].TrimStart();

            if (afterHash.StartsWith("define "))
            {
                var sym = afterHash[7..].Trim();
                if (sym.Length > 0 && IsIdentifier(sym))
                    defined.Add(sym);
            }
            else if (afterHash.StartsWith("undef "))
            {
                var sym = afterHash[6..].Trim();
                if (sym.Length > 0 && IsIdentifier(sym))
                    undefined.Add(sym);
            }
            else if (afterHash.StartsWith("if ") || afterHash.StartsWith("if("))
            {
                var condition = afterHash.StartsWith("if(") ? afterHash[2..] : afterHash[3..];
                ExtractIdentifiers(condition, symbols);
            }
            else if (afterHash.StartsWith("elif ") || afterHash.StartsWith("elif("))
            {
                var condition = afterHash.StartsWith("elif(") ? afterHash[4..] : afterHash[5..];
                ExtractIdentifiers(condition, symbols);
            }
        }

        // Remove locally defined/undefined symbols — they're not external
        symbols.ExceptWith(defined);
        symbols.ExceptWith(undefined);
        // Remove boolean literals
        symbols.Remove("true");
        symbols.Remove("false");

        return symbols;
    }

    /// <summary>
    /// Transforms source text by evaluating conditional directives against the given defined symbols.
    /// Directive lines and excluded code lines are replaced with empty lines to preserve line count.
    /// When <paramref name="directiveLineToIndex"/> is provided, directive lines are replaced with
    /// ghost comments (<c>//DIRECTIVE:N</c>) instead of empty lines, enabling structural tracking.
    /// </summary>
    public static string Transform(string source, HashSet<string> definedSymbols,
        Dictionary<int, int>? directiveLineToIndex = null)
    {
        var lines = SplitLines(source);
        var result = new string[lines.Length];

        // Stack tracks per #if group:
        //   groupActive:  whether the enclosing context is active (for nesting)
        //   branchActive: whether the current branch within this group is active
        //   anyTaken:     whether any branch in this group has been taken
        var stack = new Stack<(bool groupActive, bool branchActive, bool anyTaken)>();
        var localDefines = new HashSet<string>(definedSymbols);

        for (int i = 0; i < lines.Length; i++)
        {
            var trimmed = lines[i].TrimStart();

            if (!trimmed.StartsWith('#'))
            {
                bool active = stack.Count == 0 || stack.Peek().branchActive;
                result[i] = active ? lines[i] : "";
                continue;
            }

            var afterHash = trimmed[1..].TrimStart();

            if (afterHash.StartsWith("if ") || afterHash.StartsWith("if("))
            {
                var condition = afterHash.StartsWith("if(") ? afterHash[2..] : afterHash[3..];
                bool groupActive = stack.Count == 0 || stack.Peek().branchActive;
                bool branchActive = groupActive && EvaluateCondition(condition, localDefines);
                stack.Push((groupActive, branchActive, branchActive));
                result[i] = GhostForDirective(i, directiveLineToIndex);
            }
            else if (afterHash.StartsWith("elif ") || afterHash.StartsWith("elif("))
            {
                var condition = afterHash.StartsWith("elif(") ? afterHash[4..] : afterHash[5..];
                var (groupActive, _, anyTaken) = stack.Pop();
                bool branchActive = groupActive && !anyTaken && EvaluateCondition(condition, localDefines);
                stack.Push((groupActive, branchActive, anyTaken || branchActive));
                result[i] = GhostForDirective(i, directiveLineToIndex);
            }
            else if (afterHash.StartsWith("else") && !IsLongerDirectiveKeyword(afterHash, "else"))
            {
                var (groupActive, _, anyTaken) = stack.Pop();
                bool branchActive = groupActive && !anyTaken;
                stack.Push((groupActive, branchActive, true));
                result[i] = GhostForDirective(i, directiveLineToIndex);
            }
            else if (afterHash.StartsWith("endif"))
            {
                stack.Pop();
                result[i] = GhostForDirective(i, directiveLineToIndex);
            }
            else if (afterHash.StartsWith("define "))
            {
                bool active = stack.Count == 0 || stack.Peek().branchActive;
                if (active)
                {
                    var sym = afterHash[7..].Trim();
                    if (sym.Length > 0) localDefines.Add(sym);
                }
                result[i] = active ? lines[i] : "";
            }
            else if (afterHash.StartsWith("undef "))
            {
                bool active = stack.Count == 0 || stack.Peek().branchActive;
                if (active)
                {
                    var sym = afterHash[6..].Trim();
                    if (sym.Length > 0) localDefines.Remove(sym);
                }
                result[i] = active ? lines[i] : "";
            }
            else
            {
                // Non-conditional directive (#region, #pragma, etc.)
                bool active = stack.Count == 0 || stack.Peek().branchActive;
                result[i] = active ? lines[i] : "";
            }
        }

        return string.Join("\n", result);
    }

    private static string GhostForDirective(int lineNumber, Dictionary<int, int>? directiveLineToIndex)
    {
        if (directiveLineToIndex != null && directiveLineToIndex.TryGetValue(lineNumber, out var index))
            return $"//DIRECTIVE:{index}";
        return "";
    }

    private static bool IsLongerDirectiveKeyword(string afterHash, string keyword)
    {
        return afterHash.Length > keyword.Length && char.IsLetterOrDigit(afterHash[keyword.Length]);
    }

    /// <summary>
    /// Records metadata about each conditional directive in the original source.
    /// </summary>
    public static List<DirectiveLine> GetDirectivePositions(string source)
    {
        var directives = new List<DirectiveLine>();
        var lines = SplitLines(source);
        var groupStack = new Stack<int>();
        int nextGroupId = 0;

        for (int i = 0; i < lines.Length; i++)
        {
            var trimmed = lines[i].TrimStart();
            if (!trimmed.StartsWith('#')) continue;

            var afterHash = trimmed[1..].TrimStart();

            if (afterHash.StartsWith("if ") || afterHash.StartsWith("if("))
            {
                int groupId = nextGroupId++;
                groupStack.Push(groupId);
                directives.Add(new DirectiveLine(i, lines[i], PreprocessorDirectiveKind.If, groupId, -1));
            }
            else if (afterHash.StartsWith("elif ") || afterHash.StartsWith("elif("))
            {
                int groupId = groupStack.Count > 0 ? groupStack.Peek() : -1;
                directives.Add(new DirectiveLine(i, lines[i], PreprocessorDirectiveKind.Elif, groupId, -1));
            }
            else if (afterHash.StartsWith("else") && !afterHash.StartsWith("elseif"))
            {
                // Make sure it's actually #else and not a longer keyword
                var rest = afterHash[4..];
                if (rest.Length == 0 || !char.IsLetterOrDigit(rest[0]))
                {
                    int groupId = groupStack.Count > 0 ? groupStack.Peek() : -1;
                    directives.Add(new DirectiveLine(i, lines[i], PreprocessorDirectiveKind.Else, groupId, -1));
                }
            }
            else if (afterHash.StartsWith("endif"))
            {
                int groupId = groupStack.Count > 0 ? groupStack.Pop() : -1;
                directives.Add(new DirectiveLine(i, lines[i], PreprocessorDirectiveKind.Endif, groupId, -1));
            }
        }

        return directives;
    }

    /// <summary>
    /// Generates unique clean source permutations with deduplication.
    /// Returns list of (cleanSource, definedSymbols).
    /// The primary branch (all symbols defined) is always first.
    /// When <paramref name="directiveLineToIndex"/> is provided, directive lines are replaced with
    /// ghost comments (<c>//DIRECTIVE:N</c>) for structural tracking.
    /// </summary>
    public static List<(string CleanSource, HashSet<string> DefinedSymbols)> GenerateUniquePermutations(
        string source, HashSet<string> symbols,
        Dictionary<int, int>? directiveLineToIndex = null)
    {
        var symbolList = symbols.OrderBy(s => s).ToList();
        int permCount = 1 << symbolList.Count;
        if (permCount > MaxPermutations)
            permCount = MaxPermutations;

        var seen = new Dictionary<string, int>(); // hash → index in result
        var result = new List<(string CleanSource, HashSet<string> DefinedSymbols)>();

        // Generate "all defined" first (primary branch)
        var allDefined = new HashSet<string>(symbolList);
        var primaryClean = Transform(source, allDefined, directiveLineToIndex);
        seen[primaryClean] = 0;
        result.Add((primaryClean, allDefined));

        // Generate remaining permutations
        for (int bits = 0; bits < permCount; bits++)
        {
            var defined = new HashSet<string>();
            for (int j = 0; j < symbolList.Count && j < 30; j++)
            {
                if ((bits & (1 << j)) != 0)
                    defined.Add(symbolList[j]);
            }

            // Skip if this is the "all defined" case (already added as primary)
            if (defined.SetEquals(allDefined))
                continue;

            var clean = Transform(source, defined, directiveLineToIndex);
            if (!seen.ContainsKey(clean))
            {
                seen[clean] = result.Count;
                result.Add((clean, defined));
            }
        }

        return result;
    }

    /// <summary>
    /// Computes ActiveBranchIndex for each DirectiveLine based on the given branches.
    /// For each directive:
    /// - If: first branch where the condition evaluates to true
    /// - Elif: first branch where all preceding conditions were false AND this condition is true
    /// - Else: first branch where all preceding conditions were false
    /// - Endif: -1
    /// </summary>
    public static void ComputeActiveBranchIndices(
        List<DirectiveLine> directives,
        List<(string CleanSource, HashSet<string> DefinedSymbols)> branches)
    {
        // Group directives by GroupId
        var groups = new Dictionary<int, List<int>>(); // groupId → indices into directives list
        for (int i = 0; i < directives.Count; i++)
        {
            var d = directives[i];
            if (!groups.ContainsKey(d.GroupId))
                groups[d.GroupId] = [];
            groups[d.GroupId].Add(i);
        }

        foreach (var (groupId, indices) in groups)
        {
            // For each branch, determine which directive in this group activates it
            for (int di = 0; di < indices.Count; di++)
            {
                var directive = directives[indices[di]];

                if (directive.Kind == PreprocessorDirectiveKind.Endif)
                {
                    directives[indices[di]] = directive with { ActiveBranchIndex = -1 };
                    continue;
                }

                // Find first branch where this directive's condition is the active one
                int activeBranch = -1;
                for (int bi = 0; bi < branches.Count; bi++)
                {
                    var symbols = branches[bi].DefinedSymbols;

                    // Check if this directive is the one that activates for this branch
                    bool thisIsActive = true;

                    // All preceding directives in the group must evaluate to false
                    for (int pi = 0; pi < di; pi++)
                    {
                        var prev = directives[indices[pi]];
                        if (prev.Kind == PreprocessorDirectiveKind.If ||
                            prev.Kind == PreprocessorDirectiveKind.Elif)
                        {
                            var condition = ExtractConditionFromDirectiveText(prev.Text);
                            if (EvaluateCondition(condition, symbols))
                            {
                                thisIsActive = false;
                                break;
                            }
                        }
                    }

                    if (!thisIsActive) continue;

                    // This directive must evaluate to true (for If/Elif) or be Else
                    if (directive.Kind == PreprocessorDirectiveKind.Else)
                    {
                        activeBranch = bi;
                        break;
                    }

                    var myCondition = ExtractConditionFromDirectiveText(directive.Text);
                    if (EvaluateCondition(myCondition, symbols))
                    {
                        activeBranch = bi;
                        break;
                    }
                }

                directives[indices[di]] = directive with { ActiveBranchIndex = activeBranch };
            }
        }
    }

    #region Condition Evaluation

    /// <summary>
    /// Evaluates a preprocessor condition against a set of defined symbols.
    /// Supports: identifiers, !, &&, ||, (), true, false
    /// </summary>
    internal static bool EvaluateCondition(string condition, HashSet<string> definedSymbols)
    {
        var tokens = Tokenize(condition);
        int pos = 0;
        return ParseOr(tokens, ref pos, definedSymbols);
    }

    private static bool ParseOr(List<string> tokens, ref int pos, HashSet<string> symbols)
    {
        bool result = ParseAnd(tokens, ref pos, symbols);
        while (pos < tokens.Count && tokens[pos] == "||")
        {
            pos++;
            result = ParseAnd(tokens, ref pos, symbols) || result;
        }
        return result;
    }

    private static bool ParseAnd(List<string> tokens, ref int pos, HashSet<string> symbols)
    {
        bool result = ParseUnary(tokens, ref pos, symbols);
        while (pos < tokens.Count && tokens[pos] == "&&")
        {
            pos++;
            result = ParseUnary(tokens, ref pos, symbols) && result;
        }
        return result;
    }

    private static bool ParseUnary(List<string> tokens, ref int pos, HashSet<string> symbols)
    {
        if (pos < tokens.Count && tokens[pos] == "!")
        {
            pos++;
            return !ParseUnary(tokens, ref pos, symbols);
        }
        return ParsePrimary(tokens, ref pos, symbols);
    }

    private static bool ParsePrimary(List<string> tokens, ref int pos, HashSet<string> symbols)
    {
        if (pos >= tokens.Count) return false;

        if (tokens[pos] == "(")
        {
            pos++;
            bool result = ParseOr(tokens, ref pos, symbols);
            if (pos < tokens.Count && tokens[pos] == ")")
                pos++;
            return result;
        }

        var token = tokens[pos++];
        return token switch
        {
            "true" => true,
            "false" => false,
            _ => symbols.Contains(token)
        };
    }

    private static List<string> Tokenize(string condition)
    {
        var tokens = new List<string>();
        int i = 0;
        while (i < condition.Length)
        {
            char c = condition[i];
            if (char.IsWhiteSpace(c))
            {
                i++;
            }
            else if (c == '(' || c == ')' || c == '!')
            {
                tokens.Add(c.ToString());
                i++;
            }
            else if (i + 1 < condition.Length && c == '&' && condition[i + 1] == '&')
            {
                tokens.Add("&&");
                i += 2;
            }
            else if (i + 1 < condition.Length && c == '|' && condition[i + 1] == '|')
            {
                tokens.Add("||");
                i += 2;
            }
            else if (c == '=' && i + 1 < condition.Length && condition[i + 1] == '=')
            {
                // == operator — skip for now, treat as separator
                i += 2;
            }
            else if (c == '!' && i + 1 < condition.Length && condition[i + 1] == '=')
            {
                // != operator — skip for now
                i += 2;
            }
            else if (char.IsLetterOrDigit(c) || c == '_')
            {
                int start = i;
                while (i < condition.Length && (char.IsLetterOrDigit(condition[i]) || condition[i] == '_'))
                    i++;
                tokens.Add(condition[start..i]);
            }
            else
            {
                i++; // skip unknown
            }
        }
        return tokens;
    }

    #endregion

    #region Helpers

    private static string ExtractConditionFromDirectiveText(string directiveText)
    {
        var trimmed = directiveText.TrimStart();
        if (!trimmed.StartsWith('#')) return "";
        var afterHash = trimmed[1..].TrimStart();

        if (afterHash.StartsWith("if("))
            return afterHash[2..];
        if (afterHash.StartsWith("if "))
            return afterHash[3..];
        if (afterHash.StartsWith("elif("))
            return afterHash[4..];
        if (afterHash.StartsWith("elif "))
            return afterHash[5..];
        return "";
    }

    private static void ExtractIdentifiers(string condition, HashSet<string> symbols)
    {
        int i = 0;
        while (i < condition.Length)
        {
            if (char.IsLetter(condition[i]) || condition[i] == '_')
            {
                int start = i;
                while (i < condition.Length && (char.IsLetterOrDigit(condition[i]) || condition[i] == '_'))
                    i++;
                var id = condition[start..i];
                if (id != "true" && id != "false")
                    symbols.Add(id);
            }
            else
            {
                i++;
            }
        }
    }

    private static bool IsIdentifier(string s)
    {
        if (s.Length == 0) return false;
        if (!char.IsLetter(s[0]) && s[0] != '_') return false;
        for (int i = 1; i < s.Length; i++)
        {
            if (!char.IsLetterOrDigit(s[i]) && s[i] != '_') return false;
        }
        return true;
    }

    private static IEnumerable<string> EnumerateLines(string source)
    {
        int start = 0;
        for (int i = 0; i < source.Length; i++)
        {
            if (source[i] == '\n')
            {
                yield return source[start..i].TrimEnd('\r');
                start = i + 1;
            }
        }
        if (start <= source.Length)
            yield return source[start..].TrimEnd('\r');
    }

    private static string[] SplitLines(string source)
    {
        // Split preserving the original line structure but normalizing line endings
        // We need to be careful: the source may use \r\n or \n
        return source.Split('\n');
    }

    #endregion
}
