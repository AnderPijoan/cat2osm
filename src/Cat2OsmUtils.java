import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;


public class Cat2OsmUtils {

	private static long idnode = 1;    // Comienzo de id de nodos
	private static long idway = 1;     // Comienzo de id de ways
	private static long idrelation = 1; // Comienzo de id de relations
	// Lista de nodos (para evitar repetidos)
	private final Map <NodeOsm, Long> totalNodes = new HashMap <NodeOsm, Long>();
	// Lista de ways (para manejar los que se comparten)
	private final Map <WayOsm, Long> totalWays = new HashMap <WayOsm, Long>();
	// Lista de relations
	private final Map <RelationOsm, Long> totalRelations = new HashMap <RelationOsm, Long>();
	
	
	public Map<NodeOsm, Long> getTotalNodes() {
		return totalNodes;
	}
	
	public void addNode(NodeOsm n, Long idnode){
		totalNodes.put(n, idnode);
	}
	
	public Map<WayOsm, Long> getTotalWays() {
		return totalWays;
	}
	
	public void addWay(WayOsm w, Long idway){
		totalWays.put(w, idway);
	}
	
	public Map<RelationOsm, Long> getTotalRelations() {
		return totalRelations;
	} 
	
	public void addRelation(RelationOsm r, Long idrel){
		totalRelations.put(r, idrel);
	}
	

	/** Mira si existe un nodo con las mismas coordenadas
	 * de lo contrario crea el nuevo nodo. Despues devuelve el id
	 * @param coor Coordenadas del nodo
	 * @param tags Tags del nodo
	 * @param nodes Lista de nodos existentes
	 * @return Devuelve el id del nodo ya sea creado o el que existia
	 */
	@SuppressWarnings("unchecked")
	public long getNodeId(Coordinate coor, List<String[]> tags){

		Long id = null;
		if (!totalNodes.isEmpty())
			id = totalNodes.get(new NodeOsm(coor));
		if (id != null){
			if (tags != null)
				((NodeOsm) getKeyFromValue((Map<Object, Long>) ((Object) totalNodes), id)).addTags(tags);
			return id;
			}
		else{
			idnode++;
			NodeOsm n = new NodeOsm(coor);
			if (tags != null)
				n.addTags(tags);
			totalNodes.put(n, idnode);
			return idnode;
		}
	}
	
	
	/** Mira si existe un way con los mismos nodos y en ese caso añade
	 * los tags, de lo contrario crea uno. Despues devuelve el id
	 * @param nodes Lista de nodos
	 * @param tags Lista de tags
	 * @param ways Lista de ways existentes
	 * @return devuelve el id del way creado o el del que ya existia
	 */
	@SuppressWarnings("unchecked")
	public long getWayId(List<Long> nodes, String refCatastral ){

		Long id = null;
		if (!totalWays.isEmpty())
			id = totalWays.get(new WayOsm(nodes));
		if (id != null){
			if (refCatastral != null)
				((WayOsm) getKeyFromValue((Map<Object, Long>) ((Object) totalWays), id)).addRefCat(refCatastral);
			return id;
			}
		else{
			idway++;
			WayOsm w = new WayOsm(nodes);
			if (refCatastral != null)
				w.addRefCat(refCatastral);
			totalWays.put(w, idway);
			return idway;
		}
	}
	
	
	/** Mira si existe una relation con los mismos ways y en ese caso añade 
	 * los tags, de lo contrario crea una. Despues devuelve el id
	 * @param ids Lista de ids de los members q componen la relacion
	 * @param types Lista de los tipos de los members de la relacion (por lo general ways)
	 * @param roles Lista de los roles de los members de la relacion (inner,outer...)
	 * @param tags Lista de los tags de la relacion
	 * @param relations Map de relaciones ya existentes
	 * @return devuelve el id de la relacion creada o el de la que ya existia
	 */
	@SuppressWarnings("unchecked")
	public long getRelationId(List<Long> ids, List<String> types, List<String> roles, List<String[]> tags){
		
		Long id = null;
		if (!totalRelations.isEmpty())
			id = totalRelations.get(new RelationOsm(ids,types,roles));
		if (id != null){
			if (tags != null)
				((RelationOsm) getKeyFromValue((Map<Object, Long>) ((Object)totalRelations), id)).addTags(tags);
			return id;
			}
		else{
			idrelation++;
			RelationOsm r = new RelationOsm(ids,types,roles);
			if (tags != null)
				r.addTags(tags);
			totalRelations.put(r, idrelation);
			return idrelation;
		}
	}
	
	/** Dado un Value de un Map devuelve su Key
	 * @param map Mapa
	 * @param id Value en el map para obtener su Key
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Object getKeyFromValue(Map <Object, Long> map, Long id){
		
		for (Object o: map.entrySet()) {
			Map.Entry<Object,Long> entry = (Map.Entry<Object,Long>) o;

			if(entry.getValue().equals(id))
				return entry.getKey();
		}
		return null;
	}

}
