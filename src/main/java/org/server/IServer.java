package org.server;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.UUID;

/**
 * To build the Coordinator, please find instructions in
 * Either the README.md or in Coordinator.java
 * Running the coordinator will spin up the 5 servers.
 */
public interface IServer extends Remote {

  public String sayHello() throws RemoteException;

  public Integer getPortNumber() throws RemoteException;

  public void setOtherServers(Set<Integer> otherServers) throws RemoteException;

  public String handleRequest(UUID requestID, String operation, String key, String value) throws RemoteException, NotBoundException;
  public String prepare (UUID requestID, String operation, String key, String value) throws RemoteException, NotBoundException ;

  public String commit (UUID requestID, String operation, String key, String value) throws RemoteException, NotBoundException ;
}
