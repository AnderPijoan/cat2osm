import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.h2.util.IOUtils;


public class Config {

	/** Properties con la configuracion. */
	private static Properties configuration = new Properties();

	
	/**
	 * Carga la configuración.
	 * @param file archivo de configuracion.
	 */
	public static void loadConfig(String file) {
		FileReader reader = null;
		try {
			reader = new FileReader(new File(file));
			configuration.load(reader);
		} catch (Exception e) {
			System.out
					.println("["
							+ new Timestamp(new Date().getTime())
							+ "] Linea inadecuada"
							+ " en el archivo de configuracion: \""
							+ ""
							+ "\"."
							+ "\n Los comentarios en el archivo de"
							+ " configuracion deben estar indicados con el caracter '#' al inicio.");
		} finally {
			try {
				if (null != reader) {
					reader.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		
		postProcessConfig ();
	}
	/**
	 * Post-procesa el archivo de configuración:
	 * - Descomprime los archivos zip de catastro si están comprimidos.
	 * - Obtiene el sistema de coordenadas de los shapes leyendo de sus prj.
	 * - Descarga la rejilla si no está descargada y está en modmo auto.
	 * 
	 */
	private static void postProcessConfig() {
		createDirAtHome();
		normalizeVariableContent("UrbanoSHPPath");
		normalizeVariableContent("RusticoSHPPath");
		try {
			CatExtractor.extract(Config.get("UrbanoSHPPath"));
			CatExtractor.extract(Config.get("RusticoSHPPath"));
		}
		catch (IOException ioe){
			ioe.printStackTrace();
		}
		String proyeccion = configuration.getProperty("Proyeccion", "");
		String directorio = Config.get("UrbanoSHPPath");
		if (StringUtils.isBlank(directorio)){
			directorio = Config.get("RusticoSHPPath");
		}		
		if (!StringUtils.isBlank(directorio) && (StringUtils.isBlank(proyeccion) || "auto".equals(proyeccion))){
			configuration.setProperty(
				"Proyeccion", 
				CatProjectionReader.autodetectProjection(directorio)
			);
		}
		configureGrid();
	}
	/** 
	 * Crea un directorio para catosm (".catosm") en el directorio del usuario.
	 * En este directorio se guardarán los archivos de rejilla.
	 */
	private static void createDirAtHome() {
		File directorioCatOsm = new File(System.getProperty("user.home") + File.separator + ".cat2osm");
		directorioCatOsm.mkdirs();
		configuration.setProperty("home", directorioCatOsm.getAbsolutePath());
	}
	/**
	 * Elimina la extensión zip del nombre de archivo de shp de catastro.
	 * Para usar en el resto del programa el directorio en lugar del zip
	 * @param varName
	 */
	private static void normalizeVariableContent(String varName) {
		String value = Config.get(varName);
		if (value != null && value.toLowerCase().endsWith(".zip")){
			Config.set(varName, value.replaceFirst("\\.[Zz][Ii][Pp]$", ""));
		}
	}
	/**
	 * Cambia la configuración de la rejilla si está en modo auto.
	 * Si es necesario, descarga la rejilla de http://www.01.ign.es/ign/resources/herramientas/
	 * y lo descarga en ${home}/.catosm/
	 */
	private static void configureGrid() {
		String rejilla = configuration.getProperty("NadgridsPath");
		if (StringUtils.isBlank(rejilla)){
			rejilla = "auto:peninsula";
		}
		if (!rejilla.startsWith("auto")){
			return;
		}
		File homeDir = new File(System.getProperty("user.home") + File.separator + ".cat2osm");
		File archivoRejilla = null;
		String url = "";
		if ("auto:peninsula".equals(rejilla)){
			url = "http://www.01.ign.es/ign/resources/herramientas/PENR2009.zip";
			archivoRejilla = new File(homeDir, "peninsula.gsb");
		}
		else if ("auto:baleares".equals(rejilla)){
			url = "http://www.01.ign.es/ign/resources/herramientas/BALR2009.zip";
			archivoRejilla = new File(homeDir, "baleares.gsb");
		}		
		configuration.setProperty("NadgridsPath", archivoRejilla.getAbsolutePath());
		downloadGrid(url, archivoRejilla);
	}
	/**
	 * Descarga el archivo de rejilla y lo descomprime.
	 * @param url
	 * @param destino
	 */
	private static void downloadGrid(String url, File destino) {
		if (destino.exists()) {
			return;
		}
		try {
			File temporal = File.createTempFile("cat2osm", "tmp.zip");
			System.out.println("["+new Timestamp(new Date().getTime())+"] Descargando rejilla "+url);
			URL descarga = new URL(url);
			InputStream inputStream = descarga.openStream();
			OutputStream outputStream = new FileOutputStream(temporal);
			IOUtils.copy(inputStream, outputStream);
			inputStream.close();
			outputStream.close();
			
			ZipInputStream zis = new ZipInputStream(new FileInputStream(temporal));
			zis.getNextEntry();
			OutputStream outputStreamDestino = new FileOutputStream(destino);
			IOUtils.copy(zis, outputStreamDestino);
			zis.closeEntry();
			zis.close();
			outputStreamDestino.close();
			temporal.delete();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Obtiene la opcion de configuracion. Si no existe devuelve "".
	 *  @param option Opcion de configuracion a buscar.
	 *  @param required Indica si la opción es obligatoria (true) u opcional (false).
	 *  @return Valor que tiene en el hashMap o "". */
	public static String get(String option, boolean required){
		
		if (configuration.get(option) == null && required){
			System.out.println("["+new Timestamp(new Date().getTime())+"] No se ha encontrado el campo "+option+" en el archivo de configuración. Compruebe que existe o si no ejecute cat2osm con el parámetro -ui para crear un nuevo archivo de configuración.");
		}
		return configuration.getProperty(option, "");
	}
	
	public static String get(String option) {
		return get(option, true);
	}


	/** Modifica la opcion del hashMap de configuracion.
	 * @param option Opcion a modificar.
	 * @param value  Nuevo valor a poner. */
	public static void set(String option, String value){ 
		configuration.put(option,value);
	}
	
	
	/** Anade la lista al hashMap de configuracion.
	 * @param l Lista de pares de strings*/
	public static void set(List<String[]> l){

		for (String[] s : l)
			configuration.put(s[0], s[1]);
	}

}
