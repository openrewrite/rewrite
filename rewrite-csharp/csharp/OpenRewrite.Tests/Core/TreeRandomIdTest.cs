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
using CoreTree = OpenRewrite.Core.Tree;

namespace OpenRewrite.Tests.Core;

public class TreeRandomIdTest
{
    [Fact]
    public void CarriesTheVersionAndVariantOfAV4Uuid()
    {
        for (int i = 0; i < 200; i++)
        {
            // Guid lays its first three fields out little-endian, so the nibbles are only in
            // these positions if the bytes were stamped in RFC order.
            var text = CoreTree.RandomId().ToString();

            Assert.Equal('4', text[14]);
            Assert.Contains(text[19], "89ab");
        }
    }
}
