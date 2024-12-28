package net.anzop.helpers

import breeze.linalg.DenseVector
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ArrayHelpersSpec extends AnyFlatSpec with Matchers {

  "appendFromHead on Arrays" should "append 0 elements from head of source array to destination array" in {
    val src               = Array(4, 5, 6)
    val dest              = Array(1, 2, 3)
    val (newSrc, newDest) = ArrayHelpers.appendFromHead(src, dest, 0)
    newSrc should be(src)
    newDest should be(dest)
    newSrc.length + newDest.length should be(src.length + dest.length)
  }

  "appendFromHead on Arrays" should "append 1 elements from head of source array to destination array" in {
    val src               = Array(4, 5, 6)
    val dest              = Array(1, 2, 3)
    val (newSrc, newDest) = ArrayHelpers.appendFromHead(src, dest, 1)
    newSrc should be(Array(5, 6))
    newDest should be(Array(1, 2, 3, 4))
    newSrc.length + newDest.length should be(src.length + dest.length)
  }

  "appendFromHead on Arrays" should "append n elements from head of source array to destination array" in {
    val src               = Array(4, 5, 6)
    val dest              = Array(1, 2, 3)
    val (newSrc, newDest) = ArrayHelpers.appendFromHead(src, dest, 3)
    newSrc should be(Array())
    newDest should be(Array(1, 2, 3, 4, 5, 6))
    newSrc.length + newDest.length should be(src.length + dest.length)
  }

  "appendFromHead on Arrays" should "throw IllegalArgumentException when n is negative" in {
    val src  = Array(4, 5, 6)
    val dest = Array(1, 2, 3)
    an[IllegalArgumentException] should be thrownBy {
      ArrayHelpers.appendFromHead(src, dest, -1)
    }
  }

  "appendFromHead on Arrays" should "throw IllegalArgumentException when source array does not contain n elements" in {
    val src  = Array(4, 5, 6)
    val dest = Array(1, 2, 3)
    an[IllegalArgumentException] should be thrownBy {
      ArrayHelpers.appendFromHead(src, dest, 4)
    }
  }

  "appendFromHead on Arrays" should "throw IllegalArgumentException when source array is empty" in {
    val src  = Array[Int]()
    val dest = Array(1, 2, 3)
    an[IllegalArgumentException] should be thrownBy {
      ArrayHelpers.appendFromHead(src, dest, 1)
    }
  }

  "appendFromHead on DenseVectors" should "append 0 elements from head of source DenseVector to destination DenseVector" in {
    val src               = DenseVector(4, 5, 6)
    val dest              = DenseVector(1, 2, 3)
    val (newSrc, newDest) = ArrayHelpers.appendFromHead(src, dest, 0)
    newSrc should be(src)
    newDest should be(dest)
  }

  "appendFromHead on DenseVectors" should "append 1 elements from head of source DenseVector to destination DenseVector" in {
    val src               = DenseVector(4, 5, 6)
    val dest              = DenseVector(1, 2, 3)
    val (newSrc, newDest) = ArrayHelpers.appendFromHead(src, dest, 1)
    newSrc should be(DenseVector(5, 6))
    newDest should be(DenseVector(1, 2, 3, 4))
  }

  "appendFromHead on DenseVectors" should "append n elements from head of source DenseVector to destination DenseVector" in {
    val src               = DenseVector(4, 5, 6)
    val dest              = DenseVector(1, 2, 3)
    val (newSrc, newDest) = ArrayHelpers.appendFromHead(src, dest, 3)
    newSrc.toArray should be(Array())
    newDest should be(DenseVector(1, 2, 3, 4, 5, 6))
  }

  "appendFromHead on DenseVectors" should "drop n elements from head after appending n to tail when fixedSizeDest" in {
    val src               = DenseVector(4, 5, 6)
    val dest              = DenseVector(1, 2, 3)
    val (newSrc, newDest) = ArrayHelpers.appendFromHead(src, dest, 2, fixedSizeDest = true)
    newSrc should be(DenseVector(6))
    newDest should be(DenseVector(3, 4, 5))
  }
}
