High Level Description

The program is divided into two parts, the server and the client packages.
I refactored my previous assignment 2 in order to build off that in a clear and compact way.
In implementing the server, I chose to extend the coordinator from the server. The Server implement IServer which extends the Remote. This is the stub used to have coordinator borrow its methods.

The Coodinator is the first process that kicks off. Once the coordinator starts, it will generate the servers for the user on different ports.
Since it inherits from the IServer, it has all the capacities of the server, and it also has extra capacities in addition to a typical server, such as initiating the preparation phase and Commit phase, as well as listening to the response from the server for results from the server during those phases.


The client will be the second process to start after the coordinator has already started. I kept the client simple. it starts from a command line expression that includes the port of each of the servers. A user can enter more than 5 port to be clear. 
The client then performs the 5 of each of the CRUD operations. The user is then prompted to enter an operation, GET, PUT, or DELETE.

Instructions to Get Started 
The jars are included in the target folder
1. cd into target
2. Run the following command to start the Coordinator first
   1. ```java -jar Coordinator.jar 10001 10002 10003 10004 10005```
   2. The numbers above are the ports of the servers respectively
   3. The port of the coordinator is hardcoded at 10000
3. Run the following command to start the Client next
   1. ```java -jar Client.jar 10001 10002 10003 10004 10005```
   2. The numbers above are the ports of the servers respectively

    If you get this exception running the client, please start the servers first.
    Client exception: java.lang.RuntimeException: java.rmi.ConnectException: Connection refused to host: LOCALHOST; nested exception is:
    java.net.ConnectException: Connection refused

    Please see example-of-runtime-commands.txt for examples of what the program looks like when running.

If you choose build your own copies of the jar or run using maven, the pom.xml is included.
You can utilize the pox xml by importing the project as a maven project in your favorite IDE.





