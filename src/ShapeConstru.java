import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;


public class ShapeConstru extends Shape {

	private Long shapeId = (long) 0; // Id del shape
	private List<LineString> poligons; //[0] Outer, [1..N] inner
	private List<List<Long>> nodes; //[0] Outer, [1..N] inner
	private List<List<Long>> ways; //[0] Outer, [1..N] inner
	private Long relation; // Relacion de sus ways
	private String refCatastral; // Referencia catastral
	private String constru; // Campo Constru solo en Constru.shp
	private List<ShapeAttribute> atributos;

	public ShapeConstru(SimpleFeature f) {
		super(f);

		shapeId = super.newShapeId();
		
		this.poligons = new ArrayList<LineString>();

		// Constru.shp trae la geometria en formato MultiPolygon
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiPolygon")){

			// Poligono, trae el primer punto de cada poligono repetido al final.
			Geometry geom = (Geometry) f.getDefaultGeometry();

			// Cogemos cada poligono del shapefile (por lo general sera uno solo
			// que puede tener algun subpoligono)
			for (int x = 0; x < geom.getNumGeometries(); x++) { 
				Polygon p = (Polygon) geom.getGeometryN(x); 

				// Obtener el outer
				LineString outer = p.getExteriorRing();
				poligons.add(outer);

				// Comprobar si tiene subpoligonos
				for (int y = 0; y < p.getNumInteriorRing(); y++)
					poligons.add(p.getInteriorRingN(y));
			}
		}
		else
			System.out.println("Formato geométrico "+ f.getDefaultGeometry().getClass().getName() +" desconocido dentro del shapefile CONSTRU");

		// Inicializamos las listas
		this.nodes = new ArrayList<List<Long>>();
		this.ways = new ArrayList<List<Long>>();
		for(int x = 0; x < poligons.size(); x++){
			List<Long> lw = new ArrayList<Long>();
			List<Long> ln = new ArrayList<Long>();
			nodes.add(ln);
			ways.add(lw);
		}

		// Los demas atributos son metadatos y de ellos sacamos 
		refCatastral = (String) f.getAttribute("REFCAT");

		constru = (String) f.getAttribute("CONSTRU");

		// Si queremos coger todos los atributos del .shp
		/*this.atributos = new ArrayList<ShapeAttribute>();
			for (int x = 1; x < f.getAttributes().size(); x++){
			atributos.add(new ShapeAttribute(f.getFeatureType().getDescriptor(x).getType(), f.getAttributes().get(x)));
		}*/
	}

	
	public Long getShapeId(){
		return shapeId;
	}
	
	
	public String getShapeIdString(){
		return shapeId.toString();
	}
	
	
	public void addNode(int pos, long nodeId){
		if (poligons.size()>pos)
			nodes.get(pos).add(nodeId);
	}
	
	
	public void addWay(int pos, long wayId){
		if (poligons.size()>pos && !ways.get(pos).contains(wayId))
			ways.get(pos).add(wayId);
	}
	
	
	public synchronized void deleteWay(int pos, long wayId){
		if (poligons.size()>pos)
			ways.get(pos).remove(wayId);
	}
	
	
	public void setRelation(long relationId){
		relation = relationId;
	}
	
	
	public List<LineString> getPoligons(){
		return poligons;
	}
	
	
	/** Devuelve la lista de ids de nodos del poligono en posicion pos
	 * @param pos posicion que ocupa el poligono en la lista
	 * @return Lista de ids de nodos del poligono en posicion pos
	 */
	public synchronized List<Long> getNodesIds(int pos){
		if (nodes.size()>pos)
			return nodes.get(pos);
		else
			return null;
	}
	
	
	/** Devuelve la lista de ids de ways
	 * del poligono en posicion pos
	 * @param pos posicion que ocupa el poligono en la lista
	 * @return Lista de ids de ways del poligono en posicion pos
	 */
	public synchronized List<Long> getWaysIds(int pos) {
		if (nodes.size()>pos)
			return ways.get(pos);
		else
			return null;
	}
	
	
	/** Comprueba la fechaAlta y fechaBaja del shape para ver si se ha creado entre AnyoDesde y AnyoHasta
	 * @param shp Shapefile a comprobar
	 * @return boolean Devuelve si se ha creado entre fechaAlta y fechaBaja o no
	 */
	public boolean checkShapeDate(long fechaDesde, long fechaHasta){
		return (fechaAlta >= fechaDesde && fechaAlta < fechaHasta && fechaBaja >= fechaHasta);
	}

	
	public synchronized Long getRelationId(){
		return relation;
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
		
		if (refCatastral != null){
		s[0] = "catastro:ref"; s[1] = refCatastral;
		l.add(s);
		}
		
		if (constru != null){
			s = new String[2];
			s[0] = "CONSTRU"; s[1] = constru;
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "multipolygon";
			l.add(s);
			}
		
		s = new String[2];
		s[0] = "SHAPEID"; s[1] = getShapeIdString();
		l.add(s);
		
		s = new String[2];
		s[0] = "add:country"; s[1] = "ES";
		l.add(s);
		
		s = new String[2];
		s[0] = "source"; s[1] = "catastro";
		l.add(s);
		
		s = new String[2];
		s[0] = "type"; s[1] = "multipolygon";
		l.add(s);
		
		return l;
	}
	
	
	public String getRefCat(){
		return refCatastral;
	}
	
	
	public String getConstru() {
		return constru;
	}
	
	
	public Coordinate[] getCoordenadas(int i){
		return poligons.get(i).getCoordinates();
	}
	
	
	public Coordinate getCoor(){
		return null;
	}
	
	
	public String getTtggss() {
		return null;
	}
	
	
	public boolean shapeValido (){
		return true;
	}
	
}


