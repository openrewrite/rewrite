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
/// Hears about each request to auto-format a tree made on the thread it is installed on, so that the test harness
/// can check that a recipe formats only what it changed.
/// </summary>
internal abstract class FormattingListener : IDisposable
{
    [ThreadStatic] private static FormattingListener? _current;

    // Plain and never cleared, so formatters pay next to nothing until a listener is installed
    private static bool _everInstalled;

    private FormattingListener? _previous;

    /// <summary>
    /// Formatters call this before they auto-format <paramref name="tree"/>, stopping after
    /// <paramref name="stopAfter"/> when it is not null.
    /// </summary>
    public static void BeforeFormat(Tree tree, Tree? stopAfter, Cursor? cursor)
    {
        if (_everInstalled)
        {
            _current?.OnFormat(tree, stopAfter, cursor);
        }
    }

    protected abstract void OnFormat(Tree tree, Tree? stopAfter, Cursor? cursor);

    /// <summary>Listens to formatting on this thread until <see cref="Dispose"/>.</summary>
    public FormattingListener Install()
    {
        _previous = _current;
        _everInstalled = true;
        _current = this;
        return this;
    }

    public void Dispose() => _current = _previous;
}
