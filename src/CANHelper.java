import java.util.Iterator;
import java.util.Random;
import java.util.Set;


public class CANHelper {

	public static float generateRandom() {
		// TODO Auto-generated method stub
		//return random value between 0 and 10

		Random random = new Random();
		float fRandom = random.nextFloat() * 10;
		return fRandom;
	}

	public static String getMessage(String messageType, String command, String newNodeAddress,
			String newNodeID, String x, String y, String params) {
		// TODO Auto-generated method stub
		StringBuilder commandBuilder=new StringBuilder();
		commandBuilder.append(messageType);
		commandBuilder.append("|");
		commandBuilder.append(command);
		commandBuilder.append("|");
		commandBuilder.append(newNodeAddress);
		commandBuilder.append("|");
		commandBuilder.append(newNodeID);
		commandBuilder.append("|");
		commandBuilder.append(x);
		commandBuilder.append("|");
		commandBuilder.append(y);
		commandBuilder.append("|");
		commandBuilder.append(params);

		return commandBuilder.toString();

	}

	public static float getDistance(float x_mid, float y_mid, float x, float y) {
		// TODO Auto-generated method stub
		//distance of (x_mid,y_mid) from (x,y)

		float x_diff_square = (x_mid - x) * (x_mid - x);
		float y_diff_square = (y_mid - y) * (y_mid - y);
		return (float) Math.sqrt(x_diff_square + y_diff_square);
	}
	/*
	public static boolean isZoneASquare(Zone zoneToBeSplit) {
		// TODO Auto-generated method stub
		return false;
	}
	 */
	public static boolean isKeyInNewZone(String key, float x_start, float x_end, float y_start, float y_end) {
		// TODO Auto-generated method stub
		//Specifically, the x-coordinate of a keyword (a string) is computed as CharAtOdd mod 10, where CharAtOdd is the addition of the character values at odd positions. 
		//The y-coordinate of the keyword is computed similarly, which is CharAtEven mod 10 where ChatAtEven is the addition of the character values at even positions. Note that a floating-point number can be used for the coordinates for accurate zone splitting. 
	

		float x_hash = getXCoordFrmKey(key);
		float y_hash = getYCoordFrmKey(key);

//		System.out.println(x_hash);
//		System.out.println(y_hash);

		if((x_hash >= x_start && x_hash < x_end) &&
				(y_hash >= y_start && y_hash < y_end)){
			return true;
		}

		return false;
	}


	public static float getXCoordFrmKey(String key)
	{
		int nCharAtOdd = 0;
		int x_hash = 0;

		for (int i = 0; i < key.length(); i++) {
			if((i % 2) == 1){
				nCharAtOdd += key.charAt(i);
			}
		}
//		System.out.println(nCharAtOdd);
		x_hash = nCharAtOdd % 10;
		return x_hash;
	}


	public static float getYCoordFrmKey(String key)
	{
		int nCharAtEven = 0;
		int y_hash = 0;

		for (int i = 0; i < key.length(); i++) {
			if((i % 2) == 0){
				nCharAtEven += key.charAt(i);
			}
		}
//		System.out.println(nCharAtEven);
		y_hash = nCharAtEven % 10;
		return y_hash;
	}

	public static void paintInfo(PeerData nodeInfo) {
		// TODO Auto-generated method stub
		System.out.println("================="+nodeInfo.identifier+"===========================");
		System.out.println("NodeID: "+nodeInfo.identifier);
		System.out.println("ipAddress: "+nodeInfo.ipAddress);
		
		//Zone Info
		System.out.println("Zone co-ordinates: ");
		System.out.println("\t\tX_Start: "+nodeInfo.zoneList.get(0).getxStart());
		System.out.println("\t\tX_End: "+nodeInfo.zoneList.get(0).getxEnd());
		System.out.println("\t\tY_Start: "+nodeInfo.zoneList.get(0).getyStart());
		System.out.println("\t\tY_End: "+nodeInfo.zoneList.get(0).getyEnd());
		
		//Display Neighbours
		String neighbourList="";
		if(nodeInfo.neighbours.size()>0){
			neighbourList=""+nodeInfo.neighbours.get(0).identifier;
			for(int i=1;i<nodeInfo.neighbours.size();i++){
				neighbourList+=", "+nodeInfo.neighbours.get(i).identifier;	
			}
		}
		System.out.println("List of neighbours: [ "+neighbourList+" ]");

		//Display Data
		Set<String> dataKeySet=nodeInfo.zoneList.get(0).getLocalHashTable().keySet();
		Iterator<String> dataItr=dataKeySet.iterator();
		String dataKeyList="";
		if(dataItr.hasNext())
			dataKeyList=dataItr.next();
		while(dataItr.hasNext()){
			dataKeyList+=", "+dataItr.next();
		}
		System.out.println("Keys contained at node "+nodeInfo.identifier+": ["+dataKeyList+"]");
		System.out.println("");
	}
}
