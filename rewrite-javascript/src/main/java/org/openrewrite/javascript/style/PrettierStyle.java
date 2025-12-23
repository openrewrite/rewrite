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
package org.openrewrite.javascript.style;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Marker containing the resolved Prettier configuration for a source file.
 * <p>
 * This marker is added by the parser when a Prettier config is detected in the project.
 * The config is resolved per-file (with overrides applied), so files with different
 * override rules will have different marker instances.
 * <p>
 * When this marker is present, AutoformatVisitor will use Prettier for formatting
 * instead of the built-in formatting visitors.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class PrettierStyle extends NamedStyles implements RpcCodec<PrettierStyle> {
    private static final String NAME = "org.openrewrite.javascript.Prettier";
    private static final String DISPLAY_NAME = "Prettier";
    private static final String DESCRIPTION = "Prettier code formatter configuration.";

    /**
     * The resolved Prettier options for this file (with overrides applied).
     */
    private final Map<String, Object> config;

    /**
     * The Prettier version from the project's package.json.
     * At formatting time, this version of Prettier will be loaded dynamically
     * (similar to npx) to ensure consistent formatting.
     */
    private final String prettierVersion;

    /**
     * Whether this file is ignored by .prettierignore.
     * When true, Prettier formatting should be skipped for this file.
     */
    private final boolean ignored;

    public PrettierStyle(UUID id, Map<String, Object> config, String prettierVersion, boolean ignored) {
        super(id, NAME, DISPLAY_NAME, DESCRIPTION, Collections.emptySet(), Collections.emptyList());
        this.config = config;
        this.prettierVersion = prettierVersion;
        this.ignored = ignored;
    }

    @Override
    public PrettierStyle withId(UUID id) {
        return id == getId() ? this : new PrettierStyle(id, config, prettierVersion, ignored);
    }

    public PrettierStyle withConfig(Map<String, Object> config) {
        return config == this.config ? this : new PrettierStyle(getId(), config, prettierVersion, ignored);
    }

    public PrettierStyle withPrettierVersion(String prettierVersion) {
        return prettierVersion.equals(this.prettierVersion) ? this : new PrettierStyle(getId(), config, prettierVersion, ignored);
    }

    public PrettierStyle withIgnored(boolean ignored) {
        return ignored == this.ignored ? this : new PrettierStyle(getId(), config, prettierVersion, ignored);
    }

    @Override
    public PrettierStyle withStyles(Collection<Style> styles) {
        // PrettierStyle doesn't use the styles collection
        return this;
    }

    @Override
    public void rpcSend(PrettierStyle after, RpcSendQueue q) {
        q.getAndSend(after, NamedStyles::getId);
        q.getAndSend(after, PrettierStyle::getConfig);
        q.getAndSend(after, PrettierStyle::getPrettierVersion);
        q.getAndSend(after, PrettierStyle::isIgnored);
    }

    @Override
    public PrettierStyle rpcReceive(PrettierStyle before, RpcReceiveQueue q) {
        return before.withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withConfig(q.receive(before.getConfig()))
                .withPrettierVersion(q.receive(before.getPrettierVersion()))
                .withIgnored(q.receive(before.isIgnored()));
    }
}
