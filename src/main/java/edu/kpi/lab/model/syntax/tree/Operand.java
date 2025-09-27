package edu.kpi.lab.model.syntax.tree;

import edu.kpi.lab.model.syntax.SyntaxType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Operand extends Node{

  private SyntaxType type;

  private String value;

  public Operand(int position, SyntaxType type, String value) {
    super(position);
    this.type = type;
    this.value = value;
  }
}
