import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		// Ruta al fichero de configuracion por parametro
		new Config(args[0]);
		Cat2OsmUtils utils = new Cat2OsmUtils();
		Cat2Osm catastro = new Cat2Osm(utils);
		List<Shape> shapes = new ArrayList<Shape>();
		
		// Seleccionamos los archivos .shp (Necesario que esten tambien los .shx, .prj y .dbf)
		System.out.println("\nLeyendo SHAPEFile CONSTRU");
		shapes.addAll( catastro.shpParser(new File(Config.get("ConstruSHPFile"))) );
		
		System.out.println("\nLeyendo SHAPEFile SUBPARCE");
		shapes.addAll( catastro.shpParser(new File(Config.get("SubparceSHPFile"))) );
		
		System.out.println("\nLeyendo SHAPEFile PARCELA");
		shapes.addAll( catastro.shpParser(new File(Config.get("ParcelaSHPFile"))) );
		
		System.out.println("\nLeyendo SHAPEFile MASA");
		shapes.addAll( catastro.shpParser(new File(Config.get("MasaSHPFile"))) );
		
		// Seleccionamos el archivo .cat
		// Los otros shapefiles que vienen despues no tienen referencia catastral por lo que 
		// no hay forma de relacionarlos con los registros de catastro.
		System.out.println("\nLeyendo CATFile");
		catastro.catParser(new File(Config.get("CATFile")), shapes);

		// Seleccionamos los otros archivos .shp (Necesario que esten tambien los .shx, .prj y .dbf)
		System.out.println("\nLeyendo SHAPEFile ELEMLIN");
		shapes.addAll( catastro.shpParser(new File(Config.get("ElemlinSHPFile"))) );
		
		System.out.println("\nLeyendo SHAPEFile ELEMPUN");
		shapes.addAll( catastro.shpParser(new File(Config.get("ElempunSHPFile"))) );
		
		System.out.println("\nLeyendo SHAPEFile ELEMTEX");
		shapes.addAll( catastro.shpParser(new File(Config.get("ElemtexSHPFile"))) );
		
		System.out.println("\nLeyendo SHAPEFile EJES");
		shapes.addAll( catastro.shpParser(new File(Config.get("EjesSHPFile"))) );
		
		// Escribir los datos
		System.out.println("\nEscribiendo "+ Cat2Osm.utils.getTotalNodes().size() +" NODOS");
		catastro.printNodes( Cat2Osm.utils.getTotalNodes());
		System.out.println("\nEscribiendo "+ Cat2Osm.utils.getTotalWays().size() +" WAYS");
		catastro.printWays(Cat2Osm.utils.getTotalWays());
		System.out.println("\nEscribiendo "+ Cat2Osm.utils.getTotalRelations().size() +" RELATIONS");
		catastro.printRelations( Cat2Osm.utils.getTotalRelations());
		System.out.println("\nJUNTANDO los tres archivos");
		catastro.joinFiles(Config.get("ResultFileName"));
		System.out.println("\nTERMINADO");

	}


	
}
