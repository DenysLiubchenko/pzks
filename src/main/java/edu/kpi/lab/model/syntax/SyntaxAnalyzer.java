package edu.kpi.lab.model.syntax;

import edu.kpi.lab.model.lexical.Token;
import edu.kpi.lab.model.syntax.tree.Function;
import edu.kpi.lab.model.syntax.tree.Node;
import edu.kpi.lab.model.syntax.tree.Operand;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import static edu.kpi.lab.model.syntax.SyntaxType.OPERAND;
import static edu.kpi.lab.model.syntax.SyntaxType.OPERATION_ADD;
import static edu.kpi.lab.model.syntax.SyntaxType.OPERATION_DIVIDE;
import static edu.kpi.lab.model.syntax.SyntaxType.OPERATION_MINUS;
import static edu.kpi.lab.model.syntax.SyntaxType.OPERATION_MULTIPLY;
import static edu.kpi.lab.model.syntax.SyntaxValidator.getTokenSyntaxType;

public class SyntaxAnalyzer {

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

    Function tree = buildBalancedParallelTree(operands, operators);

    return optimizeTree(tree);
  }

  private void parseTokensToList(List<Token> tokens, List<Node> operands, List<SyntaxType> operators) {
    Stack<List<Node>> operandStack = new Stack<>();
    Stack<List<SyntaxType>> operatorStack = new Stack<>();

    List<Node> currentOperands = new ArrayList<>();
    List<SyntaxType> currentOperators = new ArrayList<>();

    boolean expectOperand = true;

    for (Token token : tokens) {
      SyntaxType type = getTokenSyntaxType(token);

      switch (type) {
        case OPERAND:
          currentOperands.add(new Operand(OPERAND, token.getValue()));
          expectOperand = false;
          break;

        case OPERATION_MINUS:
          if (expectOperand) {
            currentOperands.add(new Operand(OPERAND, "0"));
            currentOperators.add(OPERATION_MINUS);
            expectOperand = true;
          } else {
            currentOperators.add(OPERATION_MINUS);
            expectOperand = true;
          }
          break;

        case OPERATION_ADD:
          if (expectOperand) {
            expectOperand = true;
          } else {
            currentOperators.add(OPERATION_ADD);
            expectOperand = true;
          }
          break;

        case OPERATION_MULTIPLY:
        case OPERATION_DIVIDE:
          currentOperators.add(type);
          expectOperand = true;
          break;

        case OPEN_BRACKET:
          operandStack.push(new ArrayList<>(currentOperands));
          operatorStack.push(new ArrayList<>(currentOperators));
          currentOperands = new ArrayList<>();
          currentOperators = new ArrayList<>();
          expectOperand = true;
          break;

        case CLOSE_BRACKET:
          if (!currentOperands.isEmpty()) {
            Function bracketResult = buildBalancedParallelTree(currentOperands, currentOperators);
            currentOperands = operandStack.pop();
            currentOperators = operatorStack.pop();
            currentOperands.add(bracketResult);
          }
          expectOperand = false;
          break;
      }
    }

    operands.addAll(currentOperands);
    operators.addAll(currentOperators);
  }

  private Function buildBalancedParallelTree(List<Node> operands, List<SyntaxType> operators) {
    if (operators.isEmpty()) {
      if (operands.size() == 1) {
        if (operands.getFirst() instanceof Function) {
          return (Function) operands.getFirst();
        } else {
          throw new IllegalArgumentException("Single operand cannot form expression tree");
        }
      } else {
        throw new IllegalArgumentException("Multiple operands but no operators");
      }
    }

    if (operators.size() != operands.size() - 1) {
      throw new IllegalArgumentException(
        String.format("Invalid expression: %d operands but %d operators. Expected %d operators.",
          operands.size(), operators.size(), operands.size() - 1));
    }

    List<Node> processedOperands = new ArrayList<>();
    List<SyntaxType> lowPrecedenceOps = new ArrayList<>();

    processedOperands.add(operands.getFirst());

    for (int i = 0; i < operators.size(); i++) {
      SyntaxType op = operators.get(i);
      Node rightOperand = operands.get(i + 1);

      if (op == OPERATION_MULTIPLY || op == OPERATION_DIVIDE) {
        Node left = processedOperands.removeLast();
        Function func = new Function();
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

        Function func = new Function();
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

  public Function optimizeTree(Function root) {
    if (root == null) {
      return null;
    }

    Function current = root;
    Function previous;
    int iterations = 0;
    int maxIterations = 100;

    do {
      previous = copyTree(current);
      current = (Function) optimizeNode(current);
      iterations++;
    } while (!treesEqual(current, previous) && iterations < maxIterations);

    return current;
  }

  private Node optimizeNode(Node node) {
    switch (node) {
      case null -> {
        return null;
      }
      case Function func -> {
        Node leftOptimized = optimizeNode(func.getLeft());
        Node rightOptimized = optimizeNode(func.getRight());

        func.setLeft(leftOptimized);
        func.setRight(rightOptimized);

        return applyAlgebraicOptimizations(func);
      }
      default -> {
        return node;
      }
    }
  }

  private Node applyAlgebraicOptimizations(Function func) {
    SyntaxType operation = func.getOperation();
    Node left = func.getLeft();
    Node right = func.getRight();

    return switch (operation) {
      case OPERATION_ADD -> optimizeAddition(func, left, right);
      case OPERATION_MINUS -> optimizeSubtraction(func, left, right);
      case OPERATION_MULTIPLY -> optimizeMultiplication(func, left, right);
      case OPERATION_DIVIDE -> optimizeDivision(func, left, right);
      default -> func;
    };
  }

  private Node optimizeAddition(Function func, Node left, Node right) {
    if (isZero(right)) {
      return left;
    }
    if (isZero(left)) {
      return right;
    }

    if (isNumeric(left) && isNumeric(right)) {
      double leftVal = getNumericValue(left);
      double rightVal = getNumericValue(right);
      double result = leftVal + rightVal;
      return createNumericOperand(result);
    }

    List<Node> additionTerms = new ArrayList<>();
    double constantSum = 0.0;
    boolean hasConstants = false;

    collectAdditionTerms(func, additionTerms);

    List<Node> variables = new ArrayList<>();
    for (Node term : additionTerms) {
      if (isNumeric(term)) {
        constantSum += getNumericValue(term);
        hasConstants = true;
      } else {
        variables.add(term);
      }
    }

    List<Node> finalTerms = new ArrayList<>(variables);
    if (hasConstants && constantSum != 0.0) {
      finalTerms.add(createNumericOperand(constantSum));
    }

    if (finalTerms.isEmpty()) {
      return createNumericOperand(0);
    } else if (finalTerms.size() == 1) {
      return finalTerms.getFirst();
    } else {
      return buildPerfectBalancedTree(finalTerms, OPERATION_ADD);
    }
  }

  private Node optimizeSubtraction(Function func, Node left, Node right) {
    if (isZero(right)) {
      return left;
    }

    if (isNumeric(left) && isNumeric(right)) {
      double leftVal = getNumericValue(left);
      double rightVal = getNumericValue(right);
      double result = leftVal - rightVal;
      return createNumericOperand(result);
    }

    return func;
  }

  private Node optimizeMultiplication(Function func, Node left, Node right) {
    if (isZero(left) || isZero(right)) {
      return createNumericOperand(0);
    }

    if (isOne(right)) {
      return left;
    }
    if (isOne(left)) {
      return right;
    }

    if (isNumeric(left) && isNumeric(right)) {
      double leftVal = getNumericValue(left);
      double rightVal = getNumericValue(right);
      double result = leftVal * rightVal;
      return createNumericOperand(result);
    }

    return func;
  }

  private Node optimizeDivision(Function func, Node left, Node right) {
    if (isOne(right)) {
      return left;
    }

    if (isZero(left) && !isZero(right)) {
      return createNumericOperand(0);
    }

    if (isNumeric(left) && isNumeric(right) && !isZero(right)) {
      double leftVal = getNumericValue(left);
      double rightVal = getNumericValue(right);
      double result = leftVal / rightVal;
      return createNumericOperand(result);
    }

    return func;
  }

  private void collectAdditionTerms(Node node, List<Node> terms) {
    if (node instanceof Function func) {
      if (func.getOperation() == OPERATION_ADD) {
        collectAdditionTerms(func.getLeft(), terms);
        collectAdditionTerms(func.getRight(), terms);
        return;
      }
    }
    terms.add(node);
  }

  private boolean isZero(Node node) {
    if (node instanceof Operand operand) {
      String value = operand.getValue();
      try {
        return Double.parseDouble(value) == 0.0;
      } catch (NumberFormatException e) {
        return false;
      }
    }
    return false;
  }

  private boolean isOne(Node node) {
    if (node instanceof Operand operand) {
      String value = operand.getValue();
      try {
        return Double.parseDouble(value) == 1.0;
      } catch (NumberFormatException e) {
        return false;
      }
    }
    return false;
  }

  private boolean isNumeric(Node node) {
    if (node instanceof Operand operand) {
      String value = operand.getValue();
      try {
        Double.parseDouble(value);
        return true;
      } catch (NumberFormatException e) {
        return false;
      }
    }
    return false;
  }

  private double getNumericValue(Node node) {
    if (node instanceof Operand operand) {
      try {
        return Double.parseDouble(operand.getValue());
      } catch (NumberFormatException e) {
        return 0.0;
      }
    }
    return 0.0;
  }

  private Node createNumericOperand(double value) {
    String valueStr;
    if (value == (long) value) {
      valueStr = String.valueOf((long) value);
    } else {
      valueStr = String.valueOf(value);
    }
    return new Operand(OPERAND, valueStr);
  }

  private Function copyTree(Function root) {
    if (root == null) {
      return null;
    }

    Function copy = new Function();
    copy.setOperation(root.getOperation());
    copy.setLeft(copyNode(root.getLeft()));
    copy.setRight(copyNode(root.getRight()));

    return copy;
  }

  private Node copyNode(Node node) {
    return switch (node) {
      case Operand operand -> new Operand(operand.getType(), operand.getValue());
      case Function function -> copyTree(function);
      default -> null;
    };
  }

  private boolean treesEqual(Function tree1, Function tree2) {
    if (tree1 == null && tree2 == null) {
      return true;
    }
    if (tree1 == null || tree2 == null) {
      return false;
    }

    if (tree1.getOperation() != tree2.getOperation()) {
      return false;
    }

    return nodesEqual(tree1.getLeft(), tree2.getLeft()) &&
           nodesEqual(tree1.getRight(), tree2.getRight());
  }

  private boolean nodesEqual(Node node1, Node node2) {
    if (node1 == null && node2 == null) {
      return true;
    }
    if (node1 == null || node2 == null) {
      return false;
    }

    if (node1.getClass() != node2.getClass()) {
      return false;
    }

    if (node1 instanceof Operand op1 && node2 instanceof Operand op2) {
      return op1.getValue().equals(op2.getValue());
    } else if (node1 instanceof Function && node2 instanceof Function) {
      return treesEqual((Function) node1, (Function) node2);
    }

    return false;
  }
}
