import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class CatProjectionReader {
	/**
	 * Trata de autodectar el sistema de coordenadas de un archivo prj
	 * @param directorio
	 * @return
	 */

	public static String autodetectProjection(String directorio) {
		File archivoPrj = buscarArchivoPrj(new File(directorio));
		if (archivoPrj != null){
			try {
				return autodetectProjectionFromFile(archivoPrj);
			} catch (IOException  e) {
				e.printStackTrace();
			}
		}
		return "";
	}
	
	/**
	 * Busca un archivo prj dentro de un directorio.
	 * Devuelve el primero que encuentra
	 * @param directorio
	 * @return
	 */
	private static File buscarArchivoPrj(File directorio) {

		
		File[] contenidos = directorio.listFiles();
		for (File archivo: contenidos) {
			if (archivo.isDirectory()){
				File buscado = buscarArchivoPrj(archivo);
				if (buscado != null) {
					return buscado;
				}
			}
			if (archivo.isFile() && archivo.getName().toLowerCase().endsWith(".prj")){
				return archivo;
			}
		}
		return null;
	}
	/**
	 * Lee un archivo prj y devuelve el código de sistema de coordenadas
	 * @param archivo
	 * @return
	 * @throws IOException
	 */
	public static String autodetectProjectionFromFile(File archivo) throws IOException {
		String contenido = readFile(archivo);
		contenido = contenido.replaceAll("\",.*", "");
		contenido = contenido.replaceAll(".*\"", "");
		switch(contenido) {
		// TODO Faltan más casos/codigos
		case "ED_19950_UTM_Zone_29N": return "23029";
		case "ED_19950_UTM_Zone_30N": return "23030";
		case "ED_19950_UTM_Zone_31N": return "23031";
		case "ETRS_1989_UTM_Zone_29N": return "25829";
		case "ETRS_1989_UTM_Zone_30N": return "25830";
		case "ETRS_1989_UTM_Zone_31N": return "25831";
		
		default: return "";
		}

	}
	private static String readFile(File archivo) throws IOException {
		String resultado = null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(archivo)));
			resultado = reader.readLine();
		}
		finally {
			if (reader != null){
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return resultado;
	}
	/*
	 * Así no funciona: o no lo soporta bien o requiere de configuración adicional
	public static String autodetectProjectionFromFileGeoTools(File archivo) throws IOException, FactoryException {
		FileInputStream inputStream = new FileInputStream(archivo);
		FileChannel channel = inputStream.getChannel();

		PrjFileReader reader = new PrjFileReader(channel);
		inputStream.close();
		CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
		return crs.getName().getCode();
	}
	
	public static void main (String[] args) throws IOException, FactoryException {
		System.out.println(autodetectProjection("/home/alberto/cat2osm/moratinos/files/Moratinos/34_109_RA_2012-09-20_SHF"));
	}
	*/
}
