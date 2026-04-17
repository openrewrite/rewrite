// Copyright 2026 the original author or authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;

namespace OpenRewrite.Java.Rpc;

/// <summary>
/// Wire codec for <see cref="JavaType.Annotation.ElementValue"/> constant values.
/// JSON cannot distinguish e.g. <c>Integer 42</c> from <c>Long 42</c> or
/// <c>Character 'c'</c> from <c>String "c"</c>, so each constant is encoded as a
/// tagged string on the wire: <c>"&lt;kind&gt;:&lt;lexical&gt;"</c> where
/// <c>&lt;kind&gt;</c> is one of <c>s</c> (String), <c>b</c> (Boolean),
/// <c>i</c> (Integer), <c>l</c> (Long), <c>S</c> (Short), <c>B</c> (Byte),
/// <c>f</c> (Float), <c>d</c> (Double), <c>c</c> (Character). Null is encoded
/// as the literal string <c>"n"</c> (or <c>null</c> on the wire — both are
/// accepted on receive).
/// <para>
/// Only the listed primitive-like values appear as constant values; class
/// literals and enum constants flow through the <c>ReferenceValue</c> branch
/// as <see cref="JavaType"/> references.
/// </para>
/// </summary>
internal static class AnnotationConstantValueCodec
{
    public static string? Encode(object? value)
    {
        return value switch
        {
            null => null,
            string s => "s:" + s,
            bool b => "b:" + (b ? "true" : "false"),
            int i => "i:" + i.ToString(CultureInfo.InvariantCulture),
            long l => "l:" + l.ToString(CultureInfo.InvariantCulture),
            short sh => "S:" + sh.ToString(CultureInfo.InvariantCulture),
            sbyte by => "B:" + by.ToString(CultureInfo.InvariantCulture),
            byte uby => "B:" + ((sbyte)uby).ToString(CultureInfo.InvariantCulture),
            float f => "f:" + f.ToString("R", CultureInfo.InvariantCulture),
            double d => "d:" + d.ToString("R", CultureInfo.InvariantCulture),
            char c => "c:" + c,
            _ => throw new ArgumentException(
                $"Unsupported annotation constant value type: {value.GetType().FullName}"),
        };
    }

    public static object? Decode(string? encoded)
    {
        if (encoded is null || encoded == "n") return null;
        if (encoded.Length < 2 || encoded[1] != ':')
        {
            throw new ArgumentException(
                $"Malformed annotation constant value envelope: {encoded}");
        }
        var kind = encoded[0];
        var body = encoded[2..];
        return kind switch
        {
            's' => body,
            'b' => body == "true",
            'i' => int.Parse(body, CultureInfo.InvariantCulture),
            'l' => long.Parse(body, CultureInfo.InvariantCulture),
            'S' => short.Parse(body, CultureInfo.InvariantCulture),
            'B' => sbyte.Parse(body, CultureInfo.InvariantCulture),
            'f' => float.Parse(body, CultureInfo.InvariantCulture),
            'd' => double.Parse(body, CultureInfo.InvariantCulture),
            'c' => body.Length == 0
                ? throw new ArgumentException("Malformed char envelope: empty body")
                : (object)body[0],
            _ => throw new ArgumentException(
                $"Unknown annotation constant value kind: {kind}"),
        };
    }

    public static IList<string?>? EncodeList(IList<object?>? values)
    {
        return values?.Select(Encode).ToList();
    }

    public static IList<object?>? DecodeList(IList<string?>? encoded)
    {
        return encoded?.Select(Decode).ToList();
    }
}
