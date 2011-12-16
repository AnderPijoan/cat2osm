import java.util.ArrayList;
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
	public Shape(SimpleFeature f){
		
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
		else System.out.println("No se encuentra el tipo de FECHAALTA "+ f.getAttribute("FECHAALTA").getClass().getName() );	

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
		else System.out.println("No se encuentra el tipo de FECHABAJA"+ f.getAttribute("FECHABAJA").getClass().getName());

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
		
		if (codigo.toUpperCase().equals("CL"))return "Calle";
		else if (codigo.toUpperCase().equals("AL"))return "Aldea/Alameda";
		else if (codigo.toUpperCase().equals("AR"))return "Area/Arrabal";
		else if (codigo.toUpperCase().equals("AU"))return "Autopista";
		else if (codigo.toUpperCase().equals("AV"))return "Avenida";
		else if (codigo.toUpperCase().equals("AY"))return "Arroyo";
		else if (codigo.toUpperCase().equals("BJ"))return "Bajada";
		else if (codigo.toUpperCase().equals("BO"))return "Barrio";
		else if (codigo.toUpperCase().equals("BR"))return "Barranco";
		else if (codigo.toUpperCase().equals("CA"))return "Cañada";
		else if (codigo.toUpperCase().equals("CG"))return "Colegio/Cigarral";
		else if (codigo.toUpperCase().equals("CH"))return "Chalet";
		else if (codigo.toUpperCase().equals("CI"))return "Cinturon";
		else if (codigo.toUpperCase().equals("CJ"))return "Calleja/Callejón";
		else if (codigo.toUpperCase().equals("CM"))return "Camino";
		else if (codigo.toUpperCase().equals("CN"))return "Colonia";
		else if (codigo.toUpperCase().equals("CO"))return "Concejo/Colegio";
		else if (codigo.toUpperCase().equals("CP"))return "Campa/Campo";
		else if (codigo.toUpperCase().equals("CR"))return "Carretera/Carrera";
		else if (codigo.toUpperCase().equals("CS"))return "Caserío";
		else if (codigo.toUpperCase().equals("CT"))return "Cuesta/Costanilla";
		else if (codigo.toUpperCase().equals("CU"))return "Conjunto";
		else if (codigo.toUpperCase().equals("DE"))return "Detrás";
		else if (codigo.toUpperCase().equals("DP"))return "Diputación";
		else if (codigo.toUpperCase().equals("DS"))return "Diseminados";
		else if (codigo.toUpperCase().equals("ED"))return "Edificios";
		else if (codigo.toUpperCase().equals("EM"))return "Extramuros";
		else if (codigo.toUpperCase().equals("EN"))return "Entrada, Ensanche";
		else if (codigo.toUpperCase().equals("ER"))return "Extrarradio";
		else if (codigo.toUpperCase().equals("ES"))return "Escalinata";
		else if (codigo.toUpperCase().equals("EX"))return "Explanada";
		else if (codigo.toUpperCase().equals("FC"))return "Ferrocarril";
		else if (codigo.toUpperCase().equals("FN"))return "Finca";
		else if (codigo.toUpperCase().equals("GL"))return "Glorieta";
		else if (codigo.toUpperCase().equals("GR"))return "Grupo";
		else if (codigo.toUpperCase().equals("GV"))return "Gran Vía";
		else if (codigo.toUpperCase().equals("HT"))return "Huerta/Huerto";
		else if (codigo.toUpperCase().equals("JR"))return "Jardines";
		else if (codigo.toUpperCase().equals("LD"))return "Lado/Ladera";
		else if (codigo.toUpperCase().equals("LG"))return "Lugar";
		else if (codigo.toUpperCase().equals("MC"))return "Mercado";
		else if (codigo.toUpperCase().equals("ML"))return "Muelle";
		else if (codigo.toUpperCase().equals("MN"))return "Municipio";
		else if (codigo.toUpperCase().equals("MS"))return "Masias";
		else if (codigo.toUpperCase().equals("MT"))return "Monte";
		else if (codigo.toUpperCase().equals("MZ"))return "Manzana";
		else if (codigo.toUpperCase().equals("PB"))return "Poblado";
		else if (codigo.toUpperCase().equals("PD"))return "Partida";
		else if (codigo.toUpperCase().equals("PJ"))return "Pasaje/Pasadizo";
		else if (codigo.toUpperCase().equals("PL"))return "Polígono";
		else if (codigo.toUpperCase().equals("PM"))return "Paramo";
		else if (codigo.toUpperCase().equals("PQ"))return "Parroquia/Parque";
		else if (codigo.toUpperCase().equals("PR"))return "Prolongación/Continuación";
		else if (codigo.toUpperCase().equals("PS"))return "Paseo";
		else if (codigo.toUpperCase().equals("PT"))return "Puente";
		else if (codigo.toUpperCase().equals("PZ"))return "Plaza";
		else if (codigo.toUpperCase().equals("QT"))return "Quinta";
		else if (codigo.toUpperCase().equals("RB"))return "Rambla";
		else if (codigo.toUpperCase().equals("RC"))return "Rincón/Rincona";
		else if (codigo.toUpperCase().equals("RD"))return "Ronda";
		else if (codigo.toUpperCase().equals("RM"))return "Ramal";
		else if (codigo.toUpperCase().equals("RP"))return "Rampa";
		else if (codigo.toUpperCase().equals("RR"))return "Riera";
		else if (codigo.toUpperCase().equals("RU"))return "Rua";
		else if (codigo.toUpperCase().equals("SA"))return "Salida";
		else if (codigo.toUpperCase().equals("SD"))return "Senda";
		else if (codigo.toUpperCase().equals("SL"))return "Solar";
		else if (codigo.toUpperCase().equals("SN"))return "Salón";
		else if (codigo.toUpperCase().equals("SU"))return "Subida";
		else if (codigo.toUpperCase().equals("TN"))return "Terrenos";
		else if (codigo.toUpperCase().equals("TO"))return "Torrente";
		else if (codigo.toUpperCase().equals("TR"))return "Travesía";
		else if (codigo.toUpperCase().equals("UR"))return "Urbanización";
		else if (codigo.toUpperCase().equals("VR"))return "Vereda";
		else if (codigo.toUpperCase().equals("CY"))return "Caleya";

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

		// Divisiones administrativas
		if (ttggss.equals("010401")){
			s[0] = "admin_level"; s[1] ="2";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			s = new String[2];
			s[0] = "border_type"; s[1] ="nation";
			l.add(s);
			return l;}
		if (ttggss.equals("010301")){ 
			s[0] = "admin_level"; s[1] ="4";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			return l;}
		if (ttggss.equals("010201")){ 
			s[0] = "admin_level"; s[1] ="6";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			return l;}
		if (ttggss.equals("010101")){ 
			s[0] = "admin_level"; s[1] ="8";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			return l;}
		if (ttggss.equals("010102")){ 
			s[0] = "admin_level"; s[1] ="10";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			return l;}
		if (ttggss.equals("018507")){ 
			s[0] = "historic"; s[1] ="boundary_stone";
			l.add(s);
			return l;}
		if (ttggss.equals("018506")){ 
			s[0] = "historic"; s[1] ="boundary_stone";
			l.add(s);
			return l;}

		// Relieve
		if (ttggss.equals("028110")){ 
			s[0] = "man_made"; s[1] ="survey_point";
			l.add(s);
			return l;}
		if (ttggss.equals("028112")){ 
			s[0] = "man_made"; s[1] ="survey_point";
			l.add(s);
			return l;}

		// Hidrografia
		if (ttggss.equals("030102")){ 
			s[0] = "waterway"; s[1] ="river";
			l.add(s);
			return l;}
		if (ttggss.equals("030202")){ 
			s[0] = "waterway"; s[1] ="stream";
			l.add(s);
			return l;}
		if (ttggss.equals("030302")){ 
			s[0] = "waterway"; s[1] ="drain";
			l.add(s);
			return l;}
		if (ttggss.equals("032301")){ 
			s[0] = "natural"; s[1] ="coastline";
			l.add(s);
			return l;}
		if (ttggss.equals("033301")){ 
			s[0] = "natural"; s[1] ="water";
			l.add(s);
			return l;}
		if (ttggss.equals("037101")){ 
			s[0] = "man_made"; s[1] ="water_well";
			l.add(s);
			return l;}
		if (ttggss.equals("037102")){ 
			s[0] = "natural"; s[1] ="water";
			l.add(s);
			return l;}
		if (ttggss.equals("037107")){ 
			s[0] = "waterway"; s[1] ="dam";
			l.add(s);
			return l;}

		// Vias de comunicacion
		if (ttggss.equals("060102")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("060104")){ 
			s[0] = "highway"; s[1] ="motorway";
			l.add(s);
			return l;}
		if (ttggss.equals("060202")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("060204")){ 
			s[0] = "highway"; s[1] ="primary";
			l.add(s);
			return l;}
		if (ttggss.equals("060402")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("060404")){ 
			s[0] = "highway"; s[1] ="track";
			l.add(s);
			return l;}
		if (ttggss.equals("060109")){ 
			s[0] = "railway"; s[1] ="funicular";
			l.add(s);
			return l;}
		if (ttggss.equals("061104")){ 
			s[0] = "railway"; s[1] ="rail";
			l.add(s);
			return l;}
		if (ttggss.equals("067121")){ 
			s[0] = "bridge"; s[1] ="yes";
			l.add(s);
			return l;}
		if (ttggss.equals("068401")){ 
			s[0] = "highway"; s[1] ="milestone";
			l.add(s);
			s = new String[2];
			s[0] = "pk"; s[1] ="milestone"; //TODO
			l.add(s);
			s = new String[2];
			s[0] = "ref"; s[1] ="milestone"; //TODO
			l.add(s);
			return l;}

		// Red geodesica y topografica
		if (ttggss.equals("108100")){ 
			s[0] = "man_made"; s[1] ="survey_point";
			l.add(s);
			return l;}
		if (ttggss.equals("108101")){ 
			s[0] = "man_made"; s[1] ="survey_point";
			l.add(s);
			return l;}
		if (ttggss.equals("108104")){ 
			s[0] = "man_made"; s[1] ="survey_point";
			l.add(s);
			return l;}

		// Delimitaciones catastrales urbanisticas y estadisticas
		if (ttggss.equals("111101")){ 
			s[0] = "admin_level"; s[1] ="10";
			l.add(s);
			s = new String[2];
			s[0] = "boundary"; s[1] ="administrative";
			l.add(s);
			return l;}
		if (ttggss.equals("111000")){ 
			s[0] = "admin_level"; s[1] ="12";
			l.add(s);
			return l;}
		if (ttggss.equals("111200")){ 
			s[0] = "admin_level"; s[1] ="14";
			l.add(s);
			return l;}
		if (ttggss.equals("111300")){ 
			s[0] = "admin_level"; s[1] ="10";
			l.add(s);
			return l;}
		if (ttggss.equals("115101")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("115000")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("115200")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("115300")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}

		// Rustica (Compatibilidad 2006 hacia atras)
		if (ttggss.equals("120100")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("120200")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("120500")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("120180")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("120280")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("120580")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("125101")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("125201")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("125501")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("125510")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}

		// Rustica y Urbana
		if (ttggss.equals("130100")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("130200")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("130500")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("135101")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("135201")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("135501")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("135510")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}

		// Urbana (Compatibilidad 2006 hacia atras)
		if (ttggss.equals("140100")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("140190")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("140200")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("140290")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("140500")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("140590")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("145101")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("145201")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("145501")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("145510")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}

		// Infraestructura/Mobiliario
		if (ttggss.equals("160101")){ 
			s[0] = "kerb"; s[1] ="yes";
			l.add(s);
			return l;}
		if (ttggss.equals("160131")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("160132")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("160201")){ 
			s[0] = "power"; s[1] ="line";
			l.add(s);
			return l;}
		if (ttggss.equals("160202")){ 
			s[0] = "telephone"; s[1] ="line";
			l.add(s);
			return l;}
		if (ttggss.equals("160300")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("161101")){ 
			s[0] = "highway"; s[1] ="road";
			l.add(s);
			return l;}
		if (ttggss.equals("167103")){ 
			s[0] = "historic"; s[1] ="monument";
			l.add(s);
			return l;}
		if (ttggss.equals("167104")){ 
			s[0] = "highway"; s[1] ="steps";
			l.add(s);
			return l;}
		if (ttggss.equals("167106")){ 
			s[0] = "highway"; s[1] ="footway";
			l.add(s);
			s = new String[2];
			s[0] = "tunnel"; s[1] ="yes";
			l.add(s);
			return l;}
		if (ttggss.equals("167111")){ 
			s[0] = "power"; s[1] ="sub_station";
			l.add(s);
			return l;}
		if (ttggss.equals("167167")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("167201")){ 
			s[0] = "barrier"; s[1] ="hedge";
			l.add(s);
			return l;}
		if (ttggss.equals("168100")){ 
			s[0] = "ttggss"; s[1] =ttggss;
			l.add(s);
			return l;}
		if (ttggss.equals("168103")){ 
			s[0] = "historic"; s[1] ="monument";
			l.add(s);
			return l;}
		if (ttggss.equals("168113")){ 
			s[0] = "power"; s[1] ="tower";
			l.add(s);
			return l;}
		if (ttggss.equals("168116")){ 
			s[0] = "highway"; s[1] ="street_lamp";
			l.add(s);
			return l;}
		if (ttggss.equals("168153")){ 
			s[0] = "natural"; s[1] ="tree";
			l.add(s);
			return l;}
		if (ttggss.equals("168168")){ 
			s[0] = "amenity"; s[1] ="parking_entrance";
			l.add(s);
			return l;}
		else{
			s[0] = "fixme"; s[1] = "ttggss="+ttggss;
			l.add(s);
			s = new String[2];
			s[0] = "fixme"; s[1] = "Documentar si es preciso en http://wiki.openstreetmap.org/w/index.php?title=Traduccion_metadatos_catastro_a_map_features#Textos_en_Elemtex.shp";
			l.add(s);
			return l;
		}
	
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
	
}
