/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class XmlRpcRoundTripTest {

    @Test
    void jspDirectiveIsReceivedAsNewTree() {
        // given
        String jsp = "<%@ page contentType=\"text/html\" %>\n<html><% int i = 0; %><p><%= i %></p></html>\n";
        Xml.Document document = parse(jsp, "index.jsp");

        // when
        Xml.Document received = sendAndReceive(document);

        // then
        assertThat(received.getRoot().getContent()).hasSize(2);
        assertThat(received.getProlog().getJspDirectives().get(0).getType()).isEqualTo("page");
        assertThat(received.printAll()).isEqualTo(jsp);
    }

    @Test
    void documentIsReceivedAsNewTree() {
        // given
        String xml = "<?xml version=\"1.0\"?>\n<!DOCTYPE a [<!ENTITY e \"x\">]>\n<a b='c'><!-- d --><![CDATA[e]]>&e;<f/></a>\n";
        Xml.Document document = parse(xml, "file.xml");

        // when
        Xml.Document received = sendAndReceive(document);

        // then
        assertThat(received.printAll()).isEqualTo(xml);
    }

    private static Xml.Document parse(String source, String path) {
        SourceFile sourceFile = new XmlParser().parseInputs(
          singletonList(Parser.Input.fromString(Paths.get(path), source)), null, new InMemoryExecutionContext(Throwable::printStackTrace)
        ).findFirst().orElseThrow();
        assertThat(sourceFile).isInstanceOf(Xml.Document.class);
        return (Xml.Document) sourceFile;
    }

    private static Xml.Document sendAndReceive(Xml.Document after) {
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1_000_000, batches::addLast, new IdentityHashMap<>(),
          Xml.Document.class.getName(), false);
        sq.send(after, null, null);
        sq.flush();

        // The wire carries a UUID as a string, as the transport's JSON encoding would.
        List<RpcObjectData> all = new ArrayList<>();
        while (!batches.isEmpty()) {
            for (RpcObjectData data : batches.removeFirst()) {
                all.add(data.getValue() instanceof UUID ?
                  new RpcObjectData(data.getState(), data.getValueType(), data.getValue().toString(), data.getRef(), false) :
                  data);
            }
        }
        Deque<List<RpcObjectData>> drain = new ArrayDeque<>();
        drain.add(all);
        return new RpcReceiveQueue(new HashMap<>(), drain::removeFirst, Xml.Document.class.getName(), null).receive(null);
    }
}
