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
package org.openrewrite.javascript.internal.lock;

import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;

/**
 * Yarn Berry ({@code yarn.lock} carrying {@code __metadata:}) records a non-derivable {@code checksum} for
 * every registry dependency (yarn's normalized-zip hash, not {@code dist.integrity}) that this engine cannot
 * reproduce, so the patcher fails loud with {@link Reason#CHECKSUM_UNAVAILABLE} rather than emit a lock a
 * real {@code yarn install} would reject.
 */
public final class YarnBerryLockPatcher implements LockPatcher {

    @Override
    public String patch(LockEditSet edits) {
        String pkg = edits.getEdits().isEmpty() ? null : edits.getEdits().get(0).getName();
        for (PackageEdit edit : edits.getEdits()) {
            if (edit.getNewVersion() != null) {
                pkg = edit.getName();
                break;
            }
        }
        throw new EngineFailure(Reason.CHECKSUM_UNAVAILABLE, pkg,
                "yarn berry checksum not reproducible in Phase A");
    }
}
