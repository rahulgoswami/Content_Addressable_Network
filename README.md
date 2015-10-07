# Content_Addressable_Network
A Distributed Hash Table (DHT) helps searching for a file efficiently with a keyword in peer-to-peer (p2p) networks. Those DHT-based p2p networks are referred to as structured p2p networks. Content Addressable Network(CAN) is one such implementation of a DHT based peer-to-peer distributed system. This implementation is based on the following paper by S Ratnasamy et al.
conferences.sigcomm.org/sigcomm/2001/p13-ratnasamy.pdf

Commands:
i) join me: A request is sent to the bootstrap from a joining node. The bootstrap randomly chooses a node already in the CAN and routes this request to this node. This node then randomly chooses co-ordinates within the CAN. If these co-ordinates lie within this node itself, the node splits and transmits necessary state information to the joining node. Otherwise, all the neighbours are checked to find the closest one to the destination co-ordinates. The information is relayed to this peer and thus the relaying continues till the appropriate zone is discovered.
ii) insert <key>: Hash of the key is calculated and co-ordinates are determined. If the co-ordinates lie in the requesting zone itself, the key is inserted in the hashtable. Otherwise, routing to the destination co-ordinates happens the same way as in join.
iii) search <key>: Hash of the key is calculated and co-ordinates are determined. If the co-ordinates lie in the requesting zone itself, the key is looked up in the hashtable.  Otherwise, routing to the destination co-ordinates happens the same way as in join. If the key is found the route to the destination node from the source node is displayed.
iv) view [<node id>]:  If a peer node id is provided as a parameter the ip address of the node is obtained from the bootstrap. If not provided, the ip addresses of all the active nodes are obtained from the bootstrap. Each of the peers is then requested for its data by the requesting peer and the data is accordingly painted on the standard output.
v) leave: Implemented but some issues still persist. Will fix these these at the earliest convenience. The expected behaviour is that the requesting node should exit the CAN and the boundaries of the neighbours are appropriately adjusted and the keys of this leaving node get assigned to the smaller neighbouring zone to keep the distribution of the keys fairly even.

Design:
The project entails use of multi-threading and sockets to form the CAN and execute commands on it. Appropriate measures and data structures have been used to maintain synchronization between the threads and avoid concurrent modification of the data structures.
Below is a diagrammatic representation of the important classes and how they interact:
 
BootStrap: Main class which acts as the bootstrap server and maintains the ip address and status of all the nodes in the CAN 

BootStrapThread: Whenever any peer requests a connection to the BootStrap(either to join the CAN or to get all active node names for displaying the node info), the BootStrap creates a separate thread for the peer to process it’s request. The bootstrap maintains a hash map with the folloeing information about each node:
•	NodeID
•	Ip Address
•	IsActive (to maintain if the node is active or not)

Peer: The main class which serves as the terminal for the node. Requests for join, insert, search, view and leave are initiated through Peer.

PeerThread: This is the listener class for the peer. Whenever a Peer receives any request from another peer(for eg: while routing in join, insert and search)  it creates a PeerThread thread and serves this request.  One PeerThread thread is created for every request that the Peer receives. This helps in serving multiple peer requests and no peer has to wait for its request to be processed.

Other classes:
Zone: Although typically every node will have one zone associated with it, a peer can also have a list of zones associated with it(depending upon the state of the CAN after a node has left).  Each zone has a hash table associated with it. The data for this node is stored in this hash table.
PeerData: This class is used to represent the neighbor info for every Peer.

