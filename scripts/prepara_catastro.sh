#!/bin/bash
###################################################
#                                                 #
# Programa para preparar datos del Catastro       #
#     (para ser procesados por cat2osm)           #
#                                                 #
###################################################

# Necesitamos Bash 4 para crear arrays asociativos (diccionarios)
if [ -z "${BASH_VERSINFO}" ] || [ "${BASH_VERSINFO[0]}" -lt 4 ]
then
  echo "Se necesita Bash 4 o superior." 1>&2
  exit 1
fi

declare -r version="0.90"   # Versión del programa (se escribirá en los archivos config)

# Programas externos
declare -r wget="wget -q"   # '-q' es para no mostrar info de descarga (progreso, etc). De todas formas es muy rápido y no da tiempo a leer.
declare -r sort="sort"
declare -r mkdir="mkdir"
declare -r find="find"
declare -r rm="rm"
declare -r iconv="iconv"
declare -r ls="ls"
declare -r gunzip="gunzip -c"  # también funcionaría 'zcat' que es sinónimo de 'gunzip -c'
declare -r unzip="unzip -q -o" # '-o' es para sobreescribir archivos exisistentes, '-q' para ocultar info de descompresión ('inflating X' etc)

# Comprobamos si podemos encontrar esos programas
for i in "wget" "sort" "mkdir" "find" "rm" "iconv" "ls" "gunzip" "unzip" "ogr2ogr"
do
  set $i
  command -v "$1" >/dev/null 2>&1 || { echo >&2 "Se requiere el programa externo '$1', pero no se encontró."; exit 1; }
done

# Programas internos
base_name () { echo "${1##*/}"; }
dir_name() { echo `expr "x$1" : 'x\(.*\)/[^/]*' \| '.' : '\(.\)' `; }

declare -r basename="base_name" # Es mejor usar funciones internas que ejecutables
declare -r dirname="dir_name"   # 
declare -r pwd="pwd"            # pwd es interno
declare -r esteprograma="`$basename $0`"

############
# Directorios por defecto (lo que se obtiene pulsando ENTER)
declare -r dirbase=`$pwd`            ## directorio con los 'CAT.gz' y los 'SHP.zip' (se realizará una búsqueda recursiva aquí)
declare -r dirsalida="`$pwd`/files"  ## directorio que contendrá las carpetas descomprimidas y preparadas
declare -r dirconfig="`$pwd`/config" ## directorio que contendrá los archivo config
declare -r dirresult="`$pwd`/result" ## directorio donde cat2osm guardará los archivos OSM resultantes
declare -r fwtoolspath=$($dirname `command -v ogr2ogr || { echo "/usr/bin/ogr2ogr"; }` )  ## ruta por defecto a fwtools/ogr2ogr (no usado)
declare -r rejpen1="`$pwd`/peninsula.gsb" ## rejilla de la península por defecto
declare -r rejpen2="`$pwd`/PENR2009.gsb"  ## rejilla de la península por defecto (alternativa)
declare -r rejbal1="`$pwd`/baleares.gsb"  ## rejilla de Baleares por defecto
declare -r rejbal2="`$pwd`/BALR2009.gsb"  ## rejilla de Baleares por defecto (alternativa)
declare -r fechadesde="0"            ## fecha de inicio por defecto
declare -r fechahasta="99999999"     ## fecha de fin por defecto
declare -r fechaconstrudesde="0"     ## fecha de inicio de construcción de edificios por defecto
declare -r fechaconstruhasta="99999999" ## fecha de fin de construcción de edificios por defecto
declare -r tiporegistro="0"          ## tipo de registro por defecto (todos)
declare -r moverportales="1"          ## mover portales
declare -r printshapeids="0"         ## no imprimir shapeids por defecto (solo vale para depuracion)
declare -r prefijoconfig="config"    ## prefijo por defecto de los archivos config
declare -r sufijoconfig=".config"    ## sufijo de los archivos config
declare -r sufijoconfigurbano="-U.config"  ## sufijo de los archivos config (urbanos)
declare -r sufijoconfigrustico="-R.config" ## sufijo de los archivos config (rústicos)
############
declare dirb
declare dirs

######################################################################################################
# Las siguientes estructuras se rellenan al principio del programa y pueden ser muy útiles para futuras funciones.
######################################################################################################
declare -a provincias        # provincias[codigo_INE]=nombre
declare -a provincias_MHAP   # provincias_MHAP[codigo_MHAP]=nombre
declare -a codprovincias     # un array con los códigos de todas las provincias (aunque no es necesario)
declare -A municipios        # municipios[codprovincia_codmunicipio]=nombre, teniendo codmunicipio minimo 2 caracteres
# Ejemplos:
# echo ${provincias[15]}      # Imprime 'A CORUÑA'
# echo ${provincias[51]}      # Imprime 'CEUTA'
# echo ${provincias_MHAP[55]} # Imprime 'CEUTA' (el código según el Ministerio de Hacienda es distinto al que le asigna el INE)
# for i in ${codprovincias[@]}; do echo ${provincias_MHAP[$i]}; done    # Imprime los nombres de todas las provincias
# for i in ${!provincias_MHAP[@]}; do echo ${provincias_MHAP[$i]}; done # Imprime los nombres de todas las provincias
# for i in ${provincias_MHAP[@]}; do echo $i; done                      # Imprime los nombres de todas las provincias
# echo ${municipios[05_17]}   # Imprime 'AVEINTE'
# echo ${municipios[55_101]}  # Imprime 'CEUTA'

######################################################################################################
# Algunas funciones
######################################################################################################
trim() { echo $1; }
urlencode() {
  local LANG=C
  local arg
  arg="$1"
  while [[ "$arg" =~ ^([0-9a-zA-Z/:_\.\-]*)([^0-9a-zA-Z/:_\.\-])(.*) ]] ; do
    echo -n "${BASH_REMATCH[1]}"
    printf "%%%X" "'${BASH_REMATCH[2]}'"
    arg="${BASH_REMATCH[3]}"
  done
  # the remaining part
  echo -n "$arg"
}
uri() {
  a=`echo $1 | $iconv -f UTF-8 -t ISO-8859-1`
  urlencode "$a"
}
formatea() {
    s=$1
    local IFS='/'
    set $s
    echo $1
}
read_dom () {
    local IFS=\>
    read -d \< ENTITY CONTENT
}
real_path () {
  OIFS=$IFS
  IFS='/'
  for I in $1
  do
    # Resolve relative path punctuation.
    if [ "$I" = "." ] || [ -z "$I" ]
      then continue
    elif [ "$I" = ".." ]
      then FOO="${FOO%%/${FOO##*/}}"
           continue
      else FOO="${FOO}/${I}"
    fi

    # Dereference symbolic links.
    if [ -h "$FOO" ] && [ -x "/bin/ls" ]
      then IFS=$OIFS
           set `/bin/ls -l "$FOO"`
           while shift ;
           do
             if [ "$1" = "->" ]
               then FOO=$2
                    shift $#
                    break
             fi
           done
    fi
  done
  IFS=$OIFS
  echo "$FOO"
}
get_abs_path() {
     local PARENT_DIR=$($dirname "$1")
     cd "$PARENT_DIR" 2>/dev/null
     local ABS_PATH="$($pwd -P)"/"$($basename "$1")"
     cd - >/dev/null 2>&1
     echo $ABS_PATH
} 
unzip_en_dir() {
  while read cadena
  do
    local dir=`$dirname "$cadena"`
    local archivo=`$basename "$cadena"`
    local nombre=${archivo%.*}
    #local extension=${archivo##*.} 
    $mkdir -p "$dir/$nombre/" || { echo "Error creando directorio '$dir/$nombre/'." 1>&2; continue; }
    $unzip -d "$dir/$nombre/" "$cadena" || { echo "Error descomprimiendo '$cadena' en '$dir/$nombre/' con '$unzip'." 1>&2; continue; }
    $rm "$cadena" || { echo "Error borrando '$cadena'." 1>&2; }
  done
}
procesa_archivo() {
  while read cadena
  do
    local archivo=`$basename "$cadena"`
    local nombre=${archivo%.*}
    local extension=${archivo##*.}
    if [[ $extension == "gz" ]]; then
      local IFS='_'
      set $nombre
      IFS=' '
      local clave="${1}_${2}"
      local prov=`formatea "${provincias_MHAP[$1]}"`
      prov=`trim "$prov"`
      local mun=`formatea "${municipios[$clave]}"`
      mun=`trim "$mun"`
      echo -e "\n$archivo ====> ${municipios[$clave]} (${provincias_MHAP[$1]})"
      echo "> $dirs/$prov/$mun/$nombre"
      $mkdir -p "$dirs/$prov/$mun/" || { echo "Error creando directorio '$dirs/$prov/$mun/'." 1>&2; continue; }
      $gunzip "$cadena" > "$dirs/$prov/$mun/$nombre" || { echo "Error descomprimiendo '$cadena' en '$dirs/$prov/$mun/$nombre' con '$gunzip'." 1>&2; }
    elif [[ $extension == "zip" ]]; then
      local IFS='_'
      set $nombre
      IFS=' '
      local clave="${1}_${2}"   
      local prov=`formatea "${provincias_MHAP[$1]}"`
      prov=`trim "$prov"`
      local mun=`formatea "${municipios[$clave]}"`
      mun=`trim "$mun"`   
      echo -e "\n$archivo ====> ${municipios[$clave]} (${provincias_MHAP[$1]})"
      echo "> $dirs/$prov/$mun/$nombre/"
      $mkdir -p "$dirs/$prov/$mun/$nombre/" || { echo "Error creando directorio '$dirs/$prov/$mun/$nombre/'." 1>&2; continue; }
      $unzip -d "$dirs/$prov/$mun/$nombre/" "$cadena" || { echo "Error descomprimiendo '$cadena' en '$dirs/$prov/$mun/$nombre' con '$unzip'." 1>&2; continue; }
      $find "$dirs/$prov/$mun/$nombre/" -type f -iname *.zip -print | unzip_en_dir
    fi
  done
}
muestra_en_columnas() {
  let columnas=4
  let ancho=30
  let i=1
  while read cadena    
  do
    cad=${cadena:0:$ancho}
    # Desafortunadamente printf y otras funciones cuentan bytes, no caracteres
    # así que cadenas como "A CORUÑA" las considera de tamaño 9, no 8, debido a la Ñ
    printf "%-${ancho}s" "$cad"
    let "resto = $i % $columnas"
    if [[ $resto -eq 0 ]]; then
      echo -e "\n"
    fi
    i=$((i+1))
  done
}
muestra_provincias() {
  for k in "${!provincias_MHAP[@]}"
  do
    echo -e "(\033[1m$k\033[0m) ${provincias_MHAP[$k]}"
  done | $sort -k 2 | muestra_en_columnas
}

######################################################################################################
######################################################################################################
######################################################################################################
# Programa principal
######################################################################################################
######################################################################################################
######################################################################################################

#
# Necesitamos conocer los códigos y nombres oficiales de todas las provincias y municipios
# para hacer las distintas tareas: generar CSV, descomprimir clasificando por nombre y generar configs.
#
# Ver la declaración de las variables 'provincias', 'provincias_MHAP', 'codprovincias' y 'municipios'
# para ver el resultado de esto.
#
#
# NOTA: Los archivos txt generados no se utilizan en el programa. Puede borrarlos.
#       Pueden ser útiles para importar en una hoja de cálculo, etc.
#

# Lista de provincias
if [ ! -f "provincias.xml" ]; then
  $wget -O "provincias.xml" "http://ovc.catastro.meh.es/ovcservweb/OVCSWLocalizacionRC/OVCCallejero.asmx/ConsultaProvincia?"
  if [[ $? -gt 0 || ! -f "provincias.xml" ]]; then
    echo "ERROR: Error en la descarga del fichero 'provincias.xml' desde la web del Catastro." 1>&2
    exit 1
  fi
fi

let nprovincias=-1
let np=0
unset error
while read_dom; do
    if [[ $ENTITY = "cpine" ]]; then
        let codpro="10#$CONTENT"
        #codprovincias[$[${#codprovincias[@]}+1]]=$codpro
        echo -n $codpro
        np=$((np + 1))
    elif [[ $ENTITY = "np" ]]; then
        nompro=$CONTENT
        echo " $nompro"
        provincias[$codpro]="$nompro"
    elif [[ $ENTITY = "cuprov" ]]; then
        nprovincias=$CONTENT
    # Si se produce algún error desde el Catastro (parada por mantenimiento, etc) tenemos la explicación aquí
    elif [[ $ENTITY = "des" ]]; then
        error=$CONTENT
    fi
done < provincias.xml > provincias-INE.txt

if [[ $nprovincias -ne $np ]]; then
  echo "Error leyendo provincias." 1>&2
  if [[ ! -z "$error" ]]; then
    echo "Mensaje del Catastro: $error"  1>&2
  fi
  $rm ./provincias.xml || { echo -e "Error borrando \"provincias.xml\". Bórrelo manualmente."  1>&2 ; }
  echo "Vuelva a ejecutar este programa para reintentar."  1>&2
  exit 1
else
  echo "Leídas $np provincias"
fi


# Lista de municipios por cada provincia
let nmuntotal=0     # número de municipios que reportan los ficheros
let nummun=0        # número de municipios que hemos contado hasta ahora
for k in "${!provincias[@]}"
do
    #echo "$k ${provincias[$k]}"
    echo -n "Leyendo municipios de ${provincias[$k]}: "
    if [ ! -f "${provincias[$k]}.xml" ]; then
      prov=`uri "${provincias[$k]}"` # Tenemos que convertir caracteres extraños como la Ñ (A CORUÑA)      
      $wget -O "${provincias[$k]}.xml" "http://ovc.catastro.meh.es/ovcservweb/OVCSWLocalizacionRC/OVCCallejero.asmx/ConsultaMunicipio?Provincia=$prov&municipio="
      if [[ $? -gt 0 || ! -f "${provincias[$k]}.xml" ]]; then
        echo "AVISO: Error en la descarga del fichero '${provincias[$k]}.xml' desde la web del Catastro." 1>&2
        continue
      fi
    fi

    let i=0
    let nmunicipios=-1
    unset error
    while read_dom; do
      # Nombre del municipio
      if [[ $ENTITY = "nm" ]]; then
          nm=$CONTENT
      # Código del distrito (provincia) según Hacienda
      elif [[ $ENTITY = "cd" ]]; then
          let cd="10#$CONTENT"
      # Código del municipio según Hacienda
      elif [[ $ENTITY = "cmc" ]]; then
          let cmc="10#$CONTENT"
      # Código de la provincia según INE
      elif [[ $ENTITY = "cp" ]]; then
          let cp="10#$CONTENT"
      # Código del municipio según INE
      elif [[ $ENTITY = "cm" ]]; then
          let cm="10#$CONTENT"
      # Número total de municipios en esta provincia
      elif [[ $ENTITY = "cumun" ]]; then
          let nmunicipios="10#$CONTENT"
          let nmuntotal=$nmuntotal+$nmunicipios
      #### FIN de municipio
      elif [[ $ENTITY = "/muni" ]]; then
          declare -a mun$cd
          let mun$cd[$i]=$cmc

          provincias_MHAP[$cd]=${provincias[$cp]}

          if [[ $cd -lt 10 ]]; then
            codpro="0$cd" #solo un cero delante
          else
            codpro="$cd"
          fi
          if [[ $cmc -lt 10 ]]; then
            codmun="0$cmc" #solo un cero delante
          else
            codmun="$cmc"
          fi
          municipios[${codpro}_${codmun}]="$nm"

          echo "$cmc $nm"
          nummun=$((nummun + 1))
          i=$((i + 1))
      # Si se produce algún error desde el Catastro (parada por mantenimiento, etc) tenemos la explicación aquí
      elif [[ $ENTITY = "des" ]]; then
          error=$CONTENT
      fi
    done < "${provincias[$k]}.xml" > "${provincias[$k]}-municipios.txt"
    codprovincias[$[${#codprovincias[@]}+1]]=$cd
    echo -n "$i municipios"
    if [[ $nmunicipios -ne $i ]]; then
      if [[ $nmunicipios -eq -1 ]]; then
        echo -n "............Error al leer archivo (¿error en la descarga?)." 1>&2
      else
        echo -n "............Error (deberían ser $nmunicipios)." 1>&2
      fi
      if [[ ! -z "$error" ]]; then
        echo "Mensaje del Catastro: $error"  1>&2
      fi
      $rm "${provincias[$k]}.xml" || { echo -e "Error borrando \"${provincias[$k]}.xml\". Bórrelo manualmente."  1>&2 ; }
    fi
    echo ""
    
done  #| $sort -n -k1

if [[ $nmuntotal -ne $nummun ]]; then
  echo "El número de municipios leídos no coincide." 1>&2
  echo "Vuelva a ejecutar este programa para reintentar."  1>&2
  exit 1
else
  echo "Leídos $nummun municipios."
  unset provincias
fi

#for k in "${!municipios[@]}"
#do
#  echo "$k ${municipios[$k]}"
#done







######################################################################################################
# Fase 2: generamos el menú
######################################################################################################


opciones_validas=( "Generar archivo CSV para descargar datos" "Descomprimir y clasificar archivos descargados" "Generar archivos config" "Salir")
while true
do
  echo -e "\nSelecciona la operación que deseas realizar:"
  j=1
  for i in "${opciones_validas[@]}"
  do
    echo "$j) $i"
    let j++
  done
  read -e -p '> ' opcion

  case "$opcion" in
    1)
      ############################################
      # Generación de fichero CSV para descarga masiva
      ############################################
      muestra_provincias
      read -e -p "Introduzca los códigos de las provincias en las que está interesado, separados por espacios (0 para todas): " -a provs
      pr=provs[@]
      if [[ ${provs[0]} -eq 0 ]]; then
        pr=codprovincias[@]
      fi
      echo -n "Generando CSV para "
      for codpro in "${!pr}"
      do
        echo -n "${provincias_MHAP[$codpro]}, "
      done
      echo -n " ... "

      for codpro in "${!pr}"
      do
        let codpro="10#$codpro" #quitar ceros al principio
        muns=mun$codpro[@]
        for codmun in "${!muns}"
        do
          if [[ $codmun -lt 10 ]]; then
            cm="0$codmun"
          else
            cm="$codmun"
          fi
          if [[ $codpro -lt 10 ]]; then
            cp="0$codpro"
          else
            cp="$codpro"
          fi
          clave="${cp}_${cm}"
          nommun=${municipios[$clave]}
          #echo "$clave  ==>  ${provincias_MHAP[$cp]}  $nommun" 1>&2
          echo "$codpro,$nommun"
        done
      done > catastro.csv
      echo "Hecho. El resultado está en \"catastro.csv\""
      ;;
    2)
      ############################################
      # Descompresión de archivos
      ############################################
      let dir_valido=0
      while [[ $dir_valido -eq 0 ]]
      do
        read -e -p "Directorio donde se van a buscar (recursivamente) los archivos *.CAT.gz y *SHF.zip (ENTER para '$dirbase'): " dirb
        dirb=${dirb:-$dirbase}
        dirb=$(printf "%q" "$dirb")
        eval dirb="$dirb" # para que expanda caracteres especiales como la tilde: ~
        dirb=$(get_abs_path "$dirb")

        if [[ -d "$dirb" ]]; then
          let dir_valido=1
        else
          echo "El directorio no existe. Prueba otra vez." 1>&2
        fi
      done
      echo -e "> Usando \"$dirb\" como directorio de búsqueda.\n"

      let dir_valido=0
      while [[ $dir_valido -eq 0 ]]
      do
        read -e -p "Directorio donde se van a descomprimir los archivos (ENTER para '$dirsalida'): " dirs
        dirs=${dirs:-$dirsalida}
        dirs=$(printf "%q" "$dirs")
        eval dirs="$dirs" # para que expanda caracteres especiales como la tilde: ~
        dirs=$(get_abs_path "$dirs")

        if [[ -d "$dirs" ]]; then
          let dir_valido=1
        else
          echo -e -n "El directorio no existe. ¿Crearlo (\033[4m\033[1ms\033[0m\033[0m/n)? "
          unset proceder
          read -e proceder
          proceder=${proceder:-"s"}
          if [[ "$proceder" == "s" ]]; then
            echo "OK. Creamos el directorio."
            let dir_valido=1
            $mkdir -p "$dirs" || { echo "Error creando directorio '$dirs'." 1>&2; let dir_valido=0; }
          else
            let dir_valido=0
          fi
        fi
      done
      echo -e "> Usando \"$dirs\" como directorio de salida.\n"
      echo "Descomprimiendo archivos..."
      $find "$dirb" -type f -iname *.CAT.gz -print | procesa_archivo
      $find "$dirb" -type f -iname *_SHF.zip -print | procesa_archivo
      ;;
    3)
      ############################################
      # Generación de archivos config
      ############################################
      read -e -p "Prefijo para los archivos de configuración (ENTER para '$prefijoconfig'): " prefijo
      prefijo=${prefijo:-$prefijoconfig}
      echo -e "Se usará '$prefijo' como prefijo para los archivos de configuración (ej: '${prefijo}NOMBREPUEBLO.config').\n"

      let dir_valido=0
      while [[ $dir_valido -eq 0 ]]; do
        read -e -p "Directorio donde se van a buscar (recursivamente) los archivos CAT y SHF (ENTER para '${dirs:=$dirsalida}'): " dirbusqueda
        dirbusqueda=${dirbusqueda:-$dirs}          # Si hemos pulsado ENTER (cadena vacía), usamos el parámetro por defecto
        dirbusqueda=$(printf "%q" "$dirbusqueda")  # 'eval' necesita que se escapen los espacios: '~/a b c' => '~/a\ b\ c'
        eval dirbusqueda="$dirbusqueda"            # Usamos 'eval' para expandir carácteres especiales como '~'
        dirbusqueda=$(get_abs_path "$dirbusqueda") # Convertimos rutas relativas a rutas absolutas

        if [[ -d "$dirbusqueda" ]]; then
          let dir_valido=1
        else
          echo "El directorio '$dirbusqueda' no existe. Prueba otra vez." 1>&2
        fi
      done
      echo -e "Usando \"$dirbusqueda\" como directorio de búsqueda.\n"

      let dir_valido=0
      while [[ $dir_valido -eq 0 ]]; do
        read -e -p "Directorio donde se van a escribir los archivos de configuración (ENTER para '$dirconfig'): " dirc
        dirc=${dirc:-$dirconfig}
        dirc=$(printf "%q" "$dirc")
        eval dirc="$dirc"
        dirc=$(get_abs_path "$dirc")

        if [[ -d "$dirc" ]]; then
          let dir_valido=1
        else
            echo -e -n "El directorio '$dirc' no existe. ¿Crearlo (\033[4m\033[1ms\033[0m\033[0m/n)? "
            unset proceder
            read -e proceder
            proceder=${proceder:-"s"}
            if [[ "$proceder" == "s" ]]; then
              echo "OK. Creamos el directorio."
              $mkdir -p "$dirc"
              if [[ $? -eq 0 && -d "$dirc" ]]; then
                let dir_valido=1
              else
                echo "Error al crear el directorio '$dirc'." 1>&2
              fi
            else
              let dir_valido=0
            fi
        fi
      done
      echo -e "Usando \"$dirc\" como directorio de salida para los archivos de configuración.\n"

      let dir_valido=0
      while [[ $dir_valido -eq 0 ]]; do
        read -e -p "Directorio donde cat2osm guardará los resultados (ENTER para '$dirresult'): " dirr
        dirr=${dirr:-$dirresult}
        dirr=$(printf "%q" "$dirr")
        eval dirr="$dirr"
        dirr=$(get_abs_path "$dirr")

        if [[ -d "$dirr" ]]; then
          let dir_valido=1
        else
            echo -e -n "El directorio '$dirr' no existe. ¿Crearlo (\033[4m\033[1ms\033[0m\033[0m/n)? "
            unset proceder
            read -e proceder
            proceder=${proceder:-"s"}
            if [[ "$proceder" == "s" ]]; then
              echo "OK. Creamos el directorio."
              $mkdir -p "$dirr"
              if [[ $? -eq 0 && -d "$dirr" ]]; then
                let dir_valido=1
              else
                echo "Error al crear el directorio '$dirr'." 1>&2
              fi
            else
              let dir_valido=0
            fi
        fi
      done
      echo -e "Usando \"$dirr\" como directorio de salida para archivos '.osm'\n"


      # Rejilla de la península
      let fich_valido=0
      while [[ $fich_valido -eq 0 ]]; do
        read -e -p "Fichero de la rejilla de la península (ENTER para '$rejpen1'): " rejilla1
        rejilla1=${rejilla1:-$rejpen1}
        rejilla1=$(printf "%q" "$rejilla1")
        eval rejilla1="$rejilla1"
        rejilla1=$(get_abs_path "$rejilla1")

        if [[ -f "$rejilla1" ]]; then
          echo "OK. Usando '$rejilla1' como rejilla de la península."
          let fich_valido=1
        else
          rej="$rejpen2"
          rej=$(printf "%q" "$rej")
          eval rej="$rej"
          rej=$(get_abs_path "$rej")

          if [[ -f "$rej" ]]; then
            echo -e -n "No se encontró '$rejilla1', pero se encontró '$rej'. ¿Usarlo como rejilla de la península (\033[4m\033[1ms\033[0m\033[0m/n)? "
            unset proceder
            read -e proceder
            proceder=${proceder:-"s"}
            if [[ "$proceder" == "s" ]]; then
              echo -n "OK. "
              rejilla1=$rej
              let fich_valido=1
            else
              let fich_valido=0
            fi
          else
            echo -e -n "No se encontró ni '$rejilla1' ni '$rej'. ¿Descargarlo (\033[4m\033[1ms\033[0m\033[0m/n)? "
            unset proceder
            read -e proceder
            proceder=${proceder:-"s"}
            if [[ "$proceder" == "s" ]]; then
                $wget -O "./PENR2009.zip" "http://www.ign.es/ign/resources/herramientas/PENR2009.zip"
                if [[ $? -eq 0 && -f "./PENR2009.zip" ]]; then
                  $unzip "./PENR2009.zip"
                  if [[ $? -eq 0 && -f "./PENR2009.gsb" ]]; then
                    $rm "./PENR2009.zip"
                    rejilla1="`$pwd`/PENR2009.gsb"
                    rejilla1=$(printf "%q" "$rejilla1")
                    eval rejilla1="$rejilla1"
                    let fich_valido=1
                  else
                    echo "Error en la descompresión con '$unzip' (PENR2009.gsb)." 1>&2
                    let fich_valido=0
                  fi
                else
                  echo "Error en la descarga con '$wget' (PENR2009.zip)." 1>&2
                  let fich_valido=0
                fi
            else
              echo "OK. No se intentará la descarga. Prueba otra vez con el nombre del fichero..."
              let fich_valido=0              
            fi
          fi
        fi
      done
      echo -e "Se usará '$rejilla1' como rejilla de proyección de la península.\n"

      # Rejilla de las Baleares (se usará automáticamente al generar el config de las Baleares)
      let fich_valido=0
      while [[ $fich_valido -eq 0 ]]; do
        read -e -p "Fichero de la rejilla de las Islas Baleares (ENTER para '$rejbal1'): " rejilla2
        rejilla2=${rejilla2:-$rejbal1}
        rejilla2=$(printf "%q" "$rejilla2")
        eval rejilla2="$rejilla2"
        rejilla2=$(get_abs_path "$rejilla2")

        if [[ -f "$rejilla2" ]]; then
          echo -n "OK. "
          let fich_valido=1
        else
          rej="$rejbal2"
          rej=$(printf "%q" "$rej")
          eval rej="$rej"
          rej=$(get_abs_path "$rej")

          if [[ -f "$rej" ]]; then
            echo -e -n "No se encontró '$rejilla2', pero se encontró '$rej'. ¿Usarlo como rejilla de las Baleares (\033[4m\033[1ms\033[0m\033[0m/n)? "
            unset proceder
            read -e proceder
            proceder=${proceder:-"s"}
            if [[ "$proceder" == "s" ]]; then
              echo -n "OK."
              rejilla2=$rej
              let fich_valido=1
            else
              let fich_valido=0
            fi
          else
            echo -e -n "No se encontró ni '$rejilla2' ni '$rej'. ¿Descargarlo (\033[4m\033[1ms\033[0m\033[0m/n)? "
            unset proceder
            read -e proceder
            proceder=${proceder:-"s"}
            if [[ "$proceder" == "s" ]]; then
                $wget -O "./BALR2009.zip" "http://www.ign.es/ign/resources/herramientas/BALR2009.zip"
                if [[ $? -eq 0 && -f "./BALR2009.zip" ]]; then
                  $unzip "./BALR2009.zip"
                  if [[ $? -eq 0 && -f "./BALR2009.gsb" ]]; then
                    $rm "./BALR2009.zip"
                    rejilla2="`$pwd`/BALR2009.gsb"
                    rejilla2=$(printf "%q" "$rejilla2")
                    eval rejilla2="$rejilla2"
                    let fich_valido=1
                  else
                    echo "Error en la descompresión con '$unzip' (BALR2009.gsb)." 1>&2
                    let fich_valido=0
                  fi
                else
                  echo "Error en la descarga con '$wget' (BALR2009.zip)." 1>&2
                  let fich_valido=0
                fi
            else
              echo "OK. No se intentará la descarga. Prueba otra vez con el nombre del fichero..."
              let fich_valido=0              
            fi
          fi
        fi
      done
      echo -e "Se usará '$rejilla2' como rejilla de proyección de las Islas Baleares.\n"


      ## POR HACER: comprobar fechas válidas (de momento solo se comprueba que son números enteros)
      
      while ! [ "$fechad" -eq "$fechad" 2> /dev/null ]; do
        read -e -p "FILTRADO POR FECHAS (Formato AAAAMMDD): Fecha de inicio (ENTER para '$fechadesde'): " fechad
        fechad=${fechad:-$fechadesde}
      done
      echo -e "Usando '$fechad' como fecha de inicio. No se exportarán elementos anteriores a esta fecha.\n"

      while ! [ "$fechah" -eq "$fechah" 2> /dev/null ]; do
        read -e -p "FILTRADO POR FECHAS (Formato AAAAMMDD): Fecha de fin    (ENTER para '$fechahasta'): " fechah
        fechah=${fechah:-$fechahasta}
      done
      echo -e "Usando '$fechah' como fecha de fin. No se exportarán elementos posteriores a esta fecha.\n"

      while ! [ "$fechacd" -eq "$fechacd" 2> /dev/null ]; do
        read -e -p "FILTRADO POR FECHAS (Formato AAAAMMDD): Construcciones a partir de (ENTER para '$fechaconstrudesde'): " fechacd
        fechacd=${fechacd:-$fechaconstrudesde}
      done
      echo -e "Usando '$fechacd' como fecha de inicio para las construcciones. No se exportarán edificios construídos antes de esta fecha.\n"

      while ! [ "$fechach" -eq "$fechach" 2> /dev/null ]; do
        read -e -p "FILTRADO POR FECHAS (Formato AAAAMMDD): Construcciones hasta       (ENTER para '$fechaconstruhasta'): " fechach
        fechach=${fechach:-$fechaconstruhasta}
      done
      echo -e "Usando '$fechach' como fecha de fin para las construcciones. No se exportarán edificios construídos después de esta fecha.\n"

      # No tengo ni idea de lo que hace esta variable
      # La pasamos tal cual, sin hacer comprobaciones
      read -e -p "Tipo de registro de Catastro a usar (ENTER para todos): " tr
      tr=${tr:-$tiporegistro}
      echo -e "Usando '$tr' como variable TipoRegistro.\n"

      let opcion_valida=0
      while [[ $opcion_valida -eq 0 ]]; do
        echo -e -n "¿Imprimir ShapeIds (solo útil para depuración)? (s/\033[4m\033[1mn\033[0m\033[0m): "
        read -e psi
        if [[ "$psi" == "s" ]]; then
          let psi=1
          let opcion_valida=1
        elif [[ "$psi" == "n" ]]; then
          let psi=0
          let opcion_valida=1
        elif [[ -z "$psi" ]]; then
          let psi=$printshapeids
          let opcion_valida=1
        fi
      done
      case "$psi" in
        1)
          echo -e "OK. Se imprimirán los ShapeIds.\n"
          ;;
        0)
          echo -e "NO se imprimirán los ShapeIds.\n"
          ;;
        *)
          echo -e "Error inesperado.\n" 1>&2 #no debería pasar
          exit 1
      esac

      let opcion_valida=0
      while [[ $opcion_valida -eq 0 ]]; do
        echo -e -n "¿Mover portales? (\033[1ms\033[0m/\033[4m\033[0mn\033[0m\033[0m): "
        read -e movpor
        if [[ "$movpor" == "s" ]]; then
          let movpor=1
          let opcion_valida=1
        elif [[ "$movpor" == "n" ]]; then
          let movpor=0
          let opcion_valida=1
        elif [[ -z "$movpor" ]]; then
          let movpor=$moverportales
          let opcion_valida=1
        fi
      done
      case "$movpor" in
        1)
          echo -e "OK. Se moverán portales.\n"
          ;;
        0)
          echo -e "NO moverán portales.\n"
          ;;
        *)
          echo -e "Error inesperado.\n" 1>&2 #no debería pasar
          exit 1
      esac

      echo -e "\nGenerando archivos config..."


      $find "$dirbusqueda" -iname '*.CAT' -type f -printf '%h\n' | $sort -u | {
        while read dir
        do          
          archivo=`$find "$dir" -iname '*.CAT' -type f -printf '%f\n' -quit`
          nombre=${archivo%.*}
          extension=${archivo##*.}
          oldIFS=$IFS
          IFS='_'
          set $nombre
          IFS=$oldIFS
          clave="${1}_${2}"
          tipo="${3}"
          let provcod="10#$1"
          prov=`formatea "${provincias_MHAP[$1]}"`
          prov=`trim "$prov"`
          mun=`formatea "${municipios[$clave]}"`
          mun=`trim "$mun"`

          if [[ "$tipo" == "R" ]]; then
            catrustico="${dir}/${archivo}"
            caturbano=`$find "$dir" -iname "${clave}_U_*.CAT" -type f -printf '%f\n' -quit`
            if [[ -z "$caturbano" ]]; then
              echo "AVISO: No se encontró fichero CAT urbano para para ${mun} (${prov})." 1>&2
            else
              caturbano="${dir}/${caturbano}"
            fi
          elif [[ "$tipo" == "U" ]]; then
            caturbano="${dir}/${archivo}"
            catrustico=`$find "$dir" -iname "${clave}_R_*.CAT" -type f -printf '%f\n' -quit`
            if [[ -z "$catrustico" ]]; then
              echo "AVISO: No se encontró fichero CAT rústico para para ${mun} (${prov})." 1>&2
            else
              catrustico="${dir}/${catrustico}"
            fi
          else
            echo "Formato de archivo CAT desconocido: '$archivo'" 1>&2
          fi

          # OJO. No se comprueba si los directorios de los shapefiles contienen información válida
          #      (ALTIPUN, CARVIA, CONSTRU, etc... extensiones DBF, PRJ, SHP, SHX)
          shfurbano=`$find "$dir" -iname "${clave}_UA_*" -type d -printf '%f\n' -quit`
          shfrustico=`$find "$dir" -iname "${clave}_RA_*" -type d -printf '%f\n' -quit`

          if [[ -z "$shfurbano" && -z "$shfrustico" ]]; then
            echo "AVISO: No se encontraron directorios de ShapeFiles para ${mun} (${prov})." 1>&2
            echo "       Los directorios para este municipio deben empezar por: '${clave}_UA_' ó '${clave}_RA_'" 1>&2
            echo "       No se generará archivo de configuración para este municipio." 1>&2
            continue
          fi
          if [[ -z "$shfurbano" ]]; then
            echo "AVISO: No se encontraron shapefiles urbanos para para ${mun} (${prov})." 1>&2
          else
            shfurbano="${dir}/${shfurbano}"
          fi
          if [[ -z "$shfrustico" ]]; then
            echo "AVISO: No se encontraron shapefiles rústicos para para ${mun} (${prov})." 1>&2
          else
            shfrustico="${dir}/${shfrustico}"
          fi

          # OJO. Se supone que todos los ficheros PRJ contienen la misma proyección, así que cogemos el primero que encontramos
          shfdir=${shfurbano:-$shfrustico}
          prj=`$find "$shfdir" -iname "*.PRJ" -type f -print -quit`
          while read -N 31 i
          do
            oldIFS=$IFS
            IFS='"'
            set $i
            IFS=$oldIFS
            proyeccion=$2
            break
          done < "$prj"

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
          elif [ "$proyeccion" == "ED_1950_UTM_Zone_28N" ]; then
            epsg=23028
          elif [ "$proyeccion" == "ED50 / UTM zone 29N" ]; then
            epsg=23029
          elif [ "$proyeccion" == "ED_1950_UTM_Zone_29N" ]; then
            epsg=23029
          elif [ "$proyeccion" == "ED50 / UTM zone 30N" ]; then
            epsg=23030
          elif [ "$proyeccion" == "ED_1950_UTM_Zone_30N" ]; then
            epsg=23030
          elif [ "$proyeccion" == "ED50 / UTM zone 31N" ]; then
            epsg=23031
          elif [ "$proyeccion" == "ED_1950_UTM_Zone_31N" ]; then
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
            echo "AVISO: No se ha podido determinar la proyección para ${mun} (${prov})." 1>&2
            echo "       No se generará archivo de configuración para este municipio." 1>&2
            continue
          fi


          # Se generan 3 archivos de configuración por cada municipio:
          #  Normal:   Urbano y rústico. Si falta algún archivo se genera solo urbano o solo rústico.
          #  Urbano:   Se genera solo urbano. Si falta algún archivo de urbano se genera un archivo inútil.
          #  Rústico:  Se genera solo rústico. Si falta algún archivo de rústico se genera un archivo inútil.

          config_filename_normal="${dirc}/${prefijo}${mun}${sufijoconfig}"
          config_filename_rustico="${dirc}/${prefijo}${mun}${sufijoconfigrustico}"
          config_filename_urbano="${dirc}/${prefijo}${mun}${sufijoconfigurbano}"

          for config_filename in "$config_filename_normal" "$config_filename_rustico" "$config_filename_urbano"
          do
            if [[ ( -z "$shfurbano" || -z "$caturbano" ) && "$config_filename" == "$config_filename_urbano" ]]; then
              continue
            fi
            if [[ ( -z "$shfrustico" || -z "$catrustico" ) && "$config_filename" == "$config_filename_rustico" ]]; then
              continue
            fi
            echo "Escribiendo archivo ${config_filename}..."
            echo "################################################################################"  > "$config_filename"
            echo "# Fichero de configuración para 'cat2osm' correspondiente a ${mun} (${prov})"     >> "$config_filename"
            echo "#"                                                                                >> "$config_filename"
            echo "# Generado por '$esteprograma' versión $version "                                 >> "$config_filename"
            echo "#"                                                                                >> "$config_filename"
            echo "# Para más información visitar:"                                                  >> "$config_filename"
            echo "# https://wiki.openstreetmap.org/wiki/Spanish_Cadastre"                           >> "$config_filename"
            echo "# https://wiki.openstreetmap.org/wiki/Cat2Osm"                                    >> "$config_filename"
            echo "################################################################################" >> "$config_filename"
            echo "ResultPath=$dirr"           >> "$config_filename"
            echo "ResultFileName=$mun"        >> "$config_filename"

            # Shapefiles
            if [[ -z "$shfurbano" || -z "$caturbano" || "$config_filename" == "$config_filename_rustico" ]]; then
              echo -n "#"                     >> "$config_filename"
            fi
            echo "UrbanoSHPPath=$shfurbano"   >> "$config_filename"
            if [[ -z "$shfrustico" || -z "$catrustico" || "$config_filename" == "$config_filename_urbano" ]]; then
              echo -n "#"                     >> "$config_filename"
            fi
            echo "RusticoSHPPath=$shfrustico" >> "$config_filename"

            # Archivos CAT
            if [[ -z "$shfurbano" || -z "$caturbano" || "$config_filename" == "$config_filename_rustico" ]]; then
              echo -n "#"                     >> "$config_filename"
            fi
            echo "UrbanoCATFile=$caturbano"   >> "$config_filename"
            if [[ -z "$shfrustico" || -z "$catrustico" || "$config_filename" == "$config_filename_urbano" ]]; then
              echo -n "#"                     >> "$config_filename"
            fi
            echo "RusticoCATFile=$catrustico" >> "$config_filename"

            echo "FWToolsPath=$fwtoolspath"   >> "$config_filename"
            
            if [[ "$provcod" -eq 7 ]]; then
              # BALEARES
              rej=$rejilla2
            else
              # PENÍNSULA
              rej=$rejilla1
            fi
            eval rej=$rej
            echo "NadgridsPath=$rej"          >> "$config_filename"
            echo "Proyeccion=$epsg"           >> "$config_filename"
            echo "FechaDesde=$fechad"         >> "$config_filename"
            echo "FechaHasta=$fechah"         >> "$config_filename"
            echo "FechaConstruDesde=$fechacd" >> "$config_filename"
            echo "FechaConstruHasta=$fechach" >> "$config_filename"
	    echo "MovePortales=$movpor"     >> "$config_filename"
            echo "TipoRegistro=$tr"           >> "$config_filename"
            echo "PrintShapeIds=$psi"         >> "$config_filename"
          done
        done
      }

      ;;
    4)
      break
      ;;
    *)
      echo "Opción no válida" 1>&2
      continue
      ;;
  esac
done


# FIN del programa