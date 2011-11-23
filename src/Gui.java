import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

public class Gui extends JFrame {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		// Ruta al fichero de configuracion por parametro
		new Config(args[0]);
		Cat2OsmUtils utils = new Cat2OsmUtils();
		Cat2Osm catastro = new Cat2Osm(utils);
		List<Shape> shapes = new ArrayList<Shape>();

		// Recorrer los directorios Urbanos
		File dirU = new File (Config.get("UrbanoSHPPath"));

		if( dirU.exists() && dirU.isDirectory()){
			File[] filesU = dirU.listFiles();
			for(int i=0; i < filesU.length; i++)
				if ( filesU[i].getName().toUpperCase().equals("CONSTRU") ||
						filesU[i].getName().toUpperCase().equals("EJES") ||
						filesU[i].getName().toUpperCase().equals("ELEMLIN") ||
						filesU[i].getName().toUpperCase().equals("ELEMPUN") ||
						filesU[i].getName().toUpperCase().equals("ELEMTEX") ||
						filesU[i].getName().toUpperCase().equals("MASA") ||
						filesU[i].getName().toUpperCase().equals("PARCELA") ||
						filesU[i].getName().toUpperCase().equals("SUBPARCE"))
				try{
				System.out.println("Leyendo "+ filesU[i].getName() +" Urbano.");
				shapes.addAll(
						catastro.shpParser(
								catastro.reprojectWGS84(
										new File(filesU[i] + "\\" + filesU[i].getName() + ".SHP"))));
				catastro.deleteShpFiles(filesU[i].getName().toUpperCase());
				}
			catch(Exception e){}
			}
		else{
			System.out.println("UrbanoSHPDir no es un directorio valido.");
		}

		// Seleccionamos el archivo .cat
		// No todos los shapefiles tienen referencia catastral por lo que algunos
		// no hay forma de relacionarlos con los registros de catastro.
		System.out.println("Leyendo CAT Urbano");
		catastro.catParser(new File(Config.get("UrbanoCATFile")), shapes);

		// Recorrer los directorios Rusticos
		File dirR = new File (Config.get("RusticoSHPPath"));

		if( dirR.exists() && dirR.isDirectory()){
			File[] filesR = dirR.listFiles();
			for(int i=0; i < filesR.length; i++)
				if ( filesR[i].getName().toUpperCase().equals("CONSTRU") ||
						filesR[i].getName().toUpperCase().equals("EJES") ||
						filesR[i].getName().toUpperCase().equals("ELEMLIN") ||
						filesR[i].getName().toUpperCase().equals("ELEMPUN") ||
						filesR[i].getName().toUpperCase().equals("ELEMTEX") ||
						filesR[i].getName().toUpperCase().equals("MASA") ||
						filesR[i].getName().toUpperCase().equals("PARCELA") ||
						filesR[i].getName().toUpperCase().equals("SUBPARCE"))
				try{
				System.out.println("Leyendo "+ filesR[i].getName() +" Rustico.");
				shapes.addAll(
						catastro.shpParser(
								catastro.reprojectWGS84(
										new File(filesR[i] + "\\" + filesR[i].getName() + ".SHP"))));
				catastro.deleteShpFiles(filesR[i].getName().toUpperCase());
				}
			catch(Exception e){}
			}
		else{
			System.out.println("RusticoSHPDir no es un directorio valido. No se han encontrado los shapefiles.");
		}

		// Seleccionamos el archivo .cat
		// No todos los shapefiles tienen referencia catastral por lo que algunos
		// no hay forma de relacionarlos con los registros de catastro.
		System.out.println("Leyendo CAT Rustico");
		catastro.catParser(new File(Config.get("RusticoCATFile")), shapes);

		catastro.simplifyWays(shapes);

		// Escribir los datos
		System.out.println("Escribiendo "+ Cat2Osm.utils.getTotalNodes().size() +" NODOS");
		catastro.printNodes( Cat2Osm.utils.getTotalNodes());
		System.out.println("Escribiendo "+ Cat2Osm.utils.getTotalWays().size() +" WAYS");
		catastro.printWays(Cat2Osm.utils.getTotalWays());
		System.out.println("Escribiendo "+ Cat2Osm.utils.getTotalRelations().size() +" RELATIONS");
		catastro.printRelations( Cat2Osm.utils.getTotalRelations());
		System.out.println("JUNTANDO los tres archivos");
		catastro.joinFiles(Config.get("ResultFileName"));
		System.out.println("TERMINADO");

	}



}
