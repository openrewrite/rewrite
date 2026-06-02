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
/// Context for recipe execution, providing message passing between recipes and visitors.
/// </summary>
public class ExecutionContext
{
    private readonly Dictionary<string, object> _messages = new();

    public T? GetMessage<T>(string key) where T : class
        => _messages.TryGetValue(key, out var val) ? val as T : null;

    public void PutMessage(string key, object value)
        => _messages[key] = value;

    public T ComputeMessageIfAbsent<T>(string key, Func<string, T> fn) where T : notnull
    {
        if (!_messages.TryGetValue(key, out var val))
        {
            val = fn(key);
            _messages[key] = val;
        }

        return (T)val;
    }

    public Action<Exception>? OnError { get; set; }
}
