package edu.kpi.lab.model.syntax.tree;

import edu.kpi.lab.model.syntax.SyntaxType;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Function extends Node{
  private Node left;

  private Node right;

  private SyntaxType operation;

  public void printTreeStructure() {
    System.out.println("Parallel Tree Structure:");
    printTreeStructure(this, 0, "Root");
    System.out.println("Tree height: " + getTreeHeight(this));
    System.out.println("Max width: " + getMaxWidth(this));
  }

  private void printTreeStructure(Node node, int level, String position) {
    if (node == null) {
      return;
    }

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
      System.out.println(
        indent + position + ": Operand(type=" + operand.getType() + ", value=" + operand.getValue() + ")");
    }
  }

  public int getTreeHeight(Node node) {
    if (node == null) {
      return 0;
    }

    if (node instanceof Operand) {
      return 1;
    }

    if (node instanceof Function function) {
      int leftHeight = getTreeHeight(function.getLeft());
      int rightHeight = getTreeHeight(function.getRight());
      return Math.max(leftHeight, rightHeight) + 1;
    }

    return 0;
  }

  public int getMaxWidth(Function root) {
    if (root == null) {
      return 0;
    }

    Map<Integer, Integer> levelCounts = new HashMap<>();
    countNodesAtLevel(root, 0, levelCounts);

    return levelCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
  }

  private void countNodesAtLevel(Node node, int level, Map<Integer, Integer> levelCounts) {
    if (node == null) {
      return;
    }

    levelCounts.merge(level, 1, Integer::sum);

    if (node instanceof Function function) {
      countNodesAtLevel(function.getLeft(), level + 1, levelCounts);
      countNodesAtLevel(function.getRight(), level + 1, levelCounts);
    }
  }
}
