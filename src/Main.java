import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Main {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {

		if (args.length == 1 && args[0].equals("-v")){
			System.out.println("Cat2Osm versión 2012-03-02.");
		}
		else if (args.length == 1 && args[0].equals("-ui")){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando interfaz visual para crear el archivo de configuración.");
			// Iniciar el interfaz visual
			new Gui();
		}
		else if (args.length ==1 && new File(args[0]).exists()){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando Cat2Osm con el archivo de configuración.");
			// Ruta al fichero de configuracion por parametro
			new Config(args[0]);
			// Iniciar cat2osm
			ejecutarCat2Osm("*");
		}
		else if (args.length == 2 && (args[1].replaceAll("-", "").equals("constru") || args[1].replaceAll("-", "").equals("ejes") || args[1].replaceAll("-", "").equals("elemlin") || args[1].replaceAll("-", "").equals("elempun") || args[1].replaceAll("-", "").equals("elemtex") || args[1].replaceAll("-", "").equals("masa") || args[1].replaceAll("-", "").equals("parcela") || args[1].replaceAll("-", "").equals("subparce")) && new File(args[0]).exists()){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando Cat2Osm con el archivo de configuración para exportar únicamente "+ args[1].replaceAll("-", "").toUpperCase()+ ".");
			// Ruta al fichero de configuracion por parametro
			new Config(args[0]);
			// Iniciar cat2osm
			ejecutarCat2Osm(args[1].replaceAll("-", "").toUpperCase());
		}
		else if (args.length == 2 && args[1].replaceAll("-", "").equals("entradas") && new File(args[0]).exists()){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando Cat2Osm con el archivo de configuración para exportar únicamente el archivo de entradas a parcelas a corregir.");
			// Ruta al fichero de configuracion por parametro
			new Config(args[0]);
			// Iniciar metodo de creacion de puntos de entrada
			crearEntradas();
		}
		else {
			System.out.println("Cat2Osm versión 2012-03-02.\n");
			System.out.println("Forma de uso:");
			System.out.println("java -jar [-XmxMemoria] cat2osm.jar [Opción] / [RutaArchivoConfig] [NombreArchivo]\n");
			System.out.println("Es necesrio indicarle una opción y pasarle el archivo de configuración:");
			System.out.println("rutaarchivoconfig                 Ruta al archivo de configuración para ejecutar Cat2Osm con los parametros que se indiquen en él");
			System.out.println("-v                                Muestra la version de Cat2Osm");
			System.out.println("-ui                               Inicia la interfaz de usuario para crear el archivo de configuración");
			System.out.println("rutaarchivoconfig constru         Ejecutar Cat2Osm pero únicamente utilizando el shapefile CONSTRU");
			System.out.println("rutaarchivoconfig ejes            Ejecutar Cat2Osm pero únicamente utilizando el shapefile EJES");
			System.out.println("rutaarchivoconfig elemlin         Ejecutar Cat2Osm pero únicamente utilizando el shapefile ELEMLIN");
			System.out.println("rutaarchivoconfig elempun         Ejecutar Cat2Osm pero únicamente utilizando el shapefile ELEMPUN");
			System.out.println("rutaarchivoconfig elemtex         Ejecutar Cat2Osm pero únicamente utilizando el shapefile ELEMTEX");
			System.out.println("rutaarchivoconfig masa            Ejecutar Cat2Osm pero únicamente utilizando el shapefile MASA");
			System.out.println("rutaarchivoconfig parcela         Ejecutar Cat2Osm pero únicamente utilizando el shapefile PARCELA");
			System.out.println("rutaarchivoconfig subparce        Ejecutar Cat2Osm pero únicamente utilizando el shapefile SUBPARCE");
			System.out.println("Para mas informacion acceder a:");
			System.out.println("http://wiki.openstreetmap.org/wiki/Spanish_Catastro");
		}
	}

	/** Ejecutar cat2osm
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void ejecutarCat2Osm(String archivo) throws IOException, InterruptedException{

		// Clases
		Cat2OsmUtils utils = new Cat2OsmUtils();
		Cat2Osm catastro = new Cat2Osm(utils);

		if (!new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempRelations.osm").exists()
				&& !new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempWays.osm").exists()
				&& !new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempNodes.osm").exists()){


			// Listas
			List<Shape> shapes = new ArrayList<Shape>();
			List<ShapeParser> parsers = new ArrayList<ShapeParser>();


			// Recorrer los directorios Urbanos
			File dirU = new File (Config.get("UrbanoSHPPath"));

			// Si archivo es * cogemos todos los shapefiles necesarios para obtener el resultado
			// Si se indica un shapefile concreto cogemos solo ese
			if( dirU.exists() && dirU.isDirectory()){
				File[] filesU = dirU.listFiles();
				for(int i=0; i < filesU.length; i++)
					if ( (archivo.equals("*") && (
							filesU[i].getName().toUpperCase().equals("CONSTRU") ||
							filesU[i].getName().toUpperCase().equals("ELEMLIN") ||
							filesU[i].getName().toUpperCase().equals("ELEMPUN") ||
							filesU[i].getName().toUpperCase().equals("ELEMTEX") ||
							filesU[i].getName().toUpperCase().equals("PARCELA") ||
							filesU[i].getName().toUpperCase().equals("SUBPARCE")
							))
							|| filesU[i].getName().toUpperCase().equals(archivo)
							)
						try{

							System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesU[i].getName() +" Urbano.");
							parsers.add(new ShapeParser("UR", new File(filesU[i] + "/" + filesU[i].getName() + ".SHP"), utils, shapes));

						}
				catch(Exception e)
				{
					System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer alguno de los shapefiles urbanos. " + e.getMessage());}
			}

			else
				System.out.println("["+new Timestamp(new Date().getTime())+"] El directorio de shapefiles urbanos "+Config.get("UrbanoSHPPath")+" no existe.");

			// Recorrer los directorios Rusticos
			File dirR = new File (Config.get("RusticoSHPPath"));

			if( dirR.exists() && dirR.isDirectory()){
				File[] filesR = dirR.listFiles();
				for(int i=0; i < filesR.length; i++)
					if ( (archivo.equals("*") && (
							filesR[i].getName().toUpperCase().equals("CONSTRU") ||
							filesR[i].getName().toUpperCase().equals("ELEMLIN") ||
							filesR[i].getName().toUpperCase().equals("ELEMPUN") ||
							filesR[i].getName().toUpperCase().equals("ELEMTEX") ||
							filesR[i].getName().toUpperCase().equals("PARCELA") ||
							filesR[i].getName().toUpperCase().equals("SUBPARCE") 
							))
							|| filesR[i].getName().toUpperCase().equals(archivo)
							)
						try{
							System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesR[i].getName() +" Rustico.");
							parsers.add(new ShapeParser("RU", new File(filesR[i] + "/" + filesR[i].getName() + ".SHP"), utils, shapes));
						}
				catch(Exception e)
				{
					System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer alguno de los archivos shapefiles rusticos. " + e.getMessage());}
			}
			else
				System.out.println("["+new Timestamp(new Date().getTime())+"] El directorio de shapefiles rusticos "+Config.get("RusticoSHPPath")+" no existe.");


			for (ShapeParser sp : parsers)
				sp.join();

			// Seleccionamos el archivo .cat
			// No todos los shapefiles tienen referencia catastral por lo que algunos
			// no hay forma de relacionarlos con los registros de catastro.
			try {

				System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat urbano.");
				catastro.catParser(new File(Config.get("UrbanoCATFile")), shapes);

			}catch(Exception e)
			{
				System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer archivo Cat urbano. " + e.getMessage());
			}	
			try {
				System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat rustico.");
				catastro.catParser(new File(Config.get("RusticoCATFile")), shapes);
			}catch(Exception e)
			{
				System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer archivo Cat rustico. " + e.getMessage());
			}	

			// Anadimos si es posible tags de los Elemtex a las parcelas.
			// Los Elemtex tienen informacion que puede determinar con mas exactitud detalles
			// de la parcela sobre la que se encuentran.
			if (Config.get("ElemtexAConstru").equals("1")){
				System.out.println("["+new Timestamp(new Date().getTime())+"] Traspasando posibles tags de Elemtex a Constru.");
				shapes = catastro.pasarElemtexLanduseAConstru(shapes);
			}

			// Simplificamos los ways
			System.out.println("["+new Timestamp(new Date().getTime())+"] Simplificando vias.");
			shapes = catastro.simplificarWays(shapes);

			// Escribir los datos
			System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo nodos");
			catastro.printNodes( Cat2Osm.utils.getTotalNodes());
			System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo ways");
			catastro.printWays(Cat2Osm.utils.getTotalWays());
			System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo relations");
			catastro.printRelations( Cat2Osm.utils.getTotalRelations());
		}
		else 
			System.out.println("["+new Timestamp(new Date().getTime())+"] Se han encontrado 3 archivos temporales de una posible ejecución interrumpida, se procederá a juntarlos en un archivor resultado.");

		System.out.println("["+new Timestamp(new Date().getTime())+"] Juntando los tres archivos");
		catastro.juntarFiles(Config.get("ResultFileName"));
		System.out.println("["+new Timestamp(new Date().getTime())+"] Terminado");
	}

	public static void crearEntradas() throws InterruptedException, IOException{



		// Clases
		Cat2OsmUtils utils = new Cat2OsmUtils();
		Cat2Osm catastro = new Cat2Osm(utils);
		Cat2Osm.utils.setModoEntradas(true);

		if (!new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempRelations.osm").exists()
				&& !new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempWays.osm").exists()
				&& !new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempNodes.osm").exists()){

			// Listas
			List<Shape> shapes = new ArrayList<Shape>();
			List<ShapeParser> parsers = new ArrayList<ShapeParser>();

			// Recorrer los directorios Urbanos
			File dirU = new File (Config.get("UrbanoSHPPath"));

			// Si archivo es * cogemos todos los shapefiles necesarios para obtener el resultado
			// Si se indica un shapefile concreto cogemos solo ese
			if( dirU.exists() && dirU.isDirectory()){
				File[] filesU = dirU.listFiles();
				for(int i=0; i < filesU.length; i++)
					if ( 	filesU[i].getName().toUpperCase().equals("ELEMTEX") ||
							filesU[i].getName().toUpperCase().equals("PARCELA")
							)
						try{
							System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesU[i].getName() +" Urbano.");
							parsers.add(new ShapeParser("UR", new File(filesU[i] + "/" + filesU[i].getName() + ".SHP"), utils, shapes));
						}
				catch(Exception e)
				{
					System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer alguno de los shapefiles urbanos. " + e.getMessage());}
			}

			else
				System.out.println("["+new Timestamp(new Date().getTime())+"] El directorio de shapefiles urbanos "+Config.get("UrbanoSHPPath")+" no existe.");

			// Recorrer los directorios Rusticos
			File dirR = new File (Config.get("RusticoSHPPath"));

			if( dirR.exists() && dirR.isDirectory()){
				File[] filesR = dirR.listFiles();
				for(int i=0; i < filesR.length; i++)
					if ( 	filesR[i].getName().toUpperCase().equals("ELEMTEX") ||
							filesR[i].getName().toUpperCase().equals("PARCELA")
							)
						try{
							System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesR[i].getName() +" Rustico.");
							parsers.add(new ShapeParser("RU", new File(filesR[i] + "/" + filesR[i].getName() + ".SHP"), utils, shapes));
						}
				catch(Exception e)
				{
					System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer alguno de los archivos shapefiles rusticos. " + e.getMessage());}
			}
			else
				System.out.println("["+new Timestamp(new Date().getTime())+"] El directorio de shapefiles rusticos "+Config.get("RusticoSHPPath")+" no existe.");

			for (ShapeParser sp : parsers)
				sp.join();


			// Seleccionamos el archivo .cat
			// No todos los shapefiles tienen referencia catastral por lo que algunos
			// no hay forma de relacionarlos con los registros de catastro.
			try {

				System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat urbano.");
				catastro.catParser(new File(Config.get("UrbanoCATFile")), shapes);

			}catch(Exception e)
			{
				System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer archivo Cat urbano. " + e.getMessage());
			}	
			try {
				System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat rustico.");
				catastro.catParser(new File(Config.get("RusticoCATFile")), shapes);
			}catch(Exception e)
			{
				System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer archivo Cat rustico. " + e.getMessage());
			}	

			// Mover las entradas de las casas a sus respectivas parcelas
			shapes = catastro.calcularEntradas(shapes);

			//Simplificamos los ways
			System.out.println("["+new Timestamp(new Date().getTime())+"] Simplificando vias.");
			shapes = catastro.simplificarWays(shapes);

			//Escribir los datos
			System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo nodos");
			catastro.printNodes( Cat2Osm.utils.getTotalNodes());
			System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo ways");
			catastro.printWays(Cat2Osm.utils.getTotalWays());
			System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo relations");
			catastro.printRelations( Cat2Osm.utils.getTotalRelations());

			System.out.println("["+new Timestamp(new Date().getTime())+"] Juntando los tres archivos");
			catastro.juntarFiles(Config.get("ResultFileName"));
			System.out.println("["+new Timestamp(new Date().getTime())+"] Terminado");

		}

	}

}
