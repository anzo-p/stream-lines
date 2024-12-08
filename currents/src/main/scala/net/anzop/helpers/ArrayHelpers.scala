package net.anzop.helpers

import scala.reflect.ClassTag

object ArrayHelpers {

  def appendFromHead[T : ClassTag](dest: Array[T], src: Array[T], n: Int): (Array[T], Array[T]) = {
    val (shift, newSource) = src.splitAt(n)
    (dest ++ shift, newSource)
  }
}
