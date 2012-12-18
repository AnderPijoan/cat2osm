import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;

import com.linuxense.javadbf.DBFReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;


public class ShapeElemlin extends Shape {

	private String shapeId = null; // Id del shape ELEMLIN+long
	private LineString line; // Linea que representa ese elemlin
	private List<Long> nodes;
	private List<Long>  ways;
	private Long relation;
	private String ttggss; // Campo TTGGSS en Elemlin.shp
	private List<ShapeAttribute> atributos;
	private String codigoMasa; // Codigo de masa a la que pertenece
	// Esto se usa para la paralelizacion ya que luego solo se simplificaran geometrias que
	// pertenezcan a las mismas masas. Si alguna geometria no tiene codigo de masa, se le
	// asignara el nombre de tipo de archivo


	public ShapeElemlin(SimpleFeature f, String tipo) {

		super(f, tipo);
		
		// Creamos la factoria para crear objetos de GeoTools (hay otra factoria pero falla)
		com.vividsolutions.jts.geom.GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);

		shapeId = "ELEMLIN" + newShapeId();

		// Elemtex trae la geometria en formato MultiLineString
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiLineString")){

			MultiLineString l = (MultiLineString) f.getDefaultGeometry();
			line = factory.createLineString(l.getCoordinates());

		}
		else {
			System.out.println("["+new Timestamp(new Date().getTime())+"] Formato geometrico "+ 
					f.getDefaultGeometry().getClass().getName() +" desconocido dentro del shapefile ELEMLIN");
		}

		// Los demas atributos son metadatos y de ellos sacamos

		ttggss = (String) f.getAttribute("TTGGSS");

		// Para agrupar geometrias segun su codigo de masa que como en este caso no existe se
		// asigna el del nombre del fichero shapefile
		// En este caso se anade "ELEMLIN" por delante para que luego el proceso al encontrar un key
		// de ELEMLIN intente juntar todos los ways con todos los que se toquen
		// (a diferencia de las otros elementos que solo tiene que unir ways si pertenecen
		// a los mismos shapes)
		codigoMasa = "ELEMLIN-" + ttggss;
		
		// Si queremos coger todos los atributos del .shp
		/*this.atributos = new ArrayList<ShapeAttribute>();
		for (int x = 1; x < f.getAttributes().size(); x++){
		atributos.add(new ShapeAttribute(f.getFeatureType().getDescriptor(x).getType(), f.getAttributes().get(x)));
		}*/

		this.nodes = new ArrayList<Long>();
		this.ways = new ArrayList<Long>();
	}


	public String getShapeId(){
		return shapeId;
	}
	
	public String getCodigoMasa() {
		return codigoMasa;
	}


	public List<String[]> getAttributes() {
		List <String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];

		s = new String[2];
		s[0] = "CAT2OSMSHAPEID"; s[1] = getShapeId();
		l.add(s);

		if (ttggss != null){
			l.addAll(ttggssParser(ttggss));
		}
		
		return l;
	}


	public ShapeAttribute getAttribute(int x){	
		return atributos.get(x);
	}

	public String getRefCat() {
		return null;
	}


	public synchronized Long getRelationId() {
		return relation;
	}


	public List<LineString> getPoligons() {
		List<LineString> l = new ArrayList<LineString>();
		l.add(line);
		return l;
	}


	public Coordinate[] getCoordenadas(int x) {
		return line.getCoordinates();
	}


	public String getTtggss() {
		return ttggss;
	}


	public boolean shapeValido (){

		switch(ttggss){
		//case "030202":
			//return true; // Margenes de rios que van a tener natural=water
		//case "030302":
			//return true; // Emborrona mas que ayudar
		case "037101":
			return true;
		case "038101":
			return true;
		case "038102":
			return true;
		case "037102":
			return true;
		case "167111":
			return true;
		default:
			return false;
		}
	}


	public void addNode(int pos, long nodeId){
		nodes.add(nodeId);
	}


	public void addWay(int pos, long wayId){
		if (!ways.contains(wayId))
			ways.add(wayId);
	}


	public synchronized void deleteWay(int pos, long wayId){
		if (ways.size()>pos)
			ways.remove(wayId);
	}


	public void setRelation(long relationId) {
		relation = relationId;
	}


	/** Devuelve la lista de ids de nodos del poligono en posicion pos
	 * @param pos posicion que ocupa el poligono en la lista
	 * @return Lista de ids de nodos del poligono en posicion pos
	 */
	public synchronized List<Long> getNodesIds(int pos){
		return nodes;
	}


	/** Devuelve la lista de ids de ways
	 * del poligono en posicion pos
	 * @param pos posicion que ocupa el poligono en la lista
	 * @return Lista de ids de ways del poligono en posicion pos
	 */
	public synchronized List<Long> getWaysIds(int pos) {
		return ways;
	}


	public Coordinate getCoor() {
		return null;
	}

	/** Lee el archivo Carvia.dbf y relaciona el numero de via que trae el Elemlin.shp con
	 * los nombres de via que trae el Carvia.dbf.
	 * @param v Numero de via a buscar
	 * @return String tipo y nombre de via
	 * @throws IOException
	 */
	public String getVia(long v) throws IOException{
		InputStream inputStream  = new FileInputStream(Config.get("UrbanoSHPDir") + "/CARVIA/CARVIA.DBF");
		DBFReader reader = new DBFReader(inputStream); 

		Object[] rowObjects;

		while((rowObjects = reader.nextRecord()) != null) {

			if ((Double) rowObjects[2] == (v)){
				inputStream.close();
				return ((String) rowObjects[3]);
			}
		}
		return null;
	}  

	@Override
	public void setWays(List<List<Long>> waysId) {
		ways = waysId.get(0);
	}

	@Override
	public void setNodes(List<List<Long>> nodesId) {
		nodes = nodesId.get(0);
	}
	
}
