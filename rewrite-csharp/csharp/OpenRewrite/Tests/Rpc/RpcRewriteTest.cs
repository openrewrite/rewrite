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

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Defines a shared collection so all RPC test classes use a single Java RPC
/// server process and don't conflict on the static RewriteRpcServer.Current.
/// </summary>
[CollectionDefinition("RPC")]
public class RpcCollection : ICollectionFixture<RpcFixture>;

/// <summary>
/// Base class for tests that need a Java RPC connection.
/// Uses a shared collection fixture for one JVM across all test classes,
/// and IDisposable to reset state between tests.
/// </summary>
[Collection("RPC")]
public abstract class RpcRewriteTest : RewriteTest, IDisposable
{
    private readonly RpcFixture _fixture;

    protected RpcRewriteTest(RpcFixture fixture)
    {
        _fixture = fixture;
    }

    public void Dispose()
    {
        _fixture.Reset();
    }
}
