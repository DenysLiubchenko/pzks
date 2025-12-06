package edu.kpi.lab.model.runner;

import lombok.Data;

@Data
public class SimulationResult {
  private int sequentialTime;
  private int parallelTime;
  private double speedup;
  private int totalProcessors;
  private double efficiency;

  public void print() {
    System.out.println("=== Результати симуляції ===");
    System.out.println("Послідовний час виконання: " + sequentialTime + " тактів");
    System.out.println("Паралельний час виконання: " + parallelTime + " тактів");
    System.out.printf("Коефіцієнт прискорення: %.2f\n", speedup);
    System.out.println("Загальна кількість процесорів: " + totalProcessors);
    System.out.printf("Ефективність: %.2f%%\n",
      efficiency * 100);
  }
}
