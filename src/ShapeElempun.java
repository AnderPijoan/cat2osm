import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;


public class ShapeElempun extends Shape {
	
	private String shapeId = null; // Id del shape ELEMPUN+long
	private Coordinate coor;
	private Long nodo;
	private String ttggss; // Campo TTGGSS en Elempun.shp
	private List<ShapeAttribute> atributos;

	public ShapeElempun(SimpleFeature f, String tipo) {
		
		super(f, tipo);

		shapeId = "ELEMPUN" + super.newShapeId();
		
		// Elempun trae la geometria en formato Point
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.Point")){

			Point p = (Point) f.getDefaultGeometry();

			coor = p.getCoordinate();
		}
		else {
			System.out.println("["+new Timestamp(new Date().getTime())+"] Formato geometrico "+ 
		f.getDefaultGeometry().getClass().getName() +" desconocido dentro del shapefile ELEMPUN");
		}

		// Los demas atributos son metadatos y de ellos sacamos 

		ttggss = (String) f.getAttribute("TTGGSS");

		// Si queremos coger todos los atributos del .shp
		/*this.atributos = new ArrayList<ShapeAttribute>();
		for (int x = 1; x < f.getAttributes().size(); x++){
		atributos.add(new ShapeAttribute(f.getFeatureType().getDescriptor(x).getType(), f.getAttributes().get(x)));
		}*/
	}

	
	public String getShapeId(){
		return shapeId;
	}
	
	
	public ShapeAttribute getAttribute(int x){	
		return atributos.get(x);
	}

	
	/** Devuelve los atributos del shape
	 * @return Lista de atributos
	 */
	public List<String[]> getAttributes() {
		List <String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];

		if (ttggss != null){
			l.addAll(ttggssParser(ttggss));
			}

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


	public String getRefCat() {
		return null;
	}


	public Long getRelationId() {
		return null;
	}


	public List<LineString> getPoligons() {
		List<LineString> l = new ArrayList<LineString>();
		return l;
	}


	public Coordinate[] getCoordenadas(int x) {
		return null;
	}


	public String getTtggss() {
		return ttggss;
	}
	
	
	public boolean shapeValido (){
		
		if (ttggss.equals("030202"))
			return true;
		if (ttggss.equals("030302"))
			return true;
		if (ttggss.equals("037101"))
			return true;
		if (ttggss.equals("038101"))
			return true;
		if (ttggss.equals("038102"))
			return true;
		if (ttggss.equals("037102"))
			return true;
		if (ttggss.equals("068401"))
			return true;
		if (ttggss.equals("167111"))
			return true;
		if (ttggss.equals("167201"))
			return true;
		if (ttggss.equals("168103"))
			return true;
		if (ttggss.equals("168116"))
			return true;
		if (ttggss.equals("168153"))
			return true;
		if (ttggss.equals("168168"))
			return true;
		if (ttggss.equals("168113"))
			return true;
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
		if (ttggss.equals("115101"))
			return false;
		if (ttggss.equals("168100"))
			return false;
		else
			return true;
	}
	
	
	public void addNode(int pos, long nodeId){
		nodo = nodeId;
	}

	
	public void addWay(int pos, long wayId){
	}
	
	
	public void deleteWay(int pos, long wayId){
	}

	
	public void setRelation(long relationId){
	}


	/** Devuelve la lista de ids de nodos del poligono en posicion pos
	 * @param pos posicion que ocupa el poligono en la lista
	 * @return Lista de ids de nodos del poligono en posicion pos
	 */
	public List<Long> getNodesIds(int pos){
		List<Long>l = new ArrayList<Long>();
		l.add(nodo);
		return l;
	}

	/** Devuelve la lista de ids de ways
	 * del poligono en posicion pos
	 * @param pos posicion que ocupa el poligono en la lista
	 * @return Lista de ids de ways del poligono en posicion pos
	 */
	public List<Long> getWaysIds(int pos) {
		List<Long> l = new ArrayList<Long>();
			return l;
	}


	public Coordinate getCoor() {
		return coor;
	}

}
