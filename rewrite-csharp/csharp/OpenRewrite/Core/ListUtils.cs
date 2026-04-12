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
/// Utility class for list transformations that preserve reference identity when no changes occur.
/// Returns the original list reference when no elements are modified, avoiding unnecessary allocations.
/// A null return from the mapping function removes the element from the resulting list.
/// </summary>
public static class ListUtils
{
    /// <summary>
    /// Applies a mapping function to each element in the list.
    /// Returns the original list if no elements changed.
    /// If the mapper returns null, the element is removed.
    /// </summary>
    public static IList<T> Map<T>(IList<T> list, Func<T, T?> map) where T : class
    {
        if (list.Count == 0)
        {
            return list;
        }

        List<T>? newList = null;
        for (int i = 0; i < list.Count; i++)
        {
            T original = list[i];
            T? mapped = map(original);
            if (!ReferenceEquals(mapped, original))
            {
                if (newList == null)
                {
                    newList = new List<T>(list.Count);
                    for (int j = 0; j < i; j++)
                    {
                        newList.Add(list[j]);
                    }
                }
                if (mapped != null)
                {
                    newList.Add(mapped);
                }
            }
            else if (newList != null)
            {
                newList.Add(original);
            }
        }

        return newList ?? list;
    }

    /// <summary>
    /// Applies an indexed mapping function to each element in the list.
    /// Returns the original list if no elements changed.
    /// If the mapper returns null, the element is removed.
    /// </summary>
    public static IList<T> Map<T>(IList<T> list, Func<int, T, T?> map) where T : class
    {
        if (list.Count == 0)
        {
            return list;
        }

        List<T>? newList = null;
        for (int i = 0; i < list.Count; i++)
        {
            T original = list[i];
            T? mapped = map(i, original);
            if (!ReferenceEquals(mapped, original))
            {
                if (newList == null)
                {
                    newList = new List<T>(list.Count);
                    for (int j = 0; j < i; j++)
                    {
                        newList.Add(list[j]);
                    }
                }
                if (mapped != null)
                {
                    newList.Add(mapped);
                }
            }
            else if (newList != null)
            {
                newList.Add(original);
            }
        }

        return newList ?? list;
    }
}
