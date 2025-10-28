package edu.kpi.lab.model.transform.associative.node;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode
public class FunctionExpression extends Expression {
  private String name;
  private Expression arg;

  public FunctionExpression(String name, Expression arg) {
    this.name = name;
    this.arg = arg;
  }

  @Override
  String toStr(int parentPrec) {
    return getName() + "(" + getArg().toStr(0) + ")";
  }
}

