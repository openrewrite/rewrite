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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class EnableDevelocityBuildCache extends Recipe {

    String displayName = "Enable Develocity build cache";

    String description = "Add Develocity build cache configuration to any `.mvn/` Develocity configuration file that lack existing configuration.";

    @Option(displayName = "Enable local build cache",
            description = "Value for `//develocity/buildCache/local/enabled`.",
            example = "true",
            required = false)
    @Nullable
    String localEnabled;

    @Option(displayName = "Enable remote build cache",
            description = "Value for `//develocity/buildCache/remote/enabled`.",
            example = "true",
            required = false)
    @Nullable
    String remoteEnabled;

    @Option(displayName = "Enable remote build cache store",
            description = "Value for `//develocity/buildCache/remote/storeEnabled`.",
            example = "#{isTrue(env['CI'])}",
            required = false)
    @Nullable
    String remoteStoreEnabled;

    @Override
    public Validated<Object> validate(ExecutionContext ctx) {
        return super.validate(ctx)
                .and(Validated.notBlank("localEnabled", localEnabled)
                        .or(Validated.notBlank("remoteEnabled", remoteEnabled))
                        .or(Validated.notBlank("remoteStoreEnabled", remoteStoreEnabled)));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles(".mvn/*.xml"), new XmlVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Tag rootTag = document.getRoot();

                if ("develocity".equals(rootTag.getName()) && !rootTag.getChild("buildCache").isPresent()) {
                    Xml.Tag tag = Xml.Tag.build(buildCacheConfig());
                    rootTag = maybeAutoFormat(rootTag, rootTag.withContent(ListUtils.concat(rootTag.getChildren(), tag)), ctx);
                    return document.withRoot(rootTag);
                }
                return document;
            }

            private String buildCacheConfig() {
                StringBuilder sb = new StringBuilder("<buildCache>");
                if (!StringUtils.isBlank(localEnabled)) {
                    sb.append("<local>");
                    sb.append("<enabled>").append(localEnabled).append("</enabled>");
                    sb.append("</local>");
                }
                if (!StringUtils.isBlank(remoteEnabled) || !StringUtils.isBlank(remoteStoreEnabled)) {
                    sb.append("<remote>");
                    if (!StringUtils.isBlank(remoteEnabled)) {
                        sb.append("<enabled>").append(remoteEnabled).append("</enabled>");
                    }
                    if (!StringUtils.isBlank(remoteStoreEnabled)) {
                        sb.append("<storeEnabled>").append(remoteStoreEnabled).append("</storeEnabled>");
                    }
                    sb.append("</remote>");
                }
                sb.append("</buildCache>");
                return sb.toString();
            }
        });
    }
}
