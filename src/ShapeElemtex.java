import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;


public class ShapeElemtex extends Shape {

	private String shapeId = null; // Id del shape ELEMTEX+long
	private Coordinate coor;
	private Long nodo;
	private String rotulo; // Campo Rotulo solo en Elemtex.shp
	private String ttggss; // Campo TTGGSS en Elemtex.shp. Se usara para desechar algunos Elemtex y para
	// conocer cuales tienen influencia en las parcelas sobre los que estan colocados (para tags landuse)
	private List<ShapeAttribute> atributos;
	private List<String[]> tags = new ArrayList<String[]>();

	public ShapeElemtex(SimpleFeature f, String tipo) {

		super(f, tipo);

		shapeId = "ELEMTEX" + super.newShapeId();

		// Elemtex trae la geometria en formato MultiLineString
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiLineString")){

			MultiLineString l = (MultiLineString) f.getDefaultGeometry();
			LineString line = new LineString(l.getCoordinates(),null , 0);

			coor = line.getEnvelopeInternal().centre();
		}
		else {
			System.out.println("["+new Timestamp(new Date().getTime())+"] Formato geometrico "+ 
					f.getDefaultGeometry().getClass().getName() +" desconocido dentro del shapefile ELEMTEX");
		}

		// Los demas atributos son metadatos y de ellos sacamos 
		ttggss = (String) f.getAttribute("TTGGSS");

//		try {
//			rotulo = new String(f.getAttribute("ROTULO").toString().getBytes(), "UTF-8");
//			rotulo = eliminarComillas(rotulo);			
//		} catch (UnsupportedEncodingException e) {e.printStackTrace();}
		
		rotulo = eliminarComillas(f.getAttribute("ROTULO").toString());
 
		// Dependiendo del ttggss se usa o no
		if (ttggss != null){
			tags.addAll(ttggssParser(ttggss));
		}

		// Si queremos coger todos los atributos del .shp
//		this.atributos = new ArrayList<ShapeAttribute>();
//		for (int x = 1; x < f.getAttributes().size(); x++){	
//			atributos.add(new ShapeAttribute(f.getFeatureType().getDescriptor(x).getType(), f.getAttributes().get(x)));
//		}

	}


	public String getShapeId(){
		return shapeId;
	}


	public Coordinate getCoor(){
		return coor;
	}

	public void setCoor(Coordinate c){
		this.coor = c;
	}


	public ShapeAttribute getAttribute(int x){	
		return atributos.get(x);
	}


	/** Devuelve los atributos del shape
	 * @return Lista de atributos
	 */
	public List<String[]> getAttributes(){
		
		String[] s = new String[2];
		s[0] = "CAT2OSMSHAPEID"; s[1] = getShapeId();
		tags.add(s);
		
		return tags;
	}


	public String getRotulo() {
		return rotulo;
	}

	public void setRotulo(String rotulo) {
		this.rotulo = rotulo;
	}

	public String getTtggss() {
		return ttggss;
	}

	public void setTtggss(String t) {
		ttggss = t;
	}


	public void addNode(int pos, long nodeId) {
		nodo = nodeId;
	}


	public void addWay(int pos, long wayId) {
	}


	public String getRefCat(){
		return null;
	}


	public Long getRelationId(){
		return null;
	}


	public List<LineString> getPoligons(){
		List<LineString> l = new ArrayList<LineString>();
		return l;
	}


	public Coordinate[] getCoordenadas(int x){
		return null;
	}


	public void deleteWay(int pos, long wayId){
	}

	/** Devuelve la lista de ids de nodos del poligono en posicion pos
	 * @param pos posicion que ocupa el poligono en la lista
	 * @return Lista de ids de nodos del poligono en posicion pos
	 */
	public synchronized List<Long> getNodesIds(int pos){
		List<Long> l = new ArrayList<Long>();
		l.add(nodo);
		return l;
	}

	/** Devuelve la lista de ids de ways
	 * del poligono en posicion pos
	 * @param pos posicion que ocupa el poligono en la lista
	 * @return Lista de ids de ways del poligono en posicion pos
	 */
	public List<Long> getWaysIds(int pos) {
		return new ArrayList<Long>();
	}


	public void setRelation(long relationId){
	}


	public List<Long> getWaysIds(){
		return null;
	}


	/** Traduce el atributo ttggss. Los que tengan ttggss = 0 no se tienen en cuenta
	 * ya que hay elementos textuales que no queremos mostrar. Muchos atributos CONSTRU
	 * de construcciones los han metido mal como elementos textuales, esos son los de longitud
	 * menor a 3 que vamos a desechar
	 * @return Lista de tags que genera
	 */
	public List<String[]> ttggssParser(String ttggss){
		List<String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];
		
		if (rotulo != null && ttggss.equals("189203") && rotulo.length()>2){ 
			s[0] = "place"; s[1] ="locality";
			l.add(s);
			s = new String[2];
			s[0] = "name"; s[1] = rotulo;
			l.add(s);
			
			return l;}

		else if (rotulo != null && ttggss.equals("189300") && rotulo.length()>2){ 
				s = new String[2];
				s[0] = "name"; s[1] = rotulo;
				l.add(s);
			return l;}

		else if (rotulo != null && ttggss.equals("189700") && rotulo.length()>2){ 
			s = new String[2];
			s[0] = "name"; s[1] = rotulo;
			l.add(s);
			return l;}
		
		else if (rotulo != null && ttggss.equals("189401")){ 
			s = new String[2];
			s[0] = "entrance"; s[1] = "yes";
			l.add(s);
			s = new String[2];
			s[0] = "addr:housenumber"; s[1] = rotulo;
			l.add(s);
			
			return l;}
		
		else {
			s = new String[2];
			s[0] = "ttggss"; s[1] = "0";
			l.add(s);
			setTtggss("0");
		return l;
		}
	}


	/** Dependiendo del ttggss desechamos algunos Elemtex que no son necesarios.
	 */
	public boolean shapeValido (){

		if (!Cat2OsmUtils.getOnlyEntrances()){
			// Modo todos los elemtex de Parajes y Comarcas, Informacion urbana 
			// y rustica y Vegetacion y Accidentes demograficos
			if (ttggss.equals("189203"))
				return true;
			else if (ttggss.equals("189300"))
				return true;
			else if (ttggss.equals("189700"))
				return true;
			else
				return false;
		}
		else{
			// Modo solo portales, solamente sacar los portales
			if (ttggss.equals("189401"))
				return true;
			else
				return false;
		}
	}

}
