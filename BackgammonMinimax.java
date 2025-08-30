import java.util.*;

public class BackgammonMinimax {

    // === Modelado de jugadores ===

    enum Player {
        WHITE(1, "BLANCO"), BLACK(-1, "NEGRO");

        final int sign; // BLANCO: +, NEGRO: - para points[]
        final String label;

        Player(int s, String l) {
            sign = s;
            label = l;
        }

        Player opponent() {
            return this == WHITE ? BLACK : WHITE;
        }

        int dir() {
            return this == WHITE ? -1 : +1;
        } // dirección de avance en índices

        int homeStart() {
            return this == WHITE ? 0 : 18;
        } // inicio del tablero interno (6 puntos)

        int homeEnd() {
            return this == WHITE ? 5 : 23;
        } // fin (inclusive)

        boolean inHome(int idx) {
            return idx >= homeStart() && idx <= homeEnd();
        }

        int entryPoint(int die) {
            // desde barra: BLANCO entra en 24-d (23..18), NEGRO en d-1 (0..5)
            return this == WHITE ? 24 - die : die - 1;
        }
    }

    // === Estado del juego ===
    static class GameState implements Cloneable {
        int[] points = new int[24]; // positivo = blancas, negativo = negras; abs = cantidad
        int barWhite = 0, barBlack = 0;
        int offWhite = 0, offBlack = 0;

        GameState() {
        }

        GameState cloneState() {
            GameState g = new GameState();
            g.points = Arrays.copyOf(points, points.length);
            g.barWhite = barWhite;
            g.barBlack = barBlack;
            g.offWhite = offWhite;
            g.offBlack = offBlack;
            return g;
        }

        int bar(Player p) {
            return p == Player.WHITE ? barWhite : barBlack;
        }

        void incBar(Player p) {
            if (p == Player.WHITE)
                barWhite++;
            else
                barBlack++;
        }

        void decBar(Player p) {
            if (p == Player.WHITE)
                barWhite--;
            else
                barBlack--;
        }

        int off(Player p) {
            return p == Player.WHITE ? offWhite : offBlack;
        }

        void incOff(Player p) {
            if (p == Player.WHITE)
                offWhite++;
            else
                offBlack++;
        }

        boolean isTerminal() {
            return offWhite >= 15 || offBlack >= 15;
        }

        Player winner() {
            return offWhite >= 15 ? Player.WHITE : (offBlack >= 15 ? Player.BLACK : null);
        }

        static GameState initial() {
            GameState g = new GameState();
            // Posición estándar (desde la perspectiva BLANCO 23->0)
            // BLANCO: 24:2, 13:5, 8:3, 6:5 => idx 23:2, 12:5, 7:3, 5:5
            // NEGRO (espejo): 1:2, 12:5, 17:3, 19:5 => idx 0:-2, 11:-5, 16:-3, 18:-5
            g.points[23] = +2;
            g.points[12] = +5;
            g.points[7] = +3;
            g.points[5] = +5;

            g.points[0] = -2;
            g.points[11] = -5;
            g.points[16] = -3;
            g.points[18] = -5;
            return g;
        }

        int pipCount(Player p) {
            // suma de distancia total a borne off
            int sum = 0;
            if (p == Player.WHITE) {
                for (int i = 0; i < 24; i++)
                    if (points[i] > 0)
                        sum += points[i] * (i + 1); // idx 0 -> 1 pip
                sum += barWhite * 25; // desde barra al 24-point
            } else {
                for (int i = 0; i < 24; i++)
                    if (points[i] < 0)
                        sum += (-points[i]) * (24 - i); // idx 23 -> 1 pip
                sum += barBlack * 25;
            }
            return sum;
        }

        int blots(Player p) {
            int c = 0;
            for (int i = 0; i < 24; i++) {
                int v = points[i];
                if (p == Player.WHITE && v == 1)
                    c++;
                if (p == Player.BLACK && v == -1)
                    c++;
            }
            return c;
        }

        int primes(Player p) {
            // Cuenta puntos consecutivos bloqueados (>=2 fichas propias). Heurística
            // simple.
            int count = 0, best = 0;
            for (int i = 0; i < 24; i++) {
                int v = points[i] * p.sign;
                if (v >= 2) {
                    count++;
                    best = Math.max(best, count);
                } else
                    count = 0;
            }
            return best;
        }
    }

    // === Movimientos ===
    static class Move {
        // from: -1 => desde barra; to: -2 => bear off
        int from, to, die;
        boolean hits;

        Move(int from, int to, int die, boolean hits) {
            this.from = from;
            this.to = to;
            this.die = die;
            this.hits = hits;
        }

        public String toString() {
            String sfrom = (from < 0 ? "BAR" : idxToPoint(from));
            String sto = (to == -2 ? "OFF" : idxToPoint(to));
            return sfrom + " -> " + sto + " (" + die + ")" + (hits ? " *hit" : "");
        }
    }

    static class MoveSeq {
        List<Move> steps = new ArrayList<>();

        void add(Move m) {
            steps.add(m);
        }

        int size() {
            return steps.size();
        }

        public String toString() {
            if (steps.isEmpty())
                return "(paso)";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < steps.size(); i++) {
                if (i > 0)
                    sb.append("; ");
                sb.append(steps.get(i));
            }
            return sb.toString();
        }
    }

    // === Generación de sucesores ===
    static class MoveGenerator {
        static List<MoveSeq> generateAll(GameState s, Player p, int[] dice) {
            // Expande con ambas órdenes si no es doble
            List<MoveSeq> res = new ArrayList<>();
            if (dice.length == 2 && dice[0] != dice[1]) {
                res.addAll(generateForOrder(s, p, new int[] { dice[0], dice[1] }));
                res.addAll(generateForOrder(s, p, new int[] { dice[1], dice[0] }));
            } else {
                int count = (dice[0] == dice[1] ? 4 : 2);
                int[] seq = new int[count];
                Arrays.fill(seq, dice[0]); // si no doble con tamaño 2, fill con d0 y d1? lo cubre rama de arriba
                if (dice.length == 2 && dice[0] == dice[1])
                    res.addAll(generateForOrder(s, p, seq));
                else if (dice.length == 2)
                    res.addAll(generateForOrder(s, p, dice));
                else
                    res.addAll(generateForOrder(s, p, seq));
            }
            // Mantener solo las que usan el máximo número de dados posible
            int best = 0;
            for (MoveSeq ms : res)
                best = Math.max(best, ms.size());
            List<MoveSeq> filtered = new ArrayList<>();
            for (MoveSeq ms : res)
                if (ms.size() == best)
                    filtered.add(ms);
            // Eliminar duplicados por firma (estado final)
            return dedupByFinalState(s, p, filtered);
        }

        private static List<MoveSeq> generateForOrder(GameState s, Player p, int[] diceOrder) {
            List<MoveSeq> out = new ArrayList<>();
            backtrack(s, p, diceOrder, 0, new MoveSeq(), out);
            return out;
        }

        private static void backtrack(GameState s, Player p, int[] dice, int idx, MoveSeq cur, List<MoveSeq> out) {
            if (idx >= dice.length) {
                out.add(copySeq(cur));
                return;
            }
            int die = dice[idx];
            List<Move> moves = legalSingleDieMoves(s, p, die);
            if (moves.isEmpty()) {
                // no se puede usar este dado; paramos aquí (no añadimos paso vacío más allá)
                out.add(copySeq(cur));
                return;
            }
            for (Move m : moves) {
                GameState next = apply(s, p, m);
                cur.add(m);
                backtrack(next, p, dice, idx + 1, cur, out);
                cur.steps.remove(cur.steps.size() - 1);
            }
        }

        static List<Move> legalSingleDieMoves(GameState s, Player p, int die) {
            List<Move> list = new ArrayList<>();
            // Prioridad: si hay fichas en barra, solo entradas
            if (s.bar(p) > 0) {
                int dest = p.entryPoint(die);
                if (canLand(s, p, dest)) {
                    boolean hit = isBlot(s, p.opponent(), dest);
                    list.add(new Move(-1, dest, die, hit));
                }
                return list; // solo entradas posibles
            }
            // Movimientos desde el tablero
            // Borne off permitido si todas en casa
            boolean canBearOff = canBearOff(s, p);
            if (p == Player.WHITE) {
                for (int from = 23; from >= 0; from--) {
                    if (s.points[from] <= 0)
                        continue; // no blanca
                    int to = from - die;
                    if (to >= 0) {
                        if (canLand(s, p, to)) {
                            boolean hit = isBlot(s, p.opponent(), to);
                            list.add(new Move(from, to, die, hit));
                        }
                    } else if (canBearOff) {
                        // usar dado exacto o mayor desde el punto más alto ocupado
                        int needed = from + 1; // distancia a OFF
                        if (die == needed)
                            list.add(new Move(from, -2, die, false));
                        else if (die > needed && noHigherCheckers(s, p, from))
                            list.add(new Move(from, -2, die, false));
                    }
                }
            } else { // NEGRO
                for (int from = 0; from < 24; from++) {
                    if (s.points[from] >= 0)
                        continue; // no negra
                    int to = from + die;
                    if (to <= 23) {
                        if (canLand(s, p, to)) {
                            boolean hit = isBlot(s, p.opponent(), to);
                            list.add(new Move(from, to, die, hit));
                        }
                    } else if (canBearOff) {
                        int needed = 24 - from; // distancia a OFF para negro
                        if (die == needed)
                            list.add(new Move(from, -2, die, false));
                        else if (die > needed && noHigherCheckers(s, p, from))
                            list.add(new Move(from, -2, die, false));
                    }
                }
            }
            return list;
        }

        private static boolean canLand(GameState s, Player p, int dest) {
            int v = s.points[dest];
            if (p == Player.WHITE) {
                return v >= -1; // bloqueado si <= -2 (dos o más negras)
            } else {
                return v <= 1; // bloqueado si >= 2 (dos o más blancas)
            }
        }

        private static boolean isBlot(GameState s, Player opp, int dest) {
            int v = s.points[dest];
            return (opp == Player.WHITE && v == 1) || (opp == Player.BLACK && v == -1);
        }

        private static boolean canBearOff(GameState s, Player p) {
            if (s.bar(p) > 0)
                return false;
            if (p == Player.WHITE) {
                for (int i = 6; i < 24; i++)
                    if (s.points[i] > 0)
                        return false;
                return true;
            } else {
                for (int i = 0; i < 18; i++)
                    if (s.points[i] < 0)
                        return false;
                return true;
            }
        }

        private static boolean noHigherCheckers(GameState s, Player p, int from) {
            // ¿hay fichas propias en puntos "más alejados" dentro de casa?
            if (p == Player.WHITE) {
                for (int i = from + 1; i <= 5; i++)
                    if (s.points[i] > 0)
                        return false;
            } else {
                for (int i = from - 1; i >= 18; i--)
                    if (s.points[i] < 0)
                        return false;
            }
            return true;
        }

        static GameState apply(GameState s, Player p, Move m) {
            GameState g = s.cloneState();
            if (m.from == -1) {
                g.decBar(p); // salir de barra consume una ficha de la barra
            } else {
                g.points[m.from] -= p.sign; // quita ficha propia del origen
            }
            if (m.to == -2) {
                g.incOff(p); // borne off: incrementa fichas fuera
                return g;
            }
            // hit si había blot rival (exactamente 1 del rival)
            if (isBlot(s, p.opponent(), m.to)) {
                // manda rival a barra (y limpia el punto antes de colocar la nuestra)
                if (p == Player.WHITE) {
                    g.points[m.to] = 0;
                    g.incBar(Player.BLACK);
                } else {
                    g.points[m.to] = 0;
                    g.incBar(Player.WHITE);
                }
            }
            // coloca ficha propia en destino
            g.points[m.to] += p.sign;
            return g;
        }

        private static MoveSeq copySeq(MoveSeq ms) {
            MoveSeq c = new MoveSeq();
            c.steps.addAll(ms.steps);
            return c;
        }

        private static List<MoveSeq> dedupByFinalState(GameState s, Player p, List<MoveSeq> seqs) {
            // Dedupe por firma de estado final: evita listar jugadas distintas que acaban
            // igual
            Map<String, MoveSeq> seen = new LinkedHashMap<>();
            for (MoveSeq ms : seqs) {
                GameState g = s.cloneState();
                for (Move m : ms.steps)
                    g = apply(g, p, m);
                String sig = signature(g, p);
                seen.putIfAbsent(sig, ms);
            }
            return new ArrayList<>(seen.values());
        }

        private static String signature(GameState g, Player p) {
            return Arrays.toString(g.points) + "|b" + g.barWhite + "," + g.barBlack + "|o" + g.offWhite + ","
                    + g.offBlack + "|p" + p;
        }
    }

    // === Heurística ===
    static class Heuristic {
        static int evaluate(GameState s, Player pov) {
            // Cortes rápidos si alguien ya ganó (grandes constantes para priorizar mate)
            if (s.off(pov) >= 15)
                return 100000;
            if (s.off(pov.opponent()) >= 15)
                return -100000;
            int myPips = s.pipCount(pov);
            int opPips = s.pipCount(pov.opponent());
            int pipScore = (opPips - myPips); // menos pips = mejor

            int barPenalty = -25 * s.bar(pov) + 25 * s.bar(pov.opponent());
            int blotScore = -2 * s.blots(pov) + 2 * s.blots(pov.opponent());
            int primeScore = 3 * s.primes(pov) - 3 * s.primes(pov.opponent());
            int offScore = 5 * s.off(pov) - 5 * s.off(pov.opponent());
            return pipScore + barPenalty + blotScore + primeScore + offScore;
        }
    }

    // === Minimax profundidad 2 ===
    static class MinimaxAI {
        final Player me;

        MinimaxAI(Player me) {
            this.me = me;
        }

        static final int[][] ALL_ROLLS = allDicePairs();

        private static int[][] allDicePairs() {
            // Pares con a<=b para cubrir 15 combinaciones únicas (no pondera permutaciones)
            List<int[]> v = new ArrayList<>();
            for (int a = 1; a <= 6; a++)
                for (int b = a; b <= 6; b++)
                    v.add(new int[] { a, b });
            return v.toArray(new int[0][]);
        }

        MoveSeq choose(GameState s, int[] myDice) {
            List<MoveSeq> myMoves = MoveGenerator.generateAll(s, me, normalizeDice(myDice));
            if (myMoves.isEmpty())
                return new MoveSeq(); // pasar
            MoveSeq best = null;
            int bestVal = Integer.MIN_VALUE;
            for (MoveSeq mseq : myMoves) {
                GameState afterMine = applySeq(s, me, mseq);
                int worstReply = Integer.MAX_VALUE;
                // Rival responde con el peor dado posible para mí (conservador/adversarial)
                for (int[] oppDice : ALL_ROLLS) {
                    int val = bestReplyValue(afterMine, me.opponent(), oppDice);
                    if (val < worstReply)
                        worstReply = val;
                }
                if (worstReply > bestVal) {
                    bestVal = worstReply;
                    best = mseq;
                }
            }
            return best;
        }

        private int bestReplyValue(GameState s, Player opp, int[] dice) {
            List<MoveSeq> replies = MoveGenerator.generateAll(s, opp, normalizeDice(dice));
            if (replies.isEmpty())
                return Heuristic.evaluate(s, me); // rival pasa
            int best = Integer.MIN_VALUE;
            for (MoveSeq r : replies) {
                GameState after = applySeq(s, opp, r);
                int v = Heuristic.evaluate(after, me);
                if (v > best)
                    best = v;
            }
            return best;
        }
    }

    static GameState applySeq(GameState s, Player p, MoveSeq seq) {
        GameState g = s.cloneState();
        for (Move m : seq.steps)
            g = MoveGenerator.apply(g, p, m);
        return g;
    }

    // === Utilidades UI ===
    static Random RNG = new Random();

    static int[] roll() {
        return new int[] { RNG.nextInt(6) + 1, RNG.nextInt(6) + 1 };
    }

    static int[] normalizeDice(int[] d) {
        if (d.length == 2 && d[0] == d[1])
            return new int[] { d[0], d[0] }; // se tratará como doble (hasta 4 movimientos)
        return new int[] { d[0], d[1] };
    }

    // === NUEVO TABLERO ASCII MEJORADO ===
    // Muestra numeración de puntos, flechas de dirección y celdas alineadas.
    static void printBoard(GameState g) {
        System.out.println("\n============ TABLERO ============");
        System.out.println("BLANCAS: 24 -> 1   (<- hacia la izquierda)");
        System.out.println("NEGRAS : 1  -> 24  (-> hacia la derecha)");

        // Encabezado superior: números de punto (24..13)
        System.out.print("\n     ");
        for (int p = 24; p >= 13; p--)
            System.out.printf("%4s", p); // 12 columnas arriba

        // Fila TOP (índices 23..12) — se alinea con el encabezado
        System.out.print("\nTOP :");
        for (int i = 23; i >= 12; i--)
            System.out.printf("%4s", cell(g.points[i]));

        // Separador visual entre filas
        System.out.println();
        System.out.println("----+------------------------------------------------");

        // Fila BOT (índices 11..0) — representa puntos 12..1
        System.out.print("BOT :");
        for (int i = 11; i >= 0; i--)
            System.out.printf("%4s", cell(g.points[i]));

        // Pie con numeración inferior (12..1) alineada con BOT
        System.out.print("\n     ");
        for (int p = 12; p >= 1; p--)
            System.out.printf("%4s", p);

        // Estado de barra y borne-off
        System.out.printf("\n\nBarra: W=%d  B=%d   |   Off: W=%d  B=%d\n",
                g.barWhite, g.barBlack, g.offWhite, g.offBlack);

        System.out.println("Leyenda:  . = vacío | nW = n blancas | nB = n negras | Puntos: 24..1");
        System.out.println("===============================================");

        // === Panel de estado ===
        int eval = Heuristic.evaluate(g, Player.WHITE); // evalúa desde la perspectiva de Blancas
        System.out.println("\n--- ESTADO DEL JUEGO ---");
        if (eval > 50) {
            System.out.println("Ventaja clara: BLANCAS (más adelantadas).");
        } else if (eval > 0) {
            System.out.println("Ligeramente mejor: BLANCAS.");
        } else if (eval < -50) {
            System.out.println("Ventaja clara: NEGRAS (más adelantadas).");
        } else if (eval < 0) {
            System.out.println("Ligeramente mejor: NEGRAS.");
        } else {
            System.out.println("Juego equilibrado.");
        }
        System.out.println("Pips Blancas: " + g.pipCount(Player.WHITE) +
                " | Pips Negras: " + g.pipCount(Player.BLACK));
        System.out.println("Off Blancas: " + g.offWhite +
                " | Off Negras: " + g.offBlack);
        System.out.println("Barra Blancas: " + g.barWhite +
                " | Barra Negras: " + g.barBlack);
        System.out.println("--------------------------");

    }

    // Celda compacta y alineada a 3-4 caracteres.
    // v>0 => nW (n blancas); v<0 => nB (n negras); 0 => "."
    static String cell(int v) {
        if (v == 0)
            return " .";
        if (v > 0)
            return String.format("%2dW", v); // blancas
        return String.format("%2dB", -v); // negras
    }

    static String idxToPoint(int idx) {
        // Mostrar número de punto clásico desde la perspectiva BLANCO (24..1)
        return "P" + (idx + 1);
    }

    // === Loop principal ===
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        GameState g = GameState.initial();

        // Elección de color del humano (por defecto BLANCO con Enter)
        System.out.print("Elige tu color [B=Blanco / N=Negro] (por defecto B): ");
        String choiceColor = sc.nextLine().trim().toUpperCase();
        Player human = (choiceColor.startsWith("N")) ? Player.BLACK : Player.WHITE;
        Player aiP = human.opponent();
        MinimaxAI ai = new MinimaxAI(aiP);

        System.out.println("Backgammon (Minimax profundidad 2) — Humano=" + human.label + ", IA=" + aiP.label);
        printBoard(g);

        // Nota: por simplicidad, siempre empieza BLANCO (regla oficial: ambos tiran 1
        // dado y empieza el más alto).
        Player turn = Player.WHITE;

        while (!g.isTerminal()) {
            if (turn == human) {
                System.out.println("\nTu turno (" + human.label + "). Pulsa Enter para tirar dados...");
                try {
                    System.in.read();
                } catch (Exception ignored) {
                }
                int[] dice = roll();
                System.out.println("Dados: " + dice[0] + "-" + dice[1]);

                // Genera todas las secuencias legales que usan el máximo número de dados
                List<MoveSeq> moves = MoveGenerator.generateAll(g, human, dice);
                if (moves.isEmpty()) {
                    System.out.println("No tienes jugadas legales. Pasas turno.");
                } else {
                    // Se listan con índice para elegir fácilmente
                    for (int i = 0; i < moves.size(); i++) {
                        System.out.printf("[%d] %s\n", i, moves.get(i));
                    }
                    int choice = -1;
                    while (choice < 0 || choice >= moves.size()) {
                        System.out.print("Elige jugada por índice: ");
                        String line = sc.nextLine().trim();
                        try {
                            choice = Integer.parseInt(line);
                        } catch (Exception e) {
                            choice = -1;
                        }
                    }
                    g = applySeq(g, human, moves.get(choice));
                }
                printBoard(g);
                turn = aiP;
            } else {
                System.out.println("\nTurno IA (" + aiP.label + "). Tirando dados...");
                int[] dice = roll();
                System.out.println("IA obtuvo: " + dice[0] + "-" + dice[1]);

                // La IA elige la secuencia maximizando su resultado, asumiendo
                // que el rival tendrá la respuesta (tirada) más adversa.
                MoveSeq best = ai.choose(g, dice);
                if (best.steps.isEmpty()) {
                    System.out.println("IA no puede mover. Pasa turno.");
                } else {
                    System.out.println("IA juega: " + best);
                    g = applySeq(g, aiP, best);
                }
                printBoard(g);
                turn = human;
            }
            if (g.isTerminal())
                break;
        }
        Player w = g.winner();
        System.out.println("\nFIN DE LA PARTIDA — Gana: " + (w == null ? "(empate?)" : w.label));
    }
}
