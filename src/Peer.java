import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.w3c.dom.traversal.NodeIterator;

import com.can.Config;




public class Peer implements Runnable,Serializable{
	public static String ipAddress;
	public static String identifier;
	public static ArrayList<Zone> zoneList;
	//	private HashMap<String,String> dataMap;
	public static ArrayList<PeerData> neighbours;
	private ServerSocket peerServerSoc;


	public void run(){
		try{
			peerServerSoc= new ServerSocket(Config.PEER_PORT);

			//Accept the first connection which will initialize the data structures(neighbours, zones etc)
			Socket initNodeSoc=peerServerSoc.accept();
			Peer.initNodeStructures(initNodeSoc);
//			System.out.println("new Node Inititalized!!!!!!");
			while(true){
				Socket peerListenerSoc=peerServerSoc.accept();
				//	newPeer.setPeerServerSoc(peerServerSoc);
//				System.out.println("Create a new PeerThread thread");
				/*
				newPeer.processPostJoin(peerListenerSoc);
				Socket listenerSoc= peerServerSocket.accept();
				 */		
				PeerThread newPeerThread=new PeerThread(peerListenerSoc);
				Thread sThread=new Thread(newPeerThread);
				sThread.start();

				//		sThread.join();

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	private static void initNodeStructures(Socket initNodeSoc) {
		// TODO Auto-generated method stub
		try{
			ObjectInputStream inStream = new ObjectInputStream(initNodeSoc.getInputStream());
			PeerData peerObj = (PeerData) inStream.readObject();
			zoneList=peerObj.zoneList;
			ipAddress=initNodeSoc.getLocalSocketAddress().toString().split(":")[0].replace("/","");
			neighbours=peerObj.neighbours;
			identifier=peerObj.identifier;
			CANHelper.paintInfo(peerObj);
			initNodeSoc.close();
		}
		catch(IOException ex){
			ex.printStackTrace();
		}
		catch(ClassNotFoundException ex){
			ex.printStackTrace();
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
	}


	public static void main(String[] args){
		//	Peer newPeer=new Peer();
		try{
			String command;
			Scanner cmdRead;
			System.out.println("Trying to connect to "+Config.BOOTSTRAP_IP+":"+new Integer(Config.BOOTSTRAP_PORT).toString());
			Socket soc=new Socket(Config.BOOTSTRAP_IP,Config.BOOTSTRAP_PORT);
			BufferedReader in=new BufferedReader(new InputStreamReader(soc.getInputStream()));
			PrintWriter out=new PrintWriter(soc.getOutputStream(),true);
	//		System.out.println("Your address as known to you is:"+soc.getLocalSocketAddress().toString());
			System.out.println(in.readLine());
			boolean hasLeft=false;

			cmdRead = new Scanner(System.in);
			while(true){
				System.out.print("Enter command: ");

				command=cmdRead.nextLine();
				if(command!=null){
					/*	out.println(command);
					if(command.equalsIgnoreCase("join"))
						break;*/
					String[] commandArgs=command.trim().split("\\s+");
					//command=commandArgs[0];
					switch(commandArgs[0].toLowerCase()){
					case "join":
						//call method for joining
						
						if((commandArgs.length>2)||(commandArgs.length==2 && commandArgs[1]!=null && !commandArgs[1].toLowerCase().equals("me"))){
							System.out.println("Invalid command. Please enter valid parameters for \"join\" command");
							break;
						}
						Peer peerListener = new Peer();
						Thread listenerThread = new Thread(peerListener);
						listenerThread.start();
						out.println(command.trim());
						System.out.println(in.readLine());

						out.close();
						in.close();
						soc.close();
						break;
						/*
						ServerSocket peerServerSoc= new ServerSocket(Config.PEER_PORT);
		//				newPeer.setPeerServerSoc(peerServerSoc);
						Socket peerListenerSoc=peerServerSoc.accept();

						newPeer.processPostJoin(peerListenerSoc);
						break;
						 */
					case "insert":
						//First check in boot strap if the node exists at all
						if(Peer.amIActive()==false){
							System.out.println("Please join the CAN to execute any commands. Use \"join me\" to join the CAN");
							break;
						}
						
//						System.out.println("Sending insert");
						if(commandArgs.length!=2){
							System.out.println("Please provide a key to be inserted");
							break;
						}
						String key=commandArgs[1];
						float x=CANHelper.getXCoordFrmKey(key);
						float y=CANHelper.getYCoordFrmKey(key);
						String route=Peer.identifier;
						if(Peer.containsCoordinates(x,y)){
							Peer.zoneList.get(0).getLocalHashTable().put(key,key+"_Data");
							System.out.println("Resoure with key "+key+" inserted at node "+Peer.identifier);
							System.out.println("Route to the key: "+route);
						}
						else{
							String nextSearchNeighbour=Peer.routeToNeighbour(x,y);
							if(nextSearchNeighbour==null){
								System.out.println("Failure inserting node. Sorry! I don't have neighbours");
								break;
							}
							else{
								String message=CANHelper.getMessage("command", "insert", Peer.ipAddress, "!", String.valueOf(x), String.valueOf(y), key+":"+Peer.identifier);
								Socket nextNeighbourSoc=new Socket(nextSearchNeighbour.trim(),Config.PEER_PORT);
								PrintWriter outNeighbour=new PrintWriter(nextNeighbourSoc.getOutputStream(),true);
								outNeighbour.println(message);
							}
						}
						break;
					case "search":
						if(Peer.amIActive()==false){
							System.out.println("Please join the CAN to execute any commands. Use \"join me\" to join the CAN");
							break;
						}
						//First check in boot strap if the node exists at all
//						System.out.println("Sending search");

						if(commandArgs.length!=2){
							System.out.println("Please provide a key to be searched");
							break;
						}
						String searchKey=commandArgs[1];
						float x_co=CANHelper.getXCoordFrmKey(searchKey);
						float y_co=CANHelper.getYCoordFrmKey(searchKey);
						String searchRoute=Peer.identifier;
						if(Peer.containsCoordinates(x_co,y_co)){
							if(Peer.zoneList.get(0).getLocalHashTable().containsKey(searchKey)){
								//	Peer.zoneList.get(0).getLocalHashTable().put(searchKey,searchKey+"_Data");
								System.out.println("Resource with key "+searchKey+" found at node "+Peer.identifier);
								System.out.println("Route to the key: "+searchRoute);
							}

						}
						else{
							String nextSearchNeighbour=Peer.routeToNeighbour(x_co,y_co);
							if(nextSearchNeighbour==null){
								System.out.println("Failure searching node. Sorry! I don't have neighbours");
								break;
							}
							else{
								String message=CANHelper.getMessage("command", "search", Peer.ipAddress, "", String.valueOf(x_co), String.valueOf(y_co), searchKey+":"+Peer.identifier);
								Socket nextNeighbourSoc=new Socket(nextSearchNeighbour.trim(),Config.PEER_PORT);
								PrintWriter outNeighbour=new PrintWriter(nextNeighbourSoc.getOutputStream(),true);
								outNeighbour.println(message);
							}
						}
						break;
					case "view":
						//First check in boot strap if the node exists at all
						try{
							if(Peer.amIActive()==false){
								System.out.println("Please join the CAN to execute any commands. Use \"join me\" to join the CAN");
								break;
							}
//							System.out.println("Sending view");

							if(commandArgs.length>2){
								System.out.println("Cannot provide more than one peer as argument. Enter \"view\" to view all nodes");
								break;
							}
//							System.out.println("Made some progress...");
							if(commandArgs.length==2 && commandArgs[1].equals(Peer.identifier)){
								//The requested node is the node itself
								PeerData myData=new PeerData();
								myData.identifier=Peer.identifier;
								myData.ipAddress=Peer.ipAddress;
								myData.zoneList=Peer.zoneList;
								myData.neighbours=Peer.neighbours;
								CANHelper.paintInfo(myData);
								break;
							}
//							System.out.println("=========Can surpass the if block=====");
							
							Socket viewSoc=new Socket(Config.BOOTSTRAP_IP,Config.BOOTSTRAP_PORT);
//							System.out.println("=========Created socket connection for view=====");
							PrintWriter viewOut=new PrintWriter(viewSoc.getOutputStream(),true);
//							System.out.println("=========Obtained outstream for view=====");
							
									
//							System.out.println("=======Entered view command is: ======"+command.trim());
							viewOut.println(command.trim());
						//	ObjectInputStream inStream = new ObjectInputStream(initNodeSoc.getInputStream());
						//	PeerData peerObj = (PeerData) inStream.readObject();
						//	Thread.sleep(1000);
							ObjectInputStream inStream = new ObjectInputStream(viewSoc.getInputStream());
							
//							System.out.println("!!!!!Here Finally??!!");
							HashMap<String,HashMap<String,String>> activeNodes=(HashMap<String,HashMap<String,String>>)inStream.readObject();
							if(activeNodes.isEmpty()){
								if(commandArgs.length==2){
									System.out.println("Node "+commandArgs[1]+" is not active");
								}
								else if(commandArgs.length==1){
									System.out.println("No active nodes present in the CAN");
								}
								else{
									System.out.println("Something wrng with the number of arguments");
								}
								break;
							}
							inStream.close();
							viewOut.close();
							viewSoc.close();
							
							Set<String> keySet=activeNodes.keySet();
							for(String activeNode:keySet){
								if(activeNode.equals(Peer.identifier)){
									PeerData myData=new PeerData();
									myData.identifier=Peer.identifier;
									myData.ipAddress=Peer.ipAddress;
									myData.zoneList=Peer.zoneList;
									myData.neighbours=Peer.neighbours;
									CANHelper.paintInfo(myData);
									continue;
								}
								Socket peerSoc=new Socket(activeNodes.get(activeNode).get(Config.IP_KEY),Config.PEER_PORT);
								PrintWriter viewWriter=new PrintWriter(peerSoc.getOutputStream(),true);
								

								String msg=CANHelper.getMessage("command", "view", Peer.ipAddress, Peer.identifier,"!" , "!", "!");
//								System.out.println("=============View command message :"+msg);
								viewWriter.println(msg);
								ObjectInputStream objIn=new ObjectInputStream(peerSoc.getInputStream());
								PeerData nodeInfo=(PeerData)objIn.readObject();
								CANHelper.paintInfo(nodeInfo);
								
								viewWriter.close();
								objIn.close();
								peerSoc.close();
							}
						}
						catch(IOException ex){
							ex.printStackTrace();
						}
						catch(Exception ex){
							ex.printStackTrace();
						}
						break;
					case "leave":
						if(Peer.amIActive()==false){
							System.out.println("Please join the CAN to execute any commands. Use \"join me\" to join the CAN");
							break;
						}
						//First check in boot strap if the node exists at all
						if(Peer.amIActive()==false){
							System.out.println("Please join the CAN to execute any commands. Use \"join me\" to join the CAN");
							break;
						}
//						System.out.println("Sending leave");
											
						if(Peer.neighbours.size()!=0){
							//No neighbours exist
							synchronized(Peer.class){
								updateMyNeighboursBeforeLeaving();
							}			
						}
											
						Socket viewSoc=new Socket(Config.BOOTSTRAP_IP,Config.BOOTSTRAP_PORT);
//						/System.out.println("=========Created socket connection for leave=====");
						PrintWriter viewOut=new PrintWriter(viewSoc.getOutputStream(),true);
						viewOut.println("leave");
						hasLeft=true;
						//See if closing it directly causes any issues
						System.out.println("You are now leaving the CAN!");
						break;
					default:
						System.out.println("Invalid command");
					}
					
					if(hasLeft==true)
						break;	
				}

			}

		}
		catch(IOException ex){
			ex.printStackTrace();
		}
	}

	private static void updateMyNeighboursBeforeLeaving() {
		// TODO Auto-generated method stub
		//get ip of the node to be merged with
		try{
		String mergeNodeIp=getNodeToMergeWith();
		Socket upSoc=new Socket(mergeNodeIp,Config.PEER_PORT);
		PrintWriter upWrite=new PrintWriter(upSoc.getOutputStream(),true);
		
		String msg=CANHelper.getMessage("command", "leave", Peer.ipAddress, Peer.identifier,Config.BLANK , Config.BLANK, "merge");
		upWrite.println(msg);
		ObjectOutputStream oop= new ObjectOutputStream(upSoc.getOutputStream());
		PeerData myData=new PeerData();
		myData.identifier=Peer.identifier;
		myData.ipAddress=Peer.ipAddress;
		myData.zoneList=Peer.zoneList;
		myData.neighbours=Peer.neighbours;
		oop.writeObject(myData);
		
		//Update the other neigbour below
		if(Peer.neighbours.size()>1){
			for(int i=0;i<Peer.neighbours.size();i++){
				if(!Peer.neighbours.get(i).ipAddress.equals(mergeNodeIp)){
					msg=CANHelper.getMessage("command", "leave", Peer.ipAddress, Peer.identifier,Config.BLANK , Config.BLANK, "nomerge");
					Socket noMergeSoc=new Socket(Peer.neighbours.get(i).ipAddress,Config.PEER_PORT);
					PrintWriter noMergeWrite=new PrintWriter(noMergeSoc.getOutputStream(),true);
					noMergeWrite.println(msg);
					
					ObjectOutputStream nmoop= new ObjectOutputStream(noMergeSoc.getOutputStream());
					nmoop.writeObject(myData);
					
				}
			}
		}
		}
		catch(IOException ex){
			ex.printStackTrace();
		}
		
	}

	

	private static boolean amIActive() {
		// TODO Auto-generated method stub
		try{
			Socket soc=new Socket(Config.BOOTSTRAP_IP,Config.BOOTSTRAP_PORT);
			BufferedReader in=new BufferedReader(new InputStreamReader(soc.getInputStream()));
			PrintWriter out=new PrintWriter(soc.getOutputStream(),true);
			//String msg=CANHelper.getMessage("command", "status", ipAddress, identifier, "", "","");
			
			out.println("status");
			String isActiveStatus=in.readLine();
	//		System.out.println("isActiveStatus: "+isActiveStatus);
			if(isActiveStatus.equals("true")){
				return true;
			}
			else
				return false;
		}
		catch(IOException ex){
			ex.printStackTrace();
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
		return false;

	}

	public static String routeToNeighbour(float x, float y) {
		// TODO Auto-generated method stub
		int numNeighbours=Peer.neighbours.size();
		if(numNeighbours!=0){
			return Peer.getNearestNeighbourFromGivenPoint(x,y);

		}
		else
			return null;
	}

	
	public ServerSocket getPeerServerSoc() {
		return peerServerSoc;
	}

	public void setPeerServerSoc(ServerSocket peerServerSoc) {
		this.peerServerSoc = peerServerSoc;
	}


	public static boolean containsCoordinates(float x, float y) {
		// TODO Auto-generated method stub
		for(Zone z:zoneList){
			if(x>=z.getxStart() && x<z.getxEnd() && y>=z.getyStart() && y<z.getyEnd())
				return true;
		}
		return false;
	}


	public static String getNearestNeighbourFromGivenPoint(float x,
			float y) {
		// TODO Auto-generated method stub
		HashMap<Float,String> distanceMap=new HashMap<Float,String>();
		float x_mid;
		float y_mid;
		float peer_min=Float.MAX_VALUE;
		String peer_min_ip=null;

		for(PeerData p:neighbours){
			if(p.zoneList.get(0).containsPoint(x,y)){
				return p.ipAddress;
			}
			float zone_min=Float.MAX_VALUE;
			for(Zone z:p.zoneList){
				x_mid=(z.getxStart()+z.getxEnd())/2;
				y_mid=(z.getyStart()+z.getyEnd())/2;
				float dist=CANHelper.getDistance(x_mid,y_mid,x,y);
				if(dist<zone_min){
					zone_min=dist;

				}
			}
			if(zone_min<peer_min){
				peer_min=zone_min;
				peer_min_ip=p.ipAddress;
			}
			//distanceMap.put(Float.valueOf(zone_min),p.ipAddress);


		}
		return peer_min_ip;

	}

	public static Zone getZoneToSplit(float x, float y) {
		// TODO Auto-generated method stub
		//simpler implementation assuming all the zones are cut horizontally
		//In this case every node will have just one zone
		if(!zoneList.isEmpty()){
			return zoneList.get(0);	
		}

		return null;
	}

	public static void addNewNodeAsCurrentNodeNeighbour(PeerData newNodeData) {
		// TODO Auto-generated method stub
		PeerData newNode=new PeerData();
		newNode.identifier=newNodeData.identifier;
		newNode.ipAddress=newNodeData.ipAddress;
		Zone newNodeDataZone=newNodeData.zoneList.get(0);
		newNode.zoneList.add(new Zone(newNodeDataZone.getxStart(),newNodeDataZone.getxEnd(),newNodeDataZone.getyStart(),newNodeDataZone.getyEnd(),newNodeDataZone.getOwnerNodeID(),null));
		Peer.neighbours.add(newNode);
	}
	
	private static String getNodeToMergeWith() {
		// TODO Auto-generated method stub
		if(Peer.neighbours.size()==1){
			return Peer.neighbours.get(0).ipAddress;
		}
		float max=Float.MIN_VALUE;
		String mergeIpAddress=null;
		for(PeerData neighbour:Peer.neighbours){
			if(Math.abs(neighbour.zoneList.get(0).getyEnd()-neighbour.zoneList.get(0).getyStart())> max){
				max=Math.abs(neighbour.zoneList.get(0).getyEnd()-neighbour.zoneList.get(0).getyStart());
				mergeIpAddress=neighbour.ipAddress;
			}
		}
		return mergeIpAddress;
	}

	public static void updateMyZoneCoOrdinates(Zone zone) {
		// TODO Auto-generated method stub
		if(Peer.zoneList.get(0).getyStart()<zone.getyStart()){
			Peer.zoneList.get(0).setyEnd(zone.getyEnd());
		}
		else{
			Peer.zoneList.get(0).setyStart(zone.getyStart());
		}
	}
}
