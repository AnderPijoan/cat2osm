import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private static final Map<Long,String> ejesNames = new HashMap<Long,String>(); // Lista de codigos y nombres de vias (para el Ejes.shp)


	public ShapeEjes(SimpleFeature f, String tipo) throws IOException {
		
		super(f, tipo);
		

		shapeId = "EJES" + super.newShapeId();

		if (ejesNames.isEmpty()){
			readCarvia(tipo);
		}
		
		// Ejes trae la geometria en formato MultiLineString
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiLineString")){

			MultiLineString l = (MultiLineString) f.getDefaultGeometry();
			line = new LineString(l.getCoordinates(),null , 0);

		}
		else {
			System.out.println("["+new Timestamp(new Date().getTime())+"] Formato geometrico "+
		f.getDefaultGeometry().getClass().getName() +" desconocido del shapefile EJES");
		}

		if (f.getAttribute("VIA") instanceof Double){
			double v = (Double) f.getAttribute("VIA");
			via = getVia((long) v);
		}
		else if (f.getAttribute("VIA") instanceof Long){
			via = getVia((Long) f.getAttribute("VIA"));
		}
		else if (f.getAttribute("VIA") instanceof Integer){
			int v = (Integer) f.getAttribute("VIA");
			via = getVia((long) v);
		}
		else System.out.println("["+new Timestamp(new Date().getTime())+"] No se reconoce el tipo del atributo VIA "+ f.getAttribute("VIA").getClass().getName() );	

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
	
	
	public List<String[]> getAttributes() {
		List <String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];

		s = new String[2];
		s[0] = "CAT2OSMSHAPEID"; s[1] = getShapeId();
		l.add(s);
		
		// Partimos el TipoVia y NombreVia
		List<String[]> tags = null;
		if (via != null){
			s = new String[2];
			s[0] = "short_name"; s[1] = via.substring(3).toString();
			l.add(s);
			
			s = new String[2];
			s[0] = "name:type"; s[1] = nombreTipoViaParser(via.substring(0, 2));
			l.add(s);
			
			// En funcion del tipo de via, meter tags que la describan
			tags = atributosViaParser(via.substring(0, 2));
			
		}
		
		if (tags != null)
			l.addAll(tags);
		
		// TODO
		s = new String[2];
		s[0] = "type"; s[1] = "route";
		l.add(s);
		
		s = new String[2];
		s[0] = "source"; s[1] = "catastro";
		l.add(s);
		
		Pattern p = Pattern.compile("\\d{4}-\\d{1,2}");
		Matcher m = p.matcher(Config.get("UrbanoCATFile"));
		if (m.find()) {
		s = new String[2];
		s[0] = "source:date"; s[1] = m.group();
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

		if (codigo.equals("AU")){
			s[0] = "highway"; s[1] = "motorway";
			l.add(s);
		}
		else if (codigo.equals("AY")){
			s[0] = "waterway"; s[1] = "stream";
			l.add(s);
		}
		else if (codigo.equals("CG")){
			s[0] = "amenity"; s[1] = "school";
			l.add(s);
		}
		else if (codigo.equals("CJ")){
			s[0] = "highway"; s[1] = "residential";
			l.add(s);
		}
		else if (codigo.equals("CM")){
			s[0] = "highway"; s[1] = "track";
			l.add(s);
		}
		else if (codigo.equals("CR")){
			s[0] = "highway"; s[1] = "trunk";
			l.add(s);
		}
		else if (codigo.equals("ES")){
			s[0] = "highway"; s[1] = "steps";
			l.add(s);
		}
		else if (codigo.equals("FC")){
			s[0] = "railway"; s[1] = "rail";
			l.add(s);
		}
		else if (codigo.equals("GL")){
			s[0] = "junction"; s[1] = "roundabout";
			l.add(s);
		}
		else if (codigo.equals("GV")){
			s[0] = "highway"; s[1] = "primary";
			l.add(s);
		}
		else if (codigo.equals("JR")){
			s[0] = "leisure"; s[1] = "garden";
			l.add(s);
		}
		else if (codigo.equals("MC")){
			s[0] = "amenity"; s[1] = "marketplace";
			l.add(s);
		}
		else if (codigo.equals("ML")){
			s[0] = "waterway"; s[1] = "dock";
			l.add(s);
		}
		else if (codigo.equals("PZ")){
			s[0] = "highway"; s[1] = "pedestrian";
			l.add(s);
			s = new String[2];
			s[0] = "area"; s[1] = "yes";
			l.add(s);
		}
		else if (codigo.equals("RD")){
			s[0] = "highway"; s[1] = "trunk";
			l.add(s);
		}
		else if (codigo.equals("RU")){
			s[0] = "highway"; s[1] = "residential";
			l.add(s);
		}
		else if (codigo.equals("SD")){
			s[0] = "highway"; s[1] = "path";
			l.add(s);
		}
		else if (codigo.equals("TR")){
			s[0] = "highway"; s[1] = "path";
			l.add(s);
		}
		else if (codigo.equals("UR")){
			s[0] = "highway"; s[1] = "residential";
			l.add(s);
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
		inputStream = new FileInputStream(Config.get("UrbanoSHPPath") + "/CARVIA/CARVIA.DBF");
		else if (tipo.equals("RU"))
		inputStream = new FileInputStream(Config.get("RusticoSHPPath") + "/CARVIA/CARVIA.DBF");
		DBFReader reader = new DBFReader(inputStream);
		
		Object[] rowObjects;

		while((rowObjects = reader.nextRecord()) != null) {

			// La posicion 2 es el codigo de via
			// La posicion 3 es la denominacion de via
			if (rowObjects[2] instanceof Double){
				double v = (Double) rowObjects[2];
				ejesNames.put((long) v, (String) rowObjects[3]);
			}
			else if (rowObjects[2] instanceof Long){
				ejesNames.put((Long) rowObjects[2], (String) rowObjects[3]);
			}
			else if (rowObjects[2] instanceof Integer){
				int v = (Integer) rowObjects[2];
				ejesNames.put((long) v, (String) rowObjects[3]);
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
	
}
