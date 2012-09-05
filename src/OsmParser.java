import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException; 

import com.vividsolutions.jts.geom.Coordinate;

public class OsmParser{

	/** ES INNECESARIO, NO SE VA A USAR. EN UN PRINCIPIO SE OPTO POR DESCARGAR Y PARSEAR LA ZONA A 
	 * IMPORTAR CON INTENCION DE REUTILIZAR EL MAXIMO NUMERO DE NODOS Y VIAS YA EXISTENTES. 
	 * PERO ESO YA LO HACE EL SERVIDOR CUANDO NOSOTROS SUBIMOS LA INFORMACION NUEVA.
	 */
	
	/** Parsea el archivo osm para meter los elementos existentes en las listas para manejar
	 * repetidos por si alguno puede ser reutilizable
	 * @param file Archivo osm que se lee
	 * @param utils Clase utils para tener acceso a las listas
	 */
    public OsmParser (File file, Cat2OsmUtils utils){
    	
    	Long Id = (long) 0; // Id que anadimos a las importaciones de lo que ya hay en OSM
    	// de lo contrario al llamar a simplify ways, este podria juntar algunos ways de los
    	// shapefiles con lo descargado de osm.
    	
    try {

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse (file);

            // Normalizar el texto
            doc.getDocumentElement ().normalize ();
            
            // NODEOSM
            // Cargamos todos los nodos en una lista
            NodeList listOfNodes = doc.getElementsByTagName("node");

            // Analizamos cada nodo de los que hemos cargado
            for(int x=0; x<listOfNodes.getLength() ; x++){

                Node nodeOsmNodo = listOfNodes.item(x);
                if(nodeOsmNodo.getNodeType() == Node.ELEMENT_NODE){

                	// Cogemos nodo
                    Element nodeOsmElement = (Element)nodeOsmNodo;
                    
                    // Creamos la coordenada y el nodo con esa coordenada
                    Coordinate coor = new Coordinate();
                    coor.x = Double.parseDouble(nodeOsmElement.getAttribute("lon"));
                    coor.y = Double.parseDouble(nodeOsmElement.getAttribute("lat"));              
                    NodeOsm nodeOsm = new NodeOsm(coor);
                    
                    // Cargamos todos los tags en una lista
                    List<String[]> tags = new ArrayList<String[]>();
                    NodeList listOfTags = nodeOsmElement.getElementsByTagName("tag");
                    
                    // Analizamos cada tag
                    for(int y=0; y<listOfTags.getLength() ; y++){
                    	Node tagOsmNodo = listOfTags.item(y);
                        if(tagOsmNodo.getNodeType() == Node.ELEMENT_NODE){
                        	
                        	// Cogemos el tag y lo metemos en la lista
                            Element tagOsmElement = (Element)tagOsmNodo;
                    		String[] s = new String[2];
                    		s[0] = tagOsmElement.getAttribute("k"); s[1] = eliminarComillas(tagOsmElement.getAttribute("v"));
                			tags.add(s);
                            
                        }
                        
                    }
                    
                    // Metemos el nodo en la lista para manejar repetidos ya con sus tags que hay
                    // en OSM
                    nodeOsm.addTags(tags); 
                    utils.addNode("PARSER",nodeOsm, Long.parseLong(nodeOsmElement.getAttribute("id")));
                }
            }
            
           
            // WAYOSM
            // Cargamos todos los ways en una lista
            NodeList listOfWays = doc.getElementsByTagName("way");
            
            // Analizamos cada way de los que hemos cargado
            for(int x=0; x<listOfWays.getLength() ; x++){

                Node wayOsmNodo = listOfWays.item(x);
                if(wayOsmNodo.getNodeType() == Node.ELEMENT_NODE){

                	// Cogemos way
                    Element wayOsmElement = (Element)wayOsmNodo;             
                    
                    //Cargamos las referencias de los nodos que la componen en una lista
                    List<Long> refs = new ArrayList<Long>();
                    NodeList listOfRefs = wayOsmElement.getElementsByTagName("nd");
                    
                    // Analizamos cada ref
                    for(int y=0; y<listOfRefs.getLength() ; y++){
                    	Node refOsmNodo = listOfRefs.item(y);
                        if(refOsmNodo.getNodeType() == Node.ELEMENT_NODE){
                        	
                        	// Cogemos la ref y lo metemos en la lista
                            Element refOsmElement = (Element)refOsmNodo;
                			refs.add(Long.parseLong(refOsmElement.getAttribute("ref")));
                        }
                        
                    }
                    
                    WayOsm wayOsm = new WayOsm(refs);
                    
                    // Anadimos su id al way
                    Id++;
                    List<String> shapes = new ArrayList<String>();
                    shapes.add("OSM"+Id);
                    wayOsm.addShapes(shapes);
                    
                    // Cargamos todos los tags en una lista
                    List<String[]> tags = new ArrayList<String[]>();
                    NodeList listOfTags = wayOsmElement.getElementsByTagName("tag");
                    
                    // Analizamos cada tag
                    for(int y=0; y<listOfTags.getLength() ; y++){
                    	Node tagOsmNodo = listOfTags.item(y);
                        if(tagOsmNodo.getNodeType() == Node.ELEMENT_NODE){
                        	
                        	// Cogemos el tag y lo metemos en la lista
                            Element tagOsmElement = (Element)tagOsmNodo;
                    		String[] s = new String[2];
                    		s[0] = tagOsmElement.getAttribute("k"); s[1] = eliminarComillas(tagOsmElement.getAttribute("v"));
                			tags.add(s);
                        }
                    }
                    
                    // Metemos el nodo en la lista para manejar repetidos ya con sus tags que hay
                    // en OSM
                    // Se ha quitado la lista de tags de WayOsm ya que este parser no se va a utilizar.
                    //if (!tags.isEmpty())
                    	//wayOsm.addTags(tags);
                    utils.addWay("PARSER",wayOsm, Long.parseLong(wayOsmElement.getAttribute("id")));
                }
            }
            
            
         // RELATIONOSM
            // Cargamos todos las relations en una lista
            NodeList listOfRelations = doc.getElementsByTagName("relation");
            
            // Analizamos cada relation de las que hemos cargado
            for(int x=0; x<listOfRelations.getLength() ; x++){

                Node relationOsmNodo = listOfRelations.item(x);
                if(relationOsmNodo.getNodeType() == Node.ELEMENT_NODE){

                	// Cogemos relation
                    Element relationOsmElement = (Element)relationOsmNodo;             
                    
                    //Cargamos los members que la componen en una lista
                	List <Long> ids = new ArrayList<Long>(); // Ids de los ways members
                	List <String> types = new ArrayList<String>(); // Tipos (way) de los members
                	List <String> roles = new ArrayList<String>(); // Roles de los members
                    NodeList listOfMembers = relationOsmElement.getElementsByTagName("member");
                    
                    // Analizamos cada ref
                    for(int y=0; y<listOfMembers.getLength() ; y++){
                    	Node memberOsmNodo = listOfMembers.item(y);
                        if(memberOsmNodo.getNodeType() == Node.ELEMENT_NODE){
                        	
                        	// Cogemos el member y lo metemos en la lista
                            Element refOsmElement = (Element)memberOsmNodo;
                			ids.add(Long.parseLong(refOsmElement.getAttribute("ref")));
                			types.add(refOsmElement.getAttribute("type"));
                			roles.add(refOsmElement.getAttribute("role"));
                        }
                        
                    }
                    
                    RelationOsm relationOsm = new RelationOsm(ids, types, roles);
                    
                    // Cargamos todos los tags en una lista
                    List<String[]> tags = new ArrayList<String[]>();
                    NodeList listOfTags = relationOsmElement.getElementsByTagName("tag");
                    
                    // Analizamos cada tag
                    for(int y=0; y<listOfTags.getLength() ; y++){
                    	Node tagOsmNodo = listOfTags.item(y);
                        if(tagOsmNodo.getNodeType() == Node.ELEMENT_NODE){
                        	
                        	// Cogemos el tag y lo metemos en la lista
                            Element tagOsmElement = (Element)tagOsmNodo;
                    		String[] s = new String[2];
                    		s[0] = tagOsmElement.getAttribute("k"); s[1] = eliminarComillas(tagOsmElement.getAttribute("v"));
                			tags.add(s);
                        }
                    }
                    
                    // Metemos el nodo en la lista para manejar repetidos ya con sus tags que hay
                    // en OSM
                    if (!tags.isEmpty())
                    	relationOsm.addTags(tags);
                    utils.addRelation("PARSER",relationOsm, Long.parseLong(relationOsmElement.getAttribute("id")));
                }
            }


        }catch (SAXParseException err) {
        System.out.println ("["+new Timestamp(new Date().getTime())+"] Error de parseo, linea " + err.getLineNumber () + ", uri " + err.getSystemId ());
        System.out.println(" " + err.getMessage ());

        }catch (SAXException e) {
        Exception x = e.getException ();
        ((x == null) ? e : x).printStackTrace ();

        }catch (Throwable t) {
        t.printStackTrace ();}

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
