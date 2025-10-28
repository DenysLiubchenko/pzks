package edu.kpi.lab.model.transform.associative;

import edu.kpi.lab.model.transform.associative.node.BinaryExpr;
import edu.kpi.lab.model.transform.associative.node.Expr;
import edu.kpi.lab.model.transform.associative.node.FunctionExpr;
import edu.kpi.lab.model.transform.associative.node.Literal;
import edu.kpi.lab.model.transform.associative.node.Parser;
import edu.kpi.lab.model.transform.associative.node.UnaryExpr;
import edu.kpi.lab.model.lexical.Token;
import edu.kpi.lab.model.lexical.TokenType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AssociativeTransformer {

  public Set<String> generateEquivalentExpressions(List<Token> tokens) {
    Parser parser = new Parser(tokens);
    Expr initial = parser.parse();
    List<Expr> allExprs = getAllEquivalent(initial);
    Set<String> uniqueForms = new HashSet<>();
    for (Expr e : allExprs) {
      uniqueForms.add(e.toString());
    }
    return new HashSet<>(uniqueForms);
  }

  private List<Expr> getAllEquivalent(Expr expr) {
    List<Expr> results = new ArrayList<>();
    if (expr instanceof Literal) {
      results.add(expr);
    } else if (expr instanceof UnaryExpr u) {
      List<Expr> subs = getAllEquivalent(u.getOperand());
      for (Expr s : subs) {
        results.add(new UnaryExpr(u.getOp(), s));
      }
    } else if (expr instanceof FunctionExpr f) {
      List<Expr> subs = getAllEquivalent(f.getArg());
      for (Expr s : subs) {
        results.add(new FunctionExpr(f.getName(), s));
      }
    } else if (expr instanceof BinaryExpr b) {
      List<Expr> lefts = getAllEquivalent(b.getLeft());
      List<Expr> rights = getAllEquivalent(b.getRight());
      for (Expr l : lefts) {
        for (Expr r : rights) {
          results.add(new BinaryExpr(l, b.getOp(), r));
        }
      }
      String op = b.getOp();
      if (op.equals("+") || op.equals("-")) {
        List<Expr> terms = flattenAdd(expr);
        if (terms.size() > 2) {
          List<Expr> groupings = generateAll(terms, "+");
          results.addAll(groupings);
        }
        List<Expr> factored = getFactoredVariants(terms, "+");
        results.addAll(factored);
      } else
      if (op.equals("*") || op.equals("/")) {
        List<Expr> factors = flattenMul(expr);
        if (factors.size() > 2) {
          List<Expr> groupings = generateAll(factors, "*");
          results.addAll(groupings);
        }
      }
    }
    return results;
  }

  private List<Expr> getFactoredVariants(List<Expr> terms, String op) {
    if (!op.equals("+")) {
      return Collections.emptyList();
    }

    Map<Expr, List<Integer>> factorToIndices = new HashMap<>();
    for (int i = 0; i < terms.size(); i++) {
      Expr term = terms.get(i);
      if (term instanceof UnaryExpr u && u.getOp().equals("-")) {
        term = u.getOperand();
      }
      List<Expr> factors = flattenMul(term);
      for (Expr f : factors) {
        factorToIndices.computeIfAbsent(f, k -> new ArrayList<>()).add(i);
      }
    }

    List<Expr> variants = new ArrayList<>();
    for (Map.Entry<Expr, List<Integer>> entry : factorToIndices.entrySet()) {
      List<Integer> group = entry.getValue();
      if (group.size() < 2) {
        continue;
      }
      Expr common = entry.getKey();
      if (common instanceof Literal l && l.getValue().equals("1")) {
        continue;
      }

      Set<Integer> grouped = new HashSet<>(group);
      List<Expr> newTerms = new ArrayList<>();
      for (int i = 0; i < terms.size(); i++) {
        if (!grouped.contains(i)) {
          newTerms.add(terms.get(i));
        }
      }

      List<Expr> sumTerms = new ArrayList<>();
      for (int i : group) {
        Expr term = terms.get(i);
        Expr quotient = divide(term, common);
        sumTerms.add(quotient);
      }

      Expr sum = buildAdd(sumTerms);
      Expr factored = new BinaryExpr(common, "*", sum);
      newTerms.add(factored);

      Expr newExpr = buildAdd(newTerms);
      variants.add(newExpr);
    }
    return variants;
  }

  private Expr divide(Expr term, Expr divisor) {
    boolean negative = false;
    if (term instanceof UnaryExpr u && u.getOp().equals("-")) {
      negative = true;
      term = u.getOperand();
    }
    List<Expr> factors = flattenMul(term);
    int index = -1;
    for (int j = 0; j < factors.size(); j++) {
      if (factors.get(j).equals(divisor)) {
        index = j;
        break;
      }
    }
    if (index == -1) {
      return null;
    }
    factors.remove(index);
    Expr quotient = buildMul(factors);
    if (negative) {
      quotient = new UnaryExpr("-", quotient);
    }
    return quotient;
  }

  private Expr buildAdd(List<Expr> terms) {
    if (terms.isEmpty()) {
      return new Literal("0", TokenType.INTEGER);
    }
    Expr res = terms.getFirst();
    for (int i = 1; i < terms.size(); i++) {
      Expr right = terms.get(i);
      if (right instanceof UnaryExpr u && u.getOp().equals("-")) {
        res = new BinaryExpr(res, "-", u.getOperand());
      } else {
        res = new BinaryExpr(res, "+", right);
      }
    }
    return res;
  }

  private Expr buildMul(List<Expr> factors) {
    if (factors.isEmpty()) {
      return new Literal("1", TokenType.INTEGER);
    }
    Expr res = factors.getFirst();
    for (int i = 1; i < factors.size(); i++) {
      Expr right = factors.get(i);
      if (right instanceof UnaryExpr u && u.getOp().equals("/")) {
        res = new BinaryExpr(res, "/", u.getOperand());
      } else {
        res = new BinaryExpr(res, "*", right);
      }
    }
    return res;
  }

  private List<Expr> flattenAdd(Expr expr) {
    List<Expr> terms = new ArrayList<>();
    flattenAddHelper(expr, terms, true);
    return terms;
  }

  private void flattenAddHelper(Expr expr, List<Expr> terms, boolean positive) {
    if (expr instanceof BinaryExpr b) {
      if (b.getOp().equals("+")) {
        flattenAddHelper(b.getLeft(), terms, positive);
        flattenAddHelper(b.getRight(), terms, positive);
        return;
      } else if (b.getOp().equals("-")) {
        flattenAddHelper(b.getLeft(), terms, positive);
        flattenAddHelper(b.getRight(), terms, !positive);
        return;
      }
    }
    terms.add(positive ? expr : new UnaryExpr("-", expr));
  }

  private List<Expr> flattenMul(Expr expr) {
    List<Expr> factors = new ArrayList<>();
    flattenMulHelper(expr, factors, true);
    return factors;
  }

  private void flattenMulHelper(Expr expr, List<Expr> factors, boolean multiplier) {
    if (expr instanceof BinaryExpr b) {
      if (b.getOp().equals("*")) {
        flattenMulHelper(b.getLeft(), factors, multiplier);
        flattenMulHelper(b.getRight(), factors, multiplier);
        return;
      } else if (b.getOp().equals("/")) {
        flattenMulHelper(b.getLeft(), factors, multiplier);
        flattenMulHelper(b.getRight(), factors, !multiplier);
        return;
      }
    }
    factors.add(multiplier ? expr : new UnaryExpr("/", expr));
  }

  private List<Expr> generateAll(List<Expr> items, String op) {
    int n = items.size();
    if (n == 1) {
      return List.of(items.getFirst());
    }
    List<Expr> result = new ArrayList<>();
    for (int i = 1; i < n; i++) {
      List<Expr> lefts = generateAll(items.subList(0, i), op);
      List<Expr> rights = generateAll(items.subList(i, n), op);
      for (Expr left : lefts) {
        for (Expr right : rights) {
          result.add(new BinaryExpr(left, op, right));
        }
      }
    }
    return result;
  }
}

