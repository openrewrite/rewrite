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
using ExecutionContext = OpenRewrite.Core.ExecutionContext;

namespace OpenRewrite.CSharp.Recipes;

/// <summary>
/// Removes a named switch from &lt;AppContextSwitchOverrides&gt; in app.config and web.config.
/// If removing the switch leaves the value empty, the entire element (and any newly-empty
/// ancestor like &lt;runtime&gt;) is removed.
/// </summary>
public class RemoveAppContextSwitch : Recipe
{
    public override string DisplayName => "Remove an AppContext switch";

    public override string Description =>
        "Removes a single named switch from `<AppContextSwitchOverrides value=\"...\" />` in " +
        "app.config / web.config files. The value attribute is a semicolon-separated list of " +
        "`Switch.Name=value` pairs; only the entry matching the supplied switch name is removed. " +
        "If the value becomes empty, the element and any newly-empty ancestor element are removed.";

    [Option(DisplayName = "Switch name",
        Description = "The fully-qualified switch name to remove (e.g., Switch.System.Net.DontEnableSchUseStrongCrypto).",
        Example = "Switch.System.Net.DontEnableSchUseStrongCrypto")]
    public string SwitchName { get; set; } = "";

    public override ITreeVisitor<ExecutionContext> GetVisitor() =>
        new RemoveAppContextSwitchVisitor(SwitchName);
}
