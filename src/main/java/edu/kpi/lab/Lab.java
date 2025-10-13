package edu.kpi.lab;

import edu.kpi.lab.model.lexical.LexicalAnalyzer;
import edu.kpi.lab.model.lexical.Token;
import edu.kpi.lab.model.syntax.SyntaxAnalyzer;
import edu.kpi.lab.model.syntax.SyntaxValidator;
import edu.kpi.lab.model.transformation.BracketExpander;
import java.util.List;

public class Lab {
  public static void main(String[] args) {
    LexicalAnalyzer la = new LexicalAnalyzer();
    SyntaxValidator sv = new SyntaxValidator();
    SyntaxAnalyzer sa = new SyntaxAnalyzer();
    BracketExpander expander = new BracketExpander();

    List<String> expressions = List.of(
      "x+y*(m-n)-(p-q)*(r-2.5-s)-(t-u)/(v+w-z)",
      "a*b+c/(d-e)-f*(g+h)-i+j",
      "w-x*(y+z)-(a/b)*(c-d)-(e+f)/(g-h+i)",
      "k-l*(m+n)-(o-p+2-4*(8-f))*(q-(3.7-r))-(s*t)/(u-v+w)"
    );

    System.out.println("Building parallel syntax trees and expanding brackets:");

    for (String expression : expressions) {
      List<Token> tokens = la.processMathSentence(expression);
      System.out.println("\nExpression: " + expression);
      System.out.println("Tokens: " + tokens);

      boolean isQueryCorrect = sv.validateTokenQuery(tokens);

      if (isQueryCorrect) {
//        Function tree = sa.buildSyntaxTree(tokens);
//        tree.printTreeStructure();

        System.out.println("\n=== Bracket Expansion ===");

        System.out.println("\nResults below:");
        System.out.println("Final: " + expander.generateEquivalents(tokens));
      }

      System.out.println("\n----------------------------------------\n");
    }
  }
}