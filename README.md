# JVN - Javanaise Project

The aim of the Javanaise project is to implement a distributed object cache in Java. Java applications using Javanaise can create and access locally hidden distributed objects.
The project was made and compiled using Java 17.

## Installation

Clone the repository
```
git clone https://github.com/David-mrc/JVN.git
```

Or download the zipped version of the JVN releases n°1 or n°2 from github for each API version @ <https://github.com/David-mrc/JVN>

## Tests

To begin, build the project in an IDE (Eclipse, VSCode, IntelliJ). Then run the JVN Coordinator (Run Java in IDE) in src/jvn/JvnCoordImpl.java

Then, you can either run one or more stress tests in src/test/Stress.java, or run one or more IRC in src/IRC/Irc.java

## Extensions

We have successfully implemented the following additional features:
  - Deadlock prevention: Prevents deadlocks when two servers have a Read Cached token on the shared object and both try to acquire a Write lock. This has been acheived by using a finer method to acquire Write locks so that during the acquisition, the program temporarily exits the synchronized state to allow other JvnServers to perform verifications on the lock.
  - Coordinator shutdown recovery: The Coordinator logs the state of its objects in the filesystem to enable recovery of the objects upon restarting after a shutdown. When the Coordinator starts, it will always try to read and recover shared objects. The Irc instances must be restarted in order to continue using the shared objects - this is a possible improvement area.
  - Client shutdown handling: Prevents client crashes when one client unexpectedly shuts down. This has been acheived by handling the remote exceptions raised when invalidating an unresponsive client. 

 

