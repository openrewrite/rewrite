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
package org.openrewrite.xml.rpc;

import org.junit.jupiter.api.Test;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.xml.tree.Xml;

import static org.assertj.core.api.Assertions.assertThat;

class SubclassCodecDispatchTest {

    /**
     * Stand-in for a moderne-cli V3 lazy proxy: a non-final SourceFile subtype whose
     * runtime class name is not the canonical type registered with the codec.
     */
    static class LazyXmlDocument extends Xml.Document {
        LazyXmlDocument() {
            super(null, null, null, null, null, false, null, null, null, null, null);
        }
    }

    @Test
    void codecResolvesForSubclassByRuntimeClassName() {
        Xml.Document proxy = new LazyXmlDocument();
        // Sanity: codec resolves under the canonical type name (passes today).
        assertThat(DynamicDispatchRpcCodec.getCodec(proxy, Xml.Document.class.getName()))
                .as("codec under canonical type name").isNotNull();
        // Bug: RewriteRpc derives the codec key from getClass().getName(); for a
        // proxy/subclass that key is not in the codec map -> null codec (FAILS today).
        assertThat(DynamicDispatchRpcCodec.getCodec(proxy, proxy.getClass().getName()))
                .as("codec under subclass (proxy) runtime class name").isNotNull();
    }

    @Test
    void canonicalSourceFileTypeWalksToRegisteredSupertype() {
        // The wire string the rest of the pipeline (including the Python receiver) keys on
        // must be the canonical, codec-registered type name, not the proxy subclass name.
        assertThat(DynamicDispatchRpcCodec.canonicalSourceFileType(LazyXmlDocument.class))
                .as("proxy subclass resolves to canonical registered type")
                .isEqualTo(Xml.Document.class.getName());
    }

    @Test
    void canonicalSourceFileTypeUnchangedForRegisteredType() {
        // A normal LST whose runtime class IS the registered type must be unaffected.
        assertThat(DynamicDispatchRpcCodec.canonicalSourceFileType(Xml.Document.class))
                .as("registered type resolves to itself")
                .isEqualTo(Xml.Document.class.getName());
    }
}
