import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.FeatureReader;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;


public class ShapeParser extends Thread{
	
	String tipo; // UR/RU
	File file;
	Cat2OsmUtils utils;
	List<Shape> shapeList;
	
	public ShapeParser (String t, File f, Cat2OsmUtils u, List<Shape> s){
		super (f.getName());
		this.tipo = t;
		this.file = reprojectWGS84(f);
		this.utils = u;
		shapeList = s;
		start();
	}
	
	
	public void run () {
		
		try {
			
		FileDataStore store = FileDataStoreFinder.getDataStore(file);
		FeatureReader<SimpleFeatureType, SimpleFeature> reader = 
			store.getFeatureReader();
		
		long fechaDesde = Long.parseLong(Config.get("FechaDesde"));
		long fechaHasta = Long.parseLong(Config.get("FechaHasta"));
		
		// Creamos el shape dependiendo de su tipo
		if (file.getName().toUpperCase().equals(tipo+"MASA.SHP"))

			// Shapes del archivo MASA.SHP
			while (reader.hasNext()) {
				Shape shape = new ShapeMasa(reader.next());

				// Si cumple estar entre las fechas
				if (shape != null && shape.checkShapeDate(fechaDesde, fechaHasta))
					// Anadimos el shape creado a la lista
					shapeList.add(mPolygonShapeParser(shape));
			}
		else if (file.getName().toUpperCase().equals(tipo+"PARCELA.SHP"))

			// Shapes del archivo PARCELA.SHP
			while (reader.hasNext()) {
				Shape shape = new ShapeParcela(reader.next());

				// Si cumple estar entre las fechas
				if (shape != null && shape.checkShapeDate(fechaDesde, fechaHasta))
					// Anadimos el shape creado a la lista
					shapeList.add(mPolygonShapeParser(shape));
			}
		else if (file.getName().toUpperCase().equals(tipo+"SUBPARCE.SHP"))

			// Shapes del archivo SUBPARCE.SHP
			while (reader.hasNext()) {
				Shape shape = new ShapeSubparce(reader.next());

				// Si cumple estar entre las fechas
				if (shape != null && shape.checkShapeDate(fechaDesde, fechaHasta))
					// Anadimos el shape creado a la lista
					shapeList.add(mPolygonShapeParser(shape));
			}
		else if (file.getName().toUpperCase().equals(tipo+"CONSTRU.SHP"))

			// Shapes del archivo CONSTRU.SHP
			while (reader.hasNext()) {
				Shape shape = new ShapeConstru(reader.next());

				// Si cumple estar entre las fechas
				if (shape != null && shape.checkShapeDate(fechaDesde, fechaHasta))
					// Anadimos el shape creado a la lista
					shapeList.add(mPolygonShapeParser(shape));
			}
		else if (file.getName().toUpperCase().equals(tipo+"ELEMTEX.SHP"))

			// Shapes del archivo ELEMTEX.SHP
			while (reader.hasNext()) {
				Shape shape = new ShapeElemtex(reader.next());

				// Si cumple estar entre las fechas
				// Si cumple tener un ttggss valido (no interesa mostrar todos)
				if (shape != null && shape.checkShapeDate(fechaDesde, fechaHasta) && shape.shapeValido())
					// Anadimos el shape creado a la lista
					shapeList.add(pointShapeParser(shape));
			}
		else if (file.getName().toUpperCase().equals(tipo+"ELEMPUN.SHP"))

			// Shapes del archivo ELEMPUN.SHP
			while (reader.hasNext()) {
				Shape shape = new ShapeElempun(reader.next());

				// Si cumple estar entre las fechas
				// Si cumple tener un ttggss valido (no interesa mostrar todos)
				if (shape != null && shape.checkShapeDate(fechaDesde, fechaHasta) && shape.shapeValido())
					// Anadimos el shape creado a la lista
					shapeList.add(pointShapeParser(shape));
			}
		else if (file.getName().toUpperCase().equals(tipo+"ELEMLIN.SHP"))

			// Shapes del archivo ELEMLIN.SHP
			while (reader.hasNext()) {
				Shape shape = new ShapeElemlin(reader.next());

				// Si cumple estar entre las fechas
				// Si cumple tener un ttggss valido (no interesa mostrar todos)
				if (shape != null && shape.checkShapeDate(fechaDesde, fechaHasta) && shape.shapeValido())
					// Anadimos el shape creado a la lista
					shapeList.add(mLineStringShapeParser(shape));
			}
		else if (file.getName().toUpperCase().equals(tipo+"EJES.SHP"))
			
			// Shapes del archivo EJES.SHP
			while (reader.hasNext()) {
				Shape shape = new ShapeEjes(reader.next());

				// Si cumple estar entre las fechas
				if (shape != null && shape.checkShapeDate(fechaDesde, fechaHasta))
					// Anadimos el shape creado a la lista
					shapeList.add(mLineStringShapeParser(shape));
			}
		
		reader.close();
		store.dispose();
		
		deleteShpFiles(file.getName().toUpperCase());
		
		} catch (IOException e) {e.printStackTrace();}
	}
	
	
	/** Metodo para parsear los shapes cuyas geografias vienen dadas como
	 * MultiPolygon, como MASA.SHP, PARCELA.SHP, SUBPARCE.SHP y CONSTRU.SHP
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
				shape.addNode(x,utils.getNodeId(coor[y], null));
			}
		}
		
		// Partimos el poligono en el maximo numero de ways es decir uno por cada
		// dos nodos, mas adelante se juntaran los que sean posibles
		for (int x = 0; x < shape.getPoligons().size() ; x++){
			List <Long> nodeList = shape.getNodesIds(x);
			for (int y = 0; y < nodeList.size()-1 ; y++){
				List<Long> way = new ArrayList<Long>();
				way.add(nodeList.get(y));
				way.add(nodeList.get(y+1));
				if (!(nodeList.get(y) == (nodeList.get(y+1)))){
					List<String> id = new ArrayList<String>();
					id.add(shape.getShapeId());
					shape.addWay(x,utils.getWayId(way, id));
				}
			}
		}

		// Creamos una relation para el shape, metiendoe en ella todos los members
		List <Long> ids = new ArrayList<Long>(); // Ids de los members
		List <String> types = new ArrayList<String>(); // Tipos de los members
		List <String> roles = new ArrayList<String>(); // Roles de los members
		for (int x = 0; x < shape.getPoligons().size() ; x++){
			List <Long> wayList = shape.getWaysIds(x);
			for (Long way: wayList)
			if (!ids.contains(way)){
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
		shape.addNode(0,utils.getNodeId(shape.getCoor(), shape.getAttributes()));
		
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
				shape.addNode(0,utils.getNodeId(coor[x], null));
			
		// Con los nodos creamos ways
		List <Long> nodeList = shape.getNodesIds(0);
		for (int y = 0; y < nodeList.size()-1 ; y++){
			List<Long> way = new ArrayList<Long>();
			way.add(nodeList.get(y));
			way.add(nodeList.get(y+1));
			List<String> id = new ArrayList<String>();
			id.add(shape.getShapeId());
			shape.addWay(0,utils.getWayId(way, id));
		}
		
		// Con los ways creamos una relacion
		List <Long> ids = new ArrayList<Long>(); // Ids de los members
		List <String> types = new ArrayList<String>(); // Tipos de los members
		List <String> roles = new ArrayList<String>(); // Roles de los members
		for (Long way: shape.getWaysIds(0)){
			ids.add(way);
			types.add("way");
			roles.add("outer");
		}
		shape.setRelation(utils.getRelationId(ids, types, roles, shape.getAttributes()));
		
		return shape;
	}

	
	/** Utilizando ogr2ogr reproyecta el archivo de shapes de su proyeccion
	 * EPSG a WGS84 que es la que utiliza OpenStreetMap. Tambien convierte las 
	 * coordenadas UTM en Lat/Lon
	 * @param f Archivo a reproyectar
	 * @return File Archivo reproyectado
	 */
	public File reprojectWGS84(File f){
		
		try
		   {
			Process p = null;
			String line = "scripts/ogr2ogr.bat " + f.getPath().substring(0, f.getPath().length()-4) +" "+ Config.get("ResultPath")+"\\"+tipo+f.getName() +" "+ f.getPath();
			p = Runtime.getRuntime().exec("scripts/"+ Config.get("ScriptOgr2Ogr") +" "+ f.getPath().substring(0, f.getPath().length()-4) +" "+ Config.get("ResultPath")+"\\"+tipo+f.getName() +" "+ f.getPath() );
			BufferedReader bf = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    while ((line = bf.readLine()) != null)
		        System.out.println(line);

		    bf.close();

		   } catch (Exception er){ er.printStackTrace(); }
		
		return new File(Config.get("ResultPath")+"\\"+tipo+f.getName());
	}

	
	/** Borra los shapefiles temporales creados. Hay que borrar si se quiere
	 * reproyectar nuevos y como urbano y rustico tienen los mismos nombres
	 * de shapefiles, cada vez que usamos uno, lo borramos.
	 * @param filename
	 */
	public void deleteShpFiles(String filename){
	
		String path = Config.get("ResultPath");
		
		boolean borrado = true;
		
		// Borrar archivo con el mismo nombre si existe, porque sino concatenaria el nuevo
		borrado = borrado && new File(path +"\\"+ filename.substring(0, filename.length()-4) +".SHP").delete();
		borrado = borrado && new File(path +"\\"+ filename.substring(0, filename.length()-4) +".DBF").delete();
		borrado = borrado && new File(path +"\\"+ filename.substring(0, filename.length()-4) +".PRJ").delete();
		borrado = borrado && new File(path +"\\"+ filename.substring(0, filename.length()-4) +".SHX").delete();
	
		if (!borrado)
			System.out.println("NO se pudo BORRAR alguno de los archivos temporales");
		
	}

}
