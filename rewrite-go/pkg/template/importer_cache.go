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

package template

import (
	"go/types"
	"io/fs"
	"sync"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/exportdata"
)

// importerCache holds the one importer a template or pattern parses with.
// Decoding a package's export data costs far more than the parse it feeds, and
// an importer keeps what it has decoded.
type importerCache struct {
	exportData []fs.FS

	importerOnce sync.Once
	importer     types.Importer
}

func (c *importerCache) shared() types.Importer {
	c.importerOnce.Do(func() {
		c.importer = exportdata.Importer(c.exportData...)
	})
	return c.importer
}
