import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.data.FeatureReader;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;

import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;


public class Cat2Osm {
	
	static Cat2OsmUtils utils;
	
	/** Constructor
	 * @param utils Clase utils en la que se almacenan los nodos, ways y relaciones 
	 * y tiene funciones para manejarlos
	 */
	public Cat2Osm (Cat2OsmUtils utils){
		Cat2Osm.utils = utils;
	}
	
	
	/** Parsea el archivo binario shp y crea los elementos en memoria en un List
	 * @param f Archivo a parsear
	 * @returns List<Shape> Lista de los elementos parseados
	 * @throws IOException
	 */
	public List<Shape> shpParser(File f) throws IOException{

		FileDataStore store = FileDataStoreFinder.getDataStore(f);
		FeatureReader<SimpleFeatureType, SimpleFeature> reader = 
			store.getFeatureReader();

		List<Shape> shapeList = new ArrayList<Shape>();
		long fechaDesde = Long.parseLong(Config.get("FechaDesde"));
		long fechaHasta = Long.parseLong(Config.get("FechaHasta"));

		// Creamos el shape dependiendo de su tipo
		if (f.equals(new File(Config.get("MasaSHPFile"))))

			// Shapes del archivo MASA.SHP
			while (reader.hasNext()) {
				Shape shape = new ShapeMasa(reader.next());

				// Si cumple estar entre las fechas
				if (shape != null && shape.checkShapeDate(fechaDesde, fechaHasta))
					// Anadimos el shape creado a la lista
					shapeList.add(mPolygonShapeParser(shape));
			}
		else if (f.equals(new File(Config.get("ParcelaSHPFile"))))

			// Shapes del archivo PARCELA.SHP
			while (reader.hasNext()) {
				Shape shape = new ShapeParcela(reader.next());

				// Si cumple estar entre las fechas
				if (shape != null && shape.checkShapeDate(fechaDesde, fechaHasta))
					// Anadimos el shape creado a la lista
					shapeList.add(mPolygonShapeParser(shape));
			}
		else if (f.equals(new File(Config.get("SubparceSHPFile"))))

			// Shapes del archivo SUBPARCE.SHP
			while (reader.hasNext()) {
				Shape shape = new ShapeSubparce(reader.next());

				// Si cumple estar entre las fechas
				if (shape != null && shape.checkShapeDate(fechaDesde, fechaHasta))
					// Anadimos el shape creado a la lista
					shapeList.add(mPolygonShapeParser(shape));
			}
		else if (f.equals(new File(Config.get("ConstruSHPFile"))))

			// Shapes del archivo CONSTRU.SHP
			while (reader.hasNext()) {
				Shape shape = new ShapeConstru(reader.next());

				// Si cumple estar entre las fechas
				if (shape != null && shape.checkShapeDate(fechaDesde, fechaHasta))
					// Anadimos el shape creado a la lista
					shapeList.add(mPolygonShapeParser(shape));
			}
		else if (f.equals(new File(Config.get("ElemtexSHPFile"))))

			// Shapes del archivo ELEMTEX.SHP
			while (reader.hasNext()) {
				Shape shape = new ShapeElemtex(reader.next());

				// Si cumple estar entre las fechas
				if (shape != null && shape.checkShapeDate(fechaDesde, fechaHasta))
					// Anadimos el shape creado a la lista
					shapeList.add(pointShapeParser(shape));
			}
		else if (f.equals(new File(Config.get("ElempunSHPFile"))))

			// Shapes del archivo ELEMPUN.SHP
			while (reader.hasNext()) {
				Shape shape = new ShapeElempun(reader.next());

				// Si cumple estar entre las fechas
				if (shape != null && shape.checkShapeDate(fechaDesde, fechaHasta))
					// Anadimos el shape creado a la lista
					shapeList.add(pointShapeParser(shape));
			}
		else if (f.equals(new File(Config.get("ElemlinSHPFile"))))

			// Shapes del archivo ELEMLIN.SHP
			while (reader.hasNext()) {
				Shape shape = new ShapeElemlin(reader.next());

				// Si cumple estar entre las fechas
				if (shape != null && shape.checkShapeDate(fechaDesde, fechaHasta))
					// Anadimos el shape creado a la lista
					shapeList.add(mLineStringShapeParser(shape));
			}
		else if (f.equals(new File(Config.get("EjesSHPFile"))))

			// Shapes del archivo EJES.SHP
			while (reader.hasNext()) {
				Shape shape = new ShapeEjes(reader.next());

				// Si cumple estar entre las fechas
				if (shape != null && shape.checkShapeDate(fechaDesde, fechaHasta))
					// Anadimos el shape creado a la lista
					shapeList.add(mLineStringShapeParser(shape));
			}
		else System.out.println("Archivo de Shapes "+ f.getName() +" desconocido");
		
		reader.close();
		store.dispose();
		return shapeList;
	}

	
	/** Metodo para parsear los shapes cuyas geografias vienen dadas como
	 * MultiPolygon, como MASA.SHP, PARCELA.SHP y CONSTRU.SHP
	 * Asigna los valores al shape, sus nodos, sus ways y relation
	 * @param shape Shape creado pero sin los valores de los nodos, ways o relation
	 * @return Shape con todos los valores asignados
	 */
	private Shape mPolygonShapeParser(Shape shape){

		// Obtenemos las coordenadas de cada punto del shape
		for (int x = 0; x < shape.getPoligons().size(); x++){
			Coordinate[] coor = shape.getCoordenadas(x);

			// Miramos por cada punto si existe un nodo si no, lo creamos
			for (int y = 0 ; y < coor.length; y++){
				// Insertamos en la lista de nodos del shape, los ids de sus nodos
				shape.addNode(utils.getNodeId(coor[y], null));
			}
		}

		// Partimos el poligono en el maximo numero de ways es decir uno por cada
		// dos nodos, mas adelante se juntaran los que sean posibles
		for (int x = 0; x < shape.getPoligons().size() ; x++){
			List <Long> nodeList = shape.getNodesPoligonN(x, utils);
			for (int y = 0; y < nodeList.size()-1 ; y++){
				List<Long> way = new ArrayList<Long>();
				way.add(nodeList.get(y));
				way.add(nodeList.get(y+1));
				shape.addWay(utils.getWayId(way, shape.getRefCat()));
			}
		}

		// Creamos una relation para el shape, metiendoe en ella todos los members
		List <Long> ids = new ArrayList<Long>(); // Ids de los members
		List <String> types = new ArrayList<String>(); // Tipos de los members
		List <String> roles = new ArrayList<String>(); // Roles de los members
		for (int x = 0; x < shape.getPoligons().size() ; x++){
			List <Long> wayList = shape.getWaysPoligonN(x, utils);
			for (Long way: wayList){
				ids.add(way);
				types.add("way");
				if (x == 0)roles.add("outer");
				else roles.add("inner");
			}
		}
		shape.setRelation(utils.getRelationId(ids, types, roles, shape.getAttributes()));

		return shape;
	}
	
	
	/** Metodo para parsear los shapes cuyas geografias vienen dadas como Point
	 * o MultiLineString pero queremos solo un punto, como ELEMPUN.SHP y ELEMTEX.SHP
	 * Asigna los valores al shape y su unico nodo
	 * @param shape Shape creado pero sin el valor del nodo
	 * @return Shape con todos los valores asignados
	 */
	private Shape pointShapeParser(Shape shape){
		
		// Anadimos solo un nodo
		shape.addNode(utils.getNodeId(shape.getCoor(), shape.getAttributes()));
		
		return shape;
	}
	
	
	/** Metodo para parsear los shapes cuyas geografias vienen dadas como
	 * MultiLineString, como ELEMLIN.SHP y EJES.SHP
	 * Asigna los valores al shape, sus nodos, sus ways y relation
	 * @param shape Shape creado pero sin los valores de los nodos, ways o relation
	 * @return Shape con todos los valores asignados
	 */
	private Shape mLineStringShapeParser(Shape shape){
		
		// Anadimos todos los nodos
			Coordinate[] coor = shape.getCoordenadas(0);
			for (int x = 0; x < coor.length; x++)
				shape.addNode(utils.getNodeId(coor[x], null));
			
		// Con los nodos creamos ways
		List <Long> nodeList = shape.getNodes();
		for (int y = 0; y < nodeList.size()-1 ; y++){
			List<Long> way = new ArrayList<Long>();
			way.add(nodeList.get(y));
			way.add(nodeList.get(y+1));
			shape.addWay(utils.getWayId(way, null));
		}
		
		// Con los ways creamos una relacion
		List <Long> ids = new ArrayList<Long>(); // Ids de los members
		List <String> types = new ArrayList<String>(); // Tipos de los members
		List <String> roles = new ArrayList<String>(); // Roles de los members
		for (Long way: shape.getWays()){
			ids.add(way);
			types.add("way");
			roles.add("outer");
		}
		shape.setRelation(utils.getRelationId(ids, types, roles, shape.getAttributes()));
		
		return shape;
	}
	
	/** Busca en la lista de shapes los que coincidan con la ref catastral
	 * @param ref referencia catastral a buscar
	 * @returns List<Shape> lista de shapes que coinciden                    
	 */
	private static List<Shape> findRefCat(List<Shape> shapes, String ref){
		List<Shape> shapeList = new ArrayList<Shape>();

		for(Shape shape : shapes) 
			if (shape.getRefCat().equals(ref)) shapeList.add(shape);

		return shapeList;
	}
	
	
	/** Busca en la lista de shapes los que coincidan con el codigo de subparce
	 * @param subparce codigo de subparcela a buscar
	 * @returns List<Shape> lista de shapes que coinciden                    
	 */
	private static List<Shape> findSubparce(List<Shape> shapes, String subparce){
		List<Shape> shapeList = new ArrayList<Shape>();

		for(Shape shape : shapes) 
			if (shape instanceof ShapeSubparce)
				if (((ShapeSubparce) shape).getSubparce().equals(subparce))
					shapeList.add(shape);

		return shapeList;
	}

	
	/** Se encarga de solucionar casos de nodos de un shape que se hayan creado sobre el
	 * way de otro shape. Con un valor flexible del config decide si un nodo esta sobre
	 * un way de otro shape, en cuyo caso partiria el way del shape en dos unidos por el nodo y
	 * los dos shapes compartirian el way comun.
	 * SOLO para cuando los ways tienen todavia solo dos nodos.
	 * @param shapes Lista de shapes 
	 * @return La lista de shapes con los arreglos realizados
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<Shape> fixNodesOnWays(List <Shape> shapes){
		
		float valorNodeWay = Float.parseFloat(Config.get("ValorNodeWay"));
		
		// Para cada shape
		for (Shape shape : shapes){
			
			// Coger cada poligono
			for( int x = 0; x < shape.getPoligons().size(); x++){
				
				// Coger todos sus ways
				for (Long wayId : shape.getWaysPoligonN(x, utils)){
					
					WayOsm way = ((WayOsm) utils.getKeyFromValue((Map<Object, Long>) ((Object)utils.getTotalWays()), wayId));
					NodeOsm nodeA = ((NodeOsm) utils.getKeyFromValue((Map<Object, Long>) ((Object)utils.getTotalNodes()), way.getNodes().get(0))); 
					NodeOsm nodeB = ((NodeOsm) utils.getKeyFromValue((Map<Object, Long>) ((Object)utils.getTotalNodes()), way.getNodes().get(1)));
					
					// Compararlo con todos los nodos existentes
					Iterator<Entry<NodeOsm, Long>> it = utils.getTotalNodes().entrySet().iterator();
					
					while(it.hasNext()){
						Map.Entry e = (Map.Entry)it.next();
						NodeOsm node = (NodeOsm) e.getKey();
						
						if (!node.getCoor().equals(nodeA.getCoor()) && !node.getCoor().equals(nodeB.getCoor())){

							Coordinate[] coor = new Coordinate[2];
							coor[0] = nodeA.getCoor();
							coor[1] = nodeB.getCoor();
							
							// Creamos un lineString para calcular si se encuentra en el "Envelope" que crea el way
							// El envelope es el rectangulo que crearia con los ejes X e Y
							LineString line = new LineString(coor, null , 0);
							
							// Formula punto-pendiente para saber si esta en la linea
							float p = (float) Math.abs( ( ( (nodeB.getY() - nodeA.getY()) / (nodeB.getX() - nodeA.getX()) ) * (node.getX() - nodeA.getX())) + (nodeA.getY() - node.getY()) );

							// Ver si se cumple la formula
							try{
								if (valorNodeWay >= p && line.getEnvelope().intersects(line)){

									System.out.println("PUNTO QUITABLE "+ utils.getNodeId(node.getCoor(), null));
								}
							}
							catch(Exception exc){}
						}
					}
				}
			}
		}
		
		return shapes;
	}
	
	/** Escribe el osm con todos los nodos (Del archivo totalNodes, sin orden, MAS RAPIDO)
	 * @param tF Ruta donde escribir este archivo, sera temporal
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public void printNodes(Map <NodeOsm, Long> nodes) throws IOException{
		
		// Archivo temporal para escribir los nodos
		FileWriter fstreamNodes = new FileWriter(Config.get("resultPath") + "\\tempNodes.osm");
		BufferedWriter outNodes = new BufferedWriter(fstreamNodes);
		
		String huso = (Config.get("Huso")+ " " +Config.get("Hemisferio"));
		
		Iterator<Entry<NodeOsm, Long>> it = nodes.entrySet().iterator();
		
		// Escribimos todos los nodos
		while(it.hasNext()){
			Map.Entry e = (Map.Entry)it.next();
			outNodes.write(((NodeOsm) e.getKey()).printNode((Long) e.getValue(), huso));
		}
		outNodes.close();
	}
	
	
	/** Escribe el osm con unicamente los nodos de los shapes (MUY LENTO)
	 * @param tF Ruta donde escribir este archivo, sera temporal
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void printNodesShapesOrder(List<Shape> shapes, Map <NodeOsm, Long> nodes) throws IOException{
		
		// Archivo temporal para escribir los nodos
		FileWriter fstreamNodes = new FileWriter(Config.get("resultPath") + "\\tempNodes.osm");
		BufferedWriter outNodes = new BufferedWriter(fstreamNodes);
		
		String huso = (Config.get("Huso")+ " " +Config.get("Hemisferio"));
		
		// Escribimos todos los nodos
		for(Shape shape : shapes){
			for (int y = 0; y < shape.getNodes().size(); y++)
			outNodes.write( ((NodeOsm) utils.getKeyFromValue((Map<Object, Long>) ((Object)nodes), shape.getNodes().get(y))).printNode((Long) shape.getNodes().get(y), huso));
		}
		outNodes.close();
	}
	
	
	/** Escribe el osm con todos los ways (Del archivo waysTotales, sin orden, MAS RAPIDO)
	 * @param tF Ruta donde escribir este archivo, sera temporal
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public void printWays(Map <WayOsm, Long> ways) throws IOException{
		
		// Archivo temporal para escribir los ways
		FileWriter fstreamWays = new FileWriter(Config.get("resultPath") + "\\tempWays.osm");
		BufferedWriter outWays = new BufferedWriter(fstreamWays);
		
		Iterator<Entry<WayOsm, Long>> it = ways.entrySet().iterator();
		
		// Escribimos todos los ways y sus referencias a los nodos en el archivo
		while(it.hasNext()){
			Map.Entry e = (Map.Entry)it.next();
			outWays.write(((WayOsm) e.getKey()).printWay((Long) e.getValue()));
		}
		outWays.close();
	}
	
	
	/** Escribe el osm con unicamente los ways de los shapes (MUY LENTO)
	 * @param tF Ruta donde escribir este archivo, sera temporal
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void printWaysShapesOrder( List<Shape> shapes, Map <WayOsm, Long> ways) throws IOException{
		
		// Archivo temporal para escribir los ways
		FileWriter fstreamWays = new FileWriter(Config.get("resultPath") + "\\tempWays.osm");
		BufferedWriter outWays = new BufferedWriter(fstreamWays);
		
		// Escribimos todos los ways y sus referencias a los nodos en el archivo
		for(Shape shape : shapes){
			for (int y = 0; y < shape.getWays().size(); y++)
			outWays.write( ((WayOsm) utils.getKeyFromValue((Map<Object, Long>) ((Object)ways), shape.getWays().get(y))).printWay((Long) shape.getWays().get(y)));
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
		
		// Archivo temporal para escribir los ways
		FileWriter fstreamRelations = new FileWriter(Config.get("resultPath") + "\\tempRelations.osm");
		BufferedWriter outRelations = new BufferedWriter(fstreamRelations);
		
		Iterator<Entry<RelationOsm, Long>> it = relations.entrySet().iterator();
		
		// Escribimos todos las relaciones y sus referencias a los ways en el archivo
		while(it.hasNext()){
			Map.Entry e = (Map.Entry) it.next();
			outRelations.write(((RelationOsm) e.getKey()).printRelation((Long) e.getValue()));
		}
		outRelations.close();
	}
	
	
	/** Concatena los 3 archivos, Nodos + Ways + Relations y lo deja en el OutOsm.
	 * @param oF Ruta donde esta el archivo final
	 * @param tF Ruta donde estan los archivos temporadles (nodos, ways y relations)
	 * @throws IOException
	 */
	public void joinFiles(String filename) throws IOException{
		
		String path = Config.get("resultPath");
		
		// Borrar archivo con el mismo nombre si existe, porque sino concatenaria el nuevo
		new File(path + "\\"+ filename +".osm").delete();
		
		// Archivo al que se le concatenan los nodos, ways y relations
		FileWriter fstreamOsm = new FileWriter(path + "\\"+ filename +".osm", true);
		BufferedWriter outOsm = new BufferedWriter(fstreamOsm);
		
		// Juntamos los archivos en uno, al de los nodos le concatenamos el de ways y el de relations
		// Cabecera del archivo Osm
		outOsm.write("<?xml version='1.0' encoding='UTF-8'?>\n" +
		"<osm version=\"0.6\" generator=\"cat-2-osm\" xapi:planetDate=\"2011-09-16T07:38:02Z\" xmlns:xapi=\"http://jxapi.openstreetmap.org/\">\n");
		
		// Concatenamos todos los archivos
		String str;
		BufferedReader inNodes = new BufferedReader(new FileReader(path + "\\tempNodes.osm"));
		while ((str = inNodes.readLine()) != null)
			outOsm.write(str+"\n");
		
		BufferedReader inWays = new BufferedReader(new FileReader(path + "\\tempWays.osm"));
		while ((str = inWays.readLine()) != null)
			outOsm.write(str+"\n");

		BufferedReader inRelations = new BufferedReader(new FileReader(path + "\\tempRelations.osm"));
		while ((str = inRelations.readLine()) != null)
			outOsm.write(str+"\n");

		outOsm.write("</osm>\n");
		
		outOsm.close();
		inNodes.close();
		inWays.close();
		inRelations.close();
		
		boolean delNodes = (new File(path+ "\\tempNodes.osm")).delete();
		boolean delWays = (new File(path + "\\tempWays.osm")).delete();
		boolean delRelations = (new File(path + "\\tempRelations.osm")).delete();
		
		if (!delNodes || !delWays || !delRelations)
		System.out.println("(Imposible borrar alguno de los archivos temporales)");
		
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
			Cat c = catLineParser(line);

			if ( c.getFechaAlta() >= fechaDesde && c.getFechaAlta() < fechaHasta && c.getFechaBaja() >= fechaHasta && (c.getTipoRegistro() == tipoRegistro || tipoRegistro == 0)){

				// Obtenemos los shape que coinciden con la referencia catastral de la linea leida
				List <Shape> matches = findRefCat(shapesTotales, c.getRefCatastral());

				// Para los tipos de registro de subparcelas, buscamos la subparcela concreta para
				// anadirle los atributos
				if (c.getTipoRegistro() == 17)
					matches = findSubparce(matches, c.getSubparce());
				
				// Puede que no haya shapes para esa refCatastral
				if (!matches.isEmpty()){

					for (Shape shape : matches)
					if (shape != (null)){
							RelationOsm r = ((RelationOsm) utils.getKeyFromValue((Map<Object, Long>) ((Object)utils.getTotalRelations()), shape.getRelation()));
							r.addTags(c.getAttributes());
					}
				}
			}
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
	public static Shape getShapesForYear(List<Shape> shapes){

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
			//c.addAttribute("BLANCO EXCEPTO INMUEBLES ESPECIALES",line.substring(28,30));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44)); 
			//c.addAttribute("CODIGO DE PROVINCIA",line.substring(50,52));
			//c.addAttribute("NOMBRE DE PROVINCIA",line.substring(52,77));
			//c.addAttribute("CODIGO DE MUNICIPIO",line.substring(77,80));
			//c.addAttribute("CODIGO DE MUNICIPIO (INE). EXCLUIDO ULTIMO DIGITO DE CONTROL:",line.substring(80,83));
			//c.addAttribute("NOMBRE DE MUNICIPIO",line.substring(83,123));
			c.addAttribute("addr:city",line.substring(83,123));
			//c.addAttribute("NOMBRE DE LA ENTIDAD MENOR EN CASO DE EXISTIR",line.substring(123,153));
			//c.addAttribute("CODIGO DE VIA PUBLICA",line.substring(153,158));
			//c.addAttribute("TIPO DE VIA O SIGLA PUBLICA",line.substring(158,163));
			//c.addAttribute("NOMBRE DE VIA PUBLICA",line.substring(163,188));
			c.addAttribute("addr:street",tipoViaParser(line.substring(158,163))+ " " +line.substring(163,188));
			//c.addAttribute("PRIMER NUMERO DE POLICIA",line.substring(188,192));
			//c.addAttribute("PRIMERA LETRA (CARACTER DE DUPLICADO)",line.substring(192,193));
			//c.addAttribute("SEGUNDO NUMERO DE POLICIA",line.substring(193,197));
			//c.addAttribute("SEGUNDA LETRA (CARACTER DE DUPLICADO)",line.substring(197,198));
			//c.addAttribute("KILOMETRO (3enteros y 2decimales)",line.substring(198,203));
			//c.addAttribute("BLOQUE",line.substring(203,207));
			//c.addAttribute("TEXTO DE DIRECCION NO ESTRUCTURADA",line.substring(215,240));
			//c.addAttribute("CODIGO POSTAL",line.substring(240,245));
			c.addAttribute("addr:postcode",line.substring(240,245));
			//c.addAttribute("DISTRITO MUNICIPAL",line.substring(245,247));
			//c.addAttribute("CODIGO DEL MUNICIPIO ORIGEN EN CASO DE AGREGACION",line.substring(247,250));
			//c.addAttribute("CODIGO DE LA ZONA DE CONCENTRACION",line.substring(250,252));
			//c.addAttribute("CODIGO DE POLIGONO",line.substring(252,255));
			//c.addAttribute("CODIGO DE PARCELA",line.substring(255,260));
			//c.addAttribute("CODIGO DE PARAJE",line.substring(260,265));
			//c.addAttribute("NOMBRE DEL PARAJE",line.substring(265,295));
			//c.addAttribute("SUPERFICIE CATASTRAL (metros cuadrados)",line.substring(295,305));
			//c.addAttribute("SUPERFICIE CONSTRUIDA TOTAL",line.substring(305,312));
			//c.addAttribute("SUPERFICIE CONSTRUIDA SOBRE RASANTE",line.substring(312,319));
			//c.addAttribute("SUPERFICIE CUBIERTA",line.substring(319,333));
			//c.addAttribute("COORDENADA X (CON 2 DECIMALES Y SIN SEPARADOR)",line.substring(333,342));
			//c.addAttribute("COORDENADA Y (CON 2 DECIMALES Y SIN SEPARADOR)",line.substring(342,352));
			//c.addAttribute("REFERENCIA CATASTRAL BICE DE LA FINCA",line.substring(581,601));
			//c.addAttribute("DENOMINACION DEL BICE DE LA FINCA",line.substring(601,666));
			//c.addAttribute("HUSO GEOGRAFICO SRS",line.substring(666,676));

			c.addAttribute("source", "Catastro");
			c.addAttribute("addr:country","ES");
			
			return c;}
		case 13: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2));
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DE MUNICIPIO",line.substring(25,28));
			//c.addAttribute("CLASE DE LA UNIDAD CONSTRUCTIVA",line.substring(28,30));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("CODIGO DE LA UNIDAD CONSTRUCTIVA",line.substring(44,48)); 
			//c.addAttribute("CODIGO DE PROVINCIA",line.substring(50,52));
			//c.addAttribute("NOMBRE PROVINCIA",line.substring(52,77));
			//c.addAttribute("CODIGO DEL MUNICIPIO DGC",line.substring(77,80));
			//c.addAttribute("CODIGO DE MUNICIPIO (INE) EXCLUIDO EL ULTIMO DIGITO DE CONTROL",line.substring(80,83));
			//c.addAttribute("NOMBRE DEL MUNICIPIO",line.substring(83,123));
			c.addAttribute("addr:city",line.substring(83,123));
			//c.addAttribute("NOMBRE DE LA ENTIDAD MENOR EN CASO DE EXISTIR",line.substring(123,153));
			//c.addAttribute("CODIGO DE VIA PUBLICA DGC",line.substring(153,158));
			//c.addAttribute("TIPO DE VIA O SIBLA PUBLICA",line.substring(158,163));
			//c.addAttribute("NOMBRE DE VIA PUBLICA",line.substring(163,188));
			c.addAttribute("addr:street",tipoViaParser(line.substring(158,163))+ " " +line.substring(163,188));
			//c.addAttribute("PRIMER NUMERO DE POLICIA",line.substring(188,192));
			//c.addAttribute("PRIMERA LETRA (CARACTER DE DUPLICADO)",line.substring(192,193));
			//c.addAttribute("SEGUNDO NUMERO DE POLICIA",line.substring(193,197));
			//c.addAttribute("SEGUNDA LETRA (CARACTER DE DUPLICADO)",line.substring(197,198));
			//c.addAttribute("KILOMETRO (3enteros y 2decimales)",line.substring(198,203));
			//c.addAttribute("TEXTO DE DIRECCION NO ESTRUCTURADA",line.substring(215,240));
			c.addAttribute("ANO DE CONSTRUCCION (AAAA)",line.substring(295,299)); 
			c.setFechaAlta(Long.parseLong(line.substring(295,299)+"0101")); 
			c.setFechaBaja(fechaHasta);
			//c.addAttribute("INDICADOR DE EXACTITUD DEL ANO DE CONTRUCCION",line.substring(299,300));
			//c.addAttribute("SUPERFICIE DE SUELO OCUPADA POR LA UNIDAD CONSTRUCTIVA",line.substring(300,307));
			//c.addAttribute("LONGITUD DE FACHADA",line.substring(307,312));
			//c.addAttribute("CODIGO DE UNIDAD CONSTRUCTIVA MATRIZ",line.substring(409,413));

			c.addAttribute("source", "Catastro");
			c.addAttribute("addr:country","ES");
			
			return c; }
		case 14: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2)); 
			//c.addAttribute("CODIGO DE DELEGACION DEL MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
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
			c.addAttribute("ANO DE ANTIGUEDAD EFECTIVA EN CATASTRO",line.substring(78,82)); 
			c.setFechaAlta(Long.parseLong(line.substring(78,82)+"0101")); 
			c.setFechaBaja(fechaHasta);
			//c.addAttribute("INDICADOR DE LOCAL INTERIOR (S/N)",line.substring(82,83));
			//c.addAttribute("SUPERFICIE TOTAL DEL LOCAL A EFECTOS DE CATASTRO",line.substring(83,90));
			//c.addAttribute("SUPERFICIA DE PORCHES Y TERRAZAS DEL LOCAL",line.substring(90,97));
			//c.addAttribute("SUPERFICIE IMPUTABLE AL LOCAL SITUADA EN OTRAS PLANTAS",line.substring(97,104));
			//c.addAttribute("TIPOLOGIA CONSTRUCTIVA SEGUN NORMAS TECNICAS DE VALORACION",line.substring(104,109));
			//c.addAttribute("CODIGO DE MODALIDAD DE REPARTO",line.substring(111,114));

			c.addAttribute("source", "Catastro");
			c.addAttribute("addr:country","ES");
			
			return c;}
		case 15: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2));
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
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
			//c.addAttribute("NOMBRE DE PROVINCIA",line.substring(94,119));
			//c.addAttribute("CODIGO DE MUNICIPIO",line.substring(119,122));
			//c.addAttribute("CODIGO DE MUNICIPIO (INE) EXCLUIDO EL ULTIMO DIGITO DE CONTROL",line.substring(122,125));
			//c.addAttribute("NOMBRE DE MUNICIPIO",line.substring(125,165));
			c.addAttribute("addr:city",line.substring(125,165));
			//c.addAttribute("NOMBRE DE LA ENTIDAD MENOR EN CASO DE EXISTIR",line.substring(165,195));
			//c.addAttribute("CODIGO DE VIA PUBLICA",line.substring(195,200));
			//c.addAttribute("TIPO DE VIA O SIGLA PUBLICA",line.substring(200,205));
			//c.addAttribute("NOMBRE DE LA VIA PUBLICA",line.substring(205,230));
			c.addAttribute("addr:street",tipoViaParser(line.substring(200,205))+ " " +line.substring(205,230));
			//c.addAttribute("PRIMER NUMERO DE POLICIA",line.substring(230,234));
			//c.addAttribute("PRIMERA LETRA (CARACTER DE DUPLICADO)",line.substring(234,235));
			//c.addAttribute("SEGUNDO NUMERO DE POLICIA",line.substring(235,239));
			//c.addAttribute("SEGUNDA LETRA (CARACTER DE DUPLICADO)",line.substring(239,240));
			//c.addAttribute("KILOMETRO (3enteros y 2decimales)",line.substring(240,245));
			//c.addAttribute("BLOQUE",line.substring(245,249));
			//c.addAttribute("ESCALERA",line.substring(249,251));
			//c.addAttribute("PLANTA",line.substring(251,254));
			//c.addAttribute("PUERTA",line.substring(254,257));
			//c.addAttribute("TEXTO DE DIRECCION NO ESTRUCTURADA",line.substring(257,282));
			//c.addAttribute("CODIGO POSTAL",line.substring(282,287));
			c.addAttribute("addr:postcode",line.substring(282,287));
			//c.addAttribute("DISTRITO MUNICIPAL",line.substring(287,289));
			//c.addAttribute("CODIGO DEL MUNICIPIO DE ORIGEN EN CASO DE AGREGACION",line.substring(289,292));
			//c.addAttribute("CODIGO DE LA ZONA DE CONCENTRACION",line.substring(292,294));
			//c.addAttribute("CODIGO DE POLIGONO",line.substring(294,297));
			//c.addAttribute("CODIGO DE PARCELA",line.substring(297,302));
			//c.addAttribute("CODIGO DE PARAJE",line.substring(302,307));
			//c.addAttribute("NOMBRE DEL PARAJE",line.substring(307,337));
			//c.addAttribute("NUMERO DE ORDEN DEL INMUEBLE EN LA ESCRITURA DE DIVISION HORIZONTAL",line.substring(367,371));
			c.addAttribute("ANO DE ANTIGUEDAD DEL BIEN INMUEBLE",line.substring(371,375)); 
			c.setFechaAlta(Long.parseLong(line.substring(371,375)+"0101")); 
			c.setFechaBaja(fechaHasta);
			//c.addAttribute("CLAVE DE GRUPO DE LOS BIENES INMUEBLES DE CARAC ESPECIALES",line.substring(427,428));
			c.addAttribute(usoInmueblesParser(line.substring(427,428))[0],usoInmueblesParser(line.substring(427,428))[1]);
			//c.addAttribute("SUPERFICIE DEL ELEMENTO O ELEMENTOS CONSTRUCTIVOS ASOCIADOS AL INMUEBLE",line.substring(441,451));
			//c.addAttribute("SUPERFICIE ASOCIADA AL INMUEBLE",line.substring(451,461));
			//c.addAttribute("COEFICIENTE DE PROPIEDAD (3ent y 6deci)",line.substring(461,470));

			c.addAttribute("source", "Catastro");
			c.addAttribute("addr:country","ES");
			
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
			//c.addAttribute("BLOQUE REPETITIVO HASTA 15 VECES",line.substring(50,999)); //TODO ¿Necesario?

			c.addAttribute("source", "Catastro");
			c.addAttribute("addr:country","ES");
			
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
			//c.addAttribute("CALIFICACION CATASTRAL/CLASE DE CULTIVO",line.substring(65,67));
			//c.addAttribute("DENOMINACION DE LA CLASE DE CULTIVO",line.substring(67,107));
			//c.addAttribute("INTENSIDAD PRODUCTIVA",line.substring(107,109));
			//c.addAttribute("CODIGO DE MODALIDAD DE REPARTO",line.substring(126,129)); //TODO ¿Necesario?

			c.addAttribute("source", "Catastro");
			c.addAttribute("addr:country","ES");
			
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

	
	public static String tipoViaParser(String codigo){
		
		if (codigo.equals("CL   "))return "Calle";
		else if (codigo.equals("AL   "))return "Aldea/Alameda";
		else if (codigo.equals("AR   "))return "Area/Arrabal";
		else if (codigo.equals("AU   "))return "Autopista";
		else if (codigo.equals("AV   "))return "Avenida";
		else if (codigo.equals("AY   "))return "Arroyo";
		else if (codigo.equals("BJ   "))return "Bajada";
		else if (codigo.equals("BO   "))return "Barrio";
		else if (codigo.equals("BR   "))return "Barranco";
		else if (codigo.equals("CA   "))return "Cañada";
		else if (codigo.equals("CG   "))return "Colegio/Cigarral";
		else if (codigo.equals("CH   "))return "Chalet";
		else if (codigo.equals("CI   "))return "Cinturon";
		else if (codigo.equals("CJ   "))return "Calleja/Callejón";
		else if (codigo.equals("CM   "))return "Camino";
		else if (codigo.equals("CN   "))return "Colonia";
		else if (codigo.equals("CO   "))return "Concejo/Colegio";
		else if (codigo.equals("CP   "))return "Campa/Campo";
		else if (codigo.equals("CR   "))return "Carretera/Carrera";
		else if (codigo.equals("CS   "))return "Caserío";
		else if (codigo.equals("CT   "))return "Cuesta/Costanilla";
		else if (codigo.equals("CU   "))return "Conjunto";
		else if (codigo.equals("DE   "))return "Detrás";
		else if (codigo.equals("DP   "))return "Diputación";
		else if (codigo.equals("DS   "))return "Diseminados";
		else if (codigo.equals("ED   "))return "Edificios";
		else if (codigo.equals("EM   "))return "Extramuros";
		else if (codigo.equals("EN   "))return "Entrada, Ensanche";
		else if (codigo.equals("ER   "))return "Extrarradio";
		else if (codigo.equals("ES   "))return "Escalinata";
		else if (codigo.equals("EX   "))return "Explanada";
		else if (codigo.equals("FC   "))return "Ferrocarril";
		else if (codigo.equals("FN   "))return "Finca";
		else if (codigo.equals("GL   "))return "Glorieta";
		else if (codigo.equals("GR   "))return "Grupo";
		else if (codigo.equals("GV   "))return "Gran Vía";
		else if (codigo.equals("HT   "))return "Huerta/Huerto";
		else if (codigo.equals("JR   "))return "Jardines";
		else if (codigo.equals("LD   "))return "Lado/Ladera";
		else if (codigo.equals("LG   "))return "Lugar";
		else if (codigo.equals("MC   "))return "Mercado";
		else if (codigo.equals("ML   "))return "Muelle";
		else if (codigo.equals("MN   "))return "Municipio";
		else if (codigo.equals("MS   "))return "Masias";
		else if (codigo.equals("MT   "))return "Monte";
		else if (codigo.equals("MZ   "))return "Manzana";
		else if (codigo.equals("PB   "))return "Poblado";
		else if (codigo.equals("PD   "))return "Partida";
		else if (codigo.equals("PJ   "))return "Pasaje/Pasadizo";
		else if (codigo.equals("PL   "))return "Polígono";
		else if (codigo.equals("PM   "))return "Paramo";
		else if (codigo.equals("PQ   "))return "Parroquia/Parque";
		else if (codigo.equals("PR   "))return "Prolongación/Continuación";
		else if (codigo.equals("PS   "))return "Paseo";
		else if (codigo.equals("PT   "))return "Puente";
		else if (codigo.equals("PZ   "))return "Plaza";
		else if (codigo.equals("QT   "))return "Quinta";
		else if (codigo.equals("RB   "))return "Rambla";
		else if (codigo.equals("RC   "))return "Rincón/Rincona";
		else if (codigo.equals("RD   "))return "Ronda";
		else if (codigo.equals("RM   "))return "Ramal";
		else if (codigo.equals("RP   "))return "Rampa";
		else if (codigo.equals("RR   "))return "Riera";
		else if (codigo.equals("RU   "))return "Rua";
		else if (codigo.equals("SA   "))return "Salida";
		else if (codigo.equals("SD   "))return "Senda";
		else if (codigo.equals("SL   "))return "Solar";
		else if (codigo.equals("SN   "))return "Salón";
		else if (codigo.equals("SU   "))return "Subida";
		else if (codigo.equals("TN   "))return "Terrenos";
		else if (codigo.equals("TO   "))return "Torrente";
		else if (codigo.equals("TR   "))return "Travesía";
		else if (codigo.equals("UR   "))return "Urbanización";
		else if (codigo.equals("VR   "))return "Vereda";
		else if (codigo.equals("CY   "))return "Caleya";

		return codigo;
	}

	
	public static String[] usoInmueblesParser(String codigo){
		
		// TODO Mirar landuses
		switch (codigo.charAt(0)){
		case 'A':{
			String[] s = {"landuse","farmyard"};
			return s;}
		case 'V':{
			String[] s = {"landuse","residential"};
			return s;}
		case 'I':{
			String[] s = {"landuse","industrial"};
			return s;}
		case 'O':{
			String[] s = {"landuse","retail"};
			return s;}
		case 'C':{
			String[] s = {"landuse","commercial"};
			return s;}
		case 'K':{
			String[] s = {"landuse","recreation_ground"};
			return s;}
		case 'T':{
			String[] s = {"landuse","recreation_ground"};
			return s;}
		case 'G':{
			String[] s = {"landuse","retail"};
			return s;}
		case 'Y':{
			String[] s = {"landuse","health"};
			return s;}
		case 'E':{
			String[] s = {"landuse","recreation_ground"};
			return s;}
		case 'R':{
			String[] s = {"landuse","place_of_worship"};
			return s;}
		case 'M':{
			String[] s = {"landuse","allotments"};
			return s;}
		case 'P':{
			String[] s = {"landuse","singular"};
			return s;}
		case 'B':{
			String[] s = {"landuse","orchard"};
			return s;}
		case 'J':{
			String[] s = {"landuse","industrial"};
			return s;}
		case 'Z':{
			String[] s = {"landuse","orchard"};
			return s;}
		default:
			String[] s = {codigo,codigo};
			return s;}
	}
	
	
	public static List<String[]> ttggssParser(String ttggss){
		List<String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];
		
		// Divisiones administrativas
		if (ttggss.equals("010401")){
			s[0] = "admin_level"; s[1] ="2";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			s = new String[2];
			s[0] = "border_type"; s[1] ="nation";
			l.add(s);
			return l;}
		if (ttggss.equals("010301")){ 
			s[0] = "admin_level"; s[1] ="4";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			return l;}
		if (ttggss.equals("010201")){ 
			s[0] = "admin_level"; s[1] ="6";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			return l;}
		if (ttggss.equals("010101")){ 
			s[0] = "admin_level"; s[1] ="8";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			return l;}
		if (ttggss.equals("010102")){ 
			s[0] = "admin_level"; s[1] ="10";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			return l;}
		if (ttggss.equals("018507")){ 
			s[0] = "historic"; s[1] ="boundary_stone";
			l.add(s);
			return l;}
		if (ttggss.equals("018506")){ 
			s[0] = "historic"; s[1] ="boundary_stone";
			l.add(s);
			return l;}
		
		// Relieve
		if (ttggss.equals("028110")){ 
			s[0] = "man_made"; s[1] ="survey_point";
			l.add(s);
			return l;}
		if (ttggss.equals("028112")){ 
			s[0] = "man_made"; s[1] ="survey_point";
			l.add(s);
			return l;}
		
		// Hidrografia
		if (ttggss.equals("030102")){ 
			s[0] = "waterway"; s[1] ="stream";
			l.add(s);
			return l;}
		if (ttggss.equals("030202")){ 
			s[0] = "waterway"; s[1] ="stream";
			l.add(s);
			return l;}
		if (ttggss.equals("030302")){ 
			s[0] = "waterway"; s[1] ="canal";
			l.add(s);
			return l;}
		if (ttggss.equals("032301")){ 
			s[0] = "natural"; s[1] ="coastline";
			l.add(s);
			return l;}
		if (ttggss.equals("033301")){ 
			s[0] = "natural"; s[1] ="water";
			l.add(s);
			return l;}
		if (ttggss.equals("037101")){ 
			s[0] = "man_made"; s[1] ="water_well";
			l.add(s);
			return l;}
		if (ttggss.equals("037102")){ 
			s[0] = "natural"; s[1] ="spring";
			l.add(s);
			return l;}
		if (ttggss.equals("037107")){ 
			s[0] = "waterway"; s[1] ="dam";
			l.add(s);
			return l;}
		
		// Vias de comunicacion
		if (ttggss.equals("060102")){ 
			//s[0] = "natural"; s[1] ="coastline";
			//l.add(s);
			return l;}
		if (ttggss.equals("060104")){ 
			s[0] = "highway"; s[1] ="motorway";
			l.add(s);
			return l;}
		if (ttggss.equals("060202")){ 
			//s[0] = "man_made"; s[1] ="water_well";
			//l.add(s);
			return l;}
		if (ttggss.equals("060204")){ 
			s[0] = "highway"; s[1] ="primary";
			l.add(s);
			return l;}
		if (ttggss.equals("060402")){ 
			//s[0] = "waterway"; s[1] ="dam";
			//l.add(s);
			return l;}
		if (ttggss.equals("060404")){ 
			s[0] = "highway"; s[1] ="tertiary";
			l.add(s);
			return l;}
		if (ttggss.equals("060109")){ 
			s[0] = "railway"; s[1] ="funicular";
			l.add(s);
			return l;}
		if (ttggss.equals("061104")){ 
			s[0] = "railway"; s[1] ="rail";
			l.add(s);
			return l;}
		if (ttggss.equals("067121")){ 
			s[0] = "bridge"; s[1] ="yes";
			l.add(s);
			return l;}
		if (ttggss.equals("068401")){ 
			//s[0] = "waterway"; s[1] ="dam";
			//l.add(s);
			return l;}
		
		// Red geodesica y topografica
		if (ttggss.equals("108100")){ 
			s[0] = "man_made"; s[1] ="survey_point";
			l.add(s);
			return l;}
		if (ttggss.equals("108101")){ 
			s[0] = "man_made"; s[1] ="survey_point";
			l.add(s);
			return l;}
		if (ttggss.equals("108104")){ 
			s[0] = "man_made"; s[1] ="survey_point";
			l.add(s);
			return l;}
		
		// Delimitaciones catastrales urbanisticas y estadisticas
		if (ttggss.equals("111101")){ 
			s[0] = "admin_level"; s[1] ="10";
			l.add(s);
			return l;}
		if (ttggss.equals("111000")){ 
			s[0] = "admin_level"; s[1] ="12";
			l.add(s);
			return l;}
		if (ttggss.equals("111200")){ 
			s[0] = "admin_level"; s[1] ="14";
			l.add(s);
			return l;}
		if (ttggss.equals("111300")){ 
			s[0] = "admin_level"; s[1] ="10";
			l.add(s);
			return l;}
		if (ttggss.equals("115101")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("115000")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("115200")){ 
			//s[0] = "admin_level"; s[1] ="14";
			//l.add(s);
			return l;}
		if (ttggss.equals("115300")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		
		// Rustica (Compatibilidad 2006 hacia atras)
		if (ttggss.equals("120100")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("120200")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("120500")){ 
			//s[0] = "admin_level"; s[1] ="14";
			//l.add(s);
			return l;}
		if (ttggss.equals("120180")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("120280")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("120580")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("125101")){ 
			//s[0] = "admin_level"; s[1] ="14";
			//l.add(s);
			return l;}
		if (ttggss.equals("125201")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("125501")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("125510")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}

		// Rustica y Urbana
		if (ttggss.equals("130100")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("130200")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("130500")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("135101")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("135201")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("135501")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("135510")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}

		// Urbana (Compatibilidad 2006 hacia atras)
		if (ttggss.equals("140100")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("140190")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("140200")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("140290")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("140500")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("140590")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("145101")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("145201")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("145501")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("145510")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		
		// Infraestructura/Mobiliario
		if (ttggss.equals("160101")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("160131")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("160132")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("160201")){ 
			s[0] = "power"; s[1] ="line";
			l.add(s);
			return l;}
		if (ttggss.equals("160202")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("160300")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("161101")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("167103")){ 
			s[0] = "historic"; s[1] ="monument";
			l.add(s);
			return l;}
		if (ttggss.equals("167104")){ 
			s[0] = "highway"; s[1] ="steps";
			l.add(s);
			return l;}
		if (ttggss.equals("167106")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("167111")){ 
			s[0] = "power"; s[1] ="sub_station";
			l.add(s);
			return l;}
		if (ttggss.equals("167167")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("167201")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("168100")){ 
			//s[0] = "power"; s[1] ="sub_station";
			//l.add(s);
			return l;}
		if (ttggss.equals("168103")){ 
			s[0] = "historic"; s[1] ="monument";
			l.add(s);
			return l;}
		if (ttggss.equals("168113")){ 
			s[0] = "power"; s[1] ="pole";
			l.add(s);
			return l;}
		if (ttggss.equals("168116")){ 
			s[0] = "highway"; s[1] ="street_lamp";
			l.add(s);
			return l;}
		if (ttggss.equals("168153")){ 
			s[0] = "natural"; s[1] ="tree";
			l.add(s);
			return l;}
		if (ttggss.equals("168168")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		
		
	return null;	
	}
}
