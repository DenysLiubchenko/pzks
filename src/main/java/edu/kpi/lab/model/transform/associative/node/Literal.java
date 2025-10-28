package edu.kpi.lab.model.transform.associative.node;

import edu.kpi.lab.model.lexical.TokenType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode(callSuper = false)
public class Literal extends Expression {
  private String value;
  private TokenType type;

  public Literal(String value, TokenType type) {
    this.value = value;
    this.type = type;
  }

  @Override
  String toStr(int parentPrec) {
    return getValue();
  }
}

