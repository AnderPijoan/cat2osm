import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;


public class ShapeElemtex extends Shape {

	private Coordinate coor;
	private Long nodo;
	private String rotulo; // Campo Rotulo solo en Elemtex.shp
	private List<ShapeAttribute> atributos;

	public ShapeElemtex(SimpleFeature f) {
		super(f);

		// Elemtex trae la geometria en formato MultiLineString
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiLineString")){

			MultiLineString l = (MultiLineString) f.getDefaultGeometry();
			LineString line = new LineString(l.getCoordinates(),null , 0);

			coor = line.getEnvelopeInternal().centre();
		}
		else {
			System.out.println("Formato geométrico desconocido dentro del shapefile");
		}

		// Los demas atributos son metadatos y de ellos sacamos 

		rotulo = new String();
		String r = (String) f.getAttribute("ROTULO");
		for (int x = 0; x < r.length(); x++)
			if (r.charAt(x) != '"') rotulo += r.charAt(x);

		// Si queremos coger todos los atributos del .shp
		/*this.atributos = new ArrayList<ShapeAttribute>();
		for (int x = 1; x < f.getAttributes().size(); x++){
		atributos.add(new ShapeAttribute(f.getFeatureType().getDescriptor(x).getType(), f.getAttributes().get(x)));
		}*/

	}

	public Coordinate getCoor(){
		return coor;
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
	public List<String[]> getAttributes(){
		List <String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];

		if (rotulo != null){
			s = new String[2];
			s[0] = "addr:housenumber"; s[1] = rotulo;
			l.add(s);
		}

		//s = new String[2];
		//s[0] = "FECHAALTA"; s[1] = String.valueOf(fechaAlta);
		//l.add(s);

		//s = new String[2];
		//s[0] = "FECHABAJA"; s[1] = String.valueOf(fechaBaja);
		//l.add(s);

		return l;
	}

	public String getRotulo() {
		return rotulo;
	}
	
	public void addNode(long nodeId){
		nodo = nodeId;
	}
	
	public String getRefCat(){
		return "";
	}

	public Long getRelation(){
		return null;
	}

	public List<LineString> getPoligons(){
		return null;
	}

	public Coordinate[] getCoordenadas(int x){
		return null;
	}

	public List<Long> getNodesPoligonN(int x, Cat2OsmUtils utils){
		return null;
	}

	public void addWay(long wayId){	
	}

	public List<Long> getWaysPoligonN(int x, Cat2OsmUtils utils){
		return null;
	}

	public void setRelation(long relationId){
	}

	public List<Long> getNodes(){
		List<Long> l = new ArrayList<Long>();
		l.add(nodo);
		return l;
	}

	public List<Long> getWays(){
		return null;
	}
	
}
