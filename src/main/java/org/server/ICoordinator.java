package org.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

public interface ICoordinator extends IServer , Remote {

  // Called by servers after preparation phase is dispatched.
  public void updatePreparationResponses(Integer port, UUID requestID) throws RemoteException;

  // Called by servers after commit phase is dispatched.
  public void updateCommitResponses(Integer port, UUID requestID) throws RemoteException;
}
