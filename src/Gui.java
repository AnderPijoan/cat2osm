import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.jfree.ui.ExtensionFileFilter;

public class Gui extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {


		if (args.length == 0){
			System.out.println("Cat2Osm version 2012-03-02.\n");
			System.out.println("Forma de uso:");
			System.out.println("java -jar [-XmxMemoria] cat2osm.jar [Opcion] [RutaArchivoConfig]\n");
			System.out.println("Es necesrio indicarle una opción o pasarle el archivo de configuración:");
			System.out.println("-ui                     Inicia la interfaz de usuario para crear el archivo de configuracion");
			System.out.println("-v                      Muestra la version de Cat2Osm");
			System.out.println("rutaarchivoconfig       Ruta al archivo de configuracion para ejecutar Cat2Osm con los parametros que se indiquen en el\n");
			System.out.println("Para mas informacion acceder a:");
			System.out.println("http://wiki.openstreetmap.org/wiki/Spanish_Catastro");
			System.out.println("");
		}
		else if (args[0].equals("-v")){
			System.out.println("Cat2Osm version 2012-03-02.");
		}
		else if (args[0].equals("-ui")){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando interfaz visual para crear el archivo de configuracion.");
			// Iniciar el interfaz visual
			new Gui();
		}
		else if (new File(args[0]).exists()){
			System.out.println("["+new Timestamp(new Date().getTime())+"] Iniciando Cat2Osm con el archivo de configuracion.");
			// Ruta al fichero de configuracion por parametro
			new Config(args[0]);
			ejecutarCat2Osm();
		}
		else {
			System.out.println("["+new Timestamp(new Date().getTime())+"] No se ha encontrado el archivo de configuracion, compruebe que la ruta es correcta.");
		}

	}

	/** Interfaz visual en caso de que no se encuentre el archivo de configuracion
	 */
	public Gui (){
		super("Cat2Osm, ayuda para crear el archivo de configuración.");
		this.setSize(1000, 800);
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setExtendedState(JFrame.MAXIMIZED_BOTH);

		JPanel options = new JPanel();
		options.setLayout(new GridLayout(1,2));


		JPanel labels = new JPanel();
		labels.setLayout(new GridLayout(15,1));

		String[] labelsText = {"Carpeta donde se exportarán los archivos temporales y el resultado.\n(Tiene que tener privilegios lectura/escritura).",
				"Nombre del archivo que exportará Cat2Osm como resultado.",
				"Ruta a la CARPETA (con nombre generalmente XX_XXX_UA_XXXX-XX-XX_SHF) que contiene dentro las SUBCARPETAS EXTRAIDAS de los shapefiles URBANOS.\n",
				"Ruta a la CARPETA (con nombre generalmente XX_XXX_RA_XXXX-XX-XX_SHF) que contiene dentro las SUBCARPETAS EXTRAIDAS de los shapefiles RUSTICOS.\n", 
				"Ruta al ARCHIVO EXTRAIDO .CAT URBANO.",
				"Ruta al ARCHIVO EXTRAIDO .CAT RÚSTICO.", 
				"Ruta al directorio principal de FWTools.\n(De momento no es necesario)", 
				"Ruta al ARCHIVO de la rejilla de la península (PENR2009.gsb o peninsula.gsb).\n(Necesario para reprojectar, se puede descargar en http://www.01.ign.es/ign/layoutIn/herramientas.do#DATUM)",
				"Projección en la que se encuentran los archivos shapefile." +
						"\n32628 para WGS84/ Zona UTM 29N"+
						"\n23029 para ED_1950/ Zona UTM 29N,"+
						"\n23030 para ED_1950/ Zona UTM 30N,"+
						"\n23031 para ED_1950/ Zona UTM 31N,"+
						"\n25829 para ETRS_1989/ Zona UTM 29N,"+
						"\n25830 para ETRS_1989/ Zona UTM 30N,"+
						"\n25831 para ETRS_1989/ Zona UTM 31N"+
						"\n(Se puede comprobar abriendo con un editor de texto cualquiera de los archivos .PRJ)",
						"Si se quiere delimitar una fecha desde la cual coger los datos de catastro (Formato AAAAMMDD).\nTomará los datos que se han dado de alta a partir de esta fecha.\nEjemeplo: 20060127 (27 de Enero del 2006)",
						"Si se quiere delimitar una fecha hasta la cual coger los datos (Formato AAAAMMDD).\nTomará los shapes que se han dado de alta hasta esta fecha y siguen sin darse de baja después.\nEjemeplo: 20060127 (27 de Enero del 2006)", 
						"Tipo de Registro de catastro a usar (0 = todos).\nLos registros de catastro tienen la mayoría de la información necesaria para los shapefiles.",
						"Cambiar tags de las construcciones si contienen sobre ellas un texto (Elemtex) al cual se le puede asociar un tag distinto.\nPor ejemplo si es un cementerio modificar el landuse a landuse=cemetery. (Ralentiza un poco el proceso)",
						"Analizar el archivo shapefile EJES, para las rutas y carreteras. En Catastro estas no estan unidas y se componen de segmentos independientes. Utilizarlo no es recomendable salvo en casos en los que no existan carreteras importadas ya que produce un resultado que requiere de muchisimos arreglos por parte del usuario.",
		"Imprimir tanto en las vías como en las relaciones la lista de shapes que las componen o las utilizan.\nEs para casos de debugging si se quiere tener los detalles."};

		JPanel buttons = new JPanel();
		buttons.setLayout(new GridLayout(15,1));


		JButton resultPath, urbanoShpPath, rusticoShpPath, urbanoCatFile, rusticoCatFile, rejillaFile = null;

		final JFileChooser fcResult = new JFileChooser();
		fcResult.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		final JTextField resultFileName = new JTextField("Resultado");

		final JFileChooser fcShpUr = new JFileChooser();
		fcShpUr.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		final JFileChooser fcShpRu = new JFileChooser();
		fcShpRu.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		final JFileChooser fcCatUr = new JFileChooser();
		final JFileChooser fcCatRu = new JFileChooser();
		fcCatUr.setFileFilter(new ExtensionFileFilter("Archivos .cat", ".cat"));
		fcCatRu.setFileFilter(new ExtensionFileFilter("Archivos .cat", ".cat"));

		final JFileChooser fcGsb = new JFileChooser();
		fcGsb.setFileFilter(new ExtensionFileFilter("Archivos .gsb", ".gsb"));

		final JComboBox proj = new JComboBox();

		final JTextField fdesde = new JTextField("00000000");
		final JTextField fhasta= new JTextField("99999999");

		final JComboBox tipoReg = new JComboBox();
		final JComboBox texAConstru = new JComboBox();
		final JComboBox ejes = new JComboBox();
		final JComboBox shapesId = new JComboBox();

		for (int x = 0; x < labelsText.length; x++){

			JTextArea t = new JTextArea(labelsText[x]);
			t.setLineWrap(true);
			t.setBackground(new Color(220,220,220));
			labels.add(new JScrollPane(t));

			switch (x){

			case 0:{
				resultPath = new JButton("Seleccione carpeta destino");
				resultPath.addActionListener(new ActionListener()
				{  public void actionPerformed(ActionEvent e)  
				{ int retVal = fcResult.showOpenDialog(new JFrame()); }  
				});  
				buttons.add(resultPath);
				break;
			}
			case 1:{
				buttons.add(resultFileName);
				break;
			}
			case 2:{
				urbanoShpPath = new JButton("Seleccionar carpeta shapesfiles Urbanos XX_XXX_U_XXXX-XX-XX_SHF ");
				urbanoShpPath.addActionListener(new ActionListener()  
				{  public void actionPerformed(ActionEvent e)  
				{ int retVal = fcShpUr.showOpenDialog(new JFrame()); }  
				});  
				buttons.add(urbanoShpPath);
				break;
			}
			case 3:{
				rusticoShpPath = new JButton("Seleccionar carpeta shapefiles Rústicos XX_XXX_R_XXXX-XX-XX_SHF ");
				rusticoShpPath.addActionListener(new ActionListener()  
				{  public void actionPerformed(ActionEvent e)  
				{ int retVal = fcShpRu.showOpenDialog(new JFrame()); }  
				});  
				buttons.add(rusticoShpPath);
				break;
			}
			case 4:{
				urbanoCatFile = new JButton("Seleccionar archivo .CAT Urbano XX_XX_U_XXXX-XX-XX.CAT");
				urbanoCatFile.addActionListener(new ActionListener()  
				{  public void actionPerformed(ActionEvent e)  
				{ int retVal = fcCatUr.showOpenDialog(new JFrame()); }  
				});  
				buttons.add(urbanoCatFile);
				break;
			}
			case 5:{
				rusticoCatFile = new JButton("Seleccionar archivo .CAT Rústico XX_XX_R_XXXX-XX-XX.CAT");
				rusticoCatFile.addActionListener(new ActionListener()  
				{  public void actionPerformed(ActionEvent e)  
				{ int retVal = fcCatRu.showOpenDialog(new JFrame()); }  
				});  
				buttons.add(rusticoCatFile);
				break;
			}
			case 6:{
				JTextArea l = new JTextArea("");
				l.setBackground(new Color(220,220,220));
				buttons.add(l);
				break;
			}
			case 7:{
				rejillaFile = new JButton("Seleccionar rejilla (peninsula.gsb)");   
				rejillaFile.addActionListener(new ActionListener()  
				{  public void actionPerformed(ActionEvent e)  
				{ int retVal = fcGsb.showOpenDialog(new JFrame()); }  
				});  
				buttons.add(rejillaFile);
				break;
			}
			case 8:{

				proj.addItem("32628");
				proj.addItem("23029");
				proj.addItem("23030");
				proj.addItem("23031");
				proj.addItem("25829");
				proj.addItem("25830");
				proj.addItem("25831");
				proj.setBackground(new Color(255,255,255));
				buttons.add(proj);
				break;        		
			}
			case 9:{
				buttons.add(fdesde);
				break;
			}
			case 10:{
				JTextField f = new JTextField("99999999");
				buttons.add(fhasta);
				break;
			}
			case 11:{
				tipoReg.addItem("0");
				tipoReg.addItem("11");
				tipoReg.addItem("13");
				tipoReg.addItem("14");
				tipoReg.addItem("15");
				tipoReg.addItem("16");
				tipoReg.addItem("17");
				tipoReg.setBackground(new Color(255,255,255));
				buttons.add(tipoReg);
				break;
			}
			case 12:{
				texAConstru.addItem("SI");
				texAConstru.addItem("NO");
				texAConstru.setBackground(new Color(255,255,255));
				buttons.add(texAConstru);
				break;
			}
			case 13:{
				ejes.addItem("NO");
				ejes.addItem("SI");
				ejes.setBackground(new Color(255,255,255));
				buttons.add(ejes);
				break;
			}
			case 14:{
				shapesId.addItem("NO");
				shapesId.addItem("SI");
				shapesId.setBackground(new Color(255,255,255));
				buttons.add(shapesId);
				break;
			}

			}

		}

		options.add(labels, BorderLayout.CENTER);
		options.add(buttons, BorderLayout.EAST);

		this.add(options,BorderLayout.CENTER);

		final JButton exe = new JButton("CREAR ARCHIVO DE CONFIGURACIÓN");

		// Boton de ejecutar Cat2Osm
		this.add(exe,BorderLayout.SOUTH);
		exe.addActionListener(new ActionListener()  
		{  public void actionPerformed(ActionEvent e)  
		{

			JTextArea popupText = new JTextArea("");
			popupText.setLineWrap(true);
			popupText.setWrapStyleWord(true);
			JFrame popup = new JFrame("ERROR");
			popup.setLayout(new BorderLayout());
			popup.setSize(600,400);

			if (fcResult.getSelectedFile() == null){
				popupText.append("No ha especificado dónde crear el archivo resultado. Este archivo config y el archivo resultado cuando después ejecute cat2osm se crearán ahí.\n\n");
			}

			if (resultFileName.getText().equals(null)){
				popupText.append("No ha especificado el nombre que tomará el archivo resultado de cat2osm.\n\n");
			}

			if (fcShpUr.getSelectedFile() == null){
				popupText.append("No ha especificado la carpeta que contiene todas las subcarpetas de shapefiles urbanos. " +
						"Normalmente suele ser una carpeta con el siguiente formato XX_XXX_UA_XXXX-XX-XX_SHF. " +
						"Dentro tendrán que estar las subcarpetas extraidas tal cual vienen en catastro, es decir dentro de la carpeta que indicamos debería haber una carpeta CONSTRU, ELEMLIN, ELEMPUN..." +
						"y dentro de estas (generalmente) 4 archivos.\n\n");	
			}

			if (fcShpRu.getSelectedFile() == null){
				popupText.append("No ha especificado la carpeta que contiene todas las subcarpetas de shapefiles rústicos. " +
						"Normalmente suele ser una carpeta con el siguiente formato XX_XXX_RA_XXXX-XX-XX_SHF. " +
						"Dentro tendrán que estar las subcarpetas extraidas tal cual vienen en catastro, es decir dentro de la carpeta que indicamos debería haber una carpeta CONSTRU, ELEMLIN, ELEMPUN..." +
						"y dentro de estas (generalmente) 4 archivos.\n\n");	
			}

			if (fcCatUr.getSelectedFile() == null){
				popupText.append("No ha seleccionado el archivo .CAT urbano. Tiene que ser el archivo extraido con extensión .CAT\n\n");	
			}

			if (fcCatRu.getSelectedFile() == null){
				popupText.append("No ha seleccionado el archivo .CAT rústico. Tiene que ser el archivo extraido con extensión .CAT\n\n");	
			}

			if (fcGsb.getSelectedFile() == null){
				popupText.append("No ha seleccionado el archivo de rejilla para la reproyección (PENR2009.gsb o península.gsb). Este archvivo puede encontrarlo en la siguiente dirección: http://www.01.ign.es/ign/layoutIn/herramientas.do#DATUM\n\n");	
			}

			if (fdesde.getText().equals(null)){
				popupText.append("No ha especificado la fecha desde la cual tomar los datos (AAAAMMDD). Por defecto para tomar todos los datos indique 00000000\n\n");
			}

			if (fhasta.getText().equals(null)){
				popupText.append("No ha especificado la fecha hasta la cual tomar los datos (AAAAMMDD). Por defecto para tomar todos los datos indique 99999999\n\n");
			}

			if (!popupText.getText().isEmpty()){
				popupText.append("Para más ayuda o ver como debería ser el árbol de directorios consulte: http://wiki.openstreetmap.org/wiki/Catastro_España");
				popup.add(popupText, BorderLayout.CENTER);
				popup.setVisible(true);
				popup.show();

			}
			else {
				File dir = new File(""+fcResult.getSelectedFile());
				if (!dir.exists()) 
					dir.mkdirs();

				try {
					FileWriter fstream = new FileWriter(""+fcResult.getSelectedFile()+"/Config");
					BufferedWriter out = new BufferedWriter(fstream);

					exe.setText("ARCHIVO CREADO EN EL DIRECTORIO "+fcResult.getSelectedFile()+". AHORA EJECUTE CAT2OSM SEGÚN SE INDICA EN LA GUÍA DE USO.");

					out.write("\nResultPath="+fcResult.getSelectedFile());
					out.write("\nResultFileName="+resultFileName.getText());
					out.write("\nUrbanoSHPPath="+fcShpUr.getSelectedFile());
					out.write("\nRusticoSHPPath="+fcShpRu.getSelectedFile());
					out.write("\nUrbanoCATFile="+fcCatUr.getSelectedFile());
					out.write("\nRusticoCATFile="+fcCatRu.getSelectedFile());
					out.write("\nNadgridsPath="+fcGsb.getSelectedFile());
					out.write("\nProyeccion="+proj.getSelectedItem());
					out.write("\nFechaDesde="+fdesde.getText());
					out.write("\nFechaHasta="+fhasta.getText());
					out.write("\nTipoRegistro="+tipoReg.getSelectedItem());
					out.write("\nElemtexAConstru="+(texAConstru.getItemCount()-texAConstru.getSelectedIndex()-1));
					out.write("\nEjes="+ejes.getSelectedIndex());
					out.write("\nShapesId="+shapesId.getSelectedIndex());

					out.close();

				}
				catch (Exception e1){ e1.printStackTrace(); }

			}

		}  
		});  

		this.add(new JLabel("GUÍA DE USO EN: http://wiki.openstreetmap.org/wiki/Catastro_España"),BorderLayout.NORTH);
		this.setVisible(true);
	}


	/** Ejecutar cat2osm
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void ejecutarCat2Osm() throws IOException, InterruptedException{

		// Clases
		Cat2OsmUtils utils = new Cat2OsmUtils();
		Cat2Osm catastro = new Cat2Osm(utils);

		if (!new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempRelations.osm").exists()
				&& !new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempWays.osm").exists()
				&& !new File(Config.get("ResultPath") + "/" + Config.get("ResultFileName") + "tempNodes.osm").exists()){


			// Listas
			List<Shape> shapes = new ArrayList<Shape>();
			List<ShapeParser> parsers = new ArrayList<ShapeParser>();


			// Recorrer los directorios Urbanos
			File dirU = new File (Config.get("UrbanoSHPPath"));

			if( dirU.exists() && dirU.isDirectory()){
				File[] filesU = dirU.listFiles();
				for(int i=0; i < filesU.length; i++)
					if ( filesU[i].getName().toUpperCase().equals("CONSTRU") ||
							filesU[i].getName().toUpperCase().equals("ELEMLIN") ||
							filesU[i].getName().toUpperCase().equals("ELEMPUN") ||
							filesU[i].getName().toUpperCase().equals("ELEMTEX") ||
							//filesU[i].getName().toUpperCase().equals("MASA") ||
							filesU[i].getName().toUpperCase().equals("PARCELA") ||
							filesU[i].getName().toUpperCase().equals("SUBPARCE") || 
							(filesU[i].getName().toUpperCase().equals("EJES") && Config.get("Ejes").equals("1")) )
						try{

							System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesU[i].getName() +" Urbano.");
							parsers.add(new ShapeParser("UR", new File(filesU[i] + "/" + filesU[i].getName() + ".SHP"), utils, shapes));

						}
				catch(Exception e)
				{
					System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer alguno de los shapefiles urbanos. " + e.getMessage());}
			}

			else
				System.out.println("["+new Timestamp(new Date().getTime())+"] El directorio de shapefiles urbanos no es valido.");

			// Recorrer los directorios Rusticos
			File dirR = new File (Config.get("RusticoSHPPath"));

			if( dirR.exists() && dirR.isDirectory()){
				File[] filesR = dirR.listFiles();
				for(int i=0; i < filesR.length; i++)
					if ( filesR[i].getName().toUpperCase().equals("CONSTRU") ||
							//filesR[i].getName().toUpperCase().equals("EJES") ||
							filesR[i].getName().toUpperCase().equals("ELEMLIN") ||
							filesR[i].getName().toUpperCase().equals("ELEMPUN") ||
							filesR[i].getName().toUpperCase().equals("ELEMTEX") ||
							//filesR[i].getName().toUpperCase().equals("MASA") ||
							filesR[i].getName().toUpperCase().equals("PARCELA") ||
							filesR[i].getName().toUpperCase().equals("SUBPARCE") ||
							(filesR[i].getName().toUpperCase().equals("EJES") && Config.get("Ejes").equals("1")))
						try{
							System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo "+ filesR[i].getName() +" Rustico.");
							parsers.add(new ShapeParser("RU", new File(filesR[i] + "/" + filesR[i].getName() + ".SHP"), utils, shapes));
						}
				catch(Exception e)
				{
					System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer alguno de los archivos shapefiles rusticos. " + e.getMessage());}
			}
			else
				System.out.println("["+new Timestamp(new Date().getTime())+"] El directorio de shapefiles rusticos no es valido.");


			for (ShapeParser sp : parsers)
				sp.join();

			// Seleccionamos el archivo .cat
			// No todos los shapefiles tienen referencia catastral por lo que algunos
			// no hay forma de relacionarlos con los registros de catastro.
			try {

				System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat urbano.");
				catastro.catParser(new File(Config.get("UrbanoCATFile")), shapes);

			}catch(Exception e)
			{
				System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer archivo Cat urbano. " + e.getMessage());
			}	
			try {
				System.out.println("["+new Timestamp(new Date().getTime())+"] Leyendo archivo Cat rustico.");
				catastro.catParser(new File(Config.get("RusticoCATFile")), shapes);
			}catch(Exception e)
			{
				System.out.println("["+new Timestamp(new Date().getTime())+"] Fallo al leer archivo Cat rustico. " + e.getMessage());
			}	

			// Anadimos si es posible tags de los Elemtex a las parcelas.
			// Los Elemtex tienen informacion que puede determinar con mas exactitud detalles
			// de la parcela sobre la que se encuentran.
			if (Config.get("ElemtexAConstru").equals("1")){
				System.out.println("["+new Timestamp(new Date().getTime())+"] Traspasando posibles tags de Elemtex a Constru.");
				shapes = catastro.pasarElemtexLanduseAConstru(shapes);
			}

			// Simplificamos los ways
			System.out.println("["+new Timestamp(new Date().getTime())+"] Simplificando vias.");
			shapes = catastro.simplificarWays(shapes);

			// Escribir los datos
			System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo nodos");
			catastro.printNodes( Cat2Osm.utils.getTotalNodes());
			System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo ways");
			catastro.printWays(Cat2Osm.utils.getTotalWays());
			System.out.println("["+new Timestamp(new Date().getTime())+"] Escribiendo relations");
			catastro.printRelations( Cat2Osm.utils.getTotalRelations());
		}
		else 
			System.out.println("["+new Timestamp(new Date().getTime())+"] Se han encontrado 3 archivos temporales de una posible ejecución interrumpida, se procederá a juntarlos en un archivor resultado.");

		System.out.println("["+new Timestamp(new Date().getTime())+"] Juntando los tres archivos");
		catastro.juntarFiles(Config.get("ResultFileName"));
		System.out.println("["+new Timestamp(new Date().getTime())+"] Terminado");
	}

}
