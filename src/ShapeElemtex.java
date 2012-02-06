import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
	private List<String[]> tags;

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
		
		rotulo = eliminarComillas((String) f.getAttribute("ROTULO"));

		// Hay que comprobar el texto para saber si es un shapeValido. Con los demas shapes
		// se suele hacer a la hora de obtener los atributos.
		tags = infoUrbanaParser(rotulo, rotulo);
		
		// Si queremos coger todos los atributos del .shp
		/*this.atributos = new ArrayList<ShapeAttribute>();
		for (int x = 1; x < f.getAttributes().size(); x++){
		atributos.add(new ShapeAttribute(f.getFeatureType().getDescriptor(x).getType(), f.getAttributes().get(x)));
		}*/

	}

	
	public String getShapeId(){
		return shapeId;
	}
	
	
	public Coordinate getCoor(){
		return coor;
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

		s = new String[2];
		s[0] = "source"; s[1] = "catastro";
		l.add(s);
		
		return l;
	}

	
	public String getRotulo() {
		return rotulo;
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
			return null;
	}

	
	public void setRelation(long relationId){
	}

	
	public List<Long> getNodesIds(){
		List<Long> l = new ArrayList<Long>();
		l.add(nodo);
		return l;
	}

	
	public List<Long> getWaysIds(){
		return null;
	}
	
	
	/** Traduce el atributo ttggss. Los que tengan ttggss = 0 no se tienen en cuenta
	 * ya que hay elementos textuales que no queremos mostrar. Puede haber sido modificado
	 * el ttggss a "landuse", eso significa que ese elemento textual tiene que afectar
	 * a la construccion sobre la que se situa. Ya hay un metodo que mas adelante se
	 * encargara de eso si se especifica en el archivo de configuracion.
	 * @return Lista de tags que genera
	 */
	public List<String[]> ttggssParser(String ttggss){
		List<String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];
		
		if ( ttggss.contains("landuse")){ 
			if (rotulo != null){
			l.addAll(tags);
			}
			return l;}
		
		if (ttggss.equals("189203")){ 
			s[0] = "place"; s[1] ="locality";
			l.add(s);
			if (rotulo != null){
			s = new String[2];
			s[0] = "name"; s[1] = rotulo;
			l.add(s);
			}
			return l;}
		
		if (ttggss.equals("189300")){ 
			if (rotulo != null){
			l.addAll(tags);
			}
			return l;}
		
		if (ttggss.equals("189700")){ 
			if (rotulo != null){
			s = new String[2];
			s[0] = "fixme"; s[1] = "rotulo="+rotulo;
			l.add(s);
			}
			return l;}
		
		else{
			s[0] = "fixme"; s[1] = "Documentar nuevo elemento textual ttggss="+ttggss+" y rotulo="+rotulo+" si es preciso en http://wiki.openstreetmap.org/w/index.php?title=Traduccion_metadatos_catastro_a_map_features#Textos_en_Elemtex.shp";
			l.add(s);
			return l;
		}
	}
	

	/** Dependiendo del ttggss desechamos algunos Elemtex que no son necesarios. Al ttggss tambien le 
	 * indicamos poniendo en landuseX si ese shape debe afectar al suelo de la parcela sobre la que se situa.
	 */
	public boolean shapeValido (){
		
		if (ttggss.contains("="))
			return true;
		if (ttggss.equals("189203"))
			return true;
		else if (ttggss.equals("189300"))
			return true;
		else if (ttggss.equals("189700"))
			return true;
		else
			return false;
	}
	
	
	/** Parsea la informacion urbana que viene sin clasificar dentro del ttggss 189300 en los
	 * tags de OpenStreetMap. Es un mecanismo mas manual que habra que ir agrandando a medida
	 * que aparezcan nuevos textos. A los textos les eliminamos TODOS los espacios en blanco ya
	 * que hay poblaciones en las que han separado cada letra del texto para darle enfasis.
	 * Tambien modificamos el ttggss almacenando en el si hay que cambiar algun tag de la geometria
	 * sobre la que se encuentra este texto. (Por ejemplo un cementerio deberia cambiar el landuse
	 * del constru sobre el que se encuentra)
	 * El orden es muy importante (los mas generales al final)
	 * y distinguir entre contains, startsWith y equals
	 * @param rotulo Texto catalogado como informacion urbana sin clasificar.
	 * @return Lista de los tags de OpenStreetMap que representan el texto dado
	 */
	public List<String[]> infoUrbanaParser (String rotulo, String rotuloOriginal){
		List<String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];
		
		rotulo = rotulo.toUpperCase();
		
		// Anadimos el nombre que aparece en catastro
		// Para algunos tipos luego lo borraremos
		s[0] = "name"; s[1] = rotuloOriginal;
		l.add(s);
		s = new String[2];
		
		if (rotulo.contains("AYUNTAMIENTO") || rotulo.contains("AYTO")){ 
			s[0] = "amenity"; s[1] = "townhall";
			l.add(s);
			return l;}
		
		if (rotulo.contains("MUSEO")){ 
			s[0] = "tourism"; s[1] = "museum";
			l.add(s);
			return l;}
		
		if (rotulo.contains("PALACIO")){ 
			s[0] = "building"; s[1] = "palace";
			l.add(s);
			return l;}
		
		if (rotulo.contains("HOTEL") ){ 
			s[0] = "tourism"; s[1] = "hotel";
			l.add(s);
			return l;}
		
		if (rotulo.contains("CORREOS") || rotulo.contains("POSTAL")){ 
			s[0] = "amenity"; s[1] = "post_office";
			l.add(s);
			return l;}
		
		if (rotulo.startsWith("CEMENTERIO")){ 
			s[0] = "amenity"; s[1] = "grave_yard";
			l.add(s);
			setTtggss("landuse=cemetery");
			return l;}
		
		if ((rotulo.contains("AUTOB�S") || rotulo.contains("AUTOBUS") || rotulo.contains("BUS") || rotulo.contains("GUAGUA")) && (rotulo.contains("ESTACION") || rotulo.contains("ESTACI�N "))){ 
			s[0] = "amenity"; s[1] = "bus_station";
			l.add(s);
			return l;}
		
		if (rotulo.startsWith("MARQUESINA")){ 
			l.remove(0);
			s[0] = "highway"; s[1] = "bus_stop";
			l.add(s);
			s = new String[2];
			s[0] = "shelter"; s[1] = "yes";
			l.add(s);
			return l;}
		
		if ((rotulo.contains("TREN") || rotulo.contains("FERROCARRIL") || rotulo.contains("FFCC") || rotulo.contains("FF.CC.")) && (rotulo.contains("ESTACION") || rotulo.contains("ESTACI�N"))){ 
			s[0] = "railway"; s[1] = "station";
			l.add(s);
			return l;}
		
		if (rotulo.contains("AEROPUERTO")){ 
			s[0] = "aeroway"; s[1] = "aerodrome";
			l.add(s);
			return l;}
		
		if (rotulo.contains("FRONTON") || rotulo.contains("FRONT�N")){ 
			s[0] = "sport"; s[1] = "pelota";
			l.add(s);
			return l;}
		
		if (rotulo.startsWith("TEATRO")){ 
			s[0] = "amenity"; s[1] = "theatre";
			l.add(s);
			return l;}
		
		if (rotulo.contains("HOSPITAL")){ 
			s[0] = "amenity"; s[1] = "hospital";
			l.add(s);
			return l;}
		
		if (rotulo.contains("CLINICA") || rotulo.contains("CL�NICA")){ 
			s[0] = "amenity"; s[1] = "doctors";
			l.add(s);
			return l;}		
		
		if (rotulo.contains("AUDITORIO")){ 
			s[0] = "amenity"; s[1] = "community_centre";
			l.add(s);
			return l;}
		
		if (rotulo.contains("PISCINA")){ 
			s[0] = "leisure"; s[1] = "swimming_pool";
			l.add(s);
			return l;}
		
		if (rotulo.contains("ESTANQUE")){ 
			l.remove(0);
			s[0] = "natural"; s[1] = "water";
			l.add(s);
			return l;}
			
		if (rotulo.contains("TRANSFORMADOR") || rotulo.contains("TRF") || rotulo.contains("TRSF")){ 
			l.remove(0);
			s[0] = "power"; s[1] ="sub_station";
			l.add(s);
			return l;}
		
		if (rotulo.contains("APARCAMIENTO") || rotulo.contains("PARKING")){ 
			s[0] = "amenity"; s[1] ="parking";
			l.add(s);
			return l;}
		
		if (rotulo.contains("JUZGADO")){ 
			s[0] = "amenity"; s[1] ="courthouse";
			l.add(s);
			return l;}
		
		if (rotulo.contains("MONASTERIO") || rotulo.contains("CONVENTO")){ 
			s[0] = "amenity"; s[1] ="place_of_worship";
			l.add(s);
			return l;}
		
		if ((rotulo.contains("CAMPO") && (rotulo.contains("FUTBOL") || rotulo.contains("F�TBOL"))) || (rotulo.contains("PISTA") && (rotulo.contains("PADEL") || rotulo.contains("P�DEL")  || rotulo.contains("TENIS"))) || rotulo.contains("CANCHA")){ 
			s[0] = "leisure"; s[1] = "pitch";
			l.add(s);
			return l;}
		
		if (rotulo.contains("CAMPO") && rotulo.contains("GOLF")){ 
			s[0] = "leisure"; s[1] = "golf_course";
			l.add(s);
			return l;}
		
		if (rotulo.contains("ESTADIO")){ 
			s[0] = "leisure"; s[1] = "stadium";
			l.add(s);
			return l;}
		
		if (rotulo.contains("POLIDEPORTIVO") || rotulo.contains("DEPORTE") || rotulo.contains("DEPORTIVO")){ 
			s[0] = "leisure"; s[1] = "sports_centre";
			l.add(s);
			return l;}
		
		if (rotulo.contains("JUEGOS")){ 
			s[0] = "leisure"; s[1] ="playground";
			l.add(s);
			return l;}
		
		if (rotulo.contains("BOMBEROS")){ 
			s[0] = "amenity"; s[1] = "fire_station";
			l.add(s);
			return l;}
		
		if (rotulo.contains("CENTRO") && rotulo.contains("COMERCIAL") ){ 
			s[0] = "shop"; s[1] = "mall";
			l.add(s);
			return l;}
		
		if (rotulo.contains("SUPERMERCADO") || rotulo.contains("HIPERMERCADO") || rotulo.contains("HIPER")){ 
			s[0] = "shop"; s[1] = "supermarket";
			l.add(s);
			return l;}
		
		if (rotulo.contains("MERCADO")){ 
			s[0] = "amenity"; s[1] = "marketplace";
			l.add(s);
			return l;}
		
		if (rotulo.contains("GASOLINERA")){ 
			s[0] = "amenity"; s[1] = "fuel";
			l.add(s);
			return l;}
		
		if (rotulo.contains("BIBLIOTECA")){ 
			s[0] = "amenity"; s[1] = "library";
			l.add(s);
			return l;}
		
		if (rotulo.contains("GUARDERIA") || rotulo.contains("GUARDER�A")){ 
			s[0] = "amenity"; s[1] = "kindergarten";
			l.add(s);
			return l;}
		
		if (rotulo.contains("FACULTAD") || rotulo.contains("UNIVERSIDAD") || rotulo.contains("UNIVERSITARI")){ 
			s[0] = "amenity"; s[1] = "university";
			l.add(s);
			return l;}
		
		if (rotulo.contains("ESCOLAR") || rotulo.contains("ESCUELA") || rotulo.contains("COLEGIO")){ 
			s[0] = "amenity"; s[1] = "school";
			l.add(s);
			return l;}

		if (rotulo.contains("INSTITUTO")){
			s[0] = "amenity"; s[1] = "college";
			l.add(s);
			return l;}
		
		if (rotulo.contains("CENTRO") && rotulo.contains("SOCIAL")){ 
			s[0] = "amenity"; s[1] = "social_facility";
			l.add(s);
			return l;}
		
		if (rotulo.contains("CASA") && rotulo.contains("RURAL")){ 
			s[0] = "tourism"; s[1] = "chalet";
			l.add(s);
			return l;}
			 
		if (rotulo.contains("IGLESIA")){ 
			s[0] = "amenity"; s[1] = "place_of_worship";
			l.add(s);
			s = new String[2];
			s[0] = "denomination"; s[1] = "catholic";
			l.add(s);
			s = new String[2];
			s[0] = "religion"; s[1] = "christian";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "church";
			l.add(s);
			return l;}
		
		if (rotulo.contains("CATEDRAL")){
			s[0] = "amenity"; s[1] = "place_of_worship";
			l.add(s);
			s = new String[2];
			s[0] = "denomination"; s[1] = "catholic";
			l.add(s);
			s = new String[2];
			s[0] = "religion"; s[1] = "christian";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "cathedral";
			l.add(s);
			return l;} 
		
		if (rotulo.contains("CAPILLA") || rotulo.contains("ERMITA")){
			s[0] = "amenity"; s[1] = "place_of_worship";
			l.add(s);
			s = new String[2];
			s[0] = "denomination"; s[1] = "catholic";
			l.add(s);
			s = new String[2];
			s[0] = "religion"; s[1] = "christian";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "chapel";
			l.add(s);
			return l;} 
		
		if (rotulo.contains("REFUGIO")){ 
			s[0] = "amenity"; s[1] = "shelter";
			l.add(s);
			return l;}
		
		if (rotulo.contains("ESTABLO")){ 
			s[0] = "building"; s[1] = "stable";
			l.add(s);
			return l;}
		
		if (rotulo.contains("INVERNAL")){ 
			s[0] = "tourism"; s[1] = "alpine_hut";
			l.add(s);
			return l;}
		
		if (rotulo.contains("ALMACEN") || rotulo.contains("ALMAC�N") || rotulo.equals("NAVE")){ 
			s[0] = "building"; s[1] = "warehouse";
			l.add(s);
			return l;}
		
		if (rotulo.contains("TALLER")){ 
			s[0] = "shop"; s[1] = "car_repair";
			l.add(s);
			return l;}
		
		if (rotulo.contains("BOMBEO")){
			l.remove(0);
			s[0] = "man_made"; s[1] = "pumping_station";
			l.add(s);
			return l;}
		
		if (rotulo.contains("ALBERGUE")){ 
			s[0] = "tourism"; s[1] = "hostel";
			l.add(s);
			return l;}
		
		if (rotulo.startsWith("POZO")){ 
			l.remove(0);
			s[0] = "man_made"; s[1] = "water_well";
			l.add(s);
			return l;}
		
		if (rotulo.startsWith("PAJAR")){ 
			l.remove(0);
			s[0] = "building"; s[1] = "barn";
			l.add(s);
			return l;}
		
		if (rotulo.contains("DEPOSITO") || rotulo.contains("DEP�SITO")){ 
			l.remove(0);
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			return l;} 
		
		if (rotulo.contains("DEPURADORA")){ 
			l.remove(0);
			s[0] = "man_made"; s[1] = "wastewater_plant";
			l.add(s);
			return l;} 
		
		if (rotulo.contains("LAVADERO")){
			s[0] = "amenity"; s[1] = "public_building";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "wash-house";
			l.add(s);
			return l;}
		
		if (rotulo.contains("MERENDERO")){
			s[0] = "tourism"; s[1] = "picnic_site";
			l.add(s);
			return l;}
		
		if (rotulo.contains("MUELLE")){
			s[0] = "waterway"; s[1] = "dock";
			l.add(s);
			return l;}
		
		if (rotulo.startsWith("FUENTE")){
			l.remove(0);
			s[0] = "amenity"; s[1] = "fountain";
			l.add(s);
			return l;}
		
		if (rotulo.startsWith("FARO") ){ 
			l.remove(0);
			s[0] = "man_made"; s[1] = "lighthouse";
			l.add(s);
			return l;}	 
		
		if (rotulo.startsWith("PARQUE")){ 
			s[0] = "leisure"; s[1] = "park";
			l.add(s);
			return l;}
		
		if (rotulo.startsWith("PLAYA")){ 
			s[0] = "natural"; s[1] = "beach";
			l.add(s);
			return l;}
		
		if (rotulo.contains("CAFETERIA") || rotulo.contains("CAFETER�A")){ 
			s[0] = "amenity"; s[1] = "cafe";
			l.add(s);
			return l;}
		
		if (rotulo.contains("CAMPING")){ 
			s[0] = "tourism"; s[1] = "camp_site";
			l.add(s);
			return l;}
		
		if (rotulo.contains("RESTAURANTE") || rotulo.contains("RTE.")){ 
			s[0] = "amenity"; s[1] = "restaurant";
			l.add(s);
			return l;}
		
		if (rotulo.contains("BASCULA") || rotulo.contains("B�SCULA")){ 
			s[0] = "man_made"; s[1] = "weighbridge";
			l.add(s);
			return l;}
		
		if (rotulo.contains("BEBEDERO")){ 
			s[0] = "amenity"; s[1] = "watering_place";
			l.add(s);
			return l;}
		
		if (rotulo.startsWith("CINE")){ 
			s[0] = "amenity"; s[1] = "cinema";
			l.add(s);
			return l;}
		
		if (rotulo.equals("BAR")){ 
			s[0] = "amenity"; s[1] = "bar";
			l.add(s);
			return l;}
		
		if (rotulo.equals("SILO")){
			l.remove(0);
			s[0] = "man_made"; s[1] = "silo";
			l.add(s);
			return l;}
		
		if (rotulo.equals("TORRE")){ 
			s[0] = "man_made"; s[1] = "tower";
			l.add(s);
			return l;}
		
		else {
			setTtggss("0");
			return l;
		}
	}
	
	
	
}
