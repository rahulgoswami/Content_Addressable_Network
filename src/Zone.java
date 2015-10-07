import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


public class Zone implements Serializable {
	private float xStart;
	private float xEnd;
	private float yStart;
	private float yEnd;
	private String ownerNodeID;
	private ConcurrentHashMap<String,String> localHashTable;
	//private String ownerPort;
	public Zone(){
		setLocalHashTable(new ConcurrentHashMap<String,String>());
	}
	public Zone(float xStart,float xEnd, float yStart, float yEnd, String ownerNodeID, ConcurrentHashMap<String,String> map){
		this.xStart=xStart;
		this.xEnd=xEnd;
		this.yStart=yStart;
		this.yEnd=yEnd;
		this.ownerNodeID=ownerNodeID;
		this.setLocalHashTable(map);
	}
	public float getxStart() {
		return xStart;
	}
	public void setxStart(float xStart) {
		this.xStart = xStart;
	}
	public float getxEnd() {
		return xEnd;
	}
	public void setxEnd(float xEnd) {
		this.xEnd = xEnd;
	}
	public float getyStart() {
		return yStart;
	}
	public void setyStart(float yStart) {
		this.yStart = yStart;
	}
	public float getyEnd() {
		return yEnd;
	}
	public void setyEnd(float yEnd) {
		this.yEnd = yEnd;
	}
	
	public String getOwnerNodeID() {
		return ownerNodeID;
	}
	public void setOwnerNodeID(String ownerNodeID) {
		this.ownerNodeID = ownerNodeID;
	}
	public ConcurrentHashMap<String, String> getLocalHashTable() {
		return localHashTable;
	}
	public void setLocalHashTable(ConcurrentHashMap<String, String> localHashTable) {
		this.localHashTable = localHashTable;
	}
	public boolean containsPoint(float x, float y) {
		// TODO Auto-generated method stub
		if(xStart<=x && xEnd>x && yStart<=y && yEnd>y)
			return true;
		
		return false;
	}
}
