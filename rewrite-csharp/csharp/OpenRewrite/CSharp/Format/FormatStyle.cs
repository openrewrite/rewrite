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
/// Auto-detected indentation style from source text.
/// </summary>
public sealed class FormatStyle
{
    public bool UseTabs { get; }
    public int IndentationSize { get; }
    public string NewLine { get; }

    public FormatStyle(bool useTabs, int indentationSize, string newLine)
    {
        UseTabs = useTabs;
        IndentationSize = indentationSize;
        NewLine = newLine;
    }

    /// <summary>
    /// Detects indentation style from source text by analyzing leading whitespace.
    /// </summary>
    public static FormatStyle DetectStyle(string source)
    {
        var tabCount = 0;
        var spaceCount = 0;
        var indentSizes = new Dictionary<int, int>(); // size → count
        var newLine = "\n";

        // Detect line ending
        if (source.Contains("\r\n"))
            newLine = "\r\n";

        var lines = source.Split('\n');
        foreach (var rawLine in lines)
        {
            var line = rawLine.TrimEnd('\r');
            if (line.Length == 0) continue;

            var leadingSpaces = 0;
            var leadingTabs = 0;
            foreach (var c in line)
            {
                if (c == ' ') leadingSpaces++;
                else if (c == '\t') leadingTabs++;
                else break;
            }

            if (leadingTabs > 0)
            {
                tabCount++;
            }
            else if (leadingSpaces > 0)
            {
                spaceCount++;
                if (leadingSpaces <= 16)
                {
                    indentSizes.TryGetValue(leadingSpaces, out var count);
                    indentSizes[leadingSpaces] = count + 1;
                }
            }
        }

        var useTabs = tabCount > spaceCount;
        var indentSize = DetectIndentSize(indentSizes);

        return new FormatStyle(useTabs, indentSize, newLine);
    }

    private static int DetectIndentSize(Dictionary<int, int> indentSizes)
    {
        if (indentSizes.Count == 0) return 4;

        // Compute GCD of all observed indent sizes
        var gcd = 0;
        foreach (var size in indentSizes.Keys)
        {
            gcd = gcd == 0 ? size : Gcd(gcd, size);
        }

        // Sanity check: GCD should be a reasonable indent size
        if (gcd >= 2 && gcd <= 8)
            return gcd;

        // Fallback to 4
        return 4;
    }

    private static int Gcd(int a, int b)
    {
        while (b != 0)
        {
            var t = b;
            b = a % b;
            a = t;
        }
        return a;
    }
}
