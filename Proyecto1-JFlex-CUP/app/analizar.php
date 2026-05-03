<?php
header('Content-Type: text/html; charset=UTF-8');

$entrada = $_POST['entrada'] ?? '';
$entrada = trim($entrada);

if ($entrada === '') {
    echo "<div class='bloque error'><h2>Error</h2><p>Debe escribir una consulta o cargar un archivo.</p></div>";
    exit;
}

$baseProyecto = realpath(__DIR__ . '/..');
$tempDir = __DIR__ . DIRECTORY_SEPARATOR . 'temp';

if (!is_dir($tempDir)) {
    mkdir($tempDir, 0777, true);
}

$archivoEntrada = $tempDir . DIRECTORY_SEPARATOR . 'entrada_web.sql';
file_put_contents($archivoEntrada, $entrada);

$java = 'java';
$src = $baseProyecto . DIRECTORY_SEPARATOR . 'src';
$cup = $baseProyecto . DIRECTORY_SEPARATOR . 'tools' . DIRECTORY_SEPARATOR . 'java-cup-11b.jar';

$cmd = 'cd /d ' . escapeshellarg($baseProyecto) .
       ' && ' . $java .
       ' -cp ' . escapeshellarg('.;' . $src . ';' . $cup) .
       ' Main ' . escapeshellarg($archivoEntrada) . ' 2>&1';

$salida = shell_exec($cmd);

if ($salida === null || trim($salida) === '') {
    echo "<div class='bloque error'><h2>Error</h2><p>No se pudo ejecutar Java. Verifique que Java este instalado y que el proyecto este compilado.</p></div>";
    exit;
}

$lineas = preg_split('/\r\n|\r|\n/', trim($salida));

$esError = false;
foreach ($lineas as $linea) {
    if (
        stripos($linea, 'ERROR:') === 0 ||
        stripos($linea, 'Error lexico') !== false ||
        stripos($linea, 'Error sintactico') !== false
    ) {
        $esError = true;
        break;
    }
}

$posTabla = array_search('TABLA_RESULTADO', $lineas);

if ($esError) {
    echo "<div class='bloque error'>";
    echo "<h2>Error</h2>";
    echo "<pre>" . htmlspecialchars($salida, ENT_QUOTES, 'UTF-8') . "</pre>";
    echo "</div>";
    exit;
}

if ($posTabla !== false) {
    $filas = array_slice($lineas, $posTabla + 1);

    $filas = array_values(array_filter($filas, function ($f) {
        $f = trim($f);

        if ($f === '') return false;
        if (stripos($f, 'Estructura válida') !== false) return false;
        if (stripos($f, 'Base de datos seleccionada') !== false) return false;
        if (stripos($f, 'TABLA_RESULTADO') !== false) return false;

        return true;
    }));

    if (count($filas) > 0) {
        $encabezados = array_map('trim', explode('|', $filas[0]));

        echo "<div class='bloque ok'>";
        echo "<h2>Resultado de consulta</h2>";
        echo "<table>";
        echo "<thead><tr>";

        foreach ($encabezados as $h) {
            echo "<th>" . htmlspecialchars($h, ENT_QUOTES, 'UTF-8') . "</th>";
        }

        echo "</tr></thead><tbody>";

        for ($i = 1; $i < count($filas); $i++) {
            $celdas = array_map('trim', explode('|', $filas[$i]));

            echo "<tr>";
            foreach ($celdas as $c) {
                echo "<td>" . htmlspecialchars($c, ENT_QUOTES, 'UTF-8') . "</td>";
            }
            echo "</tr>";
        }

        echo "</tbody></table>";
        echo "</div>";
    }
} else {
    echo "<div class='bloque ok'>";
    echo "<h2>Resultado general</h2>";
    echo "<p>Consulta ejecutada correctamente.</p>";
    echo "</div>";
}
?>