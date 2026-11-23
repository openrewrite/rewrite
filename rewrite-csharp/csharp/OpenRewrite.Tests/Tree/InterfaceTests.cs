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

public class InterfaceTests : RewriteTest
{
    [Fact]
    public void SimpleInterface()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo {
                }
                """
            )
        );
    }

    [Fact]
    public void InterfaceWithMethod()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo {
                    void Bar();
                }
                """
            )
        );
    }

    [Fact]
    public void InterfaceWithProperty()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo {
                    int Value { get; set; }
                }
                """
            )
        );
    }

    [Fact]
    public void InterfaceWithModifiers()
    {
        RewriteRun(
            CSharp(
                """
                public interface IFoo {
                    void Bar();
                }
                """
            )
        );
    }

    [Fact]
    public void InterfaceWithBaseInterface()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo {
                }
                interface IBar : IFoo {
                }
                """
            )
        );
    }

    [Fact]
    public void InterfaceWithMultipleBaseInterfaces()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo {
                }
                interface IBar {
                }
                interface IBaz : IFoo, IBar {
                }
                """
            )
        );
    }

    [Fact]
    public void GenericInterface()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo<T> {
                    T GetValue();
                }
                """
            )
        );
    }

    [Fact]
    public void GenericInterfaceWithConstraint()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo<T> where T : class {
                    T GetValue();
                }
                """
            )
        );
    }

    [Fact]
    public void InterfaceWithDefaultImplementation()
    {
        RewriteRun(
            CSharp(
                """
                interface IFoo {
                    void Bar() {
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void InterfaceWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                interface  IFoo  {
                }
                """
            )
        );
    }
}
