import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
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
			System.out.println("Cat2Osm versión "+Cat2Osm.VERSION+".");
		}
		else if ((args.length == 1 && args[0].equals("-ui")) || (args.length == 2 && (args[1].equals("-ui") || args[0].equals("-ui") ))){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando interfaz visual para crear el archivo de configuración.");
			// Iniciar el interfaz visual
			new Gui();
		}
		else if (args.length ==1 && new File(args[0]).exists()){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando Cat2Osm con el archivo de configuración " + args[0] + ".");
			// Ruta al fichero de configuracion por parametro
			new Config(args[0]);
			// Iniciar cat2osm
			ejecutarCat2Osm("*");
		}
		else if (args.length == 2 && args[1].replaceAll("-", "").equals("portales") && new File(args[0]).exists()){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando Cat2Osm con el archivo de configuración " + args[0] + " para exportar únicamente el archivo de entradas a parcelas.");
			// Ruta al fichero de configuracion por parametro
			new Config(args[0]);
			// Iniciar metodo de creacion de puntos de entrada
			//			crearPortales();
		}
		else if (args.length == 2 && args[1].replaceAll("-", "").equals("ejes") && new File(args[0]).exists()){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando Cat2Osm con el archivo de configuración " + args[0] + " para exportar únicamente el archivo de ejes.");
			// Ruta al fichero de configuracion por parametro
			new Config(args[0]);
			// Iniciar metodo de creacion de puntos de entrada
			crearEjes();
		}
		else if (args.length == 2 && (
				args[1].replaceAll("-", "").equals("constru") ||
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
		else if (args.length == 2 && args[1].replaceAll("-", "").equals("usos") && new File(args[0]).exists()){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando Cat2Osm con el archivo de configuración para exportar únicamente el archivo de destinos a corregir.");
			// Ruta al fichero de configuracion por parametro
			new Config(args[0]);
			// Iniciar metodo de creacion de puntos de entrada
			//			crearUsos();
		}
		else {
			System.out.println("Cat2Osm versión "+Cat2Osm.VERSION+".\n");
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

		// Cuando queremos ver todo el archivo Elemtex, tendremos que mostrar no solo las entradas sino todo
		if (archivo.equals("ELEMTEX"))
			Cat2Osm.utils.setOnlyEntrances(false);

		// Cuando queremos ver todo el archivo Constru, tendremos que mostrar todas las geometrias ya que en la ejecucion
		// normal no se usan todas
		if (archivo.equals("CONSTRU"))
			Cat2Osm.utils.setOnlyConstru(true);

		// Nos aseguramos de que existe la carpeta result
		File dir = new File(Config.get("ResultPath"));
		if (!dir.exists()) 
		{
			System.out.println("["+new Timestamp(new Date().getTime())+"] Creando el directorio donde almacenar el resultado ("+Config.get("ResultPath")+").");
			try                { dir.mkdirs(); }
			catch (Exception e){ e.printStackTrace(); }
		}


		Pattern p = Pattern.compile("\\d{4}-\\d{1,2}");
		Matcher m = p.matcher(Config.get("UrbanoCATFile"));

		if (m.find()) {
			Cat2OsmUtils.setFechaArchivos(Long.parseLong(m.group().substring(0, 4)+"0101"));
		}
		else{
			System.out.println("["+new Timestamp(new Date().getTime())+"] El archivo Cat Urbano debe tener el formato de nombre que viene por defecto en Catastro (XX_XX_U_aaaa-mm-dd.CAT) para leer de él la fecha de creación.");
			System.exit(-1);
		}

		// Si va a leer ELEMTEX comprueba si existe fichero de reglas
		if (archivo.equals("ELEMTEX")) {
			if (!Config.get("ElemtexRules", false).equals("") && new File(Config.get("ElemtexRules", false)).exists()) {
				System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo de reglas " + Config.get("ElemtexRules", false));
				new Rules(Config.get("ElemtexRules", false));
			}
		}


		// Listas
		// Lista de shapes, agrupados por codigo de masa a la que pertenecen
		// Si es un tipo de shapes que no tienen codigo de masa se meteran en una cuyo
		// codigo sea el nombre del archivo shapefile
		HashMap <String, List<Shape>> shapes = new HashMap <String, List<Shape>>();
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
				catastro.catParser("UR", new File(Config.get("UrbanoCATFile")), shapes);
			}catch(Exception e)
			{System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer archivo Cat urbano. " + e.getMessage());}

			try {
				System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat rústico.");
				catastro.catParser("RU", new File(Config.get("RusticoCATFile")), shapes);
			}catch(Exception e)
			{System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer archivo Cat rústico. " + e.getMessage());}	
		}

		System.out.println("["+new Timestamp(new Date().getTime())+"] Leídas "+utils.getTotalNodes().size()+" masas de nodos, " +
				utils.getTotalWays().size()+" masas de ways y "+ utils.getTotalRelations().size()+" masas de relations.");

		int pos = 0;
		for (String key : utils.getTotalNodes().keySet()){

			System.out.println("["+new Timestamp(new Date().getTime())+"] Exportando masa " + key + "[" + pos++ +"/" + utils.getTotalNodes().keySet().size() + "]");
			
			// Por si acaso si hubiera archivos de un fallo en ejecucion anterior
			if (new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + key +"tempRelations.osm").exists()
					&& new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + key +"tempWays.osm").exists()
					&& new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + key +"tempNodes.osm").exists()){

				System.out.println("["+new Timestamp(new Date().getTime())+"] Se han encontrado 3 archivos temporales de una posible ejecución interrumpida, se procederá a juntarlos en un archivo resultado.");
				catastro.juntarFiles(key, Config.get("ResultFileName"));
				System.out.println("["+new Timestamp(new Date().getTime())+"] ¡¡Terminada la exportación!!");

			}
			else {

				// Calcular los usos / destinos de las parcelas en funcion del que mas area tiene
				System.out.println("["+new Timestamp(new Date().getTime())+"] Calculando usos de las parcelas.");
				catastro.calcularUsos(key, shapes.get(key));

				//				// Mover las entradas de las casas a sus respectivas parcelas
				//				if (archivo.equals("*")){
				//					System.out.println("["+new Timestamp(new Date().getTime())+"] Moviendo puntos de entrada a sus parcelas mas cercanas.");
				//					shapes = catastro.calcularEntradas(shapes);
				//				}

				// Operacion de simplificacion de relaciones sin tags relevantes
				if (archivo.equals("*")){
					System.out.println("["+new Timestamp(new Date().getTime())+"] Simplificando Relaciones sin tags relevantes.");
					catastro.simplificarRelationsSinTags(key, shapes.get(key));
				}

				// Operacion de simplifiacion de vias
				if (archivo.equals("*") || archivo.equals("CONSTRU") || archivo.equals("ELEMLIN") || archivo.equals("MASA") || archivo.equals("PARCELA") || archivo.equals("SUBPARCE")){
					System.out.println("["+new Timestamp(new Date().getTime())+"] Simplificando vias.");
					catastro.simplificarWays(key, shapes.get(key));
				}

				// Escribir los datos
				if (archivo.equals("*") || archivo.equals("CONSTRU") || archivo.equals("ELEMLIN") || archivo.equals("MASA") || archivo.equals("PARCELA") || archivo.equals("SUBPARCE")){
					System.out.print("["+new Timestamp(new Date().getTime())+"] Escribiendo relations.\r");
					catastro.printRelations(key, shapes.get(key));
					System.out.println("["+new Timestamp(new Date().getTime())+"] Escritas relations, Escribiendo ways.\r");
					catastro.printWays(key, shapes.get(key));
				}
				System.out.println("["+new Timestamp(new Date().getTime())+"] Escritas relations, Escritos ways, Escribiendo nodos.\r");
				catastro.printNodes(key, shapes.get(key));			

				System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo el archivo resultado.");
				catastro.juntarFiles(key, Config.get("ResultFileName") + key + (archivo.equals("*")? "" : archivo.toUpperCase()));
				System.out.println("["+new Timestamp(new Date().getTime())+"] Terminado");

			}
		}

	}


	//	/** Metodo para utilizar solamente los archivos de parcelas para crear sobre ellas nodos con todos sus usos y destinos
	//	 * leidos de los registros del .CAT 
	//	 * @throws InterruptedException
	//	 * @throws IOException
	//	 */
	//	public static void crearUsos() throws IOException, InterruptedException{
	//
	//		// Clases
	//		Cat2OsmUtils utils = new Cat2OsmUtils();
	//		Cat2Osm catastro = new Cat2Osm(utils);
	//		utils.setOnlyUsos(true);
	//
	//		Pattern p = Pattern.compile("\\d{4}-\\d{1,2}");
	//		Matcher m = p.matcher(Config.get("UrbanoCATFile"));
	//
	//		if (m.find()) {
	//			Cat2OsmUtils.setFechaArchivos(Long.parseLong(m.group().substring(0, 4)+"0101"));
	//		}
	//		else{
	//			System.out.println("["+new Timestamp(new Date().getTime())+"] El archivo Cat Urbano debe tener el formato de nombre que viene por defecto en Catastro (XX_XX_U_aaaa-mm-dd.CAT) para leer de él la fecha de creación.");
	//			System.exit(-1);
	//		}
	//
	//		if (!new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempRelations.osm").exists()
	//				&& !new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempWays.osm").exists()
	//				&& !new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempNodes.osm").exists()){
	//
	//
	//			// Listas
	//			// Lista de shapes, agrupados por codigo de masa a la que pertenecen
	//			// Si es un tipo de shapes que no tienen codigo de masa se meteran en una cuyo
	//			// codigo sea el nombre del archivo shapefile
	//			HashMap <String, List<Shape>> shapes = new HashMap <String, List<Shape>>();
	//			List<ShapeParser> parsers = new ArrayList<ShapeParser>();
	//
	//
	//			// Recorrer los directorios Urbanos
	//			File dirU = new File (Config.get("UrbanoSHPPath"));
	//
	//			// Leemos solo los archivos de parcela
	//			if( dirU.exists() && dirU.isDirectory()){
	//				File[] filesU = dirU.listFiles();
	//				for(int i=0; i < filesU.length; i++)
	//					if ( filesU[i].getName().toUpperCase().equals("PARCELA") )
	//						try{
	//
	//							System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesU[i].getName() +" Urbano.");
	//							parsers.add(new ShapeParser("UR", new File(filesU[i] + "/" + filesU[i].getName() + ".SHP"), utils, shapes));
	//
	//						}
	//				catch(Exception e)
	//				{
	//					System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer alguno de los shapefiles urbanos. " + e.getMessage());}
	//			}
	//
	//			else
	//				System.out.println("["+new Timestamp(new Date().getTime())+"] El directorio de shapefiles urbanos "+Config.get("UrbanoSHPPath")+" no existe.");
	//
	//			// Recorrer los directorios Rusticos
	//			File dirR = new File (Config.get("RusticoSHPPath"));
	//
	//			if( dirR.exists() && dirR.isDirectory()){
	//				File[] filesR = dirR.listFiles();
	//				for(int i=0; i < filesR.length; i++)
	//					if ( filesR[i].getName().toUpperCase().equals("PARCELA"))
	//						try{
	//							System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesR[i].getName() +" Rustico.");
	//							parsers.add(new ShapeParser("RU", new File(filesR[i] + "/" + filesR[i].getName() + ".SHP"), utils, shapes));
	//						}
	//				catch(Exception e)
	//				{
	//					System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer alguno de los archivos shapefiles rústicos. " + e.getMessage());}
	//			}
	//			else
	//				System.out.println("["+new Timestamp(new Date().getTime())+"] El directorio de shapefiles rústicos "+Config.get("RusticoSHPPath")+" no existe.");
	//
	//
	//			for (ShapeParser sp : parsers)
	//				sp.join();
	//
	//			// Leemos archivo .cat
	//			// No todos los shapefiles tienen referencia catastral por lo que algunos
	//			// no hay forma de relacionarlos con los registros de catastro.
	//			try {
	//				System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat urbano.");
	//				catastro.catUsosParser(new File(Config.get("UrbanoCATFile")), shapes);
	//			}catch(Exception e)
	//			{System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer archivo Cat urbano. " + e.getMessage());}
	//
	//			try {
	//				System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat rústico.");
	//				catastro.catUsosParser(new File(Config.get("RusticoCATFile")), shapes);
	//			}catch(Exception e)
	//			{System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer archivo Cat rústico. " + e.getMessage());}	
	//
	//
	//			// Borramos todos los nodos de shapes de parcelas para que no los dibuje
	//			for(String key : shapes.keySet())
	//				for (Shape s : shapes.get(key))
	//					if(s instanceof ShapeParcela){
	//						for(int x = 0; x < s.getPoligons().size(); x++)
	//							utils.deleteNodes(s.getNodesIds(x));
	//						shapes.get(key).remove(s);
	//					}
	//
	//			//Escribir solo nodos (ya que solo estan ahi los datos que interesan)
	//			System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo nodos");
	//			catastro.printNodes( Cat2Osm.utils.getTotalNodes());
	//
	//		}
	//		else 
	//			System.out.println("["+new Timestamp(new Date().getTime())+"] Se han encontrado 3 archivos temporales de una posible ejecución interrumpida, se procederá a juntarlos en un archivo resultado.");
	//
	//		System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo el archivo resultado");
	//		catastro.juntarFiles(Config.get("ResultFileName") + "USOS");
	//		System.out.println("["+new Timestamp(new Date().getTime())+"] Terminado");
	//	}
	
	
		/** Metodo para utilizar solamente los arhivos de Ejes 
		 * @throws InterruptedException
		 * @throws IOException
		 */
		public static void crearEjes() throws IOException, InterruptedException{
	
			// Clases
			Cat2OsmUtils utils = new Cat2OsmUtils();
			Cat2Osm catastro = new Cat2Osm(utils);
			utils.setOnlyUsos(true);
	
			Pattern p = Pattern.compile("\\d{4}-\\d{1,2}");
			Matcher m = p.matcher(Config.get("UrbanoCATFile"));
	
			if (m.find()) {
				Cat2OsmUtils.setFechaArchivos(Long.parseLong(m.group().substring(0, 4)+"0101"));
			}
			else{
				System.out.println("["+new Timestamp(new Date().getTime())+"] El archivo Cat Urbano debe tener el formato de nombre que viene por defecto en Catastro (XX_XX_U_aaaa-mm-dd.CAT) para leer de él la fecha de creación.");
				System.exit(-1);
			}
	
			if (new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "EJES" +"tempRelations.osm").exists()
					&& new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "EJES" +"tempWays.osm").exists()
					&& new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "EJES" +"tempNodes.osm").exists()){

				System.out.println("["+new Timestamp(new Date().getTime())+"] Se han encontrado 3 archivos temporales de una posible ejecución interrumpida, se procederá a juntarlos en un archivo resultado.");
				catastro.juntarFiles("EJES", Config.get("ResultFileName"));
				System.out.println("["+new Timestamp(new Date().getTime())+"] ¡¡Terminada la exportación!!");

			}
			else {
	
	
				// Listas
				// Lista de shapes, agrupados por codigo de masa a la que pertenecen
				// Si es un tipo de shapes que no tienen codigo de masa se meteran en una cuyo
				// codigo sea el nombre del archivo shapefile
				HashMap <String, List<Shape>> shapes = new HashMap <String, List<Shape>>();
				List<ShapeParser> parsers = new ArrayList<ShapeParser>();
	
	
				// Recorrer los directorios Urbanos
				File dirU = new File (Config.get("UrbanoSHPPath"));
	
				// Cogemos solo el archivo de Ejes
				if( dirU.exists() && dirU.isDirectory()){
					File[] filesU = dirU.listFiles();
					for(int i=0; i < filesU.length; i++)
						if ( filesU[i].getName().toUpperCase().equals("EJES") )
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
						if ( filesR[i].getName().toUpperCase().equals("EJES"))
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
	
				// Operacion de simplifiacion de vias
				System.out.println("["+new Timestamp(new Date().getTime())+"] Simplificando vias.");
				catastro.simplificarWays("EJES", shapes.get("EJES"));
	
				// Operacion de simplifiacion de vias
				System.out.println("["+new Timestamp(new Date().getTime())+"] Uniendo calles con el mismo nombre.");
				catastro.unirCalles("EJES", shapes.get("EJES"));
	
				// Escribir los datos
				System.out.print("["+new Timestamp(new Date().getTime())+"] Escribiendo relations.\r");
					catastro.printRelations("EJES", shapes.get("EJES"));
					System.out.println("["+new Timestamp(new Date().getTime())+"] Escritas relations, Escribiendo ways.\r");
					catastro.printWays("EJES", shapes.get("EJES"));
				System.out.println("["+new Timestamp(new Date().getTime())+"] Escritas relations, Escritos ways, Escribiendo nodos.\r");
				catastro.printNodes("EJES", shapes.get("EJES"));			

				System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo el archivo resultado.");
				catastro.juntarFiles("EJES", Config.get("ResultFileName") + "EJES");
				System.out.println("["+new Timestamp(new Date().getTime())+"] Terminado");
	
			}
		}
	
	
	
	//	/** Metodo para utilizar solamente los numeros de policia de los elementos Elemtex y moverlos a la parcela
	//	 * mas cercana
	//	 * @throws InterruptedException
	//	 * @throws IOException
	//	 */
	//	public static void crearPortales() throws InterruptedException, IOException{
	//
	//		// Clases
	//		Cat2OsmUtils utils = new Cat2OsmUtils();
	//		Cat2Osm catastro = new Cat2Osm(utils);
	//		Cat2Osm.utils.setOnlyEntrances(true);
	//
	//		Pattern p = Pattern.compile("\\d{4}-\\d{1,2}");
	//		Matcher m = p.matcher(Config.get("UrbanoCATFile"));
	//
	//		if (m.find()) {
	//			Cat2OsmUtils.setFechaArchivos(Long.parseLong(m.group().substring(0, 4)+"0101"));
	//		}
	//
	//		else{
	//			System.out.println("["+new Timestamp(new Date().getTime())+"] El archivo Cat Urbano debe tener el formato de nombre que viene por defecto en Catastro (XX_XX_U_aaaa-mm-dd.CAT) para leer de él la fecha de creación.");
	//			System.exit(-1);
	//		}
	//
	//		if (!new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempRelations.osm").exists()
	//				&& !new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempWays.osm").exists()
	//				&& !new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempNodes.osm").exists()){
	//
	//			// Listas
	//			// Lista de shapes, agrupados por codigo de masa a la que pertenecen
	//			// Si es un tipo de shapes que no tienen codigo de masa se meteran en una cuyo
	//			// codigo sea el nombre del archivo shapefile
	//			ConcurrentHashMap <String, List<Shape>> shapes = new ConcurrentHashMap <String, List<Shape>>();
	//			List<ShapeParser> parsers = new ArrayList<ShapeParser>();
	//
	//			// Recorrer los directorios Urbanos
	//			File dirU = new File (Config.get("UrbanoSHPPath"));
	//
	//			// Cogemos los shapefile concretos para calcular los portales
	//			if( dirU.exists() && dirU.isDirectory()){
	//
	//				File[] filesU = dirU.listFiles();
	//
	//				for(int i=0; i < filesU.length; i++)
	//					if (   filesU[i].getName().toUpperCase().equals("EJES") ||
	//							filesU[i].getName().toUpperCase().equals("PARCELA")
	//							)
	//
	//						try{
	//							System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesU[i].getName() +" urbano.");
	//							parsers.add(new ShapeParser("UR", new File(filesU[i] + "/" + filesU[i].getName() + ".SHP"), utils, shapes));
	//						}
	//				catch(Exception e)
	//				{
	//					System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer alguno de los shapefiles urbanos. " + e.getMessage());}
	//			}
	//
	//			else
	//				System.out.println("["+new Timestamp(new Date().getTime())+"] El directorio de shapefiles urbanos "+Config.get("UrbanoSHPPath")+" no existe.");
	//
	//			// Recorrer los directorios Rusticos
	//			File dirR = new File (Config.get("RusticoSHPPath"));
	//
	//			if( dirR.exists() && dirR.isDirectory()){
	//				File[] filesR = dirR.listFiles();
	//				for(int i=0; i < filesR.length; i++)
	//					if (   filesR[i].getName().toUpperCase().equals("ELEMTEX") ||
	//							filesR[i].getName().toUpperCase().equals("PARCELA")
	//							)
	//						try{
	//							System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesR[i].getName() +" rústico.");
	//							parsers.add(new ShapeParser("RU", new File(filesR[i] + "/" + filesR[i].getName() + ".SHP"), utils, shapes));
	//						}
	//
	//				catch(Exception e)
	//
	//				{
	//					System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer alguno de los archivos shapefiles rústicos. " + e.getMessage());}
	//			}
	//			else
	//				System.out.println("["+new Timestamp(new Date().getTime())+"] El directorio de shapefiles rústicos "+Config.get("RusticoSHPPath")+" no existe.");
	//
	//			for (ShapeParser sp : parsers)
	//				sp.join();
	//
	//			// Mover las entradas de las casas a sus respectivas parcelas
	//			System.out.println("["+new Timestamp(new Date().getTime())+"] Moviendo puntos de entrada a sus parcelas mas cercanas.");
	//			shapes = catastro.calcularEntradas(shapes);
	//
	//			// Borramos todos los nodos shapes de parcelas para que no los dibuje
	//			for(String key : shapes.keySet())
	//				for (Shape s : shapes.get(key))
	//					if(s instanceof ShapeParcela){
	//						for(int x = 0; x < s.getPoligons().size(); x++){
	//							utils.deleteNodes(s.getNodesIds(x));
	//						}
	//						shapes.get(key).remove(s);
	//					}
	//
	//
	//			//Escribir solo nodos (ya que solo estan ahi los datos que interesan)
	//			System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo nodos");
	//			catastro.printNodes( Cat2Osm.utils.getTotalNodes());
	//		}
	//		else 
	//			System.out.println("["+new Timestamp(new Date().getTime())+"] Se han encontrado 3 archivos temporales de una posible ejecución interrumpida, se procederá a juntarlos en un archivo resultado.");
	//
	//		System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo el archivo resultado");
	//		catastro.juntarFiles(Config.get("ResultFileName")+"PORTALES");
	//		System.out.println("["+new Timestamp(new Date().getTime())+"] Terminado");
	//	}

}
