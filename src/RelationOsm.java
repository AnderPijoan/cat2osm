import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class RelationOsm {

	private List <Long> ids; // Ids de los members
	private List <String> types; // Tipos de los members
	private List <String> roles; // Roles de los members
	private String refCatastral; // Referencia catastral para manejar las relaciones de relaciones
	private List<String[]> tags;

	
	public RelationOsm(List <Long> ids, List<String> types, List<String> roles){
		this.ids = ids;
		this.types = types;
		this.roles = roles;
		tags = new ArrayList<String[]>();
	}
	
	public void addMember(Long id , String type, String role){
		if (!ids.contains(id)){
		ids.add(id);
		types.add(type);
		roles.add(role);}
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
	
	public List<String[]> getTags() {
		return tags;
	}
	
	public void addTag(String[] tag){
		
		boolean encontrado = false;
		
		for (int x = 0; !encontrado && x < this.tags.size(); x++)
			if (this.tags.get(x)[0].equals(tag[0])){
				this.tags.get(x)[1] = tag[1];
				encontrado = true;
			}

		if (!encontrado)
			this.tags.add(tag);
	}
	
	public void addTags(List<String[]> tags) {
		
		for (int x = 0; x < tags.size(); x++){

			boolean encontrado = false;

			for (int y = 0; !encontrado && y < this.tags.size(); y++)
				if (this.tags.get(y)[0].equals(tags.get(x)[0])){
					this.tags.get(y)[1] = tags.get(x)[1];
					encontrado = true;
				}
			if (!encontrado)
				this.tags.add(tags.get(x));
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

	/** Imprime en el formato Osm la relation con la informacion
	 * @param id Id de la relation
	 * @return Devuelve en un String la relation lista para imprimir
	 */
	public String printRelation(Long id, Cat2OsmUtils utils){
		String s = null;
		
		Date date = new java.util.Date();
		s = ("<relation id=\""+ id +"\" visible=\"true\"  version=\"6\" timestamp=\""+ new Timestamp(date.getTime()) +"\" uid=\"533679\" user=\"AnderPijoan\">\n");
		
		for (int x = 0; x < ids.size(); x++)
			if (utils.getTotalWays().containsValue(ids.get(x)))
			s += ("<member type=\""+ types.get(x) +"\" ref=\""+ ids.get(x)+"\" role=\""+ roles.get(x) +"\" />\n");
		
		for (int x = 0; x < tags.size(); x++)
			s += "<tag k=\""+tags.get(x)[0]+"\" v=\""+tags.get(x)[1]+"\"/>\n";
		
		s += ("</relation>\n");
		
		return s;
	}
	
}
