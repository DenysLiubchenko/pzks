package edu.kpi.lab.model.runner;

import lombok.Data;

@Data
class ProcessorState {
  private int processorId;
  private int nextAvailableTime;

  ProcessorState(int id, int time) {
    this.processorId = id;
    this.nextAvailableTime = time;
  }
}