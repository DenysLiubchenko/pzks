package edu.kpi.lab.lab1.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import static edu.kpi.lab.lab1.model.SyntaxType.CLOSE_BRACKET;
import static edu.kpi.lab.lab1.model.SyntaxType.ERROR;
import static edu.kpi.lab.lab1.model.SyntaxType.FINISH;
import static edu.kpi.lab.lab1.model.SyntaxType.FUNCTION;
import static edu.kpi.lab.lab1.model.SyntaxType.FUNCTION_CLOSE_BRACKET;
import static edu.kpi.lab.lab1.model.SyntaxType.FUNCTION_OPEN_BRACKET;
import static edu.kpi.lab.lab1.model.SyntaxType.OPEN_BRACKET;
import static edu.kpi.lab.lab1.model.SyntaxType.OPERAND;
import static edu.kpi.lab.lab1.model.SyntaxType.OPERATION_ADD_OR_MINUS;
import static edu.kpi.lab.lab1.model.SyntaxType.OPERATION_MULTIPLY_OR_DIVIDE;
import static edu.kpi.lab.lab1.model.SyntaxType.START;

public class SyntaxAnalyzer {

  private final Map<SyntaxType, Set<SyntaxType>> allowedCombinations;
  private final Set<TokenType> operandTypes;

  {
    operandTypes = Set.of(TokenType.INTEGER, TokenType.DECIMAL, TokenType.CONSTANT, TokenType.FUNCTION);
    allowedCombinations = new HashMap<>();
    allowedCombinations.put(START,
      Set.of(OPEN_BRACKET, OPERAND, FUNCTION, OPERATION_ADD_OR_MINUS)); // replace OPERAND with FIRST_OPERAND
    allowedCombinations.put(OPEN_BRACKET,
      Set.of(OPEN_BRACKET, OPERAND, FUNCTION, OPERATION_ADD_OR_MINUS)); // replace OPERAND with FIRST_OPERAND
//    allowedCombinations.put(FIRST_OPERAND, Set.of(OPERATION_ADD_OR_MINUS, OPERATION_MULTIPLY_OR_DIVIDE));
    allowedCombinations.put(OPERATION_ADD_OR_MINUS, Set.of(OPERAND, FUNCTION, OPEN_BRACKET)); // add FIRST_OPERAND
    allowedCombinations.put(OPERATION_MULTIPLY_OR_DIVIDE, Set.of(OPEN_BRACKET, FUNCTION, OPERAND));
    allowedCombinations.put(OPERAND,
      Set.of(OPERATION_ADD_OR_MINUS, OPERATION_MULTIPLY_OR_DIVIDE, CLOSE_BRACKET, FUNCTION_CLOSE_BRACKET, FINISH));
    allowedCombinations.put(CLOSE_BRACKET,
      Set.of(OPERATION_ADD_OR_MINUS, OPERATION_MULTIPLY_OR_DIVIDE, CLOSE_BRACKET, FUNCTION_CLOSE_BRACKET, FINISH));
    allowedCombinations.put(FUNCTION, Set.of(FUNCTION_OPEN_BRACKET));
    allowedCombinations.put(FUNCTION_OPEN_BRACKET,
      Set.of(FUNCTION, OPERAND, OPEN_BRACKET, FUNCTION_CLOSE_BRACKET)); // replace OPERAND with FIRST_OPERAND
    allowedCombinations.put(FUNCTION_CLOSE_BRACKET,
      Set.of(OPERATION_ADD_OR_MINUS, OPERATION_MULTIPLY_OR_DIVIDE, CLOSE_BRACKET, FUNCTION_CLOSE_BRACKET, FINISH));
    allowedCombinations.put(ERROR, Arrays.stream(SyntaxType.values()).filter(st-> st != ERROR).collect(Collectors.toSet()));
  }

  public void processTokenQuery(List<Token> tokens) {
    List<Error> errors = new ArrayList<>();
    Stack<SyntaxType> openedBrackets = new Stack<>();

    validateTokenWith(errors, openedBrackets, tokens.getFirst(), allowedCombinations.get(START));

    for (int i = 1; i < tokens.size(); i++) {
      validateTokenWith(errors, openedBrackets,
        tokens.get(i), allowedCombinations.get(getTokenSyntaxType(tokens.get(i - 1))));
    }

    Token lastToken = tokens.getLast();
    if (!Set.of(CLOSE_BRACKET, FUNCTION_CLOSE_BRACKET, OPERAND, ERROR).contains(getTokenSyntaxType(lastToken))) {
      errors.add(new Error(lastToken.getEndPosition(), lastToken.getEndPosition(), "Query cannot be finished with " + lastToken.getTokenType()));
    }
    if (!openedBrackets.isEmpty()) {
      errors.add(new Error(lastToken.getEndPosition(), lastToken.getEndPosition(), "There are unclosed brackets in query"));
    }

    System.out.println("Errors: ");
    for (int i = 0; i < errors.size(); i++) {
      System.out.println(i+1 + ". " + errors.get(i));
    }
  }

  private void validateTokenWith(List<Error> errors, Stack<SyntaxType> openedBrackets, Token token,
                                 Set<SyntaxType> allowedPositions) {
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
    } else if (tokenType.equals(TokenType.OPERATION_ADD_OR_MINUS)) {
      return OPERATION_ADD_OR_MINUS;
    } else if (tokenType.equals(TokenType.OPERATION_MULTIPLY_OR_DIVIDE)) {
      return OPERATION_MULTIPLY_OR_DIVIDE;
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













