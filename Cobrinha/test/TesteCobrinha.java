import model.Cobrinha;
import model.Cobrinha.Dificuldade;
import model.Cobrinha.Direcao;
import model.Cobrinha.TipoComida;
import model.Recorde;

import java.awt.Point;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class TesteCobrinha {
    static int falhas = 0;

    static void check(boolean cond, String nome) {
        if (!cond) {
            falhas++;
            System.out.println("FALHOU " + nome);
        } else {
            System.out.println("OK     " + nome);
        }
    }

    // erros de invariante são acumulados (um resumo por teste) para não inundar a saída
    static final List<String> violacoes = new ArrayList<>();

    static void viola(String msg) {
        if (violacoes.size() < 5) violacoes.add(msg);
        else if (violacoes.size() == 5) violacoes.add("...");
    }

    static void fechar(String nome) {
        check(violacoes.isEmpty(), nome + (violacoes.isEmpty() ? "" : "  -> " + violacoes));
        violacoes.clear();
    }

    static Direcao rumoAte(Cobrinha c, Point alvo) {
        Point h = c.getCorpo().peekFirst();
        Direcao d = alvo.x > h.x ? Direcao.DIREITA : alvo.x < h.x ? Direcao.ESQUERDA
                : alvo.y > h.y ? Direcao.BAIXO : Direcao.CIMA;
        return d.oposta(c.getDirecao()) ? Direcao.CIMA.oposta(c.getDirecao()) ? Direcao.BAIXO : Direcao.CIMA : d;
    }

    static boolean letal(Cobrinha c, Point p) {
        if (p.x < 0 || p.y < 0 || p.x >= c.getColunas() || p.y >= c.getLinhas()) return true;
        if (c.getObstaculos().contains(p)) return true;
        Point cauda = c.getCorpo().peekLast();
        for (Point q : c.getCorpo()) {
            if (q.equals(p) && !(q == cauda && !p.equals(c.getComida()) && !p.equals(c.getBonus()))) return true;
        }
        return false;
    }

    /** Bot "seguro": vai atrás da comida (ou do bônus) escolhendo sempre um passo que não mata. */
    static Direcao botSeguro(Cobrinha c) {
        Point h = c.getCorpo().peekFirst();
        Point alvo = c.getBonus() != null ? c.getBonus() : c.getComida();
        Direcao melhor = c.getDirecao();
        int melhorDist = Integer.MAX_VALUE;
        for (Direcao d : Direcao.values()) {
            if (d.oposta(c.getDirecao())) continue;
            Point n = new Point(h.x + d.dx, h.y + d.dy);
            if (letal(c, n)) continue;
            int dist = Math.abs(n.x - alvo.x) + Math.abs(n.y - alvo.y);
            if (dist < melhorDist) {
                melhorDist = dist;
                melhor = d;
            }
        }
        return melhor;
    }

    static void invariantes(Cobrinha c, String ctx) {
        Set<Point> corpo = new HashSet<>(c.getCorpo());
        if (corpo.size() != c.getCorpo().size() && !c.isFimDeJogo()) viola(ctx + ": corpo se sobrepõe");
        if (c.isFimDeJogo()) return;

        for (Point o : c.getObstaculos()) {
            if (corpo.contains(o)) viola(ctx + ": obstáculo sobre a cobra " + o);
            if (o.x < 1 || o.y < 1 || o.x > c.getColunas() - 2 || o.y > c.getLinhas() - 2)
                viola(ctx + ": obstáculo na borda " + o);
            for (Point o2 : c.getObstaculos()) {
                if (o != o2 && Math.abs(o.x - o2.x) <= 1 && Math.abs(o.y - o2.y) <= 1)
                    viola(ctx + ": obstáculos encostados " + o + " " + o2);
            }
        }
        Point f = c.getComida();
        if (f == null || corpo.contains(f) || c.getObstaculos().contains(f)) viola(ctx + ": comida inválida " + f);
        Point b = c.getBonus();
        if (b != null) {
            if (corpo.contains(b) || c.getObstaculos().contains(b) || b.equals(f)) viola(ctx + ": bônus inválido " + b);
            if (c.getTipoBonus() == null || c.getTicksBonus() <= 0 || c.getTicksBonus() > Cobrinha.DURACAO_BONUS)
                viola(ctx + ": estado do bônus inconsistente");
        } else if (c.getTipoBonus() != null) {
            viola(ctx + ": tipo de bônus sem bônus");
        }
        if (c.getObstaculos().size() > Cobrinha.MAX_OBSTACULOS) viola(ctx + ": obstáculos demais");
    }

    public static void main(String[] a) throws Exception {
        testesBasicos();
        testeSimulacaoSegura();
        testeObstaculoMata();
        testeDificuldades();
        testeRecorde();

        System.out.println(falhas == 0 ? "\nTodos os testes passaram." : "\n" + falhas + " falha(s).");
        System.exit(falhas == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ testes

    static void testesBasicos() {
        Cobrinha c = new Cobrinha(10, 10, new Random(1));
        Point cab = new Point(c.getCorpo().peekFirst());
        c.mover(null);
        check(c.getCorpo().peekFirst().x == cab.x + 1 && c.getCorpo().size() == 3, "anda para a direita sem crescer");

        c.mover(Direcao.ESQUERDA);
        check(!c.isFimDeJogo() && c.getDirecao() == Direcao.DIREITA, "ignora virada de 180 graus");

        Cobrinha p = new Cobrinha(10, 10, new Random(1));
        for (int i = 0; i < 10; i++) p.mover(null);
        check(p.isFimDeJogo(), "bater na parede termina o jogo");

        Cobrinha m = new Cobrinha(10, 10, new Random(2));
        int tamanho = m.getCorpo().size();
        for (int i = 0; i < 80 && !m.isFimDeJogo() && m.getComidasComidas() == 0; i++) {
            m.mover(botSeguro(m));
        }
        check(m.getComidasComidas() == 1 && m.getCorpo().size() == tamanho + 1, "comer cresce 1 célula");
        check(m.getPontos() >= 1, "comer soma pontos");
        check(!m.getCorpo().contains(m.getComida()), "nova comida não nasce sobre a cobra");

        // morder o próprio corpo: cobra de tamanho 6 e giro horário de 4 curvas volta à própria célula
        Cobrinha s = new Cobrinha(20, 20, Dificuldade.FACIL, new Random(3));
        while (s.getCorpo().size() < 6 && !s.isFimDeJogo()) s.mover(botSeguro(s));
        check(!s.isFimDeJogo() && s.getCorpo().size() >= 6, "cobra de tamanho 6 pronta para o laço");
        Direcao[] horario = {Direcao.CIMA, Direcao.DIREITA, Direcao.BAIXO, Direcao.ESQUERDA};
        int i = Arrays.asList(horario).indexOf(s.getDirecao());
        for (int k = 1; k <= 4; k++) s.mover(horario[(i + k) % 4]);
        check(s.isFimDeJogo(), "morder o próprio corpo termina o jogo");

        Cobrinha q = new Cobrinha(4, 4, new Random(4));
        check(!q.isFimDeJogo() && q.getObstaculos().isEmpty(), "tabuleiro pequeno começa válido e sem obstáculos");

        Cobrinha r = new Cobrinha(10, 10, Dificuldade.DIFICIL, new Random(5));
        for (int k = 0; k < 30 && !r.isFimDeJogo(); k++) r.mover(botSeguro(r));
        r.reiniciar();
        check(r.getPontos() == 0 && r.getObstaculos().isEmpty() && r.getBonus() == null
                && r.getTicksLentidao() == 0 && !r.isFimDeJogo() && r.getCorpo().size() == 3,
                "reiniciar limpa pontos, obstáculos, bônus e efeitos");
    }

    /** Joga centenas de partidas com um bot e confere as regras de pontos, bônus e obstáculos. */
    static void testeSimulacaoSegura() {
        int ouro = 0, lentidao = 0, obstaculosMax = 0, partidasComObstaculo = 0;
        int comidas = 0;

        for (Dificuldade dif : Dificuldade.values()) {
            for (int seed = 1; seed <= 150; seed++) {
                Cobrinha c = new Cobrinha(24, 24, dif, new Random(seed));
                int idadeBonus = 0;
                Point bonusAnterior = null;

                for (int t = 0; t < 3000 && !c.isFimDeJogo(); t++) {
                    String ctx = dif + "#" + seed + " t" + t;
                    Point prevComida = c.getComida();
                    Point prevBonus = c.getBonus();
                    TipoComida prevTipo = c.getTipoBonus();
                    int prevPontos = c.getPontos();
                    Set<Point> prevObs = new HashSet<>(c.getObstaculos());

                    c.mover(botSeguro(c));
                    if (c.isFimDeJogo()) break;

                    Point cabeca = c.getCorpo().peekFirst();
                    int delta = c.getPontos() - prevPontos;
                    if (cabeca.equals(prevBonus)) {
                        if (delta != prevTipo.pontos) viola(ctx + ": bônus " + prevTipo + " deu " + delta + " pontos");
                        if (prevTipo == TipoComida.DOURADA) ouro++;
                        else {
                            lentidao++;
                            if (c.getTicksLentidao() != Cobrinha.DURACAO_LENTIDAO)
                                viola(ctx + ": lentidão não ativou com duração cheia");
                        }
                        if (c.getBonus() != null && c.getBonus().equals(prevBonus)) viola(ctx + ": bônus não foi consumido");
                    } else if (cabeca.equals(prevComida)) {
                        comidas++;
                        if (delta != 1) viola(ctx + ": comida normal deu " + delta);
                    } else if (delta != 0) {
                        viola(ctx + ": pontos mudaram sem comer (" + delta + ")");
                    }

                    // obstáculos novos respeitam a distância mínima da cabeça
                    for (Point o : c.getObstaculos()) {
                        if (!prevObs.contains(o)
                                && Math.abs(o.x - cabeca.x) + Math.abs(o.y - cabeca.y) < Cobrinha.DISTANCIA_MIN_OBSTACULO)
                            viola(ctx + ": obstáculo nasceu perto da cabeça " + o);
                    }

                    // o bônus nunca passa de DURACAO_BONUS ticks no tabuleiro
                    if (c.getBonus() != null) {
                        idadeBonus = c.getBonus().equals(bonusAnterior) ? idadeBonus + 1 : 1;
                        if (idadeBonus > Cobrinha.DURACAO_BONUS) viola(ctx + ": bônus durou " + idadeBonus + " ticks");
                    } else {
                        idadeBonus = 0;
                    }
                    bonusAnterior = c.getBonus();

                    invariantes(c, ctx);
                }

                obstaculosMax = Math.max(obstaculosMax, c.getObstaculos().size());
                if (!c.getObstaculos().isEmpty()) partidasComObstaculo++;
                int esperado = Math.min(Cobrinha.MAX_OBSTACULOS, c.getComidasComidas() / dif.comidasPorObstaculo);
                if (c.getObstaculos().size() > esperado)
                    viola(dif + "#" + seed + ": mais obstáculos (" + c.getObstaculos().size() + ") que o esperado (" + esperado + ")");
            }
        }
        fechar("simulação (450 partidas): pontos, bônus, lentidão, validade de comida e obstáculos");
        System.out.println("       (comidas=" + comidas + ", douradas=" + ouro + ", lentidão=" + lentidao
                + ", partidas com obstáculo=" + partidasComObstaculo + ", máx. obstáculos=" + obstaculosMax + ")");
        check(ouro > 0, "comida dourada aparece e é comida durante as simulações");
        check(lentidao > 0, "comida de lentidão aparece e é comida durante as simulações");
        check(partidasComObstaculo > 0, "obstáculos surgem conforme a cobra come");
    }

    /** Andando ao acaso, toda vez que o próximo passo cai num obstáculo o jogo tem que acabar. */
    static void testeObstaculoMata() {
        int batidas = 0;
        for (int seed = 1; seed <= 300; seed++) {
            Cobrinha c = new Cobrinha(24, 24, Dificuldade.DIFICIL, new Random(seed));
            Random rnd = new Random(seed * 31L);
            for (int t = 0; t < 2000 && !c.isFimDeJogo(); t++) {
                // o bot busca comida para gerar obstáculos, mas às vezes vai direto ao obstáculo mais próximo
                Direcao d = botSeguro(c);
                if (!c.getObstaculos().isEmpty() && rnd.nextInt(100) < 15) {
                    Point h = c.getCorpo().peekFirst();
                    Point alvo = c.getObstaculos().iterator().next();
                    d = rumoAte(c, alvo);
                }
                Point h = c.getCorpo().peekFirst();
                Direcao efetiva = d.oposta(c.getDirecao()) ? c.getDirecao() : d;
                Point proximo = new Point(h.x + efetiva.dx, h.y + efetiva.dy);
                boolean vaiBater = c.getObstaculos().contains(proximo);
                c.mover(d);
                if (vaiBater) {
                    batidas++;
                    if (!c.isFimDeJogo()) viola("seed " + seed + ": passou por cima do obstáculo " + proximo);
                    break;
                }
            }
        }
        fechar("bater em obstáculo encerra o jogo");
        check(batidas > 0, "o teste realmente provocou batidas em obstáculos (" + batidas + ")");
    }

    static void testeDificuldades() {
        Dificuldade f = Dificuldade.FACIL, m = Dificuldade.MEDIO, d = Dificuldade.DIFICIL;
        check(f.atrasoInicial > m.atrasoInicial && m.atrasoInicial > d.atrasoInicial, "dificuldade maior = jogo mais rápido");
        check(f.comidasPorObstaculo > m.comidasPorObstaculo && m.comidasPorObstaculo > d.comidasPorObstaculo,
                "dificuldade maior = obstáculos mais frequentes");
        for (Dificuldade x : Dificuldade.values())
            check(x.atrasoMinimo < x.atrasoInicial, "atraso mínimo menor que o inicial em " + x.nome);
    }

    static void testeRecorde() throws Exception {
        Path pasta = Files.createTempDirectory("cobrinha-teste");
        Path arq = pasta.resolve("sub/recordes.properties");

        Recorde r = new Recorde(arq);
        check(r.get(Dificuldade.FACIL) == 0, "recorde começa em zero sem arquivo");
        check(r.registrar(Dificuldade.FACIL, 7), "primeiro placar positivo é recorde");
        check(!r.registrar(Dificuldade.FACIL, 7) && !r.registrar(Dificuldade.FACIL, 3), "placar igual ou menor não é recorde");
        check(r.registrar(Dificuldade.DIFICIL, 12), "recorde de outra dificuldade é independente");
        check(!r.registrar(Dificuldade.MEDIO, 0), "zero pontos não conta como recorde");

        Recorde r2 = new Recorde(arq);
        check(r2.get(Dificuldade.FACIL) == 7 && r2.get(Dificuldade.DIFICIL) == 12 && r2.get(Dificuldade.MEDIO) == 0,
                "recordes sobrevivem a reabrir o arquivo");

        Files.writeString(arq, "FACIL=abc\nMEDIO=-5\nDIFICIL=9\n");
        Recorde r3 = new Recorde(arq);
        check(r3.get(Dificuldade.FACIL) == 0 && r3.get(Dificuldade.MEDIO) == 0 && r3.get(Dificuldade.DIFICIL) == 9,
                "valores inválidos no arquivo viram zero sem quebrar");

        Files.write(arq, new byte[]{'\\', 'u', 'Z', 'Z', 'Z', 'Z'});
        Recorde r4 = new Recorde(arq);
        check(r4.get(Dificuldade.FACIL) == 0 && r4.registrar(Dificuldade.FACIL, 2), "arquivo corrompido não derruba o jogo");
    }
}
