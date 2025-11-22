package edu.kpi.lab.model.runner;

import edu.kpi.lab.model.syntax.tree.Node;
import edu.kpi.lab.model.syntax.tree.SyntaxType;
import lombok.Data;

@Data
class PipelineTask {
  private int processorId;
  private Node node;
  private SyntaxType operation;
  private int startTime;
  private int finishTime;

  PipelineTask(int processorId, Node node, SyntaxType operation,
               int startTime, int finishTime) {
    this.processorId = processorId;
    this.node = node;
    this.operation = operation;
    this.startTime = startTime;
    this.finishTime = finishTime;
  }
}
