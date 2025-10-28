package edu.kpi.lab.model.transform.associative.node;

import edu.kpi.lab.model.lexical.Token;
import edu.kpi.lab.model.lexical.TokenType;
import java.util.List;

public class Parser {
  private final List<Token> tokens;
  private int pos = 0;

  public Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  public Expression parse() {
    return parseExpression();
  }

  private Expression parseExpression() {
    Expression expression = parseTerm();
    while (pos < tokens.size()) {
      Token t = peek();
      if (t.getTokenType() != TokenType.OPERATION_ADD && t.getTokenType() != TokenType.OPERATION_MINUS) {
        break;
      }
      advance();
      Expression right = parseTerm();
      expression = new BinaryExpression(expression, t.getValue(), right);
    }
    return expression;
  }

  private Expression parseTerm() {
    Expression expression = parseFactor();
    while (pos < tokens.size()) {
      Token t = peek();
      if (t.getTokenType() != TokenType.OPERATION_MULTIPLY && t.getTokenType() != TokenType.OPERATION_DIVIDE) {
        break;
      }
      advance();
      Expression right = parseFactor();
      expression = new BinaryExpression(expression, t.getValue(), right);
    }
    return expression;
  }

  private Expression parseFactor() {
    Token t = peek();
    if (t.getTokenType() == TokenType.OPERATION_MINUS) {
      advance();
      return new UnaryExpression("-", parseFactor());
    } else if (t.getTokenType() == TokenType.OPEN_BRACKET) {
      advance();
      Expression expression = parseExpression();
      if (peek().getTokenType() == TokenType.CLOSE_BRACKET) {
        advance();
      }
      return expression;
    } else if (t.getTokenType() == TokenType.FUNCTION) {
      advance();
      if (peek().getTokenType() == TokenType.FUNCTION_OPEN_BRACKET) {
        advance();
      }
      Expression arg = parseExpression();
      if (peek().getTokenType() == TokenType.FUNCTION_CLOSE_BRACKET) {
        advance();
      }
      return new FunctionExpression(t.getValue(), arg);
    } else if (t.getTokenType() == TokenType.INTEGER || t.getTokenType() == TokenType.DECIMAL ||
        t.getTokenType() == TokenType.CONSTANT) {
      advance();
      return new Literal(t.getValue(), t.getTokenType());
    }
    return null; // error
  }

  private Token peek() {
    if (pos >= tokens.size()) {
      return new Token(0, 0, TokenType.ERROR, "");
    }
    return tokens.get(pos);
  }

  private void advance() {
    pos++;
  }
}

