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
using OpenRewrite.CSharp;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.Core;

/// <summary>
/// Singleton markers must each carry a DISTINCT, non-empty UUID. They previously all
/// used <c>Guid.Empty</c>, which collided across types in every UUID-keyed path of the
/// V3 marker table (intern table, findById, multi-project extern resolveById): resolving
/// any zero-UUID marker returned whichever type was stored first, silently swapping e.g.
/// NullSafe for NullCoalescing and dropping <c>?.</c>/<c>??</c> from printed C# so .NET
/// migration patches failed to apply. Guards against regression.
/// </summary>
public class SingletonMarkerIdTests
{
    private static readonly (string Name, Guid Id)[] Singletons =
    [
        (nameof(NullSafe), NullSafe.Instance.Id),
        (nameof(OmitParentheses), OmitParentheses.Instance.Id),
        (nameof(OmitBraces), OmitBraces.Instance.Id),
        (nameof(PointerMemberAccess), PointerMemberAccess.Instance.Id),
        (nameof(NullCoalescing), NullCoalescing.Instance.Id),
        (nameof(DelegateInvocation), DelegateInvocation.Instance.Id),
        (nameof(MultiDimensionalArray), MultiDimensionalArray.Instance.Id),
        (nameof(PatternCombinator), PatternCombinator.Instance.Id),
        (nameof(MultiDimensionContinuation), MultiDimensionContinuation.Instance.Id),
    ];

    [Fact]
    public void AllSingletonMarkerIdsAreNonEmpty()
    {
        foreach (var (name, id) in Singletons)
        {
            Assert.True(id != Guid.Empty, $"{name}.Instance.Id must not be Guid.Empty");
        }
    }

    [Fact]
    public void AllSingletonMarkerIdsAreDistinct()
    {
        var distinct = Singletons.Select(s => s.Id).Distinct().Count();
        Assert.Equal(Singletons.Length, distinct);
    }
}
