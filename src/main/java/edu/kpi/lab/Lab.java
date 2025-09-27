package edu.kpi.lab;

import edu.kpi.lab.model.lexical.LexicalAnalyzer;
import edu.kpi.lab.model.lexical.Token;
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
      "-a*b+d1+c*d+e*f+g+h",
      "-(a+b+c)-d+((e+f+g)*h)",
      "a+b+c+d+e+f+g+h",
      "a+b+0",
      "a+1*b",
      "a+b/1",
      "a+b+c*0",
      "a+1+2+3+4*2-4/10"
    );

    System.out.println("Building parallel syntax trees for expressions:");
    for (String expression : expressions) {
      List<Token> tokens = la.processMathSentence(expression);
      System.out.println("\nExpression: " + expression);
      System.out.println("Tokens: " + tokens);
      boolean isQueryCorrect = sv.validateTokenQuery(tokens);
      if (isQueryCorrect) {
        Function tree = sa.buildSyntaxTree(tokens);

        tree.printTreeStructure();
      }
      System.out.println("\n----------------------------------------\n");
    }
  }
}
