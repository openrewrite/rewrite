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
using OpenRewrite.CSharp;
using OpenRewrite.Java;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class AttributeListTests : RewriteTest
{
    [Fact]
    public void SimpleAttribute()
    {
        RewriteRun(
            CSharp(
                """
                [Serializable]
                class Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void AttributeWithArgument()
    {
        RewriteRun(
            CSharp(
                """
                [Obsolete("This is deprecated")]
                class Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void MultipleAttributes()
    {
        RewriteRun(
            CSharp(
                """
                [Serializable]
                [Obsolete("deprecated")]
                class Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void AttributesOnSameLine()
    {
        RewriteRun(
            CSharp(
                """
                [Serializable, Obsolete("msg")]
                class Foo {
                }
                """
            )
        );
    }

    [Fact]
    public void AttributeWithEmptyArgsAndComment()
    {
        RewriteRun(
            CSharp(
                """
                [Serializable(/*bar*/)]
                class Test { }
                """
            )
        );
    }

    [Fact]
    public void AttributeWithEmptyArgsAndCommentIsStructured()
    {
        var parser = new CSharpParser();
        var cu = (CompilationUnit)parser.Parse("[Serializable(/*bar*/)]\nclass Test { }");
        var annotated = (AnnotatedStatement)cu.Members[0].Element;
        var annotation = annotated.AttributeLists[0].Attributes[0].Element;
        Assert.NotNull(annotation.Arguments);
        // The empty arg list should contain a J.Empty element
        Assert.Single(annotation.Arguments.Elements);
        // The comment should be structured in the After space, not raw whitespace
        var afterSpace = annotation.Arguments.Elements[0].After;
        Assert.Single(afterSpace.Comments);
        Assert.True(afterSpace.Comments[0].Multiline);
        Assert.Equal("bar", afterSpace.Comments[0].Text);
    }

    [Fact]
    public void AttributeOnPublicClass()
    {
        RewriteRun(
            CSharp(
                """
                [Obsolete("msg")]
                public class Test { }
                """
            )
        );
    }

    [Fact]
    public void AttributeOnStruct()
    {
        RewriteRun(
            CSharp(
                """
                [Serializable]
                struct Foo { }
                """
            )
        );
    }

    [Fact]
    public void AttributeOnEnum()
    {
        RewriteRun(
            CSharp(
                """
                [Flags]
                enum Permissions { Read = 1, Write = 2 }
                """
            )
        );
    }
}
