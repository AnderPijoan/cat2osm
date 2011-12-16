import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class WayOsm {

	private List<Long> nodos; // Nodos que componen ese way
	private List<String> shapes; // Lista de Shapes a los que pertenece, para la simplificacion de ways

	public WayOsm(List<Long> l){
		if (l == null)
			this.nodos = new ArrayList<Long>();
		else
			this.nodos = l;
		shapes = new ArrayList<String>();
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
	
	
	public void setShapes(List<String> s){
		shapes = s;
	}
	
	
	public List<String> getShapes(){
		return shapes;
	}
	
	
	public synchronized void addShapes(List<String> shapes){
		for (String s : shapes)
			if (!this.shapes.contains(s))
				this.shapes.add(s);
	}
	
	
	/** Metodo para comparar si dos WayOsm pertenecen a los mismos shapes, de esta forma se
	 * iran simplificando los ways.
	 * @param s Lista de shapes del otro WayOsm a comparar
	 * @return boolean de si pertencen a los mismos o no.
	 */
	public synchronized boolean sameShapes(List<String> s){
		
		if (this.shapes == null || s == null)
			return false;
		
		if (this.shapes.size() != s.size())
			return false; 
				
		List<String> l1 = new ArrayList<String>();
		List<String> l2 = new ArrayList<String>();
		for (String l : this.shapes)
			l1.add(l);
		Collections.sort(l1);
		for (String l : s)
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
		int result = 17;
		for (long l : sortNodos())
			result = result * prime +  (int) (l^(l>>>32));
		
//		result = prime * result + ((sortNodos() == null) ? 0 :sortNodos().hashCode());
		
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
		
		// Si un way no tiene mas de dos nodos, es incorrecto
		if (nodos.size()<2)
			System.out.println("Way id="+ id +" con menos de dos nodos. No se imprimira.");
		else {
		s = ("<way id=\""+ id +"\" version=\"6\">\n");
		
		// Referencias a los nodos
		for (int x = 0; x < nodos.size(); x++)
			s += ("<nd ref=\""+ nodos.get(x) +"\"/>\n");
		
		// Mostrar los shapes que utilizan este way, para debugging
		if (shapes != null && Config.get("PrintShapeIds").equals("1"))
			for (int x = 0; x < shapes.size(); x++)
				s += "<tag k=\"CAT2OSMSHAPEID"+x+"\" v=\""+shapes.get(x)+"\"/>\n";
		
		s += ("</way>\n");
		}
		return s;
	}
	
}
