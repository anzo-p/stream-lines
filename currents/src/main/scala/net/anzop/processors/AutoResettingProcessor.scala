package net.anzop.processors

trait AutoResettingProcessor {

  protected def autoResetState(elemTs: Long): Boolean =
    if (elemTs <= earliestExpectedElemTimestamp) {
      resetOp()
      true
    }
    else {
      false
    }

  val earliestExpectedElemTimestamp: Long

  def resetOp: () => Unit
}
