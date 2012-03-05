import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

public abstract class Shape {

	private List<ShapeAttribute> atributos;
	protected long fechaAlta; // Formato AAAAMMDD
	protected long fechaBaja;
	protected volatile static Long Id = (long) 0; // Id que tomaran los shapes
	// para la simplificacion de ways


	/**Constructor
	 * @param f Linea del archivo shp
	 */
	public Shape(SimpleFeature f, String tipo){

		// Algunos conversores de DATUM cambian el formato de double a int en el .shp
		// FECHAALATA y FECHABAJA siempre existen
		if (f.getAttribute("FECHAALTA") instanceof Double){
			double fa = (Double) f.getAttribute("FECHAALTA");
			fechaAlta = (long) fa;
		}
		else if (f.getAttribute("FECHAALTA") instanceof Long){
			fechaAlta = (Long) f.getAttribute("FECHAALTA");
		}
		else if (f.getAttribute("FECHAALTA") instanceof Integer){
			int fa = (Integer) f.getAttribute("FECHAALTA");
			fechaAlta = (long) fa;
		}
		else System.out.println("["+new Timestamp(new Date().getTime())+"] No se reconoce el tipo del atributo FECHAALTA "
				+ f.getAttribute("FECHAALTA").getClass().getName());	

		if (f.getAttribute("FECHABAJA") instanceof Integer){
			int fb = (Integer) f.getAttribute("FECHABAJA");
			fechaBaja = (long) fb;
		}
		else  if (f.getAttribute("FECHABAJA") instanceof Double){
			double fb = (Double) f.getAttribute("FECHABAJA");
			fechaBaja = (long) fb;
		}
		else if (f.getAttribute("FECHABAJA") instanceof Long){
			fechaBaja = (Long) f.getAttribute("FECHABAJA");
		}
		else System.out.println("["+new Timestamp(new Date().getTime())+"] No se reconoce el tipo del atributo FECHABAJA"
				+ f.getAttribute("FECHABAJA").getClass().getName());

		// Si queremos coger todos los atributos del .shp
		/*this.atributos = new ArrayList<ShapeAttribute>();
		for (int x = 1; x < f.getAttributes().size(); x++){
		atributos.add(new ShapeAttribute(f.getFeatureType().getDescriptor(x).getType(), f.getAttributes().get(x)));
		}*/

	}


	/** Comprueba la fechaAlta y fechaBaja del shape para ver si se ha creado entre AnyoDesde y AnyoHasta
	 * Deben seguir dados de alta despues de fechaHasta para que los devuelva. Es decir, shapes que se hayan
	 * creado y dado de baja en ese intervalo no las devolvera.
	 * @param fechaDesde fecha a partir de la cual se cogeran los shapes
	 * @param fechaHasta fecha hasta la cual se cogeran
	 * @return boolean Devuelve si se ha creado entre fechaAlta y fechaBaja o no
	 */
	public boolean checkShapeDate(long fechaDesde, long fechaHasta){
		return (fechaAlta >= fechaDesde && fechaAlta < fechaHasta && fechaBaja >= fechaHasta);
	}


	/** Devuelve un atributo concreto
	 * @return Atributo
	 */
	public ShapeAttribute getAttribute(int x){	
		return atributos.get(x);
	}


	public long getFechaAlta(){
		return fechaAlta;
	}


	public long getFechaBaja(){
		return fechaBaja;
	}


	public synchronized long newShapeId(){
		Id++;
		return Id;
	}


	public abstract String getShapeId();

	public abstract List<String[]> getAttributes();

	public abstract String getRefCat();

	public abstract List<LineString> getPoligons();

	public abstract Coordinate[] getCoordenadas(int x);

	public abstract void addNode(int pos, long nodeId);

	public abstract void addWay(int pos, long wayId);

	public abstract void deleteWay(int pos, long wayId);

	public abstract void setRelation(long relationId);

	public abstract List<Long> getNodesIds(int pos);

	public abstract List<Long> getWaysIds(int pos);

	public abstract Long getRelationId();

	public abstract Coordinate getCoor();

	public abstract String getTtggss();

	public abstract boolean shapeValido();


	/** Traduce el tipo de via
	 * @param codigo Codigo de via
	 * @return Nombre del tipo de via
	 */
	public static String nombreTipoViaParser(String codigo){

		switch (codigo){
		case "CL":return "Calle";
		case "AL":return "Aldea/Alameda";
		case "AR":return "Area/Arrabal";
		case "AU":return "Autopista";
		case "AV":return "Avenida";
		case "AY":return "Arroyo";
		case "BJ":return "Bajada";
		case "BO":return "Barrio";
		case "BR":return "Barranco";
		case "CA":return "Cañada";
		case "CG":return "Colegio/Cigarral";
		case "CH":return "Chalet";
		case "CI":return "Cinturon";
		case "CJ":return "Calleja/Callejón";
		case "CM":return "Camino";
		case "CN":return "Colonia";
		case "CO":return "Concejo/Colegio";
		case "CP":return "Campa/Campo";
		case "CR":return "Carretera/Carrera";
		case "CS":return "Caserío";
		case "CT":return "Cuesta/Costanilla";
		case "CU":return "Conjunto";
		case "DE":return "Detrás";
		case "DP":return "Diputación";
		case "DS":return "Diseminados";
		case "ED":return "Edificios";
		case "EM":return "Extramuros";
		case "EN":return "Entrada, Ensanche";
		case "ER":return "Extrarradio";
		case "ES":return "Escalinata";
		case "EX":return "Explanada";
		case "FC":return "Ferrocarril";
		case "FN":return "Finca";
		case "GL":return "Glorieta";
		case "GR":return "Grupo";
		case "GV":return "Gran Vía";
		case "HT":return "Huerta/Huerto";
		case "JR":return "Jardines";
		case "LD":return "Lado/Ladera";
		case "LG":return "Lugar";
		case "MC":return "Mercado";
		case "ML":return "Muelle";
		case "MN":return "Municipio";
		case "MS":return "Masias";
		case "MT":return "Monte";
		case "MZ":return "Manzana";
		case "PB":return "Poblado";
		case "PD":return "Partida";
		case "PJ":return "Pasaje/Pasadizo";
		case "PL":return "Polígono";
		case "PM":return "Paramo";
		case "PQ":return "Parroquia/Parque";
		case "PR":return "Prolongación/Continuación";
		case "PS":return "Paseo";
		case "PT":return "Puente";
		case "PZ":return "Plaza";
		case "QT":return "Quinta";
		case "RB":return "Rambla";
		case "RC":return "Rincón/Rincona";
		case "RD":return "Ronda";
		case "RM":return "Ramal";
		case "RP":return "Rampa";
		case "RR":return "Riera";
		case "RU":return "Rua";
		case "SA":return "Salida";
		case "SD":return "Senda";
		case "SL":return "Solar";
		case "SN":return "Salón";
		case "SU":return "Subida";
		case "TN":return "Terrenos";
		case "TO":return "Torrente";
		case "TR":return "Travesía";
		case "UR":return "Urbanización";
		case "VR":return "Vereda";
		case "CY":return "Caleya";
		}

		return codigo;
	}


	/** Traduce el ttggss de Elemlin y Elempun. Elemtex tiene en su clase su propio parser
	 * ya que necesita mas datos suyos propios.
	 * @param ttggss Atributo ttggss
	 * @return Lista de los tags que genera
	 */
	public List<String[]> ttggssParser(String ttggss){
		List<String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];

		switch (ttggss){

		// Divisiones administrativas
		case "010401": 
			s[0] = "admin_level"; s[1] ="2";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			s = new String[2];
			s[0] = "border_type"; s[1] ="nation";
			l.add(s);
			return l;	
		case "010301": 
			s[0] = "admin_level"; s[1] ="4";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			return l;
		case "010201": 
			s[0] = "admin_level"; s[1] ="6";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			return l;
		case "010101": 
			s[0] = "admin_level"; s[1] ="8";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			return l;
		case "010102": 
			s[0] = "admin_level"; s[1] ="10";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			return l;
		case "018507": 
			s[0] = "historic"; s[1] ="boundary_stone";
			l.add(s);
			return l;
		case "018506": 
			s[0] = "historic"; s[1] ="boundary_stone";
			l.add(s);
			return l;

			// Relieve
		case "028110": 
			s[0] = "man_made"; s[1] ="survey_point";
			l.add(s);
			return l;
		case "028112": 
			s[0] = "man_made"; s[1] ="survey_point";
			l.add(s);
			return l;

			// Hidrografia
		case "030102": 
			s[0] = "waterway"; s[1] ="river";
			l.add(s);
			return l;
		case "030202": 
			s[0] = "waterway"; s[1] ="stream";
			l.add(s);
			return l;
		case "030302": 
			s[0] = "waterway"; s[1] ="drain";
			l.add(s);
			return l;
		case "032301": 
			s[0] = "natural"; s[1] ="coastline";
			l.add(s);
			return l;
		case "033301": 
			s[0] = "landuse"; s[1] ="reservoir";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] ="Especificar tipo de agua (natural=water / leisure=swimming_pool / man_made=water_well / amenity=fountain / ...), eliminar landuse=reservoir y/o comprobar que no este duplicado o contenido en otra geometria de agua.";
			l.add(s);
			return l;
		case "037101": 
			s[0] = "man_made"; s[1] ="water_well";
			l.add(s);
			return l;
		case "038101": 
			s[0] = "man_made"; s[1] ="water_well";
			l.add(s);
			return l;
		case "038102": 
			s[0] = "man_made"; s[1] ="water_well";
			l.add(s);
			return l;
		case "037102": 
			s[0] = "landuse"; s[1] ="reservoir";
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] ="Especificar tipo de agua (natural=water / leisure=swimming_pool / man_made=water_well / amenity=fountain / ...), eliminar landuse=reservoir y/o comprobar que no este duplicado o contenido en otra geometria de agua.";
			l.add(s);
			return l;
		case "037107": 
			s[0] = "waterway"; s[1] ="dam";
			l.add(s);
			return l;

			// Vias de comunicacion
		case "060102": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "060104": 
			s[0] = "highway"; s[1] ="motorway";
			l.add(s);
			return l;
		case "060202": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "060204": 
			s[0] = "highway"; s[1] ="primary";
			l.add(s);
			return l;
		case "060402": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "060404": 
			s[0] = "highway"; s[1] ="track";
			l.add(s);
			return l;
		case "060109": 
			s[0] = "railway"; s[1] ="funicular";
			l.add(s);
			return l;
		case "061104": 
			s[0] = "railway"; s[1] ="rail";
			l.add(s);
			return l;
		case "067121": 
			s[0] = "bridge"; s[1] ="yes";
			l.add(s);
			return l;
		case "068401": 
			s[0] = "highway"; s[1] ="milestone";
			l.add(s);
			return l;

			// Red geodesica y topografica
		case "108100": 
			s[0] = "man_made"; s[1] ="survey_point";
			l.add(s);
			return l;
		case "108101": 
			s[0] = "man_made"; s[1] ="survey_point";
			l.add(s);
			return l;
		case "108104": 
			s[0] = "man_made"; s[1] ="survey_point";
			l.add(s);
			return l;

			// Delimitaciones catastrales urbanisticas y estadisticas
		case "111101": 
			s[0] = "admin_level"; s[1] ="10";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			return l;
		case "111000": 
			s[0] = "admin_level"; s[1] ="12";
			l.add(s);
			return l;
		case "111200": 
			s[0] = "admin_level"; s[1] ="14";
			l.add(s);
			return l;
		case "111300": 
			s[0] = "admin_level"; s[1] ="10";
			l.add(s);
			return l;
		case "115101": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "115000": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "115200": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "115300": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;

			// Rustica (Compatibilidad 2006 hacia atras)
		case "120100": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "120200": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "120500": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "120180": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "120280": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "120580": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "125101": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "125201": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "125501": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "125510": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;

			// Rustica y Urbana
		case "130100": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "130200": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "130500": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "135101": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "135201": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "135501": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "135510": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;

			// Urbana (Compatibilidad 2006 hacia atras)
		case "140100": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "140190": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "140200": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "140290": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "140500": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "140590": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "145101": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "145201": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "145501": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "145510": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;

			// Infraestructura/Mobiliario
		case "160101": 
			s[0] = "kerb"; s[1] ="yes";
			l.add(s);
			return l;
		case "160131": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "160132": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "160201": 
			s[0] = "power"; s[1] ="line";
			l.add(s);
			return l;
		case "160202": 
			s[0] = "telephone"; s[1] ="line";
			l.add(s);
			return l;
		case "160300": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "161101": 
			s[0] = "highway"; s[1] ="road";
			l.add(s);
			return l;
		case "167103": 
			s[0] = "historic"; s[1] ="monument";
			l.add(s);
			return l;
		case "167104": 
			s[0] = "highway"; s[1] ="steps";
			l.add(s);
			return l;
		case "167106": 
			s[0] = "highway"; s[1] ="footway";
			l.add(s);
			s = new String[2];
			s[0] = "tunnel"; s[1] ="yes";
			l.add(s);
			return l;
		case "167111": 
			s[0] = "power"; s[1] ="sub_station";
			l.add(s);
			return l;
		case "167167": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "167201": 
			s[0] = "barrier"; s[1] ="hedge";
			l.add(s);
			return l;
		case "168100": 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;
		case "168103": 
			s[0] = "historic"; s[1] ="monument";
			l.add(s);
			return l;
		case "168113": 
			s[0] = "power"; s[1] ="tower";
			l.add(s);
			return l;
		case "168116": 
			s[0] = "highway"; s[1] ="street_lamp";
			l.add(s);
			return l;
		case "168153": 
			s[0] = "natural"; s[1] ="tree";
			l.add(s);
			return l;
		case "168168": 
			s[0] = "amenity"; s[1] ="parking_entrance";
			l.add(s);
			return l;
		default: if (!ttggss.isEmpty()){
			s[0] = "fixme"; s[1] = "Documentar ttggss="+ttggss+" si es preciso en http://wiki.openstreetmap.org/w/index.php?title=Traduccion_metadatos_catastro_a_map_features";
			l.add(s);
		}
		}
		return l;
	}


	/** Elimina los puntos '.' y espacios en un String
	 * @param s String en el cual eliminar los puntos
	 * @return String sin los puntos
	 */
	public static String eliminarPuntosString(String s){
		if (!s.isEmpty()){
			s = s.replace('.', ' ');
			s = s.replace(" ", "");
		}
		return s.trim();
	}


	/** Eliminar las comillas '"' de los textos, sino al leerlo JOSM devuelve error
	 * pensando que ha terminado un valor antes de tiempo.
	 * @param s String al que quitar las comillas
	 * @return String sin las comillas
	 */
	public static String eliminarComillas(String s){
		String ret = new String();
		for (int x = 0; x < s.length(); x++)
			if (s.charAt(x) != '"') ret += s.charAt(x);
		return ret;
	}

}
