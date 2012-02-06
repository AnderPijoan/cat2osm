import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opengis.feature.simple.SimpleFeature;

import com.linuxense.javadbf.DBFException;
import com.linuxense.javadbf.DBFReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;


public class ShapeSubparce extends Shape {

	private String shapeId = null; // Id del shape SUBPARCE+long
	private List<LineString> poligons; //[0] Outer, [1..N] inner
	private List<List<Long>> nodes; //[0] Outer, [1..N] inner
	private List<List<Long>> ways; //[0] Outer, [1..N] inner
	private Long relation; // Relacion de sus ways
	private String refCatastral; // Referencia catastral
	private String subparce; // Clave de Subparcela
	private String cultivo; // Tipo de cultivo de la subparcela
	private List<ShapeAttribute> atributos;
	private static final Map<String,String> lSub = new HashMap<String,String>(); // Lista de subparce y calificacion (para el Subparce.shp)
	private static final Map<String,String> lCul = new HashMap<String,String>(); // Lista de cc y denominacion (para el Subparce.shp)


	/** Constructor
	 * @param f Linea del archivo shp
	 * @throws IOException 
	 */
	public ShapeSubparce(SimpleFeature f, String tipo) throws IOException {
		
		super(f, tipo);
		
		shapeId = "SUBPARCE" + super.newShapeId();
		
		if (lSub.isEmpty() || lCul.isEmpty()){
			readSubparceDetails(tipo);
		}
		
		this.poligons = new ArrayList<LineString>();

		// Parcela.shp trae la geometria en formato MultiPolygon
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
			System.out.println("["+new Timestamp(new Date().getTime())+"] Formato geometrico "+ 
		f.getDefaultGeometry().getClass().getName() +" desconocido dentro del shapefile SUBPARCE");

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
		subparce = (String) f.getAttribute("SUBPARCE");
		if (subparce != null){
			cultivo = getCultivo(subparce);
		}
		
		// Si queremos coger todos los atributos del .shp
		/*this.atributos = new ArrayList<ShapeAttribute>();
		for (int x = 1; x < f.getAttributes().size(); x++){
		atributos.add(new ShapeAttribute(f.getFeatureType().getDescriptor(x).getType(), f.getAttributes().get(x)));
		}*/
	}


	public String getShapeId(){
		return shapeId;
	}
	
	
	public void addNode(int pos, long nodeId){
		if (nodes.size()>pos)
			nodes.get(pos).add(nodeId);
	}

	public void addWay(int pos, long wayId){
		if (ways.size()>pos && !ways.get(pos).contains(wayId))
			ways.get(pos).add(wayId);
	}


	public synchronized void deleteWay(int pos, long wayId){
		if (ways.size()>pos)
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


	public Long getRelationId(){
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
			s = new String[2];
			s[0] = "catastro:ref"; s[1] = refCatastral;
			l.add(s);
		}

		if (cultivo != null){
			l.addAll(cultivoParser(cultivo));
		}
		
		s = new String[2];
		s[0] = "CAT2OSMSHAPEID"; s[1] = getShapeId();
		l.add(s);
		
		s = new String[2];
		s[0] = "source"; s[1] = "catastro";
		l.add(s);
			
		return l;
	}

	
	public String getRefCat(){
		return refCatastral;
	}
	
	
	public String getSubparce(){
		return subparce;
	}

	
	public Coordinate[] getCoordenadas(int i){
		return poligons.get(i).getCoordinates();
	}

	
	public Coordinate getCoor(){
		return null;
	}
	
	
	/** Lee el archivo Rusubparcela.dbf y lo almacena para despues relacionar la clave subparce 
	 * de Subparce.shp con la calificacion catastral que trae Rusubparcela.dbf. Con la cc se accedera
	 * al rucultivo.dbf. Se supone que estos archivos solo existen en el caso de subparcelas rusticas,
	 * por si acaso se pasa el tipo para futuras mejoras.
	 * @throws IOException 
	 */
	public void readSubparceDetails(String tipo) throws IOException {
		
		if (tipo.equals("RU")){
			InputStream inputStream = new FileInputStream(Config.get("RusticoSHPPath") + "/RUSUBPARCELA/RUSUBPARCELA.DBF");
			DBFReader reader = new DBFReader(inputStream);
			Object[] rowObjects;

			while((rowObjects = reader.nextRecord()) != null) {

				// La posicion 6 es el codigo subparce
				// La posicion 8 es la calificacion catastral
				lSub.put(((String) rowObjects[6]).trim(), ((String) rowObjects[8]).trim());

			}
			inputStream.close();

			inputStream = new FileInputStream(Config.get("RusticoSHPPath") + "/RUCULTIVO/RUCULTIVO.DBF");
			reader = new DBFReader(inputStream);

			while((rowObjects = reader.nextRecord()) != null) {

				// La posicion 1 es la calificacion catastral
				// La posicion 2 es la denominacion
				lCul.put(((String) rowObjects[1]).trim(), ((String) rowObjects[2]).trim());

			}
			inputStream.close();
		}
	}  
	
	
	/**Relaciona el codigo de subparcela que trae el Subparce.shp con
	 * el codigo de cultivo que trae Rusubparcela.dbf y este a su vez con el 
	 * Rucultivo.dbf. Solo es para subparcelas rurales
	 * @param v Numero de subparcela a buscar
	 * @return String tipo de cultivo
	 */
	public String getCultivo(String s){
		return lCul.get(lSub.get(s));
	} 

	
	public String getTtggss() {
		return null;
	}
	
	
	public boolean shapeValido (){
		return true;
	}
	
	/** Parsea el tipo de cultivo con la nomenclatura de catastro y lo convierte
	 * a los tags de OSM
	 * @param cultivo Cultivo con la nomenclatura de catastro
	 * @return Lista de los tags que genera
	 */
	public List<String[]> cultivoParser(String cultivo){
		List <String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];
		
		if (cultivo.toUpperCase().contains("PASTOS")){
			s[0] = "landuse"; s[1] = "meadow";
			l.add(s);
			s = new String[2];
			s[0] = "meadow"; s[1] = "perpetual";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().contains("PRADOS") || cultivo.toUpperCase().contains("PRADERAS")){
			s[0] = "landuse"; s[1] = "meadow";
			l.add(s);
			s = new String[2];
			s[0] = "meadow"; s[1] = "agricultural";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().contains("LABOR") || (cultivo.toUpperCase().contains("LABRADIO") && cultivo.toUpperCase().contains("SECANO"))){
			s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().contains("IMPRODUCTIVO")){
			s[0] = "natural"; s[1] = "scree";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().contains("MATORRAL")){
			s[0] = "natural"; s[1] = "scrub";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().contains("OLIVOS")){
			s[0] = "natural"; s[1] = "orchard";
			l.add(s);
			return l;
		}
		else {
			s[0] = "fixme"; s[1] = "Documentar nuevo cultivo landuse="+ cultivo +" en http://wiki.openstreetmap.org/w/index.php?title=Traduccion_metadatos_catastro_a_map_features#Tipos_de_cultivo";
			l.add(s);
		}
		
		return l;
	}
}
