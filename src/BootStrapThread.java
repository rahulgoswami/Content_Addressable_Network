import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.can.Config;


public class BootStrapThread implements Runnable{
	Socket clientSoc;

	public BootStrapThread(Socket clientSoc){
		this.clientSoc=clientSoc;
	}

	public void run(){
		BufferedReader in =null;
		PrintWriter out =null;
		//ObjectOutputStream outputObjStream =null;
		try{
			in = new BufferedReader(new InputStreamReader(clientSoc.getInputStream()));
			out = new PrintWriter(clientSoc.getOutputStream(), true);
			
//			System.out.println("Waiting for command");
			String command=in.readLine();
//			System.out.println("Command obtined!");
			if(command!=null){
				//String command=in.readLine().toLowerCase();
				String[] cmd=command.trim().split("\\s+");
				command=cmd[0];

				switch(command.toLowerCase()){
				case "join":
					//call method for joining
			//		out = new PrintWriter(clientSoc.getOutputStream(), true);
					
					System.out.println("Received join");
					String nodeID=null;
					String param=null;

					String address=clientSoc.getRemoteSocketAddress().toString().split(":")[0].replace("/","");
					//address=address.replace("/","");

					String connectionAddressDetails=address;
					boolean isNewNode=BootStrap.isNewNode(address);
					if(isNewNode)
						nodeID=BootStrap.generateNodeId();
					else
						nodeID=BootStrap.getNodeId(address);

					if(cmd.length==2 && cmd[1]!=null && cmd[1].toLowerCase().equals("me"))
						param=cmd[1].toLowerCase();


					boolean hasJoined=insertNode(nodeID,connectionAddressDetails,param);
					if(hasJoined==true){

						if(isNewNode){
							out.println("Welcome to the network! Your node ID is "+nodeID);
							HashMap<String,String> nodeInfo=new HashMap<String,String>();
							nodeInfo.put("ip",address);
							nodeInfo.put("isActive","true");
							BootStrap.canNodesInfo.put(nodeID,nodeInfo);
							BootStrap.nodeIDGenerator++;
						}
						else{
							out.println("Welcome back "+nodeID+"!");
							BootStrap.canNodesInfo.get(nodeID).put(Config.IS_ACTIVE_KEY,"true");
						}
					}
					else{
						out.println("Failure adding you to the node");
					}
					break;
				case "view":
					//out = new PrintWriter(clientSoc.getOutputStream(), true);
					ObjectOutputStream outputObjStream = new ObjectOutputStream(clientSoc.getOutputStream());
					System.out.println("Processing view request");
					HashMap<String,HashMap<String,String>> activeNodeInfo=null;
					if(cmd.length==1){
						activeNodeInfo=BootStrap.getActiveNodesInfo("all");
					}
					else if(cmd.length==2){
						activeNodeInfo=BootStrap.getActiveNodesInfo(cmd[1].trim());
					}
					//---------------
//					System.out.println("Available Nodes at bootstrap");
//					for(String key: activeNodeInfo.keySet()){
//						
//						System.out.println(key);
//					}
					//-------------
					outputObjStream.writeObject(activeNodeInfo);
					outputObjStream.flush();
//					System.out.println("View completed successfully");
					break;
				case "leave":
					String leavingIp=clientSoc.getRemoteSocketAddress().toString().split(":")[0].replace("/","");
					String leavingNodeId=BootStrap.getNodeId(leavingIp);
					BootStrap.canNodesInfo.get(leavingNodeId).put(Config.IS_ACTIVE_KEY, "false");
					in.close();
					out.close();
					clientSoc.close();
					break;
				case "status":
				//	out = new PrintWriter(clientSoc.getOutputStream(), true);
					String reqNodeId=BootStrap.getNodeId(clientSoc.getRemoteSocketAddress().toString().split(":")[0].replace("/", ""));
//					System.out.println("Received request for amIActive!!!");
					if(reqNodeId==null){
						out.println("false");
						System.out.println("reqNodeId is found to be null");
						break;
					}
					if(!BootStrap.canNodesInfo.get(reqNodeId).get(Config.IS_ACTIVE_KEY).equals("true")){
						out.println("false");
						System.out.println("is active key found to be false");
						break;
					}
//			em.out.println("Returning true for isActive!!!!!");
					out.println("true");
					//out.flush();
					break;
				case "exit":
					System.out.println("Received exit");
					break;
				default:
				//	out = new PrintWriter(clientSoc.getOutputStream(), true);
					System.out.println("Invalid command");
					out.println("Invalid command. Please enter \"join\" to join the CAN OR \"exit\" to quit");
				}
			}

//			in.close();
//			out.close();
//			clientSoc.close();
			
		}
		catch(IOException ex){
			ex.printStackTrace();
			
		}

	}


	private boolean insertNode(String newNodeID,String newNodeConnectionDetails, String param) {
		// TODO Auto-generated method stub
		//	Set<String> activeNodeIds=BootStrap.getActiveNodeIDs();
		if(param.equals("me")){
			/*
			if(activeNodeIds.size()==0){

			}
			else{
				//randomly select a peer to initiate the insertion process
				//String peerAddress=BootStrap.canNodesInfo.iterator().next();
				String existingNodeID= activeNodeIds.iterator().next();
				String existingNodeDetails=BootStrap.canNodesInfo.get(existingNodeID).get(Config.IP_KEY);

				/*
			String[] existingNodeDetails=BootStrap.canNodesInfo.get(existingNodeID).split(":");
			String existingNodeAddress=existingNodeDetails[0];
			int existingNodePort=Integer.valueOf(existingNodeDetails[1]).intValue(); */

			//				return requestPeerForJoin(existingNodeID,existingNodeDetails,newNodeID,newNodeConnectionDetails);

			return singleNodeJoin(newNodeID,newNodeConnectionDetails);

		}
		else{
			//logic for making all nodes join
			//call singleNodeJoin repeatedly
			return true;
		}

	}
	private boolean singleNodeJoin(String joiningNodeID,String joiningNodeAddress){
		try{
			Set<String> activeNodeIds=BootStrap.getActiveNodeIDs();
//			System.out.println("Joining node address: "+joiningNodeAddress);
			if(activeNodeIds.size()==0){
//				System.out.println("This is the first node!");
				//Entire zone is available for the joining node
				//Could have returned the data on the same connecting socket
				//but that would mean listening for 2 different sockets for the object
				//one from bootstrap and other from another peer.
				//Hence creating another socket to always listen to just one socket
				//System.out.println("Joining node address: "+joiningNodeAddress);
				Socket soc=new Socket(joiningNodeAddress,Config.PEER_PORT);
				//	PrintWriter pw=new PrintWriter(soc.getOutputStream(),true);
				ObjectOutputStream outputStream = new ObjectOutputStream(soc.getOutputStream());

				PeerData newPeer=new PeerData();

				newPeer.zoneList.add(new Zone(0,10,0,10,joiningNodeID,new ConcurrentHashMap<String,String>()));
				newPeer.identifier=joiningNodeID;
				newPeer.neighbours=new ArrayList<PeerData>();
				newPeer.ipAddress=joiningNodeAddress;
				outputStream.writeObject(newPeer);
				outputStream.flush();
				//outputStream.writeObject(new ArrayList<Peer>());
				return true;
			}
			else{
				//randomly select a peer to initiate the insertion process
				//String peerAddress=BootStrap.canNodesInfo.iterator().next();
//				System.out.println("There already exists a node in the CAN network!");
				String existingNodeID= activeNodeIds.iterator().next();
	//			System.out.println("Node selected for routing: "+existingNodeID);
				String existingNodeDetails=BootStrap.canNodesInfo.get(existingNodeID).get(Config.IP_KEY);

				/*
			String[] existingNodeDetails=BootStrap.canNodesInfo.get(existingNodeID).split(":");
			String existingNodeAddress=existingNodeDetails[0];
			int existingNodePort=Integer.valueOf(existingNodeDetails[1]).intValue();
				 */
				return requestPeerForJoin(existingNodeID,existingNodeDetails,joiningNodeID,joiningNodeAddress);

			}
		}
		catch(Exception ex){
			ex.printStackTrace();
			return false;
		}
	}



	private boolean requestPeerForJoin(String existingNodeID,
			String existingNodeAddress, String newNodeID,
			String newNodeAddress) {
		// TODO Auto-generated method stub
		try{
			//	String peerAddress=existingNodeAddress;
			Socket nodeConnector=new Socket(existingNodeAddress,Config.PEER_PORT);
			PrintWriter pw=new PrintWriter(nodeConnector.getOutputStream(),true);
			String message=CANHelper.getMessage("command","join",newNodeAddress.trim(),newNodeID,"!","!","!");
			pw.println(message);
			//if(nodeHasErrors())
			//return true;
		}
		catch(IOException ex){
			ex.printStackTrace();
			return false;
		}
		return true;
	}


}