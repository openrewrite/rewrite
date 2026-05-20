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
/// Path-opening marker for declaring a recipe's placement in the marketplace category tree.
/// Each <see cref="CategoryAttribute"/> instance starts a new category path; subsequent
/// <see cref="CategoryDescriptorAttribute"/> instances (in declaration order) form the levels
/// of that path from root to leaf.
/// <example>
/// <code>
/// [Category, Csproj]                    // single placement: ".NET"
/// [Category, CSharp, Migration]         // nested: "C#" → "Migration"
/// [Category, CSharp, Migration]         // multi-placement
/// [Category, CSharp, Net5]              // also at "C#" → ".NET 5"
/// public class FindUtf7Encoding : Recipe { }
/// </code>
/// </example>
/// </summary>
[AttributeUsage(AttributeTargets.Class, AllowMultiple = true, Inherited = false)]
public sealed class CategoryAttribute : Attribute;
