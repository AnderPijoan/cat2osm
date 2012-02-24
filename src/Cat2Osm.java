import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


public class Cat2Osm {

	static Cat2OsmUtils utils;

	/** Constructor
	 * @param utils Clase utils en la que se almacenan los nodos, ways y relaciones 
	 * y tiene funciones para manejarlos
	 */
	public Cat2Osm (Cat2OsmUtils utils){
		Cat2Osm.utils = utils;
	}


	/** Busca en la lista de shapes los que coincidan con la ref catastral
	 * @param ref referencia catastral a buscar
	 * @returns List<Shape> lista de shapes que coinciden                    
	 */
	private static List<Shape> buscarRefCat(List<Shape> shapes, String ref){
		List<Shape> shapeList = new ArrayList<Shape>();

		for(Shape shape : shapes) 
			if (shape.getRefCat() != null && shape.getRefCat().equals(ref)) shapeList.add(shape);

		return shapeList;
	}


	/** Busca en la lista de shapes los que coincidan con el codigo de subparce
	 * @param subparce codigo de subparcela a buscar
	 * @returns List<Shape> lista de shapes que coinciden                    
	 */
	private static List<Shape> buscarSubparce(List<Shape> shapes, String subparce){
		List<Shape> shapeList = new ArrayList<Shape>();

		for(Shape shape : shapes) 
			if (shape instanceof ShapeSubparce)
				if (((ShapeSubparce) shape).getSubparce().equals(subparce))
					shapeList.add(shape);

		return shapeList;
	}


	/** Los elementos textuales traen informacion sobre que hay en alguna construccion como pueden ser cementerios,
	 * hospitales, etc. Las opciones en las construcciones son limitadas y puede hacer que cambie su landuse. 
	 * Por eso se calcula si un elemtex se encuentra sobre una constru y se anade el landuse al constru.
	 * @param shapes Lista de shapes original
	 * @return lista de shapes con los tags modificados
	 */
	public List<Shape> pasarElemtexLanduseAConstru(List<Shape> shapes){

		GeometryFactory factory = new GeometryFactory();

		for (Shape shape: shapes){

			// Si le hemos modificado el ttggss para que ahora cambie alguno de los tags
			if (shape instanceof ShapeElemtex && shape.getTtggss().contains("=")){

				for (Shape s: shapes)

					if (s instanceof ShapeConstru && s.getPoligons() != null){
						LinearRing l = factory.createLinearRing(s.getPoligons().get(0).getCoordinates());
						Polygon parcela = factory.createPolygon(l, null);
						Point coor = factory.createPoint(shape.getCoor());

						// Si cumple, cambiamos el tag de la relacion
						if (coor.intersects(parcela)){
							RelationOsm r = ((RelationOsm) utils.getKeyFromValue((Map<Object, Long>) ((Object) utils.getTotalRelations()), s.getRelationId()));

							// Los tags vienen como "key=value,key=value" almacenados
							// en el ttggss
							String[] pares = shape.getTtggss().split(",");
							List<String[]> tags = new ArrayList<String[]>();

							for (String par : pares)
								tags.add(par.split("="));

							r.addTags(tags);
						}
					}

				shape.getCoor();

			}

		}

		return shapes;
	}
	
	
	/** Los ways inicialmente estan divididos lo maximo posible, es decir un way por cada
	 * dos nodes. Este metodo compara los tags de los ways para saber que ways se pueden
	 * unir para formar uno unico nuevo. Los tags de los ways se insertan al crear el way y
	 * si un way es compartido por otra geometria se le anaden los de la otra tambien. De esta forma
	 * los ways que se pueden "concatenar" tendran los mismos tags. Al borrar un way, hay que borrarlo
	 * de todos los shapes y relaciones que lo usaban. Este metodo borra de los shapes
	 * y el del utils borra de las relaciones. Una vez hecho esto, los tags
	 * de los ways son prescindibles.
	 * @param shapes Lista de shapes con los ways divididos por cada 2 nodes.
	 * @return Lista de shapes con la simplificacion hecha.
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("unchecked")
	public List<Shape> simplificarWays(List<Shape> shapes) throws InterruptedException{

		WayOsm way1 = null;
		WayOsm way2 = null;
		WayOsm removeWay = null;
		Map<WayOsm, Long> ways = utils.getTotalWays();

		for (Shape shape : shapes)

			for (int x = 0; shape.getPoligons() != null && !shape.getPoligons().isEmpty() && x < shape.getPoligons().size(); x++)

				for(int y = 0; shape.getWaysIds(x) != null && !shape.getWaysIds(x).isEmpty() && y < shape.getWaysIds(x).size()+1; y++){

					if (removeWay != null){
						removeWay = null;
						y = -1;
					}

					try{

						// Formula para que compruebe tambien el way(0) con el ultimo way o way(size()) 
						way1 = ((WayOsm) 
								utils.getKeyFromValue(
										(Map<Object, Long>) (
												(Object)ways), 
												shape.getWaysIds(x).get((y+shape.getWaysIds(x).size())%shape.getWaysIds(x).size())));
						way2 = ((WayOsm) 
								utils.getKeyFromValue(
										(Map<Object, Long>) (
												(Object)ways), 
												shape.getWaysIds(x).get((y+1+shape.getWaysIds(x).size())%shape.getWaysIds(x).size())));

						if (way1 != null && way2 != null && !way1.getNodes().equals(way2.getNodes()) && way1.sameShapes(way2.getShapes()) ){

							// Juntamos los ways y borra el way que no se va a usar de las relations
							removeWay = utils.unirWays(way1, way2);

							if (removeWay != null){

								// Eliminamos de la lista total de ways el way a eliminar
								long wayId = utils.getTotalWays().get(removeWay);
								utils.getTotalWays().remove(removeWay);

								// Borramos el way que no se va a usar de los shapes
								for (int shapeIds = 0; shapeIds < removeWay.getShapes().size(); shapeIds++){

									String shapeId = removeWay.getShapes().get(shapeIds);

									for (Shape s : shapes)
										if (s.getShapeId() == shapeId)
											for (int pos = 0; pos < s.getPoligons().size(); pos++)
												s.deleteWay(pos,wayId);
								}
							}
						}

					}catch(Exception e) {System.out.println("["+new Timestamp(new Date().getTime())+"] Error simplificando vía. " + e.getMessage());}
				}

		return shapes;
	}


	/** Escribe el osm con todos los nodos (Del archivo totalNodes, sin orden)
	 * @param tF Ruta donde escribir este archivo, sera temporal
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public void printNodes(Map <NodeOsm, Long> nodes) throws IOException{

	    File dir = new File(Config.get("ResultPath")); // TODO FIXME poner esto en donde debe
	    if (!dir.exists()) 
	    {
	      try                { dir.mkdirs(); }
	      catch (Exception e){ e.printStackTrace(); }
	    }
		
		// Archivo temporal para escribir los nodos
		FileWriter fstreamNodes = new FileWriter(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempNodes.osm");
		BufferedWriter outNodes = new BufferedWriter(fstreamNodes);

		Iterator<Entry<NodeOsm, Long>> it = nodes.entrySet().iterator();

		// Escribimos todos los nodos
		while(it.hasNext()){
			Map.Entry e = (Map.Entry)it.next();
			outNodes.write(((NodeOsm) e.getKey()).printNode((Long) e.getValue()));
		}
		outNodes.close();
	}


	/** Escribe el osm con unicamente los nodos de los shapes que le pasamos (MUY LENTO)
	 * @param tF Ruta donde escribir este archivo, sera temporal
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void printNodesOrdenShapes(List<Shape> shapes, Map <NodeOsm, Long> nodes) throws IOException{

	    File dir = new File(Config.get("ResultPath")); // TODO FIXME poner esto en donde debe
	    if (!dir.exists()) 
	    {
	      try                { dir.mkdirs(); }
	      catch (Exception e){ e.printStackTrace(); }
	    }
	    
		// Archivo temporal para escribir los nodos
		FileWriter fstreamNodes = new FileWriter(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempNodes.osm");
		BufferedWriter outNodes = new BufferedWriter(fstreamNodes);
		// Escribimos todos los nodos
		for(Shape shape : shapes){

			for (int x = 0; x < shape.getPoligons().size(); x++)

				for (int y = 0; y < shape.getNodesIds(x).size(); y++)
					outNodes.write( ((NodeOsm) utils.getKeyFromValue((Map<Object, Long>) ((Object)nodes), shape.getNodesIds(x).get(y))).printNode((Long) shape.getNodesIds(x).get(y)));
		}
		outNodes.close();
	}


	/** Escribe el osm con todos los ways (Del archivo waysTotales, sin orden)
	 * @param tF Ruta donde escribir este archivo, sera temporal
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public void printWays(Map <WayOsm, Long> ways) throws IOException{

	    File dir = new File(Config.get("ResultPath")); // TODO FIXME poner esto en donde debe
	    if (!dir.exists()) 
	    {
	      try                { dir.mkdirs(); }
	      catch (Exception e){ e.printStackTrace(); }
	    }

		// Archivo temporal para escribir los ways
		FileWriter fstreamWays = new FileWriter(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempWays.osm");
		BufferedWriter outWays = new BufferedWriter(fstreamWays);

		Iterator<Entry<WayOsm, Long>> it = ways.entrySet().iterator();

		// Escribimos todos los ways y sus referencias a los nodos en el archivo
		while(it.hasNext()){
			Map.Entry e = (Map.Entry)it.next();
			outWays.write(((WayOsm) e.getKey()).printWay((Long) e.getValue()));
		}
		outWays.close();
	}


	/** Escribe el osm con unicamente los ways de los shapes que le pasamos (MUY LENTO)
	 * @param tF Ruta donde escribir este archivo, sera temporal
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void printWaysOrdenShapes( List<Shape> shapes, Map <WayOsm, Long> ways) throws IOException{
		
	    File dir = new File(Config.get("ResultPath")); // TODO FIXME poner esto en donde debe
	    if (!dir.exists()) 
	    {
	      try                { dir.mkdirs(); }
	      catch (Exception e){ e.printStackTrace(); }
	    }

		// Archivo temporal para escribir los ways
		FileWriter fstreamWays = new FileWriter(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempWays.osm");
		BufferedWriter outWays = new BufferedWriter(fstreamWays);

		// Escribimos todos los ways y sus referencias a los nodos en el archivo
		for(Shape shape : shapes){

			for (int x = 0; x < shape.getPoligons().size(); x++)
				for (int y = 0; y < shape.getWaysIds(x).size(); y++)
					outWays.write( ((WayOsm) utils.getKeyFromValue((Map<Object, Long>) ((Object)ways), shape.getWaysIds(x).get(y))).printWay((Long) shape.getWaysIds(x).get(y)));
		}
		outWays.close();
	}


	/** Escribe el osm con todas las relations. 
	 * Este si que hay que hacerlo desde los shapes para saber los poligonos inner y outer
	 * @param tF Ruta donde escribir este archivo, sera temporal
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public void printRelations( Map <RelationOsm, Long> relations) throws IOException{
		
	    File dir = new File(Config.get("ResultPath")); // TODO FIXME poner esto en donde debe
	    if (!dir.exists()) 
	    {
	      try                { dir.mkdirs(); }
	      catch (Exception e){ e.printStackTrace(); }
	    }

		// Archivo temporal para escribir los ways
		FileWriter fstreamRelations = new FileWriter(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempRelations.osm");
		BufferedWriter outRelations = new BufferedWriter(fstreamRelations);

		Iterator<Entry<RelationOsm, Long>> it = relations.entrySet().iterator();

		// Escribimos todos las relaciones y sus referencias a los ways en el archivo
		while(it.hasNext()){
			Map.Entry e = (Map.Entry) it.next();
			outRelations.write(((RelationOsm) e.getKey()).printRelation((Long) e.getValue(), utils));
		}
		outRelations.close();
	}


	/** Concatena los 3 archivos, Nodos + Ways + Relations y lo deja en el OutOsm.
	 * @param oF Ruta donde esta el archivo final
	 * @param tF Ruta donde estan los archivos temporadles (nodos, ways y relations)
	 * @throws IOException
	 */
	public void juntarFiles(String filename) throws IOException{

		String path = Config.get("ResultPath");

		// Borrar archivo con el mismo nombre si existe, porque sino concatenaria el nuevo
		new File(path + "/"+ filename +".osm").delete();

		// Archivo al que se le concatenan los nodos, ways y relations
		String fstreamOsm = path + "/" + filename + ".osm";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outOsm = new BufferedWriter( new OutputStreamWriter (new FileOutputStream(fstreamOsm), "UTF-8"));

		// Juntamos los archivos en uno, al de los nodos le concatenamos el de ways y el de relations
		// Cabecera del archivo Osm
		outOsm.write("<?xml version='1.0' encoding='UTF-8'?>");outOsm.newLine();
		outOsm.write("<osm version=\"0.6\" generator=\"cat2osm\">");outOsm.newLine();	

		// Concatenamos todos los archivos
		String str;
		BufferedReader inNodes = new BufferedReader(new FileReader(path + "/"+ Config.get("ResultFileName") + "tempNodes.osm"));
		while ((str = inNodes.readLine()) != null){
			outOsm.write(str);
			outOsm.newLine();
		}
		BufferedReader inWays = new BufferedReader(new FileReader(path + "/"+ Config.get("ResultFileName") + "tempWays.osm"));
		while ((str = inWays.readLine()) != null){
			outOsm.write(str);
			outOsm.newLine();
		}
		BufferedReader inRelations = new BufferedReader(new FileReader(path + "/"+ Config.get("ResultFileName") + "tempRelations.osm"));
		while ((str = inRelations.readLine()) != null){
			outOsm.write(str);
			outOsm.newLine();
		}
		outOsm.write("</osm>");
		outOsm.newLine();

		outOsm.close();
		inNodes.close();
		inWays.close();
		inRelations.close();

		boolean borrado = true;
		borrado = borrado && (new File(path+ "/" + Config.get("ResultFileName") + "tempNodes.osm")).delete();
		borrado = borrado && (new File(path + "/" + Config.get("ResultFileName") + "tempWays.osm")).delete();
		borrado = borrado && (new File(path + "/" + Config.get("ResultFileName") + "tempRelations.osm")).delete();

		if (!borrado)
			System.out.println("["+new Timestamp(new Date().getTime())+"] NO se pudo borrar alguno de los archivos temporales." +
					" Estos estaran en la carpeta "+ path +".");

	}


	/** Lee linea a linea el archivo cat, coge los shapes q coincidan 
	 * con esa referencia catastral y los pasa a formato osm
	 * @param car Archivo cat del que lee linea a linea
	 * @param List<Shape> Lista de los elementos shp parseados ya en memoria
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void catParser(File cat, List<Shape> shapesTotales) throws IOException{

		BufferedReader bufRdr  = new BufferedReader(new FileReader(cat));
		String line = null; // Para cada linea leida del archivo .cat

		long fechaDesde = Long.parseLong(Config.get("FechaDesde"));
		long fechaHasta = Long.parseLong(Config.get("FechaHasta"));
		int tipoRegistro = Integer.parseInt(Config.get("TipoRegistro"));

		// Lectura del archivo .cat
		while((line = bufRdr.readLine()) != null)
		{
			try {
				Cat c = catLineParser(line);

				if ( c.getFechaAlta() >= fechaDesde && c.getFechaAlta() < fechaHasta && c.getFechaBaja() >= fechaHasta && (c.getTipoRegistro() == tipoRegistro || tipoRegistro == 0)){

					// Obtenemos los shape que coinciden con la referencia catastral de la linea leida
					List <Shape> matches = buscarRefCat(shapesTotales, c.getRefCatastral());

					// Para los tipos de registro de subparcelas, buscamos la subparcela concreta para
					// anadirle los atributos
					if (c.getTipoRegistro() == 17)
						matches = buscarSubparce(matches, c.getSubparce());

					// Puede que no haya shapes para esa refCatastral
					if (!matches.isEmpty()){

						for (Shape shape : matches)
							if (shape != (null)){
								RelationOsm r = ((RelationOsm) utils.getKeyFromValue((Map<Object, Long>) ((Object)utils.getTotalRelations()), shape.getRelationId()));
								r.addTags(c.getAttributes());
							}
					}
				}
			}
			catch(Exception e){System.out.println("["+new Timestamp(new Date().getTime())+"] Error leyendo linea del archivo. " + e.getMessage());}
		}
	}


	/** Parsea el archivo .cat y crea los elementos en memoria en un List
	 * @param f Archivo a parsear
	 * @returns List<Cat> Lista de los elementos parseados
	 * @throws IOException 
	 * @see http://www.catastro.meh.es/pdf/formatos_intercambio/catastro_fin_cat_2006.pdf
	 */
	public List<Cat> catParser(File f) throws IOException{

		BufferedReader bufRdr  = new BufferedReader(new FileReader(f));
		String line = null;

		List<Cat> l = new ArrayList<Cat>();

		long fechaDesde = Long.parseLong(Config.get("FechaDesde"));
		long fechaHasta = Long.parseLong(Config.get("FechaHasta"));
		int tipoRegistro = Integer.parseInt(Config.get("tipoRegistro"));

		while((line = bufRdr.readLine()) != null)
		{
			Cat c = catLineParser(line);
			// No todos los tipos de registros de catastro tienen FechaAlta y FechaBaja
			// Los que no tienen, pasan el filtro
			if ((c.getTipoRegistro() == tipoRegistro || tipoRegistro == 0) && c.getFechaAlta() >= fechaDesde && c.getFechaAlta() <= fechaHasta && c.getFechaBaja() >= fechaHasta)
				l.add(c);
		}
		bufRdr.close();
		return l;
	}


	/** De la lista de shapes con la misma referencia catastral (deduzco que es la
	 * misma parcela pero puede haber cambiado algo por obras) devuelve la mas actual
	 * del periodo indicado.
	 * @param shapes Lista de shapes
	 * @returns El shape que hay para el periodo indicado porque hay shapes que 
	 * tienen distintas versiones a lo largo de los anos
	 */
	public static Shape buscarShapesParaFecha(List<Shape> shapes){

		// Lo habitual es que venga ordenado de mas reciente a mas antiguo
		Shape s = shapes.get(shapes.size()-1);
		long fA = 00000101;
		long fechaHasta = Long.parseLong(Config.get("FechaHasta"));

		for (int x = shapes.size()-1 ; x >= 0 ; x--){

			if (shapes.get(x).getFechaAlta() >= fA && shapes.get(x).getFechaAlta() < fechaHasta && shapes.get(x).getFechaBaja() >= fechaHasta){
				s = shapes.get(x);
				fA = s.getFechaAlta();
			}
		}
		return s;
	}


	/** Parsea la linea del archivo .cat y devuelve un elemento Cat
	 * @param line Linea del archivo .cat
	 * @returns Cat Elemento Cat con todos los campos leidos en la linea
	 * @throws IOException 
	 * @see http://www.catastro.meh.es/pdf/formatos_intercambio/catastro_fin_cat_2006.pdf
	 */
	private static Cat catLineParser(String line) throws IOException{

		long fechaDesde = Long.parseLong(Config.get("FechaDesde"));
		long fechaHasta = Long.parseLong(Config.get("FechaHasta"));
		Cat c = new Cat(Integer.parseInt(line.substring(0,2)));

		switch(c.getTipoRegistro()){ // Formato de los tipos distintos de registro .CAT
		case 01: {

			/*System.out.println("\nTIPO DE REGISTRO 01: REGISTRO DE CABECERA\n");
			System.out.println("TIPO DE ENTIDAD GENERADORA                  :"+line.substring(2,3));
			System.out.println("CODIGO DE LA ENTIDAD GENERADORA             :"+line.substring(3,12));
			System.out.println("NOMBRE DE LA ENTIDAD GENERADORA             :"+line.substring(12,39));
			System.out.println("FECHA DE GENERACION (AAAAMMDD)              :"+line.substring(39,47)); 
			System.out.println("HORA DE GENERACION (HHMMSS)                 :"+line.substring(47,53));
			System.out.println("TIPO DE FICHERO                             :"+line.substring(53,57));
			System.out.println("DESCRIPCION DEL CONTENIDO DEL FICHERO       :"+line.substring(57,96));
			System.out.println("NOMBRE DE FICHERO                           :"+line.substring(96,117));
			System.out.println("CODIGO DE LA ENTIDAD DESTINATARIA           :"+line.substring(117,120));
			System.out.println("FECHA DE INICIO DEL PERIODO (AAAAMMDD)      :"+line.substring(120,128));
			System.out.println("FECHA DE FIN DEL PERIODO (AAAAMMDD)         :"+line.substring(128,136));
			 */
			break;}
		case 11: {

			// Este tipo no tiene fechaAlta ni fechaBaja
			// Deben salir todos siempre porque son los datos de las fincas a las que luego
			// se hace referencia en los demas tipos de registro.
			c.setFechaAlta(fechaDesde); 
			c.setFechaBaja(fechaHasta);

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2)); 
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(25,28)));
			//c.addAttribute("BLANCO EXCEPTO INMUEBLES ESPECIALES",line.substring(28,30));
			c.addAttribute("catastro:special",eliminarComillas(line.substring(28,30)));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44)); 
			//c.addAttribute("CODIGO DE PROVINCIA",line.substring(50,52));
			//c.addAttribute("catastro:ref:province",eliminarCerosString(line.substring(50,52)));
			//c.addAttribute("NOMBRE DE PROVINCIA",line.substring(52,77));
			//c.addAttribute("is_in:province",line.substring(52,77));
			//c.addAttribute("CODIGO DE MUNICIPIO",line.substring(77,80));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(77,80)));
			//c.addAttribute("CODIGO DE MUNICIPIO (INE). EXCLUIDO ULTIMO DIGITO DE CONTROL:",line.substring(80,83));
			//c.addAttribute("ine:ref:municipality",eliminarCerosString(line.substring(80,83)));
			//c.addAttribute("NOMBRE DE MUNICIPIO",line.substring(83,123));
			//c.addAttribute("is_in:municipality",eliminarComillas(line.substring(83,123)));
			//c.addAttribute("NOMBRE DE LA ENTIDAD MENOR EN CASO DE EXISTIR",line.substring(123,153));
			//c.addAttribute("CODIGO DE VIA PUBLICA",line.substring(153,158));
			//c.addAttribute("catastro:ref:way",eliminarCerosString(line.substring(153,158)));
			//c.addAttribute("TIPO DE VIA O SIGLA PUBLICA",line.substring(158,163));
			//c.addAttribute("NOMBRE DE VIA PUBLICA",line.substring(163,188));
			c.addAttribute("addr:street",nombreTipoViaParser(line.substring(158,163).trim())+" "+formatearNombreCalle(eliminarComillas(line.substring(163,188).trim())));
			//c.addAttribute("PRIMER NUMERO DE POLICIA",line.substring(188,192));
			c.addAttribute("addr:housenumber",eliminarCerosString(line.substring(188,192)));
			//c.addAttribute("PRIMERA LETRA (CARACTER DE DUPLICADO)",line.substring(192,193));
			//c.addAttribute("SEGUNDO NUMERO DE POLICIA",line.substring(193,197));
			//c.addAttribute("SEGUNDA LETRA (CARACTER DE DUPLICADO)",line.substring(197,198));
			//c.addAttribute("KILOMETRO (3enteros y 2decimales)",line.substring(198,203));
			//c.addAttribute("BLOQUE",line.substring(203,207));
			//c.addAttribute("TEXTO DE DIRECCION NO ESTRUCTURADA",line.substring(215,240));
			c.addAttribute("addr:full",eliminarComillas(line.substring(215,240)));
			//c.addAttribute("CODIGO POSTAL",line.substring(240,245));
			c.addAttribute("addr:postcode",eliminarCerosString(line.substring(240,245)));
			c.addAttribute("addr:country","ES");
			//c.addAttribute("DISTRITO MUNICIPAL",line.substring(245,247));
			//c.addAttribute("CODIGO DEL MUNICIPIO ORIGEN EN CASO DE AGREGACION",line.substring(247,250));
			//c.addAttribute("CODIGO DE LA ZONA DE CONCENTRACION",line.substring(250,252));
			//c.addAttribute("CODIGO DE POLIGONO",line.substring(252,255));
			//c.addAttribute("catastro:ref:polygon",eliminarCerosString(line.substring(252,255)));
			//c.addAttribute("CODIGO DE PARCELA",line.substring(255,260));
			//c.addAttribute("CODIGO DE PARAJE",line.substring(260,265));
			//c.addAttribute("NOMBRE DEL PARAJE",line.substring(265,295));
			//c.addAttribute("SUPERFICIE CATASTRAL (metros cuadrados)",line.substring(295,305));
			c.addAttribute("catastro:surface",eliminarCerosString(line.substring(295,305)));
			//c.addAttribute("SUPERFICIE CONSTRUIDA TOTAL",line.substring(305,312));
			c.addAttribute("catastro:surface:built",eliminarCerosString(line.substring(305,312)));
			//c.addAttribute("SUPERFICIE CONSTRUIDA SOBRE RASANTE",line.substring(312,319));
			c.addAttribute("catastro:surface:overground",eliminarCerosString(line.substring(312,319)));
			//c.addAttribute("SUPERFICIE CUBIERTA",line.substring(319,333));
			//c.addAttribute("COORDENADA X (CON 2 DECIMALES Y SIN SEPARADOR)",line.substring(333,342));
			//c.addAttribute("COORDENADA Y (CON 2 DECIMALES Y SIN SEPARADOR)",line.substring(342,352));
			//c.addAttribute("REFERENCIA CATASTRAL BICE DE LA FINCA",line.substring(581,601));
			//c.addAttribute("catastro:ref:bice",eliminarCerosString(line.substring(581,601)));
			//c.addAttribute("DENOMINACION DEL BICE DE LA FINCA",line.substring(601,666));
			//c.addAttribute("HUSO GEOGRAFICO SRS",line.substring(666,676));

			return c;}
		case 13: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2));
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DE MUNICIPIO",line.substring(25,28));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(25,28)));
			//c.addAttribute("CLASE DE LA UNIDAD CONSTRUCTIVA",line.substring(28,30));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("CODIGO DE LA UNIDAD CONSTRUCTIVA",line.substring(44,48)); 
			//c.addAttribute("CODIGO DE PROVINCIA",line.substring(50,52));
			//c.addAttribute("catastro:ref:province",eliminarCerosString(line.substring(50,52)));
			//c.addAttribute("NOMBRE PROVINCIA",line.substring(52,77));
			//c.addAttribute("is_in_province",line.substring(52,77));
			//c.addAttribute("CODIGO DEL MUNICIPIO DGC",line.substring(77,80));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(77,80)));
			//c.addAttribute("CODIGO DE MUNICIPIO (INE) EXCLUIDO EL ULTIMO DIGITO DE CONTROL",line.substring(80,83));
			//c.addAttribute("ine:ref:municipality",eliminarCerosString(line.substring(80,83)));
			//c.addAttribute("NOMBRE DEL MUNICIPIO",line.substring(83,123));
			//c.addAttribute("is_in:municipality",eliminarComillas(line.substring(83,123)));
			//c.addAttribute("NOMBRE DE LA ENTIDAD MENOR EN CASO DE EXISTIR",line.substring(123,153));
			//c.addAttribute("CODIGO DE VIA PUBLICA DGC",line.substring(153,158));
			//c.addAttribute("catastro:ref:way",eliminarCerosString(line.substring(153,158)));
			//c.addAttribute("TIPO DE VIA O SIBLA PUBLICA",line.substring(158,163));
			//c.addAttribute("NOMBRE DE VIA PUBLICA",line.substring(163,188));
			c.addAttribute("addr:street",nombreTipoViaParser(line.substring(158,163).trim())+" "+formatearNombreCalle(eliminarComillas(line.substring(163,188).trim())));
			//c.addAttribute("PRIMER NUMERO DE POLICIA",line.substring(188,192));
			c.addAttribute("addr:housenumber",eliminarCerosString(line.substring(188,192)));
			//c.addAttribute("PRIMERA LETRA (CARACTER DE DUPLICADO)",line.substring(192,193));
			//c.addAttribute("SEGUNDO NUMERO DE POLICIA",line.substring(193,197));
			//c.addAttribute("SEGUNDA LETRA (CARACTER DE DUPLICADO)",line.substring(197,198));
			//c.addAttribute("KILOMETRO (3enteros y 2decimales)",line.substring(198,203));
			//c.addAttribute("TEXTO DE DIRECCION NO ESTRUCTURADA",line.substring(215,240));
			c.addAttribute("addr:full",eliminarComillas(line.substring(215,240).trim()));
			//c.addAttribute("ANO DE CONSTRUCCION (AAAA)",line.substring(295,299));
			c.setFechaAlta(Long.parseLong(line.substring(295,299)+"0101")); 
			c.setFechaBaja(fechaHasta);
			//c.addAttribute("INDICADOR DE EXACTITUD DEL ANO DE CONTRUCCION",line.substring(299,300));
			//c.addAttribute("SUPERFICIE DE SUELO OCUPADA POR LA UNIDAD CONSTRUCTIVA",line.substring(300,307));
			c.addAttribute("catastro:surface",eliminarCerosString(line.substring(300,307)));
			//c.addAttribute("LONGITUD DE FACHADA",line.substring(307,312));
			//c.addAttribute("CODIGO DE UNIDAD CONSTRUCTIVA MATRIZ",line.substring(409,413));


			return c; }
		case 14: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2)); 
			//c.addAttribute("CODIGO DE DELEGACION DEL MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(25,28)));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("NUMERO DE ORDEN DEL ELEMENTO DE CONSTRUCCION",line.substring(44,48));
			//c.addAttribute("NUMERO DE ORDEN DEL BIEN INMUEBLE FISCAL",line.substring(50,54));
			//c.addAttribute("CODIGO DE LA UNIDAD CONSTRUCTIVA A LA QUE ESTA ASOCIADO EL LOCAL",line.substring(54,58));
			//c.addAttribute("BLOQUE",line.substring(58,62));
			//c.addAttribute("ESCALERA",line.substring(62,64));
			//c.addAttribute("PLANTA",line.substring(64,67));
			//c.addAttribute("PUERTA",line.substring(67,70));
			//c.addAttribute("CODIGO DE DESTINO SEGUN CODIFICACION DGC",line.substring(70,73));
			//c.addAttribute("INDICADOR DEL TIPO DE REFORMA O REHABILITACION",line.substring(73,74));
			//c.addAttribute("ANO DE REFORMA EN CASO DE EXISTIR",line.substring(74,78));
			//c.addAttribute("ANO DE ANTIGUEDAD EFECTIVA EN CATASTRO",line.substring(78,82)); 
			c.setFechaAlta(Long.parseLong(line.substring(78,82)+"0101")); 
			c.setFechaBaja(fechaHasta);
			//c.addAttribute("INDICADOR DE LOCAL INTERIOR (S/N)",line.substring(82,83));
			//c.addAttribute("SUPERFICIE TOTAL DEL LOCAL A EFECTOS DE CATASTRO",line.substring(83,90));
			//c.addAttribute("SUPERFICIA DE PORCHES Y TERRAZAS DEL LOCAL",line.substring(90,97));
			//c.addAttribute("SUPERFICIE IMPUTABLE AL LOCAL SITUADA EN OTRAS PLANTAS",line.substring(97,104));
			//c.addAttribute("TIPOLOGIA CONSTRUCTIVA SEGUN NORMAS TECNICAS DE VALORACION",line.substring(104,109));
			//c.addAttribute("CODIGO DE MODALIDAD DE REPARTO",line.substring(111,114));

			return c;}
		case 15: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2));
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(25,28)));
			//c.addAttribute("CLASE DE BIEN INMUEBLE (UR, RU, BI)",line.substring(28,30));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("NUMERO SECUENCIAL DEL BIEN INMUEBLE DENTRO DE LA PARCELA",line.substring(44,48));
			//c.addAttribute("PRIMER CARACTER DE CONTROL",line.substring(48,49));
			//c.addAttribute("SEGUNDO CARACTER DE CONTROL",line.substring(49,50));
			//c.addAttribute("NUMERO FIJO DEL BIEN INMUEBLE",line.substring(50,58));
			//c.addAttribute("CAMPO PARA LA IDENTIFICACION DEL BIEN INMUEBLE ASIGNADO POR EL AYTO",line.substring(58,73));
			//c.addAttribute("NUMERO DE FINCA REGISTRAL",line.substring(73,92));
			//c.addAttribute("CODIGO DE PROVINCIA",line.substring(92,94));
			//c.addAttribute("catastro:ref:province",eliminarCerosString(line.substring(92,94)));
			//c.addAttribute("NOMBRE DE PROVINCIA",line.substring(94,119));
			//c.addAttribute("is_in:province",eliminarComillas(line.substring(94,119)));
			//c.addAttribute("CODIGO DE MUNICIPIO",line.substring(119,122));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(119,122)));
			//c.addAttribute("CODIGO DE MUNICIPIO (INE) EXCLUIDO EL ULTIMO DIGITO DE CONTROL",line.substring(122,125));
			//c.addAttribute("catastro:ref:municipality",eliminarCerosString(line.substring(122,125)));
			//c.addAttribute("NOMBRE DE MUNICIPIO",line.substring(125,165));
			//c.addAttribute("is_in:municipality",eliminarComillas(line.substring(125,165)));
			//c.addAttribute("NOMBRE DE LA ENTIDAD MENOR EN CASO DE EXISTIR",line.substring(165,195));
			//c.addAttribute("CODIGO DE VIA PUBLICA",line.substring(195,200));
			//c.addAttribute("catastro:ref:way",eliminarCerosString(line.substring(195,200)));
			//c.addAttribute("TIPO DE VIA O SIGLA PUBLICA",line.substring(200,205));
			//c.addAttribute("NOMBRE DE VIA PUBLICA",line.substring(205,230));
			c.addAttribute("addr:street",nombreTipoViaParser(line.substring(200,205).trim())+" "+formatearNombreCalle(eliminarComillas(line.substring(205,230).trim())));
			//c.addAttribute("PRIMER NUMERO DE POLICIA",line.substring(230,234));
			c.addAttribute("addr:housenumber",eliminarCerosString(line.substring(230,234)));
			//c.addAttribute("PRIMERA LETRA (CARACTER DE DUPLICADO)",line.substring(234,235));
			//c.addAttribute("SEGUNDO NUMERO DE POLICIA",line.substring(235,239));
			//c.addAttribute("SEGUNDA LETRA (CARACTER DE DUPLICADO)",line.substring(239,240));
			//c.addAttribute("KILOMETRO (3enteros y 2decimales)",line.substring(240,245));
			//c.addAttribute("BLOQUE",line.substring(245,249));
			//c.addAttribute("ESCALERA",line.substring(249,251));
			//c.addAttribute("PLANTA",line.substring(251,254));
			//c.addAttribute("PUERTA",line.substring(254,257));
			//c.addAttribute("TEXTO DE DIRECCION NO ESTRUCTURADA",line.substring(257,282));
			c.addAttribute("addr:full",eliminarComillas(line.substring(257,282).trim()));
			//c.addAttribute("CODIGO POSTAL",line.substring(282,287));
			c.addAttribute("addr:postcode",eliminarCerosString(line.substring(282,287)));
			c.addAttribute("addr:country" ,"ES");
			//c.addAttribute("DISTRITO MUNICIPAL",line.substring(287,289));
			//c.addAttribute("CODIGO DEL MUNICIPIO DE ORIGEN EN CASO DE AGREGACION",line.substring(289,292));
			//c.addAttribute("CODIGO DE LA ZONA DE CONCENTRACION",line.substring(292,294));
			//c.addAttribute("CODIGO DE POLIGONO",line.substring(294,297));
			//c.addAttribute("CODIGO DE PARCELA",line.substring(297,302));
			//c.addAttribute("CODIGO DE PARAJE",line.substring(302,307));
			//c.addAttribute("NOMBRE DEL PARAJE",line.substring(307,337));
			//c.addAttribute("NUMERO DE ORDEN DEL INMUEBLE EN LA ESCRITURA DE DIVISION HORIZONTAL",line.substring(367,371));
			//c.addAttribute("ANO DE ANTIGUEDAD DEL BIEN INMUEBLE",line.substring(371,375)); 
			c.setFechaAlta(Long.parseLong(line.substring(371,375)+"0101")); 
			c.setFechaBaja(fechaHasta);
			//c.addAttribute("CLAVE DE GRUPO DE LOS BIENES INMUEBLES DE CARAC ESPECIALES",line.substring(427,428));
			c.addAttribute(usoInmueblesParser(line.substring(427,428)));
			//c.addAttribute("SUPERFICIE DEL ELEMENTO O ELEMENTOS CONSTRUCTIVOS ASOCIADOS AL INMUEBLE",line.substring(441,451));
			//c.addAttribute("SUPERFICIE ASOCIADA AL INMUEBLE",line.substring(451,461));
			//c.addAttribute("COEFICIENTE DE PROPIEDAD (3ent y 6deci)",line.substring(461,470));


			return c;}
		case 16: {

			// Este tipo no tiene fechaAlta ni fechaBaja
			c.setFechaAlta(fechaDesde); 
			c.setFechaBaja(fechaHasta);

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2));
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("NUMERO DE ORDEN DEL ELEMENTO CUYO VALOR SE REPARTE",line.substring(44,48));
			//c.addAttribute("CALIFICACION CATASTRAL DE LA SUBPARCELA",line.substring(48,50));
			//c.addAttribute("BLOQUE REPETITIVO HASTA 15 VECES",line.substring(50,999));


			return c;}
		case 17: {

			// Este tipo no tiene fechaAlta ni fechaBaja
			c.setFechaAlta(fechaDesde); 
			c.setFechaBaja(fechaHasta);

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2));
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
			//c.addAttribute("NATURALEZA DEL SUELO OCUPADO POR EL CULTIVO (UR, RU)",line.substring(28,30));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("CODIGO DE LA SUBPARCELA",line.substring(44,48));
			c.setSubparce(line.substring(44,48));
			//c.addAttribute("NUMERO DE ORDEN DEL BIEN INMUEBLE FISCAL",line.substring(50,54));
			//c.addAttribute("TIPO DE SUBPARCELA (T, A, D)",line.substring(54,55));
			//c.addAttribute("SUPERFICIE DE LA SUBPARCELA (m cuadrad)",line.substring(55,65));
			c.addAttribute("catastro:surface",eliminarCerosString(line.substring(55,65)));
			//c.addAttribute("CALIFICACION CATASTRAL/CLASE DE CULTIVO",line.substring(65,67));
			//c.addAttribute("DENOMINACION DE LA CLASE DE CULTIVO",line.substring(67,107));
			//c.addAttribute("INTENSIDAD PRODUCTIVA",line.substring(107,109));
			//c.addAttribute("CODIGO DE MODALIDAD DE REPARTO",line.substring(126,129));


			return c;}
		case 90: {

			/*System.out.println("\nTIPO DE REGISTRO 90: REGISTRO DE COLA\n"); 
			System.out.println("Numero total de registros tipo 11           :"+line.substring(9,16));
			System.out.println("Numero total de registros tipo 13           :"+line.substring(23,30));
			System.out.println("Numero total de registros tipo 14           :"+line.substring(30,37));
			System.out.println("Numero total de registros tipo 15           :"+line.substring(37,44));
			System.out.println("Numero total de registros tipo 16           :"+line.substring(44,51));
			System.out.println("Numero total de registros tipo 17           :"+line.substring(51,58));
			 */
			break;}
		}
		return c;
	}

	/** Elimina ceros a la izquierda en un String
	 * @param s String en el cual eliminar los ceros de la izquierda
	 * @return String sin los ceros de la izquierda
	 */
	public static String eliminarCerosString(String s){
		String temp = s.trim();
		if (esNumero(temp) && !temp.isEmpty()){
			Integer i = Integer.parseInt(temp);
			if (i != 0)
				temp = i.toString();
			else 
				temp = null;
		}
		return temp;
	}


	/** Comprueba si solo contiene caracteres numericos
	 * @param str String en el cual comprobar
	 * @return boolean de si es o no
	 */
	public static boolean esNumero(String s)
	{
		for (int x = 0; x < s.length(); x++) {
			if (!Character.isDigit(s.charAt(x)))
				return false;
		}
		return true;
	}
	
	
	/** Eliminar las comillas '"' de los textos, sino al leerlo JOSM devuelve error
	 * pensando que ha terminado un valor antes de tiempo.
	 * @param s String al que quitar las comillas
	 * @return String sin las comillas
	 */
	public static String eliminarComillas(String s){
		String ret = new String();
		for (int x = 0; x < s.length(); x++)
			if (s.charAt(x) != '"') ret += s.charAt(x);
		return ret;
	}


	public static String nombreTipoViaParser(String codigo){

		if (codigo.equals("CL"))return "Calle";
		else if (codigo.equals("AL"))return "Aldea/Alameda";
		else if (codigo.equals("AR"))return "Area/Arrabal";
		else if (codigo.equals("AU"))return "Autopista";
		else if (codigo.equals("AV"))return "Avenida";
		else if (codigo.equals("AY"))return "Arroyo";
		else if (codigo.equals("BJ"))return "Bajada";
		else if (codigo.equals("BO"))return "Barrio";
		else if (codigo.equals("BR"))return "Barranco";
		else if (codigo.equals("CA"))return "Cañada";
		else if (codigo.equals("CG"))return "Colegio/Cigarral";
		else if (codigo.equals("CH"))return "Chalet";
		else if (codigo.equals("CI"))return "Cinturon";
		else if (codigo.equals("CJ"))return "Calleja/Callejón";
		else if (codigo.equals("CM"))return "Camino";
		else if (codigo.equals("CN"))return "Colonia";
		else if (codigo.equals("CO"))return "Concejo/Colegio";
		else if (codigo.equals("CP"))return "Campa/Campo";
		else if (codigo.equals("CR"))return "Carretera/Carrera";
		else if (codigo.equals("CS"))return "Caserío";
		else if (codigo.equals("CT"))return "Cuesta/Costanilla";
		else if (codigo.equals("CU"))return "Conjunto";
		else if (codigo.equals("DE"))return "Detrás";
		else if (codigo.equals("DP"))return "Diputación";
		else if (codigo.equals("DS"))return "Diseminados";
		else if (codigo.equals("ED"))return "Edificios";
		else if (codigo.equals("EM"))return "Extramuros";
		else if (codigo.equals("EN"))return "Entrada, Ensanche";
		else if (codigo.equals("ER"))return "Extrarradio";
		else if (codigo.equals("ES"))return "Escalinata";
		else if (codigo.equals("EX"))return "Explanada";
		else if (codigo.equals("FC"))return "Ferrocarril";
		else if (codigo.equals("FN"))return "Finca";
		else if (codigo.equals("GL"))return "Glorieta";
		else if (codigo.equals("GR"))return "Grupo";
		else if (codigo.equals("GV"))return "Gran Vía";
		else if (codigo.equals("HT"))return "Huerta/Huerto";
		else if (codigo.equals("JR"))return "Jardines";
		else if (codigo.equals("LD"))return "Lado/Ladera";
		else if (codigo.equals("LG"))return "Lugar";
		else if (codigo.equals("MC"))return "Mercado";
		else if (codigo.equals("ML"))return "Muelle";
		else if (codigo.equals("MN"))return "Municipio";
		else if (codigo.equals("MS"))return "Masias";
		else if (codigo.equals("MT"))return "Monte";
		else if (codigo.equals("MZ"))return "Manzana";
		else if (codigo.equals("PB"))return "Poblado";
		else if (codigo.equals("PD"))return "Partida";
		else if (codigo.equals("PJ"))return "Pasaje/Pasadizo";
		else if (codigo.equals("PL"))return "Polígono";
		else if (codigo.equals("PM"))return "Paramo";
		else if (codigo.equals("PQ"))return "Parroquia/Parque";
		else if (codigo.equals("PR"))return "Prolongación/Continuación";
		else if (codigo.equals("PS"))return "Paseo";
		else if (codigo.equals("PT"))return "Puente";
		else if (codigo.equals("PZ"))return "Plaza";
		else if (codigo.equals("QT"))return "Quinta";
		else if (codigo.equals("RB"))return "Rambla";
		else if (codigo.equals("RC"))return "Rincón/Rincona";
		else if (codigo.equals("RD"))return "Ronda";
		else if (codigo.equals("RM"))return "Ramal";
		else if (codigo.equals("RP"))return "Rampa";
		else if (codigo.equals("RR"))return "Riera";
		else if (codigo.equals("RU"))return "Rua";
		else if (codigo.equals("SA"))return "Salida";
		else if (codigo.equals("SD"))return "Senda";
		else if (codigo.equals("SL"))return "Solar";
		else if (codigo.equals("SN"))return "Salón";
		else if (codigo.equals("SU"))return "Subida";
		else if (codigo.equals("TN"))return "Terrenos";
		else if (codigo.equals("TO"))return "Torrente";
		else if (codigo.equals("TR"))return "Travesía";
		else if (codigo.equals("UR"))return "Urbanización";
		else if (codigo.equals("VR"))return "Vereda";
		else if (codigo.equals("CY"))return "Caleya";

		return codigo;
	}


	/** Traduce el codigo de uso de inmueble que traen los .cat a sus tags en OSM
	 * Como los cat se leen despues de los shapefiles, hay tags que los shapefiles traen
	 * mas concretos, que esto los machacaria. Es por eso que si al tag le ponemos un '*'
	 * por delante, comprueba que no exista ese tag antes de meterlo. En caso de existir
	 * dejaria el que ya estaba.
	 * @param codigo Codigo de uso de inmueble
	 * @return Lista de tags que genera
	 */
	public static List<String[]> usoInmueblesParser(String codigo){
		List<String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];

		switch (codigo.charAt(0)){
		case 'A':{
			s[0] = "building"; s[1] = "warehouse";
			l.add(s);
			s = new String[2];
			s[0] =  "*landuse"; s[1] = "industrial";
			l.add(s);
			return l;}
		case 'V':{
			s[0] = "*landuse"; s[1] = "residential";
			l.add(s);
			return l;}
		case 'I':{
			s[0] = "*landuse"; s[1] = "industrial";
			l.add(s);
			return l;}
		case 'O':{
			s[0] = "*landuse"; s[1] = "commercial";
			l.add(s);
			return l;}
		case 'C':{
			s[0] = "*landuse"; s[1] = "retail";
			l.add(s);
			return l;}
		case 'K':{
			s[0] = "*landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] = "recreation_type"; s[1] = "sports";
			l.add(s);
			return l;}
		case 'T':{
			s[0] = "*landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] = "recreation_type"; s[1] = "entertainment";
			l.add(s);
			return l;}
		case 'G':{
			s[0] = "*landuse"; s[1] = "retail";
			l.add(s);
			return l;}
		case 'Y':{
			s[0] = "*landuse"; s[1] = "health";
			l.add(s);
			return l;}
		case 'E':{
			s[0] = "*landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] =  "recreation_type"; s[1] = "culture";
			l.add(s);
			return l;}
		case 'R':{
			s[0] = "building"; s[1] = "church";
			l.add(s);
			return l;}
		case 'M':{
			s[0] = "*landuse"; s[1] = "greenfield";
			l.add(s);
			return l;}
		case 'P':{
			s[0] = "amenity"; s[1] = "public_building";
			l.add(s);
			s = new String[2];
			s[0] ="building"; s[1] ="yes";
			l.add(s);
			return l;}
		case 'B':{
			s[0] = "building"; s[1] = "warehouse";
			l.add(s);
			s = new String[2];
			s[0] = "*landuse"; s[1] ="farmyard";
			l.add(s);
			return l;}
		case 'J':{
			s[0] = "*landuse"; s[1] = "industrial";
			l.add(s);
			return l;}
		case 'Z':{
			s[0] = "*landuse"; s[1] = "farm";
			l.add(s);
			return l;}
		default:
			s[0] = "fixme"; s[1] = "Documentar nuevo codificación de los usos de los vienes inmuebles en catastro código="+ codigo +" en http://wiki.openstreetmap.org/w/index.php?title=Traduccion_metadatos_catastro_a_map_features";
			l.add(s);
			return l;}
	}

	
	/** Pasa todo el nombre de la calle a minusculas y luego va poniendo en mayusculas las primeras
	 * letras de todas las palabras a menos que sean DE|DEL|EL|LA|LOS|LAS
	 * @param s El nombre de la calle
	 * @return String con el nombre de la calle pasando los articulos a minusculas.
	 */
	public static String formatearNombreCalle(String c){
		
		String[] l = c.toLowerCase().split(" ");
		String ret = "";
		
		for (String s : l){
			if (!s.isEmpty() && !s.equals("de") && !s.equals("del") && !s.equals("la") && !s.equals("las") && !s.equals("el") && !s.equals("los")){
				char mayus = Character.toUpperCase(s.charAt(0));
				ret += mayus + s.substring(1, s.length())+" ";
			}
			else
				ret += s+" ";
		}
		
		return ret.trim();
	}

}
