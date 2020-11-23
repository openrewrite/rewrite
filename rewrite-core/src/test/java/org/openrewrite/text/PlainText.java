/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.text;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.*;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;

import static java.util.Collections.emptyList;

public class PlainText implements SourceFile, Tree {
    private final UUID id;
    private final String text;
    private final Formatting formatting;
    private final Collection<Style> styles;

    @JsonCreator
    public PlainText(@JsonProperty("id") UUID id,
                     @JsonProperty("text") String text,
                     @JsonProperty("formatting") Formatting formatting,
                     @JsonProperty("styles") Collection<Style> styles) {
        this.id = id;
        this.text = text;
        this.formatting = formatting;
        this.styles = styles;
    }

    @Override
    public Collection<Style> getStyles() {
        return styles;
    }

    @Override
    public URI getSourcePath() {
        return null;
    }

    @Override
    public Collection<Metadata> getMetadata() {
        return emptyList();
    }

    @Override
    public PlainText withMetadata(Collection<Metadata> metadata) {
        return this;
    }

    @Override
    public Formatting getFormatting() {
        return formatting;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public PlainText withText(String toText) {
        return new PlainText(id, toText, formatting, styles);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Tree> T withFormatting(Formatting fmt) {
        return (T) new PlainText(id, text, fmt, styles);
    }

    @Override
    public String print() {
        return formatting.getPrefix() + text + formatting.getSuffix();
    }
}
