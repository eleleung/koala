package mfwgenerics.kotq.query.built

import mfwgenerics.kotq.query.Cte
import mfwgenerics.kotq.query.LabelList
import mfwgenerics.kotq.query.WithType
import mfwgenerics.kotq.sql.Scope

class BuiltInsert: BuiltStatement {
    lateinit var relation: BuiltRelation

    var withType: WithType = WithType.NOT_RECURSIVE
    var withs: List<BuiltWith> = emptyList()

    lateinit var query: BuiltSubquery

    override fun populateScope(scope: Scope) {

    }
}