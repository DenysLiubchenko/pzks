package edu.kpi.lab.model.runner;

import edu.kpi.lab.model.syntax.tree.Node;
import edu.kpi.lab.model.syntax.tree.SyntaxType;
import lombok.Data;

@Data
public class PipelineTask {
  private Node node;
  private String nodeIdShort;
  private SyntaxType operation;

  private int readTime;
  private int executionStartTime;
  private int latencyPerStage;
  private int writeTime;

  public PipelineTask(Node node, String shortId, SyntaxType operation, int readTime, int latencyPerStage,
                      int numStages) {
    this.node = node;
    this.nodeIdShort = shortId;
    this.operation = operation;
    this.readTime = readTime;
    this.latencyPerStage = latencyPerStage;

    this.executionStartTime = readTime + 1;

    int executionDuration = numStages * latencyPerStage;

    this.writeTime = executionStartTime + executionDuration;
  }

  public int getTotalFinishTime() {
    return writeTime + 1;
  }
}