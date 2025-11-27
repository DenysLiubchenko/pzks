package edu.kpi.lab.model.runner;

import edu.kpi.lab.model.syntax.tree.Node;
import edu.kpi.lab.model.syntax.tree.SyntaxType;
import lombok.Data;

@Data
public class PipelineTask {
  private int processorId;
  private Node node;
  private SyntaxType operation;
  private int startTime;
  private int finishTime;
  private String stage;
  private boolean active = true; // Чи операція активна в цьому шарі

  public PipelineTask(int processorId, Node node, SyntaxType operation,
                      int startTime, int finishTime) {
    this.processorId = processorId;
    this.node = node;
    this.operation = operation;
    this.startTime = startTime;
    this.finishTime = finishTime;
    this.active = true;
  }
}