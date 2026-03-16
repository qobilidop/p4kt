package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ErrorTest {
  @Test
  fun errorDeclaration() {
    val err =
      P4Error(listOf("IPv4OptionsNotSupported", "IPv4IncorrectVersion", "IPv4ChecksumError"))
    assertEquals(
      """
                error {
                    IPv4OptionsNotSupported,
                    IPv4IncorrectVersion,
                    IPv4ChecksumError
                }
            """
        .trimIndent(),
      err.toP4(),
    )
  }

  @Test
  fun errorInProgram() {
    val program = P4.program { errors("IPv4OptionsNotSupported", "IPv4IncorrectVersion") }
    assertEquals(
      """
                error {
                    IPv4OptionsNotSupported,
                    IPv4IncorrectVersion
                }
            """
        .trimIndent(),
      program.toP4(),
    )
  }

  @Test
  fun errorDeclInLibrary() {
    assertEquals(
      """
                error {
                    NoError,
                    PacketTooShort
                }
            """
        .trimIndent(),
      TestErrorLib.toP4(),
    )
    assertEquals("error.NoError", TestErrorLib.error.NoError.toP4())
    assertEquals("error.PacketTooShort", TestErrorLib.error.PacketTooShort.toP4())
  }
}

private object TestErrorLib : P4.Library() {
  object error : P4.ErrorDecl() {
    val NoError by member()
    val PacketTooShort by member()
  }

  init {
    register(error)
  }
}
