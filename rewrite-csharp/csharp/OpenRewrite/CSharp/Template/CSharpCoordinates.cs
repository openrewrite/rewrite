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
using OpenRewrite.Java;

namespace OpenRewrite.CSharp.Template;

/// <summary>
/// Specifies where and how to apply a template in the tree.
/// </summary>
public sealed class CSharpCoordinates
{
    public J Tree { get; }
    public CoordinateMode Mode { get; }

    private CSharpCoordinates(J tree, CoordinateMode mode)
    {
        Tree = tree;
        Mode = mode;
    }

    /// <summary>
    /// Replace the target tree element with the template result.
    /// </summary>
    public static CSharpCoordinates Replace(J tree) => new(tree, CoordinateMode.Replace);

    /// <summary>
    /// Insert the template result before the target tree element.
    /// </summary>
    public static CSharpCoordinates Before(J tree) => new(tree, CoordinateMode.Before);

    /// <summary>
    /// Insert the template result after the target tree element.
    /// </summary>
    public static CSharpCoordinates After(J tree) => new(tree, CoordinateMode.After);
}

public enum CoordinateMode
{
    Replace,
    Before,
    After
}
