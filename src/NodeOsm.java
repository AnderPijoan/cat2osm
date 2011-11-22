import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;


public class NodeOsm {

	private Coordinate coor;
	private List<String[]> tags; // TODO Solo se usan para los Elemtex
	
	
	public NodeOsm(Coordinate c){
		coor = new Coordinate();
		// Coordenadas en Lat/Lon. Ogr2Ogr hace el cambio de 
		// UTM a Lat/Lon ya que en el shapefile vienen en UTM
		this.coor.x = c.x; 
		this.coor.y = c.y;
		this.coor.z = c.z;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((coor == null) ? 0 : coor.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeOsm other = (NodeOsm) obj;
		if (coor == null) {
			if (other.coor != null)
				return false;
		} else if (!coor.equals(other.coor))
			return false;
		return true;
	}
	
	public Coordinate getCoor(){
		return coor;
	}
	
	public double getX() {
		return coor.x;
	}

	public void setX(Coordinate c) {
		this.coor.x = c.x;
	}

	public double getY() {
		return coor.y;
	}

	public List<String[]> getTags() {
		return tags;
	}
	
	public void addTags(List<String[]> tags) {
		if (this.tags == null && !tags.isEmpty())
			this.tags = new ArrayList<String[]>();

			for (String [] s : tags)
				this.tags.add(s);
	}
	
	public void setY(Coordinate c) {
		this.coor.y = c.y;
	}

	/** Convierte coordenadas UTM a Lat/Long
	 * @param utm String en formato "Huso Northing Easting" Ejemplo: "30 N 4266508 257016"
	 * @returns double[] [0]Lat [1]Lon                    
	 */
	public static double[] UTM2LatLong(String utm)
	{
		CoordinateConversion obj=new CoordinateConversion();
		return obj.utm2LatLon(utm);
	}
	
	/** Imprime en el formato Osm el nodo con la informacion
	 * @param id Id del nodo
	 * @param huso Huso geografico para la conversion UTM a Lat/Long
	 * @return Devuelve en un String el nodo listo para imprimir
	 */
	public String printNode(Long id, String huso){
		String s = null;
			
		// Hora para el timestamp
		Date date = new java.util.Date();
		s = ("<node id=\""+ id +"\" version=\"6\" timestamp=\""+ new Timestamp(date.getTime()) +"\" uid=\"533679\" user=\"AnderPijoan\" lat=\""+this.coor.y+"\" lon=\""+this.coor.x+"\">\n");
		
		
		if (tags != null)
			for (int x = 0; x < tags.size(); x++)
				s += "<tag k=\""+tags.get(x)[0]+"\" v=\""+tags.get(x)[1]+"\"/>\n";
		
		s += ("</node>\n");
		
		return s;
	}
	
}
