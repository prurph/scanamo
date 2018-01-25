package com.gu.scanamo.update

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary._
import com.gu.scanamo.syntax._
import scala.collection.JavaConverters._

class UpdateExpressionTest extends org.scalatest.FunSpec
  with org.scalatest.Matchers
  with org.scalatest.prop.Checkers {

  implicit lazy val arbSymbol: Arbitrary[Symbol] = Arbitrary(Gen.alphaNumStr.map(Symbol(_)))

  def leaf: Gen[UpdateExpression] =
    for {
      s <- arbitrary[Symbol]
      i <- arbitrary[Int]
      si <- arbitrary[Set[Int]]
      l <- arbitrary[List[String]]
      u <- Gen.oneOf(List(set(s -> i), add(s -> i), remove(s), delete(s -> si), append(s -> i), prepend(s -> i), appendList(s -> l), prependList(s -> l)))
    } yield u

  def genNode(level: Int): Gen[UpdateExpression] = for {
    left <- genTree(level)
    right <- genTree(level)
  } yield left and right

  def genTree(level: Int): Gen[UpdateExpression] =
    if (level >= 100) leaf else {
      Gen.oneOf(leaf, genNode(level + 1))
    }
  implicit lazy val update: Arbitrary[UpdateExpression] = Arbitrary(genTree(0))

  it("should have all value placeholders in the expression") {
    check {
      (ue: UpdateExpression) =>
        ue.attributeValues.keys.forall(s => {
          ue.expression.contains(s)
        })
    }
  }

  it("should have all name placeholders in the expression") {
    check {
      (ue: UpdateExpression) =>
        ue.attributeNames.keys.forall(s => {
          ue.expression.contains(s)
        })
    }
  }

  it("append/prepend should wrap scalar values in a list") {
    check {
      (s: Symbol, v: String) =>
        append(s -> v).unprefixedAttributeValues.get("update").exists(_.getL.asScala.toList.map(_.getS) == List(v))
        prepend(s -> v).unprefixedAttributeValues.get("update").exists(_.getL.asScala.toList.map(_.getS) == List(v))
    }
  }

  it("appendList/prependList should take the value as a list") {
    check {
      (s: Symbol, l: List[String]) =>
        appendList(s -> l).unprefixedAttributeValues.get("update").exists(_.getL.asScala.toList.map(_.getS) == l)
        prependList(s -> l).unprefixedAttributeValues.get("update").exists(_.getL.asScala.toList.map(_.getS) == l)
    }
  }
}
