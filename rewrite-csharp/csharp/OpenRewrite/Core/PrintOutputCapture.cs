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
