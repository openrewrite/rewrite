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
