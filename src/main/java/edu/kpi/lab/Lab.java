package edu.kpi.lab;

import edu.kpi.lab.model.transform.associative.AssociativeTransformer;
import edu.kpi.lab.model.transform.commutative.CommutativeTransformer;
import edu.kpi.lab.model.lexical.LexicalAnalyzer;
import edu.kpi.lab.model.lexical.Token;
import edu.kpi.lab.model.syntax.SyntaxAnalyzer;
import edu.kpi.lab.model.syntax.SyntaxValidator;
import java.util.List;

public class Lab {
  public static void main(String[] args) {
    LexicalAnalyzer la = new LexicalAnalyzer();
    SyntaxValidator sv = new SyntaxValidator();
    SyntaxAnalyzer sa = new SyntaxAnalyzer();

    CommutativeTransformer commutativeTransformer = new CommutativeTransformer();
    AssociativeTransformer associativeTransformer = new AssociativeTransformer();

    List<String> expressions = List.of(
      "a+b*c+d", //b*c + a + d
      "a*b+a*c", //a*(b+c)
      "a*b+a*c+b*c",
      "a*(b-2)+c*(b-2)",
      "a*b-2*a+b*c-c*2",
      "a/b-c/b+2/b",
      "a/(b-1)-c/(b-1)+2/(b-1)-t",
      "a-b*k+b*t-f*f*5.9+f*q+g*f*5.9-g*q-f/(d+q-w)-g/(d+q-w)",
      "a-b*(k-t+(f-g)*(f*5.9-q)+(w-y*(m-1))/p)-(x-3)*(x+3)/(d+q-w)"
    );

    System.out.println("Building parallel syntax trees and expanding brackets:");

    for (String expression : expressions) {
      List<Token> tokens = la.processMathSentence(expression);
      System.out.println("\nExpression: " + expression);
      System.out.println("Tokens: " + tokens);

      boolean isQueryCorrect = sv.validateTokenQuery(tokens);

      if (isQueryCorrect) {
        System.out.println("Equivalences Commutative:");
        commutativeTransformer.generateEquivalentExpressions(tokens).forEach(System.out::println);
        System.out.println("Equivalences Associative:");
        associativeTransformer.generateEquivalentExpressions(tokens).forEach(System.out::println);
      }

      System.out.println("\n----------------------------------------\n");
    }
  }
}