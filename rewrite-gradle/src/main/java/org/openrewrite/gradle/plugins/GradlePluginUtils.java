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
package org.openrewrite.gradle.plugins;

import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.ipc.http.HttpSender;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

public class GradlePluginUtils {

    public static List<String> availablePluginVersions(String pluginId, ExecutionContext ctx) {
        String uri = "https://plugins.gradle.org/plugin/" + pluginId;
        HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getHttpSender();

        try (HttpSender.Response response = httpSender.send(httpSender.get(uri).build())) {
            if (response.isSuccessful()) {
                @Language("xml")
                String responseBody = StringUtils.readFully(response.getBody(), StandardCharsets.UTF_8);

                List<String> versions = new ArrayList<>();
                Matcher matcher = Pattern.compile("href=\"/plugin/" + pluginId + "/([^\"]+)\"").matcher(responseBody);
                int lastFind = 0;
                while (matcher.find(lastFind)) {
                    versions.add(matcher.group(1));
                    lastFind = matcher.end();
                }

                matcher = Pattern.compile("Version (\\S+) \\(latest\\)").matcher(responseBody);
                if (matcher.find()) {
                    versions.add(matcher.group(1));
                }

                return versions;
            }
        } catch (Throwable ignored) {
        }
        return emptyList();
    }
}
