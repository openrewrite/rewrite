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

public class RecordDeclarationTests : RewriteTest
{
    [Fact]
    public void SimpleRecord()
    {
        RewriteRun(
            CSharp(
                """
                record Person {
                }
                """
            )
        );
    }

    [Fact]
    public void RecordWithPrimaryConstructor()
    {
        RewriteRun(
            CSharp(
                """
                record Person(string Name, int Age) {
                }
                """
            )
        );
    }

    [Fact]
    public void PositionalRecord()
    {
        RewriteRun(
            CSharp(
                """
                record Person(string Name, int Age);
                """
            )
        );
    }

    [Fact]
    public void RecordWithBody()
    {
        RewriteRun(
            CSharp(
                """
                record Person {
                    public string Name { get; init; }
                }
                """
            )
        );
    }

    [Fact]
    public void RecordClass()
    {
        RewriteRun(
            CSharp(
                """
                record class Person {
                }
                """
            )
        );
    }

    [Fact]
    public void RecordStruct()
    {
        RewriteRun(
            CSharp(
                """
                record struct Point {
                }
                """
            )
        );
    }

    [Fact]
    public void RecordStructWithPrimaryConstructor()
    {
        RewriteRun(
            CSharp(
                """
                record struct Point(int X, int Y) {
                }
                """
            )
        );
    }

    [Fact]
    public void PositionalRecordStruct()
    {
        RewriteRun(
            CSharp(
                """
                record struct Point(int X, int Y);
                """
            )
        );
    }

    [Fact]
    public void RecordWithInheritance()
    {
        RewriteRun(
            CSharp(
                """
                record Person(string Name);
                record Employee(string Name, string Department) : Person(Name);
                """
            )
        );
    }

    [Fact]
    public void GenericRecord()
    {
        RewriteRun(
            CSharp(
                """
                record Container<T>(T Value) {
                }
                """
            )
        );
    }

    [Fact]
    public void RecordWithModifiers()
    {
        RewriteRun(
            CSharp(
                """
                public sealed record Person(string Name) {
                }
                """
            )
        );
    }
}
