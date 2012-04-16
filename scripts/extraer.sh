#!/bin/bash
# versión 2012-04-03

# Parámetros
municipio=$1
rejilla=./peninsula.gsb
fecha_desde=0
fecha_hasta=99999999
fecha_constru_desde=0
fecha_constru_hasta=99999999
tipo_registro=0
print_shape_ids=0
if [ $# -ne 1 ]; then
  echo "Descomprime los ficheros descargados de Catastro existentes en cat2osm/files/MUNICIPIO."
  echo "Crea el archivo de configuración cat2osm/config/configMUNICIPIO."
  echo "Uso: `basename $0` MUNICIPIO"
  exit 1
fi
if [ -L "$0" ]; then
  cd "$( dirname "$( readlink "$0" )" )"
else
  cd "$( dirname "$0" )"
fi
cd ..
if [ ! -d "files/$municipio" ]; then
  echo "No existe files/$municipio"
  exit 2
fi

# Obtiene nombres ficheros
catrus=$( basename "files/$municipio/"*_R_*.CAT.gz )
catrus=${catrus%\.gz}
if [ "$catrus" == "*_R_*.CAT" ]; then
  echo "Aviso: No existe el fichero CAT de Rústica."
  catrus=""
fi
caturb=$( basename "files/$municipio/"*_U_*.CAT.gz )
caturb=${caturb%\.gz}
if [ "$caturb" == "*_U_*.CAT" ]; then
  echo "Aviso: No existe el fichero CAT de Urbana."
  caturb=""
fi
shfrus=$( basename "files/$municipio/"*_RA_*_SHF.zip )
shfrus=${shfrus%\.zip}
if [ "$shfrus" == "*_UA_*_SHF" ]; then
  echo "Aviso: No existe el fichero Shapefile de Urbana."
  shfrus=""
fi
shfurb=$( basename "files/$municipio/"*_UA_*_SHF.zip )
shfurb=${shfurb%\.zip}
if [ "$shfurb" == "*_RA_*_SHF" ]; then
  echo "Aviso: No existe el fichero Shapefile de Rústica."
  shfurb=""
fi

# Extrae
7z x -o"files/$municipio/" "files/$municipio/$catrus.gz"
7z x -o"files/$municipio/" "files/$municipio/$caturb.gz"
mkdir "files/$municipio/$shfrus"
mkdir "files/$municipio/$shfurb"
unzip "files/$municipio/$shfrus.zip" -d "files/$municipio/$shfrus/"
unzip "files/$municipio/$shfurb.zip" -d "files/$municipio/$shfurb/"
for f in "files/$municipio/$shfrus/"*.zip
do
  mkdir "${f%\.zip}"
  unzip "$f" -d "${f%\.zip}"
  rm "$f"
done
for f in "files/$municipio/$shfurb/"*.zip
do
  mkdir "${f%\.zip}"
  unzip "$f" -d "${f%\.zip}"
  rm "$f"
done

# Obtiene proyección
proyeccion=$( cat $( find "files/$municipio" -iname *.PRJ | head -1 ) | sed 's/[^"]*"// ; s/".*//' )
if [ "$proyeccion" == "ETRS_1989_UTM_Zone_28N" ]; then
  epsg=25828
elif [ "$proyeccion" == "ETRS_1989_UTM_Zone_29N" ]; then
  epsg=25829
elif [ "$proyeccion" == "ETRS_1989_UTM_Zone_30N" ]; then
  epsg=25830
elif [ "$proyeccion" == "ETRS_1989_UTM_Zone_31N" ]; then
  epsg=25831
elif [ "$proyeccion" == "ED50 / UTM zone 28N" ]; then
  epsg=23028
elif [ "$proyeccion" == "ED50 / UTM zone 29N" ]; then
  epsg=23029
elif [ "$proyeccion" == "ED50 / UTM zone 30N" ]; then
  epsg=23030
elif [ "$proyeccion" == "ED50 / UTM zone 31N" ]; then
  epsg=23031
elif [ "$proyeccion" == "WGS_1984_UTM_Zone_27N" ]; then
  epsg=32627
elif [ "$proyeccion" == "WGS_1984_UTM_Zone_28N" ]; then
  epsg=32628
elif [ "$proyeccion" == "WGS_1984_UTM_Zone_29N" ]; then
  epsg=32629
elif [ "$proyeccion" == "WGS_1984_UTM_Zone_30N" ]; then
  epsg=32630
elif [ "$proyeccion" == "WGS_1984_UTM_Zone_31N" ]; then
  epsg=32631
else
  echo "No se ha podido determinar la proyección"
  exit -3
fi

# Escribe configuración
cat >>"config/config$municipio" <<EOF
ResultPath=result
ResultFileName=$municipio
UrbanoSHPPath=files/$municipio/$shfurb
RusticoSHPPath=files/$municipio/$shfrus
UrbanoCATFile=files/$municipio/$caturb
RusticoCATFile=files/$municipio/$catrus
FWToolsPath=C:\Program Files (x86)\FWTools2.4.7
NadgridsPath=$rejilla
Proyeccion=$epsg
FechaDesde=$fecha_desde
FechaHasta=$fecha_hasta
FechaConstruDesde=$fecha_constru_desde
FechaConstruHasta=$fecha_constru_hasta
TipoRegistro=$tipo_registro
PrintShapeIds=$print_shape_ids
EOF

