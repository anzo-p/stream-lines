package net.anzop.serdes

trait DataSerializer[T] {
  def serialize(data: T): String
}
