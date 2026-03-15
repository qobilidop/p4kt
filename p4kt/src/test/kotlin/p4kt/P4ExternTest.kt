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
  fun externDsl() {
    val program = p4Program {
      @Suppress("UnusedPrivateProperty")
      val Ck16 by extern {
        constructor_()
        method("clear", P4.void_)
        method("get", P4.bit(16))
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
  fun externInstanceRendering() {
    val inst = P4ExternInstance("Ck16", "ck")
    assertEquals("Ck16() ck;", inst.toP4())
  }

  @Test
  fun externInstanceInControl() {
    val ext = P4Extern("Ck16", emptyList())
    val ctrl =
      p4Control("MyCtrl") {
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
