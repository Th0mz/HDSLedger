## HDS Ledger

### Goals
This project aims to develop a simplified permissioned (closed membership)
blockchain system with high dependability guarantees, called HDS Ledger (HDL).

### How to run 
Compile with mvn from the HDSLedger folder with  
`mvn install`

#### Blockchain
To start a blockchain member, from the Blockchain module run:  
`mvn exec:java -Dexec.args="<config-file-absolute-path> <process-id>"` 
  
(Open as many instances as the the number of members in the config file)  

#### Client
To start the client, from the Client module, run:  
`mvn exec:java -Dexec.args="<config-file-absolute-path>"`  

### Test
Simply run `mvn tests` from the HDSLedger folder   

