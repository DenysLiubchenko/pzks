package edu.kpi.lab.model.lexical;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Token {

  private Integer startPosition;

  private Integer endPosition;

  private TokenType tokenType;

  private String value;

  public Token(Integer startPosition) {
    this.startPosition = startPosition;
  }

  public void addToValue(Character character) {
    if (value != null) {
      value += character;
    } else {
      value = character.toString();
    }
  }

  @Override
  public String toString() {
    return tokenType + "('" + value + "') [" + startPosition + "; " + endPosition + "]";
  }
}
