package edu.kpi.lab.model.transform.associative.node;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode
public class FunctionExpr extends Expr {
  private String name;
  private Expr arg;

  public FunctionExpr(String name, Expr arg) {
    this.name = name;
    this.arg = arg;
  }

  @Override
  String toStr(int parentPrec) {
    return getName() + "(" + getArg().toStr(0) + ")";
  }
}

