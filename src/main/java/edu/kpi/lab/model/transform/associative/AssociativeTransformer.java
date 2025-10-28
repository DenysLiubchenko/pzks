package edu.kpi.lab.model.transform.associative;

import edu.kpi.lab.model.transform.associative.node.BinaryExpression;
import edu.kpi.lab.model.transform.associative.node.Expression;
import edu.kpi.lab.model.transform.associative.node.FunctionExpression;
import edu.kpi.lab.model.transform.associative.node.Literal;
import edu.kpi.lab.model.transform.associative.node.Parser;
import edu.kpi.lab.model.transform.associative.node.UnaryExpression;
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
    Expression initial = parser.parse();
    List<Expression> allExpressions = getAllEquivalent(initial);
    Set<String> uniqueForms = new HashSet<>();
    for (Expression e : allExpressions) {
      uniqueForms.add(e.toString());
    }
    return new HashSet<>(uniqueForms);
  }

  private List<Expression> getAllEquivalent(Expression expression) {
    List<Expression> results = new ArrayList<>();
    if (expression instanceof Literal) {
      results.add(expression);
    } else if (expression instanceof UnaryExpression u) {
      List<Expression> subs = getAllEquivalent(u.getOperand());
      for (Expression s : subs) {
        results.add(new UnaryExpression(u.getOp(), s));
      }
    } else if (expression instanceof FunctionExpression f) {
      List<Expression> subs = getAllEquivalent(f.getArg());
      for (Expression s : subs) {
        results.add(new FunctionExpression(f.getName(), s));
      }
    } else if (expression instanceof BinaryExpression b) {
      List<Expression> lefts = getAllEquivalent(b.getLeft());
      List<Expression> rights = getAllEquivalent(b.getRight());
      for (Expression l : lefts) {
        for (Expression r : rights) {
          results.add(new BinaryExpression(l, b.getOperation(), r));
        }
      }
      String op = b.getOperation();
      if (op.equals("+") || op.equals("-")) {
        List<Expression> terms = flattenAdd(expression);
        if (terms.size() > 2) {
          List<Expression> groupings = generateAll(terms, "+");
          results.addAll(groupings);
        }
        List<Expression> factored = getFactoredVariants(terms, "+");
        results.addAll(factored);
      } else
      if (op.equals("*") || op.equals("/")) {
        List<Expression> factors = flattenMultiply(expression);
        if (factors.size() > 2) {
          List<Expression> groupings = generateAll(factors, "*");
          results.addAll(groupings);
        }
      }
    }
    return results;
  }

  private List<Expression> getFactoredVariants(List<Expression> terms, String op) {
    if (!op.equals("+")) {
      return Collections.emptyList();
    }

    Map<Expression, List<Integer>> factorToIndices = new HashMap<>();
    for (int i = 0; i < terms.size(); i++) {
      Expression term = terms.get(i);
      if (term instanceof UnaryExpression u && u.getOp().equals("-")) {
        term = u.getOperand();
      }
      List<Expression> factors = flattenMultiply(term);
      for (Expression f : factors) {
        factorToIndices.computeIfAbsent(f, k -> new ArrayList<>()).add(i);
      }
    }

    List<Expression> variants = new ArrayList<>();
    for (Map.Entry<Expression, List<Integer>> entry : factorToIndices.entrySet()) {
      List<Integer> group = entry.getValue();
      if (group.size() < 2) {
        continue;
      }
      Expression common = entry.getKey();
      if (common instanceof Literal l && l.getValue().equals("1")) {
        continue;
      }

      Set<Integer> grouped = new HashSet<>(group);
      List<Expression> newTerms = new ArrayList<>();
      for (int i = 0; i < terms.size(); i++) {
        if (!grouped.contains(i)) {
          newTerms.add(terms.get(i));
        }
      }

      List<Expression> sumTerms = new ArrayList<>();
      for (int i : group) {
        Expression term = terms.get(i);
        Expression quotient = divide(term, common);
        sumTerms.add(quotient);
      }

      Expression sum = buildAdd(sumTerms);
      Expression factored = new BinaryExpression(common, "*", sum);
      newTerms.add(factored);

      Expression newExpression = buildAdd(newTerms);
      variants.add(newExpression);
    }
    return variants;
  }

  private Expression divide(Expression term, Expression divisor) {
    boolean negative = false;
    if (term instanceof UnaryExpression u && u.getOp().equals("-")) {
      negative = true;
      term = u.getOperand();
    }
    List<Expression> factors = flattenMultiply(term);
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
    Expression quotient = buildMultiply(factors);
    if (negative) {
      quotient = new UnaryExpression("-", quotient);
    }
    return quotient;
  }

  private Expression buildAdd(List<Expression> terms) {
    if (terms.isEmpty()) {
      return new Literal("0", TokenType.INTEGER);
    }
    Expression res = terms.getFirst();
    for (int i = 1; i < terms.size(); i++) {
      Expression right = terms.get(i);
      if (right instanceof UnaryExpression u && u.getOp().equals("-")) {
        res = new BinaryExpression(res, "-", u.getOperand());
      } else {
        res = new BinaryExpression(res, "+", right);
      }
    }
    return res;
  }

  private Expression buildMultiply(List<Expression> factors) {
    if (factors.isEmpty()) {
      return new Literal("1", TokenType.INTEGER);
    }
    Expression res = factors.getFirst();
    for (int i = 1; i < factors.size(); i++) {
      Expression right = factors.get(i);
      if (right instanceof UnaryExpression u && u.getOp().equals("/")) {
        res = new BinaryExpression(res, "/", u.getOperand());
      } else {
        res = new BinaryExpression(res, "*", right);
      }
    }
    return res;
  }

  private List<Expression> flattenAdd(Expression expression) {
    List<Expression> terms = new ArrayList<>();
    flattenAddHelper(expression, terms, true);
    return terms;
  }

  private void flattenAddHelper(Expression expression, List<Expression> terms, boolean positive) {
    if (expression instanceof BinaryExpression b) {
      if (b.getOperation().equals("+")) {
        flattenAddHelper(b.getLeft(), terms, positive);
        flattenAddHelper(b.getRight(), terms, positive);
        return;
      } else if (b.getOperation().equals("-")) {
        flattenAddHelper(b.getLeft(), terms, positive);
        flattenAddHelper(b.getRight(), terms, !positive);
        return;
      }
    }
    terms.add(positive ? expression : new UnaryExpression("-", expression));
  }

  private List<Expression> flattenMultiply(Expression expression) {
    List<Expression> factors = new ArrayList<>();
    flattenMulHelper(expression, factors, true);
    return factors;
  }

  private void flattenMulHelper(Expression expression, List<Expression> factors, boolean multiplier) {
    if (expression instanceof BinaryExpression b) {
      if (b.getOperation().equals("*")) {
        flattenMulHelper(b.getLeft(), factors, multiplier);
        flattenMulHelper(b.getRight(), factors, multiplier);
        return;
      } else if (b.getOperation().equals("/")) {
        flattenMulHelper(b.getLeft(), factors, multiplier);
        flattenMulHelper(b.getRight(), factors, !multiplier);
        return;
      }
    }
    factors.add(multiplier ? expression : new UnaryExpression("/", expression));
  }

  private List<Expression> generateAll(List<Expression> items, String op) {
    int n = items.size();
    if (n == 1) {
      return List.of(items.getFirst());
    }
    List<Expression> result = new ArrayList<>();
    for (int i = 1; i < n; i++) {
      List<Expression> lefts = generateAll(items.subList(0, i), op);
      List<Expression> rights = generateAll(items.subList(i, n), op);
      for (Expression left : lefts) {
        for (Expression right : rights) {
          result.add(new BinaryExpression(left, op, right));
        }
      }
    }
    return result;
  }
}

