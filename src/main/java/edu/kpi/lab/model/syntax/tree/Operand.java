package edu.kpi.lab.model.syntax.tree;

import edu.kpi.lab.model.syntax.SyntaxType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Operand extends Node{

  private SyntaxType type;

  private String value;
}
