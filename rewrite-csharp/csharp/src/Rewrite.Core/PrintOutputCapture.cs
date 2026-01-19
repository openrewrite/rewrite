using System.Text;

namespace Rewrite.Core;

/// <summary>
/// Captures output during tree printing operations.
/// </summary>
public class PrintOutputCapture<T>
{
    private readonly StringBuilder _output = new();

    public T Context { get; }

    public PrintOutputCapture(T context)
    {
        Context = context;
    }

    public PrintOutputCapture<T> Append(string? text)
    {
        if (text != null)
        {
            _output.Append(text);
        }
        return this;
    }

    public PrintOutputCapture<T> Append(char c)
    {
        _output.Append(c);
        return this;
    }

    public override string ToString() => _output.ToString();
}
