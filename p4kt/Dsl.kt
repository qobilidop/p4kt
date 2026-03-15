package p4kt

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass

@PublishedApi
internal fun <T : StructRef> createStructRef(clazz: KClass<T>, base: P4Expr): T {
  return clazz.constructors.first().call(base)
}

val IN = Direction.IN
val OUT = Direction.OUT
val INOUT = Direction.INOUT

fun bit(width: Int) = P4Type.Bit(width)

val bool_ = P4Type.Bool
val void_ = P4Type.Void

fun ref(name: String) = P4Expr.Ref(name)

fun lit(value: Long) = P4Expr.Lit(value)

fun lit(value: Int) = P4Expr.Lit(value.toLong())

fun lit(width: Int, value: Long) = P4Expr.TypedLit(width, value)

fun lit(width: Int, value: Int) = P4Expr.TypedLit(width, value.toLong())

operator fun P4Expr.minus(other: P4Expr) = P4Expr.BinOp(BinOpKind.SUB, this, other)

infix fun P4Expr.eq(other: P4Expr) = P4Expr.BinOp(BinOpKind.EQ, this, other)

infix fun P4Expr.ne(other: P4Expr) = P4Expr.BinOp(BinOpKind.NE, this, other)

open class StatementBuilder {
  protected val body = mutableListOf<P4Statement>()

  fun varDecl(type: P4Type, init: P4Expr? = null) = VarDeclDelegate(body, type, init)

  fun assign(target: P4Expr, value: P4Expr) {
    body.add(P4Statement.Assign(target, value))
  }

  fun if_(condition: P4Expr, block: StatementBuilder.() -> Unit): IfBuilder {
    val thenBuilder = StatementBuilder()
    thenBuilder.block()
    body.add(P4Statement.If(condition, thenBuilder.statements(), emptyList()))
    return IfBuilder(body, body.size - 1)
  }

  fun return_(expr: P4Expr) {
    body.add(P4Statement.Return(expr))
  }

  fun statements() = body.toList()
}

class VarDeclDelegate(
  private val body: MutableList<P4Statement>,
  private val type: P4Type,
  private val init: P4Expr?,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, P4Expr.Ref> {
    body.add(P4Statement.VarDecl(property.name, type, init))
    return ReadOnlyProperty { _, _ -> P4Expr.Ref(property.name) }
  }
}

class IfBuilder(private val parentBody: MutableList<P4Statement>, private val index: Int) {
  infix fun else_(block: StatementBuilder.() -> Unit) {
    val elseBuilder = StatementBuilder()
    elseBuilder.block()
    val oldIf = parentBody[index] as P4Statement.If
    parentBody[index] = oldIf.copy(elseBody = elseBuilder.statements())
  }
}

class TypedParamDelegate<T : StructRef>(
  private val params: MutableList<P4Param>,
  private val clazz: kotlin.reflect.KClass<T>,
  private val direction: Direction? = null,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, T> {
    params.add(P4Param(property.name, P4Type.Named(clazz.simpleName!!), direction))
    val instance = createStructRef(clazz, P4Expr.Ref(property.name))
    return ReadOnlyProperty { _, _ -> instance }
  }
}

class FunctionBuilder(private val name: String, private val returnType: P4Type) :
  StatementBuilder() {
  @PublishedApi internal val params = mutableListOf<P4Param>()

  fun param(type: P4Type, direction: Direction) = ParamDelegate(params, type, direction)

  fun param(type: P4TypeReference, direction: Direction) =
    ParamDelegate(params, type.typeRef, direction)

  inline fun <reified T : StructRef> param() = TypedParamDelegate(params, T::class)

  inline fun <reified T : StructRef> param(direction: Direction) =
    TypedParamDelegate(params, T::class, direction)

  fun build() = P4Function(name, returnType, params, body)
}

fun typeName(name: String) = P4Type.Named(name)

class ConstDelegate(
  private val type: P4Type,
  private val value: P4Expr,
  private val register: (P4Const) -> Unit,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, P4Expr.Ref> {
    register(P4Const(property.name, type, value))
    return ReadOnlyProperty { _, _ -> P4Expr.Ref(property.name) }
  }
}

fun p4Const(name: String, type: P4Type, value: P4Expr) = P4Const(name, type, value)

fun p4Typedef(name: String, type: P4Type) = P4Typedef(name, type)

class FieldsBuilder {
  private val fields = mutableListOf<P4Field>()

  fun field(name: String, type: P4Type) {
    fields.add(P4Field(name, type))
  }

  fun field(name: String, type: P4TypeReference) {
    fields.add(P4Field(name, type.typeRef))
  }

  fun build() = fields.toList()
}

fun p4Header(name: String, block: FieldsBuilder.() -> Unit): P4Header {
  val builder = FieldsBuilder()
  builder.block()
  return P4Header(name, builder.build())
}

fun p4Struct(name: String, block: FieldsBuilder.() -> Unit): P4Struct {
  val builder = FieldsBuilder()
  builder.block()
  return P4Struct(name, builder.build())
}

class FieldDelegate(
  private val fields: MutableList<P4Field>,
  private val expr: P4Expr,
  private val type: P4Type,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, P4Expr> {
    fields.add(P4Field(property.name, type))
    val fieldAccess = P4Expr.FieldAccess(expr, property.name)
    return ReadOnlyProperty { _, _ -> fieldAccess }
  }
}

abstract class StructRef(val expr: P4Expr) {
  val fields = mutableListOf<P4Field>()

  fun field(type: P4Type) = FieldDelegate(fields, expr, type)

  fun field(type: P4TypeReference) = FieldDelegate(fields, expr, type.typeRef)
}

abstract class HeaderRef(expr: P4Expr) : StructRef(expr)

class ParamDelegate(
  private val params: MutableList<P4Param>,
  private val type: P4Type,
  private val direction: Direction? = null,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, P4Expr.Ref> {
    params.add(P4Param(property.name, type, direction))
    return ReadOnlyProperty { _, _ -> P4Expr.Ref(property.name) }
  }
}

class ActionBuilder : StatementBuilder() {
  @PublishedApi internal val params = mutableListOf<P4Param>()

  fun param(type: P4Type) = ParamDelegate(params, type)

  fun param(type: P4Type, direction: Direction) = ParamDelegate(params, type, direction)

  fun param(type: P4TypeReference) = ParamDelegate(params, type.typeRef)

  fun param(type: P4TypeReference, direction: Direction) =
    ParamDelegate(params, type.typeRef, direction)

  inline fun <reified T : StructRef> param() = TypedParamDelegate(params, T::class)

  inline fun <reified T : StructRef> param(direction: Direction) =
    TypedParamDelegate(params, T::class, direction)

  fun build(name: String) = P4Action(name, params, body)
}

fun p4Action(name: String, block: ActionBuilder.() -> Unit): P4Action {
  val builder = ActionBuilder()
  builder.block()
  return builder.build(name)
}

fun p4Function(name: String, returnType: P4Type, block: FunctionBuilder.() -> Unit): P4Function {
  val builder = FunctionBuilder(name, returnType)
  builder.block()
  return builder.build()
}

class DeclDelegate<T : P4Declaration>(
  private val factory: (String) -> T,
  private val register: (T) -> Unit,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, T> {
    val decl = factory(property.name)
    register(decl)
    return ReadOnlyProperty { _, _ -> decl }
  }
}

class ProgramBuilder {
  @PublishedApi internal val declarations = mutableListOf<P4Declaration>()

  fun typedef(type: P4Type) =
    DeclDelegate<P4Typedef>(
      factory = { name -> p4Typedef(name, type) },
      register = { declarations.add(it) },
    )

  fun header(block: FieldsBuilder.() -> Unit) =
    DeclDelegate<P4Header>(
      factory = { name -> p4Header(name, block) },
      register = { declarations.add(it) },
    )

  fun struct(block: FieldsBuilder.() -> Unit) =
    DeclDelegate<P4Struct>(
      factory = { name -> p4Struct(name, block) },
      register = { declarations.add(it) },
    )

  fun const_(type: P4Type, value: P4Expr) = ConstDelegate(type, value) { declarations.add(it) }

  fun const_(type: P4TypeReference, value: P4Expr) =
    ConstDelegate(type.typeRef, value) { declarations.add(it) }

  fun action(block: ActionBuilder.() -> Unit) =
    DeclDelegate<P4Action>(
      factory = { name -> p4Action(name, block) },
      register = { declarations.add(it) },
    )

  inline fun <reified T : StructRef> struct() {
    val dummy = createStructRef(T::class, P4Expr.Ref(""))
    val name = T::class.simpleName!!
    declarations.add(P4Struct(name, dummy.fields.toList()))
  }

  inline fun <reified T : HeaderRef> header() {
    val dummy = createStructRef(T::class, P4Expr.Ref(""))
    val name = T::class.simpleName!!
    declarations.add(P4Header(name, dummy.fields.toList()))
  }

  fun function(returnType: P4Type, block: FunctionBuilder.() -> Unit) =
    DeclDelegate<P4Function>(
      factory = { name -> p4Function(name, returnType, block) },
      register = { declarations.add(it) },
    )

  fun build() = P4Program(declarations.toList())
}

fun p4Program(block: ProgramBuilder.() -> Unit): P4Program {
  val builder = ProgramBuilder()
  builder.block()
  return builder.build()
}
