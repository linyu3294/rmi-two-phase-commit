package org.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.server.IServer;
import java.util.UUID;

/**
 * To Build the Client, use maven.
 * The pom.xml is attached. When installed, the program will produce two jars in target.
 * One for the Client.jar and the other for Coordinator.jar.
 * When starting the program, make sure to run the Coordinator.jar first.
 * After the Coordinator.jar is running, next to run the Client, find the Client.jar in target.
 * Run java -jar Client.jar <port1 num> <port2 num> ... <port5 num>
 */
public class Client implements IClient {

  private List<Integer> serverPorts = new ArrayList<>();
  private Map<Integer, IServer> serverStubs = new HashMap<>();
  private Map<Integer, Registry> serverRegistries = new HashMap<>();


  private final String programCommandLineExpression =
      "\njava -jar " + this.getClass().getName() + "<port1> <port2> ... <port5>";

  public Client() {
  }


  public static void main(String args[]) {
    boolean hasPrefilled = false;
    Client newClient = new Client();
    if (newClient.canParseProgramArgs(args)) {
      try {
        String userInput = "";
        do {
          newClient.setUpServerStubsAndRegistries();
          if (!hasPrefilled) {
            hasPrefilled = true;
            newClient.prefillServerStore(newClient.serverPorts.get(0));
          }
          userInput = newClient.promptUserInput();
          if (newClient.canParseUserInput(userInput)) {
            String[] inputStrings = newClient.parseUserInput(userInput);
            Integer serverNumber = Integer.parseInt(inputStrings[0]);
            String response = newClient.serverStubs.get(serverNumber)
                .handleRequest
                    (
                        UUID.randomUUID(),
                        inputStrings[1],
                        inputStrings[2],
                        inputStrings.length == 4 ? inputStrings[3] : ""
                    );
            System.out.println("\nresponse: " + response);
          }
        } while (!userInput.toUpperCase().contains("EXIT"));
      } catch (Exception e) {
        System.err.println("\nClient exception: " + e.toString());
        e.printStackTrace();
        System.exit(1);
      }
    } // No need for else statement to follow 'if (newClient.canParseProgramArgs(args))'
    // Program will exit on its own if argument requirements are not satisfied.
  }


  @Override
  public String promptUserInput() {
    System.out.println("\n\n"
        + "The server stores a map of key and value pairs on our collection of servers.\n"
        + "Please instruct one of our servers what to do next by choosing from one of the following commands.\n"
        + "    1) <SERVER No.> PUT <KEY> <VALUE> \n"
        + "    2) <SERVER No.> GET <KEY> \n"
        + "    3) <SERVER No.> DELETE <KEY> \n"
        + "Note that you can quit the program by typing the word 'EXIT'. \n"
    );
    Scanner input = new Scanner(System.in);
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    System.out.print(formatter.format(date) + " | Type here  :   ");
    String userCmd = input.nextLine();
    return userCmd;
  }

  @Override
  public String[] parseUserInput(String userInput) {
    String[] userInputStrings = userInput.trim().split(" ");
    return userInputStrings;
  }

  private boolean canParseUserInput(String userInput) {
    boolean canParse = false;
    if (userInput.split(" ").length > 0) {
      String[] userInputStrings = userInput.split(" ");
      List<String> portStrings = new ArrayList<>();
      this.serverPorts.stream().forEach(x -> portStrings.add(x.toString()));
      if (!portStrings.contains(userInputStrings[0].trim())) {
        System.out.println("The most recent user command had incorrect format!");
        System.out.println("Please check the format and re-enter a new command!");
        return false;
      }
      if (userInput.split(" ").length == 3) {
        if (userInputStrings[1].trim().equalsIgnoreCase("GET")) {
          return true;
        }
        if (userInputStrings[1].trim().equalsIgnoreCase("DELETE")) {
          return true;
        }
      } else if (userInput.split(" ").length == 4) {
        if (userInputStrings[1].trim().equalsIgnoreCase("PUT")) {
          return true;
        }
      }
    }
    if (canParse == false) {
      System.out.println("The most recent user command had incorrect format!");
      System.out.println("Please check the format and re-enter a new command!");
    }
    return canParse;
  }


  private boolean canParseProgramArgs(String[] args) {
    System.out.println(
        String.format("Client Program starting... Received %s arguments.", args.length));
    if (args.length < 5) {
      System.out.println("\n"
          + "\n" + "Please provide at least 5 ports."
          + "\n" + "Command-line arguments must conform to the following format"
          + "\n" + this.programCommandLineExpression
      );
      System.exit(1);
    }
    for (String arg : args) {
      try {
        int portNumber = Integer.parseInt(arg);
        this.serverPorts.add(portNumber);
      } catch (Exception e) {
        System.out.println("\n"
            + "\n" + "Unable to parse some of the arguments provided."
            + "\n"
            + "Please make sure that the command-line arguments conform to the following format:"
            + "\n" + this.programCommandLineExpression
        );
        System.exit(1);
      }
    }
    return true;
  }

  private void setUpServerStubsAndRegistries() throws NotBoundException, RemoteException {
    this.serverPorts
        .stream()
        .forEach((portNumber) -> {
          IServer server;
          Registry registry;
          try {
            registry = LocateRegistry.getRegistry("LOCALHOST", portNumber);
            server = (IServer) registry.lookup("SERVER");
          } catch (RemoteException | NotBoundException e) {
            System.err.println("\n" + String.format(
                "Client is unable to create a registry for server at port number: %s", portNumber));
            throw new RuntimeException(e);
          }
          this.serverStubs.put(portNumber, server);
          this.serverRegistries.put(portNumber, registry);
        });

  }


  private void prefillServerStore(Integer port) throws NotBoundException, RemoteException {
    System.out.println("\nTransacting initial 5 PUTS");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "PUT", "APPLE", "$1");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "PUT", "ORANGE", "$2");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "PUT", "BANANA", "$5");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "PUT", "KIWI", "$9");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "PUT", "WATERMELON", "$3");
    System.out.println("\nTransacting initial 5 GETS");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "GET", "APPLE", "");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "GET", "ORANGE", "");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "GET", "BANANA", "");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "GET", "KIWI", "");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "GET", "WATERMELON", "");
    System.out.println("\nTransacting initial 5 DELETES");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "DELETE", "APPLE", "");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "DELETE", "ORANGE", "");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "DELETE", "BANANA", "");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "DELETE", "KIWI", "");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "DELETE", "WATERMELON", "");
    System.out.println("\nTransacting an extra 5 PUTS for testing");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "PUT", "APPLE", "$1");
    System.out.println("10001 PUT APPLE $1");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "PUT", "ORANGE", "$2");
    System.out.println("10001 PUT ORANGE $2");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "PUT", "BANANA", "$5");
    System.out.println("10001 PUT BANANA $5");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "PUT", "KIWI", "$9");
    System.out.println("10001 PUT KIWI $9");
    this.serverStubs.get(port)
        .handleRequest(UUID.randomUUID(), "PUT", "WATERMELON", "$3");
    System.out.println("10001 PUT WATERMELON $3");
  }
}


