@echo off
SET FWTOOLS_DIR=C:\PROGRA~2\FWTOOL~1.7

PATH=%FWTOOLS_DIR%\bin;%FWTOOLS_DIR%\python;%PATH%
set PYTHONPATH=%FWTOOLS_DIR%\pymod
set PROJ_LIB=%FWTOOLS_DIR%\proj_lib
set GEOTIFF_CSV=%FWTOOLS_DIR%\data
set GDAL_DATA=%FWTOOLS_DIR%\data
set GDAL_DRIVER_PATH=%FWTOOLS_DIR%\gdal_plugins

ogr2ogr.exe -s_srs "+init=epsg:23029 +nadgrids=./peninsula.gsb +wktext" -t_srs EPSG:4326 %2 %3