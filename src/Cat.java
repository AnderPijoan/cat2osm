import java.util.ArrayList;
import java.util.List;

public class Cat {

	int tipoRegistro;
	String refCatastral; // Referencia Catastral
	String subparce; // Codigo Subparcela
	int numOrdenConstru; // Numero de orden de construccion
	List<String[]> atributos;
	long fechaConstru = Long.MAX_VALUE; // Fecha de construccion AAAAMMDD
	// Empieza en el valor maximo y se reduce a la menor fecha de construccion de los inmuebles
	// de la parcela

	
	/** Constructor
	 * @param r Tipo de Registro
	 */
	public Cat(int r){
		tipoRegistro = r;
		atributos = new ArrayList<String[]>();
	}
	
	
	public int getTipoRegistro(){
		return tipoRegistro;
	}
	
	
	public void setRefCatastral(String r){
		refCatastral = r;
	}
	
	
	public void setSubparce(String r){
		subparce = r;
	}
	
	
	public String getRefCatastral(){
		return refCatastral;
	}
	
	
	public int getNumOrdenConstru() {
		return numOrdenConstru;
	}


	public void setNumOrdenConstru(int numOrdenConstru) {
		this.numOrdenConstru = numOrdenConstru;
	}
	
	
	public String getSubparce(){
		return subparce;
	}

	
	public long getFechaConstru() {
		return fechaConstru;
	}


	public void setFechaConstru(long fechaConstru) {
		this.fechaConstru = fechaConstru;
	}


	/** Anade un atributo leido a la lista
	 * @param nombre Clave del atributo
	 * @param valor Valor del atributo
	 */
	public void addAttribute(String nombre, String valor){
		if (valor != null){
			String v = valor.trim();
			String[] temp = {nombre, v};
			if (!v.isEmpty() && !atributos.contains(temp))
				atributos.add(temp);
		}
	}
	
	
	public void addAttribute(List<String[]> l){
		if (l != null)
			atributos.addAll(l);
	}
	
	
	public List<String[]> getAttributes(){
		return atributos;
	}
}
