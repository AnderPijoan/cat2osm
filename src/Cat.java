import java.util.ArrayList;
import java.util.List;

public class Cat {

	int tipoRegistro;
	String refCatastral; // Referencia Catastral
	String subparce; // Codigo Subparcela
	List<String[]> atributos;
	long fechaAlta; // Formato AAAAMMDD
	long fechaBaja;

	
	/** Constructor
	 * @param r Tipo de Registro
	 */
	public Cat(int r){
		tipoRegistro = r;
		atributos = new ArrayList<String[]>();
		// Valor minimo posible
		fechaAlta = 00000101;
		// Valor maximo posible
		fechaBaja = 99999999;
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
	
	
	public String getSubparce(){
		return subparce;
	}
	
	
	public void setFechaAlta(long l){
		fechaAlta = l;
	}
	
	
	public void setFechaBaja(long a){
		fechaBaja = a;
	}
	
	
	public long getFechaAlta(){
		return fechaAlta;
	}
	
	
	public long getFechaBaja(){
		return fechaBaja;
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
