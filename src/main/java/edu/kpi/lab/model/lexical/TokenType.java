package edu.kpi.lab.model.lexical;

import java.util.Set;

public enum TokenType {
  INTEGER, DECIMAL, CONSTANT, FUNCTION, FUNCTION_OPEN_BRACKET, FUNCTION_CLOSE_BRACKET, OPERATION_ADD, OPERATION_MINUS,
  OPERATION_MULTIPLY, OPERATION_DIVIDE, OPEN_BRACKET, CLOSE_BRACKET, ERROR;

  public static final Set<TokenType> operandTypes =
    Set.of(TokenType.INTEGER, TokenType.DECIMAL, TokenType.CONSTANT, TokenType.FUNCTION);
}
