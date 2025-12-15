package net.anzop.processors

import org.apache.flink.util.Collector

import scala.collection.mutable.ListBuffer

trait BaseWindowSpec {

  class ListCollector[T] extends Collector[T] {
    private val list: ListBuffer[T] = ListBuffer.empty[T]

    override def collect(record: T): Unit = {
      list += record
    }

    override def close(): Unit = {}

    def getResults: List[T] = list.toList
  }
}
