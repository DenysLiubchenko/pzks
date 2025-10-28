package edu.kpi.lab.model.transform.commutative.node;

import edu.kpi.lab.model.syntax.SyntaxType;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Function {
  private SyntaxType type;
  private String value;
  private Function left;
  private Function right;
  private Function param;

  public Function(SyntaxType type) {
    this.type = type;
  }

  public int getWeight() {
    int w = 1;
    if (left != null) w += left.getWeight();
    if (right != null) w += right.getWeight();
    if (param != null) w += param.getWeight();
    return w;
  }

  private int getPrecedence() {
    return switch (type) {
      case OPERATION_ADD, OPERATION_MINUS -> 1;
      case OPERATION_MULTIPLY, OPERATION_DIVIDE -> 2;
      default -> 100; // high for atoms
    };
  }

  public String getExpression() {
    return getExpression(0);
  }

  private String getExpression(int parentPrec) {
    String s;
    int myPrec;
    switch (type) {
      case OPERAND -> {
        s = Objects.requireNonNullElse(value, "?");
        myPrec = 100;
      }
      case FUNCTION -> {
        s = value + "(" + param.getExpression(0) + ")";
        myPrec = 100;
      }
      case OPERATION_ADD, OPERATION_MINUS, OPERATION_MULTIPLY, OPERATION_DIVIDE -> {
        boolean isCommutAssoc = (type == SyntaxType.OPERATION_ADD || type == SyntaxType.OPERATION_MULTIPLY);
        if (type == SyntaxType.OPERATION_MINUS && left != null && left.getType() == SyntaxType.OPERAND && "0".equals(left.getValue())) {
          // unary minus
          String rightStr = right.getExpression(3);
          s = "-" + rightStr;
          myPrec = 3;
        } else if (type == SyntaxType.OPERATION_DIVIDE && left != null && left.getType() == SyntaxType.OPERAND && "1".equals(left.getValue())) {
          // reciprocal
          String rightStr = right.getExpression(3);
          s = "1/" + rightStr;
          myPrec = 3;
        } else if (type == SyntaxType.OPERATION_ADD &&
                   right != null && right.getType() == SyntaxType.OPERATION_MINUS &&
                   right.getLeft() != null && right.getLeft().getType() == SyntaxType.OPERAND &&
                   "0".equals(right.getLeft().getValue())) {
          // left + (-subright) => left - subright
          int leftPrec = 1;
          int rightPrec = 2;
          String leftStr = left.getExpression(leftPrec);
          String rightStr = right.getRight().getExpression(rightPrec);
          s = leftStr + "-" + rightStr;
          myPrec = 1;
        } else if (type == SyntaxType.OPERATION_MULTIPLY &&
                   right != null && right.getType() == SyntaxType.OPERATION_DIVIDE &&
                   right.getLeft() != null && right.getLeft().getType() == SyntaxType.OPERAND &&
                   "1".equals(right.getLeft().getValue())) {
          // left * (1/subright) => left / subright
          int leftPrec = 2;
          int rightPrec = 3;
          String leftStr = left.getExpression(leftPrec);
          String rightStr = right.getRight().getExpression(rightPrec);
          s = leftStr + "/" + rightStr;
          myPrec = 2;
        } else {
          String opStr = switch (type) {
            case OPERATION_ADD -> "+";
            case OPERATION_MINUS -> "-";
            case OPERATION_MULTIPLY -> "*";
            case OPERATION_DIVIDE -> "/";
            default -> "?";
          };
          int leftPrec = getPrecedence();
          int rightPrec = isCommutAssoc ? getPrecedence() : getPrecedence() + 1;
          String leftStr = left.getExpression(leftPrec);
          String rightStr = right.getExpression(rightPrec);
          s = leftStr + opStr + rightStr;
          myPrec = getPrecedence();
        }
      }
      default -> {
        s = "ERROR";
        myPrec = 0;
      }
    }
    if (myPrec < parentPrec) {
      s = "(" + s + ")";
    }
    return s;
  }

  public void printTreeStructure() {
    printTreeStructure(0);
  }

  private void printTreeStructure(int level) {
    String indent = "  ".repeat(level);
    System.out.println(indent + type + (value != null ? "('" + value + "')" : ""));
    if (left != null) {
      System.out.println(indent + "Left:");
      left.printTreeStructure(level + 1);
    }
    if (right != null) {
      System.out.println(indent + "Right:");
      right.printTreeStructure(level + 1);
    }
    if (param != null) {
      System.out.println(indent + "Param:");
      param.printTreeStructure(level + 1);
    }
  }

  @Override
  public String toString() {
    return getExpression();
  }
}