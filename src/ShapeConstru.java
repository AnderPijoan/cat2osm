import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;


public class ShapeConstru extends Shape {

	private String shapeId = null; // Id del shape CONSTRU+long
	private List<LineString> poligons; //[0] Outer, [1..N] inner
	private List<List<Long>> nodes; //[0] Outer, [1..N] inner
	private List<List<Long>> ways; //[0] Outer, [1..N] inner
	private Long relation; // Relacion de sus ways
	private String refCatastral; // Referencia catastral
	private String constru; // Campo Constru solo en Constru.shp
	private List<ShapeAttribute> atributos;

	public ShapeConstru(SimpleFeature f, String tipo) {
		super(f, tipo);

		shapeId = "CONSTRU" + super.newShapeId();

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
			System.out.println("["+new Timestamp(new Date().getTime())+"] Formato geometrico "
		+ f.getDefaultGeometry().getClass().getName() +" desconocido del shapefile CONSTRU");

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

	public String getShapeId(){
		return shapeId;
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

		if (refCatastral != null && !refCatastral.isEmpty()){
			s[0] = "catastro:ref"; s[1] = refCatastral;
			l.add(s);
		}

		if (constru != null && !constru.isEmpty()){
			l.addAll(construParser(constru));
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

	
	/** Parsea el atributo constru entero, este se compone de distintos
	 * elementos separados por el caracter '+'
	 * @param constru Atributo constru
	 * @return Lista con los tags que genera
	 */
	public List<String[]> construParser(String constru){

		List<String[]> l = new ArrayList<String[]>();
		constru = constru.trim();
		String[] construs = constru.split("\\+");
		int alturaMax = -9999;
		int alturaMin = 9999;

		for (String s: construs){
			
			List<String[]> temp = construElemParser(s.toUpperCase());
			
			// Si es un numero, no sabemos si es el de altura superior o inferior
			// por eso lo almacenamos hasta el final.
			if (!temp.isEmpty() && temp.get(0)[0].equals("NUM")) {
				String[] num = temp.get(0);
				alturaMax = (alturaMax>Integer.parseInt(num[1]))? alturaMax : Integer.parseInt(num[1]);
				alturaMin = (alturaMin<Integer.parseInt(num[1]))? alturaMin : Integer.parseInt(num[1]);
			}
			else
			l.addAll(temp);
			}

		// Comparamos si tenemos algun numero almacenado
		if (alturaMax != -9999 && alturaMin != 9999){

			// Si los dos valores han quedado iguales, es que solo se
			// ha recogido un numero, se entiende si es mayor que 0, que alturaMin
			// es 0 y si menor que 0, entonces alturaMax sera 0
			if (alturaMax == alturaMin) {
				alturaMax = (alturaMax>0)? alturaMax : 0;
				alturaMin = (alturaMin<0)? alturaMin : 0;
			}

			String[] s = new String[2];
			s[0] = "building:levels"; s[1] = alturaMax+""; 
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] ="yes";
			l.add(s);
			
			if(alturaMin != 0) {
				s = new String[2];
				s[0] = "building:min_level"; s[1] = alturaMin+"";
				l.add(s);
			}
		}
		
		return l;
	}

	
	/** Parsea cada elemento que ha sido separado
	 * @param elem Elemto a parsear
	 * @return Lista con los tags que genera cada elemento
	 */
	private List<String[]> construElemParser(String elem){

		List<String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];

		switch(elem){
		
		case "B":
			return l;

		case "T":
			return l;

		case "TZA":
			return l;

		case "POR":
			return l;

		case "SOP":
			return l;

		case "PJE":
			return l;

		case "MAR":
			return l;

		case "P":
			return l;

		case "CO":
			s[0] = "building"; s[1] = "warehouse";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "EPT":
			return l;

		case "SS":
			return l;

		case "ALT":
			return l;

		case "PI":
			s[0] = "leisure"; s[1] = "swimming_pool";
			l.add(s);
			return l;

		case "TEN":
			s[0] = "leisure"; s[1] = "pitch";
			l.add(s);
			return l;

		case "ETQ":
			return l;

		case "SILO":
			s[0] = "man_made"; s[1] = "silo";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "SUELO":
		case "TERRENY":
		case "SOLAR":
			s[0] = "landuse"; s[1] = "greenfield";
			l.add(s);
			return l;

		case "PRG":
			return l;

		case "DEP":
			s[0] = "man_made"; s[1] = "storage_tank";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "ESC":
			s[0] = "highway"; s[1] ="steps";
			l.add(s);
			return l;

		case "TRF":
			s[0] = "power"; s[1] ="sub_station";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "JD":
			s[0] = "leisure"; s[1] = "garden";
			l.add(s);
			return l;

		case "YJD":
			s[0] = "leisure"; s[1] = "garden";
			l.add(s);
			return l;

		case "FUT":
			s[0] = "leisure"; s[1] = "stadium";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "VOL":
			return l;

		case "ZD":
			s[0] = "leisure"; s[1] = "sports_centre";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "RUINA":
			s[0] = "ruins"; s[1] = "yes";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "CONS":
			s[0] = "landuse"; s[1] = "construction";
			l.add(s);
			return l;
	
		case "PRESA":
			s[0] = "waterway"; s[1] = "dam";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "ZBE":
			return l;

		case "ZPAV":
			return l;

		case "GOLF":
			s[0] = "leisure"; s[1] = "golf_course";
			l.add(s);
			return l;

		case "CAMPING":
			s[0] = "tourism"; s[1] = "camp_site";
			l.add(s);
			return l;

		case "HORREO":
			return l;

		case "PTLAN":
			s[0] = "man_made"; s[1] = "pier";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		case "DARSENA":
			s[0] = "waterway"; s[1] = "dock";
			l.add(s);
			s = new String[2];
			s[0] = "building"; s[1] = "yes";
			l.add(s);
			return l;

		default: if (!elem.isEmpty()) 
			l.addAll(numRomanoParser(elem));
		}

		return l;

	}

	/** Parsea los numeros romanos del atributo constru
	 * @param elem numero romano a parsear
	 * @return equivalente en numero decimal
	 */
	public List<String[]> numRomanoParser(String elem){

		List<String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];
		String numRomano = elem;
		int sumaTotal = 0;
		boolean negativo = numRomano.startsWith("-");

		for (int x = 0; x < numRomano.length()-2; x++){
			if (numRomano.substring(x, x+3).toUpperCase().equals("III")){
				sumaTotal += 3;
				numRomano = numRomano.replace("III", "");
			}
			else if (numRomano.substring(x, x+3).toUpperCase().equals("XXX")){
				sumaTotal += 30;
				numRomano = numRomano.replace("XXX", "");
			}
			else if (numRomano.substring(x, x+3).toUpperCase().equals("CCC")){
				sumaTotal += 300;
				numRomano = numRomano.replace("CCC", "");
			}
			else if (numRomano.substring(x, x+3).toUpperCase().equals("MMM")){
				sumaTotal += 3000;
				numRomano = numRomano.replace("MMM", "");
			}
		}

		for (int x = 0; x < numRomano.length()-1; x++){
			if (numRomano.substring(x, x+2).toUpperCase().equals("IV")){
				sumaTotal += 4;				
				numRomano = numRomano.replace("IV", "");
			}
			else if (elem.substring(x, x+2).toUpperCase().equals("IX")){
				sumaTotal += 9;				
				numRomano = numRomano.replace("IX", "");
			}
			else if (elem.substring(x, x+2).toUpperCase().equals("XL")){
				sumaTotal += 40;				
				numRomano = numRomano.replace("XL", "");
			}
			else if (numRomano.substring(x, x+2).toUpperCase().equals("XC")){
				sumaTotal += 90;				
				numRomano = numRomano.replace("XC", "");
			}
			else if (numRomano.substring(x, x+2).toUpperCase().equals("CD")){
				sumaTotal += 400;				
				numRomano = numRomano.replace("CD", "");
			}
			else if (numRomano.substring(x, x+2).toUpperCase().equals("CM")){
				sumaTotal += 900;				
				numRomano = numRomano.replace("CM", "");
			}
		}

		for (int x = 0; x < numRomano.length(); x++){
			if (numRomano.substring(x, x+1).toUpperCase().equals("I"))
				sumaTotal += 1;
			else if (numRomano.substring(x, x+1).toUpperCase().equals("V"))
				sumaTotal += 5;
			else if (numRomano.substring(x, x+1).toUpperCase().equals("X"))
				sumaTotal += 10;
			else if (numRomano.substring(x, x+1).toUpperCase().equals("L"))
				sumaTotal += 50;
			else if (numRomano.substring(x, x+1).toUpperCase().equals("C"))
				sumaTotal += 100;
			else if (numRomano.substring(x, x+1).toUpperCase().equals("D"))
				sumaTotal += 500;
			else if (numRomano.substring(x, x+1).toUpperCase().equals("M"))
				sumaTotal += 1000;
		}

		if (negativo)
			sumaTotal = (0 - sumaTotal);

		s[0] = "NUM"; s[1] = sumaTotal+"";
		l.add(s);
		return l;
	}

}


