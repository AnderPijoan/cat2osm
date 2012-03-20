import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.geometry.jts.JTSFactoryFinder;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;


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
	 * @param shapes lista de shapes que han coincidido con la refCat para buscar en ella las subparcelas
	 * @param subparce Codigo de subparcela para obtener la que corresponda
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

	/** Busca en la lista de shapes los que sean parcelas
	 * @param shapes lista de shapes
	 * @return List<Shape> lista de shapes que coinciden  
	 */
	private static List<Shape> buscarParce(List<Shape> shapes){
		List<Shape> shapeList = new ArrayList<Shape>();

		for(Shape shape : shapes) 
			if (shape instanceof ShapeParcela)
				shapeList.add(shape);

		return shapeList;
	}


	/** Los elementos textuales traen informacion con los numeros de portal pero sin datos de la parcela ni unidos a ellas
	 * Con esto, sabiendo el numero de portal buscamos la parcela mas cercana con ese numero y le pasamos los tags al elemento
	 * textual que es un punto
	 * @param shapes Lista de shapes original
	 * @return lista de shapes con los tags modificados
	 */
	@SuppressWarnings("unchecked")
	public List<Shape> calcularPortales(List<Shape> shapes){

		// Creamos la factoria para crear objetos de GeoTools
		com.vividsolutions.jts.geom.GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(null);

		// Creamos una cache para meter las geometrias de las parcelas y una lista con los numeros de policia de esas parcelas
		final SpatialIndex index = new STRtree();

		for (Shape shapePar : shapes)

			// Si es un shape de parcela y tiene geometria
			if (shapePar instanceof ShapeParcela &&  shapePar.getPoligons() != null && !shapePar.getPoligons().isEmpty()){

				// Cogemos la geometria exterior de la parcela
				Geometry geom = (LineString) shapePar.getPoligons().get(0);

				// Para hacer la query es necesario en Envelope
				Envelope env = geom.getEnvelopeInternal();

				// Si existe
				if (!env.isNull())
					index.insert(env, new LocationIndexedLine(geom));
			}


		// Para cada shape de elemtex que tengamos, que son los portales,
		// metemos su coordenada en una lista
		for (Shape shapeTex : shapes)
			if (shapeTex instanceof ShapeElemtex){

				com.vividsolutions.jts.geom.Point point = gf.createPoint(shapeTex.getCoor());

				double minDist = 0.00008; // Distancia minima ~ 80 metros

				// Creamos el punto de busqueda con la coordenada del punto y la expandimos
				// en la distancia minima para obtener
				// una linea de desplazamiento para tocar la parcela
				Envelope search = new Envelope(point.getCoordinate());
				search.expandBy(minDist);

				// Hacemos la query
				List<LocationIndexedLine> lines = index.query(search);

				// Cada linea que nos devuelve representa el desplazamiento
				// que hay que darle a la coordenada para que se situe sobre la linea de la
				// geometria de la parcela
				for (LocationIndexedLine line : lines) {
					LinearLocation here = line.project(point.getCoordinate());
					Coordinate fixedCoor = line.extractPoint(here);
					double dist = fixedCoor.distance(point.getCoordinate());

					if (dist < minDist) {
						// Acualizamos la variable minDist
						minDist = dist;

						// Actualizamos la coordenada en el shape
						((ShapeElemtex) shapeTex).setCoor(fixedCoor);

						// Y Actualizamos la coordenada en el nodeOsm
						if (shapeTex.getNodesIds(0) != null && !shapeTex.getNodesIds(0).isEmpty()){
							NodeOsm node = ((NodeOsm) utils.getKeyFromValue((Map<Object, Long>) ((Object)utils.getTotalNodes()), shapeTex.getNodesIds(0).get(0)));
							node.setCoor(fixedCoor);
						}
					}
				}
			}
		return shapes;
	}

	/** Recorre todos los shapes despues de haber leido todos los usos del .CAT
	 * En las shapes de parcela se han almacenado los usos/destinos y sus areas y se cogera
	 * el que mas area tenga
	 * @param shapes
	 * @return
	 */
	public List<Shape> calcularUsos(List<Shape> shapes){
		
		for (Shape shape : shapes)
			if (shape != (null) && shape instanceof ShapeParcela){
					RelationOsm r = ((RelationOsm) utils.getKeyFromValue((Map<Object, Long>) ((Object) utils.getTotalRelations()), shape.getRelationId()));
					if (r != null)
					r.addTags(destinoParser(((ShapeParcela)shape).getUsoMasArea()));
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

		System.out.print("Progreso = 0%. Estimando tiempo restante...\r");
		int bar = 0;
		long timeElapsed = 0;
		float size = shapes.size();
		long time = System.currentTimeMillis();
		WayOsm way1 = null;
		WayOsm way2 = null;
		WayOsm removeWay = null;
		Map<WayOsm, Long> ways = utils.getTotalWays();

		for (Shape shape : shapes){

			int progress = (int) ((shapes.indexOf(shape)/size)*100);
			if (bar != progress){
				timeElapsed = (timeElapsed+(100-progress)*(System.currentTimeMillis()-time)/1000)/2;
				long hor = Math.round((timeElapsed/3600));
				long min = Math.round((timeElapsed-3600*hor)/60);
				long seg = Math.round((timeElapsed-3600*hor-60*min));

				System.out.print("Progreso = "+progress+"%. Tiempo restante estimado = "+hor+" horas, "+min+" minutos, "+seg+" segundos.\r");
				bar = progress;
				time = System.currentTimeMillis();
			}

			for (int x = 0; shape != null && shape.getPoligons() != null && !shape.getPoligons().isEmpty() && x < shape.getPoligons().size(); x++)

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
										if (s != null && s.getShapeId() == shapeId)
											for (int pos = 0; pos < s.getPoligons().size(); pos++)
												s.deleteWay(pos,wayId);
								}
							}
						}

					}catch(Exception e) {System.out.println("["+new Timestamp(new Date().getTime())+"] Error simplificando vía. " + e.getMessage());}
				}
		}

		return shapes;
	}


	/** Escribe el osm con todos los nodos (Del archivo totalNodes, sin orden)
	 * @param tF Ruta donde escribir este archivo, sera temporal
	 * @throws IOException
	 */
	@SuppressWarnings("rawtypes")
	public void printNodes(Map <NodeOsm, Long> nodes) throws IOException{

		File dir = new File(Config.get("ResultPath"));
		if (!dir.exists()) 
		{
			try                { dir.mkdirs(); }
			catch (Exception e){ e.printStackTrace(); }
		}

		// Archivo temporal para escribir los nodos
		String fstreamNodes = Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempNodes.osm";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outNodes = new BufferedWriter(new OutputStreamWriter (new FileOutputStream(fstreamNodes), "UTF-8"));

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

		File dir = new File(Config.get("ResultPath")); 
		if (!dir.exists()) 
		{
			try                { dir.mkdirs(); }
			catch (Exception e){ e.printStackTrace(); }
		}

		// Archivo temporal para escribir los nodos
		String fstreamNodes = Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempNodes.osm";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outNodes = new BufferedWriter(new OutputStreamWriter (new FileOutputStream(fstreamNodes), "UTF-8"));
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

		File dir = new File(Config.get("ResultPath"));
		if (!dir.exists()) 
		{
			try                { dir.mkdirs(); }
			catch (Exception e){ e.printStackTrace(); }
		}

		// Archivo temporal para escribir los ways
		String fstreamWays = Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempWays.osm";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outWays = new BufferedWriter(new OutputStreamWriter (new FileOutputStream(fstreamWays), "UTF-8"));

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

		File dir = new File(Config.get("ResultPath")); 
		if (!dir.exists()) 
		{
			try                { dir.mkdirs(); }
			catch (Exception e){ e.printStackTrace(); }
		}

		// Archivo temporal para escribir los ways
		String fstreamWays = Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempWays.osm";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outWays = new BufferedWriter(new OutputStreamWriter (new FileOutputStream(fstreamWays), "UTF-8"));

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

		File dir = new File(Config.get("ResultPath"));
		if (!dir.exists()) 
		{
			try                { dir.mkdirs(); }
			catch (Exception e){ e.printStackTrace(); }
		}

		// Archivo temporal para escribir los ways
		String fstreamRelations = Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempRelations.osm";
		// Indicamos que el archivo se codifique en UTF-8
		BufferedWriter outRelations = new BufferedWriter(new OutputStreamWriter (new FileOutputStream(fstreamRelations), "UTF-8"));

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

		if (new File(path + "/"+ Config.get("ResultFileName") + "tempNodes.osm").exists())
		{
			BufferedReader inNodes = new BufferedReader(new FileReader(path + "/"+ Config.get("ResultFileName") + "tempNodes.osm"));
			while ((str = inNodes.readLine()) != null){
				outOsm.write(str);
				outOsm.newLine();

			}
			inNodes.close();
		}

		if (new File(path + "/"+ Config.get("ResultFileName") + "tempWays.osm").exists())
		{
			BufferedReader inWays = new BufferedReader(new FileReader(path + "/"+ Config.get("ResultFileName") + "tempWays.osm"));
			while ((str = inWays.readLine()) != null){
				outOsm.write(str);
				outOsm.newLine();
			}
			inWays.close();
		}

		if (new File(path + "/"+ Config.get("ResultFileName") + "tempRelations.osm").exists())
		{
			BufferedReader inRelations = new BufferedReader(new FileReader(path + "/"+ Config.get("ResultFileName") + "tempRelations.osm"));
			while ((str = inRelations.readLine()) != null){
				outOsm.write(str);
				outOsm.newLine();
			}
			inRelations.close();
		}
		outOsm.write("</osm>");
		outOsm.newLine();

		outOsm.close();

		boolean borrado = true;
		borrado = borrado && (new File(path+ "/" + Config.get("ResultFileName") + "tempNodes.osm")).delete();
		borrado = borrado && (new File(path + "/" + Config.get("ResultFileName") + "tempWays.osm")).delete();
		borrado = borrado && (new File(path + "/" + Config.get("ResultFileName") + "tempRelations.osm")).delete();

		if (!borrado)
			System.out.println("["+new Timestamp(new Date().getTime())+"] NO se pudo borrar alguno de los archivos temporales." +
					" Estos estaran en la carpeta "+ path +".");

	}


	/** Lee linea a linea el archivo cat, coge los shapes q coincidan 
	 * con esa referencia catastral y les anade los tags de los registros .cat
	 * @param cat Archivo cat del que lee linea a linea
	 * @param List<Shape> Lista de los elementos shp parseados ya en memoria
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void catParser(File cat, List<Shape> shapesTotales) throws IOException{
		
		BufferedReader bufRdr = 
				new BufferedReader(new InputStreamReader(new FileInputStream(cat), "ISO-8859-15"));
		String line = null; // Para cada linea leida del archivo .cat

		int tipoRegistro = Integer.parseInt(Config.get("TipoRegistro"));

		// Lectura del archivo .cat
		while((line = bufRdr.readLine()) != null)
		{
			try {
				Cat c = catLineParser(line);
				
				if ( (c.getTipoRegistro() == tipoRegistro || tipoRegistro == 0)){

					// Obtenemos los shape que coinciden con la referencia catastral de la linea leida
					List <Shape> matches = buscarRefCat(shapesTotales, c.getRefCatastral());

					if (matches != null)
					switch (c.getTipoRegistro()){
					
					// El registro 11 solo se tiene que asignar a parcelas
					case 11:
						matches = buscarParce(matches);
						break;
					
					// El registro 15 es para bienes inmuebles pero como no hay forma de relacionarlo
					// se ha hecho que la parcela acumule todos los destinos y al final elija el que mas
					// area tiene. Ese dato solo se almacena en el shape, luego habra que llamar al metodo
					// que calcule los destinos para pasarlos a las relaciones.
					case 14:
						matches = buscarParce(matches);
						for (Shape match : matches)
								((ShapeParcela) match).addUso(c.getUsoDestino(), c.getArea());
						break;
						
					// El registro 15 es para bienes inmuebles pero como no hay forma de relacionarlo
					// se ha hecho que la parcela acumule todos los destinos y al final elija el que mas
					// area tiene. Ese dato solo se almacena en el shape, luego habra que llamar al metodo
					// que calcule los destinos para pasarlos a las relaciones.
					case 15:
						matches = buscarParce(matches);
						for (Shape match : matches)
								((ShapeParcela) match).addUso(c.getUsoDestino(), c.getArea());
						break;
					
					// Para los tipos de registro de subparcelas, buscamos la subparcela concreta para
					// anadirle los atributos
					case 17:
						matches = buscarSubparce(matches, c.getSubparce());
						break;
							
					}

					// Puede que no haya shapes para esa refCatastral
					if (matches != null)
					for (Shape shape : matches)
						if (shape != (null)){
							RelationOsm r = ((RelationOsm) utils.getKeyFromValue((Map<Object, Long>) ((Object) utils.getTotalRelations()), shape.getRelationId()));
							if (r != null)
							r.addTags(c.getAttributes());
							
							// Ponemos a la relacion su fecha de construccion
							r.setFechaConstru(c.getFechaConstru());
						}
				}
			}
			catch(Exception e){System.out.println("["+new Timestamp(new Date().getTime())+"] Error leyendo linea del archivo. " + e.getMessage());}
		}
	}

	
	/** Lee linea a linea el archivo cat, coge los registros 14 que son los que tienen uso
	 * de inmuebles y con el punto X e Y del centroide de la parcela que coincide con su referencia
	 * catastral crea nodos con los usos
	 * de los bienes inmuebles
	 * @param cat Archivo cat del que lee linea a linea
	 * @param List<Shape> Lista de los elementos shp parseados ya en memoria
	 * @param t solo sirve para diferenciar del otro metodo
	 * @throws IOException
	 */
	public void catUsosParser(File cat, List<Shape> shapesTotales) throws IOException{

		BufferedReader bufRdr  = new BufferedReader(new FileReader(cat));
		String line = null; // Para cada linea leida del archivo .cat

		// Lectura del archivo .cat
		while((line = bufRdr.readLine()) != null)
		{
			try {
				
				// Si es registro 14
				if (esNumero(line.substring(0,2)) && line.substring(0,2).equals("14")){
					
					// Cogemos las geometrias con esa referencia catastral.
					List<Shape> matches = buscarRefCat(shapesTotales, line.substring(30,44));
					
					// Puede que no haya shapes para esa refCatastral
					if (matches != null)
					for (Shape shape : matches)
						if (shape != (null) &&  shape.getPoligons() != null && !shape.getPoligons().isEmpty()){

							// Cogemos la geometria exterior de la parcela
							Geometry geom = (LineString) shape.getPoligons().get(0);
							
							// Creamos los tags que tendra el nodo
							List<String[]> tags = new ArrayList<String[]>();
							
							// Metemos los tags de uso de inmuebles con el numero de inmueble por delante
							tags.addAll(destinoParser(line.substring(70,73).trim()));
							for (String[] tag : tags){
								tag[0] = tag[0].replace("*", "");
							}
							
							// Anadimos la referencia catastral
							tags.add(new String[] {"catastro:ref", line.substring(30,44) + line.substring(44,48)});
							
							tags.add(new String[] {"addr:floor", line.substring(64,67).trim() });
							
							// Creamos el nodo en la lista de nodos de utils, pero no se lo anadimos al shape sino luego 
							// lo borraria ya que eliminamos todos los nodos que sean de geometrias de shape
							// Ademas a las coordenadas del nodo les sumamos un pequeno valor en funcion del numero de inmueble
							// para que cree nodos distintos en el mismo punto. De ser la misma coordenada reutilizaria el nodo
							float r = (float) (Integer.parseInt(line.substring(44,48))*0.0000002);
							utils.getNodeId(new Coordinate(geom.getCentroid().getX()+r,geom.getCentroid().getY()), tags);
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

		int tipoRegistro = Integer.parseInt(Config.get("tipoRegistro"));

		while((line = bufRdr.readLine()) != null)
		{
			Cat c = catLineParser(line);
			// No todos los tipos de registros de catastro tienen FechaAlta y FechaBaja
			// Los que no tienen, pasan el filtro
			if ((c.getTipoRegistro() == tipoRegistro || tipoRegistro == 0) )
				l.add(c);
		}
		bufRdr.close();
		return l;
	}


	/** De la lista de shapes con la misma referencia catastral devuelve la mas actual
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

		Cat c = null;

		if (esNumero(line.substring(0,2)))
			c = new Cat(Integer.parseInt(line.substring(0,2)));
		else
			c = new Cat(0);

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
			if (!eliminarCerosString(line.substring(295,305)).equals(eliminarCerosString(line.substring(305,312))))
				c.addAttribute("catastro:surface:built",eliminarCerosString(line.substring(305,312)));
			//c.addAttribute("SUPERFICIE CONSTRUIDA SOBRE RASANTE",line.substring(312,319));
			if (!eliminarCerosString(line.substring(295,305)).equals(eliminarCerosString(line.substring(312,319))))
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
			if (esNumero(line.substring(44,48)) && Integer.parseInt(line.substring(44,48)) != 0)
				c.setNumOrdenConstru(Integer.parseInt(line.substring(44,48)));
			else
				c.setSubparce(line.substring(44,48));
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
			c.setFechaConstru(Long.parseLong(line.substring(295,299)+"0101"));
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
			if (esNumero(line.substring(44,48)) && Integer.parseInt(line.substring(44,48)) != 0)
				c.setNumOrdenConstru(Integer.parseInt(line.substring(44,48)));
			else
				c.setSubparce(line.substring(44,48));
			//c.addAttribute("NUMERO DE ORDEN DEL BIEN INMUEBLE FISCAL",line.substring(50,54));
			//c.addAttribute("CODIGO DE LA UNIDAD CONSTRUCTIVA A LA QUE ESTA ASOCIADO EL LOCAL",line.substring(54,58));
			//c.addAttribute("BLOQUE",line.substring(58,62));
			//c.addAttribute("ESCALERA",line.substring(62,64));
			//c.addAttribute("PLANTA",line.substring(64,67));
			//c.addAttribute("PUERTA",line.substring(67,70));
			//c.addAttribute("CODIGO DE DESTINO SEGUN CODIFICACION DGC",line.substring(70,73));
			c.setUsoDestino(line.substring(70,73).trim());
			//c.addAttribute("INDICADOR DEL TIPO DE REFORMA O REHABILITACION",line.substring(73,74));
			//c.addAttribute("ANO DE REFORMA EN CASO DE EXISTIR",line.substring(74,78));
			//c.addAttribute("ANO DE ANTIGUEDAD EFECTIVA EN CATASTRO",line.substring(78,82)); 
			//c.addAttribute("INDICADOR DE LOCAL INTERIOR (S/N)",line.substring(82,83));
			//c.addAttribute("SUPERFICIE TOTAL DEL LOCAL A EFECTOS DE CATASTRO",line.substring(83,90));
			if (esNumero(line.substring(83,90).trim()))
				c.setArea(Double.parseDouble(line.substring(83,90).trim()));
			else
				c.setArea((double) 10);
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
			if (esNumero(line.substring(44,48)) && Integer.parseInt(line.substring(44,48)) != 0)
				c.setNumOrdenConstru(Integer.parseInt(line.substring(44,48)));
			else
				c.setSubparce(line.substring(44,48));
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
			//c.addAttribute("CLAVE DE GRUPO DE LOS BIENES INMUEBLES DE CARAC ESPECIALES",line.substring(427,428));
			c.setUsoDestino(line.substring(427,428).trim());
			//c.addAttribute("SUPERFICIE DEL ELEMENTO O ELEMENTOS CONSTRUCTIVOS ASOCIADOS AL INMUEBLE",line.substring(441,451));
			if (esNumero(line.substring(441,451).trim()))
				c.setArea(Double.parseDouble(line.substring(441,451).trim()));
			else
				c.setArea((double) 10);
			//c.addAttribute("SUPERFICIE ASOCIADA AL INMUEBLE",line.substring(451,461));
			//c.addAttribute("COEFICIENTE DE PROPIEDAD (3ent y 6deci)",line.substring(461,470));


			return c;}
		
		case 16: {

			//c.addAttribute("TIPO DE REGISTRO",line.substring(0,2));
			//c.addAttribute("CODIGO DE DELEGACION MEH",line.substring(23,25));
			//c.addAttribute("CODIGO DEL MUNICIPIO",line.substring(25,28));
			//c.addAttribute("PARCELA CATASTRAL",line.substring(30,44)); 
			c.setRefCatastral(line.substring(30,44));
			//c.addAttribute("NUMERO DE ORDEN DEL ELEMENTO CUYO VALOR SE REPARTE",line.substring(44,48));
			if (esNumero(line.substring(44,48)) && Integer.parseInt(line.substring(44,48)) != 0)
				c.setNumOrdenConstru(Integer.parseInt(line.substring(44,48)));
			else
				c.setSubparce(line.substring(44,48));
			//c.addAttribute("CALIFICACION CATASTRAL DE LA SUBPARCELA",line.substring(48,50));
			//c.addAttribute("BLOQUE REPETITIVO HASTA 15 VECES",line.substring(50,999));


			return c;}
		
		case 17: {
			
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
			if (esNumero(line.substring(55,65).trim()))
				c.setArea(Double.parseDouble(line.substring(55,65).trim()));
			else
				c.setArea((double) 10);
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
				temp = "";
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

		switch(codigo){
		case "CL":return "Calle";
		case "AL":return "Aldea/Alameda";
		case "AR":return "Area/Arrabal";
		case "AU":return "Autopista";
		case "AV":return "Avenida";
		case "AY":return "Arroyo";
		case "BJ":return "Bajada";
		case "BO":return "Barrio";
		case "BR":return "Barranco";
		case "CA":return "Cañada";
		case "CG":return "Colegio/Cigarral";
		case "CH":return "Chalet";
		case "CI":return "Cinturon";
		case "CJ":return "Calleja/Callejón";
		case "CM":return "Camino";
		case "CN":return "Colonia";
		case "CO":return "Concejo/Colegio";
		case "CP":return "Campa/Campo";
		case "CR":return "Carretera/Carrera";
		case "CS":return "Caserío";
		case "CT":return "Cuesta/Costanilla";
		case "CU":return "Conjunto";
		case "DE":return "Detrás";
		case "DP":return "Diputación";
		case "DS":return "Diseminados";
		case "ED":return "Edificios";
		case "EM":return "Extramuros";
		case "EN":return "Entrada, Ensanche";
		case "ER":return "Extrarradio";
		case "ES":return "Escalinata";
		case "EX":return "Explanada";
		case "FC":return "Ferrocarril";
		case "FN":return "Finca";
		case "GL":return "Glorieta";
		case "GR":return "Grupo";
		case "GV":return "Gran Vía";
		case "HT":return "Huerta/Huerto";
		case "JR":return "Jardines";
		case "LD":return "Lado/Ladera";
		case "LG":return "Lugar";
		case "MC":return "Mercado";
		case "ML":return "Muelle";
		case "MN":return "Municipio";
		case "MS":return "Masias";
		case "MT":return "Monte";
		case "MZ":return "Manzana";
		case "PB":return "Poblado";
		case "PD":return "Partida";
		case "PJ":return "Pasaje/Pasadizo";
		case "PL":return "Polígono";
		case "PM":return "Paramo";
		case "PQ":return "Parroquia/Parque";
		case "PR":return "Prolongación/Continuación";
		case "PS":return "Paseo";
		case "PT":return "Puente";
		case "PZ":return "Plaza";
		case "QT":return "Quinta";
		case "RB":return "Rambla";
		case "RC":return "Rincón/Rincona";
		case "RD":return "Ronda";
		case "RM":return "Ramal";
		case "RP":return "Rampa";
		case "RR":return "Riera";
		case "RU":return "Rua";
		case "SA":return "Salida";
		case "SD":return "Senda";
		case "SL":return "Solar";
		case "SN":return "Salón";
		case "SU":return "Subida";
		case "TN":return "Terrenos";
		case "TO":return "Torrente";
		case "TR":return "Travesía";
		case "UR":return "Urbanización";
		case "VR":return "Vereda";
		case "CY":return "Caleya";
		}

		return codigo;
	}


	/** Traduce el codigo de uso de inmueble que traen los registros 15 .cat a sus tags en OSM
	 * Estos registros son para cada unidad constructiva dentro de la misma parcela. Estas unidades
	 * todas tienen la misma refCat por lo que no se puede saber con precision a cual pertenece. 
	 * Es por eso que cogemos solamente los tags mas representativos.
	 * Como los cat se leen despues de los shapefiles, hay tags que los shapefiles traen
	 * mas concretos, que esto los machacaria. Es por eso que si al tag le ponemos un '*'
	 * por delante cuando son tipos genericos sin especificaciones,
	 * comprueba que no exista ese tag antes de meterlo. En caso de existir
	 * dejaria el que ya estaba.
	 * @param codigo Codigo de uso de inmueble
	 * @return Lista de tags que genera
	 */
	public static List<String[]> usoInmueblesParser(String codigo){
		List<String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];

		switch (codigo){

		case "A":
		case "B":
			return l;

		case "C":
		case "D":
			s[0] = "*landuse"; s[1] = "retail";
			l.add(s);
			return l;

		case "E":
		case "F":
			s[0] = "landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] = "recreation_type"; s[1] = "culture";
			l.add(s);
			return l;

		case "G":
		case "H":
			return l;

		case "I":
		case "J":
			s[0] = "*landuse"; s[1] = "industrial";
			l.add(s);
			return l;

		case "K":
		case "L":
			s[0] = "*landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] = "recreation_type"; s[1] = "sports";
			l.add(s);
			return l;

		case "M":
		case "N":
			s[0] = "*landuse"; s[1] = "greenfield";
			l.add(s);
			return l;

		case "O":
		case "X":
			s[0] = "*landuse"; s[1] = "commercial";
			l.add(s);
			return l;

		case "P":
		case "Q":
			return l;

		case "R":
		case "S":
			s[0] = "amenity"; s[1] = "place_of_worship";
			l.add(s);
			return l;

		case "T":
		case "U":
			s[0] = "landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] = "recreation_type"; s[1] = "entertainment";
			l.add(s);
			return l;

		case "V":
		case "W":
			s[0] = "*landuse"; s[1] = "residential";
			l.add(s);
			return l;

		case "Y":
		case "Z":
			return l;

		default:
			s[0] = "fixme"; s[1] = "Documentar nuevo codificación de los usos de los bienes inmuebles en catastro código="+ codigo +" en http://wiki.openstreetmap.org/w/index.php?title=Traduccion_metadatos_catastro_a_map_features";
			l.add(s);
			return l;

		}
	}



	/** Traduce el codigo de destino de los registros 14  que traen los .cat a sus tags en OSM
	 * Como los cat se leen despues de los shapefiles, hay tags que los shapefiles traen
	 * mas concretos, que esto los machacaria. Es por eso que si al tag le ponemos un '*'
	 * por delante cuando son tipos genericos sin especificaciones,
	 * comprueba que no exista ese tag antes de meterlo. En caso de existir
	 * dejaria el que ya estaba.
	 * @param codigo Codigo de uso de inmueble
	 * @return Lista de tags que genera
	 */
	public static List<String[]> destinoParser(String codigo){
		List<String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];

		switch (codigo){
		case "A":
		case "B":
			s[0] = "*landuse"; s[1] ="farmyard";
			l.add(s);
			return l;
			
		case "AAL":
		case "BAL":
			s[0] = "building"; s[1] = "warehouse";
			l.add(s);
			return l;

		case "AAP":
		case "BAP":
			s[0] = "amenity"; s[1] = "parking";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Comprobar que sea parking publico o al aire libre, en caso de no serlo deberia ser building=garage o landuse=garages";
			l.add(s);
			return l;

		case"ACR":
		case"BCR":
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "ACT":
		case "BCT":
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			s = new String[2];
			s[0] = "power"; s[1] = "sub_station";
			l.add(s);
			return l;

		case "AES":
		case "BES":
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			s = new String[2];
			s[0] = "public_transport"; s[1] = "station";
			l.add(s);
			return l;

		case "AIG":
		case "BIG":
			s[0] = "building"; s[1] = "livestock";
			l.add(s);
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			return l;

		case "C":
		case "D":
			s[0] = "*landuse"; s[1] = "retail";
			l.add(s);
			return l;

		case "CAT":
		case "DAT":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "shop"; s[1] = "car";
			l.add(s);
			return l;

		case "CBZ":
		case "DBZ":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "shop"; s[1] = "electronics";
			l.add(s);
			return l;

		case "CCE":
		case "DCE":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "*";
			l.add(s);
			return l;

		case "CCL":
		case "DCL":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "shop"; s[1] = "shoes";
			l.add(s);
			return l;

		case "CCR":
		case "DCR":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "shop"; s[1] = "butcher";
			l.add(s);
			return l;

		case "CDM":
		case "DDM":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "*";
			l.add(s);
			return l;

		case "CDR":
		case "DDR":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "shop"; s[1] = "chemist";
			l.add(s);
			return l;

		case "CFN":
		case "DFN":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "amenity"; s[1] = "bank";
			l.add(s);
			return l;

		case "CFR":
		case "DFR":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "shop"; s[1] = "pharmacy";
			l.add(s);
			return l;

		case "CFT":
		case "DFT":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "craft"; s[1] = "plumber";
			l.add(s);
			return l;

		case "CGL":
		case "DGL":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "amenity"; s[1] = "marketplace";
			l.add(s);
			return l;

		case "CIM":
		case "DIM":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "shop"; s[1] = "copyshop";
			l.add(s);
			return l;

		case "CJY":
		case "DJY":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "shop"; s[1] = "jewelry";
			l.add(s);
			return l;

		case "CLB":
		case "DLB":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "shop"; s[1] = "books";
			l.add(s);
			return l;

		case "CMB":
		case "DMB":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "shop"; s[1] = "furniture";
			l.add(s);
			return l;

		case "CPA":
		case "DPA":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "*";
			l.add(s);
			return l;

		case "CPR":
		case "DPR":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "shop"; s[1] = "chemist";
			l.add(s);
			return l;

		case "CRL":
		case "DRL":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "craft"; s[1] = "watchmaker";
			l.add(s);
			return l;

		case "CSP":
		case "DSP":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "shop"; s[1] = "clothes";
			l.add(s);
			return l;

		case "CTJ":
		case "DTJ":
			s[0] = "landuse"; s[1] = "retail";
			l.add(s);
			s = new String[2];
			s[0] = "shop"; s[1] = "supermarket";
			l.add(s);
			return l;

		case "E":
		case "F":
			s[0] = "*amenity"; s[1] = "school";
			l.add(s);
			return l;

		case "EBL":
		case "FBL":
			s[0] = "amenity"; s[1] = "library";
			l.add(s);
			return l;

		case "EBS":
		case "FBS":
			s[0] = "amenity"; s[1] = "school";
			l.add(s);
			s = new String[2];
			s[0] = "isced:level"; s[1] = "1;2";
			l.add(s);
			return l;

		case "ECL":
		case "FCL":
			s[0] = "amenity"; s[1] = "comunity_centre";
			l.add(s);
			return l;

		case "EIN":
		case "FIN":
			s[0] = "amenity"; s[1] = "school";
			l.add(s);
			s = new String[2];
			s[0] = "isced:level"; s[1] = "3;4";
			l.add(s);
			return l;

		case "EMS":
		case "FMS":
			s[0] = "tourism"; s[1] = "museum";
			l.add(s);
			return l;

		case "EPR":
		case "FPR":
			s[0] = "amenity"; s[1] = "school";
			l.add(s);
			s = new String[2];
			s[0] = "isced:level"; s[1] = "4";
			l.add(s);
			return l;

		case "EUN":
		case "FUN":
			s[0] = "amenity"; s[1] = "university";
			l.add(s);
			return l;

		case "G":
		case "H":
			s[0] = "tourism"; s[1] = "hotel";
			l.add(s);
			return l;

		case "GC1":
		case "HC1":
			s[0] = "amenity"; s[1] = "cafe";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "1";
			l.add(s);
			return l;

		case "GC2":
		case "HC2":
			s[0] = "amenity"; s[1] = "cafe";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "2";
			l.add(s);
			return l;

		case "GC3":
		case "HC3":
			s[0] = "amenity"; s[1] = "cafe";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "3";
			l.add(s);
			return l;

		case "GC4":
		case "HC4":
			s[0] = "amenity"; s[1] = "cafe";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "4";
			l.add(s);
			return l;

		case "GC5":
		case "HC5":
			s[0] = "amenity"; s[1] = "cafe";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "5";
			l.add(s);
			return l;

		case "GH1":
		case "HH1":
			s[0] = "amenity"; s[1] = "hotel";
			l.add(s);
			s = new String[2];
			s[0] = "stars"; s[1] = "1";
			l.add(s);
			return l;

		case "GH2":
		case "HH2":
			s[0] = "amenity"; s[1] = "hotel";
			l.add(s);
			s = new String[2];
			s[0] = "stars"; s[1] = "2";
			l.add(s);
			return l;

		case "GH3":
		case "HH3":
			s[0] = "amenity"; s[1] = "hotel";
			l.add(s);
			s = new String[2];
			s[0] = "stars"; s[1] = "3";
			l.add(s);
			return l;

		case "GH4":
		case "HH4":
			s[0] = "amenity"; s[1] = "hotel";
			l.add(s);
			s = new String[2];
			s[0] = "stars"; s[1] = "4";
			l.add(s);
			return l;

		case "GH5":
		case "HH5":
			s[0] = "amenity"; s[1] = "hotel";
			l.add(s);
			s = new String[2];
			s[0] = "stars"; s[1] = "5";
			l.add(s);
			return l;

		case "GP1":
		case "HP1":
			s[0] = "tourism"; s[1] = "apartments";
			l.add(s);
			s = new String[2];
			s[0] = "category"; s[1] = "1";
			l.add(s);
			return l;

		case "GP2":
		case "HP2":
			s[0] = "tourism"; s[1] = "apartments";
			l.add(s);
			s = new String[2];
			s[0] = "category"; s[1] = "2";
			l.add(s);
			return l;

		case "GP3":
		case "HP3":
			s[0] = "tourism"; s[1] = "apartments";
			l.add(s);
			s = new String[2];
			s[0] = "category"; s[1] = "3";
			l.add(s);
			return l;

		case "GPL":
		case "HPL":
			s[0] = "tourism"; s[1] = "apartments";
			l.add(s);
			return l;

		case "GR1":
		case "HR1":
			s[0] = "amenity"; s[1] = "restaurant";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "1";
			l.add(s);
			return l;

		case "GR2":
		case "HR2":
			s[0] = "amenity"; s[1] = "restaurant";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "2";
			l.add(s);
			return l;

		case "GR3":
		case "HR3":
			s[0] = "amenity"; s[1] = "restaurant";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "3";
			l.add(s);
			return l;

		case "GR4":
		case "HR4":
			s[0] = "amenity"; s[1] = "restaurant";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "4";
			l.add(s);
			return l;

		case "GR5":
		case "HR5":
			s[0] = "amenity"; s[1] = "restaurant";
			l.add(s);
			s = new String[2];
			s[0] = "forks"; s[1] = "5";
			l.add(s);
			return l;

		case "GS1":
		case "HS1":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "stars"; s[1] = "1";
			l.add(s);
			return l;

		case "GS2":
		case "HS2":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "stars"; s[1] = "2";
			l.add(s);
			return l;

		case "GS3":
		case "HS3":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "stars"; s[1] = "3";
			l.add(s);
			return l;

		case "GT1":
		case "HT1":
			return l;

		case "GT2":
		case "HT2":
			return l;

		case "GT3":
		case "HT3":
			return l;

		case "GTL":
		case "HTL":
			return l;

		case "I":
		case "J":
			s[0] = "*landuse"; s[1] = "industrial";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			return l;

		case "IAJ":
		case "JAG":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "farming";
			l.add(s);
			return l;

		case "IAL":
		case "JAL":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "works";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "food";
			l.add(s);
			return l;

		case "IAM":
		case "JAM":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "content"; s[1] = "OMW";
			l.add(s);
			return l;

		case "IAR":
		case "JAR":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "agricultural";
			l.add(s);
			return l;

		case "IAS":
		case "JAS":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "craft"; s[1] = "sawmill";
			l.add(s);
			return l;

		case "IBB":
		case "JBB":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "drinks";
			l.add(s);
			return l;

		case "IBD":
		case "JBD":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "winery";
			l.add(s);
			return l;

		case "IBR":
		case "JBR":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "ceramic";
			l.add(s);
			return l;

		case "ICH":
		case "JCH":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "mushrooms";
			l.add(s);
			return l;

		case "ICN":
		case "JCN":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "building";
			l.add(s);
			return l;

		case "ICT":
		case "JCT":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "quarry";
			l.add(s);
			return l;

		case "IEL":
		case "JEL":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "electric";
			l.add(s);
			return l;

		case "IGR":
		case "JGR":
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			return l;

		case "IIM":
		case "JIM":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "chemistry";
			l.add(s);
			return l;

		case "IIN":
		case "JIN":
			s[0] = "landuse"; s[1] = "greenhouse_horticulture";
			l.add(s);
			return l;

		case "IMD":
		case "JMD":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "wood";
			l.add(s);
			return l;

		case "IMN":
		case "JMN":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "manufacturing";
			l.add(s);
			return l;

		case "IMT":
		case "JMT":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "metal";
			l.add(s);
			return l;

		case "IMU":
		case "JMU":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "machinery";
			l.add(s);
			return l;

		case "IPL":
		case "JPL":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "plastics";
			l.add(s);
			return l;

		case "IPP":
		case "JPP":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "paper";
			l.add(s);
			return l;

		case "IPS":
		case "JPS":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "fishing";
			l.add(s);
			return l;

		case "IPT":
		case "JPT":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "petroleum";
			l.add(s);
			return l;

		case "ITB":
		case "JTB":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "tobacco";
			l.add(s);
			return l;

		case "ITX":
		case "JTX":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "clothing";
			l.add(s);
			return l;

		case "IVD":
		case "JVD":
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "works"; s[1] = "glass";
			l.add(s);
			return l;

		case "K":
		case "L":
			s[0] = "*landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] = "recreation_type"; s[1] = "sports";
			l.add(s);
			return l;

		case "KDP":
		case "LDP":
			s[0] = "landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] = "recreation_type"; s[1] = "sports";
			l.add(s);
			s = new String[2];
			s[0] = "leisure"; s[1] = "pitch";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", afinar sport=X si es posible.";
			l.add(s);
			return l;

		case "KES":
		case "LES":
			s[0] = "landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] = "recreation_type"; s[1] = "sports";
			l.add(s);
			s = new String[2];
			s[0] = "leisure"; s[1] = "stadium";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", afinar sport=X si es posible.";
			l.add(s);
			return l;

		case "KPL":
		case "LPL":
			s[0] = "landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] = "recreation_type"; s[1] = "sports";
			l.add(s);
			s = new String[2];
			s[0] = "leisure"; s[1] = "sports_centre";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", afinar sport=X si es posible.";
			l.add(s);
			return l;

		case "KPS":
		case "LPS":
			s[0] = "landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] = "recreation_type"; s[1] = "sports";
			l.add(s);
			s = new String[2];
			s[0] = "leisure"; s[1] = "swimming_pool";
			l.add(s);
			s = new String[2];
			s[0] = "sport"; s[1] = "swimming";
			l.add(s);
			return l;

		case "M":
		case "N":
			s[0] = "*landuse"; s[1] = "greenfield";
			l.add(s);
			return l;

		case "O":
		case "X":
			s[0] = "*landuse"; s[1] = "commercial";
			l.add(s);
			return l;

		case "O02":
		case "X02":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", afinar office=X si es posible.";
			l.add(s);
			return l;

		case "O03":
		case "X03":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", afinar office=X si es posible.";
			l.add(s);
			return l;

		case "O06":
		case "X06":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", afinar office=X si es posible.";
			l.add(s);
			return l;

		case "O07":
		case "X07":
			s[0] = "landuse"; s[1] = "health";
			l.add(s);
			s = new String[2];
			s[0] = "health_facility:type"; s[1] = "office";
			l.add(s);
			s = new String[2];
			s[0] = "health_person:type"; s[1] = "nurse";
			l.add(s);
			return l;

		case "O11":
		case "X11":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", afinar office=X si es posible.";
			l.add(s);
			return l;

		case "O13":
		case "X13":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", afinar office=X si es posible.";
			l.add(s);
			return l;

		case "O15":
		case "X15":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "office"; s[1] = "writer";
			l.add(s);
			return l;

		case "O16":
		case "X16":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "craft"; s[1] = "painter";
			l.add(s);
			return l;

		case "O17":
		case "X17":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "office"; s[1] = "musician";
			l.add(s);
			return l;

		case "O43":
		case "X43":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "office"; s[1] = "salesman";
			l.add(s);
			return l;

		case "O44":
		case "X44":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", afinar office=X si es posible.";
			l.add(s);
			return l;

		case "O75":
		case "X75":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "craft"; s[1] = "weaver";
			l.add(s);
			return l;

		case "O79":
		case "X79":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "craft"; s[1] = "tailor";
			l.add(s);
			return l;

		case "O81":
		case "X81":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "craft"; s[1] = "carpenter";
			l.add(s);
			return l;

		case "O88":
		case "X88":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "craft"; s[1] = "jeweller";
			l.add(s);
			return l;

		case "O99":
		case "X99":
			s[0] = "landuse"; s[1] = "commercial";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", afinar office=X si es posible.";
			l.add(s);
			return l;

		case "P":
		case "Q":
			s[0] = "amenity"; s[1] = "public_building";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "public";
			l.add(s);
			return l;

		case "PAA":
		case "QAA":
			s[0] = "amenity"; s[1] = "townhall";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "public";
			l.add(s);
			return l;

		case "PAD":
		case "QAD":
			s[0] = "amenity"; s[1] = "courthouse";
			l.add(s);
			s = new String[2];
			s[0] = "operator"; s[1] = "autonomous_community";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "public";
			l.add(s);
			return l;

		case "PAE":
		case "QAE":
			s[0] = "amenity"; s[1] = "townhall";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "public";
			l.add(s);
			return l;

		case "PCB":
		case "QCB":
			s[0] = "office"; s[1] = "administrative";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "public";
			l.add(s);
			return l;

		case "PDL":
		case "QDL":
		case "PGB":
		case "QGB":
			s[0] = "office"; s[1] = "government";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "public";
			l.add(s);
			return l;

		case "PJA":
		case "QJA":
			s[0] = "amenity"; s[1] = "courthouse";
			l.add(s);
			s = new String[2];
			s[0] = "operator"; s[1] = "county";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "public";
			l.add(s);
			return l;

		case "PJO":
		case "QJO":
			s[0] = "amenity"; s[1] = "courthouse";
			l.add(s);
			s = new String[2];
			s[0] = "operator"; s[1] = "province";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "public";
			l.add(s);
			return l;

		case "R":
		case "S":
			s[0] = "amenity"; s[1] = "place_of_worship";
			l.add(s);
			return l;

		case "RBS":
		case "SBS":
			s[0] = "amenity"; s[1] = "place_of_worship";
			l.add(s);
			s = new String[2];
			s[0] = "religion"; s[1] = "christian";
			l.add(s);
			s = new String[2];
			s[0] = "denomination"; s[1] = "catholic";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "basilica";
			l.add(s);
			return l;

		case "RCP":
		case "SCP":
			s[0] = "amenity"; s[1] = "place_of_worship";
			l.add(s);
			s = new String[2];
			s[0] = "religion"; s[1] = "christian";
			l.add(s);
			s = new String[2];
			s[0] = "denomination"; s[1] = "catholic";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "chapel";
			l.add(s);
			return l;

		case "RCT":
		case "SCT":
			s[0] = "amenity"; s[1] = "place_of_worship";
			l.add(s);
			s = new String[2];
			s[0] = "religion"; s[1] = "christian";
			l.add(s);
			s = new String[2];
			s[0] = "denomination"; s[1] = "catholic";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "cathedral";
			l.add(s);
			return l;

		case "RER":
		case "SER":
			s[0] = "amenity"; s[1] = "place_of_worship";
			l.add(s);
			s = new String[2];
			s[0] = "religion"; s[1] = "christian";
			l.add(s);
			s = new String[2];
			s[0] = "denomination"; s[1] = "catholic";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "hermitage";
			l.add(s);
			return l;

		case "RPR":
		case "SPR":
			s[0] = "amenity"; s[1] = "place_of_worship";
			l.add(s);
			s = new String[2];
			s[0] = "religion"; s[1] = "christian";
			l.add(s);
			s = new String[2];
			s[0] = "denomination"; s[1] = "catholic";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "parish_church";
			l.add(s);
			return l;

		case "RSN":
		case "SSN":
			s[0] = "amenity"; s[1] = "hospital";
			l.add(s);
			s = new String[2];
			s[0] = "landuse"; s[1] = "health";
			l.add(s);
			return l;

		case "T":
		case "U":
			s[0] = "landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] = "recreation_type"; s[1] = "entertainment";
			l.add(s);
			return l;

		case "TAD":
		case "UAD":
			s[0] = "landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] = "recreation_type"; s[1] = "entertainment";
			l.add(s);
			s = new String[2];
			s[0] = "amenity"; s[1] = "auditorium";
			l.add(s);
			return l;

		case "TCM":
		case "UCM":
			s[0] = "landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] = "recreation_type"; s[1] = "entertainment";
			l.add(s);
			s = new String[2];
			s[0] = "amenity"; s[1] = "cinema";
			l.add(s);
			return l;

		case "TCN":
		case "UCN":
			s[0] = "landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] = "recreation_type"; s[1] = "entertainment";
			l.add(s);
			s = new String[2];
			s[0] = "amenity"; s[1] = "cinema";
			l.add(s);
			return l;

		case "TSL":
		case "USL":
			s[0] = "landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] = "recreation_type"; s[1] = "entertainment";
			l.add(s);
			s = new String[2];
			s[0] = "amenity"; s[1] = "hall";
			l.add(s);
			return l;

		case "TTT":
		case "UTT":
			s[0] = "landuse"; s[1] = "recreation_ground";
			l.add(s);
			s = new String[2];
			s[0] = "recreation_type"; s[1] = "entertainment";
			l.add(s);
			s = new String[2];
			s[0] = "amenity"; s[1] = "theatre";
			l.add(s);
			return l;

		case "V":
		case "W":
			s[0] = "*landuse"; s[1] = "residential";
			l.add(s);
			return l;

		case "Y":
			s[0] = "*landuse"; s[1] = "health";
			l.add(s);
			return l;

		case "Z":
			s[0] = "*landuse"; s[1] = "farm";
			l.add(s);
			return l;

		case "YAM":
		case "ZAM":
		case "YCL":
		case "ZCL":
			s[0] = "landuse"; s[1] = "health";
			l.add(s);
			s = new String[2];
			s[0] = "amenity"; s[1] = "clinic";
			l.add(s);
			s = new String[2];
			s[0] = "medical_system:western"; s[1] = "yes";
			l.add(s);
			s = new String[2];
			s[0] = "health_facility:type"; s[1] = "clinic";
			l.add(s);
			return l;

		case "YBE":
		case "ZBE":
			s[0] = "landuse"; s[1] = "pond";
			l.add(s);
			return l;

		case "YCA":
		case "ZCA":
			s[0] = "amenity"; s[1] = "casino";
			l.add(s);
			return l;

		case "YCB":
		case "ZCB":
			s[0] = "amenity"; s[1] = "club";
			l.add(s);
			return l;

		case "YCE":
		case "ZCE":
			s[0] = "amenity"; s[1] = "casino";
			l.add(s);
			return l;

		case "YCT":
		case "ZCT":
			s[0] = "landuse"; s[1] = "quarry";
			l.add(s);
			return l;

		case "YDE":
		case "ZDE":
			s[0] = "man_made"; s[1] = "wastewater_plant";
			l.add(s);
			return l;

		case "YDG":
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "content"; s[1] = "gas";
			l.add(s);
			return l;

		case "ZDG":
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "content"; s[1] = "gas";
			l.add(s);
			return l;

		case "YDL":
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "content"; s[1] = "liquid";
			l.add(s);
			return l;

		case "ZDL":
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "content"; s[1] = "liquid";
			l.add(s);
			return l;

		case "YDS":
		case "ZDS":
			s[0] = "landuse"; s[1] = "health";
			l.add(s);
			s = new String[2];
			s[0] = "medical_system:western"; s[1] = "yes";
			l.add(s);
			s = new String[2];
			s[0] = "health_facility:type"; s[1] = "dispensary";
			l.add(s);
			return l;

		case "YGR":
		case "ZGR":
			s[0] = "amenity"; s[1] = "kindergarten";
			l.add(s);
			return l;

		case "YGV":
		case "ZGV":
			s[0] = "landuse"; s[1] = "surface_mining";
			l.add(s);
			s = new String[2];
			s[0] = "mining_resource"; s[1] = "gravel";
			l.add(s);
			return l;

		case "YHG":
		case "ZHG":
			return l;

		case "YHS":
		case "ZHS":
		case "YSN":
		case "ZSN":
			s[0] = "landuse"; s[1] = "health";
			l.add(s);
			s = new String[2];
			s[0] = "amenity"; s[1] = "hospital";
			l.add(s);
			s = new String[2];
			s[0] = "medical_system:western"; s[1] = "yes";
			l.add(s);
			s = new String[2];
			s[0] = "health_facility:type"; s[1] = "hospital";
			l.add(s);
			return l;

		case "YMA":
		case "ZMA":
			s[0] = "landuse"; s[1] = "surface_mining";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Codigo="+codigo+", afinar mining_resource=X si es posible.";
			l.add(s);
			return l;

		case "YME":
		case "ZME":
			s[0] = "man_made"; s[1] = "pier";
			l.add(s);
			return l;

		case "YPC":
		case "ZPC":
			s[0] = "landuse"; s[1] = "aquaculture";
			l.add(s);
			return l;

		case "YRS":
		case "ZRS":
			s[0] = "social_facility"; s[1] = "group_home";
			l.add(s);
			return l;

		case "YSA":
		case "ZSA":
		case "YSO":
		case "ZSO":
			s[0] = "office"; s[1] = "labor_union";
			l.add(s);
			return l;

		case "YSC":
		case "ZSC":
			s[0] = "health_facility:type"; s[1] = "first_aid";
			l.add(s);
			s = new String[2];
			s[0] = "medical_system:western"; s[1] = "yes";
			l.add(s);
			s = new String[2];
			s[0] = "yes"; s[1] = "yes";
			l.add(s);
			return l;

		case "YSL":
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "content"; s[1] = "solid";
			l.add(s);
			return l;

		case "ZSL":
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "content"; s[1] = "solid";
			l.add(s);
			return l;

		case "YVR":
		case "ZVR":
			s[0] = "landuse"; s[1] = "landfill";
			l.add(s);
			return l;

		default:
			if (!codigo.isEmpty()){
				s[0] = "fixme"; s[1] = "Documentar nuevo codificación de los usos de los vienes inmuebles en catastro código="+ codigo +" en http://wiki.openstreetmap.org/wiki/Traduccion_metadatos_catastro_a_map_features#Codificacion_de_los_usos_de_los_bienes_inmuebles";
				l.add(s);}


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
