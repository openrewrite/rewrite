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
package org.openrewrite.rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.moderne.jsonrpc.JsonRpcRequest;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import org.junit.jupiter.api.Test;
import org.openrewrite.TreeVisitor;
import org.openrewrite.config.RecipeDescriptor;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class RpcRecipeTest {

    @Test
    void peerHandleAndPreconditionVisitorsStayOffTheRpcWire() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new JsonMessageFormatter(new SimpleModule()).serialize(JsonRpcRequest.newRequest("Visit", rpcRecipe()), out);

        assertThat(out.toString())
          .doesNotContain("\"rpc\":")
          .doesNotContain("\"editPreconditionVisitor\":")
          .doesNotContain("\"scanPreconditionVisitor\":")
          .doesNotContain("cursor");
    }

    @Test
    void peerHandleAndPreconditionVisitorsAreNotGetterVisibleProperties() {
        // The RPC formatter reads fields only, where `transient` suppresses these in-process
        // handles. A mapper on Jackson's default visibility, as RpcObjectData's is, reads public
        // getters, so the exclusion has to hold for getters too.
        assertThat(getterVisibleProperties())
          .doesNotContain("rpc", "editPreconditionVisitor", "scanPreconditionVisitor");
    }

    private static RpcRecipe rpcRecipe() {
        RecipeDescriptor descriptor = new RecipeDescriptor("com.example.Remote", "Remote", "Remote", "",
          emptySet(), null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
          emptyList(), URI.create("test:rpc"));
        // A visitor carries a cursor, which is what reaches this run's accumulated state.
        return new RpcRecipe(null, "remote-id", descriptor, "EditVisitor", TreeVisitor.noop(),
          "ScanVisitor", TreeVisitor.noop(), List.of());
    }

    private static List<String> getterVisibleProperties() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.getSerializationConfig()
          .introspect(mapper.constructType(RpcRecipe.class))
          .findProperties().stream()
          .map(BeanPropertyDefinition::getName)
          .collect(toList());
    }
}
