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
package org.openrewrite.javascript.internal.lock.pnpm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsYaml;
import org.openrewrite.javascript.internal.lock.JsYamlDumper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The YAML reader and writer against @zkochan/js-yaml 0.0.11, which pnpm 11.2.2 bundles, by the rows of
 * {@code pnpm/generate/yaml-differential.js}.
 */
class JsYamlDifferentialTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    // a date as a mapping key: its text is Date.prototype.toString's, in the generating host's time zone
    private static final Pattern DATE_TEXT = Pattern.compile("[A-Z][a-z]{2} [A-Z][a-z]{2} \\d{2} -?\\d+ " +
                                                             "\\d{2}:\\d{2}:\\d{2} GMT");

    @Test
    void dumpsAsPnpmDoes() {
        List<String> differences = new ArrayList<>();
        for (String[] row : rows("dump")) {
            String expected = row[2];
            Object value = value(tree(row[1]));
            String actual;
            try {
                actual = JsYamlDumper.dump(value);
            } catch (RuntimeException e) {
                if (!expected.startsWith("!")) {
                    differences.add(row[1] + " => " + expected + " / threw " + e);
                }
                continue;
            }
            if (expected.startsWith("!") || !actual.equals(tree(expected).textValue())) {
                differences.add(row[1] + " => " + expected + " / " + MAPPER.valueToTree(actual));
            }
        }
        assertThat(differences).isEmpty();
    }

    @Test
    void loadsAsPnpmDoes() {
        List<String> differences = new ArrayList<>();
        for (String[] row : rows("load")) {
            String text = tree(row[1]).textValue();
            String expected = row[2];
            Object actual;
            try {
                actual = JsYaml.load(text);
            } catch (CannotKnow e) {
                if (!DATE_TEXT.matcher(expected).find()) {
                    differences.add(row[1] + " => " + expected + " / threw " + e);
                }
                continue;
            } catch (RuntimeException e) {
                if (!expected.startsWith("!") || !(e instanceof JsYaml.YamlException) ||
                    !tree(expected.substring(1)).textValue().equals(e.getMessage())) {
                    differences.add(row[1] + " => " + expected + " / threw " + e);
                }
                continue;
            }
            if (expected.startsWith("!") || !tree(expected).equals(tag(actual))) {
                differences.add(row[1] + " => " + expected + " / " + tag(actual));
            }
        }
        assertThat(differences).isEmpty();
    }

    private static @Nullable Object value(JsonNode tagged) {
        Map.Entry<String, JsonNode> entry = tagged.fields().next();
        JsonNode v = entry.getValue();
        switch (entry.getKey()) {
            case "u":
                return Js.UNDEFINED;
            case "z":
                return null;
            case "s":
                return v.textValue();
            case "b":
                return v.booleanValue();
            case "n":
                return "-0".equals(v.textValue()) ? -0.0 : Double.parseDouble(v.textValue());
            case "d":
                return new JsYaml.Timestamp(v.asDouble());
            case "y": {
                byte[] bytes = new byte[v.size()];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = (byte) v.get(i).intValue();
                }
                return bytes;
            }
            case "a": {
                List<@Nullable Object> out = new ArrayList<>();
                for (JsonNode element : v) {
                    out.add(value(element));
                }
                return out;
            }
            case "o": {
                Map<String, @Nullable Object> out = new LinkedHashMap<>();
                for (JsonNode pair : v) {
                    out.put(pair.get(0).textValue(), value(pair.get(1)));
                }
                return out;
            }
            default:
                throw new IllegalArgumentException(tagged.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private static JsonNode tag(@Nullable Object value) {
        ObjectNode out = NODES.objectNode();
        if (value == Js.UNDEFINED) {
            out.put("u", 1);
        } else if (value == null) {
            out.put("z", 1);
        } else if (value instanceof String) {
            out.put("s", (String) value);
        } else if (value instanceof Boolean) {
            out.put("b", (Boolean) value);
        } else if (value instanceof Double) {
            double d = (Double) value;
            out.put("n", d == 0 && 1 / d < 0 ? "-0" : Js.numberToString(d));
        } else if (value instanceof JsYaml.Timestamp) {
            double time = ((JsYaml.Timestamp) value).getTime();
            if (Double.isNaN(time)) {
                out.putNull("d");
            } else {
                out.put("d", (long) time);
            }
        } else if (value instanceof byte[]) {
            ArrayNode bytes = out.putArray("y");
            for (byte b : (byte[]) value) {
                bytes.add(b & 0xFF);
            }
        } else if (value instanceof List) {
            ArrayNode elements = out.putArray("a");
            for (Object element : (List<Object>) value) {
                elements.add(tag(element));
            }
        } else if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            ArrayNode pairs = out.putArray("o");
            for (String key : Js.keys(map)) {
                pairs.add(NODES.arrayNode().add(key).add(tag(map.get(key))));
            }
        } else {
            throw new IllegalArgumentException(value.getClass().toString());
        }
        return out;
    }

    private static JsonNode tree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String[]> rows(String kind) {
        List<String[]> out = new ArrayList<>();
        InputStream gzipped = JsYamlDifferentialTest.class.getClassLoader()
          .getResourceAsStream("pnpm/differential/yaml.tsv.gz");
        assertThat(gzipped).isNotNull();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(gzipped),
          StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\t", -1);
                if (fields[0].equals(kind)) {
                    out.add(fields);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertThat(out).as(kind).isNotEmpty();
        return out;
    }
}
