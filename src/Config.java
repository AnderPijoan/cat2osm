import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Config {

	/** HashMap con la configuracion. */
	private static HashMap<String,String> map = new HashMap<String,String>(20);

	/** Constructor. Abre el fichero y llena el hashMap con los datos.
	 *  El fichero de texto plano con entradas del tipo key=value.
	 *  Las lineas en blanco sera ignoradas asi como las lineas que
	 *  comiencen por #.
	 *  @param file Fichero de configuracion.*/
	public Config(String file)
	{
		String[] aux; // Auxiliar para parsear las lineas
		String   temp = null; // Temporal para recoger las lineas

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
					aux = temp.split("=");
					String concat = aux[1];
					for (int i = 2; i < aux.length; i++)
						concat = concat + "=" + aux[i];
					map.put(aux[0],concat);
				}
				temp = br.readLine();
			}
		} catch (Exception e) { System.out.println("["+new Timestamp(new Date().getTime())+"] Linea inadecuada" +
				" en el archivo de configuracion: \""+ temp +"\"." +
				"\n Los comentarios en el archivo de" +
				" configuracion deben estar indicados con el caracter '#' al inicio.");}
		finally
		{
			try                  {if (null != fr) fr.close();}
			catch (Exception e2) { e2.printStackTrace();}
		}
	}


	/** Obtiene la opcion del hashMap de configuracion. Si no existe devuelve "".
	 *  @param option Opcion de configuracion a buscar.
	 *  @return Valor que tiene en el hashMap o "". */
	public static String get(String option){
		
		if (map.get(option) == null)
			System.out.println("["+new Timestamp(new Date().getTime())+"] No se ha encontrado el campo "+option+" en el archivo de configuración. Compruebe que existe o si no ejecute cat2osm con el parámetro -ui para crear un nuevo archivo de configuración.");
		
		return (map.get(option) == null) ? "" : map.get(option);
	}


	/** Modifica la opcion del hashMap de configuracion.
	 * @param option Opcion a modificar.
	 * @param value  Nuevo valor a poner. */
	public static void set(String option, String value){ 
		map.put(option,value);
	}
	
	
	/** Anade la lista al hashMap de configuracion.
	 * @param l Lista de pares de strings*/
	public static void set(List<String[]> l){

		for (String[] s : l)
			map.put(s[0], s[1]);
	}

}
