import org.opengis.feature.type.AttributeType;


public class ShapeAttribute {

	AttributeType tipo;
	Object valor;

	public ShapeAttribute(AttributeType a, Object o){

		tipo = a;
		valor = o;

	}

	public AttributeType getAttributeType(){

		return tipo;
	}

	public Object getValue(){

		return valor;
	}

	public String toString(){

		return (tipo.getName()+" = "+valor);
	}
	
}
