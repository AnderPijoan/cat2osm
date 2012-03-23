import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {

		if ((args.length == 1 && args[0].equals("-v")) || (args.length == 2 && (args[1].equals("-v") || args[0].equals("-v") ))){
			System.out.println("Cat2Osm versión 2012-03-23.");
		}
		else if ((args.length == 1 && args[0].equals("-ui")) || (args.length == 2 && (args[1].equals("-ui") || args[0].equals("-ui") ))){
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
		else if (args.length == 2 && (
				args[1].replaceAll("-", "").equals("constru") ||
				args[1].replaceAll("-", "").equals("ejes") ||
				args[1].replaceAll("-", "").equals("elemlin") ||  
				args[1].replaceAll("-", "").equals("elempun") ||
				args[1].replaceAll("-", "").equals("elemtex") ||
				args[1].replaceAll("-", "").equals("masa") || 
				args[1].replaceAll("-", "").equals("parcela") || 
				args[1].replaceAll("-", "").equals("subparce")) 
				&& new File(args[0]).exists()){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando Cat2Osm con el archivo de configuración para exportar únicamente "+ args[1].replaceAll("-", "").toUpperCase()+ ".");
			// Ruta al fichero de configuracion por parametro
			new Config(args[0]);
			// Iniciar cat2osm
			ejecutarCat2Osm(args[1].replaceAll("-", "").toUpperCase());
		}
		else if (args.length == 2 && args[1].replaceAll("-", "").equals("portales") && new File(args[0]).exists()){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando Cat2Osm con el archivo de configuración para exportar únicamente el archivo de entradas a parcelas a corregir.");
			// Ruta al fichero de configuracion por parametro
			new Config(args[0]);
			// Iniciar metodo de creacion de puntos de entrada
			crearPortales();
		}
		else if (args.length == 2 && args[1].replaceAll("-", "").equals("usos") && new File(args[0]).exists()){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando Cat2Osm con el archivo de configuración para exportar únicamente el archivo de destinos a corregir.");
			// Ruta al fichero de configuracion por parametro
			new Config(args[0]);
			// Iniciar metodo de creacion de puntos de entrada
			crearUsos();
		}
		else {
			System.out.println("Cat2Osm versión 2012-03-23.\n");
			System.out.println("Forma de uso:");
			System.out.println("java -jar [-XmxMemoria] cat2osm.jar [Opción] / [RutaArchivoConfig] [NombreArchivo]\n");
			System.out.println("Es necesrio indicarle una opción y pasarle el archivo de configuración:");
			System.out.println("rutaarchivoconfig                 Ejecutar Cat2Osm con los parametros que se indiquen en el archivo de configuración del cual se pasa la ruta para llegar hasta él");
			System.out.println("-v                                Muestra la version de Cat2Osm");
			System.out.println("-ui                               Abrir la interfaz de usuario para crear el archivo de configuración");
			System.out.println("rutaarchivoconfig -constru        Generar un archivo con las geometrías CONSTRU");
			System.out.println("rutaarchivoconfig -ejes           Generar un archivo con las geometrías EJES");
			System.out.println("rutaarchivoconfig -elemlin        Generar un archivo con las geometrías ELEMLIN");
			System.out.println("rutaarchivoconfig -elempun        Generar un archivo con las geometrías ELEMPUN");
			System.out.println("rutaarchivoconfig -elemtex        Generar un archivo con las geometrías ELEMTEX y mostrando todos los textos de Parajes y Comarcas, Información urbana y rústica y Vegetación y Accidentes demográficos");
			System.out.println("rutaarchivoconfig -masa           Generar un archivo con las geometrías MASA");
			System.out.println("rutaarchivoconfig -parcela        Generar un archivo con las geometrías PARCELA");
			System.out.println("rutaarchivoconfig -subparce       Generar un archivo con las geometrías SUBPARCE");
			System.out.println("rutaarchivoconfig -portales       Generar un archivo con los números de portal del archivo ELEMTEX y ajustándolos a su parcela más cercana");
			System.out.println("rutaarchivoconfig -usos           Generar un archivo con los usos de inmuebles que no se pueden asignar directamente a una construcción");
			System.out.println("Para mas informacion acceder a:");
			System.out.println("http://wiki.openstreetmap.org/wiki/Cat2Osm");
		}
	}


	/** Ejecutar cat2osm, dependiendo si se indica para un archivo concreto o para todos cambian
	 * un poco las operaciones que hace
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void ejecutarCat2Osm(String archivo) throws IOException, InterruptedException{

		// Clases
		Cat2OsmUtils utils = new Cat2OsmUtils();
		Cat2Osm catastro = new Cat2Osm(utils);
		
		// Nos aseguramos de que existe la carpeta result
		File dir = new File(Config.get("ResultPath"));
		if (!dir.exists()) 
		{
			System.out.println("["+new Timestamp(new Date().getTime())+"] Creando el directorio donde almacenar el resultado ("+Config.get("ResultParh")+").");
			try                { dir.mkdirs(); }
			catch (Exception e){ e.printStackTrace(); }
		}
		
		
		Pattern p = Pattern.compile("\\d{4}-\\d{1,2}");
		Matcher m = p.matcher(Config.get("UrbanoCATFile"));
		
		if (m.find()) {
			Cat2OsmUtils.setFechaActual(Long.parseLong(m.group().substring(0, 4)+"0101"));
		}
		else{
			System.out.println("["+new Timestamp(new Date().getTime())+"] El archivo Cat Urbano debe tener el formato de nombre que viene por defecto en Catastro. El nombre debe traer la fecha de creación y en este no se ha encontrado.");
			System.exit(-1);
		}

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
					System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer alguno de los archivos shapefiles rústicos. " + e.getMessage());}
			}
			else
				System.out.println("["+new Timestamp(new Date().getTime())+"] El directorio de shapefiles rústicos "+Config.get("RusticoSHPPath")+" no existe.");


			for (ShapeParser sp : parsers)
				sp.join();

			// Leemos archivo .cat
			// No todos los shapefiles tienen referencia catastral por lo que algunos
			// no hay forma de relacionarlos con los registros de catastro.
			if (archivo.equals("*") || archivo.equals("CONSTRU") || archivo.equals("PARCELA") || archivo.equals("SUBPARCE")){
				try {
					System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat urbano.");
					catastro.catParser(new File(Config.get("UrbanoCATFile")), shapes);
				}catch(Exception e)
				{System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer archivo Cat urbano. " + e.getMessage());}

				try {
					System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat rústico.");
					catastro.catParser(new File(Config.get("RusticoCATFile")), shapes);
				}catch(Exception e)
				{System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer archivo Cat rústico. " + e.getMessage());}	
			}

			// Calculando los usos / destinos de las parcelas en funcion del que mas area tiene
			System.out.println("["+new Timestamp(new Date().getTime())+"] Calculando usos de las parcelas.");
			shapes = catastro.calcularUsos(shapes);
			
			// Simplificamos los ways
			if (archivo.equals("*") || archivo.equals("CONSTRU") || archivo.equals("EJES") || archivo.equals("ELEMLIN") || archivo.equals("MASA") || archivo.equals("PARCELA") || archivo.equals("SUBPARCE")){
				System.out.println("["+new Timestamp(new Date().getTime())+"] Simplificando vias.");
				shapes = catastro.simplificarWays(shapes);
			}
			
			// Escribir los datos
			System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo nodos");
			catastro.printNodes( Cat2Osm.utils.getTotalNodes());
			
			if (archivo.equals("*") || archivo.equals("CONSTRU") || archivo.equals("EJES") || archivo.equals("ELEMLIN") || archivo.equals("MASA") || archivo.equals("PARCELA") || archivo.equals("SUBPARCE")){
				System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo ways");
				catastro.printWays(Cat2Osm.utils.getTotalWays());
				System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo relations");
				catastro.printRelations( Cat2Osm.utils.getTotalRelations());
			}
		}
		else 
			System.out.println("["+new Timestamp(new Date().getTime())+"] Se han encontrado 3 archivos temporales de una posible ejecución interrumpida, se procederá a juntarlos en un archivo resultado.");

		System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo el archivo resultado");
		catastro.juntarFiles(Config.get("ResultFileName") + (archivo.equals("*")? "" : archivo.toUpperCase()));
		System.out.println("["+new Timestamp(new Date().getTime())+"] Terminado");
	}


	/** Metodo para utilizar solamente los numeros de policia de los elementos Elemtex y moverlos a la parcela
	 * mas cercana
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void crearPortales() throws InterruptedException, IOException{

		// Clases
		Cat2OsmUtils utils = new Cat2OsmUtils();
		Cat2Osm catastro = new Cat2Osm(utils);
		Cat2Osm.utils.setModoPortales(true);
		
		Pattern p = Pattern.compile("\\d{4}-\\d{1,2}");
		Matcher m = p.matcher(Config.get("UrbanoCATFile"));
		
		if (m.find()) {
			Cat2OsmUtils.setFechaActual(Long.parseLong(m.group().substring(0, 4)+"0101"));
		}
		else{
			System.out.println("["+new Timestamp(new Date().getTime())+"] El archivo Cat Urbano debe tener el formato de nombre que viene por defecto en Catastro. El nombre debe traer la fecha de creación y en este no se ha encontrado.");
			System.exit(-1);
		}

		if (!new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempRelations.osm").exists()
				&& !new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempWays.osm").exists()
				&& !new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempNodes.osm").exists()){

			// Listas
			List<Shape> shapes = new ArrayList<Shape>();
			List<ShapeParser> parsers = new ArrayList<ShapeParser>();

			// Recorrer los directorios Urbanos
			File dirU = new File (Config.get("UrbanoSHPPath"));

			// Cogemos los shapefile concretos para calcular los portales
			if( dirU.exists() && dirU.isDirectory()){
				File[] filesU = dirU.listFiles();
				for(int i=0; i < filesU.length; i++)
					if ( 	filesU[i].getName().toUpperCase().equals("ELEMTEX") ||
							filesU[i].getName().toUpperCase().equals("PARCELA")
							)
						try{
							System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesU[i].getName() +" urbano.");
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
							System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesR[i].getName() +" rústico.");
							parsers.add(new ShapeParser("RU", new File(filesR[i] + "/" + filesR[i].getName() + ".SHP"), utils, shapes));
						}
				catch(Exception e)
				{
					System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer alguno de los archivos shapefiles rústicos. " + e.getMessage());}
			}
			else
				System.out.println("["+new Timestamp(new Date().getTime())+"] El directorio de shapefiles rústicos "+Config.get("RusticoSHPPath")+" no existe.");

			for (ShapeParser sp : parsers)
				sp.join();

			// Mover las entradas de las casas a sus respectivas parcelas
			System.out.println("["+new Timestamp(new Date().getTime())+"] Moviendo puntos de entrada a sus parcelas mas cercanas.");
			shapes = catastro.calcularPortales(shapes);

			// Borramos todos los nodos shapes de parcelas para que no los dibuje
			Iterator<Shape> iterator = shapes.iterator();

			while(iterator.hasNext()) {
			Shape shape = iterator.next();
			if(shape instanceof ShapeParcela){
					iterator.remove();
					for(int x = 0; x < shape.getPoligons().size(); x++)
						utils.deleteNodes(shape.getNodesIds(x));
				}
			}
		
			
			//Escribir los datos
			System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo nodos");
			catastro.printNodes( Cat2Osm.utils.getTotalNodes());
		}
		else 
			System.out.println("["+new Timestamp(new Date().getTime())+"] Se han encontrado 3 archivos temporales de una posible ejecución interrumpida, se procederá a juntarlos en un archivo resultado.");

			System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo el archivo resultado");
			catastro.juntarFiles(Config.get("ResultFileName")+"PORTALES");
			System.out.println("["+new Timestamp(new Date().getTime())+"] Terminado");

	}

	
	public static void crearUsos() throws IOException, InterruptedException{

		// Clases
		Cat2OsmUtils utils = new Cat2OsmUtils();
		Cat2Osm catastro = new Cat2Osm(utils);
		utils.setModoUsos(true);
		
		Pattern p = Pattern.compile("\\d{4}-\\d{1,2}");
		Matcher m = p.matcher(Config.get("UrbanoCATFile"));
		
		if (m.find()) {
			Cat2OsmUtils.setFechaActual(Long.parseLong(m.group().substring(0, 4)+"0101"));
		}
		else{
			System.out.println("["+new Timestamp(new Date().getTime())+"] El archivo Cat Urbano debe tener el formato de nombre que viene por defecto en Catastro. El nombre debe traer la fecha de creación y en este no se ha encontrado.");
			System.exit(-1);
		}

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
					if ( filesU[i].getName().toUpperCase().equals("PARCELA") )
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
					if ( filesR[i].getName().toUpperCase().equals("PARCELA"))
						try{
							System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesR[i].getName() +" Rustico.");
							parsers.add(new ShapeParser("RU", new File(filesR[i] + "/" + filesR[i].getName() + ".SHP"), utils, shapes));
						}
				catch(Exception e)
				{
					System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer alguno de los archivos shapefiles rústicos. " + e.getMessage());}
			}
			else
				System.out.println("["+new Timestamp(new Date().getTime())+"] El directorio de shapefiles rústicos "+Config.get("RusticoSHPPath")+" no existe.");


			for (ShapeParser sp : parsers)
				sp.join();

			// Leemos archivo .cat
			// No todos los shapefiles tienen referencia catastral por lo que algunos
			// no hay forma de relacionarlos con los registros de catastro.
			try {
				System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat urbano.");
				catastro.catUsosParser(new File(Config.get("UrbanoCATFile")), shapes);
			}catch(Exception e)
			{System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer archivo Cat urbano. " + e.getMessage());}

			try {
				System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat rústico.");
				catastro.catUsosParser(new File(Config.get("RusticoCATFile")), shapes);
			}catch(Exception e)
			{System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer archivo Cat rústico. " + e.getMessage());}	

			
			// Borramos todos los nodos shapes de parcelas para que no los dibuje
			Iterator<Shape> iterator = shapes.iterator();

			while(iterator.hasNext()) {
				Shape shape = iterator.next();
				if(shape instanceof ShapeParcela){
					iterator.remove();
					for(int x = 0; x < shape.getPoligons().size(); x++)
						utils.deleteNodes(shape.getNodesIds(x));
				}
			}
			

			// Escribir los datos
			System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo nodos");
			catastro.printNodes( Cat2Osm.utils.getTotalNodes());
			
		}
		else 
			System.out.println("["+new Timestamp(new Date().getTime())+"] Se han encontrado 3 archivos temporales de una posible ejecución interrumpida, se procederá a juntarlos en un archivo resultado.");

		System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo el archivo resultado");
		catastro.juntarFiles(Config.get("ResultFileName") + "USOS");
		System.out.println("["+new Timestamp(new Date().getTime())+"] Terminado");
	}
	
}
