package edu.kpi.lab.model.transform.associative.node;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode(callSuper = false)
public class BinaryExpression extends Expression {
  private Expression left;
  private String operation;
  private Expression right;

  public BinaryExpression(Expression left, String operation, Expression right) {
    this.left = left;
    this.operation = operation;
    this.right = right;
  }

  private int getPrec() {
    if (getOperation().equals("+") || getOperation().equals("-")) {
      return 1;
    }
    if (getOperation().equals("*") || getOperation().equals("/")) {
      return 2;
    }
    return 0;
  }

  @Override
  String toStr(int parentPrec) {
    int myPrec = getPrec();
    String s;
    if (getOperation().equals("+")) {
      if (getRight() instanceof UnaryExpression u && u.getOp().equals("-")) {
        s = getLeft().toStr(myPrec) + "-" + u.getOperand().toStr(myPrec + 1);
      } else if (getLeft() instanceof UnaryExpression u && u.getOp().equals("-")) {
        s = getRight().toStr(myPrec) + "-" + u.getOperand().toStr(myPrec + 1);
      } else {
        s = getLeft().toStr(myPrec) + "+" + getRight().toStr(myPrec);
      }
    } else if (getOperation().equals("*")) {
      if (getRight() instanceof UnaryExpression u && u.getOp().equals("/")) {
        s = getLeft().toStr(myPrec) + "/" + u.getOperand().toStr(myPrec + 1);
      } else if (getLeft() instanceof UnaryExpression u && u.getOp().equals("/")) {
        s = getRight().toStr(myPrec) + "/" + u.getOperand().toStr(myPrec + 1);
      } else {
        s = getLeft().toStr(myPrec) + "*" + getRight().toStr(myPrec);
      }
    } else {
      s = getLeft().toStr(myPrec) + getOperation() +
          getRight().toStr(myPrec + (getOperation().equals("-") || getOperation().equals("/") ? 1 : 0));
    }
    if (myPrec <= parentPrec) {
      s = "(" + s + ")";
    }
    return s;
  }
}

