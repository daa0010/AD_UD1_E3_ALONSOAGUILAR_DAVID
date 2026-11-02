# Decisiones de Diseño y Criterios de Implementación (DECISIONES.md)

## 1. ¿Qué haces con una línea incompleta y por qué?

* **Acción:** Si al trocear una línea del fichero de texto mediante el separador punto y coma se detecta que no cuenta
  exactamente con los 4 campos esperados (`campos.length != 4`), la línea es descartada de inmediato sin procesarse. Se
  registra la incidencia correspondiente en el fichero `incidencias.log` indicando el número de línea y el motivo (
  `campos incompletos`), y se continúa con la lectura de la siguiente línea (`continue`).
* **Por qué:** Un registro incompleto carece de la información estructural mínima indispensable (contador, abonado,
  fecha o lectura). Intentar parsearlo provocaría excepciones en tiempo de ejecución (`ArrayIndexOutOfBoundsException`),
  por lo que el filtrado previo garantiza la robustez del programa ante ficheros corruptos o malformados.

## 2. ¿Cómo distingues un contador sustituido de una lectura errónea?

* **Distinción:**
    * Se identifica una **lectura errónea** cuando el valor numérico de la lectura no cumple con el formato válido
      establecido (número entero opcionalmente negativo, seguido de una coma y exactamente tres cifras decimales,
      evaluado mediante la expresión regular `^-?\\d+,\\d{3}$`) o cuando se detectan anomalías formales en el contador o
      en la fecha. En tal caso, la línea entera se descarta y se anota en el log.
    * Por el contrario, se identifica un **contador sustituido** (estado `POSIBLE_SUSTITUCION` en el resumen) cuando el
      registro es formalmente correcto y pasa todas las validaciones de formato, pero al calcular el consumo diferencial
      respecto al registro anterior del mismo contador en el histórico binario (`lecturaActual - lecturaAnterior`), el
      resultado es **negativo** (es decir, el contador actual marca un número inferior al anterior, lo que ocurre
      habitualmente cuando se instala un contador nuevo que se reinicia a cero o con valores bajos).

## 3. ¿Qué pasaría si el binario se abriera en modo sobrescribir?

* **Consecuencia:** Si el fichero binario `lecturas.dat` se abriera en modo sobrescritura (es decir, instanciando
  `FileOutputStream` sin el parámetro `true`, o utilizando un flujo por defecto que trunca el archivo), **se perdería
  todo el histórico acumulado** de días o ejecuciones anteriores cada vez que se ejecutase el programa.
* **Impacto:** El sistema procesaría únicamente las lecturas del parte diario actual, machacando el contenido previo del
  archivo binario. Como consecuencia directa, el cálculo del consumo en el `resumen.txt` y el histórico de los
  contadores quedarían invalidados, ya que no se dispondría de las lecturas anteriores necesarias para realizar
  comparativas ni se cumpliría el requisito funcional de acumular los datos de forma persistente entre diferentes
  ejecuciones.