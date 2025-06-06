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
package org.openrewrite.config;

import lombok.Getter;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.*;
import java.util.Map;
import java.util.Optional;

@Getter
public class LicenseKey {
    private static final String MODERNE_CLI_CONFIG_PATH = System.getProperty("user.home") + "/.moderne/cli/moderne.yml";

    private final String key;

    private LicenseKey(String key){
        this.key = key;
    }

    public static LicenseKey of(String key){
        return new LicenseKey(key);
    }

    public static Optional<LicenseKey> ofModerneCli() {
        File cliConfig = new File(MODERNE_CLI_CONFIG_PATH);
        if (cliConfig.exists()) {
            try (BufferedReader config = new BufferedReader(new FileReader(cliConfig))) {
                Object loaded = new Yaml(new SafeConstructor(new LoaderOptions())).load(config);
                if (loaded instanceof Map) {
                    Object license = ((Map<?, ?>) loaded).get("license");
                    if (license instanceof Map) {
                        Object licenseKey = ((Map<?, ?>) license).get("key");
                        if (licenseKey instanceof String) {
                            return Optional.of(LicenseKey.of((String) licenseKey));
                        }
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return Optional.empty();
    }
}
