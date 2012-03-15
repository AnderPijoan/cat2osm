import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opengis.feature.simple.SimpleFeature;

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
	private String cultivo; // Codigo de cultivo de la subparcela
	private List<ShapeAttribute> atributos;
	private long area; // Area para saber si poner landuse allotments (<400m2) o el que sea
	private static final Map<String,Map<String,String>> lSub = new HashMap<String,Map<String,String>>(); // Mapa <RefCat<ClaveSubparce,CodigoCultivo>> (para el Subparce.shp)


	/** Constructor
	 * @param f Linea del archivo shp
	 * @throws IOException 
	 */
	public ShapeSubparce(SimpleFeature f, String tipo) throws IOException {

		super(f, tipo);

		shapeId = "SUBPARCE" + super.newShapeId();

		if (lSub.isEmpty()){
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
		
		// Al campo AREA puede estar en distintos formatos, ogr2ogr cambia el formato
		if (f.getAttribute("AREA") instanceof Double){
			double a = (Double) f.getAttribute("AREA");
			setArea((long) a);
		}
		else if (f.getAttribute("AREA") instanceof Long){
			setArea((Long) f.getAttribute("AREA"));
		}
		else if (f.getAttribute("AREA") instanceof Integer){
			int a = (Integer) f.getAttribute("AREA");
			setArea((long) a);
		}
		else System.out.println("["+new Timestamp(new Date().getTime())+"] No se reconoce el tipo del atributo AREA "
				+ f.getAttribute("AREA").getClass().getName());	
		
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
			return new ArrayList<Long>();
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

		return lSub.get(refCat).get(subparce);
	} 


	public String getTtggss() {
		return "";
	}


	public boolean shapeValido (){

		if (cultivo == null)
			return true;
		if (cultivo.equals("VT"))
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

		switch(cultivo.toUpperCase()){

		case"A-":
		case"A":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "crop"; s[1] = "rice";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"AB":
		case"AK":
		case"HB":
		case"HK":
		case"HR":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "crop"; s[1] = "vegetables";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;


		case"AG":
			return l;

		case"AM":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "almond_trees";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case"AO":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "hazels";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case"AP":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "vineyard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "olives";
			l.add(s);
			return l;

		case"AT":
		case"AY":
		case"AZ":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "natural"; s[1] = "scrub";
			l.add(s);
			s = new String[2];
			s[0] = "scrub"; s[1] = "esparto";
			l.add(s);
			return l;

		case"AV":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "hazels";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"BB":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "livestock";
			l.add(s);
			s = new String[2];
			s[0] = "livestock"; s[1] = "cattle";
			l.add(s);
			s = new String[2];
			s[0] = "produce"; s[1] = "fighting_bulls";
			l.add(s);
			return l;

		case"BC":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "livestock";
			l.add(s);
			s = new String[2];
			s[0] = "livestock"; s[1] = "cattle";
			l.add(s);
			s = new String[2];
			s[0] = "produce"; s[1] = "meat";
			l.add(s);
			return l;

		case"BL":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "livestock";
			l.add(s);
			s = new String[2];
			s[0] = "livestock"; s[1] = "cattle";
			l.add(s);
			s = new String[2];
			s[0] = "produce"; s[1] = "milk";
			l.add(s);
			return l;

		case"BM":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "livestock";
			l.add(s);
			s = new String[2];
			s[0] = "livestock"; s[1] = "cattle";
			l.add(s);
			s = new String[2];
			s[0] = "produce"; s[1] = "milk,meat";
			l.add(s);
			return l;

		case"C-":
		case"C":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case"CA":
			s = new String[2];
			s[0] = "landuse"; s[1] = "quarry";
			l.add(s);
			return l;

		case"CB":
		case"CK":
		case"HT":
		case"HV":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "crop"; s[1] = "cereal";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"CC":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "chestnut_trees";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case"CE":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "holm_oaks";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case"CF":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "fruit_trees";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case"CG":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "oaks";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case"CH":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "prickly_pears";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case"CL":
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "livestock";
			l.add(s);
			s = new String[2];
			s[0] = "livestock"; s[1] = "goats";
			l.add(s);
			s = new String[2];
			s[0] = "produce"; s[1] = "milk";
			l.add(s);
			return l;

		case"CM":
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "livestock";
			l.add(s);
			s = new String[2];
			s[0] = "livestock"; s[1] = "goats";
			l.add(s);
			s = new String[2];
			s[0] = "produce"; s[1] = "meat";
			l.add(s);
			return l;

		case"CN":
		case"CQ":
		case"CT":
		case"CV":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "crop"; s[1] = "cereal";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case "CP":
			return l;

		case"CR":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"CS":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "cork_oaks";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case"CX":
			return l;

		case"CZ":
			s = new String[2];
			s[0] = "tourism"; s[1] = "camp_site";
			l.add(s);
			return l;


		case"E-":
		case"E":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "meadow";
			l.add(s);
			s = new String[2];
			s[0] = "meadow"; s[1] = "perpetual";
			l.add(s);
			return l;

		case"EA":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "use"; s[1] = "agricultural";
			l.add(s);
			return l;

		case"EE":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "meadow";
			l.add(s);
			s = new String[2];
			s[0] = "meadow"; s[1] = "perpetual";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "holm_oaks";
			l.add(s);
			return l;

		case"EG":
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "use"; s[1] = "livestocking";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case"EO":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "meadow";
			l.add(s);
			s = new String[2];
			s[0] = "meadow"; s[1] = "perpetual";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "olives";
			l.add(s);
			return l;

		case"ES":
			return l;

		case"EU":
		case"KI":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "evergreen";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "eucalyptus";
			l.add(s);
			return l;

		case"EV":
			return l;

		case"EX":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmfield";
			return l;

		case"F-":
		case"F":
		case"FY":
		case"FZ":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case"FA":
		case"TF":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			return l;

		case"FB":
		case"FK":
		case"FR":
		case"FV":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"FC":
		case"SC":
		case"ZC":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
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
			return l;

		case"FE":
		case"KE":
		case"SE":
		case"ZE":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "evergreen";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "holm_oaks";
			l.add(s);
			return l;

		case"FF":
			s = new String[2];
			s[0] = "landuse"; s[1] = "railway";
			l.add(s);
			return l;

		case"FG":
		case"KG":
		case"SG":
		case"ZG":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "wood"; s[1] = "deciduous";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "oaks";
			l.add(s);
			return l;

		case"FH":
		case"KH":
		case"SH":
		case"ZH":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "wood"; s[1] = "deciduous";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "beeches";
			l.add(s);
			return l;

		case"FM":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "peach_trees";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"FN":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "apple_trees";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"FO":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "cherry_trees";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"FP":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "pear_trees";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"FQ":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "apricot_trees_trees";
			l.add(s);
			return l;

		case"FS":
		case"ZS":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "cork_oaks";
			l.add(s);
			return l;

		case"FU":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "plum_trees";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"G-":
		case"G":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "carobs";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case"GB":
			s = new String[2];
			s[0] = "landuse"; s[1] = "surface_mining";
			l.add(s);
			return l;

		case"GR":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "carobs";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"HC":
			s = new String[2];
			s[0] = "landuse"; s[1] = "reservoir";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Comprobar si es embalse, acequia o canal...";
			return l;

		case"HE":
		case"HY":
		case"HZ":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "crop"; s[1] = "vegetables";
			l.add(s);
			return l;

		case"HG":
			s = new String[2];
			s[0] = "waterway"; s[1] = "riverbank";
			l.add(s);
			return l;

		case"HS":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "crop"; s[1] = "vegetables";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;


		case"I-":
		case"I":
			return l;

		case"IF":
		case"IO":
			s = new String[2];
			s[0] = "landuse"; s[1] = "greenhouse_horticulture";
			l.add(s);
			s = new String[2];
			s[0] = "crop"; s[1] = "flowers";
			l.add(s);
			return l;

		case"IH":
			s = new String[2];
			s[0] = "landuse"; s[1] = "greenhouse_horticulture";
			l.add(s);
			s = new String[2];
			s[0] = "crop"; s[1] = "vegetables";
			l.add(s);
			return l;

		case"IN":
			s = new String[2];
			s[0] = "landuse"; s[1] = "greenhouse_horticulture";
			l.add(s);
			return l;


		case"IR":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"KA":
		case"MA":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
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
			return l;

		case"KB":
		case"MS":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
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
			return l;

		case"KC":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
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
			return l;

		case"KL":
		case"MH":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
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
			return l;

		case"KN":
		case"ME":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
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
			return l;

		case"KP":
		case"KY":
		case"KZ":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
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
			return l;

		case"KR":
		case"RI":
		case"RY":
		case"RZ":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
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
			return l;

		case"KS":
		case"SS":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
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
			return l;

		case"KX":
		case"MX":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
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
			return l;

		case"LA":
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "livestock";
			l.add(s);
			s = new String[2];
			s[0] = "livestock"; s[1] = "sheep";
			l.add(s);
			s = new String[2];
			s[0] = "produce"; s[1] = "wool";
			l.add(s);
			return l;

		case"LE":
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "livestock";
			l.add(s);
			s = new String[2];
			s[0] = "livestock"; s[1] = "sheep";
			l.add(s);
			s = new String[2];
			s[0] = "produce"; s[1] = "wool";
			l.add(s);
			s = new String[2];
			s[0] = "variety"; s[1] = "entrefino";
			l.add(s);
			return l;

		case"LG":
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "livestock";
			l.add(s);
			s = new String[2];
			s[0] = "livestock"; s[1] = "sheep";
			l.add(s);
			s = new String[2];
			s[0] = "produce"; s[1] = "wool";
			l.add(s);
			s = new String[2];
			s[0] = "variety"; s[1] = "alcudia";
			l.add(s);
			return l;

		case"LM":
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "livestock";
			l.add(s);
			s = new String[2];
			s[0] = "livestock"; s[1] = "sheep";
			l.add(s);
			s = new String[2];
			s[0] = "produce"; s[1] = "wool";
			l.add(s);
			s = new String[2];
			s[0] = "variety"; s[1] = "manchego";
			l.add(s);
			return l;

		case"LP":
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "livestock";
			l.add(s);
			s = new String[2];
			s[0] = "livestock"; s[1] = "sheep";
			l.add(s);
			s = new String[2];
			s[0] = "produce"; s[1] = "wool";
			l.add(s);
			s = new String[2];
			s[0] = "variety"; s[1] = "monte";
			l.add(s);
			return l;

		case"LT":
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "livestock";
			l.add(s);
			s = new String[2];
			s[0] = "livestock"; s[1] = "sheep";
			l.add(s);
			s = new String[2];
			s[0] = "produce"; s[1] = "wool";
			l.add(s);
			s = new String[2];
			s[0] = "variety"; s[1] = "talaverano";
			l.add(s);
			return l;

		case"MB":
		case"MT":
			s = new String[2];
			s[0] = "natural"; s[1] = "scrub";
			l.add(s);
			return l;

		case"MF":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
			l.add(s);
			s = new String[2];
			s[0] = "wood"; s[1] = "mixed";
			l.add(s);
			s = new String[2];
			s[0] = "type"; s[1] = "mixed";
			l.add(s);
			return l;

		case"MI":
			s = new String[2];
			s[0] = "wetland"; s[1] = "marsh";
			l.add(s);
			return l;

		case"MM":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
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
			s = new String[2];
			s[0] = "produce"; s[1] = "wood";
			l.add(s);
			return l;

		case"MP":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
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
			s = new String[2];
			s[0] = "produce"; s[1] = "pine_cones";
			l.add(s);
			return l;

		case"MR":
			s = new String[2];
			s[0] = "landuse"; s[1] = "forest";
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
			s = new String[2];
			s[0] = "produce"; s[1] = "resin";
			l.add(s);
			return l;

		case"MY":
		case"MZ":
			return l;

		case"NB":
		case"NK":
		case"NR":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "citrus_trees";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"NJ":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "orange_trees";
			l.add(s);
			return l;

		case"NL":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "lemon_trees";
			l.add(s);
			return l;

		case"NM":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "tangerine_trees";
			l.add(s);
			return l;

		case"NS":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "citrus_trees";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case"O-":
		case"O":
		case"OY":
		case"TO":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "olives";
			l.add(s);
			return l;

		case"OB":
		case"OK":
		case"OR":
		case"OS":
		case"OV":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "olives";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"OZ":
		case"VO":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "olives";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case"PA":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "crop"; s[1] = "herbs";
			l.add(s);
			return l;

		case"PC":
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "livestock";
			l.add(s);
			s = new String[2];
			s[0] = "livestock"; s[1] = "pigs";
			l.add(s);
			s = new String[2];
			s[0] = "variety"; s[1] = "fooder-fed";
			l.add(s);
			return l;

		case"PD":
		case"PH":
		case"PP":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "meadow";
			l.add(s);
			s = new String[2];
			s[0] = "use"; s[1] = "agricultural";
			l.add(s);
			return l;

		case"PF":
			s = new String[2];
			s[0] = "landuse"; s[1] = "aquaculture";
			l.add(s);
			return l;

		case"PL":
			s = new String[2];
			s[0] = "natural"; s[1] = "scrub";
			l.add(s);
			s = new String[2];
			s[0] = "scrub"; s[1] = "palms";
			l.add(s);
			return l;

		case"PM":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "palm_trees";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case"PR":
		case"PT":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "meadow";
			l.add(s);
			s = new String[2];
			s[0] = "use"; s[1] = "agricultural";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"PV":
			s = new String[2];
			s[0] = "landuse"; s[1] = "farmyard";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "livestock";
			l.add(s);
			s = new String[2];
			s[0] = "livestock"; s[1] = "pigs";
			l.add(s);
			return l;

		case"PZ":
			s = new String[2];
			s[0] = "landuse"; s[1] = "pond";
			l.add(s);
			return l;

		case"R-":
		case"R":
		case"RR":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "fig_trees";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case"SM":
			s = new String[2];
			s[0] = "landuse"; s[1] = "salt_pond";
			l.add(s);
			return l;

		case"TA":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "almond_trees";
			l.add(s);
			return l;

		case"TG":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "carobs";
			l.add(s);
			return l;

		case"TM":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "crop"; s[1] = "tomatoes";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"TN":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "citrus_trees";
			l.add(s);
			return l;

		case"TP":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "orchard";
			l.add(s);
			s = new String[2];
			s[0] = "trees"; s[1] = "banana_trees";
			l.add(s);
			return l;

		case"TR":
			return l;

		case"U-":
		case"U":
			s = new String[2];
			s[0] = "landuse"; s[1] = "residential";
			l.add(s);
			return l;

		case"V-":
		case"V":
			s = new String[2];
			s[0] = "landuse"; s[1] = "vineyard";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		case"VB":
		case"VK":
		case"VP":
		case"VR":
		case"VY":
		case"VZ":
			s = new String[2];
			s[0] = "landuse"; s[1] = "vineyard";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "yes";
			l.add(s);
			return l;

		case"VC":
			return l;

		case"VS":
		case"VV":
			s = new String[2];
			s[0] = "landuse"; s[1] = "vineyard";
			l.add(s);
			return l;
			
		case"VT":
			return l;
			
		case"XX":
			return l;
			
		case"Z-":
			s = new String[2];
			if (this.area <= 400){
				s[0] = "landuse"; s[1] = "allotments";}
			else
				s[0] = "landuse"; s[1] = "farmland";
			l.add(s);
			s = new String[2];
			s[0] = "crop"; s[1] = "sumac";
			l.add(s);
			s = new String[2];
			s[0] = "irrigated"; s[1] = "no";
			l.add(s);
			return l;

		default:
			if (l.isEmpty() && !cultivo.isEmpty()){
				s[0] = "fixme"; s[1] = "Tagear cultivo "+ cultivo +" en http://wiki.openstreetmap.org/w/index.php?title=Traduccion_metadatos_catastro_a_map_features#Tipos_de_cultivo.";
				l.add(s);
			}
			return l;

		}

	}

	public long getArea() {
		return area;
	}

	public void setArea(long area) {
		this.area = area;
	}
}
