import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

public abstract class Shape {

	private List<ShapeAttribute> atributos;
	protected long fechaAlta; // Formato AAAAMMDD
	protected long fechaBaja;

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
	 * @param shp Shapefile a comprobar
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

	public abstract List<String[]> getAttributes();

	public abstract String getRefCat();

	public abstract Long getRelation();

	public abstract List<LineString> getPoligons();

	public abstract Coordinate[] getCoordenadas(int x);

	public abstract void addNode(long nodeId);

	public abstract List<Long> getNodesPoligonN(int x, Cat2OsmUtils utils);

	public abstract void addWay(long wayId);

	public abstract List<Long> getWaysPoligonN(int x, Cat2OsmUtils utils);

	public abstract void setRelation(long relationId);

	public abstract List<Long> getNodes();

	public abstract List<Long> getWays();

	public abstract Coordinate getCoor();

	public static String nombreTipoViaParser(String codigo){
		
		if (codigo.equals("CL"))return "Calle";
		else if (codigo.equals("AL"))return "Aldea/Alameda";
		else if (codigo.equals("AR"))return "Area/Arrabal";
		else if (codigo.equals("AU"))return "Autopista";
		else if (codigo.equals("AV"))return "Avenida";
		else if (codigo.equals("AY"))return "Arroyo";
		else if (codigo.equals("BJ"))return "Bajada";
		else if (codigo.equals("BO"))return "Barrio";
		else if (codigo.equals("BR"))return "Barranco";
		else if (codigo.equals("CA"))return "Cañada";
		else if (codigo.equals("CG"))return "Colegio/Cigarral";
		else if (codigo.equals("CH"))return "Chalet";
		else if (codigo.equals("CI"))return "Cinturon";
		else if (codigo.equals("CJ"))return "Calleja/Callejón";
		else if (codigo.equals("CM"))return "Camino";
		else if (codigo.equals("CN"))return "Colonia";
		else if (codigo.equals("CO"))return "Concejo/Colegio";
		else if (codigo.equals("CP"))return "Campa/Campo";
		else if (codigo.equals("CR"))return "Carretera/Carrera";
		else if (codigo.equals("CS"))return "Caserío";
		else if (codigo.equals("CT"))return "Cuesta/Costanilla";
		else if (codigo.equals("CU"))return "Conjunto";
		else if (codigo.equals("DE"))return "Detrás";
		else if (codigo.equals("DP"))return "Diputación";
		else if (codigo.equals("DS"))return "Diseminados";
		else if (codigo.equals("ED"))return "Edificios";
		else if (codigo.equals("EM"))return "Extramuros";
		else if (codigo.equals("EN"))return "Entrada, Ensanche";
		else if (codigo.equals("ER"))return "Extrarradio";
		else if (codigo.equals("ES"))return "Escalinata";
		else if (codigo.equals("EX"))return "Explanada";
		else if (codigo.equals("FC"))return "Ferrocarril";
		else if (codigo.equals("FN"))return "Finca";
		else if (codigo.equals("GL"))return "Glorieta";
		else if (codigo.equals("GR"))return "Grupo";
		else if (codigo.equals("GV"))return "Gran Vía";
		else if (codigo.equals("HT"))return "Huerta/Huerto";
		else if (codigo.equals("JR"))return "Jardines";
		else if (codigo.equals("LD"))return "Lado/Ladera";
		else if (codigo.equals("LG"))return "Lugar";
		else if (codigo.equals("MC"))return "Mercado";
		else if (codigo.equals("ML"))return "Muelle";
		else if (codigo.equals("MN"))return "Municipio";
		else if (codigo.equals("MS"))return "Masias";
		else if (codigo.equals("MT"))return "Monte";
		else if (codigo.equals("MZ"))return "Manzana";
		else if (codigo.equals("PB"))return "Poblado";
		else if (codigo.equals("PD"))return "Partida";
		else if (codigo.equals("PJ"))return "Pasaje/Pasadizo";
		else if (codigo.equals("PL"))return "Polígono";
		else if (codigo.equals("PM"))return "Paramo";
		else if (codigo.equals("PQ"))return "Parroquia/Parque";
		else if (codigo.equals("PR"))return "Prolongación/Continuación";
		else if (codigo.equals("PS"))return "Paseo";
		else if (codigo.equals("PT"))return "Puente";
		else if (codigo.equals("PZ"))return "Plaza";
		else if (codigo.equals("QT"))return "Quinta";
		else if (codigo.equals("RB"))return "Rambla";
		else if (codigo.equals("RC"))return "Rincón/Rincona";
		else if (codigo.equals("RD"))return "Ronda";
		else if (codigo.equals("RM"))return "Ramal";
		else if (codigo.equals("RP"))return "Rampa";
		else if (codigo.equals("RR"))return "Riera";
		else if (codigo.equals("RU"))return "Rua";
		else if (codigo.equals("SA"))return "Salida";
		else if (codigo.equals("SD"))return "Senda";
		else if (codigo.equals("SL"))return "Solar";
		else if (codigo.equals("SN"))return "Salón";
		else if (codigo.equals("SU"))return "Subida";
		else if (codigo.equals("TN"))return "Terrenos";
		else if (codigo.equals("TO"))return "Torrente";
		else if (codigo.equals("TR"))return "Travesía";
		else if (codigo.equals("UR"))return "Urbanización";
		else if (codigo.equals("VR"))return "Vereda";
		else if (codigo.equals("CY"))return "Caleya";

		return codigo;
	}
	
	public static List<String[]> ttggssParser(String ttggss){
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
			s[0] = "waterway"; s[1] ="canal";
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
			//s[0] = "natural"; s[1] ="coastline";
			//l.add(s);
			return l;}
		if (ttggss.equals("060104")){ 
			s[0] = "highway"; s[1] ="motorway";
			l.add(s);
			return l;}
		if (ttggss.equals("060202")){ 
			//s[0] = "man_made"; s[1] ="water_well";
			//l.add(s);
			return l;}
		if (ttggss.equals("060204")){ 
			s[0] = "highway"; s[1] ="primary";
			l.add(s);
			return l;}
		if (ttggss.equals("060402")){ 
			//s[0] = "waterway"; s[1] ="dam";
			//l.add(s);
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
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("115000")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("115200")){ 
			//s[0] = "admin_level"; s[1] ="14";
			//l.add(s);
			return l;}
		if (ttggss.equals("115300")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}

		// Rustica (Compatibilidad 2006 hacia atras)
		if (ttggss.equals("120100")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("120200")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("120500")){ 
			//s[0] = "admin_level"; s[1] ="14";
			//l.add(s);
			return l;}
		if (ttggss.equals("120180")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("120280")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("120580")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("125101")){ 
			//s[0] = "admin_level"; s[1] ="14";
			//l.add(s);
			return l;}
		if (ttggss.equals("125201")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("125501")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("125510")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}

		// Rustica y Urbana
		if (ttggss.equals("130100")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("130200")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("130500")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("135101")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("135201")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("135501")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("135510")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}

		// Urbana (Compatibilidad 2006 hacia atras)
		if (ttggss.equals("140100")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("140190")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("140200")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("140290")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("140500")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("140590")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("145101")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("145201")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("145501")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("145510")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}

		// Infraestructura/Mobiliario
		if (ttggss.equals("160101")){ 
			s[0] = "kerb"; s[1] ="yes";
			l.add(s);
			return l;}
		if (ttggss.equals("160131")){ 
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("160132")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
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
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
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
			//s[0] = "admin_level"; s[1] ="10";
			//l.add(s);
			return l;}
		if (ttggss.equals("167201")){ 
			//s[0] = "admin_level"; s[1] ="12";
			//l.add(s);
			return l;}
		if (ttggss.equals("168100")){ 
			//s[0] = "power"; s[1] ="sub_station";
			//l.add(s);
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

		return l;	
	}

}
