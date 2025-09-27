package edu.kpi.lab.model.syntax.tree;

import edu.kpi.lab.model.syntax.SyntaxType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Function extends Node{
  private Node left;

  private Node right;

  private SyntaxType operation;

  public Function(int position, Node left, Node right, SyntaxType operation) {
    super(position);
    this.left = left;
    this.right = right;
    this.operation = operation;
  }

  public Function(int position) {
    super(position);
  }
}
