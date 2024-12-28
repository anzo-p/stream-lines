package net.anzop.helpers

import breeze.linalg.DenseVector
import net.anzop.models.Types.DV

import scala.reflect.ClassTag

object ArrayHelpers {

  def appendFromHead[T : ClassTag](src: Array[T], dest: Array[T], n: Int): (Array[T], Array[T]) = {
    require(n >= 0, "n should be non-negative")
    require(src.length >= n, s"Source array must contain at least $n elements")
    val (shift, newSource) = src.splitAt(n)
    (newSource, dest ++ shift)
  }

  def appendFromHead[T : ClassTag](
      src: DV[T],
      dest: DV[T],
      n: Int,
      fixedSizeDest: Boolean = false
    ): (DV[T], DV[T]) = {
    val (newSrc, newDest) = ArrayHelpers.appendFromHead(src.toArray, dest.toArray, n)
    val fixedDest         = if (fixedSizeDest) newDest.drop(n) else newDest
    (DenseVector(newSrc), DenseVector(fixedDest))
  }
}
