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
using System.Collections.Concurrent;
using OpenRewrite.Core.Rpc;

namespace OpenRewrite.Tests.Rpc;

public class RpcRefsTest
{
    [Fact]
    public void ConcurrentRequestsNeverShareAnId()
    {
        var refs = new RpcRefs();
        var issued = new ConcurrentBag<int>();

        Parallel.For(0, 8, _ =>
        {
            for (int i = 0; i < 2000; i++)
            {
                issued.Add(refs.NextId());
            }
        });

        Assert.Equal(16000, issued.Distinct().Count());
    }

    [Fact]
    public void RollbackDropsLaterRefsAndReissuesFromTheMark()
    {
        var refs = new RpcRefs();
        var kept = new object();
        refs[kept] = refs.NextId();
        var mark = refs.HighWater;

        var evicted = new object();
        refs[evicted] = refs.NextId();

        refs.RollbackTo(mark);

        Assert.False(refs.ContainsKey(evicted));
        Assert.Equal(1, refs[kept]);

        // Reissuing from the mark is what keeps a later checkpoint's mark meaningful.
        Assert.Equal(2, refs.NextId());
    }
}
