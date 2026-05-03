import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class Main {
    private static final Path ROOT = Paths.get("General");
    private static String bdActual = "";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("ERROR: Debe proporcionar un archivo de entrada.");
            return;
        }

        try {
            Files.createDirectories(ROOT);
            String entrada = Files.readString(Paths.get(args[0]), StandardCharsets.UTF_8).trim();

            if (entrada.isEmpty()) {
                System.out.println("ERROR: La entrada esta vacia.");
                return;
            }

            List<String> errores = validarEspaciosSQL(entrada);
            if (!errores.isEmpty()) {
                System.out.println("ERROR: La consulta no respeta los espacios obligatorios de SQL Server.");
                for (String e : errores) System.out.println("- " + e);
                return;
            }

            List<String> sentencias = separarSentencias(entrada);
            List<String> mensajes = new ArrayList<>();

            for (String sentencia : sentencias) {
                ejecutarSentencia(sentencia.trim(), mensajes);
            }

            System.out.println("Estructura valida");
            for (String m : mensajes) System.out.println(m);

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private static List<String> validarEspaciosSQL(String sql) {
        List<String> errores = new ArrayList<>();
        String sinSaltos = sql.replace("\r", " ").replace("\n", " ");

        String[] patrones = {
            "(?i)\\bSELECT\\*",
            "(?i)\\*FROM\\b",
            "(?i)\\bFROM[A-Za-z_]",
            "(?i)\\bWHERE[A-Za-z_]",
            "(?i)\\bUPDATE[A-Za-z_]",
            "(?i)\\bSET[A-Za-z_]",
            "(?i)\\bINSERT[A-Za-z_]",
            "(?i)\\bINTO[A-Za-z_]",
            "(?i)\\bVALUES[A-Za-z_]",
            "(?i)\\bCREATE[A-Za-z_]",
            "(?i)\\bDATABASE[A-Za-z_]",
            "(?i)\\bTABLE[A-Za-z_]",
            "(?i)\\bUSE[A-Za-z_]",
            "(?i)[A-Za-z_]FROM\\b",
            "(?i)[A-Za-z_]WHERE\\b",
            "(?i)[A-Za-z_]VALUES\\b",
            "(?i)[A-Za-z_]SET\\b"
        };

        for (String p : patrones) {
            Matcher m = Pattern.compile(p).matcher(sinSaltos);
            if (m.find()) {
                errores.add("Token pegado detectado cerca de: " + m.group());
            }
        }

        return errores;
    }

    private static List<String> separarSentencias(String sql) throws Exception {
        List<String> lista = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean enCadena = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\'') enCadena = !enCadena;
            if (c == ';' && !enCadena) {
                String s = actual.toString().trim();
                if (!s.isEmpty()) lista.add(s);
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }

        String ultima = actual.toString().trim();
        if (!ultima.isEmpty()) lista.add(ultima);

        if (lista.isEmpty()) throw new Exception("No se encontro ninguna sentencia SQL valida.");
        return lista;
    }

    private static void ejecutarSentencia(String sql, List<String> mensajes) throws Exception {
        if (coincide(sql, "(?is)^CREATE\\s+DATABASE\\s+[A-Za-z_][A-Za-z0-9_]*$")) {
            crearBaseDatos(sql, mensajes);
        } else if (coincide(sql, "(?is)^USE\\s+[A-Za-z_][A-Za-z0-9_]*$")) {
            usarBaseDatos(sql, mensajes);
        } else if (coincide(sql, "(?is)^CREATE\\s+TABLE\\s+[A-Za-z_][A-Za-z0-9_]*\\s*\\(.+\\)$")) {
            crearTabla(sql, mensajes);
        } else if (coincide(sql, "(?is)^INSERT\\s+INTO\\s+[A-Za-z_][A-Za-z0-9_]*(\\s*\\([^)]*\\))?\\s+VALUES\\s*\\(.+\\)$")) {
            insertar(sql, mensajes);
        } else if (coincide(sql, "(?is)^SELECT\\s+\\*\\s+FROM\\s+[A-Za-z_][A-Za-z0-9_]*$")) {
            seleccionar(sql);
        } else {
            throw new Exception("Sentencia no valida o no soportada: " + sql);
        }
    }

    private static boolean coincide(String texto, String regex) {
        return Pattern.compile(regex).matcher(texto.trim()).matches();
    }

    private static void crearBaseDatos(String sql, List<String> mensajes) throws Exception {
        Matcher m = Pattern.compile("(?is)^CREATE\\s+DATABASE\\s+([A-Za-z_][A-Za-z0-9_]*)$").matcher(sql.trim());
        if (!m.find()) throw new Exception("CREATE DATABASE invalido.");

        String nombreBD = m.group(1);
        Path carpetaBD = ROOT.resolve(nombreBD);
        Files.createDirectories(carpetaBD);
        bdActual = nombreBD;
        mensajes.add("Base de datos creada/usada: " + nombreBD);
    }

    private static void usarBaseDatos(String sql, List<String> mensajes) throws Exception {
        Matcher m = Pattern.compile("(?is)^USE\\s+([A-Za-z_][A-Za-z0-9_]*)$").matcher(sql.trim());
        if (!m.find()) throw new Exception("USE invalido.");

        String nombreBD = m.group(1);
        Path carpetaBD = ROOT.resolve(nombreBD);
        if (!Files.exists(carpetaBD)) throw new Exception("La base de datos no existe: " + nombreBD);

        bdActual = nombreBD;
        mensajes.add("Base de datos seleccionada: " + nombreBD);
    }

    private static void crearTabla(String sql, List<String> mensajes) throws Exception {
        validarBDActual();

        Matcher m = Pattern.compile("(?is)^CREATE\\s+TABLE\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\((.+)\\)$").matcher(sql.trim());
        if (!m.find()) throw new Exception("CREATE TABLE invalido.");

        String tabla = m.group(1);
        String columnasTexto = m.group(2).trim();
        List<String> columnas = obtenerNombresColumnas(columnasTexto);

        Path archivoTabla = ROOT.resolve(bdActual).resolve(tabla + ".txt");
        if (Files.exists(archivoTabla)) throw new Exception("La tabla ya existe: " + tabla);

        List<String> contenido = new ArrayList<>();
        contenido.add("TABLA: " + tabla);
        contenido.add("COLUMNAS:");
        contenido.add(String.join(" | ", columnas));
        contenido.add("");
        contenido.add("FILAS:");
        contenido.add(String.join(" | ", columnas));

        Files.write(archivoTabla, contenido, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        mensajes.add("Tabla creada: " + tabla + " en BD " + bdActual);
    }

    private static List<String> obtenerNombresColumnas(String columnasTexto) throws Exception {
        List<String> partes = separarPorComas(columnasTexto);
        List<String> columnas = new ArrayList<>();

        for (String parte : partes) {
            String limpia = parte.trim();
            Matcher m = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)").matcher(limpia);
            if (!m.find()) throw new Exception("Columna invalida: " + limpia);
            columnas.add(m.group(1));
        }

        if (columnas.isEmpty()) throw new Exception("La tabla debe tener columnas.");
        return columnas;
    }

    private static void insertar(String sql, List<String> mensajes) throws Exception {
        validarBDActual();

        Matcher m = Pattern.compile("(?is)^INSERT\\s+INTO\\s+([A-Za-z_][A-Za-z0-9_]*)(\\s*\\([^)]*\\))?\\s+VALUES\\s*\\((.+)\\)$").matcher(sql.trim());
        if (!m.find()) throw new Exception("INSERT INTO invalido.");

        String tabla = m.group(1);
        String valoresTexto = m.group(3).trim();
        Path archivoTabla = ROOT.resolve(bdActual).resolve(tabla + ".txt");

        if (!Files.exists(archivoTabla)) throw new Exception("La tabla no existe: " + tabla);

        List<String> columnas = leerColumnas(archivoTabla);
        List<String> valores = separarPorComas(valoresTexto);

        if (valores.size() != columnas.size()) {
            throw new Exception("Cantidad de valores incorrecta. La tabla " + tabla + " tiene " + columnas.size() + " columnas y el INSERT trae " + valores.size() + " valores.");
        }

        List<String> limpios = new ArrayList<>();
        for (String v : valores) limpios.add(limpiarValor(v));

        Files.writeString(archivoTabla, String.join(" | ", limpios) + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        mensajes.add("Dato insertado en tabla: " + tabla);
    }

    private static void seleccionar(String sql) throws Exception {
        validarBDActual();

        Matcher m = Pattern.compile("(?is)^SELECT\\s+\\*\\s+FROM\\s+([A-Za-z_][A-Za-z0-9_]*)$").matcher(sql.trim());
        if (!m.find()) throw new Exception("SELECT invalido.");

        String tabla = m.group(1);
        Path archivoTabla = ROOT.resolve(bdActual).resolve(tabla + ".txt");

        if (!Files.exists(archivoTabla)) throw new Exception("La tabla no existe: " + tabla);

        List<String> lineas = Files.readAllLines(archivoTabla, StandardCharsets.UTF_8);
        int posFilas = lineas.indexOf("FILAS:");
        if (posFilas == -1 || posFilas + 1 >= lineas.size()) throw new Exception("Archivo de tabla dañado: " + tabla);

        System.out.println("TABLA_RESULTADO");
        for (int i = posFilas + 1; i < lineas.size(); i++) {
            String linea = lineas.get(i).trim();
            if (!linea.isEmpty()) System.out.println(linea);
        }
    }

    private static List<String> leerColumnas(Path archivoTabla) throws Exception {
        List<String> lineas = Files.readAllLines(archivoTabla, StandardCharsets.UTF_8);
        int posColumnas = lineas.indexOf("COLUMNAS:");
        if (posColumnas == -1 || posColumnas + 1 >= lineas.size()) throw new Exception("No se pudieron leer las columnas.");
        return Arrays.asList(lineas.get(posColumnas + 1).split("\\s*\\|\\s*"));
    }

    private static List<String> separarPorComas(String texto) throws Exception {
        List<String> lista = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean enCadena = false;
        int parentesis = 0;

        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '\'') enCadena = !enCadena;
            if (!enCadena && c == '(') parentesis++;
            if (!enCadena && c == ')') parentesis--;

            if (c == ',' && !enCadena && parentesis == 0) {
                lista.add(actual.toString().trim());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }

        if (enCadena) throw new Exception("Cadena de texto sin cerrar.");
        String ultimo = actual.toString().trim();
        if (!ultimo.isEmpty()) lista.add(ultimo);
        return lista;
    }

    private static String limpiarValor(String valor) {
        String v = valor.trim();
        if (v.startsWith("'") && v.endsWith("'") && v.length() >= 2) {
            v = v.substring(1, v.length() - 1);
        }
        return v;
    }

    private static void validarBDActual() throws Exception {
        if (bdActual == null || bdActual.isEmpty()) {
            throw new Exception("Primero debe ejecutar CREATE DATABASE nombreBD; o USE nombreBD;");
        }
    }
}
