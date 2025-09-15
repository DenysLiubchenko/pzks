package edu.kpi.lab.lab1.model;

import java.util.EmptyStackException;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class LexicalAnalyzer {

  public List<Token> processMathSentence(String sentence) {
    if (sentence.isBlank()) {
      throw new IllegalArgumentException("Sentence is empty");
    }

    char[] charArray = sentence.toCharArray();
    Token currentToken = new Token(0);
    List<Token> query = new LinkedList<>();
    Stack<TokenType> openedBrackets = new Stack<>();

    for(int i = 0; i < charArray.length; i++) {

      if (charArray[i] == '(') {
        if (currentToken.getTokenType() == TokenType.CONSTANT) {
          currentToken.setTokenType(TokenType.FUNCTION);
          openedBrackets.push(TokenType.FUNCTION_OPEN_BRACKET);
          currentToken = addAndFinishAnother(currentToken, i, query, TokenType.FUNCTION_OPEN_BRACKET, charArray);
        } else {
          openedBrackets.push(TokenType.OPEN_BRACKET);
          currentToken = addAndFinishAnother(currentToken, i, query, TokenType.OPEN_BRACKET, charArray);
        }
      } else if (charArray[i] == ')') {
        try {
          TokenType lastOpenedBracket = openedBrackets.pop();
          if (lastOpenedBracket == TokenType.FUNCTION_OPEN_BRACKET) {
            currentToken = addAndFinishAnother(currentToken, i, query, TokenType.FUNCTION_CLOSE_BRACKET, charArray);
          } else if (lastOpenedBracket == TokenType.OPEN_BRACKET) {
            currentToken = addAndFinishAnother(currentToken, i, query, TokenType.CLOSE_BRACKET, charArray);
          } else {
            currentToken = addAndFinishAnother(currentToken, i, query, TokenType.CLOSE_BRACKET, charArray);
          }
        } catch (EmptyStackException e) {
          currentToken = addAndFinishAnother(currentToken, i, query, TokenType.CLOSE_BRACKET, charArray);
        }
      } else if (charArray[i] == '+' || charArray[i] == '-') {
        currentToken = addAndFinishAnother(currentToken, i, query, TokenType.OPERATION_ADD_OR_MINUS, charArray);
      } else if (charArray[i] == '*' || charArray[i] == '/') {
        currentToken = addAndFinishAnother(currentToken, i, query, TokenType.OPERATION_MULTIPLY_OR_DIVIDE, charArray);
      } else if (charArray[i] == '.') {
        if (currentToken.getTokenType() == TokenType.INTEGER) {
          currentToken.setTokenType(TokenType.DECIMAL);
          currentToken.addToValue(charArray[i]);
        } else if (currentToken.getTokenType() == TokenType.DECIMAL || currentToken.getTokenType() == TokenType.FUNCTION) {
          currentToken.addToValue(charArray[i]);
        } else {
          currentToken = addAndFinishAnother(currentToken, i, query, TokenType.ERROR, charArray);
        }
      } else if (Character.isDigit(charArray[i])) {
        if (currentToken.getTokenType() == null) {
          currentToken.setTokenType(TokenType.INTEGER);
          currentToken.addToValue(charArray[i]);
        } else if (currentToken.getTokenType() == TokenType.INTEGER || currentToken.getTokenType() == TokenType.DECIMAL || currentToken.getTokenType() == TokenType.FUNCTION) {
          currentToken.addToValue(charArray[i]);
        } else {
          currentToken = addAndFinishAnother(currentToken, i, query, TokenType.ERROR, charArray);
        }
      } else if (Character.isLetter(charArray[i])) {
        if (currentToken.getTokenType() == null) {
          currentToken.setTokenType(TokenType.CONSTANT);
          currentToken.addToValue(charArray[i]);
        } else if (currentToken.getTokenType() == TokenType.CONSTANT || currentToken.getTokenType() == TokenType.FUNCTION) {
          currentToken.addToValue(charArray[i]);
        } else {
          currentToken = addAndFinishAnother(currentToken, i, query, TokenType.ERROR, charArray);
        }
      } else {
        currentToken = addAndFinishAnother(currentToken, i, query, TokenType.ERROR, charArray);
      }
    }
    if (currentToken.getTokenType() != null) {
      finishProcessing(currentToken, charArray.length - 1, query);
    }

    return query;
  }

  private Token addAndFinishAnother(Token currentToken, int i, List<Token> query, TokenType tokenType, char[] charArray) {
    if (currentToken.getTokenType() != null) {
      currentToken = finishProcessing(currentToken, i-1, query);
    }
    currentToken.setTokenType(tokenType);
    currentToken.addToValue(charArray[i]);
    currentToken = finishProcessing(currentToken, i, query);
    return currentToken;
  }

  private Token finishProcessing(Token currentToken, int i, List<Token> query) {
    currentToken.setEndPosition(i);
    query.add(currentToken);
    currentToken = new Token(i + 1);
    return currentToken;
  }
}
