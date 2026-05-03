<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Analizador SQL Simplificado</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
<div class="contenedor">
    <div class="tarjeta">
        <h1>Analizador SQL Simplificado</h1>
        <p class="subtitulo">Proyecto con JFlex + CUP + Java + PHP</p>

        <form id="formAnalizar">
            <label for="archivo">Cargar archivo .txt o .sql</label>
            <input type="file" id="archivo" accept=".txt,.sql">

            <label for="entrada" class="label-textarea">O ingrese una o varias sentencias SQL</label>

            <textarea name="entrada" id="entrada" rows="18" placeholder="Ejemplo:
CREATE DATABASE academia;
USE academia;
CREATE TABLE alumno (id INT, nombre VARCHAR(50), edad INT);
INSERT INTO alumno VALUES (1, 'Ana', 20);
SELECT * FROM alumno;"></textarea>

            <div class="botones">
                <button type="submit" class="btn">Analizar</button>
                <button type="button" class="btn" onclick="ejemploCrearBD()">Ejemplo BD</button>
                <button type="button" class="btn" onclick="ejemploInsertar()">Ejemplo INSERT</button>
                <button type="button" class="btn" onclick="ejemploSelect()">Ejemplo SELECT</button>
                <button type="reset" class="btn secundario" onclick="limpiarResultado()">Limpiar</button>
            </div>
        </form>

        <div id="resultadoAjax"></div>
    </div>
</div>

<script>
document.getElementById("formAnalizar").addEventListener("submit", function(e) {
    e.preventDefault();

    const archivo = document.getElementById("archivo").files[0];
    const entradaTexto = document.getElementById("entrada").value.trim();

    if (entradaTexto !== "") {
        analizarTexto(entradaTexto);
    } else if (archivo) {
        const lector = new FileReader();
        lector.onload = function(evento) {
            const contenidoArchivo = evento.target.result;
            document.getElementById("entrada").value = contenidoArchivo;
            analizarTexto(contenidoArchivo);
        };
        lector.readAsText(archivo);
    } else {
        document.getElementById("resultadoAjax").innerHTML =
            "<div class='bloque error'><h2>Error</h2><p>Debe escribir una consulta o cargar un archivo.</p></div>";
    }
});

function analizarTexto(texto) {
    const resultado = document.getElementById("resultadoAjax");
    resultado.innerHTML = "<div class='bloque info'><h2>Analizando...</h2></div>";

    const formData = new FormData();
    formData.append("entrada", texto);

    fetch("analizar.php", {
        method: "POST",
        body: formData
    })
    .then(response => response.text())
    .then(data => {
        resultado.innerHTML = data;
    })
    .catch(error => {
        resultado.innerHTML =
            "<div class='bloque error'><h2>Error</h2><p>No se pudo analizar la consulta.</p></div>";
        console.error(error);
    });
}

function ejemploCrearBD() {
    document.getElementById("entrada").value =
`CREATE DATABASE academia;
USE academia;
CREATE TABLE alumno (id INT, nombre VARCHAR(50), direccion VARCHAR(50), telefono INT, edad INT, trabajo VARCHAR(10));`;
}

function ejemploInsertar() {
    document.getElementById("entrada").value =
`USE academia;
INSERT INTO alumno VALUES (1, 'Juan', 'Zona1', 1111, 20, 'Si');
INSERT INTO alumno VALUES (2, 'Jose', 'Zona2', 2222, 21, 'No');
INSERT INTO alumno VALUES (3, 'Eric', 'Zona3', 3333, 22, 'Si');`;
}

function ejemploSelect() {
    document.getElementById("entrada").value =
`USE academia;
SELECT * FROM alumno;`;
}

function limpiarResultado() {
    document.getElementById("resultadoAjax").innerHTML = "";
}
</script>
</body>
</html>
