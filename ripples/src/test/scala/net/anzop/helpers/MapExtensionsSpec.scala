package net.anzop.helpers

import net.anzop.helpers.MapExtensions.MapOps
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MapExtensionsSpec extends AnyFlatSpec with Matchers {

  "map intersect" should "return intersecting keys of given maps and their values from the latter" in {
    val a = Map(
      "a" -> 1,
      "b" -> 2,
      "c" -> 3
    )

    val b = Map(
      "b" -> 20,
      "c" -> 30,
      "d" -> 40
    )

    val expected = Map(
      "b" -> 20,
      "c" -> 30
    )

    a.intersect(b) should be(expected)
  }
}
