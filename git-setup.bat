@echo off
echo =================================
echo    SETUP GIT PARA JAVA PROJECT
echo =================================
echo.

REM Inicializar repositorio git
echo Inicializando repositorio git...
git init
git add .
git commit -m "Initial commit: Java Rules Engine migration from Python FastAPI to Spring Boot"

echo.
echo Repositorio inicializado localmente.
echo.

echo Para subir a GitHub, ejecuta estos comandos:
echo.
echo git remote add origin https://github.com/cristian-data-science/java_def_recomendations.git
echo git branch -M main
echo git push -u origin main
echo.

echo =================================
echo    COMANDOS ADICIONALES
echo =================================
echo.
echo # Ver status
echo git status
echo.
echo # Agregar cambios
echo git add .
echo git commit -m "Descripcion del cambio"
echo git push
echo.

pause

