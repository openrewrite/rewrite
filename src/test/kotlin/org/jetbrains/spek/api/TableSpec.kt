package org.jetbrains.spek.api;

import org.jetbrains.spek.api.Spek

class TableSpec : Spek({
    describe("using a table") {
        table { a: String, b: Int ->
            describe("$a and $b are parameters") {
                it("should work") {
                    assert(a is String)
                    assert(b is Int)
                }
            }
        } where {
            row("foo", 1)
            row("bar", 2)
        }
    }

    table { a: Int, b: Int, c: Int ->
        it("adds up $a and $b to equal $c") {
            assert(a + b == c)
        }
    } where {
        row(1, 2, 3)
        row(4, 6, 10)
    }
})