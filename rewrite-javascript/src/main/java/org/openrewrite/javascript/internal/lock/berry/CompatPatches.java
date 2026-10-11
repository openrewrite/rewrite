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
package org.openrewrite.javascript.internal.lock.berry;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Port of @yarnpkg/plugin-compat sources/index.ts {@code getBuiltinPatch} and {@code reduceDependency} (Yarn
 * 4.10.3). Each release's patch texts come from {@code compat-patches.json.gz}.
 */
final class CompatPatches {

    private static final String RESOURCE = "compat-patches.json.gz";
    private static final String TAG = "compat/";

    private static volatile @Nullable Map<String, Object> data;

    private CompatPatches() {
    }

    /**
     * The patch text of {@code compat/<ident>}, null for any other name.
     */
    @SuppressWarnings("unchecked")
    static @Nullable String getBuiltinPatch(String name, YarnRelease release) {
        if (!name.startsWith(TAG)) {
            return null;
        }
        Ident ident = StructUtils.parseIdent(name.substring(TAG.length()));
        for (String patch : release.getPatches()) {
            if (StructUtils.makeIdent(null, patch).identHash.equals(ident.identHash)) {
                Map<String, Object> all = data();
                Object sections = Js.get(Js.get(all.get("releases"), release.getVersion()), patch);
                if (!(sections instanceof List)) {
                    throw new IllegalStateException(RESOURCE + " has no " + patch + " patch for Yarn " +
                                                    release.getVersion());
                }
                StringBuilder text = new StringBuilder();
                for (Object id : (List<Object>) sections) {
                    text.append((String) Js.get(all.get("sections"), (String) id));
                }
                return text.toString();
            }
        }
        return null;
    }

    static Descriptor reduceDependency(Descriptor dependency, YarnRelease release) {
        for (String name : release.getPatches()) {
            if (StructUtils.makeIdent(null, name).identHash.equals(dependency.identHash)) {
                return StructUtils.makeDescriptor(dependency, StructUtils.makeRange("patch:",
                        StructUtils.stringifyDescriptor(dependency),
                        "optional!builtin<compat/" + StructUtils.stringifyIdent(dependency) + ">", null));
            }
        }
        return dependency;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> data() {
        Map<String, Object> loaded = data;
        if (loaded == null) {
            InputStream resource = CompatPatches.class.getResourceAsStream(RESOURCE);
            if (resource == null) {
                throw new IllegalStateException(RESOURCE + " is missing");
            }
            try (InputStream in = new GZIPInputStream(resource)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[65536];
                int n;
                while ((n = in.read(buffer)) > 0) {
                    out.write(buffer, 0, n);
                }
                loaded = (Map<String, Object>) Js.jsonParse(new String(out.toByteArray(), StandardCharsets.UTF_8));
                data = loaded;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return loaded;
    }
}
