import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;


public class ShapeElempun extends Shape {

	private Coordinate coor;
	private Long nodo;
	private String ttggss; // Campo TTGGSS solo en Elempun.shp
	private List<ShapeAttribute> atributos;

	public ShapeElempun(SimpleFeature f) {
		super(f);

		// Elempun trae la geometria en formato Point
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.Point")){

			Point p = (Point) f.getDefaultGeometry();

			coor = p.getCoordinate();
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

	}

	/** Comprueba la fechaAlta y fechaBaja del shape para ver si se ha creado entre AnyoDesde y AnyoHasta
	 * @param shp Shapefile a comprobar
	 * @return boolean Devuelve si se ha creado entre fechaAlta y fechaBaja o no
	 */
	public boolean checkShapeDate(long fechaDesde, long fechaHasta){
		return (fechaAlta >= fechaDesde && fechaAlta < fechaHasta && fechaBaja >= fechaHasta);
	}

	public ShapeAttribute getAttribute(int x){	
		return atributos.get(x);
	}

	/** Devuelve los atributos del shape
	 * @return Lista de atributos
	 */
	@Override
	public List<String[]> getAttributes() {
		List <String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];

		if (ttggss != null){
			l.addAll(ttggssParser(ttggss));
			}
		return l;
	}

	@Override
	public String getRefCat() {
		return null;
	}

	@Override
	public Long getRelation() {
		return null;
	}

	@Override
	public List<LineString> getPoligons() {
		return null;
	}

	@Override
	public Coordinate[] getCoordenadas(int x) {
		return null;
	}

	@Override
	public void addNode(long nodeId) {
	}

	@Override
	public List<Long> getNodesPoligonN(int x, Cat2OsmUtils utils) {
		List<Long> l = new ArrayList<Long>();
		l.add(nodo);
		return l;
	}

	@Override
	public void addWay(long wayId) {
	}

	@Override
	public List<Long> getWaysPoligonN(int x, Cat2OsmUtils utils) {
		return null;
	}

	@Override
	public void setRelation(long relationId) {
	}

	@Override
	public List<Long> getNodes() {
		List<Long> l = new ArrayList<Long>();
		l.add(nodo);
		return l;
	}

	@Override
	public List<Long> getWays() {
		return null;
	}

	@Override
	public Coordinate getCoor() {
		return coor;
	}

}
