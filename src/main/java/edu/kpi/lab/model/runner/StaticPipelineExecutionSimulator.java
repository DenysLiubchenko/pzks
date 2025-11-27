package edu.kpi.lab.model.runner;

import edu.kpi.lab.model.syntax.tree.Function;
import edu.kpi.lab.model.syntax.tree.Node;
import edu.kpi.lab.model.syntax.tree.Operand;
import edu.kpi.lab.model.syntax.tree.SyntaxType;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

public class StaticPipelineExecutionSimulator {

  private static final int NUM_STAGES = 6;

  private static final Map<SyntaxType, Integer> OPERATION_TIME = Map.of(
    SyntaxType.OPERATION_ADD, 1,
    SyntaxType.OPERATION_MINUS, 1,
    SyntaxType.OPERATION_MULTIPLY, 2,
    SyntaxType.OPERATION_DIVIDE, 3
  );

  private Function tree;
  private Map<Node, Set<Node>> nodeDependencies;
  private Map<Node, Integer> nodeFinishTime;
  private List<Node> executionOrder;
  private List<PipelineTask> executionSchedule;

  private int[] stageAvailableTime;
  private int readWriteAvailableTime;

  public StaticPipelineExecutionSimulator(Function tree) {
    this.tree = tree;
    this.nodeDependencies = new HashMap<>();
    this.nodeFinishTime = new HashMap<>();
    this.executionOrder = new ArrayList<>();
    this.executionSchedule = new ArrayList<>();
    this.stageAvailableTime = new int[NUM_STAGES];
    this.readWriteAvailableTime = 0;
  }

  public SimulationResult simulate() {
    System.out.println("=== Статичний конвеєр ===");
    System.out.println("Кількість шарів: " + NUM_STAGES);
    System.out.println();

    printOperationTimes();

    buildDependencyGraph();
    buildExecutionOrder();
    scheduleExecution();

    int parallelTime = getMaxFinishTime();
    int sequentialTime = calculateSequentialTime();

    SimulationResult result = new SimulationResult();
    result.setSequentialTime(sequentialTime);
    result.setParallelTime(parallelTime);
    result.setSpeedup((double) sequentialTime / parallelTime);
    result.setActiveProcessors(NUM_STAGES);
    result.setTotalProcessors(NUM_STAGES);
    result.setEfficiencyActive(result.getSpeedup() / NUM_STAGES);
    result.setEfficiencyTotal(result.getSpeedup() / NUM_STAGES);

    return result;
  }

  private void printOperationTimes() {
    System.out.println("--- Час виконання ---");
    System.out.println("Додавання/Віднімання: 1 такт × 6 шарів = 6 тактів");
    System.out.println("Множення: 2 такти × 6 шарів = 12 тактів");
    System.out.println("Ділення: 3 такти × 6 шарів = 18 тактів");
    System.out.println("Read/Write: 1 такт");
    System.out.println();
  }

  private void buildDependencyGraph() {
    collectDependencies(tree);
  }

  private Set<Node> collectDependencies(Node node) {
    if (node == null) {
      return new HashSet<>();
    }

    if (node instanceof Operand) {
      nodeDependencies.put(node, new HashSet<>());
      nodeFinishTime.put(node, 0);
      return new HashSet<>();
    }

    if (node instanceof Function f) {
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

    return new HashSet<>();
  }

  private void buildExecutionOrder() {
    System.out.println("--- Порядок виконання ---");

    List<Node> allNodes = new ArrayList<>();
    collectAllNodes(tree, allNodes);

    List<Function> allOperations = allNodes.stream()
      .filter(n -> n instanceof Function)
      .map(n -> (Function) n)
      .collect(Collectors.toList());

    allOperations.sort(Comparator.comparingInt(f -> nodeDependencies.get(f).size()));

    executionOrder.addAll(allOperations);

    for (int i = 0; i < executionOrder.size(); i++) {
      Function f = (Function) executionOrder.get(i);
      System.out.println((i + 1) + ". " + f.getOperation() +
                         " (залежностей: " + nodeDependencies.get(f).size() + ")");
    }
    System.out.println();
  }

  private void collectAllNodes(Node node, List<Node> result) {
    if (node == null) return;
    result.add(node);
    if (node instanceof Function f) {
      collectAllNodes(f.getLeft(), result);
      collectAllNodes(f.getRight(), result);
    }
  }

  private void scheduleExecution() {
    System.out.println("--- Виконання ---");

    // Групуємо операції за типом, зберігаючи порядок
    Map<SyntaxType, List<Function>> operationsByType = new LinkedHashMap<>();
    for (Node node : executionOrder) {
      if (node instanceof Function f) {
        operationsByType.computeIfAbsent(f.getOperation(), k -> new ArrayList<>()).add(f);
      }
    }

    int allStagesFreeTime = 0; // Коли всі шари звільнилися

    // Для кожного типу операції виконуємо конвеєрно
    for (Map.Entry<SyntaxType, List<Function>> entry : operationsByType.entrySet()) {
      SyntaxType opType = entry.getKey();
      List<Function> operations = entry.getValue();
      int operationDuration = OPERATION_TIME.get(opType);

      // Початок обробки нового типу - чекаємо поки всі шари звільняться
      Arrays.fill(stageAvailableTime, allStagesFreeTime);

      for (Function operation : operations) {
        int dependenciesReadyTime = getDependenciesReadyTime(operation);

        // READ - глобальна блокування
        int readStart = Math.max(dependenciesReadyTime, readWriteAvailableTime);
        int readFinish = readStart + 1;

        PipelineTask readTask = new PipelineTask(-1, operation, null, readStart, readFinish);
        readTask.setStage("READ");
        executionSchedule.add(readTask);

        // Шари - конвеєрна обробка для операцій одного типу
        int prevStageFinish = readFinish;

        for (int stage = 0; stage < NUM_STAGES; stage++) {
          int stageStart = Math.max(prevStageFinish, stageAvailableTime[stage]);
          int stageFinish = stageStart + operationDuration;

          stageAvailableTime[stage] = stageFinish;
          prevStageFinish = stageFinish;

          PipelineTask stageTask = new PipelineTask(stage, operation, opType, stageStart, stageFinish);
          stageTask.setStage("S" + (stage + 1));
          stageTask.setActive(true);
          executionSchedule.add(stageTask);
        }

        // WRITE - глобальна блокування
        int writeStart = Math.max(prevStageFinish, readWriteAvailableTime);
        int writeFinish = writeStart + 1;
        readWriteAvailableTime = writeFinish;

        PipelineTask writeTask = new PipelineTask(-2, operation, null, writeStart, writeFinish);
        writeTask.setStage("WRITE");
        executionSchedule.add(writeTask);

        nodeFinishTime.put(operation, writeFinish);

        System.out.println(opType + ": READ[" + readStart + "] -> S1-S6[" +
                           readFinish + "-" + prevStageFinish + "] -> WRITE[" + writeStart + "]");
      }

      // Знаходимо коли всі шари звільнилися після цього типу
      allStagesFreeTime = Arrays.stream(stageAvailableTime).max().orElse(0);
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
      .mapToInt(PipelineTask::getFinishTime)
      .max()
      .orElse(0);
  }

  private int calculateSequentialTime() {
    int total = 0;
    for (Node node : executionOrder) {
      if (node instanceof Function f) {
        int opTime = OPERATION_TIME.get(f.getOperation());
        total += 1 + opTime + 1;
      }
    }
    return total;
  }

  public void printGanttChart() {
    System.out.println("=== Діаграма Ганта ===");

    int maxTime = getMaxFinishTime();

    Map<String, List<PipelineTask>> tasksByStage = new HashMap<>();
    tasksByStage.put("READ", new ArrayList<>());
    for (int i = 1; i <= NUM_STAGES; i++) {
      tasksByStage.put("S" + i, new ArrayList<>());
    }
    tasksByStage.put("WRITE", new ArrayList<>());

    for (PipelineTask task : executionSchedule) {
      tasksByStage.get(task.getStage()).add(task);
    }

    System.out.print("       ");
    for (int t = 0; t <= maxTime; t++) {
      System.out.printf("%3d", t);
    }
    System.out.println();
    System.out.println("       " + "---".repeat(maxTime + 1));

    printStageLine("READ", tasksByStage.get("READ"), maxTime);

    for (int i = 1; i <= NUM_STAGES; i++) {
      printStageLine("S" + i, tasksByStage.get("S" + i), maxTime);
    }

    printStageLine("WRITE", tasksByStage.get("WRITE"), maxTime);

    System.out.println("       " + "---".repeat(maxTime + 1));
    System.out.println();

    System.out.println("Операції:");
    int opNum = 1;
    for (Node node : executionOrder) {
      if (node instanceof Function f) {
        System.out.println("[" + opNum + "] " + f.getOperation());
        opNum++;
      }
    }
    System.out.println();
  }

  private void printStageLine(String stageName, List<PipelineTask> tasks, int maxTime) {
    System.out.printf("%-6s|", stageName);

    tasks.sort(Comparator.comparingInt(PipelineTask::getStartTime));

    int currentTime = 0;
    for (PipelineTask task : tasks) {
      while (currentTime < task.getStartTime()) {
        System.out.print("   ");
        currentTime++;
      }
      while (currentTime < task.getFinishTime()) {
        int opIndex = executionOrder.indexOf(task.getNode()) + 1;
        System.out.printf("[%d]", opIndex);
        currentTime++;
      }
    }

    while (currentTime <= maxTime) {
      System.out.print("   ");
      currentTime++;
    }

    System.out.println("|");
  }
}