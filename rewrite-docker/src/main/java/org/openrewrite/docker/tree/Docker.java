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
package org.openrewrite.docker.tree;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.docker.DockerVisitor;
import org.openrewrite.docker.internal.DockerPrinter;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface Docker extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptDocker(v.adapt(DockerVisitor.class), p);
    }

    default <P> @Nullable Docker acceptDocker(DockerVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(DockerVisitor.class);
    }

    Space getPrefix();

    <D extends Docker> D withPrefix(Space prefix);

    /**
     * Root node representing a complete Dockerfile or Containerfile
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class File implements Docker, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        Path sourcePath;
        Space prefix;
        Markers markers;

        @With(AccessLevel.PRIVATE)
        String charsetName;

        boolean charsetBomMarked;

        @Nullable
        Checksum checksum;

        @Nullable
        FileAttributes fileAttributes;

        @Override
        public Charset getCharset() {
            return Charset.forName(charsetName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public File withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        /**
         * Global ARG instructions that appear before the first FROM instruction.
         * These have global scope and can be referenced in FROM instructions.
         */
        List<Arg> globalArgs;

        /**
         * Build stages, each starting with a FROM instruction.
         * Even single-stage Dockerfiles will have one Stage.
         */
        List<Stage> stages;

        Space eof;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitFile(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new DockerPrinter<>();
        }
    }

    /**
     * A build stage in a multi-stage Dockerfile.
     * Each stage begins with a FROM instruction and contains subsequent instructions
     * until the next FROM or end of file.
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Stage implements Docker {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * The FROM instruction that starts this stage
         */
        From from;

        /**
         * Instructions in this stage (RUN, COPY, etc.)
         * Does not include the FROM instruction or global ARGs
         */
        List<Instruction> instructions;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitStage(this, p);
        }
    }

    /**
     * Base interface for all Dockerfile instructions
     */
    interface Instruction extends Docker {
    }

    /**
     * FROM instruction - sets the base image
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class From implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        @Nullable
        List<Flag> flags;

        Argument imageName;

        @Nullable
        Argument tag;

        @Nullable
        Argument digest;

        @Nullable
        As as;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitFrom(this, p);
        }

        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class As {
            @EqualsAndHashCode.Include
            UUID id;

            Space prefix;
            Markers markers;
            String keyword;
            Argument name;
        }
    }

    /**
     * RUN instruction - executes commands
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Run implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        @Nullable
        List<Flag> flags;

        CommandLine commandLine;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitRun(this, p);
        }
    }

    /**
     * ADD instruction - adds files from source to destination (can extract archives and fetch URLs)
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Add implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        @Nullable
        List<Flag> flags;

        /**
         * Either a heredoc or a list of source paths (mutually exclusive)
         */
        @Nullable
        HeredocForm heredoc;

        @Nullable
        List<Argument> sources;

        @Nullable
        Argument destination;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitAdd(this, p);
        }
    }

    /**
     * COPY instruction - copies files from source to destination
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Copy implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        @Nullable
        List<Flag> flags;

        /**
         * Either a heredoc or a list of source paths (mutually exclusive)
         */
        @Nullable
        HeredocForm heredoc;

        @Nullable
        List<Argument> sources;

        @Nullable
        Argument destination;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitCopy(this, p);
        }
    }

    /**
     * ARG instruction - defines a build argument
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Arg implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        Argument name;

        @Nullable
        Argument value;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitArg(this, p);
        }
    }

    /**
     * ENV instruction - sets environment variables
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Env implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        List<EnvPair> pairs;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitEnv(this, p);
        }

        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class EnvPair {
            @EqualsAndHashCode.Include
            UUID id;

            Space prefix;
            Markers markers;
            Argument key;
            boolean hasEquals;  // true for KEY=value, false for KEY value (old format)
            Argument value;
        }
    }

    /**
     * LABEL instruction - adds metadata to an image
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Label implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        List<LabelPair> pairs;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitLabel(this, p);
        }

        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class LabelPair {
            @EqualsAndHashCode.Include
            UUID id;

            Space prefix;
            Markers markers;
            Argument key;
            // LABEL always uses equals sign
            Argument value;
        }
    }

    /**
     * CMD instruction - provides defaults for executing container
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Cmd implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        CommandLine commandLine;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitCmd(this, p);
        }
    }

    /**
     * ENTRYPOINT instruction - configures container to run as executable
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Entrypoint implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        CommandLine commandLine;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitEntrypoint(this, p);
        }
    }

    /**
     * EXPOSE instruction - documents ports that the container listens on
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Expose implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        List<Argument> ports;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitExpose(this, p);
        }
    }

    /**
     * VOLUME instruction - creates a mount point
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Volume implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        boolean jsonForm;  // true for ["path1", "path2"], false for path1 path2
        List<Argument> values;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitVolume(this, p);
        }
    }

    /**
     * SHELL instruction - sets the default shell
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Shell implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        List<Argument> arguments;  // JSON array elements

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitShell(this, p);
        }
    }

    /**
     * WORKDIR instruction - sets the working directory
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Workdir implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        Argument path;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitWorkdir(this, p);
        }
    }

    /**
     * USER instruction - sets the user and optionally group
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class User implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        Argument user;

        @Nullable
        Argument group;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitUser(this, p);
        }
    }

    /**
     * STOPSIGNAL instruction - sets the signal to stop the container
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Stopsignal implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        Argument signal;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitStopsignal(this, p);
        }
    }

    /**
     * ONBUILD instruction - prefixes another instruction to be executed later
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Onbuild implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        Instruction instruction;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitOnbuild(this, p);
        }
    }

    /**
     * HEALTHCHECK instruction - tells Docker how to test container health
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Healthcheck implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        boolean isNone;  // true for HEALTHCHECK NONE

        @Nullable
        List<Flag> flags;

        @Nullable
        Cmd cmd;  // null when isNone is true

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitHealthcheck(this, p);
        }
    }

    /**
     * MAINTAINER instruction - sets the author field (deprecated but still supported)
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Maintainer implements Instruction {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String keyword;

        Argument text;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitMaintainer(this, p);
        }
    }

    /**
     * Command line that can be in shell or exec form
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class CommandLine implements Docker {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * Either ShellForm or ExecForm
         */
        CommandForm form;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitCommandLine(this, p);
        }
    }

    /**
     * Base for command forms
     */
    interface CommandForm extends Docker {
    }

    /**
     * Shell form: CMD command param1 param2
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ShellForm implements CommandForm {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<Argument> arguments;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitShellForm(this, p);
        }
    }

    /**
     * Exec form: CMD ["executable", "param1", "param2"]
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class ExecForm implements CommandForm {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<Argument> arguments;

        /**
         * Whitespace before the closing bracket (to preserve " ]" vs "]")
         */
        Space closingBracketPrefix;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitExecForm(this, p);
        }
    }

    /**
     * Heredoc form: RUN <<EOF\ncommands\nEOF or COPY <<EOF /dest\ncommands\nEOF
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class HeredocForm implements CommandForm {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        /**
         * The opening marker including << and optional dash (e.g., "<<EOF" or "<<-EOF")
         */
        String opening;

        /**
         * Optional destination path (for COPY/ADD with inline destination)
         */
        @Nullable
        Argument destination;

        /**
         * Content lines between opening and closing markers
         */
        List<String> contentLines;

        /**
         * The closing marker (e.g., "EOF")
         */
        String closing;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitHeredocForm(this, p);
        }
    }

    /**
     * A flag like --platform=linux/amd64
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Flag implements Docker {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String name;

        @Nullable
        Argument value;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitFlag(this, p);
        }
    }

    /**
     * An argument which can be plain text, quoted string, or environment variable
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Argument implements Docker {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        List<ArgumentContent> contents;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitArgument(this, p);
        }
    }

    /**
     * Content within an argument (text, quoted string, or variable reference)
     */
    interface ArgumentContent extends Docker {
    }

    /**
     * Plain unquoted text
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class PlainText implements ArgumentContent {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String text;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitPlainText(this, p);
        }
    }

    /**
     * Quoted string (single or double quotes)
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class QuotedString implements ArgumentContent {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String value;
        QuoteStyle quoteStyle;

        public enum QuoteStyle {
            DOUBLE, SINGLE
        }

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitQuotedString(this, p);
        }
    }

    /**
     * Environment variable reference like $VAR or ${VAR}
     */
    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class EnvironmentVariable implements ArgumentContent {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        String name;
        boolean braced;

        @Override
        public <P> Docker acceptDocker(DockerVisitor<P> v, P p) {
            return v.visitEnvironmentVariable(this, p);
        }
    }
}
