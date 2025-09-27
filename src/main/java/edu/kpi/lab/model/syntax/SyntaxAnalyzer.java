package edu.kpi.lab.model.syntax;

import edu.kpi.lab.model.lexical.Token;
import edu.kpi.lab.model.lexical.TokenType;
import edu.kpi.lab.model.syntax.tree.Function;
import edu.kpi.lab.model.syntax.tree.Node;
import edu.kpi.lab.model.syntax.tree.Operand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import static edu.kpi.lab.model.syntax.SyntaxType.CLOSE_BRACKET;
import static edu.kpi.lab.model.syntax.SyntaxType.ERROR;
import static edu.kpi.lab.model.syntax.SyntaxType.FINISH;
import static edu.kpi.lab.model.syntax.SyntaxType.FUNCTION;
import static edu.kpi.lab.model.syntax.SyntaxType.FUNCTION_CLOSE_BRACKET;
import static edu.kpi.lab.model.syntax.SyntaxType.FUNCTION_OPEN_BRACKET;
import static edu.kpi.lab.model.syntax.SyntaxType.OPEN_BRACKET;
import static edu.kpi.lab.model.syntax.SyntaxType.OPERAND;
import static edu.kpi.lab.model.syntax.SyntaxType.OPERATION_ADD;
import static edu.kpi.lab.model.syntax.SyntaxType.OPERATION_DIVIDE;
import static edu.kpi.lab.model.syntax.SyntaxType.OPERATION_MINUS;
import static edu.kpi.lab.model.syntax.SyntaxType.OPERATION_MULTIPLY;
import static edu.kpi.lab.model.syntax.SyntaxType.START;

public class SyntaxAnalyzer {

  private final Map<SyntaxType, Set<SyntaxType>> allowedCombinations;
  private final Set<TokenType> operandTypes;

  {
    operandTypes = Set.of(TokenType.INTEGER, TokenType.DECIMAL, TokenType.CONSTANT, TokenType.FUNCTION);
    allowedCombinations = new HashMap<>();
    allowedCombinations.put(START,
      Set.of(OPEN_BRACKET, OPERAND, FUNCTION, OPERATION_ADD, OPERATION_MINUS));
    allowedCombinations.put(OPEN_BRACKET,
      Set.of(OPEN_BRACKET, OPERAND, FUNCTION, OPERATION_ADD, OPERATION_MINUS));
    allowedCombinations.put(OPERATION_ADD, Set.of(OPERAND, FUNCTION, OPEN_BRACKET));
    allowedCombinations.put(OPERATION_MINUS, Set.of(OPERAND, FUNCTION, OPEN_BRACKET));
    allowedCombinations.put(OPERATION_MULTIPLY, Set.of(OPEN_BRACKET, FUNCTION, OPERAND));
    allowedCombinations.put(OPERATION_DIVIDE, Set.of(OPEN_BRACKET, FUNCTION, OPERAND));
    allowedCombinations.put(OPERAND,
      Set.of(OPERATION_ADD, OPERATION_MINUS, OPERATION_MULTIPLY, OPERATION_DIVIDE, CLOSE_BRACKET, FUNCTION_CLOSE_BRACKET, FINISH));
    allowedCombinations.put(CLOSE_BRACKET,
      Set.of(OPERATION_ADD, OPERATION_MINUS, OPERATION_MULTIPLY, OPERATION_DIVIDE, CLOSE_BRACKET, FUNCTION_CLOSE_BRACKET, FINISH));
    allowedCombinations.put(FUNCTION, Set.of(FUNCTION_OPEN_BRACKET));
    allowedCombinations.put(FUNCTION_OPEN_BRACKET,
      Set.of(FUNCTION, OPERAND, OPEN_BRACKET, FUNCTION_CLOSE_BRACKET));
    allowedCombinations.put(FUNCTION_CLOSE_BRACKET,
      Set.of(OPERATION_ADD, OPERATION_MINUS, OPERATION_MULTIPLY, OPERATION_DIVIDE, CLOSE_BRACKET, FUNCTION_CLOSE_BRACKET, FINISH));
    allowedCombinations.put(ERROR, Arrays.stream(SyntaxType.values()).filter(st-> st != ERROR).collect(Collectors.toSet()));
  }

  public Function buildSyntaxTree(List<Token> tokens) {
    List<Node> operands = new ArrayList<>();
    List<SyntaxType> operators = new ArrayList<>();

    parseTokensToList(tokens, operands, operators);

    if (operands.isEmpty()) {
      throw new IllegalArgumentException("No operands found");
    }

    if (operands.size() == 1) {
      throw new IllegalArgumentException("Single operand cannot form expression tree");
    }

    return buildBalancedParallelTree(operands, operators);
  }

  private void parseTokensToList(List<Token> tokens, List<Node> operands, List<SyntaxType> operators) {
    Stack<List<Node>> operandStack = new Stack<>();
    Stack<List<SyntaxType>> operatorStack = new Stack<>();

    List<Node> currentOperands = new ArrayList<>();
    List<SyntaxType> currentOperators = new ArrayList<>();

    int pos = 0;

    for (Token token : tokens) {
      SyntaxType type = getTokenSyntaxType(token);

      switch (type) {
        case OPERAND:
          currentOperands.add(new Operand(pos++, OPERAND, token.getValue()));
          break;

        case OPERATION_ADD:
        case OPERATION_MINUS:
        case OPERATION_MULTIPLY:
        case OPERATION_DIVIDE:
          currentOperators.add(type);
          break;

        case OPEN_BRACKET:
          operandStack.push(new ArrayList<>(currentOperands));
          operatorStack.push(new ArrayList<>(currentOperators));
          currentOperands = new ArrayList<>();
          currentOperators = new ArrayList<>();
          break;

        case CLOSE_BRACKET:
          if (!currentOperands.isEmpty()) {
            Function bracketResult = buildBalancedParallelTree(currentOperands, currentOperators);
            currentOperands = operandStack.pop();
            currentOperators = operatorStack.pop();
            currentOperands.add(bracketResult);
          }
          break;
      }
    }

    operands.addAll(currentOperands);
    operators.addAll(currentOperators);
  }

  private Function buildBalancedParallelTree(List<Node> operands, List<SyntaxType> operators) {
    if (operators.isEmpty()) {
      throw new IllegalArgumentException("No operators");
    }

    List<Node> processedOperands = new ArrayList<>();
    List<SyntaxType> lowPrecedenceOps = new ArrayList<>();

    processedOperands.add(operands.getFirst());

    for (int i = 0; i < operators.size(); i++) {
      SyntaxType op = operators.get(i);
      Node rightOperand = operands.get(i + 1);

      if (op == OPERATION_MULTIPLY || op == OPERATION_DIVIDE) {
        Node left = processedOperands.removeLast();
        Function func = new Function(0);
        func.setOperation(op);
        func.setLeft(left);
        func.setRight(rightOperand);
        processedOperands.add(func);
      } else {
        lowPrecedenceOps.add(op);
        processedOperands.add(rightOperand);
      }
    }

    if (lowPrecedenceOps.isEmpty()) {
      return (Function) processedOperands.getFirst();
    }

    SyntaxType operation = lowPrecedenceOps.getFirst();
    return (Function) buildPerfectBalancedTree(processedOperands, operation);
  }

  private Node buildPerfectBalancedTree(List<Node> operands, SyntaxType operation) {
    if (operands.size() == 1) {
      return operands.getFirst();
    }

    Queue<Node> queue = new LinkedList<>(operands);

    while (queue.size() > 1) {
      Queue<Node> nextLevel = new LinkedList<>();

      while (queue.size() >= 2) {
        Node left = queue.poll();
        Node right = queue.poll();

        Function func = new Function(0);
        func.setOperation(operation);
        func.setLeft(left);
        func.setRight(right);

        nextLevel.offer(func);
      }

      if (!queue.isEmpty()) {
        nextLevel.offer(queue.poll());
      }

      queue = nextLevel;
    }

    return queue.poll();
  }

  public void printTreeStructure(Function root) {
    System.out.println("Parallel Tree Structure:");
    printTreeStructure(root, 0, "Root");
    System.out.println("Tree height: " + getTreeHeight(root));
    System.out.println("Max width: " + getMaxWidth(root));
  }

  private void printTreeStructure(Node node, int level, String position) {
    if (node == null) return;

    String indent = "  ".repeat(level);

    if (node instanceof Function function) {
      System.out.println(indent + position + ": Operation: " + function.getOperation());

      if (function.getLeft() != null) {
        printTreeStructure(function.getLeft(), level + 1, "Left");
      }
      if (function.getRight() != null) {
        printTreeStructure(function.getRight(), level + 1, "Right");
      }
    } else if (node instanceof Operand operand) {
      System.out.println(indent + position + ": Operand(type=" + operand.getType() + ", value=" + operand.getValue() + ")");
    }
  }

  public int getTreeHeight(Node node) {
    if (node == null) return 0;

    if (node instanceof Operand) return 1;

    if (node instanceof Function function) {
      int leftHeight = getTreeHeight(function.getLeft());
      int rightHeight = getTreeHeight(function.getRight());
      return Math.max(leftHeight, rightHeight) + 1;
    }

    return 0;
  }

  public int getMaxWidth(Function root) {
    if (root == null) return 0;

    Map<Integer, Integer> levelCounts = new HashMap<>();
    countNodesAtLevel(root, 0, levelCounts);

    return levelCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
  }

  private void countNodesAtLevel(Node node, int level, Map<Integer, Integer> levelCounts) {
    if (node == null) return;

    levelCounts.merge(level, 1, Integer::sum);

    if (node instanceof Function function) {
      countNodesAtLevel(function.getLeft(), level + 1, levelCounts);
      countNodesAtLevel(function.getRight(), level + 1, levelCounts);
    }
  }

  public void processTokenQuery(List<Token> tokens) {
    List<Error> errors = new ArrayList<>();
    Stack<SyntaxType> openedBrackets = new Stack<>();

    validateTokenWith(errors, openedBrackets, tokens.getFirst(), START);

    for (int i = 1; i < tokens.size(); i++) {
      validateTokenWith(errors, openedBrackets,
        tokens.get(i), getTokenSyntaxType(tokens.get(i - 1)));
    }

    Token lastToken = tokens.getLast();
    if (!Set.of(CLOSE_BRACKET, FUNCTION_CLOSE_BRACKET, OPERAND, ERROR).contains(getTokenSyntaxType(lastToken))) {
      errors.add(new Error(lastToken.getEndPosition(), lastToken.getEndPosition(), "Query cannot be finished with " + lastToken.getTokenType()));
    }
    if (!openedBrackets.isEmpty()) {
      errors.add(new Error(lastToken.getEndPosition(), lastToken.getEndPosition(), "There are unclosed brackets in query"));
    }

    if (errors.isEmpty()) {
      System.out.println("Query has no errors");
    } else {
      System.out.println("Errors: ");
      for (int i = 0; i < errors.size(); i++) {
        System.out.println(i + 1 + ". " + errors.get(i));
      }
    }
  }

  private void validateTokenWith(List<Error> errors, Stack<SyntaxType> openedBrackets, Token token,
                                 SyntaxType previousTokenSyntaxType) {
    Set<SyntaxType> allowedPositions = allowedCombinations.get(previousTokenSyntaxType);
    SyntaxType syntaxType = getTokenSyntaxType(token);
    try {
      if (syntaxType == OPEN_BRACKET || syntaxType == FUNCTION_OPEN_BRACKET) {
        openedBrackets.push(syntaxType);
      } else if (syntaxType == CLOSE_BRACKET) {
        SyntaxType pop = openedBrackets.pop();
        if (pop != OPEN_BRACKET) {
          throw new IllegalArgumentException("Regular bracket must be closed");
        }
      } else if (syntaxType == FUNCTION_CLOSE_BRACKET) {
        SyntaxType pop = openedBrackets.pop();
        if (pop != FUNCTION_OPEN_BRACKET) {
          throw new IllegalArgumentException("Function bracket must be closed");
        }
      }

      if (token.getTokenType().equals(TokenType.INTEGER) && token.getValue().equals("0") && previousTokenSyntaxType.equals(OPERATION_DIVIDE)) {
        errors.add(new Error(token.getEndPosition(), token.getEndPosition(), "Divide by zero"));
      }

      boolean isPlaceCorrect = allowedPositions.contains(syntaxType);
      if (!isPlaceCorrect) {
        String message = token.getTokenType() + " is unexpected.";
        token.setTokenType(TokenType.ERROR);
        errors.add(new Error(token.getStartPosition(), token.getEndPosition(), message));
      }

    } catch (EmptyStackException e) {
      String message = token.getTokenType() + " is unexpected.";
      token.setTokenType(TokenType.ERROR);
      errors.add(new Error(token.getStartPosition(), token.getEndPosition(), message));
    }
  }

  private SyntaxType getTokenSyntaxType(Token token) {
    TokenType tokenType = token.getTokenType();
    if (operandTypes.contains(tokenType)) {
      if (tokenType.equals(TokenType.FUNCTION)) {
        return FUNCTION;
      } else {
        return OPERAND;
      }
    } else if (tokenType.equals(TokenType.OPERATION_ADD)) {
      return OPERATION_ADD;
    }else if (tokenType.equals(TokenType.OPERATION_MINUS)) {
      return OPERATION_MINUS;
    } else if (tokenType.equals(TokenType.OPERATION_MULTIPLY)) {
      return OPERATION_MULTIPLY;
    } else if (tokenType.equals(TokenType.OPERATION_DIVIDE)) {
      return OPERATION_DIVIDE;
    } else if (tokenType.equals(TokenType.OPEN_BRACKET)) {
      return OPEN_BRACKET;
    } else if (tokenType.equals(TokenType.CLOSE_BRACKET)) {
      return CLOSE_BRACKET;
    } else if (tokenType.equals(TokenType.FUNCTION_OPEN_BRACKET)) {
      return FUNCTION_OPEN_BRACKET;
    } else if (tokenType.equals(TokenType.FUNCTION_CLOSE_BRACKET)) {
      return FUNCTION_CLOSE_BRACKET;
    } else {
      return ERROR;
    }
  }

  @Getter
  @EqualsAndHashCode
  private class Error {
    private final Integer startPosition;

    private final Integer endPosition;

    private final String message;

    public Error(Integer startPosition, Integer endPosition, String message) {
      this.startPosition = startPosition;
      this.endPosition = endPosition;
      this.message = message;
    }

    @Override
    public String toString() {
      return "Error: " + message +  " On position [" + startPosition + "; " + endPosition + "]";
    }
  }
}
