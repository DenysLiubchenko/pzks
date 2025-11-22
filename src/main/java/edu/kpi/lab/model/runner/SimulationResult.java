package edu.kpi.lab.model.runner;

import lombok.Data;

@Data
public class SimulationResult {
  private int sequentialTime;
  private int parallelTime;
  private double speedup;
  private int activeProcessors;
  private int totalProcessors;
  private double efficiencyActive;
  private double efficiencyTotal;

  public void print() {
    System.out.println("=== Результати симуляції ===");
    System.out.println("Послідовний час виконання: " + sequentialTime + " тактів");
    System.out.println("Паралельний час виконання: " + parallelTime + " тактів");
    System.out.printf("Коефіцієнт прискорення: %.2f\n", speedup);
    System.out.println("Кількість активних процесорів: " + activeProcessors);
    System.out.println("Загальна кількість процесорів: " + totalProcessors);
    System.out.printf("Ефективність (активні процесори): %.2f%%\n",
      efficiencyActive * 100);
    System.out.printf("Ефективність (всі процесори): %.2f%%\n",
      efficiencyTotal * 100);
  }
}
