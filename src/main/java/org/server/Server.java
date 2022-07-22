package org.server;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Running Coordinator.jar will spin up 5 individual servers. Please find instructions to run
 * Coordinator in either Coordinator.java or in README.md.
 */
public class Server extends Thread implements IServer {


  Integer myPortNumber = null;
  Set<Integer> otherServers = new HashSet<>();
  boolean lock = false;
  ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

  public Server(Integer portNumber) {
    this.myPortNumber = portNumber;
  }

  @Override
  public String sayHello() {
    return String.format("Server at port: %s says Hello!", this.myPortNumber);
  }

  @Override
  public Integer getPortNumber() {
    return myPortNumber;
  }

  @Override
  public void setOtherServers(Set<Integer> otherServers) {
    this.otherServers = otherServers;
  }

  @Override
  public String handleRequest(UUID requestID, String operation, String key, String value)
      throws RemoteException, NotBoundException {
    // GET operations does not need 2PC
    if (operation.equalsIgnoreCase("GET")) {
      return store.getOrDefault(key, ""
          + "\nThe value you queried is NOT Currently In store."
          + "\nAborting Prepare and Commit Phase.");
    }
    // Get the registry of the coordinator at port hardcoded as 10000
    Registry registry = LocateRegistry.getRegistry("LOCALHOST", 10000);
    IServer coordinator = (IServer) registry.lookup("COORDINATOR");

    // Upon receiving a request, delegate the task of 2PC to the coordinator.
    return coordinator.handleRequest(requestID, operation, key, value);
  }

  @Override
  public String prepare(UUID requestID, String operation, String key, String value)
      throws RemoteException, NotBoundException {
    try {
      // Get the registry of the coordinator at port hardcoded as 10000
      Registry registry = LocateRegistry.getRegistry("LOCALHOST", 10000);
      ICoordinator coordinator = (ICoordinator) registry.lookup("COORDINATOR");
      coordinator.updatePreparationResponses(((IServer) this).getPortNumber(), requestID);
      return String.format("\nSuccess | Server at port | %s | is prepared.",
          ((IServer) this).getPortNumber());
    } catch (Exception e) {
      return String.format("\nFailure | Server at port | %s | failed to prepare.",
          ((IServer) this).getPortNumber());
    }
  }

  @Override
  public String commit(UUID requestID, String operation, String key, String value)
      throws RemoteException, NotBoundException {
    // Get the registry of the coordinator at port hardcoded as 10000
    Registry registry = LocateRegistry.getRegistry("LOCALHOST", 10000);
    ICoordinator coordinator = (ICoordinator) registry.lookup("COORDINATOR");
    boolean isCommitSuccessful = false;
    if (operation.equalsIgnoreCase("PUT")) {
      isCommitSuccessful = putKeyValue(key, value);
    } else if (operation.equalsIgnoreCase("DELETE")) {
      isCommitSuccessful = deleteKeyValue(key);
    }
    if (isCommitSuccessful) {
      coordinator.updateCommitResponses(((IServer) this).getPortNumber(), requestID);
      return String.format("\nSuccess | Server at port | %s | is committed.",
          ((IServer) this).getPortNumber());
    }
    return String.format("\nFailure | Server at port | %s | failed to commit.",
        ((IServer) this).getPortNumber());
  }

  private boolean putKeyValue(String key, String value) {
    this.store.put(key, value);
    if (this.store.get(key) == value) {
      return true;
    }
    return false;
  }

  private boolean deleteKeyValue(String key) {
    synchronized (store) {
      this.store.remove(key);
    }
    if (this.store.get(key) != null) {
      return true;
    }
    return false;
  }


}
