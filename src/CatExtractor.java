import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;



public class CatExtractor {

	public static void extract (String filename) throws IOException{
		if (filename == null || "".equals(filename.trim())){
			return;
		}
		File directorio = new File(filename);
		File archivoZip = new File(directorio.getAbsolutePath() + ".zip");
		if (directorio.exists() && directorio.isDirectory()) {
			return;
		}
		if (archivoZip.exists()){
			//System.out.println("Descomprimiendo el archivo de catastro " + archivoZip.getName());
			directorio.mkdirs();
			ZipInputStream zis = new ZipInputStream(new FileInputStream(archivoZip));
			extract(directorio, zis);
			zis.close();			
		}
	}
	private static void extract (File directorio, ZipInputStream zis) throws IOException {
		
		ZipEntry entry = null;
		while ( (entry = zis.getNextEntry()) != null) {
			String nombre = entry.getName();
			if (nombre.toLowerCase().endsWith(".zip")){
				nombre = nombre.substring(0, nombre.indexOf('.'));
				File subdirectorio = new File(directorio, nombre);
				subdirectorio.mkdirs();
				ZipInputStream contenido = new ZipInputStream(zis);
				extract (subdirectorio, contenido);
			}
			else {
				System.out.println("Descomprimiendo archivo " + nombre);
				File archivo = new File(directorio, nombre);
				OutputStream fos = new FileOutputStream(archivo);
				IOUtils.copy(zis, fos);
			}
			zis.closeEntry();
		}
		
	}
}
