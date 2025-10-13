package edu.kpi.lab.model.transformation.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UnaryNode extends ExpressionNode {
  private final String operation;
  private final ExpressionNode child;

  @Override
  public String toExpression() {
    String childStr = child.toExpression();
    if (needsParens(child, 3, false)) {
      childStr = "(" + childStr + ")";
    }
    return operation + childStr;
  }
}
