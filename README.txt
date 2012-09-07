Autor: 
Ander Pijoan Lamas (ander.pijoan@deusto.es)


Forma de uso:
java -jar [-XmxMemoria] cat2osm.jar [Opción] / [RutaArchivoConfig] [NombreArchivo]
Es necesrio indicarle una opción y pasarle el archivo de configuración:
rutaarchivoconfig                 Ejecutar Cat2Osm con los parametros que se indiquen en el archivo de configuración del cual se pasa la ruta para llegar hasta él
-v                                Muestra la version de Cat2Osm
-ui                               Abrir la interfaz de usuario para crear el archivo de configuración
rutaarchivoconfig -constru        Generar un archivo con las geometrías CONSTRU
rutaarchivoconfig -ejes           Generar un archivo con las geometrías EJES
rutaarchivoconfig -elemlin        Generar un archivo con las geometrías ELEMLIN
rutaarchivoconfig -elempun        Generar un archivo con las geometrías ELEMPUN
rutaarchivoconfig -elemtex        Generar un archivo con las geometrías ELEMTEX y mostrando todos los textos de Parajes y Comarcas, Información urbana y rústica y Vegetación y Accidentes demográficos
rutaarchivoconfig -masa           Generar un archivo con las geometrías MASA
rutaarchivoconfig -parcela        Generar un archivo con las geometrías PARCELA
rutaarchivoconfig -subparce       Generar un archivo con las geometrías SUBPARCE
rutaarchivoconfig -usos           Generar un archivo con los usos de inmuebles que no se pueden asignar directamente a una construcción


Documentación y Forma de Uso:
http://wiki.openstreetmap.org/wiki/Cat2Osm
http://wiki.openstreetmap.org/wiki/Traduccion_metadatos_catastro_a_map_features