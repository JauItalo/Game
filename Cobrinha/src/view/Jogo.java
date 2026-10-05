package view;

import model.Cobrinha;
import model.Cobrinha.Dificuldade;
import model.Cobrinha.Direcao;
import model.Cobrinha.TipoComida;
import model.Recorde;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tela do jogo da cobrinha (Swing).
 *
 * Menu: setas/WS ou 1-3 escolhem a dificuldade, ENTER começa.
 * Jogo: setas ou WASD movem, P pausa, ENTER/R reinicia ao perder, ESC volta ao menu.
 */
public class Jogo extends JPanel {

    private static final int COLUNAS = 24;
    private static final int LINHAS = 24;
    private static final int CELULA = 24;
    /** Quanto o atraso aumenta enquanto o efeito de lentidão está ativo. */
    private static final double FATOR_LENTIDAO = 1.7;

    private static final Color FUNDO = new Color(0x1B1F2A);
    private static final Color GRADE = new Color(0x232938);
    private static final Color CABECA = new Color(0x7CFC9A);
    private static final Color CORPO = new Color(0x3DBB63);
    private static final Color CORPO_LENTO = new Color(0x3DA5BB);
    private static final Color COMIDA = new Color(0xFF5C6C);
    private static final Color OURO = new Color(0xFFD24A);
    private static final Color GELO = new Color(0x5CC8FF);
    private static final Color OBSTACULO = new Color(0x6B7280);
    private static final Color OBSTACULO_BORDA = new Color(0x4B5563);

    private enum Tela { MENU, JOGO }

    private final Recorde recordes = Recorde.padrao();
    private final Timer timer;
    // fila de comandos: permite virar duas vezes rápido entre dois ticks
    private final Deque<Direcao> comandos = new ArrayDeque<>();

    private Tela tela = Tela.MENU;
    private Dificuldade dificuldade = Dificuldade.MEDIO;
    private Cobrinha jogo = new Cobrinha(COLUNAS, LINHAS, dificuldade);
    private boolean pausado = false;
    private boolean iniciado = false;
    private boolean novoRecorde = false;

    public Jogo() {
        setPreferredSize(new Dimension(COLUNAS * CELULA, LINHAS * CELULA));
        setBackground(FUNDO);
        setFocusable(true);

        timer = new Timer(dificuldade.atrasoInicial, e -> tick());

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                teclaPressionada(e.getKeyCode());
            }
        });
    }

    // ---------------------------------------------------------------- entrada

    private void teclaPressionada(int tecla) {
        if (tela == Tela.MENU) {
            teclaMenu(tecla);
        } else {
            teclaJogo(tecla);
        }
        repaint();
    }

    private void teclaMenu(int tecla) {
        Dificuldade[] niveis = Dificuldade.values();
        int i = dificuldade.ordinal();
        switch (tecla) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> dificuldade = niveis[(i + niveis.length - 1) % niveis.length];
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> dificuldade = niveis[(i + 1) % niveis.length];
            case KeyEvent.VK_1 -> dificuldade = niveis[0];
            case KeyEvent.VK_2 -> dificuldade = niveis[1];
            case KeyEvent.VK_3 -> dificuldade = niveis[2];
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> iniciarPartida();
            default -> { }
        }
    }

    private void teclaJogo(int tecla) {
        Direcao d = switch (tecla) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> Direcao.CIMA;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> Direcao.BAIXO;
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> Direcao.ESQUERDA;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> Direcao.DIREITA;
            default -> null;
        };

        boolean parado = !iniciado || pausado || jogo.isFimDeJogo();

        if (d != null) {
            if (jogo.isFimDeJogo() || pausado) {
                return;
            }
            if (!iniciado) {
                iniciado = true;
                timer.start();
            }
            if (comandos.size() < 2) {
                comandos.addLast(d);
            }
        } else if (tecla == KeyEvent.VK_P && iniciado && !jogo.isFimDeJogo()) {
            pausado = !pausado;
            if (pausado) timer.stop(); else timer.start();
        } else if ((tecla == KeyEvent.VK_ENTER || tecla == KeyEvent.VK_R) && jogo.isFimDeJogo()) {
            iniciarPartida();
        } else if ((tecla == KeyEvent.VK_ESCAPE || tecla == KeyEvent.VK_M) && parado) {
            timer.stop();
            tela = Tela.MENU; // só sai com o jogo parado, para não perder a partida sem querer
        }
    }

    // ---------------------------------------------------------------- andamento

    private void iniciarPartida() {
        jogo = new Cobrinha(COLUNAS, LINHAS, dificuldade);
        comandos.clear();
        pausado = false;
        iniciado = false;
        novoRecorde = false;
        timer.stop();
        timer.setDelay(dificuldade.atrasoInicial);
        tela = Tela.JOGO;
    }

    private void tick() {
        jogo.mover(comandos.pollFirst());

        if (jogo.isFimDeJogo()) {
            timer.stop();
            novoRecorde = recordes.registrar(dificuldade, jogo.getPontos());
        } else {
            timer.setDelay(calcularAtraso());
        }
        repaint();
    }

    /** Acelera um pouco a cada comida; a comida de lentidão segura a velocidade por um tempo. */
    private int calcularAtraso() {
        int atraso = Math.max(dificuldade.atrasoMinimo,
                dificuldade.atrasoInicial - jogo.getComidasComidas() * 3);
        if (jogo.getTicksLentidao() > 0) {
            atraso = (int) (atraso * FATOR_LENTIDAO);
        }
        return atraso;
    }

    // ---------------------------------------------------------------- desenho

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        desenharGrade(g);

        if (tela == Tela.MENU) {
            desenharMenu(g);
            return;
        }

        desenharObstaculos(g);
        desenharComida(g);
        desenharBonus(g);
        desenharCobra(g);
        desenharPlacar(g);

        if (jogo.isFimDeJogo()) {
            String titulo = jogo.isVenceu() ? "VOCÊ VENCEU!" : "FIM DE JOGO";
            String linha1 = "Pontos: " + jogo.getPontos() + (novoRecorde ? "  -  NOVO RECORDE!" : "");
            mensagem(g, titulo, linha1, "ENTER: jogar de novo    ESC: menu");
        } else if (!iniciado) {
            mensagem(g, dificuldade.nome.toUpperCase(), "Use as setas ou WASD para começar", "ESC: voltar ao menu");
        } else if (pausado) {
            mensagem(g, "PAUSADO", "P para continuar", "ESC: menu");
        }
    }

    private void desenharGrade(Graphics2D g) {
        g.setColor(GRADE);
        g.setStroke(new BasicStroke(1));
        for (int i = 1; i < COLUNAS; i++) g.drawLine(i * CELULA, 0, i * CELULA, getHeight());
        for (int i = 1; i < LINHAS; i++) g.drawLine(0, i * CELULA, getWidth(), i * CELULA);
    }

    private void desenharObstaculos(Graphics2D g) {
        for (Point o : jogo.getObstaculos()) {
            g.setColor(OBSTACULO);
            g.fillRoundRect(o.x * CELULA + 1, o.y * CELULA + 1, CELULA - 2, CELULA - 2, 4, 4);
            g.setColor(OBSTACULO_BORDA);
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(o.x * CELULA + 3, o.y * CELULA + 3, CELULA - 7, CELULA - 7, 3, 3);
        }
    }

    private void desenharComida(Graphics2D g) {
        Point c = jogo.getComida();
        if (c != null) {
            g.setColor(COMIDA);
            g.fillOval(c.x * CELULA + 3, c.y * CELULA + 3, CELULA - 6, CELULA - 6);
        }
    }

    /**
     * Comida dourada = losango amarelo; comida de lentidão = círculo azul com cruz.
     * O arco ao redor mostra quanto tempo falta, e a peça pisca perto do fim.
     */
    private void desenharBonus(Graphics2D g) {
        Point b = jogo.getBonus();
        if (b == null) {
            return;
        }
        int restante = jogo.getTicksBonus();
        if (restante <= 15 && restante % 2 == 0) {
            return;
        }

        int x = b.x * CELULA;
        int y = b.y * CELULA;
        int cx = x + CELULA / 2;
        int cy = y + CELULA / 2;
        boolean ouro = jogo.getTipoBonus() == TipoComida.DOURADA;

        g.setColor(ouro ? OURO : GELO);
        if (ouro) {
            int r = CELULA / 2 - 3;
            g.fillPolygon(new int[]{cx, cx + r, cx, cx - r}, new int[]{cy - r, cy, cy + r, cy}, 4);
        } else {
            g.fillOval(x + 4, y + 4, CELULA - 8, CELULA - 8);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2));
            g.drawLine(cx - 4, cy, cx + 4, cy);
            g.drawLine(cx, cy - 4, cx, cy + 4);
        }

        g.setColor(ouro ? OURO : GELO);
        g.setStroke(new BasicStroke(2));
        int angulo = 360 * restante / Cobrinha.DURACAO_BONUS;
        g.drawArc(x + 1, y + 1, CELULA - 3, CELULA - 3, 90, angulo);
    }

    private void desenharCobra(Graphics2D g) {
        boolean lento = jogo.getTicksLentidao() > 0;
        boolean cabeca = true;
        for (Point p : jogo.getCorpo()) {
            g.setColor(cabeca ? CABECA : (lento ? CORPO_LENTO : CORPO));
            g.fillRoundRect(p.x * CELULA + 1, p.y * CELULA + 1, CELULA - 2, CELULA - 2, 8, 8);
            cabeca = false;
        }
    }

    private void desenharPlacar(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(Color.WHITE);
        int recorde = Math.max(recordes.get(dificuldade), jogo.getPontos());
        String texto = "Pontos: " + jogo.getPontos() + "   Recorde: " + recorde + "   [" + dificuldade.nome + "]";
        g.drawString(texto, 8, 18);

        if (jogo.getTicksLentidao() > 0) {
            g.setColor(GELO);
            g.drawString("LENTO " + jogo.getTicksLentidao(), 8, 36);
        }
    }

    private void desenharMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, getWidth(), getHeight());

        int meio = getWidth() / 2;
        g.setColor(CABECA);
        g.setFont(new Font("SansSerif", Font.BOLD, 46));
        centralizar(g, "COBRINHA", meio, 100);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        centralizar(g, "Escolha a dificuldade", meio, 138);

        Dificuldade[] niveis = Dificuldade.values();
        int largura = 400;
        int altura = 56;
        for (int i = 0; i < niveis.length; i++) {
            Dificuldade d = niveis[i];
            int x = meio - largura / 2;
            int y = 165 + i * (altura + 12);
            boolean selecionada = d == dificuldade;

            g.setColor(selecionada ? new Color(0x2C3A4F) : new Color(0x1F2634));
            g.fillRoundRect(x, y, largura, altura, 12, 12);
            g.setColor(selecionada ? CABECA : GRADE);
            g.setStroke(new BasicStroke(selecionada ? 2.5f : 1.5f));
            g.drawRoundRect(x, y, largura, altura, 12, 12);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString((i + 1) + "  " + d.nome, x + 16, y + 24);
            g.setColor(new Color(0xA0A8B8));
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.drawString(d.descricao, x + 16, y + 44);

            String rec = "Recorde: " + recordes.get(d);
            g.setColor(OURO);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(rec, x + largura - 16 - fm.stringWidth(rec), y + 24);
        }

        // legenda dos itens
        int ly = 395;
        int lx = meio - 190;
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));

        g.setColor(OURO);
        int r = 8;
        g.fillPolygon(new int[]{lx + 10, lx + 10 + r, lx + 10, lx + 10 - r},
                new int[]{ly - 5 - r, ly - 5, ly - 5 + r, ly - 5}, 4);
        g.setColor(Color.WHITE);
        g.drawString("Comida dourada: vale 3 pontos, mas some rápido", lx + 28, ly);

        ly += 26;
        g.setColor(GELO);
        g.fillOval(lx + 2, ly - 13, 16, 16);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawLine(lx + 6, ly - 5, lx + 14, ly - 5);
        g.drawLine(lx + 10, ly - 9, lx + 10, ly - 1);
        g.drawString("Comida azul: deixa o jogo mais lento por um tempo", lx + 28, ly);

        ly += 26;
        g.setColor(OBSTACULO);
        g.fillRoundRect(lx + 2, ly - 13, 16, 16, 4, 4);
        g.setColor(Color.WHITE);
        g.drawString("Blocos cinza: obstáculos, bateu perdeu", lx + 28, ly);

        g.setColor(new Color(0xA0A8B8));
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        centralizar(g, "Setas ou 1-3 para escolher   -   ENTER para jogar", meio, getHeight() - 22);
    }

    private void centralizar(Graphics2D g, String texto, int centroX, int baseY) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto, centroX - fm.stringWidth(texto) / 2, baseY);
    }

    private void mensagem(Graphics2D g, String titulo, String linha1, String linha2) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, getWidth(), getHeight());

        int meio = getWidth() / 2;
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        centralizar(g, titulo, meio, getHeight() / 2 - 14);

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(novoRecorde && jogo.isFimDeJogo() ? OURO : Color.WHITE);
        centralizar(g, linha1, meio, getHeight() / 2 + 18);

        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(0xC8CEDA));
        centralizar(g, linha2, meio, getHeight() / 2 + 46);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame janela = new JFrame("Cobrinha");
            Jogo painel = new Jogo();
            janela.add(painel);
            janela.pack();
            janela.setResizable(false);
            janela.setLocationRelativeTo(null);
            janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            janela.setVisible(true);
            painel.requestFocusInWindow();
        });
    }
}
