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
	"math/big"
	"reflect"
	"regexp"
	"strconv"
	"strings"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

const maxShapeProblemsPerFile = 5

var whitespaceRun = regexp.MustCompile(`\s+`)

// treeShapesFailure describes the nodes of the recipe's output that the parser
// makes differently of the text they print, or "" when there are none. When
// some of the output does not parse, it reports only that, since the fresh
// parse of the others may then lack the types it otherwise sees.
func treeShapesFailure(output *outputParse, checks *runChecks) string {
	var changed []int
	unparsed := false
	for k := range output.finals {
		if strings.TrimSpace(output.printed[k]) == "" ||
			output.befores[k] != nil && printTree(output.befores[k], printer.SanitizedMarkerPrinter) == output.printed[k] {
			continue
		}
		changed = append(changed, k)
		unparsed = unparsed || output.errs[k] != nil
	}
	var found strings.Builder
	for _, k := range changed {
		var problems []string
		switch {
		case output.errs[k] != nil:
			problems = []string{"the text it prints doesn't parse: " + firstLine(output.errs[k].Error())}
		case !unparsed && output.trees[k] != nil:
			problems = shapeProblems(output.finals[k], output.trees[k])
		}
		if len(problems) == 0 {
			continue
		}
		fmt.Fprintf(&found, "  %s%s:\n", sourcePathOf(output.finals[k]), checks.changedByOf(output.finals[k]))
		for _, p := range problems {
			fmt.Fprintf(&found, "    %s\n", p)
		}
	}
	if found.Len() == 0 {
		return ""
	}
	return "Write code with a template from pkg/template that names what its code uses through Imports, Context " +
		"or ExportData, or with the parser, or build the nodes the way the parser builds the text they print. " +
		"The recipe's output holds nodes that the parser makes differently of the same text:\n" + found.String() +
		"A recipe that runs after it matches the tree, not the text, so it misses or mistakes a node that " +
		"parsed code never holds. The parser builds a call such as `time.Duration(x)` as a java.TypeCast only " +
		"when it can resolve the name to a type. If the output is meant to differ from parsed code, set " +
		"spec.CheckParserShapedTrees = false."
}

// shapeNode is a node of a tree, with the nodes it holds as GoVisitor visits
// them and the cursor of its parent, to print it with.
type shapeNode struct {
	tree     java.Tree
	parent   *visitor.Cursor
	children []*shapeNode
}

func shapeProblems(final, parsed java.Tree) (problems []string) {
	node, parsedNode := skeleton(final), skeleton(parsed)
	if node == nil || parsedNode == nil {
		return nil
	}
	compareShapes(node, parsedNode, &problems)
	return problems
}

func compareShapes(node, parsed *shapeNode, problems *[]string) {
	if len(*problems) >= maxShapeProblemsPerFile {
		return
	}
	if reflect.TypeOf(node.tree) != reflect.TypeOf(parsed.tree) {
		if unresolvedConversion(node, parsed) {
			// The last children are the operand and the argument.
			compareShapes(node.children[len(node.children)-1], parsed.children[len(parsed.children)-1], problems)
			return
		}
		*problems = append(*problems, fmt.Sprintf("%s is a %s where the parser makes a %s",
			node, typeName(reflect.TypeOf(node.tree)), typeName(reflect.TypeOf(parsed.tree))))
		return
	}
	if key, description := shapeValue(node.tree); key != "" {
		if parsedKey, parsedDescription := shapeValue(parsed.tree); key != parsedKey {
			*problems = append(*problems, fmt.Sprintf("%s has %s where the parser makes %s",
				node, description, parsedDescription))
		}
	}
	if len(node.children) != len(parsed.children) {
		*problems = append(*problems, fmt.Sprintf("%s holds %s where the parser makes %s",
			node, shapeKinds(node.children), shapeKinds(parsed.children)))
		return
	}
	for i := range node.children {
		compareShapes(node.children[i], parsed.children[i], problems)
	}
}

// unresolvedConversion reports whether node is a conversion where parsed is a
// call with one argument and no method type. The parser makes such a call of a
// conversion to a type it cannot resolve, as of a package outside the standard
// library and the test's sources.
func unresolvedConversion(node, parsed *shapeNode) bool {
	_, cast := node.tree.(*java.TypeCast)
	call, ok := parsed.tree.(*java.MethodInvocation)
	if !cast || !ok || call.MethodType != nil || len(call.Arguments.Elements) != 1 ||
		len(node.children) == 0 || len(parsed.children) == 0 {
		return false
	}
	_, empty := call.Arguments.Elements[0].Element.(*java.Empty)
	return !empty
}

// shapeValue is what tells a node apart from another of its type with the same
// children, as a key to compare and a description, or "" when nothing does.
func shapeValue(t java.Tree) (key, description string) {
	operator := func(op fmt.Stringer, value int) (string, string) {
		return strconv.Itoa(value), "the operator " + op.String()
	}
	switch n := t.(type) {
	case *java.Identifier:
		return "name " + n.Name, "the name `" + n.Name + "`"
	case *java.Literal:
		description = literalValue(n.Value)
		return description, description
	case *java.Binary:
		return operator(n.Operator.Element, int(n.Operator.Element))
	case *golang.Binary:
		return operator(n.Operator.Element, int(n.Operator.Element))
	case *java.Unary:
		return operator(n.Operator.Element, int(n.Operator.Element))
	case *golang.Unary:
		return operator(n.Operator.Element, int(n.Operator.Element))
	case *java.AssignmentOperation:
		return operator(n.Operator.Element, int(n.Operator.Element))
	case *golang.AssignmentOperation:
		return operator(n.Operator.Element, int(n.Operator.Element))
	}
	return "", ""
}

func literalValue(v any) string {
	var printed string
	switch x := v.(type) {
	case nil:
		return "the value nil"
	case string:
		if r := []rune(x); len(r) > 40 {
			x = string(r[:40]) + "..."
		}
		printed = strconv.Quote(x)
	case int64:
		printed = strconv.FormatInt(x, 10)
	case int:
		printed = strconv.Itoa(x)
	case int32:
		printed = strconv.FormatInt(int64(x), 10)
	case float64:
		printed = strconv.FormatFloat(x, 'g', -1, 64)
	case bool:
		printed = strconv.FormatBool(x)
	case *big.Int:
		printed = x.String()
	default:
		return "a " + typeName(reflect.TypeOf(v)) + " value"
	}
	return "the " + typeName(reflect.TypeOf(v)) + " value " + printed
}

func shapeKinds(nodes []*shapeNode) string {
	if len(nodes) == 0 {
		return "nothing"
	}
	kinds := make([]string, len(nodes))
	for i, n := range nodes {
		kinds[i] = typeName(reflect.TypeOf(n.tree))
	}
	return strings.Join(kinds, ", ")
}

func (n *shapeNode) String() (s string) {
	// A node the printer cannot print on its own just goes without its text.
	defer func() {
		if recover() != nil {
			s = "`?`"
		}
	}()
	printed := strings.TrimSpace(whitespaceRun.ReplaceAllString(
		printer.PrintWithCursor(n.tree, n.parent, printer.SanitizedMarkerPrinter), " "))
	if r := []rune(printed); len(r) > 60 {
		printed = string(r[:60]) + "..."
	}
	return "`" + printed + "`"
}

// skeleton is the shape of t, leaving out empty elements, which the parser adds
// where recipes often leave none, or nil when t cannot be visited.
func skeleton(t java.Tree) (root *shapeNode) {
	defer func() {
		if recover() != nil {
			root = nil
		}
	}()
	b := visitor.Init(&skeletonBuilder{})
	b.Visit(t, nil)
	return b.root
}

type skeletonBuilder struct {
	visitor.GoVisitor
	root *shapeNode
	// path holds the node open at each depth, the root at depth 1.
	path []*shapeNode
	// skipBelow is the depth of the empty element being left out, or 0.
	skipBelow int
}

func (b *skeletonBuilder) PreVisit(t java.Tree, _ any) java.Tree {
	depth := 0
	for c := b.Cursor(); c != nil; c = c.Parent() {
		depth++
	}
	if b.skipBelow > 0 && depth > b.skipBelow {
		return t
	}
	b.skipBelow = 0
	if _, empty := t.(*java.Empty); empty {
		b.skipBelow = depth
		return t
	}
	if len(b.path) >= depth {
		b.path = b.path[:depth-1]
	}
	n := &shapeNode{tree: t, parent: b.Cursor().Parent()}
	if len(b.path) == 0 {
		if b.root == nil {
			b.root = n
		}
	} else {
		parent := b.path[len(b.path)-1]
		parent.children = append(parent.children, n)
	}
	b.path = append(b.path, n)
	return t
}
