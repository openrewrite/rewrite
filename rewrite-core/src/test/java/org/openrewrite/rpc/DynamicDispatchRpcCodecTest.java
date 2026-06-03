/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.rpc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicDispatchRpcCodecTest {

    /**
     * A language marker interface, plus a concrete tree node that sits directly on
     * {@link Object} -- the shape every real LST node has. {@code Generated*} stand in for
     * synthetic subclasses produced at runtime (e.g. by a lazy-loading proxy generator),
     * whose {@code getClass().getName()} the remote has no codec for.
     */
    interface TestTree {
    }

    static class TestLeaf implements TestTree {
    }

    static class GeneratedLeaf extends TestLeaf {
    }

    static class GeneratedLeaf2 extends GeneratedLeaf {
    }

    static class TestTreeCodec extends DynamicDispatchRpcCodec<TestTree> {
        @Override
        public String getSourceFileType() {
            return TestLeaf.class.getName();
        }

        @Override
        public Class<? extends TestTree> getType() {
            return TestTree.class;
        }

        @Override
        public void rpcSend(TestTree after, RpcSendQueue q) {
        }

        @Override
        public TestTree rpcReceive(TestTree before, RpcReceiveQueue q) {
            return before;
        }
    }

    private final TestTreeCodec codec = new TestTreeCodec();

    @Test
    void realNodeKeepsItsOwnType() {
        assertThat(codec.valueType(new TestLeaf())).isEqualTo(TestLeaf.class.getName());
    }

    @Test
    void syntheticSubclassResolvesToRegisteredType() {
        assertThat(codec.valueType(new GeneratedLeaf())).isEqualTo(TestLeaf.class.getName());
    }

    @Test
    void multiLevelSyntheticSubclassResolvesToRegisteredType() {
        assertThat(codec.valueType(new GeneratedLeaf2())).isEqualTo(TestLeaf.class.getName());
    }

    @Test
    void defaultValueTypeIsRuntimeClassName() {
        RpcCodec<String> selfEncoding = new RpcCodec<String>() {
            @Override
            public void rpcSend(String after, RpcSendQueue q) {
            }

            @Override
            public String rpcReceive(String before, RpcReceiveQueue q) {
                return before;
            }
        };
        assertThat(selfEncoding.valueType("x")).isEqualTo(String.class.getName());
    }
}
