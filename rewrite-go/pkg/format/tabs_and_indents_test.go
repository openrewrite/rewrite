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

package format

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
)

func TestTabsAndIndentsVisitorIndentsALaterImportDeclaration(t *testing.T) {
	// given a second import declaration written indented
	cu, err := parser.NewGoParser().Parse("t.go", "package p\n\nimport \"fmt\"\n\n  import \"os\"\n\nvar _ = fmt.Sprint\nvar _ = os.Args\n")
	require.NoError(t, err)

	// when
	out := NewTabsAndIndentsVisitor(nil).Visit(cu, nil)

	// then
	assert.Equal(t, "package p\n\nimport \"fmt\"\n\nimport \"os\"\n\nvar _ = fmt.Sprint\nvar _ = os.Args\n", printer.Print(out))
}
