package mfwgenerics.kotq.dsl

import mfwgenerics.kotq.data.DataType
import mfwgenerics.kotq.expr.*
import mfwgenerics.kotq.query.Subqueryable
import mfwgenerics.kotq.query.fluent.SelectedJust

infix fun <T : Any> Expr<T>.`as`(reference: Reference<T>): SelectedExpr<T> =
    SelectedExpr(this, reference)

inline infix fun <reified T : Any> T.`as`(reference: Reference<T>): SelectedExpr<T> =
    SelectedExpr(literal(this), reference)

infix fun <T : Any> Expr<T>.eq(rhs: ComparisonOperand<T>): Expr<Boolean> = OperationType.EQ(this, rhs)
inline infix fun <reified T : Any> Expr<T>.eq(rhs: T): Expr<Boolean> = eq(literal(rhs))

infix fun <T : Any> Expr<T>.neq(rhs: ComparisonOperand<T>): Expr<Boolean> = OperationType.NEQ(this, rhs)
inline infix fun <reified T : Any> Expr<T>.neq(rhs: T): Expr<Boolean> = neq(literal(rhs))

infix fun <T : Any> Expr<T>.less(rhs: ComparisonOperand<T>): Expr<Boolean> = OperationType.LT(this, rhs)
inline infix fun <reified T : Any> Expr<T>.less(rhs: T): Expr<Boolean> = less(literal(rhs))

infix fun <T : Any> Expr<T>.lessOrEq(rhs: ComparisonOperand<T>): Expr<Boolean> = OperationType.LTE(this, rhs)
inline infix fun <reified T : Any> Expr<T>.lessOrEq(rhs: T): Expr<Boolean> = lessOrEq(literal(rhs))

infix fun <T : Any> Expr<T>.greater(rhs: ComparisonOperand<T>): Expr<Boolean> = OperationType.GT(this, rhs)
inline infix fun <reified T : Any> Expr<T>.greater(rhs: T): Expr<Boolean> = greater(literal(rhs))

infix fun <T : Any> Expr<T>.greaterOrEq(rhs: ComparisonOperand<T>): Expr<Boolean> = OperationType.GTE(this, rhs)
inline infix fun <reified T : Any> Expr<T>.greaterOrEq(rhs: T): Expr<Boolean> = greaterOrEq(literal(rhs))

infix fun Expr<Boolean>.and(rhs: Expr<Boolean>): Expr<Boolean> = OperationType.AND(this, rhs)
infix fun Expr<Boolean>.or(rhs: Expr<Boolean>): Expr<Boolean> = OperationType.OR(this, rhs)

fun not(expr: Expr<Boolean>): Expr<Boolean> = OperationType.NOT(expr)

fun <T : Any> Expr<T>.isNull(): Expr<Boolean> = OperationType.IS_NULL(this)
fun <T : Any> Expr<T>.isNotNull(): Expr<Boolean> = OperationType.IS_NOT_NULL(this)

fun exists(query: Subqueryable): Expr<Boolean> = OperationType.EXISTS(SubqueryExpr(query.buildQuery()))
fun notExists(query: Subqueryable): Expr<Boolean> = OperationType.NOT_EXISTS(SubqueryExpr(query.buildQuery()))

infix fun <T : Any> Expr<T>.inQuery(query: SelectedJust<T>): Expr<Boolean> =
    OperationType.IN(this, SubqueryExpr(query.buildQuery()))
infix fun <T : Any> Expr<T>.notInQuery(query: SelectedJust<T>): Expr<Boolean> =
    OperationType.NOT_IN(this, SubqueryExpr(query.buildQuery()))

infix fun <T : Any> Expr<T>.inExprs(values: Collection<Expr<T>>): Expr<Boolean> =
    OperationType.IN(this, ExprListExpr(values))
infix fun <T : Any> Expr<T>.notInExprs(values: Collection<Expr<T>>): Expr<Boolean> =
    OperationType.NOT_IN(this, ExprListExpr(values))

inline infix fun <reified T : Any> Expr<T>.inValues(values: Collection<T>): Expr<Boolean> =
    inExprs(values.map { Literal(T::class, it) })
inline infix fun <reified T : Any> Expr<T>.notInValues(values: Collection<T>): Expr<Boolean> =
    notInExprs(values.map { Literal(T::class, it) })

fun <T : Any> cast(from: Expr<*>, to: DataType<T>): Expr<T> =
    CastExpr(from, to)

inline fun <reified T : Any> literal(value: T?): Literal<T> =
    Literal(T::class, value)

fun <T : Any> all(subquery: SelectedJust<T>): ComparisonOperand<T> =
    ComparedQuery(ComparedQueryType.ALL, subquery.buildQuery())

fun <T : Any> any(subquery: SelectedJust<T>): ComparisonOperand<T> =
    ComparedQuery(ComparedQueryType.ANY, subquery.buildQuery())

operator fun Expr<out Number>.div(rhs: Expr<out Number>): Expr<Double> =
    OperationType.DIVIDE(this, rhs)
inline operator fun <reified T : Number> Expr<out Number>.div(rhs: T): Expr<Double> =
    this / literal(rhs)

operator fun <T : Number> Expr<T>.times(rhs: Expr<T>): Expr<T> =
    OperationType.MULTIPLY(this, rhs)
inline operator fun <reified T : Number> Expr<T>.times(rhs: T): Expr<T> =
    this * literal(rhs)

operator fun <T : Number> Expr<T>.plus(rhs: Expr<T>): Expr<T> =
    OperationType.PLUS(this, rhs)
inline operator fun <reified T : Number> Expr<T>.plus(rhs: T): Expr<T> =
    this * literal(rhs)

operator fun <T : Number> Expr<T>.minus(rhs: Expr<T>): Expr<T> =
    OperationType.MINUS(this, rhs)
inline operator fun <reified T : Number> Expr<T>.minus(rhs: T): Expr<T> =
    this * literal(rhs)

operator fun <T : Number> Expr<T>.unaryMinus(): Expr<T> =
    OperationType.UNARY_MINUS(this)

fun <T : Any, R : Any> case(expr: Expr<T>, vararg cases: CaseWhenThen<T, R>): ElseableCaseExpr<T, R> =
    ElseableCaseExpr(false, expr, cases.asList(), null)

fun <R : Any> case(vararg cases: CaseWhenThen<Boolean, R>): ElseableCaseExpr<Boolean, R> =
    ElseableCaseExpr(true, literal(true), cases.asList(), null)

fun <T : Any> when_(expr: Expr<T>): CaseWhen<T> = CaseWhen(expr)
inline fun <reified T : Any> when_(expr: T): CaseWhen<T> = when_(literal(expr))

inline infix fun <T : Any, reified R : Any> ElseableCaseExpr<T, R>.else_(expr: R): Expr<R> =
    CaseExpr(isGeneralCase, onExpr, cases, literal(expr))

fun <T : Any> coalesce(expr: Expr<T>, vararg operands: Expr<T>): Expr<T> =
    OperationType.COALESCE(expr, *operands)