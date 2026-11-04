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
using OpenRewrite.CSharp.Template;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.Template;

public class CaptureTests
{
    [Fact]
    public void CaptureGeneratesPlaceholderIdentifier()
    {
        var expr = Capture.Of<Expression>("expr");
        Assert.Equal("__plh_expr__", expr.ToString());
    }

    [Fact]
    public void CaptureAutoGeneratesName()
    {
        var c = Capture.Of<Expression>();
        Assert.StartsWith("_capture_", c.Name);
        Assert.Contains(c.Name, c.ToString());
    }

    [Fact]
    public void VariadicCaptureIsMarked()
    {
        var args = Capture.Expression("args", variadic: new());
        Assert.True(args.IsVariadic);
        Assert.Equal("__plh_args__", args.ToString());
    }

    [Fact]
    public void VariadicCaptureWithBounds()
    {
        var args = Capture.Expression("args", variadic: new(Min: 1, Max: 3));
        Assert.True(args.IsVariadic);
        Assert.Equal(1, args.MinCount);
        Assert.Equal(3, args.MaxCount);
    }

    [Fact]
    public void RawCodeToString()
    {
        var raw = Raw.Code("MyMethod");
        Assert.Equal("MyMethod", raw.ToString());
    }

    [Fact]
    public void PlaceholderRoundTrip()
    {
        Assert.True(Placeholder.IsPlaceholder("__plh_expr__"));
        Assert.Equal("expr", Placeholder.FromPlaceholder("__plh_expr__"));
    }

    [Fact]
    public void PlaceholderRejectsNonPlaceholders()
    {
        Assert.False(Placeholder.IsPlaceholder("myVariable"));
        Assert.False(Placeholder.IsPlaceholder("__plh___")); // too short
        Assert.Null(Placeholder.FromPlaceholder("myVariable"));
    }

    [Fact]
    public void HandlerInterceptsCaptureAndRaw()
    {
        var expr = Capture.Of<Expression>("expr");
        var raw = Raw.Code("Log");

        // Simulate what the compiler does with the interpolated string handler
        var handler = new TemplateStringHandler(20, 2);
        handler.AppendLiteral("logger.");
        handler.AppendFormatted(raw);
        handler.AppendLiteral("(");
        handler.AppendFormatted(expr);
        handler.AppendLiteral(")");

        Assert.Equal("logger.Log(__plh_expr__)", handler.GetCode());
        var captures = handler.GetCaptures();
        Assert.Single(captures);
        Assert.True(captures.ContainsKey("expr"));
    }
}
