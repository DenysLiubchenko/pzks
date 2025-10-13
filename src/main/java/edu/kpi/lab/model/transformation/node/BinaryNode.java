package edu.kpi.lab.model.transformation.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BinaryNode extends ExpressionNode {
  private final String operation;
  private final ExpressionNode left, right;

  @Override
  public String toExpression() {
    int myPrec = getPrecedence(operation);
    String leftStr = left.toExpression();
    if (needsParens(left, myPrec, true)) {
      leftStr = "(" + leftStr + ")";
    }
    String rightStr = right.toExpression();
    if (needsParens(right, myPrec, false)) {
      rightStr = "(" + rightStr + ")";
    }
    return leftStr + operation + rightStr;
  }
}
