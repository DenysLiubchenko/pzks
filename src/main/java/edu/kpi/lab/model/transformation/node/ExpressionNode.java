package edu.kpi.lab.model.transformation.node;

public abstract class ExpressionNode {
  public abstract String toExpression();

  protected static int getPrecedence(String op) {
    return switch (op) {
      case "+", "-" -> 1;
      case "*", "/" -> 2;
      default -> 0;
    };
  }

  protected static boolean needsParens(ExpressionNode node, int parentPrec, boolean isLeft) {
    if (node instanceof LeafNode) {
      return false;
    }
    if (node instanceof UnaryNode) {
      return 3 < parentPrec;
    }
    if (node instanceof BinaryNode bn) {
      int childPrec = getPrecedence(bn.getOperation());
      if (childPrec < parentPrec) {
        return true;
      }
      if (childPrec > parentPrec) {
        return false;
      }
      return !isLeft;
    }
    return false;
  }
}
