import java.io.Serializable;
import java.util.ArrayList;


public class PeerData implements Serializable{
	public String ipAddress;
	public String identifier;
	public ArrayList<Zone> zoneList;
	public ArrayList<PeerData> neighbours;
	public PeerData(){
		zoneList=new ArrayList<Zone>();
		neighbours=new ArrayList<PeerData>();
	}
	public boolean isNewNodesNeighbour(Zone newNodeZone) {
		// TODO Auto-generated method stub
		if(newNodeZone.getyStart()<zoneList.get(0).getyStart()){
			return true;
		}
		return false;
	}
}
