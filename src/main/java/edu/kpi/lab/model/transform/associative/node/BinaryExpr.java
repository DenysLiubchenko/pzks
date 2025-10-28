package edu.kpi.lab.model.transform.associative.node;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode
public class BinaryExpr extends Expr {
  private Expr left;
  private String op;
  private Expr right;

  public BinaryExpr(Expr left, String op, Expr right) {
    this.left = left;
    this.op = op;
    this.right = right;
  }

  private int getPrec() {
    if (getOp().equals("+") || getOp().equals("-")) {
      return 1;
    }
    if (getOp().equals("*") || getOp().equals("/")) {
      return 2;
    }
    return 0;
  }

  @Override
  String toStr(int parentPrec) {
    int myPrec = getPrec();
    String s;
    if (getOp().equals("+")) {
      if (getRight() instanceof UnaryExpr u && u.getOp().equals("-")) {
        s = getLeft().toStr(myPrec) + "-" + u.getOperand().toStr(myPrec + 1);
      } else if (getLeft() instanceof UnaryExpr u && u.getOp().equals("-")) {
        s = getRight().toStr(myPrec) + "-" + u.getOperand().toStr(myPrec + 1);
      } else {
        s = getLeft().toStr(myPrec) + "+" + getRight().toStr(myPrec);
      }
    } else if (getOp().equals("*")) {
      if (getRight() instanceof UnaryExpr u && u.getOp().equals("/")) {
        s = getLeft().toStr(myPrec) + "/" + u.getOperand().toStr(myPrec + 1);
      } else if (getLeft() instanceof UnaryExpr u && u.getOp().equals("/")) {
        s = getRight().toStr(myPrec) + "/" + u.getOperand().toStr(myPrec + 1);
      } else {
        s = getLeft().toStr(myPrec) + "*" + getRight().toStr(myPrec);
      }
    } else {
      s = getLeft().toStr(myPrec) + getOp() +
          getRight().toStr(myPrec + (getOp().equals("-") || getOp().equals("/") ? 1 : 0));
    }
    if (myPrec <= parentPrec) {
      s = "(" + s + ")";
    }
    return s;
  }
}

