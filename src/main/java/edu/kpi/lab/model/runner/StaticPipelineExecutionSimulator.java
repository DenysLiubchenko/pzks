package edu.kpi.lab.model.runner;

import edu.kpi.lab.model.syntax.tree.Function;
import edu.kpi.lab.model.syntax.tree.Node;
import edu.kpi.lab.model.syntax.tree.Operand;
import edu.kpi.lab.model.syntax.tree.SyntaxType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

public class StaticPipelineExecutionSimulator {

  private static final int NUM_PROCESSORS = 6;

  private static final Map<SyntaxType, Integer> OPERATION_TIME = Map.of(
    SyntaxType.OPERATION_ADD, 1,
    SyntaxType.OPERATION_MINUS, 1,
    SyntaxType.OPERATION_MULTIPLY, 2,
    SyntaxType.OPERATION_DIVIDE, 3
  );

  private Map<SyntaxType, List<Integer>> operationProcessors;

  private Function tree;
  private Map<Node, Set<Node>> nodeDependencies;
  private Map<Node, Integer> nodeStartTime;
  private Map<Node, Integer> nodeFinishTime;
  private List<PipelineTask> executionSchedule;
  private List<Node> executionOrder;

  public StaticPipelineExecutionSimulator(Function tree) {
    this.tree = tree;
    this.nodeDependencies = new HashMap<>();
    this.nodeStartTime = new HashMap<>();
    this.nodeFinishTime = new HashMap<>();
    this.executionSchedule = new ArrayList<>();
    this.executionOrder = new ArrayList<>();

    initializeProcessorAssignment();
  }

  private void initializeProcessorAssignment() {
    operationProcessors = new HashMap<>();
    operationProcessors.put(SyntaxType.OPERATION_ADD, Arrays.asList(0));
    operationProcessors.put(SyntaxType.OPERATION_MINUS, Arrays.asList(1));
    operationProcessors.put(SyntaxType.OPERATION_MULTIPLY, Arrays.asList(2, 3));
    operationProcessors.put(SyntaxType.OPERATION_DIVIDE, Arrays.asList(4, 5));
  }

  public SimulationResult simulate() {
    printProcessorAssignment();

    buildDependencyGraph();

    buildExecutionOrder();

    scheduleExecution();

    int parallelTime = getMaxFinishTime();
    int sequentialTime = calculateSequentialTime();

    SimulationResult result = new SimulationResult();
    result.setSequentialTime(sequentialTime);
    result.setParallelTime(parallelTime);
    result.setSpeedup((double) sequentialTime / parallelTime);
    result.setActiveProcessors(calculateActiveProcessors());
    result.setTotalProcessors(NUM_PROCESSORS);
    result.setEfficiencyActive(result.getSpeedup() / result.getActiveProcessors());
    result.setEfficiencyTotal(result.getSpeedup() / result.getTotalProcessors());

    return result;
  }

  private void printProcessorAssignment() {
    System.out.println("--- Призначення процесорів ---");
    for (Map.Entry<SyntaxType, List<Integer>> entry : operationProcessors.entrySet()) {
      String opName = getOperationName(entry.getKey());
      String processors = entry.getValue().stream()
        .map(p -> "P" + p)
        .collect(Collectors.joining(", "));
      int time = OPERATION_TIME.get(entry.getKey());
      System.out.println("  " + opName + " (час: " + time + "): " + processors);
    }
    System.out.println();
  }

  private String getOperationName(SyntaxType type) {
    return switch (type) {
      case OPERATION_ADD -> "Додавання (+)";
      case OPERATION_MINUS -> "Віднімання (-)";
      case OPERATION_MULTIPLY -> "Множення (*)";
      case OPERATION_DIVIDE -> "Ділення (/)";
      default -> type.toString();
    };
  }

  private void buildDependencyGraph() {
    collectDependencies(tree);
  }

  private Set<Node> collectDependencies(Node node) {
    switch (node) {
      case null -> {
        return new HashSet<>();
      }
      case Operand operand -> {
        nodeDependencies.put(node, new HashSet<>());
        nodeStartTime.put(node, 0);
        nodeFinishTime.put(node, 0);
        return new HashSet<>();
      }
      case Function f -> {
        Set<Node> dependencies = new HashSet<>();

        Set<Node> leftDeps = collectDependencies(f.getLeft());
        dependencies.addAll(leftDeps);
        if (f.getLeft() instanceof Function) {
          dependencies.add(f.getLeft());
        }

        Set<Node> rightDeps = collectDependencies(f.getRight());
        dependencies.addAll(rightDeps);
        if (f.getRight() instanceof Function) {
          dependencies.add(f.getRight());
        }

        nodeDependencies.put(node, dependencies);

        Set<Node> allNodesInSubtree = new HashSet<>(dependencies);
        allNodesInSubtree.add(node);

        return allNodesInSubtree;
      }
      default -> {
      }
    }

    return new HashSet<>();
  }

  private void buildExecutionOrder() {
    System.out.println("--- Визначення порядку виконання ---");
    System.out.println();

    List<Node> allNodes = new ArrayList<>();
    collectAllNodes(tree, allNodes);

    List<Function> allOperations = allNodes.stream()
      .filter(n -> n instanceof Function)
      .map(n -> (Function) n)
      .toList();

    allOperations = new ArrayList<>(allOperations);
    allOperations.sort(Comparator.comparingInt(f -> nodeDependencies.get(f).size()));

    executionOrder.addAll(allOperations);

    System.out.println("Порядок виконання операцій:");
    for (int i = 0; i < executionOrder.size(); i++) {
      Function f = (Function) executionOrder.get(i);
      System.out.println("  " + (i + 1) + ". " + f.getOperation() +
                         " (залежностей: " + nodeDependencies.get(f).size() + ")");
    }
    System.out.println();
  }

  private void collectAllNodes(Node node, List<Node> result) {
    if (node == null) {
      return;
    }
    result.add(node);
    if (node instanceof Function f) {
      collectAllNodes(f.getLeft(), result);
      collectAllNodes(f.getRight(), result);
    }
  }

  private void scheduleExecution() {
    System.out.println("--- Планування виконання ---");

    Map<Integer, Integer> processorNextAvailable = new HashMap<>();
    for (int i = 0; i < NUM_PROCESSORS; i++) {
      processorNextAvailable.put(i, 0);
    }

    Map<SyntaxType, PriorityQueue<ProcessorState>> processorQueues = new HashMap<>();
    for (Map.Entry<SyntaxType, List<Integer>> entry : operationProcessors.entrySet()) {
      PriorityQueue<ProcessorState> queue = new PriorityQueue<>(
        Comparator.comparingInt(ProcessorState::getNextAvailableTime)
      );
      for (int procId : entry.getValue()) {
        queue.offer(new ProcessorState(procId, 0));
      }
      processorQueues.put(entry.getKey(), queue);
    }

    for (Node node : executionOrder) {
      if (!(node instanceof Function operation)) {
        continue;
      }

      SyntaxType opType = operation.getOperation();
      int duration = OPERATION_TIME.get(opType);

      int dependenciesReadyTime = getDependenciesReadyTime(operation);

      PriorityQueue<ProcessorState> queue = processorQueues.get(opType);
      ProcessorState processor = queue.poll();

      int startTime = Math.max(dependenciesReadyTime, processor.getNextAvailableTime());
      int finishTime = startTime + duration;

      nodeStartTime.put(operation, startTime);
      nodeFinishTime.put(operation, finishTime);
      processor.setNextAvailableTime(finishTime);
      processorNextAvailable.put(processor.getProcessorId(), finishTime);
      queue.offer(processor);

      PipelineTask task = new PipelineTask(
        processor.getProcessorId(),
        operation,
        opType,
        startTime,
        finishTime
      );
      executionSchedule.add(task);

      System.out.println("P" + processor.getProcessorId() +
                         ": " + operation.getOperation() +
                         " [" + startTime + "-" + finishTime + "]");
    }
    System.out.println();
  }

  private int getDependenciesReadyTime(Function operation) {
    Set<Node> dependencies = nodeDependencies.get(operation);

    if (dependencies == null || dependencies.isEmpty()) {
      return 0;
    }

    int maxTime = 0;
    for (Node dep : dependencies) {
      int depFinishTime = nodeFinishTime.getOrDefault(dep, 0);
      maxTime = Math.max(maxTime, depFinishTime);
    }

    return maxTime;
  }

  private int getMaxFinishTime() {
    return executionSchedule.stream()
      .mapToInt(task -> task.getFinishTime())
      .max()
      .orElse(0);
  }

  private int calculateSequentialTime() {
    int total = executionSchedule.stream()
      .mapToInt(task -> task.getFinishTime() - task.getStartTime())
      .sum();
    System.out.println("Послідовний час виконання: " + total);
    return total;
  }

  private int calculateActiveProcessors() {
    Set<Integer> used = new HashSet<>();
    for (PipelineTask task : executionSchedule) {
      used.add(task.getProcessorId());
    }
    return used.size();
  }

  public void printGanttChart() {
    System.out.println("=== Діаграма Ганта (СТАТИЧНИЙ КОНВЕЄР) ===");

    int maxTime = getMaxFinishTime();

    Map<Integer, List<PipelineTask>> processorTasks = new HashMap<>();
    for (PipelineTask task : executionSchedule) {
      processorTasks.computeIfAbsent(task.getProcessorId(), k -> new ArrayList<>()).add(task);
    }

    System.out.print("      ");
    for (int t = 0; t <= maxTime; t++) {
      System.out.printf("%3d", t);
    }
    System.out.println();
    System.out.println("       " + "---".repeat(maxTime + 1));

    for (int p = 0; p < NUM_PROCESSORS; p++) {
      String opType = "";
      for (Map.Entry<SyntaxType, List<Integer>> entry : operationProcessors.entrySet()) {
        if (entry.getValue().contains(p)) {
          opType = switch (entry.getKey()) {
            case OPERATION_ADD -> "+";
            case OPERATION_MINUS -> "-";
            case OPERATION_MULTIPLY -> "*";
            case OPERATION_DIVIDE -> "/";
            default -> "?";
          };
          break;
        }
      }

      System.out.printf("P%-2d(%s)|", p, opType);

      List<PipelineTask> tasks = processorTasks.getOrDefault(p, new ArrayList<>());
      tasks.sort(Comparator.comparingInt(PipelineTask::getStartTime));

      int currentTime = 0;
      for (PipelineTask task : tasks) {
        while (currentTime < task.getStartTime()) {
          System.out.print("   ");
          currentTime++;
        }
        while (currentTime < task.getFinishTime()) {
          System.out.print(" ██");
          currentTime++;
        }
      }

      while (currentTime <= maxTime) {
        System.out.print("   ");
        currentTime++;
      }

      System.out.println("|");
    }

    System.out.println("       " + "---".repeat(maxTime + 1));
    System.out.println();
  }
}
