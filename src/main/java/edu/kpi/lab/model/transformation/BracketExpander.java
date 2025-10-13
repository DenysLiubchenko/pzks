package edu.kpi.lab.model.transformation;

import edu.kpi.lab.model.lexical.Token;
import edu.kpi.lab.model.lexical.TokenType;
import edu.kpi.lab.model.transformation.node.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BracketExpander {

  private List<Token> tokens;
  private int pos;

  public String generateEquivalents(List<Token> tokens) {
    this.tokens = tokens;
    this.pos = 0;
    ExpressionNode tree = parseExpression();

    String form = tokens.stream().map(Token::getValue).collect(Collectors.joining(""));

    ExpressionNode current = tree;
    while (true) {
      ExpressionNode newTree = process(current);
      String newStr = newTree.toExpression();
      String currStr = current.toExpression();
      if (newStr.equals(currStr)) {
        break;
      }
      form = newStr;
      current = newTree;
    }
    return form;
  }

  private ExpressionNode parseExpression() {
    ExpressionNode node = parseTerm();
    while (pos < tokens.size() && (isAdd() || isMinus())) {
      String op = tokens.get(pos).getValue();
      pos++;
      ExpressionNode right = parseTerm();
      node = new BinaryNode(op, node, right);
    }
    return node;
  }

  private ExpressionNode parseTerm() {
    ExpressionNode node = parseFactor();
    while (pos < tokens.size() && (isMultiply() || isDivide())) {
      String op = tokens.get(pos).getValue();
      pos++;
      ExpressionNode right = parseFactor();
      node = new BinaryNode(op, node, right);
    }
    return node;
  }

  private ExpressionNode parseFactor() {
    Token t = tokens.get(pos);
    if (t.getTokenType() == TokenType.OPERATION_MINUS) {
      pos++;
      ExpressionNode child = parseFactor();
      return new UnaryNode("-", child);
    } else if (t.getTokenType() == TokenType.OPEN_BRACKET) {
      pos++;
      ExpressionNode node = parseExpression();
      if (pos < tokens.size() && tokens.get(pos).getTokenType() == TokenType.CLOSE_BRACKET) {
        pos++;
      }
      return node;
    } else if (TokenType.operandTypes.contains(t.getTokenType())) {
      pos++;
      return new LeafNode(t.getValue());
    }
    throw new RuntimeException("Parse error");
  }

  private boolean isAdd() {
    return pos < tokens.size() && tokens.get(pos).getTokenType() == TokenType.OPERATION_ADD;
  }

  private boolean isMinus() {
    return pos < tokens.size() && tokens.get(pos).getTokenType() == TokenType.OPERATION_MINUS;
  }

  private boolean isMultiply() {
    return pos < tokens.size() && tokens.get(pos).getTokenType() == TokenType.OPERATION_MULTIPLY;
  }

  private boolean isDivide() {
    return pos < tokens.size() && tokens.get(pos).getTokenType() == TokenType.OPERATION_DIVIDE;
  }

  private ExpressionNode process(ExpressionNode node) {
    if (node instanceof LeafNode) {
      return node;
    }
    if (node instanceof UnaryNode un) {
      ExpressionNode newChild = process(un.getChild());
      return new UnaryNode(un.getOperation(), newChild);
    }
    BinaryNode bn = (BinaryNode) node;
    ExpressionNode newLeft = process(bn.getLeft());
    ExpressionNode newRight = process(bn.getRight());
    BinaryNode newBn = new BinaryNode(bn.getOperation(), newLeft, newRight);
    if (isDistributable(newBn)) {
      return distribute(newBn);
    }
    return newBn;
  }

  private boolean isDistributable(BinaryNode bn) {
    String op = bn.getOperation();
    if (op.equals("*") && (isAdditive(bn.getLeft()) || isAdditive(bn.getRight()))) {
      return true;
    }
    if (op.equals("/") && isAdditive(bn.getLeft())) {
      return true;
    }
    return op.equals("-") && isAdditive(bn.getRight());
  }

  private boolean isAdditive(ExpressionNode node) {
    return node instanceof BinaryNode &&
           (((BinaryNode) node).getOperation().equals("+") || ((BinaryNode) node).getOperation().equals("-"));
  }

  private ExpressionNode distribute(BinaryNode bn) {
    String op = bn.getOperation();
    System.out.println("Expanding brackets for operation: " + op);
    System.out.println("Before expansion: " + bn.toExpression());

    ExpressionNode result;
    switch (op) {
      case "*" -> {
        List<Pair> leftTerms = flattenAdditive(bn.getLeft());
        List<Pair> rightTerms = flattenAdditive(bn.getRight());
        List<Pair> newTerms = new ArrayList<>();
        for (Pair lt : leftTerms) {
          for (Pair rt : rightTerms) {
            boolean isPos = lt.isPositive() == rt.isPositive();
            ExpressionNode prod = new BinaryNode("*", lt.getTerm(), rt.getTerm());
            newTerms.add(new Pair(isPos, prod));
          }
        }
        result = buildAdditive(newTerms);
      }
      case "/" -> {
        List<Pair> leftTerms = flattenAdditive(bn.getLeft());
        List<Pair> newTerms = new ArrayList<>();
        for (Pair lt : leftTerms) {
          ExpressionNode div = new BinaryNode("/", lt.getTerm(), bn.getRight());
          newTerms.add(new Pair(lt.isPositive(), div));
        }
        result = buildAdditive(newTerms);
      }
      case "-" -> {
        List<Pair> leftTerms = flattenAdditive(bn.getLeft());
        List<Pair> rightTerms = flattenAdditive(bn.getRight());
        invertSigns(rightTerms);
        leftTerms.addAll(rightTerms);
        result = buildAdditive(leftTerms);
      }
      default -> throw new RuntimeException("Invalid distribute");
    }

    System.out.println("After expansion: " + result.toExpression());
    System.out.println();
    return result;
  }

  private List<Pair> flattenAdditive(ExpressionNode node) {
    List<Pair> terms = new ArrayList<>();
    flattenHelper(node, true, terms);
    return terms;
  }

  private void flattenHelper(ExpressionNode node, boolean sign, List<Pair> terms) {
    if (node instanceof BinaryNode bn) {
      if (bn.getOperation().equals("+")) {
        flattenHelper(bn.getLeft(), sign, terms);
        flattenHelper(bn.getRight(), sign, terms);
      } else if (bn.getOperation().equals("-")) {
        flattenHelper(bn.getLeft(), sign, terms);
        flattenHelper(bn.getRight(), !sign, terms);
      } else {
        terms.add(new Pair(sign, node));
      }
    } else if (node instanceof UnaryNode un) {
      if (un.getOperation().equals("-")) {
        flattenHelper(un.getChild(), !sign, terms);
      } else {
        terms.add(new Pair(sign, node));
      }
    } else {
      terms.add(new Pair(sign, node));
    }
  }

  private void invertSigns(List<Pair> terms) {
    for (Pair p : terms) {
      p.setPositive(!p.isPositive());
    }
  }

  private ExpressionNode buildAdditive(List<Pair> terms) {
    if (terms.isEmpty()) {
      return new LeafNode("0");
    }
    ExpressionNode curr;

    if (terms.getFirst().isPositive()) {
      curr = terms.getFirst().getTerm();
    } else {
      curr = new UnaryNode("-", terms.getFirst().getTerm());
    }

    for (int i = 1; i < terms.size(); i++) {
      Pair p = terms.get(i);
      String nextOp = p.isPositive() ? "+" : "-";
      curr = new BinaryNode(nextOp, curr, p.getTerm());
    }
    return curr;
  }
}
