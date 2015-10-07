import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.can.Config;


public class PeerThread implements Runnable {
	public Socket listenerSoc;
	public String signal;
	public String command;
	public String address;
	public String nodeID;
	public String message_x;
	public String message_y;
	public String params;

	public PeerThread(Socket listenerSoc){
		this.listenerSoc=listenerSoc;
	}
	public void run(){
		//open streams with listenerSoc and accept the commands routed to this node
		//in a switch case. No while(true)
		try{
			BufferedReader in = new BufferedReader(new InputStreamReader(listenerSoc.getInputStream()));
			PrintWriter out = new PrintWriter(listenerSoc.getOutputStream(), true);
			String message=in.readLine();
//			System.out.println("Message obtained via routing!!!!!! as "+message);
			processMessage(message);
//			System.out.println(signal+" "+command+" "+address+" "+nodeID);
			if(signal.trim().equals("command")){
				float x;
				float y;
				switch(command.trim().toLowerCase()){
				case "join":
//					System.out.println("Request arrived at node "+Peer.identifier+" for node "+nodeID);
					PeerData newPeer;
					//	if(message_x.trim().isEmpty() && message_y.trim().isEmpty()){
					if(message_x.trim().equals(Config.BLANK) && message_y.trim().equals(Config.BLANK)){
						/*This means that the bootstrap has ordered it to
						 * select random co-ordinates for the joining node 
						 */
						x=CANHelper.generateRandom();
						y=CANHelper.generateRandom();
						String[] messageContents=message.split("\\|");
						message=CANHelper.getMessage(messageContents[0], messageContents[1], messageContents[2], messageContents[3], Float.valueOf(x).toString(), Float.valueOf(y).toString(), messageContents[6]);
					}
					else{
						x=Float.valueOf(message_x).floatValue();
						y=Float.valueOf(message_y).floatValue();

					}
					if(Peer.containsCoordinates(x,y)){
						//split
						//if square-cut vertically
						//if rectangle - cut horizontally
						Socket soc=new Socket(address,Config.PEER_PORT);
						PrintWriter pw=new PrintWriter(soc.getOutputStream(),true);
						ObjectOutputStream outputStream = new ObjectOutputStream(soc.getOutputStream());

						synchronized(PeerThread.class){
							newPeer= splitZone(x,y,address,nodeID);
						}
						//	Socket newNodeSoc=new Socket(address,Config.PEER_PORT);
						//	ObjectOutputStream objOut = new ObjectOutputStream(newNodeSoc.getOutputStream());
//						System.out.println("node "+Peer.identifier+"SPLIT UP for node "+nodeID);
						outputStream.writeObject(newPeer);
						outputStream.flush();
//						System.out.println("INSIDE THE SPLITTED PARENT. PRINTING THE NEW NODE OBJECT DETAILS");
						CANHelper.paintInfo(newPeer);

					}
					else{
						//route
						String nextSearchNeighbour=Peer.routeToNeighbour(x,y);
						if(nextSearchNeighbour==null){
							System.out.println("Sorry! I don't have neighbours");
						}
						else{
							Socket nextNeighbourSoc=new Socket(nextSearchNeighbour.trim(),Config.PEER_PORT);
							PrintWriter outNeighbour=new PrintWriter(nextNeighbourSoc.getOutputStream(),true);
							outNeighbour.println(message);
						}

					}
					break;
				case "insert":
					String[] insertParams=params.split(":");
					String insertKey=insertParams[0];
					String insertRoute=insertParams[1];

					x=Float.valueOf(message_x).floatValue();
					y=Float.valueOf(message_y).floatValue();
					String insertResult;
					if(Peer.containsCoordinates(x,y)){
						Socket conn=new Socket(address,Config.PEER_PORT);
						PrintWriter insertOut=new PrintWriter(conn.getOutputStream(),true);

						Peer.zoneList.get(0).getLocalHashTable().put(insertKey,insertKey+"_Data");
						insertResult="Resource with key "+insertKey+" is inserted at node "+Peer.identifier+". Route to the key: "+insertRoute+"-->"+Peer.identifier;

						String msg=CANHelper.getMessage("info", "insert", address, nodeID, "!", "!", insertResult);
						insertOut.println(msg);
					}
					else{
						String nextInsertNeighbour=Peer.routeToNeighbour(x,y);
						if(nextInsertNeighbour==null){
							System.out.println("Failure while insertion. Sorry! I don't have neighbours");
							Socket conn=new Socket(address,Config.PEER_PORT);
							PrintWriter outStr=new PrintWriter(conn.getOutputStream(),true);
							insertResult="Failure inserting key "+insertKey;
							String msg=CANHelper.getMessage("info", "insert", address, nodeID, "!", "!", insertResult);
							outStr.println(msg);
							break;
						}
						else{
							String routeMessage=CANHelper.getMessage("command", "insert", address, "!", String.valueOf(x), String.valueOf(y), insertKey+":"+insertRoute+"-->"+Peer.identifier);
							Socket nextNeighbourSoc=new Socket(nextInsertNeighbour.trim(),Config.PEER_PORT);
							PrintWriter outNeighbour=new PrintWriter(nextNeighbourSoc.getOutputStream(),true);
							outNeighbour.println(routeMessage);
						}
					}
					break;
				case "search":
					String[] searchParams=params.split(":");
					String searchKey=searchParams[0];
					String searchRoute=searchParams[1];

					x=Float.valueOf(message_x).floatValue();
					y=Float.valueOf(message_y).floatValue();
					String searchResult;
					if(Peer.containsCoordinates(x,y)){
						Socket conn=new Socket(address,Config.PEER_PORT);
						PrintWriter searchOut=new PrintWriter(conn.getOutputStream(),true);

						if(Peer.zoneList.get(0).getLocalHashTable().containsKey(searchKey)){
							searchResult="Resource with key "+searchKey+" is present at node "+Peer.identifier+". Route to the key: "+searchRoute+"-->"+Peer.identifier;
						}
						else{
							searchResult="Resource with key "+searchKey+" not found";

						}
						String msg=CANHelper.getMessage("info", "search", address, nodeID, "!", "!", searchResult);
						searchOut.println(msg);
					}
					else{
						String nextSearchNeighbour=Peer.routeToNeighbour(x,y);
						if(nextSearchNeighbour==null){
							System.out.println("Failure searching node. Sorry! I don't have neighbours");
							Socket conn=new Socket(address,Config.PEER_PORT);
							PrintWriter outStr=new PrintWriter(conn.getOutputStream(),true);
							searchResult="Failure searching node!";
							String msg=CANHelper.getMessage("info", "search", address, nodeID, "!", "!", searchResult);
							outStr.println(msg);
							break;
						}
						else{
							String routeMessage=CANHelper.getMessage("command", "search", address, "!", String.valueOf(x), String.valueOf(y), searchKey+":"+searchRoute+"-->"+Peer.identifier);
							Socket nextNeighbourSoc=new Socket(nextSearchNeighbour.trim(),Config.PEER_PORT);
							PrintWriter outNeighbour=new PrintWriter(nextNeighbourSoc.getOutputStream(),true);
							outNeighbour.println(routeMessage);
						}
					}
					break;
				case "view":
					PeerData myData=new PeerData();
					myData.identifier=Peer.identifier;
					myData.ipAddress=Peer.ipAddress;
					myData.zoneList=Peer.zoneList;
					myData.neighbours=Peer.neighbours;
					ObjectOutputStream outInfo= new ObjectOutputStream(listenerSoc.getOutputStream());
					outInfo.writeObject(myData);
					outInfo.flush();
					break;
				case "leave":
					if(params.equals("merge")){
						int myIndex=-1;
						for(int i=0;i<Peer.neighbours.size();i++){
							if(Peer.neighbours.get(i).ipAddress.equals(address)){
								myIndex=i;
								break;
							}
						}
						Peer.neighbours.remove(myIndex);


						try{
//							System.out.println("Could encounter problem here!!");
							ObjectInputStream ois=new ObjectInputStream(listenerSoc.getInputStream());
	//						System.out.println("======Problem avoided!!!=====");
							PeerData leavingNodeData=(PeerData)ois.readObject();
							ConcurrentHashMap<String,String> mergeData=leavingNodeData.zoneList.get(0).getLocalHashTable();
							Set<String> mergeDataKeys=mergeData.keySet();
							for(String key : mergeDataKeys){
								Peer.zoneList.get(0).getLocalHashTable().put(key, mergeData.get(key));
							}
							Peer.updateMyZoneCoOrdinates(leavingNodeData.zoneList.get(0));

							//Add any other neighbour of the to be deleted node to the node you are merging with
							for(int i=0;i<leavingNodeData.neighbours.size();i++){
								if(!leavingNodeData.neighbours.get(i).identifier.equals(Peer.identifier)){
									Peer.neighbours.add(leavingNodeData.neighbours.get(i));
									break;
								}
							}

						}
						catch(IOException ex){
							ex.printStackTrace();
						} 
						catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					else{
						try{
						int myIndex=-1;
						for(int i=0;i<Peer.neighbours.size();i++){
							if(Peer.neighbours.get(i).ipAddress.equals(address)){
								myIndex=i;
								break;
							}
						}
						Peer.neighbours.remove(myIndex);
						
						ObjectInputStream ois=new ObjectInputStream(listenerSoc.getInputStream());
//						System.out.println("======Problem avoided!!!=====");
						PeerData leavingNodeData=(PeerData)ois.readObject();
												
						for(int i=0;i<leavingNodeData.neighbours.size();i++){
							if(!leavingNodeData.neighbours.get(i).identifier.equals(Peer.identifier)){
								PeerData otherNeighbour=leavingNodeData.neighbours.get(i);
								
								if(Peer.zoneList.get(0).getyStart()<otherNeighbour.zoneList.get(0).getyStart()){
									otherNeighbour.zoneList.get(0).setyStart(Peer.zoneList.get(0).getyEnd());
								}
								else{
									otherNeighbour.zoneList.get(0).setyEnd(Peer.zoneList.get(0).getyStart());
								}
								
								Peer.neighbours.add(otherNeighbour);
								break;
							}
						}
						}
						catch(Exception ex){
							ex.printStackTrace();
						}
					}
				case "update neighbour":
					int index=-1;
					for(PeerData previousNeighbour:Peer.neighbours){
						if(previousNeighbour.zoneList.get(0).getxStart()<Peer.zoneList.get(0).getyStart()){
							index=Peer.neighbours.indexOf(previousNeighbour);
							break;
						}
					}
					Peer.neighbours.remove(index);
					PeerData newNodeData=new PeerData();
					newNodeData.identifier=nodeID;
					newNodeData.ipAddress=address;
					newNodeData.zoneList.add(new Zone(0,10,Float.valueOf(params).floatValue(),Peer.zoneList.get(0).getyStart(),nodeID,null));
					Peer.neighbours.add(newNodeData);
					break;
				default:
					break;
				}
			}
			else if(signal.equals("info")){
				switch(command){
				case "join":
					//deserialize the object and set the structures
					break;
				case "insert":
					System.out.println(params);
					break;
				case "search":
					System.out.println(params);
					break;
				}

			}
			in.close();
			out.close();
			listenerSoc.close();
		}
		catch(IOException ex){
			ex.printStackTrace();

		}

	}
	private PeerData splitZone(float x, float y, String address2, String nodeID2) {
		// TODO Auto-generated method stub
		PeerData newNodeData=new PeerData();
		//Enhancement-In case of multiple zones for a node get  the zone to be split
		//Zone zoneToBeSplit=Peer.getZoneToSplit(x,y);
		Zone zoneToBeSplit=Peer.zoneList.get(0);
		if(zoneToBeSplit==null){
			System.out.println("This node doesn't have any zones associated with it");
			return null;
		}
		Zone newNodeZone=new Zone();

		//split the node with left as the new node horizontally
		//and assign the upper zone to the new node

		float new_y_start=(zoneToBeSplit.getyStart()+zoneToBeSplit.getyEnd())/2;
		newNodeZone.setxStart(zoneToBeSplit.getxStart());
		newNodeZone.setxEnd(zoneToBeSplit.getxEnd());
		newNodeZone.setyStart(new_y_start);
		newNodeZone.setyEnd(zoneToBeSplit.getyEnd());
		newNodeZone.setOwnerNodeID(nodeID2);
		newNodeZone.setLocalHashTable(getHashTableForNewNode(zoneToBeSplit,zoneToBeSplit.getxStart(),zoneToBeSplit.getxEnd(),new_y_start,zoneToBeSplit.getyEnd()));

		//Set the new zone for the current node
		Peer.zoneList.get(0).setyEnd(new_y_start);

		newNodeData.zoneList.add(newNodeZone);
		newNodeData.identifier=nodeID2;
		newNodeData.ipAddress=address2;
		newNodeData.neighbours=getNeighboursOfNewNode(newNodeZone);

		Peer.addNewNodeAsCurrentNodeNeighbour(newNodeData);
		//updateAllNeighbours();
		return newNodeData;
	}

	private ArrayList<PeerData> getNeighboursOfNewNode(Zone newNodeZone) {
		// TODO Auto-generated method stub
		ArrayList<PeerData> newNodeNeighbours=new ArrayList<PeerData>();
		PeerData myself=new PeerData();
		myself.ipAddress=Peer.ipAddress;
		myself.identifier=Peer.identifier;

		Zone myZone=Peer.zoneList.get(0);

		myself.zoneList.add(new Zone(myZone.getxStart(),myZone.getxEnd(),myZone.getyStart(),myZone.getyEnd(),myZone.getOwnerNodeID(),null));
		newNodeNeighbours.add(myself);
		int index=-1;
		//Determine other neighbours of new node
		for(PeerData currNeighbour:Peer.neighbours){
			if(currNeighbour.isNewNodesNeighbour(newNodeZone)){
				//				PeerData newNeighbour=new PeerData();
				index=Peer.neighbours.indexOf(currNeighbour);
				newNodeNeighbours.add(currNeighbour);

				PeerData newNodeAsNeighbour=new PeerData();
				newNodeAsNeighbour.ipAddress=address;
				newNodeAsNeighbour.identifier=nodeID;
				newNodeAsNeighbour.zoneList.add(new Zone(newNodeZone.getxStart(),newNodeZone.getxEnd(),newNodeZone.getyStart(),newNodeZone.getyEnd(),newNodeZone.getOwnerNodeID(),null));
				updateNewNeighbour(currNeighbour,newNodeAsNeighbour);

			}
		}
		if(index!=-1){
			Peer.neighbours.remove(index);
		}
		return newNodeNeighbours;
	}
	private void updateNewNeighbour(PeerData currNeighbour,
			PeerData newNodeAsNeighbour) {
		// TODO Auto-generated method stub
		try{
			Socket neighbourSoc=new Socket(currNeighbour.ipAddress,Config.PEER_PORT);
			PrintWriter out=new PrintWriter(neighbourSoc.getOutputStream(),true);
			String updateMessage=CANHelper.getMessage("command", "update neighbour", newNodeAsNeighbour.ipAddress,
					newNodeAsNeighbour.identifier, "!", "!", ""+newNodeAsNeighbour.zoneList.get(0).getyStart());
			//Peer.identifier+"#"+newNodeAsNeighbour.zoneList.get(0).getyStart()+"#"+newNodeAsNeighbour.zoneList.get(0).getyEnd()) ;
			out.println(updateMessage);
		}
		catch(IOException ex){
			ex.printStackTrace();
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
	}

	//public Zone(float xStart,float xEnd, float yStart, float yEnd, String ownerNodeID, ConcurrentHashMap<String,String> map){
	//Takes parameters - The zone to be split for the current node and co-ordinates of the new zone
	private ConcurrentHashMap<String, String> getHashTableForNewNode(Zone zoneToBeSplit, float x_start, float x_end, float y_start, float y_end) {
		// TODO Auto-generated method stub
		Set<String> localTableKeys=zoneToBeSplit.getLocalHashTable().keySet();
		ConcurrentHashMap<String, String> newZoneHashTable=new ConcurrentHashMap<String,String>();
		if(!localTableKeys.isEmpty()){
			for(String key:localTableKeys){
				if(CANHelper.isKeyInNewZone(key,x_start, x_end, y_start, y_end)){
					newZoneHashTable.put(key, zoneToBeSplit.getLocalHashTable().get(key));
					zoneToBeSplit.getLocalHashTable().remove(key);
				}
			}
		}
		return newZoneHashTable;
	}

	private void processMessage(String message) {
		// TODO Auto-generated method stub
		if(message==null || message.isEmpty()){
			System.out.println("Error in method processMesage");
			return;
		}
		String[] elements=message.split("\\|");
		signal=elements[0];
		command=elements[1];
		address=elements[2];
		nodeID=elements[3];
		message_x=elements[4];
		message_y=elements[5];
		params=elements[6];

	}
}
