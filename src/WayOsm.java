import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class WayOsm {

	private List<Long> nodos; // Nodos que componen ese way
	private List<String> shapes; // Lista de Shapes a los que pertenece, para la simplificacion de ways
	// Esta clase no tiene tags porque al leer los shapefiles, se parte la geometria en el
	// numero maximo posible de ways de dos nodos. Los tags de esa geometria se almacenan en una relacion
	// que estara compuesta por los ways que hemos partido y a la hora de imprimir si se ve que una 
	// relacion solo tiene un mienbro, entonces esa relacion se imprime como way.
	
	public WayOsm(List<Long> l){
		if (l == null)
			this.nodos = new ArrayList<Long>();
		else
			this.nodos = l;
		shapes = new ArrayList<String>();
	}

	/** Anade un nodo al final de la lista, comprueba que no este repetido salvo si es igual que el primero
	 * @param l Nodo a anadir
	 */
	public void addNode(Long l){
		if (!nodos.contains(l))
			nodos.add(l);
		else if (l == nodos.get(0)){
			nodos.add(l);
			}
	}
	
	
	/** Anade un nodo desplazando los existentes hacia la derecha
	 * @param pos
	 * @param l
	 */
	public void addNode(int pos, Long l){
		if (!nodos.contains(l))
			nodos.add(pos, l);
	}
	
	
	/** Anade nodos a la lista de nodos del way comprobando que no puedan estar repetidos salvo el ultimo
	 * que puede ser igual al primero
	 * @param l Lista de nodos a anadir
	 */
	public void addNodes(List<Long> l){
		for (int x = 0; x < l.size(); x++)
			if (x != l.size()-1){
				if(!nodos.contains(l.get(x))) 
					nodos.add(l.get(x));
			}
			else
				if (!nodos.contains(l.get(x)))
					nodos.add(l.get(x));
				else
					if (l.get(x) == nodos.get(0))
						nodos.add(l.get(x));
	}
	
	
	/** Anade una lista de nodos en esa posicion desplazando a la derecha. No comprueba que puedan estar repetidos
	 * @param l Lista de nodos
	 * @param pos Posicion a anadir
	 */
	public void addNodes(List<Long> l, int pos){
		
		nodos.addAll(pos, l);
	}
	
	
	public List<Long> getNodes() {
		return nodos;
	}
	
	
	public void reverseNodes(){
		Collections.reverse(nodos);
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
	
	public synchronized void deleteShape(String shape){
		shapes.remove(shape);
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
		
	
	public List<Long> sortNodes(){
		List<Long> result = new ArrayList<Long>();
		for (Long l : nodos)
			result.add(l);
		Collections.sort(result);
		return result;
	}
	
	
	/** Sobreescribir el hashcode, para que compare los nodos aunque estan en otro orden
	 * para que dos ways con los mismos nodos pero en distinta direccion se detecten como iguales.
	 * ATENCION: ESTO PUEDE DAR PROBLEMAS EN EL FUTURO SI ALGUIEN INTENTA COMPARAR WAYS PARA OTRO USO
	 * DOS WAYS IGUALES PERO EN DISTINTO SENTIDO ESTE LO DA COMO QUE SON EL MISMO, YA QUE A EFECTOS DE
	 * SIMPLIFICACIÓN, TIENEN QUE SER IGUALES.
	 */
	@Override
	public synchronized int hashCode() {
		final int prime = 31 + nodos.size();
		long result = 17;
		for (long l : sortNodes())
			result = result * prime +  (int) (l^(l>>>32));
		
		return (int)result;
	}

	
	/** Sobreescribir el equals, para que compare los nodos aunque estan en otro orden
	 * para que dos ways con los mismos nodos pero en distinta direccion se detecten como iguales.
	 * ATENCION: ESTO PUEDE DAR PROBLEMAS EN EL FUTURO SI ALGUIEN INTENTA COMPARAR WAYS PARA OTRO USO
	 * DOS WAYS IGUALES PERO EN DISTINTO SENTIDO ESTE LO DA COMO QUE SON EL MISMO, YA QUE A EFECTOS DE
	 * SIMPLIFICACIÓN, TIENEN QUE SER IGUALES.
	 */
	@Override
	public synchronized boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;

		WayOsm other = (WayOsm) obj;
		
		if (nodos == null) {
			if (other.nodos != null)
				return false;
		} else if (this.sortNodes().size() == other.sortNodes().size()){
			boolean equal = true;
			for(int x = 0; equal && x < this.sortNodes().size(); x++)
				equal = this.sortNodes().get(x).equals(other.sortNodes().get(x));
			return equal;
			}
			else 
				return false; 
		
		return true;
	}

	
	/** Comprueba si este way esta conectado en alguno de sus nodos a el
	 * way dado 
	 * @param way Way al que comprobar si esta conectado
	 * @return boolean de si lo esta o no
	 */
	public boolean connectedTo(WayOsm way){
		
		boolean encontrado = false;
		
		for (int x = 0; !encontrado && x < this.nodos.size(); x++)
			encontrado = way.getNodes().contains(this.nodos.get(x));
		
		return encontrado;
	}
	
	
	/** Imprime en el formato Osm el way con la informacion
	 * @param id Id del way
	 * @return Devuelve en un String el way listo para imprimir
	 */
	public String printWay(Long id){
		String s = "";
		
		// Si un way no tiene mas de dos nodos, es incorrecto
		if (nodos.size()<2){
			System.out.println("Way id="+ id +" con menos de dos nodos. No se imprimira.");
			return "";
		}
		
		// Si no pertenece a ningun shape porque se ha visto que no tenia tags representativos
		// o se ha delimitado la busqueda
		if (shapes.isEmpty()){
			return "";
		}
		
		
		// Si pertenece a algun shape, es decir si sus shapes no se han desechado por no tener informacion relevante
			if (!shapes.isEmpty()){
		s = ("<way id=\""+ id +"\" timestamp=\""+new Timestamp(new Date().getTime())+"\" version=\"6\">\n");
		
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
