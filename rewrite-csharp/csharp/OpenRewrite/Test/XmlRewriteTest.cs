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
using OpenRewrite.Xml;

namespace OpenRewrite.Test;

/// <summary>
/// Base class for XML parser tests with round-trip validation.
/// </summary>
public abstract class XmlRewriteTest
{
    private readonly XmlParser _parser = new();
    private readonly XmlPrinter<object> _printer = new();

    protected void RewriteRun(params XmlSourceSpec[] specs)
    {
        foreach (var spec in specs)
        {
            var source = _parser.Parse(spec.Before, spec.SourcePath);

            // Verify round-trip: printed should match input
            var printed = Print(source);
            AssertContentEquals(spec.Before, printed, source.SourcePath,
                "The printed source didn't match the original source code. " +
                "This means there is a bug in the parser implementation itself.");

            // Verify idempotence: reparse and reprint should match
            var reparsed = _parser.Parse(printed, spec.SourcePath);
            var reprinted = Print(reparsed);
            AssertContentEquals(printed, reprinted, source.SourcePath,
                "The source is not print idempotent. Printing, re-parsing, and re-printing produced different output.");
        }
    }

    private string Print(Document document)
    {
        var p = new PrintOutputCapture<object>(new object());
        _printer.Visit(document, p);
        return p.ToString();
    }

    protected static XmlSourceSpec Xml(string before, string? sourcePath = null)
    {
        return new XmlSourceSpec(before, sourcePath ?? "file.xml");
    }

    private static void AssertContentEquals(string expected, string actual, string sourcePath, string errorMessagePrefix)
    {
        if (expected == actual) return;
        var diff = DiffUtils.UnifiedDiff(expected, actual, sourcePath);
        Assert.Fail($"{errorMessagePrefix} \"{sourcePath}\":\n{diff}");
    }
}

public record XmlSourceSpec(string Before, string SourcePath = "file.xml");
