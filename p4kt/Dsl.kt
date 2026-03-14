package p4kt

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

val IN = Direction.IN
val OUT = Direction.OUT
val INOUT = Direction.INOUT

fun bit(width: Int) = P4Type.Bit(width)

class FunctionBuilder(private val name: String, private val returnType: P4Type) {
    private val params = mutableListOf<P4Param>()
    private val body = mutableListOf<P4Statement>()

    fun param(type: P4Type, direction: Direction): ReadOnlyProperty<Any?, P4Expr.Ref> {
        var registered = false
        return ReadOnlyProperty { _, property ->
            if (!registered) {
                params.add(P4Param(property.name, type, direction))
                registered = true
            }
            P4Expr.Ref(property.name)
        }
    }

    fun return_(expr: P4Expr) {
        body.add(P4Statement.Return(expr))
    }

    fun build() = P4Function(name, returnType, params, body)
}

fun p4Function(name: String, returnType: P4Type, block: FunctionBuilder.() -> Unit): P4Function {
    val builder = FunctionBuilder(name, returnType)
    builder.block()
    return builder.build()
}
