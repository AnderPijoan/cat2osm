import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class RelationOsm {

	private List <Long> ids; // Ids de los ways members
	private List <String> types; // Tipos (way) de los members
	private List <String> roles; // Roles de los members
	private String refCatastral; // Referencia catastral para manejar las relaciones de relaciones
	private List<String[]> tags;
	private List<String> shapes; // Ids de los shapes a los que pertenece
	private long fechaConstru = Cat2OsmUtils.getFechaArchivos(); // Fecha de construccion de la parcela que representa AAAAMMDD


	public RelationOsm(List <Long> ids, List<String> types, List<String> roles){
		this.ids = ids;
		this.types = types;
		this.roles = roles;
		tags = new ArrayList<String[]>();
		shapes = new ArrayList<String>();
	}


	public void addMember(Long id , String type, String role){
		if (!ids.contains(id)){
			ids.add(id);
			types.add(type);
			roles.add(role);}

		// Borramos si existiese algun nulo
		ids.remove(null);
		types.remove(null);
		roles.remove(null);
	}


	/** Inserta un nuevo member y desplaza los existentes a la derecha
	 * @param pos
	 * @param id
	 * @param type
	 * @param role
	 */
	public void addMember(int pos, Long id , String type, String role){
		if (!ids.contains(id)){
			ids.add(pos,id);
			types.add(pos,type);
			roles.add(pos,role);}

		// Borramos si existiese algun nulo
		ids.remove(null);
		types.remove(null);
		roles.remove(null);
	}


	public synchronized void removeMember(Long id){
		if (ids.contains(id)){
			int pos = ids.indexOf(id);
			ids.remove(pos);
			types.remove(pos);
			roles.remove(pos);
		}
	}


	public List<Long> getIds() {
		return ids;
	}


	public List<String> getTypes() {
		return types;
	}


	public List<String> getRoles() {
		return roles;
	}


	public String getRefCat(){
		return refCatastral;
	}
	
	public void setRefCat(String refCat){
		this.refCatastral = refCat;
	}


	public List<String[]> getTags() {
		return tags;
	}

	/** Anade tag a la relacion. Si ya existe un tag con esa clave lo sobreescribe por eso
	 * vamos a indicar que si la clave viene con '*' por delante, si ya existe un tag con esa
	 * clave, no lo sobreescriba. Es porque primero se leen los shapefiles y despues los cat,
	 * los shapefiles tienen algunos tags mas concretos que los .cat y entonces al venir despues
	 * serian machacados por los del .cat.
	 * @param tag Nuevo tag a anadir
	 */
	public void addTag(String[] tag){

		// Nos interesa conservar todos los Ids de los shapes a los que
		// pertenece esa relacion. Los demas tags comprueba si empieza por '*'
		// si es asi, conserva si ya existiese uno con esa clave, si no empieza
		// por '*' lo machaca si existiese uno igual.
		// con el nuevo valor.
		// También comprobamos que si es un *landuse, no se introduzca en relaciones
		// en las que haya tags que ya especifican que uso y de forma mas correcta
		boolean encontrado = (tag[0].equals("CAT2OSMSHAPEID"));
		String[] s = {tag[0].replace("*", ""), tag[1].replaceAll("\"", "")};

		for (int x = 0; !encontrado && x < this.tags.size(); x++){
			if (tag[0].startsWith("*") && (s[0].equals(this.tags.get(x)[0]) || (this.tags.get(x)[0].equals("natural") && this.tags.get(x)[1].equals("water")) || (this.tags.get(x)[0].equals("waterway") && this.tags.get(x)[1].equals("riverbank")) ))
				encontrado = true;

			else if (this.tags.get(x)[0].equals(s[0])){
				this.tags.get(x)[1] = s[1];
				encontrado = true;
			}
		}

		if (!encontrado || (tag[0].equals("CAT2OSMSHAPEID"))){
			this.tags.add(s);
		}
	}

	/** Anade tags a la relacion. Si ya existe un tag con esa clave lo sobreescribe por eso
	 * vamos a indicar que si la clave viene con '*' por delante, si ya existe un tag con esa
	 * clave, no lo sobreescriba. Es porque primero se leen los shapefiles y despues los cat,
	 * los shapefiles tienen algunos tags mas concretos que los .cat y entonces al venir despues
	 * serian machacados por los del .cat.
	 * @param tags Lista de tags a anadir en la relacion
	 */
	public void addTags(List<String[]> tags) {

		for (int x = 0; x < tags.size(); x++){

			// Nos interesa conservar todos los Ids de los shapes a los que
			// pertenece esa relacion. Los demas tags comprueba si empieza por '*'
			// si es asi, conserva si ya existiese uno con esa clave, si no empieza
			// por '*' lo machaca si existiese uno igual con el nuevo valor.
			boolean encontrado = (tags.get(x)[0].equals("CAT2OSMSHAPEID"));
			String[] s = {tags.get(x)[0].replace("*", ""), tags.get(x)[1].replaceAll("\"", "")};

			for (int y = 0; !encontrado && y < this.tags.size(); y++){
				if (tags.get(x)[0].startsWith("*") && (s[0].equals(this.tags.get(y)[0]) || (this.tags.get(y)[0].equals("natural") && this.tags.get(y)[1].equals("water")) || (this.tags.get(x)[0].equals("waterway") && this.tags.get(x)[1].equals("riverbank")) ))
					encontrado = true;
				else if (this.tags.get(y)[0].equals(s[0])){
					this.tags.get(y)[1] = s[1];
					encontrado = true;
				}
			}
			if (!encontrado || (s[0].equals("CAT2OSMSHAPEID"))){
				this.tags.add(s);
			}
		}
	}


	public List<Long> sortIds(){
		List<Long> result = new ArrayList<Long>();
		for (Long l : ids)
			result.add(l);
		Collections.sort(result);
		return result;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sortIds() == null) ? 0 : sortIds().hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RelationOsm other = (RelationOsm) obj;
		if (ids == null) {
			if (other.ids != null)
				return false;
		} else if (!sortIds().equals(other.sortIds()))
			return false;
		if (roles == null) {
			if (other.roles != null)
				return false;
		} else if (!roles.equals(other.roles))
			return false;
		if (types == null) {
			if (other.types != null)
				return false;
		} else if (!types.equals(other.types))
			return false;
		return true;
	}


	/** Para evitar que elementos lineales compuestos de varios
	 * ways que no cierran un area tengan el tag multipoligono.
	 * @param utils Clase utils para acceder a la lista de ways
	 * @return si es un area sin cerrar o no
	 */
	public boolean AreaCerrada(Cat2OsmUtils utils){

		return true;
	}


	/** Imprime en el formato Osm la relation con la informacion. En caso de que
	 * la relacion solo tenga un way, la devuelve como way ya que sino es
	 * redundante.
	 * @param id Id de la relation
	 * @return Devuelve en un String la relation lista para imprimir
	 */
	@SuppressWarnings({ "unchecked" })
	public String printRelation(String key, Long id, Cat2OsmUtils utils){
		String s = "";

		// Si una relation tiene menos de dos ways, deberia quedarse como way
		// ya que sino es redundante.
		if (ids.size() == 1){

			// Un way que va a ser inner en una relation solo tiene que tener los tags distintos
			// respecto a sus outter
			for (RelationOsm relation : utils.getTotalRelations().get(key).keySet()){

				// Si una tiene el id del way que vamos a imprimir
				if ( relation.getIds().contains(ids.get(0))){

					// Cogemos la posicion que ocupa ese id en la lista de ids de la relacion
					int pos = relation.getIds().indexOf(ids.get(0));

					// Si tiene un role de inner
					if (relation.getRoles().get(pos).equals("inner")){
						for (int y = 0; y < relation.getIds().size(); y++)
							if (y != pos)
								for (int z = 0; z < relation.getTags().size(); z++)
									for (int w = 0; w < this.tags.size(); w++)
										// Eliminamos los tags que coincidan ya que los del inner estan implicitos y no hay que escribirlos
										if ( (this.tags.get(w)[0]).equals(relation.getTags().get(z)[0]) && (this.tags.get(w)[1]).equals(relation.getTags().get(z)[1]))
											this.tags.remove(w);

						// Este metodo de arriba se carga el building=yes y es necesario si tiene un building:levels
						for (String[] tag : relation.getTags())
							if (tag[0].equals("building:levels")){
								relation.addTag(new String[]{"building","yes"});
							}
					}
				}
			}

			WayOsm way = ((WayOsm) utils.getKeyFromValue((Map<String, Map<Object, Long>>) ((Object)utils.getTotalWays()), key, ids.get(0)));

			if (way == null){
				System.out.println("["+new Timestamp(new Date().getTime())+"] Una vía ha dado un error y no se imprimirá. Esto dejará sus nodos sueltos.");
				return "";
			}

			s = ("<way id=\""+ ids.get(0) +"\" timestamp=\""+new Timestamp(new Date().getTime())+"\" version=\"6\">\n");
			
			way.getNodes().remove(null);
			
			// Referencias a los nodos
			for (Long nodeId : way.getNodes()){				
					s += ("<nd ref=\""+ nodeId +"\"/>\n");
			}
			
			// Mostrar los shapes que usan ese way, para debugging
			if (way.getShapes() != null && Config.get("PrintShapeIds").equals("1"))
				for (int x = 0; x < way.getShapes().size(); x++)
					s += "<tag k=\"CAT2OSMSHAPEID"+x+"\" v=\""+way.getShapes().get(x)+"\"/>\n";

			// Imprimir los tags
			for (int x = 0; x < tags.size(); x++) {

				// Filtramos para que no salgan todos los tags, siguiente bucle se explica el porque
				if (!tags.get(x)[0].equals("addr:housenumber") && !tags.get(x)[0].equals("addr:postcode") && !tags.get(x)[0].equals("addr:country") && !tags.get(x)[0].equals("addr:street") && !tags.get(x)[0].equals("name") && !tags.get(x)[0].startsWith("CAT2OSMSHAPEID"))
					s += "<tag k=\""+tags.get(x)[0]+"\" v=\""+tags.get(x)[1]+"\"/>\n";

				// El tag addr:housenumber, addr:street, addr:postcode y addr:country
				// solo se puede asignar a parcelas y en ejes
				// Por eso habra que hacer otra iteracion para comprobar si es una relation de un
				// shapeParcela
				else if (tags.get(x)[0].equals("addr:housenumber") || tags.get(x)[0].equals("addr:postcode") || tags.get(x)[0].equals("addr:country")|| tags.get(x)[0].equals("addr:street") || tags.get(x)[0].equals("name")){
					for (String[] tag : tags)
						if (tag[0].startsWith("CAT2OSMSHAPEID") && (tag[1].startsWith("PARCELA") || tag[1].startsWith("EJES") ))
							s += "<tag k=\""+tags.get(x)[0]+"\" v=\""+tags.get(x)[1]+"\"/>\n";
				}

				// Mostrar los shapes que utilizan esta relacion, para debugging
				else if (tags.get(x)[0].startsWith("CAT2OSMSHAPEID") && Config.get("PrintShapeIds").equals("1"))
					s += "<tag k=\""+tags.get(x)[0]+"\" v=\""+tags.get(x)[1]+"\"/>\n";
			}

			s += "<tag k=\"source\" v=\"catastro\"/>\n";
			s += "<tag k=\"source:date\" v=\""+new StringBuffer(Cat2OsmUtils.getFechaArchivos()+"").insert(4, "-").toString().substring(0, 7)+"\"/>\n";

			s += ("</way>\n");
		}

		// En caso de que tenga varios ways, si que se imprime como una relacion de ways.
		else {

			// Variable para comprobar si una relacion tiene datos relevantes
			// Ya se ha hecho este proceso en un punto anterior de la ejecucion pero ahora se comprueban
			// algunos tags mas que no son relevantes a no ser que sean en parcelas
			boolean with_data = false;
			
			if (Cat2OsmUtils.hasRelevantTags(tags))
				with_data = true;

			s = ("<relation id=\""+ id +"\" timestamp=\""+new Timestamp(new Date().getTime())+"\" visible=\"true\"  version=\"6\">\n");

			for (int x = 0; x < ids.size(); x++)
				if (utils.getTotalWays().get(key).containsValue(ids.get(x)))
					s += ("<member type=\""+ types.get(x) +"\" ref=\""+ ids.get(x)+"\" role=\""+ roles.get(x) +"\" />\n");			

			for (int x = 0; x < tags.size(); x++){
				
				// Filtramos para que no salgan todos los tags, abajo se explica el porque
				if (!tags.get(x)[0].equals("addr:housenumber") && !tags.get(x)[0].equals("addr:postcode") && !tags.get(x)[0].equals("addr:country") && !tags.get(x)[0].equals("addr:street") && !tags.get(x)[0].equals("name") && !tags.get(x)[0].startsWith("CAT2OSMSHAPEID")) {

					s += "<tag k=\""+tags.get(x)[0]+"\" v=\""+tags.get(x)[1]+"\"/>\n";
				}

				// El tag addr:housenumber, addr:street,  addr:full, addr:postcode, addr:country y name 
				// solo se puede asignar a parcelas y ejes. Por eso habra
				// que hacer otra iteracion para comprobar si es una relation de un
				// shapeParcela	
				else if (tags.get(x)[0].equals("addr:housenumber") || tags.get(x)[0].equals("addr:postcode") || tags.get(x)[0].equals("addr:country") || tags.get(x)[0].equals("addr:street") || tags.get(x)[0].equals("name")){
					for (String[] tag : tags)
						if (tag[0].startsWith("CAT2OSMSHAPEID") && (tag[1].startsWith("PARCELA") || tag[1].startsWith("EJES") )) {
							with_data = true;
							s += "<tag k=\""+tags.get(x)[0]+"\" v=\""+tags.get(x)[1]+"\"/>\n";
						}
				}

				// Mostrar los shapes que utilizan esta relacion, para debugging
				else if (tags.get(x)[0].startsWith("CAT2OSMSHAPEID") && Config.get("PrintShapeIds").equals("1"))
					s += "<tag k=\""+tags.get(x)[0]+"\" v=\""+tags.get(x)[1]+"\"/>\n";
			}

			// Al tener varios ways si forman un area cerrada
			//sera un multipolygono
			if (AreaCerrada(utils))
				s += "<tag k=\"type\" v=\"multipolygon\"/>\n";

			s += "<tag k=\"source\" v=\"catastro\"/>\n";
			s += "<tag k=\"source:date\" v=\""+new StringBuffer(Cat2OsmUtils.getFechaArchivos()+"").insert(4, "-").toString().substring(0, 7)+"\"/>\n";

			s += ("</relation>\n");

			if (!with_data){
				return "";
			}

		}

		return s;
	}
	
	/** Comprueba si dos relaciones tienen los mismos tags.
	 * Se usa para hacer la union de relaciones (principalmente en subparcelas).
	 * @param relation
	 * @return
	 */
	public boolean sameTags(RelationOsm relation){
		
		boolean tagsCoinciden = true;
		
		for(String[] tag : relation.getTags()){
			
			boolean tagEncontrado = false;
			
			if(!tag[0].equals("catastro:ref") && !tag[0].equals("CAT2OSMSHAPEID") &&
					!tag[0].equals("addr:street") && !tag[0].equals("addr:housenumber") &&
					!tag[0].equals("unible")){
				for(String[] tag2 : this.tags){
					if (tag[0].trim().equals(tag2[0].trim()) && tag[1].trim().equals(tag2[1].trim()))
						tagEncontrado = true;
				}
				tagsCoinciden = tagsCoinciden && tagEncontrado;
			}
			
			if(!tagsCoinciden)
				return false;
		}
		
			for(String[] tag : this.tags){
			
			boolean tagEncontrado = false;
			
			if(!tag[0].equals("catastro:ref") && !tag[0].equals("CAT2OSMSHAPEID") &&
					!tag[0].equals("addr:street") && !tag[0].equals("addr:housenumber") &&
					!tag[0].equals("unible")){
				for(String[] tag2 : relation.getTags()){
					if (tag[0].equals(tag2[0]) && tag[1].equals(tag2[1]))
						tagEncontrado = true;
				}

				tagsCoinciden = tagsCoinciden && tagEncontrado;
			}
			
			if(!tagsCoinciden)
				return false;
		}
			
		return tagsCoinciden;
	}
	
	/** Comprueba si dos relations estan una al lado de la otra.
	 * De ser asi el ID de uno de sus ways debe ser compartido por las dos.
	 * @param relation
	 * @return
	 */
	public boolean isNextTo(RelationOsm relation){
		
		for(Long wayId : relation.getIds())
			if(this.ids.contains(wayId))
				return true;
		
		return false;
	}

	public List<String> getShapes() {
		return shapes;
	}


	public void setShapes(List<String> shapes) {
		this.shapes = shapes;
	}
	
	public void addShapes(List<String> shapes) {
		this.shapes.addAll(shapes);
	}


	public void addShapes(String shape) {
		if (!this.shapes.contains(shape))
			this.shapes.add(shape);
	}


	public long getFechaConstru() {
		return fechaConstru;
	}


	public void setFechaConstru(long fechaConstru) {
		if (this.fechaConstru > fechaConstru)
			this.fechaConstru = fechaConstru;
	}

}
