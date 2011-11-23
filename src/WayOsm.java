import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class WayOsm {

	private List<Long> nodos; // Nodos que componen ese way
	private List<String[]> tags; // Tags, para la simplificacion de ways

	public WayOsm(List<Long> l){
		this.nodos = l;
		tags = new ArrayList<String[]>();
	}
	
	public void addNode(Long l){
		if (!nodos.contains(l))
		nodos.add(l);
	}
	
	public void addNodes(List<Long> l){
		for (int x = 0; x < l.size(); x++)
			if (!nodos.contains(l.get(x)))
				nodos.add(l.get(x));
	}
	
	public List<Long> getNodes() {
		return nodos;
	}

	public List<String[]> getTags(){
		return tags;
	}
	
	public void setTags(List<String[]> s){
		tags = s;
	}
	
	public void addTags(List<String[]> s){
		if (!tags.equals(s))
		tags.addAll(s);
	}
	
	public List<Long> sortNodos(){
		List<Long> result = nodos;
		Collections.sort(result);
		return result;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sortNodos() == null) ? 0 :sortNodos().hashCode());
		
		return result;
	}

	/** Sobreescribir el equals, para que compare los nodos aunque estén en otro orden
	 * para que dos ways con los mismos nodos pero en distinta direccion se detecten como iguales.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		WayOsm other = (WayOsm) obj;
		
		if (nodos == null) {
			if (other.nodos != null)
				return false;
		} else if (this.sortNodos().size() == other.sortNodos().size()){
			boolean equal = true;
			for(int x = 0; equal && x < this.sortNodos().size(); x++)
				equal = this.sortNodos().get(x).equals(other.sortNodos().get(x));
				return equal;
			}

		return true;
	}

	/** Imprime en el formato Osm el way con la informacion
	 * @param id Id del way
	 * @return Devuelve en un String el way listo para imprimir
	 */
	public String printWay(Long id){
		String s = null;
		
		// Hora para el timestamp
		Date date = new java.util.Date();
		s = ("<way id=\""+ id +"\" version=\"6\" timestamp=\""+ new Timestamp(date.getTime()) +"\" uid=\"533679\" user=\"AnderPijoan\">\n");
		
		// Referencias a los nodos
		for (int x = 0; x < nodos.size(); x++)
			s += ("<nd ref=\""+ nodos.get(x) +"\"/>\n");
		
		if (tags != null)
			for (int x = 0; x < tags.size(); x++)
				s += "<tag k=\""+tags.get(x)[0]+"\" v=\""+tags.get(x)[1]+"\"/>\n";
		
		s += ("</way>\n");
		
		return s;
	}
	
}
