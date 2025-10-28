package edu.kpi.lab.model.transform.associative.node;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode(callSuper = false)
public class UnaryExpression extends Expression {
  private String op;
  private Expression operand;

  public UnaryExpression(String op, Expression operand) {
    this.op = op;
    this.operand = operand;
  }

  @Override
  String toStr(int parentPrec) {
    if (getOp().equals("-")) {
      String s = "-" + getOperand().toStr(3);
      if (parentPrec >= 3) {
        s = "(" + s + ")";
      }
      return s;
    } else if (getOp().equals("/")) {
      String s = "1/" + getOperand().toStr(3);
      if (parentPrec > 2) {
        s = "(" + s + ")";
      }
      return s;
    }
    return "";
  }
}

