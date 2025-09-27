package edu.kpi.lab.model.syntax;

import edu.kpi.lab.model.lexical.Token;
import edu.kpi.lab.model.lexical.TokenType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class SyntaxValidator {
  private final Map<SyntaxType, Set<SyntaxType>> allowedCombinations;

  {
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
      Set.of(OPERATION_ADD, OPERATION_MINUS, OPERATION_MULTIPLY, OPERATION_DIVIDE, CLOSE_BRACKET,
        FUNCTION_CLOSE_BRACKET, FINISH));
    allowedCombinations.put(CLOSE_BRACKET,
      Set.of(OPERATION_ADD, OPERATION_MINUS, OPERATION_MULTIPLY, OPERATION_DIVIDE, CLOSE_BRACKET,
        FUNCTION_CLOSE_BRACKET, FINISH));
    allowedCombinations.put(FUNCTION, Set.of(FUNCTION_OPEN_BRACKET));
    allowedCombinations.put(FUNCTION_OPEN_BRACKET,
      Set.of(FUNCTION, OPERAND, OPEN_BRACKET, FUNCTION_CLOSE_BRACKET));
    allowedCombinations.put(FUNCTION_CLOSE_BRACKET,
      Set.of(OPERATION_ADD, OPERATION_MINUS, OPERATION_MULTIPLY, OPERATION_DIVIDE, CLOSE_BRACKET,
        FUNCTION_CLOSE_BRACKET, FINISH));
    allowedCombinations.put(ERROR,
      Arrays.stream(SyntaxType.values()).filter(st -> st != ERROR).collect(Collectors.toSet()));
  }

  public boolean validateTokenQuery(List<Token> tokens) {
    List<Error> errors = new ArrayList<>();
    Stack<SyntaxType> openedBrackets = new Stack<>();

    validateTokenWith(errors, openedBrackets, tokens.getFirst(), START);

    for (int i = 1; i < tokens.size(); i++) {
      validateTokenWith(errors, openedBrackets,
        tokens.get(i), getTokenSyntaxType(tokens.get(i - 1)));
    }

    Token lastToken = tokens.getLast();
    if (!Set.of(CLOSE_BRACKET, FUNCTION_CLOSE_BRACKET, OPERAND, ERROR).contains(getTokenSyntaxType(lastToken))) {
      errors.add(new Error(lastToken.getEndPosition(), lastToken.getEndPosition(),
        "Query cannot be finished with " + lastToken.getTokenType()));
    }
    if (!openedBrackets.isEmpty()) {
      errors.add(
        new Error(lastToken.getEndPosition(), lastToken.getEndPosition(), "There are unclosed brackets in query"));
    }

    if (errors.isEmpty()) {
      System.out.println("Query has no errors");
      return true;
    } else {
      System.out.println("Errors: ");
      for (int i = 0; i < errors.size(); i++) {
        System.out.println(i + 1 + ". " + errors.get(i));
      }
      return false;
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

      if (token.getTokenType().equals(TokenType.INTEGER) && token.getValue().equals("0") &&
          previousTokenSyntaxType.equals(OPERATION_DIVIDE)) {
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

  public static SyntaxType getTokenSyntaxType(Token token) {
    TokenType tokenType = token.getTokenType();
    if (TokenType.operandTypes.contains(tokenType)) {
      if (tokenType.equals(TokenType.FUNCTION)) {
        return FUNCTION;
      } else {
        return OPERAND;
      }
    } else if (tokenType.equals(TokenType.OPERATION_ADD)) {
      return OPERATION_ADD;
    } else if (tokenType.equals(TokenType.OPERATION_MINUS)) {
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
      return "Error: " + message + " On position [" + startPosition + "; " + endPosition + "]";
    }
  }
}
