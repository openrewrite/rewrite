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
using OpenRewrite.CSharp.Recipes;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.CSharp.Recipes;

public class RemoveAppContextSwitchTests(RpcFixture fixture) : RpcRewriteTest(fixture)
{
    private static SourceSpec AppConfig(string before, string? after = null, string sourcePath = "app.config") =>
        new(before, after, sourcePath, "org.openrewrite.xml.tree.Xml$Document");

    [Fact]
    public void RemoveSwitchFromMultiSwitchValue()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveAppContextSwitch
            {
                SwitchName = "Switch.System.Net.DontEnableSchUseStrongCrypto"
            }),
            AppConfig(
                """
                <configuration>
                  <runtime>
                    <AppContextSwitchOverrides value="Switch.System.Net.DontEnableSchUseStrongCrypto=true;Switch.Other=false" />
                  </runtime>
                </configuration>
                """,
                """
                <configuration>
                  <runtime>
                    <AppContextSwitchOverrides value="Switch.Other=false" />
                  </runtime>
                </configuration>
                """
            )
        );
    }

    [Fact]
    public void RemoveLastSwitchDropsElementAndEmptyAncestor()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveAppContextSwitch
            {
                SwitchName = "Switch.System.Net.DontEnableSchUseStrongCrypto"
            }),
            AppConfig(
                """
                <configuration>
                  <runtime>
                    <AppContextSwitchOverrides value="Switch.System.Net.DontEnableSchUseStrongCrypto=true" />
                  </runtime>
                </configuration>
                """,
                """
                <configuration/>
                """
            )
        );
    }

    [Fact]
    public void RemoveLastSwitchKeepsAncestorWithOtherChildren()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveAppContextSwitch
            {
                SwitchName = "Switch.System.Net.DontEnableSchUseStrongCrypto"
            }),
            AppConfig(
                """
                <configuration>
                  <runtime>
                    <AppContextSwitchOverrides value="Switch.System.Net.DontEnableSchUseStrongCrypto=true" />
                    <gcServer enabled="true" />
                  </runtime>
                </configuration>
                """,
                """
                <configuration>
                  <runtime>
                    <gcServer enabled="true" />
                  </runtime>
                </configuration>
                """
            )
        );
    }

    [Fact]
    public void NoChangeWhenSwitchAbsent()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveAppContextSwitch
            {
                SwitchName = "Switch.NotPresent"
            }),
            AppConfig(
                """
                <configuration>
                  <runtime>
                    <AppContextSwitchOverrides value="Switch.System.Net.DontEnableSchUseStrongCrypto=true;Switch.Other=false" />
                  </runtime>
                </configuration>
                """
            )
        );
    }

    [Fact]
    public void TolerantOfWhitespaceAroundSwitchEntries()
    {
        RewriteRun(
            spec => spec.SetRecipe(new RemoveAppContextSwitch
            {
                SwitchName = "Switch.System.Net.DontEnableSchUseStrongCrypto"
            }),
            AppConfig(
                """
                <configuration>
                  <runtime>
                    <AppContextSwitchOverrides value=" Switch.System.Net.DontEnableSchUseStrongCrypto = true ;  Switch.Other=false " />
                  </runtime>
                </configuration>
                """,
                """
                <configuration>
                  <runtime>
                    <AppContextSwitchOverrides value="Switch.Other=false" />
                  </runtime>
                </configuration>
                """
            )
        );
    }

    [Fact]
    public void DoesNotMatchSwitchByPrefix()
    {
        // Switch.Foo should not match Switch.FooBar.
        RewriteRun(
            spec => spec.SetRecipe(new RemoveAppContextSwitch
            {
                SwitchName = "Switch.Foo"
            }),
            AppConfig(
                """
                <configuration>
                  <runtime>
                    <AppContextSwitchOverrides value="Switch.FooBar=true" />
                  </runtime>
                </configuration>
                """
            )
        );
    }

    [Fact]
    public void HandlesValuelessSwitchEntries()
    {
        // Tolerate degenerate entries with no '=value' part.
        RewriteRun(
            spec => spec.SetRecipe(new RemoveAppContextSwitch
            {
                SwitchName = "Switch.Stray"
            }),
            AppConfig(
                """
                <configuration>
                  <runtime>
                    <AppContextSwitchOverrides value="Switch.Keep=true;Switch.Stray" />
                  </runtime>
                </configuration>
                """,
                """
                <configuration>
                  <runtime>
                    <AppContextSwitchOverrides value="Switch.Keep=true" />
                  </runtime>
                </configuration>
                """
            )
        );
    }
}
