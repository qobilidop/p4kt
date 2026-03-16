package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ExternTest {
  @Test
  fun externDeclaration() {
    val ext =
      P4Extern(
        "Ck16",
        listOf(
          P4ExternMethod("Ck16", P4Type.Void, emptyList()),
          P4ExternMethod("clear", P4Type.Void, emptyList()),
          P4ExternMethod(
            "update",
            P4Type.Void,
            listOf(P4Param("data", P4Type.Named("T"), Direction.IN)),
          ),
          P4ExternMethod("get", P4Type.Bit(16), emptyList()),
        ),
      )
    assertEquals(
      """
                extern Ck16 {
                    Ck16();
                    void clear();
                    void update(in T data);
                    bit<16> get();
                }
            """
        .trimIndent(),
      ext.toP4(),
    )
  }

  @Test
  fun externMethodWithTypeParams() {
    val ext =
      P4Extern(
        "packet_in",
        listOf(
          P4ExternMethod(
            "extract",
            P4Type.Void,
            listOf(P4Param("hdr", P4Type.Var("T"), Direction.OUT)),
            typeParams = listOf("T"),
          ),
          P4ExternMethod("lookahead", P4Type.Var("T"), emptyList(), typeParams = listOf("T")),
        ),
      )
    assertEquals(
      """
            extern packet_in {
                void extract<T>(out T hdr);
                T lookahead<T>();
            }
        """
        .trimIndent(),
      ext.toP4(),
    )
  }

  @Test
  fun externDsl() {
    val program =
      P4.program {
        @Suppress("UnusedPrivateProperty")
        val Ck16 by extern {
          constructor_()
          val clear by method(P4.void_)
          val get by method(P4.bit(16))
        }
      }
    assertEquals(
      """
                extern Ck16 {
                    Ck16();
                    void clear();
                    bit<16> get();
                }
            """
        .trimIndent(),
      program.toP4(),
    )
  }

  @Test
  fun externMethodTypeParamDsl() {
    val ext =
      P4.program {
        @Suppress("UnusedPrivateProperty")
        val packet_in by extern {
          val extract by
            method(P4.void_) {
              val T by typeParam()
              val hdr by param(T, P4.OUT)
            }
          val lookahead by method {
            val T by typeParam()
            returnType(T)
          }
          val advance by
            method(P4.void_) {
              val sizeInBits by param(P4.bit(32), P4.IN)
            }
          val length by method(P4.bit(32))
        }
      }
    assertEquals(
      """
        extern packet_in {
            void extract<T>(out T hdr);
            T lookahead<T>();
            void advance(in bit<32> sizeInBits);
            bit<32> length();
        }
    """
        .trimIndent(),
      ext.toP4(),
    )
  }

  @Test
  fun methodOverload() {
    val ext =
      P4.program {
        @Suppress("UnusedPrivateProperty")
        val packet_in by extern {
          val extract by
            method(P4.void_) {
              val T by typeParam()
              val hdr by param(T, P4.OUT)
            }
          val extract_var by
            overload(extract, P4.void_) {
              val T by typeParam()
              val variableSizeHeader by param(T, P4.OUT)
              val variableFieldSizeInBits by param(P4.bit(32), P4.IN)
            }
        }
      }
    assertEquals(
      """
        extern packet_in {
            void extract<T>(out T hdr);
            void extract<T>(out T variableSizeHeader, in bit<32> variableFieldSizeInBits);
        }
    """
        .trimIndent(),
      ext.toP4(),
    )
  }

  @Test
  fun externFunctionDeclaration() {
    val fn =
      P4ExternFunction(
        "verify",
        P4Type.Void,
        listOf(
          P4Param("check", P4Type.Bool, Direction.IN),
          P4Param("toSignal", P4Type.Error, Direction.IN),
        ),
      )
    assertEquals("extern void verify(in bool check, in error toSignal);", fn.toP4())
  }

  @Test
  fun externFunctionInLibrary() {
    @Suppress("UnusedPrivateProperty")
    val lib =
      object : P4.Library() {
        val verify by
          externFunction(P4.void_) {
            val check by param(P4.bool_, P4.IN)
            val toSignal by param(P4.errorType, P4.IN)
          }
      }
    assertEquals("extern void verify(in bool check, in error toSignal);", lib.toP4())
  }

  @Test
  fun externInstanceRendering() {
    val inst = P4ExternInstance("Ck16", "ck")
    assertEquals("Ck16() ck;", inst.toP4())
  }

  @Test
  fun externInstanceInControl() {
    val ext = P4Extern("Ck16", emptyList())
    val ctrl =
      P4.control("MyCtrl") {
        val ck by externInstance(ext)
        apply { call(ck, "clear") }
      }
    assertEquals(
      """
                control MyCtrl() {
                    Ck16() ck;
                    apply {
                        ck.clear();
                    }
                }
            """
        .trimIndent(),
      ctrl.toP4(),
    )
  }
}
