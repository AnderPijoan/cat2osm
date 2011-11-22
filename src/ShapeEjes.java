import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.linuxense.javadbf.DBFReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class ShapeEjes extends Shape {

	private LineString line; // Linea que representa ese eje
	private List<Long> nodes;
	private List<Long> ways;
	private Long relation; // Relacion de sus ways
	private String via; // Nombre de via, solo en Ejes.shp, lo coge de Carvia.dbf
	private String ttggss; // Campo TTGGSS en Carvia.dbf
	private List<ShapeAttribute> atributos;

	public ShapeEjes(SimpleFeature f) throws IOException {
		super(f);

		// Ejes trae la geometria en formato MultiLineString
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiLineString")){

			MultiLineString l = (MultiLineString) f.getDefaultGeometry();
			line = new LineString(l.getCoordinates(),null , 0);

		}
		else {
			System.out.println("Formato geometrico "+ f.getDefaultGeometry().getClass().getName() +" desconocido dentro del shapefile EJES");
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
		else System.out.println("No se encuentra el tipo de VIA "+ f.getAttribute("VIA").getClass().getName() );	

		// Si queremos coger todos los atributos del .shp
		/*this.atributos = new ArrayList<ShapeAttribute>();
		for (int x = 1; x < f.getAttributes().size(); x++){
		atributos.add(new ShapeAttribute(f.getFeatureType().getDescriptor(x).getType(), f.getAttributes().get(x)));
		}*/

		this.nodes = new ArrayList<Long>();
		this.ways = new ArrayList<Long>();
	}

	@Override
	public List<String[]> getAttributes() {
		List <String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];

		//String[] s = new String[2];
		//s = new String[2];
		//s[0] = "FECHAALTA"; s[1] = String.valueOf(fechaAlta);
		//l.add(s);

		//s = new String[2];
		//s[0] = "FECHABAJA"; s[1] = String.valueOf(fechaBaja);
		//l.add(s);

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

		s = new String[2];
		s[0] = "source"; s[1] = "catastro";
		l.add(s);
		s = new String[2];
		s[0] = "add:country"; s[1] = "ES";
		l.add(s);
		
		return l;
	}

	public ShapeAttribute getAttribute(int x){	
		return atributos.get(x);
	}

	@Override
	public String getRefCat() {
		return null;
	}


	@Override
	public Long getRelation() {
		return relation;
	}

	@Override
	public List<LineString> getPoligons() {
		List<LineString> l = new ArrayList<LineString>();
		l.add(line);
		return l;
	}

	@Override
	public Coordinate[] getCoordenadas(int x) {
		return line.getCoordinates();
	}

	@Override
	public void addNode(long nodeId) {
		if (!nodes.contains(nodeId))
			nodes.add(nodeId);
	}

	@Override
	public List<Long> getNodesPoligonN(int x, Cat2OsmUtils utils) {
		return nodes;
	}

	@Override
	public void addWay(long wayId) {
		if (!ways.contains(wayId))
			ways.add(wayId);
	}

	@Override
	public List<Long> getWaysPoligonN(int x, Cat2OsmUtils utils) {
		return ways;
	}

	@Override
	public void setRelation(long relationId) {
		relation = relationId;
	}
	
	public void setTtggss(String t) {
		ttggss = t;
	}

	@Override
	public List<Long> getNodes() {
		return nodes;
	}

	@Override
	public List<Long> getWays() {
		return ways;
	}

	@Override
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

	/** Lee el archivo Carvia.dbf y relaciona el numero de via que trae el Ejes.shp con
	 * los nombres de via que trae el Carvia.dbf. El nombre de via trae tambien el tipo
	 * en formato 2caracteres de tipo de via, un espacio en blanco y el nombre de via
	 * @param v Numero de via a buscar
	 * @return String tipo y nombre de via
	 * @throws IOException
	 */
	public String getVia(long v) throws IOException{
		InputStream inputStream  = new FileInputStream(Config.get("UrbanoSHPDir") + "\\CARVIA\\CARVIA.DBF");
		DBFReader reader = new DBFReader(inputStream); 

		Object[] rowObjects;

		while((rowObjects = reader.nextRecord()) != null) {

			// La posicion 2 es el codigo de via
			if ((Double) rowObjects[2] == (v)){
				inputStream.close();
				// La posicion 3 es la denominacion de via
				
				setTtggss((String) rowObjects[1]);
				
				return ((String) rowObjects[3]);
			}
		}
		return null;
	}  

	public String getTtggss() {
		return null;
	}
	
	public boolean shapeValido (){
		return true;
	}
	
}
