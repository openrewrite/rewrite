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

namespace OpenRewrite.CSharp.Recipes;

/// <summary>
/// Common category leaf attributes exposed by the OpenRewrite SDK. Recipe packages
/// can re-use these as roots in their category paths (e.g. <c>[Category, CSharp, Migration]</c>
/// where <c>CSharp</c> comes from here) and add their own leaves in their own <c>Categories</c>
/// class. Consumers <c>using static OpenRewrite.CSharp.Recipes.Categories;</c> to use the bare names.
/// </summary>
public static class Categories
{
    public sealed class CSharpAttribute() : CategoryDescriptorAttribute("C#");

    public sealed class CsprojAttribute() : CategoryDescriptorAttribute(".NET", ".NET project file transformation recipes");

    public sealed class XmlAttribute() : CategoryDescriptorAttribute("XML", "XML transformation recipes");

    public sealed class MigrationAttribute() : CategoryDescriptorAttribute("Migration", "Recipes for migrating between versions");
}
