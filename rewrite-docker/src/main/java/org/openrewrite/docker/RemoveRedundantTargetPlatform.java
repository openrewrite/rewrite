/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.docker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.internal.ArgumentContents;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.internal.ListUtils;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveRedundantTargetPlatform extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove a `--platform=$TARGETPLATFORM` from `FROM`";
    }

    @Override
    public String getDescription() {
        return "BuildKit's `RedundantTargetPlatform` check reports a `FROM --platform=$TARGETPLATFORM`, which asks " +
                "for the platform the build already targets. Any other platform, whether a constant such as " +
                "`linux/amd64` or the `$BUILDPLATFORM` a cross-compiling stage builds on, says something the " +
                "default does not and is left alone.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new DockerIsoVisitor<ExecutionContext>() {
            @Override
            public Docker.From visitFrom(Docker.From from, ExecutionContext ctx) {
                Docker.From f = super.visitFrom(from, ctx);
                if (f.getFlags() == null) {
                    return f;
                }
                List<Docker.Flag> flags = ListUtils.map(f.getFlags(), flag ->
                        isTargetPlatform(flag) ? null : flag);
                return flags.isEmpty() ? f.withFlags(null) : f.withFlags(flags);
            }
        };
    }

    private static boolean isTargetPlatform(Docker.Flag flag) {
        if (!"platform".equals(flag.getName()) || flag.getValue() == null) {
            return false;
        }
        String value = ArgumentContents.textWithVariables(flag.getValue());
        return "$TARGETPLATFORM".equals(value) || "${TARGETPLATFORM}".equals(value);
    }
}
