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
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.Test;

/// <summary>
/// The execution context of a test's recipe run, which fails the test when the recipe puts a message on it other than
/// the ones the SDK keeps there.
/// </summary>
internal sealed class UnchangedExecutionContext(RunChecks checks) : ExecutionContext
{
    private static readonly HashSet<string> SdkKeys =
    [
        DataTable<object>.DataTableStoreKey,
        DotNetBuildContext.ContextKey,
        MSBuildProjectHelper.StaleAttestationsKey
    ];

    private string? _failure;

    public override void PutMessage(string key, object value)
    {
        if (!SdkKeys.Contains(key))
        {
            var message = Message(key);
            _failure ??= message;
            Assert.Fail(message);
        }
        base.PutMessage(key, value);
    }

    /// <summary>Fails for a message the recipe put on the context while it swallowed the failure.</summary>
    public void AssertUnchanged()
    {
        if (_failure != null)
        {
            Assert.Fail(_failure);
        }
    }

    private string Message(string key) =>
        "Keep state that spans source files in a ScanningRecipe's accumulator, and state within one source file in " +
        "fields of the visitor that GetVisitor() creates for that file, rather than on the ExecutionContext.\n" +
        $"{checks.Recipe?.Name ?? "A recipe"} put \"{key}\" on the ExecutionContext, which every recipe and source " +
        "file in a run shares, so what it keeps there leaks into the others. A recipe that must change the " +
        "ExecutionContext opts out with spec.SetValidations(new Validations { ExecutionContextUnchanged = false }).";
}
