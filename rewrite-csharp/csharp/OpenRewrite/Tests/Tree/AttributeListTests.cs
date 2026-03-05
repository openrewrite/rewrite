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
}
