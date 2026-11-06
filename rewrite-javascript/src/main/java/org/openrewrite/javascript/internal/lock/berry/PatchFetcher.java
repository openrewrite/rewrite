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
import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.List;

/**
 * Port of @yarnpkg/plugin-patch sources/PatchFetcher.ts (Yarn 4.10.3).
 */
final class PatchFetcher implements Fetcher {

    @Override
    public boolean supports(Locator locator, FetchOptions opts) {
        return PatchUtils.isPatchLocator(locator);
    }

    @Override
    public @Nullable String fetch(Locator locator, FetchOptions opts) {
        Project project = opts.getProject();
        return Cache.fetchPackageFromCache(locator, () -> {
            ZipArchive archive = archive(locator, opts);
            return () -> Cache.getCacheKey(project.configuration) + "/" + HashUtils.sha512Hex(
                    archive.toByteArray(project.configuration.get("compressionLevel")));
        }, project);
    }

    private static ZipArchive archive(Locator locator, FetchOptions opts) {
        PatchUtils.Spec<Locator> spec = PatchUtils.parseLocator(locator);
        Locator source = spec.getSourceItem();
        ZipArchive archive;
        if (PatchUtils.isPatchLocator(source)) {
            archive = archive(source, opts).stableCopy();
        } else if (new NpmSemverFetcher().supports(source, opts)) {
            archive = TgzUtils.convertToZip(NpmSemverFetcher.fetchFromNetwork(source, opts),
                    StructUtils.getIdentVendorPath(locator), 1).stableCopy();
        } else if (new NpmHttpFetcher().supports(source, opts)) {
            StructUtils.Range range = StructUtils.parseRange(source.reference);
            String url = (String) range.getParams().get("__archiveUrl");
            byte[] tgz = NpmHttpUtils.get(url, source, range.getSelector(), opts.getProject(), opts.getRegistry());
            archive = TgzUtils.convertToZip(tgz, StructUtils.getIdentVendorPath(locator), 1).stableCopy();
        } else {
            throw new CannotKnow("the source archive of patched " + StructUtils.stringifyLocator(source));
        }
        List<@Nullable String> patches = PatchUtils.loadPatchFiles(spec.getParentLocator(),
                spec.getPatchPaths(), opts.getProject());
        String prefix = StructUtils.getIdentVendorPath(locator) + "/";
        for (int n = 0; n < patches.size(); n++) {
            String text = patches.get(n);
            if (text == null) continue;
            ZipArchive attempt = archive.copy();
            try {
                PatchApply.apply(PatchParse.parsePatchFile(text), attempt, prefix, spec.getSourceVersion());
                archive = attempt;
            } catch (PatchApply.UnmatchedHunk e) {
                if (!spec.getPatchPaths().get(n).startsWith("optional!")) {
                    throw new ReportError(66, StructUtils.stringifyLocator(locator) + ": " + e.getMessage());
                }
                // Yarn discards the entire optional patch, including effects preceding the failed hunk.
            }
        }
        return archive;
    }
}
