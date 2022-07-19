package org.server;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Coordinator extends Server implements ICoordinator {

  private final String programCommandLineExpression =
      "\njava -jar Coordinator.jar <port1> <port2> ... <port5>" ;

  private List<Integer> serverPorts = new ArrayList<>();
  private Map<Integer, IServer> serverStubs = new HashMap<>();

  ConcurrentHashMap<Integer, List<UUID>> serverPreparationResponses = new ConcurrentHashMap<>();
  ConcurrentHashMap<Integer, List<UUID>> serverCommitResponses = new ConcurrentHashMap<>();
//  private Map<Integer, Registry> serverRegistries = new HashMap<>();

  public Coordinator(Integer portNumber) {
    super(portNumber);
  }


  public static void main(String[] args) {
    // Coordinator's default port is hardcoded at 10000;
    IServer server = new Coordinator(10000);
    Coordinator coordinator = ((Coordinator) server);
    if (coordinator.canParseProgramArgs(args)) {
      try {
        server = (IServer) UnicastRemoteObject.exportObject(coordinator, 10000);
        Registry registry = LocateRegistry.createRegistry(10000);
        registry.bind("COORDINATOR", server);
        coordinator.setUpServerStubsAndRegistries();
        coordinator.broadCastOtherServerPorts();
      } catch (Exception e) {
        System.err.println("Coordinator exception: " + e.toString());
        e.printStackTrace();
        System.exit(1);
      }
    } // No need for else statement to follow 'if (newClient.canParseProgramArgs(args))'
    // Program will exit on its own if argument requirements are not satisfied.
  }


  @Override
  public String handleRequest(UUID requestID, String operation, String key, String value)
      throws RemoteException {
    String greetingFromCoordinator = String.format("Coordinator at port: %s says Hello!",
        this.myPortNumber) + " Coordinator is ready to start the 2PC process.";
    String messageToClient = greetingFromCoordinator
        + handlePrepare(requestID, operation, key, value);
    if (messageToClient.contains("failed to prepare")) {
      return messageToClient;
    } else {
      return messageToClient + handleCommit(requestID, operation, key, value);
    }
  }


  private String handlePrepare(UUID requestID, String operation, String key, String value) {
    dispatchPrepare(requestID, operation, key, value);
    long startTime = System.currentTimeMillis();
    long elapsedTime = 0L;
    boolean allServersHasPrepared;
    do {
      allServersHasPrepared =
          this.serverPorts.stream()
              .map(port -> this.serverPreparationResponses.get(port))
              .map((list) -> {
                boolean statuses = list.contains(requestID) == true;
                return statuses;
              })
              .collect(Collectors.toList())
              .size() == serverPorts.size();
      elapsedTime = (new Date()).getTime() - startTime;
      // If a minutes has passed and not all servers responded, return a failure message
      if (elapsedTime > 60 * 1000) {
        return String.format("\nCoordinator failed to prepare servers for request %s.", requestID);
      }
    } while (!allServersHasPrepared);
    return String.format(
        "\nCoordinator Succeeded to affirm that all servers prepared for request %s.", requestID);
  }


  // Called by server to update coordinator's record of serverPreparationResponses
  public void updatePreparationResponses(Integer port, UUID requestID) {
    ((Coordinator) this).serverPreparationResponses.put(
        port, serverPreparationResponses.getOrDefault(port, Arrays.asList(requestID))
    );
  }


  private void dispatchPrepare(UUID requestID, String operation, String key, String value) {
    ((Coordinator) this).serverPorts.forEach((port) -> {
      IServer stub = ((Coordinator) this).serverStubs.get(port);
      try {
        stub.prepare(requestID, operation, key, value);
      } catch (RemoteException | NotBoundException e) {
        throw new RuntimeException(e);
      }
    });
  }


  private String handleCommit(UUID requestID, String operation, String key, String value) {
    ((Coordinator) this).dispatchCommit(requestID, operation, key, value);
    long startTime = System.currentTimeMillis();
    long elapsedTime = 0L;
    boolean allServersHasCommited;
    String coordinatorFailedToCommit =
        String.format("\nCoordinator failed to commit servers for for request %s.", requestID);
    do {
      try {
        allServersHasCommited =
            this.serverPorts.stream()
                .map(port -> this.serverCommitResponses.get(port))
                .map((list) -> {
                  if (list.size() == 0) {
                    return coordinatorFailedToCommit;
                  }
                  boolean statuses = list.contains(requestID) == true;
                  return statuses;
                })
                .collect(Collectors.toList())
                .size() == serverPorts.size();
        elapsedTime = (new Date()).getTime() - startTime;
      } catch (Exception e) {
        return coordinatorFailedToCommit + "\n" + e;
      }
      // If a minutes has passed and not all servers responded, return a failure message
      if (elapsedTime > 60 * 1000) {
        return coordinatorFailedToCommit;
      }
    } while (!allServersHasCommited);
    return String.format(
        "\nCoordinator Succeeded to affirm that all servers committed for request %s.",
        requestID);
  }

  // Called by server to update coordinator's record of serverCommitResponses
  public void updateCommitResponses(Integer port, UUID requestID) {
    ((Coordinator) this).serverCommitResponses.put(
        port, serverCommitResponses.getOrDefault(port, Arrays.asList(requestID))
    );
  }

  private void dispatchCommit(UUID requestID, String operation, String key, String value) {
    ((Coordinator) this).serverPorts.forEach((port) -> {
      IServer stub = ((Coordinator) this).serverStubs.get(port);
      try {
        stub.commit(requestID, operation, key, value);
      } catch (RemoteException | NotBoundException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private boolean canParseProgramArgs(String[] args) {
    System.out.println(
        String.format("Coordinator Program starting... Received %s arguments.", args.length));
    System.out.println("Waiting for Client ...");
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

  private void setUpServerStubsAndRegistries() {
    ((Coordinator) this).serverPorts
        .stream()
        .forEach((portNumber) -> {
          IServer server = new Server(portNumber);
          Registry registry;
          try {
            server = (IServer) UnicastRemoteObject.exportObject(server, portNumber);
            registry = LocateRegistry.createRegistry(portNumber);
            registry.bind("SERVER", server);
          } catch (RemoteException | AlreadyBoundException e) {
            System.err.println("\n" + String.format(
                "Coordinator is unable to bind a registry for server at port number: %s.",
                portNumber));
            throw new RuntimeException(e);
          }
          this.serverStubs.put(portNumber, server);
//          this.serverRegistries.put(portNumber, registry);
        });
  }

  private void broadCastOtherServerPorts() throws Exception {
    Set<Integer> otherServers = new HashSet<>();
    ((Coordinator) this).serverPorts.forEach(
        (self) -> {
          try {
            Registry registry = LocateRegistry.getRegistry(self);
            IServer stub = (IServer) registry.lookup("SERVER");
            for (Integer port : serverPorts) {
              if (port != self) {
                otherServers.add(port);
              }
            }
            stub.setOtherServers(otherServers);
          } catch (RemoteException | NotBoundException e) {
            System.out.println(
                "Coordinator cannot connect to server at the following port " + self);
            throw new RuntimeException(e);
          }
        });
  }
}



