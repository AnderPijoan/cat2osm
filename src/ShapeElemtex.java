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
	private String ttggss; // Campo TTGGSS en Elemtex.shp
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
			System.out.println("Formato geométrico "+ f.getDefaultGeometry().getClass().getName() +" desconocido dentro del shapefile ELEMTEX");
		}

		// Los demas atributos son metadatos y de ellos sacamos 
		ttggss = (String) f.getAttribute("TTGGSS");
		
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
		
		if (ttggss != null){
			l.addAll(ttggssParser(ttggss));
			}

		//s = new String[2];
		//s[0] = "FECHAALTA"; s[1] = String.valueOf(fechaAlta);
		//l.add(s);

		//s = new String[2];
		//s[0] = "FECHABAJA"; s[1] = String.valueOf(fechaBaja);
		//l.add(s);

		s = new String[2];
		s[0] = "source"; s[1] = "catastro";
		l.add(s);
		s = new String[2];
		s[0] = "add:country"; s[1] = "ES";
		l.add(s);
		
		return l;
	}

	public String getRotulo() {
		return rotulo;
	}
	
	public String getTtggss() {
		return ttggss;
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
	
	
	/** Comprueba si es un ttggss valido. Hay elementos textuales que no queremos mostrar
	 * @return
	 */
	public List<String[]> ttggssParser(String ttggss){
		List<String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];
		
		if (ttggss.equals("189203")){ 
			s[0] = "place"; s[1] ="locality";
			l.add(s);
			if (rotulo != null){
			s = new String[2];
			s[0] = "name"; s[1] = rotulo;
			l.add(s);
			}
			return l;}
		
		if (ttggss.equals("189102")){ 
			if (rotulo != null){
			s = new String[2];
			s[0] = "addr:housenumber"; s[1] = rotulo;
			l.add(s);
			}
			return l;}
		
		if (ttggss.equals("189300")){ 
			if (rotulo != null){
			s = new String[2];
			System.out.println(rotulo);
			s[0] = "addr:housenumber"; s[1] = rotulo;
			l.add(s);
			}
			return l;}
		
		else{
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		}
	}
	
	public boolean shapeValido (){

		if (ttggss.equals("189300"))
			return true;
		else if (ttggss.equals("189700"))
			return true;
		else if (ttggss.equals("189203"))
			return true;
		else
			return false;
	}
	
}
