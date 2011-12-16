import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;


public class NodeOsm {

	private Coordinate coor;
	private List<String[]> tags; // Se usan para los Elemtex y Elempun
	
	
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
	public synchronized boolean equals(Object obj) {
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

			for (int x = 0; x < tags.size(); x++){

				boolean encontrado = false;
				tags.get(x)[1] = tags.get(x)[1].replaceAll("\"", "");
				
				for (int y = 0; !encontrado && y < this.tags.size(); y++)
					if (this.tags.get(y)[0].equals(tags.get(x)[0])){
						this.tags.get(y)[1] = tags.get(x)[1];
						encontrado = true;
					}
				if (!encontrado)
					this.tags.add(tags.get(x));
			}
	}
	
	
	public void addTag(String[] tag){
		if (this.tags == null)
			this.tags = new ArrayList<String[]>();
		
		boolean encontrado = false;
		tag[1] = tag[1].replaceAll("\"", "");
		
		for (int x = 0; !encontrado && x < this.tags.size(); x++)
			if (this.tags.get(x)[0].equals(tag[0])){
				this.tags.get(x)[1] = tag[1];
				encontrado = true;
			}

		if (!encontrado)
			this.tags.add(tag);
	}
	
		
	public void setY(Coordinate c) {
		this.coor.y = c.y;
	}
	
	
	/** Imprime en el formato Osm el nodo con la informacion
	 * @param id Id del nodo
	 * @param huso Huso geografico para la conversion UTM a Lat/Long
	 * @return Devuelve en un String el nodo listo para imprimir
	 * @throws UnsupportedEncodingException 
	 */
	public String printNode(Long id){
		String s = null;
			
		s = ("<node id=\""+ id +"\" version=\"6\" lat=\""+this.coor.y+"\" lon=\""+this.coor.x+"\">\n");
		
		
		if (tags != null)
			for (int x = 0; x < tags.size(); x++)
				s += "<tag k=\""+tags.get(x)[0]+"\" v=\""+tags.get(x)[1]+"\"/>\n";
		
		s += ("</node>\n");
		
		return s;
	}
	
}
