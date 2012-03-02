import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private static final Map<String,Map<String,String>> lSub = new HashMap<String,Map<String,String>>(); // Mapa <RefCat<ClaveSubparce,CodigoCultivo>> (para el Subparce.shp)
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
			cultivo = getCultivo(refCatastral,subparce);
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
		
		Pattern p = Pattern.compile("\\d{4}-\\d{1,2}");
		Matcher m = p.matcher(Config.get("UrbanoCATFile"));
		if (m.find()) {
		s = new String[2];
		s[0] = "source:date"; s[1] = m.group();
		l.add(s);
		}
			
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

				// La posicion 2 es la referencia catastral
				// La posicion 6 es la clave de la subparcela dentro de esa parcela
				// La posicion 8 es la calificacion catastral = codigo de cultivo
				if (lSub.get(((String) rowObjects[2]).trim()) == null)
					lSub.put(((String) rowObjects[2]).trim(), new HashMap<String,String>());
			
				lSub.get(((String) rowObjects[2]).trim()).put(((String) rowObjects[6]).trim(), ((String) rowObjects[8]).trim());
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
	public String getCultivo(String refCat, String subparce){
		
		if (lSub.get(refCat) == null || lSub.get(refCat).isEmpty())
			return "";
		
		if (lSub.get(refCat).get(subparce) == null || lSub.get(refCat).get(subparce).isEmpty())
		return "";
		
		return lCul.get(lSub.get(refCat).get(subparce));
	} 

	
	public String getTtggss() {
		return null;
	}
	
	
	public boolean shapeValido (){

		if (cultivo == null)
			return true;
		if (cultivo.equals("Vía de comunicación de dominio público"))
			return false;
		else
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
		
		// Valores fijos a los que no anadirles luego mas tags
		if (cultivo.toUpperCase().contains("HIDROGRAFÍA NATURAL") || cultivo.toUpperCase().contains("HIDROGRAFIA NATURAL")){
			s[0] = "natural"; s[1] = "water";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().contains("HIDROGRAFÍA CONSTRUIDA") || cultivo.toUpperCase().contains("HIDROGRAFIA CONSTRUIDA")){
			s[0] = "landuse"; s[1] = "reservoir";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] ="Especificar tipo de agua artificial, eliminar landuse=reservoir y/o comprobar que no este duplicado o contenido en otra geometria de agua.";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().equals("EDIFICACIONES GANADERAS")){
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] ="yes";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().equals("MATORRAL")){
			s = new String[2];
			s[0] = "natural"; s[1] = "scrub";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().equals("VÍA FÉRREA") || cultivo.toUpperCase().equals("VIA FERREA")){
			s[0] = "landuse"; s[1] = "railway";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().equals("CANTERA")){
			s[0] = "landuse"; s[1] = "quarry";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().equals("MONTE BAJO")){
			s[0] = "natural"; s[1] = "scrub";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().equals("ESPARTIZAR") || cultivo.toUpperCase().equals("ATOCHAR")){
			s[0] = "natural"; s[1] = "scrub";
			l.add(s);
			s = new String[2];
			s[0] = "scrub"; s[1] = "esparto";
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
		if (cultivo.toUpperCase().contains("EUCALIPTUS")){
			s[0] = "landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "evergreen";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "eucalyptus";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().contains("PISCIFACTORÍA") || cultivo.toUpperCase().contains("PISCIFACTORIA")){
			s[0] = "landuse"; s[1] = "aquaculture";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().contains("MIMBRERAS") || cultivo.toUpperCase().contains("CAÑAVERALES")){
			s = new String[2];
			s[0] = "wetland"; s[1] = "marsh";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().equals("CAMPING")){
			s[0] = "tourism"; s[1] = "camp_site";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().contains("SALINAS")){
			s[0] = "landuse"; s[1] = "salt_pond";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().contains("POZO") || cultivo.toUpperCase().contains("BALSA") || cultivo.toUpperCase().contains("CHARCA") || cultivo.toUpperCase().contains("SONDEO")){
			s[0] = "landuse"; s[1] = "pond";
			l.add(s);
			return l;
		}
		if (cultivo.toUpperCase().contains("IMPRODUCTIVO")){
		//s = new String[2];
		//s[0] = "fixme"; s[1] = "Suelo Improductivo, tagearlo manualmente a la traduccion que mas se asemeje siguiendo http://wiki.openstreetmap.org/wiki/Map_features.";
		//l.add(s); INNECESARIO E IMPRECISO
		return l;
		}
		
		
		// Valores que se van componiendo de tags poco a poco, para optimizar la incorporacion
		// de nuevos elementos.
		// Estos aguantan hasta el return final
		
		// USO GENERAL
		if (cultivo.toUpperCase().contains("LABOR") || cultivo.toUpperCase().contains("LABRADIO") || cultivo.toUpperCase().contains("LABRADÍO")){
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("HUERTA")){
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "crop"; s[1] = "vegetables";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("PASTOS")){
			s = new String[2];
			s[0] = "landuse"; s[1] = "meadow";
			l.add(s);
			s = new String[2];
			s[0] = "meadow"; s[1] = "perpetual";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("FRUTALES")){
			s = new String[2];
			s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("PLANTAS INDUSTRIALES")){
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("INVERNADERO")){
			s = new String[2];
			s[0] = "landuse"; s[1] = "greenhouse_horticulture";
			l.add(s);
		}
		
		// ESPECIFICAR DEPENDIENDO DEL CULTIVO
		if (cultivo.toUpperCase().contains("ARROZ")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "crop"; s[1] = "rice";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("ALMENDR")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "almond_trees";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("AVELLAN")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "hazels";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("VIÑA") || cultivo.toUpperCase().contains("PARRAL") ){
			s = new String[2];
			s[0] = "landuse"; s[1] = "vineyard";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("OLIVAR") || cultivo.toUpperCase().contains("OLIVO")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "olives";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("CEREAL")){
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "crop"; s[1] = "cereal";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("FRUTAL")){
			s = new String[2];
			s[0] = "trees"; s[1] = "fruit_trees";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("CHUMBERAS")){
			s = new String[2];
			s[0] = "trees"; s[1] = "prickly_pears";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("ALCORNOQUE") || cultivo.toUpperCase().contains("ALCORNOCAL")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "wood"; s[1] = "deciduous";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "deciduous";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "cork_oaks";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("MANZANO")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "apple_trees";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("MELOCOTÓN") || cultivo.toUpperCase().contains("MELOCOTON")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "peach_trees";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("GUINDO") || cultivo.toUpperCase().contains("CEREZO")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "cherry_trees";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("PERAL")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "pear_trees";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("ALBARICOQUE")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "apricot_trees";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("CIRUEL")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "plum_trees";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("ALGARROB")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "carobs";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("FLORES")){
			s = new String[2];
			s[0] = "crop"; s[1] = "flowers";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("HORTALIZAS")){
			s = new String[2];
			s[0] = "crop"; s[1] = "vegetables";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("ORNAMENTACIÓN") || cultivo.toUpperCase().contains("ORNAMENTACION")){
			s = new String[2];
			s[0] = "crop"; s[1] = "flowers";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("CASTAÑ")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "wood"; s[1] = "deciduous";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "deciduous";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "chestnut_trees";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("ENCINA")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "evergreen";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "holm_oaks";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("ROBLE")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "wood"; s[1] = "deciduous";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "oaks";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("HAYA")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "wood"; s[1] = "deciduous";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "deciduous";
			l.add(s);
			s = new String[2];
			s[0] = "beeches"; s[1] = "beeches";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("ABETO")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "wood"; s[1] = "coniferous";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "evergreen";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "firs";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("SABINA")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "wood"; s[1] = "coniferous";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "evergreen";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "tetraclinis";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("ALERCE")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "wood"; s[1] = "coniferous";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "evergreen";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "larches";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("ENEBR")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "wood"; s[1] = "coniferous";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "evergreen";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "junipers";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("PINOS") || cultivo.toUpperCase().contains("PINAR")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "wood"; s[1] = "coniferous";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "evergreen";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "pines";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("ÁRBOLES DE RIBERA") || cultivo.toUpperCase().contains("ARBOLES DE RIBERA")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "wood"; s[1] = "deciduous";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "deciduous";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "aspens";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("PISAPOS") || cultivo.toUpperCase().contains("PINSAPOS")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "wood"; s[1] = "coniferous";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "evergreen";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "spanish_firs";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("ESPECIES MEZCLADAS")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "wood"; s[1] = "mixed";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "mixed";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "pines";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("AGRIOS")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "wood"; s[1] = "coniferous";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("NARANJ")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "orange_trees";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("LIMONE")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "lemon_trees";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("MANDARIN")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "tangerine_trees";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("PALMERA") || cultivo.toUpperCase().contains("PALMITAR")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "palm_trees";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("PLÁTANO") || cultivo.toUpperCase().contains("PLATANO")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "banana_trees";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("HIGUERA")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "fig_trees";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("TOMATE")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "crop"; s[1] = "tomatoes";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("ZUMAQUE")){
			s = new String[2];
			s[0] = "*landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "crop"; s[1] = "sumac";
			l.add(s);
		}
		

				
		
		
		// GANADO
		if (cultivo.toUpperCase().contains("GANADO")){
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "livestock";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("BOVINO")){
			s = new String[2];
			s[0] = "livestock"; s[1] = "cattle";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("CABRIO")){
			s = new String[2];
			s[0] = "livestock"; s[1] = "goats";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("PORCINO")){
			s = new String[2];
			s[0] = "livestock"; s[1] = "pigs";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("LANAR")){
			s = new String[2];
			s[0] = "livestock"; s[1] = "sheep";
			l.add(s);
			s = new String[2];
			s[0] = "produce"; s[1] = "wool";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("BRAVO")){
			s = new String[2];
			s[0] = "produce"; s[1] = "fighting_bulls";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("CARNE")){
			s = new String[2];
			s[0] = "produce"; s[1] = "meat";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains(" CEBO")){
			s = new String[2];
			s[0] = "variety"; s[1] = "fodder-fed";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("LECHE")){
			s = new String[2];
			s[0] = "produce"; s[1] = "milk";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("MIXTO")){
			s = new String[2];
			s[0] = "produce"; s[1] = "milk,meat";
			l.add(s);
		}
		
		// PRODUCEN
		if (cultivo.toUpperCase().contains("MADERABLE")){
			s = new String[2];
			s[0] = "produce"; s[1] = "wood";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("RESINABLE")){
			s = new String[2];
			s[0] = "produce"; s[1] = "resin";
			l.add(s);
		}
		
		// TIPOS DE RIEGO
		if (cultivo.toUpperCase().contains("REGADIO") || cultivo.toUpperCase().contains("REGADÍO") || cultivo.toUpperCase().contains("RIEGO")){
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
		}
		if (cultivo.toUpperCase().contains("SECANO")){
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
		}
		
		if (l.isEmpty() && !cultivo.isEmpty()){
			s[0] = "fixme"; s[1] = "Tagear cultivo "+ cultivo +" en http://wiki.openstreetmap.org/w/index.php?title=Traduccion_metadatos_catastro_a_map_features#Tipos_de_cultivo.";
			l.add(s);
		}
		
		return l;
	}
}
