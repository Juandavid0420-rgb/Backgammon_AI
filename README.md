# Backgammon_AI
Taller 4 de IA (JUEGOS)


1. Correr el juego.
2. Entender cómo funciona el Backgammon.
3. Leer el tablero ASCII.
4. Usar el panel de ventajas.
5. Saber cómo se gana.

---

# 🎲 Backgammon Minimax (Java)

Un juego clásico de **Backgammon** jugable por consola 🖥️ con un oponente IA 🤖 basado en **minimax**.
Este proyecto busca que cualquiera pueda divertirse sin conocimientos técnicos avanzados.

---

## 🚀 Cómo correr el juego

1. Asegúrate de tener **Java 11+** instalado:

   ```bash
   java -version
   ```

2. Descarga o copia el archivo **BackgammonMinimax.java** en una carpeta.

3. Compila (Windows, Mac o Linux):

   ```bash
   javac -encoding UTF-8 BackgammonMinimax.java
   ```

4. Ejecuta el juego:

   ```bash
   java BackgammonMinimax
   ```

5. ¡Listo! 🎉 El sistema te preguntará con qué color quieres jugar (**Blanco o Negro**).

---

## 🕹️ Cómo funciona el juego

* Cada jugador (👤 tú y 🤖 la IA) empieza con **15 fichas**.
* El objetivo es **sacar todas tus fichas del tablero** antes que el rival.
* En cada turno se tiran **dos dados** 🎲:

  * El programa lista todas tus jugadas legales.
  * Tú eliges el índice `[0], [1], [2]...` de la jugada que quieras.
  * La IA hace su jugada automáticamente.
* El juego termina cuando uno de los dos logra sacar todas sus fichas (**Off = 15**).

---

## 🧾 Cómo leer el tablero ASCII

Ejemplo:

```
============ TABLERO ============
BLANCAS: 24 -> 1   (<- hacia la izquierda)
NEGRAS : 1  -> 24  (-> hacia la derecha)

       24  23  22  21  20  19  18  17  16  15  14  13
TOP :  2W   .   .   .   .  5B   .  3B   .   .   .  5W
----+------------------------------------------------
BOT :  5B   .   .   .  3W   .  5W   .   .   .   .  2B
       12  11  10   9   8   7   6   5   4   3   2   1

Barra: W=0  B=0   |   Off: W=0  B=0
Leyenda:  . = vacío | nW = n blancas | nB = n negras | Puntos: 24..1
===============================================
```

📌 **Cómo leerlo:**

* **Blancas (W)** avanzan de **24 → 1**.
* **Negras (B)** avanzan de **1 → 24**.
* `2W` = 2 fichas blancas, `5B` = 5 fichas negras.
* `Barra` = fichas que fueron comidas (*hit*) y esperan para volver a entrar.
* `Off` = fichas que ya salieron del tablero (ganar = 15).
* `.` = casilla vacía.

---

## 📊 Panel de ventajas

Después de cada turno aparece un resumen con la evaluación del estado actual del juego:

```
--- ESTADO DEL JUEGO ---
Ventaja clara: NEGRAS (más adelantadas).
Pips Blancas: 135 | Pips Negras: 110
Off Blancas: 0 | Off Negras: 0
Barra Blancas: 1 | Barra Negras: 0
--------------------------
```

📌 **Explicación:**

* **Ventaja clara / ligera / equilibrio** → Quién va mejor según la posición.
* **Pips** → Distancia total que aún tienen que recorrer tus fichas (menos = mejor).
* **Off** → Fichas sacadas.
* **Barra** → Fichas fuera por haber sido comidas.

---

## 🏆 Cómo ganar

✔ Saca todas tus fichas antes que la IA.
✔ Evita quedarte con fichas solas (*blots*), porque pueden ser comidas.
✔ Intenta formar muros de 2 o más fichas seguidas para bloquear al rival.
✔ Usa los dobles (ej. 6–6) para avanzar rápido.
✔ Recuerda: si tienes fichas en la **Barra**, debes reingresarlas antes de mover otras.

**El juego termina** cuando en el panel veas:

```
FIN DE LA PARTIDA — Gana: BLANCO
```

o

```
FIN DE LA PARTIDA — Gana: NEGRO
```

---

## 🎯 Resumen rápido para principiantes

1. **Mueve tus fichas hacia tu lado del tablero.**
2. **Come las fichas solitarias del rival cuando puedas.**
3. **Evita dejar fichas solitarias propias.**
4. **Construye bloqueos con 2+ fichas.**
5. **Saca todas tus fichas para ganar.**

---

✨ ¡Y ya puedes disfrutar Backgammon en tu consola contra una IA! ✨

---


