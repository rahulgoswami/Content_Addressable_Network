import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.can.Config;


public class BootStrap {
//	public static List<Socket> connectionList=new ArrayList<Socket>();
//	public static List<String> userNames=new ArrayList<String>();
//	public static HashMap<String,String> nodeList=new HashMap<String,String>();
//---------------------------------------------------------------------------------
	public static String ipAddress=Config.BOOTSTRAP_IP;
	public static int port=Config.BOOTSTRAP_PORT;
	public static ConcurrentHashMap<String,HashMap<String,String>> canNodesInfo=new ConcurrentHashMap<String,HashMap<String,String>>();
	public static int nodeIDGenerator=1;
	public static HashMap<String,ArrayList<String>> errorLog= new HashMap<String,ArrayList<String>>();
	//public static HashSet<HashMap<String,String>> canNodesInfo = new HashSet<HashMap<String,String>>(); 
	public static void main(String[] args){
		BootStrap boot=new BootStrap();
		boot.startServer();
	}

	private void startServer() {
		// TODO Auto-generated method stub
		try {
			ServerSocket servSock=new ServerSocket(port);
		//	System.out.println("Waiting for users to join...");
			while(true){
				System.out.println("Waiting for new connections...");
				Socket listenSoc=servSock.accept();
				System.out.println("Connection successful");
				PrintWriter out = new PrintWriter(listenSoc.getOutputStream(), true);
		//		connectionList.add(listenSoc);
		//		System.out.println(listenSoc.getLocalAddress().getHostName()+" just connected");
		//		addUserName(listenSoc);
				
				//listenSoc.getRemoteSocketAddress().toString().split(":")[0]
				
				String id=BootStrap.getNodeId(listenSoc.getRemoteSocketAddress().toString().split(":")[0].replace("/",""));
				System.out.println("ID for the requesting node is: "+id);
				if(id==null || !BootStrap.canNodesInfo.get(id).get(Config.IS_ACTIVE_KEY).equals("true")){
					out.println("Welcome! Enter \"join me\" to join the CAN");
				}
				
				BootStrapThread connThread=new BootStrapThread(listenSoc);
				Thread sThread=new Thread(connThread);
				sThread.start();
				
		//		displayCANInfo();			
		//		sThread.join();
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private static void displayCANInfo() {
		// TODO Auto-generated method stub
		Set<String> map=canNodesInfo.keySet();
		for(String key:map){
			System.out.println(key+":<ipaddress:"+canNodesInfo.get(key).get(Config.IP_KEY)+">,<isActive:"+canNodesInfo.get(key).get(Config.IS_ACTIVE_KEY));
		}
	}

	public static String generateNodeId(){
		int localIDGenerator=BootStrap.nodeIDGenerator;
		return "Node_"+String.valueOf(localIDGenerator);
	}
	private void addUserName(Socket listenSoc) throws IOException {
		// TODO Auto-generated method stub
		/*
		Scanner scan=new Scanner(listenSoc.getInputStream());
		String username=scan.nextLine();
		userNames.add(username);
		userMap.put(listenSoc,username);
		for(Socket userSoc:connectionList){
			PrintWriter pw=new PrintWriter(userSoc.getOutputStream());
			pw.println(userNames);
			pw.flush();
		}
		
		//Try to remove this line if any problems reading
		scan.close();
*/
	}
	public static void logError(String nodeID,String message){
		ArrayList<String> errors;
		if(errorLog.containsKey(nodeID)){
			errors=BootStrap.errorLog.get(nodeID);
			
		}
		else{
			errors=new ArrayList<String>();
		}
		errors.add(message);
		BootStrap.errorLog.put(nodeID,errors);
	}

	public static boolean isNewNode(String address) {
		// TODO Auto-generated method stub
		if(address!=null)
			address=address.trim();
		if(getIPAddressList().contains(address))
			return false;
		return true;
	}

	private static List<String> getIPAddressList() {
		// TODO Auto-generated method stub
		List<String> ipList=new ArrayList<String>();
		if(!canNodesInfo.isEmpty()){
			Iterator<String> itr=canNodesInfo.keySet().iterator();
			while(itr.hasNext()){
				ipList.add(canNodesInfo.get(itr.next()).get("ip"));
			}
		}
		return ipList;
		
	}

	public static String getNodeId(String address) {
		// TODO Auto-generated method stub
		List<String> ipList=getIPAddressList();
		String key=null;
		if(!canNodesInfo.isEmpty()){
			Iterator<String> itr=canNodesInfo.keySet().iterator();
			while(itr.hasNext()){
				//ipList.add(canNodesInfo.get(itr.next()).get("ip"));
				key=itr.next();
				if(canNodesInfo.get(key).get("ip").equals(address)){
					return key;
				}
			}
		}
		return null;
	}

	public static Set<String> getActiveNodeIDs() {
		// TODO Auto-generated method stub
		Set<String> nodeSet=new HashSet<String>();
		if(!canNodesInfo.isEmpty()){
			Iterator<String> itr=canNodesInfo.keySet().iterator();
			String key=null;
			while(itr.hasNext()){
				key=itr.next();
				if(canNodesInfo.get(key).get(Config.IS_ACTIVE_KEY).equals("true"))
					nodeSet.add(key);
			}
		}
		return nodeSet;
		
	}
	public static HashMap<String,HashMap<String,String>> getActiveNodesInfo(String activeNodeId){
		try{
		HashMap<String,HashMap<String,String>> activeNodes=new HashMap<String,HashMap<String,String>>();
		if(activeNodeId==null || BootStrap.canNodesInfo.isEmpty()){
			return activeNodes;
		}
		if(activeNodeId.equals("all")){
			Set<String> keys=BootStrap.canNodesInfo.keySet();
			for(String id:keys){
				if(canNodesInfo.get(id).get(Config.IS_ACTIVE_KEY).equals("true")){
					activeNodes.put(id, canNodesInfo.get(id));
				}
			}
		}
		else{
			if(canNodesInfo.containsKey(activeNodeId) && canNodesInfo.get(activeNodeId).get(Config.IS_ACTIVE_KEY).equals("true")){
				activeNodes.put(activeNodeId,canNodesInfo.get(activeNodeId));
			}
		}
		return activeNodes;
		}
		catch(OutOfMemoryError ex){
			ex.printStackTrace();
			return null;
		}
	}

}
