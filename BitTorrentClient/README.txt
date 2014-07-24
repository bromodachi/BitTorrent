README:
How to run the program:
In order to run the program, copy the files to your current directory.
After doing so, change your directory to the soure folder, not the bitClient folder where all the java files are.
Example:
cd /.autofs/ilab/ilab_users/[your net id]/BitTorrentClient/src/

How to compile:
You will run and compile the program in this source folder. Now to compile, do the following:
javac  btClient/TorrentInfoRU.java btClient/TorrentInfo.java btClient/RUBTClient.java btClient/CommunicationTracker.java btClient/BencodingException.java btClient/Bencoder2.java btClient/Peer.java btClient/BtUtils.java btClient/Piece.java btClient/MessageHandler.java btClient/BtException.java

Finally, to run the program:
java btClient/RUBTClient [torrent file] [whatever you what the file to be]

Example:
java  btClient/RUBTClient project1.torrent ding.png

Sit back and relax for a minute while waiting for your file to download

Write up:
For the write up portion of our project please refer to the javadoc folder where you will find eclipse-generate javadoc files detailing everything about out project
as well as a RUBTClient class dependencies.png file depicting a uml class dependencies diagram