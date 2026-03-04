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
                condition = StripLineComment(condition);
                ExtractIdentifiers(condition, symbols);
            }
            else if (afterHash.StartsWith("elif ") || afterHash.StartsWith("elif("))
            {
                var condition = afterHash.StartsWith("elif(") ? afterHash[4..] : afterHash[5..];
                condition = StripLineComment(condition);
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
    /// When <paramref name="invertedLines"/> is provided, conditions on those lines are logically inverted,
    /// allowing tautological conditions like <c>#if true</c> to be toggled for branch coverage.
    /// </summary>
    public static string Transform(string source, HashSet<string> definedSymbols,
        Dictionary<int, int>? directiveLineToIndex = null,
        HashSet<int>? invertedLines = null)
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
                condition = StripLineComment(condition);
                bool groupActive = stack.Count == 0 || stack.Peek().branchActive;
                bool condResult = EvaluateCondition(condition, localDefines);
                if (invertedLines != null && invertedLines.Contains(i))
                    condResult = !condResult;
                bool branchActive = groupActive && condResult;
                stack.Push((groupActive, branchActive, branchActive));
                result[i] = GhostForDirective(i, directiveLineToIndex);
            }
            else if (afterHash.StartsWith("elif ") || afterHash.StartsWith("elif("))
            {
                var condition = afterHash.StartsWith("elif(") ? afterHash[4..] : afterHash[5..];
                condition = StripLineComment(condition);
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
    /// Uses targeted branch coverage: for each directive group, computes symbol sets
    /// that activate each specific branch, ensuring no branch content is lost even
    /// when the total number of symbols exceeds what brute-force enumeration can cover.
    /// </summary>
    public static List<(string CleanSource, HashSet<string> DefinedSymbols)> GenerateUniquePermutations(
        string source, HashSet<string> symbols,
        Dictionary<int, int>? directiveLineToIndex = null)
    {
        var symbolList = symbols.OrderBy(s => s).ToList();
        var seen = new Dictionary<string, int>();
        var result = new List<(string CleanSource, HashSet<string> DefinedSymbols)>();

        // Generate "all defined" first (primary branch)
        var allDefined = new HashSet<string>(symbolList);
        var primaryClean = Transform(source, allDefined, directiveLineToIndex);
        seen[primaryClean] = 0;
        result.Add((primaryClean, allDefined));

        // Add "none defined" (covers #else branches)
        var noneDefined = new HashSet<string>();
        var noneClean = Transform(source, noneDefined, directiveLineToIndex);
        if (!seen.ContainsKey(noneClean))
        {
            seen[noneClean] = result.Count;
            result.Add((noneClean, noneDefined));
        }

        // Targeted branch coverage: for each directive group, compute symbol sets
        // that activate each specific branch
        var directives = GetDirectivePositions(source);
        AddBranchCoveragePermutations(source, directives, symbols, directiveLineToIndex, seen, result);

        // If still small enough, also do brute-force for completeness
        int permCount = 1 << symbolList.Count;
        if (permCount <= MaxPermutations)
        {
            for (int bits = 0; bits < permCount; bits++)
            {
                var defined = new HashSet<string>();
                for (int j = 0; j < symbolList.Count && j < 30; j++)
                {
                    if ((bits & (1 << j)) != 0)
                        defined.Add(symbolList[j]);
                }

                if (defined.SetEquals(allDefined))
                    continue;

                var clean = Transform(source, defined, directiveLineToIndex);
                if (!seen.ContainsKey(clean))
                {
                    seen[clean] = result.Count;
                    result.Add((clean, defined));
                }
            }
        }

        return result;
    }

    /// <summary>
    /// For each directive group (#if/#elif/#else/#endif), computes targeted symbol sets
    /// that activate each specific branch. This ensures every branch is covered regardless
    /// of how many total symbols exist.
    /// Also handles tautological conditions (#if true, #if false) by generating inverted
    /// permutations that toggle the literal condition.
    /// </summary>
    private static void AddBranchCoveragePermutations(
        string source, List<DirectiveLine> directives, HashSet<string> allSymbols,
        Dictionary<int, int>? directiveLineToIndex,
        Dictionary<string, int> seen,
        List<(string CleanSource, HashSet<string> DefinedSymbols)> result)
    {
        // Group by GroupId
        var groups = new Dictionary<int, List<DirectiveLine>>();
        foreach (var d in directives)
        {
            if (!groups.ContainsKey(d.GroupId))
                groups[d.GroupId] = [];
            groups[d.GroupId].Add(d);
        }

        // Build parent map: for nested groups, record enclosing group's ID
        var parentOf = new Dictionary<int, int>();
        var groupStack = new Stack<int>();
        foreach (var d in directives.OrderBy(d => d.LineNumber))
        {
            if (d.Kind == PreprocessorDirectiveKind.If)
            {
                if (groupStack.Count > 0)
                    parentOf[d.GroupId] = groupStack.Peek();
                groupStack.Push(d.GroupId);
            }
            else if (d.Kind == PreprocessorDirectiveKind.Endif)
            {
                if (groupStack.Count > 0)
                    groupStack.Pop();
            }
        }

        // Compute enclosing requirements for a group — symbols needed to make
        // all ancestor #if conditions true so nested content is reachable
        HashSet<string> GetEnclosingRequirements(int groupId)
        {
            var requirements = new HashSet<string>();
            var current = groupId;
            while (parentOf.TryGetValue(current, out var parent))
            {
                if (groups.TryGetValue(parent, out var parentDirs))
                {
                    var ifDir = parentDirs.FirstOrDefault(d => d.Kind == PreprocessorDirectiveKind.If);
                    if (ifDir != null)
                    {
                        var cond = StripLineComment(ExtractConditionFromDirectiveText(ifDir.Text));
                        var pos = new HashSet<string>();
                        var neg = new HashSet<string>();
                        ExtractConditionRequirements(cond, pos, neg);
                        requirements.UnionWith(pos);
                        requirements.ExceptWith(neg);
                    }
                }
                current = parent;
            }
            return requirements;
        }

        // Collect tautological #if lines that need inverted permutations
        var tautologicalIfLines = new HashSet<int>();

        foreach (var (groupId, groupDirs) in groups)
        {
            var precedingPositiveSymbols = new HashSet<string>();
            bool hasTautologicalIf = false;
            var enclosingReqs = GetEnclosingRequirements(groupId);

            foreach (var dir in groupDirs)
            {
                if (dir.Kind == PreprocessorDirectiveKind.Endif)
                    continue;

                if (dir.Kind == PreprocessorDirectiveKind.If)
                {
                    var condition = StripLineComment(ExtractConditionFromDirectiveText(dir.Text));
                    if (IsTautologicalCondition(condition) && groupDirs.Count > 2)
                    {
                        // This #if has a literal condition and there are other branches
                        tautologicalIfLines.Add(dir.LineNumber);
                        hasTautologicalIf = true;
                    }
                }

                HashSet<string> targetSymbols;
                if (dir.Kind == PreprocessorDirectiveKind.Else)
                {
                    // #else needs enclosing requirements to be reachable
                    targetSymbols = new HashSet<string>(enclosingReqs);
                }
                else
                {
                    var condition = StripLineComment(ExtractConditionFromDirectiveText(dir.Text));
                    var positive = new HashSet<string>();
                    var negative = new HashSet<string>();
                    ExtractConditionRequirements(condition, positive, negative);

                    targetSymbols = new HashSet<string>(positive);
                    targetSymbols.ExceptWith(negative);
                    targetSymbols.ExceptWith(precedingPositiveSymbols.Except(positive));

                    if (!EvaluateCondition(condition, targetSymbols))
                    {
                        targetSymbols = new HashSet<string>(positive);
                        targetSymbols.ExceptWith(negative);
                    }

                    // Add enclosing requirements so nested content is reachable
                    targetSymbols.UnionWith(enclosingReqs);

                    precedingPositiveSymbols.UnionWith(positive);
                }

                if (!hasTautologicalIf)
                {
                    var clean = Transform(source, targetSymbols, directiveLineToIndex);
                    if (!seen.ContainsKey(clean))
                    {
                        seen[clean] = result.Count;
                        result.Add((clean, targetSymbols));
                    }
                }
            }
        }

        // Generate inverted permutations for tautological conditions
        if (tautologicalIfLines.Count > 0)
        {
            // Generate a permutation with all tautological #if lines inverted
            var clean = Transform(source, allSymbols, directiveLineToIndex, tautologicalIfLines);
            if (!seen.ContainsKey(clean))
            {
                seen[clean] = result.Count;
                // Use a copy of allSymbols tagged to indicate inversion
                result.Add((clean, new HashSet<string>(allSymbols)));
            }

            // Also generate with empty symbols + inversion
            clean = Transform(source, new HashSet<string>(), directiveLineToIndex, tautologicalIfLines);
            if (!seen.ContainsKey(clean))
            {
                seen[clean] = result.Count;
                result.Add((clean, new HashSet<string>()));
            }
        }
    }

    /// <summary>
    /// Extracts the symbols that appear in positive and negative positions in a condition.
    /// Positive: symbols that need to be defined for the condition to be true.
    /// Negative: symbols behind ! that must not be defined.
    /// </summary>
    private static void ExtractConditionRequirements(
        string condition, HashSet<string> positive, HashSet<string> negative)
    {
        var tokens = Tokenize(condition);
        for (int i = 0; i < tokens.Count; i++)
        {
            if (tokens[i] == "!" && i + 1 < tokens.Count && IsIdentifier(tokens[i + 1])
                && tokens[i + 1] != "true" && tokens[i + 1] != "false")
            {
                negative.Add(tokens[i + 1]);
                i++;
            }
            else if (IsIdentifier(tokens[i]) && tokens[i] != "true" && tokens[i] != "false")
            {
                positive.Add(tokens[i]);
            }
        }
    }

    /// <summary>
    /// Computes ActiveBranchIndex for each DirectiveLine by analyzing which branch's
    /// clean source has content in the section after each directive.
    /// This approach works correctly even for tautological conditions (#if true/#if false)
    /// and inverted permutations, because it checks the actual content rather than
    /// re-evaluating conditions.
    /// </summary>
    public static void ComputeActiveBranchIndices(
        List<DirectiveLine> directives,
        List<(string CleanSource, HashSet<string> DefinedSymbols)> branches)
    {
        if (directives.Count == 0 || branches.Count == 0) return;

        // Pre-split each branch's clean source into lines
        var branchLines = new string[branches.Count][];
        for (int b = 0; b < branches.Count; b++)
            branchLines[b] = SplitLines(branches[b].CleanSource);

        // For each non-Endif directive, find the first branch where the section
        // after that directive has non-empty content
        for (int d = 0; d < directives.Count; d++)
        {
            var directive = directives[d];
            if (directive.Kind == PreprocessorDirectiveKind.Endif)
            {
                directives[d] = directive with { ActiveBranchIndex = -1 };
                continue;
            }

            // Find the line range for this directive's section:
            // from directive.LineNumber+1 to the next directive in the same group
            int sectionStart = directive.LineNumber + 1;
            int sectionEnd = FindNextDirectiveLineInGroup(directives, d);

            int activeBranch = -1;
            for (int b = 0; b < branches.Count; b++)
            {
                if (SectionHasContent(branchLines[b], sectionStart, sectionEnd))
                {
                    activeBranch = b;
                    break;
                }
            }

            directives[d] = directive with { ActiveBranchIndex = activeBranch };
        }
    }

    /// <summary>
    /// Finds the line number of the next directive in the same group, or total line count.
    /// </summary>
    private static int FindNextDirectiveLineInGroup(List<DirectiveLine> directives, int currentIndex)
    {
        var current = directives[currentIndex];
        for (int i = currentIndex + 1; i < directives.Count; i++)
        {
            if (directives[i].GroupId == current.GroupId)
                return directives[i].LineNumber;
        }
        // If no next directive found in group, return a large number
        return int.MaxValue;
    }

    /// <summary>
    /// Checks if any line in the given range has non-empty content (not blank, not ghost comment).
    /// </summary>
    private static bool SectionHasContent(string[] lines, int startLine, int endLine)
    {
        for (int i = startLine; i < endLine && i < lines.Length; i++)
        {
            var trimmed = lines[i].Trim();
            if (trimmed.Length > 0 && !trimmed.StartsWith("//DIRECTIVE:"))
                return true;
        }
        return false;
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

        string raw;
        if (afterHash.StartsWith("if("))
            raw = afterHash[2..];
        else if (afterHash.StartsWith("if "))
            raw = afterHash[3..];
        else if (afterHash.StartsWith("elif("))
            raw = afterHash[4..];
        else if (afterHash.StartsWith("elif "))
            raw = afterHash[5..];
        else
            return "";

        return StripLineComment(raw);
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

    /// <summary>
    /// Strips a trailing // comment from a preprocessor condition.
    /// E.g., "true //DESKTOPGL" → "true"
    /// </summary>
    private static string StripLineComment(string condition)
    {
        // Find // that's not inside an identifier
        for (int i = 0; i < condition.Length - 1; i++)
        {
            if (condition[i] == '/' && condition[i + 1] == '/')
                return condition[..i].TrimEnd();
        }
        return condition.TrimEnd('\r', ' ');
    }

    /// <summary>
    /// Checks if a stripped condition is a boolean literal that always evaluates the same
    /// regardless of symbol definitions.
    /// </summary>
    private static bool IsTautologicalCondition(string strippedCondition)
    {
        var trimmed = strippedCondition.Trim();
        return trimmed == "true" || trimmed == "false";
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
