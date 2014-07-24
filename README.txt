# README #

### RUBTClient ###
This is a BitTorrent client written for the Rutgers university Internet Technologies (198:352) course during summer session 2014

### Team ###

* Cody Goodman
* Conrado Uraga

### Compile & Run ###
How to run the program:
Be sure to be in the src folder. Example:
cd /.autofs/ilab/ilab_users/[your net id]/BitTorrentClient/src/

How to compile:
javac  btClient/TorrentInfoRU.java btClient/TorrentInfo.java btClient/RUBTClient.java btClient/CommunicationTracker.java btClient/BencodingException.java btClient/Bencoder2.java btClient/Peer.java btClient/BtUtils.java btClient/Piece.java btClient/MessageHandler.java btClient/BtException.java

Finally, to run the program:
java  btClient/RUBTClient project1.torrent ding.png