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
using Nerdbank.Streams;
using OpenRewrite.Core;
using OpenRewrite.CSharp.Rpc;
using StreamJsonRpc;

namespace OpenRewrite.Tests.Rpc;

public class NotificationOrderTest
{
    [Fact]
    public async Task ANotificationTakesEffectBeforeTheNextRequestIsHandled()
    {
        var (serverSide, javaSide) = FullDuplexStream.CreatePair();
        using var connection = new StringErrorDataJsonRpc(
            new HeaderDelimitedMessageHandler(serverSide, serverSide, new SystemTextJsonFormatter()));
        var evicted = false;
        connection.AddLocalRpcMethod("SlowEvict", () =>
        {
            // long enough for a request handled alongside it to answer first
            Thread.Sleep(300);
            evicted = true;
        });
        connection.AddLocalRpcMethod("WasEvicted", () => evicted);
        new RewriteRpcServer(new RecipeMarketplace()).Connect(connection);

        using var java = JsonRpc.Attach(javaSide);
        await java.NotifyAsync("SlowEvict");
        Assert.True(await java.InvokeAsync<bool>("WasEvicted"));
    }
}
