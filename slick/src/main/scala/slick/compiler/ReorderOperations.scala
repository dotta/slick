package slick.compiler

import slick.ast._
import slick.ast.Util._
import slick.ast.TypeUtil._
import slick.util.{Ellipsis, ??}

/** Reorder certain stream operations for more efficient merging in `mergeToComprehensions`. */
class ReorderOperations extends Phase {
  val name = "reorderOperations"

  def apply(state: CompilerState) = state.map(convert)

  def convert(tree: Node): Node = tree.replace({
    // Push Bind into Union
    case n @ Bind(s1, Union(l1, r1, all), sel) =>
      logger.debug("Pushing Bind into both sides of a Union", Ellipsis(n, List(0, 0), List(0, 1)))
      val s1l, s1r = new AnonSymbol
      val n2 = Union(
        Bind(s1l, l1, sel.replace { case Ref(s) if s == s1 => Ref(s1l) }),
        Bind(s1r, r1, sel.replace { case Ref(s) if s == s1 => Ref(s1r) }),
        all).infer()
      logger.debug("Pushed Bind into both sides of a Union", Ellipsis(n2, List(0, 0), List(1, 0)))
      n2

    // Push Filter into Union
    case n @ Filter(s1, Union(l1, r1, all), pred) =>
      logger.debug("Pushing Filter into both sides of a Union", Ellipsis(n, List(0, 0), List(0, 1)))
      val s1l, s1r = new AnonSymbol
      val n2 = Union(
        Filter(s1l, l1, pred.replace { case Ref(s) if s == s1 => Ref(s1l) }),
        Filter(s1r, r1, pred.replace { case Ref(s) if s == s1 => Ref(s1r) }),
        all).infer()
      logger.debug("Pushed Filter into both sides of a Union", Ellipsis(n2, List(0, 0), List(1, 0)))
      n2

    // Push CollectionCast into Union
    case n @ CollectionCast(Union(l1, r1, all), cons) =>
      logger.debug("Pushing CollectionCast into both sides of a Union", Ellipsis(n, List(0, 0), List(0, 1)))
      val n2 = Union(CollectionCast(l1, cons), CollectionCast(r1, cons), all).infer()
      logger.debug("Pushed CollectionCast into both sides of a Union", Ellipsis(n2, List(0, 0), List(1, 0)))
      n2

    case n => n
  }, keepType = true, bottomUp = true)
}
