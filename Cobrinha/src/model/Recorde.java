package model;

import model.Cobrinha.Dificuldade;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Guarda o melhor placar de cada dificuldade em um arquivo de texto (formato .properties).
 * Se o arquivo não existir, estiver corrompido ou não puder ser gravado, o jogo segue normalmente:
 * o recorde só deixa de ser lembrado entre execuções.
 */
public class Recorde {

    private final Path arquivo;
    private final Properties props = new Properties();

    public Recorde(Path arquivo) {
        this.arquivo = arquivo;
        carregar();
    }

    /** Arquivo padrão: ~/.cobrinha/recordes.properties */
    public static Recorde padrao() {
        return new Recorde(Path.of(System.getProperty("user.home"), ".cobrinha", "recordes.properties"));
    }

    public int get(Dificuldade d) {
        try {
            return Math.max(0, Integer.parseInt(props.getProperty(d.name(), "0").trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Registra o placar se ele superar o recorde. Devolve true quando foi um novo recorde. */
    public boolean registrar(Dificuldade d, int pontos) {
        if (pontos <= get(d)) {
            return false;
        }
        props.setProperty(d.name(), String.valueOf(pontos));
        salvar();
        return true;
    }

    private void carregar() {
        if (!Files.isRegularFile(arquivo)) {
            return;
        }
        try (InputStream in = Files.newInputStream(arquivo)) {
            props.load(in);
        } catch (IOException | IllegalArgumentException e) {
            props.clear(); // arquivo ilegível: começa do zero
        }
    }

    private void salvar() {
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            try (OutputStream out = Files.newOutputStream(arquivo)) {
                props.store(out, "Recordes do jogo Cobrinha");
            }
        } catch (IOException e) {
            // sem permissão de escrita etc.: ignora, o recorde vale só nesta execução
        }
    }
}
