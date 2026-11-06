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

import org.openrewrite.javascript.internal.registry.Environment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A conservative boundary for manager request policies the live adapter cannot yet bind. It never renders values:
 * proxy URLs and certificate settings may contain credentials. Offline recorded sources keep their own policy.
 */
public final class NetworkPolicy {
    private NetworkPolicy() {
    }

    public static void requireDirect(String manager, Map<String, ?> settings) {
        for (Map.Entry<String, ?> setting : settings.entrySet()) {
            Object value = setting.getValue();
            if (value != null && !Boolean.FALSE.equals(value) && !"".equals(value) &&
                (!(value instanceof Map) || !((Map<?, ?>) value).isEmpty())) {
                throw new CannotKnow(manager + " network policy requires " + setting.getKey() +
                                     ", which this registry adapter cannot safely apply");
            }
        }
    }

    /** Capture only named host settings, without copying credentials or changing the host environment. */
    static Map<String, String> environment(Environment environment, String... names) {
        Map<String, String> settings = new LinkedHashMap<>();
        for (String name : names) {
            String value = environment.getenv(name);
            if (value != null && !value.isEmpty()) {
                settings.put(name, value);
            }
        }
        return settings;
    }

    /** Defaults such as strict SSL must be normalized by the manager before this boundary. */
    public static Map<String, Object> select(Map<String, ?> values, String... keys) {
        Map<String, Object> settings = new LinkedHashMap<>();
        for (String key : keys) {
            settings.put(key, values.get(key));
        }
        return settings;
    }
}
