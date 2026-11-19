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
using OpenRewrite.Core;
using OpenRewrite.Test;
using Xunit.Sdk;

namespace OpenRewrite.Tests.Rpc;

[Collection("RPC")]
public class ParseErrorRewriteRunTest(RpcFixture fixture) : RpcRewriteTest(fixture)
{
    private const string XmlDocument = "org.openrewrite.xml.tree.Xml$Document";

    [Fact]
    public void ASourceThatFailsToParseFailsTheTest()
    {
        var failure = Assert.Throws<FailException>(() => RewriteRun(
            new SourceSpec("<root><unclosed></root>", SourcePath: "broken.xml", SourceFileType: XmlDocument)
        ));

        Assert.StartsWith("Parsed source contains a ParseError at broken.xml: ", failure.Message);
    }

    [Fact]
    public void ASourceThatParsesPasses()
    {
        RewriteRun(
            new SourceSpec("<root><closed/></root>", SourcePath: "valid.xml", SourceFileType: XmlDocument)
        );
    }

    [Fact]
    public void AParseErrorPrintsItsText()
    {
        // given
        var parseError = ParseError.Build("broken.cs", "class {", new InvalidOperationException("boom"));

        // when
        var printed = PrintTree(parseError);

        // then
        Assert.Equal("class {", printed);
    }
}
