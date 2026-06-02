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
namespace OpenRewrite.Core;

/// <summary>
/// Base class for a category descriptor attribute. Concrete subclasses bind a
/// fixed <see cref="CategoryDescriptor"/> at the type level and are applied to
/// <see cref="Recipe"/> classes — preceded by a <see cref="CategoryAttribute"/>
/// marker — to declare the recipe's marketplace placement.
/// <para>
/// Each path is opened by a <see cref="CategoryAttribute"/>; subsequent
/// <c>CategoryDescriptorAttribute</c> instances (in declaration order) form
/// the levels of that path from root to leaf. The next <c>CategoryAttribute</c>
/// starts a new path, enabling multi-placement.
/// </para>
/// <para>
/// Define concrete categories as nested classes in a static <c>Categories</c>
/// container per assembly (so consumers can <c>using static</c> to write bare
/// names at the call site), e.g.:
/// </para>
/// <example>
/// <code>
/// public static class Categories
/// {
///     public sealed class CSharpAttribute() : CategoryDescriptorAttribute("C#");
///     public sealed class MigrationAttribute() : CategoryDescriptorAttribute("Migration", "Recipes for migrating .NET versions");
/// }
/// </code>
/// </example>
/// </summary>
[AttributeUsage(AttributeTargets.Class, AllowMultiple = true, Inherited = false)]
public abstract class CategoryDescriptorAttribute(string displayName, string? description = null) : Attribute
{
    public CategoryDescriptor Descriptor { get; } = new(displayName, description);
}
