/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.polyglot;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.apache.commons.io.IOUtils;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.ByteSequence;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.graalvm.polyglot.Value.asValue;

public class PolyglotParser implements Parser<Polyglot.Source> {

    private static final String JS_UTILS = StringUtils.readFully(PolyglotParser.class.getResourceAsStream("/META-INF/rewrite/polyglot.js"));

    private static final ThreadLocal<Engine> ENGINES = new InheritableThreadLocal<Engine>() {
        @Override
        protected Engine initialValue() {
            return Engine.newBuilder()
                    .allowExperimentalOptions(true)
                    .build();
        }
    };

    private final HostAccess hostAccess;

    public PolyglotParser() {
        HostAccess.Builder b = HostAccess.newBuilder()
                .allowPublicAccess(true)
                .allowAllImplementations(true).allowAllClassImplementations(true)
                .allowArrayAccess(true).allowListAccess(true).allowBufferAccess(true)
                .allowIterableAccess(true).allowIteratorAccess(true)
                .allowMapAccess(true);

        ScanResult classpath = new ClassGraph()
                .enableAllInfo()
                .scan();
        for (ClassInfo ci : classpath.getClassesImplementing(PolyglotMapping.class)) {
            ci.getConstructorInfo().stream()
                    .findFirst()
                    .map(mi -> {
                        try {
                            //noinspection unchecked
                            return (PolyglotMapping<Object, Object>) mi.loadClassAndGetConstructor().newInstance();
                        } catch (Throwable t) {
                            throw new IllegalStateException(t);
                        }
                    })
                    .ifPresent(pm -> b.targetTypeMapping(pm.inputType(), pm.outputType(), pm, pm));
        }

        this.hostAccess = b.build();
    }

    private final ThreadLocal<Context> context = new InheritableThreadLocal<Context>() {
        @Override
        protected Context initialValue() {
            return Context.newBuilder()
                    .engine(ENGINES.get())
                    .allowHostAccess(hostAccess)
                    .allowAllAccess(true)
                    .allowExperimentalOptions(true)
                    .build();
        }
    };

    public List<Polyglot.Source> parse(ExecutionContext ex, Source... sources) {
        ex.putMessage("POLYGLOT_CONTEXT", context.get());

        return Stream.of(sources).flatMap(src -> {
                    Path srcPath = Paths.get(src.getPath() == null ? src.getName() + "." + src.getLanguage() : src.getPath());
                    InputStream is = new ByteArrayInputStream(src.hasBytes() ? src.getBytes().toByteArray() : src.getCharacters().toString().getBytes(UTF_8));
                    Input in = new Input(srcPath, () -> is, true);
                    return parseInputs(singletonList(in), null, new InMemoryExecutionContext()).stream();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Polyglot.Source> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ex) {
        return StreamSupport.stream(sources.spliterator(), false)
                .map(in -> {
                    Context ctx = context.get();
                    String language = PolyglotUtils.getLanguage(in.getPath().toString());
                    try {
                        Source.Builder src = Source.newBuilder(language, in.getPath().toUri().toURL());
                        if ("js".equals(language)) {
                            src.content(IOUtils.toString(in.getSource()));
                        } else {
                            src.content(ByteSequence.create(IOUtils.toByteArray(in.getSource())));
                        }
                        ctx.eval(src.build());
                        Value bindings = ctx.getBindings(language);
                        bindings.putMember("Polyglot", new PolyglotHelper());
                        ctx.getPolyglotBindings().putMember("sourceUri", in.getPath().toUri());
                        return new Polyglot.Source(Tree.randomId(), Markers.EMPTY, in.getPath(), bindings);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean accept(Path path) {
        return path.endsWith(".js");
    }

    private static class PolyglotHelper {
        public Value randomId() {
            return asValue(Tree.randomId());
        }
    }
}
