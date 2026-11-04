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

internal static class PathUtil
{
    /// <summary>
    /// Returns the canonical absolute form of <paramref name="path"/> so paths produced by
    /// different sources compare equal. On macOS, <c>/var</c> and <c>/tmp</c> are firmlinks to
    /// <c>/private/var</c> and <c>/private/tmp</c>; native libraries (libgit2, MSBuild) resolve
    /// them while <see cref="Path.GetFullPath(string)"/> does not, so we apply the same prefix
    /// fix-up. No-op on other platforms.
    /// </summary>
    public static string Canonicalize(string path)
    {
        var full = Path.GetFullPath(path);
        if (OperatingSystem.IsMacOS())
        {
            if (full.StartsWith("/var/", StringComparison.Ordinal))
                full = "/private" + full;
            else if (full.StartsWith("/tmp/", StringComparison.Ordinal))
                full = "/private" + full;
        }
        return full;
    }
}
