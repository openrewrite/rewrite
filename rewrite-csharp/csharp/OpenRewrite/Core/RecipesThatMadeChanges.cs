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
using Newtonsoft.Json;

namespace OpenRewrite.Core;

/// <summary>
/// Marker that records which recipes made changes to a source file.
/// This is an opaque marker — C# stores the raw recipe data without interpreting it.
/// </summary>
public sealed class RecipesThatMadeChanges : Marker
{
    [JsonProperty("id")]
    public Guid Id { get; init; }

    [JsonProperty("recipes")]
    public object? Recipes { get; init; }
}
