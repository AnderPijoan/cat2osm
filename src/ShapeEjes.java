import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class ShapeEjes extends Shape {

	private LineString line; // Linea que representa ese eje
	private List<Long> nodes;
	private List<Long> ways;
	private Long relation; // Relacion de sus ways
	private Long via; // Numero de via, solo en Ejes.shp, para enlazar con Carvia
	private List<ShapeAttribute> atributos;
	
	public ShapeEjes(SimpleFeature f) {
		super(f);
		
		// Ejes trae la geometria en formato MultiLineString
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiLineString")){

			MultiLineString l = (MultiLineString) f.getDefaultGeometry();
			line = new LineString(l.getCoordinates(),null , 0);

		}
		else {
			System.out.println("Formato geometrico "+ f.getDefaultGeometry().getClass().getName() +" desconocido dentro del shapefile");
		}
		
		
		if (f.getAttribute("VIA") instanceof Double){
			double v = (Double) f.getAttribute("VIA");
			via = (long) v;
		}
		else if (f.getAttribute("VIA") instanceof Long){
			via = (Long) f.getAttribute("VIA");
		}
		else if (f.getAttribute("VIA") instanceof Integer){
			int v = (Integer) f.getAttribute("VIA");
			via = (long) v;
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
		
		if (via != null){
			s = new String[2];
			s[0] = "via"; s[1] = via.toString();
			l.add(s);
			}
		
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
		nodes.add(nodeId);
	}

	@Override
	public List<Long> getNodesPoligonN(int x, Cat2OsmUtils utils) {
		return nodes;
	}

	@Override
	public void addWay(long wayId) {
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

}
