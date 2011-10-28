import java.util.ArrayList;
import java.util.List;


public class Cat {

	int tipoRegistro;
	String refCatastral;
	List<String[]> atributos;
	long fechaAlta; // Formato AAAAMMDD
	long fechaBaja;

	/** Constructor
	 * @param r Tipo de Registro
	 */
	public Cat(int r){

		tipoRegistro = r;
		refCatastral = "-1";
		atributos = new ArrayList<String[]>();
		fechaAlta = 00000101;
		fechaBaja = 99999999;
	}
	
	public int getTipoRegistro(){
		return tipoRegistro;
	}
	
	public void setRefCatastral(String r){
		refCatastral = r;
	}
	
	public String getRefCatastral(){
		return refCatastral;
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

	public void addAttribute(String nombre, String valor){
		String[] temp = {nombre, valor};
		if (!atributos.contains(temp))
			atributos.add(temp);
	}
	
	public List<String[]> getAttributes(){
		return atributos;
	}

}
