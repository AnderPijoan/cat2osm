import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.vividsolutions.jts.geom.Coordinate;


public class Cat2OsmUtils {

	private volatile static long idnode = -1;    // Comienzo de id de nodos
	private volatile static long idway = -1;     // Comienzo de id de ways
	private volatile static long idrelation = -1; // Comienzo de id de relations
	// Lista de nodos (para evitar repetidos)
	private final ConcurrentHashMap <NodeOsm, Long> totalNodes = new ConcurrentHashMap <NodeOsm, Long>();
	// Listaa de ways (para manejar los que se comparten)
	private final ConcurrentHashMap <WayOsm, Long> totalWays = new ConcurrentHashMap <WayOsm, Long>();
	// Listaa de relations
	private final ConcurrentHashMap <RelationOsm, Long> totalRelations = new ConcurrentHashMap <RelationOsm, Long>();
	
	public synchronized ConcurrentHashMap<NodeOsm, Long> getTotalNodes() {
		return totalNodes;
	}
	
	public synchronized void addNode(NodeOsm n, Long idnode){
		totalNodes.put(n, idnode);
	}
	
	public synchronized ConcurrentHashMap<WayOsm, Long> getTotalWays() {
		return totalWays;
	}
	
	
	public synchronized void addWay(WayOsm w, Long idway){
		totalWays.put(w, idway);
	}
	
	
	/** A la hora de simplificar, hay ways que se eliminan porque sus nodos se concatenan
	 * a otro way. Borramos los ways que no se vayan a usar de las relaciones que los contenian
	 * @param w Way a borrar
	 */
	@SuppressWarnings("rawtypes")
	public synchronized void deleteWayFromRelations(WayOsm w){
		Iterator<Entry<RelationOsm, Long>> it = totalRelations.entrySet().iterator();
		
		while(it.hasNext()){
			Map.Entry e = (Map.Entry)it.next();
			RelationOsm r = (RelationOsm) e.getKey();
			r.removeMember(totalWays.get(w));
		}
	}
	
	
	/** Junta dos ways en uno.
	 * dar.
	 * @param w1 Way1 Dependiendo del caso se eliminara un way o el otro
	 * @param w2 Way2
	 * @return long Id de way que hay que eliminar de los shapes, porque se ha juntado al otro
	 */
	public synchronized WayOsm unirWays(WayOsm w1, WayOsm w2){
		
		if ( !w1.getNodes().isEmpty() && !w2.getNodes().isEmpty()){
			
			// Caso1: w1.final = w2.primero
			if (w1.getNodes().get(w1.getNodes().size()-1).equals(w2.getNodes().get(0)) && totalWays.get(w1) != null && totalWays.get(w2) != null){
				
				// Clonamos el way al que le anadiremos los nodos, w1
				long l1 = totalWays.get(w1);
				WayOsm w3 = new WayOsm(null);
				for (Long lo : w1.getNodes())
					w3.addNode(lo);
				w3.setShapes(w1.getShapes());
				
				// Copiamos la lista de nodos del way que eliminaremos, w2
				List<Long> nodes = new ArrayList<Long>();
				for (Long lo : w2.getNodes())
					nodes.add(lo);
				
				// Eliminamos el nodo que comparten de la lista de nodos
				nodes.remove(w2.getNodes().get(0));
				
				// Concatenamos al final del way3 (copia del way1) los nodos del way2
				w3.addNodes(nodes);
				
				// Borramos el way de las relaciones
				totalWays.remove(w1);
				deleteWayFromRelations(w2);
				
				// Guardamos way3 en la lista de ways, manteniendo el id del way1
				totalWays.put(w3, l1);
				
				return w2;
			}
			
			// Caso2: w1.primero = w2.final
			else if (w1.getNodes().get(0).equals(w2.getNodes().get(w2.getNodes().size()-1)) && totalWays.get(w1) != null  && totalWays.get(w2) != null){
				
				// Es igual que el Caso1 pero cambiados de orden.
				return unirWays(w2, w1);
			}			
		}
		return null;
	}
	
	public synchronized ConcurrentHashMap<RelationOsm, Long> getTotalRelations() {
		return totalRelations;
	} 
	
	public synchronized void addRelation(RelationOsm r, Long idrel){
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
	public synchronized long getNodeId(Coordinate coor, List<String[]> tags){

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
			totalNodes.putIfAbsent(n, idnode);
			return idnode;
		}
	}
	
	
	/** Mira si existe un way con los mismos nodos y en ese caso a�ade
	 * los tags, de lo contrario crea uno. Despues devuelve el id
	 * @param nodes Lista de nodos
	 * @param shapes Lista de los shapes a los que pertenecera
	 * @return devuelve el id del way creado o el del que ya existia
	 */
	@SuppressWarnings("unchecked")
	public synchronized long getWayId(List<Long> nodes, List<String> shapes ){

		Long id = null;
		if (!totalWays.isEmpty())
			id = totalWays.get(new WayOsm(nodes));
		
		if (id != null){
			if (shapes != null)
				((WayOsm) getKeyFromValue((Map<Object, Long>) ((Object) totalWays), id)).addShapes(shapes);
			return id;
			}
		else{
			idway--;
			WayOsm w = new WayOsm(nodes);
			if (shapes != null)
				w.addShapes(shapes);
			totalWays.putIfAbsent(w, idway);
			return idway;
		}
	}
	
	
	/** Mira si existe una relation con los mismos ways y en ese caso a�ade 
	 * los tags, de lo contrario crea una. Despues devuelve el id
	 * @param ids Lista de ids de los members q componen la relacion
	 * @param types Lista de los tipos de los members de la relacion (por lo general ways)
	 * @param roles Lista de los roles de los members de la relacion (inner,outer...)
	 * @param tags Lista de los tags de la relacion
	 * @param relations Map de relaciones ya existentes
	 * @return devuelve el id de la relacion creada o el de la que ya existia
	 */
	@SuppressWarnings("unchecked")
	public synchronized long getRelationId(List<Long> ids, List<String> types, List<String> roles, List<String[]> tags){
		
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
			totalRelations.putIfAbsent(r, idrelation);
			return idrelation;
		}
	}
	
	/** Dado un Value de un Map devuelve su Key
	 * @param map Mapa
	 * @param id Value en el map para obtener su Key
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public synchronized Object getKeyFromValue(Map <Object, Long> map, Long id){
		
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
	public synchronized List<WayOsm> getWays(List<Long> ids){
		List<WayOsm> ways = new ArrayList<WayOsm>();
		
		for (Long l: ids)
			ways.add(((WayOsm) getKeyFromValue((Map<Object, Long>) ((Object)totalWays), l)));
		
		return ways;
	}
	
	
	/** Dada una lista de identificadores de nodes, devuelve una lista con esos
     * nodes
     * @return nodes lista de NodeOsm
     */
    @SuppressWarnings("unchecked")
    public synchronized List<NodeOsm> getNodes(List<Long> ids){
        List<NodeOsm> nodes = new ArrayList<NodeOsm>();
        
        for (Long l: ids)
            nodes.add(((NodeOsm) getKeyFromValue((Map<Object, Long>) ((Object)totalNodes), l)));
        
        return nodes;
    }
	
}
