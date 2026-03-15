@file:Suppress("TooManyFunctions", "MatchingDeclarationName")

package p4kt

import kotlin.properties.ReadOnlyProperty

// User-visible IR types

sealed class P4Type {
  data class Bit(val width: Int) : P4Type()

  data class Named(val name: String) : P4Type()

  data object Bool : P4Type()

  data object Void : P4Type()

  data object Error : P4Type()

  data object PacketIn : P4Type()

  data object PacketOut : P4Type()
}

enum class Direction {
  IN,
  OUT,
  INOUT,
}

enum class MatchKind {
  EXACT,
  LPM,
  TERNARY,
}

sealed class P4Expr {
  data class Ref(val name: String) : P4Expr()

  data class Lit(val value: Long) : P4Expr()

  data class TypedLit(val width: Int, val value: Long) : P4Expr()

  data class FieldAccess(val expr: P4Expr, val field: String) : P4Expr()

  data class BinOp(val op: BinOpKind, val left: P4Expr, val right: P4Expr) : P4Expr()

  data class MethodCall(val expr: P4Expr, val method: String, val args: List<P4Expr>) : P4Expr()

  data class ErrorMember(val name: String) : P4Expr()

  operator fun minus(other: P4Expr) = BinOp(BinOpKind.SUB, this, other)

  infix fun eq(other: P4Expr) = BinOp(BinOpKind.EQ, this, other)

  infix fun ne(other: P4Expr) = BinOp(BinOpKind.NE, this, other)

  fun call(method: String, vararg args: P4Expr) = MethodCall(this, method, args.toList())
}

sealed interface P4Declaration

sealed interface P4TypeReference {
  val typeRef: P4Type.Named
}

data class P4Typedef(val name: String, val type: P4Type) : P4Declaration, P4TypeReference {
  override val typeRef
    get() = P4Type.Named(name)
}

data class P4Header(val name: String, val fields: List<P4Field>) : P4Declaration, P4TypeReference {
  override val typeRef
    get() = P4Type.Named(name)
}

data class P4Struct(val name: String, val fields: List<P4Field>) : P4Declaration, P4TypeReference {
  override val typeRef
    get() = P4Type.Named(name)
}

data class P4Const(val name: String, val type: P4Type, val value: P4Expr) : P4Declaration {
  val ref
    get() = P4Expr.Ref(name)
}

data class P4Action(val name: String, val params: List<P4Param>, val body: List<P4Statement>) :
  P4Declaration

data class P4Table(
  val name: String,
  val keys: List<P4KeyEntry>,
  val actions: List<String>,
  val size: Int? = null,
  val defaultAction: String,
  val isDefaultActionConst: Boolean,
) : P4Declaration

data class P4Control(
  val name: String,
  val params: List<P4Param>,
  val declarations: List<P4Declaration>,
  val body: List<P4Statement>,
) : P4Declaration

data class P4Error(val members: List<String>) : P4Declaration

data class P4Extern(val name: String, val methods: List<P4ExternMethod>) :
  P4Declaration, P4TypeReference {
  override val typeRef
    get() = P4Type.Named(name)
}

data class P4Parser(
  val name: String,
  val params: List<P4Param>,
  val declarations: List<P4Declaration>,
  val states: List<P4ParserState>,
) : P4Declaration

data class P4PackageInstance(val typeName: String, val args: List<String>, val name: String) :
  P4Declaration

data class P4Program(val declarations: List<P4Declaration>) {
  fun toP4(): String = declarations.joinToString("\n\n") { it.toP4() }
}

// P4 object - user-facing API

object P4 {
  // Type factories

  fun bit(width: Int) = P4Type.Bit(width)

  val bool_ = P4Type.Bool
  val void_ = P4Type.Void
  val errorType = P4Type.Error
  val packet_in = P4Type.PacketIn
  val packet_out = P4Type.PacketOut

  fun typeName(name: String) = P4Type.Named(name)

  // Expression factories

  fun ref(name: String) = P4Expr.Ref(name)

  fun lit(value: Long) = P4Expr.Lit(value)

  fun lit(value: Int) = P4Expr.Lit(value.toLong())

  fun lit(width: Int, value: Long) = P4Expr.TypedLit(width, value)

  fun lit(width: Int, value: Int) = P4Expr.TypedLit(width, value.toLong())

  fun error_(name: String) = P4Expr.ErrorMember(name)

  // Direction constants

  val IN = Direction.IN
  val OUT = Direction.OUT
  val INOUT = Direction.INOUT

  // Match kind constants

  val LPM = MatchKind.LPM
  val EXACT = MatchKind.EXACT
  val TERNARY = MatchKind.TERNARY

  // StructRef and HeaderRef

  abstract class StructRef(val expr: P4Expr) {
    val fields = mutableListOf<P4Field>()

    fun field(type: P4Type) = FieldDelegate(fields, expr, type)

    fun field(type: P4TypeReference) = FieldDelegate(fields, expr, type.typeRef)

    fun <T : StructRef> field(factory: (P4Expr) -> T) = TypedFieldDelegate(fields, expr, factory)

    fun call(method: String, vararg args: P4Expr) =
      P4Expr.MethodCall(this.expr, method, args.toList())
  }

  abstract class HeaderRef(expr: P4Expr) : StructRef(expr)

  // Table and state refs

  class TableRef(val name: String)

  class StateRef(val name: String)

  val accept = StateRef("accept")
  val reject = StateRef("reject")

  // Library base class

  abstract class Library {
    private val declarations = mutableListOf<P4Declaration>()

    protected fun typedef(name: String, type: P4Type): P4Typedef {
      val td = p4Typedef(name, type)
      declarations.add(td)
      return td
    }

    protected fun const_(name: String, type: P4Type, value: P4Expr): P4Const {
      val c = p4Const(name, type, value)
      declarations.add(c)
      return c
    }

    protected fun <T : StructRef> struct(factory: (P4Expr) -> T) {
      val dummy = factory(P4Expr.Ref(""))
      declarations.add(P4Struct(dummy::class.simpleName!!, dummy.fields.toList()))
    }

    protected fun <T : HeaderRef> header(factory: (P4Expr) -> T) {
      val dummy = factory(P4Expr.Ref(""))
      declarations.add(P4Header(dummy::class.simpleName!!, dummy.fields.toList()))
    }

    protected fun extern(name: String, block: ExternBuilder.() -> Unit): P4Extern {
      val builder = ExternBuilder(name)
      builder.block()
      val ext = builder.build()
      declarations.add(ext)
      return ext
    }

    protected fun errors(vararg members: String) {
      declarations.add(P4Error(members.toList()))
    }

    fun toP4() = P4Program(declarations).toP4()
  }

  // Field delegates (used inside StructRef/HeaderRef)

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

  class TypedFieldDelegate<T : StructRef>(
    private val fields: MutableList<P4Field>,
    private val expr: P4Expr,
    private val factory: (P4Expr) -> T,
  ) {
    operator fun provideDelegate(
      thisRef: Any?,
      property: kotlin.reflect.KProperty<*>,
    ): ReadOnlyProperty<Any?, T> {
      val instance = factory(P4Expr.FieldAccess(expr, property.name))
      fields.add(P4Field(property.name, P4Type.Named(instance::class.simpleName!!)))
      return ReadOnlyProperty { _, _ -> instance }
    }
  }
}

fun p4Program(block: ProgramBuilder.() -> Unit): P4Program {
  val builder = ProgramBuilder()
  builder.block()
  return builder.build()
}
