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
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;


public class CSharpSyntaxFragmentTests : RewriteTest
{
    /// <summary>
    /// Verifies that all supported syntax fragments round-trip through the parser and printer.
    /// </summary>
    [Theory]
    [MemberData(nameof(PassingFragments))]
    public void RoundTrip(SourceTestCase testCase)
    {
        RewriteRun(
            CSharp(testCase.SourceText)
        );
    }

    public static IEnumerable<object[]> PassingFragments()
    {
        return CSharpSyntaxFragments.GetData()
            .Select(tc => new object[] { tc });
    }

}
