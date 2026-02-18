using System.Text;

namespace OpenRewrite.Core;

/// <summary>
/// Captures output during tree printing operations.
/// </summary>
public class PrintOutputCapture<T>
{
    private readonly StringBuilder _output = new();

    public T Context { get; }

    public IMarkerPrinter MarkerPrinter { get; }

    public PrintOutputCapture(T context) : this(context, Core.MarkerPrinter.Default)
    {
    }

    public PrintOutputCapture(T context, IMarkerPrinter markerPrinter)
    {
        Context = context;
        MarkerPrinter = markerPrinter;
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
