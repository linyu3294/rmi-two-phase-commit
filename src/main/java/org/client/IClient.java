package org.client;

/**
 * To build the client, please find instructions in
 * Either the README.md or in Client.java
 */

public interface IClient {
  // This function: promptUserInput is blocking code
  public String promptUserInput () ;

  public String[] parseUserInput(String userInput);
}
