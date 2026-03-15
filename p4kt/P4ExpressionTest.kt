package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ExpressionTest {
  @Test
  fun boolType() {
    assertEquals("bool", bool_.toP4())
  }

  @Test
  fun voidType() {
    assertEquals("void", void_.toP4())
  }

  @Test
  fun untypedLiteral() {
    assertEquals("42", lit(42).toP4())
  }

  @Test
  fun hexLiteral() {
    assertEquals("2048", lit(0x0800).toP4())
  }

  @Test
  fun typedLiteral() {
    assertEquals("4w4", lit(4, 4).toP4())
  }

  @Test
  fun typedLiteralZero() {
    assertEquals("16w0", lit(16, 0).toP4())
  }

  @Test
  fun fieldAccess() {
    val headers = P4Expr.Ref("headers")
    assertEquals("headers.ip", P4Expr.FieldAccess(headers, "ip").toP4())
  }

  @Test
  fun chainedFieldAccess() {
    val headers = P4Expr.Ref("headers")
    assertEquals(
      "headers.ip.ttl",
      P4Expr.FieldAccess(P4Expr.FieldAccess(headers, "ip"), "ttl").toP4(),
    )
  }

  @Test
  fun subtraction() {
    val a = P4Expr.Ref("a")
    val b = P4Expr.Ref("b")
    assertEquals("a - b", (a - b).toP4())
  }

  @Test
  fun equality() {
    val a = P4Expr.Ref("a")
    val b = P4Expr.Ref("b")
    assertEquals("a == b", (a eq b).toP4())
  }

  @Test
  fun inequality() {
    val a = P4Expr.Ref("a")
    val b = P4Expr.Ref("b")
    assertEquals("a != b", (a ne b).toP4())
  }

  @Test
  fun nestedExpression() {
    val headers = P4Expr.Ref("headers")
    val ttl = P4Expr.FieldAccess(P4Expr.FieldAccess(headers, "ip"), "ttl")
    assertEquals("headers.ip.ttl - 1", (ttl - lit(1)).toP4())
  }
}
