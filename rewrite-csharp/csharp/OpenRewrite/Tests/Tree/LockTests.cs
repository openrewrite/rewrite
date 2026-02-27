using OpenRewrite.Test;

namespace OpenRewrite.Tests.Tree;

public class LockTests : RewriteTest
{
    [Fact]
    public void SimpleLock()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    object _sync = new object();
                    void Bar() {
                        lock (_sync) {
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void LockWithThis()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    void Bar() {
                        lock (this) {
                            DoSomething();
                        }
                    }
                    void DoSomething() { }
                }
                """
            )
        );
    }

    [Fact]
    public void LockWithFieldAccess()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    object _syncObj = new object();
                    void Bar() {
                        lock (this._syncObj) {
                            DoSomething();
                        }
                    }
                    void DoSomething() { }
                }
                """
            )
        );
    }

    [Fact]
    public void LockWithStaticField()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    static object SyncRoot = new object();
                    void Bar() {
                        lock (SyncRoot) {
                            DoSomething();
                        }
                    }
                    void DoSomething() { }
                }
                """
            )
        );
    }

    [Fact]
    public void LockWithMultipleStatements()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    object _sync = new object();
                    int _value;
                    void Bar() {
                        lock (_sync) {
                            _value = 1;
                            _value = 2;
                            _value = 3;
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void LockWithWhitespace()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    object _sync = new object();
                    void Bar() {
                        lock  (  _sync  )  {
                        }
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NestedLocks()
    {
        RewriteRun(
            CSharp(
                """
                class Foo {
                    object _sync1 = new object();
                    object _sync2 = new object();
                    void Bar() {
                        lock (_sync1) {
                            lock (_sync2) {
                                DoSomething();
                            }
                        }
                    }
                    void DoSomething() { }
                }
                """
            )
        );
    }
}
