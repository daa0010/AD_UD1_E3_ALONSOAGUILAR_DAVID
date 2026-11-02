import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Locale;

public class LectorContadores {

    // Creo una clase interna estática para almacenar las estadísticas de cada contador
    private static class DatosContador {
        int idContador = 0;
        String abonado = "";
        int numLecturas = 0;
        double lecturaAnterior = 0.0;
        double lecturaUltima = 0.0;
    }

    public static void main(String[] args) {

        // Compruebo que me han pasado la ruta del fichero por argumento al ejecutar el programa
        if (args.length == 0) {
            System.out.println("Error: No se ha indicado el fichero de lecturas.");
            System.out.println("Uso: java -cp out LectorContadores datos/lecturas_20261019.txt");
            // Salgo con código de error 2 sin lanzar excepciones
            System.exit(2);
        }

        // Guardo la ruta que me pasan e instancio mi objeto File, recordando que esto solo representa la ruta y no
        // lo crea en el disco
        String rutaEntrada = args[0];
        File ficheroLecturas = new File(rutaEntrada);

        // Compruebo si el fichero realmente existe utilizando la función exists()
        if (!ficheroLecturas.exists()) {
            System.out.println("Error: El fichero '" + rutaEntrada + "' no existe.");
            System.exit(2);
        }

        // Utilizo la función length() para obtener el tamaño en bytes y verificar que no esté vacío
        if (ficheroLecturas.length() == 0) {
            System.out.println("Error: El fichero '" + rutaEntrada + "' está vacío.");
            System.exit(2);
        }

        // Extraigo la carpeta madre donde está el fichero de entrada, ya que el
        // enunciado pide que las tres salidas se escriban en esta misma carpeta.
        File carpetaDestino = ficheroLecturas.getParentFile();

        // Si me pasan solo el nombre del archivo sin ruta (ej: "fichero.txt"),
        // getParentFile() devuelve null. En ese caso, asumo la carpeta actual (".").
        if (carpetaDestino == null) {
            carpetaDestino = new File(".");
        }

        // Si paso todas las comprobaciones, utilizo getName() para sacar el nombre por consola y confirmar
        System.out.println("Fichero correcto. Procediendo a leer: " + ficheroLecturas.getName());

        // Preparo un contador para saber en qué línea estoy exactamente,
        // ya que el log de incidencias me exige poner el número real de la línea.
        int numeroLinea = 0;
        // Creo una lista para recordar qué contadores he procesado y detectar duplicados
        ArrayList<String> contadoresAceptados = new ArrayList<>();

        // Preparo las rutas de los ficheros de salida en la misma carpeta que el de entrada
        File ficheroBinario = new File(carpetaDestino, "lecturas.dat");
        File ficheroLog = new File(carpetaDestino, "incidencias.log");

        // Abro un DataInputStream para contar los registros existentes hasta que salte el fin de fichero
        int registrosPrevios = 0;
        if (ficheroBinario.exists()) {
            try (DataInputStream dis = new DataInputStream(new FileInputStream(ficheroBinario))) {
                while (true) {
                    dis.readInt();
                    dis.readLong();
                    dis.readDouble();
                    dis.readUTF();
                    registrosPrevios++; // Si he conseguido leer los 4 campos, cuento un registro más
                }
            } catch (EOFException e) {
                // Al llegar al final del fichero binario, Java lanza esta excepción. Es normal.
            } catch (IOException e) {
                System.out.println("Error leyendo el histórico binario: " + e.getMessage());
            }
        }

        System.out.println("Registros previos en el fichero histórico: " + registrosPrevios);

        // Abro el fichero de texto para leerlo línea a línea con BufferedReader.
        // También abro los escritores para el binario y el log.
        // El parámetro 'true' al instanciar FileOutputStream y FileWriter indica que abro los ficheros
        // en modo añadir (append), tal y como pide el enunciado.
        try (BufferedReader lector = new BufferedReader(new FileReader(ficheroLecturas, StandardCharsets.ISO_8859_1));
             DataOutputStream escritorBinario = new DataOutputStream(new FileOutputStream(ficheroBinario, true));
             FileWriter escritorLog = new FileWriter(ficheroLog, StandardCharsets.UTF_8, true)) {

            String linea;

            // Voy leyendo el fichero de forma secuencial hasta que devuelva null (fin de fichero)
            while ((linea = lector.readLine()) != null) {

                // Incremento el número de línea nada más leerla (incluyendo las líneas en blanco)
                numeroLinea++;

                // Si la línea está en blanco, la ignoro sin registrar ninguna incidencia y paso a la siguiente
                if (linea.trim().isEmpty()) {
                    continue;
                }

                // Troceo la línea usando el punto y coma como separador.
                // Uso el límite -1 para que si el último campo está vacío (ej: termina en ';'),
                // no lo elimine del array y me lo cuente correctamente.
                String[] campos = linea.split(";", -1);

                // Empiezo asumiendo que la línea es correcta (sin error)
                String motivoError = null;

                // 1. ¿Campos incompletos? (Tienen que ser exactamente 4)
                if (campos.length != 4) {
                    motivoError = "campos incompletos";
                }
                // Si hay 4 campos, paso a comprobar el resto extrayendo sus valores
                else {
                    String contador = campos[0];
                    String abonado = campos[1];
                    String fecha = campos[2];
                    String lectura = campos[3];

                    // 2. ¿Contador no válido? (Debe tener 5 dígitos y no ser ceros)
                    // Uso una expresión regular (\\d{5}) para forzar que sean exactamente 5 números
                    if (!contador.matches("\\d{5}") || contador.equals("00000")) {
                        motivoError = "contador no válido";
                    }
                    // 3. ¿Abonado vacío?
                    else if (abonado.isEmpty()) {
                        motivoError = "abonado vacío";
                    }
                    // 4. ¿Fecha no válida? (Tiene que ser AAAA-MM-DD y existir en el calendario)
                    else {
                        try {
                            // Intento convertir el texto a fecha real. Si el formato está mal o el día no existe (ej. 32 de enero), saltará al catch
                            LocalDate.parse(fecha);
                        } catch (DateTimeParseException e) {
                            motivoError = "fecha no válida";
                        }

                        // Sigo comprobando si la fecha fue correcta (motivoError sigue siendo null)
                        if (motivoError == null) {
                            // 5. ¿Lectura no válida? (Número con coma y 3 decimales)
                            // Expresión regular: opcionalmente un signo menos (^-?), uno o más números (\\d+), una coma (,), y exactamente 3 números (\\d{3}$)
                            if (!lectura.matches("^-?\\d+,\\d{3}$")) {
                                motivoError = "lectura no válida";
                            }
                            // 6. ¿Lectura duplicada? (Compruebo si el contador ya lo metí en mi lista de aceptados)
                            else if (contadoresAceptados.contains(contador)) {
                                motivoError = "lectura duplicada";
                            }
                        }
                    }
                }

                // Evalúo si he encontrado algún error en la línea
                if (motivoError != null) {
                    // Escribo físicamente en el fichero incidencias.log el número de línea y el motivo, añadiendo el salto de línea correspondiente.
                    escritorLog.write(numeroLinea + ";" + motivoError + System.lineSeparator());
                    continue; // Me salto el resto del código y paso a leer la siguiente línea del txt
                }

                // Si llego aquí, la línea ha pasado todos los filtros y es completamente válida.
                // Guardo el contador en mi lista de aceptados para que si vuelve a aparecer más abajo salte como duplicado
                contadoresAceptados.add(campos[0]);

                // Convierto los textos a los tipos de datos primitivos que se exigen para el binario.
                int contadorBinario = Integer.parseInt(campos[0]);
                // Elimino los guiones de la fecha ("2026-10-19" -> "20261019") y la convierto a long
                long fechaBinario = Long.parseLong(campos[2].replace("-", ""));
                // Sustituyo la coma por un punto ("1234,500" -> "1234.500") para poder parsearlo a Double
                double lecturaBinario = Double.parseDouble(campos[3].replace(",", "."));

                // Escribo los datos en el binario respetando el orden especificado en el diseño
                escritorBinario.writeInt(contadorBinario);
                escritorBinario.writeLong(fechaBinario);
                escritorBinario.writeDouble(lecturaBinario);
                escritorBinario.writeUTF(campos[1]);
            }

            System.out.println("Fin de la lectura del parte nocturno.");

        } catch (IOException e) {
            System.out.println("Error procesando los ficheros: " + e.getMessage());
        }

        ArrayList<DatosContador> listaResumen = new ArrayList<>();

        if (ficheroBinario.exists()) {
            try (DataInputStream dis = new DataInputStream(new FileInputStream(ficheroBinario))) {
                while (true) {
                    int contador = dis.readInt();
                    long fecha = dis.readLong();
                    double lectura = dis.readDouble();
                    String abonado = dis.readUTF();

                    // Busco manualmente si este contador ya existe en mi lista
                    DatosContador datos = null;
                    for (int i = 0; i < listaResumen.size(); i++) {
                        if (listaResumen.get(i).idContador == contador) {
                            datos = listaResumen.get(i);
                            break;
                        }
                    }

                    // Si no existe, lo creo y lo añado
                    if (datos == null) {
                        datos = new DatosContador();
                        datos.idContador = contador;
                        listaResumen.add(datos);
                    }

                    datos.numLecturas++;
                    datos.lecturaAnterior = datos.lecturaUltima;
                    datos.lecturaUltima = lectura;
                    datos.abonado = abonado;
                }
            } catch (EOFException e) {
                // Fin del fichero binario alcanzado
            } catch (IOException e) {
                System.out.println("Error al leer el histórico para el resumen: " + e.getMessage());
            }
        }

        // Ordeno la lista de contadores de menor a mayor
        for (int i = 0; i < listaResumen.size() - 1; i++) {
            for (int j = 0; j < listaResumen.size() - 1 - i; j++) {
                if (listaResumen.get(j).idContador > listaResumen.get(j + 1).idContador) {
                    DatosContador temporal = listaResumen.get(j);
                    listaResumen.set(j, listaResumen.get(j + 1));
                    listaResumen.set(j + 1, temporal);
                }
            }
        }

        // Genero el fichero resumen.txt machacando el anterior si existía
        File ficheroResumen = new File(carpetaDestino, "resumen.txt");

        try (PrintWriter escritorResumen = new PrintWriter(new FileWriter(ficheroResumen, StandardCharsets.UTF_8))) {

            for (int i = 0; i < listaResumen.size(); i++) {
                DatosContador datos = listaResumen.get(i);
                String contadorStr = String.format("%05d", datos.idContador);
                String consumoStr = "-";
                String estado;

                if (datos.numLecturas == 1) {
                    estado = "SIN_HISTORICO";
                } else {
                    double consumo = datos.lecturaUltima - datos.lecturaAnterior;
                    consumoStr = String.format(Locale.US, "%.3f", consumo).replace('.', ',');

                    if (consumo == 0) {
                        estado = "SIN_CONSUMO";
                    } else if (consumo < 0) {
                        estado = "POSIBLE_SUSTITUCION";
                    } else {
                        estado = "OK";
                    }
                }

                escritorResumen.println(contadorStr + ";" + datos.abonado + ";" + datos.numLecturas + ";" + consumoStr + ";" + estado);
            }

            System.out.println("Resumen generado con éxito en: " + ficheroResumen.getName());

        } catch (IOException e) {
            System.out.println("Error al escribir el resumen: " + e.getMessage());
        }
    }
}