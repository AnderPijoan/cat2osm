import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;

import com.linuxense.javadbf.DBFReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class ShapeEjes extends Shape {
	
	private String shapeId = null; // Id del shape EJES+long
	private LineString line; // Linea que representa ese eje
	private List<Long> nodes;
	private List<Long> ways;
	private Long relation; // Relacion de sus ways
	private String via; // Nombre de via, solo en Ejes.shp, lo coge de Carvia.dbf
	private List<ShapeAttribute> atributos;
	private String tipo; // Tipo de ejes Rusticos o Urbanos. Todos los urbanos tendran highway=residential
	private static final Map<Long,String> ejesNames = new HashMap<Long, String>(); // Lista de codigos y nombres de vias (para el Ejes.shp)
	private String codigoMasa; // Codigo de masa a la que pertenece
	// Esto se usa para la paralelizacion ya que luego solo se simplificaran geometrias que
	// pertenezcan a las mismas masas. Si alguna geometria no tiene codigo de masa, se le
	// asignara el nombre de tipo de archivo


	public ShapeEjes(SimpleFeature f, String tipo) throws IOException {
		
		super(f, tipo);
		
		// Creamos la factoria para crear objetos de GeoTools (hay otra factoria pero falla)
		com.vividsolutions.jts.geom.GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);

		this.shapeId = "EJES" + newShapeId();
		
		this.tipo = tipo;

		if (tipo.equals("UR") && ejesNames.isEmpty()){
			readCarvia(tipo);
		}
		
		// Ejes trae la geometria en formato MultiLineString
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiLineString")){

			MultiLineString l = (MultiLineString) f.getDefaultGeometry();
			line = factory.createLineString(l.getCoordinates());

		}
		else {
			System.out.println("["+new Timestamp(new Date().getTime())+"] Formato geometrico "+
		f.getDefaultGeometry().getClass().getName() +" desconocido del shapefile EJES");
		}
		
		if (tipo.equals("UR") && f.getAttribute("VIA") instanceof Double){
			double v = (Double) f.getAttribute("VIA");
			via = getVia((long) v);
		}
		else if (tipo.equals("UR") && f.getAttribute("VIA") instanceof Long){
			via = getVia((Long) f.getAttribute("VIA"));
		}
		else if (tipo.equals("UR") && f.getAttribute("VIA") instanceof Integer){
			int v = (Integer) f.getAttribute("VIA");
			via = getVia((long) v);
		}
		else if (tipo.equals("UR") && f.getAttribute("VIA") instanceof String){
			int v = Integer.parseInt((String) f.getAttribute("VIA"));
			via = getVia((long) v);
		}
		else if(tipo.equals("UR"))  System.out.println("["+new Timestamp(new Date().getTime())+"] No se reconoce el tipo del atributo VIA "+ f.getAttribute("VIA").getClass().getName() );	

		// Si queremos coger todos los atributos del .shp
		/*this.atributos = new ArrayList<ShapeAttribute>();
		for (int x = 1; x < f.getAttributes().size(); x++){
		atributos.add(new ShapeAttribute(f.getFeatureType().getDescriptor(x).getType(), f.getAttributes().get(x)));
		}*/

		// Para agrupar geometrias segun su codigo de masa que como en este caso no existe se
		// asigna el del nombre del fichero shapefile
		// En este caso se anade "EJES" por delante para que luego el proceso al encontrar un key
		// de EJES intente juntar todos los ways con todos los que se toquen
		// (a diferencia de las otros elementos que solo tiene que unir ways si pertenecen
		// a los mismos shapes)
		codigoMasa = (via == null ? "EJESSINNOMBRE" : "EJES" + via.trim().replaceAll("[^\\p{L}\\p{N}]", ""));
		
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
		
		// Via trae el tipo (substring de 0 a 2) y el nombre (substring de 3 en adelante)
		// Se parsea el tipo para traducirlo al nombre del tipo y para sacar tags extra
		List<String[]> tags = null;
		
		if (via != null && !via.isEmpty()){
			s = new String[2];
			s[0] = "name"; s[1] = nombreTipoViaParser(via.substring(0, 2)) +" "+ formatearNombreCalle(via.substring(3));
			l.add(s);
			
			// En funcion del tipo de via, meter tags que la describan
			tags = atributosViaParser(via.substring(0, 2));
		}
		else{
			tags = new ArrayList<String[]>();
			tags.add(new String[]{"highway","unclassified"});
		}
		
		if (tags != null && !tags.isEmpty())
			l.addAll(tags);
		
		if (tipo.equals("UR")){
			s = new String[2];
			s[0] = "highway"; s[1] = "residential";
			l.add(s);
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


	public void addNode(int pos, long nodeId){
			nodes.add(nodeId);
	}


	public void addWay(int pos, long wayId){
		if (!ways.contains(wayId))
			ways.add(wayId);
	}
	
	
	public synchronized void deleteWay(int pos, long wayId){
		ways.remove(wayId);
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


	public void setRelation(long relationId) {
		relation = relationId;
	}

	
	public void setVia(String t) {
		via = t;
	}
	

	public Coordinate getCoor() {
		return null;
	}

	
	public List<String[]> atributosViaParser(String codigo){
		List<String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];

		switch(codigo){
		
		case "AU":
			s[0] = "highway"; s[1] = "motorway";
			l.add(s);
		return l;
		
		case "AY":
			s[0] = "waterway"; s[1] = "stream";
			l.add(s);
		return l;
		
		case "CG":
			s[0] = "amenity"; s[1] = "school";
			l.add(s);
		return l;
		
		case "CJ":
			s[0] = "highway"; s[1] = "residential";
			l.add(s);
		return l;
		
		case "CM":
			s[0] = "highway"; s[1] = "track";
			l.add(s);
		return l;
		
		case "CR":
			s[0] = "highway"; s[1] = "unclassified";
			l.add(s);
		return l;
		
		case "ES":
			s[0] = "highway"; s[1] = "steps";
			l.add(s);
		return l;
		
		case "FC":
			s[0] = "railway"; s[1] = "rail";
			l.add(s);
		return l;
		
		case "GL":
			s[0] = "junction"; s[1] = "roundabout";
			l.add(s);
		return l;
		
		case "GV":
			s[0] = "highway"; s[1] = "primary";
			l.add(s);
		return l;
		
		case "JR":
			s[0] = "leisure"; s[1] = "garden";
			l.add(s);
		return l;
		
		case "MC":
			s[0] = "amenity"; s[1] = "marketplace";
			l.add(s);
		return l;
		
		case "ML":
			s[0] = "waterway"; s[1] = "dock";
			l.add(s);
		return l;
		
		case "PZ":
			s[0] = "highway"; s[1] = "unclassified";
			l.add(s);
		return l;
		
		case "RD":
			s[0] = "highway"; s[1] = "unclassified";
			l.add(s);
		return l;
		
		case "RU":
			s[0] = "highway"; s[1] = "residential";
			l.add(s);
		return l;
		
		case "SD":
			s[0] = "highway"; s[1] = "path";
			l.add(s);
		return l;
		
		case "TR":
			s[0] = "highway"; s[1] = "path";
			l.add(s);
		return l;
		
		case "UR":
			s[0] = "highway"; s[1] = "residential";
			l.add(s);
		return l;
		
		default:
			if (codigo.isEmpty()){
				s[0] = "fixme"; s[1] = "Tagear tipo de via "+ codigo +" en http://wiki.openstreetmap.org/w/index.php?title=Traduccion_metadatos_catastro_a_map_features.";
				l.add(s);
			}
		}
		
		return l;
	}

	
	public String getTtggss() {
		return null;
	}
	
	
	public boolean shapeValido (){
		return true;
	}
	
	
	/** Lee el archivo Carvia.dbf y lo almacena para despues relacionar el numero de via 
	 * de Ejes.shp con los nombres de via que trae Carvia.dbf. El nombre de via trae tambien el tipo
	 * en formato 2caracteres de tipo de via, un espacio en blanco y el nombre de via
	 * @param file Archivo Ejes.shp del para acceder a su Carvia.dbf
	 * @throws IOException 
	 * @throws IOException
	 */
	public void readCarvia(String tipo) throws IOException{
		
		InputStream inputStream = null;
		if (tipo.equals("UR"))
		inputStream = new FileInputStream(Config.get("UrbanoSHPPath") + "/CARVIA/Carvia.DBF");
		else if (tipo.equals("RU"))
		inputStream = new FileInputStream(Config.get("RusticoSHPPath") + "/CARVIA/Carvia.DBF");
		DBFReader reader = new DBFReader(inputStream);
		
		int indiceVia = 0;
		int indiceDenomina = 0;
		for (int i = 0; i < reader.getFieldCount(); i++)
		{
			if (reader.getField(i).getName().equals("VIA"))      indiceVia = i;
			if (reader.getField(i).getName().equals("DENOMINA")) indiceDenomina = i;
		}
		
		Object[] rowObjects;

		while((rowObjects = reader.nextRecord()) != null) {
			
			// La posicion indiceVia es el codigo de via
			// La posicion indiceDenomina es la denominacion de via
			if (rowObjects[indiceVia] instanceof Double){
				double v = (Double) rowObjects[indiceVia];
				ejesNames.put((long) v, (String) rowObjects[indiceDenomina]);
			}
			else if (rowObjects[indiceVia] instanceof Long){
				ejesNames.put((Long) rowObjects[indiceVia], (String) rowObjects[indiceDenomina]);
			}
			else if (rowObjects[indiceVia] instanceof Integer){
				int v = (Integer) rowObjects[indiceVia];
				ejesNames.put((long) v, (String) rowObjects[indiceDenomina]);
			}
		}
	}  
	
	
	/** Relaciona el numero de via de Ejes.shp con los nombres de via que trae Carvia.dbf. El nombre de via trae tambien el tipo
	 * en formato 2caracteres de tipo de via, un espacio en blanco y el nombre de via
	 * @param v Numero de via a buscar
	 * @return String tipo y nombre de via
	 */
	public String getVia(long v){
			return ejesNames.get(v);
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
