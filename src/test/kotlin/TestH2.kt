import mfwgenerics.kotq.ColumnType
import mfwgenerics.kotq.Table
import mfwgenerics.kotq.dialect.h2.H2Dialect
import mfwgenerics.kotq.dsl.*
import mfwgenerics.kotq.expr.DataType
import mfwgenerics.kotq.expr.`as`
import mfwgenerics.kotq.jdbc.ConnectionWithDialect
import mfwgenerics.kotq.jdbc.performWith
import java.sql.DriverManager
import kotlin.test.Test

class TestH2 {
    object TestTable : Table("Test") {
        val column1 = column("test0", ColumnType.INT)
        val column2 = column("test1", ColumnType.INT)
    }

    @Test
    fun `triangular numbers from values clause subquery`() {
        val cxn = ConnectionWithDialect(
            H2Dialect(),
            DriverManager.getConnection("jdbc:h2:mem:test")
        )

        cxn
            .jdbc
            .prepareStatement("""
                CREATE TABLE Test(
                    test0 INT NOT NULL,
                    test1 INT NOT NULL
                )
            """.trimIndent())
            .execute()

        val insert = TestTable
            .insert(values(
                listOf(1,2,3,4,7,7,10).asSequence(),
                TestTable.column1,
                TestTable.column2
            ) {
                value(TestTable.column1, it)
                value(TestTable.column2, it * 10)
            })

        val compiledInsert = H2Dialect()
            .compile(insert.buildInsert())

        val preparedInsert = cxn.jdbc.prepareStatement(compiledInsert.sql)

        compiledInsert.parameters.forEachIndexed { ix, it ->
            preparedInsert.setObject(ix + 1, it)
        }

        preparedInsert.execute()

        val number = name<Int>("number")
        val summed = name<Int>("sumUnder")

        /* need this cast to workaround H2 bug (? in VALUES aren't typed correctly) */
        val castNumber = cast(number, DataType.INTEGER)

        val alias = alias("A")

        val results = values((1..20).asSequence(), number)
            { value(number, it) }
            .subquery()
            .orderBy(castNumber.desc())
            .select(
                number,
                sum(castNumber)
                    .over(all()
                    .orderBy(castNumber)
                ) `as` summed
            )
            .subquery()
            .alias(alias)
            .where(alias[summed] greater 9)
            .select(alias[number], alias[summed])
            .performWith(cxn)
            .map { row ->
                "${row[alias[number]]}, ${row[alias[summed]]}"
            }
            .joinToString("\n")

        val expected = (1..20)
            .scan(0) { x, y -> x + y }
            .filter { it > 9 }
            .withIndex()
            .reversed()
            .asSequence()
            .map {
                "${it.index + 4}, ${it.value}"
            }
            .joinToString("\n")

        assert(expected == results)

        cxn.jdbc.close()
    }
}