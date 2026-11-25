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
using OpenRewrite.CSharp.Format;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Core;

public class FormattingReconciliationTests
{
    private static readonly TimeSpan Timeout = TimeSpan.FromSeconds(30);
    private static readonly CSharpParser Parser = new();
    private static readonly CompilationUnit Original = Parser.Parse("class C { }");
    private static readonly CompilationUnit Formatted = Parser.Parse("class C { void M() { } }");

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task OverlappingRunsKeepTheirOwnValidationSetting(bool firstEnabled)
    {
        using var firstEntered = new ManualResetEventSlim();
        using var secondEntered = new ManualResetEventSlim();
        using var firstFinished = new ManualResetEventSlim();
        Exception? firstMismatch = null;
        Exception? secondMismatch = null;

        var first = Task.Factory.StartNew(() =>
        {
            try
            {
                new Harness().Run(firstEnabled, () =>
                {
                    firstEntered.Set();
                    Assert.True(secondEntered.Wait(Timeout));
                    firstMismatch = Record.Exception(() => Reconcile());
                });
            }
            finally
            {
                firstFinished.Set();
            }
        }, TaskCreationOptions.LongRunning);
        var second = Task.Factory.StartNew(() =>
        {
            Assert.True(firstEntered.Wait(Timeout));
            new Harness().Run(!firstEnabled, () =>
            {
                secondEntered.Set();
                Assert.True(firstFinished.Wait(Timeout));
                secondMismatch = Record.Exception(() => Reconcile());
            });
        }, TaskCreationOptions.LongRunning);
        await Task.WhenAll(first, second);

        AssertMismatch(firstEnabled, firstMismatch);
        AssertMismatch(!firstEnabled, secondMismatch);
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public void NestedRunsRestoreTheOuterValidationSetting(bool outerEnabled)
    {
        var harness = new Harness();
        harness.Run(outerEnabled, () =>
        {
            AssertMismatch(outerEnabled, Record.Exception(() => Reconcile()));
            harness.Run(!outerEnabled, () =>
                AssertMismatch(!outerEnabled, Record.Exception(() => Reconcile())));
            AssertMismatch(outerEnabled, Record.Exception(() => Reconcile()));
        });
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public void AFailingNestedRunRestoresTheOuterValidationSetting(bool outerEnabled)
    {
        var harness = new Harness();
        harness.Run(outerEnabled, () =>
        {
            Assert.Throws<InvalidOperationException>(() => harness.Run(!outerEnabled,
                () => throw new InvalidOperationException("Recipe assertion failed")));
            AssertMismatch(outerEnabled, Record.Exception(() => Reconcile()));
        });
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public void ExplicitConstructorSettingOverridesTheRun(bool enabled)
    {
        new Harness().Run(enabled, () =>
            AssertMismatch(!enabled, Record.Exception(() => Reconcile(!enabled))));
    }

    [Fact]
    public void RunsLeaveTheProcessDefaultUnchanged()
    {
        var harness = new Harness();
        var processDefault = WhitespaceReconciler.ThrowOnMismatchDefault;
        harness.Run(!processDefault, () =>
        {
            Assert.Equal(processDefault, WhitespaceReconciler.ThrowOnMismatchDefault);
            AssertMismatch(!processDefault, Record.Exception(() => Reconcile()));
        });
        Assert.Equal(processDefault, WhitespaceReconciler.ThrowOnMismatchDefault);
        AssertMismatch(processDefault, Record.Exception(() => Reconcile()));
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public void ValidationSettingFlowsToTasksStartedDuringTheRun(bool enabled)
    {
        new Harness().Run(enabled, () =>
        {
            var mismatch = Task.Run(() => Record.Exception(() => Reconcile())).GetAwaiter().GetResult();
            AssertMismatch(enabled, mismatch);
        });
    }

    private static void AssertMismatch(bool enabled, Exception? mismatch)
    {
        if (enabled)
        {
            Assert.IsType<WhitespaceReconciler.WhitespaceReconcileMismatchException>(mismatch);
        }
        else
        {
            Assert.Null(mismatch);
        }
    }

    private static void Reconcile(bool? throwOnMismatch = null)
    {
        var reconciler = new WhitespaceReconciler(throwOnMismatch);
        reconciler.Reconcile(Original, Formatted);
        Assert.False(reconciler.IsCompatible);
    }

    private class Harness : RewriteTest
    {
        public void Run(bool enabled, Action assertion) => RewriteRun(
            spec => spec.SetRecipe(new Noop())
                .SetExpectedCyclesThatMakeChanges(0)
                .SetValidations(new Validations { FormattingReconciliation = enabled }),
            CSharp("class C { }").AfterRecipe(_ => assertion()));
    }

    private class Noop : global::OpenRewrite.Core.Recipe
    {
        public override string DisplayName => "Keep the source unchanged";
        public override string Description => "Keeps the source unchanged.";
    }
}
