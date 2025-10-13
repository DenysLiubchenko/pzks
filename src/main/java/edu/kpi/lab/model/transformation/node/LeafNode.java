package edu.kpi.lab.model.transformation.node;

public class LeafNode extends ExpressionNode {
  private final String value;

  public LeafNode(String value) {
    this.value = value;
  }

  @Override
  public String toExpression() {
    return value;
  }
}
