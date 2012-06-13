import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.regex.Pattern;

/** Clase para acceder a un fichero de reglas de proceso para ELEMTEX. 
 * El fichero es un conjunto de líneas de la forma: 
 * regla \t tag1 \t ... \t tagN 
 * Donde regla es una expresión regular y las etiquetas tienen la forma key=value 
 * Estas reglas se usarán para asignar etiquetas en función del rótulo. 
 * Las reglas que no tengan etiquetas sirven para definir como 
 * no valido el elemento que cumpla la regla. */
public class Rules {

	/** Clase interna que define una entrada del fichero de reglas
	 */
	private class RuleEntry {
		private Pattern rule;
		private List<String[]> tags = new ArrayList<String[]>();
		/** Constructor de regla de validacion
		 */
		public RuleEntry(String rule) {
			this.rule = Pattern.compile(rule, Pattern.CASE_INSENSITIVE);
		}
		/** Constructor de regla de etiquetado
		 */
		public RuleEntry(String[] tags) {
			this.rule = Pattern.compile(tags[0], Pattern.CASE_INSENSITIVE);
			for (int i=1; i<tags.length; i++) {
				String[] t = tags[i].split("=");
				if (t.length != 2 || (t.length==2 && t[1].trim().equals(""))) {
					throw new IllegalArgumentException("Etiqueta mal definida.");
				}
				this.tags.add(t);
			}
		}
		/** Comprueba la regla
		 * @param s Rotulo a comprobar
		 * @return true si s cumple la regla
		 */
		public boolean matches(String s) {
			return this.rule.matcher(s).matches();
		}
		/** Devuelve las etiquetas si se cumple la regla
		 */
		public List<String[]> getTags(String s) {
			List<String[]> t = new ArrayList<String[]>();
			if (matches(s)) {
				t.addAll(this.tags);
			}
			return t;
		}
	}
	
	/** Lista con reglas de proceso para etiquetas. */
	private static List<RuleEntry> tagslist = new ArrayList<RuleEntry>();
	/** Lista con reglas de proceso para validación. */
	private static List<RuleEntry> validlist = new ArrayList<RuleEntry>();

	/** Constructor. Abre el fichero y llena las listas con reglas.
	 *  Las lineas en blanco sera ignoradas asi como las lineas que
	 *  comiencen por #.
	 *  @param file Fichero de configuracion. */
	public Rules(String file) {
		String[] aux; // Auxiliar para parsear las lineas
		String   temp = null; // Temporal para recoger las lineas
		String   rule, tags;

		File           f  = null;
		FileReader     fr = null;
		BufferedReader br = null;

		try
		{
			f  = new File(file);
			fr = new FileReader(f);
			br = new BufferedReader(fr);

			temp = br.readLine();
			while (temp != null) // Mientras haya datos
			{
				// Ignora comentarios y lineas en blanco
				if ((temp.length() != 0) && (temp.charAt(0) != '#'))
				{
					aux = temp.split("\t+");
					if (aux.length == 1) {
						validlist.add(new RuleEntry(aux[0]));
					} else {
						tagslist.add(new RuleEntry(aux));
					}
				}
				temp = br.readLine();
			}
		} catch (Exception e) { System.out.println("["+new Timestamp(new Date().getTime())+"] Linea inadecuada" +
				" en el archivo de reglas: \""+ temp +"\"." +
				"\n Los comentarios en el archivo de" +
				" reglas deben estar indicados con el caracter '#' al inicio.");}
		finally
		{
			try                  {if (null != fr) fr.close();}
			catch (Exception e2) { e2.printStackTrace();}
		}
	}
	
	/**
	 * Comprueba si s cumple alguna regla (no es valido)
	 * @param s rotulo a validar
	 * @return true si es valido
	 */
	public static boolean isValid(String s) {
		boolean match = false;
		Iterator<RuleEntry> it = validlist.iterator();
		while (it.hasNext() && !match) {
			match = it.next().matches(s);
		}
		return !match;
	}
	
	/**
	 * Devuelve las etiquetas de las reglas que se cumplan
	 * @param s rotulo a comprobar
	 * @return lista de etiquetas
	 */
	public static List<String[]> getTags(String s) {
		List<String[]> tags = new ArrayList<String[]>();
		for (RuleEntry r: tagslist) {
			tags.addAll(r.getTags(s));
		}
		return tags;
	}
}