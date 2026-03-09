package net.anzop.serdes

trait LocalJsonSerializer {
  private def escape(s: String): String =
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

  private def unescape(s: String): String =
    s.replace("\\\"", "\"")
      .replace("\\\\", "\\")
      .replace("\\b", "\b")
      .replace("\\f", "\f")
      .replace("\\n", "\n")
      .replace("\\r", "\r")
      .replace("\\t", "\t")

  private def parse(json: String): Map[String, String] = {
    val pattern =
      "\"((?:\\\\.|[^\"])*)\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"".r

    pattern
      .findAllMatchIn(json)
      .map(m => unescape(m.group(1)) -> unescape(m.group(2)))
      .toMap
  }

  protected def jsString(s: String): String =
    "\"" + escape(s) + "\""

  protected def tagsJson(tags: Map[String, String]): String =
    tags
      .map { case (k, v) => jsString(k) + ":" + jsString(v) }
      .mkString("{", ",", "}")

  protected def jsonValue(json: String, key: String): Option[String] =
    parse(json).get(key)
}
