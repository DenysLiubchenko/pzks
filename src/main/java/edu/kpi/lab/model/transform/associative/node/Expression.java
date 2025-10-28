package edu.kpi.lab.model.transform.associative.node;

public abstract class Expression {
  abstract String toStr(int parentPrec);

  @Override
  public String toString() {
    return toStr(0);
  }
}

