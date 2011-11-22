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


public class ShapeElemlin extends Shape {

	private LineString line; // Linea que representa ese elemlin
	private List<Long> nodes;
	private List<Long>  ways;
	private Long relation;
	private String ttggss; // Campo TTGGSS en Elemlin.shp
	private List<ShapeAttribute> atributos;
	
	public ShapeElemlin(SimpleFeature f) {
		super(f);

		// Elemtex trae la geometria en formato MultiLineString
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiLineString")){

			MultiLineString l = (MultiLineString) f.getDefaultGeometry();
			line = new LineString(l.getCoordinates(),null , 0);

		}
		else {
			System.out.println("Formato geométrico "+ f.getDefaultGeometry().getClass().getName() +" desconocido dentro del shapefile");
		}

		// Los demas atributos son metadatos y de ellos sacamos
		
		ttggss = (String) f.getAttribute("TTGGSS");

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

		if (ttggss != null){
			l.addAll(ttggssParser(ttggss));
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			}
		
		s = new String[2];
		s[0] = "source"; s[1] = "catastro";
		l.add(s);
		s = new String[2];
		s[0] = "add:country"; s[1] = "ES";
		l.add(s);
		
		return l;
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
	public String getTtggss() {
		return ttggss;
	}
	
	public boolean shapeValido (){

		if (ttggss.equals("060402"))
			return false;
		if (ttggss.equals("060202"))
			return false;
		if (ttggss.equals("160300"))
			return false;
		if (ttggss.equals("067121"))
			return false;
		if (ttggss.equals("160101"))
			return false;
		else
			return true;
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

	/** Lee el archivo Carvia.dbf y relaciona el numero de via que trae el Elemlin.shp con
	 * los nombres de via que trae el Carvia.dbf.
	 * @param v Numero de via a buscar
	 * @return String tipo y nombre de via
	 * @throws IOException
	 */
	public String getVia(long v) throws IOException{
		InputStream inputStream  = new FileInputStream(Config.get("UrbanoSHPDir") + "\\CARVIA\\CARVIA.DBF");
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
	
}
