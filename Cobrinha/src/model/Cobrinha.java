package model;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Lógica do jogo da cobrinha, sem nenhuma dependência de interface gráfica.
 * O tabuleiro é uma grade de colunas x linhas; a cobra anda uma célula por tick.
 *
 * Elementos do jogo:
 *  - comida normal: +1 ponto, a cobra cresce;
 *  - comida dourada (bônus): +3 pontos, some depois de alguns ticks;
 *  - comida de lentidão (bônus): +1 ponto e deixa o jogo mais lento por um tempo;
 *  - obstáculos: blocos fixos que aparecem conforme a cobra come. Bater neles encerra o jogo.
 */
public class Cobrinha {

    public enum Direcao {
        CIMA(0, -1), BAIXO(0, 1), ESQUERDA(-1, 0), DIREITA(1, 0);

        public final int dx;
        public final int dy;

        Direcao(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        public boolean oposta(Direcao outra) {
            return dx + outra.dx == 0 && dy + outra.dy == 0;
        }
    }

    /** Ajustes de cada nível. Os atrasos são em milissegundos entre um passo e outro. */
    public enum Dificuldade {
        FACIL("Fácil", "Mais devagar, poucos obstáculos", 160, 90, 6, 30),
        MEDIO("Médio", "Ritmo normal, obstáculos moderados", 130, 60, 4, 22),
        DIFICIL("Difícil", "Rápido, muitos obstáculos", 100, 45, 3, 15);

        public final String nome;
        public final String descricao;
        public final int atrasoInicial;
        public final int atrasoMinimo;
        /** A cada quantas comidas surge um novo obstáculo. */
        public final int comidasPorObstaculo;
        /** Chance (em %) de surgir uma comida bônus ao comer uma comida normal. */
        public final int chanceBonus;

        Dificuldade(String nome, String descricao, int atrasoInicial, int atrasoMinimo,
                    int comidasPorObstaculo, int chanceBonus) {
            this.nome = nome;
            this.descricao = descricao;
            this.atrasoInicial = atrasoInicial;
            this.atrasoMinimo = atrasoMinimo;
            this.comidasPorObstaculo = comidasPorObstaculo;
            this.chanceBonus = chanceBonus;
        }
    }

    public enum TipoComida {
        NORMAL(1), DOURADA(3), LENTIDAO(1);

        public final int pontos;

        TipoComida(int pontos) {
            this.pontos = pontos;
        }
    }

    /** Quantos ticks uma comida bônus fica no tabuleiro antes de sumir. */
    public static final int DURACAO_BONUS = 60;
    /** Quantos ticks dura o efeito da comida de lentidão. */
    public static final int DURACAO_LENTIDAO = 40;
    /** Limite de obstáculos no tabuleiro, para o jogo continuar possível. */
    public static final int MAX_OBSTACULOS = 20;
    /** Obstáculos nunca nascem a menos de N células (distância Manhattan) da cabeça. */
    public static final int DISTANCIA_MIN_OBSTACULO = 5;
    /** A comida bônus nasce a no máximo N células da cabeça, para dar tempo de chegar. */
    private static final int DISTANCIA_MAX_BONUS = 22;
    private static final int DISTANCIA_MIN_BONUS = 4;

    private final int colunas;
    private final int linhas;
    private final Random random;
    private final Dificuldade dificuldade;

    // primeiro elemento = cabeça
    private final Deque<Point> corpo = new ArrayDeque<>();
    private final Set<Point> obstaculos = new LinkedHashSet<>();
    private Direcao direcao;
    private Point comida;
    private Point bonus;
    private TipoComida tipoBonus;
    private int ticksBonus;
    private int ticksLentidao;
    private int pontos;
    private int comidasComidas;
    private boolean fimDeJogo;
    private boolean venceu;

    public Cobrinha(int colunas, int linhas) {
        this(colunas, linhas, Dificuldade.MEDIO, new Random());
    }

    public Cobrinha(int colunas, int linhas, Random random) {
        this(colunas, linhas, Dificuldade.MEDIO, random);
    }

    public Cobrinha(int colunas, int linhas, Dificuldade dificuldade) {
        this(colunas, linhas, dificuldade, new Random());
    }

    public Cobrinha(int colunas, int linhas, Dificuldade dificuldade, Random random) {
        this.colunas = colunas;
        this.linhas = linhas;
        this.dificuldade = dificuldade;
        this.random = random;
        reiniciar();
    }

    public void reiniciar() {
        corpo.clear();
        obstaculos.clear();
        int x = colunas / 2;
        int y = linhas / 2;
        // cobra começa com 3 células, andando para a direita
        corpo.addLast(new Point(x, y));
        corpo.addLast(new Point(x - 1, y));
        corpo.addLast(new Point(x - 2, y));
        direcao = Direcao.DIREITA;
        pontos = 0;
        comidasComidas = 0;
        ticksLentidao = 0;
        fimDeJogo = false;
        venceu = false;
        comida = null;
        removerBonus();
        sortearComida();
    }

    /**
     * Avança a cobra uma célula na direção informada.
     * Ignora tentativas de virar 180 graus (a cobra não pode dar ré).
     */
    public void mover(Direcao nova) {
        if (fimDeJogo) {
            return;
        }
        if (nova != null && !nova.oposta(direcao)) {
            direcao = nova;
        }

        Point cabeca = corpo.peekFirst();
        Point novaCabeca = new Point(cabeca.x + direcao.dx, cabeca.y + direcao.dy);

        boolean comeuComida = novaCabeca.equals(comida);
        boolean comeuBonus = novaCabeca.equals(bonus);

        // a cauda sai do lugar neste mesmo tick (a menos que a cobra coma),
        // então encostar na posição atual da cauda não é colisão
        if (!comeuComida && !comeuBonus) {
            corpo.removeLast();
        }

        if (bateuNaParede(novaCabeca) || corpo.contains(novaCabeca) || obstaculos.contains(novaCabeca)) {
            fimDeJogo = true;
            return;
        }

        corpo.addFirst(novaCabeca);

        // passagem do tempo para os efeitos
        if (ticksLentidao > 0) {
            ticksLentidao--;
        }
        if (bonus != null && !comeuBonus && --ticksBonus <= 0) {
            removerBonus();
        }

        if (comeuBonus) {
            pontos += tipoBonus.pontos;
            if (tipoBonus == TipoComida.LENTIDAO) {
                ticksLentidao = DURACAO_LENTIDAO;
            }
            comidasComidas++;
            removerBonus();
        }

        if (comeuComida) {
            pontos += TipoComida.NORMAL.pontos;
            comidasComidas++;
            if (!sortearComida()) {
                venceu = true; // não há mais onde colocar comida: zerou o jogo
                fimDeJogo = true;
                return;
            }
            if (bonus == null && random.nextInt(100) < dificuldade.chanceBonus) {
                sortearBonus();
            }
        }

        if (comeuComida || comeuBonus) {
            gerarObstaculosPendentes();
        }
    }

    private boolean bateuNaParede(Point p) {
        return p.x < 0 || p.y < 0 || p.x >= colunas || p.y >= linhas;
    }

    /** Todas as células sem cobra, obstáculo ou comida. */
    private List<Point> celulasLivres() {
        Set<Point> ocupadas = new HashSet<>(corpo);
        ocupadas.addAll(obstaculos);
        if (comida != null) ocupadas.add(comida);
        if (bonus != null) ocupadas.add(bonus);

        List<Point> livres = new ArrayList<>();
        for (int x = 0; x < colunas; x++) {
            for (int y = 0; y < linhas; y++) {
                Point p = new Point(x, y);
                if (!ocupadas.contains(p)) {
                    livres.add(p);
                }
            }
        }
        return livres;
    }

    private boolean sortearComida() {
        comida = null;
        List<Point> livres = celulasLivres();
        if (livres.isEmpty() && bonus != null) {
            removerBonus();
            livres = celulasLivres();
        }
        if (livres.isEmpty()) {
            return false;
        }
        comida = livres.get(random.nextInt(livres.size()));
        return true;
    }

    private void sortearBonus() {
        Point cabeca = corpo.peekFirst();
        List<Point> candidatos = new ArrayList<>();
        for (Point p : celulasLivres()) {
            int d = distancia(p, cabeca);
            if (d >= DISTANCIA_MIN_BONUS && d <= DISTANCIA_MAX_BONUS) {
                candidatos.add(p);
            }
        }
        if (candidatos.isEmpty()) {
            return;
        }
        bonus = candidatos.get(random.nextInt(candidatos.size()));
        tipoBonus = random.nextInt(100) < 60 ? TipoComida.DOURADA : TipoComida.LENTIDAO;
        ticksBonus = DURACAO_BONUS;
    }

    private void removerBonus() {
        bonus = null;
        tipoBonus = null;
        ticksBonus = 0;
    }

    private void gerarObstaculosPendentes() {
        int alvo = Math.min(MAX_OBSTACULOS, comidasComidas / dificuldade.comidasPorObstaculo);
        while (obstaculos.size() < alvo) {
            if (!adicionarObstaculo()) {
                break;
            }
        }
    }

    /**
     * Coloca um obstáculo em uma célula que não deixa o jogo injusto ou impossível:
     * fora das bordas, longe da cabeça e sem encostar (nem na diagonal) em outro obstáculo.
     * Assim os blocos ficam sempre isolados e nunca fecham uma região do tabuleiro.
     */
    private boolean adicionarObstaculo() {
        Point cabeca = corpo.peekFirst();
        List<Point> candidatos = new ArrayList<>();
        for (Point p : celulasLivres()) {
            boolean naBorda = p.x == 0 || p.y == 0 || p.x == colunas - 1 || p.y == linhas - 1;
            if (naBorda || distancia(p, cabeca) < DISTANCIA_MIN_OBSTACULO) {
                continue;
            }
            if (temObstaculoVizinho(p)) {
                continue;
            }
            candidatos.add(p);
        }
        if (candidatos.isEmpty()) {
            return false;
        }
        obstaculos.add(candidatos.get(random.nextInt(candidatos.size())));
        return true;
    }

    private boolean temObstaculoVizinho(Point p) {
        for (Point o : obstaculos) {
            if (Math.abs(o.x - p.x) <= 1 && Math.abs(o.y - p.y) <= 1) {
                return true;
            }
        }
        return false;
    }

    private static int distancia(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    public int getColunas() { return colunas; }
    public int getLinhas() { return linhas; }
    public Dificuldade getDificuldade() { return dificuldade; }
    public Deque<Point> getCorpo() { return corpo; }
    public Set<Point> getObstaculos() { return Collections.unmodifiableSet(obstaculos); }
    public Direcao getDirecao() { return direcao; }
    public Point getComida() { return comida; }
    public Point getBonus() { return bonus; }
    public TipoComida getTipoBonus() { return tipoBonus; }
    public int getTicksBonus() { return ticksBonus; }
    public int getTicksLentidao() { return ticksLentidao; }
    public int getPontos() { return pontos; }
    public int getComidasComidas() { return comidasComidas; }
    public boolean isFimDeJogo() { return fimDeJogo; }
    public boolean isVenceu() { return venceu; }
}
