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
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.PosixPath;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Port of @yarnpkg/plugin-patch sources/PatchFetcher.ts (Yarn 4.10.3), with the {@code copyPromise} of
 * @yarnpkg/fslib sources/algorithms/copyPromise.ts that makes the package's copy.
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
            ZipArchive patched = patchPackage(locator, opts);
            return () -> Cache.checksum(patched, locator, project.configuration);
        }, project);
    }

    private static ZipArchive patchPackage(Locator locator, FetchOptions opts) {
        PatchUtils.Spec<Locator> spec = PatchUtils.parseLocator(locator);
        List<PatchUtils.PatchFile> patchFiles = PatchUtils.loadPatchFiles(spec.getParentLocator(),
                spec.getPatchPaths(), opts.getProject());
        ZipArchive sourceFs = opts.getFetcher().fetchPackageFs(spec.getSourceItem(), opts);
        return patchPackage(locator, sourceFs, patchFiles, spec.getSourceVersion(), opts.getProject().configuration);
    }

    /**
     * {@code patchPackage} once the patch files are loaded and the source fetched.
     */
    static ZipArchive patchPackage(Locator locator, ZipArchive sourceFs, List<PatchUtils.PatchFile> patchFiles,
                                   @Nullable String sourceVersion, Configuration configuration) {
        String prefixPath = StructUtils.getIdentVendorPath(locator);
        ZipArchive current = new ZipArchive();
        copyPromise(current, prefixPath, sourceFs, prefixPath);

        for (PatchUtils.PatchFile patchFile : patchFiles) {
            if (patchFile.getSource() == null) {
                continue;
            }
            // each patch file applies to a copy, so a failed optional one leaves the package as the last one did
            ZipArchive patchedPackage = current.copy();
            try {
                PatchApply.applyPatchFile(PatchParse.parsePatchFile(patchFile.getSource()), patchedPackage,
                        prefixPath, sourceVersion);
            } catch (PatchApply.UnmatchedHunkError e) {
                if (patchFile.isOptional()) {
                    continue;
                }
                boolean enableInlineHunks = Boolean.TRUE.equals(configuration.get("enableInlineHunks"));
                throw new ReportError(ReportError.PATCH_HUNK_FAILED, StructUtils.stringifyLocator(locator) + ": " +
                                                                     e.getMessage() + (enableInlineHunks ? "" :
                        " (set enableInlineHunks for details)"));
            }
            current = patchedPackage;
        }
        return current;
    }

    /**
     * {@code copyPromise} with {@code stableSort}: the destination's parent made, then the source's entries in
     * depth-first order, each directory's names sorted, every entry given the source's permission bits.
     */
    private static void copyPromise(ZipArchive destinationFs, String destination, ZipArchive sourceFs, String source) {
        destinationFs.mkdirp(PosixPath.dirname(destination));
        List<Runnable> postlayout = new ArrayList<>();
        copyImpl(postlayout, destinationFs, destination, sourceFs, source);
        for (Runnable operation : postlayout) {
            operation.run();
        }
    }

    private static void copyImpl(List<Runnable> postlayout, ZipArchive destinationFs, String destination,
                                 ZipArchive sourceFs, String source) {
        Integer sourceMode = sourceFs.lstatMode(source);
        if (sourceMode == null) {
            throw new JsError(JsError.ERROR, "ENOENT: no such file or directory, lstat '/" + source + "'", "ENOENT");
        }
        if (ZipArchive.isDirectory(sourceMode)) {
            destinationFs.mkdir(destination, sourceMode);
            List<String> entries = sourceFs.readdir(source);
            Collections.sort(entries);
            for (String entry : entries) {
                copyImpl(postlayout, destinationFs, destination + "/" + entry, sourceFs, source + "/" + entry);
            }
        } else if (ZipArchive.isSymbolicLink(sourceMode)) {
            destinationFs.symlink(new String(sourceFs.readFile(source), StandardCharsets.UTF_8),
                    destination);
        } else {
            destinationFs.writeFile(destination, sourceFs.readFile(source), sourceMode);
        }
        int mode = sourceMode & 0777;
        postlayout.add(() -> destinationFs.chmod(destination, mode));
    }
}
