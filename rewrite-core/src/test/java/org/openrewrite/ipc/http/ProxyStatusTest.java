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
package org.openrewrite.ipc.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ProxyStatusTest {

    @Test
    void identifiersOnly() {
        assertThat(parse("revproxy1.example.net, ExampleCDN")).containsExactly(
          new ProxyStatus("revproxy1.example.net", null, null, null, null),
          new ProxyStatus("ExampleCDN", null, null, null, null)
        );
    }

    @Test
    void rfcExamples() {
        assertThat(parse("r34.example.net; error=http_request_error, ExampleCDN")).containsExactly(
          new ProxyStatus("r34.example.net", null, "http_request_error", null, null),
          new ProxyStatus("ExampleCDN", null, null, null, null)
        );
        assertThat(parse("cdn.example.org; next-hop=backend.example.org:8001")).containsExactly(
          new ProxyStatus("cdn.example.org", "backend.example.org:8001", null, null, null));
        assertThat(parse("\"proxy.example.org\"; next-protocol=h2")).containsExactly(
          new ProxyStatus("proxy.example.org", null, null, null, null));
        assertThat(parse("ExampleCDN; received-status=200")).containsExactly(
          new ProxyStatus("ExampleCDN", null, null, 200, null));
        assertThat(parse("proxy.example.net; error=\"http_protocol_error\"; details=\"Malformed response header: space before colon\""))
          .containsExactly(
          new ProxyStatus("proxy.example.net", null, "http_protocol_error", null, "Malformed response header: space before colon"));
    }

    @Test
    void quotedValuesMayContainDelimitersAndEscapes() {
        assertThat(parse("gw; next-hop=\"https://repo.example.com/maven\"; details=\"a, b; \\\"c\\\" \\\\ d\"; flag; weight=0.5"))
          .containsExactly(new ProxyStatus("gw", "https://repo.example.com/maven", null, null, "a, b; \"c\" \\ d"));
    }

    @Test
    void fieldLinesJoinInOrderWhateverTheHeaderNameCase() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(null, singletonList("HTTP/1.1 404 Not Found"));
        headers.put("proxy-status", List.of("a; received-status=404", "b; error=dns_error"));
        assertThat(ProxyStatus.parse(headers)).extracting(ProxyStatus::getIdentifier).containsExactly("a", "b");
    }

    @ParameterizedTest
    @ValueSource(strings = {"gw;", "gw; Error=dns_error", "gw, ", "gw; details=\"unterminated", "(a b)", "gw next"})
    void malformedFieldsYieldNothing(String field) {
        assertThat(parse(field)).isEmpty();
    }

    @Test
    void absentField() {
        assertThat(ProxyStatus.parse(Map.of("Content-Type", List.of("text/xml")))).isEmpty();
    }

    @Test
    void render() {
        assertThat(ProxyStatus.render(parse(
          "gw; next-hop=\"https://repo.example.com/maven\"; received-status=404, " +
          "gw; next-hop=\"https://mirror.example.com/maven\"; error=connection_timeout; details=\"after 30s\", " +
          "edge; error=http_response_timeout, " +
          "passthrough"
        ))).isEqualTo(", proxying:\n" +
                      "  https://repo.example.com/maven: HTTP 404\n" +
                      "  https://mirror.example.com/maven: connection_timeout - after 30s\n" +
                      "  edge: http_response_timeout");
        assertThat(ProxyStatus.render(parse("passthrough"))).isEmpty();
    }

    private static List<ProxyStatus> parse(String field) {
        return ProxyStatus.parse(Map.of("Proxy-Status", singletonList(field)));
    }
}
