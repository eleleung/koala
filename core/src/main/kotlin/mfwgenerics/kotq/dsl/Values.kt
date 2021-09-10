package mfwgenerics.kotq.dsl

import mfwgenerics.kotq.LiteralAssignment
import mfwgenerics.kotq.expr.Reference
import mfwgenerics.kotq.query.LabelList
import mfwgenerics.kotq.query.Values
import mfwgenerics.kotq.values.*

inline fun <T> values(
    source: Sequence<T>,
    references: List<Reference<*>>,
    crossinline writer: RowWriter.(T) -> Unit
): Values {
    val columns = LabelList(references)

    return Values(columns) {
        var row = PreLabeledRow(columns)

        val iter = source.iterator()

        object : RowIterator, ValuesRow by row {
            override fun next(): Boolean {
                if (!iter.hasNext()) return false

                row.clear()
                row.writer(iter.next())

                return true
            }

            override fun consume(): ValuesRow {
                val result = row
                row = PreLabeledRow(columns)
                return result
            }

            override fun close() { }
        }
    }
}

inline fun <T> values(
    source: Sequence<T>,
    crossinline build: RowWriter.(T) -> Unit
): Values {
    val rows = arrayListOf<ValuesRow>()

    val columns = arrayListOf<Reference<*>>()
    val columnPositions = hashMapOf<Reference<*>, Int>()

    val writer = object : RowWriter {
        private var values = arrayListOf<Any?>()

        override fun <T : Any> set(reference: Reference<T>, value: T?) {
            val ix = columnPositions.putIfAbsent(reference, columnPositions.size)

            if (ix != null) {
                values[ix] = value
            } else {
                columns.add(reference)
                values.add(value)
            }
        }

        fun next() {
            rows.add(BuiltRow(
                columnPositions, /* it is deliberate that this continues to be mutated after BuiltRow is constructed */
                values
            ))

            val nextValues = ArrayList<Any?>(columnPositions.size)
            repeat(columnPositions.size) { nextValues.add(null) }

            values = nextValues
        }
    }

    source.forEach {
        writer.build(it)
        writer.next()
    }

    val labels = LabelList(
        columns,
        columnPositions
    )

    return Values(labels) {
        IteratorToRowIterator(labels, rows.iterator())
    }
}

inline fun <T> values(
    source: Iterable<T>,
    references: List<Reference<*>>,
    crossinline writer: RowWriter.(T) -> Unit
): Values = values(source.asSequence(), references) { writer(it) }

inline fun <T> values(
    source: Iterable<T>,
    crossinline writer: RowWriter.(T) -> Unit
): Values = values(source.asSequence()) { writer(it) }

fun values(
    rows: Iterable<ValuesRow>
): Values {
    val labelSet = hashSetOf<Reference<*>>()

    rows.forEach {
        labelSet.addAll(it.columns)
    }

    val columns = LabelList(labelSet.toList())

    return Values(columns) {
        IteratorToRowIterator(columns, rows.iterator())
    }
}

fun values(vararg rows: ValuesRow): Values = values(rows.asList())

fun rowOf(vararg assignments: LiteralAssignment<*>): ValuesRow {
    /* could be done more efficiently (?) by building labels and row values together */
    val row = PreLabeledRow(LabelList(assignments.map { it.reference }))

    assignments.forEach { it.placeIntoRow(row) }

    return row
}