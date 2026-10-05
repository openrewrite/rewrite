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
using System.Text.Json;
using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;
using OpenRewrite.CSharp.Rpc;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// A single DependencyTypes response is paginated across the client's repeated identical requests:
/// each call returns at most <c>DependencyTypesBatchSize</c> items, the concatenation reproduces a
/// single-shot run exactly (same items, same order, END_OF_OBJECT last), and the cache entry is
/// freed once drained so a fresh request rebuilds. Mirrors the Go/JS/Python engine tests.
/// </summary>
public class DependencyTypesPaginationTest
{
    // A null version resolves from the engine's own shared framework, so no package fixtures.
    // System.Linq is a real implementation assembly there (System.Runtime is a facade of forwarders).
    private static DependencyRequest Request() => new()
    {
        Id = "System.Linq",
        Version = null,
        TargetFramework = "net10.0",
    };

    [Fact]
    public void PagesResponseAcrossRepeatedRequests()
    {
        // Single-shot reference: one batch large enough to hold the whole response.
        var single = new RewriteRpcServer(new RecipeMarketplace()) { DependencyTypesBatchSize = int.MaxValue };
        var want = single.DependencyTypes(Request()).Result;
        Assert.NotEmpty(want);
        Assert.Equal(RpcObjectData.ObjectState.END_OF_OBJECT, want[^1].State);
        Assert.True(want.Count > 2, "response should exceed a single tiny slice");

        // Paginated: same request, tiny batch, drained across repeated calls.
        var server = new RewriteRpcServer(new RecipeMarketplace()) { DependencyTypesBatchSize = 2 };
        var got = new List<RpcObjectData>();
        var slices = 0;
        for (var calls = 0; ; calls++)
        {
            Assert.True(calls <= want.Count + 2, "pagination did not terminate");
            var batch = server.DependencyTypes(Request()).Result;
            Assert.True(batch.Count <= 2, $"batch of {batch.Count} exceeds batchSize 2");
            slices++;
            got.AddRange(batch);
            if (batch.Count > 0 && batch[^1].State == RpcObjectData.ObjectState.END_OF_OBJECT)
            {
                break;
            }
        }

        Assert.True(slices > 1, "expected the response to span multiple slices");
        // Same items, same order as the single-shot run (compared by serialized wire form).
        Assert.Equal(
            JsonSerializer.Serialize(want, RpcJson.Options),
            JsonSerializer.Serialize(got, RpcJson.Options));

        // Cache freed after drain: a fresh request rebuilds and hands back the first slice again
        // (a non-evicted entry would have nothing left to return).
        var afterDrain = server.DependencyTypes(Request()).Result;
        Assert.Equal(
            JsonSerializer.Serialize(got.Take(2), RpcJson.Options),
            JsonSerializer.Serialize(afterDrain, RpcJson.Options));
    }
}
