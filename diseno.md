# Diseño del Fichero Binario (lecturas.dat)

## 1. Estructura y Reparto de Bytes por Registro

Cada registro de lectura almacenado en el fichero binario `lecturas.dat` se compone de cuatro campos escritos
secuencialmente mediante la clase `DataOutputStream`, ocupando un tamaño fijo de bytes por tipo de dato:

1. **Identificador del Contador (`int`):**
    * **Tamaño:** 4 bytes.
    * **Justificación:** Almacena los 5 dígitos numéricos del contador convertidos a tipo entero. Es el tamaño estándar
      en Java para representar enteros de 32 bits, suficiente para cubrir el rango de códigos.
2. **Fecha de Lectura (`long`):**
    * **Tamaño:** 8 bytes.
    * **Justificación:** La fecha en formato texto (`AAAA-MM-DD`) se transforma eliminando los guiones (ej. `20261019`)
      y se almacena como un número entero largo (`long`) de 64 bits. Permite un almacenamiento compacto y facilita
      futuras comparaciones u ordenaciones cronológicas.
3. **Valor de la Lectura (`double`):**
    * **Tamaño:** 8 bytes.
    * **Justificación:** Almacena el valor numérico decimal del contador (con 3 decimales) como un número en coma
      flotante de doble precisión (64 bits), garantizando la precisión matemática necesaria para calcular consumos
      diferenciales.
4. **Nombre del Abonado (`String`):**
    * **Tamaño:** Variable (2 bytes de cabecera que indican la longitud de la cadena + los bytes de los caracteres en
      codificación UTF).
    * **Justificación:** Almacena el texto completo del nombre del abonado mediante el método `writeUTF()`, adaptándose
      dinámicamente al espacio requerido por cada nombre.

## 2. Justificación del Orden de los Campos

El orden establecido en la escritura (`int contador` -> `long fecha` -> `double lectura` -> `String abonado`) responde a
criterios de optimización de lectura y estructuración lógica:

* Se colocan primero los campos de longitud fija (`int`, `long`, `double` suman un total de 20 bytes por registro), lo
  que facilita el cálculo de desplazamientos y la lectura secuencial rápida.
* El **contador** se sitúa en la primera posición por ser la clave principal de identificación y el criterio fundamental
  de agrupación para el resumen posterior.
* La **fecha** y la **lectura** le siguen de forma consecutiva para contextualizar temporal y cuantitativamente el
  registro.
* Por último, se sitúa el **abonado** (de longitud variable) al final del registro, permitiendo que los flujos de
  lectura (`DataInputStream`) lean primero los bloques fijos y extraigan la cadena de texto al final de cada iteración
  sin alterar el puntero de los datos primitivos.