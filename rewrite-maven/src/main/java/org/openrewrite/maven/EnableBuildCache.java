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
package org.openrewrite.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class EnableBuildCache extends Recipe {

    @Override
    public String getDisplayName() {
        return "Enable Develocity build cache";
    }

    @Override
    public String getDescription() {
        return "Add Develocity build cache configuration to any `.mvn/` Develocity configuration files that lack existing configuration.";
    }

    @Option(displayName = "Value for buildCache->local->localEnabled",
            description = "Value for buildCache->local->localEnabled.",
            example = "true",
            required = false)
    @Nullable
    String buildCacheLocalEnabled;

    @Option(displayName = "Value for buildCache->remote->enabled",
            description = "Value for buildCache->remote->enabled.",
            example = "true",
            required = false)
    @Nullable
    String buildCacheRemoteEnabled;

    @Option(displayName = "Value for buildCache->remote->storeEnabled",
            description = "Value for buildCache->remote->storeEnabled.",
            example = "#{isTrue(env['CI'])}",
            required = false)
    @Nullable
    String buildCacheRemoteStoreEnabled;

    @Override
    public Validated<Object> validate(ExecutionContext ctx) {
        return super.validate(ctx)
                .and(Validated.notBlank("buildCacheLocalEnabled", buildCacheLocalEnabled)
                        .or(Validated.notBlank("buildCacheRemoteEnabled", buildCacheRemoteEnabled))
                        .or(Validated.notBlank("buildCacheRemoteStoreEnabled", buildCacheRemoteStoreEnabled)));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        XmlVisitor<ExecutionContext> visitor = new XmlVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Tag rootTag = document.getRoot();

                if ("develocity".equals(rootTag.getName()) && !rootTag.getChild("buildCache").isPresent()) {
                    Xml.Tag tag = Xml.Tag.build(getBuildCacheConfig());
                    rootTag = maybeAutoFormat(rootTag, rootTag.withContent(ListUtils.concat(rootTag.getChildren(), tag)), ctx);
                    return document.withRoot(rootTag);
                }
                return document;
            }
        };

        return Preconditions.check(new FindSourceFiles(".mvn/*.xml"), visitor);
    }

    private String getBuildCacheConfig() {
        StringBuilder sb = new StringBuilder("<buildCache>\n");

        if (buildCacheLocalEnabled != null) {
            sb.append("  <local>\n");
            sb.append("    <storeEnabled>").append(buildCacheLocalEnabled).append("</storeEnabled>\n");
            sb.append("  </local>\n");
        }

        if (buildCacheRemoteEnabled != null || buildCacheRemoteStoreEnabled != null) {
            sb.append("  <remote>\n");
            if (buildCacheRemoteEnabled != null) {
                sb.append("    <enabled>").append(buildCacheRemoteEnabled).append("</enabled>\n");
            }
            if (buildCacheRemoteStoreEnabled != null) {
                sb.append("    <storeEnabled>").append(buildCacheRemoteStoreEnabled).append("</storeEnabled>\n");
            }
            sb.append("  </remote>\n");
        }
        sb.append("</buildCache>");
        return sb.toString();
    }
}
