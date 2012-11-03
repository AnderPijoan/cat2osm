import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;


public class Main {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {

		if ((args.length == 1 && args[0].replaceAll("-", "").equals("v")) || (args.length == 2 && (args[1].replaceAll("-", "").equals("v") || args[0].replaceAll("-", "").equals("v") ))){
			System.out.println("Cat2Osm versión "+Cat2Osm.VERSION+".");
		}
		else if ((args.length == 1 && args[0].replaceAll("-", "").equals("ui")) || (args.length == 2 && (args[1].replaceAll("-", "").equals("ui") || args[0].replaceAll("-", "").equals("ui") ))){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando interfaz visual para crear el archivo de configuración.");
			// Iniciar el interfaz visual
			new Gui();
		}
		else if (args.length ==1 && new File(args[0]).exists()){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando Cat2Osm con el archivo de configuración " + args[0] + ".");
			// Ruta al fichero de configuracion por parametro
			Config.loadConfig(args[0]);
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
			Config.loadConfig(args[0]);
			// Iniciar cat2osm
			ejecutarCat2Osm(args[1].replaceAll("-", "").toUpperCase());
		}
		else if (args.length == 2 && args[1].replaceAll("-", "").equals("usos") && new File(args[0]).exists()){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando Cat2Osm con el archivo de configuración para exportar únicamente el archivo de destinos a corregir.");
			// Ruta al fichero de configuracion por parametro
			Config.loadConfig(args[0]);
			// Iniciar metodo de creacion de puntos de entrada
			crearUsos();
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
			System.out.println("["+new Timestamp(new Date().getTime())+"] Creando el directorio general de resultados ("+Config.get("ResultPath")+").");
			try                { dir.mkdirs(); }
			catch (Exception e){ e.printStackTrace(); }
		}

		// Nos aseguramos de que existe la carpeta result/nombreresult
		File dir2 = new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName"));
		if (!dir2.exists()) 
		{
			System.out.println("["+new Timestamp(new Date().getTime())+"] Creando el directorio donde almacenar este resultado concreto ("+Config.get("ResultPath")+ "/" + Config.get("ResultFileName")+").");
			try                { dir2.mkdirs(); }
			catch (Exception e){ e.printStackTrace(); }
		}

		
		// Archivo global de resultado
		// Borrar archivo con el mismo nombre si existe, porque sino concatenaria el nuevo
		new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/" + Config.get("ResultFileName") + ".osm").delete();
		new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/" + Config.get("ResultFileName") + ".osm.gz").delete();
		
		// Archivo al que se le concatenan todos los archivos de nodos, ways y relations
		String fstreamOsm = Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "/" + Config.get("ResultFileName") + ".osm.gz";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outOsmGlobal = new BufferedWriter( new OutputStreamWriter (new GZIPOutputStream(new FileOutputStream(fstreamOsm)), "UTF-8"));

		// Cabecera del archivo
		outOsmGlobal.write("<?xml version='1.0' encoding='UTF-8'?>");outOsmGlobal.newLine();
		outOsmGlobal.write("<osm version=\"0.6\" generator=\"cat2osm-"+Cat2Osm.VERSION+"\">");outOsmGlobal.newLine();
		

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
						filesU[i].getName().toUpperCase().equals("EJES") 	||
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
				System.out.println("["+new Timestamp(new Date().getTime())+"]    Fallo al leer alguno de los shapefiles urbanos. " + e.getMessage());}
		}

		else
			System.out.println("["+new Timestamp(new Date().getTime())+"]    El directorio de shapefiles urbanos "+Config.get("UrbanoSHPPath")+" no existe.");

		// Recorrer los directorios Rusticos
		File dirR = new File (Config.get("RusticoSHPPath"));

		if( dirR.exists() && dirR.isDirectory()){
			File[] filesR = dirR.listFiles();
			for(int i=0; i < filesR.length; i++)
				if ( (archivo.equals("*") && (
						filesR[i].getName().toUpperCase().equals("CONSTRU") ||
						filesR[i].getName().toUpperCase().equals("EJES") 	||
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
				System.out.println("["+new Timestamp(new Date().getTime())+"]    Fallo al leer alguno de los archivos shapefiles rústicos. " + e.getMessage());}
		}
		else
			System.out.println("["+new Timestamp(new Date().getTime())+"]    El directorio de shapefiles rústicos "+Config.get("RusticoSHPPath")+" no existe.");


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
			{System.out.println("["+new Timestamp(new Date().getTime())+"]    Fallo al leer archivo Cat urbano. " + e.getMessage());}

			try {
				System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat rústico.");
				catastro.catParser("RU", new File(Config.get("RusticoCATFile")), shapes);
			}catch(Exception e)
			{System.out.println("["+new Timestamp(new Date().getTime())+"]    Fallo al leer archivo Cat rústico. " + e.getMessage());}	
		}

		System.out.println("["+new Timestamp(new Date().getTime())+"] Leídos "+utils.getTotalNodes().size()+" códigos para nodos, " +
				utils.getTotalWays().size()+" códigos para ways y "+ utils.getTotalRelations().size()+" códigos para relations.");

		// Mover las entradas de las casas a sus respectivas parcelas
		if (archivo.equals("*") && Config.get("MovePortales").equals("1")){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Moviendo puntos de entrada a sus parcelas mas cercanas.");
			HashMap <String, List<Shape>> shapesTemp = catastro.calcularEntradas(shapes);
			if (shapesTemp != null)
			shapes = shapesTemp;
		}

		int pos = 0;
		for (String key : utils.getTotalNodes().keySet()){

			String folder = key.startsWith("ELEM")? "elementos" : ( key.startsWith("EJES")? "ejes" : "masas" );

			System.out.println("["+new Timestamp(new Date().getTime())+"] Exportando " + Config.get("ResultFileName") + "-" + key + "[" + pos++ +"/" + utils.getTotalNodes().keySet().size() + "]");

			// Por si acaso si hubiera archivos de un fallo en ejecucion anterior
			if (new File(Config.get("ResultPath") + "/" + folder + "/" + Config.get("ResultFileName") + key +"tempRelations.osm").exists()
					&& new File(Config.get("ResultPath") + "/" + folder + "/" + Config.get("ResultFileName") + key +"tempWays.osm").exists()
					&& new File(Config.get("ResultPath") + "/" + folder + "/" + Config.get("ResultFileName") + key +"tempNodes.osm").exists()){

				System.out.println("["+new Timestamp(new Date().getTime())+"] Se han encontrado 3 archivos temporales de una posible ejecución interrumpida, se procederá a juntarlos en un archivo resultado.");
				catastro.juntarFilesTemporales(key, folder, Config.get("ResultFileName"), outOsmGlobal);
				System.out.println("["+new Timestamp(new Date().getTime())+"] ¡¡Terminada la exportación de " + Config.get("ResultFileName") + "!!");

			}
			else if (shapes.get(key) != null){

				// Calcular los usos / destinos de las parcelas en funcion del que mas area tiene
				if (!key.startsWith("EJES") && !key.startsWith("ELEM") ){
					System.out.println("["+new Timestamp(new Date().getTime())+"]    Calculando usos de las parcelas.");
					catastro.calcularUsos(key, shapes.get(key));
				}


				// Operacion de simplificacion de relaciones sin tags relevantes
				if (archivo.equals("*")){
					System.out.println("["+new Timestamp(new Date().getTime())+"]    Simplificando Relaciones sin tags relevantes.");
					catastro.simplificarRelationsSinTags(key, shapes.get(key));
				}


				// Operacion de simplifiacion de vias
				if (!key.startsWith("ELEMPUN") && !key.startsWith("ELEMTEX") ){
					System.out.println("["+new Timestamp(new Date().getTime())+"]    Simplificando vias.");
					catastro.simplificarWays(key, shapes.get(key));
				}


				// Si son ELEMLIN o EJES, juntar todos los ways que compartan un node
				// aunque sean de distintos shapes
				if (key.startsWith("EJES") || key.startsWith("ELEMLIN") ){
					System.out.println("["+new Timestamp(new Date().getTime())+"]    Uniendo shapes.");
					catastro.unirShapes(key, shapes.get(key));
				}
				
				// Escribir los datos en los archivos temporales
				System.out.print("["+new Timestamp(new Date().getTime())+"]    Escribiendo archivos temporales.\r");
				catastro.printResults(key, folder, shapes.get(key));

				System.out.print("["+new Timestamp(new Date().getTime())+"]    Escribiendo el archivo resultado.\r");
				catastro.juntarFilesTemporales(key, folder, Config.get("ResultFileName") + "-" + key, outOsmGlobal);
				System.out.println("["+new Timestamp(new Date().getTime())+"]    Terminado " + Config.get("ResultFileName") + "-" + key + "[" + pos++ +"/" + utils.getTotalNodes().keySet().size() + "]\r");

			}
		}

		// Terminamos el archivo global de resultado
		outOsmGlobal.write("</osm>");outOsmGlobal.newLine();
		outOsmGlobal.close();

		System.out.println("["+new Timestamp(new Date().getTime())+"] ¡¡Terminada la exportación de " + Config.get("ResultFileName") + "!!");

	}


	/** Metodo para utilizar solamente los archivos de parcelas para crear sobre ellas nodos con todos sus usos y destinos
	 * leidos de los registros del .CAT 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static void crearUsos() throws IOException, InterruptedException{

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

		// Listas
		// Lista de shapes, agrupados por codigo de masa a la que pertenecen
		// Si es un tipo de shapes que no tienen codigo de masa se meteran en una cuyo
		// codigo sea el nombre del archivo shapefile
		HashMap <String, List<Shape>> shapes = new HashMap <String, List<Shape>>();
		List<ShapeParser> parsers = new ArrayList<ShapeParser>();


		// Recorrer los directorios Urbanos
		File dirU = new File (Config.get("UrbanoSHPPath"));

		// Leemos solo los archivos de parcela
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
				System.out.println("["+new Timestamp(new Date().getTime())+"]    Fallo al leer alguno de los shapefiles urbanos. " + e.getMessage());}
		}

		else
			System.out.println("["+new Timestamp(new Date().getTime())+"]    El directorio de shapefiles urbanos "+Config.get("UrbanoSHPPath")+" no existe.");

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
				System.out.println("["+new Timestamp(new Date().getTime())+"]    Fallo al leer alguno de los archivos shapefiles rústicos. " + e.getMessage());}
		}
		else
			System.out.println("["+new Timestamp(new Date().getTime())+"]    El directorio de shapefiles rústicos "+Config.get("RusticoSHPPath")+" no existe.");


		for (ShapeParser sp : parsers)
			sp.join();

		// Leemos archivo .cat
		// No todos los shapefiles tienen referencia catastral por lo que algunos
		// no hay forma de relacionarlos con los registros de catastro.
		try {
			System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat urbano.");
			catastro.catUsosParser("UR", new File(Config.get("UrbanoCATFile")), shapes);
		}catch(Exception e)
		{System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer archivo Cat urbano. " + e.getMessage());}

		try {
			System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat rústico.");
			catastro.catUsosParser("RU", new File(Config.get("RusticoCATFile")), shapes);
		}catch(Exception e)
		{System.out.println("["+new Timestamp(new Date().getTime())+"]    Fallo al leer archivo Cat rústico. " + e.getMessage());}	


		// Solo hay que sacar los nodos con key="USOS"
		// (ya que solo estan ahi los datos que interesan)
		System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo nodos");

		File dir = new File(Config.get("ResultFileName") + "USOS");
		if (!dir.exists()) 
		{
			try                { dir.mkdirs(); }
			catch (Exception e){ e.printStackTrace(); }
		}

		// Archivo temporal para escribir los nodos
		String fstreamNodes = Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "USOS.osm";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outNodes = new BufferedWriter(new OutputStreamWriter (new FileOutputStream(fstreamNodes), "UTF-8"));

		outNodes.write("<?xml version='1.0' encoding='UTF-8'?>");outNodes.newLine();
		outNodes.write("<osm version=\"0.6\" generator=\"cat2osm-"+Cat2Osm.VERSION+"\">");outNodes.newLine();	


		for(NodeOsm node : utils.getTotalNodes().get("USOS").keySet())
			outNodes.write(node.printNode(utils.getTotalNodes().get("USOS").get(node)));

		outNodes.write("</osm>");
		outNodes.newLine();
		outNodes.close();
		System.out.println("["+new Timestamp(new Date().getTime())+"] ¡¡Terminada la exportación de usos de " + Config.get("ResultFileName") + "!!");

	}

}
