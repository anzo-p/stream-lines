package net.anzop.serdes

trait LocalJsonSerializer {
  protected def escape(s: String): String =
    s.flatMap {
      case '"'  => "\\\""
      case '\\' => "\\\\"
      case '\b' => "\\b"
      case '\f' => "\\f"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case c    => c.toString
    }

  protected def jsString(s: String): String =
    "\"" + escape(s) + "\""

  protected def tagsJson(tags: Map[String, String]): String =
    tags
      .map { case (k, v) => jsString(k) + ":" + jsString(v) }
      .mkString("{", ",", "}")
}
