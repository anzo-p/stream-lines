package net.anzop.helpers

object MapExtensions {
  implicit class EnvOps(env: Map[String, String]) {

    def getOrThrow(key: String, errMsg: String): String = {
      env.getOrElse(key, throw new IllegalArgumentException(errMsg))
    }
  }

  implicit class MapOps[A, B](a: Map[A, B]) {

    def intersect(b: Map[A, B]): Map[A, B] = a
      .keySet
      .intersect(b.keySet)
      .map(k => k -> b(k))
      .toMap
  }
}
