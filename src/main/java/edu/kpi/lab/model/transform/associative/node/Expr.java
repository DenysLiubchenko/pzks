package edu.kpi.lab.model.transform.associative.node;

/** Base expression node for associative transformations. */
public abstract class Expr {
  abstract String toStr(int parentPrec);

  @Override
  public String toString() {
    return toStr(0);
  }
}

