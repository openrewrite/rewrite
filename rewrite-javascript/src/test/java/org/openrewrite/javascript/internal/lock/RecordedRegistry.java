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
package org.openrewrite.javascript.internal.lock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A registry replaying a scenario's recorded responses, which reports every route it was asked for and does not have.
 */
public abstract class RecordedRegistry<S extends LockScenario> {

    private static final ObjectMapper JSON = new ObjectMapper();

    protected final S scenario;
    private final List<String> unrecorded = new ArrayList<>();

    /**
     * The routes the replay asked for that the scenario does not record, in the order asked.
     */
    public final List<String> missing = Collections.unmodifiableList(unrecorded);

    protected RecordedRegistry(S scenario) {
        this.scenario = scenario;
    }

    protected void notRecorded(String route) {
        unrecorded.add(route);
    }

    /**
     * Whether a route the replay asked for is not recorded, for a scenario recorded elsewhere than by this module's
     * recorders, with no cache to fill it from: the replay could not be served.
     */
    public boolean unservedElsewhereRecording() {
        return !missing.isEmpty() && scenario.recordedWith() == null && scenario.getHttpCache() == null;
    }

    public String describe() {
        return missing.isEmpty() ? "" : " [routes not recorded: " + String.join(", ", missing) + "]";
    }

    /**
     * The scenario's {@code http-<name>/} for a registry other than those named, when it recorded that registry's own
     * documents ({@code registry.<name>.<tld>}).
     */
    protected @Nullable String mirror(String registry, String... publicRegistries) {
        String host = registry.replaceFirst("^[a-z]+://", "").replaceFirst("[/:].*$", "");
        String[] labels = host.split("\\.");
        if (labels.length < 3 || !"registry".equals(labels[0])) {
            return null;
        }
        for (String publicRegistry : publicRegistries) {
            if (publicRegistry.equals(labels[1])) {
                return null;
            }
        }
        String dir = "http-" + labels[1];
        return Files.isDirectory(scenario.getDir().resolve(dir)) ? dir : null;
    }

    /**
     * A packument whose empty versions a downloaded fixture records as {@code http/<name>-<version>} documents, filled
     * from them.
     */
    protected String withRecordedVersions(String name, String body) {
        try {
            JsonNode packument = JSON.readTree(body);
            JsonNode versions = packument.get("versions");
            if (!(versions instanceof ObjectNode)) {
                return body;
            }
            boolean filled = false;
            Iterator<Map.Entry<String, JsonNode>> entries = versions.fields();
            while (entries.hasNext()) {
                Map.Entry<String, JsonNode> entry = entries.next();
                String manifest = entry.getValue().size() == 0 ? scenario.http(name + "-" + entry.getKey()) : null;
                if (manifest != null) {
                    entry.setValue(JSON.readTree(manifest));
                    filled = true;
                }
            }
            return filled ? JSON.writeValueAsString(packument) : body;
        } catch (IOException e) {
            return body;
        }
    }
}
