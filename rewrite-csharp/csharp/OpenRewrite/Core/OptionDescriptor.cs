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
using JetBrains.Annotations;

namespace OpenRewrite.Core;

/// <summary>
/// Describes a single configurable option on a recipe.
/// Built via reflection from properties annotated with <see cref="OptionAttribute"/>.
/// </summary>
public record OptionDescriptor(
    string Name,
    string Type,
    [property: LanguageInjection("markdown")] string DisplayName,
    [property: LanguageInjection("markdown")] string Description,
    string? Example,
    IReadOnlyList<string>? Valid,
    bool Required,
    object? Value
);
