package edu.kpi.lab.model.transform.commutative;

import edu.kpi.lab.model.transform.commutative.node.Function;
import edu.kpi.lab.model.lexical.Token;
import edu.kpi.lab.model.lexical.TokenType;
import edu.kpi.lab.model.syntax.SyntaxType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CommutativeTransformer {
  private static SyntaxType getSyntaxTypeFromTokenType(TokenType tokenType) {
    return switch (tokenType) {
      case OPERATION_ADD -> SyntaxType.OPERATION_ADD;
      case OPERATION_MINUS -> SyntaxType.OPERATION_MINUS;
      case OPERATION_MULTIPLY -> SyntaxType.OPERATION_MULTIPLY;
      case OPERATION_DIVIDE -> SyntaxType.OPERATION_DIVIDE;
      default -> SyntaxType.ERROR;
    };
  }

  private Function parseExpression(List<Token> tokens, int[] position) {
    Function result = parseTerm(tokens, position);
    while (position[0] < tokens.size()) {
      Token current = tokens.get(position[0]);
      TokenType type = current.getTokenType();
      if (type != TokenType.OPERATION_ADD && type != TokenType.OPERATION_MINUS) {
        break;
      }
      position[0]++;
      Function right = parseTerm(tokens, position);
      Function newNode = new Function(getSyntaxTypeFromTokenType(type));
      newNode.setLeft(result);
      newNode.setRight(right);
      result = newNode;
    }
    return result;
  }

  private Function parseTerm(List<Token> tokens, int[] position) {
    Function result = parseFactor(tokens, position);
    while (position[0] < tokens.size()) {
      Token current = tokens.get(position[0]);
      TokenType type = current.getTokenType();
      if (type != TokenType.OPERATION_MULTIPLY && type != TokenType.OPERATION_DIVIDE) {
        break;
      }
      position[0]++;
      Function right = parseFactor(tokens, position);
      Function newNode = new Function(getSyntaxTypeFromTokenType(type));
      newNode.setLeft(result);
      newNode.setRight(right);
      result = newNode;
    }
    return result;
  }

  private Function parseFactor(List<Token> tokens, int[] position) {
    Token current = tokens.get(position[0]);
    position[0]++;
    return switch (current.getTokenType()) {
      case CONSTANT, INTEGER, DECIMAL -> {
        Function node = new Function(SyntaxType.OPERAND);
        node.setValue(current.getValue());
        yield node;
      }
      case FUNCTION -> {
        if (tokens.get(position[0]).getTokenType() != TokenType.FUNCTION_OPEN_BRACKET) {
          throw new RuntimeException("Expected open bracket after function");
        }
        position[0]++;
        Function arg = parseExpression(tokens, position);
        if (tokens.get(position[0]).getTokenType() != TokenType.FUNCTION_CLOSE_BRACKET) {
          throw new RuntimeException("Expected close bracket after function arg");
        }
        position[0]++;
        Function node = new Function(SyntaxType.FUNCTION);
        node.setValue(current.getValue());
        node.setParam(arg);
        yield node;
      }
      case OPEN_BRACKET -> {
        Function expr = parseExpression(tokens, position);
        if (tokens.get(position[0]).getTokenType() != TokenType.CLOSE_BRACKET) {
          throw new RuntimeException("Expected close bracket");
        }
        position[0]++;
        yield expr;
      }
      case OPERATION_MINUS -> {
        Function child = parseFactor(tokens, position);
        Function zero = new Function(SyntaxType.OPERAND);
        zero.setValue("0");
        Function node = new Function(SyntaxType.OPERATION_MINUS);
        node.setLeft(zero);
        node.setRight(child);
        yield node;
      }
      default -> throw new RuntimeException("Unexpected token in factor: " + current);
    };
  }

  public Set<String> generateEquivalentExpressions(List<Token> tokens) {
    int[] position = new int[1];
    Function tree = parseExpression(tokens, position);
    if (position[0] != tokens.size()) {
      throw new RuntimeException("Parsing error: extra tokens");
    }

    Set<String> results = new LinkedHashSet<>();

    Function balancedDesc = balanceTree(tree, Comparator.comparingInt(Function::getWeight).reversed().thenComparing(Function::getExpression));
    results.add(balancedDesc.getExpression());

    Function balancedLex = balanceTree(tree, Comparator.comparing(Function::getExpression));
    results.add(balancedLex.getExpression());

    return results;
  }

  private Function balanceTree(Function node, Comparator<Function> comparator) {
    if (node == null) {
      return null;
    }

    if (node.getLeft() != null) {
      node.setLeft(balanceTree(node.getLeft(), comparator));
    }
    if (node.getRight() != null) {
      node.setRight(balanceTree(node.getRight(), comparator));
    }
    if (node.getParam() != null) {
      node.setParam(balanceTree(node.getParam(), comparator));
    }

    SyntaxType type = node.getType();
    if (type == SyntaxType.OPERATION_ADD || type == SyntaxType.OPERATION_MULTIPLY) {
      List<Function> operands = collectOperands(node, type);
      operands.sort(comparator);
      return buildBalanced(operands, type);
    }

    return node;
  }

  private List<Function> collectOperands(Function node, SyntaxType opType) {
    List<Function> operands = new ArrayList<>();
    if (node.getType() == opType) {
      operands.addAll(collectOperands(node.getLeft(), opType));
      operands.addAll(collectOperands(node.getRight(), opType));
    } else {
      operands.add(node);
    }
    return operands;
  }

  private Function buildBalanced(List<Function> operands, SyntaxType opType) {
    if (operands.isEmpty()) {
      return null;
    }
    if (operands.size() == 1) {
      return operands.getFirst();
    }
    int mid = operands.size() / 2;
    Function left = buildBalanced(operands.subList(0, mid), opType);
    Function right = buildBalanced(operands.subList(mid, operands.size()), opType);
    Function node = new Function(opType);
    node.setLeft(left);
    node.setRight(right);
    return node;
  }
}