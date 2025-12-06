package edu.kpi.lab;

import edu.kpi.lab.model.lexical.LexicalAnalyzer;
import edu.kpi.lab.model.lexical.Token;
import edu.kpi.lab.model.runner.SimulationResult;
import edu.kpi.lab.model.runner.StaticPipelineExecutionSimulator;
import edu.kpi.lab.model.syntax.SyntaxAnalyzer;
import edu.kpi.lab.model.syntax.SyntaxValidator;
import edu.kpi.lab.model.syntax.tree.Function;
import edu.kpi.lab.model.transform.associative.AssociativeTransformer;
import edu.kpi.lab.model.transform.commutative.CommutativeTransformer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Lab {
  public static void main(String[] args) {
    LexicalAnalyzer la = new LexicalAnalyzer();
    SyntaxValidator sv = new SyntaxValidator();
    SyntaxAnalyzer sa = new SyntaxAnalyzer();
    AssociativeTransformer at = new AssociativeTransformer();
    CommutativeTransformer ct = new CommutativeTransformer();

    List<String> expressions = List.of(
      "a+b+c/d+d-e-g*h+i/j",
      "a*b+a*c+b*c",
      "a*(b-2)+c*(b-2)"
    );

    for (String expression : expressions) {
      List<Token> startTokens = la.processMathSentence(expression);
      System.out.println("\nExpression: " + expression);
      System.out.println("Tokens: " + startTokens);

      boolean isQueryCorrect = sv.validateTokenQuery(startTokens);

      if (isQueryCorrect) {
        System.out.println("\n----------------------------------------\n");

        Set<String> allExpressionVariants = new HashSet<>();
        allExpressionVariants.add(expression);
        allExpressionVariants.addAll(at.generateEquivalentExpressions(startTokens));
        allExpressionVariants.addAll(ct.generateEquivalentExpressions(startTokens));

        Set<String> bestVariants = new HashSet<>();
        SimulationResult bestResult = null;
        for (String variant : allExpressionVariants) {
          System.out.println(variant);
          List<Token> tokens = la.processMathSentence(variant);
          Function syntaxTree = sa.buildSyntaxTree(tokens);
          StaticPipelineExecutionSimulator ps = new StaticPipelineExecutionSimulator(syntaxTree);
          SimulationResult simulationResult = ps.simulate();
//          ps.printGanttChart();
          simulationResult.print();
          System.out.println("\n----------------------------------------\n");

          if (bestResult == null || simulationResult.getParallelTime() < bestResult.getParallelTime()) {
            bestResult = simulationResult;
            bestVariants = new HashSet<>();
            bestVariants.add(variant);
          } else if (simulationResult.getParallelTime() == bestResult.getParallelTime()) {
            bestVariants.add(variant);
          }
        }

        System.out.println("The best variants are: " + bestVariants);
        System.out.println("With Results: ");
        bestResult.print();
      }
    }
  }
}