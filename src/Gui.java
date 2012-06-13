import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

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
				"Ruta al ARCHIVO de la rejilla de la península (PENR2009.gsb o peninsula.gsb).\n(Necesario para reproyectar, se puede descargar en http://www.01.ign.es/ign/layoutIn/herramientas.do#DATUM)",
				"Proyección en la que se encuentran los archivos shapefile." +
						"\n32628 para WGS84/ Zona UTM 29N"+
						"\n23029 para ED_1950/ Zona UTM 29N,"+
						"\n23030 para ED_1950/ Zona UTM 30N,"+
						"\n23031 para ED_1950/ Zona UTM 31N,"+
						"\n25829 para ETRS_1989/ Zona UTM 29N,"+
						"\n25830 para ETRS_1989/ Zona UTM 30N,"+
						"\n25831 para ETRS_1989/ Zona UTM 31N"+
						"\n(Se puede comprobar abriendo con un editor de texto cualquiera de los archivos .PRJ)",
				"Si se quiere delimitar una fecha de alta de los datos (Formato AAAAMMDD).\nTomará los datos que se han dado de alta a partir de esta fecha.\nEjemeplo: 20060127 (27 de Enero del 2006)",
				"Si se quiere delimitar una fecha de baja de los datos (Formato AAAAMMDD).\nTomará los shapes que se han dado de alta hasta esta fecha y siguen sin darse de baja después.\nEjemeplo: 20060127 (27 de Enero del 2006)", 
				"Si se quiere delimitar una fecha de construcción desde la cual coger los datos (Formato AAAAMMDD).\nÚnicamente imprimirá las relations que cumplan estar entre las fechas de construcción.\nEjemeplo: 20060127 (27 de Enero del 2006)",
				"Si se quiere delimitar una fecha de construcción hasta la cual coger los datos (Formato AAAAMMDD).\nÚnicamente imprimirá las relations que cumplan estar entre las fechas de construcción.\nEjemeplo: 20060127 (27 de Enero del 2006)", 
				"Tipo de Registro de catastro a usar (0 = todos).\nLos registros de catastro tienen la mayoría de la información necesaria para los shapefiles.",
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

		final JComboBox<String> proj = new JComboBox<String>();

		final JTextField fdesde = new JTextField("00000000");
		final JTextField fhasta= new JTextField("99999999");
		final JTextField fconstrudesde = new JTextField("00000000");
		final JTextField fconstruhasta= new JTextField("99999999");

		final JComboBox<String> tipoReg = new JComboBox<String>();
		final JComboBox<String> shapesId = new JComboBox<String>();

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
				{ fcResult.showOpenDialog(new JFrame()); }  
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
				{ fcShpUr.showOpenDialog(new JFrame()); }  
				});  
				buttons.add(urbanoShpPath);
				break;
			}
			case 3:{
				rusticoShpPath = new JButton("Seleccionar carpeta shapefiles Rústicos XX_XXX_R_XXXX-XX-XX_SHF ");
				rusticoShpPath.addActionListener(new ActionListener()  
				{  public void actionPerformed(ActionEvent e)  
				{ fcShpRu.showOpenDialog(new JFrame()); }  
				});  
				buttons.add(rusticoShpPath);
				break;
			}
			case 4:{
				urbanoCatFile = new JButton("Seleccionar archivo .CAT Urbano XX_XX_U_XXXX-XX-XX.CAT");
				urbanoCatFile.addActionListener(new ActionListener()  
				{  public void actionPerformed(ActionEvent e)  
				{ fcCatUr.showOpenDialog(new JFrame()); }  
				});  
				buttons.add(urbanoCatFile);
				break;
			}
			case 5:{
				rusticoCatFile = new JButton("Seleccionar archivo .CAT Rústico XX_XX_R_XXXX-XX-XX.CAT");
				rusticoCatFile.addActionListener(new ActionListener()  
				{  public void actionPerformed(ActionEvent e)  
				{ fcCatRu.showOpenDialog(new JFrame()); }  
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
				{ fcGsb.showOpenDialog(new JFrame()); }  
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
				new JTextField("99999999");
				buttons.add(fhasta);
				break;
			}
			case 11:{
				buttons.add(fconstrudesde);
				break;
			}
			case 12:{
				new JTextField("99999999");
				buttons.add(fconstruhasta);
				break;
			}
			case 13:{
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
			
			if (fconstrudesde.getText().equals(null)){
				popupText.append("No ha especificado la fecha de construcción desde la cual tomar los datos (AAAAMMDD). Por defecto para tomar todos los datos indique 00000000\n\n");
			}

			if (fconstruhasta.getText().equals(null)){
				popupText.append("No ha especificado la fecha de construcción hasta la cual tomar los datos (AAAAMMDD). Por defecto para tomar todos los datos indique 99999999\n\n");
			}

			if (!popupText.getText().isEmpty()){
				popupText.append("Para más ayuda o ver como debería ser el árbol de directorios consulte: http://wiki.openstreetmap.org/wiki/Cat2Osm");
				popup.add(popupText, BorderLayout.CENTER);
				popup.setVisible(true);
				popup.show();

			}
			else {
				File dir = new File(""+fcResult.getSelectedFile());
				if (!dir.exists()) 
					dir.mkdirs();

				try {
					FileWriter fstream = new FileWriter(""+fcResult.getSelectedFile()+"/config");
					BufferedWriter out = new BufferedWriter(fstream);

					exe.setText("ARCHIVO CREADO EN EL DIRECTORIO "+fcResult.getSelectedFile()+". AHORA EJECUTE CAT2OSM SEGUN SE INDICA EN LA GUÍA DE USO.");

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
					out.write("\nFechaConstruDesde="+fconstrudesde.getText());
					out.write("\nFechaConstruHasta="+fconstruhasta.getText());
					out.write("\nTipoRegistro="+tipoReg.getSelectedItem());
					out.write("\nShapeIds="+shapesId.getSelectedIndex());

					out.close();

				}
				catch (Exception e1){ e1.printStackTrace(); }

			}

		}  
		});  

		this.add(new JLabel("GUÍA DE USO EN: http://wiki.openstreetmap.org/wiki/Cat2Osm"),BorderLayout.NORTH);
		this.setVisible(true);
	}

}
