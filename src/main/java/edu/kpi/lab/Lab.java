package edu.kpi.lab;

import edu.kpi.lab.model.lexical.LexicalAnalyzer;
import edu.kpi.lab.model.lexical.Token;
import edu.kpi.lab.model.runner.SimulationResult;
import edu.kpi.lab.model.runner.StaticPipelineExecutionSimulator;
import edu.kpi.lab.model.syntax.SyntaxAnalyzer;
import edu.kpi.lab.model.syntax.SyntaxValidator;
import edu.kpi.lab.model.syntax.tree.Function;
import java.util.List;

public class Lab {
  public static void main(String[] args) {
    LexicalAnalyzer la = new LexicalAnalyzer();
    SyntaxValidator sv = new SyntaxValidator();
    SyntaxAnalyzer sa = new SyntaxAnalyzer();

    List<String> expressions = List.of(
      "(a+b+c/d+d-e-g*h+i/j)+(a+b+c/d+d-e-g*h+i/j)",
      "a+b+c/d+d-e-g*h+i/j",
      "a*b+a*c",
      "a*b+a*c+b*c",
      "a*(b-2)+c*(b-2)"
    );

    for (String expression : expressions) {
      List<Token> tokens = la.processMathSentence(expression);
      System.out.println("\nExpression: " + expression);
      System.out.println("Tokens: " + tokens);

      boolean isQueryCorrect = sv.validateTokenQuery(tokens);

      if (isQueryCorrect) {

        System.out.println("\n----------------------------------------\n");
        Function syntaxTree = sa.buildSyntaxTree(tokens);
        syntaxTree.printTreeStructure();
        StaticPipelineExecutionSimulator ps = new StaticPipelineExecutionSimulator(syntaxTree);
        SimulationResult simulate = ps.simulate();
        ps.printGanttChart();
        simulate.print();
      }

      System.out.println("\n----------------------------------------\n");
    }
  }
}