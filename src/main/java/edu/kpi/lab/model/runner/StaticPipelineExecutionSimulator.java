package edu.kpi.lab.model.runner;

import edu.kpi.lab.model.syntax.tree.Function;
import edu.kpi.lab.model.syntax.tree.Node;
import edu.kpi.lab.model.syntax.tree.Operand;
import edu.kpi.lab.model.syntax.tree.SyntaxType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StaticPipelineExecutionSimulator {

  private static final int NUM_STAGES = 6;
  private static final Map<SyntaxType, Integer> LATENCY_PER_STAGE = Map.of(
    SyntaxType.OPERATION_ADD, 1,
    SyntaxType.OPERATION_MINUS, 1,
    SyntaxType.OPERATION_MULTIPLY, 2,
    SyntaxType.OPERATION_DIVIDE, 3
  );

  private final Function tree;
  private final Map<Node, Set<Node>> nodeDependencies;
  private final Map<Node, Integer> nodeFinishTime;
  private final List<PipelineTask> executionSchedule;

  private final Set<Integer> busBusyTime;

  private SyntaxType currentPipelineType;
  private int pipelineDrainTime;
  private int lastReadTime;

  public StaticPipelineExecutionSimulator(Function tree) {
    this.tree = tree;
    this.nodeDependencies = new HashMap<>();
    this.nodeFinishTime = new HashMap<>();
    this.executionSchedule = new ArrayList<>();
    this.busBusyTime = new HashSet<>();

    this.currentPipelineType = null;
    this.pipelineDrainTime = 0;
    this.lastReadTime = -100;
  }

  public SimulationResult simulate() {
    buildDependencyGraph();
    scheduleExecution();

    int parallelTime = getMaxFinishTime();
    int sequentialTime = calculateSequentialTime();

    SimulationResult result = new SimulationResult();
    result.setSequentialTime(sequentialTime);
    result.setParallelTime(parallelTime);
    result.setSpeedup(parallelTime > 0 ? (double) sequentialTime / parallelTime : 0);
    result.setActiveProcessors(NUM_STAGES);
    result.setTotalProcessors(NUM_STAGES);
    result.setEfficiencyActive(result.getSpeedup() / NUM_STAGES);
    result.setEfficiencyTotal(result.getSpeedup() / NUM_STAGES);
    return result;
  }

  private void buildDependencyGraph() {
    collectDependencies(tree);
  }

  private Set<Node> collectDependencies(Node node) {
    if (node == null) {
      return new HashSet<>();
    }
    if (node instanceof Operand) {
      nodeFinishTime.put(node, 0);
      return new HashSet<>();
    }
    if (node instanceof Function f) {
      Set<Node> dependencies = new HashSet<>();
      dependencies.addAll(collectDependencies(f.getLeft()));
      if (f.getLeft() instanceof Function) {
        dependencies.add(f.getLeft());
      }
      dependencies.addAll(collectDependencies(f.getRight()));
      if (f.getRight() instanceof Function) {
        dependencies.add(f.getRight());
      }
      nodeDependencies.put(node, dependencies);
      Set<Node> allSubtree = new HashSet<>(dependencies);
      allSubtree.add(node);
      return allSubtree;
    }
    return new HashSet<>();
  }

  private void scheduleExecution() {
    System.out.println("--- Планування ---");

    List<Function> allOperations = new ArrayList<>(nodeDependencies.keySet().stream()
      .map(n -> (Function) n)
      .toList());

    Set<Node> scheduledNodes = new HashSet<>();
    int currentTime = 0;
    int opCounter = 1;
    Map<Node, String> nodeShortIds = new HashMap<>();

    while (scheduledNodes.size() < allOperations.size()) {

      int finalCurrentTime = currentTime;
      List<Function> readyCandidates = allOperations.stream()
        .filter(op -> !scheduledNodes.contains(op))
        .filter(op -> {
          Set<Node> deps = nodeDependencies.get(op);
          return deps.stream()
            .allMatch(d -> nodeFinishTime.containsKey(d) && nodeFinishTime.get(d) <= finalCurrentTime);
        })
        .sorted((f1, f2) -> {
          boolean f1Same = f1.getOperation() == currentPipelineType;
          boolean f2Same = f2.getOperation() == currentPipelineType;
          if (f1Same && !f2Same) {
            return -1;
          }
          return 0;
        })
        .toList();

      Function bestCandidate = null;

      for (Function candidate : readyCandidates) {
        SyntaxType type = candidate.getOperation();
        int latency = LATENCY_PER_STAGE.get(type);

        boolean canStartPipeline;

        if (currentPipelineType == null) {
          canStartPipeline = true;
        } else if (currentPipelineType == type) {
          canStartPipeline = (currentTime >= lastReadTime + latency);
        } else {
          canStartPipeline = (currentTime > pipelineDrainTime);
        }

        if (!canStartPipeline) {
          continue;
        }

        int totalExecutionTime = latency * NUM_STAGES;
        int potentialWriteTime = currentTime + 1 + totalExecutionTime;
        if (!busBusyTime.contains(currentTime) && !busBusyTime.contains(potentialWriteTime)) {
          bestCandidate = candidate;
          break;
        }
      }

      if (bestCandidate != null) {
        String name = bestCandidate.getName();
        SyntaxType type = bestCandidate.getOperation();
        int latency = LATENCY_PER_STAGE.get(type);

        if (currentPipelineType != type) {
          currentPipelineType = type;
        }

        String shortId = String.valueOf(opCounter++);
        nodeShortIds.put(bestCandidate, shortId);

        PipelineTask task = new PipelineTask(
          bestCandidate, shortId, type,
          currentTime, latency, NUM_STAGES
        );
        executionSchedule.add(task);
        scheduledNodes.add(bestCandidate);

        busBusyTime.add(task.getReadTime());
        busBusyTime.add(task.getWriteTime());

        nodeFinishTime.put(bestCandidate, task.getTotalFinishTime());
        pipelineDrainTime = task.getWriteTime();
        lastReadTime = currentTime;

        System.out.println("Scheduled [" + shortId + "] " + name + " at T=" + currentTime);
      }

      currentTime++;
      if (currentTime > 2000) {
        break;
      }
    }
  }

  private int calculateSequentialTime() {
    return executionSchedule.stream()
      .mapToInt(t -> 1 + (LATENCY_PER_STAGE.get(t.getOperation()) * NUM_STAGES) + 1)
      .sum();
  }

  private int getMaxFinishTime() {
    return executionSchedule.stream()
      .mapToInt(PipelineTask::getTotalFinishTime)
      .max()
      .orElse(0);
  }

  public void printGanttChart() {
    System.out.println("\n=== Діаграма Ганта ===");
    int maxTime = getMaxFinishTime();

    System.out.print("      ");
    for (int t = 0; t <= maxTime; t++) {
      System.out.printf("%-3d", t);
    }
    System.out.println();
    System.out.println("-".repeat(6 + (maxTime + 1) * 3));

    String[] rows = new String[NUM_STAGES + 2];
    rows[0] = "READ ";
    for (int i = 1; i <= NUM_STAGES; i++) {
      rows[i] = "S" + i + "   ";
    }
    rows[NUM_STAGES + 1] = "WRITE";

    for (int r = 0; r < rows.length; r++) {
      System.out.print(rows[r] + " |");

      for (int t = 0; t <= maxTime; t++) {
        String cell = getCell(r, t);
        System.out.print(cell);
      }
      System.out.println();
    }
    System.out.println("-".repeat(6 + (maxTime + 1) * 3));
  }

  private String getCell(int stageIndex, int t) {
    String cell = "   ";
    for (PipelineTask task : executionSchedule) {
      String id = "[" + task.getNodeIdShort() + "]";

      if (stageIndex == 0) {
        if (task.getReadTime() == t) {
          cell = String.format("%-3s", id);
        }
      } else if (stageIndex == NUM_STAGES + 1) {
        if (task.getWriteTime() == t) {
          cell = String.format("%-3s", id);
        }
      } else {
        int latency = task.getLatencyPerStage();

        int enterStageTime = task.getExecutionStartTime() + (stageIndex - 1) * latency;
        int exitStageTime = enterStageTime + latency;

        if (t >= enterStageTime && t < exitStageTime) {
          cell = String.format("%-3s", id);
        }
      }
    }
    return cell;
  }
}