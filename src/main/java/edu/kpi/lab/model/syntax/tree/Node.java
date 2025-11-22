package edu.kpi.lab.model.syntax.tree;

import java.util.UUID;
import lombok.Data;

@Data
public abstract class Node {
  private final UUID id = UUID.randomUUID();

  public String getId() {
    return id.toString().substring(0, 8);
  }
}
