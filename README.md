FTP-Server
==========

A multi-threaded FTP server than can handle concurrent puts and gets

To compile & run & clean server:
cd server
make compile
make run
make clean

To compile & run & clean client:
cd client
make compile
make run
make clean


NOTE:
The ftp client and server run on localhost on nport 30020 and tport 30021
To run them manually:

Server:
cd server
java ftp.server.Main [nport] [tport]

Client
cd client
java ftp.client.Main [hostname] [nport] [tport]

ADDITIONAL NOTE:
SERVER: two command line arguments: port numbers where the server will wait on (one for normal commands and another for the "terminate" command)
CLIENT: three command line arguments: the machine name where the server resides, the normal port number, and the terminate port number
