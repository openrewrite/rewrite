/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://docs.moderne.io/licensing/moderne-source-available-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test

import (
	"fmt"
	"strings"
)

// scannerEdit is a source file the scanner of a ScanningRecipe changed or
// deleted.
type scannerEdit struct {
	recipe, sourcePath string
	deleted            bool
}

func (c *runChecks) scannerEdited(edit scannerEdit) {
	for _, known := range c.scannerEdits {
		if known.recipe == edit.recipe && known.sourcePath == edit.sourcePath {
			return
		}
	}
	c.scannerEdits = append(c.scannerEdits, edit)
}

func scannerEditsFailure(edits []scannerEdit) string {
	var found strings.Builder
	for _, e := range edits {
		verb := "changed"
		if e.deleted {
			verb = "deleted"
		}
		fmt.Fprintf(&found, "  %s's scanner %s %s\n", e.recipe, verb, e.sourcePath)
	}
	return "Record what the scanner finds in the accumulator, and make the edits in EditorWithData(acc), " +
		"since the scanning phase discards them:\n" + found.String() +
		"A scanner that reuses an editing visitor opts out with spec.CheckNoEditsInScanningPhase = false."
}
