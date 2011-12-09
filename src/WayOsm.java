import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class WayOsm {

	private List<Long> nodos; // Nodos que componen ese way
	private List<Long> shapes; // Lista de Shapes a los que pertenece, para la simplificacion de ways

	public WayOsm(List<Long> l){
		if (l == null)
			this.nodos = new ArrayList<Long>();
		else
			this.nodos = l;
		shapes = new ArrayList<Long>();
	}
	
	
	public void addNode(Long l){
		if (!nodos.contains(l))
		nodos.add(l);
	}
	
	
	public void addNode(int pos, Long l){
		if (!nodos.contains(l))
		nodos.add(pos, l);
	}
	
	
	public void addNodes(List<Long> l){
		for (int x = 0; x < l.size(); x++)
				nodos.add(l.get(x));
	}
	
	
	public List<Long> getNodes() {
		return nodos;
	}
	
	
	public void setShapes(List<Long> s){
		shapes = s;
	}
	
	
	public List<Long> getShapes(){
		return shapes;
	}
	
	
	public void addShapes(List<Long> s){
		for (Long l : s)
			if (!shapes.contains(l))
				shapes.add(l);
	}
	
	
	/** Metodo para comparar si dos WayOsm pertenecen a los mismos shapes, de esta forma se
	 * iran simplificando los ways.
	 * @param s Lista de shapes del otro WayOsm a comparar
	 * @return boolean de si pertencen a los mismos o no.
	 */
	public boolean sameShapes(List<Long> s){
		
		if (this.shapes == null || s == null)
			return false;
		
		if (this.shapes.size() != s.size())
			return false; 
				
		List<Long> l1 = new ArrayList<Long>();
		List<Long> l2 = new ArrayList<Long>();
		for (Long l : this.shapes)
			l1.add(l);
		Collections.sort(l1);
		for (Long l : s)
			l2.add(l);
		Collections.sort(l2);
		
		return l1.equals(l2);
	}
	
	
	/** Invierte el orden de la lista de los nodos y la devuelve.
	 * @return La lista de nodos despues de haber invertido el orden.
	 */
	public void reverseNodes(){
		Collections.reverse(nodos);
	}
	
	
	public List<Long> sortNodos(){
		List<Long> result = new ArrayList<Long>();
		for (Long l : nodos)
			result.add(l);
		Collections.sort(result);
		return result;
	}
	
	
	/** Sobreescribir el hashcode, para que compare los nodos aunque estén en otro orden
	 * para que dos ways con los mismos nodos pero en distinta direccion se detecten como iguales.
	 * ATENCION: ESTO PUEDE DAR PROBLEMAS EN EL FUTURO SI ALGUIEN INTENTA COMPARAR WAYS PARA OTRO USO
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sortNodos() == null) ? 0 :sortNodos().hashCode());
		
		return result;
	}

	
	/** Sobreescribir el equals, para que compare los nodos aunque estén en otro orden
	 * para que dos ways con los mismos nodos pero en distinta direccion se detecten como iguales.
	 * ATENCION: ESTO PUEDE DAR PROBLEMAS EN EL FUTURO SI ALGUIEN INTENTA COMPARAR WAYS
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
		//s = ("<way id=\""+ id +"\" version=\"6\" timestamp=\""+ new Timestamp(date.getTime()) +"\">\n");
		s = ("<way id=\""+ id +"\" version=\"6\">\n");
		
		
		if (nodos.size()<2)
			System.out.println("Way con menos de dos nodos.");
		
		// Referencias a los nodos
		for (int x = 0; x < nodos.size(); x++)
			s += ("<nd ref=\""+ nodos.get(x) +"\"/>\n");
		
		if (shapes != null)
			for (int x = 0; x < shapes.size(); x++)
				s += "<tag k=\"shape"+x+"\" v=\""+shapes.get(x)+"\"/>\n";
		
		s += ("</way>\n");
		
		return s;
	}
	
}
