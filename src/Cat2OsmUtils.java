import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.vividsolutions.jts.geom.Coordinate;


public class Cat2OsmUtils {

	private static long idnode = -1;    // Comienzo de id de nodos
	private static long idway = -1;     // Comienzo de id de ways
	private static long idrelation = -1; // Comienzo de id de relations
	// Lista de nodos (para evitar repetidos)
	private final Map <NodeOsm, Long> totalNodes = new HashMap <NodeOsm, Long>();
	// Listaa de ways (para manejar los que se comparten)
	private final Map <WayOsm, Long> totalWays = new HashMap <WayOsm, Long>();
	// Listaa de relations
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
	
	@SuppressWarnings("rawtypes")
	public void deleteWay(WayOsm w){
		Iterator<Entry<RelationOsm, Long>> it = totalRelations.entrySet().iterator();
		
		while(it.hasNext()){
			Map.Entry e = (Map.Entry)it.next();
			RelationOsm r = (RelationOsm) e.getKey();
			r.removeMember(totalWays.get(w));
		}
		totalWays.remove(w);
	}
	
	/** Junta dos ways en uno. Hay que tener en cuenta los 4 casos que se pueden
	 * dar.
	 * @param w1 Way1 Dependiendo del caso se eliminara un way o el otro
	 * @param w2 Way2
	 * @return long Id de way que hay que eliminar de los shapes, porque se ha juntado al otro
	 */
	public long joinWays(WayOsm w1, WayOsm w2){
		
		if ( !w1.getNodes().isEmpty() && !w2.getNodes().isEmpty()){
			
			// Caso1: w1.final = w2.primero
			if (w1.getNodes().get(w1.getNodes().size()-1).equals(w2.getNodes().get(0)) && totalWays.get(w2) != null){
				System.out.println("CASO1");
				long l = totalWays.get(w2);
				List<Long> nodes = w2.getNodes();
				nodes.remove(w2.getNodes().get(0));
				w1.addNodes(nodes);
				deleteWay(w2);
				return l;
			}
			
			// Caso2: w1.primero = w2.final
			else if (w1.getNodes().get(0).equals(w2.getNodes().get(w2.getNodes().size()-1)) && totalWays.get(w1) != null){
				System.out.println("CASO2");
				return joinWays(w2, w1);
			}
		}
		return 0;
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
			idnode--;
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
	public long getWayId(List<Long> nodes, List<String[]> tags ){

		Long id = null;
		if (!totalWays.isEmpty())
			id = totalWays.get(new WayOsm(nodes));
		
		if (id != null){
			if (tags != null)
				((WayOsm) getKeyFromValue((Map<Object, Long>) ((Object) totalWays), id)).addTags(tags);
			return id;
			}
		else{
			idway--;
			WayOsm w = new WayOsm(nodes);
			if (tags != null)
				w.addTags(tags);
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
			idrelation--;
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
	
	/** Dada una lista de identificadores de ways, devuelve una lista con esos
	 * ways
	 * @return ways lista de WayOsm
	 */
	@SuppressWarnings("unchecked")
	public List<WayOsm> getWays(List<Long> ids){
		List<WayOsm> ways = new ArrayList<WayOsm>();
		
		for (Long l: ids)
			ways.add(((WayOsm) getKeyFromValue((Map<Object, Long>) ((Object)totalWays), l)));
		
		return ways;
	}
	
}
