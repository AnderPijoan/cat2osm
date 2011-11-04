import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class WayOsm {

	private List<Long> nodos; // Nodos que componen ese way
	private String refCatastral; // Referencia catastral para la simplificacion de ways
	//private List<String[]> tags; //TODO No creo que sean necesarios

	public WayOsm(List<Long> l){
		this.nodos = l;
		this.refCatastral = "";
		//tags = new ArrayList<String[]>();
	}
	
	public void addNode(Long l){
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
	
	/*public List<String[]> getTags() {
		return tags;
	}

	public void setTags(List<String[]> tags) {
		this.tags = tags;
	}*/
	
	/*public void addTags(List<String[]> tags) {
		boolean encontrado = false;
		for (int x = 0; x < tags.size(); x++){
			encontrado = false;
			for (int y = 0 ; y < this.tags.size() && !encontrado; y++){
				String[] s;
				if (this.tags.contains(tags.get(x)))
					encontrado = true;
				else if (tags.get(x)[0].equals(this.tags.get(y)[0])){
					s = this.tags.get(y);
					this.tags.remove(y);
					s[1] += tags.get(x)[1];
					this.tags.add(s);
					encontrado = true;
				}
			}
			if (!encontrado) this.tags.add(tags.get(x));
		}
	}*/

	public String getRefCat(){
		return refCatastral;
	}
	
	public void setRefCat(String s){
		refCatastral = s;
	}
	
	public void addRefCat(String s){
		if (!refCatastral.equals(s))
		refCatastral += s;
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
		} else if (this.sortNodos() == other.sortNodos())
				return true;

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
		s = ("<way id=\""+ id +"\" version=\"6\" timestamp=\""+ new Timestamp(date.getTime()) +"\" uid=\"292702\" user=\"AnderPijoan\" changeset=\"5407370\">\n");
		
		// Referencias a los nodos
		for (int x = 0; x < nodos.size(); x++)
			s += ("<nd ref=\""+ nodos.get(x) +"\"/>\n");
		
		/*for (int x = 0; x < tags.size(); x++)
			s += "<tag k=\""+tags.get(x)[0]+"\" v=\""+tags.get(x)[1]+"\"/>\n";
		*/
		
		if (refCatastral != null && !refCatastral.isEmpty())
		s += "<tag k=\"ref-cat\" v=\""+refCatastral+"\"/>\n";
		
		s += ("</way>\n");
		
		return s;
	}
	
}
