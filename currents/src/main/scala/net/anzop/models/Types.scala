package net.anzop.models

import breeze.linalg.{DenseMatrix, DenseVector, SparseVector}

object Types {

  type DM[T] = DenseMatrix[T]
  type DV[T] = DenseVector[T]
  type SV[T] = SparseVector[T]
}
