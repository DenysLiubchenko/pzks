package edu.kpi.lab;

import edu.kpi.lab.model.lexical.LexicalAnalyzer;
import edu.kpi.lab.model.syntax.SyntaxAnalyzer;
import edu.kpi.lab.model.lexical.Token;
import edu.kpi.lab.model.syntax.tree.Function;
import java.util.List;

public class Lab {
  public static void main(String[] args) {
    LexicalAnalyzer la = new LexicalAnalyzer();
    SyntaxAnalyzer sa = new SyntaxAnalyzer();
    List<String> expressions = List.of(
      "a*b+d1+c*d+e*f+g+h",
      "(a+b+c)+d+((e+f+g)+h)",
      "a+b+c+d+e+f+g+h"
    );

    System.out.println("Building parallel syntax trees for expressions:");
    for (String expression : expressions) {
      List<Token> tokens = la.processMathSentence(expression);
      System.out.println("\nExpression: " + expression);
      System.out.println("Tokens: " + tokens);
      sa.processTokenQuery(tokens);

      // Build the parallel tree
      Function tree = sa.buildSyntaxTree(tokens);

      // Print the tree structure
      System.out.println("\nParallel Tree Structure:");
      sa.printTreeStructure(tree);

      System.out.println("\n----------------------------------------\n");
    }
  }
}
