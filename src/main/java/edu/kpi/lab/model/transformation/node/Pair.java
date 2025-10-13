package edu.kpi.lab.model.transformation.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Pair {
  private boolean isPositive;
  private final ExpressionNode term;
}
