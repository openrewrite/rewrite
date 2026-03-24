/*
 * Copyright 2025 the original author or authors.
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
	"testing"

	. "github.com/openrewrite/rewrite/pkg/test"
)

func TestParseRealisticServer(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import (
				"fmt"
				"net/http"
			)

			// Server represents an HTTP server.
			type Server struct {
				Host string
				Port int
			}

			// NewServer creates a new server.
			func NewServer(host string, port int) *Server {
				return &Server{Host: host, Port: port}
			}

			// ListenAddr returns the listen address.
			func (s *Server) ListenAddr() string {
				return fmt.Sprintf("%s:%d", s.Host, s.Port)
			}

			// Start starts the server.
			func (s *Server) Start() error {
				return http.ListenAndServe(s.ListenAddr(), nil)
			}
		`))
}

func TestParseRealisticErrorHandling(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			import "fmt"

			func process(items []string) error {
				for i, item := range items {
					if item == "" {
						return fmt.Errorf("empty item at index %d", i)
					}
				}
				return nil
			}
		`))
}

func TestParseRealisticChannelPipeline(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func generate(nums ...int) <-chan int {
				out := make(chan int)
				go func() {
					for _, n := range nums {
						out <- n
					}
					close(out)
				}()
				return out
			}
		`))
}

func TestParseRealisticTableTest(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func runTests() {
				tests := []struct {
					name string
					in   int
					want int
				}{
					{"zero", 0, 0},
					{"one", 1, 1},
					{"two", 2, 4},
				}
				for _, tt := range tests {
					use(tt.name, tt.in, tt.want)
				}
			}
		`))
}

func TestParseRealisticDeferCleanup(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func withResource() error {
				r, err := acquire()
				if err != nil {
					return err
				}
				defer r.Close()

				data, err := r.Read()
				if err != nil {
					return err
				}
				return process(data)
			}
		`))
}

func TestParseRealisticWorkerPool(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func worker(id int, jobs <-chan int, results chan<- int) {
				for j := range jobs {
					result := j * 2
					results <- result
				}
			}
		`))
}

func TestParseRealisticOptionPattern(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Option func(*Config)

			type Config struct {
				timeout int
				retries int
			}

			func WithTimeout(t int) Option {
				return func(c *Config) {
					c.timeout = t
				}
			}
		`))
}

func TestParseRealisticSwitchOnString(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func parseCommand(cmd string) {
				switch cmd {
				case "start":
					start()
				case "stop":
					stop()
				case "restart":
					stop()
					start()
				default:
					unknown(cmd)
				}
			}
		`))
}

func TestParseRealisticInterfaceImplementation(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Reader interface {
				Read(p []byte) (n int, err error)
			}

			type Writer interface {
				Write(p []byte) (n int, err error)
			}

			type ReadWriter interface {
				Reader
				Writer
			}
		`))
}

func TestParseRealisticSelectTimeout(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func waitWithTimeout(ch chan int) (int, bool) {
				select {
				case v := <-ch:
					return v, true
				case <-timeout():
					return 0, false
				}
			}
		`))
}

func TestParseMapOfSlices(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() {
				m := map[string][]int{
					"a": {1, 2, 3},
					"b": {4, 5, 6},
				}
				use(m)
			}
		`))
}

func TestParseNestedCompositeLiteral(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			type Inner struct {
				Value int
			}

			type Outer struct {
				Items []Inner
			}

			func f() {
				o := Outer{
					Items: []Inner{
						{Value: 1},
						{Value: 2},
					},
				}
				use(o)
			}
		`))
}

func TestParseMultiReturnAssign(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func divide(a, b int) (int, error) {
				if b == 0 {
					return 0, errDivByZero
				}
				return a / b, nil
			}
		`))
}

func TestParseSliceOfMaps(t *testing.T) {
	NewRecipeSpec().RewriteRun(t,
		Golang(`
			package main

			func f() []map[string]int {
				return []map[string]int{
					{"a": 1},
					{"b": 2},
				}
			}
		`))
}
