package edu.kpi.lab.lab1;

import edu.kpi.lab.lab1.model.LexicalAnalyzer;
import edu.kpi.lab.lab1.model.SyntaxAnalyzer;
import edu.kpi.lab.lab1.model.Token;
import java.util.List;

public class Lab1 {
  public static void main(String[] args) {
    LexicalAnalyzer la = new LexicalAnalyzer();
    SyntaxAnalyzer sa = new SyntaxAnalyzer();
    List<String> correctExpressions = List.of(
        "5+3",
        "(2-1)*7",
        "CONST+func(2.5)",
        "round(12.41-(func(22*2)-21))+((8/2)-(-6))",
        "a+var-(4/func())",
        "((10.15+4)*(3-1))",
        "0.157-(2*(-5))",
        "((-120.024)*(-3)+9)",
        "((6+(-4))/2)",
        "pi()+e"
    );

    List<String> errorExpressions = List.of(
        // Start errors
        ")+5*3",           // starts with close bracket
        "*2+3",            // starts with *
        "/7-2",            // starts with /
        "+",               // starts and ends with operator

        // End errors
        "5+3-",            // ends with operator
        "func(2.5)*",      // ends with operator
        "(7-2)/",          // ends with operator

        // Invalid names
        "const$+3",        // invalid character in variable name
        "func@()",         // invalid character in function name

        // Double operators
        "5++3",            // double +
        "7--2",            // double -
        "4**2",            // double *
        "8//2",            // double /

        // Operator placement errors
        "(*2+3)",          // * after open bracket
        "(5/)",            // / before close bracket
        "(7-)+2",          // - before close bracket

        // Bracket errors
        "((5+3)",          // missing close bracket
        "5+3))",           // extra close bracket
        "(()5+3)",         // empty brackets
        "5+)(3-2)",        // wrong bracket order
        "((5+3)*(-2))+CONST)", // extra close bracket at end
        "((5+3)*(-2))+((CONST)" // missing close bracket
    );

    System.out.println("Correct expressions: ");
    correctExpressions.forEach(expression -> {
      List<Token> query = la.processMathSentence(expression);
      System.out.println(expression);
      System.out.println(query);
      sa.processTokenQuery(query);
      System.out.println();
    });

    System.out.println("Expressions with errors: ");
    errorExpressions.forEach(expression -> {
      List<Token> query = la.processMathSentence(expression);
      System.out.println(expression);
      System.out.println(query);
      sa.processTokenQuery(query);
      System.out.println();
    });
  }
}
