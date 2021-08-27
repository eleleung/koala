package mfwgenerics.kotq.expr

import mfwgenerics.kotq.IdentifierName
import mfwgenerics.kotq.data.DataType
import mfwgenerics.kotq.expr.built.BuildsIntoAggregatedExpr
import mfwgenerics.kotq.expr.built.BuiltAggregatable
import mfwgenerics.kotq.query.Alias
import mfwgenerics.kotq.query.built.BuiltSubquery
import kotlin.reflect.KClass

sealed interface QuasiExpr

class SubqueryExpr(
    val subquery: BuiltSubquery
): QuasiExpr

class ExprListExpr<T : Any>(
    val exprs: Collection<Expr<T>>
): QuasiExpr

sealed interface ComparisonOperand<T : Any>: QuasiExpr

class ComparedQuery<T : Any>(
    val type: ComparedQueryType,
    val subquery: BuiltSubquery
): ComparisonOperand<T>

sealed interface Expr<T : Any>: ComparisonOperand<T>, Ordinal<T>, OrderableAggregatable<T> {
    override fun toOrderKey(): OrderKey<T> = OrderKey(SortOrder.ASC, this)

    fun asc() = OrderKey(SortOrder.ASC, this)
    fun desc() = OrderKey(SortOrder.DESC, this)

    override fun buildIntoAggregatable(into: BuiltAggregatable): BuildsIntoAggregatable? {
        into.expr = this
        return null
    }
}

sealed interface Reference<T : Any>: Expr<T>, SelectArgument {
    val type: KClass<T>

    val identifier: IdentifierName?

    override fun buildIntoSelection(selection: SelectionBuilder) {
        selection.expression(this, this)
    }
}

class AliasedReference<T : Any>(
    override val type: KClass<T>,
    val of: Alias,
    val reference: Reference<T>
): Reference<T>, SelectArgument {
    override val identifier: IdentifierName? get() = null

    override fun equals(other: Any?): Boolean =
        other is AliasedReference<*> &&
        of.identifier == other.of.identifier &&
        reference == other.reference

    override fun hashCode(): Int = of.identifier.hashCode() xor reference.hashCode()
    override fun toString(): String = "${of.identifier}.${reference}"
}

abstract class NamedReference<T : Any>(
    override val type: KClass<T>,
    override val identifier: IdentifierName
): Reference<T> {
    override fun equals(other: Any?): Boolean =
        other is NamedReference<*> && identifier == other.identifier

    override fun hashCode(): Int = identifier.hashCode()
    override fun toString(): String = "$identifier"
}

interface AggregatedExpr<T : Any>: Expr<T>, BuildsIntoAggregatedExpr

class CastExpr<T : Any>(
    val of: Expr<*>,
    val type: DataType<T>
): Expr<T>

class Literal<T : Any>(
    val type: KClass<T>,
    val value: T?
): Expr<T>

class OperationExpr<T : Any>(
    val type: OperationType,
    val args: Collection<QuasiExpr>
): Expr<T>