package org.jetbrains.spek.api

fun <T1, T2> table(body: (T1, T2) -> Unit): TableBuilder2<T1, T2> {
    return TableBuilder2(body)
}

fun <T1, T2, T3> table(body: (T1, T2, T3) -> Unit): TableBuilder3<T1, T2, T3> {
    return TableBuilder3(body)
}

fun <T1, T2, T3, T4> table(body: (T1, T2, T3, T4) -> Unit): TableBuilder4<T1, T2, T3, T4> {
    return TableBuilder4(body)
}

fun <T1, T2, T3, T4, T5> table(body: (T1, T2, T3, T4, T5) -> Unit): TableBuilder5<T1, T2, T3, T4, T5> {
    return TableBuilder5(body)
}

fun <T1, T2, T3, T4, T5, T6> table(body: (T1, T2, T3, T4, T5, T6) -> Unit): TableBuilder6<T1, T2, T3, T4, T5, T6> {
    return TableBuilder6(body)
}


class TableBuilder2<T1, T2>(private val tableBody: (T1, T2) -> Unit) {
    infix fun where(whereBody: RowBuilder.() -> Unit) {
        RowBuilder().whereBody()
    }

    inner class RowBuilder {
        fun row(col1: T1, col2: T2) = tableBody.invoke(col1, col2)
    }
}

class TableBuilder3<T1, T2, T3>(private val tableBody: (T1, T2, T3) -> Unit) {
    infix fun where(whereBody: RowBuilder.() -> Unit) {
        RowBuilder().whereBody()
    }

    inner class RowBuilder {
        fun row(col1: T1, col2: T2, col3: T3) {
            tableBody.invoke(col1, col2, col3)
        }
    }
}

class TableBuilder4<T1, T2, T3, T4>(private val tableBody: (T1, T2, T3, T4) -> Unit) {
    infix fun where(whereBody: RowBuilder.() -> Unit) {
        RowBuilder().whereBody()
    }

    inner class RowBuilder {
        fun row(col1: T1, col2: T2, col3: T3, col4: T4) {
            tableBody.invoke(col1, col2, col3, col4)
        }
    }
}

class TableBuilder5<T1, T2, T3, T4, T5>(private val tableBody: (T1, T2, T3, T4, T5) -> Unit) {
    infix fun where(whereBody: RowBuilder.() -> Unit) {
        RowBuilder().whereBody()
    }

    inner class RowBuilder {
        fun row(col1: T1, col2: T2, col3: T3, col4: T4, col5: T5) {
            tableBody.invoke(col1, col2, col3, col4, col5)
        }
    }
}

class TableBuilder6<T1, T2, T3, T4, T5, T6>(private val tableBody: (T1, T2, T3, T4, T5, T6) -> Unit) {
    infix fun where(whereBody: RowBuilder.() -> Unit) {
        RowBuilder().whereBody()
    }

    inner class RowBuilder {
        fun row(col1: T1, col2: T2, col3: T3, col4: T4, col5: T5, col6: T6) {
            tableBody.invoke(col1, col2, col3, col4, col5, col6)
        }
    }
}