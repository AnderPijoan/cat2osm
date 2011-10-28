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
	
}
