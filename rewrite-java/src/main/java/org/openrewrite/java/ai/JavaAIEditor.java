/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.ai;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.ai.AIExecutionContextView;
import org.openrewrite.ai.CodeEditRequest;
import org.openrewrite.ai.CodeEditResponse;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markup;

import java.io.IOException;
import java.util.function.Supplier;

public class JavaAIEditor {
    private static final ObjectMapper mapper = JsonMapper.builder()
        .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
        .build()
        .registerModule(new ParameterNamesModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final Supplier<Cursor> cursor;
    private final HttpSender http;
    private final AIExecutionContextView ctx;

    public JavaAIEditor(Supplier<Cursor> cursor, ExecutionContext context) {
        this.cursor = cursor;
        this.ctx = AIExecutionContextView.view(context);
        this.http = HttpSenderExecutionContextView.view(context).getHttpSender();
    }

    public <J2 extends Statement> J2 edit(J2 j, String instruction) {
        String input = j.printTrimmed(cursor.get());
        try (HttpSender.Response raw = http
            .post("https://api.openai.com/v1/edits")
            .withHeader("Authorization", "Bearer " + ctx.getOpenapiToken().trim())
            .withContent("application/json", mapper.writeValueAsBytes(new CodeEditRequest(instruction, input)))
            .send()) {

            CodeEditResponse response = mapper.readValue(raw.getBodyAsBytes(), CodeEditResponse.class);
            if (response.getError() != null) {
                return Markup.warn(j, new IllegalStateException("Code edit failed: " + response.getError()));
            }

            return j.withTemplate(JavaTemplate.builder(cursor, response.getChoices().get(0).getText()).build(),
                j.getCoordinates().replace());
        } catch (IOException e) {
            return Markup.warn(j, e);
        }
    }
}
