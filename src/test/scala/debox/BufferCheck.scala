package debox

import org.scalatest.matchers.ShouldMatchers
import org.scalatest._
import prop._
import org.scalacheck.Arbitrary._
import org.scalacheck._
import Gen._
import Arbitrary.arbitrary

import scala.collection.mutable
import scala.reflect._

abstract class BufferCheck[A: Arbitrary: ClassTag]
    extends PropSpec with Matchers with GeneratorDrivenPropertyChecks {

  import scala.collection.immutable.Set
  import scala.collection.immutable.Vector

  def hybridEq[A](d: Buffer[A], s: IndexedSeq[A]): Boolean =
    d.length == s.length && (0 until d.length).forall(i => d(i) == s(i))

  property("unsafe") {
    forAll { (xs: Array[A], a: A) =>
      val buf = Buffer.unsafe(xs)
      hybridEq(buf, xs) shouldBe true

      if (xs.length > 0 && xs(0) != a) {
        buf(0) = a
        hybridEq(buf, xs) shouldBe true
      }
    }
  }

  property("fromArray") {
    forAll { (xs: Array[A], a: A) =>
      val buf = Buffer.fromArray(xs)
      hybridEq(buf, xs) shouldBe true

      if (xs.length > 0 && xs(0) != a) {
        buf(0) = a
        hybridEq(buf, xs) shouldBe false
      }
    }
  }

  property("fromIterable, apply") {
    forAll { xs: List[A] =>
      val buf1 = Buffer.fromIterable(xs)
      val buf2 = Buffer(xs: _*)
      val seq = xs.toIndexedSeq
      hybridEq(buf1, seq) shouldBe true
      hybridEq(buf2, seq) shouldBe true
    }
  }

  property("fill") {
    forAll { (n0: Byte, a: A) =>
      val n = n0 & 0xff // make sure n is non-negative and also not huge
      val buf = Buffer.fill(n)(a)
      val arr = Array.fill(n)(a)
      hybridEq(buf, arr) shouldBe true
    }
  }

  property("equals (==), hashCode (##)") {
    forAll { xs: List[A] =>
      val seq = xs.toIndexedSeq
      val a = Buffer.fromIterable(seq)
      val b = Buffer.fromIterable(seq ++ seq).slice(0, seq.length)
      a shouldBe b
      a.## shouldBe b.##
    }
  }

  property("copy") {
    forAll { (xs: List[A], x: A) =>
      val a = Buffer.fromIterable(xs)
      val b = a.copy
      a shouldBe b
      while (a.nonEmpty) {
        a.pop
        a should not be b
      }
    }
  }

  property("clear") {
    forAll { xs: List[A] =>
      val a = Buffer.fromIterable(xs)
      a.clear
      a.isEmpty shouldBe true
      a.length shouldBe 0
      a shouldBe Buffer.empty[A]
    }
  }

  property("adding elements (+=)") {
    forAll { xs: List[A] =>
      val buf = Buffer.empty[A]
      val control = mutable.ArrayBuffer.empty[A]
      xs.foreach { x =>
        buf += x
        control += x
        hybridEq(buf, control) shouldBe true
      }
    }
  }

  property("removing elements (-=)") {
    forAll { xs: List[A] =>
      val buf = Buffer.fromIterable(xs)
      val control = mutable.ArrayBuffer(xs.toSeq: _*)
      xs.foreach { _ =>
        buf.pop
        control.remove(control.length - 1)
        hybridEq(buf, control) shouldBe true
      }
    }
  }

  property("random append and pop") {
    forAll { (steps: List[Option[A]]) =>
      val buf = Buffer.empty[A]
      val control = mutable.ArrayBuffer.empty[A]
      var lvl: Int = 0
      steps.foreach {
        case Some(x) =>
          buf += x
          control += x
          lvl += 1
        case None =>
          if (lvl > 0) {
            buf.pop
            control.remove(control.length - 1)
            lvl -= 1
          }
      }
      hybridEq(buf, control) shouldBe true
    }
  }

  property("bulk add (++=)") {
    forAll { (xs: List[A], ys: List[A]) =>
      val buf = Buffer.empty[A]
      val control = mutable.ArrayBuffer.empty[A]
  
      buf ++= xs
      control ++= xs
      hybridEq(buf, control) shouldBe true
  
      buf ++= ys
      control ++= ys
      hybridEq(buf, control) shouldBe true
    }
  }
}

class BooleanBufferCheck extends BufferCheck[Boolean]
class IntBufferCheck extends BufferCheck[Int]
class DoubleBufferCheck extends BufferCheck[Double]
class StringBufferCheck extends BufferCheck[String]
