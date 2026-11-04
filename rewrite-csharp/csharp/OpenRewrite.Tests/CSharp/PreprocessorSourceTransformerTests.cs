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
using OpenRewrite.CSharp;

namespace OpenRewrite.Tests.CSharp;

public class PreprocessorSourceTransformerTests
{
    [Fact]
    public void ExtractSymbols_IncludesLocallyDefinedSymbolsUsedInConditions()
    {
        var source = """
            #define GUICS
            using System;
            #if GUICS
            int x;
            #else
            int y;
            #endif
            """;
        var symbols = PreprocessorSourceTransformer.ExtractSymbols(source);
        Assert.Contains("GUICS", symbols);
    }

    [Fact]
    public void ExtractSymbols_IncludesLocallyUndefinedSymbolsUsedInConditions()
    {
        var source = """
            #undef FEATURE
            #if FEATURE
            int x;
            #endif
            """;
        var symbols = PreprocessorSourceTransformer.ExtractSymbols(source);
        Assert.Contains("FEATURE", symbols);
    }

    [Fact]
    public void GenerateUniquePermutations_ProducesTwoBranchesForLocalDefine()
    {
        var source = """
            #define GUICS
            #if GUICS
            int x;
            #else
            int y;
            #endif
            """;
        var symbols = PreprocessorSourceTransformer.ExtractSymbols(source);
        var permutations = PreprocessorSourceTransformer.GenerateUniquePermutations(source, symbols);

        Assert.True(permutations.Count >= 2, $"Expected at least 2 permutations, got {permutations.Count}");

        // First permutation (all defined): #if GUICS is true, so "int x;" is kept
        Assert.Contains("int x;", permutations[0].CleanSource);
        Assert.DoesNotContain("int y;", permutations[0].CleanSource);

        // Second permutation (none defined): #if GUICS is false, so "int y;" is kept
        Assert.Contains("int y;", permutations[1].CleanSource);
        Assert.DoesNotContain("int x;", permutations[1].CleanSource);
    }

    [Fact]
    public void Transform_IgnoreFileDefines_DoesNotProcessLocalDefine()
    {
        var source = """
            #define GUICS
            #if GUICS
            int x;
            #else
            int y;
            #endif
            """;

        // Without ignoreFileDefines: #define adds GUICS to localDefines,
        // so even with empty definedSymbols, #if GUICS is true
        var withoutIgnore = PreprocessorSourceTransformer.Transform(source, [], ignoreFileDefines: false);
        Assert.Contains("int x;", withoutIgnore);
        Assert.DoesNotContain("int y;", withoutIgnore);

        // With ignoreFileDefines: #define is not processed,
        // so with empty definedSymbols, #if GUICS is false
        var withIgnore = PreprocessorSourceTransformer.Transform(source, [], ignoreFileDefines: true);
        Assert.DoesNotContain("int x;", withIgnore);
        Assert.Contains("int y;", withIgnore);
    }

    [Fact]
    public void Transform_IgnoreFileDefines_ExternalSymbolStillWorks()
    {
        var source = """
            #define GUICS
            #if GUICS
            int x;
            #else
            int y;
            #endif
            """;

        // With ignoreFileDefines but GUICS in definedSymbols:
        // the externally-supplied symbol controls the branch, not the #define
        var result = PreprocessorSourceTransformer.Transform(
            source, new HashSet<string> { "GUICS" }, ignoreFileDefines: true);
        Assert.Contains("int x;", result);
        Assert.DoesNotContain("int y;", result);
    }
}
