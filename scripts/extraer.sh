#!/bin/bash
# Descomprime archivos catastro y crea archivo de configuración

# Parámetros
proyeccion=32628
municipio=$1
if [ $# -ne 1 ]
then
  echo "Uso: `basename $0` provincia"
  exit 1
fi
if [ ! -d "files/$municipio" ]
then
  echo "No existe files/$municipio"
  exit 2
fi

# Obtiene nombres ficheros
catrus=`basename files/$municipio/*_R_*.CAT.gz`
catrus=${catrus%\.gz}
caturb=`basename files/$municipio/*_U_*.CAT.gz`
caturb=${caturb%\.gz}
shfrus=`basename files/$municipio/*_UA_*_SHF.zip`
shfrus=${shfrus%\.zip}
shfurb=`basename files/$municipio/*_RA_*_SHF.zip`
shfurb=${shfurb%\.zip}

# Escribe configuración
echo "ResultPath=result" > config/config$municipio
echo "ResultFileName=$municipio" >> config/config$municipio
echo "UrbanoSHPPath=files/$municipio/$shfurb" >> config/config$municipio
echo "RusticoSHPPath=files/$municipio/$shfrus" >> config/config$municipio
echo "UrbanoCATFile=files/$municipio/$caturb" >> config/config$municipio
echo "RusticoCATFile=files/$municipio/$catrus" >> config/config$municipio
echo "FWToolsPath=C:\Program Files (x86)\FWTools2.4.7" >> config/config$municipio
echo "NadgridsPath=./peninsula.gsb" >> config/config$municipio
echo "Proyeccion=$epsg" >> config/config$municipio
echo "FechaDesde=0" >> config/config$municipio
echo "FechaHasta=99999999" >> config/config$municipio
echo "FechaConstruDesde=0" >> config/config$municipio
echo "FechaConstruHasta=99999999" >> config/config$municipio
echo "TipoRegistro=0" >> config/config$municipio
echo "PrintShapeIds=0" >> config/config$municipio

# Extrae
7z x -ofiles/$municipio/ files/$municipio/$catrus.gz 
7z x -ofiles/$municipio/ files/$municipio/$caturb.gz 
mkdir files/$municipio/$shfrus
mkdir files/$municipio/$shfurb
unzip files/$municipio/$shfrus.zip -d files/$municipio/$shfrus/
unzip files/$municipio/$shfurb.zip -d files/$municipio/$shfurb/
for f in files/$municipio/$shfrus/*.zip
do
  mkdir ${f%\.zip}
  unzip $f -d ${f%\.zip}
  rm $f
done
for f in files/$municipio/$shfurb/*.zip
do
  mkdir ${f%\.zip}
  unzip $f -d ${f%\.zip}
  rm $f
done
